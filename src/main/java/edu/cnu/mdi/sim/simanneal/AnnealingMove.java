package edu.cnu.mdi.sim.simanneal;

/**
 * A move that can be applied to an annealing solution.
 *
 * @param <S> the type of annealing solution
 */
public interface AnnealingMove<S extends AnnealingSolution> {

    /**
     * Apply the move to the solution.
     * @param sol the solution to apply the move on
     */
    void apply(S sol);

    /**
     * Undo the move (optional but recommended for speed).
     * @param sol the solution to undo the move on
     */
    default void undo(S sol) { throw new UnsupportedOperationException(); }
}
