package edu.cnu.mdi.sim.demo.network;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;

/**
 * Force-directed "decluttering" simulation for a server/client network using normalized world coordinates.
 * <p>
 * The model lives in the unit square:
 * </p>
 * <ul>
 *   <li>{@code x in [0,1]}</li>
 *   <li>{@code y in [0,1]}</li>
 * </ul>
 * <p>
 * Rendering is responsible for mapping world coordinates into pixel coordinates. The view should also
 * compute and populate {@link NetworkModel.Node#worldRadius} for each node every frame (or whenever
 * the viewport size changes), so this simulation can:
 * </p>
 * <ul>
 *   <li>apply overlap-aware repulsion based on icon size</li>
 *   <li>clamp positions so icons remain fully visible inside the unit square</li>
 * </ul>
 *
 * <h2>Force model</h2>
 * <p>
 * Each step computes the net force on each node as the sum of:
 * </p>
 * <ol>
 *   <li>
 *     <b>Springs (edges)</b> for each client→server connection:
 *     {@code F = k * (r - r0) * rhat}
 *     <ul>
 *       <li>{@code r} is current distance between endpoints</li>
 *       <li>{@code r0} is the equilibrium distance</li>
 *       <li>{@code k} controls stiffness</li>
 *     </ul>
 *   </li>
 *   <li>
 *     <b>Repulsion (all pairs)</b> for node separation:
 *     approximately {@code F ~ repulsionC / (r^2 + eps)} along {@code rhat}.
 *     <p>
 *     Repulsion is increased if icons are close enough to overlap and may be further increased
 *     when one or both nodes are servers via {@link #serverRepulsion}.
 *     </p>
 *   </li>
 *   <li>
 *     <b>Centering</b> toward (0.5, 0.5):
 *     {@code F = -centerK * (x - 0.5, y - 0.5)}.
 *     <p>
 *     This prevents the common “everything evacuates to the boundary” artifact in layouts that
 *     combine repulsion with hard clamping. Set too high, it can also cause “all servers pile up
 *     in the center”, so keep it small.
 *     </p>
 *   </li>
 * </ol>
 *
 * <h2>Integration and convergence</h2>
 * <p>
 * Velocity is updated with damping:
 * </p>
 * <pre>
 * v <- damping * v + dt * F
 * x <- x + v
 * </pre>
 * <p>
 * A per-component velocity clamp ({@link #vmax}) limits occasional numeric spikes.
 * The simulation stops when either:
 * </p>
 * <ul>
 *   <li>cancel is requested,</li>
 *   <li>the average node speed falls below {@link #settleVel} after {@link #minSteps}, or</li>
 *   <li>{@link #maxSteps} is reached.</li>
 * </ul>
 *
 * <h2>Parameter tuning hints (unit-square world)</h2>
 * <ul>
 *   <li><b>Servers clump in the center</b>: increase {@link #serverRepulsion}, increase {@link #repulsionC},
 *       reduce {@link #centerK}, or reduce {@link #k}.</li>
 *   <li><b>Clients collapse onto servers</b>: increase {@link #r0} and/or {@link #repulsionC}.</li>
 *   <li><b>Layout “rings” the boundary</b>: increase {@link #centerK} slightly (but not too much) and/or
 *       reduce {@link #repulsionC}.</li>
 *   <li><b>Jitter/oscillation</b>: increase {@link #damping}, reduce {@link #k}, reduce {@link #vmax}.</li>
 * </ul>
 */
public final class NetworkDeclutterSimulation implements Simulation {

    /** Network model updated in-place by this simulation. */
    private final NetworkModel model;

    // -------------------------------------------------------------------------
    // Tunable physics parameters (unit-square world)
    // -------------------------------------------------------------------------

    /**
     * Spring constant (stiffness) used on each client→server edge.
     * <p>
     * Higher values pull connected endpoints toward the equilibrium distance faster, but can
     * introduce oscillation unless {@link #damping} is increased.
     * </p>
     */
    private double k = 1.45;

    /**
     * Equilibrium spring length in world units.
     * <p>
     * Larger {@code r0} encourages clients to sit farther from their server and generally
     * increases layout spread. Smaller values pull clients inward (tighter stars around servers).
     * </p>
     */
    private double r0 = 0.05;

    /**
     * Base repulsion strength applied to all node pairs.
     * <p>
     * Repulsion magnitude is approximately {@code repulsionC / (r^2 + eps)} along the line
     * connecting the nodes. Increasing this spreads nodes apart and reduces overlaps, but if
     * too large can push nodes toward the boundary and slow convergence.
     * </p>
     */
    private double repulsionC = 0.0001;

