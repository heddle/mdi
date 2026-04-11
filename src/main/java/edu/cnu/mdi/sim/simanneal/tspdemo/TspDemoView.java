package edu.cnu.mdi.sim.simanneal.tspdemo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.Random;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.sim.SimulationEngine;
import edu.cnu.mdi.sim.SimulationEngineConfig;
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
 * Traveling Salesperson Problem (TSP) simulated annealing demo, hosted in an
 * MDI {@link SimulationView}.
 *
 * <h2>What this demo shows</h2>
 * <p>
 * The view displays a set of randomly placed cities connected by their current
 * best tour (a closed Hamiltonian cycle). As the annealer runs, the tour
 * progressively untangles from its initial random spaghetti into a
 * near-optimal route. The optional "river" (a vertical blue line) adds a
 * configurable penalty or bonus for each edge that crosses it, changing the
 * shape of the optimal solution.
 * </p>
 * <p>
 * An Energy vs. Temperature scatter plot in the right-hand diagnostics panel
 * shows accepted moves as a gray point cloud and best solutions as connected
 * red squares. Together these reveal the Metropolis acceptance criterion
 * operating across the full temperature range.
 * </p>
 *
 * <h2>Layout</h2>
 * <p>
 * The view uses the standard {@link SimulationView} diagnostics split:
 * </p>
 * <ul>
 *   <li>Left (60%): world-coordinate canvas showing cities and tour.</li>
 *   <li>Right (40%): {@link EvsTPlotPanel} (E vs T scatter plot).</li>
 *   <li>South: {@link TspDemoControlPanel} with media buttons, city/penalty
 *       sliders, and a Reset button.</li>
 * </ul>
 *
 * <h2>Threading model</h2>
 * <p>
 * The annealing algorithm runs on the simulation thread. The view receives
 * two kinds of callbacks on the EDT:
 * </p>
 * <ul>
 *   <li>{@link #onSimulationProgress} — updates {@link #bestTourSnapshot} from
 *       the simulation's best solution, throttled by
 *       {@link SimulatedAnnealingConfig#progressEverySteps()}.</li>
 *   <li>{@link #newBest} ({@link IAcceptedMoveListener}) — updates
 *       {@link #bestTourSnapshot} immediately when a new best is found,
 *       giving more responsive visual feedback during the early fast-improving
 *       phase of annealing. The volatile write to {@link #bestTourSnapshot} is
 *       safe because it is a single reference assignment.</li>
 * </ul>
 * <p>
 * The before/after draw hooks run on the EDT during each repaint and read only
 * the volatile {@link #model} and {@link #bestTourSnapshot} snapshots — they
 * never touch mutable simulation internals directly.
 * </p>
 *
 * <h2>Construction pattern</h2>
 * <p>
 * Because {@code super(...)} must be the first statement and the
 * {@link SimulationEngine} holds its {@link edu.cnu.mdi.sim.Simulation} as a
 * {@code final} field, the simulation and model must be created before
 * {@code super()} is called. A {@link ThreadLocal} ({@link #BUNDLE_TL}) is
 * used to smuggle the newly created {@link TspModel} across the
 * {@code super()} boundary so the constructor can retrieve it immediately
 * after.
 * </p>
 *
 * <h2>Reset</h2>
 * <p>
 * Reset is handled by {@link SimulationView#requestEngineReset}, which stops
 * the current engine (if running), builds a new simulation via the supplied
 * factory, swaps it in, and restarts. The after-swap hook detaches the
 * {@link IAcceptedMoveListener} from the old simulation and attaches it to the
 * new one, ensuring the E vs T plot continues to receive data correctly.
 * </p>
 */
@SuppressWarnings("serial")
public class TspDemoView extends SimulationView implements ITspDemoResettable, IAcceptedMoveListener {

    /** Default number of cities for a new random problem. */
    public static final int DEFAULT_NUM_CITY = 100;

    /** Default river crossing penalty (positive = penalty, negative = bonus). */
    public static final float DEFAULT_RIVER_PENALTY = 0.35f;

    /**
     * Thread-local used to pass the newly created {@link TspModel} across the
     * {@code super(...)} boundary.
     * <p>
     * {@link #createSimulationAndStashBundle} stores the model here before
     * returning. The constructor retrieves and removes the value immediately
     * after {@code super()} returns. The value is always removed — either by
     * the constructor or by the reset after-swap hook — so the thread-local
     * never retains a stale reference.
     * </p>
     */
    private static final ThreadLocal<Bundle> BUNDLE_TL = new ThreadLocal<>();

    /**
     * The current TSP model (city positions and river configuration).
     * <p>
     * Volatile because it is written on the EDT (during reset) and read on the
     * EDT (during painting). The simulation thread reads it only indirectly
     * through {@link TspAnnealingProblem}, which holds its own reference.
     * </p>
     */
    private volatile TspModel model;

    /**
     * The current SA simulation instance.
     * <p>
     * Volatile because it is written on the EDT during reset and read on both
     * the EDT (in drawing and listener callbacks) and the simulation thread
     * (indirectly through the engine).
     * </p>
     */
    private volatile SimulatedAnnealingSimulation<TspSolution> sim;

    /**
     * Cached best tour snapshot used exclusively by the drawing hooks.
     * <p>
     * Written by {@link #newBest} and {@link #onSimulationProgress} on the
     * simulation thread and EDT respectively; read by the before-draw hook on
     * the EDT during each repaint. The volatile modifier ensures the EDT always
     * sees the most recently written array reference. Array contents are not
     * modified after the snapshot is taken ({@code tour.clone()}).
     * </p>
     */
    private volatile int[] bestTourSnapshot;

    /** Radius in pixels for the filled circle drawn at each city. */
    private final int cityRadiusPx = 4;

    /**
     * RNG seed for model generation.
     * <p>
     * {@code 0L} means non-deterministic (a new random seed each run).
     * Set to a non-zero value to make runs reproducible for testing.
     * </p>
     */
    private static final long seed = 0L;

    /**
     * The Energy vs. Temperature scatter plot installed as the right-side
     * diagnostics panel. Receives accepted-move and new-best notifications
     * via {@link IAcceptedMoveListener}.
     */
    private EvsTPlotPanel evtPlot;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Create the TSP demo view.
     *
     * <p>
     * The constructor calls {@link #createSimulationAndStashBundle} to build
     * the initial simulation and stash the model in {@link #BUNDLE_TL}, then
     * passes the simulation to {@code super(...)}. After {@code super()} returns
     * it recovers the model from the thread-local and wires up the remaining
     * references.
     * </p>
     *
     * @param keyVals standard {@link edu.cnu.mdi.view.BaseView} key-value args
     */
    public TspDemoView(Object... keyVals) {
        super(
            createSimulationAndStashBundle(DEFAULT_NUM_CITY, DEFAULT_RIVER_PENALTY, seed),
            new SimulationEngineConfig(
                    33,    // refreshIntervalMs  ~30 Hz
                    250,   // progressIntervalMs ~4 Hz
                    30,    // cooperativeYieldMs
                    false  // autoRun
            ),
            true,
            (SimulationView.ControlPanelFactory) TspDemoControlPanel::new,
            true,                        // include diagnostics panel
            TspDemoView::createScatterPanel,
            0.6,                         // main panel gets 60% of width
            keyVals
        );

        this.evtPlot = (EvsTPlotPanel) getDiagnosticsComponent();

        // Recover bundle created during createSimulationAndStashBundle().
        Bundle b = BUNDLE_TL.get();
        BUNDLE_TL.remove();

        this.model = b.model;

        @SuppressWarnings("unchecked")
        SimulatedAnnealingSimulation<TspSolution> s =
                (SimulatedAnnealingSimulation<TspSolution>) getSimulationEngine().getSimulation();
        this.sim = s;
        this.sim.addAcceptedMoveListener(this);
        this.sim.setEngine(getSimulationEngine());

        setBeforeDraw();
        setAfterDraw();

        if (getIContainer() != null) {
            getIContainer().scale(1.25);
        }

        pack();
        startSimulation();
    }

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    /**
     * Create the E vs T diagnostics panel with a reversed x-axis so that
     * temperature decreases left-to-right (high T on the left, low T on
     * the right), matching the natural reading direction of the annealing
     * process.
     *
     * @return configured {@link EvsTPlotPanel}
     */
    private static EvsTPlotPanel createScatterPanel() {
        EvsTPlotPanel panel =
                new EvsTPlotPanel("Traveling Salesperson Problem", "temperature", "energy");
        panel.getPlotCanvas().getParameters().setReverseXaxis(true);
        return panel;
    }

    /**
     * Build a new {@link SimulatedAnnealingSimulation} for the given parameters
     * and stash the associated {@link TspModel} in {@link #BUNDLE_TL} so the
     * constructor (or reset after-swap hook) can retrieve it.
     *
     * <p>
     * This static method is used both during initial construction (called from
     * within {@code super(...)}) and during reset (called from the supplier
     * lambda in {@link #requestReset}). In both cases the caller removes the
     * thread-local value immediately after retrieving it.
     * </p>
     *
     * @param cityCount    number of cities to generate
     * @param riverPenalty penalty (positive) or bonus (negative) for crossing
     *                     the river; range {@code [-1, +1]}
     * @param seed         RNG seed; {@code 0L} for non-deterministic
     * @return a fully configured simulation (not yet started)
     */
    private static SimulatedAnnealingSimulation<TspSolution> createSimulationAndStashBundle(
            int cityCount, double riverPenalty, long seed) {

        Random rng = (seed == 0L) ? new Random() : new Random(seed);

        TspModel model = new TspModel(cityCount, true, riverPenalty, rng);
        TspAnnealingProblem problem = new TspAnnealingProblem(model);
        SimulatedAnnealingConfig cfg = SimulatedAnnealingConfig.defaults();
        AnnealingSchedule schedule = new GeometricAnnealingSchedule();
        TemperatureHeuristic<TspSolution> heuristic =
                new EnergyDistributionHeuristic<>(300, 0.80, 1e-6);

        SimulatedAnnealingSimulation<TspSolution> sim =
                new SimulatedAnnealingSimulation<>(problem, cfg, schedule, heuristic);

        BUNDLE_TL.set(new Bundle(model));
        return sim;
    }

    // -------------------------------------------------------------------------
    // Reset (ITspDemoResettable)
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>
     * Clears the E vs T plot immediately (before the engine stops, so stale
     * data disappears at once), then delegates to
     * {@link SimulationView#requestEngineReset}. The after-swap hook:
     * </p>
     * <ol>
     *   <li>Recovers the new model from {@link #BUNDLE_TL}.</li>
     *   <li>Moves the {@link IAcceptedMoveListener} from the old simulation to
     *       the new one.</li>
     *   <li>Updates {@link #model}, {@link #sim}, and
     *       {@link #bestTourSnapshot}.</li>
     *   <li>Injects the new engine into the new simulation.</li>
     * </ol>
     */
    @Override
    public void requestReset(int cityCount, double riverPenalty) {
        evtPlot.clearData();

        final SimulatedAnnealingSimulation<TspSolution> oldSim = this.sim;

        requestEngineReset(
            () -> createSimulationAndStashBundle(cityCount, riverPenalty, seed),

            (SimulationEngine newEngine) -> {
                Bundle b = BUNDLE_TL.get();
                BUNDLE_TL.remove();

                @SuppressWarnings("unchecked")
                SimulatedAnnealingSimulation<TspSolution> newSim =
                        (SimulatedAnnealingSimulation<TspSolution>) newEngine.getSimulation();

                if (oldSim != null) {
                    oldSim.removeAcceptedMoveListener(this);
                }
                newSim.addAcceptedMoveListener(this);

                this.model            = b.model;
                this.sim              = newSim;
                this.bestTourSnapshot = null;
                this.sim.setEngine(newEngine);
            },

            true,   // autoStart
            true    // refresh immediately
        );
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    /**
     * Install the before-draw hook that renders the river and the current best
     * tour onto the world canvas.
     *
     * <p>
     * The river is drawn first (behind the tour). The tour is read from
     * {@link #bestTourSnapshot} — a volatile reference that is updated by
     * {@link #newBest} and {@link #onSimulationProgress} and is therefore
     * always a recent copy of the best solution found so far.
     * </p>
     */
    private void setBeforeDraw() {
        getIContainer().setBeforeDraw(new DrawableAdapter() {
            @Override
            public void draw(Graphics2D g2, IContainer container) {
                if (container == null) return;

                TspModel m = model;
                if (m == null) return;

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                drawRiverIfEnabled(g2, container, m);

                int[] tour = bestTourSnapshot;
                if (tour == null || tour.length < 2) {
                    // Seed the snapshot on the very first paint.
                    SimulatedAnnealingSimulation<TspSolution> s = sim;
                    if (s != null) {
                        TspSolution best = s.getBestSolutionCopy();
                        if (best != null && best.tour != null) {
                            bestTourSnapshot = best.tour.clone();
                            tour = bestTourSnapshot;
                        }
                    }
                }
                if (tour == null || tour.length < 2) return;

                g2.setColor(Color.RED.darker());
                g2.setStroke(new BasicStroke(2f));

                Point p0 = new Point();
                Point p1 = new Point();

                for (int i = 0; i < tour.length; i++) {
                    int a = tour[i];
                    int b = tour[(i + 1) % tour.length];
                    container.worldToLocal(p0, m.cities[a].x, m.cities[a].y);
                    container.worldToLocal(p1, m.cities[b].x, m.cities[b].y);
                    g2.drawLine(p0.x, p0.y, p1.x, p1.y);
                }
            }
        });
    }

    /**
     * Install the after-draw hook that renders a filled circle at each city
     * position, on top of the tour lines.
     */
    private void setAfterDraw() {
        getIContainer().setAfterDraw(new DrawableAdapter() {
            @Override
            public void draw(Graphics2D g2, IContainer container) {
                if (container == null) return;

                TspModel m = model;
                if (m == null) return;

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(Color.BLACK);
                Point p = new Point();
                for (var c : m.cities) {
                    container.worldToLocal(p, c.x, c.y);
                    g2.fillOval(p.x - cityRadiusPx, p.y - cityRadiusPx,
                                2 * cityRadiusPx, 2 * cityRadiusPx);
                }
            }
        });
    }

    /**
     * Draw the river as a vertical blue line if it is present and enabled.
     *
     * @param g2        the graphics context
     * @param container the world container (for coordinate mapping)
     * @param m         volatile model snapshot
     */
    private void drawRiverIfEnabled(Graphics2D g2, IContainer container, TspModel m) {
        if (!m.includeRiver || !m.isRiverEnabled() || !Double.isFinite(m.riverX)) return;

        g2.setColor(new Color(40, 90, 200));
        g2.setStroke(new BasicStroke(1.5f));

        Point p0 = new Point();
        Point p1 = new Point();
        container.worldToLocal(p0, m.riverX, 0.0);
        container.worldToLocal(p1, m.riverX, 1.0);
        g2.drawLine(p0.x, p0.y, p1.x, p1.y);
    }

    // -------------------------------------------------------------------------
    // SimulationView hooks
    // -------------------------------------------------------------------------

    /**
     * Refresh the best tour snapshot from the simulation's current best
     * solution.
     *
     * <p>
     * Called on the EDT at the rate set by
     * {@link SimulatedAnnealingConfig#progressEverySteps()}, providing a
     * backstop refresh in case {@link #newBest} misses a repaint. This also
     * handles the initial state where {@link #bestTourSnapshot} is null —
     * the first progress callback will always populate it.
     * </p>
     *
     * @param ctx      simulation context
     * @param progress current progress info
     */
    @Override
    protected void onSimulationProgress(
            edu.cnu.mdi.sim.SimulationContext ctx,
            edu.cnu.mdi.sim.ProgressInfo progress) {

        SimulatedAnnealingSimulation<TspSolution> s = sim;
        if (s == null) return;

        TspSolution best = s.getBestSolutionCopy();
        if (best != null && best.tour != null) {
            bestTourSnapshot = best.tour.clone();
        }
    }

    // -------------------------------------------------------------------------
    // IAcceptedMoveListener
    // -------------------------------------------------------------------------

    /**
     * Called on the simulation thread each time any move is accepted
     * (including uphill moves).
     * <p>
     * Forwards the (temperature, energy) point to the E vs T scatter plot as
     * a gray accepted-move point.
     * </p>
     *
     * @param temperature current annealing temperature
     * @param energy      current solution energy (may be higher than best)
     */
    @Override
    public void acceptedMove(double temperature, double energy) {
        evtPlot.addAccepted(temperature, energy);
    }

    /**
     * Called on the simulation thread each time a new best solution is found.
     *
     * <p>
     * Two things happen here:
     * </p>
     * <ol>
     *   <li>The best tour snapshot is updated immediately via a volatile
     *       reference write, so the next repaint shows the improved tour
     *       without waiting for the next progress callback. This gives
     *       responsive visual feedback during the early fast-improving phase
     *       of annealing when new bests arrive frequently.</li>
     *   <li>The (temperature, energy) point is added to the E vs T plot as a
     *       red best-solution square.</li>
     * </ol>
     *
     * @param temperature temperature at which the new best was found
     * @param energy      energy of the new best solution
     */
    @Override
    public void newBest(double temperature, double energy) {
        TspSolution best = sim.getBestSolutionCopy();
        if (best != null && best.tour != null) {
            bestTourSnapshot = best.tour.clone();
        }
        evtPlot.addBest(temperature, energy);
    }

    // -------------------------------------------------------------------------
    // Internal types
    // -------------------------------------------------------------------------

    /**
     * Carrier for objects that must cross the {@code super(...)} boundary.
     * Created by {@link #createSimulationAndStashBundle} and consumed by the
     * constructor or the reset after-swap hook.
     */
    private static final class Bundle {
        final TspModel model;

        Bundle(TspModel model) {
            this.model = model;
        }
    }
}