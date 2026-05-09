package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Objects;

import javax.swing.JToggleButton;

@SuppressWarnings("serial")
/**
 * A base class for toolbar buttons that perform an action on a single click. This
 * class implements MouseListener to handle mouse events on the canvas, and
 * provides a framework for subclasses to define specific actions when the button
 * is clicked.
 *
 * @author heddle
 *
 */
public abstract class ASingleClickButton  extends JToggleButton implements MouseListener {

	/** Component that owns the current gesture (null when idle). */
	protected Component canvas;

	/** Toolbar that owns this tool. */
	protected AToolBar toolBar;

	/**
	 * Create a single-click button that performs an action when clicked.
	 * @param canvas  the component that will receive mouse events when this tool is active
	 * @param toolBar the toolbar that owns this button
	 */
	public ASingleClickButton(Component canvas, AToolBar toolBar) {
		this.toolBar = toolBar;
		this.canvas = canvas;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	/**
	 * @return cursor to use while active. Default is crosshair.
	 */
	protected Cursor activeCursor() {
		return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		Objects.requireNonNull(canvas, "canvas");
		Objects.requireNonNull(toolBar, "toolBar");
		canvasClick(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// No action needed on mouse release
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// No action needed on mouse enter
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// No action needed on mouse exit
	}

	/**
	 * Handle a mouse click on the canvas. Subclasses should implement this to
	 * perform the desired action.
	 *
	 * @param e the mouse event representing the click
	 */
	public abstract void canvasClick(MouseEvent e);

}
