package edu.cnu.mdi.sim.simanneal.tspdemo;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.lang.reflect.Method;
import java.util.Random;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.sim.SimulationEngine;
import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.SimulationState;
import edu.cnu.mdi.sim.simanneal.AnnealingSchedule;
import edu.cnu.mdi.sim.simanneal.EvsTPlotPanel;
import edu.cnu.mdi.sim.simanneal.GeometricAnnealingSchedule;
import edu.cnu.mdi.sim.simanneal.IAcceptedMoveListener;
import edu.cnu.mdi.sim.simanneal.SimulatedAnnealingConfig;
import edu.cnu.mdi.sim.simanneal.SimulatedAnnealingSimulation;
import edu.cnu.mdi.sim.simanneal.TemperatureHeuristic;
import edu.cnu.mdi.sim.simanneal.heuristics.EnergyDistributionHeuristic;
import edu.cnu.mdi.sim.ui.SimulationView;

/**
 * Traveling Salesperson (TSP) simulated annealing demo hosted in an MDI
 * {@link SimulationView}.
 * <p>
 * This view uses the {@code edu.cnu.mdi.sim.simanneal} package with a TSP
 * problem adapter and displays the current best tour as annealing improves it.
 * </p>
 *
 * <h2>Controls</h2>
 * <p>
 * The demo uses {@link TspDemoControlPanel}, which adds sliders for city count
 * and river penalty plus a Reset button. These controls are enabled only in
 * {@link SimulationState#READY} or {@link SimulationState#TERMINATED}.
 * </p>
 *
 * <h2>Reset</h2>
 * <p>
 * Reset rebuilds the model, problem, and simulation, and swaps a new
 * {@link SimulationEngine} into the view. This is required because
 * {@link SimulationEngine} holds its {@code Simulation} as a {@code final}
 * field.
 * </p>
 */
@SuppressWarnings("serial")
public class TspDemoView extends SimulationView implements ITspDemoResettable, IAcceptedMoveListener {

	public static final int DEFAULT_NUM_CITY = 60;
	public static final float DEFAULT_RIVER_PENALTY = 0.35f;

	/**
	 * Thread-local bundle used to pass created objects across the super(...)
	 * boundary.
	 */
	private static final ThreadLocal<Bundle> BUNDLE_TL = new ThreadLocal<>();

	/** Backing model (world coords in [0,1]). */
	private volatile TspModel model;

	/** Concrete SA simulation instance used by this view. */
	private volatile SimulatedAnnealingSimulation<TspSolution> sim;

	/** Cached best tour snapshot for drawing. */
	private volatile int[] bestTourSnapshot;

	/** Cached city screen radius in pixels. */
	private final int cityRadiusPx = 4;

	/** RNG seed used for reproducible model generation (0L => nondeterministic). */
	private static final long seed = 0L;

	private static EvsTPlotPanel evtPlot;

	/**
	 * Create the TSP demo view.
	 *
	 * @param keyVals standard {@link edu.cnu.mdi.view.BaseView} key-value arguments
	 */
	public TspDemoView(Object... keyVals) {

		// Must call super(...) first; create simulation + stash model bundle for
		// retrieval.
		super(createSimulationAndStashBundle(DEFAULT_NUM_CITY, DEFAULT_RIVER_PENALTY, seed),
				new SimulationEngineConfig(33, 250, 30, false), true,
				(SimulationView.ControlPanelFactory) TspDemoControlPanel::new, keyVals);

		evtPlot = createScatterPanel();
		add(evtPlot, BorderLayout.EAST);


		// Recover bundle created by createSimulationAndStashBundle().
		Bundle b = BUNDLE_TL.get();
		BUNDLE_TL.remove();

		this.model = b.model;

		@SuppressWarnings("unchecked")
		SimulatedAnnealingSimulation<TspSolution> s = (SimulatedAnnealingSimulation<TspSolution>) getSimulationEngine()
				.getSimulation();
		this.sim = s;
		this.sim.addAcceptedMoveListener(this);


		// Allow sim to post progress/messages/refresh through this view’s engine.
		this.sim.setEngine(getSimulationEngine());

		// Install drawing hooks.
		setBeforeDraw();
		setAfterDraw();

		// Initial view scale (cities are in [0,1]).
		if (getContainer() != null) {
			getContainer().scale(1.25);
		}

		pack();
		// Start engine thread (paused until Run unless autoRun or startAndRun()).
		startSimulation();
	}

