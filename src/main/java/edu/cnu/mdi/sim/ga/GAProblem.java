package edu.cnu.mdi.sim.ga;

import java.util.Random;

/**
 * Interface for GA problems. A GA problem defines the fitness function and how to generate random individuals.
 * The GA will use this interface to evaluate and evolve the population.
 */
public interface GAProblem<T extends GASolution> {
    double fitness(T individual);       // higher is better (invert for minimization)
    T randomIndividual(Random rng);
    GAPopulation<T> initialPopulation(int size, Random rng);
}