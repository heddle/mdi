package edu.cnu.mdi.sim.ga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;

/**
 * A generic genetic algorithm simulation. This class is responsible for managing the population, 
 * applying the GA operators, and tracking the best solution found.
 * It implements the Simulation interface, allowing it to be run in a simulation engine with a GUI.
 *
 * @param <C> The type of solutions in the population, which must extend GASolution.
 */
public class GeneticAlgorithmSimulation<C extends GASolution<C>> implements Simulation {

	private final GAProblem<C> problem;
	private final GAConfig cfg;
	private final GAOperators<C> operators;
	private volatile GAFeedback feedback = GAFeedback.none();

	private volatile GAPopulation<C> population;
	private volatile C bestIndividual;
	private volatile double bestFitness = Double.NEGATIVE_INFINITY;
	private volatile double[] fitnesses;
	private volatile long generation;
	private volatile GAState stateSnapshot =
			new GAState(0, Double.NEGATIVE_INFINITY, 0.0, 0.0, 0.0);
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
	 * Set the optional output channel for messages, progress, and refresh hints.
	 * @param feedback output channel, or {@code null} to discard output
	 */
	public void setFeedback(GAFeedback feedback) {
		this.feedback = feedback == null ? GAFeedback.none() : feedback;
	}

	/**
	 * Seeds the RNG, builds the initial population via
	 * {@link GAProblem#initialPopulation}, evaluates and tracks the best
	 * individual, and resets the generation counter to zero.
	 *
	 * @param ctx the simulation context (unused; present to satisfy
	 *            {@link Simulation#init})
	 * @throws IllegalStateException if the problem's initial population is
	 *                               {@code null} or does not contain exactly
	 *                               {@link GAConfig#populationSize()}
	 *                               individuals
	 */
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
		stateSnapshot = createStateSnapshot(generation, bestFitness, fitnesses);
		feedback.message("Population initialized. Best=" + fmt(bestFitness));
		feedback.progress(ProgressInfo.indeterminate("Ready"));
		feedback.refresh();
	}

	/**
	 * Advances the GA by one generation: builds an offspring pool via
	 * selection/crossover/mutation, evaluates it, replaces the population via
	 * {@link ReplacementOperator#replace}, and re-evaluates the resulting
	 * generation.
	 * <p>
	 * The replaced generation ({@code nextGen}) is always fully re-evaluated
	 * rather than reusing {@code currentFits}/{@code offFitness} by slot,
	 * because {@link ReplacementOperator} is explicitly allowed to reorder or
	 * select individuals from either input in arbitrary ways — fitness values
	 * cannot safely be reconstructed by positional convention.
	 * </p>
	 * <p>
	 * Returns {@code false} (stopping the simulation) once
	 * {@link SimulationContext#isCancelRequested()} is {@code true} or
	 * {@link GAConfig#maxGenerations()} has been reached.
	 * </p>
	 *
	 * @param ctx the simulation context, used to check for cancellation
	 * @return {@code true} if another generation should run; {@code false} to
	 *         stop
	 * @throws NullPointerException if a selection or crossover operator
	 *                               returns {@code null}, or a mutated child
	 *                               is {@code null}
	 * @throws IllegalStateException if the replacement operator's requested
	 *                               offspring count is out of range, if
	 *                               crossover produces no children, or if the
	 *                               replacement operator does not return
	 *                               exactly {@code popSize} non-null
	 *                               individuals
	 */
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
	    int offspringNeeded = operators.replacement().offspringCount(popSize);
	    if (offspringNeeded < 0 || offspringNeeded > popSize) {
	        throw new IllegalStateException(
	                "Replacement offspring count must be in [0," + popSize + "]");
	    }
	    while (offspring.size() < offspringNeeded) {
	        C p1 = Objects.requireNonNull(
	                operators.selection().select(currentInds, currentFits, rng),
	                "selection operator returned null");
	        C p2 = Objects.requireNonNull(
	                operators.selection().select(currentInds, currentFits, rng),
	                "selection operator returned null");
	        List<C> children = rng.nextDouble() < cfg.crossoverRate()
	                ? operators.crossover().crossover(p1, p2, rng)
	                : List.of(copyOf(p1, "selected parent"));
	        if (children == null || children.isEmpty()) {
	            throw new IllegalStateException(
	                    "Crossover operator must produce at least one child");
	        }
	        for (C child : children) {
	            if (offspring.size() >= offspringNeeded) break;
	            C mutated = operators.mutation().mutate(
	                    Objects.requireNonNull(child, "crossover child"), rng,
	                    cfg.mutationRate());
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

	    population = SimpleGAPopulation.of(nextGen);
	    fitnesses  = newFitnesses;
	    trackBest(population.individuals(), fitnesses);
	    generation++;
	    stateSnapshot = createStateSnapshot(generation, bestFitness, fitnesses);
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
		return stateSnapshot;
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
	public C getBestIndividualCopy() {
		return bestIndividual == null ? null : copyOf(bestIndividual, "best individual");
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
			snapshot.add(copyOf(individual, "population individual"));
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
	private void trackBest(List<C> individuals, double[] f) {
		for (int i = 0; i < f.length; i++) {
			if (f[i] > bestFitness) {
				bestFitness = f[i];
				bestIndividual = copyOf(individuals.get(i), "best individual");
			}
		}
	}

	private static <C extends GASolution<C>> C copyOf(C individual, String description) {
		return Objects.requireNonNull(individual.copy(),
				description + " copy must not be null");
	}

	private static GAState createStateSnapshot(long generation, double bestFitness,
			double[] currentFitnesses) {
		double mean = Arrays.stream(currentFitnesses).average().orElse(0.0);
		double worst = Arrays.stream(currentFitnesses).min().orElse(0.0);
		return new GAState(generation, bestFitness, mean, worst, 0.0);
	}

	private static String fmt(double x) {
		return String.format("%.4g", x);
	}

	private void publishGenerationUpdates() {
		GAFeedback currentFeedback = feedback;
		if (cfg.progressEveryGens() > 0
				&& generation % cfg.progressEveryGens() == 0) {
			double fraction = cfg.maxGenerations() == 0
					? 1.0
					: (double) generation / cfg.maxGenerations();
			currentFeedback.progress(ProgressInfo.determinate(
					fraction,
					"Generation " + generation + ", best=" + fmt(bestFitness)));
		}
		if (cfg.refreshEveryGens() > 0
				&& generation % cfg.refreshEveryGens() == 0) {
			currentFeedback.refresh();
		}
	}
}
