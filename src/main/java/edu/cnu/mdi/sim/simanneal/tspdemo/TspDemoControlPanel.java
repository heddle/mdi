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
 *
 * <h2>Structure</h2>
 * <p>
 * Follows the standard MDI composite control panel pattern:
 * </p>
 * <ul>
 *   <li><b>Left</b> — {@link IconSimulationControlPanel} providing the
 *       standard play/pause/stop media buttons and progress bar.</li>
 *   <li><b>Center</b> — two labeled sliders:
 *       <ul>
 *         <li><em>Number Cities</em> ({@link #MIN_CITIES}..{@link #MAX_CITIES})
 *             — number of randomly placed cities.</li>
 *         <li><em>River Crossing Penalty or Bonus</em> (−1..+1) — adds a
 *             constant to the distance of any edge that crosses the river.
 *             Positive values penalize crossings (the tour avoids the river);
 *             negative values reward them (the tour seeks crossings).</li>
 *       </ul>
 *   </li>
 *   <li><b>Right</b> — Reset button that rebuilds the model and simulation
 *       from the current slider values.</li>
 * </ul>
 *
 * <h2>State gating</h2>
 * <p>
 * The sliders and Reset button are only enabled when the engine is in
 * {@link SimulationState#READY} or {@link SimulationState#TERMINATED}. This
 * prevents the user from changing parameters while the simulation thread is
 * actively mutating the model.
 * </p>
 *
 * <h2>Reset behavior</h2>
 * <p>
 * Pressing Reset calls {@link ITspDemoResettable#requestReset} on the host if
 * it implements that interface, passing the current slider values. The host
 * ({@link TspDemoView}) is responsible for stopping the current engine,
 * building a new simulation with the new parameters, and restarting.
 * </p>
 * <p>
 * An optional auto-reset mode ({@link #autoResetOnSliderRelease}) can trigger
 * a reset automatically when the user releases a slider knob in an editable
 * state. It is disabled by default.
 * </p>
 *
 * <h2>River slider precision</h2>
 * <p>
 * {@link JSlider} operates on integers, so the river penalty slider is scaled
 * by {@link #RIVER_SCALE} ({@code 10^}{@link #RIVER_NUM_DECIMALS}) internally
 * and divided back to a {@code double} in {@link #getRiverPenalty()}.
 * </p>
 */
@SuppressWarnings("serial")
public class TspDemoControlPanel extends JPanel implements ISimulationControlPanel, SimulationListener {

    /** Minimum number of cities available on the city slider. */
    public static final int MIN_CITIES = 10;

    /** Maximum number of cities available on the city slider. */
    public static final int MAX_CITIES = 1000;

    /**
     * Number of decimal places retained in the river penalty slider.
     * The slider integer value is divided by {@link #RIVER_SCALE} to recover
     * the {@code double} penalty.
     */
    private static final int RIVER_NUM_DECIMALS = 2;

    /** Scale factor for the river penalty slider ({@code 10^RIVER_NUM_DECIMALS}). */
    private static final int RIVER_SCALE = (int) Math.pow(10, RIVER_NUM_DECIMALS);

    /** Standard media-button panel (play/pause/stop/cancel + progress bar). */
    private final IconSimulationControlPanel basePanel;

    /** Slider controlling the number of cities in the next problem. */
    private final JSlider citySlider;

    /**
     * Slider controlling the river crossing penalty or bonus.
     * Range: −1.00 (maximum bonus) to +1.00 (maximum penalty).
     */
    private final JSlider riverSlider;

    /** Button that triggers a full model/simulation reset. */
    private JButton resetButton;

    /** The simulation host this panel is bound to (typically {@link TspDemoView}). */
    private ISimulationHost host;

    /**
     * True if the slider values have changed since the last reset.
     * Used to decide whether Reset should be enabled.
     * All reads and writes occur on the EDT so no synchronization is needed.
     */
    private boolean dirty;

    /**
     * If {@code true}, the panel automatically requests a reset when the user
     * releases a slider knob ({@code getValueIsAdjusting() == false}) while the
     * engine is in an editable state.
     * <p>
     * Disabled by default. Enable if you want the demo to reinitialize
     * immediately whenever a slider value changes, without requiring an explicit
     * Reset button press.
     * </p>
     */
    private final boolean autoResetOnSliderRelease = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Construct with the standard MDI icon set.
     */
    public TspDemoControlPanel() {
        this(new StandardSimIcons());
    }

    /**
     * Construct with a caller-supplied icon set.
     *
     * @param icons simulation icons (non-null)
     */
    public TspDemoControlPanel(StandardSimIcons icons) {
        super(new BorderLayout(8, 0));
        Objects.requireNonNull(icons, "icons");

        basePanel = new IconSimulationControlPanel(icons, false);
        add(basePanel, BorderLayout.WEST);

        addResetButton();

        // ── Sliders (center) ─────────────────────────────────────────────────
        JPanel sliderPanel = new JPanel(new BorderLayout(0, 0));
        sliderPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JPanel cPanel = new JPanel();
        cPanel.setBorder(new CommonBorder("Number Cities"));

        JPanel rPanel = new JPanel();
        rPanel.setBorder(new CommonBorder("River Crossing Penalty or Bonus"));

        Font font = Fonts.tinyFont;

        int tickSpace = (MAX_CITIES - MIN_CITIES) / 5;
        citySlider = SliderFactory.createLabeledSlider(
                cPanel, MIN_CITIES, MAX_CITIES,
                TspDemoView.DEFAULT_NUM_CITY, tickSpace, 0, font, true);

        riverSlider = SliderFactory.createLabeledSlider(
                rPanel, -1f, 1f,
                TspDemoView.DEFAULT_RIVER_PENALTY, 0.4f, 0f, font, true, RIVER_NUM_DECIMALS);

        JPanel rows = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rows.add(cPanel);
        rows.add(rPanel);
        sliderPanel.add(rows, BorderLayout.CENTER);
        add(sliderPanel, BorderLayout.CENTER);

        // ── Slider change listener ────────────────────────────────────────────
        ChangeListener cl = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                dirty = true;
                updateResetEnabled();

                if (autoResetOnSliderRelease) {
                    boolean adjusting = citySlider.getValueIsAdjusting()
                                     || riverSlider.getValueIsAdjusting();
                    if (!adjusting) {
                        maybeAutoReset();
                    }
                }
            }
        };
        citySlider.addChangeListener(cl);
        riverSlider.addChangeListener(cl);

        setBorder(BorderFactory.createEtchedBorder());
    }

    // -------------------------------------------------------------------------
    // Private layout helpers
    // -------------------------------------------------------------------------

    /**
     * Build and add the Reset button to the EAST slot.
     * The button starts disabled; it is enabled by {@link #updateResetEnabled()}
     * when the engine is in an editable state.
     */
    private void addResetButton() {
        resetButton = new JButton("Reset");
        resetButton.setEnabled(false);
        resetButton.addActionListener(e -> requestResetFromHost());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        btnPanel.add(resetButton);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        add(btnPanel, BorderLayout.EAST);
    }

    // -------------------------------------------------------------------------
    // ISimulationControlPanel
    // -------------------------------------------------------------------------

    /**
     * Bind this panel to the given simulation host.
     * <p>
     * Binds the embedded {@link #basePanel} to the same host, registers this
     * panel as a {@link SimulationListener}, and applies the current engine
     * state immediately.
     * </p>
     *
     * @param host the simulation host (non-null; typically {@link TspDemoView})
     */
    @Override
    public void bind(ISimulationHost host) {
        this.host = Objects.requireNonNull(host, "host");
        basePanel.bind(host);
        host.getSimulationEngine().addListener(this);
        applyState(host.getSimulationState());
    }

    /**
     * Unbind this panel from its host.
     * <p>
     * Removes the listener from the engine and unbinds the embedded
     * {@link #basePanel}. The panel becomes inert until bound again.
     * </p>
     */
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

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Return the city count currently selected on the slider.
     *
     * @return number of cities in the range [{@link #MIN_CITIES},
     *         {@link #MAX_CITIES}]
     */
    public int getCityCount() {
        return citySlider.getValue();
    }

    /**
     * Return the river penalty currently selected on the slider.
     *
     * @return penalty in {@code [−1.0, +1.0]}; positive values penalize river
     *         crossings, negative values reward them
     */
    public double getRiverPenalty() {
        return riverSlider.getValue() / (double) RIVER_SCALE;
    }

    // -------------------------------------------------------------------------
    // SimulationListener — state gating
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void onStateChange(SimulationContext ctx, SimulationState from,
                               SimulationState to, String reason) {
        applyState(to);
    }

    /** {@inheritDoc} */
    @Override
    public void onReady(SimulationContext ctx) {
        applyState(SimulationState.READY);
    }

    /** {@inheritDoc} */
    @Override
    public void onDone(SimulationContext ctx) {
        applyState(SimulationState.TERMINATED);
    }

    /** {@inheritDoc} */
    @Override
    public void onFail(SimulationContext ctx, Throwable error) {
        applyState(SimulationState.FAILED);
    }

    /**
     * {@inheritDoc}
     * No-op — progress display is handled by the embedded {@link #basePanel}.
     */
    @Override
    public void onProgress(SimulationContext ctx, ProgressInfo progress) {
        // no-op
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Enable or disable controls based on the current simulation state.
     * <p>
     * Sliders and Reset are enabled only in {@link SimulationState#READY} or
     * {@link SimulationState#TERMINATED} to prevent parameter changes while
     * the simulation thread is running or paused.
     * </p>
     *
     * @param state current simulation state
     */
    private void applyState(SimulationState state) {
        boolean editable = (state == SimulationState.READY
                         || state == SimulationState.TERMINATED);
        citySlider.setEnabled(editable);
        riverSlider.setEnabled(editable);
        updateResetEnabled();
    }

    /**
     * Update the Reset button's enabled state.
     * <p>
     * Reset is enabled whenever the engine is in an editable state, regardless
     * of whether the sliders are dirty. This allows the user to reset to the
     * current parameters (generating a new random city layout) without changing
     * any values.
     * </p>
     */
    private void updateResetEnabled() {
        if (host == null) {
            resetButton.setEnabled(false);
            return;
        }
        SimulationState state = host.getSimulationState();
        boolean editable = (state == SimulationState.READY
                         || state == SimulationState.TERMINATED);
        resetButton.setEnabled(editable);
    }

    /**
     * If the engine is in an editable state and the sliders are dirty,
     * automatically request a reset.
     * <p>
     * Only called when {@link #autoResetOnSliderRelease} is {@code true}.
     * </p>
     */
    private void maybeAutoReset() {
        if (host == null || !dirty) return;

        SimulationState state = host.getSimulationState();
        boolean editable = (state == SimulationState.READY
                         || state == SimulationState.TERMINATED);
        if (editable) {
            requestResetFromHost();
        }
    }

    /**
     * Read the current slider values and forward a reset request to the host
     * if it implements {@link ITspDemoResettable}.
     * <p>
     * Called on the EDT (button action listener), but defensively wraps in
     * {@link SwingUtilities#invokeLater} if somehow called off-EDT.
     * </p>
     */
    private void requestResetFromHost() {
        if (host == null) return;

        if (host instanceof ITspDemoResettable resettable) {
            int cities      = getCityCount();
            double penalty  = getRiverPenalty();

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