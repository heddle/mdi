package edu.cnu.mdi.sim.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;

import edu.cnu.mdi.sim.CompletionStatus;
import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.task.TaskHandle;
import edu.cnu.mdi.sim.task.TaskListener;

/**
 * Compact Swing controls for a one-shot {@link TaskHandle}.
 *
 * <p>The panel presents Start and Cancel buttons, a determinate or
 * indeterminate progress bar, status text, and elapsed worker time. It is
 * intentionally smaller than {@link SimulationControlPanel}: a one-shot task
 * has no meaningful pause, resume, or repeated-step controls.</p>
 *
 * <h2>Threading</h2>
 * <p>{@code TaskHandle} listener callbacks are delivered on Swing's EDT, so
 * this panel updates its components directly. A Swing timer updates elapsed
 * time four times per second while work is active. Binding and unbinding
 * should also be performed on the EDT, as with ordinary Swing component
 * configuration.</p>
 *
 * <h2>Lifecycle</h2>
 * <p>A panel may be constructed unbound and later attached with
 * {@link #bind(TaskHandle)}, or constructed with a handle. Rebinding first
 * unregisters the panel from its previous handle. Unbinding does not cancel
 * the task; task ownership remains with application code.</p>
 *
 * @param <R> task result type
 */
@SuppressWarnings("serial")
public class TaskControlPanel<R> extends JPanel implements TaskListener<R> {

	private final JButton startButton = new JButton("Start");
	private final JButton cancelButton = new JButton("Cancel");
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	private final JLabel statusLabel = new JLabel("Not started");
	private final JLabel elapsedLabel = new JLabel("0.0 s");
	private final Timer elapsedTimer;

	private TaskHandle<R> handle;

	/** Creates an unbound task control panel. */
	public TaskControlPanel() {
		setLayout(new BorderLayout(6, 6));
		progressBar.setStringPainted(true);
		progressBar.setString(" ");

		JPanel labels = new JPanel(new BorderLayout(6, 0));
		labels.add(statusLabel, BorderLayout.CENTER);
		labels.add(elapsedLabel, BorderLayout.EAST);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		buttons.add(startButton);
		buttons.add(cancelButton);

		add(labels, BorderLayout.NORTH);
		add(progressBar, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		startButton.addActionListener(event -> {
			if (handle != null) {
				handle.start();
			}
		});
		cancelButton.addActionListener(event -> {
			if (handle != null) {
				handle.cancel();
				statusLabel.setText("Cancellation requested…");
			}
		});

		elapsedTimer = new Timer(250, event -> updateElapsed());
		applyUnboundState();
	}

	/**
	 * Creates and binds a task control panel.
	 *
	 * @param handle handle to control
	 * @throws NullPointerException if {@code handle} is null
	 */
	public TaskControlPanel(TaskHandle<R> handle) {
		this();
		bind(handle);
	}

	/**
	 * Binds this panel to a task handle, replacing any previous binding.
	 * Binding does not start the task.
	 *
	 * @param newHandle handle to control
	 * @throws NullPointerException if {@code newHandle} is null
	 */
	public final void bind(TaskHandle<R> newHandle) {
		Objects.requireNonNull(newHandle, "newHandle");
		if (handle == newHandle) {
			return;
		}
		unbind();
		handle = newHandle;
		handle.addListener(this);
		startButton.setEnabled(!handle.isCompleted());
		cancelButton.setEnabled(!handle.isCompleted());
		statusLabel.setText(handle.isCompleted()
				? readableStatus(handle.getCompletionStatus()) : "Ready");
		updateElapsed();
	}

	/**
	 * Removes the current listener binding without stopping or cancelling work.
	 */
	public final void unbind() {
		if (handle != null) {
			handle.removeListener(this);
			handle = null;
		}
		elapsedTimer.stop();
		applyUnboundState();
	}

	/** @return currently bound handle, or {@code null} */
	public final TaskHandle<R> getTaskHandle() {
		return handle;
	}

	/** @return Start button, for application-specific presentation or automation */
	public final JButton getStartButton() {
		return startButton;
	}

	/** @return Cancel button, for application-specific presentation or automation */
	public final JButton getCancelButton() {
		return cancelButton;
	}

	/** @return progress component displayed by this panel */
	public final JProgressBar getProgressBar() {
		return progressBar;
	}

	/** @return status label displayed by this panel */
	public final JLabel getStatusLabel() {
		return statusLabel;
	}

	/** @return elapsed-time label displayed by this panel */
	public final JLabel getElapsedLabel() {
		return elapsedLabel;
	}

	@Override
	public void onStarted(TaskHandle<R> taskHandle) {
		startButton.setEnabled(false);
		cancelButton.setEnabled(true);
		progressBar.setIndeterminate(true);
		progressBar.setString("Working…");
		statusLabel.setText("Running");
		elapsedTimer.start();
	}

	@Override
	public void onProgress(TaskHandle<R> taskHandle, ProgressInfo progress) {
		if (progress.indeterminate) {
			progressBar.setIndeterminate(true);
			progressBar.setString(progress.message == null ? "Working…" : progress.message);
		} else {
			int percent = (int) Math.round(100.0 * progress.fraction);
			progressBar.setIndeterminate(false);
			progressBar.setValue(percent);
			progressBar.setString(progress.message == null ? percent + "%" : progress.message);
		}
	}

	@Override
	public void onMessage(TaskHandle<R> taskHandle, String message) {
		if (message != null && !message.isBlank()) {
			statusLabel.setText(message);
		}
	}

	@Override
	public void onCompleted(TaskHandle<R> taskHandle, CompletionStatus status, Throwable error) {
		elapsedTimer.stop();
		updateElapsed();
		startButton.setEnabled(false);
		cancelButton.setEnabled(false);
		progressBar.setIndeterminate(false);
		if (status == CompletionStatus.SUCCEEDED) {
			progressBar.setValue(100);
			progressBar.setString("Done");
		} else {
			progressBar.setString(readableStatus(status));
		}
		statusLabel.setText(status == CompletionStatus.FAILED && error != null
				? "Failed: " + failureText(error) : readableStatus(status));
	}

	private void applyUnboundState() {
		startButton.setEnabled(false);
		cancelButton.setEnabled(false);
		progressBar.setIndeterminate(false);
		progressBar.setValue(0);
		progressBar.setString(" ");
		statusLabel.setText("Not started");
		elapsedLabel.setText("0.0 s");
	}

	private void updateElapsed() {
		double seconds = handle == null ? 0.0 : handle.getElapsedSeconds();
		elapsedLabel.setText(String.format("%.1f s", seconds));
	}

	private static String readableStatus(CompletionStatus status) {
		if (status == null) {
			return "Ready";
		}
		return switch (status) {
			case SUCCEEDED -> "Done";
			case STOPPED -> "Stopped";
			case CANCELLED -> "Cancelled";
			case FAILED -> "Failed";
		};
	}

	private static String failureText(Throwable error) {
		String message = error.getMessage();
		return error.getClass().getSimpleName()
				+ (message == null || message.isBlank() ? "" : ": " + message);
	}
}
