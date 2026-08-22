package edu.cnu.mdi.sim.ga;

import java.util.Random;

/**
 * Interface for GA problems. A GA problem defines the fitness function and how to generate random individuals.
 * The GA will use this interface to evaluate and evolve the population.
 */
public interface GAProblem<T extends GASolution<T>> {
	/**
	 * Evaluate an individual.
	 * @param individual solution to evaluate
	 * @return finite fitness value; higher is better
	 */
    double fitness(T individual);
	/**
	 * Create one random individual.
	 * @param rng source of randomness
	 * @return new individual
	 */
    T randomIndividual(Random rng);
	/**
	 * Create the initial population.
	 * @param size required population size
	 * @param rng source of randomness
	 * @return population containing exactly {@code size} individuals
	 */
    GAPopulation<T> initialPopulation(int size, Random rng);
}
