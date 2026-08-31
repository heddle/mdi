package edu.cnu.mdi.sim.ga.triimage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.sim.ga.GASolution;

/**
 * Regression coverage for {@link ElitistReplacement}, which had zero prior
 * test coverage.
 */
class ElitistReplacementTest {

	private record LabeledSolution(String label) implements GASolution<LabeledSolution> {
		@Override
		public LabeledSolution copy() {
			return this;
		}

		@Override
		public int length() {
			return 1;
		}
	}

	@Test
	void constructorRejectsNegativeEliteCount() {
		assertThrows(IllegalArgumentException.class, () -> new ElitistReplacement<LabeledSolution>(-1));
	}

	@Test
	void offspringCountIsPopulationSizeMinusEliteCount() {
		ElitistReplacement<LabeledSolution> replacement = new ElitistReplacement<>(2);
		assertEquals(8, replacement.offspringCount(10));
	}

	@Test
	void offspringCountRejectsEliteCountLargerThanPopulation() {
		ElitistReplacement<LabeledSolution> replacement = new ElitistReplacement<>(5);
		assertThrows(IllegalArgumentException.class, () -> replacement.offspringCount(3));
	}

	@Test
	void replaceKeepsTheFittestIndividualsAndFillsTheRestWithOffspring() {
		List<LabeledSolution> population = List.of(
				new LabeledSolution("p0"), new LabeledSolution("p1"),
				new LabeledSolution("p2"), new LabeledSolution("p3"));
		double[] popFitness = { 1.0, 5.0, 3.0, 0.5 }; // best-to-worst by index: 1, 2, 0, 3
		List<LabeledSolution> offspring = List.of(
				new LabeledSolution("o0"), new LabeledSolution("o1"));

		ElitistReplacement<LabeledSolution> replacement = new ElitistReplacement<>(2);
		List<LabeledSolution> next = replacement.replace(
				population, offspring, popFitness, new double[] { 0, 0 }, new Random());

		assertEquals(4, next.size());
		// The two fittest parents (indices 1 and 2) survive, in fitness order.
		assertSame(population.get(1), next.get(0));
		assertSame(population.get(2), next.get(1));
		// The rest of the generation is filled with offspring, in order.
		assertSame(offspring.get(0), next.get(2));
		assertSame(offspring.get(1), next.get(3));
	}

	@Test
	void zeroEliteCountKeepsNoneOfTheCurrentPopulation() {
		List<LabeledSolution> population = List.of(new LabeledSolution("p0"), new LabeledSolution("p1"));
		List<LabeledSolution> offspring = List.of(new LabeledSolution("o0"), new LabeledSolution("o1"));

		ElitistReplacement<LabeledSolution> replacement = new ElitistReplacement<>(0);
		List<LabeledSolution> next = replacement.replace(
				population, offspring, new double[] { 1.0, 2.0 }, new double[] { 0, 0 }, new Random());

		assertEquals(offspring, next);
	}

	@Test
	void replaceRejectsTooFewOffspringToFillTheGeneration() {
		List<LabeledSolution> population = List.of(new LabeledSolution("p0"), new LabeledSolution("p1"));
		List<LabeledSolution> offspring = List.of(new LabeledSolution("o0")); // need 1, have 1 -- ok boundary
		ElitistReplacement<LabeledSolution> replacement = new ElitistReplacement<>(1);

		// Exactly enough offspring: must not throw.
		replacement.replace(population, offspring, new double[] { 1.0, 2.0 }, new double[] { 0 }, new Random());

		// One fewer than needed: must throw.
		ElitistReplacement<LabeledSolution> stricter = new ElitistReplacement<>(0);
		assertThrows(IllegalArgumentException.class,
				() -> stricter.replace(population, offspring, new double[] { 1.0, 2.0 }, new double[] { 0 },
						new Random()));
	}

	@Test
	void replaceValidatesFitnessArrayLength() {
		List<LabeledSolution> population = List.of(new LabeledSolution("p0"), new LabeledSolution("p1"));
		List<LabeledSolution> offspring = List.of(new LabeledSolution("o0"), new LabeledSolution("o1"));
		ElitistReplacement<LabeledSolution> replacement = new ElitistReplacement<>(1);

		assertThrows(IllegalArgumentException.class,
				() -> replacement.replace(population, offspring, new double[] { 1.0 }, new double[] { 0, 0 },
						new Random()));
	}
}
