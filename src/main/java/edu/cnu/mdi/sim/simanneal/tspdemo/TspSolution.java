package edu.cnu.mdi.sim.simanneal.tspdemo;

import edu.cnu.mdi.sim.simanneal.AnnealingSolution;

/**
 * A TSP solution represented by a permutation of city indices.
 */
public class TspSolution implements AnnealingSolution {

    /** Immutable backing model. */
    public final TspModel model;

    /** Permutation of city indices defining the tour. */
    public int[] tour;

    public TspSolution(TspModel model) {
        this.model = model;
    }

    @Override
    public AnnealingSolution copy() {
        TspSolution copy = new TspSolution(this.model);
        copy.tour = this.tour.clone();
        return copy;
    }

    /**
     * Compute full tour length (closed loop).
     */
    public double getTourLength() {
        double L = 0.0;
        for (int i = 0; i < tour.length; i++) {
            int a = tour[i];
            int b = tour[(i + 1) % tour.length];
            L += model.getDistance(a, b);
        }
        return L;
    }

    /**
     * Convenience method for ΔE calculations.
     */
    public double edgeLength(int a, int b) {
        return model.getDistance(a, b);
    }
}
