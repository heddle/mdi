package edu.cnu.mdi.sim.demo.network;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;

/**
 * Force-directed "decluttering" simulation for a server/client network using
 * normalized world coordinates.
 * <p>
 * The model lives in the unit square:
 * </p>
 * <ul>
 * <li>{@code x in [0,1]}</li>
 * <li>{@code y in [0,1]}</li>
 * </ul>
 * <p>
 * Rendering is responsible for mapping world coordinates into pixel
 * coordinates. The view should also compute and populate
 * NetworkModel.Node.worldRadius for each node every frame (or whenever
 * the viewport size changes), so this simulation can:
 * </p>
 * <ul>
 * <li>apply overlap-aware repulsion based on icon size</li>
 * <li>clamp positions so icons remain fully visible inside the unit square</li>
 * </ul>
 *
 * <h2>Force model</h2>
 * <p>
 * Each step computes the net force on each node as the sum of:
 * </p>
 * <ol>
 * <li><b>Springs (edges)</b> for each client→server connection:
 * {@code F = k * (r - r0) * rhat}
 * <ul>
 * <li>{@code r} is current distance between endpoints</li>
 * <li>{@code r0} is the equilibrium distance</li>
 * <li>{@code k} controls stiffness</li>
 * </ul>
 * </li>
 * <li><b>Repulsion (all pairs)</b> for node separation: approximately
 * {@code F ~ repulsionC / (r^2 + eps)} along {@code rhat}.
 * <p>
 * Repulsion is increased if icons are close enough to overlap and may be
 * further increased when one or both nodes are servers via
 * {@link #serverRepulsion}.
 * </p>
 * </li>
 * <li><b>Centering</b> toward (0.5, 0.5):
 * {@code F = -centerK * (x - 0.5, y - 0.5)}.
 * <p>
 * This prevents the common “everything evacuates to the boundary” artifact in
 * layouts that combine repulsion with hard clamping. Set too high, it can also
 * cause “all servers pile up in the center”, so keep it small.
 * </p>
 * </li>
 * </ol>
 *
 * <h2>Integration and convergence</h2>
 * <p>
 * Velocity is updated with damping:
 * </p>
 *
 * <pre>
 * v from damping * v + dt * F
 * x from x + v
 * </pre>
 * <p>
 * A per-component velocity clamp ({@link #vmax}) limits occasional numeric
 * spikes. The simulation stops when either:
 * </p>
 * <ul>
 * <li>cancel is requested,</li>
 * <li>the average node speed falls below {@link #settleVel} after
 * {@link #minSteps}, or</li>
 * <li>{@link #maxSteps} is reached.</li>
 * </ul>
 *
 * <h2>Parameter tuning hints (unit-square world)</h2>
 * <ul>
 * <li><b>Servers clump in the center</b>: increase {@link #serverRepulsion},
 * increase {@link #repulsionC}, reduce {@link #centerK}, or reduce
 * {@link #k}.</li>
 * <li><b>Clients collapse onto servers</b>: increase {@link #r0} and/or
 * {@link #repulsionC}.</li>
 * <li><b>Layout “rings” the boundary</b>: increase {@link #centerK} slightly
 * (but not too much) and/or reduce {@link #repulsionC}.</li>
 * <li><b>Jitter/oscillation</b>: increase {@link #damping}, reduce {@link #k},
 * reduce {@link #vmax}.</li>
 * </ul>
 */
public final class NetworkDeclutterSimulation implements Simulation {

	private static final double REPULSION_EPS = 1.0e-4;
	private static final double OVERLAP_PAD = 0.01;
	private static final double PRINTER_RBOOST = 0.8;

	/** Network model updated in-place by this simulation. */
	private final NetworkModel model;

	// -------------------------------------------------------------------------
	// Tunable physics parameters (unit-square world)
	// -------------------------------------------------------------------------

	/**
	 * Spring constant (stiffness) used on each client→server edge.
	 * <p>
	 * Higher values pull connected endpoints toward the equilibrium distance
	 * faster, but can introduce oscillation unless {@link #damping} is increased.
	 * </p>
	 */
	private double k = 1.45;

