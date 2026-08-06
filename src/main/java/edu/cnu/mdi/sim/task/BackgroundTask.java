package edu.cnu.mdi.sim.task;

/**
 * A single background computation managed by MDI's simulation infrastructure.
 *
 * <p>A background task differs from a step-oriented simulation in one important
 * way: its entire calculation is expressed by one invocation of
 * {@link #execute(TaskContext)}. MDI adapts that invocation to a
 * {@code SimulationEngine}, so the task receives the same daemon-thread
 * execution, cooperative cancellation, coalesced progress reporting, and EDT
 * callback guarantees as a simulation without requiring application code to
 * manufacture a one-step {@code Simulation} implementation.</p>
 *
 * <p>The method always runs off the Swing event-dispatch thread. It may return
 * {@code null}. Long-running implementations should periodically call
 * {@link TaskContext#throwIfCancellationRequested()} or inspect
 * {@link TaskContext#isCancellationRequested()}.</p>
 *
 * @param <R> result type
 */
@FunctionalInterface
public interface BackgroundTask<R> {

	/**
	 * Perform the computation.
	 *
	 * @param context services and cancellation state for this invocation
	 * @return the computed result, which may be {@code null}
	 * @throws Exception if the computation fails
	 */
	R execute(TaskContext context) throws Exception;
}
