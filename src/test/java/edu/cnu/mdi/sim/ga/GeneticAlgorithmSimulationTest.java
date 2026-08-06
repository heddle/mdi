package edu.cnu.mdi.sim.ga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;
import edu.cnu.mdi.sim.SimulationEngineConfig;

public class GeneticAlgorithmSimulationTest {

    @Test
    public void testFitnessesFollowArbitraryReplacementOrdering() {
        ValueProblem problem = new ValueProblem(List.of(new ValueSolution(1), new ValueSolution(3)));
        GAConfig config = config(2, 1);
        GAOperators<ValueSolution> operators = new GAOperators<>(
                (population, fitnesses, rng) -> population.get(0),
                (first, second, rng) -> List.of(new ValueSolution(10)),
                (individual, rng) -> individual,
                (population, offspring, popFitness, offFitness, rng) ->
                        List.of(offspring.get(0), population.get(1)));
        GeneticAlgorithmSimulation<ValueSolution> simulation =
                new GeneticAlgorithmSimulation<>(problem, config, operators);

        SimulationContext context = context();
        simulation.init(context);
        simulation.step(context);

        assertEquals(6.5, simulation.getState().meanFitness());
        assertEquals(10.0, simulation.getState().bestFitness());
    }

    @Test
    public void testOffspringPoolIsLimitedToRequiredSize() {
        ValueProblem problem = new ValueProblem(List.of(
                new ValueSolution(1), new ValueSolution(2),
                new ValueSolution(3), new ValueSolution(4)));
        AtomicInteger offspringSeen = new AtomicInteger();
        GAConfig config = config(4, 1);
        GAOperators<ValueSolution> operators = new GAOperators<>(
                (population, fitnesses, rng) -> population.get(0),
                (first, second, rng) -> List.of(
                        new ValueSolution(5), new ValueSolution(6)),
                (individual, rng) -> individual,
                (population, offspring, popFitness, offFitness, rng) -> {
                    offspringSeen.set(offspring.size());
                    List<ValueSolution> next = new ArrayList<>();
                    next.add(population.get(3));
                    next.addAll(offspring);
                    return next;
                });
        GeneticAlgorithmSimulation<ValueSolution> simulation =
                new GeneticAlgorithmSimulation<>(problem, config, operators);

        SimulationContext context = context();
        simulation.init(context);
        simulation.step(context);

        assertEquals(3, offspringSeen.get());
        assertEquals(4, simulation.getPopulationSnapshot().size());
    }

    @Test
    public void testPopulationSnapshotCopiesIndividuals() {
        ValueProblem problem = new ValueProblem(List.of(new ValueSolution(7)));
        GAOperators<ValueSolution> operators = new GAOperators<>(
                (population, fitnesses, rng) -> population.get(0),
                (first, second, rng) -> List.of(first.copy()),
                (individual, rng) -> individual,
                (population, offspring, popFitness, offFitness, rng) -> population);
        GeneticAlgorithmSimulation<ValueSolution> simulation =
                new GeneticAlgorithmSimulation<>(problem, config(1, 0), operators);
        simulation.init(context());

        simulation.getPopulationSnapshot().get(0).value = 99;
        assertEquals(7, simulation.getPopulationSnapshot().get(0).value);
    }

	@Test
	public void testStateIsAvailableBeforeInitialization() {
		ValueProblem problem = new ValueProblem(List.of(new ValueSolution(1)));
		GAOperators<ValueSolution> operators = new GAOperators<>(
				(population, fitnesses, rng) -> population.get(0),
				(first, second, rng) -> List.of(first.copy()),
				(individual, rng) -> individual,
				(population, offspring, popFitness, offFitness, rng) -> population);
		GeneticAlgorithmSimulation<ValueSolution> simulation =
				new GeneticAlgorithmSimulation<>(problem, config(1, 0), operators);

		assertEquals(0, simulation.getState().generation());
		assertEquals(Double.NEGATIVE_INFINITY, simulation.getState().bestFitness());
	}

    @Test
    public void testConfigurationRejectsInvalidPopulationAndRates() {
        assertThrows(IllegalArgumentException.class,
                () -> new GAConfig(0, 1, 0.5, 0.1, 0, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new GAConfig(2, 1, Double.NaN, 0.1, 0, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new GAConfig(2, 1, 0.5, 0.1, 3, 1, 1, 1));
    }

	@Test
    public void testNonFiniteFitnessIsRejected() {
		ValueProblem problem = new ValueProblem(List.of(new ValueSolution(1))) {
			@Override
			public double fitness(ValueSolution individual) {
				return Double.NaN;
			}
		};
		GAOperators<ValueSolution> operators = new GAOperators<>(
				(population, fitnesses, rng) -> population.get(0),
				(first, second, rng) -> List.of(first.copy()),
				(individual, rng) -> individual,
				(population, offspring, popFitness, offFitness, rng) -> population);
		GeneticAlgorithmSimulation<ValueSolution> simulation =
				new GeneticAlgorithmSimulation<>(problem, config(1, 0), operators);

		assertThrows(IllegalStateException.class, () -> simulation.init(context()));
	}

	@Test
	public void testConfiguredMutationRateIsApplied() {
		AtomicInteger mutations = new AtomicInteger();
		ValueProblem problem = new ValueProblem(List.of(new ValueSolution(1)));
		GAOperators<ValueSolution> operators = new GAOperators<>(
				(population, fitnesses, rng) -> population.get(0),
				(first, second, rng) -> List.of(first.copy()),
				(individual, rng) -> {
					mutations.incrementAndGet();
					return individual;
				},
				(population, offspring, popFitness, offFitness, rng) -> offspring);

		GeneticAlgorithmSimulation<ValueSolution> neverMutates =
				new GeneticAlgorithmSimulation<>(problem,
						new GAConfig(1, 1, 1.0, 0.0, 0, 0, 0, 123L), operators);
		neverMutates.init(context());
		neverMutates.step(context());
		assertEquals(0, mutations.get());

		GeneticAlgorithmSimulation<ValueSolution> alwaysMutates =
				new GeneticAlgorithmSimulation<>(problem,
						new GAConfig(1, 1, 1.0, 1.0, 0, 0, 0, 123L), operators);
		alwaysMutates.init(context());
		alwaysMutates.step(context());
		assertEquals(1, mutations.get());
	}

    private static GAConfig config(int populationSize, int eliteCount) {
        return new GAConfig(populationSize, 10, 1.0, 0.0,
                eliteCount, 0, 0, 123L);
    }

    private static SimulationContext context() {
        return new SimulationEngine(new edu.cnu.mdi.sim.Simulation() {
            @Override public void init(SimulationContext ctx) {}
            @Override public boolean step(SimulationContext ctx) { return false; }
        }, new SimulationEngineConfig(0, 0, 0, false)).getContext();
    }

    private static final class ValueSolution implements GASolution {
        private int value;

        ValueSolution(int value) {
            this.value = value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S extends GASolution> S copy() {
            return (S) new ValueSolution(value);
        }

        @Override
        public int length() {
            return 1;
        }
    }

	private static class ValueProblem implements GAProblem<ValueSolution> {
        private final List<ValueSolution> initial;

        ValueProblem(List<ValueSolution> initial) {
            this.initial = initial;
        }

        @Override
        public double fitness(ValueSolution individual) {
            return individual.value;
        }

        @Override
        public ValueSolution randomIndividual(Random rng) {
            return new ValueSolution(rng.nextInt());
        }

        @Override
        public GAPopulation<ValueSolution> initialPopulation(int size, Random rng) {
            return SimpleGAPopulation.of(new ArrayList<>(initial));
        }
    }
}
