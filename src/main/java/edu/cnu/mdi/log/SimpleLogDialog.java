package edu.cnu.mdi.log;

import java.awt.BorderLayout;

import javax.swing.JDialog;

import edu.cnu.mdi.graphics.GraphicsUtils;

@SuppressWarnings("serial")
public class SimpleLogDialog extends JDialog {

	/**
	 * Creat a simple dialog for displaying log messages.
	 */
	public SimpleLogDialog() {
		setTitle("Log Messages");
		setModal(false);
		add(new SimpleLogPane(), BorderLayout.CENTER);
		pack();
		GraphicsUtils.centerComponent(this);
	}
}
