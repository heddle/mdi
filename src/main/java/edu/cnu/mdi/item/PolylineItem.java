package edu.cnu.mdi.item;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;


public class PolylineItem extends PathBasedItem {

	/**
	 * Create a world polyline item
	 *
	 * @param itemList  the list this item is on.
	 * @param points the points of the polygon
	 */
	public PolylineItem(ItemList itemList, Point2D.Double points[]) {
		super(itemList);

		// get the path
		_path = WorldGraphicsUtils.worldPolygonToPath(points);
		_focus = WorldGraphicsUtils.getCentroid(_path);

		_style.setFillColor(null);
	}

	/**
	 * Custom drawer for the item.
	 *
	 * @param g         the graphics context.
	 * @param container the graphical container being rendered.
	 */
	@Override
	public void drawItem(Graphics2D g, IContainer container) {
		_lastDrawnPolygon = WorldGraphicsUtils.drawPath2D(g, container, _path, _style, false);

	}

	/**
	 * Reshape the polygon based on the modification. Not much we can do to a
	 * polygon except move the selected point. Keep in mind that if control or shift
	 * was pressed, the polygon will scale rather than coming here.
	 */
	@Override
	protected void reshape() {
		int index = _modification.getSelectIndex();
		Point2D.Double[] wpoly = WorldGraphicsUtils.pathToWorldPolygon(_path);
		Point2D.Double wp = _modification.getCurrentWorldPoint();
		wpoly[index] = wp;
		_path = WorldGraphicsUtils.worldPolygonToPath(wpoly);
		updateFocus();
	}

}
