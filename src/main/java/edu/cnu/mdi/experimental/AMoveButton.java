package edu.cnu.mdi.experimental;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JToggleButton;

public abstract class AMoveButton extends JToggleButton implements MouseListener, MouseMotionListener {

	// Is a move operation in progress?
	private boolean moving = false;

	/** Component that owns the current gesture (null when idle). */
	protected Component canvas;

	/** Toolbar that owns this tool. */
	protected AToolBar toolBar;
	
	protected Dimension dimension;

	/**
	 * Create a single-click button that performs an action when clicked.
	 *
	 */
	public AMoveButton(Component canvas, AToolBar toolBar, final Dimension dimension) {
		this.toolBar = toolBar;
		this.canvas = canvas;
		this.dimension = dimension;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		
	}

	
	private boolean contained(Point p) {
		//contained if box centered at p with size dimension is fully within canvas
		int halfWidth = dimension.width / 2;
		int halfHeight = dimension.height / 2;
		return (p.x - halfWidth >= 0 && p.y - halfHeight >=
				0 && p.x + halfWidth < canvas.getWidth() && p.y + halfHeight < canvas.getHeight());
	}
	
	
	@Override
	public void mousePressed(MouseEvent e) {
		endMove(e.getPoint());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}
	
	private void endMove(Point p) {
		moving = false;
		toolBar.resetDefaultToggleButton();
		doneMove(p);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
		endMove(e.getPoint());
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (contained(e.getPoint())) {
			moving = true;
			System.out.println("Moving at " + e.getPoint());
			handleMove(e.getPoint());
		} 
	
	}
	
	public abstract void handleMove(Point p);
	public abstract void doneMove(Point p);

}
