package edu.cnu.mdi.sim.simanneal.tspdemo;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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


/**
 * Control panel for the TSP simulated annealing demo.
 * <p>
 * This panel composes:
 * </p>
 * <ul>
 *   <li>a standard {@link IconSimulationControlPanel} (Start/Run/Pause/Resume/Stop/Cancel + progress)</li>
 *   <li>a "Cities" slider (10..500)</li>
 *   <li>a "River penalty" slider (-1..+1)</li>
 *   <li>a Reset button</li>
 * </ul>
 *
 * <h2>State gating</h2>
 * <p>
 * The demo parameters (cities, river penalty, reset) are enabled only when the engine
 * is in {@link SimulationState#READY} or {@link SimulationState#TERMINATED}.
 * This avoids mutating the model while the simulation thread is running/paused.
 * </p>
 *
 * <h2>Reset behavior</h2>
 * <p>
 * The Reset button requests a rebuild/reinitialize from the host if the host implements
 * {@link ITspDemoResettable}. Slider changes also enable the Reset button immediately,
 * and may optionally auto-reset when the user releases the slider knob (see
 * {@link #autoResetOnSliderRelease}).
 * </p>
 */
@SuppressWarnings("serial")
public class TspDemoControlPanel extends JPanel implements ISimulationControlPanel, SimulationListener {

	/** Default min cities. */
	public static final int MIN_CITIES = 10;

	/** Default max cities. */
	public static final int MAX_CITIES = 500;
	
	//for the float based river slider
	private final static int RIVER_NUM_DECIMALS = 2;
	private final static int RIVER_SCALE = (int) Math.pow(10, RIVER_NUM_DECIMALS);

	private final IconSimulationControlPanel basePanel;

	private final JSlider citySlider;

	private final JSlider riverSlider;

	private final JButton resetButton;

	private ISimulationHost host;

	private volatile boolean dirty;

	/**
	 * If true, the panel automatically requests reset when the user releases a slider knob
	 * (i.e., when {@code getValueIsAdjusting() == false}) and the engine is in a safe state.
	 * <p>
	 * This matches "reinitialize if those values change" while still keeping an explicit
	 * Reset button.
	 * </p>
	 */
	private final boolean autoResetOnSliderRelease = false;

	/**
	 * Construct a new TSP demo control panel with standard icons.
	 */
	public TspDemoControlPanel() {
		this(new StandardSimIcons());
	}

	/**
	 * Construct a new TSP demo control panel using the provided icon set.
	 *
	 * @param icons simulation icons (non-null)
	 */
	public TspDemoControlPanel(StandardSimIcons icons) {
		super(new BorderLayout(8, 0));
		Objects.requireNonNull(icons, "icons");

		basePanel = new IconSimulationControlPanel(icons, false);

		// -------- Sliders panel (center) --------
		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new BorderLayout(0,0));
		sliderPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

		// Cities row
		JPanel cPanel = new JPanel();
		cPanel.setBorder(new CommonBorder("Number Cities"));
		JPanel rPanel = new JPanel();
		rPanel.setBorder(new CommonBorder("River Crossing Penalty or Bonus"));
		
		Font font = Fonts.tinyFont;
		
		int tickSpace = (MAX_CITIES - MIN_CITIES) / 5;
		citySlider  = SliderFactory.createLabeledSlider(cPanel, MIN_CITIES, MAX_CITIES, 
				TspDemoView.DEFAULT_NUM_CITY, tickSpace, font, true);
		
		
		float fTickSpace = 0.4f;
		riverSlider = SliderFactory.createLabeledSlider(rPanel, -1f, 1f, TspDemoView.DEFAULT_RIVER_PENALTY, fTickSpace, font, true, RIVER_NUM_DECIMALS);

		JPanel rows = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		rows.add(cPanel);
		rows.add(rPanel);

		sliderPanel.add(rows, BorderLayout.CENTER);

