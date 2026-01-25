package edu.cnu.mdi.graphics.toolbar;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.util.PrintUtils;
import edu.cnu.mdi.util.TakePicture;

/**
 * Default tool handler that performs no actions except for panning,
 * printing, and capturing images. It is intended as a base class for
 * custom tool handlers. It can be used like an Adapter.
 *
 * @author heddle
 */
public class DefaultToolHandler implements IToolHandler {

	//for panning
	private BufferedImage base;
	private BufferedImage buffer;

	@Override
	public void createRectangle(GestureContext gc, Rectangle bounds) {
		// no-op
	}

	@Override
	public void createEllipse(GestureContext gc, Rectangle bounds) {
		// no-op
	}

	@Override
	public void createRadArc(GestureContext gc, Point[] vertices) {
		// no-op
	}

	@Override
	public void createTextItem(Point location) {
		// no-op
	}

	@Override
	public Object hitTest(GestureContext gc, Point p) {
		return null;
	}

	@Override
	public void pointerRubberbanding(GestureContext gc, Rectangle bounds) {
		// no-op
	}

	@Override
	public void pointerClick(GestureContext gc) {
		// no-op
	}

	@Override
	public void pointerDoubleClick(GestureContext gc) {
		// no-op
	}

	@Override
	public void beginDragObject(GestureContext gc) {
		// no-op
	}

	@Override
	public void dragObjectBy(GestureContext gc, int dx, int dy) {
		// no-op
	}

	@Override
	public void endDragObject(GestureContext gc) {
		// no-op
	}

	@Override
	public boolean doNotDrag(GestureContext gc) {
		return false;
	}

	@Override
	public void panStartDrag(GestureContext gc) {
		base = GraphicsUtils.getComponentImage(gc.getCanvas());
		buffer = GraphicsUtils.getComponentImageBuffer(gc.getCanvas());
	}

	@Override
	public void panUpdateDrag(GestureContext gc) {

		Point start = gc.getPressPoint();
		Point current = gc.getCurrentPoint();

		int totalDx = current.x - start.x;
		int totalDy = current.y - start.y;

		Graphics gg = buffer.getGraphics();
		gg.setColor(gc.getCanvas().getBackground());
		gg.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
		gg.drawImage(base, totalDx, totalDy, gc.getCanvas());
		gg.dispose();

		Graphics g = gc.getCanvas().getGraphics();
		g.drawImage(buffer, 0, 0, 	gc.getCanvas());
		g.dispose();
	}

	@Override
	public void panDoneDrag(GestureContext gc) {
		base = null;
		buffer = null;
		gc.getCanvas().repaint();
	}

	@Override
	public void magnifyStartMove(GestureContext gc) {
		// no-op
	}

	@Override
	public void magnifyUpdateMove(GestureContext gc) {
		// no-op
	}

	@Override
	public void magnifyDoneMove(GestureContext gc) {
		// no-op
	}

	@Override
	public void recenter(GestureContext gc) {
		// no-op
	}

	@Override
	public void zoomIn(GestureContext gc) {
		// no-op
	}

	@Override
	public void zoomOut(GestureContext gc) {
		// no-op
	}

	@Override
	public void undoZoom(GestureContext gc) {
		// no-op
	}

	@Override
	public void resetZoom(GestureContext gc) {
		// no-op
	}

	@Override
	public void styleEdit(GestureContext gc) {
		// no-op
	}

	@Override
	public void delete(GestureContext gc) {
		// no-op
	}

	@Override
	public void captureImage(GestureContext gc) {
		TakePicture.takePicture(gc.getCanvas());
	}

	@Override
	public void print(GestureContext gc) {
		PrintUtils.printComponent(gc.getCanvas());
	}

	@Override
	public void createConnection(GestureContext gc, Point start, Point end) {
		// no-op
	}

	@Override
	public boolean approveConnectionPoint(GestureContext gc, Point p) {
			return false;
	}

	@Override
	public void boxZoomRubberbanding(GestureContext gc, Rectangle bounds) {
		// no-op
	}

	@Override
	public void createPolygon(GestureContext gc, Point[] vertices) {
	}

	@Override
	public void createPolyline(GestureContext gc, Point[] vertices) {
	}

	@Override
	public void createLine(GestureContext gc, Point start, Point end) {
	}

}
