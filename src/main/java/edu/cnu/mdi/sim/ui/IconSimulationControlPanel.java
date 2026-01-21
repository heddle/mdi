package edu.cnu.mdi.sim.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.lang.reflect.Constructor;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;

import edu.cnu.mdi.sim.ISimulationHost;
import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationListener;
import edu.cnu.mdi.sim.SimulationState;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * A more graphical variant of {@link SimulationControlPanel} that prefers icons and
 * supports either an indeterminate "busy" graphic (when available) or an indeterminate
 * progress bar fallback.
 *
 * <p>MDI-agnostic: icons are supplied by an {@link IconProvider}.</p>
 */
@SuppressWarnings("serial")
public class IconSimulationControlPanel extends JPanel implements SimulationListener, ISimulationControlPanel {

	public interface IconProvider {
		Icon start();
		Icon run();
		Icon pause();
		Icon resume();
		Icon stop();
		Icon cancel();
	}

	private ISimulationHost host;
	private final IconProvider icons;

	private final JLabel statusLabel = new JLabel("State: NEW");
	private final JLabel messageLabel = new JLabel(" ");

	private final JProgressBar progressBar = new JProgressBar(0, 100);

	// Optional busy indicator component (spinner-style) if available
	private final JComponent busyIndicator;

	private final JButton startBtn;
	private final JButton runBtn;
	private final JButton pauseBtn;
	private final JButton resumeBtn;
	private final JButton stopBtn;
	private final JButton cancelBtn;

	public IconSimulationControlPanel(IconProvider icons) {
		this.icons = Objects.requireNonNull(icons, "icons");

		statusLabel.setFont(Fonts.smallFont);
		messageLabel.setFont(Fonts.tinyFont);
		setLayout(new BorderLayout(6, 6));

		// Top: status
		JPanel top = new JPanel(new BorderLayout());
		top.add(statusLabel, BorderLayout.WEST);

		// Center: progress + message
		progressBar.setStringPainted(true);

		busyIndicator = tryCreateFlatBusyLabel(); // may be null

		JPanel center = new JPanel(new BorderLayout(6, 2));
		center.add(progressBar, BorderLayout.CENTER);
		center.add(messageLabel, BorderLayout.SOUTH);

		// Bottom: toolbar with icons
		JToolBar tb = new JToolBar();
		tb.setFloatable(false);

		startBtn  = toolButton(icons.start(),  "Start (initialize)");
		runBtn    = toolButton(icons.run(),    "Run");
		pauseBtn  = toolButton(icons.pause(),  "Pause");
		resumeBtn = toolButton(icons.resume(), "Resume");
		stopBtn   = toolButton(icons.stop(),   "Stop");
		cancelBtn = toolButton(icons.cancel(), "Cancel");

		tb.add(startBtn);
		tb.add(runBtn);
		tb.addSeparator();
		tb.add(pauseBtn);
		tb.add(resumeBtn);
		tb.addSeparator();
		tb.add(stopBtn);
		tb.add(cancelBtn);

		// Optional: put spinner to the right of the toolbar if available
		JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(tb, BorderLayout.WEST);
		if (busyIndicator != null) {
			JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
			right.add(busyIndicator);
			bottom.add(right, BorderLayout.EAST);
			busyIndicator.setVisible(false);
		}

		add(top, BorderLayout.NORTH);
		add(center, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);

		// Actions (no-op until bound)
		startBtn.addActionListener(e -> { if (host != null) host.startSimulation(); });
		runBtn.addActionListener(e -> { if (host != null) host.runSimulation(); });
		pauseBtn.addActionListener(e -> { if (host != null) host.pauseSimulation(); });
		resumeBtn.addActionListener(e -> { if (host != null) host.resumeSimulation(); });
		stopBtn.addActionListener(e -> { if (host != null) host.stopSimulation(); });
		cancelBtn.addActionListener(e -> { if (host != null) host.cancelSimulation(); });

		applyState(SimulationState.NEW, "unbound");
		setIndeterminate(false, " ");
		
		Dimension size = getPreferredSize();
		size.width = 300;
		setPreferredSize(size);
	}

	public void bind(ISimulationHost host) {
		this.host = Objects.requireNonNull(host, "host");
		host.getSimulationEngine().addListener(this);
		applyState(host.getSimulationState(), "bound");
	}

	public void unbind() {
		if (host != null) {
			host.getSimulationEngine().removeListener(this);
			host = null;
		}
		applyState(SimulationState.NEW, "unbound");
		setIndeterminate(false, " ");
		messageLabel.setText(" ");
	}

