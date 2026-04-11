package edu.cnu.mdi.sim.ga;

/**
 * A record to hold the operators used in a genetic algorithm. This includes selection, 
 * crossover, mutation, and replacement operators.
 * The GA will use these operators to evolve the population of solutions.
 */
public record GAOperators<C extends GASolution>(
	    SelectionOperator<C>   selection,
	    CrossoverOperator<C>   crossover,
	    MutationOperator<C>    mutation,
	    ReplacementOperator<C> replacement
	) {}