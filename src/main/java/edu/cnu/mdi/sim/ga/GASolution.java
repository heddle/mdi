package edu.cnu.mdi.sim.ga;

/**
 * Interface for solutions used in genetic algorithms. GA solutions must support
 * copying for elitism and best-tracking.
 */
//FIX: self-referential bound makes copy() return C directly,
//eliminating the (C) cast in GeneticAlgorithmSimulation.step()
public interface GASolution extends Cloneable {
	<S extends GASolution> S copy(); // concrete implementations return their own type

	int length();
}