package edu.cnu.mdi.sim.ga.triimage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.sim.SimulationState;

class ImageEvolutionOperatorsTest {

	@Test
	void targetImageCanOnlyChangeInEditableStates() {
		assertTrue(ImageEvolutionDemoView.isImageChangeAllowed(SimulationState.READY));
		assertTrue(ImageEvolutionDemoView.isImageChangeAllowed(SimulationState.PAUSED));
		assertTrue(ImageEvolutionDemoView.isImageChangeAllowed(SimulationState.TERMINATED));
		assertTrue(ImageEvolutionDemoView.isImageChangeAllowed(SimulationState.FAILED));
		assertFalse(ImageEvolutionDemoView.isImageChangeAllowed(SimulationState.RUNNING));
		assertFalse(ImageEvolutionDemoView.isImageChangeAllowed(SimulationState.INITIALIZING));
	}

	@Test
	void crossoverCopiesWholeTriangles() {
		PolygonChromosome first = chromosomeFilledWith(2, 0.1);
		PolygonChromosome second = chromosomeFilledWith(2, 0.9);
		var children = new UniformBlendCrossover()
				.crossover(first, second, new Random(42));
		assertEquals(2, children.size());
		PolygonChromosome child = children.get(0);
		PolygonChromosome complement = children.get(1);

		for (int triangle = 0; triangle < child.numTriangles; triangle++) {
			int base = triangle * PolygonChromosome.DOUBLES_PER_TRIANGLE;
			double expected = child.genes[base];
			assertTrue(expected == 0.1 || expected == 0.9);
			assertNotEquals(expected, complement.genes[base]);
			for (int gene = 1; gene < PolygonChromosome.DOUBLES_PER_TRIANGLE; gene++) {
				assertEquals(expected, child.genes[base + gene]);
				assertEquals(complement.genes[base], complement.genes[base + gene]);
			}
		}
	}

	@Test
	void mutationKeepsAllGenesInRange() {
		PolygonChromosome chromosome = chromosomeFilledWith(20, 0.5);
		GaussianMutation mutation = new GaussianMutation(1.0, 0.5, 0.2);
		mutation.beginGeneration(10_000, 100_000);
		mutation.mutate(chromosome, new Random(7));

		for (double gene : chromosome.genes) {
			assertTrue(gene >= 0.0 && gene <= 1.0);
		}
	}

	@Test
	void problemUsesTargetAverageAsBackground() {
		BufferedImage target = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < target.getHeight(); y++) {
			for (int x = 0; x < target.getWidth(); x++) {
				target.setRGB(x, y, new Color(80, 120, 160).getRGB());
			}
		}
		ImageApproximationProblem problem = new ImageApproximationProblem(target, 1);
		PolygonChromosome chromosome = problem.randomIndividual(new Random(1));

		assertEquals(new Color(80, 120, 160).getRGB(), chromosome.backgroundRgb);
		assertNotEquals(Color.BLACK.getRGB(), chromosome.backgroundRgb);
		assertArrayEquals(chromosome.getGenesCopy(), chromosome.copy().getGenesCopy());
		assertEquals(chromosome.backgroundRgb, chromosome.copy().backgroundRgb);
	}

	@Test
	void lineAwareFitnessAddsAnEdgeErrorSignal() {
		BufferedImage target = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < 50; y++) {
			for (int x = 0; x < 50; x++) {
				target.setRGB(x, y, x < 25 ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
			}
		}
		ImageApproximationProblem colorProblem = new ImageApproximationProblem(
				target, 1, ImageFitnessMode.COLOR_MSE);
		ImageApproximationProblem lineProblem = new ImageApproximationProblem(
				target, 1, ImageFitnessMode.LINE_AWARE);
		PolygonChromosome chromosome = chromosomeFilledWith(1, 0.5);

		assertEquals(ImageFitnessMode.LINE_AWARE, lineProblem.getFitnessMode());
		assertNotEquals(colorProblem.fitness(chromosome),
				lineProblem.fitness(chromosome));
	}

	private static PolygonChromosome chromosomeFilledWith(int triangles, double value) {
		PolygonChromosome chromosome = new PolygonChromosome(triangles);
		java.util.Arrays.fill(chromosome.genes, value);
		return chromosome;
	}
}