	// ------------------------------------------------------------------------
	// SimulationListener
	// ------------------------------------------------------------------------

	@Override
	public void onStateChange(SimulationContext ctx, SimulationState from, SimulationState to, String reason) {
		applyState(to, reason);
	}

	@Override
	public void onRun(SimulationContext ctx) {
		messageLabel.setText("Running…");
		setIndeterminate(true, "Running…");
	}

	@Override
	public void onResume(SimulationContext ctx) {
		messageLabel.setText("Running…");
		setIndeterminate(true, "Running…");
	}

	@Override
	public void onPause(SimulationContext ctx) {
		messageLabel.setText("Paused.");
		setIndeterminate(false, "Paused");
	}

	@Override
	public void onDone(SimulationContext ctx) {
		messageLabel.setText("Done.");
		setIndeterminate(false, "Done");
	}

	@Override
	public void onFail(SimulationContext ctx, Throwable error) {
		String msg = (error == null) ? "FAILED"
				: ("FAILED: " + error.getClass().getSimpleName()
					+ (error.getMessage() == null ? "" : (": " + error.getMessage())));
		messageLabel.setText(msg);
		setIndeterminate(false, "Failed");
	}

	@Override
	public void onCancelRequested(SimulationContext ctx) {
		messageLabel.setText("Cancel requested…");
		setIndeterminate(true, "Canceling…");
	}

	@Override
	public void onMessage(SimulationContext ctx, String message) {
		if (message != null && !message.isBlank()) {
			messageLabel.setText(message);
		}
	}

	@Override
	public void onProgress(SimulationContext ctx, ProgressInfo progress) {
		if (progress == null) return;

		if (progress.indeterminate) {
			setIndeterminate(true, progress.message == null ? "Working…" : progress.message);
		}
		else {
			showBusy(false);
			progressBar.setIndeterminate(false);
			int val = (int) Math.round(100.0 * progress.fraction);
			progressBar.setValue(val);
			progressBar.setString(progress.message == null ? (val + "%") : progress.message);
		}
	}

	@Override
	public void onRefresh(SimulationContext ctx) {
		// no-op (panel doesn't repaint the simulation view)
	}

	// ------------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------------

	private JButton toolButton(Icon icon, String tooltip) {
		JButton b = new JButton(icon);
		b.setToolTipText(tooltip);
		b.setFocusable(false);
		return b;
	}

	private void applyState(SimulationState state, String reason) {
		statusLabel.setText("State: " + state + (reason == null || reason.isBlank() ? "" : ("  (" + reason + ")")));

		boolean bound = (host != null);

		startBtn.setEnabled(bound && state != SimulationState.TERMINATED && state != SimulationState.FAILED);
		runBtn.setEnabled(bound && (state == SimulationState.READY || state == SimulationState.PAUSED));
		pauseBtn.setEnabled(bound && state == SimulationState.RUNNING);
		resumeBtn.setEnabled(bound && state == SimulationState.PAUSED);

		boolean canStopOrCancel = bound && (state == SimulationState.INITIALIZING || state == SimulationState.READY
				|| state == SimulationState.RUNNING || state == SimulationState.PAUSED
				|| state == SimulationState.SWITCHING);

		stopBtn.setEnabled(canStopOrCancel);
		cancelBtn.setEnabled(canStopOrCancel);
	}

	private void setIndeterminate(boolean indeterminate, String text) {
		if (indeterminate) {
			showBusy(true);
			progressBar.setIndeterminate(busyIndicator == null); // if we have a spinner, keep bar calm
			progressBar.setString(text == null ? "Working…" : text);
		} else {
			showBusy(false);
			progressBar.setIndeterminate(false);
			progressBar.setString(text == null ? "" : text);
		}
	}

	private void showBusy(boolean on) {
		if (busyIndicator != null) {
			busyIndicator.setVisible(on);
		}
	}

	/**
	 * If FlatLaf Extras is on the classpath, use FlatBusyLabel as a spinner/busy indicator.
	 * Otherwise return null and we fall back to JProgressBar indeterminate.
	 */
	private static JComponent tryCreateFlatBusyLabel() {
		try {
			Class<?> c = Class.forName("com.formdev.flatlaf.extras.components.FlatBusyLabel");
			Constructor<?> ctor = c.getConstructor();
			Object inst = ctor.newInstance();
			if (inst instanceof JComponent jc) {
				jc.setName("busyIndicator");
				jc.setAlignmentY(Component.CENTER_ALIGNMENT);
				return jc;
			}
		} catch (Throwable ignored) {
			// FlatLaf extras not present (or any other failure) => no spinner
		}
		return null;
	}
}