	/**
	 * Equilibrium spring length in world units.
	 * <p>
	 * Larger {@code r0} encourages clients to sit farther from their server and
	 * generally increases layout spread. Smaller values pull clients inward
	 * (tighter stars around servers).
	 * </p>
	 */
	private double r0 = 0.05;

	/**
	 * Base repulsion strength applied to all node pairs.
	 * <p>
	 * Repulsion magnitude is approximately {@code repulsionC / (r^2 + eps)} along
	 * the line connecting the nodes. Increasing this spreads nodes apart and
	 * reduces overlaps, but if too large can push nodes toward the boundary and
	 * slow convergence.
	 * </p>
	 */
	private double repulsionC = 0.0001;

	/**
	 * Repulsion multiplier used when a repulsion pair involves at least one server.
	 * <p>
	 * This is the simplest effective knob for preventing "all servers collapse into
	 * one knot". Typical values:
	 * </p>
	 * <ul>
	 * <li>{@code 1.0}: no special server treatment</li>
	 * <li>{@code 3.0 .. 8.0}: recommended range for this demo</li>
	 * <li>{@code > 10}: servers may repel too strongly and dominate the layout</li>
	 * </ul>
	 * <p>
	 * Implementation detail: if either node in a pair is a server, the repulsion
	 * strength for that pair is multiplied by {@code serverRepulsion}.
	 * </p>
	 */
	private double serverRepulsion = 6.0;

	/**
	 * Velocity damping factor applied each step (0..1).
	 * <p>
	 * Each step uses: {@code v <- damping * v + dt * F}. Values closer to 1.0
	 * preserve momentum longer (more oscillation); smaller values converge faster
	 * but can look “sticky”.
	 * </p>
	 */
	private double damping = 0.90;

    /**
	 * Time step scaling applied to forces when updating velocity.
	 * <p>
	 * This acts like a global gain on all forces. Most demos leave this at 1.0 and
	 * tune {@link #k} and {@link #repulsionC} instead.
	 * </p>
	 */
	private double dt = 0.1;

	/**
	 * Weak centering ("gravity") strength toward the world center (0.5,0.5).
	 * <p>
	 * This helps avoid the layout evacuating to the unit-square boundary. Keep this
	 * small; if too large it will pull everything into the middle and can cause
	 * server clumping.
	 * </p>
	 */
	private double centerK = 0.15;

    /**
	 * Repulsion multiplier applied when nodes are within the overlap threshold.
	 * <p>
	 * This helps quickly resolve icon overlaps without needing a globally huge
	 * repulsion.
	 * </p>
	 */
	private double overlapBoost = 3.0;

    /**
	 * Printer-specific multiplier applied to the spring constant.
	 *
	 * <p>Springs involving printers use {@code k * printerKBoost} as their
	 * effective stiffness, making printer connections stiffer and helping them
	 * resist being pulled around by other forces.</p>
	 */
	private double printerKBoost = 1.2;

	/**
	 * Return the printer spring-constant multiplier.
	 *
	 * @return the current {@code printerKBoost} value
	 */
	public double getPrinterKBoost() {
	    return printerKBoost;
	}

	/**
	 * Set the printer spring-constant multiplier.
	 *
	 * @param printerKBoost the new multiplier; values less than 1.0 make
	 *                      printer springs softer than the base constant,
	 *                      values greater than 1.0 make them stiffer
	 */
	public void setPrinterKBoost(double printerKBoost) {
	    this.printerKBoost = printerKBoost;
	}

	/**
	 * Per-component velocity clamp (world units per step).
	 * <p>
	 * Caps both {@code vx} and {@code vy} to {@code [-vmax, +vmax]}. This prevents
	 * occasional extreme forces from moving nodes large distances in a single step.
	 * </p>
	 */
	private double vmax = 0.012;

	// -------------------------------------------------------------------------
	// Termination / convergence parameters
	// -------------------------------------------------------------------------

