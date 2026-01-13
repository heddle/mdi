package edu.cnu.mdi.experimental;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JToggleButton;

@SuppressWarnings("serial")
public abstract class ADragButton extends JToggleButton implements MouseListener, MouseMotionListener {

	// Is a drag operation in progress?
	private boolean dragging = false;
	
	// Starting point of the drag
	private Point startPoint = null;
	
	// Current point during the drag
	private Point currentPoint = new Point();
	
	// Previous point during the drag
	private Point previousPoint = new Point();
	
	/** Component that owns the current gesture (null when idle). */
	protected Component canvas;

	/** Toolbar that owns this tool. */
	protected AToolBar toolBar;
	
	/**
	 * Create a single-click button that performs an action when clicked.
	 *
	 */
	public ADragButton(Component canvas, AToolBar toolBar) {
		this.toolBar = toolBar;
		this.canvas = canvas;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		
	}

	
	private boolean contained(Point p) {
		return (p.x >= 0 && p.y >= 0 && p.x < canvas.getWidth() && p.y < canvas.getHeight());
	}
	
	
	@Override
	public void mousePressed(MouseEvent e) {
		startPoint = e.getPoint();
		currentPoint.setLocation(startPoint);
		previousPoint.setLocation(startPoint);
		startDrag(startPoint);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(dragging) {
			endDrag();
		}
	}
	
	private void endDrag() {
		dragging = false;
		doneDrag(startPoint, currentPoint);
	    startPoint = null;
		currentPoint.setLocation(0, 0);
		previousPoint.setLocation(0, 0);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!contained(e.getPoint())) {
			return;
		}
		dragging = true;
		previousPoint.setLocation(currentPoint);
		currentPoint.setLocation(e.getPoint());
		if(previousPoint.equals(currentPoint)) {
			return;
		}
		updateDrag(startPoint, previousPoint, currentPoint);		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}
	
	public abstract void startDrag(Point start);
	public abstract void updateDrag(Point start, Point previous, Point current);
	public abstract void doneDrag(Point start, Point end);
	

}