    /**
     * Repulsion multiplier used when a repulsion pair involves at least one server.
     * <p>
     * This is the simplest effective knob for preventing "all servers collapse into one knot".
     * Typical values:
     * </p>
     * <ul>
     *   <li>{@code 1.0}: no special server treatment</li>
     *   <li>{@code 3.0 .. 8.0}: recommended range for this demo</li>
     *   <li>{@code > 10}: servers may repel too strongly and dominate the layout</li>
     * </ul>
     * <p>
     * Implementation detail: if either node in a pair is a server, the repulsion strength
     * for that pair is multiplied by {@code serverRepulsion}.
     * </p>
     */
    private double serverRepulsion = 6.0;

    /**
     * Velocity damping factor applied each step (0..1).
     * <p>
     * Each step uses: {@code v <- damping * v + dt * F}. Values closer to 1.0 preserve
     * momentum longer (more oscillation); smaller values converge faster but can look “sticky”.
     * </p>
     */
    private double damping = 0.90;

    /**
     * Time step scaling applied to forces when updating velocity.
     * <p>
     * This acts like a global gain on all forces. Most demos leave this at 1.0 and tune
     * {@link #k} and {@link #repulsionC} instead.
     * </p>
     */
    private double dt = 1.0;

    /**
     * Weak centering ("gravity") strength toward the world center (0.5,0.5).
     * <p>
     * This helps avoid the layout evacuating to the unit-square boundary.
     * Keep this small; if too large it will pull everything into the middle and can
     * cause server clumping.
     * </p>
     */
    private double centerK = 0.15;

    /**
     * Softening constant for repulsion distance-squared computation.
     * <p>
     * Repulsion uses {@code r2 = dx^2 + dy^2 + eps}. This prevents division by zero and
     * reduces extreme impulses at very small separation.
     * <p>
     * If you see occasional “teleport” jumps or flashing long edges, increasing {@code eps}
     * is often the best first fix.
     * </p>
     */
    private double eps = 1.0e-4;

    /**
     * Extra spacing added on top of the sum of icon radii when determining “overlap proximity”.
     * <p>
     * If two nodes are closer than {@code a.worldRadius + b.worldRadius + overlapPad}, we
     * treat them as “overlapping” and increase repulsion by {@link #overlapBoost}.
     * </p>
     */
    private double overlapPad = 0.01;

    /**
     * Repulsion multiplier applied when nodes are within the overlap threshold.
     * <p>
     * This helps quickly resolve icon overlaps without needing a globally huge repulsion.
     * </p>
     */
    private double overlapBoost = 3.0;

    /**
     * Per-component velocity clamp (world units per step).
     * <p>
     * Caps both {@code vx} and {@code vy} to {@code [-vmax, +vmax]}. This prevents occasional
     * extreme forces from moving nodes large distances in a single step.
     * </p>
     */
    private double vmax = 0.012;

    // -------------------------------------------------------------------------
    // Termination / convergence parameters
    // -------------------------------------------------------------------------

    /**
     * Minimum number of steps to run before allowing convergence to stop the simulation.
     * <p>
     * Prevents premature termination early in the relaxation.
     * </p>
     */
    private int minSteps = 250;

    /**
     * Hard stop after this many steps.
     * <p>
     * Protects against parameter combinations that converge very slowly or not at all.
     * </p>
     */
    private int maxSteps = 8000;

    /**
     * Average node speed threshold for declaring the layout "settled".
     * <p>
     * After {@link #minSteps}, if the mean speed (averaged across all nodes) falls below
     * this value, the simulation stops.
     * </p>
     */
    private double settleVel = r0/3.3;

    // -------------------------------------------------------------------------
    // Bookkeeping / framework integration
    // -------------------------------------------------------------------------

    /** Current step index. */
    private int step;

    /** True if {@link #cancel(SimulationContext)} has been called. */
    private volatile boolean canceled;

    /** Optional engine used for posting progress/messages/refresh. */
    private SimulationEngine engine;

    /**
     * Create a decluttering simulation for the given model.
     *
     * @param model the world network model to update in-place (non-null)
     * @throws IllegalArgumentException if {@code model} is null
     */
    public NetworkDeclutterSimulation(NetworkModel model) {
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        this.model = model;
    }

    /**
     * Get the model being updated by this simulation.
     * <p>
     * Views typically use this to render nodes/edges.
     * </p>
     *
     * @return the model (never null)
     */
    public NetworkModel getModel() {
        return model;
    }

