package edu.cnu.mdi.sim;

/**
 * Simulation engine states. These represent what the engine "is" currently
 * doing.
 * <p>
 * States are owned and managed by {@link SimulationEngine}. Clients typically
 * observe state transitions via
 * {@link SimulationListener#onStateChange(SimulationContext, SimulationState, SimulationState, String)}.
 * </p>
 */
public enum SimulationState {

	/**
	 * Engine created but not started.
	 */
	NEW,

	/**
	 * {@link Simulation#init(SimulationContext)} is being executed on the
	 * simulation thread.
	 */
	INITIALIZING,

	/**
	 * Initialization is complete; engine is ready to run.
	 */
	READY,

	/**
	 * The engine is actively executing {@link Simulation#step(SimulationContext)}
	 * in a loop.
	 */
	RUNNING,

	/**
	 * The engine is paused and not executing steps. It can be resumed.
	 */
	PAUSED,

	/**
	 * The engine is switching an internal phase, mode, or sub-state.
	 * <p>
	 * This is optional but useful for multi-stage simulations (e.g., loading →
	 * solving → post-processing).
	 * </p>
	 */
	SWITCHING,

	/**
	 * The engine is terminating due to stop/cancel/completion.
	 */
	TERMINATING,

	/**
	 * The simulation ended normally (or via stop/cancel) and shutdown has
	 * completed.
	 */
	TERMINATED,

	/**
	 * The engine encountered an unrecoverable error.
	 */
	FAILED
}
