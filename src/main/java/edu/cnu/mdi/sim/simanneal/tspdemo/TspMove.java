package edu.cnu.mdi.sim.simanneal.tspdemo;

import java.util.Random;

import edu.cnu.mdi.sim.simanneal.DeltaEnergyMove;

/**
 * A 2-opt move for TSP that supports O(1) ΔE computation.
 *
 * <p>
 * The move reverses a contiguous segment {@code [i..k]} in-place.
 * It is parameterized by the endpoints {@code i < k} chosen in
 * {@link #prepare(TspSolution)}. The same prepared parameters are used
 * by {@link #deltaE(TspSolution)} and {@link #apply(TspSolution)}.
 * </p>
 *
 * <p>
 * Undo is performed by reversing the same segment again.
 * </p>
 */
public class TspMove implements DeltaEnergyMove<TspSolution> {

    private final Random rng;

    /** Prepared segment endpoints (i < k). */
    private int i, k;

    /** Whether this move has been prepared for the current step. */
    private boolean prepared;

    public TspMove(Random rng) {
        this.rng = rng;
    }

    /**
     * Choose a non-degenerate 2-opt segment for the next move.
     *
     * @param sol current solution (non-null)
     */
    @Override
    public void prepare(TspSolution sol) {
        pickSegment(sol.tour.length);
        prepared = true;
    }

    /**
     * Compute the exact ΔE for the prepared 2-opt reversal.
     *
     * @param sol current solution (non-null)
     * @return ΔE = E_after - E_before for the prepared segment
     * @throws IllegalStateException if called before {@link #prepare}
     */
    @Override
    public double deltaE(TspSolution sol) {
        if (!prepared) {
            throw new IllegalStateException("TspMove.deltaE called before prepare()");
        }

        int[] t = sol.tour;
        int n = t.length;

        int a = t[(i - 1 + n) % n];
        int b = t[i];
        int c = t[k];
        int d = t[(k + 1) % n];

        double removed = sol.edgeLength(a, b) + sol.edgeLength(c, d);
        double added   = sol.edgeLength(a, c) + sol.edgeLength(b, d);

        return added - removed;
    }

    /**
     * Apply the prepared 2-opt reversal in-place.
     *
     * @param sol current solution (non-null)
     * @throws IllegalStateException if called before {@link #prepare}
     */
    @Override
    public void apply(TspSolution sol) {
        if (!prepared) {
            throw new IllegalStateException("TspMove.apply called before prepare()");
        }
        reverse(sol.tour, i, k);
    }

    /**
     * Undo by reversing the same segment again.
     *
     * @param sol current solution (non-null)
     */
    @Override
    public void undo(TspSolution sol) {
        // Undo is valid after apply; reverse is its own inverse.
        reverse(sol.tour, i, k);

        // Clear prepared so this move instance isn't accidentally reused.
        prepared = false;
    }

    /**
     * Pick a valid segment (i,k) for 2-opt.
     * Reject adjacent indices and the full-wrap segment.
     */
    private void pickSegment(int n) {
        int ii, kk;
        do {
            ii = rng.nextInt(n);
            kk = rng.nextInt(n);
            if (ii > kk) {
                int t = ii; ii = kk; kk = t;
            }
        } while (kk <= ii + 1 || (ii == 0 && kk == n - 1));
        this.i = ii;
        this.k = kk;
    }

    /** Reverse tour segment [i..k] in-place. */
    private static void reverse(int[] tour, int i, int k) {
        while (i < k) {
            int tmp = tour[i];
            tour[i] = tour[k];
            tour[k] = tmp;
            i++;
            k--;
        }
    }
}
