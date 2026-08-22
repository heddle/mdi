package edu.cnu.mdi.sim.simanneal;

/**
 * Marker for a proposed annealing transition. Implementations must also
 * implement either {@link ReversibleAnnealingMove} for an efficient in-place
 * transition or {@link CandidateAnnealingMove} to produce a separate candidate.
 * The split makes rejection behavior explicit and prevents a missing undo
 * implementation from corrupting the working solution.
 *
 * @param <S> the type of annealing solution
 */
public interface AnnealingMove<S extends AnnealingSolution<S>> {
}
