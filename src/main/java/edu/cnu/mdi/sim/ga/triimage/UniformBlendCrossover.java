package edu.cnu.mdi.sim.ga.triimage;

import java.util.List;
import java.util.Random;

import edu.cnu.mdi.sim.ga.CrossoverOperator;

/**
 * A simple uniform blend crossover operator for PolygonChromosome. For each gene, 
 * randomly select from one of the two parents.
 * This is a common and straightforward crossover method that can be effective for many problems.
 */
public final class UniformBlendCrossover implements CrossoverOperator<PolygonChromosome> {

	@Override
	public List<PolygonChromosome> crossover(PolygonChromosome p1, PolygonChromosome p2, Random rng) {
		PolygonChromosome child = new PolygonChromosome(p1.numTriangles);
		for (int i = 0; i < p1.genes.length; i++) {
			child.genes[i] = rng.nextBoolean() ? p1.genes[i] : p2.genes[i];
		}
		return List.of(child);
	}
}