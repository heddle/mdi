package edu.cnu.mdi.container;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;

import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.EllipseItem;
import edu.cnu.mdi.item.ItemList;
import edu.cnu.mdi.item.LineItem;
import edu.cnu.mdi.item.PolygonItem;
import edu.cnu.mdi.item.PolylineItem;
import edu.cnu.mdi.item.RadArcItem;
import edu.cnu.mdi.item.RectangleItem;


public class DrawingContainer extends BaseContainer {

	public DrawingContainer(Double worldSystem) {
		super(worldSystem);
	}

	/**
	 * From a given screen rectangle, create an ellipse item.
	 *
	 * @param itemList the list to put the item on
	 * @param rect  the bounding screen rectangle, probably from rubber banding.
	 * @return the new item
	 */
	public AItem createEllipseItem(ItemList itemList, Rectangle rect) {
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
		localToWorld(p0, wp0);
		localToWorld(p1, wp1);
		double width = wp0.distance(wp1);

		p0.setLocation(xc, t);
		p1.setLocation(xc, b);
		localToWorld(p0, wp0);
		localToWorld(p1, wp1);
		double height = wp0.distance(wp1);

		Point pc = new Point(xc, yc);
		Point2D.Double center = new Point2D.Double();
		localToWorld(pc, center);

		return new EllipseItem(itemList, width, height, 0.0, center);
	}

	/**
	 * From a given screen rectangle, create a rectangle item.
	 *
	 * @param itemList the list to put the item on
	 * @param b     the screen rectangle, probably from rubber banding.
	 * @return the new item
	 */
	public AItem createRectangleItem(ItemList itemList, Rectangle b) {
		Rectangle2D.Double wr = new Rectangle2D.Double();
		localToWorld(b, wr);
		return new RectangleItem(itemList, wr);
	}

	/**
	 * From two given screen points, create a line item
	 *
	 * @param itemList the list to put the item on
	 * @param p0    one screen point, probably from rubber banding.
	 * @param p1    another screen point, probably from rubber banding.
	 * @return the new item
	 */
	public AItem createLineItem(ItemList itemList, Point p0, Point p1) {
		Point2D.Double wp0 = new Point2D.Double();
		Point2D.Double wp1 = new Point2D.Double();
		localToWorld(p0, wp0);
		localToWorld(p1, wp1);
		return new LineItem(itemList, wp0, wp1);
	}

	/**
	 * Create a radarc item from the given parameters, probably obtained by
	 * rubberbanding.
	 *
	 * @param itemList the list to put the item on
	 * @param pc       the center of the arc
	 * @param p1       the point at the end of the first leg. Thus pc->p1 determine
	 *                 the radius.
	 * @param arcAngle the opening angle COUNTERCLOCKWISE in degrees.
	 * @return the new item
	 */
	public AItem createRadArcItem(ItemList itemList, Point pc, Point p1, double arcAngle) {
		Point2D.Double wpc = new Point2D.Double();
		Point2D.Double wp1 = new Point2D.Double();
		localToWorld(pc, wpc);
		localToWorld(p1, wp1);
		return new RadArcItem(itemList, wpc, wp1, arcAngle);
		// return new ArcItem(layer, wpc, wp1, arcAngle);
	}

	/**
	 * From a given screen polygon, create a polygon item.
	 *
	 * @param itemList the list to put the item on
	 * @param pp    the screen polygon, probably from rubber banding.
	 * @return the new item
	 */
	public AItem createPolygonItem(ItemList itemList, Point pp[]) {
		if ((pp == null) || (pp.length < 3)) {
			return null;
		}
		Point2D.Double wp[] = new Point2D.Double[pp.length];
		for (int index = 0; index < pp.length; index++) {
			wp[index] = new Point2D.Double();
			localToWorld(pp[index], wp[index]);
		}

		return new PolygonItem(itemList, wp);
	}

	/**
	 * From a given screen polygon, create a polyline item.
	 *
	 * @param itemList the list to put the item on
	 * @param pp    the screen polyline, probably from rubber banding.
	 * @return the new item
	 */
	public AItem createPolylineItem(ItemList itemList, Point pp[]) {
		if ((pp == null) || (pp.length < 3)) {
			return null;
		}

		Point2D.Double wp[] = new Point2D.Double[pp.length];
		for (int index = 0; index < pp.length; index++) {
			wp[index] = new Point2D.Double();
			localToWorld(pp[index], wp[index]);
		}

		AItem item = new PolylineItem(itemList, wp);

		return item;
	}

}
