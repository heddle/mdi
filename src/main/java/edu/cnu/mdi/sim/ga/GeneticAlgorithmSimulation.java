package edu.cnu.mdi.sim.ga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;

/**
 * A generic genetic algorithm simulation. This class is responsible for managing the population, 
 * applying the GA operators, and tracking the best solution found.
 * It implements the Simulation interface, allowing it to be run in a simulation engine with a GUI.
 *
 * @param <C> The type of solutions in the population, which must extend GASolution.
 */
public class GeneticAlgorithmSimulation<C extends GASolution> implements Simulation {

	private final GAProblem<C> problem;
	private final GAConfig cfg;
	private final GAOperators<C> operators;
	private transient SimulationEngine engine;

	private volatile GAPopulation<C> population;
	private volatile C bestIndividual;
	private volatile double bestFitness = Double.NEGATIVE_INFINITY;
	private volatile double[] fitnesses;
	private volatile long generation;
	private Random rng;

	/** Constructor for the GeneticAlgorithmSimulation.
	 * @param problem The GA problem to solve, which defines the fitness function and initial population.
	 * @param cfg The configuration for the GA, including parameters like population size and mutation rate.
	 * @param operators The set of GA operators to use, including selection, crossover, mutation, and replacement.
	 */
	public GeneticAlgorithmSimulation(GAProblem<C> problem, GAConfig cfg, GAOperators<C> operators) {
		this.problem = Objects.requireNonNull(problem, "problem");
		this.cfg = Objects.requireNonNull(cfg, "cfg");
		this.operators = Objects.requireNonNull(operators, "operators");
	}

	/**
	 * Sets the simulation engine for this GA simulation. The engine is used to post messages and progress updates to the UI.
	 * @param engine The simulation engine to use for UI interactions. This can be null if the simulation is run without a UI.
	 */
	public void setEngine(SimulationEngine engine) {
		this.engine = engine;
	}

	@Override
	public void init(SimulationContext ctx) {
		rng = cfg.randomSeed() == 0 ? new Random() : new Random(cfg.randomSeed());
		population = problem.initialPopulation(cfg.populationSize(), rng);
		if (population == null || population.individuals() == null
				|| population.size() != cfg.populationSize()
				|| population.individuals().size() != cfg.populationSize()) {
			throw new IllegalStateException(
					"Initial population must contain exactly "
					+ cfg.populationSize() + " individuals");
		}
		bestIndividual = null;
		bestFitness = Double.NEGATIVE_INFINITY;
		fitnesses = evaluateAll(population.individuals());
		trackBest(population.individuals(), fitnesses);
		generation = 0;
		if (engine != null) {
			engine.postMessage("Population initialized. Best=" + fmt(bestFitness));
			engine.postProgress(ProgressInfo.indeterminate("Ready"));
			engine.requestRefresh();
		}
	}

	@Override
	public boolean step(SimulationContext ctx) {
	    if (ctx.isCancelRequested() || generation >= cfg.maxGenerations())
	        return false;

	    final List<C>  currentInds = population.individuals();
	    final double[] currentFits = fitnesses;
	    final int      popSize     = currentInds.size();
	    operators.mutation().beginGeneration(generation, cfg.maxGenerations());

	    // 1. Build offspring pool
	    List<C> offspring = new ArrayList<>(popSize);
	    int offspringNeeded = popSize - cfg.eliteCount();
	    while (offspring.size() < offspringNeeded) {
	        C p1 = operators.selection().select(currentInds, currentFits, rng);
	        C p2 = operators.selection().select(currentInds, currentFits, rng);
	        List<C> children = rng.nextDouble() < cfg.crossoverRate()
	                ? operators.crossover().crossover(p1, p2, rng)
	                : List.of(p1.copy());
	        if (children == null || children.isEmpty()) {
	            throw new IllegalStateException(
	                    "Crossover operator must produce at least one child");
	        }
	        for (C child : children) {
	            if (offspring.size() >= offspringNeeded) break;
	            C mutated = operators.mutation().mutate(
	                    Objects.requireNonNull(child, "crossover child"), rng);
	            offspring.add(Objects.requireNonNull(mutated, "mutated child"));
	        }
	    }

	    // 2. Evaluate offspring
	    double[] offFitness = evaluateAll(offspring);

	    // 3. Replace and evaluate the resulting population. ReplacementOperator
	    // deliberately permits arbitrary ordering and selection from either input,
	    // so fitness values cannot safely be reconstructed by slot convention.
	    List<C> nextGen = operators.replacement()
	            .replace(currentInds, offspring, currentFits, offFitness, rng);
	    if (nextGen == null || nextGen.size() != popSize
	            || nextGen.stream().anyMatch(Objects::isNull)) {
	        throw new IllegalStateException(
	                "Replacement operator must return exactly "
	                + popSize + " non-null individuals");
	    }
	    double[] newFitnesses = evaluateAll(nextGen);

	    population = wrap(nextGen);
	    fitnesses  = newFitnesses;
	    trackBest(population.individuals(), fitnesses);
	    generation++;
	    publishGenerationUpdates();

	    return true;
	}

