package edu.cnu.mdi.sim.ga;

import java.util.List;
import java.util.Random;

/**
 * Interface for crossover operators in genetic algorithms. 
 * A crossover operator defines how two parents are combined to produce one or more children.
 * The GA will call this interface to perform crossover between selected parents.
 */
public interface CrossoverOperator<T extends GASolution> {
	/**
	 * Combine two parents into one or more new children.
	 *
	 * @param parent1 first selected parent
	 * @param parent2 second selected parent
	 * @param rng source of randomness
	 * @return non-null, non-empty list of non-null children
	 */
    List<T> crossover(T parent1, T parent2, Random rng);
}
