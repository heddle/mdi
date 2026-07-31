package edu.cnu.mdi.sim.simanneal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

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
                return new AnnealingMove<>() {
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

    private static SimulatedAnnealingConfig config() {
        return new SimulatedAnnealingConfig(10, 1, 0.9, 0.0, 0, 0, 123L);
    }

    private static SimulationContext context() {
        return new SimulationEngine(new edu.cnu.mdi.sim.Simulation() {
            @Override public void init(SimulationContext ctx) {}
            @Override public boolean step(SimulationContext ctx) { return false; }
        }, new SimulationEngineConfig(0, 0, 0, false)).getContext();
    }

    private static final class ValueSolution implements AnnealingSolution {
        private double value;

        ValueSolution(double value) {
            this.value = value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S extends AnnealingSolution> S copy() {
            return (S) new ValueSolution(value);
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
            return new AnnealingMove<>() {
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
