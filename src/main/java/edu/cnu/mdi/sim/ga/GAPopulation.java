package edu.cnu.mdi.sim.ga;

import java.util.List;

/**
 * Interface for a population of GA solutions. The GA will maintain and evolve a population of individuals.
 * The population must support copying for elitism and best-tracking.
 */
public interface GAPopulation<T extends GASolution<T>> {
	/** @return individuals in population order */
 List<T> individuals();
	/** @return number of individuals */
 int size();
	/** @return an independent population copy */
 GAPopulation<T> copy();
}
