package edu.cnu.mdi.sim.simanneal.tspdemo;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;
import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.SimulationListener;
import edu.cnu.mdi.sim.SimulationState;
import edu.cnu.mdi.sim.simanneal.AnnealingSchedule;
import edu.cnu.mdi.sim.simanneal.GeometricAnnealingSchedule;
import edu.cnu.mdi.sim.simanneal.SimulatedAnnealingConfig;
import edu.cnu.mdi.sim.simanneal.SimulatedAnnealingSimulation;
import edu.cnu.mdi.sim.simanneal.TemperatureHeuristic;
import edu.cnu.mdi.sim.simanneal.heuristics.EnergyDistributionHeuristic;
import edu.cnu.mdi.sim.simanneal.tspdemo.TspAnnealingProblem;
import edu.cnu.mdi.sim.simanneal.tspdemo.TspModel;
import edu.cnu.mdi.sim.simanneal.tspdemo.TspSolution;

/**
 * Console-only (no UI) runner for the TSP simulated annealing demo.
 * <p>
 * This intentionally runs through {@link SimulationEngine} so the simulation
 * sees the real {@link SimulationContext}, real state transitions, and the same
 * threading model used by MDI views.
 * </p>
 */
public final class TspHeadlessRunner {

	private TspHeadlessRunner() {
	}

	public static void main(String[] args) throws Exception {

		// ------------------------------------------------------------
		// 1) Build model (use a deterministic RNG for reproducibility)
		// ------------------------------------------------------------
		long seed = 12345L; // set 0L for nondeterministic
		Random modelRng = (seed == 0L) ? new Random() : new Random(seed);

		int cityCount = 60;
		boolean includeRiver = true;
		double riverPenalty = 0.35;

		// Your TspModel currently has includeRiver final; for a runtime toggle,
		// you’ll want the small “riverEnabled” switch we discussed.
		// For now, this runner *assumes* you have:
		// - boolean isRiverEnabled()
		// - void setRiverEnabled(boolean)
		TspModel model = new TspModel(cityCount, includeRiver, riverPenalty, modelRng);

		// Optional: start with river enabled
		if (includeRiver) {
			trySetRiverEnabled(model, true);
		}

		// ------------------------------------------------------------
		// 2) Build annealing problem + SA simulation
		// ------------------------------------------------------------
		TspAnnealingProblem problem = new TspAnnealingProblem(model);

		SimulatedAnnealingConfig saCfg = SimulatedAnnealingConfig.defaults().withProgressEverySteps(20_000);

		// If your AnnealingSchedule returns a temperature “multiplier”, your SA sim
		// already uses T0*alpha^k internally, so schedule can be stop-only.

		AnnealingSchedule schedule = new GeometricAnnealingSchedule(); // use whatever you currently have

		TemperatureHeuristic<TspSolution> heuristic = new EnergyDistributionHeuristic<>(300, 0.80, 1e-6);

		SimulatedAnnealingSimulation<TspSolution> sim = new SimulatedAnnealingSimulation<>(problem, saCfg, schedule,
				heuristic);

		// Important: inject engine later, once created (matches your sim pattern).

		// ------------------------------------------------------------
		// 3) Create engine (this creates SimulationContext internally)
		// ------------------------------------------------------------
		SimulationEngineConfig engineCfg = new SimulationEngineConfig(0, // refreshIntervalMs: 0 disables periodic
																			// refresh
				25, // progress ping (indeterminate) interval; optional
				0, // cooperativeYieldMs: 0 = max speed
				false // autoRun: false so we explicitly requestRun
		);

		SimulationEngine engine = new SimulationEngine(sim, engineCfg);
		sim.setEngine(engine);

		// ------------------------------------------------------------
		// 4) Listener prints lifecycle + progress + “river toggle” mid-run
		// ------------------------------------------------------------
		CountDownLatch done = new CountDownLatch(1);

		engine.addListener(new SimulationListener() {

			private long lastPrintedStep = -1;

			@Override
			public void onStateChange(SimulationContext ctx, SimulationState from, SimulationState to, String reason) {
				System.out.println("STATE: " + from + " -> " + to + (reason == null ? "" : (" (" + reason + ")")));
			}

			@Override
			public void onMessage(SimulationContext ctx, String message) {
				System.out.println("MSG: " + message);
			}

			@Override
			public void onReady(SimulationContext ctx) {
				System.out.println("READY: requesting run now");
				engine.requestRun();
			}

			@Override
			public void onProgress(SimulationContext ctx, ProgressInfo progress) {
				// ctx.getStepCount() is maintained by the engine each step
				long step = ctx.getStepCount();

				// Throttle printing
				if (step == lastPrintedStep)
					return;
				lastPrintedStep = step;

				// Print a compact line occasionally

				var st = sim.getState();
				System.out.println("step=" + st.step() + "  T=" + fmt(st.temperature()) + "  Tabs="
						+ fmt(sim.getAbsoluteTemperature()) + "  E=" + fmt(st.currentEnergy()) + "  best="
						+ fmt(st.bestEnergy()) + "  acc=" + st.acceptedMoves() + "  up=" + st.uphillAcceptedMoves());

			}

			@Override
			public void onDone(SimulationContext ctx) {
				System.out.println(
						"DONE: steps=" + ctx.getStepCount() + " elapsed=" + fmt(ctx.getElapsedSeconds()) + "s");
				var st = sim.getState();
				System.out.println("BEST: " + fmt(st.bestEnergy()) + " accepted=" + st.acceptedMoves() + " uphill="
						+ st.uphillAcceptedMoves());
				done.countDown();
			}

			@Override
			public void onFail(SimulationContext ctx, Throwable error) {
				System.err.println("FAILED: " + error);
				error.printStackTrace(System.err);
				done.countDown();
			}
		});

		// ------------------------------------------------------------
		// 5) Run
		// ------------------------------------------------------------

		engine.start();

		// Wait for completion
		done.await(10, TimeUnit.MINUTES);

		// If it didn’t finish, stop it
		if (engine.getState() != SimulationState.TERMINATED && engine.getState() != SimulationState.FAILED) {
			System.out.println("Timeout; requesting stop.");
			engine.requestStop();
		}
	}

	private static String fmt(double x) {
		return String.format("%.4g", x);
	}

	/**
	 * Helper that attempts to call the river toggle API if present.
	 * <p>
	 * This lets the runner compile even before you finalize the toggle methods.
	 * Once you add {@code setRiverEnabled(boolean)} directly, replace this with a
	 * direct call.
	 * </p>
	 *
	 * @return true if the toggle call succeeded
	 */
	private static boolean trySetRiverEnabled(TspModel model, boolean enabled) {
		try {
			// Replace with: model.setRiverEnabled(enabled);
			var m = model.getClass().getMethod("setRiverEnabled", boolean.class);
			m.invoke(model, enabled);
			return true;
		} catch (Throwable t) {
			return false;
		}
	}
}
