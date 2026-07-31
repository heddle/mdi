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
}
