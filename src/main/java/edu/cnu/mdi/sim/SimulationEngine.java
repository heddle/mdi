package edu.cnu.mdi.sim;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import javax.swing.SwingUtilities;

/**
 * A state-machine-driven simulation engine that runs computation on a dedicated
 * background thread and delivers all UI callbacks on the Swing/AWT Event
 * Dispatch Thread (EDT).
 *
 * <p>This engine is MDI-agnostic: it has no dependency on any MDI view class.
 * Views and other UI components participate by subscribing as
 * {@link SimulationListener}s.</p>
 *
 * <h2>Threading model</h2>
 * <ul>
 *   <li>The <em>simulation thread</em> runs {@link Simulation#init},
 *       {@link Simulation#step}, {@link Simulation#cancel}, and
 *       {@link Simulation#shutdown} — all computation stays on this thread.</li>
 *   <li>Every {@link SimulationListener} callback is posted to the EDT via
 *       {@link SwingUtilities#invokeLater}. It is therefore safe to update
 *       Swing components directly inside listener methods.</li>
 *   <li>EDT-to-engine control signals ({@code requestPause},
 *       {@code requestStop}, etc.) are written as {@code volatile} flags and
 *       observed on the simulation thread without further synchronization.</li>
 * </ul>
 *
 * <h2>State machine</h2>
 * <p>The engine progresses through {@link SimulationState} values in order.
 * The normal-completion path is:</p>
 * <pre>
 *   NEW → INITIALIZING → READY → RUNNING → TERMINATING → TERMINATED
 * </pre>
 * <p>Pause/resume loops inside RUNNING without changing the path.
 * Cancellation reaches TERMINATED via the same terminal steps, but
 * {@link SimulationListener#onDone} is <em>not</em> fired — only
 * {@link SimulationListener#onCancelRequested} and the {@code TERMINATED}
 * state-change callback are delivered.</p>
 *
 * <h2>EDT coalescing</h2>
 * <p>High-frequency data (refresh, progress, messages) is coalesced so that
 * at most one pending task of each type is queued on the EDT at any moment.
 * This prevents the EDT queue from backing up when the simulation thread runs
 * faster than the UI can drain it.</p>
 *
 * <h2>Control</h2>
 * <p>Supports pause/resume, stop, and cooperative cancel. Cancellation is
 * observed by the simulation via
 * {@link SimulationContext#isCancelRequested()}.</p>
 */
public final class SimulationEngine {

	/** The computation plugin. Runs exclusively on the simulation thread. */
	private final Simulation simulation;

	/**
	 * Immutable engine configuration. Holds rate-limit intervals and the
	 * {@code autoRun} flag.
	 */
	private final SimulationEngineConfig config;

	/**
	 * Context shared between the engine and the simulation. Safe to read from
	 * both the simulation thread and the EDT; the engine owns all mutation
	 * except for cancellation, which can be requested externally.
	 */
	private final SimulationContext context = new SimulationContext();

	/**
	 * Registered listeners (views, control panels, etc.).
	 *
	 * <p>{@link CopyOnWriteArrayList} is used because listeners are typically
	 * added and removed on the EDT while the simulation thread iterates the list
	 * on every step. The list is read far more often than it is written, making
	 * the copy-on-write trade-off appropriate.</p>
	 */
	private final List<SimulationListener> listeners = new CopyOnWriteArrayList<>();

	/**
	 * Current engine state. Written exclusively on the simulation thread;
	 * read from both the simulation thread and the EDT. {@code volatile}
	 * ensures cross-thread visibility without a lock.
	 */
	private volatile SimulationState state = SimulationState.NEW;

	/**
	 * The dedicated simulation thread. Set once in {@link #start()} and never
	 * reassigned. {@code volatile} so that the EDT can see the thread reference
	 * immediately after {@code start()} returns.
	 */
	private volatile Thread simThread;

	/**
	 * Set to {@code true} by {@link #requestPause()}; cleared by
	 * {@link #requestResume()} / {@link #requestStop()} / cancel.
	 * {@code volatile} so the simulation thread observes EDT writes promptly.
	 */
	private volatile boolean pauseRequested;

