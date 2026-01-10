package edu.cnu.mdi.sim;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import javax.swing.SwingUtilities;

/**
 * A state-machine driven simulation engine that runs computation on a dedicated
 * thread and delivers UI callbacks on the Swing/AWT Event Dispatch Thread
 * (EDT).
 * <p>
 * This engine is MDI-agnostic. Views can subscribe as
 * {@link SimulationListener}s.
 * </p>
 *
 * <h2>Threading</h2>
 * <ul>
 * <li>The simulation thread runs {@link Simulation#init(SimulationContext)} and
 * {@link Simulation#step(SimulationContext)}.</li>
 * <li>All listener callbacks are delivered on the EDT using
 * {@link SwingUtilities#invokeLater(Runnable)}.</li>
 * </ul>
 *
 * <h2>Control</h2> Supports pause/resume, stop, and cancel. Cancellation is
 * cooperative and observed via {@link SimulationContext#isCancelRequested()}.
 */
public final class SimulationEngine {

	private final Simulation simulation;
	private final SimulationEngineConfig config;
	private final SimulationContext context = new SimulationContext();

	private final List<SimulationListener> listeners = new CopyOnWriteArrayList<>();

	private volatile SimulationState state = SimulationState.NEW;
	private volatile Thread simThread;

	private volatile boolean pauseRequested;
	private volatile boolean stopRequested;

	// Coalescing flags to avoid flooding the EDT.
	private final AtomicBoolean refreshPending = new AtomicBoolean(false);
	private final AtomicBoolean progressPending = new AtomicBoolean(false);

	/**
	 * Create an engine for a simulation.
	 *
	 * @param simulation simulation implementation (non-null)
	 * @param config     engine config (non-null)
	 */
	public SimulationEngine(Simulation simulation, SimulationEngineConfig config) {
		this.simulation = Objects.requireNonNull(simulation, "simulation");
		this.config = Objects.requireNonNull(config, "config");
	}

	/**
	 * Get the engine's current state.
	 *
	 * @return current state
	 */
	public SimulationState getState() {
		return state;
	}

	/**
	 * Get the shared simulation context.
	 *
	 * @return context
	 */
	public SimulationContext getContext() {
		return context;
	}

