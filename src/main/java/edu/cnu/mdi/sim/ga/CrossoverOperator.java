package edu.cnu.mdi.sim.ga;

import java.util.List;
import java.util.Random;

/**
 * Interface for crossover operators in genetic algorithms. 
 * A crossover operator defines how two parents are combined to produce one or more children.
 * The GA will call this interface to perform crossover between selected parents.
 */
public interface CrossoverOperator<T extends GASolution> {
    // Produce one or two children; return list of length 1 or 2
    List<T> crossover(T parent1, T parent2, Random rng);
}