	/**
	 * Minimum number of steps to run before allowing convergence to stop the
	 * simulation.
	 * <p>
	 * Prevents premature termination early in the relaxation.
	 * </p>
	 */
	private int minSteps = 250;

	/**
	 * Hard stop after this many steps.
	 * <p>
	 * Protects against parameter combinations that converge very slowly or not at
	 * all.
	 * </p>
	 */
	private int maxSteps = 2000;


	/**
	 * Force threshold for declaring the layout "settled".
	 */
	private double settleForce = 0.010;

	// -------------------------------------------------------------------------
	// Bookkeeping / framework integration
	// -------------------------------------------------------------------------

	/** Current step index. */
	private int step;

	/**
	 * Optional engine handle used to post progress/messages/refresh to the EDT.
	 * Set via {@link #setEngine(SimulationEngine)}. May be {@code null} if the
	 * caller does not need UI hints beyond what the engine itself provides.
	 */
	private SimulationEngine engine;

	/**
	 * Per-step diagnostic samples produced during the simulation and drained by
	 * the view on each refresh callback.
	 *
	 * <p>A {@link java.util.concurrent.ConcurrentLinkedQueue} is used because
	 * the simulation thread enqueues samples and the EDT drains them inside
	 * {@code onRefresh} — a lock-free queue avoids any coordination between
	 * the two threads.</p>
	 */
	private final java.util.concurrent.ConcurrentLinkedQueue<Diagnostics> diagnosticsSamples =
	        new java.util.concurrent.ConcurrentLinkedQueue<>();


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
	 * This is optional. If unset, the simulation still runs but does not emit UI
	 * hints beyond what the engine itself already provides.
	 * </p>
	 *
	 * @param engine engine executing this simulation (may be null)
	 */
	public void setEngine(SimulationEngine engine) {
		this.engine = engine;
	}

	/**
	 * Initialize the simulation: reset the step counter and post an initial
	 * refresh so the user sees the starting layout before it starts moving.
	 *
	 * @param ctx the simulation context (not used directly, but required by
	 *            the {@link Simulation} contract)
	 * @throws Exception not thrown by this implementation
	 */
	@Override
	public void init(SimulationContext ctx) throws Exception {
		step = 0;

		if (engine != null) {
			engine.postMessage("Network generated. Relaxing layout…");
			engine.postProgress(ProgressInfo.indeterminate("Relaxing…"));
			engine.requestRefresh();
		}
	}