	/**
	 * Add a listener for lifecycle/progress/refresh callbacks.
	 * <p>
	 * Listener methods are always invoked on the EDT.
	 * </p>
	 *
	 * @param listener listener to add (ignored if null)
	 */
	public void addListener(SimulationListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	/**
	 * Remove a listener.
	 *
	 * @param listener listener to remove
	 */
	public void removeListener(SimulationListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Start the engine. Calling start multiple times is safe; only the first call
	 * starts a thread.
	 */
	public synchronized void start() {
		if (simThread != null) {
			return;
		}
		simThread = new Thread(this::runLoop, "SimulationThread");
		simThread.setDaemon(true);
		simThread.start();
	}

	/**
	 * Get the simulation instance being executed by this engine.
	 * <p>
	 * This is primarily useful for owners (e.g., views/controllers) that need
	 * access to simulation-specific state for rendering or inspection. The returned
	 * object is the same instance passed into
	 * {@link #SimulationEngine(Simulation, SimulationEngineConfig)}.
	 * </p>
	 *
	 * @return the simulation instance (never null)
	 */
	public Simulation getSimulation() {
		return simulation;
	}

	/**
	 * Request that the engine begin or continue running.
	 * <p>
	 * If paused, this resumes. If currently READY and autoRun is false, this starts
	 * running.
	 * </p>
	 */
	public void requestRun() {
		pauseRequested = false;
	}

	/**
	 * Request pause. The engine will enter {@link SimulationState#PAUSED} at the
	 * next safe point.
	 */
	public void requestPause() {
		pauseRequested = true;
	}

	/**
	 * Request resume from pause. Equivalent to {@link #requestRun()}.
	 */
	public void requestResume() {
		pauseRequested = false;
	}

	/**
	 * Request a normal stop. The engine will terminate and call
	 * {@link Simulation#shutdown(SimulationContext)}.
	 */
	public void requestStop() {
		stopRequested = true;
		pauseRequested = false;
	}

	/**
	 * Request cancellation. This is cooperative:
	 * <ul>
	 * <li>{@link SimulationContext#isCancelRequested()} becomes true</li>
	 * <li>{@link Simulation#cancel(SimulationContext)} is called on the simulation
	 * thread</li>
	 * </ul>
	 */
	public void requestCancel() {
		context.requestCancel();
		postEDT(l -> l.onCancelRequested(context));
		pauseRequested = false;
	}

	/**
	 * Post a UI message to listeners on the EDT.
	 *
	 * @param message message text (ignored if null)
	 */
	public void postMessage(String message) {
		if (message == null) {
			return;
		}
		postEDT(l -> l.onMessage(context, message));
	}

	/**
	 * Post a progress update to listeners on the EDT.
	 * <p>
	 * This method coalesces rapid calls (if many are posted quickly, the EDT will
	 * receive the latest soon).
	 * </p>
	 *
	 * @param progress progress payload (ignored if null)
	 */
	public void postProgress(ProgressInfo progress) {
		if (progress == null) {
			return;
		}
		// Coalesce: ensure at most one pending invokeLater at a time.
		if (progressPending.compareAndSet(false, true)) {
			SwingUtilities.invokeLater(() -> {
				progressPending.set(false);
				for (SimulationListener l : listeners) {
					try {
						l.onProgress(context, progress);
					} catch (Throwable ignored) {
						// listener exceptions must not break the EDT
					}
				}
			});
		}
	}

	/**
	 * Request a graphics refresh event to listeners on the EDT.
	 * <p>
	 * This method is coalesced to prevent EDT flooding.
	 * </p>
	 */
	public void requestRefresh() {
		if (refreshPending.compareAndSet(false, true)) {
			SwingUtilities.invokeLater(() -> {
				refreshPending.set(false);
				for (SimulationListener l : listeners) {
					try {
						l.onRefresh(context);
					} catch (Throwable ignored) {
						// listener exceptions must not break the EDT
					}
				}
			});
		}
	}

	/**
	 * Request a transition into SWITCHING state, typically when changing internal
	 * phases.
	 * <p>
	 * This can be used by the simulation owner (e.g., a controller) to annotate
	 * major phases.
	 * </p>
	 *
	 * @param reason human-readable reason (may be null)
	 */
	public void markSwitching(String reason) {
		transition(SimulationState.SWITCHING, reason);
	}

	// ------------------------------------------------------------------------
	// Internal engine loop
	// ------------------------------------------------------------------------

	private void runLoop() {
		context.markStarted();

		try {
			transition(SimulationState.INITIALIZING, "start");
			simulation.init(context);

			transition(SimulationState.READY, "initialized");

			if (!config.autoRun) {
				// Wait in READY until someone requests run.
				pauseRequested = true;
			} else {
				transition(SimulationState.RUNNING, "auto-run");
				postEDT(l -> l.onRun(context));
			}

			long lastRefresh = System.nanoTime();
			long lastProgress = System.nanoTime();

			while (!stopRequested) {

				// Pause handling
				if (pauseRequested) {
					if (state != SimulationState.PAUSED && state != SimulationState.READY) {
						transition(SimulationState.PAUSED, "pause requested");
						postEDT(l -> l.onPause(context));
					} else if (state == SimulationState.READY) {
						// Remain in READY while waiting to run.
						// No-op here; just wait.
					}
					while (pauseRequested && !stopRequested && !context.isCancelRequested()) {
						LockSupport.parkNanos(10_000_000L); // 10ms
					}
					if (stopRequested || context.isCancelRequested()) {
						break;
					}

					// Starting or resuming
					if (state == SimulationState.READY) {
						transition(SimulationState.RUNNING, "run requested");
						postEDT(l -> l.onRun(context));
					} else {
						transition(SimulationState.RUNNING, "resume");
						postEDT(l -> l.onResume(context));
					}
				}

				// Cancellation check
				if (context.isCancelRequested()) {
					transition(SimulationState.TERMINATING, "cancel requested");
					try {
						simulation.cancel(context);
					} catch (Exception ignored) {
						// best-effort
					}
					break;
				}

				// One simulation step
				boolean keepGoing = simulation.step(context);
				context.incrementStep();

				long now = System.nanoTime();

				// Periodic refresh (rate-limited)
				if (config.refreshIntervalMs > 0 && (now - lastRefresh) >= config.refreshIntervalMs * 1_000_000L) {
					lastRefresh = now;
					requestRefresh();
				}

				// Periodic progress ping (rate-limited, indeterminate by default)
				if (config.progressIntervalMs > 0 && (now - lastProgress) >= config.progressIntervalMs * 1_000_000L) {
					lastProgress = now;
					postProgress(ProgressInfo.indeterminate("Running…"));
				}

				if (!keepGoing) {
					break;
				}

				// Optional cooperative yield
				if (config.cooperativeYieldMs > 0) {
					LockSupport.parkNanos(config.cooperativeYieldMs * 1_000_000L);
				}
			}

			// Termination
			transition(SimulationState.TERMINATING,
					context.isCancelRequested() ? "cancel" : (stopRequested ? "stop" : "complete"));
			try {
				simulation.shutdown(context);
			} catch (Exception ignored) {
				// best-effort
			}

			transition(SimulationState.TERMINATED, "done");
			postEDT(l -> l.onDone(context));

		} catch (Throwable t) {
			state = SimulationState.FAILED;
			postEDT(l -> l.onFail(context, t));
		} finally {
			simThread = null;
		}
	}

	private void transition(SimulationState to, String reason) {
		SimulationState from = this.state;
		this.state = to;
		postEDT(l -> l.onStateChange(context, from, to, reason));

		// State-specific lifecycle hooks
		switch (to) {
		case INITIALIZING -> postEDT(l -> l.onInit(context));
		case READY -> postEDT(l -> l.onReady(context));
		default -> {
			// handled elsewhere
		}
		}
	}

	private void postEDT(java.util.function.Consumer<SimulationListener> call) {
		SwingUtilities.invokeLater(() -> {
			for (SimulationListener l : listeners) {
				try {
					call.accept(l);
				} catch (Throwable ignored) {
					// never let listener exceptions break the EDT
				}
			}
		});
	}
}
