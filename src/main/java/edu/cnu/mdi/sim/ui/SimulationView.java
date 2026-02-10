package edu.cnu.mdi.sim.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
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
 * how to repaint an {@link IContainer} on refresh and optionally hosts UI
 * components (control panel, diagnostics).
 * </p>
 *
 * <h2>Layout</h2>
 * <p>
 * {@link BaseView} already installs the container component in
 * {@link BorderLayout#CENTER} (or inside a split pane), and may add a toolbar
 * in {@link BorderLayout#NORTH}. This class adds:
 * </p>
 * <ul>
 * <li>Optional control panel in {@link BorderLayout#SOUTH}</li>
 * <li>Optional diagnostics component on the right, installed by wrapping the
 * current CENTER component in a {@link JSplitPane}</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * <p>
 * All {@link SimulationListener} callbacks arrive on the Swing EDT (as
 * guaranteed by {@link SimulationEngine}). Therefore,
 * {@link #onRefresh(SimulationContext)} safely repaints the container
 * component.
 * </p>
 */
@SuppressWarnings("serial")
public class SimulationView extends BaseView implements ISimulationHost, SimulationListener {

	/**
	 * Factory for creating a simulation control panel component. Demos can supply
	 * different factories to change the UI without subclassing views.
	 */
	@FunctionalInterface
	public interface ControlPanelFactory {
		JComponent createControlPanel();
	}

	/**
	 * Factory for creating an optional diagnostics component (e.g.,
	 * plots/inspector) that will appear on the right side of a split pane.
	 */
	@FunctionalInterface
	public interface DiagnosticFactory {
		JComponent createDiagnostics();
	}

	/** Hosted engine (never null). */
	protected volatile SimulationEngine engine;

	/** Optional control panel component (may be null). */
	protected final JComponent controlPanel;

	/** Optional diagnostics component (may be null). */
	protected final JComponent diagnostics;

	/** If diagnostics is installed, this is the split pane (else null). */
	protected final JSplitPane diagnosticsSplitPane;

	/** Default control panel factory: the standard text-button panel. */
	private static final ControlPanelFactory DEFAULT_FACTORY = SimulationControlPanel::new;

	/** Default diagnostics split fraction (main/left portion). */
	private static final double DEFAULT_DIAG_SPLIT_FRACTION = 0.72;

	// -------------------------------------------------------------------------
	// Shared reset support (centralized pattern for demos)
	// -------------------------------------------------------------------------

	private volatile java.util.function.Supplier<edu.cnu.mdi.sim.Simulation> _pendingResetSimSupplier;
	private volatile java.util.function.Consumer<edu.cnu.mdi.sim.SimulationEngine> _pendingResetAfterSwap;
	private volatile boolean _pendingResetAutoStart;
	private volatile boolean _pendingResetRefresh;

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Construct a simulation view using default engine configuration and including
	 * the default control panel.
	 *
	 * @param simulation the simulation to run (non-null)
	 * @param keyVals    standard {@link BaseView} key-value arguments
	 */
	public SimulationView(Simulation simulation, Object... keyVals) {
		this(simulation, SimulationEngineConfig.defaults(), true, DEFAULT_FACTORY, false, null,
				DEFAULT_DIAG_SPLIT_FRACTION, keyVals);
	}

	/**
	 * Construct a simulation view with optional inclusion of the default control
	 * panel.
	 *
	 * @param simulation          the simulation to run (non-null)
	 * @param config              engine configuration (non-null)
	 * @param includeControlPanel if true, creates and adds the default control
	 *                            panel at SOUTH
	 * @param keyVals             standard {@link BaseView} key-value arguments
	 */
	public SimulationView(Simulation simulation, SimulationEngineConfig config, boolean includeControlPanel,
			Object... keyVals) {

		this(simulation, config, includeControlPanel, DEFAULT_FACTORY, false, null, DEFAULT_DIAG_SPLIT_FRACTION,
				keyVals);
	}

	/**
	 * Construct a simulation view with a caller-provided control panel factory.
	 *
	 * @param simulation          the simulation to run (non-null)
	 * @param config              engine configuration (non-null)
	 * @param includeControlPanel if true, creates and adds a control panel at SOUTH
	 * @param factory             factory used to create the control panel component
	 * @param keyVals             standard {@link BaseView} key-value arguments
	 */
	public SimulationView(Simulation simulation, SimulationEngineConfig config, boolean includeControlPanel,
			ControlPanelFactory factory, Object... keyVals) {

		this(simulation, config, includeControlPanel, factory, false, null, DEFAULT_DIAG_SPLIT_FRACTION, keyVals);
	}

	/**
	 * Construct a simulation view with optional control panel and optional
	 * diagnostics panel.
	 *
	 * @param simulation           the simulation to run (non-null)
	 * @param config               engine configuration (non-null)
	 * @param includeControlPanel  if true, creates and adds a control panel at
	 *                             SOUTH
	 * @param controlPanelFactory  factory used to create the control panel
	 *                             component (may be null if includeControlPanel is
	 *                             false)
	 * @param includeDiagnostics   if true, installs a right-side diagnostics
	 *                             component via a split pane
	 * @param diagnosticFactory    factory used to create the diagnostics component
	 *                             (required if includeDiagnostics is true)
	 * @param initialSplitFraction fraction of width given to the main (left)
	 *                             component on startup (0..1)
	 * @param keyVals              standard {@link BaseView} key-value arguments
	 */
	public SimulationView(Simulation simulation, 
			SimulationEngineConfig config, 
			boolean includeControlPanel,
			ControlPanelFactory controlPanelFactory, 
			boolean includeDiagnostics, 
			DiagnosticFactory diagnosticFactory,
			double initialSplitFraction, 
			Object... keyVals) {

		super(keyVals);

		Objects.requireNonNull(simulation, "simulation");
		Objects.requireNonNull(config, "config");

		// The engine gets created first since the control panel factory 
		// may want to bind to it during creation. 
		engine = new SimulationEngine(simulation, config);
		engine.addListener(this);

		// if the control panel is included, create it and bind to this host. The factory
		// can be null if the caller doesn't want a control panel but includeControlPanel is true.
		if (includeControlPanel) {
			ControlPanelFactory f = (controlPanelFactory != null) ? controlPanelFactory : DEFAULT_FACTORY;

			JComponent cp = f.createControlPanel();
			controlPanel = cp;

			// If the control panel implements ISimulationControlPanel, bind it to 
			// this host. The effect of binding is that the control panel's buttons 
			// will control this view's engine.
			if (cp instanceof ISimulationControlPanel scp) {
				scp.bind(this);
			}

			add(cp, BorderLayout.SOUTH);
		} else {
			controlPanel = null;
		}

		// --- Diagnostics (right-side split) ---
		if (includeDiagnostics) {
			Objects.requireNonNull(diagnosticFactory, "diagnosticFactory");
			double frac = clamp01(initialSplitFraction);

			JComponent diag = diagnosticFactory.createDiagnostics();
			diagnostics = diag;

			diagnosticsSplitPane = installDiagnosticsSplit(diag, frac);
		} else {
			diagnostics = null;
			diagnosticsSplitPane = null;
		}

		// Pack after structural changes (SOUTH and/or split pane)
		pack();
	}

	private static double clamp01(double x) {
		if (Double.isNaN(x))
			return DEFAULT_DIAG_SPLIT_FRACTION;
		return Math.max(0.0, Math.min(1.0, x));
	}

	/**
	 * Wrap the current CENTER component in a horizontal split pane, placing
	 * {@code diag} on the right. If no CENTER component exists, this method does
	 * nothing and returns null.
	 *
	 * @param diag         diagnostics component (non-null)
	 * @param mainFraction fraction of width given to the main (left) side (0..1)
	 * @return the created split pane, or null if no CENTER component was found
	 */
	private JSplitPane installDiagnosticsSplit(JComponent diag, double mainFraction) {
		Objects.requireNonNull(diag, "diag");

		Container cp = getContentPane();
		if (!(cp.getLayout() instanceof BorderLayout bl)) {
			// Unexpected layout; best effort: do nothing
			return null;
		}

		java.awt.Component center = bl.getLayoutComponent(cp, BorderLayout.CENTER);
		if (center == null) {
			return null;
		}

		// Remove old center and replace with split pane.
		cp.remove(center);

		Component left = center;
		
		JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, diag);

		// Divider location based on fraction needs a realized size; set it after
		// layout.
		SwingUtilities.invokeLater(() -> {
			try {
				sp.setDividerLocation(mainFraction);
			} catch (Throwable ignored) {
				// Some LAFs can throw if not displayable yet; resizeWeight still makes it
				// reasonable.
			}
		});

		cp.add(sp, BorderLayout.CENTER);
		cp.revalidate();
		cp.repaint();
		


		return sp;
	}

	/**
	 * Get the diagnostics component, if any.
	 * @return the diagnostics component, or null if none
	 */
	protected JComponent getDiagnosticsComponent() {
		return diagnostics;
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
	 */
	@Override
	public void onRefresh(SimulationContext ctx) {
		onSimulationRefresh(ctx);

		IContainer cont = getContainer();
		if (cont != null && cont.getComponent() != null) {
			cont.getComponent().repaint();
		} else {
			repaint();
		}
	}

	// ------------------------------------------------------------------------
	// Overridable hooks (all called on EDT)
	// ------------------------------------------------------------------------

	protected void onSimulationStateChange(SimulationContext ctx, SimulationState from, SimulationState to,
			String reason) {
	}

	protected void onSimulationInit(SimulationContext ctx) {
	}

	protected void onSimulationReady(SimulationContext ctx) {
	}

	protected void onSimulationRun(SimulationContext ctx) {
	}

	protected void onSimulationResume(SimulationContext ctx) {
	}

	protected void onSimulationPause(SimulationContext ctx) {
	}

	protected void onSimulationDone(SimulationContext ctx) {
	}

	protected void onSimulationFail(SimulationContext ctx, Throwable error) {
	}

	protected void onSimulationCancelRequested(SimulationContext ctx) {
	}

	protected void onSimulationMessage(SimulationContext ctx, String message) {
	}

	protected void onSimulationProgress(SimulationContext ctx, ProgressInfo progress) {
	}

	protected void onSimulationRefresh(SimulationContext ctx) {
	}

	// ------------------------------------------------------------------------
	// Engine replacement / reset (unchanged)
	// ------------------------------------------------------------------------

	protected final void replaceEngine(SimulationEngine newEngine) {
		Objects.requireNonNull(newEngine, "newEngine");

		if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
			javax.swing.SwingUtilities.invokeLater(() -> replaceEngine(newEngine));
			return;
		}

		SimulationEngine old = this.engine;
		if (old != null) {
			try {
				old.removeListener(this);
			} catch (Throwable ignored) {
			}
		}

		this.engine = newEngine;
		this.engine.addListener(this);

		if (controlPanel instanceof edu.cnu.mdi.sim.ui.ISimulationControlPanel scp) {
			try {
				scp.unbind();
			} catch (Throwable ignored) {
			}
			scp.bind(this);
		}
	}

	protected final void requestEngineReset(java.util.function.Supplier<edu.cnu.mdi.sim.Simulation> simSupplier,
			java.util.function.Consumer<edu.cnu.mdi.sim.SimulationEngine> afterSwap, boolean autoStart,
			boolean refresh) {

		java.util.Objects.requireNonNull(simSupplier, "simSupplier");

		final SimulationEngine e = getSimulationEngine();
		if (e == null) {
			return;
		}

		SimulationState state = e.getState();

		boolean safe = (state == SimulationState.NEW || state == SimulationState.READY
				|| state == SimulationState.TERMINATED || state == SimulationState.FAILED);

		if (safe) {
			doEngineResetNow(simSupplier, afterSwap, autoStart, refresh);
			return;
		}

		_pendingResetSimSupplier = simSupplier;
		_pendingResetAfterSwap = afterSwap;
		_pendingResetAutoStart = autoStart;
		_pendingResetRefresh = refresh;

		if (state != SimulationState.TERMINATING) {
			e.requestStop();
		}
	}

	private void doEngineResetNow(java.util.function.Supplier<edu.cnu.mdi.sim.Simulation> simSupplier,
			java.util.function.Consumer<edu.cnu.mdi.sim.SimulationEngine> afterSwap, boolean autoStart,
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
