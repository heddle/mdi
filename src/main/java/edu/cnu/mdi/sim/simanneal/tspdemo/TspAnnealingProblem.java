package edu.cnu.mdi.sim.simanneal.tspdemo;

import java.util.Random;

import edu.cnu.mdi.sim.simanneal.AnnealingMove;
import edu.cnu.mdi.sim.simanneal.AnnealingProblem;

/**
 * Annealing problem adapter for the TSP demo.
 */
public final class TspAnnealingProblem implements AnnealingProblem<TspSolution> {

    private final TspModel model;

    public TspAnnealingProblem(TspModel model) {
        this.model = model;
    }

    @Override
    public TspSolution randomSolution(Random rng) {
        TspSolution sol = new TspSolution(model);
        sol.tour = new int[model.cityCount];
        for (int i = 0; i < model.cityCount; i++) {
			sol.tour[i] = i;
		}
        shuffle(sol.tour, rng);
        return sol;
    }

    @Override
    public double energy(TspSolution sol) {
        return sol.getTourLength();
    }

    @Override
    public AnnealingMove<TspSolution> randomMove(Random rng, TspSolution current) {
        return new TspMove(rng);
    }

    private static void shuffle(int[] a, Random rng) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int t = a[i]; a[i] = a[j]; a[j] = t;
        }
    }
}
