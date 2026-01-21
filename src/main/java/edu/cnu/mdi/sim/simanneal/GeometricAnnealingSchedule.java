package edu.cnu.mdi.sim.simanneal;

/**
 * A standard geometric (exponential) annealing schedule.
 *
 * <p>
 * The temperature decays according to:
 * </p>
 *
 * <pre>
 *   T(k) = alpha^k
 * </pre>
 *
 * <p>
 * where {@code k = step / stepsPerTemperature}. The absolute temperature
 * scale ({@code T0}) is handled by {@link SimulatedAnnealingSimulation},
 * which multiplies this factor by its internally estimated initial
 * temperature.
 * </p>
 *
 * <p>
 * This schedule is suitable for:
 * </p>
 * <ul>
 *   <li>Traveling Salesman Problem (TSP)</li>
 *   <li>Layout and decluttering problems</li>
 *   <li>General combinatorial optimization</li>
 * </ul>
 *
 * <h2>Stopping</h2>
 * <p>
 * Annealing stops when {@code step >= cfg.maxSteps()} (default behavior).
 * </p>
 */
public final class GeometricAnnealingSchedule implements AnnealingSchedule {

    @Override
    public double temperature(long step, SimulatedAnnealingConfig cfg) {

        long spt = cfg.stepsPerTemperature();
        long k = (spt <= 0) ? step : (step / spt);

        return Math.pow(cfg.alpha(), k);
    }
}
