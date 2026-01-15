package edu.cnu.mdi.container;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Objects;

import edu.cnu.mdi.experimental.AToolBar;
import edu.cnu.mdi.experimental.DefaultToolHandler;

public class ContainerToolHandler extends DefaultToolHandler {
	
	// Zoom factor for each zoom in/out action
	private static final double ZOOM_FACTOR = 0.8;
	
	private BaseContainer container;
	
	public ContainerToolHandler(BaseContainer container) {
		Objects.requireNonNull(container, "container");
		this.container = container;
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
	public void magnifyStartMove(AToolBar toolBar, Component canvas, Point start) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void magnifyUpdateMove(AToolBar toolBar, Component canvas, Point start, Point p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void magnifyDoneMove(AToolBar toolBar, Component canvas, Point start, Point end) {
		// TODO Auto-generated method stub
		
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
}
