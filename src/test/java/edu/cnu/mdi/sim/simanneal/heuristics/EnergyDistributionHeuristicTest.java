package edu.cnu.mdi.sim.simanneal.heuristics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.sim.simanneal.AnnealingMove;
import edu.cnu.mdi.sim.simanneal.AnnealingProblem;
import edu.cnu.mdi.sim.simanneal.AnnealingSolution;
import edu.cnu.mdi.sim.simanneal.InitialTemperature;

/**
 * Regression coverage for {@link EnergyDistributionHeuristic}, which had zero
 * prior test coverage despite being the default initial-temperature
 * estimator wired into the TSP demo.
 */
class EnergyDistributionHeuristicTest {

	/** A trivial solution that just carries a fixed energy value. */
	private record FixedEnergySolution(double energy) implements AnnealingSolution<FixedEnergySolution> {
		@Override
		public FixedEnergySolution copy() {
			return this;
		}
	}

	/** A problem that hands out a scripted, deterministic sequence of energies. */
	private static AnnealingProblem<FixedEnergySolution> scriptedProblem(double[] energies) {
		AtomicInteger cursor = new AtomicInteger(0);
		return new AnnealingProblem<>() {
			@Override
			public double energy(FixedEnergySolution sol) {
				return sol.energy();
			}

			@Override
			public FixedEnergySolution randomSolution(Random rng) {
				return new FixedEnergySolution(energies[cursor.getAndIncrement()]);
			}

			@Override
			public AnnealingMove<FixedEnergySolution> randomMove(Random rng, FixedEnergySolution current) {
				throw new UnsupportedOperationException("not used by the heuristic under test");
			}
		};
	}

	@Test
	void constructorRejectsOutOfRangeArguments() {
		assertThrows(IllegalArgumentException.class,
				() -> new EnergyDistributionHeuristic<FixedEnergySolution>(9, 0.8, 0.0));
		assertThrows(IllegalArgumentException.class,
				() -> new EnergyDistributionHeuristic<FixedEnergySolution>(10, 0.0, 0.0));
		assertThrows(IllegalArgumentException.class,
				() -> new EnergyDistributionHeuristic<FixedEnergySolution>(10, 1.0, 0.0));
		assertThrows(IllegalArgumentException.class,
				() -> new EnergyDistributionHeuristic<FixedEnergySolution>(10, 0.8, -1.0));
	}

	@Test
	void estimateComputesMedianAndMadFromARobustDistribution() {
		// 10 samples with one outlier (100); hand-verified median and MAD below.
		double[] energies = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 100 };
		EnergyDistributionHeuristic<FixedEnergySolution> heuristic =
				new EnergyDistributionHeuristic<>(energies.length, 0.8, 0.0);

		InitialTemperature result = heuristic.estimate(scriptedProblem(energies), new Random());

		assertEquals(5.5, result.energyMedian(), 1.0e-12);
		assertEquals(2.5, result.energyMad(), 1.0e-12);
		assertEquals(energies.length, result.samples());

		// T0 = -deltaE / ln(p), where deltaE = 1.4826 * MAD for a non-flat distribution.
		double expectedDeltaE = 1.4826 * 2.5;
		double expectedT0 = -expectedDeltaE / Math.log(0.8);
		assertEquals(expectedT0, result.T0(), 1.0e-9);
	}

	@Test
	void estimateFallsBackToAFixedDeltaEForACompletelyFlatLandscape() {
		// Every sampled energy identical: MAD and IQR are both zero, so the
		// heuristic must fall back to deltaE = 1.0 rather than dividing by zero.
		double[] energies = new double[12];
		java.util.Arrays.fill(energies, 7.0);
		EnergyDistributionHeuristic<FixedEnergySolution> heuristic =
				new EnergyDistributionHeuristic<>(energies.length, 0.8, 0.0);

		InitialTemperature result = heuristic.estimate(scriptedProblem(energies), new Random());

		assertEquals(7.0, result.energyMedian(), 1.0e-12);
		assertEquals(0.0, result.energyMad(), 1.0e-12);

		double expectedT0 = -1.0 / Math.log(0.8);
		assertEquals(expectedT0, result.T0(), 1.0e-9);
	}

	@Test
	void estimateNeverReturnsBelowTheConfiguredMinimumTemperature() {
		double[] energies = new double[10];
		java.util.Arrays.fill(energies, 7.0); // flat -> deltaE = 1.0 fallback
		double minT0 = 5.0;
		EnergyDistributionHeuristic<FixedEnergySolution> heuristic =
				new EnergyDistributionHeuristic<>(energies.length, 0.5, minT0);

		// Unfloored T0 would be -1.0 / ln(0.5) ~= 1.44, well below minT0.
		InitialTemperature result = heuristic.estimate(scriptedProblem(energies), new Random());

		assertEquals(minT0, result.T0(), 1.0e-12);
	}

	@Test
	void estimateRejectsNonFiniteSampledEnergies() {
		double[] energies = { 1, 2, 3, 4, 5, 6, 7, 8, 9, Double.NaN };
		EnergyDistributionHeuristic<FixedEnergySolution> heuristic =
				new EnergyDistributionHeuristic<>(energies.length, 0.8, 0.0);

		assertThrows(IllegalStateException.class,
				() -> heuristic.estimate(scriptedProblem(energies), new Random()));
	}

	@Test
	void estimateProducesAPositiveFiniteT0EvenAtTheGuardrail() {
		double[] energies = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		EnergyDistributionHeuristic<FixedEnergySolution> heuristic =
				new EnergyDistributionHeuristic<>(energies.length, 0.9, 0.0);

		InitialTemperature result = heuristic.estimate(scriptedProblem(energies), new Random());

		assertTrue(Double.isFinite(result.T0()) && result.T0() > 0.0);
	}
}
