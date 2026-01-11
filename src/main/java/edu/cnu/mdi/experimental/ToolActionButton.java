package edu.cnu.mdi.experimental;

import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JButton;

import edu.cnu.mdi.graphics.ImageManager;

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
public abstract class ToolActionButton extends JButton {

	/** Preferred size for toolbar buttons. */
	private static final Dimension PREFERRED_SIZE = new Dimension(24, 24);
	/**
	 * Create a new action button.
	 *
	 * @param imageFile image resource path for the icon.
	 * @param toolTip   tooltip text.
	 */
	protected ToolActionButton(String imageFile, String toolTip) {
		setFocusPainted(false);
		setBorderPainted(false);
		setToolTipText(toolTip);

		Icon icon = ImageManager.getInstance().loadUiIcon(imageFile, 20, 20);
		setIcon(icon);

		// Works with injection: ctx will be set by BaseToolBar before the user can
		// click.
		addActionListener(e -> {
			doAction();
		});
	}
	
	/**
	 * Perform the action associated with this button.
	 */
	public abstract void doAction();

	@Override
	public Dimension getPreferredSize() {
		return PREFERRED_SIZE;
	}

	@Override
	public Dimension getMinimumSize() {
		return PREFERRED_SIZE;
	}

	@Override
	public Dimension getMaximumSize() {
		return PREFERRED_SIZE;
	}
}