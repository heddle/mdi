package edu.cnu.mdi.sim.ga;

/**
 * Interface for solutions used in genetic algorithms. GA solutions must support
 * copying for elitism and best-tracking.
 */
public interface GASolution<S extends GASolution<S>> {
	/**
	 * Create an independent copy of this solution.
	 *
	 * @return a deep copy whose runtime type matches this solution
	 */
	S copy();

	/**
	 * Return the number of encoded elements in this solution.
	 *
	 * @return solution length
	 */
	int length();
}
