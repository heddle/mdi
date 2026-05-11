package edu.cnu.mdi.sim.simanneal;

/**
 * Interface for defining an annealing schedule, which determines the temperature
 * at each step of the simulated annealing process and when to stop.
 */
public interface AnnealingSchedule {
    /** Temperature at step k (0-based), given config. */
    double temperature(long step, SimulatedAnnealingConfig cfg);

    /** Whether annealing should stop at this step. */
    default boolean shouldStop(long step, SimulatedAnnealingConfig cfg) {
        return step >= cfg.maxSteps();
    }
}
