package edu.cnu.mdi.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import edu.cnu.mdi.ui.fonts.Fonts;

/** A non-modal, read-only dialog for displaying monospaced text. */
@SuppressWarnings("serial")
public class TextDisplayDialog extends SimpleDialog {

	private static final int WIDTH = 800;
	private static final int HEIGHT = 900;

	private JTextArea textArea;

	public TextDisplayDialog(String title) {
		super(title, false, "Close");
		textArea.setEditable(false);
		textArea.setLineWrap(false);
		textArea.setFont(Fonts.mono);
		setSize(WIDTH, HEIGHT);
		DialogUtils.centerDialog(this);
	}

	public void setText(String text) {
		textArea.setText(text);
	}

	@Override
	protected Component createCenterComponent() {
		JPanel panel = new JPanel(new BorderLayout(4, 4)) {
			@Override
			public Insets getInsets() {
				Insets insets = super.getInsets();
				return new Insets(insets.top + 4, insets.left + 4, insets.bottom + 4, insets.right + 4);
			}
		};
		textArea = new JTextArea(20, 80);
		panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
		return panel;
	}

	public JTextArea getTextArea() {
		return textArea;
	}
}
