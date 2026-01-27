package edu.cnu.mdi.sim.ui;

import java.awt.BorderLayout;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.sim.ISimulationHost;
import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;
import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.SimulationListener;
import edu.cnu.mdi.sim.SimulationState;
import edu.cnu.mdi.view.BaseView;

/**
 * Base class for an MDI {@link BaseView} that hosts a {@link SimulationEngine}.
 * <p>
 * This class is the "MDI bridge" for the MDI-agnostic simulation framework: the
 * simulation engine knows nothing about views/containers, while this view knows
 * how to repaint an {@link IContainer} on refresh and optionally hosts a
 * control panel at {@link BorderLayout#SOUTH}.
 * </p>
 *
 * <h2>Layout</h2>
 * <p>
 * {@link BaseView} already installs the container component in
 * {@link BorderLayout#CENTER} (or inside a split pane), and may add a toolbar
 * in {@link BorderLayout#NORTH}. This class adds the optional control panel at
 * {@link BorderLayout#SOUTH}.
 * </p>
 *
 * <h2>Threading</h2>
 * <p>
 * All {@link SimulationListener} callbacks arrive on the Swing EDT (as
 * guaranteed by {@link SimulationEngine}). Therefore,
 * {@link #onRefresh(SimulationContext)} safely repaints the container
 * component.
 * </p>
 *
 * <h2>Typical Usage</h2>
 *
 * <pre>{@code
 * public class IsingModelView extends SimulationView {
 *     public IsingModelView(Object... keyVals) {
 *         super(new IsingSimulation(), SimulationEngineConfig.defaults(), SimulationControlPanel::new, keyVals);
 *         startAndRun(); // optional
 *     }
 * }
 * }</pre>
 */
@SuppressWarnings("serial")
public class SimulationView extends BaseView implements ISimulationHost, SimulationListener {

	/**
	 * Factory for creating a simulation control panel component.
	 * Demos can supply different factories to change the UI without subclassing views.
	 */
	@FunctionalInterface
	public interface ControlPanelFactory {
		JComponent createControlPanel();
	}

	/** Hosted engine (never null). */
	protected volatile SimulationEngine engine;

	/** Optional control panel component (may be null). */
	protected final JComponent controlPanel;

	/** Default control panel factory: the standard text-button panel. */
	private static final ControlPanelFactory DEFAULT_FACTORY = SimulationControlPanel::new;

	// -------------------------------------------------------------------------
	// Shared reset support (centralized pattern for demos)
	// -------------------------------------------------------------------------

	private volatile java.util.function.Supplier<edu.cnu.mdi.sim.Simulation> _pendingResetSimSupplier;
	private volatile java.util.function.Consumer<edu.cnu.mdi.sim.SimulationEngine> _pendingResetAfterSwap;
	private volatile boolean _pendingResetAutoStart;
	private volatile boolean _pendingResetRefresh;

	/**
	 * Construct a simulation view using default engine configuration and including
	 * the default control panel.
	 *
	 * @param simulation the simulation to run (non-null)
	 * @param keyVals    standard {@link BaseView} key-value arguments
	 */
	public SimulationView(Simulation simulation, Object... keyVals) {
		this(simulation,
		     SimulationEngineConfig.defaults(),
		     true,
		     DEFAULT_FACTORY,
		     keyVals);
	}
	/**
	 * Construct a simulation view with optional inclusion of the default control panel.
	 *
	 * @param simulation          the simulation to run (non-null)
	 * @param config              engine configuration (non-null)
	 * @param includeControlPanel if true, creates and adds the default control panel at SOUTH
	 * @param keyVals             standard {@link BaseView} key-value arguments
	 */
	public SimulationView(Simulation simulation,
			SimulationEngineConfig config,
			boolean includeControlPanel,
			Object... keyVals) {

		this(simulation,
		     config,
		     includeControlPanel,
		     DEFAULT_FACTORY,
		     keyVals);
	}
	/**
	 * Construct a simulation view with a caller-provided control panel factory.
	 * <p>
	 * If {@code includeControlPanel} is true and {@code factory} is null, the
	 * default factory ({@link SimulationControlPanel}) is used.
	 * </p>
	 *
	 * @param simulation          the simulation to run (non-null)
	 * @param config              engine configuration (non-null)
	 * @param includeControlPanel if true, creates and adds a control panel at SOUTH
	 * @param factory             factory used to create the control panel component
	 * @param keyVals             standard {@link BaseView} key-value arguments
	 */
	public SimulationView(Simulation simulation, SimulationEngineConfig config, boolean includeControlPanel,
			ControlPanelFactory factory, Object... keyVals) {

		super(keyVals);

		Objects.requireNonNull(simulation, "simulation");
		Objects.requireNonNull(config, "config");

		engine = new SimulationEngine(simulation, config);
		engine.addListener(this);

		if (includeControlPanel) {
			ControlPanelFactory f = (factory != null) ? factory : DEFAULT_FACTORY;

			JComponent cp = f.createControlPanel();
			controlPanel = cp;

			// Bind if the panel supports it (keeps this class independent of the panel type)
			if (cp instanceof ISimulationControlPanel scp) {
				scp.bind(this);
			}

			add(cp, BorderLayout.SOUTH);

			// pack to account for new SOUTH component
			pack();
		} else {
			controlPanel = null;
		}
	}

