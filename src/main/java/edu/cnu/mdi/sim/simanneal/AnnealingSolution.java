package edu.cnu.mdi.sim.simanneal;

/**
 * Interface for solutions used in simulated annealing.
 * Annealing solutions must support deep copying.
 */
public interface AnnealingSolution extends Cloneable {
    AnnealingSolution copy();   // deep copy
}
