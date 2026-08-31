package edu.cnu.mdi.sim.ga.triimage;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.sim.ga.GASolution;

/**
 * Regression coverage for {@link TournamentSelection}, which had zero prior
 * test coverage.
 */
class TournamentSelectionTest {

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

	/** A Random whose nextInt(bound) returns a scripted sequence. */
	private static final class ScriptedRandom extends Random {
		private final int[] values;
		private int cursor = 0;

		ScriptedRandom(int... values) {
			this.values = values;
		}

		@Override
		public int nextInt(int bound) {
			return values[cursor++];
		}
	}

	@Test
	void constructorRejectsNonPositiveTournamentSize() {
		assertThrows(IllegalArgumentException.class, () -> new TournamentSelection<LabeledSolution>(0));
		assertThrows(IllegalArgumentException.class, () -> new TournamentSelection<LabeledSolution>(-1));
	}

	@Test
	void selectReturnsTheFittestOfTheDrawnContestants() {
		List<LabeledSolution> population = List.of(
				new LabeledSolution("a"), new LabeledSolution("b"),
				new LabeledSolution("c"), new LabeledSolution("d"));
		double[] fitness = { 1.0, 5.0, 3.0, 0.5 };

		// Tournament of 3, drawing indices 0, 3, 1 -> fittest among those is index 1 ("b").
		TournamentSelection<LabeledSolution> selection = new TournamentSelection<>(3);
		LabeledSolution winner = selection.select(population, fitness, new ScriptedRandom(0, 3, 1));

		assertSame(population.get(1), winner);
	}

	@Test
	void aLaterContestantOnlyReplacesTheIncumbentOnStrictlyHigherFitness() {
		List<LabeledSolution> population = List.of(new LabeledSolution("a"), new LabeledSolution("b"));
		double[] fitness = { 5.0, 5.0 }; // tie: incumbent must be kept

		TournamentSelection<LabeledSolution> selection = new TournamentSelection<>(2);
		LabeledSolution winner = selection.select(population, fitness, new ScriptedRandom(0, 1));

		assertSame(population.get(0), winner, "a tie must not replace the incumbent");
	}

	@Test
	void selectValidatesArguments() {
		List<LabeledSolution> population = List.of(new LabeledSolution("a"));
		double[] fitness = { 1.0 };
		TournamentSelection<LabeledSolution> selection = new TournamentSelection<>(1);

		assertThrows(NullPointerException.class,
				() -> selection.select(null, fitness, new Random()));
		assertThrows(IllegalArgumentException.class,
				() -> selection.select(List.of(), fitness, new Random()));
		assertThrows(IllegalArgumentException.class,
				() -> selection.select(population, new double[] { 1.0, 2.0 }, new Random()));
	}
}
