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
	protected final SimulationEngine engine;

	/** Optional control panel component (may be null). */
	protected final JComponent controlPanel;

	/** Default control panel factory: the standard text-button panel. */
	private static final ControlPanelFactory DEFAULT_FACTORY = SimulationControlPanel::new;

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
