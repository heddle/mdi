package edu.cnu.mdi.sim.simanneal;

/**
 * An in-place annealing move that can restore the exact prior solution when a
 * proposal is rejected.
 *
 * @param <S> solution type
 */
public interface ReversibleAnnealingMove<S extends AnnealingSolution<S>>
		extends AnnealingMove<S> {

	/** Apply this move in place. */
	void apply(S solution);

	/** Restore the solution to its state immediately before {@link #apply}. */
	void undo(S solution);
}