	@Override
	public final SimulationEngine getSimulationEngine() {
		return engine;
	}

	// ------------------------------------------------------------------------
	// SimulationListener default behavior + overridable hooks
	// ------------------------------------------------------------------------

	@Override
	public void onStateChange(SimulationContext ctx, SimulationState from, SimulationState to, String reason) {

	    // If a reset is pending, complete it once the current engine is fully stopped.
	    if ((to == SimulationState.TERMINATED || to == SimulationState.FAILED) && _pendingResetSimSupplier != null) {
	        java.util.function.Supplier<Simulation> supplier = _pendingResetSimSupplier;
	        java.util.function.Consumer<SimulationEngine> after = _pendingResetAfterSwap;
	        boolean autoStart = _pendingResetAutoStart;
	        boolean refresh = _pendingResetRefresh;

	        // Clear first (defensive against re-entrancy)
	        _pendingResetSimSupplier = null;
	        _pendingResetAfterSwap = null;

	        doEngineResetNow(supplier, after, autoStart, refresh);
	        // fall through to hook
	    }

	    onSimulationStateChange(ctx, from, to, reason);
	}

	@Override
	public void onInit(SimulationContext ctx) {
		onSimulationInit(ctx);
	}

	@Override
	public void onReady(SimulationContext ctx) {
		onSimulationReady(ctx);
	}

	@Override
	public void onRun(SimulationContext ctx) {
		onSimulationRun(ctx);
	}

	@Override
	public void onResume(SimulationContext ctx) {
		onSimulationResume(ctx);
	}

	@Override
	public void onPause(SimulationContext ctx) {
		onSimulationPause(ctx);
	}

	@Override
	public void onDone(SimulationContext ctx) {
		onSimulationDone(ctx);
	}

	@Override
	public void onFail(SimulationContext ctx, Throwable error) {
		onSimulationFail(ctx, error);
	}

	@Override
	public void onCancelRequested(SimulationContext ctx) {
		onSimulationCancelRequested(ctx);
	}

	@Override
	public void onMessage(SimulationContext ctx, String message) {
		onSimulationMessage(ctx, message);
	}

	@Override
	public void onProgress(SimulationContext ctx, ProgressInfo progress) {
		onSimulationProgress(ctx, progress);
	}

	/**
	 * Default refresh behavior for MDI: repaint the container component.
	 * <p>
	 * The container is available via {@link BaseView#getContainer()}.
	 * </p>
	 * <p>
	 * If this view has no container, this falls back to repainting the frame
	 * content.
	 * </p>
	 */
	@Override
	public void onRefresh(SimulationContext ctx) {
		onSimulationRefresh(ctx);

		IContainer cont = getContainer();
		if (cont != null && cont.getComponent() != null) {
			cont.getComponent().repaint();
		} else {
			// e.g., a container-less view: repaint the internal frame's content
			repaint();
		}
	}

	// ------------------------------------------------------------------------
	// Overridable hooks (all called on EDT)
	// ------------------------------------------------------------------------

	/** Hook called on any state change. */
	protected void onSimulationStateChange(SimulationContext ctx, SimulationState from, SimulationState to,
			String reason) {
	}

	/** Hook called when initialization begins. */
	protected void onSimulationInit(SimulationContext ctx) {
	}

	/**
	 * Hook called when initialization completes and the simulation becomes ready.
	 */
	protected void onSimulationReady(SimulationContext ctx) {
	}

