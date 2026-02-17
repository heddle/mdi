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
 * {@link NetworkModel.Node#worldRadius} for each node every frame (or whenever
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
 * v <- damping * v + dt * F
 * x <- x + v
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
	 * Printer-specific increase to spring constant.
	 * <p>
	 * Springs involving printers use {@code k * printerKBoost} as their
	 * stiffness. This makes printer connections stiffer, helping them
	 * resist being pulled around by other forces.
	 * </p>
	 */
	public double printerKBoost = 1.2;

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

	/** True if {@link #cancel(SimulationContext)} has been called. */
	private volatile boolean canceled;

	/** Simulation engine used for posting progress/messages/refresh. */
	private SimulationEngine engine;
	
	/** Queue of energy samples collected during the simulation. */
	private final java.util.concurrent.ConcurrentLinkedQueue<EnergySample> energySamples = new java.util.concurrent.ConcurrentLinkedQueue<>();

	/** Buffered per-step diagnostics samples (drained by the view on refresh). */
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

	    // return false (terminate) if normal completion conditions met
	    if (canceled || ctx.isCancelRequested()) {
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
	    	double frac = 1.0 - Math.min(1.0, avgSpeed / (5.0 * settleVel()));
	        engine.postProgress(ProgressInfo.determinate(frac, "Relaxing… step " + step));
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
	 * <p>
	 * After {@link #minSteps}, if the mean speed (averaged across all nodes) falls
	 * below this value, the simulation stops.
	 * </p>
	 */
	private double settleVel() {
		return r0 / 25.0;
	}

	@Override
	public void cancel(SimulationContext ctx) {
		canceled = true;
		if (engine != null) {
			engine.postMessage("Cancel requested.");
			engine.postProgress(ProgressInfo.indeterminate("Canceling…"));
		}
	}
	
	/**
	 * Get the queue of diagnostics samples collected during the simulation.
	 * 
	 * @return the diagnostics samples queue
	 */
	public java.util.concurrent.ConcurrentLinkedQueue<Diagnostics> getDiagnosticsSamples() {
	    return diagnosticsSamples;
	}

	
	/**
	 * Get the current simulation step index.
	 *
	 * @return the step (0-based)
	 */
	public int getStep() {
	    return step;
	}

	/**
	 * Get the queue of energy samples collected during the simulation.
	 * 
	 * @return the energy samples queue
	 */
	public java.util.concurrent.ConcurrentLinkedQueue<EnergySample> getEnergySamples() {
	    return energySamples;
	}
	
	/**
	 * Compute a pseudo-energy diagnostic for the current layout.
	 * <p>
	 * This is not a conserved physical energy (due to damping, clamping,
	 * velocity caps, and piecewise forces), but it is a useful monotone-ish
	 * objective for diagnosing convergence and tuning parameters.
	 * </p>
	 */
	public Energy computeEnergy() {

	    double Uspring = 0.0;
	    double Urep = 0.0;
	    double Ucenter = 0.0;
	    double K = 0.0;

	    // --- spring energy ---
	    for (NetworkModel.Edge e : model.edges) {
	        var a = e.node1;
	        var b = e.node2;

	        double dx = b.x - a.x;
	        double dy = b.y - a.y;
	        double r = Math.sqrt(dx * dx + dy * dy) + 1e-12;

	        double rboost = 1.0;
	        double kboost = 1.0;

	        if (a.type == Node.NodeType.PRINTER || b.type == Node.NodeType.PRINTER) {
	            rboost = 0.8;
	            kboost = printerKBoost;
	        }

	        double dr = r - rboost * r0;
	        Uspring += 0.5 * (k * kboost) * dr * dr;
	    }

	    // --- repulsion energy ---
	    int n = model.nodes.size();
	    double eps = 1.0e-4;

	    for (int i = 0; i < n; i++) {
	        var a = model.nodes.get(i);
	        for (int j = i + 1; j < n; j++) {
	            var b = model.nodes.get(j);

	            double dx = a.x - b.x;
	            double dy = a.y - b.y;
	            double r2 = dx * dx + dy * dy + eps;
	            double r = Math.sqrt(r2);

	            double strength = repulsionC;
	            if (a.type == Node.NodeType.SERVER || b.type == Node.NodeType.SERVER) {
	                strength *= serverRepulsion;
	            }

	            double minDist = a.worldRadius + b.worldRadius + 0.01;
	            if (r2 < minDist * minDist) {
	                strength *= overlapBoost;
	            }

	            // Potential ~ strength / r
	            Urep += strength / r;
	        }
	    }

	    // --- centering energy ---
	    for (var node : model.nodes) {
	        double dx = node.x - 0.5;
	        double dy = node.y - 0.5;
	        Ucenter += 0.5 * centerK * (dx * dx + dy * dy);
	    }

	    // --- kinetic energy ---
	    for (var node : model.nodes) {
	        K += 0.5 * (node.vx * node.vx + node.vy * node.vy);
	    }

	    return new Energy(Uspring, Urep, Ucenter, K);
	}

	/**
	 * Energy sample at a given simulation step.
	 */
	public static final class EnergySample {
	    public final int step;
	    public final Energy energy;
	    public EnergySample(int step, Energy energy) {
	    	this.step = step; 
	    	this.energy = energy; }
	}
	
	/**
	 * Compute diagnostics for the current model state.
	 * <p>
	 * Note: Energies are "pseudo-energies" for tuning/monitoring; the dynamics are
	 * not Hamiltonian due to damping, clamping, velocity caps, and piecewise forces.
	 * </p>
	 *
	 * @param avgSpeed optional average speed computed during integration step; if
	 *                 unknown, pass {@code Double.NaN} and it will be recomputed
	 * @param vmaxHitCount optional count of nodes that hit the velocity clamp during
	 *                     integration; if unknown, pass {@code -1} and it will be
	 *                     recomputed (slower)
	 */
	public Diagnostics computeDiagnostics(double avgSpeed, int vmaxHitCount, double FrmsNow) {

	    // ---- energies ----
	    double Uspring = 0.0;
	    double Urep = 0.0;
	    double Ucenter = 0.0;
	    double K = 0.0;

	    // spring energy (edges)
	    for (NetworkModel.Edge e : model.edges) {
	        var a = e.node1;
	        var b = e.node2;

	        double dx = b.x - a.x;
	        double dy = b.y - a.y;
	        double r = Math.sqrt(dx * dx + dy * dy) + 1e-12;

	        double rboost = 1.0;
	        double kboost = 1.0;
	        if (a.type == Node.NodeType.PRINTER || b.type == Node.NodeType.PRINTER) {
	            rboost = 0.8;
	            kboost = printerKBoost;
	        }

	        double dr = r - rboost * r0;
	        Uspring += 0.5 * (k * kboost) * dr * dr;
	    }

	    // repulsion energy (all pairs)
	    int n = model.nodes.size();
	    double eps = 1.0e-4;
	    double overlapPad = 0.01;

	    for (int i = 0; i < n; i++) {
	        var a = model.nodes.get(i);
	        for (int j = i + 1; j < n; j++) {
	            var b = model.nodes.get(j);

	            double dx = a.x - b.x;
	            double dy = a.y - b.y;
	            double r2 = dx * dx + dy * dy + eps;
	            double r = Math.sqrt(r2);

	            double strength = repulsionC;
	            if (a.type == Node.NodeType.SERVER || b.type == Node.NodeType.SERVER) {
	                strength *= serverRepulsion;
	            }

	            double minDist = a.worldRadius + b.worldRadius + overlapPad;
	            if (r2 < minDist * minDist) {
	                strength *= overlapBoost;
	            }

	            // Potential ~ strength / r for force ~ strength / r^2
	            Urep += strength / r;
	        }
	    }

	    // centering potential
	    for (var node : model.nodes) {
	        double dx = node.x - 0.5;
	        double dy = node.y - 0.5;
	        Ucenter += 0.5 * centerK * (dx * dx + dy * dy);
	    }

	    // kinetic
	    for (var node : model.nodes) {
	        K += 0.5 * (node.vx * node.vx + node.vy * node.vy);
	    }


	    // ---- avg speed + clamp fraction ----
	    if (Double.isNaN(avgSpeed)) {
	        double speedSum = 0.0;
	        for (var node : model.nodes) {
	            speedSum += Math.sqrt(node.vx * node.vx + node.vy * node.vy);
	        }
	        avgSpeed = speedSum / Math.max(1, n);
	    }

		if (vmaxHitCount < 0) {
			// recompute (slower, but still O(N))
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
	    
	    // ---- min pairwise separation ----
	    double minSep = minPairwiseSeparation();

	    return new Diagnostics(step, Uspring, Urep, Ucenter, K, avgSpeed, FrmsNow, vmaxFrac, minSep);
	}

	// compute minimum pairwise separation ratio
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
