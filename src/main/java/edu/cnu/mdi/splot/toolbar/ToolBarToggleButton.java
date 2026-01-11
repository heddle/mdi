package edu.cnu.mdi.splot.toolbar;

import java.awt.Cursor;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

import edu.cnu.mdi.graphics.ImageManager;

@SuppressWarnings("serial")
public class ToolBarToggleButton extends JToggleButton {

	/**
	 * preferred size. Default, for whatever reason, will be 24x24.
	 */
	protected Dimension preferredSize;

	/**
	 * Custom cursor.
	 */
	protected Cursor customCursor;


	/**
	 * Used in conjunction with custom cursor.
	 */
	protected boolean triedOnce = false;

	/**
	 * The x coordinate of hot spot of a custom cursor, if there is one. A negative
	 * value means it will use the center of the cursor.
	 */
	protected int xhot = -1;

	/**
	 * The y coordinate of hot spot of a custom cursor, if there is one. A negative
	 * value means it will use the center of the cursor.
	 */
	protected int yhot = -1;

	protected static int ICONSIZE = 24;

	/**
	 * Create a toolbar toggle button
	 *
	 * @param container     the owner container.
	 * @param imageFileName the name if the file holding the icon
	 * @param toolTip       a string for a tool tip
	 * @param actionCommand theaction command.
	 */
	public ToolBarToggleButton(String imageFileName, String toolTip, String actionCommand) {
		this(imageFileName, toolTip, actionCommand, -1, -1);
	}

	/**
	 * Create a toolbar toggle button
	 *
	 * @param container     the owner container.
	 * @param imageFileName the name if the file holding the icon
	 * @param toolTip       a string for a tool tip
	 * @param actionCommand theaction command.
	 */
	public ToolBarToggleButton(String imageFileName, String toolTip,
			String actionCommand, int xh, int yh) {
		this(imageFileName, toolTip, actionCommand, xh, yh, ICONSIZE, ICONSIZE);
	}

	/**
	 * Create a toolbar toggle button
	 *
	 * @param container     the owner container.
	 * @param imageFileName the name if the file holding the icon
	 * @param toolTip       a string for a tool tip
	 * @param actionCommand theaction command.
	 */
	public ToolBarToggleButton(String imageFileName, String toolTip, String actionCommand, int xh, int yh,
			 int iw, int ih) {
		super();
		preferredSize = new Dimension(iw, ih);

		xhot = xh;
		yhot = yh;

		// setFocusPainted(false);

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

	/**
	 * Get the appropriate cursor for this tool.
	 *
	 * @return The cursor appropriate when the mouse is in the container. The
	 *         default will be a cross hair.
	 */
	public Cursor canvasCursor() {
		return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}
}
