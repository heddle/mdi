package edu.cnu.mdi.sim.simanneal;

import java.util.Objects;
import java.util.Random;

import javax.swing.event.EventListenerList;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;

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
 *   <li>Optional UI feedback (messages, progress, refresh requests) is posted via an injected
 *       {@link SimulationEngine} using {@link #setEngine(SimulationEngine)}.</li>
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
 * uses a geometric decay based on the configured {@code alpha} and {@code stepsPerTemperature}.
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
 * <h2>Move undo requirement</h2>
 * <p>
 * Rejected moves are reverted by calling {@link AnnealingMove#undo(AnnealingSolution)}.
 * Therefore, moves should support undo for correctness. If your problem's move type does not
 * support undo, redesign the move API (e.g., "copy neighbor") or maintain a pre-move copy.
 * </p>
 *
 * @param <S> concrete solution type for the annealing problem
 */
public class SimulatedAnnealingSimulation<S extends AnnealingSolution> implements Simulation {

	private EventListenerList _listenerList;

	/** The annealing problem (solution generator, energy function, and move generator). */
	private final AnnealingProblem<S> problem;

	/** Configuration parameters controlling step limits, cooling rate, and UI throttling. */
	private final SimulatedAnnealingConfig cfg;

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
	private S best;

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
	 * Optional engine reference used to post messages/progress/refresh.
	 * <p>
	 * Marked {@code transient} because simulations may be serialized as part of view state;
	 * the engine is runtime infrastructure and must be re-injected after deserialization.
	 * </p>
	 */
	private transient SimulationEngine engine;

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
	 * Inject the owning {@link SimulationEngine} so this simulation can emit UI signals.
	 * <p>
	 * The MDI {@link SimulationContext} is intentionally minimal and does not provide
	 * message/progress/refresh APIs. In MDI, those notifications are posted via the engine.
	 * </p>
	 * <p>
	 * Typical usage from the hosting view:
	 * </p>
	 *
	 * <pre>{@code
	 * SimulatedAnnealingSimulation<?> sim =
	 *     (SimulatedAnnealingSimulation<?>) getSimulationEngine().getSimulation();
	 * sim.setEngine(getSimulationEngine());
	 * }</pre>
	 *
	 * @param engine the engine hosting this simulation (may be null to disable UI posting)
	 */
	public void setEngine(SimulationEngine engine) {
		this.engine = engine;
	}

	/**
	 * Get a snapshot of the current annealing state.
	 * <p>
	 * The returned value is suitable for display or logging. The temperature reported here
	 * is derived from {@link AnnealingSchedule#temperature(long, SimulatedAnnealingConfig)}.
	 * Note that the actual temperature used internally for acceptance decisions comes from
	 * {@link #temperatureAt(long)} which incorporates the estimated {@code T0}.
	 * </p>
	 *
	 * @return a state snapshot
	 */
	public SimulatedAnnealingState getState() {
		return new SimulatedAnnealingState(
				step,
				schedule.temperature(step, cfg),
				currentE,
				bestE,
				accepted,
				uphillAccepted);
	}

	/**
	 * Return a defensive copy of the best solution found so far.
	 *
	 * @return a copy of the best solution, or {@code null} if initialization has not occurred
	 */
	@SuppressWarnings("unchecked")
	public S getBestSolutionCopy() {
		return (best == null) ? null : (S) best.copy();
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
	 *   <li>Optionally posts a message/progress/refresh via the injected engine.</li>
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
		T0 = it.T0();

		if (engine != null) {
			engine.postMessage(
				"Initial temperature estimated: T0=" + fmt(T0) +
				" (medianE=" + fmt(it.energyMedian()) +
				", MAD=" + fmt(it.energyMad()) +
				", n=" + it.samples() + ")"
			);
			engine.postProgress(ProgressInfo.indeterminate("Ready"));
			engine.requestRefresh();
		}

		// Initialize current/best solution
		current = problem.randomSolution(rng);
		best = (S) current.copy();

		currentE = problem.energy(current);
		bestE = currentE;

		// Reset counters
		step = 0;
		accepted = 0;
		uphillAccepted = 0;
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
	 * @throws RuntimeException if a move is rejected and {@link AnnealingMove#undo(AnnealingSolution)}
	 *                          is not supported (caller should ensure moves support undo)
	 */
	@Override
	public boolean step(SimulationContext ctx) {

		// Cancellation check (external request via engine)
		if (ctx.isCancelRequested()) {
			if (engine != null) {
				engine.postMessage("Cancel requested.");
			}
			return false;
		}

		// Stop conditions driven by schedule/config
		if (schedule.shouldStop(step, cfg)) {
			return false;
		}

		// Temperature floor check
		double T = temperatureAt(step);
		if (T <= cfg.minTemperature()) {
			if (engine != null) {
				engine.postMessage("Temperature reached minimum; stopping.");
			}
			return false;
		}

		// Propose a move from current state
		AnnealingMove<S> move = problem.randomMove(rng, current);

		double dE;

		if (move instanceof DeltaEnergyMove<?> dem) {
		    @SuppressWarnings("unchecked")
		    DeltaEnergyMove<S> dm = (DeltaEnergyMove<S>) dem;

		    dm.prepare(current);          // <-- critical
		    dE = dm.deltaE(current);      // uses prepared parameters
		    move.apply(current);          // applies the SAME prepared move
		} else {
		    double before = currentE;
		    move.apply(current);
		    double after = problem.energy(current);
		    dE = after - before;
		}

		// Metropolis acceptance criterion
		boolean accept = (dE <= 0) || (rng.nextDouble() < Math.exp(-dE / T));

		if (accept) {
			currentE += dE;
			accepted++;

			if (dE > 0) {
				uphillAccepted++;
			}
			onAcceptedMove(T, currentE);
			notifyListeners(T, currentE, 0); // accepted move

			// Track best-so-far
			if (currentE < bestE) {
				bestE = currentE;
				best = (S) current.copy();
				notifyListeners(T, bestE, 1); // new best
			}
		} else {
			// Revert rejected move (moves must support undo for correctness)
			move.undo(current);
		}

		step++;

		// Optional UI signals (throttled)
		if (engine != null) {

			if (cfg.progressEverySteps() > 0 && (step % cfg.progressEverySteps() == 0)) {
				double frac = Math.min(1.0, (double) step / (double) cfg.maxSteps());
				engine.postProgress(
					ProgressInfo.determinate(frac,
						"T=" + fmt(T) +
						"  E=" + fmt(currentE) +
						"  best=" + fmt(bestE) +
						"  acc=" + accepted)
				);
			}

			if (cfg.refreshEverySteps() > 0 && (step % cfg.refreshEverySteps() == 0)) {
				engine.requestRefresh();
			}
		}

		return true;
	}
	
	/** Current absolute temperature used in Metropolis acceptance (includes T0). */
	public double getAbsoluteTemperature() {
	    return temperatureAt(step); // or store lastT
	}

	
	/**
	 * Called after each accepted move.
	 * <p>
	 * Subclasses or listeners may override to record (T, E) pairs
	 * for live plotting.
	 * </p>
	 */
	protected void onAcceptedMove(double temperature, double energy) {
	//    System.err.println("accepted T = " + temperature + "  E = " + energy);
	}


	/**
	 * Compute the temperature used for acceptance decisions at a given step.
	 * <p>
	 * This implementation uses a geometric cooling schedule:
	 * </p>
	 *
	 * <pre>
	 *   T(step) = T0 * alpha^k
	 *   k = step / stepsPerTemperature
	 * </pre>
	 *
	 * <p>
	 * If {@link SimulatedAnnealingConfig#stepsPerTemperature()} is non-positive, this
	 * method uses {@code k = step}.
	 * </p>
	 *
	 * @param step step index (0-based)
	 * @return temperature for this step
	 */
	private double temperatureAt(long step) {
		long k = (cfg.stepsPerTemperature() <= 0) ? step : (step / cfg.stepsPerTemperature());
		return T0 * Math.pow(cfg.alpha(), k);
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
	private void notifyListeners(double temperature, double energy, int option) {

		if (_listenerList == null) {
			return;
		}

		// Guaranteed to return a non-null array
		Object[] listeners = _listenerList.getListenerList();

		// This weird loop is the bullet proof way of notifying all listeners.
		// for (int i = listeners.length - 2; i >= 0; i -= 2) {
		// order is flipped so it goes in order as added
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == IAcceptedMoveListener.class) {

				IAcceptedMoveListener listener = (IAcceptedMoveListener) listeners[i + 1];

				if (option == 1)
					listener.newBest(temperature, energy);
				else if (option == 0)
					listener.acceptedMove(temperature, energy);

			}
		}
	}
	
	/**
	 * Add an AcceptedMoveListener.
	 *
	 * @param listener the AcceptedMoveListener to add.
	 */
	public void addAcceptedMoveListener(IAcceptedMoveListener listener) {

		if (_listenerList == null) {
			_listenerList = new EventListenerList();
		}

		// avoid adding duplicates
		_listenerList.remove(IAcceptedMoveListener.class, listener);
		_listenerList.add(IAcceptedMoveListener.class, listener);
	}

	/**
	 * Remove an AcceptedMoveListener.
	 *
	 * @param listener the AcceptedMoveListener to remove.
	 */

	public void removeAcceptedMoveListener(IAcceptedMoveListener listener) {

		if ((listener == null) || (_listenerList == null)) {
			return;
		}

		_listenerList.remove(IAcceptedMoveListener.class, listener);
	}

}
