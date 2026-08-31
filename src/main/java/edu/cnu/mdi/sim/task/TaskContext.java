package edu.cnu.mdi.sim.task;

import java.util.Objects;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;

/**
 * Services made available to a {@link BackgroundTask} while it executes.
 *
 * <p>The context is owned by its {@link TaskHandle} and is valid for the
 * lifetime of that handle. Its methods may be called from the task's worker
 * thread. Progress, message, and refresh notifications are routed through the
 * underlying simulation engine and therefore reach listeners on Swing's EDT.
 * High-frequency progress, message, and refresh notifications retain the
 * engine's coalescing behavior.</p>
 */
public final class TaskContext {

	private final SimulationEngine engine;
	private final SimulationContext simulationContext;

	TaskContext(SimulationEngine engine, SimulationContext simulationContext) {
		this.engine = Objects.requireNonNull(engine, "engine");
		this.simulationContext = Objects.requireNonNull(simulationContext, "simulationContext");
	}

	/**
	 * Tests whether cancellation has been requested.
	 *
	 * @return {@code true} after the first call to {@link TaskHandle#cancel()}
	 */
	public boolean isCancellationRequested() {
		return simulationContext.isCancelRequested();
	}

	/**
	 * Aborts the current task cooperatively if cancellation was requested.
	 *
	 * <p>The resulting exception is intercepted by MDI and reported as
	 * cancellation, not failure.</p>
	 *
	 * @throws TaskCancelledException if cancellation was requested
	 */
	public void throwIfCancellationRequested() {
		if (isCancellationRequested()) {
			throw new TaskCancelledException();
		}
	}

	/**
	 * Publishes determinate progress. Values outside {@code [0,1]} are clamped
	 * by {@link ProgressInfo#determinate(double, String)}.
	 *
	 * @param fraction completed fraction
	 * @param message optional user-facing status text
	 */
	public void reportProgress(double fraction, String message) {
		engine.postProgress(ProgressInfo.determinate(fraction, message));
	}

	/**
	 * Publishes indeterminate progress.
	 *
	 * @param message optional user-facing status text
	 */
	public void reportIndeterminateProgress(String message) {
		engine.postProgress(ProgressInfo.indeterminate(message));
	}

	/**
	 * Publishes transient user-facing status text.
	 *
	 * @param message non-null message
	 * @throws NullPointerException if {@code message} is null
	 */
	public void postMessage(String message) {
		engine.postMessage(message);
	}

	/** Requests a coalesced UI refresh from registered listeners. */
	public void requestRefresh() {
		engine.requestRefresh();
	}

	/**
	 * Returns elapsed wall-clock time since the worker engine started.
	 *
	 * @return elapsed seconds, or zero before startup bookkeeping completes
	 */
	public double getElapsedSeconds() {
		return simulationContext.getElapsedSeconds();
	}
}
