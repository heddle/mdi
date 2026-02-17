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

	// the actual simulation
	private final Simulation simulation;
	
	// configuration (immutable) holds parameters for engine behavior such as refresh 
	// intervals and auto-run
	private final SimulationEngineConfig config;
	
	// context (e.g., state) shared with the simulation; also used for bookkeeping 
	// such as step count and cancellation flag
	private final SimulationContext context = new SimulationContext();

	// listeners (e.g., views) are stored in a thread-safe list since they can be 
	// added/removed from the EDT while the simulation thread is running
	private final List<SimulationListener> listeners = new CopyOnWriteArrayList<>();

	// State is volatile since it's read by the EDT and written by the simulation thread.
	private volatile SimulationState state = SimulationState.NEW;

	// The simulation thread that runs the main loop. 
	// Marked volatile to ensure visibility across threads.
	private volatile Thread simThread;

	private volatile boolean pauseRequested;
	private volatile boolean stopRequested;

	// Coalescing flags to avoid flooding the EDT.
	private final AtomicBoolean refreshPending = new AtomicBoolean(false);
	private final AtomicBoolean progressPending = new AtomicBoolean(false);
	private final AtomicBoolean messagePending = new AtomicBoolean(false);

	// Last posted payloads (coalesced).
	private volatile ProgressInfo lastProgress;
	private volatile String lastMessage;

	/**
	 * Create an engine.
	 *
	 * @param simulation computation plugin
	 * @param config     engine configuration
	 */
	public SimulationEngine(Simulation simulation, SimulationEngineConfig config) {
		this.simulation = Objects.requireNonNull(simulation, "simulation");
		this.config = Objects.requireNonNull(config, "config");
	}

	/**
	 * Add a listener.
	 *
	 * @param listener listener to add
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
	 * Get the current state.
	 *
	 * @return state
	 */
	public SimulationState getState() {
		return state;
	}

	/**
	 * Start the engine. No-op if already started.
	 */
	public synchronized void start() {
		if (simThread != null) {
			return;
		}
		stopRequested = false;
		pauseRequested = false;

		simThread = new Thread(this::runLoop, "SimulationEngine");
		simThread.setDaemon(true);
		simThread.start();
	}

	/**
	 * Request a pause.
	 */
	public void requestPause() {
		pauseRequested = true;
	}

	/**
	 * Request resume from PAUSED (or start running from READY).
	 */
	public void requestResume() {
		pauseRequested = false;
	}

	/**
	 * Alias for {@link #requestResume()}.
	 */
	public void requestRun() {
		requestResume();
	}

	/**
	 * Request stop (normal termination).
	 */
	public void requestStop() {
		stopRequested = true;
		pauseRequested = false;
	}

	/**
	 * Request cancellation (cooperative).
	 */
	public void requestCancel() {
		context.requestCancel();
		pauseRequested = false;
		postEDT(l -> l.onCancelRequested(context));
	}

	/**
	 * Request a UI refresh (coalesced).
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
	 * Post progress information to listeners (coalesced).
	 *
	 * @param info progress info
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
	 * Post a message to listeners (coalesced).
	 *
	 * @param message message
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

			// Cooperative yield: rate-limited. This avoids "sleep per step" which is
			// catastrophic for fast inner loops (e.g., TSP annealing).
			final long coopEveryNs = (config.cooperativeYieldMs > 0) ? (config.cooperativeYieldMs * 1_000_000L) : 0L;
			long lastCoopYield = System.nanoTime();

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

				// Cooperative yield (rate-limited)
				// NOTE: Thread.yield() is intentionally used instead of park/sleep:
				// tiny parks in a hot loop can deschedule the thread and incur OS timer
				// granularity delays that are orders of magnitude larger than requested.
				if (coopEveryNs > 0 && (now - lastCoopYield) >= coopEveryNs) {
					Thread.yield();
					lastCoopYield = now;
				}
			}

			// Termination
			transition(SimulationState.TERMINATING, "stop/complete");
			try {
				simulation.shutdown(context);
			} catch (Exception ignored) {
				// best-effort
			}
			transition(SimulationState.TERMINATED, "done");

		} catch (Exception ex) {
			transition(SimulationState.FAILED, ex.toString());
			postEDT(l -> l.onFail(context, ex));
		}
	}
	
	/**
	 * Get the simulation instance.
	 *
	 * @return simulation
	 */
	public Simulation getSimulation() {
		return simulation;
	}
	
	/**
	 * Get the engine configuration.
	 *
	 * @return config
	 */
	public SimulationEngineConfig getConfig() {
		return config;
	}
	
	/**
	 * Get the simulation context.
	 *
	 * @return context
	 */
	public SimulationContext getContext() {
		return context;
	}
	

	// Transition state and notify listeners on the EDT.
	private void transition(SimulationState newState, String reason) {
	    SimulationState old = state;
	    state = newState;

	    // Always report state change
	    postEDT(l -> l.onStateChange(context, old, newState, reason));

	    // ALSO fire lifecycle callbacks that correspond to key states
	    switch (newState) {
	        case INITIALIZING:
	            postEDT(l -> l.onInit(context));
	            break;

	        case READY:
	            postEDT(l -> l.onReady(context));
	            break;

	        case TERMINATED:
	            postEDT(l -> l.onDone(context));
	            break;

	        default:
	            // no-op (RUNNING/PAUSED handled explicitly elsewhere; FAILED posts onFail explicitly)
	            break;
	    }
	}

	private void postEDT(java.util.function.Consumer<SimulationListener> call) {
		if (listeners.isEmpty()) {
			return;
		}
		SwingUtilities.invokeLater(() -> {
			for (SimulationListener l : listeners) {
				try {
					call.accept(l);
				} catch (Exception ignored) {
					// Listener failures are isolated.
				}
			}
		});
	}
}
