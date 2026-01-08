package edu.cnu.mdi.sim.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import edu.cnu.mdi.sim.ISimulationHost;
import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationListener;
import edu.cnu.mdi.sim.SimulationState;

/**
 * A reusable Swing control panel for driving a simulation hosted by an {@link ISimulationHost}.
 * <p>
 * This panel is MDI-agnostic and can be embedded anywhere (views, dialogs, side panels).
 * It binds to a host and listens to the host engine via {@link SimulationListener} events,
 * updating:
 * <ul>
 *   <li>buttons enabled/disabled based on state</li>
 *   <li>progress bar (indeterminate or determinate)</li>
 *   <li>status and message labels</li>
 * </ul>
 * </p>
 *
 * <h2>Binding</h2>
 * <p>
 * Call {@link #bind(ISimulationHost)} once after creating the panel.
 * </p>
 */
@SuppressWarnings("serial")
public class SimulationControlPanel extends JPanel implements SimulationListener {

    private ISimulationHost host;

    private final JLabel statusLabel = new JLabel("State: NEW");
    private final JLabel messageLabel = new JLabel(" ");

    private final JProgressBar progressBar = new JProgressBar(0, 100);

    private final JButton startBtn = new JButton("Start");
    private final JButton runBtn = new JButton("Run");
    private final JButton pauseBtn = new JButton("Pause");
    private final JButton resumeBtn = new JButton("Resume");
    private final JButton stopBtn = new JButton("Stop");
    private final JButton cancelBtn = new JButton("Cancel");

    /**
     * Create an unbound control panel. Call {@link #bind(ISimulationHost)} to attach it.
     */
    public SimulationControlPanel() {
        setLayout(new BorderLayout(6, 6));

        progressBar.setStringPainted(true);

        JPanel top = new JPanel(new BorderLayout());
        top.add(statusLabel, BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout());
        center.add(progressBar, BorderLayout.CENTER);
        center.add(messageLabel, BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(startBtn);
        buttons.add(runBtn);
        buttons.add(pauseBtn);
        buttons.add(resumeBtn);
        buttons.add(stopBtn);
        buttons.add(cancelBtn);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        // Default button actions (no-op until bound)
        startBtn.addActionListener(e -> { if (host != null) {
			host.startSimulation();
		} });
        runBtn.addActionListener(e -> { if (host != null) {
			host.runSimulation();
		} });
        pauseBtn.addActionListener(e -> { if (host != null) {
			host.pauseSimulation();
		} });
        resumeBtn.addActionListener(e -> { if (host != null) {
			host.resumeSimulation();
		} });
        stopBtn.addActionListener(e -> { if (host != null) {
			host.stopSimulation();
		} });
        cancelBtn.addActionListener(e -> { if (host != null) {
			host.cancelSimulation();
		} });

        // Initial state
        applyState(SimulationState.NEW, "unbound");
        setProgressIndeterminate(false, " ");
    }

    /**
     * Bind this panel to a host.
     * <p>
     * This method:
     * <ul>
     *   <li>stores the host reference</li>
     *   <li>registers this panel as a {@link SimulationListener}</li>
     *   <li>updates UI to reflect the host's current state</li>
     * </ul>
     * </p>
     *
     * @param host the simulation host (non-null)
     */
    public void bind(ISimulationHost host) {
        this.host = Objects.requireNonNull(host, "host");
        host.getSimulationEngine().addListener(this);

        // reflect current state immediately
        applyState(host.getSimulationState(), "bound");
    }

    /**
     * Unbind the panel from its host (if any).
     * <p>
     * After unbinding, the panel becomes inert until bound again.
     * </p>
     */
    public void unbind() {
        if (host != null) {
            host.getSimulationEngine().removeListener(this);
            host = null;
        }
        applyState(SimulationState.NEW, "unbound");
        setProgressIndeterminate(false, " ");
        messageLabel.setText(" ");
    }

    // ------------------------------------------------------------------------
    // SimulationListener implementation (EDT callbacks)
    // ------------------------------------------------------------------------

    @Override
    public void onStateChange(SimulationContext ctx, SimulationState from, SimulationState to, String reason) {
        applyState(to, reason);
    }

    @Override
    public void onRun(SimulationContext ctx) {
        messageLabel.setText("Running…");
        setProgressIndeterminate(true, "Running…");
    }

    @Override
    public void onResume(SimulationContext ctx) {
        messageLabel.setText("Running…");
        setProgressIndeterminate(true, "Running…");
    }

    @Override
    public void onPause(SimulationContext ctx) {
        messageLabel.setText("Paused.");
        setProgressIndeterminate(false, "Paused");
    }

    @Override
    public void onDone(SimulationContext ctx) {
        messageLabel.setText("Done.");
        setProgressIndeterminate(false, "Done");
    }

    @Override
    public void onFail(SimulationContext ctx, Throwable error) {
        String msg = (error == null) ? "FAILED" : ("FAILED: " + error.getClass().getSimpleName() +
                (error.getMessage() == null ? "" : (": " + error.getMessage())));
        messageLabel.setText(msg);
        setProgressIndeterminate(false, "Failed");
    }

    @Override
    public void onCancelRequested(SimulationContext ctx) {
        messageLabel.setText("Cancel requested…");
        setProgressIndeterminate(true, "Canceling…");
    }

    @Override
    public void onMessage(SimulationContext ctx, String message) {
        if (message != null && !message.isBlank()) {
            messageLabel.setText(message);
        }
    }

    @Override
    public void onProgress(SimulationContext ctx, ProgressInfo progress) {
        if (progress == null) {
            return;
        }
        if (progress.indeterminate) {
            setProgressIndeterminate(true, progress.message == null ? "Working…" : progress.message);
        } else {
            progressBar.setIndeterminate(false);
            int val = (int) Math.round(100.0 * progress.fraction);
            progressBar.setValue(val);
            progressBar.setString(progress.message == null ? (val + "%") : progress.message);
        }
    }

    // ------------------------------------------------------------------------
    // UI helpers
    // ------------------------------------------------------------------------

    private void applyState(SimulationState state, String reason) {
        statusLabel.setText("State: " + state + (reason == null || reason.isBlank() ? "" : ("  (" + reason + ")")));

        boolean bound = (host != null);

        // Button enable rules:
        // - Start is useful anytime before a thread exists; engine.start() is idempotent.
        // - Run is useful when READY (autoRun false) or PAUSED.
        // - Pause only when RUNNING.
        // - Resume only when PAUSED.
        // - Stop/cancel when RUNNING/PAUSED/READY/INITIALIZING/SWITCHING.
        startBtn.setEnabled(bound && state != SimulationState.TERMINATED && state != SimulationState.FAILED);

        runBtn.setEnabled(bound && (state == SimulationState.READY || state == SimulationState.PAUSED));

        pauseBtn.setEnabled(bound && state == SimulationState.RUNNING);

        resumeBtn.setEnabled(bound && state == SimulationState.PAUSED);

        boolean canStopOrCancel = bound && (state == SimulationState.INITIALIZING
                || state == SimulationState.READY
                || state == SimulationState.RUNNING
                || state == SimulationState.PAUSED
                || state == SimulationState.SWITCHING);

        stopBtn.setEnabled(canStopOrCancel);
        cancelBtn.setEnabled(canStopOrCancel);
    }

    private void setProgressIndeterminate(boolean indeterminate, String text) {
        progressBar.setIndeterminate(indeterminate);
        if (!indeterminate) {
            // keep the current value, just change string
            progressBar.setString(text == null ? "" : text);
        } else {
            progressBar.setString(text == null ? "Working…" : text);
        }
    }
}
