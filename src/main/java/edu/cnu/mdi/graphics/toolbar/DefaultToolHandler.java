package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.util.PrintUtils;
import edu.cnu.mdi.util.TakePicture;

public class DefaultToolHandler implements IToolHandler {
	
	//for panning
	private BufferedImage base;
	private BufferedImage buffer;


	@Override
	public Object hitTest(AToolBar toolBar, Component canvas, Point p) {
		return null;
	}

	@Override
	public void pointerClick(AToolBar toolBar, Component canvas, Point p, Object obj, MouseEvent e) {
	}

	@Override
	public void pointerDoubleClick(AToolBar toolBar, Component canvas, Point p, Object obj, MouseEvent e) {
	}

	@Override
	public void pointerRubberbanding(AToolBar toolBar, Component canvas, Rectangle bounds) {
		
	}
	
	@Override
	public void beginDragObject(AToolBar toolBar, Component canvas, Object obj, MouseEvent e) {
		
	}
	
	@Override
	public void dragObjectBy(AToolBar toolBar, Component canvas, Object obj, int dx, int dy, MouseEvent e) {
		// no-op
	}
	
	@Override
	public void endDragObject(AToolBar toolBar, Component canvas, Object obj, MouseEvent e) {
		// no-op
	}

	@Override
	public boolean doNotDrag(AToolBar toolBar, Component canvas, Object obj, MouseEvent e) {
		return false;
	}

	
	@Override
	public void boxZoomRubberbanding(AToolBar toolBar, Component canvas, Rectangle bounds) {
		// no-op
	}

	@Override
	public void panStartDrag(AToolBar toolBar, Component canvas, Point start) {
		base = GraphicsUtils.getComponentImage(canvas);
		buffer = GraphicsUtils.getComponentImageBuffer(canvas);
	}

	@Override
	public void panUpdateDrag(AToolBar toolBar, Component canvas, Point start, Point previous, Point current) {
		int totalDx = current.x - start.x;
		int totalDy = current.y - start.y;

		Graphics gg = buffer.getGraphics();
		gg.setColor(canvas.getBackground());
		gg.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
		gg.drawImage(base, totalDx, totalDy, canvas);
		gg.dispose();

		Graphics g = canvas.getGraphics();
		g.drawImage(buffer, 0, 0, canvas);
		g.dispose();
	}

	@Override
	public void panDoneDrag(AToolBar toolBar, Component canvas, Point start, Point end) {
		base = null;
		buffer = null;
		canvas.repaint();
	}

	@Override
	public void magnifyStartMove(AToolBar toolBar, Component canvas, Point start, MouseEvent e) {
		// no-op
	}

	@Override
	public void magnifyUpdateMove(AToolBar toolBar, Component canvas, Point start, Point p, MouseEvent e) {
		// no-op
	}

	@Override
	public void magnifyDoneMove(AToolBar toolBar, Component canvas, Point start, Point end, MouseEvent e) {
		// no-op
	}

	@Override
	public void recenter(AToolBar toolBar, Component canvas, Point center) {
		toolBar.resetDefaultToggleButton();
	}

	@Override
	public void zoomIn(AToolBar toolBar, Component canvas) {
		// no-op
	}

	@Override
	public void zoomOut(AToolBar toolBar, Component canvas) {
		// no-op
	}
	
	@Override
	public void undoZoom(AToolBar toolBar, Component canvas) {
		// no-op
	}
	
	@Override
	public void resetZoom(AToolBar toolBar, Component canvas) {
		// no-op
	}
	
	@Override
	public void styleEdit(AToolBar toolBar, Component canvas) {
		// no-op
	}
	
	@Override
	public void delete(AToolBar toolBar, Component canvas) {
		// no-op
	}
	
	@Override
	public void captureImage(AToolBar toolBar, Component canvas) {
		TakePicture.takePicture(canvas);
	}
	
	@Override
	public void print(AToolBar toolBar, Component canvas) {
		PrintUtils.printComponent(canvas);
	}
	
	@Override
	public void createConnection(AToolBar toolBar, Component canvas, Point start, Point end) {
	}

	
	@Override
	public boolean approveConnectionPoint(AToolBar toolBar, Component canvas, Point p) {
		return false;
	}
	
	@Override
	public void createRectangle(AToolBar toolBar, Component canvas, Rectangle bounds) {
		// no-op
	}

}
