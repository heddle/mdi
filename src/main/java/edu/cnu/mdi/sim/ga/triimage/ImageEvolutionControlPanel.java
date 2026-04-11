package edu.cnu.mdi.sim.ga.triimage;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.graphics.SliderFactory;
import edu.cnu.mdi.sim.ISimulationHost;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationListener;
import edu.cnu.mdi.sim.SimulationState;
import edu.cnu.mdi.sim.ui.ISimulationControlPanel;
import edu.cnu.mdi.sim.ui.IconSimulationControlPanel;
import edu.cnu.mdi.sim.ui.StandardSimIcons;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Control panel for the image evolution GA demo.
 *
 * <h2>Structure</h2>
 * <p>
 * Follows the same composite pattern as {@code NetworkDeclutterControlPanel}:
 * an {@link IconSimulationControlPanel} on the left provides the standard
 * play/pause/stop media buttons; the center contains two parameter sliders
 * (triangle count and population size); and a Reset button on the right
 * triggers a full re-run with the new parameters.
 * </p>
 *
 * <h2>State-dependent enabling</h2>
 * <p>
 * The sliders and Reset button are only enabled when the simulation is in an
 * "editable" state (READY, PAUSED, TERMINATED, or FAILED). While RUNNING the
 * sliders are disabled to prevent inconsistent parameter changes mid-run.
 * </p>
 */
@SuppressWarnings("serial")
public class ImageEvolutionControlPanel extends JPanel
        implements ISimulationControlPanel, SimulationListener {

    // -------------------------------------------------------------------------
    // Slider ranges
    // -------------------------------------------------------------------------

    private static final int MIN_TRIANGLES = 10;
    private static final int MAX_TRIANGLES = 200;

    private static final int MIN_POPULATION = 20;
    private static final int MAX_POPULATION = 300;

    // -------------------------------------------------------------------------
    // Components
    // -------------------------------------------------------------------------

    /** Standard media-button panel (play/pause/stop). */
    private final IconSimulationControlPanel basePanel;

    private final JSlider triangleSlider;
    private final JSlider populationSlider;
    private final JButton resetButton;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** The host this panel is bound to. Set in {@link #bind(ISimulationHost)}. */
    private ISimulationHost host;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Construct the panel with default (standard) icons.
     */
    public ImageEvolutionControlPanel() {
        super(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEtchedBorder());

        // ── Left: standard media buttons ─────────────────────────────────────
        basePanel = new IconSimulationControlPanel(new StandardSimIcons(), false);
        add(basePanel, BorderLayout.WEST);

        // ── Center: parameter sliders ─────────────────────────────────────────
        JPanel triPanel = new JPanel();
        triPanel.setBorder(new CommonBorder("Triangles"));

        JPanel popPanel = new JPanel();
        popPanel.setBorder(new CommonBorder("Population size"));

        int triTick = (MAX_TRIANGLES - MIN_TRIANGLES) / 5;
        triangleSlider = SliderFactory.createLabeledSlider(
                triPanel, MIN_TRIANGLES, MAX_TRIANGLES,
                ImageEvolutionDemoView.DEFAULT_NUM_TRIANGLES,
                triTick, 0, Fonts.tinyFont, true);

        int popTick = (MAX_POPULATION - MIN_POPULATION) / 5;
        populationSlider = SliderFactory.createLabeledSlider(
                popPanel, MIN_POPULATION, MAX_POPULATION,
                ImageEvolutionDemoView.DEFAULT_POPULATION_SIZE,
                popTick, 0, Fonts.tinyFont, true);

        JPanel sliders = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        sliders.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        sliders.add(triPanel);
        sliders.add(popPanel);
        add(sliders, BorderLayout.CENTER);

        // ── Right: reset button ───────────────────────────────────────────────
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

    @Override
    public void bind(ISimulationHost host) {
        this.host = Objects.requireNonNull(host, "host");
        basePanel.bind(host);
        host.getSimulationEngine().addListener(this);
        applyState(host.getSimulationState());
    }

    @Override
    public void unbind() {
        if (host != null) {
            try {
                host.getSimulationEngine().removeListener(this);
            } catch (Throwable ignored) {
                // Defensive: engine may already be stopping
            }
            basePanel.unbind();
            host = null;
        }
    }

    // -------------------------------------------------------------------------
    // SimulationListener
    // -------------------------------------------------------------------------

    @Override
    public void onStateChange(SimulationContext ctx, SimulationState from,
                               SimulationState to, String reason) {
        applyState(to);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Enable or disable controls based on the current simulation state.
     * Sliders and Reset are only editable when the simulation is not actively
     * running.
     */
    private void applyState(SimulationState state) {
        boolean editable = state == SimulationState.READY
                || state == SimulationState.PAUSED
                || state == SimulationState.TERMINATED
                || state == SimulationState.FAILED;

        triangleSlider.setEnabled(editable);
        populationSlider.setEnabled(editable);
        resetButton.setEnabled(editable && host != null);
    }

    /**
     * Read slider values and call through to the host's reset method if it
     * implements {@link IImageEvolutionResettable}.
     */
    private void requestResetFromHost() {
        if (host instanceof IImageEvolutionResettable resettable) {
            int triangles  = triangleSlider.getValue();
            int population = populationSlider.getValue();

            if (SwingUtilities.isEventDispatchThread()) {
                resettable.requestReset(triangles, population);
            } else {
                SwingUtilities.invokeLater(
                        () -> resettable.requestReset(triangles, population));
            }
        }
    }
}