	// ── accessors ────────────────────────────────────────────────────────────

	/** 
	 * Returns the current state of the GA, including generation number, best fitness, mean fitness, worst fitness, and diversity.
	 * This method computes the mean and worst fitness from the current fitnesses array.
	 * The diversity is currently set to 0.0 as a placeholder; it can be implemented based on the population's characteristics.
	 * @return A GAState object representing the current state of the GA, which can be used for UI display or logging.
	 */
	public GAState getState() {
		double[] currentFitnesses = fitnesses;
		if (currentFitnesses == null) {
			return new GAState(0, Double.NEGATIVE_INFINITY, 0.0, 0.0, 0.0);
		}
		double mean = Arrays.stream(currentFitnesses).average().orElse(0.0);
		double worst = Arrays.stream(currentFitnesses).min().orElse(0.0);
		return new GAState(generation, bestFitness, mean, worst, 0.0);
	}
	
	/**
	 * Returns the GA problem being solved by this simulation. This allows external code to access the problem definition,
	 * including the fitness function and initial population generator, which can be useful for UI components or logging.
	 * @return The GA problem being solved by this simulation.
	 */
	public GAProblem<C> getProblem() {
	    return problem;
	}

	/**
	 * Returns a copy of the best individual found so far in the GA. This allows external code to access the best solution without risking modification of the internal state.
	 * If no individuals have been evaluated yet, this method returns null.
	 * @return A copy of the best individual found so far, or null if no individuals have been evaluated.
	 */
	@SuppressWarnings("unchecked")
	public C getBestIndividualCopy() {
		return bestIndividual == null ? null : (C) bestIndividual.copy();
	}

	/**
	 * Returns the current generation number of the GA. This is incremented at the end of each step and can be used for tracking progress or logging.
	 * @return The current generation number of the GA.
	 */
	public long getGeneration() {
		return generation;
	}
	
	/**
	 * Returns a snapshot of the current population as an unmodifiable list. This allows external code to access 
	 * the current individuals in the population without risking modification of the internal state.
	 * If the population has not been initialized yet, this method returns an empty list.
	 * @return An unmodifiable list of the current individuals in the population, or an empty list if the 
	 * population is not initialized.
	 */
	public List<C> getPopulationSnapshot() {
		GAPopulation<C> pop = population;
		if (pop == null) return List.of();
		List<C> snapshot = new ArrayList<>(pop.size());
		for (C individual : pop.individuals()) {
			@SuppressWarnings("unchecked")
			C copy = (C) individual.copy();
			snapshot.add(copy);
		}
		return List.copyOf(snapshot);
	}



	// ── private helpers ──────────────────────────────────────────────────────

	private double[] evaluateAll(List<C> individuals) {
	    double[] f = new double[individuals.size()];
	    for (int i = 0; i < individuals.size(); i++) {
			f[i] = problem.fitness(individuals.get(i));
			if (!Double.isFinite(f[i])) {
				throw new IllegalStateException(
						"Fitness must be finite at population index " + i);
			}
	    }
	    return f;
	}
	// trackBest helper to update the best individual and fitness found so far. 
	// This method iterates through the given individuals and their fitnesses,
	@SuppressWarnings("unchecked")
	private void trackBest(List<C> individuals, double[] f) {
		for (int i = 0; i < f.length; i++) {
			if (f[i] > bestFitness) {
				bestFitness = f[i];
				bestIndividual = (C) individuals.get(i).copy();
			}
		}
	}

	// wrap helper to create a GAPopulation from a list of individuals. This allows us to maintain 
	// the population as a GAPopulation interface while still using simple lists internally.
	private GAPopulation<C> wrap(List<C> individuals) {
		return new GAPopulation<C>() {
			@Override
			public List<C> individuals() {
				return individuals;
			}

			@Override
			public int size() {
				return individuals.size();
			}

			@Override
			public GAPopulation<C> copy() {
				return wrap(new ArrayList<>(individuals));
			}
		};
	}

	private static String fmt(double x) {
		return String.format("%.4g", x);
	}

	private void publishGenerationUpdates() {
		SimulationEngine currentEngine = engine;
		if (currentEngine == null) {
			return;
		}
		if (cfg.progressEveryGens() > 0
				&& generation % cfg.progressEveryGens() == 0) {
			double fraction = cfg.maxGenerations() == 0
					? 1.0
					: (double) generation / cfg.maxGenerations();
			currentEngine.postProgress(ProgressInfo.determinate(
					fraction,
					"Generation " + generation + ", best=" + fmt(bestFitness)));
		}
		if (cfg.refreshEveryGens() > 0
				&& generation % cfg.refreshEveryGens() == 0) {
			currentEngine.requestRefresh();
		}
	}
}
