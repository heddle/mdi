package edu.cnu.mdi.sim.task;

import java.util.concurrent.CancellationException;

/**
 * Internal control-flow exception raised by
 * {@link TaskContext#throwIfCancellationRequested()}.
 *
 * <p>Applications normally do not need to catch this exception. The MDI
 * one-shot adapter catches it and completes the task with the
 * {@code CANCELLED} status rather than treating cooperative cancellation as a
 * failure &mdash; but only when the engine's cancellation flag is actually
 * set at that point (i.e. this exception genuinely originated from
 * {@link TaskContext#throwIfCancellationRequested()} in response to a real
 * {@code cancel()} request). Constructing and throwing this exception
 * directly, without a preceding cancellation request, is not equivalent to
 * cooperative cancellation: the adapter finds the cancellation flag unset and
 * rethrows it, completing the task as {@code FAILED} instead. It is public so
 * task code may perform local cleanup in a {@code catch} block when
 * necessary; cleanup that must always run is usually better placed in a
 * {@code finally} block.</p>
 */
public final class TaskCancelledException extends CancellationException {

	private static final long serialVersionUID = 1L;

	/** Creates an exception with a stable diagnostic message. */
	public TaskCancelledException() {
		super("Background task cancellation was requested");
	}
}
