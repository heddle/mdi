package edu.cnu.mdi.sim.simanneal;

import java.util.Random;

/**
 * Strategy for estimating a suitable initial simulated-annealing temperature.
 *
 * @param <S> solution type handled by the problem
 */
public interface TemperatureHeuristic<S extends AnnealingSolution> {
	/**
	 * Estimate an initial temperature and associated diagnostics.
	 *
	 * @param problem problem whose energy landscape is sampled
	 * @param rng source of randomness
	 * @return non-null estimate with a finite, positive initial temperature
	 */
    InitialTemperature estimate(AnnealingProblem<S> problem, Random rng);
}
