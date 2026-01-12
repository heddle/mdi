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
	 * @param bits   controls which predefined buttons are added to the toolbar.
	 * See {@link AToolBar} and {@link ToolBarBits}  for details. You
	 * are not limited to these bits; you can always add your own buttons after
	 * creating the toolbar.
	 */
	public BaseToolBar(Component canvas, long bits) {
		this(canvas, bits, HORIZONTAL);
		addPredefinedButtons(bits);
	}
	
	/**
	 * 
	 * Adds predefined buttons to the toolbar based on the provided bits.
	 * The order is based on common usage patterns.
	 *
	 * @param bits controls which predefined buttons are added to the toolbar.
	 * See {@link AToolBar} and {@link ToolBarBits}  for details.
	 */
	private void addPredefinedButtons(long bits) {
		if (ToolBarBits.hasPointerButton(bits)) {
			// make pointer button the default active button
		}
		
	}
	
	/**
	 * 
	 * Creates a new toolbar associated with a canvas.
	 *
	 * @param canvas      the canvas component this toolbar is associated with
	 * @param bits        controls which predefined buttons are added to the toolbar.
	 * See {@link AToolBar} and {@link ToolBarBits}  for details. You
	 * are not limited to these bits; you can always add your own buttons after
	 * 
	 * @param orientation the initial orientation -- it must be either
	 *                    <code>HORIZONTAL</code> or <code>VERTICAL</code>
	 */
	public BaseToolBar(Component canvas, long bits, int orientation) {
		super(orientation);
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