	private EvsTPlotPanel createScatterPanel() {
		EvsTPlotPanel scatterPanel = new EvsTPlotPanel("Traveling Salesperson Problem", "temperature", "energy (length)");
		scatterPanel.getPlotCanvas().getParameters().setReverseXaxis(true);
		return scatterPanel;
	}

	/**
	 * Create a simulation instance and stash a bundle (model) in a thread-local so
	 * the constructor can retrieve it after {@code super(...)} returns.
	 *
	 * @param cityCount    number of cities
	 * @param riverPenalty river penalty in [-1,1]
	 * @param seed         RNG seed (0L for nondeterministic)
	 * @return simulation instance
	 */
	private static SimulatedAnnealingSimulation<TspSolution> createSimulationAndStashBundle(int cityCount,
			double riverPenalty, long seed) {
		Random rng = (seed == 0L) ? new Random() : new Random(seed);

		boolean includeRiver = true;
		TspModel model = new TspModel(cityCount, includeRiver, riverPenalty, rng);

		TspAnnealingProblem problem = new TspAnnealingProblem(model);

		SimulatedAnnealingConfig cfg = SimulatedAnnealingConfig.defaults();

		AnnealingSchedule schedule = new GeometricAnnealingSchedule();


		TemperatureHeuristic<TspSolution> heuristic = new EnergyDistributionHeuristic<>(300, 0.80, 1e-6);

		SimulatedAnnealingSimulation<TspSolution> sim = new SimulatedAnnealingSimulation<>(problem, cfg, schedule,
				heuristic);



		BUNDLE_TL.set(new Bundle(model));
		return sim;
	}

	// -------------------------------------------------------------------------
	// Reset hook (from TspDemoControlPanel)
	// -------------------------------------------------------------------------

	@Override
	public void requestReset(int cityCount, double riverPenalty) {

		// Only honor reset in safe states.
		SimulationState state = getSimulationEngine().getState();
		boolean editable = (state == SimulationState.READY || state == SimulationState.TERMINATED);
		if (!editable) {
			// ignore (panel should already be disabled, but be defensive)
			return;
		}

		evtPlot.clearData();

		// Build a new model/sim with the requested parameters.
		SimulatedAnnealingSimulation<TspSolution> newSim = createSimulationAndStashBundle(cityCount, riverPenalty,
				seed);

		sim.removeAcceptedMoveListener(this);
		newSim.addAcceptedMoveListener(this);

		Bundle b = BUNDLE_TL.get();
		BUNDLE_TL.remove();

		TspModel newModel = b.model;

		// Reuse the engine config (refresh/progress/yield/autRun) from the current
		// engine.
		SimulationEngineConfig engineCfg = getSimulationEngine().getConfig();

		// Create a brand new engine (SimulationEngine holds a final Simulation).
		SimulationEngine newEngine = new SimulationEngine(newSim, engineCfg);

		// Swap engine into the view (requires the small SimulationView patch below).
		replaceEngine(newEngine);

		// Update local references.
		this.model = newModel;
		this.sim = newSim;
		this.bestTourSnapshot = null;

		// Let sim post through the new engine.
		this.sim.setEngine(getSimulationEngine());

		// Start new engine thread (paused in READY until Run).
		startSimulation();

		// Kick a refresh so the new city set appears immediately.
		getSimulationEngine().requestRefresh();
	}

	// -------------------------------------------------------------------------
	// Drawing
	// -------------------------------------------------------------------------

	/** Install "before draw" hook: draw tour + (optional) river. */
	private void setBeforeDraw() {
		getContainer().setBeforeDraw(new DrawableAdapter() {
			@Override
			public void draw(Graphics2D g2, IContainer container) {
				if (container == null) {
					return;
				}

				TspModel m = model; // volatile snapshot
				if (m == null) {
					return;
				}

				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				// Draw optional river first (behind tour).
				drawRiverIfEnabled(g2, container, m);

				// Draw best tour (if available)
				int[] tour = bestTourSnapshot;
				if (tour == null || tour.length < 2) {
					// Seed snapshot from current best solution (may be null early).
					SimulatedAnnealingSimulation<TspSolution> s = sim;
					if (s != null) {
						TspSolution best = s.getBestSolutionCopy();
						if (best != null && best.tour != null) {
							bestTourSnapshot = best.tour.clone();
							tour = bestTourSnapshot;
						}
					}
				}
				if (tour == null || tour.length < 2) {
					return;
				}

				g2.setColor(Color.RED.darker());
				g2.setStroke(new BasicStroke(2f));

				Point p0 = new Point();
				Point p1 = new Point();

				for (int i = 0; i < tour.length; i++) {
					int a = tour[i];
					int b = tour[(i + 1) % tour.length];

					var wa = m.cities[a];
					var wb = m.cities[b];

					container.worldToLocal(p0, wa.x, wa.y);
					container.worldToLocal(p1, wb.x, wb.y);

					g2.drawLine(p0.x, p0.y, p1.x, p1.y);
				}
			}
		});
	}

