package edu.cnu.mdi.sim;

/**
 * Describes why a {@link SimulationEngine} reached a terminal state.
 *
 * <p>The status is delivered exactly once through
 * {@link SimulationListener#onCompleted(SimulationContext, CompletionStatus, Throwable)}.
 * It supplements, rather than replaces, the original lifecycle callbacks. In
 * particular, existing listeners continue to receive {@code onDone},
 * {@code onFail}, and {@code onCancelRequested} with their established
 * semantics.</p>
 */
public enum CompletionStatus {

	/** The simulation returned {@code false} from {@link Simulation#step}. */
	SUCCEEDED,

	/** The engine terminated in response to {@link SimulationEngine#requestStop()}. */
	STOPPED,

	/** The engine terminated in response to cooperative cancellation. */
	CANCELLED,

	/** Initialization, execution, or another engine-managed operation failed. */
	FAILED
}
