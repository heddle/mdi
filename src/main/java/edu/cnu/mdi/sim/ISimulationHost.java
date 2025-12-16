package edu.cnu.mdi.sim;

/**
 * Convenience interface for any object that *hosts* a {@link SimulationEngine}.
 * <p>
 * This interface is intentionally minimal and MDI-agnostic. It provides:
 * <ul>
 *   <li>a single accessor for the underlying {@link SimulationEngine}</li>
 *   <li>default lifecycle control methods (start, run, pause, resume, stop, cancel)</li>
 * </ul>
 * </p>
 *
 * <h2>Typical Usage</h2>
 * <pre>{@code
 * public class IsingModelView extends BaseView
 *         implements ISimulationHost, SimulationListener {
 *
 *     private final SimulationEngine engine;
 *
 *     public IsingModelView(...) {
 *         engine = new SimulationEngine(new IsingSimulation(), SimulationEngineConfig.defaults());
 *         engine.addListener(this);
 *     }
 *
 *     @Override
 *     public SimulationEngine getSimulationEngine() {
 *         return engine;
 *     }
 *
 *     @Override
 *     public void onRefresh(SimulationContext ctx) {
 *         getContainer().repaint();
 *     }
 * }
 * }</pre>
 *
 * <p>
 * By implementing this interface, UI code (toolbars, menus, control panels)
 * can manipulate simulations in a uniform way without knowing implementation details.
 * </p>
 */
public interface ISimulationHost {

    /**
     * Return the {@link SimulationEngine} hosted by this object.
     *
     * @return the simulation engine (never null)
     */
    SimulationEngine getSimulationEngine();

    // ------------------------------------------------------------------------
    // Convenience lifecycle control methods
    // ------------------------------------------------------------------------

    /**
     * Start the simulation engine.
     * <p>
     * This is safe to call multiple times; only the first call has an effect.
     * </p>
     */
    default void startSimulation() {
        getSimulationEngine().start();
    }

    /**
     * Request the simulation to begin or continue running.
     * <p>
     * If the engine is paused, this resumes execution.
     * If the engine is READY and {@code autoRun == false}, this begins execution.
     * </p>
     */
    default void runSimulation() {
        getSimulationEngine().requestRun();
    }

    /**
     * Request that the simulation pause.
     * <p>
     * The engine will transition to {@link SimulationState#PAUSED} at the next safe point.
     * </p>
     */
    default void pauseSimulation() {
        getSimulationEngine().requestPause();
    }

    /**
     * Resume a paused simulation.
     * <p>
     * This is equivalent to {@link #runSimulation()}.
     * </p>
     */
    default void resumeSimulation() {
        getSimulationEngine().requestResume();
    }

    /**
     * Request a normal stop.
     * <p>
     * The simulation will terminate and invoke {@link Simulation#shutdown(SimulationContext)}
     * on the simulation thread.
     * </p>
     */
    default void stopSimulation() {
        getSimulationEngine().requestStop();
    }

    /**
     * Request cancellation.
     * <p>
     * Cancellation is cooperative. The simulation should observe
     * {@link SimulationContext#isCancelRequested()} and exit promptly.
     * </p>
     */
    default void cancelSimulation() {
        getSimulationEngine().requestCancel();
    }

    /**
     * Convenience method to query the current simulation state.
     *
     * @return current {@link SimulationState}
     */
    default SimulationState getSimulationState() {
        return getSimulationEngine().getState();
    }

    /**
     * Convenience method to query the simulation context.
     *
     * @return simulation context
     */
    default SimulationContext getSimulationContext() {
        return getSimulationEngine().getContext();
    }
}