	@Override
	public boolean step(SimulationContext ctx) {

	    if (ctx.isCancelRequested()) {
	        return false;
	    }

	    step++;

	    // zero out forces
	    for (Node node : model.nodes) {
	        node.fx = 0.0;
	        node.fy = 0.0;
	    }

	    // 1) Springs on edges
	    for (NetworkModel.Edge e : model.edges) {
	        var node1 = e.node1;
	        var node2 = e.node2;

	        double dx = node2.x - node1.x;
	        double dy = node2.y - node1.y;
	        double r = Math.sqrt(dx * dx + dy * dy) + 1e-12;

	        double rboost = 1.0;
	        double kboost = 1.0;

	        // boosts if one node is a printer
	        if (node1.type == Node.NodeType.PRINTER || node2.type == Node.NodeType.PRINTER) {
	            // Printer-specific reduction to equilibrium spring length.
	            rboost = PRINTER_RBOOST;
	            kboost = printerKBoost;
	        }

	        // f>0 pulls endpoints together;
	        //f<0 pushes apart (rest length rboost*r0)
	        double f = kboost * k * (r - rboost * r0);
	        double ux = dx / r;
	        double uy = dy / r;

	        node1.fx += f * ux;
	        node1.fy += f * uy;

	        node2.fx -= f * ux;
	        node2.fy -= f * uy;
	    }

	    // 2) Repulsion (all pairs)
	    int n = model.nodes.size();
	    for (int i = 0; i < n; i++) {
	        var node1 = model.nodes.get(i);
	        for (int j = i + 1; j < n; j++) {
	            var node2 = model.nodes.get(j);

	            double dx = node1.x - node2.x;
	            double dy = node1.y - node2.y;

	            // Softening constant for repulsion distance-squared computation.
	            double r2 = dx * dx + dy * dy + REPULSION_EPS;
	            double r = Math.sqrt(r2);

	            // Base repulsion strength
	            double strength = repulsionC;

	            // If either node is a server, boost repulsion for this pair.
	            if (node1.type == Node.NodeType.SERVER || node2.type == Node.NodeType.SERVER) {
	                strength *= serverRepulsion;
	            }

	            // Overlap-aware boost when icons are too close
	            double minDist = node1.worldRadius + node2.worldRadius + OVERLAP_PAD;
	            double minDist2 = minDist * minDist;
	            if (r2 < minDist2) {
	                strength *= overlapBoost;
	            }

	            double inv = strength / r2;
	            double ux = dx / r;
	            double uy = dy / r;

	            node1.fx += inv * ux;
	            node1.fy += inv * uy;
	            node2.fx -= inv * ux;
	            node2.fy -= inv * uy;
	        }
	    }

	    // 3) Weak centering toward the middle
	    for (int i = 0; i < n; i++) {
	        var node = model.nodes.get(i);
	        node.fx += -centerK * (node.x - 0.5);
	        node.fy += -centerK * (node.y - 0.5);
	    }

	    //compute rms force
	    double f2sum = 0.0;
	    for (var node : model.nodes) {
	        f2sum += node.fx * node.fx + node.fy * node.fy;
	    }
	    double FrmsNow = Math.sqrt(f2sum / Math.max(1, n));

	 // 4) Integrate + damping + clamp to unit-square
	    double speedSum = 0.0;
	    int vmaxHits = 0;

	    for (int i = 0; i < n; i++) {
	        var node = model.nodes.get(i);

	        // Velocity update with damping
	        node.vx = damping * node.vx + dt * node.fx;
	        node.vy = damping * node.vy + dt * node.fy;

	        // Clamp by magnitude (speed), not per-component.
	        // This avoids the artificial ~sqrt(2)*vmax speed floor you were seeing.
	        double v2 = node.vx * node.vx + node.vy * node.vy;
	        double vmax2 = vmax * vmax;
	        if (v2 > vmax2) {
	            double v = Math.sqrt(v2);
	            double s = vmax / (v + 1e-12);
	            node.vx *= s;
	            node.vy *= s;
	            vmaxHits++;
	        }

	        node.x += node.vx;
	        node.y += node.vy;

	        double rad = Math.max(0.0, node.worldRadius);
	        node.x = Math.max(rad, Math.min(1.0 - rad, node.x));
	        node.y = Math.max(rad, Math.min(1.0 - rad, node.y));

	        speedSum += Math.sqrt(node.vx * node.vx + node.vy * node.vy);
	    }

	    double avgSpeed = speedSum / Math.max(1, n);

	    // Collect diagnostics/energy samples every 5 steps (uses avgSpeed + vmaxHits).
	    if (step % 5 == 0) {
	    	Diagnostics d = computeDiagnostics(avgSpeed, vmaxHits, FrmsNow);
	    	diagnosticsSamples.add(d);
	    }

	    if (engine != null && (step % 10 == 0)) {
	        // max(0,...) guards against a negative fraction early in the run
	        // when avgSpeed is far above the settle threshold.
	        double frac = Math.max(0.0,
	                1.0 - Math.min(1.0, avgSpeed / (5.0 * settleVel())));
	        engine.postProgress(ProgressInfo.determinate(frac,
	                "Relaxing… step " + step));
	    }

	    if (engine != null && (step % 2 == 0)) {
	        engine.requestRefresh();
	    }

	    if (step >= minSteps && avgSpeed < settleVel() && FrmsNow < settleForce) {

	        if (engine != null) {
	            engine.postMessage("Settled.");
	            engine.postProgress(ProgressInfo.determinate(1.0, "Settled"));
	            engine.requestRefresh();
	        }
	        return false;
	    }
	    return step < maxSteps;
	}

