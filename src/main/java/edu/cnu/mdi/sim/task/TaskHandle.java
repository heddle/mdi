package edu.cnu.mdi.sim.task;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import edu.cnu.mdi.sim.CompletionStatus;
import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;
import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.SimulationListener;
import edu.cnu.mdi.sim.SimulationState;

/**
 * Controls and observes one execution of a typed {@link BackgroundTask}.
 *
 * <p>A handle is single-use. Calling {@link #start()} more than once is safe
 * but only the first call starts a worker thread. The task runs on the daemon
 * thread owned by an internal {@link SimulationEngine}; all
 * {@link TaskListener} callbacks run on Swing's event-dispatch thread.</p>
 *
 * <p>Cancellation is cooperative. {@link #cancel()} sets a flag immediately,
 * but the task must periodically inspect its {@link TaskContext} to finish
 * promptly. The result and failure accessors are safe to read from any thread
 * after completion because their backing fields are published with volatile
 * semantics.</p>
 *
 * @param <R> task result type
 */
public final class TaskHandle<R> {

	private final BackgroundTask<R> task;
	private final SimulationEngine engine;
	private final CopyOnWriteArrayList<TaskListener<R>> listeners = new CopyOnWriteArrayList<>();

	private volatile R result;
	private volatile Throwable failure;
	private volatile CompletionStatus completionStatus;

	/**
	 * Creates an unstarted handle with quiet, automatically running defaults.
	 * Periodic simulation refresh and heartbeat events are disabled; explicit
	 * task progress, messages, and refresh requests remain available.
	 *
	 * @param task computation to execute
	 * @throws NullPointerException if {@code task} is null
	 */
	public TaskHandle(BackgroundTask<R> task) {
		this(task, new SimulationEngineConfig(0, 0, 0, true));
	}

	/**
	 * Creates an unstarted handle with an explicit engine configuration.
	 *
	 * <p>The configuration must use automatic running because a one-shot task
	 * has no separate READY/RUN user gesture. This restriction prevents a task
	 * from appearing to start while silently waiting in the simulation READY
	 * state.</p>
	 *
	 * @param task computation to execute
	 * @param config engine scheduling and notification configuration; its
	 *               {@link SimulationEngineConfig#autoRun} field must be true
	 * @throws NullPointerException if either argument is null
	 * @throws IllegalArgumentException if automatic running is disabled
	 */
	public TaskHandle(BackgroundTask<R> task, SimulationEngineConfig config) {
		this.task = Objects.requireNonNull(task, "task");
		Objects.requireNonNull(config, "config");
		if (!config.autoRun) {
			throw new IllegalArgumentException("One-shot tasks require config.autoRun == true");
		}
		engine = new SimulationEngine(new TaskSimulation(), config);
		engine.addListener(new EngineBridge());
	}

	/** Starts this task unless it was already started. */
	public void start() {
		engine.start();
	}

	/**
	 * Requests cooperative cancellation. Repeated calls are harmless.
	 * Cancellation before {@link #start()} prevents the task body from running.
	 */
	public void cancel() {
		engine.requestCancel();
	}

	/**
	 * Adds a listener if the same instance is not already registered.
	 * Registration does not replay events that occurred earlier.
	 *
	 * @param listener listener to add; {@code null} is ignored
	 */
	public void addListener(TaskListener<R> listener) {
		if (listener != null) {
			listeners.addIfAbsent(listener);
		}
	}

	/** Removes a listener. @param listener listener to remove */
	public void removeListener(TaskListener<R> listener) {
		listeners.remove(listener);
	}

	/** @return current underlying engine state */
	public SimulationState getState() {
		return engine.getState();
	}

	/**
	 * Returns the terminal outcome, or {@code null} while execution is pending.
	 *
	 * @return terminal status or {@code null}
	 */
	public CompletionStatus getCompletionStatus() {
		return completionStatus;
	}

	/** @return {@code true} after any terminal outcome has been published */
	public boolean isCompleted() {
		return completionStatus != null;
	}

	/**
	 * Returns the task result. A successful task is permitted to return
	 * {@code null}; callers should inspect {@link #getCompletionStatus()} to
	 * distinguish that result from an incomplete or failed task.
	 *
	 * @return result value, or {@code null}
	 */
	public R getResult() {
		return result;
	}

	/** @return failure, or {@code null} unless completion status is FAILED */
	public Throwable getFailure() {
		return failure;
	}

	/** @return elapsed worker time in seconds */
	public double getElapsedSeconds() {
		return engine.getContext().getElapsedSeconds();
	}

	/**
	 * Waits for the worker thread to finish. This method does not flush pending
	 * EDT callbacks; use listener latches when a test must wait for UI delivery.
	 *
	 * @param timeoutMillis maximum wait, or zero to wait indefinitely
	 * @return {@code true} if the worker has terminated
	 * @throws InterruptedException if interrupted while waiting
	 */
	public boolean awaitTermination(long timeoutMillis) throws InterruptedException {
		return engine.awaitTermination(timeoutMillis);
	}

	/**
	 * Exposes the underlying engine for advanced integration with existing MDI
	 * simulation listeners and diagnostics. Application code should normally
	 * use this handle's typed API.
	 *
	 * @return engine owned by this handle
	 */
	public SimulationEngine getSimulationEngine() {
		return engine;
	}

	private final class TaskSimulation implements Simulation {
		private TaskContext taskContext;

		@Override
		public void init(SimulationContext context) {
			taskContext = new TaskContext(engine, context);
		}

		@Override
		public boolean step(SimulationContext context) throws Exception {
			try {
				result = task.execute(taskContext);
			} catch (TaskCancelledException cancelled) {
				// The engine sees the already-set context flag immediately after this
				// step and takes its normal cancellation path.
				if (!context.isCancelRequested()) {
					throw cancelled;
				}
			}
			return false;
		}
	}

	private final class EngineBridge implements SimulationListener {
		@Override
		public void onRun(SimulationContext context) {
			dispatch(listener -> listener.onStarted(TaskHandle.this));
		}

		@Override
		public void onProgress(SimulationContext context, ProgressInfo progress) {
			dispatch(listener -> listener.onProgress(TaskHandle.this, progress));
		}

		@Override
		public void onMessage(SimulationContext context, String message) {
			dispatch(listener -> listener.onMessage(TaskHandle.this, message));
		}

		@Override
		public void onCompleted(SimulationContext context, CompletionStatus status, Throwable error) {
			failure = error;
			completionStatus = status;
			switch (status) {
				case SUCCEEDED -> dispatch(listener -> listener.onSucceeded(TaskHandle.this, result));
				case CANCELLED -> dispatch(listener -> listener.onCancelled(TaskHandle.this));
				case STOPPED -> dispatch(listener -> listener.onStopped(TaskHandle.this));
				case FAILED -> dispatch(listener -> listener.onFailed(TaskHandle.this, error));
			}
			dispatch(listener -> listener.onCompleted(TaskHandle.this, status, error));
		}
	}

	@FunctionalInterface
	private interface ListenerCall<R> {
		void accept(TaskListener<R> listener);
	}

	private void dispatch(ListenerCall<R> call) {
		for (TaskListener<R> listener : listeners) {
			try {
				call.accept(listener);
			} catch (Throwable ignored) {
				// Match SimulationEngine: one bad listener must not block others.
			}
		}
	}
}
