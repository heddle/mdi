package edu.cnu.mdi.container;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Objects;

import edu.cnu.mdi.component.MagnifyWindow;
import edu.cnu.mdi.experimental.AToolBar;
import edu.cnu.mdi.experimental.DefaultToolHandler;
import edu.cnu.mdi.view.BaseView;

public class BaseContainerToolHandler extends DefaultToolHandler {
	
	// Zoom factor for each zoom in/out action
	private static final double ZOOM_FACTOR = 0.8;
	
	private BaseContainer container;
	
	public BaseContainerToolHandler(BaseContainer container) {
		Objects.requireNonNull(container, "container");
		this.container = container;
	}
	
	/**
	 * Hit test at the given point on the canvas.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas  JComponent on which the hit test is occurring
	 * @param p       Point to hit test
	 * @return Object that was hit, or null if nothing was hit
	 */
	@Override
	public Object hitTest(AToolBar toolBar, Component canvas, Point p) {
		return container.getItemAtPoint(p);
	}

	/**
	 * Handle pointer click on an object at the given point.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the click is occurring
	 * @param p      Point where the click occurred
	 * @param obj    Object that was clicked or null if none
	 * @param e      MouseEvent that triggered the click
	 */
	@Override
	public void pointerClick(AToolBar toolBar, Component canvas, Point p, Object obj, MouseEvent e) {
		boolean addModifier = isAddModifier(e);
		System.out.println("Pointer click at " + p + " on object " + obj + ", addModifier=" + addModifier);
	}
	
	/**
	 * Handle pointer double click on an object at the given point.
	 * 
	 * @param toolBar ToolBar that owns this tool
	 * @param canvas JComponent on which the double click is occurring
	 * @param p      Point where the double click occurred
	 * @param obj    Object that was double clicked or null if none
	 * @param e      MouseEvent that triggered the click
	 */
	@Override
	public void pointerDoubleClick(AToolBar toolBar, Component canvas, Point p, Object obj, MouseEvent e) {
		System.out.println("Pointer double click at " + p + " on object " + obj);
	}

	@Override
	public void pointerRubberbanding(AToolBar toolBar, Component canvas, Rectangle bounds) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void boxZoomRubberbanding(AToolBar toolBar, Component canvas, Rectangle bounds) {
		container.rubberBanded(bounds);
	}

	@Override
	public void magnifyStartMove(AToolBar toolBar, Component canvas, Point start, MouseEvent e) {
		BaseView view = container.getView();
		if (view != null) {
			view.handleMagnify(e);
		}
	}

	@Override
	public void magnifyUpdateMove(AToolBar toolBar, Component canvas, Point start, Point p, MouseEvent e) {
		BaseView view = container.getView();
		if (view != null) {
			view.handleMagnify(e);
		}
	}

	@Override
	public void magnifyDoneMove(AToolBar toolBar, Component canvas, Point start, Point end, MouseEvent e) {
		MagnifyWindow.closeMagnifyWindow();
	}

	@Override
	public void recenter(AToolBar toolBar, Component canvas, Point center) {
		container.prepareToZoom();
		container.recenter(center);
		container.refresh();
	}

	@Override
	public void zoomIn(AToolBar toolBar, Component canvas) {
		container.scale(ZOOM_FACTOR);
	}

	@Override
	public void zoomOut(AToolBar toolBar, Component canvas) {
		container.scale(1.0 / ZOOM_FACTOR);
	}

	@Override
	public void undoZoom(AToolBar toolBar, Component canvas) {
		container.undoLastZoom();
	}

	@Override
	public void resetZoom(AToolBar toolBar, Component canvas) {
		container.restoreDefaultWorld();
	}

	@Override
	public void styleEdit(AToolBar toolBar, Component canvas) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(AToolBar toolBar, Component canvas) {
		// TODO Auto-generated method stub
		
	}
	
	// Check if the add modifier key is pressed (Shift, Ctrl, or Command)
	private boolean isAddModifier(MouseEvent e) {
		int m = e.getModifiersEx();
		return e.isShiftDown() || (m & InputEvent.CTRL_DOWN_MASK) != 0 || (m & InputEvent.META_DOWN_MASK) != 0; // mac
	}

}
