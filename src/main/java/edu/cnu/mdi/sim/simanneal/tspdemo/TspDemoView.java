package edu.cnu.mdi.sim.simanneal.tspdemo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.lang.reflect.Method;
import java.util.Random;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.simanneal.AnnealingSchedule;
import edu.cnu.mdi.sim.simanneal.GeometricAnnealingSchedule;
import edu.cnu.mdi.sim.simanneal.SimulatedAnnealingConfig;
import edu.cnu.mdi.sim.simanneal.SimulatedAnnealingSimulation;
import edu.cnu.mdi.sim.simanneal.TemperatureHeuristic;
import edu.cnu.mdi.sim.simanneal.heuristics.EnergyDistributionHeuristic;
import edu.cnu.mdi.sim.ui.IconSimulationControlPanel;
import edu.cnu.mdi.sim.ui.SimulationView;
import edu.cnu.mdi.sim.ui.StandardSimIcons;

/**
 * Traveling Salesperson (TSP) simulated annealing demo hosted in an MDI {@link SimulationView}.
 * <p>
 * This view uses the {@code edu.cnu.mdi.sim.simanneal} package with a TSP problem adapter and
 * displays the current best tour as annealing improves it.
 * </p>
 *
 * <h2>Rendering</h2>
 * <ul>
 *   <li>Draws the best tour as a closed polyline.</li>
 *   <li>Draws city locations as filled circles.</li>
 *   <li>Optionally draws a vertical "river" line if the model supports toggling.</li>
 * </ul>
 *
 * <h2>Simulation</h2>
 * <p>
 * Uses {@link SimulatedAnnealingSimulation} with {@link TspAnnealingProblem} and a
 * {@link EnergyDistributionHeuristic} for initial temperature.
 * </p>
 *
 * <h2>Constructor ordering</h2>
 * <p>
 * Java requires {@code super(...)} to be the first statement. To allow the view to keep
 * references to the created {@link TspModel} and concrete simulation instance, this class
 * uses a small thread-local "bundle" created by {@link #createBundle()} and retrieved
 * immediately after {@code super(...)} returns.
 * </p>
 */
@SuppressWarnings("serial")
public class TspDemoView extends SimulationView {

    /** Thread-local bundle used to pass created objects across the super(...) boundary. */
    private static final ThreadLocal<Bundle> BUNDLE_TL = new ThreadLocal<>();

    /** Backing model (world coords in [0,1]). */
    private final TspModel model;

    /** Concrete SA simulation instance used by this view. */
    private final SimulatedAnnealingSimulation<TspSolution> sim;

    /** Cached best tour snapshot for drawing (may be null until first improvement). */
    private volatile int[] bestTourSnapshot;

    /** Cached city screen radius in pixels. */
    private final int cityRadiusPx = 4;

    /**
     * Create the TSP demo view.
     *
     * @param keyVals standard {@link edu.cnu.mdi.view.BaseView} key-value arguments
     */
    public TspDemoView(Object... keyVals) {

        // Create the simulation (and a bundle containing the model) via a helper.
        // Must call super(...) first.
        super(
            createSimulationAndStashBundle(),
            new SimulationEngineConfig(33, 250, 0, false),
            true,
            (SimulationView.ControlPanelFactory) () ->
                new IconSimulationControlPanel(new StandardSimIcons()),
            keyVals
        );

        // Recover bundle created by createSimulationAndStashBundle().
        Bundle b = BUNDLE_TL.get();
        BUNDLE_TL.remove();

        this.model = b.model;

        @SuppressWarnings("unchecked")
        SimulatedAnnealingSimulation<TspSolution> s =
            (SimulatedAnnealingSimulation<TspSolution>) getSimulationEngine().getSimulation();
        this.sim = s;

        // Let the simulation post progress/messages/refresh through this view’s engine.
        this.sim.setEngine(getSimulationEngine());

        // Install drawing hooks.
        setBeforeDraw();
        setAfterDraw();

        // Initial view scale (cities are in [0,1]).
        if (getContainer() != null) {
            getContainer().scale(1.25);
        }

        // Start engine thread (paused until Run unless autoRun or startAndRun()).
        startSimulation();
    }

