package edu.cnu.mdi.sim.ga;

import java.util.List;
import java.util.Random;

/**
 * Interface for replacement operators in genetic algorithms. A replacement operator defines how the next 
 * generation of the population is formed from the current population and the offspring.
 * The GA will call this interface to determine which individuals survive to the next generation 
 * based on their fitness.
 */
public interface ReplacementOperator<T extends GASolution> {
    // Given old population + offspring, return the next generation
    List<T> replace(List<T> population, List<T> offspring,
                    double[] popFitness, double[] offFitness, Random rng);
}