package edu.cnu.mdi.sim.ga.triimage;

import java.util.Random;

import edu.cnu.mdi.sim.ga.MutationOperator;

/**
 * Mutation operator that applies Gaussian noise to the genes of a
 * {@link PolygonChromosome}.
 *
 * <h2>Mutation model</h2>
 * <p>
 * Each triangle is mutated with probability {@code mutationRate}. A selected
 * triangle receives one coherent edit: translation, scaling about its centroid,
 * movement of one vertex, color/opacity adjustment, or an occasional full reset.
 * This preserves useful triangles while still exploring their local neighborhood.
 * </p>
 * <p>The effective mutation rate and Gaussian scale decay exponentially over
 * roughly the first 3,000 generations, retaining 25% and 20% of their initial
 * values respectively. This shifts the search from exploration toward fine
 * adjustment without ever eliminating mutation entirely.</p>
 *
 * <h2>Typical values</h2>
 * <ul>
 * <li>{@code mutationRate} — {@code 0.02} to {@code 0.05} per triangle</li>
 * <li>{@code sigma} — {@code 0.03} to {@code 0.08}; larger values explore more
 * aggressively but can destabilize good solutions</li>
 * <li>{@code resetProbability} — {@code 0.05} to {@code 0.15}; keep small so
 * resets are rare escapes, not routine disruption</li>
 * </ul>
 */
public final class GaussianMutation implements MutationOperator<PolygonChromosome> {

	/** Per-triangle probability of applying a mutation. */
	private final double mutationRate;

	/** Standard deviation of the Gaussian perturbation. */
	private final double sigma;

	/** Generation-adjusted per-triangle mutation probability. */
	private double effectiveMutationRate;

	/** Generation-adjusted Gaussian scale. */
	private double effectiveSigma;

	/**
	 * Probability that a selected gene is fully reset to a uniform random value
	 * rather than perturbed. Must be in {@code [0, 1]}.
	 */
	private final double resetProbability;

	/**
	 * Construct with explicit mutation rate, sigma, and reset probability.
	 *
	 * @param mutationRate     per-triangle mutation probability (must be in [0, 1])
	 * @param sigma            standard deviation of Gaussian perturbation (must be
	 *                         &gt; 0)
	 * @param resetProbability probability of full gene reset vs. perturbation (must
	 *                         be in [0, 1])
	 * @throws IllegalArgumentException if any argument is out of range
	 */
	public GaussianMutation(double mutationRate, double sigma, double resetProbability) {
		if (!Double.isFinite(mutationRate) || mutationRate < 0 || mutationRate > 1)
			throw new IllegalArgumentException("mutationRate must be in [0,1]");
		if (!Double.isFinite(sigma) || sigma <= 0)
			throw new IllegalArgumentException("sigma must be > 0");
		if (!Double.isFinite(resetProbability) || resetProbability < 0 || resetProbability > 1)
			throw new IllegalArgumentException("resetProbability must be in [0,1]");

		this.mutationRate = mutationRate;
		this.sigma = sigma;
		this.resetProbability = resetProbability;
		this.effectiveMutationRate = mutationRate;
		this.effectiveSigma = sigma;
	}

	/**
	 * Convenience constructor with no reset mutations. Equivalent to
	 * {@code GaussianMutation(mutationRate, sigma, 0.0)}.
	 *
	 * @param mutationRate per-triangle mutation probability
	 * @param sigma        standard deviation of Gaussian perturbation
	 */
	public GaussianMutation(double mutationRate, double sigma) {
		this(mutationRate, sigma, 0.0);
	}

	@Override
	public void beginGeneration(long generation, long maxGenerations) {
		double cooling = Math.exp(-Math.max(0L, generation) / 1500.0);
		effectiveMutationRate = mutationRate * (0.25 + 0.75 * cooling);
		effectiveSigma = sigma * (0.20 + 0.80 * cooling);
	}

	@Override
	public PolygonChromosome mutate(PolygonChromosome individual, Random rng) {
		return mutateAtRate(individual, rng, effectiveMutationRate);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Interprets {@code configuredRate} as a direct per-triangle mutation
	 * probability (finer-grained than the interface's default whole-individual
	 * gate), then applies this generation's cooling to it. The cooling is
	 * applied as the dimensionless ratio {@code effectiveMutationRate /
	 * mutationRate}, which reduces to the cooling schedule's {@code (0.25 +
	 * 0.75 * cooling)} factor regardless of this instance's own
	 * {@code mutationRate} — so {@code configuredRate} need not match the
	 * constructor's {@code mutationRate} for the cooling curve to apply
	 * correctly.</p>
	 */
	@Override
	public PolygonChromosome mutate(PolygonChromosome individual, Random rng,
			double configuredRate) {
		double coolingMultiplier = mutationRate == 0.0
				? 1.0 : effectiveMutationRate / mutationRate;
		return mutateAtRate(individual, rng, configuredRate * coolingMultiplier);
	}

	private PolygonChromosome mutateAtRate(PolygonChromosome individual, Random rng,
			double rate) {
		for (int t = 0; t < individual.numTriangles; t++) {
			if (rng.nextDouble() >= rate) {
				continue;
			}
			int base = t * PolygonChromosome.DOUBLES_PER_TRIANGLE;
			if (rng.nextDouble() < resetProbability) {
				for (int g = 0; g < PolygonChromosome.DOUBLES_PER_TRIANGLE; g++) {
					individual.genes[base + g] = rng.nextDouble();
				}
				continue;
			}

			switch (rng.nextInt(5)) {
			case 0 -> translate(individual.genes, base, rng);
			case 1 -> scale(individual.genes, base, rng);
			case 2 -> moveVertex(individual.genes, base, rng);
			case 3 -> mutateColor(individual.genes, base, rng);
			default -> individual.genes[base + 9] = perturb(
					individual.genes[base + 9], rng, effectiveSigma);
			}
		}
		return individual;
	}

	private void translate(double[] genes, int base, Random rng) {
		double dx = rng.nextGaussian() * effectiveSigma;
		double dy = rng.nextGaussian() * effectiveSigma;
		for (int vertex = 0; vertex < 3; vertex++) {
			int offset = base + 2 * vertex;
			genes[offset] = clamp(genes[offset] + dx);
			genes[offset + 1] = clamp(genes[offset + 1] + dy);
		}
	}

	private void scale(double[] genes, int base, Random rng) {
		double cx = (genes[base] + genes[base + 2] + genes[base + 4]) / 3.0;
		double cy = (genes[base + 1] + genes[base + 3] + genes[base + 5]) / 3.0;
		double factor = Math.max(0.25, 1.0 + rng.nextGaussian() * effectiveSigma * 2.0);
		for (int vertex = 0; vertex < 3; vertex++) {
			int offset = base + 2 * vertex;
			genes[offset] = clamp(cx + (genes[offset] - cx) * factor);
			genes[offset + 1] = clamp(cy + (genes[offset + 1] - cy) * factor);
		}
	}

	private void moveVertex(double[] genes, int base, Random rng) {
		int offset = base + 2 * rng.nextInt(3);
		genes[offset] = perturb(genes[offset], rng, effectiveSigma);
		genes[offset + 1] = perturb(genes[offset + 1], rng, effectiveSigma);
	}

	private void mutateColor(double[] genes, int base, Random rng) {
		for (int channel = 6; channel <= 8; channel++) {
			genes[base + channel] = perturb(genes[base + channel], rng, effectiveSigma);
		}
	}

	private static double perturb(double value, Random rng, double sigma) {
		return clamp(value + rng.nextGaussian() * sigma);
	}

	private static double clamp(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}
}