		// Reset button (right)
		resetButton = new JButton("Reset");
		resetButton.setEnabled(false);
		resetButton.addActionListener(e -> requestResetFromHost());

		add(basePanel, BorderLayout.WEST);
		add(sliderPanel, BorderLayout.CENTER);
		add(resetButton, BorderLayout.EAST);

		// Slider listeners
		ChangeListener cl = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {

				dirty = true;
				updateResetEnabled();

				// Optional auto-reset when user releases knob
				if (autoResetOnSliderRelease) {
					boolean adjusting =
						citySlider.getValueIsAdjusting() || riverSlider.getValueIsAdjusting();
					if (!adjusting) {
						maybeAutoReset();
					}
				}
			}
		};
		citySlider.addChangeListener(cl);
		riverSlider.addChangeListener(cl);
	}


	//make a slider
//	private JSlider makeSlider(JPanel panel, int min, int max,
//			int initial, int majorTick) {
//		panel.setLayout(new BorderLayout(0, 0));
//		JSlider slider = new JSlider(min, max, initial);
//		JLabel valueLabel = new JLabel("Value: " + slider.getValue());
//
//		slider.setPreferredSize(new Dimension(140, 38));
//		
//		slider.addChangeListener(e -> {
//		    // Updates the label in real-time as the knob moves
//		    valueLabel.setText("Value: " + slider.getValue());
//		});
//
//
//		majorTick = (max-min)/majorTick;
//		slider.setMajorTickSpacing(majorTick);
//		slider.setMinorTickSpacing(0);
//		slider.setPaintTicks(true);
//		slider.setPaintLabels(true);
//		slider.setFont(Fonts.tinyFont);
//		valueLabel.setFont(Fonts.tinyFont);
//		panel.add(slider, BorderLayout.CENTER);
//		panel.add(valueLabel, BorderLayout.NORTH);
//		return slider;
//	}

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

	/**
	 * Get the selected city count.
	 *
	 * @return city count
	 */
	public int getCityCount() {
		return citySlider.getValue();
	}

	/**
	 * Get the selected river penalty in [-1, +1].
	 *
	 * @return river penalty
	 */
	public double getRiverPenalty() {
		return riverSlider.getValue() / (double) RIVER_SCALE;
	}

	// -------------------------------------------------------------------------
	// SimulationListener: state gating
	// -------------------------------------------------------------------------

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

	private void applyState(SimulationState state) {
		boolean editable = (state == SimulationState.READY || state == SimulationState.TERMINATED);

		citySlider.setEnabled(editable);
		riverSlider.setEnabled(editable);

		updateResetEnabled();
	}

	private void updateResetEnabled() {
		if (host == null) {
			resetButton.setEnabled(false);
			return;
		}
		SimulationState state = host.getSimulationState();
		boolean editable = (state == SimulationState.READY || state == SimulationState.TERMINATED);

		// Enable Reset only when editable
		resetButton.setEnabled(editable);
	}

	private void maybeAutoReset() {
		if (host == null || !dirty) {
			return;
		}

		SimulationState state = host.getSimulationState();
		boolean editable = (state == SimulationState.READY || state == SimulationState.TERMINATED);
		if (!editable) {
			return;
		}

		requestResetFromHost();
	}

	private void requestResetFromHost() {
		if (host == null) {
			return;
		}

		// The host is the view. We only require it implement ITspDemoResettable.
		if (host instanceof ITspDemoResettable resettable) {
			int cities = getCityCount();
			double penalty = getRiverPenalty();

			// Reset should happen on EDT (button already on EDT), but be defensive.
			if (SwingUtilities.isEventDispatchThread()) {
				resettable.requestReset(cities, penalty);
			} else {
				SwingUtilities.invokeLater(() -> resettable.requestReset(cities, penalty));
			}

			dirty = false;
			updateResetEnabled();
		}
	}

}