    /**
     * Attach the engine so this simulation can post messages/progress/refresh.
     * <p>
     * This is optional. If unset, the simulation still runs but does not emit UI hints
     * beyond what the engine itself already provides.
     * </p>
     *
     * @param engine engine executing this simulation (may be null)
     */
    public void setEngine(SimulationEngine engine) {
        this.engine = engine;
    }

    @Override
    public void init(SimulationContext ctx) {
        step = 0;
        canceled = false;

        if (engine != null) {
            engine.postMessage("Network generated. Relaxing layout…");
            engine.postProgress(ProgressInfo.indeterminate("Relaxing…"));
            engine.requestRefresh();
        }
    }

    @Override
    public boolean step(SimulationContext ctx) {
        if (canceled || ctx.isCancelRequested()) {
            return false;
        }

        step++;

        int n = model.nodes.size();
        double[] fx = new double[n];
        double[] fy = new double[n];

        // 1) Springs on edges
        for (NetworkModel.Edge e : model.edges) {
            var c = model.nodes.get(e.clientIndex);
            var s = model.nodes.get(e.serverIndex);

            double dx = s.x - c.x;
            double dy = s.y - c.y;
            double r = Math.sqrt(dx * dx + dy * dy) + 1e-12;

            double f = k * (r - r0);
            double ux = dx / r;
            double uy = dy / r;

            fx[e.clientIndex] += f * ux;
            fy[e.clientIndex] += f * uy;

            fx[e.serverIndex] -= f * ux;
            fy[e.serverIndex] -= f * uy;
        }

        // 2) Repulsion (all pairs)
        for (int i = 0; i < n; i++) {
            var a = model.nodes.get(i);
            for (int j = i + 1; j < n; j++) {
                var b = model.nodes.get(j);

                double dx = a.x - b.x;
                double dy = a.y - b.y;

                double r2 = dx * dx + dy * dy + eps;
                double r = Math.sqrt(r2);

                // Base repulsion strength
                double strength = repulsionC;

                // If either node is a server, boost repulsion for this pair.
                if (a.type == NetworkModel.NodeType.SERVER || b.type == NetworkModel.NodeType.SERVER) {
                    strength *= serverRepulsion;
                }

                // Overlap-aware boost when icons are too close
                double minDist = a.worldRadius + b.worldRadius + overlapPad;
                double minDist2 = minDist * minDist;
                if (r2 < minDist2) {
                    strength *= overlapBoost;
                }

                double inv = strength / r2;
                double ux = dx / r;
                double uy = dy / r;

                fx[i] += inv * ux;
                fy[i] += inv * uy;
                fx[j] -= inv * ux;
                fy[j] -= inv * uy;
            }
        }

        // 3) Weak centering toward the middle
        for (int i = 0; i < n; i++) {
            var node = model.nodes.get(i);
            fx[i] += -centerK * (node.x - 0.5);
            fy[i] += -centerK * (node.y - 0.5);
        }

        // 4) Integrate + damping + clamp to unit-square
        double speedSum = 0.0;

        for (int i = 0; i < n; i++) {
            var node = model.nodes.get(i);

            node.vx = damping * node.vx + dt * fx[i];
            node.vy = damping * node.vy + dt * fy[i];

            node.vx = Math.max(-vmax, Math.min(vmax, node.vx));
            node.vy = Math.max(-vmax, Math.min(vmax, node.vy));

            node.x += node.vx;
            node.y += node.vy;

            double rad = Math.max(0.0, node.worldRadius);
            node.x = Math.max(rad, Math.min(1.0 - rad, node.x));
            node.y = Math.max(rad, Math.min(1.0 - rad, node.y));

            speedSum += Math.sqrt(node.vx * node.vx + node.vy * node.vy);
        }

        double avgSpeed = speedSum / Math.max(1, n);

        if (engine != null && (step % 10 == 0)) {
            double frac = 1.0 - Math.min(1.0, avgSpeed / 0.01);
            engine.postProgress(ProgressInfo.determinate(frac, "Relaxing… step " + step));
        }

        if (engine != null && (step % 2 == 0)) {
            engine.requestRefresh();
        }

        if (step >= minSteps && avgSpeed < settleVel) {
            if (engine != null) {
                engine.postMessage("Settled.");
                engine.postProgress(ProgressInfo.determinate(1.0, "Settled"));
                engine.requestRefresh();
            }
            return false;
        }

        return step < maxSteps;
    }

    @Override
    public void cancel(SimulationContext ctx) {
        canceled = true;
        if (engine != null) {
            engine.postMessage("Cancel requested.");
            engine.postProgress(ProgressInfo.indeterminate("Canceling…"));
        }
    }
}