	/**
	 * Set to {@code true} by {@link #requestStop()}. Once set, the simulation
	 * loop exits at the next iteration boundary. {@code volatile} for the same
	 * reason as {@link #pauseRequested}.
	 */
	private volatile boolean stopRequested;

	/**
	 * Coalescing guards that prevent flooding the EDT with more than one pending
	 * task of each type at a time.
	 *
	 * <p>Pattern: the simulation thread does a {@code compareAndSet(false, true)}
	 * before posting. The EDT runnable resets the flag to {@code false} before
	 * dispatching, so the next simulation-thread post can proceed immediately.</p>
	 */
	private final AtomicBoolean refreshPending  = new AtomicBoolean(false);
	private final AtomicBoolean progressPending = new AtomicBoolean(false);
	private final AtomicBoolean messagePending  = new AtomicBoolean(false);

	/**
	 * Most recently coalesced payloads. Written on the simulation thread, read
	 * on the EDT inside the corresponding {@code invokeLater} runnable.
	 * {@code volatile} ensures the EDT sees the last write without a lock.
	 */
	private volatile ProgressInfo lastProgress;
	private volatile String       lastMessage;

	/**
	 * Create a new engine for the given simulation and configuration.
	 *
	 * @param simulation the computation plugin; must not be {@code null}
	 * @param config     engine configuration; must not be {@code null}
	 * @throws NullPointerException if either argument is {@code null}
	 */
	public SimulationEngine(Simulation simulation, SimulationEngineConfig config) {
		this.simulation = Objects.requireNonNull(simulation, "simulation");
		this.config     = Objects.requireNonNull(config,     "config");
	}

