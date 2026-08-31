package edu.cnu.mdi.sim.ga;

/**
 * Immutable statistics snapshot for a genetic algorithm generation.
 *
 * @param generation generation number, starting at zero
 * @param bestFitness highest fitness observed over the entire run
 * @param meanFitness mean fitness of the current population
 * @param worstFitness lowest fitness in the current population
 * @param diversityIndex population diversity measure, or zero when unavailable
 */
public record GAState(
	    long   generation,
	    double bestFitness,
	    double meanFitness,
	    double worstFitness,
	    double diversityIndex
	) {}
