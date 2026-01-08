package edu.cnu.mdi.splot.toolbar;

import java.awt.Cursor;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JButton;

import edu.cnu.mdi.graphics.ImageManager;

public class ToolBarButton extends JButton {

	/**
	 * Preferred size for toolbar buttons.
	 */
	private static Dimension preferredSize = new Dimension(24, 24);

	/**
	 * Constructor
	 *
	 * @param container     the owner container.
	 * @param imageFileName the name if the file holding the icon
	 * @param toolTip       a string for a tool tip
	 */
	public ToolBarButton(String imageFileName, String toolTip, String actionCommand) {
		super();

		setActionCommand(actionCommand);
		Icon icon = ImageManager.getInstance().loadUiIcon(imageFileName, 20, 20);
		setIcon(icon);

		String bareName = new String(imageFileName);
		int index = bareName.indexOf(".");
		if (index > 1) {
			bareName = bareName.substring(0, index);
		}

		setFocusPainted(false);
		setToolTipText(toolTip);
		setRolloverEnabled(true);
		setBorderPainted(false);

	}

	/**
	 * Get the appropriate cursor for this tool.
	 *
	 * @return the cursor appropriate when the mouse is in the container (and this
	 *         button is active).
	 */
	public Cursor canvasCursor() {
		return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
	}

	/**
	 * Get the preferred size.
	 *
	 * @return the preferred size for layout.
	 */
	@Override
	public Dimension getPreferredSize() {
		return preferredSize;
	}
}