	/**
	 * Average node speed threshold for declaring the layout "settled".
	 *
	 * <p>After {@link #minSteps} steps, if the mean node speed falls below this
	 * value <em>and</em> the force RMS is below {@link #settleForce}, the
	 * simulation stops.</p>
	 *
	 * <p>The threshold is intentionally coupled to {@link #r0}: a shorter
	 * equilibrium spring length naturally produces a tighter, less-mobile layout
	 * at rest, so the settle speed scales accordingly. Consequence: changing
	 * {@link #r0} after construction silently shifts the effective settle
	 * threshold.</p>
	 *
	 * @return the per-node speed threshold in world-units per step
	 */
	private double settleVel() {
		return r0 / 25.0;
	}

	/**
	 * Cancellation hook called by the engine on the simulation thread.
	 *
	 * <p>Posts a UI message and indeterminate progress indicator so the user
	 * sees immediate feedback. The engine calls
	 * {@link Simulation#shutdown(SimulationContext)} immediately after this
	 * method returns.</p>
	 *
	 * <p>No separate simulation-side cancellation flag is needed:
	 * {@link SimulationContext#isCancelRequested()} is the authoritative
	 * cancellation signal and is already checked at the top of
	 * {@link #step(SimulationContext)}.</p>
	 *
	 * @param ctx the simulation context
	 */
	@Override
	public void cancel(SimulationContext ctx) {
		if (engine != null) {
			engine.postMessage("Cancel requested.");
			engine.postProgress(ProgressInfo.indeterminate("Canceling…"));
		}
	}

	/**
	 * Return the queue of per-step diagnostic samples produced during the
	 * simulation.
	 *
	 * <p>The simulation enqueues a sample every 5 steps on the simulation
	 * thread. The view drains the queue by calling
	 * {@link java.util.Queue#poll()} repeatedly inside its {@code onRefresh}
	 * callback on the EDT. {@link java.util.concurrent.ConcurrentLinkedQueue}
	 * is lock-free, so concurrent access from both threads is safe without any
	 * additional synchronization.</p>
	 *
	 * @return the live diagnostics queue; never {@code null}
	 */
	public java.util.concurrent.ConcurrentLinkedQueue<Diagnostics> getDiagnosticsSamples() {
	    return diagnosticsSamples;
	}

	/**
	 * Return the current simulation step index (1-based; 0 before the first
	 * step has completed).
	 *
	 * @return the step count
	 */
	public int getStep() {
	    return step;
	}

	/**
	 * Compute a pseudo-energy snapshot for the current layout.
	 *
	 * <p>Delegates to {@link #computeEnergyInternal()}, the single canonical
	 * implementation shared with {@link #computeDiagnostics} to eliminate
	 * code duplication.</p>
	 *
	 * <p><strong>Note:</strong> These values are not a conserved Hamiltonian.
	 * See the class-level javadoc for the full explanation of the force/energy
	 * mismatch and other non-conservative effects.</p>
	 *
	 * @return an {@link Energy} record for the current node positions and
	 *         velocities
	 */
	public Energy computeEnergy() {
	    return computeEnergyInternal();
	}