	/** Hook called when the simulation begins running. */
	protected void onSimulationRun(SimulationContext ctx) {
	}

	/** Hook called when the simulation resumes from pause. */
	protected void onSimulationResume(SimulationContext ctx) {
	}

	/** Hook called when the simulation pauses. */
	protected void onSimulationPause(SimulationContext ctx) {
	}

	/** Hook called when the simulation terminates normally. */
	protected void onSimulationDone(SimulationContext ctx) {
	}

	/** Hook called when the simulation fails. */
	protected void onSimulationFail(SimulationContext ctx, Throwable error) {
	}

	/** Hook called when cancel is requested. */
	protected void onSimulationCancelRequested(SimulationContext ctx) {
	}

	/** Hook called when a UI message is posted. */
	protected void onSimulationMessage(SimulationContext ctx, String message) {
	}

	/** Hook called when progress is posted. */
	protected void onSimulationProgress(SimulationContext ctx, ProgressInfo progress) {
	}

	/** Hook called when a refresh is requested (prior to repaint). */
	protected void onSimulationRefresh(SimulationContext ctx) {
	}

	/**
	 * Replace the hosted engine with a new one.
	 * <p>
	 * This is primarily intended for demos that need to rebuild a simulation with
	 * a different model size/parameters (e.g., TSP city count changes).
	 * </p>
	 * <p>
	 * This method:
	 * </p>
	 * <ul>
	 *   <li>removes this view as a listener from the old engine</li>
	 *   <li>installs the new engine</li>
	 *   <li>adds this view as a listener to the new engine</li>
	 *   <li>rebinds the control panel if it supports {@link ISimulationControlPanel}</li>
	 * </ul>
	 *
	 * <p>
	 * Must be called on the EDT.
	 * </p>
	 *
	 * @param newEngine new engine (non-null)
	 */
	protected final void replaceEngine(SimulationEngine newEngine) {
		Objects.requireNonNull(newEngine, "newEngine");

		if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
			javax.swing.SwingUtilities.invokeLater(() -> replaceEngine(newEngine));
			return;
		}

		SimulationEngine old = this.engine;
		if (old != null) {
			try { old.removeListener(this); } catch (Throwable ignored) {}
		}

		this.engine = newEngine;
		this.engine.addListener(this);

