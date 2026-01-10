package edu.cnu.mdi.graphics.toolbar;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.EllipseItem;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.item.LineItem;
import edu.cnu.mdi.item.PolygonItem;
import edu.cnu.mdi.item.PolylineItem;
import edu.cnu.mdi.item.RadArcItem;
import edu.cnu.mdi.item.RectangleItem;

@SuppressWarnings("serial")
public class DrawingToolSupport {

	/**
	 * From a given screen rectangle, create an ellipse item.
	 *
	 * @param layer the z layer to put the item on
	 * @param rect  the bounding screen rectangle, probably from rubber banding.
	 * @return the new item
	 */
	public static AItem createEllipseItem(Layer layer, Rectangle rect) {

		IContainer container = layer.getContainer();
		int l = rect.x;
		int t = rect.y;
		int r = l + rect.width;
		int b = t + rect.height;

		int xc = (l + r) / 2;
		int yc = (t + b) / 2;

		Point p0 = new Point(l, yc);
		Point p1 = new Point(r, yc);

		Point2D.Double wp0 = new Point2D.Double();
		Point2D.Double wp1 = new Point2D.Double();
		container.localToWorld(p0, wp0);
		container.localToWorld(p1, wp1);
		double width = wp0.distance(wp1);

		p0.setLocation(xc, t);
		p1.setLocation(xc, b);
		container.localToWorld(p0, wp0);
		container.localToWorld(p1, wp1);
		double height = wp0.distance(wp1);

		Point pc = new Point(xc, yc);
		Point2D.Double center = new Point2D.Double();
		container.localToWorld(pc, center);

		return new EllipseItem(layer, width, height, 0.0, center);
	}

	/**
	 * From a given screen rectangle, create a rectangle item.
	 *
	 * @param layer the z layer to put the item on
	 * @param b     the screen rectangle, probably from rubber banding.
	 * @return the new item
	 */
	public static AItem createRectangleItem(Layer layer, Rectangle b) {
		IContainer container = layer.getContainer();
		Rectangle2D.Double wr = new Rectangle2D.Double();
		container.localToWorld(b, wr);
		return new RectangleItem(layer, wr);
	}

	/**
	 * From two given screen points, create a line item
	 *
	 * @param layer the z layer to put the item on
	 * @param p0    one screen point, probably from rubber banding.
	 * @param p1    another screen point, probably from rubber banding.
	 * @return the new item
	 */
	public static AItem createLineItem(Layer layer, Point p0, Point p1) {
		IContainer container = layer.getContainer();
		Point2D.Double wp0 = new Point2D.Double();
		Point2D.Double wp1 = new Point2D.Double();
		container.localToWorld(p0, wp0);
		container.localToWorld(p1, wp1);
		return new LineItem(layer, wp0, wp1);
	}

	/**
	 * Create a radarc item from the given parameters, probably obtained by
	 * rubberbanding.
	 *
	 * @param layer    the z layer to put the item on
	 * @param pc       the center of the arc
	 * @param p1       the point at the end of the first leg. Thus pc->p1 determine
	 *                 the radius.
	 * @param arcAngle the opening angle COUNTERCLOCKWISE in degrees.
	 * @return the new item
	 */
	public static AItem createRadArcItem(Layer layer, Point pc, Point p1, double arcAngle) {
		IContainer container = layer.getContainer();
		Point2D.Double wpc = new Point2D.Double();
		Point2D.Double wp1 = new Point2D.Double();
		container.localToWorld(pc, wpc);
		container.localToWorld(p1, wp1);
		return new RadArcItem(layer, wpc, wp1, arcAngle);
		// return new ArcItem(layer, wpc, wp1, arcAngle);
	}

	/**
	 * From a given screen polygon, create a polygon item.
	 *
	 * @param layer the z layer to put the item on
	 * @param pp    the screen polygon, probably from rubber banding.
	 * @return the new item
	 */
	public static AItem createPolygonItem(Layer layer, Point pp[]) {
		IContainer container = layer.getContainer();
		if ((pp == null) || (pp.length < 3)) {
			return null;
		}
		Point2D.Double wp[] = new Point2D.Double[pp.length];
		for (int index = 0; index < pp.length; index++) {
			wp[index] = new Point2D.Double();
			container.localToWorld(pp[index], wp[index]);
		}

		return new PolygonItem(layer, wp);
	}

	/**
	 * From a given screen polygon, create a polyline item.
	 *
	 * @param layer the z layer to put the item on
	 * @param pp    the screen polyline, probably from rubber banding.
	 * @return the new item
	 */
	public static AItem createPolylineItem(Layer layer, Point pp[]) {
		IContainer container = layer.getContainer();
		if ((pp == null) || (pp.length < 3)) {
			return null;
		}

		Point2D.Double wp[] = new Point2D.Double[pp.length];
		for (int index = 0; index < pp.length; index++) {
			wp[index] = new Point2D.Double();
			container.localToWorld(pp[index], wp[index]);
		}

		AItem item = new PolylineItem(layer, wp);

		return item;
	}

}
