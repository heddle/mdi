package edu.cnu.mdi.sim.simanneal.heuristics;

import java.util.Arrays;
import java.util.Random;

import edu.cnu.mdi.sim.simanneal.AnnealingProblem;
import edu.cnu.mdi.sim.simanneal.AnnealingSolution;
import edu.cnu.mdi.sim.simanneal.InitialTemperature;
import edu.cnu.mdi.sim.simanneal.TemperatureHeuristic;

/**
 * Estimates an initial temperature for simulated annealing by sampling the
 * distribution of energies from random solutions.
 * <p>
 * This heuristic draws a number of independent random solutions from the
 * {@link AnnealingProblem}, computes their energies, and uses robust statistics
 * to estimate a characteristic energy scale {@code ΔE}. The initial temperature
 * {@code T0} is then chosen so that a typical uphill move of size {@code ΔE}
 * would be accepted with a specified probability {@code p}:
 * </p>
 *
 * <pre>
 *     p ≈ exp(-ΔE / T0)  ⇒  T0 = -ΔE / ln(p)
 * </pre>
 *
 * <h2>Robust statistics</h2>
 * <p>
 * Rather than relying on the mean and standard deviation (which can be highly
 * sensitive to outliers), this heuristic uses:
 * </p>
 * <ul>
 *   <li><b>Median</b> of the sampled energies as a location estimate</li>
 *   <li><b>Median Absolute Deviation (MAD)</b> as a robust scale estimate</li>
 * </ul>
 *
 * <p>
 * The MAD is converted to a "sigma-like" scale using the standard normal
 * consistency factor {@code 1.4826}. If the energy landscape is effectively flat
 * (MAD ≈ 0), the heuristic falls back to an interquartile-range-based estimate.
 * </p>
 *
 * <h2>Intended use</h2>
 * <p>
 * This heuristic is designed to be used during simulation initialization to
 * automatically choose a reasonable starting temperature without manual tuning.
 * It is problem-agnostic and works well for a wide range of combinatorial and
 * continuous optimization problems.
 * </p>
 *
 * @param <S> the concrete solution type for the annealing problem
 */
public class EnergyDistributionHeuristic<S extends AnnealingSolution>
        implements TemperatureHeuristic<S> {

    /** Number of random solutions to sample (must be ≥ 10). */
    private final int samples;

    /**
     * Target acceptance probability for a typical uphill move at {@code T0}.
     * <p>
     * Common values are in the range {@code 0.6–0.9}. Larger values correspond
     * to higher initial temperatures and more exploratory behavior.
     * </p>
     */
    private final double targetAcceptance;

    /**
     * Minimum allowed initial temperature.
     * <p>
     * Acts as a guardrail against pathological cases where the estimated
     * energy scale is extremely small or zero.
     * </p>
     */
    private final double minT0;

    /**
     * Construct a new energy-distribution-based temperature heuristic.
     *
     * @param samples           number of random solutions to sample (must be ≥ 10)
     * @param targetAcceptance  desired acceptance probability for a typical
     *                          uphill move (must lie strictly in {@code (0,1)})
     * @param minT0             minimum allowed initial temperature (non-negative)
     * @throws IllegalArgumentException if {@code samples < 10} or
     *                                  {@code targetAcceptance ∉ (0,1)}
     */
    public EnergyDistributionHeuristic(int samples,
                                       double targetAcceptance,
                                       double minT0) {
        if (samples < 10) {
            throw new IllegalArgumentException("samples must be >= 10");
        }
        if (!(targetAcceptance > 0 && targetAcceptance < 1)) {
            throw new IllegalArgumentException("targetAcceptance must be (0,1)");
        }
        this.samples = samples;
        this.targetAcceptance = targetAcceptance;
        this.minT0 = Math.max(0.0, minT0);
    }

    /**
     * Estimate an initial temperature by sampling the energy distribution of
     * random solutions.
     *
     * @param problem the annealing problem providing random solutions and
     *                an energy function
     * @param rng     source of randomness
     * @return an {@link InitialTemperature} record containing the estimated
     *         temperature and diagnostic statistics (median, MAD, sample count)
     */
    @Override
    public InitialTemperature estimate(AnnealingProblem<S> problem, Random rng) {

        // Sample energies of random solutions
        double[] E = new double[samples];
        for (int i = 0; i < samples; i++) {
            S s = problem.randomSolution(rng);
            E[i] = problem.energy(s);
        }
        Arrays.sort(E);

        // Robust location and scale
        double median = medianSorted(E);
        double mad = mad(E, median);

        // Convert MAD to a sigma-like scale (normal consistency)
        double scale = 1.4826 * mad;

        // Fallback for nearly flat landscapes
        double deltaE = (scale > 0)
                ? scale
                : (iqrSorted(E) * 0.7413);

        if (deltaE <= 0) {
            deltaE = 1.0;
        }

        // Solve p ≈ exp(-ΔE / T0) for T0
        double T0 = -deltaE / Math.log(targetAcceptance);

        // Guard against NaN, infinity, or overly small values
        if (!Double.isFinite(T0) || T0 < minT0) {
            T0 = Math.max(minT0, 1e-6);
        }

        return new InitialTemperature(T0, median, mad, samples);
    }

    /**
     * Compute the median of a sorted array.
     *
     * @param s sorted array
     * @return median value
     */
    private static double medianSorted(double[] s) {
        int n = s.length;
        if ((n & 1) == 1) {
            return s[n / 2];
        }
        return 0.5 * (s[n / 2 - 1] + s[n / 2]);
    }

    /**
     * Compute the median absolute deviation (MAD) from a sorted array of energies.
     *
     * @param sortedE sorted energies
     * @param median  precomputed median of {@code sortedE}
     * @return MAD value
     */
    private static double mad(double[] sortedE, double median) {
        double[] dev = new double[sortedE.length];
        for (int i = 0; i < sortedE.length; i++) {
            dev[i] = Math.abs(sortedE[i] - median);
        }
        Arrays.sort(dev);
        return medianSorted(dev);
    }

    /**
     * Compute the interquartile range (IQR) of a sorted array.
     *
     * @param s sorted array
     * @return {@code Q3 − Q1}
     */
    private static double iqrSorted(double[] s) {
        return quantileSorted(s, 0.75) - quantileSorted(s, 0.25);
    }

    /**
     * Compute an arbitrary quantile from a sorted array using linear interpolation.
     *
     * @param s sorted array
     * @param q quantile in {@code [0,1]}
     * @return quantile value
     */
    private static double quantileSorted(double[] s, double q) {
        double pos = q * (s.length - 1);
        int i = (int) Math.floor(pos);
        int j = Math.min(i + 1, s.length - 1);
        double frac = pos - i;
        return (1 - frac) * s[i] + frac * s[j];
    }
}
