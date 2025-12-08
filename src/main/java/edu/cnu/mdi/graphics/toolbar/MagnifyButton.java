package edu.cnu.mdi.graphics.toolbar;

import java.awt.event.MouseEvent;

import edu.cnu.mdi.component.MagnifyWindow;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.view.BaseView;

/**
 * Used to rubber band a zoom.
 *
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public class MagnifyButton extends ToolBarToggleButton {

	/**
	 * Create the button for magnification
	 *
	 * @param container the owner container.
	 */
	public MagnifyButton(IContainer container) {
		super(container, "images/magnify.png", "Magnification");
		customCursorImageFile = "images/box_zoomcursor.gif";
	}

	/**
	 * Handle a mouse enter (into the container) event (if this tool is active).
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseEntered(MouseEvent mouseEvent) {
	}

	/**
	 * Handle a mouse exit (into the container) event (if this tool is active).
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseExited(MouseEvent mouseEvent) {
		System.out.println("MagnifyButton.mouseExited");
		MagnifyWindow.closeMagnifyWindow();
	}

	/**
	 * Handle a mouse press (into the container) event (if this tool is active).
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mousePressed(MouseEvent mouseEvent) {
	}

	/**
	 * Handle a mouse move (into the container) event (if this tool is active).
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseMoved(MouseEvent mouseEvent) {
		BaseView view = container.getView();

		if (view == null) {
			return;
		}

		view.handleMagnify(mouseEvent);
	}

}