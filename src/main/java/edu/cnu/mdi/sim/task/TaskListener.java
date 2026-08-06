package edu.cnu.mdi.sim.task;

import edu.cnu.mdi.sim.CompletionStatus;
import edu.cnu.mdi.sim.ProgressInfo;

/**
 * Listener for a typed, one-shot {@link BackgroundTask}.
 *
 * <p>Every callback is delivered on Swing's event-dispatch thread. Methods are
 * default no-ops so clients can override only the events they need.</p>
 *
 * @param <R> task result type
 */
public interface TaskListener<R> {

	/** Called when execution begins. @param handle originating handle */
	default void onStarted(TaskHandle<R> handle) { }

	/**
	 * Called for a coalesced progress update.
	 * @param handle originating handle
	 * @param progress latest progress value
	 */
	default void onProgress(TaskHandle<R> handle, ProgressInfo progress) { }

	/**
	 * Called for a coalesced status message.
	 * @param handle originating handle
	 * @param message latest message
	 */
	default void onMessage(TaskHandle<R> handle, String message) { }

	/**
	 * Called after successful computation.
	 * @param handle originating handle
	 * @param result task result, which may be {@code null}
	 */
	default void onSucceeded(TaskHandle<R> handle, R result) { }

	/** Called after cooperative cancellation. @param handle originating handle */
	default void onCancelled(TaskHandle<R> handle) { }

	/** Called after a normal stop request. @param handle originating handle */
	default void onStopped(TaskHandle<R> handle) { }

	/**
	 * Called after failure.
	 * @param handle originating handle
	 * @param error failure thrown by the task or engine lifecycle
	 */
	default void onFailed(TaskHandle<R> handle, Throwable error) { }

	/**
	 * Called exactly once for every terminal outcome, after the corresponding
	 * outcome-specific callback above.
	 * @param handle originating handle
	 * @param status terminal outcome
	 * @param error failure, or {@code null} for a non-failure outcome
	 */
	default void onCompleted(TaskHandle<R> handle, CompletionStatus status, Throwable error) { }
}
