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
	protected static Dimension PREFERRED_SIZE = new Dimension(24, 24);
	
	/**
	 * Create a new action button.
	 *
	 * @param imageFile for example, "images/svg/delete.svg".
	 * @param toolTip   tooltip text.
	 */
	protected ToolActionButton(String imageFile, String toolTip) {
		setFocusPainted(false);
		setBorderPainted(false);
		setToolTipText(toolTip);

		int imageWidth = PREFERRED_SIZE.width - 4;
		int imageHeight = PREFERRED_SIZE.height - 4;
		Icon icon = ImageManager.getInstance().loadUiIcon(imageFile, imageWidth, imageHeight);
		setIcon(icon);

		addActionListener(e -> {
			doAction();
		});
	}
	
	/**
	 * Perform the action associated with this button.
	 * This is implemented by subclasses.
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