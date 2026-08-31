package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

/**
 * A non-toggle toolbar button that performs an immediate action (one-shot).
 * <p>
 * Unlike the {@code JToggleButton}-based tool buttons in this package (e.g.
 * {@link APointerButton}, {@link AMoveButton}, {@link ADragButton},
 * {@link ASingleClickButton}, {@link ARubberbandButton}), this class does not
 * become the active tool or participate in the toolbar's mutual-exclusion
 * group — it just fires an action and leaves whichever tool was active
 * selected.
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