		// Rebind control panel if present
		if (controlPanel instanceof edu.cnu.mdi.sim.ui.ISimulationControlPanel scp) {
			try { scp.unbind(); } catch (Throwable ignored) {}
			scp.bind(this);
		}
	}
	
	/**
	 * Centralized reset pattern:
	 * <ul>
	 *   <li>Reuse current engine config</li>
	 *   <li>Stop old engine if needed</li>
	 *   <li>Swap to a brand new engine (SimulationEngine holds a final Simulation)</li>
	 *   <li>Optionally auto-start and refresh</li>
	 * </ul>
	 *
	 * @param simSupplier builds a brand-new Simulation
	 * @param afterSwap   optional hook invoked after replaceEngine(newEngine) (may be null)
	 * @param autoStart   if true, calls startSimulation() after the swap
	 * @param refresh     if true, requests a refresh on the new engine after the swap
	 */
	protected final void requestEngineReset(
	        java.util.function.Supplier<edu.cnu.mdi.sim.Simulation> simSupplier,
	        java.util.function.Consumer<edu.cnu.mdi.sim.SimulationEngine> afterSwap,
	        boolean autoStart,
	        boolean refresh) {

	    java.util.Objects.requireNonNull(simSupplier, "simSupplier");

	    final SimulationEngine e = getSimulationEngine();
	    if (e == null) {
	        return;
	    }

	    SimulationState state = e.getState();

	    // "Safe" means we can swap immediately without needing to stop a running thread.
	    boolean safe = (state == SimulationState.NEW
	            || state == SimulationState.READY
	            || state == SimulationState.TERMINATED
	            || state == SimulationState.FAILED);

	    if (safe) {
	        doEngineResetNow(simSupplier, afterSwap, autoStart, refresh);
	        return;
	    }

	    // Defer swap until engine reaches TERMINATED/FAILED
	    _pendingResetSimSupplier = simSupplier;
	    _pendingResetAfterSwap = afterSwap;
	    _pendingResetAutoStart = autoStart;
	    _pendingResetRefresh = refresh;

	    if (state != SimulationState.TERMINATING) {
	        e.requestStop();
	    }
	}

	/**
	 * Immediately performs a simulation engine reset by constructing and swapping in
	 * a brand-new {@link SimulationEngine} backed by a new {@link Simulation}.
	 * <p>
	 * This method is the final step of the shared reset workflow initiated by
	 * {@link #requestEngineReset(java.util.function.Supplier, java.util.function.Consumer, boolean, boolean)}.
	 * It assumes that the current engine is in a <em>safe</em> state (i.e., not running)
	 * and therefore does <strong>not</strong> attempt to stop or synchronize with an
	 * active engine thread.
	 * </p>
	 *
	 * <h3>Reset contract</h3>
	 * <ul>
	 *   <li>The current engine's {@link SimulationEngineConfig} is reused verbatim.</li>
	 *   <li>A new {@link Simulation} is obtained from {@code simSupplier}.</li>
	 *   <li>A new {@link SimulationEngine} is constructed and atomically installed
	 *       via {@link #replaceEngine(SimulationEngine)}.</li>
	 *   <li>The previous engine is abandoned; callers must ensure it is already
	 *       terminated or has never been started.</li>
	 *   <li>The optional {@code afterSwap} hook is invoked <em>after</em> the engine
	 *       replacement, allowing view-specific rewiring (model references,
	 *       listeners, sim-to-engine binding, etc.).</li>
	 *   <li>If {@code autoStart} is {@code true}, the new engine thread is started
	 *       (entering {@link SimulationState#READY} until Run is requested).</li>
	 *   <li>If {@code refresh} is {@code true}, a refresh request is issued so the
	 *       new simulation state is immediately visible.</li>
	 * </ul>
	 *
	 * <h3>Threading</h3>
	 * <p>
	 * This method must execute on the Swing Event Dispatch Thread (EDT). If invoked
	 * from a non-EDT thread, it will automatically marshal itself onto the EDT before
	 * performing the reset.
	 * </p>
	 *
	 * <h3>Usage notes</h3>
	 * <ul>
	 *   <li>This method should not be called directly by demos; use
	 *       {@code requestEngineReset(...)} instead, which safely handles running
	 *       engines by stopping them first and deferring the swap until termination.</li>
	 *   <li>The supplied {@code Simulation} instance should be <em>brand new</em>
	 *       and must not be shared with any other engine.</li>
	 * </ul>
	 *
	 * @param simSupplier supplies a brand-new {@link Simulation} instance
	 * @param afterSwap optional hook invoked after the new engine is installed
	 *                  (may be {@code null})
	 * @param autoStart if {@code true}, automatically starts the new engine thread
	 * @param refresh if {@code true}, requests a refresh after the swap so the
	 *                new simulation state is rendered immediately
	 */	private void doEngineResetNow(
	        java.util.function.Supplier<edu.cnu.mdi.sim.Simulation> simSupplier,
	        java.util.function.Consumer<edu.cnu.mdi.sim.SimulationEngine> afterSwap,
	        boolean autoStart,
	        boolean refresh) {

	    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
	        javax.swing.SwingUtilities.invokeLater(() -> doEngineResetNow(simSupplier, afterSwap, autoStart, refresh));
	        return;
	    }

	    final SimulationEngine oldEngine = getSimulationEngine();
	    if (oldEngine == null) {
	        return;
	    }

	    final SimulationEngineConfig cfg = oldEngine.getConfig();
	    final Simulation newSim = simSupplier.get();
	    final SimulationEngine newEngine = new SimulationEngine(newSim, cfg);

	    replaceEngine(newEngine);

	    if (afterSwap != null) {
	        afterSwap.accept(newEngine);
	    }

	    if (autoStart) {
	        startSimulation();
	    }
	    if (refresh) {
	        newEngine.requestRefresh();
	    }
	}



	// ------------------------------------------------------------------------
	// Convenience
	// ------------------------------------------------------------------------

	/**
	 * Convenience method to start the engine (if needed) and request run.
	 * <p>
	 * Safe to call from any thread; it marshals to the EDT if necessary.
	 * </p>
	 */
	public final void startAndRun() {
		if (SwingUtilities.isEventDispatchThread()) {
			startSimulation();
			runSimulation();
		} else {
			SwingUtilities.invokeLater(() -> {
				startSimulation();
				runSimulation();
			});
		}
	}
}
