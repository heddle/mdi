package edu.cnu.mdi.sim.simanneal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;
import edu.cnu.mdi.sim.SimulationEngineConfig;

public class SimulatedAnnealingSimulationTest {

    @Test
    public void testInjectedScheduleControlsAbsoluteTemperature() {
        AnnealingSchedule schedule = new AnnealingSchedule() {
            @Override
            public double temperature(long step, SimulatedAnnealingConfig cfg) {
                return 0.5;
            }
        };
        SimulatedAnnealingSimulation<ValueSolution> simulation =
                new SimulatedAnnealingSimulation<>(new DecreasingProblem(), config(),
                        schedule, (problem, rng) -> new InitialTemperature(10.0, 0, 0, 1));

        SimulationContext context = context();
        simulation.init(context);
        assertEquals(5.0, simulation.getAbsoluteTemperature());
        assertEquals(5.0, simulation.getState().temperature());
        simulation.step(context);
        assertEquals(5.0, simulation.getAbsoluteTemperature());
    }

    @Test
    public void testRejectedMoveIsUndone() {
        ValueSolution initial = new ValueSolution(0);
        AnnealingProblem<ValueSolution> problem = new AnnealingProblem<>() {
            @Override
            public double energy(ValueSolution solution) {
                return solution.value;
            }

            @Override
            public ValueSolution randomSolution(Random rng) {
                return initial;
            }

            @Override
            public AnnealingMove<ValueSolution> randomMove(
                    Random rng, ValueSolution current) {
                return new ReversibleAnnealingMove<>() {
                    @Override
                    public void apply(ValueSolution solution) {
						solution.value = 1000.0;
                    }

                    @Override
                    public void undo(ValueSolution solution) {
                        solution.value = 0;
                    }
                };
            }
        };
        SimulatedAnnealingSimulation<ValueSolution> simulation =
                new SimulatedAnnealingSimulation<>(problem, config(),
                        new GeometricAnnealingSchedule(),
                        (p, rng) -> new InitialTemperature(1.0, 0, 0, 1));

        SimulationContext context = context();
        simulation.init(context);
        simulation.step(context);
        assertEquals(0.0, simulation.getState().currentEnergy());
        assertEquals(0.0, simulation.getBestSolutionCopy().value);
    }

    @Test
    public void testConfigurationRejectsInvalidCoolingParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulatedAnnealingConfig(0, 1, 0.9, 0, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new SimulatedAnnealingConfig(10, 1, 1.1, 0, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new SimulatedAnnealingConfig(10, 1, 0.9, Double.NaN, 1, 1, 1));
    }

	@Test
	public void testInvalidHeuristicTemperatureIsRejected() {
		SimulatedAnnealingSimulation<ValueSolution> simulation =
				new SimulatedAnnealingSimulation<>(new DecreasingProblem(), config(),
						new GeometricAnnealingSchedule(),
						(problem, rng) -> new InitialTemperature(Double.NaN, 0, 0, 1));

		assertThrows(IllegalStateException.class, () -> simulation.init(context()));
	}

	@Test
	public void testInvalidScheduleTemperatureIsRejected() {
		SimulatedAnnealingSimulation<ValueSolution> simulation =
				new SimulatedAnnealingSimulation<>(new DecreasingProblem(), config(),
						(step, cfg) -> Double.NaN,
						(problem, rng) -> new InitialTemperature(1, 0, 0, 1));
		simulation.init(context());

		assertThrows(IllegalStateException.class, () -> simulation.step(context()));
	}

	@Test
	public void testAcceptedMoveListenersRunOnEdt() throws Exception {
		SimulatedAnnealingSimulation<ValueSolution> simulation =
				new SimulatedAnnealingSimulation<>(new DecreasingProblem(), config(),
						new GeometricAnnealingSchedule(),
						(problem, rng) -> new InitialTemperature(1, 0, 0, 1));
		CountDownLatch notified = new CountDownLatch(1);
		java.util.concurrent.atomic.AtomicBoolean onEdt =
				new java.util.concurrent.atomic.AtomicBoolean();
		simulation.addAcceptedMoveListener(new IAcceptedMoveListener() {
			@Override public void acceptedMove(double temperature, double energy) {
				onEdt.set(SwingUtilities.isEventDispatchThread());
				notified.countDown();
			}
			@Override public void newBest(double temperature, double energy) {}
		});
		simulation.init(context());
		simulation.step(context());

		assertTrue(notified.await(3, TimeUnit.SECONDS));
		assertTrue(onEdt.get());
	}