	/**
	 * Canonical energy computation shared by {@link #computeEnergy()} and
	 * {@link #computeDiagnostics}.
	 *
	 * <p>Computes spring pseudo-energy, repulsion pseudo-energy, centering
	 * potential, and kinetic energy. The repulsion pseudo-energy uses
	 * {@code strength / sqrt(r² + ε)} while the corresponding integration force
	 * uses {@code strength / (r² + ε)} — see the class javadoc for why this
	 * mismatch exists and why it is acceptable for a demo.</p>
	 *
	 * @return a new {@link Energy} for the current node state
	 */
	private Energy computeEnergyInternal() {

	    double Uspring  = 0.0;
	    double Urep     = 0.0;
	    double Ucenter  = 0.0;
	    double K        = 0.0;

	    // Spring pseudo-energy: ½ k·kboost·(r - rboost·r0)²
	    for (NetworkModel.Edge e : model.edges) {
	        var a = e.node1;
	        var b = e.node2;

	        double dx = b.x - a.x;
	        double dy = b.y - a.y;
	        double r  = Math.sqrt(dx * dx + dy * dy) + 1e-12;

	        double rboost = 1.0;
	        double kboost = 1.0;
	        if (a.type == Node.NodeType.PRINTER || b.type == Node.NodeType.PRINTER) {
	            rboost = PRINTER_RBOOST;
	            kboost = printerKBoost;
	        }

	        double dr = r - rboost * r0;
	        Uspring += 0.5 * (k * kboost) * dr * dr;
	    }

	    // Repulsion pseudo-energy: strength / sqrt(r² + ε)
	    // (The integration force is strength/(r²+ε), so force ≠ -∇U here;
	    //  see class javadoc for the full discussion.)
	    int n = model.nodes.size();
	    for (int i = 0; i < n; i++) {
	        var a = model.nodes.get(i);
	        for (int j = i + 1; j < n; j++) {
	            var b = model.nodes.get(j);

	            double dx = a.x - b.x;
	            double dy = a.y - b.y;
	            double r2 = dx * dx + dy * dy + REPULSION_EPS;
	            double r  = Math.sqrt(r2);

	            double strength = repulsionC;
	            if (a.type == Node.NodeType.SERVER || b.type == Node.NodeType.SERVER) {
	                strength *= serverRepulsion;
	            }
	            double minDist = a.worldRadius + b.worldRadius + OVERLAP_PAD;
	            if (r2 < minDist * minDist) {
	                strength *= overlapBoost;
	            }
	            Urep += strength / r;
	        }
	    }

	    // Centering potential: ½ centerK · |r - center|²
	    for (var node : model.nodes) {
	        double dx = node.x - 0.5;
	        double dy = node.y - 0.5;
	        Ucenter += 0.5 * centerK * (dx * dx + dy * dy);
	    }

	    // Kinetic pseudo-energy: ½ v²
	    for (var node : model.nodes) {
	        K += 0.5 * (node.vx * node.vx + node.vy * node.vy);
	    }

	    return new Energy(Uspring, Urep, Ucenter, K);
	}



	/**
	 * Compute a {@link Diagnostics} snapshot for the current model state.
	 *
	 * <p>Energies are obtained from {@link #computeEnergyInternal()} — the
	 * single canonical implementation shared with {@link #computeEnergy()} —
	 * so the two can never diverge.</p>
	 *
	 * <p>The caller may supply {@code avgSpeed} and {@code vmaxHitCount} from
	 * values already computed during the integration step to avoid an extra
	 * O(N) scan. Pass {@code Double.NaN} / {@code -1} to have this method
	 * recompute them from the current node velocities.</p>
	 *
	 * @param avgSpeed     mean node speed in world-units/step from the most
	 *                     recent integration, or {@code Double.NaN} to recompute
	 * @param vmaxHitCount number of nodes that hit the speed clamp in the most
	 *                     recent integration step, or {@code -1} to recompute
	 * @param FrmsNow      force RMS computed during the current step
	 * @return a fully-populated {@link Diagnostics} for the current step
	 */
	public Diagnostics computeDiagnostics(double avgSpeed, int vmaxHitCount, double FrmsNow) {

	    // Delegate to the shared helper — no duplicated loops.
	    Energy e = computeEnergyInternal();

	    int n = model.nodes.size();

	    // Recompute avgSpeed if the caller did not supply it.
	    if (Double.isNaN(avgSpeed)) {
	        double speedSum = 0.0;
	        for (var node : model.nodes) {
	            speedSum += Math.sqrt(node.vx * node.vx + node.vy * node.vy);
	        }
	        avgSpeed = speedSum / Math.max(1, n);
	    }

	    // Recompute vmaxHitCount if the caller did not supply it.
	    if (vmaxHitCount < 0) {
	        vmaxHitCount = 0;
	        final double tol = 1e-12;
	        for (var node : model.nodes) {
	            double v2 = node.vx * node.vx + node.vy * node.vy;
	            if (v2 >= (vmax - tol) * (vmax - tol)) {
	                vmaxHitCount++;
	            }
	        }
	    }

	    double vmaxFrac = vmaxHitCount / (double) Math.max(1, n);
	    double minSep   = minPairwiseSeparation();

	    return new Diagnostics(step,
	            e.spring, e.repulsion, e.center, e.kinetic,
	            avgSpeed, FrmsNow, vmaxFrac, minSep);
	}

