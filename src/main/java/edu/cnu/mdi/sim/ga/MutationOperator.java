package edu.cnu.mdi.sim.ga;

import java.util.Random;

/**
 * Interface for mutation operators in genetic algorithms. A mutation operator defines 
 * how an individual solution is randomly modified.
 * The GA will call this interface to perform mutation on individuals in the population.
 */
public interface MutationOperator<T extends GASolution> {
	/**
	 * Notify the operator before offspring are created for a generation.
	 * Stateful operators may use this to adapt their mutation strength.
	 *
	 * @param generation generation about to be produced, starting at zero
	 * @param maxGenerations configured generation limit
	 */
	default void beginGeneration(long generation, long maxGenerations) {
		// Most mutation operators are generation-independent.
	}

	/**
	 * Mutate an individual, either in-place or by returning a replacement.
	 *
	 * @param individual individual to mutate
	 * @param rng source of randomness
	 * @return non-null mutated individual
	 */
	T mutate(T individual, Random rng);

	/**
	 * Apply the configured mutation probability to one offspring.
	 * Operators with gene-level mutation semantics may override this method and
	 * interpret {@code mutationRate} at that finer granularity.
	 *
	 * @param individual offspring to consider for mutation
	 * @param rng source of randomness
	 * @param mutationRate configured rate in {@code [0,1]}
	 * @return the original or mutated individual; never {@code null}
	 */
	default T mutate(T individual, Random rng, double mutationRate) {
		return rng.nextDouble() < mutationRate ? mutate(individual, rng) : individual;
	}
}
