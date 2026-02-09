package edu.cnu.mdi.sim.demo.forestfire;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import edu.cnu.mdi.sim.ISimulationHost;
import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationListener;
import edu.cnu.mdi.sim.SimulationState;
import edu.cnu.mdi.sim.demo.network.IResettable;
import edu.cnu.mdi.sim.ui.ISimulationControlPanel;
import edu.cnu.mdi.sim.ui.IconSimulationControlPanel;
import edu.cnu.mdi.sim.ui.StandardSimIcons;

@SuppressWarnings("serial")
public class ForestFireControlPanel extends JPanel  implements ISimulationControlPanel, SimulationListener {

	// Base panel with buttons + progress
	private final IconSimulationControlPanel basePanel;

	// Reset button resets the demo with current parameters
	private JButton resetButton;
	
	// the host is typically a subclass of SimulationView
	private ISimulationHost host;

	
	/**
	 * Construct a new forest fire demo control panel with standard icons.
	 */
	public ForestFireControlPanel() {
		this(new StandardSimIcons());
	}

	/**
	 * Construct a new TSP demo control panel using the provided icon set.
	 *
	 * @param icons simulation icons (non-null)
	 */
	public ForestFireControlPanel(StandardSimIcons icons) {
		super(new BorderLayout(8, 0));
		Objects.requireNonNull(icons, "icons");

		// Base panel (left) has the standard media icons
		basePanel = new IconSimulationControlPanel(icons, false);
		add(basePanel, BorderLayout.WEST);

		// Reset button (right)
		addResetButton();

	}
	
	@Override
	public void bind(ISimulationHost host) {
		this.host = Objects.requireNonNull(host, "host");

		// Bind the base panel (buttons + progress) to the same host.
		basePanel.bind(host);

		// Listen for state changes so we can enable/disable our controls.
		host.getSimulationEngine().addListener(this);

		// Apply current state immediately.
		applyState(host.getSimulationState());
	}
	

	// add the reset button on the right
	private void addResetButton() {
		// put the reset button (East) on a panel to keep it from stretching
		resetButton = new JButton("Reset");
		resetButton.setEnabled(false);
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
		btnPanel.add(resetButton);
		btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		resetButton.addActionListener(e -> requestResetFromHost());
		add(btnPanel, BorderLayout.EAST);
	}


	@Override
	public void unbind() {
		if (host != null) {
			try {
				host.getSimulationEngine().removeListener(this);
			} catch (Throwable ignored) {
				// Defensive: engine may already be stopping.
			}
		}
		basePanel.unbind();
		host = null;
	}

	@Override
	public void onStateChange(SimulationContext ctx, SimulationState from, SimulationState to, String reason) {
		applyState(to);
	}

	@Override
	public void onReady(SimulationContext ctx) {
		applyState(SimulationState.READY);
	}

	@Override
	public void onDone(SimulationContext ctx) {
		applyState(SimulationState.TERMINATED);
	}

	@Override
	public void onFail(SimulationContext ctx, Throwable error) {
		applyState(SimulationState.FAILED);
	}

	@Override
	public void onProgress(SimulationContext ctx, ProgressInfo progress) {
		// no-op (base panel handles progress UI)
	}

	// based on simulation state, enable/disable our controls
	private void applyState(SimulationState state) {
	    boolean editable = (state == SimulationState.READY
	            || state == SimulationState.PAUSED
	            || state == SimulationState.TERMINATED
	            || state == SimulationState.FAILED);

//	    serverSlider.setEnabled(editable);
//	    clientSlider.setEnabled(editable);
//	    printerSlider.setEnabled(editable);

	    updateResetEnabled();
	}

// Enable Reset only when in editable state
private void updateResetEnabled() {
    if (host == null) {
        resetButton.setEnabled(false);
        return;
    }
    SimulationState state = host.getSimulationState();
    boolean editable = (state == SimulationState.READY
            || state == SimulationState.PAUSED
            || state == SimulationState.TERMINATED
            || state == SimulationState.FAILED);

    resetButton.setEnabled(editable);
}
	// Request reset from host if it implements ITspDemoResettable
	private void requestResetFromHost() {
		if (host == null) {
			return;
		}

		// The host is the view. We only require it implement ITspDemoResettable.
		if (host instanceof IResettable resettable) {
			
//			int servers = serverSlider.getValue();
//			int clients = clientSlider.getValue();
//			int printers = printerSlider.getValue();

			// Reset should happen on EDT (button already on EDT), but be defensive.
//			if (SwingUtilities.isEventDispatchThread()) {
//				 resettable.requestReset(servers, clients, printers);
//			} else {
//				 SwingUtilities.invokeLater(() -> resettable.requestReset(servers, clients, printers));
//			}

			updateResetEnabled();
		}
	}
}
