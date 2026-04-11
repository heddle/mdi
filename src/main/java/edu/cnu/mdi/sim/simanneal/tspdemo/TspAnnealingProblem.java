package edu.cnu.mdi.sim.simanneal.tspdemo;

import java.util.Random;

import edu.cnu.mdi.sim.simanneal.AnnealingMove;
import edu.cnu.mdi.sim.simanneal.AnnealingProblem;

/**
 * {@link AnnealingProblem} adapter that connects the TSP model to the MDI
 * simulated annealing framework.
 *
 * <p>
 * This class is responsible for three things the framework requires:
 * </p>
 * <ol>
 *   <li><b>Solution generation</b> — {@link #randomSolution} produces a
 *       uniformly random permutation of all city indices using a Fisher-Yates
 *       shuffle. This is used both for the initial working solution and, during
 *       initialization, by the {@link edu.cnu.mdi.sim.simanneal.TemperatureHeuristic}
 *       to sample the energy distribution for T₀ estimation.</li>
 *   <li><b>Energy evaluation</b> — {@link #energy} delegates to
 *       {@link TspSolution#getTourLength()}, which sums edge distances including
 *       any river penalty or bonus.</li>
 *   <li><b>Move generation</b> — {@link #randomMove} returns a reusable
 *       {@link TspMove} instance. The move object is allocated once and cached;
 *       its random parameters are chosen freshly each step inside
 *       {@link TspMove#prepare(TspSolution)}.</li>
 * </ol>
 *
 * <h2>Move reuse</h2>
 * <p>
 * Rather than allocating a new {@link TspMove} on every call to
 * {@link #randomMove}, this class caches a single instance in
 * {@link #cachedMove}. This is safe because the framework's inner loop always
 * calls {@link TspMove#prepare} before {@link TspMove#deltaE} and
 * {@link TspMove#apply} or {@link TspMove#undo}, so the move is always in a
 * freshly prepared state when used. Eliminating per-step allocation avoids
 * generating millions of short-lived objects and the GC pressure that entails.
 * </p>
 *
 * <h2>Thread safety</h2>
 * <p>
 * This class is not thread-safe. It is owned exclusively by the simulation
 * thread and must not be shared across threads.
 * </p>
 */
public final class TspAnnealingProblem implements AnnealingProblem<TspSolution> {

    /**
     * The TSP model providing city coordinates, the river position, and the
     * distance function. Shared by reference with {@link TspSolution} instances;
     * never modified after construction.
     */
    private final TspModel model;

    /**
     * Reusable move instance.
     * <p>
     * Allocated lazily on the first call to {@link #randomMove} and reused
     * for every subsequent call. The move's random parameters ({@code i} and
     * {@code k}) are re-chosen each step inside
     * {@link TspMove#prepare(TspSolution)}, so the same object correctly
     * represents a different 2-opt swap every step.
     * </p>
     */
    private TspMove cachedMove;

    /**
     * Construct the problem adapter for the given TSP model.
     *
     * @param model the TSP model containing city coordinates and the optional
     *              river (non-null)
     */
    public TspAnnealingProblem(TspModel model) {
        this.model = model;
    }

    /**
     * Generate a uniformly random tour by shuffling all city indices.
     * <p>
     * Uses a Fisher-Yates shuffle to produce an unbiased random permutation.
     * This method is called by the framework both to create the initial
     * working solution and (repeatedly) during T₀ estimation by the
     * {@link edu.cnu.mdi.sim.simanneal.TemperatureHeuristic}.
     * </p>
     *
     * @param rng source of randomness (non-null)
     * @return a new {@link TspSolution} with a random tour visiting all cities
     */
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

    /**
     * Compute the energy (cost) of a solution as its total tour length.
     * <p>
     * Delegates to {@link TspSolution#getTourLength()}, which sums
     * {@link TspModel#getDistance(int, int)} for all consecutive city pairs
     * including the closing edge. River penalties or bonuses are incorporated
     * by the distance function.
     * </p>
     *
     * @param sol the solution to evaluate (non-null, tour must be initialised)
     * @return total tour length; lower is better
     */
    @Override
    public double energy(TspSolution sol) {
        return sol.getTourLength();
    }

    /**
     * Return a prepared {@link TspMove} for the next annealing step.
     * <p>
     * Returns the cached move instance, creating it on the first call. The
     * framework calls {@link TspMove#prepare(TspSolution)} immediately after
     * this method returns, so the move's random segment parameters are always
     * freshly chosen before {@link TspMove#deltaE} or {@link TspMove#apply}
     * is called.
     * </p>
     *
     * @param rng     source of randomness passed to the move (non-null)
     * @param current the current solution (unused here; segment selection
     *                happens inside {@link TspMove#prepare})
     * @return the reusable {@link TspMove} instance
     */
    @Override
    public AnnealingMove<TspSolution> randomMove(Random rng, TspSolution current) {
        if (cachedMove == null) {
            cachedMove = new TspMove(rng);
        }
        return cachedMove;
    }

    /**
     * Fisher-Yates shuffle for an integer array.
     * <p>
     * Produces a uniformly random permutation in O(n) time.
     * </p>
     *
     * @param a   the array to shuffle in-place
     * @param rng source of randomness
     */
    private static void shuffle(int[] a, Random rng) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }
}