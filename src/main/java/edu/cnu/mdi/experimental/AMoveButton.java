package edu.cnu.mdi.experimental;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JToggleButton;

@SuppressWarnings("serial")
public abstract class AMoveButton extends JToggleButton implements MouseListener, MouseMotionListener {

	// Is a move operation in progress?
	private boolean moving = false;
	
	// Starting point of the drag
	private Point startPoint = null;

	/** Component that owns the current gesture (null when idle). */
	protected Component canvas;

	/** Toolbar that owns this tool. */
	protected AToolBar toolBar;
	
	/** Dimension of the move area. */
	protected Dimension size;

	/**
	 * Create a single-click button that performs an action when clicked.
	 * @param canvas Component on which the move is occurring
	 * @param toolBar Toolbar that owns this tool
	 * @param size Dimension of the move area. The entire area centered at the point
	 * must be within the canvas to be valid. Use <code>null</code> to ignore.
	 * 
	 */
	public AMoveButton(Component canvas, AToolBar toolBar, final Dimension size) {
		this.toolBar = toolBar;
		this.canvas = canvas;
		this.size = size;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
	}

	
	/**
	 * Check if point is contained within the canvas, considering size.
	 * @param p Point to check
	 * @return true if contained (including size), false otherwise
	 */
	private boolean contained(Point p) {
		//contained if box centered at p with size dimension is fully within canvas
		int halfWidth = (size == null ? 0 : size.width / 2);
		int halfHeight = (size == null ? 0 : size.height / 2);
		return (p.x - halfWidth >= 0 && p.y - halfHeight >=
				0 && p.x + halfWidth < canvas.getWidth() && p.y + halfHeight < canvas.getHeight());
	}
	
	
	@Override
	public void mousePressed(MouseEvent e) {
		// mouse press ends move if in progress
		endMove(e.getPoint());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}
	
	// Complete the move operation
	private void endMove(Point end) {
		moving = false;
		toolBar.resetDefaultToggleButton();
		doneMove(startPoint, end);
	    startPoint = null;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// mouse exit ends move if in progress
		endMove(e.getPoint());
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (contained(e.getPoint())) {
			moving = true;

			if (startPoint == null) {
				startPoint = e.getPoint();
				// System.out.println("Starting move at " + e.getPoint());
				startMove(startPoint);
			} else {
				System.out.println("Moving at " + e.getPoint());
				updateMove(startPoint, e.getPoint());
			}
		}

	}
	
	public abstract void startMove(Point start);
	public abstract void updateMove(Point start, Point p);
	public abstract void doneMove(Point start, Point end);

}
