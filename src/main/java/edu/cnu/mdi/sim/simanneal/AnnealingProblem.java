package edu.cnu.mdi.sim.simanneal;

import java.util.Random;

public interface AnnealingProblem<S extends AnnealingSolution> {
    /**
     * Compute the energy of a solution.
     * @param sol the solution to evaluate
     */
    double energy(S sol);

    /**
     * Produce a random solution (used for init temperature heuristic too).
     * @param rng the random number generator to use
     * @return a new random solution
     */
    S randomSolution(Random rng);

    /**
     * Produce a random move for the current solution.
     * @param rng the random number generator to use
     * @param current the current solution
     * @return a new random move
     */
    AnnealingMove<S> randomMove(Random rng, S current);
}
