package edu.cnu.mdi.sim.task;

/** Factory methods for concise one-shot background execution. */
public final class BackgroundTasks {

	private BackgroundTasks() { }

	/**
	 * Creates an unstarted handle, allowing listeners to be registered before
	 * execution begins.
	 *
	 * @param <R> result type
	 * @param task task to wrap
	 * @return new unstarted handle
	 */
	public static <R> TaskHandle<R> create(BackgroundTask<R> task) {
		return new TaskHandle<>(task);
	}

	/**
	 * Creates and immediately starts a task.
	 *
	 * <p>Use {@link #create(BackgroundTask)} when no callback may be missed,
	 * because a very short submitted task can finish before a listener is added.</p>
	 *
	 * @param <R> result type
	 * @param task task to run
	 * @return started handle
	 */
	public static <R> TaskHandle<R> submit(BackgroundTask<R> task) {
		TaskHandle<R> handle = create(task);
		handle.start();
		return handle;
	}
}
