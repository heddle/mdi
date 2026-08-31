package edu.cnu.mdi.sim.simanneal;

/**
 * Interface for solutions used in simulated annealing.
 * Annealing solutions must support deep copying.
 */
public interface AnnealingSolution<S extends AnnealingSolution<S>> {
	/** @return an independent deep copy of this solution */
	S copy();
}
