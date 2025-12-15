package edu.cnu.mdi.sim;

/**
 * Computation plugin for {@link SimulationEngine}.
 * <p>
 * A {@code Simulation} contains no UI code. It runs entirely on the simulation thread.
 * It can request UI updates via {@link SimulationEngine#requestRefresh()} and
 * {@link SimulationEngine#postProgress(ProgressInfo)} / {@link SimulationEngine#postMessage(String)}.
 * </p>
 */
public interface Simulation {

    /**
     * Initialize simulation resources. Called exactly once on the simulation thread.
     *
     * @param ctx simulation context
     * @throws Exception any error; will cause engine to transition to FAILED
     */
    void init(SimulationContext ctx) throws Exception;

    /**
     * Execute a single step of the simulation. Called repeatedly on the simulation thread
     * while running.
     *
     * @param ctx simulation context
     * @return true to continue running; false to stop (normal completion)
     * @throws Exception any error; will cause engine to transition to FAILED
     */
    boolean step(SimulationContext ctx) throws Exception;

    /**
     * Shutdown hook called on the simulation thread when the engine is terminating
     * (normal completion or stop/cancel).
     *
     * @param ctx simulation context
     * @throws Exception ignored by the engine (best-effort cleanup)
     */
    default void shutdown(SimulationContext ctx) throws Exception {
        // no-op
    }

    /**
     * Cancellation hook called on the simulation thread after cancel is requested.
     * <p>
     * The simulation should attempt to exit promptly. The engine will still call
     * {@link #shutdown(SimulationContext)} afterwards.
     * </p>
     *
     * @param ctx simulation context
     * @throws Exception ignored by the engine (best-effort cancellation)
     */
    default void cancel(SimulationContext ctx) throws Exception {
        // no-op
    }
}
