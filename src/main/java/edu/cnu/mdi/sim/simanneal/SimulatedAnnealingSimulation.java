package edu.cnu.mdi.sim.simanneal;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;

/**
 * A {@link Simulation} implementation that performs Simulated Annealing (SA) to minimize
 * an energy (cost) function over a problem-defined solution space.
 *
 * <p>
 * This class is part of the MDI simulation framework integration:
 * </p>
 * <ul>
 *   <li>The algorithm itself is UI-agnostic and runs under {@link edu.cnu.mdi.sim.SimulationEngine}.</li>
 *   <li>{@link SimulationContext} is intentionally minimal (cancellation + timing + step count).</li>
 *   <li>Optional UI feedback is posted through an injected
 *       {@link AnnealingFeedback} channel.</li>
 * </ul>
 *
 * <h2>Core algorithm</h2>
 * <p>
 * Each iteration proposes a random move from the current solution (via
 * {@link AnnealingProblem#randomMove(Random, AnnealingSolution)}), applies it, evaluates the
 * resulting energy, and accepts or rejects the move using the Metropolis criterion:
 * </p>
 *
 * <pre>
 *   accept if ΔE ≤ 0
 *   otherwise accept with probability exp(-ΔE / T)
 * </pre>
 *
 * <p>
 * The temperature {@code T} decreases over time according to a schedule; this implementation
 * delegates cooling to the configured {@link AnnealingSchedule}.
 * An initial temperature {@code T0} is estimated by a {@link TemperatureHeuristic}.
 * </p>
 *
 * <h2>Threading / UI</h2>
 * <p>
 * The simulation engine invokes {@link #init(SimulationContext)} once and then calls
 * {@link #step(SimulationContext)} repeatedly until it returns {@code false}. Any UI-related
 * updates (messages, progress, refresh) are posted to the engine, which is responsible for
 * EDT marshalling and listener notification.
 * </p>
 *
 * <h2>Move models</h2>
 * <p>
 * A problem explicitly returns either a {@link ReversibleAnnealingMove}, which
 * mutates in place and is undone on rejection, or a
 * {@link CandidateAnnealingMove}, which leaves the current solution unchanged
 * and returns a separate proposal.
 * </p>
 *
 * @param <S> concrete solution type for the annealing problem
 */
public final class SimulatedAnnealingSimulation<S extends AnnealingSolution<S>> implements Simulation {

	private final CopyOnWriteArrayList<IAcceptedMoveListener> acceptedMoveListeners =
			new CopyOnWriteArrayList<>();
	private final ConcurrentLinkedQueue<MoveNotification> notifications =
			new ConcurrentLinkedQueue<>();
	private final AtomicInteger queuedNotificationCount = new AtomicInteger();
	private final AtomicBoolean notificationPending = new AtomicBoolean();
	private long acceptedNotificationCount;
	private volatile SimulatedAnnealingState stateSnapshot =
			new SimulatedAnnealingState(0, 0, 0, 0, 0, 0);

	/** The annealing problem (solution generator, energy function, and move generator). */
	private final AnnealingProblem<S> problem;

	/** Configuration parameters controlling step limits, cooling rate, and UI throttling. */
	private final SimulatedAnnealingConfig cfg;
	
	private enum NotifyType { ACCEPTED_MOVE, NEW_BEST }
	private record MoveNotification(double temperature, double energy, NotifyType type) {}

	/**
	 * High-level stopping/temperature schedule policy.
	 * <p>
	 * This object is consulted for {@link AnnealingSchedule#shouldStop(long, SimulatedAnnealingConfig)}
	 * and for reporting a temperature value in {@link #getState()}.
	 * </p>
	 */
	private final AnnealingSchedule schedule;

	/**
	 * Heuristic used to estimate the initial temperature {@code T0} (often by sampling energies of
	 * random solutions).
	 */
	private final TemperatureHeuristic<S> tempHeuristic;

	/** Random number generator used for move proposals and acceptance decisions. */
	private Random rng;

	/** Current working solution. Mutated in-place by accepted moves. */
	private S current;

	/** Best solution found so far (a copy of a previous accepted solution). */
	private volatile S best;

	/** Energy of the current solution. Lower is better. */
	private double currentE;

	/** Best (lowest) energy observed so far. */
	private double bestE;

	/** Current step index (number of completed iterations). */
	private long step;

	/** Count of accepted moves. */
	private long accepted;

	/** Count of accepted uphill moves (ΔE &gt; 0). */
	private long uphillAccepted;

	/** Estimated initial temperature. */
	private double T0;

