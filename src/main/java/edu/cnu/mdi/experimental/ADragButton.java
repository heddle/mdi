package edu.cnu.mdi.experimental;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JToggleButton;

@SuppressWarnings("serial")
public abstract class ADragButton extends JToggleButton implements MouseListener, MouseMotionListener {

	private boolean dragging = false;
	
	private Point startPoint = null;
	
	private Point currentPoint = new Point();
	
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

	@Override
	public void mousePressed(MouseEvent e) {
		System.out.println("mouse pressed at " + e.getPoint());
		startPoint = e.getPoint();
		currentPoint.setLocation(startPoint);
		previousPoint.setLocation(startPoint);
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (!dragging) {
			return;
		}
		System.out.println("mouse released at " + e.getPoint());
		dragging = false;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		dragging = true;
		previousPoint.setLocation(currentPoint);
		currentPoint.setLocation(e.getPoint());
		if(previousPoint.equals(currentPoint)) {
			System.out.println("no movement");
			return;
		}
		System.out.println("dragging start " + startPoint + " previous " + previousPoint + " current " + currentPoint);
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	public abstract void updateDrag(Point start, Point previous, Point current);
	

}
