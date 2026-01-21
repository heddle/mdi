package edu.cnu.mdi.sim.simanneal;

public interface AnnealingSchedule {
    /** Temperature at step k (0-based), given config. */
    double temperature(long step, SimulatedAnnealingConfig cfg);

    /** Whether annealing should stop at this step. */
    default boolean shouldStop(long step, SimulatedAnnealingConfig cfg) {
        return step >= cfg.maxSteps();
    }
}
