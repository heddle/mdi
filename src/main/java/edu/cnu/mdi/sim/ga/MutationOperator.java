package edu.cnu.mdi.sim.ga;

import java.util.Random;

/**
 * Interface for mutation operators in genetic algorithms. A mutation operator defines 
 * how an individual solution is randomly modified.
 * The GA will call this interface to perform mutation on individuals in the population.
 */
public interface MutationOperator<T extends GASolution> {
    // Mutate in-place; return the same individual
    T mutate(T individual, Random rng);
}