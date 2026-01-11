package edu.cnu.mdi.experimental;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Objects;

import javax.swing.JToggleButton;

@SuppressWarnings("serial")
public class BaseToolBar extends AToolBar implements MouseListener, MouseMotionListener {

	// the canvas this toolbar is associated with
	private Component canvas;
	
	/**
	 * Creates a new horizontal toolbar associated with a canvas.
	 *
	 * @param canvas the canvas component this toolbar is associated with
	 */
	public BaseToolBar(Component canvas) {
		this(canvas, HORIZONTAL);
	}
	
	/**
	 * Creates a new toolbar associated with a canvas.
	 *
	 * @param canvas      the canvas component this toolbar is associated with
	 * @param orientation the initial orientation -- it must be either
	 *                    <code>HORIZONTAL</code> or <code>VERTICAL</code>
	 */
	public BaseToolBar(Component canvas, int orientation) {
		Objects.requireNonNull(canvas, "Canvas component cannot be null");
		
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
	}

	@Override
	protected void activeToggleButtonChanged(JToggleButton newlyActive) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
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
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(java.awt.event.MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(java.awt.event.MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

}
