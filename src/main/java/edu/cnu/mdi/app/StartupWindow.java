package edu.cnu.mdi.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.log.LogPane;

/** Optional lightweight startup window with application metadata and live logging. */
public final class StartupWindow implements AutoCloseable {

	private final JWindow window;
	private final JLabel status = new JLabel("Starting…", SwingConstants.LEFT);
	private final LogPane logPane = new LogPane();

	public StartupWindow(StartupInfo info) {
		if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Create StartupWindow on the EDT");
		window = new JWindow((Window) null);
		window.getRootPane().setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(java.awt.Color.GRAY), BorderFactory.createEmptyBorder(14, 16, 12, 16)));
		window.setLayout(new BorderLayout(8, 10));
		window.add(header(info), BorderLayout.NORTH);
		logPane.setPreferredSize(new Dimension(620, 230));
		window.add(logPane, BorderLayout.CENTER);
		status.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 2));
		window.add(status, BorderLayout.SOUTH);
		window.pack();
		window.setLocationRelativeTo(null);
	}

	public void show() { runOnEdt(() -> window.setVisible(true)); }
	public void status(String message) { runOnEdt(() -> status.setText(message == null ? "" : message)); }
	public boolean isShowing() { return window.isShowing(); }

	@Override public void close() {
		runOnEdt(() -> { logPane.detach(); window.setVisible(false); window.dispose(); });
	}

	private static JPanel header(StartupInfo info) {
		JPanel panel = new JPanel(); panel.setOpaque(false); panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		if (info.logo() != null) { panel.add(new JLabel(info.logo())); panel.add(Box.createHorizontalStrut(14)); }
		JPanel text = new JPanel(); text.setOpaque(false); text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		JLabel name = new JLabel(info.applicationName() + (info.version().isBlank() ? "" : "  " + info.version()));
		name.setFont(name.getFont().deriveFont(java.awt.Font.BOLD, name.getFont().getSize2D() + 4)); text.add(name);
		if (!info.organization().isBlank()) text.add(new JLabel(info.organization()));
		if (!info.copyright().isBlank()) text.add(new JLabel(info.copyright()));
		panel.add(text); panel.add(Box.createHorizontalGlue()); return panel;
	}

	private static void runOnEdt(Runnable task) {
		if (SwingUtilities.isEventDispatchThread()) task.run(); else SwingUtilities.invokeLater(task);
	}
}
