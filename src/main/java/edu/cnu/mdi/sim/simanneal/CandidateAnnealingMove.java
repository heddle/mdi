package edu.cnu.mdi.sim.simanneal;

/**
 * An annealing proposal that produces a separate candidate solution. This form
 * is useful when an efficient or reliable in-place undo operation is not
 * available.
 *
 * @param <S> solution type
 */
@FunctionalInterface
public interface CandidateAnnealingMove<S extends AnnealingSolution<S>>
		extends AnnealingMove<S> {

	/**
	 * Produce a non-null candidate without modifying {@code current}.
	 *
	 * @param current current solution
	 * @return proposed candidate solution
	 */
	S candidate(S current);
}
