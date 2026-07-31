package edu.cnu.mdi.sim.ga;

import java.util.List;
import java.util.Random;

/**
 * Interface for selection operators in genetic algorithms. A selection operator defines how parents are selected from the population for crossover.
 * The GA will call this interface to select parents based on their fitness.
 */
public interface SelectionOperator<T extends GASolution> {
	/**
	 * Select one parent from an aligned population and fitness array.
	 *
	 * @param population candidate individuals
	 * @param fitnesses fitness at each corresponding population index
	 * @param rng source of randomness
	 * @return selected individual
	 */
    T select(List<T> population, double[] fitnesses, Random rng);
}
