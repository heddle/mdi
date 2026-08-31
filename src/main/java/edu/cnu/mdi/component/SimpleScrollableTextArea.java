package edu.cnu.mdi.component;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** A text area that creates and exposes its containing scroll pane. */
@SuppressWarnings("serial")
public class SimpleScrollableTextArea extends JTextArea {

	private final JScrollPane scrollPane;

	public SimpleScrollableTextArea(int rows, int columns) {
		super(rows, columns);
		scrollPane = new JScrollPane(this);
	}

	public JScrollPane getScrollPane() {
		return scrollPane;
	}
}
