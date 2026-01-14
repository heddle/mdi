package edu.cnu.mdi.experimental;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import edu.cnu.mdi.graphics.GraphicsUtils;

public class DefaultToolHandler implements IToolHandler {
	
	//for panning
	private BufferedImage base;
	private BufferedImage buffer;


	@Override
	public void pointerRubberbanding(AToolBar toolBar, Component canvas, Rectangle bounds) {
		System.out.println("Pointer button rubberbanded: " + bounds);
	}

	@Override
	public void boxZoomRubberbanding(AToolBar toolBar, Component canvas, Rectangle bounds) {
		System.out.println("Box zoom button rubberbanded: " + bounds);
	}

	@Override
	public void panStartDrag(AToolBar toolBar, Component canvas, Point start) {
		System.out.println("start dragging at " + start);
		base = GraphicsUtils.getComponentImage(canvas);
		buffer = GraphicsUtils.getComponentImageBuffer(canvas);
	}

	@Override
	public void panUpdateDrag(AToolBar toolBar, Component canvas, Point start, Point previous, Point current) {
		System.out.println("dragging start " + start + " previous " + previous + " current " + current);
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
		System.out.println("done dragging from " + start + " to " + end);
		base = null;
		buffer = null;
		canvas.repaint();
	}

	@Override
	public void magnifyStartMove(AToolBar toolBar, Component canvas, Point start) {
		System.out.println("start move at " + start);
	}

	@Override
	public void magnifyUpdateMove(AToolBar toolBar, Component canvas, Point start, Point p) {
		System.out.println("moved from " + start + " to " + p);
	}

	@Override
	public void magnifyDoneMove(AToolBar toolBar, Component canvas, Point start, Point end) {
		System.out.println("done move from " + start + " to " + end);
	}

	@Override
	public void recenter(AToolBar toolBar, Component canvas, Point center) {
		System.out.println("Center button clicked at: " + center);
		toolBar.resetDefaultToggleButton();
	}
	
	

}