    /**
     * Create a simulation instance and stash a bundle (model + config) in a thread-local
     * so the constructor can retrieve it after {@code super(...)} returns.
     */
    private static SimulatedAnnealingSimulation<TspSolution> createSimulationAndStashBundle() {

        // ------------------------------------------------------------
        // Model parameters (tweak as desired)
        // ------------------------------------------------------------
        long seed = 12345L;         // 0L for nondeterministic
        Random rng = (seed == 0L) ? new Random() : new Random(seed);

        int cityCount = 60;
        boolean includeRiver = true;
        double riverPenalty = -.35;

        TspModel model = new TspModel(cityCount, includeRiver, riverPenalty, rng);

        // ------------------------------------------------------------
        // Annealing problem + sim config
        // ------------------------------------------------------------
        TspAnnealingProblem problem = new TspAnnealingProblem(model);

        // Use defaults (you can override fields via your record "with" helpers, if you added them).
        SimulatedAnnealingConfig cfg = SimulatedAnnealingConfig.defaults();

        AnnealingSchedule schedule = new GeometricAnnealingSchedule();

        TemperatureHeuristic<TspSolution> heuristic =
            new EnergyDistributionHeuristic<>(300, 0.80, 1e-6);

        SimulatedAnnealingSimulation<TspSolution> sim =
            new SimulatedAnnealingSimulation<>(problem, cfg, schedule, heuristic);

        // Stash bundle for constructor to recover
        BUNDLE_TL.set(new Bundle(model));

        return sim;
    }

    /** Install "before draw" hook: draw tour + (optional) river. */
    private void setBeforeDraw() {
        getContainer().setBeforeDraw(new DrawableAdapter() {
            @Override
            public void draw(Graphics2D g2, IContainer container) {
                if (container == null) return;

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw optional river first (so it appears behind the tour).
                drawRiverIfEnabled(g2, container);

                // Draw best tour (if available)
                int[] tour = bestTourSnapshot;
                if (tour == null || tour.length < 2) {
                    // Try to seed snapshot from the current best solution (may be null early).
                    TspSolution best = sim.getBestSolutionCopy();
                    if (best != null && best.tour != null) {
                        bestTourSnapshot = best.tour.clone();
                        tour = bestTourSnapshot;
                    }
                }

                if (tour == null || tour.length < 2) return;

                // Tour polyline
                g2.setColor(Color.RED.darker());
                g2.setStroke(new BasicStroke(2f));

                Point p0 = new Point();
                Point p1 = new Point();

                for (int i = 0; i < tour.length; i++) {
                    int a = tour[i];
                    int b = tour[(i + 1) % tour.length];

                    var wa = model.cities[a];
                    var wb = model.cities[b];

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
                if (container == null) return;

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Point p = new Point();

                // City markers
                g2.setColor(Color.BLACK);
                for (var c : model.cities) {
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
     * <ul>
     *   <li>a direct {@code boolean isRiverEnabled()} API, or</li>
     *   <li>fallback: draw if {@code includeRiver} was true at construction (legacy behavior).</li>
     * </ul>
     * </p>
     */
    private void drawRiverIfEnabled(Graphics2D g2, IContainer container) {
        boolean draw = isRiverEnabledReflective(model);

        // If no toggle API exists, fall back to "includeRiver" semantics (riverX is NaN otherwise).
        if (!draw && !Double.isFinite(model.riverX)) return;
        if (!draw && Double.isFinite(model.riverX)) {
            // Legacy: includeRiver true but no toggle method; draw it.
            draw = true;
        }
        if (!draw) return;

        double x = model.riverX;
        if (!Double.isFinite(x)) return;

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
     * Called on the EDT when progress arrives. We use this to refresh the cached best tour
     * snapshot so drawing doesn’t need to touch mutable simulation internals.
     */
    @Override
    protected void onSimulationProgress(edu.cnu.mdi.sim.SimulationContext ctx, edu.cnu.mdi.sim.ProgressInfo progress) {
        // Update best snapshot when it changes.
        TspSolution best = sim.getBestSolutionCopy();
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
}
