package edu.cnu.mdi.sim.demo.network;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.graphics.SliderFactory;
import edu.cnu.mdi.sim.ISimulationHost;
import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationListener;
import edu.cnu.mdi.sim.SimulationState;
import edu.cnu.mdi.sim.ui.ISimulationControlPanel;
import edu.cnu.mdi.sim.ui.IconSimulationControlPanel;
import edu.cnu.mdi.sim.ui.StandardSimIcons;
import edu.cnu.mdi.ui.fonts.Fonts;

@SuppressWarnings("serial")
public class NetworkDeclutterControlPanel extends JPanel implements ISimulationControlPanel, SimulationListener {
	// Server slider parameters
	private static final int MIN_SERVERS = 5;
	private static final int MAX_SERVERS = 45;
	
	// Client slider parameters
	private static final int MIN_CLIENTS = 10;
	private static final int MAX_CLIENTS = 210;
	
	// Printer slider parameters
	private static final int MIN_PRINTERS = 0;
	private static final int MAX_PRINTERS = 20;
	
	// Base panel with buttons + progress
	private final IconSimulationControlPanel basePanel;
	
	// Sliders
	private final JSlider serverSlider;
	private final JSlider clientSlider;
	private final JSlider printerSlider;


	// Reset button resets the demo with current parameters
	private JButton resetButton;

	// the host is typically a subclass of SimulationView
	private ISimulationHost host;

	/**
	 * Construct a new TSP demo control panel with standard icons.
	 */
	public NetworkDeclutterControlPanel() {
		this(new StandardSimIcons());
	}

	/**
	 * Construct a new TSP demo control panel using the provided icon set.
	 *
	 * @param icons simulation icons (non-null)
	 */
	public NetworkDeclutterControlPanel(StandardSimIcons icons) {
		super(new BorderLayout(8, 0));
		Objects.requireNonNull(icons, "icons");

		// Base panel (left) has the standard media icons
		basePanel = new IconSimulationControlPanel(icons, false);
		add(basePanel, BorderLayout.WEST);

		// Reset button (right)
		addResetButton();

		// add custom content
		// -------- Sliders panel (center) --------
		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new BorderLayout(0,0));
		sliderPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

		JPanel sPanel = createCommonPanel("Number Servers");
		JPanel cPanel = createCommonPanel("Number Clients");
		JPanel pPanel = createCommonPanel("Number Printers");

		

		Font font = Fonts.tinyFont;

		int tickSpace = (MAX_SERVERS - MIN_SERVERS) / 5;
		serverSlider = SliderFactory.createLabeledSlider(sPanel, MIN_SERVERS, MAX_SERVERS, 
				NetworkDeclutterDemoView.DEFAULT_NUM_SERVERS, tickSpace, 0, font, true);
		
		tickSpace = (MAX_CLIENTS - MIN_CLIENTS) / 5;
		clientSlider  = SliderFactory.createLabeledSlider(cPanel, MIN_CLIENTS, MAX_CLIENTS, 
				NetworkDeclutterDemoView.DEFAULT_NUM_CLIENTS, tickSpace, 0, font, true);
		
		tickSpace = (MAX_PRINTERS - MIN_PRINTERS) / 5;
		printerSlider  = SliderFactory.createLabeledSlider(pPanel, MIN_PRINTERS, MAX_PRINTERS, 
				NetworkDeclutterDemoView.DEFAULT_NUM_PRINTERS, tickSpace, 0, font, true);

		JPanel rows = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		rows.add(sPanel);
		rows.add(cPanel);
		rows.add(pPanel);

		sliderPanel.add(rows, BorderLayout.CENTER);
		add(sliderPanel, BorderLayout.CENTER);

		setBorder(BorderFactory.createEtchedBorder());
	}


	// create a panel with common border
	private JPanel createCommonPanel(String title) {
		JPanel panel = new JPanel();
		panel.setBorder(new CommonBorder(title));
		return panel;
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
	public void bind(ISimulationHost host) {
		this.host = Objects.requireNonNull(host, "host");

		// Bind the base panel (buttons + progress) to the same host.
		basePanel.bind(host);

		// Listen for state changes so we can enable/disable our controls.
		host.getSimulationEngine().addListener(this);

		// Apply current state immediately.
		applyState(host.getSimulationState());
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

	    serverSlider.setEnabled(editable);
	    clientSlider.setEnabled(editable);
	    printerSlider.setEnabled(editable);

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
			
			int servers = serverSlider.getValue();
			int clients = clientSlider.getValue();
			int printers = printerSlider.getValue();

			// Reset should happen on EDT (button already on EDT), but be defensive.
			if (SwingUtilities.isEventDispatchThread()) {
				 resettable.requestReset(servers, clients, printers);
			} else {
				 SwingUtilities.invokeLater(() -> resettable.requestReset(servers, clients, printers));
			}

			updateResetEnabled();
		}
	}

}