	/**
	 * Optional output channel used to post messages, progress, and refresh hints.
	 * <p>
	 * The default channel discards output, keeping headless use free of UI setup.
	 * </p>
	 */
	private volatile AnnealingFeedback feedback = AnnealingFeedback.none();

	/**
	 * Construct a simulated annealing simulation with explicit configuration and policies.
	 *
	 * @param problem        the annealing problem definition (non-null)
	 * @param cfg            configuration controlling cooling and pacing (non-null)
	 * @param schedule       stopping and temperature reporting policy (non-null)
	 * @param tempHeuristic  heuristic for estimating {@code T0} (non-null)
	 * @throws NullPointerException if any argument is null
	 */
	public SimulatedAnnealingSimulation(AnnealingProblem<S> problem,
			SimulatedAnnealingConfig cfg,
			AnnealingSchedule schedule,
			TemperatureHeuristic<S> tempHeuristic) {

		this.problem = Objects.requireNonNull(problem, "problem");
		this.cfg = Objects.requireNonNull(cfg, "cfg");
		this.schedule = Objects.requireNonNull(schedule, "schedule");
		this.tempHeuristic = Objects.requireNonNull(tempHeuristic, "tempHeuristic");
	}

	/**
	 * Set the optional output channel used by this simulation.
	 * <p>
	 * The MDI {@link SimulationContext} is intentionally minimal and does not provide
	 * message, progress, or refresh APIs; the feedback channel supplies them.
	 * </p>
	 * <p>
	 * Typical usage from the hosting view:
	 * </p>
	 *
	 * <pre>{@code
	 * SimulatedAnnealingSimulation<?> sim =
	 *     (SimulatedAnnealingSimulation<?>) getSimulationEngine().getSimulation();
	 * sim.setFeedback(AnnealingFeedback.forEngine(getSimulationEngine()));
	 * }</pre>
	 *
	 * @param feedback output channel, or {@code null} to discard output
	 */
	public void setFeedback(AnnealingFeedback feedback) {
		this.feedback = feedback == null ? AnnealingFeedback.none() : feedback;
	}

	/**
	 * Get a snapshot of the current annealing state.
	 * <p>
	 * The returned value is suitable for display or logging. The temperature reported here
	 * is the same absolute temperature used for acceptance decisions: the
	 * schedule's relative value multiplied by the estimated {@code T0}.
	 * </p>
	 *
	 * @return a state snapshot
	 */
	public SimulatedAnnealingState getState() {
		return stateSnapshot;
	}

	/**
	 * Return a defensive copy of the best solution found so far.
	 *
	 * @return a copy of the best solution, or {@code null} if initialization has not occurred
	 */
	public S getBestSolutionCopy() {
		return (best == null) ? null : copySolution(best);
	}

	/**
	 * Initialize the simulation.
	 * <p>
	 * This method:
	 * </p>
	 * <ol>
	 *   <li>Initializes the RNG (seeded or non-deterministic based on config).</li>
	 *   <li>Estimates the initial temperature {@code T0} using {@link TemperatureHeuristic}.</li>
	 *   <li>Generates a starting solution using {@link AnnealingProblem#randomSolution(Random)}.</li>
	 *   <li>Computes initial energy and resets counters.</li>
	 *   <li>Posts initialization output through the configured feedback channel.</li>
	 * </ol>
	 *
	 * @param ctx simulation context (cancellation/timing bookkeeping)
	 */
	@Override
	public void init(SimulationContext ctx) {
		long seed = cfg.randomSeed();
		rng = (seed == 0L) ? new Random() : new Random(seed);

		// Estimate initial temperature from problem-specific heuristic
		InitialTemperature it = tempHeuristic.estimate(problem, rng);
		if (it == null || !Double.isFinite(it.T0()) || it.T0() <= 0.0) {
			throw new IllegalStateException(
					"Temperature heuristic must return a finite T0 > 0");
		}
		T0 = it.T0();

		feedback.message(
				"Initial temperature estimated: T0=" + fmt(T0) +
				" (medianE=" + fmt(it.energyMedian()) +
				", MAD=" + fmt(it.energyMad()) +
				", n=" + it.samples() + ")"
			);
		feedback.progress(ProgressInfo.indeterminate("Ready"));
		feedback.refresh();

		// Initialize current/best solution
		current = problem.randomSolution(rng);
		if (current == null) {
			throw new IllegalStateException("Problem returned a null initial solution");
		}
		best = copySolution(current);

		currentE = problem.energy(current);
		requireFiniteEnergy(currentE);
		bestE = currentE;

		// Reset counters
		step = 0;
		accepted = 0;
		uphillAccepted = 0;
		acceptedNotificationCount = 0;
		notifications.clear();
		queuedNotificationCount.set(0);
		publishStateSnapshot();
	}