	/**
	 * Compute the minimum pairwise separation ratio across all node pairs.
	 *
	 * <p>For each pair {@code (a, b)}, the ratio is:
	 * <pre>  r / (worldRadius_a + worldRadius_b + OVERLAP_PAD)</pre>
	 * A value below 1.0 indicates the two icons overlap. The global minimum is
	 * returned; a value near or below 1.0 signals unresolved overlaps.</p>
	 *
	 * <p>This is an O(N²) scan, called only from {@link #computeDiagnostics},
	 * which itself runs only every 5 steps.</p>
	 *
	 * @return the minimum separation ratio; {@link Double#POSITIVE_INFINITY} if
	 *         fewer than two nodes exist
	 */
	private double minPairwiseSeparation() {
		double minSep = Double.POSITIVE_INFINITY;
		int n = model.nodes.size();
		for (int i = 0; i < n; i++) {
			var a = model.nodes.get(i);
			for (int j = i + 1; j < n; j++) {
				var b = model.nodes.get(j);
				double dx = a.x - b.x;
				double dy = a.y - b.y;
				double r = Math.sqrt(dx * dx + dy * dy);
				r /= a.worldRadius + b.worldRadius + OVERLAP_PAD;
				if (r < minSep) {
					minSep = r;
				}
			}
		}
		return minSep;
	}
	/**
	 * Energy diagnostics for the current simulation state.
	 */
	public static final class Energy {
	    public final double spring;
	    public final double repulsion;
	    public final double center;
	    public final double kinetic;

	    public Energy(double spring, double repulsion, double center, double kinetic) {
	        this.spring = spring;
	        this.repulsion = repulsion;
	        this.center = center;
	        this.kinetic = kinetic;
	    }

	    public double potential() {
	        return spring + repulsion + center;
	    }

	    public double total() {
	        return potential() + kinetic;
	    }
	}

	/** Per-step diagnostics useful for plots and tuning. */
	public static final class Diagnostics {
	    public final int step;

	    // Energies (pseudo-energy)
	    public final double Uspring;
	    public final double Urepulsion;
	    public final double Ucenter;
	    public final double kinetic;

	    // Derived
	    public final double avgSpeed;
	    public final double Frms;
	    public final double vmaxHitFraction;
	    public final double minPairwiseSeparation;

	    public Diagnostics(int step,
	                       double Uspring, double Urepulsion, double Ucenter, double Kinetic,
	                       double avgSpeed, double Frms, double vmaxHitFraction, double minPairwiseSeparation) {
	        this.step = step;
	        this.Uspring = Uspring;
	        this.Urepulsion = Urepulsion;
	        this.Ucenter = Ucenter;
	        this.kinetic = Kinetic;
	        this.avgSpeed = avgSpeed;
	        this.Frms = Frms;
	        this.vmaxHitFraction = vmaxHitFraction;
	        this.minPairwiseSeparation = minPairwiseSeparation;
	    }

	    /**
	     * Get the total potential energy.
	     * @return the potential energy
	     */
	    public double potential() {
	        return Uspring + Urepulsion + Ucenter;
	    }

	    /**
	     * Get the total energy (potential + kinetic).
	     * @return the total energy
	     */
	    public double total() {
	        return potential() + kinetic;
	    }
	}

}