	/** Install "after draw" hook: draw city markers. */
	private void setAfterDraw() {
		getContainer().setAfterDraw(new DrawableAdapter() {
			@Override
			public void draw(Graphics2D g2, IContainer container) {
				if (container == null) {
					return;
				}

				TspModel m = model; // volatile snapshot
				if (m == null) {
					return;
				}

				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				Point p = new Point();

				g2.setColor(Color.BLACK);
				for (var c : m.cities) {
					container.worldToLocal(p, c.x, c.y);
					g2.fillOval(p.x - cityRadiusPx, p.y - cityRadiusPx, 2 * cityRadiusPx, 2 * cityRadiusPx);
				}
			}
		});
	}

	/**
	 * Attempt to draw a "river" line if the model supports it and it is enabled.
	 * <p>
	 * Supports either:
	 * </p>
	 * <ul>
	 * <li>a direct {@code boolean isRiverEnabled()} API, or</li>
	 * <li>fallback: draw if {@code riverX} is finite (legacy behavior).</li>
	 * </ul>
	 *
	 * @param g2        graphics
	 * @param container container
	 * @param m         model snapshot
	 */
	private void drawRiverIfEnabled(Graphics2D g2, IContainer container, TspModel m) {
		boolean draw = isRiverEnabledReflective(m);

		// If no toggle API exists, fall back to "riverX finite means it exists".
		if (!draw && !Double.isFinite(m.riverX)) {
			return;
		}
		if (!draw && Double.isFinite(m.riverX)) {
			draw = true;
		}
		if (!draw) {
			return;
		}

		double x = m.riverX;
		if (!Double.isFinite(x)) {
			return;
		}

		g2.setColor(new Color(40, 90, 200));
		g2.setStroke(new BasicStroke(1.5f));

		Point p0 = new Point();
		Point p1 = new Point();
		container.worldToLocal(p0, x, 0.0);
		container.worldToLocal(p1, x, 1.0);
		g2.drawLine(p0.x, p0.y, p1.x, p1.y);
	}

	/**
	 * Reflection helper to support a future {@code isRiverEnabled()} API without
	 * forcing it today.
	 *
	 * @param model model
	 * @return true if the model exposes and returns true from
	 *         {@code isRiverEnabled()}
	 */
	private static boolean isRiverEnabledReflective(TspModel model) {
		try {
			Method m = model.getClass().getMethod("isRiverEnabled");
			Object r = m.invoke(model);
			return (r instanceof Boolean) ? ((Boolean) r).booleanValue() : false;
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Called on the EDT when progress arrives. We use this to refresh the cached
	 * best tour snapshot so drawing doesn’t need to touch mutable simulation
	 * internals.
	 */
	@Override
	protected void onSimulationProgress(edu.cnu.mdi.sim.SimulationContext ctx, edu.cnu.mdi.sim.ProgressInfo progress) {
		SimulatedAnnealingSimulation<TspSolution> s = sim;
		if (s == null) {
			return;
		}

		TspSolution best = s.getBestSolutionCopy();
		if (best != null && best.tour != null) {
			bestTourSnapshot = best.tour.clone();
		}
	}

	/** Simple bundle holding model references across the super(...) boundary. */
	private static final class Bundle {
		final TspModel model;

		Bundle(TspModel model) {
			this.model = model;
		}
	}

	@Override
	public void acceptedMove(double temperature, double energy) {
		evtPlot.addAccepted(temperature, energy);
	}

	@Override
	public void newBest(double temperature, double energy) {
		evtPlot.addBest(temperature, energy);
	}
}