	/**
	 * Perform one annealing iteration.
	 * <p>
	 * The engine repeatedly calls this method until it returns {@code false}. This method returns
	 * {@code false} when:
	 * </p>
	 * <ul>
	 *   <li>Cancellation is requested via {@link SimulationContext#isCancelRequested()}</li>
	 *   <li>The schedule indicates stopping via {@link AnnealingSchedule#shouldStop(long, SimulatedAnnealingConfig)}</li>
	 *   <li>The temperature falls below {@link SimulatedAnnealingConfig#minTemperature()}</li>
	 * </ul>
	 *
	 * <p>
	 * Progress and refresh notifications are throttled according to
	 * {@link SimulatedAnnealingConfig#progressEverySteps()} and
	 * {@link SimulatedAnnealingConfig#refreshEverySteps()}.
	 * </p>
	 *
	 * @param ctx simulation context (cancellation/timing bookkeeping)
	 * @return {@code true} to continue running, {@code false} to stop
	 * @throws IllegalStateException if the problem returns an unsupported move
	 *                               implementation or a non-finite energy
	 */
	@Override
	public boolean step(SimulationContext ctx) {

		// Cancellation check (external request via engine)
		if (ctx.isCancelRequested()) {
			feedback.message("Cancel requested.");
			return false;
		}

		// Stop conditions driven by schedule/config
		if (schedule.shouldStop(step, cfg)) {
			return false;
		}

		// Temperature floor check
		double T = temperatureAt(step);
		if (!Double.isFinite(T) || T < 0.0) {
			throw new IllegalStateException(
					"Annealing schedule produced an invalid temperature: " + T);
		}
		if (T <= cfg.minTemperature()) {
			feedback.message("Temperature reached minimum; stopping.");
			return false;
		}

		// Propose a move from current state
		AnnealingMove<S> move = Objects.requireNonNull(
				problem.randomMove(rng, current), "problem random move");

		double dE;
		S candidate = current;
		ReversibleAnnealingMove<S> reversible = null;

		if (move instanceof DeltaEnergyMove<?> dem) {
		    @SuppressWarnings("unchecked")
		    DeltaEnergyMove<S> dm = (DeltaEnergyMove<S>) dem;

		    dm.prepare(current);          // <-- critical
		    dE = dm.deltaE(current);      // uses prepared parameters
		    dm.apply(current);             // applies the SAME prepared move
		    reversible = dm;
		} else if (move instanceof ReversibleAnnealingMove<?> rm) {
			@SuppressWarnings("unchecked")
			ReversibleAnnealingMove<S> typed = (ReversibleAnnealingMove<S>) rm;
		    double before = currentE;
		    typed.apply(current);
		    double after = problem.energy(current);
		    requireFiniteEnergy(after);
		    dE = after - before;
			reversible = typed;
		} else if (move instanceof CandidateAnnealingMove<?> cm) {
			@SuppressWarnings("unchecked")
			CandidateAnnealingMove<S> typed = (CandidateAnnealingMove<S>) cm;
			candidate = Objects.requireNonNull(typed.candidate(current),
					"candidate move returned null");
			double after = problem.energy(candidate);
			requireFiniteEnergy(after);
			dE = after - currentE;
		} else {
			throw new IllegalStateException("Unsupported annealing move type: "
					+ move.getClass().getName());
		}
		if (!Double.isFinite(dE)) {
			throw new IllegalStateException("Move energy difference must be finite");
		}

		// Metropolis acceptance criterion
		boolean accept = (dE <= 0) || (rng.nextDouble() < Math.exp(-dE / T));

		if (accept) {
			current = candidate;
			currentE += dE;
			accepted++;

			if (dE > 0) {
				uphillAccepted++;
			}
			notifyListeners(T, currentE, NotifyType.ACCEPTED_MOVE); // accepted move

			// Track best-so-far
			if (currentE < bestE) {
				bestE = currentE;
				best = copySolution(current);
				notifyListeners(T, bestE, NotifyType.NEW_BEST); // new best
			}
		} else {
			if (reversible != null) {
				reversible.undo(current);
			}
		}

		step++;

        // Periodic energy resync to prevent floating-point drift accumulation.
        // currentE is updated incrementally (currentE += dE) which accumulates
        // rounding error over millions of steps. Re-synchronizing against a full
        // energy recomputation keeps the error bounded without measurable overhead
        // since problem.energy() is called at most once every 10,000 steps.
	       if (step % 10_000 == 0) {
	            currentE = problem.energy(current);
			requireFiniteEnergy(currentE);
			if (currentE < bestE) {
				bestE = currentE;
				best = copySolution(current);
				notifyListeners(T, bestE, NotifyType.NEW_BEST);
			}
	        }

		// Optional UI signals (throttled)
		if (cfg.progressEverySteps() > 0 && (step % cfg.progressEverySteps() == 0)) {
				double frac = Math.min(1.0, (double) step / (double) cfg.maxSteps());
				feedback.progress(
					ProgressInfo.determinate(frac,
						"T=" + fmt(T) +
						"  E=" + fmt(currentE) +
						"  best=" + fmt(bestE) +
						"  acc=" + accepted)
				);
			}

		if (cfg.refreshEverySteps() > 0 && (step % cfg.refreshEverySteps() == 0)) {
			feedback.refresh();
		}

		publishStateSnapshot();
		return true;
	}

