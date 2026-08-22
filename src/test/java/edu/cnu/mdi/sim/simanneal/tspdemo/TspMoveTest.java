package edu.cnu.mdi.sim.simanneal.tspdemo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.geom.Point2D;
import java.util.Random;

import org.junit.jupiter.api.Test;

class TspMoveTest {

	@Test
	void prepareRejectsTourTooShortForTwoOpt() {
		TspModel model = new TspModel(4, false, 0.0, new Random(1L));
		TspSolution solution = new TspSolution(model);
		solution.tour = new int[] {0, 1, 2};

		IllegalArgumentException failure = assertThrows(
				IllegalArgumentException.class,
				() -> new TspMove(new Random(2L)).prepare(solution));
		assertEquals("2-opt requires a tour with at least four cities",
				failure.getMessage());
	}

	@Test
	void deltaEnergyMatchesFullRecomputationAndUndoRestoresTour() {
		Random rng = new Random(48291L);
		TspModel model = new TspModel(30, true, 0.35, rng);
		TspSolution solution = new TspAnnealingProblem(model).randomSolution(rng);
		TspMove move = new TspMove(rng);

		for (int iteration = 0; iteration < 500; iteration++) {
			int[] originalTour = solution.tour.clone();
			double before = solution.getTourLength();

			move.prepare(solution);
			double predictedDelta = move.deltaE(solution);
			move.apply(solution);
			double after = solution.getTourLength();

			assertEquals(after - before, predictedDelta, 1.0e-12);
			assertNotEquals(0, java.util.Arrays.compare(originalTour, solution.tour));

			move.undo(solution);
			assertArrayEquals(originalTour, solution.tour);
			assertEquals(before, solution.getTourLength(), 1.0e-12);
		}
	}

	@Test
	void riverPenaltyAppliesOnlyToCrossingEdgesWhenEnabled() {
		TspModel model = new TspModel(4, true, 2.5, new Random(9L));
		double riverX = model.riverX;
		model.cities[0] = new Point2D.Double(riverX - 0.2, 0.5);
		model.cities[1] = new Point2D.Double(riverX + 0.2, 0.5);
		model.cities[2] = new Point2D.Double(riverX - 0.1, 0.5);

		assertEquals(0.4 + 2.5, model.getDistance(0, 1), 1.0e-12);
		assertEquals(0.1, model.getDistance(0, 2), 1.0e-12);

		model.setRiverEnabled(false);
		assertEquals(0.4, model.getDistance(0, 1), 1.0e-12);
	}
}
