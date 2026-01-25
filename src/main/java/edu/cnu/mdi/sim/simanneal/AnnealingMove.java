package edu.cnu.mdi.sim.simanneal;

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