	/** Current absolute temperature used in Metropolis acceptance (includes T0). */
	public double getAbsoluteTemperature() {
	    return temperatureAt(step); // or store lastT
	}



	/**
	 * Compute the temperature used for acceptance decisions at a given step.
	 * <p>
	 * The injected schedule supplies a relative temperature factor:
	 * </p>
	 *
	 * <pre>
	 *   T(step) = T0 * schedule.temperature(step, config)
	 * </pre>
	 *
	 * <p>
	 * @param step step index (0-based)
	 * @return temperature for this step
	 */
	private double temperatureAt(long step) {
		return T0 * schedule.temperature(step, cfg);
	}

	private static void requireFiniteEnergy(double energy) {
		if (!Double.isFinite(energy)) {
			throw new IllegalStateException("Problem energy must be finite: " + energy);
		}
	}

	private S copySolution(S solution) {
		return Objects.requireNonNull(solution.copy(),
				"Solution copy must not be null");
	}

	private void publishStateSnapshot() {
		stateSnapshot = new SimulatedAnnealingState(step, temperatureAt(step),
				currentE, bestE, accepted, uphillAccepted);
	}

	/**
	 * Format a floating point value for UI/status messages.
	 *
	 * @param x value
	 * @return compact formatted string
	 */
	private static String fmt(double x) {
		return String.format("%.4g", x);
	}

	// notify listeners of message
	private void notifyListeners(double temperature, double energy, NotifyType option) {
		if (acceptedMoveListeners.isEmpty()) {
			return;
		}
		if (option == NotifyType.ACCEPTED_MOVE) {
			acceptedNotificationCount++;
			long stride = cfg.notificationPolicy().acceptedMoveStride();
			if (acceptedNotificationCount % stride != 0) {
				return;
			}
		}
		if (queuedNotificationCount.incrementAndGet()
				> cfg.notificationPolicy().maximumQueued()) {
			queuedNotificationCount.decrementAndGet();
			return;
		}
		notifications.add(new MoveNotification(temperature, energy, option));
		queueNotificationDrain();
	}

	private void queueNotificationDrain() {
		if (notificationPending.compareAndSet(false, true)) {
			SwingUtilities.invokeLater(this::drainNotifications);
		}
	}

	private void drainNotifications() {
		notificationPending.set(false);
		int delivered = 0;
		MoveNotification notification;
		while (delivered < cfg.notificationPolicy().maximumPerDrain()
				&& (notification = notifications.poll()) != null) {
			queuedNotificationCount.decrementAndGet();
			for (IAcceptedMoveListener listener : acceptedMoveListeners) {
				try {
					if (notification.type() == NotifyType.ACCEPTED_MOVE) {
						listener.acceptedMove(notification.temperature(), notification.energy());
					} else {
						listener.newBest(notification.temperature(), notification.energy());
					}
				} catch (Throwable failure) {
					edu.cnu.mdi.log.Log.getInstance().exception(failure);
				}
			}
			delivered++;
		}
		if (!notifications.isEmpty()) {
			queueNotificationDrain();
		}
	}

	/**
	 * Add an AcceptedMoveListener.
	 * Duplicate registrations are ignored. This method is safe to call while the
	 * simulation and EDT notification drain are active.
	 *
	 * @param listener the AcceptedMoveListener to add.
	 */
	public void addAcceptedMoveListener(IAcceptedMoveListener listener) {
		if (listener != null) {
			acceptedMoveListeners.addIfAbsent(listener);
		}
	}

	/**
	 * Remove an AcceptedMoveListener.
	 * This method is safe to call from any thread and has no effect when the
	 * listener is not registered.
	 *
	 * @param listener the AcceptedMoveListener to remove.
	 */

	public void removeAcceptedMoveListener(IAcceptedMoveListener listener) {

		acceptedMoveListeners.remove(listener);
	}

}
