package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

/**
 * A non-toggle toolbar button that performs an immediate action (one-shot).
 * <p>

 * Unlike ToolToggleButton, this class does not change the active tool.
 * </p>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public abstract class AOneShotButton extends JButton {


	/** Component that owns the current gesture. */
	protected Component canvas;

	/** Toolbar that owns this tool. */
	protected AToolBar toolBar;

	/**
	 * Creates a one-shot button.
	 *
	 * @param canvas  the component that owns the current gesture
	 * @param toolBar the toolbar that owns this button
	 */
	protected AOneShotButton(Component canvas, AToolBar toolBar) {
		this.toolBar = toolBar;
		this.canvas = canvas;

		ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.resetDefaultToggleButton();
				performAction();
			}
		};
		addActionListener(al);
	}

	/**
	 * Perform the action associated with this button.
	 * This is implemented by subclasses.
	 */
	public abstract void performAction();


}