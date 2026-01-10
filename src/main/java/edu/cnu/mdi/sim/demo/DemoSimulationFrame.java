package edu.cnu.mdi.sim.demo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;
import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.SimulationListener;
import edu.cnu.mdi.sim.SimulationState;

/**
 * Tiny Swing demo that exercises the simulation framework without any MDI
 * dependencies.
 * <p>
 * It shows:
 * <ul>
 * <li>Indeterminate progress while running</li>
 * <li>Determinate progress updates</li>
 * <li>Pause/resume/stop/cancel</li>
 * </ul>
 * </p>
 */
public class DemoSimulationFrame extends JFrame {

	/**
	 * Launch the demo.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			DemoSimulationFrame f = new DemoSimulationFrame();
			f.setVisible(true);
		});
	}

	private final JLabel stateLabel = new JLabel("State: NEW");
	private final JLabel msgLabel = new JLabel(" ");
	private final JProgressBar progress = new JProgressBar(0, 100);

	/**
	 * Create the frame and wire up an example simulation.
	 */
	public DemoSimulationFrame() {
		super("Simulation Framework Demo");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(520, 180));

		progress.setStringPainted(true);

		JButton start = new JButton("Start");
		JButton pause = new JButton("Pause");
		JButton resume = new JButton("Resume");
		JButton stop = new JButton("Stop");
		JButton cancel = new JButton("Cancel");

		JPanel top = new JPanel(new BorderLayout());
		top.add(stateLabel, BorderLayout.WEST);

		JPanel center = new JPanel(new BorderLayout());
		center.add(progress, BorderLayout.CENTER);
		center.add(msgLabel, BorderLayout.SOUTH);

		JPanel buttons = new JPanel();
		buttons.add(start);
		buttons.add(pause);
		buttons.add(resume);
		buttons.add(stop);
		buttons.add(cancel);

		getContentPane().setLayout(new BorderLayout(8, 8));
		getContentPane().add(top, BorderLayout.NORTH);
		getContentPane().add(center, BorderLayout.CENTER);
		getContentPane().add(buttons, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(null);

		Simulation sim = new Simulation() {
			private int i;

			@Override
			public void init(SimulationContext ctx) throws Exception {
				// pretend initialization work
				Thread.sleep(300);
			}

			@Override
			public boolean step(SimulationContext ctx) throws Exception {
				// pretend compute work
				Thread.sleep(10);
				i++;

				// determinate progress
				return i <= 500;
			}

			@Override
			public void cancel(SimulationContext ctx) throws Exception {
				// optional cancellation cleanup
			}

			@Override
			public void shutdown(SimulationContext ctx) throws Exception {
				// optional shutdown cleanup
			}
		};

		SimulationEngine engine = new SimulationEngine(sim, new SimulationEngineConfig(33, 200, 0, false));

		engine.addListener(new SimulationListener() {
			@Override
			public void onStateChange(SimulationContext ctx, SimulationState from, SimulationState to, String reason) {
				stateLabel.setText("State: " + to + (reason == null ? "" : ("  (" + reason + ")")));
			}

			@Override
			public void onRun(SimulationContext ctx) {
				msgLabel.setText("Running…");
				progress.setIndeterminate(true);
			}

			@Override
			public void onPause(SimulationContext ctx) {
				msgLabel.setText("Paused.");
				progress.setIndeterminate(false);
			}

			@Override
			public void onProgress(SimulationContext ctx, ProgressInfo p) {
				if (p.indeterminate) {
					progress.setIndeterminate(true);
					progress.setString(p.message == null ? "Working…" : p.message);
				} else {
					progress.setIndeterminate(false);
					int val = (int) Math.round(100.0 * p.fraction);
					progress.setValue(val);
					progress.setString(p.message == null ? (val + "%") : p.message);
				}
			}

			@Override
			public void onRefresh(SimulationContext ctx) {
				// here you would repaint a view; demo just shows determinate progress computed
				// from stepCount
				double frac = Math.min(1.0, ctx.getStepCount() / 500.0);
				engine.postProgress(ProgressInfo.determinate(frac, "Step " + ctx.getStepCount() + " / 500"));
			}

			@Override
			public void onDone(SimulationContext ctx) {
				msgLabel.setText("Done.");
				progress.setIndeterminate(false);
			}

			@Override
			public void onFail(SimulationContext ctx, Throwable error) {
				msgLabel.setText("FAILED: " + error.getClass().getSimpleName() + ": " + error.getMessage());
				progress.setIndeterminate(false);
			}
		});

		start.addActionListener(e -> {
			engine.start();
			engine.requestRun();
		});
		pause.addActionListener(e -> engine.requestPause());
		resume.addActionListener(e -> engine.requestResume());
		stop.addActionListener(e -> engine.requestStop());
		cancel.addActionListener(e -> engine.requestCancel());
	}
}
