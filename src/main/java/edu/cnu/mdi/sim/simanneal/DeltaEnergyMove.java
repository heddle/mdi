package edu.cnu.mdi.sim.simanneal;

/**
 * Optional interface for annealing moves that can provide an O(1) (or cheap)
 * energy difference (ΔE) for the proposed move.
 *
 * <p>
 * A {@code DeltaEnergyMove} must ensure that the ΔE returned by
 * {@link #deltaE(AnnealingSolution)} corresponds to the <em>same</em> move that
 * will be performed by {@link #apply(AnnealingSolution)}.
 * </p>
 *
 * <p>
 * To support this, the annealer will call {@link #prepare(AnnealingSolution)}
 * once per step before requesting ΔE and applying the move. Implementations
 * should choose their random parameters in {@code prepare} and then reuse them
 * in both {@code deltaE} and {@code apply}.
 * </p>
 *
 * @param <S> solution type
 */
public interface DeltaEnergyMove<S extends AnnealingSolution> extends AnnealingMove<S> {

    /**
     * Prepare the move for evaluation and application.
     * <p>
     * This is called once per annealing step before {@link #deltaE} and
     * {@link #apply}. Implementations typically choose random indices or other
     * parameters here.
     * </p>
     *
     * @param solution the current solution (non-null)
     */
    default void prepare(S solution) {
        // default no-op
    }

    /**
     * Compute the change in energy if this prepared move were applied.
     * <p>
     * Must be consistent with the subsequent call to {@link #apply}.
     * </p>
     *
     * @param solution the current solution (non-null)
     * @return energy change (new - old)
     */
    double deltaE(S solution);
}
