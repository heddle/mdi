package edu.cnu.mdi.experimental;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Objects;

import javax.swing.JToggleButton;

public abstract class ASingleClickButton  extends JToggleButton implements MouseListener {

	/** Component that owns the current gesture (null when idle). */
	protected Component canvas;

	/** Toolbar that owns this tool. */
	protected AToolBar toolBar;
	
	/**
	 * Create a single-click button that performs an action when clicked.
	 *
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
		handleCanvasClick(e);
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
	
	public abstract void handleCanvasClick(MouseEvent e);

}
