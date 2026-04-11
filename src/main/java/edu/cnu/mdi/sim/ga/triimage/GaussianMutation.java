package edu.cnu.mdi.sim.ga.triimage;

import java.util.Random;

import edu.cnu.mdi.sim.ga.MutationOperator;

/**
 * Mutation operator that applies Gaussian noise to the genes of a
 * {@link PolygonChromosome}.
 *
 * <h2>Mutation model</h2>
 * <p>
 * Each gene is mutated independently with probability {@code mutationRate}.
 * When selected for mutation, two outcomes are possible:
 * </p>
 * <ul>
 * <li><b>Gaussian perturbation</b> (probability {@code 1 - resetProbability}):
 * the gene is shifted by a value drawn from {@code N(0, sigma)} and clamped to
 * {@code [0, 1]}. This explores the neighborhood of the current value.</li>
 * <li><b>Full reset</b> (probability {@code resetProbability}): the gene is
 * replaced by a fresh uniform sample from {@code [0, 1]}. This allows escape
 * from plateaus that small perturbations cannot cross.</li>
 * </ul>
 *
 * <h2>Typical values</h2>
 * <ul>
 * <li>{@code mutationRate} — {@code 0.02} to {@code 0.05} per gene</li>
 * <li>{@code sigma} — {@code 0.03} to {@code 0.08}; larger values explore more
 * aggressively but can destabilize good solutions</li>
 * <li>{@code resetProbability} — {@code 0.05} to {@code 0.15}; keep small so
 * resets are rare escapes, not routine disruption</li>
 * </ul>
 */
public final class GaussianMutation implements MutationOperator<PolygonChromosome> {

	/** Per-gene probability of applying any mutation. */
	private final double mutationRate;

	/** Standard deviation of the Gaussian perturbation. */
	private final double sigma;

	/**
	 * Probability that a selected gene is fully reset to a uniform random value
	 * rather than perturbed. Must be in {@code [0, 1]}.
	 */
	private final double resetProbability;

	/**
	 * Construct with explicit mutation rate, sigma, and reset probability.
	 *
	 * @param mutationRate     per-gene mutation probability (must be in [0, 1])
	 * @param sigma            standard deviation of Gaussian perturbation (must be
	 *                         &gt; 0)
	 * @param resetProbability probability of full gene reset vs. perturbation (must
	 *                         be in [0, 1])
	 * @throws IllegalArgumentException if any argument is out of range
	 */
	public GaussianMutation(double mutationRate, double sigma, double resetProbability) {
		if (mutationRate < 0 || mutationRate > 1)
			throw new IllegalArgumentException("mutationRate must be in [0,1]");
		if (sigma <= 0)
			throw new IllegalArgumentException("sigma must be > 0");
		if (resetProbability < 0 || resetProbability > 1)
			throw new IllegalArgumentException("resetProbability must be in [0,1]");

		this.mutationRate = mutationRate;
		this.sigma = sigma;
		this.resetProbability = resetProbability;
	}

	/**
	 * Convenience constructor with no reset mutations. Equivalent to
	 * {@code GaussianMutation(mutationRate, sigma, 0.0)}.
	 *
	 * @param mutationRate per-gene mutation probability
	 * @param sigma        standard deviation of Gaussian perturbation
	 */
	public GaussianMutation(double mutationRate, double sigma) {
		this(mutationRate, sigma, 0.0);
	}

	@Override
	public PolygonChromosome mutate(PolygonChromosome individual, Random rng) {
		for (int i = 0; i < individual.genes.length; i++) {
			if (rng.nextDouble() < mutationRate) {
				if (resetProbability > 0.0 && rng.nextDouble() < resetProbability) {
					// Full reset — escape plateaus
					individual.genes[i] = rng.nextDouble();
				} else {
					// Gaussian perturbation — explore neighborhood
					double val = individual.genes[i] + rng.nextGaussian() * sigma;
					individual.genes[i] = Math.max(0.0, Math.min(1.0, val));
				}
			}
		}

		// In GaussianMutation.mutate(), after the normal mutation loop:
		// Identify and nudge oversized triangles
		for (int t = 0; t < individual.numTriangles; t++) {
			int base = t * PolygonChromosome.DOUBLES_PER_TRIANGLE;

			// Compute span of this triangle in gene space
			double minX = Math.min(individual.genes[base],
					Math.min(individual.genes[base + 2], individual.genes[base + 4]));
			double maxX = Math.max(individual.genes[base],
					Math.max(individual.genes[base + 2], individual.genes[base + 4]));

			double minY = Math.min(individual.genes[base + 1],
					Math.min(individual.genes[base + 3], individual.genes[base + 5]));
			double maxY = Math.max(individual.genes[base + 1],
					Math.max(individual.genes[base + 3], individual.genes[base + 5]));

			double spanX = maxX - minX;
			double spanY = maxY - minY;

			// Nudge if either axis is oversized
			if ((spanX > 0.6 && spanY > 0.6) && rng.nextDouble() < 0.05) {
				for (int g = 0; g < 6; g++) {
					individual.genes[base + g] = rng.nextDouble();
				}
			}
		}
		return individual;
	}
}