package edu.cnu.mdi.sim;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Context object shared with the simulation.
 * <p>
 * This object is safe to read from both simulation thread and EDT. The engine
 * owns mutation of internal bookkeeping, except for cancellation which can be
 * requested externally via {@link SimulationEngine#requestCancel()}.
 * </p>
 */
public final class SimulationContext {

	// Internal cancellation flag
	private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

	// Internal bookkeeping
	private volatile long startNanos;
	private volatile long stepCount;

	/**
	 * Package-private constructor (owned by engine).
	 */
	SimulationContext() {
		// package-private construction (owned by engine)
	}

	/**
	 * Mark the simulation as started (internal use only).
	 */
	void markStarted() {
		startNanos = System.nanoTime();
	}
	/**
	 * Increment the step count (internal use only).
	 */
	void incrementStep() {
		stepCount++;
	}

	/**
	 * Records the first cancellation request.
	 *
	 * @return {@code true} only when this call changed the flag
	 */
	boolean requestCancel() {
		return cancelRequested.compareAndSet(false, true);
	}

	/**
	 * Check whether cancellation has been requested.
	 *
	 * @return true if cancellation has been requested
	 */
	public boolean isCancelRequested() {
		return cancelRequested.get();
	}

	/**
	 * Get the number of completed steps.
	 *
	 * @return step count
	 */
	public long getStepCount() {
		return stepCount;
	}

	/**
	 * Get elapsed time since the engine started, in seconds.
	 *
	 * @return elapsed seconds
	 */
	public double getElapsedSeconds() {
		long started = startNanos;
		return (started == 0L)
				? 0.0
				: (System.nanoTime() - started) * 1e-9;
	}
}
