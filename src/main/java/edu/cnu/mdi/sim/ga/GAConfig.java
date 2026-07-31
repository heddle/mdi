package edu.cnu.mdi.sim.ga;

/**
 * A record to hold the configuration parameters for a genetic algorithm. This includes population size, 
 * number of generations, crossover and mutation rates, elitism count, and logging/refresh intervals.
 * The GA will use these parameters to control the evolution process.
 *
 * @param populationSize number of individuals; must be positive
 * @param maxGenerations generation limit; must be non-negative
 * @param crossoverRate probability of applying crossover, in {@code [0,1]}
 * @param mutationRate mutation rate supplied for problem/operator configuration,
 *        in {@code [0,1]}
 * @param eliteCount number of current individuals retained by elitist operators
 * @param progressEveryGens progress interval, or zero to disable progress posts
 * @param refreshEveryGens refresh interval, or zero to disable refresh requests
 * @param randomSeed deterministic seed, or zero for a nondeterministic seed
 */
public record GAConfig(int populationSize, int maxGenerations, double crossoverRate, 
		double mutationRate,
		int eliteCount,
		long progressEveryGens,
		long refreshEveryGens,
		long randomSeed) {

	public GAConfig {
		if (populationSize <= 0) {
			throw new IllegalArgumentException("populationSize must be > 0");
		}
		if (maxGenerations < 0) {
			throw new IllegalArgumentException("maxGenerations must be >= 0");
		}
		if (!Double.isFinite(crossoverRate)
				|| crossoverRate < 0.0 || crossoverRate > 1.0) {
			throw new IllegalArgumentException("crossoverRate must be finite and in [0,1]");
		}
		if (!Double.isFinite(mutationRate)
				|| mutationRate < 0.0 || mutationRate > 1.0) {
			throw new IllegalArgumentException("mutationRate must be finite and in [0,1]");
		}
		if (eliteCount < 0 || eliteCount > populationSize) {
			throw new IllegalArgumentException(
					"eliteCount must be in [0,populationSize]");
		}
		if (progressEveryGens < 0 || refreshEveryGens < 0) {
			throw new IllegalArgumentException("generation intervals must be >= 0");
		}
	}

	/**
	 * Return a practical general-purpose configuration.
	 *
	 * @return default configuration
	 */
	public static GAConfig defaults() {
		return new GAConfig(100, // populationSize
				1000, // maxGenerations
				0.7, // crossoverRate
				0.01, // mutationRate
				2, // eliteCount
				10, // progressEveryGens
				50, // refreshEveryGens
				System.currentTimeMillis() // randomSeed
		);
	}
}
