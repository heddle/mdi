package edu.cnu.mdi.sim.demo.forestfire;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.sim.ISimulationHost;
import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationListener;
import edu.cnu.mdi.sim.SimulationState;
import edu.cnu.mdi.sim.ui.IconSimulationControlPanel;
import edu.cnu.mdi.sim.ui.ISimulationControlPanel;
import edu.cnu.mdi.sim.ui.StandardSimIcons;

@SuppressWarnings("serial")
public class ForestFireControlPanel extends JPanel implements ISimulationControlPanel, SimulationListener {

    private static final int MIN_P = 0;    // 0.00
    private static final int MAX_P = 100;  // 1.00

    private final IconSimulationControlPanel basePanel;

    private final JSlider pSpreadSlider = new JSlider(MIN_P, MAX_P, 60);
    private final JLabel pLabel = new JLabel();

    private JButton resetButton;

    private ISimulationHost host;

    public ForestFireControlPanel() {
        this(new StandardSimIcons());
    }

    public ForestFireControlPanel(StandardSimIcons icons) {
        super(new BorderLayout(8, 0));
        Objects.requireNonNull(icons, "icons");

        basePanel = new IconSimulationControlPanel(icons, false);
        add(basePanel, BorderLayout.WEST);

        addParamPanel();
        addResetButton();
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        updatePLabel();
        pSpreadSlider.addChangeListener(e -> updatePLabel());
    }

    private void addParamPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.add(new JLabel("pSpread:"));
        pSpreadSlider.setPaintTicks(true);
        pSpreadSlider.setMajorTickSpacing(20);
        pSpreadSlider.setMinorTickSpacing(5);
        p.add(pSpreadSlider);
        p.add(pLabel);
        add(p, BorderLayout.CENTER);
    }

    private void addResetButton() {
        resetButton = new JButton("Reset");
        resetButton.setEnabled(false);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        btnPanel.add(resetButton);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));

        resetButton.addActionListener(e -> requestResetFromHost());
        add(btnPanel, BorderLayout.EAST);
    }

    private void updatePLabel() {
        double p = pSpreadSlider.getValue() / 100.0;
        pLabel.setText(String.format("%.2f", p));
    }

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

    private void applyState(SimulationState state) {
        boolean editable = (state == SimulationState.READY
                || state == SimulationState.PAUSED
                || state == SimulationState.TERMINATED
                || state == SimulationState.FAILED);

        pSpreadSlider.setEnabled(editable);
        resetButton.setEnabled(editable && host != null);
    }

    private void requestResetFromHost() {
        if (host == null) {
            return;
        }
        if (!(host instanceof IForestFireResettable resettable)) {
            return;
        }

        double pSpread = pSpreadSlider.getValue() / 100.0;

        // Keep dims fixed for now (view can define defaults).
        int w = ForestFireDemoView.DEFAULT_GRID_WIDTH;
        int h = ForestFireDemoView.DEFAULT_GRID_HEIGHT;

        if (SwingUtilities.isEventDispatchThread()) {
            resettable.requestReset(w, h, pSpread);
        } else {
            SwingUtilities.invokeLater(() -> resettable.requestReset(w, h, pSpread));
        }
    }
}
