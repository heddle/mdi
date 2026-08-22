package edu.cnu.mdi.sim.ga;

import java.util.List;
import java.util.Random;

/**
 * Interface for replacement operators in genetic algorithms. A replacement operator defines how the next 
 * generation of the population is formed from the current population and the offspring.
 * The GA will call this interface to determine which individuals survive to the next generation 
 * based on their fitness.
 */
public interface ReplacementOperator<T extends GASolution<T>> {
	/**
	 * Return the number of offspring required to form a population of the given
	 * size. Replacement policies that retain current individuals may request
	 * fewer offspring.
	 *
	 * @param populationSize current population size
	 * @return required offspring count in {@code [0, populationSize]}
	 */
	default int offspringCount(int populationSize) {
		return populationSize;
	}
	/**
	 * Form the next generation from the current population and its offspring.
	 * Fitness arrays are aligned with their corresponding lists. The returned
	 * ordering is unrestricted, but its size must equal the current population.
	 *
	 * @param population current population
	 * @param offspring newly created offspring
	 * @param popFitness current-population fitness values
	 * @param offFitness offspring fitness values
	 * @param rng source of randomness
	 * @return complete next generation
	 */
    List<T> replace(List<T> population, List<T> offspring,
                    double[] popFitness, double[] offFitness, Random rng);
}
