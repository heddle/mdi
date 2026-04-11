package edu.cnu.mdi.sim.ga;

import java.util.List;

//The thing the GA optimizes over — a typed population wrapper.
//The GA mutates this in place each generation.
/**
 * Interface for a population of GA solutions. The GA will maintain and evolve a population of individuals.
 * The population must support copying for elitism and best-tracking.
 */
public interface GAPopulation<T extends GASolution> {
 List<T> individuals();
 int size();
 GAPopulation<T> copy();     // for elitism / best-tracking
}
