package edu.cnu.mdi.sim.simanneal.tspdemo;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import edu.cnu.mdi.sim.ISimulationHost;
import edu.cnu.mdi.sim.SimulationState;
import edu.cnu.mdi.sim.ui.ISimulationControlPanel;
import edu.cnu.mdi.sim.ui.IconSimulationControlPanel;

public final class TspDemoControlPanel extends JPanel
implements ISimulationControlPanel {

private ISimulationHost host;
private final TspResetHandler resetHandler;

private final JSlider citySlider;
private final JSlider riverSlider;
private final JButton resetButton;

public TspDemoControlPanel(
    IconSimulationControlPanel runPanel,
    TspResetHandler resetHandler
) {
this.resetHandler = resetHandler;
setLayout(new BorderLayout());

// Reuse standard run controls
add(runPanel, BorderLayout.WEST);

// -------- Parameters --------
JPanel params = new JPanel();
params.setLayout(new GridLayout(2, 2, 4, 2));

citySlider = new JSlider(10, 500, 60);
citySlider.setMajorTickSpacing(100);
citySlider.setMinorTickSpacing(10);
citySlider.setPaintTicks(true);
citySlider.setPaintLabels(true);

riverSlider = new JSlider(-100, 100, 35);
riverSlider.setMajorTickSpacing(50);
riverSlider.setMinorTickSpacing(10);
riverSlider.setPaintTicks(true);
riverSlider.setPaintLabels(true);

params.add(new JLabel("Cities"));
params.add(citySlider);
params.add(new JLabel("River penalty"));
params.add(riverSlider);

add(params, BorderLayout.CENTER);

// -------- Reset --------
resetButton = new JButton("Reset");
resetButton.addActionListener(e -> fireReset());
add(resetButton, BorderLayout.EAST);

setControlsEnabled(false);
}

@Override
public void bind(ISimulationHost host) {
this.host = host;
updateEnabled();
}

private void fireReset() {
resetHandler.resetRequested(
        citySlider.getValue(),
        riverSlider.getValue() / 100.0
);
}

public void updateEnabled() {
if (host == null) return;
var state = host.getSimulationEngine().getState();
boolean enable = (state == SimulationState.READY ||
                  state == SimulationState.TERMINATED);
setControlsEnabled(enable);
}

private void setControlsEnabled(boolean enabled) {
citySlider.setEnabled(enabled);
riverSlider.setEnabled(enabled);
resetButton.setEnabled(enabled);
}
}
