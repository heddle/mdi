package edu.cnu.mdi.sim;

/**
 * Listener interface for simulation lifecycle, state, UI refresh, and progress
 * events.
 * <p>
 * All listener callbacks are executed on the AWT/Swing Event Dispatch Thread
 * (EDT), so it is safe to update UI components directly inside these methods.
 * </p>
 * <p>
 * Implementers can override only the methods they care about; all methods are
 * default no-ops.
 * </p>
 */
public interface SimulationListener {

	// ------------------------------------------------------------------------
	// Lifecycle / state callbacks (EDT)
	// ------------------------------------------------------------------------

	/**
	 * Called after the engine transitions to {@link SimulationState#INITIALIZING}.
	 *
	 * @param ctx simulation context
	 */
	default void onInit(SimulationContext ctx) {
	}

	/**
	 * Called when initialization completes and the engine reaches
	 * {@link SimulationState#READY}.
	 *
	 * @param ctx simulation context
	 */
	default void onReady(SimulationContext ctx) {
	}

	/**
	 * Called when the engine begins execution (enters
	 * {@link SimulationState#RUNNING}).
	 *
	 * @param ctx simulation context
	 */
	default void onRun(SimulationContext ctx) {
	}

	/**
	 * Called when the engine resumes from {@link SimulationState#PAUSED} back to
	 * {@link SimulationState#RUNNING}.
	 *
	 * @param ctx simulation context
	 */
	default void onResume(SimulationContext ctx) {
	}

	/**
	 * Called when the engine enters {@link SimulationState#PAUSED}.
	 *
	 * @param ctx simulation context
	 */
	default void onPause(SimulationContext ctx) {
	}

	/**
	 * Called on every state transition.
	 *
	 * @param ctx    simulation context
	 * @param from   previous state
	 * @param to     new state
	 * @param reason human-readable reason (may be null)
	 */
	default void onStateChange(SimulationContext ctx, SimulationState from, SimulationState to, String reason) {
	}

	/**
	 * Called when the engine ends normally (reaches
	 * {@link SimulationState#TERMINATED}).
	 *
	 * @param ctx simulation context
	 */
	default void onDone(SimulationContext ctx) {
	}

	/**
	 * Called when the engine fails (reaches {@link SimulationState#FAILED}).
	 *
	 * @param ctx   simulation context
	 * @param error the exception/error that caused failure
	 */
	default void onFail(SimulationContext ctx, Throwable error) {
	}

	/**
	 * Called when cancellation is requested via
	 * {@link SimulationEngine#requestCancel()}.
	 * <p>
	 * This callback occurs on the EDT; the simulation thread observes cancellation
	 * via {@link SimulationContext#isCancelRequested()}.
	 * </p>
	 *
	 * @param ctx simulation context
	 */
	default void onCancelRequested(SimulationContext ctx) {
	}

	// ------------------------------------------------------------------------
	// UI-friendly update callbacks (EDT)
	// ------------------------------------------------------------------------

	/**
	 * Called for a graphics refresh/update event. Views typically repaint in
	 * response.
	 *
	 * @param ctx simulation context
	 */
	default void onRefresh(SimulationContext ctx) {
	}

	/**
	 * Called for progress updates (indeterminate or determinate).
	 *
	 * @param ctx      simulation context
	 * @param progress progress payload
	 */
	default void onProgress(SimulationContext ctx, ProgressInfo progress) {
	}

	/**
	 * Called for status/log messages intended for UI display.
	 *
	 * @param ctx     simulation context
	 * @param message message text (non-null)
	 */
	default void onMessage(SimulationContext ctx, String message) {
	}
}
