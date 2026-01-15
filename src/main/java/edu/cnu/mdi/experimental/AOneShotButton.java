package edu.cnu.mdi.experimental;

import java.awt.Component;

import javax.swing.JButton;

import edu.cnu.mdi.graphics.toolbar.ToolContext;

/**
 * A non-toggle toolbar button that performs an immediate action (one-shot).
 * <p>

 * Unlike {@link ToolToggleButton}, this class does not change the active tool.
 * It just runs {@link #perform(ToolContext)} when clicked.
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
	 * Create a new action button.
	 *
	 * @param imageFile for example, "images/svg/delete.svg".
	 * @param toolTip   tooltip text.
	 */
	protected AOneShotButton(Component canvas, AToolBar toolBar) {
		this.toolBar = toolBar;
		this.canvas = canvas;
		addActionListener(e -> performAction());
	}

	/**
	 * Perform the action associated with this button.
	 * This is implemented by subclasses.
	 */
	public abstract void performAction();


}