	/**
	 * Add a simulation listener.
	 *
	 * <p>Safe to call from any thread. If {@code listener} is {@code null} it
	 * is silently ignored.</p>
	 *
	 * @param listener the listener to add
	 */
	public void addListener(SimulationListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	/**
	 * Remove a simulation listener.
	 *
	 * <p>Safe to call from any thread. If {@code listener} is not currently
	 * registered this is a no-op.</p>
	 *
	 * @param listener the listener to remove
	 */
	public void removeListener(SimulationListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Return the current engine state.
	 *
	 * <p>The state is written on the simulation thread and read here without a
	 * lock; the {@code volatile} field guarantees visibility. Treat the returned
	 * value as a snapshot — it may change immediately after this call returns.
	 * </p>
	 *
	 * @return the current {@link SimulationState}; never {@code null}
	 */
	public SimulationState getState() {
		return state;
	}

	/**
	 * Start the simulation engine on a dedicated daemon thread.
	 *
	 * <p>This method is idempotent: if the engine has already been started, the
	 * call is a no-op. The engine thread is a daemon so that it does not prevent
	 * JVM shutdown if the application exits while a simulation is running.</p>
	 */
	public synchronized void start() {
		if (simThread != null) {
			return;
		}
		stopRequested  = false;
		pauseRequested = false;

		simThread = new Thread(this::runLoop, "SimulationEngine");
		simThread.setDaemon(true);
		simThread.start();
	}

	/**
	 * Request that the engine pause at the next safe point.
	 *
	 * <p>The simulation thread observes this flag at the top of each loop
	 * iteration and transitions to {@link SimulationState#PAUSED}. This method
	 * returns immediately; callers that need to know when the pause has taken
	 * effect should observe {@link SimulationListener#onPause}.</p>
	 */
	public void requestPause() {
		pauseRequested = true;
	}

	/**
	 * Request that the engine resume from {@link SimulationState#PAUSED}, or
	 * begin running from {@link SimulationState#READY} when
	 * {@code autoRun == false}.
	 */
	public void requestResume() {
		pauseRequested = false;
	}

	/**
	 * Alias for {@link #requestResume()}.
	 *
	 * <p>Provided so that call sites that conceptually "start running" read
	 * more naturally than those that "resume".</p>
	 */
	public void requestRun() {
		requestResume();
	}

	/**
	 * Request a normal stop.
	 *
	 * <p>The engine exits its step loop at the next iteration boundary,
	 * calls {@link Simulation#shutdown}, transitions to
	 * {@link SimulationState#TERMINATED}, and fires
	 * {@link SimulationListener#onDone}.</p>
	 */
	public void requestStop() {
		stopRequested  = true;
		pauseRequested = false;
	}

	/**
	 * Request cooperative cancellation.
	 *
	 * <p>Sets the cancellation flag in the {@link SimulationContext} (observable
	 * via {@link SimulationContext#isCancelRequested()}) and fires
	 * {@link SimulationListener#onCancelRequested} on the EDT immediately.
	 * The simulation thread observes the flag at the top of its next loop
	 * iteration, calls {@link Simulation#cancel} and then
	 * {@link Simulation#shutdown}, and transitions to
	 * {@link SimulationState#TERMINATED}.
	 *
	 * <p><strong>Note:</strong> {@link SimulationListener#onDone} is
	 * <em>not</em> fired after a cancel. Cancellation is communicated solely
	 * via {@code onCancelRequested} and the {@code TERMINATED}
	 * state-change callback (with {@code reason="cancelled"}).</p>
	 */
	public void requestCancel() {
		context.requestCancel();
		pauseRequested = false;
		postEDT(l -> l.onCancelRequested(context));
	}

	/**
	 * Request a UI refresh (coalesced).
	 *
	 * <p>If a refresh is already pending on the EDT this call is a no-op,
	 * preventing the EDT queue from accumulating stale refresh tasks when the
	 * simulation thread runs faster than the UI.</p>
	 */
	public void requestRefresh() {
		if (refreshPending.compareAndSet(false, true)) {
			postEDT(l -> {
				refreshPending.set(false);
				l.onRefresh(context);
			});
		}
	}

	/**
	 * Post a progress update to listeners (coalesced).
	 *
	 * <p>If a progress event is already pending the new {@code info} replaces
	 * the queued payload — the EDT will deliver the most recent value rather
	 * than every intermediate one. This is appropriate for smooth progress bars
	 * where skipping intermediate percentages is acceptable.</p>
	 *
	 * @param info the progress payload; {@code null} is technically accepted but
	 *             meaningless — prefer {@link ProgressInfo#indeterminate}
	 */
	public void postProgress(ProgressInfo info) {
		lastProgress = info;
		if (progressPending.compareAndSet(false, true)) {
			postEDT(l -> {
				progressPending.set(false);
				l.onProgress(context, lastProgress);
			});
		}
	}

	/**
	 * Post a status message to listeners (coalesced).
	 *
	 * <p>If a message event is already pending the new message replaces the
	 * queued one. This is appropriate for transient status text where only the
	 * most recent value matters.</p>
	 *
	 * @param message the message text; {@code null} is forwarded as-is
	 */
	public void postMessage(String message) {
		lastMessage = message;
		if (messagePending.compareAndSet(false, true)) {
			postEDT(l -> {
				messagePending.set(false);
				l.onMessage(context, lastMessage);
			});
		}
	}

	// -------------------------------------------------------------------------
	// Simulation thread — main loop
	// -------------------------------------------------------------------------

	/**
	 * Main simulation loop. Runs entirely on the dedicated simulation thread.
	 *
	 * <p>State transitions are performed by {@link #transition}, which always
	 * posts the generic {@link SimulationListener#onStateChange} callback and
	 * additionally fires the targeted lifecycle callback ({@code onInit},
	 * {@code onReady}) for the two states that are always entered via
	 * {@code transition}.</p>
	 *
	 * <p>The RUNNING/PAUSED/RESUME cycle and the final TERMINATING → TERMINATED
	 * path are handled explicitly here rather than through {@code transition},
	 * because each requires additional logic around which lifecycle method to
	 * fire and whether to suppress {@code onDone} after a cancel.</p>
	 *
	 * <h3>Cancellation vs. normal termination</h3>
	 * <p>The {@code cancelledCleanly} flag records whether the loop exited due
	 * to a cancel request. If {@code true}, {@link SimulationListener#onDone}
	 * is suppressed; cancellation is communicated via
	 * {@link SimulationListener#onCancelRequested} (fired from
	 * {@link #requestCancel}) and the {@code TERMINATED} state-change callback.
	 * {@link Simulation#shutdown} is always called, regardless of the exit
	 * reason, so simulations can release resources unconditionally.</p>
	 *
	 * <h3>Cooperative yield</h3>
	 * <p>{@link Thread#yield()} is used (rate-limited by {@code coopEveryNs})
	 * rather than {@code park}/{@code sleep}. A tiny {@code park} inside a
	 * tight simulation loop can incur OS timer-granularity delays orders of
	 * magnitude larger than intended and devastate throughput.</p>
	 */
	private void runLoop() {
		context.markStarted();

		// Tracks whether the loop exited due to cancellation, so we can
		// suppress onDone and take the cancellation-specific path.
		boolean cancelledCleanly = false;

		try {
			transition(SimulationState.INITIALIZING, "start");
			simulation.init(context);
			transition(SimulationState.READY, "initialized");

			if (!config.autoRun) {
				// Hold in READY until the user explicitly requests Run.
				pauseRequested = true;
			} else {
				transition(SimulationState.RUNNING, "auto-run");
				postEDT(l -> l.onRun(context));
			}

			long lastRefreshNs  = System.nanoTime();
			// Named distinctly from the field "lastProgress" (a coalesced payload)
			// to avoid accidental shadowing.
			long lastProgressNs = System.nanoTime();

			final long coopEveryNs = (config.cooperativeYieldMs > 0)
					? (config.cooperativeYieldMs * 1_000_000L) : 0L;
			long lastCoopYield = System.nanoTime();

			while (!stopRequested) {

				// --- Pause handling ---
				if (pauseRequested) {
					if (state != SimulationState.PAUSED && state != SimulationState.READY) {
						transition(SimulationState.PAUSED, "pause requested");
						postEDT(l -> l.onPause(context));
					}
					// Spin-park until resumed, stopped, or cancelled.
					while (pauseRequested && !stopRequested && !context.isCancelRequested()) {
						LockSupport.parkNanos(10_000_000L); // 10 ms
					}
					if (stopRequested || context.isCancelRequested()) {
						break;
					}
					// Determine whether we are starting fresh (READY) or resuming.
					if (state == SimulationState.READY) {
						transition(SimulationState.RUNNING, "run requested");
						postEDT(l -> l.onRun(context));
					} else {
						transition(SimulationState.RUNNING, "resume");
						postEDT(l -> l.onResume(context));
					}
				}

				// --- Cancellation check ---
				if (context.isCancelRequested()) {
					cancelledCleanly = true;
					transition(SimulationState.TERMINATING, "cancel requested");
					try {
						simulation.cancel(context);
					} catch (Exception ignored) { /* best-effort */ }
					break;
				}

				// --- One simulation step ---
				boolean keepGoing = simulation.step(context);
				context.incrementStep();

				long now = System.nanoTime();

				if (config.refreshIntervalMs > 0
						&& (now - lastRefreshNs) >= config.refreshIntervalMs * 1_000_000L) {
					lastRefreshNs = now;
					requestRefresh();
				}

				if (config.progressIntervalMs > 0
						&& (now - lastProgressNs) >= config.progressIntervalMs * 1_000_000L) {
					lastProgressNs = now;
					postProgress(ProgressInfo.indeterminate("Running…"));
				}

				if (!keepGoing) {
					break;
				}

				if (coopEveryNs > 0 && (now - lastCoopYield) >= coopEveryNs) {
					Thread.yield();
					lastCoopYield = now;
				}
			}

			// Shutdown is always called, regardless of exit reason, so
			// simulations can release resources unconditionally.
			transition(SimulationState.TERMINATING,
					cancelledCleanly ? "cancelled" : "stop/complete");
			try {
				simulation.shutdown(context);
			} catch (Exception ignored) { /* best-effort */ }

			transition(SimulationState.TERMINATED,
					cancelledCleanly ? "cancelled" : "done");

			// onDone fires only for normal stop/completion. Cancellation is
			// communicated via onCancelRequested() (already fired in
			// requestCancel()) and the TERMINATED state-change above.
			if (!cancelledCleanly) {
				postEDT(l -> l.onDone(context));
			}

		} catch (Exception ex) {
			transition(SimulationState.FAILED, ex.toString());
			postEDT(l -> l.onFail(context, ex));
		}
	}

	// -------------------------------------------------------------------------
	// Accessors
	// -------------------------------------------------------------------------

	/**
	 * Return the {@link Simulation} plugin running inside this engine.
	 *
	 * @return the simulation; never {@code null}
	 */
	public Simulation getSimulation() {
		return simulation;
	}

	/**
	 * Return the immutable engine configuration.
	 *
	 * @return the configuration; never {@code null}
	 */
	public SimulationEngineConfig getConfig() {
		return config;
	}

	/**
	 * Return the {@link SimulationContext} shared with the simulation.
	 *
	 * @return the context; never {@code null}
	 */
	public SimulationContext getContext() {
		return context;
	}

	// -------------------------------------------------------------------------
	// Internal helpers
	// -------------------------------------------------------------------------

	/**
	 * Transition to {@code newState}, fire the generic
	 * {@link SimulationListener#onStateChange} callback on the EDT, and
	 * additionally fire the targeted lifecycle callback for states that are
	 * always entered via this method.
	 *
	 * <h3>Which states fire a targeted callback here</h3>
	 * <ul>
	 *   <li>{@link SimulationState#INITIALIZING} → {@link SimulationListener#onInit}</li>
	 *   <li>{@link SimulationState#READY}        → {@link SimulationListener#onReady}</li>
	 * </ul>
	 *
	 * <p>RUNNING, PAUSED, and RESUME transitions are handled explicitly in
	 * {@link #runLoop} because each requires additional logic (distinguishing
	 * first-run from resume, etc.).</p>
	 *
	 * <p>TERMINATED is handled explicitly in {@link #runLoop} so that
	 * {@link SimulationListener#onDone} can be conditionally suppressed after
	 * a cancel. Firing {@code onDone} from inside this switch would cause a
	 * double-fire on normal completion (once here, once explicitly in
	 * {@code runLoop}).</p>
	 *
	 * <p>FAILED posts {@link SimulationListener#onFail} explicitly in
	 * {@code runLoop} with the causing exception, which this method cannot
	 * access.</p>
	 *
	 * @param newState the state to transition to
	 * @param reason   a short human-readable description of why the transition
	 *                 occurred; delivered to listeners via {@code onStateChange}
	 */
	private void transition(SimulationState newState, String reason) {
		SimulationState old = state;
		state = newState;

		postEDT(l -> l.onStateChange(context, old, newState, reason));

		switch (newState) {
			case INITIALIZING -> postEDT(l -> l.onInit(context));
			case READY        -> postEDT(l -> l.onReady(context));
			// RUNNING/PAUSED: handled explicitly in runLoop (onRun / onResume / onPause).
			// TERMINATING:    no dedicated lifecycle callback.
			// TERMINATED:     onDone fired explicitly in runLoop to allow cancel suppression.
			// FAILED:         onFail fired explicitly in runLoop with the exception.
			default           -> { /* no additional callback for this state */ }
		}
	}

	/**
	 * Post {@code call} to every registered listener on the EDT.
	 *
	 * <p>Exceptions thrown by individual listeners are caught and discarded so
	 * that a misbehaving listener cannot disrupt delivery to the remaining
	 * ones.</p>
	 *
	 * <p>If there are no listeners the EDT task is not even queued.</p>
	 *
	 * @param call the callback to invoke on each listener; executed on the EDT
	 */
	private void postEDT(java.util.function.Consumer<SimulationListener> call) {
		if (listeners.isEmpty()) {
			return;
		}
		SwingUtilities.invokeLater(() -> {
			for (SimulationListener l : listeners) {
				try {
					call.accept(l);
				} catch (Exception ignored) {
					// Listener failures are isolated so one bad listener
					// cannot prevent delivery to the rest.
				}
			}
		});
	}
}