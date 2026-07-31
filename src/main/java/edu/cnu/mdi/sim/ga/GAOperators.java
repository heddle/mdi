package edu.cnu.mdi.sim.ga;

import java.util.Objects;

/**
 * A record to hold the operators used in a genetic algorithm. This includes selection, 
 * crossover, mutation, and replacement operators.
 * The GA will use these operators to evolve the population of solutions.
 *
 * @param <C> solution type
 * @param selection parent-selection strategy
 * @param crossover parent recombination strategy
 * @param mutation offspring mutation strategy
 * @param replacement next-generation strategy
 */
public record GAOperators<C extends GASolution>(
	    SelectionOperator<C>   selection,
	    CrossoverOperator<C>   crossover,
	    MutationOperator<C>    mutation,
	    ReplacementOperator<C> replacement
	) {
	public GAOperators {
		Objects.requireNonNull(selection, "selection");
		Objects.requireNonNull(crossover, "crossover");
		Objects.requireNonNull(mutation, "mutation");
		Objects.requireNonNull(replacement, "replacement");
	}
}
