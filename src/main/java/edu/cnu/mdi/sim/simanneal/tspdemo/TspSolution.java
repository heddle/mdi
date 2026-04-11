package edu.cnu.mdi.sim.simanneal.tspdemo;

import edu.cnu.mdi.sim.simanneal.AnnealingSolution;

/**
 * A TSP solution represented as a permutation of city indices.
 *
 * <p>
 * The permutation {@link #tour} defines a closed Hamiltonian cycle through all
 * cities in {@link #model}. City {@code tour[i]} is visited immediately before
 * {@code tour[(i+1) % n]}, and the last city connects back to the first.
 * </p>
 *
 * <h2>Energy</h2>
 * <p>
 * The energy (cost) of a solution is its total tour length, computed by
 * {@link #getTourLength()}. Distances between cities are obtained from
 * {@link TspModel#getDistance(int, int)}, which incorporates the optional
 * river penalty or bonus.
 * </p>
 *
 * <h2>Copying</h2>
 * <p>
 * {@link #copy()} performs a deep copy: the returned {@code TspSolution}
 * shares the same immutable {@link #model} reference but has an independent
 * clone of the {@link #tour} array. The annealing framework calls
 * {@code copy()} to snapshot the best solution found so far; move operators
 * mutate the working solution in-place without affecting the snapshot.
 * </p>
 *
 * <h2>Thread safety</h2>
 * <p>
 * Instances are not thread-safe. The working solution is owned exclusively by
 * the simulation thread. Snapshots produced by {@link #copy()} are handed to
 * the view, which reads them on the EDT.
 * </p>
 */
public class TspSolution implements AnnealingSolution {

    /**
     * The immutable TSP model shared by all solutions in a run.
     * <p>
     * Holds city coordinates, the river position, and the distance function.
     * Shared by reference — never modified after construction.
     * </p>
     */
    public final TspModel model;

    /**
     * Permutation of city indices {@code [0, cityCount)} defining the tour.
     * <p>
     * The tour visits {@code tour[0], tour[1], ..., tour[n-1], tour[0]}.
     * Modified in-place by move operators ({@link TspMove#apply} /
     * {@link TspMove#undo}).
     * </p>
     */
    public int[] tour;

    /**
     * Construct an uninitialised solution for the given model.
     * <p>
     * {@link #tour} is {@code null} until set by
     * {@link TspAnnealingProblem#randomSolution}.
     * </p>
     *
     * @param model the TSP model (non-null)
     */
    public TspSolution(TspModel model) {
        this.model = model;
    }

    /**
     * Return an independent deep copy of this solution.
     * <p>
     * The copy shares the same immutable {@link #model} reference but has its
     * own clone of the {@link #tour} array, so mutations to either solution do
     * not affect the other.
     * </p>
     *
     * @return a new {@code TspSolution} with a cloned tour array
     */
    @Override
    public TspSolution copy() {
        TspSolution copy = new TspSolution(this.model);
        copy.tour = this.tour.clone();
        return copy;
    }

    /**
     * Compute the total length of the closed tour.
     * <p>
     * Sums {@link TspModel#getDistance(int, int)} for every consecutive pair
     * of cities in the tour, including the wrap-around edge from the last city
     * back to the first. If the model includes a river, crossing penalties or
     * bonuses are incorporated by the distance function.
     * </p>
     *
     * @return total tour length (may be negative if river crossings carry a
     *         bonus that exceeds the Euclidean distances)
     */
    public double getTourLength() {
        double length = 0.0;
        for (int i = 0; i < tour.length; i++) {
            int a = tour[i];
            int b = tour[(i + 1) % tour.length];
            length += model.getDistance(a, b);
        }
        return length;
    }

    /**
     * Return the distance between two cities as given by the model.
     * <p>
     * Convenience wrapper used by {@link TspMove#deltaE} to avoid exposing
     * the model directly in ΔE calculations.
     * </p>
     *
     * @param a index of the first city
     * @param b index of the second city
     * @return distance (including any river penalty or bonus)
     */
    public double edgeLength(int a, int b) {
        return model.getDistance(a, b);
    }
}