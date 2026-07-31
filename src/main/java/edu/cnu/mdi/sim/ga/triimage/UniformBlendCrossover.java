package edu.cnu.mdi.sim.ga.triimage;

import java.util.List;
import java.util.Random;

import edu.cnu.mdi.sim.ga.CrossoverOperator;

/**
 * Uniform crossover that treats each encoded triangle as an indivisible building
 * block. All ten genes for a child triangle come from the same parent, preserving
 * useful combinations of geometry, color, and opacity. Two complementary
 * children are returned: whenever the first receives a triangle from one parent,
 * the second receives the corresponding triangle from the other. This retains
 * more population diversity than discarding half of each mating's genetic material.
 */
public final class UniformBlendCrossover implements CrossoverOperator<PolygonChromosome> {

	@Override
	public List<PolygonChromosome> crossover(PolygonChromosome p1, PolygonChromosome p2, Random rng) {
		if (p1.numTriangles != p2.numTriangles) {
			throw new IllegalArgumentException("Parents must have equal triangle counts");
		}
		PolygonChromosome firstChild = new PolygonChromosome(p1.numTriangles, p1.backgroundRgb);
		PolygonChromosome secondChild = new PolygonChromosome(p1.numTriangles, p1.backgroundRgb);
		for (int triangle = 0; triangle < p1.numTriangles; triangle++) {
			int base = triangle * PolygonChromosome.DOUBLES_PER_TRIANGLE;
			boolean firstParentFirst = rng.nextBoolean();
			double[] firstSource = firstParentFirst ? p1.genes : p2.genes;
			double[] secondSource = firstParentFirst ? p2.genes : p1.genes;
			System.arraycopy(firstSource, base, firstChild.genes, base,
					PolygonChromosome.DOUBLES_PER_TRIANGLE);
			System.arraycopy(secondSource, base, secondChild.genes, base,
					PolygonChromosome.DOUBLES_PER_TRIANGLE);
		}
		return List.of(firstChild, secondChild);
	}
}
