package edu.cnu.mdi.sim.ga;

/**
 * A record to hold the configuration parameters for a genetic algorithm. This includes population size, 
 * number of generations, crossover and mutation rates, elitism count, and logging/refresh intervals.
 * The GA will use these parameters to control the evolution process.
 */
public record GAConfig(int populationSize, int maxGenerations, double crossoverRate, 
		double mutationRate, // per-gene or per-individual depending on operator
		int eliteCount, // carried over unchanged each generation
		long progressEveryGens, // mirrors progressEverySteps
		long refreshEveryGens, // mirrors refreshEverySteps
		long randomSeed) {
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