	@Test
	public void testAcceptedMovesAreBatchedWithoutCollapsingToLatest() throws Exception {
		SimulatedAnnealingConfig longerConfig =
				new SimulatedAnnealingConfig(100, 1, 0.99, 0.0, 0, 0, 123L);
		SimulatedAnnealingSimulation<ValueSolution> simulation =
				new SimulatedAnnealingSimulation<>(new DecreasingProblem(), longerConfig,
						new GeometricAnnealingSchedule(),
						(problem, rng) -> new InitialTemperature(1, 0, 0, 1));
		java.util.concurrent.atomic.AtomicInteger accepted =
				new java.util.concurrent.atomic.AtomicInteger();
		simulation.addAcceptedMoveListener(new IAcceptedMoveListener() {
			@Override public void acceptedMove(double temperature, double energy) {
				accepted.incrementAndGet();
			}
			@Override public void newBest(double temperature, double energy) {}
		});

		CountDownLatch edtBlocked = new CountDownLatch(1);
		CountDownLatch releaseEdt = new CountDownLatch(1);
		SwingUtilities.invokeLater(() -> {
			edtBlocked.countDown();
			try {
				releaseEdt.await(3, TimeUnit.SECONDS);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		});
		assertTrue(edtBlocked.await(3, TimeUnit.SECONDS));
		simulation.init(context());
		for (int i = 0; i < 20; i++) {
			simulation.step(context());
		}
		releaseEdt.countDown();
		SwingUtilities.invokeAndWait(() -> {});

		assertEquals(20, accepted.get());
	}

	@Test
	public void testCandidateMoveDoesNotRequireUndo() {
		AnnealingProblem<ValueSolution> problem = new AnnealingProblem<>() {
			@Override public double energy(ValueSolution solution) { return solution.value; }
			@Override public ValueSolution randomSolution(Random rng) {
				return new ValueSolution(10);
			}
			@Override public AnnealingMove<ValueSolution> randomMove(
					Random rng, ValueSolution current) {
				return (CandidateAnnealingMove<ValueSolution>) solution ->
						new ValueSolution(solution.value - 1);
			}
		};
		SimulatedAnnealingSimulation<ValueSolution> simulation =
				new SimulatedAnnealingSimulation<>(problem, config(),
						new GeometricAnnealingSchedule(),
						(p, rng) -> new InitialTemperature(1, 0, 0, 1));
		simulation.init(context());

		simulation.step(context());

		assertEquals(9.0, simulation.getState().currentEnergy());
		assertEquals(9.0, simulation.getBestSolutionCopy().value);
	}

	@Test
	public void testNotificationPolicyRejectsInvalidLimits() {
		assertThrows(IllegalArgumentException.class,
				() -> new AnnealingNotificationPolicy(0, 10, 1));
		assertThrows(IllegalArgumentException.class,
				() -> new AnnealingNotificationPolicy(10, 10, 0));
	}

    private static SimulatedAnnealingConfig config() {
        return new SimulatedAnnealingConfig(10, 1, 0.9, 0.0, 0, 0, 123L);
    }

    private static SimulationContext context() {
        return new SimulationEngine(new edu.cnu.mdi.sim.Simulation() {
            @Override public void init(SimulationContext ctx) {}
            @Override public boolean step(SimulationContext ctx) { return false; }
        }, new SimulationEngineConfig(0, 0, 0, false)).getContext();
    }

    private static final class ValueSolution implements AnnealingSolution<ValueSolution> {
        private double value;

        ValueSolution(double value) {
            this.value = value;
        }

        @Override
        public ValueSolution copy() {
            return new ValueSolution(value);
        }
    }

    private static final class DecreasingProblem implements AnnealingProblem<ValueSolution> {
        @Override
        public double energy(ValueSolution solution) {
            return solution.value;
        }

        @Override
        public ValueSolution randomSolution(Random rng) {
            return new ValueSolution(10);
        }

        @Override
        public AnnealingMove<ValueSolution> randomMove(Random rng, ValueSolution current) {
            return new ReversibleAnnealingMove<>() {
                @Override
                public void apply(ValueSolution solution) {
                    solution.value--;
                }

                @Override
                public void undo(ValueSolution solution) {
                    solution.value++;
                }
            };
        }
    }
}
