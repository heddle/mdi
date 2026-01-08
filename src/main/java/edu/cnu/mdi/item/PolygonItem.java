package edu.cnu.mdi.item;

import java.awt.geom.Point2D;

import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;

/**
 * A closed, filled/stroked polygon defined in world coordinates.
 * <p>
 * The polygon geometry is stored as a world-coordinate {@link java.awt.geom.Path2D}.
 * Selection/reshape modifies vertices of that path.
 * </p>
 */
public class PolygonItem extends PathBasedItem {

	/**
	 * Create a world polygon item
	 *
	 * @param itemList  the layer this item is on.
	 * @param points the points of the polygon
	 */
	public PolygonItem(Layer itemList, Point2D.Double points[]) {
		super(itemList);

		// set the path
		if (points != null) {
			setPath(points);
		}
	}

	/**
	 * Create a world polygon item
	 *
	 * @param itemList the layer this item is on.
	 */
	public PolygonItem(Layer itemList) {
		super(itemList);
	}

	/**
	 * Set the path from a world polygon.
	 *
	 * @param points the points of the polygon.
	 */
	public void setPath(Point2D.Double points[]) {
		_path = WorldGraphicsUtils.worldPolygonToPath(points);
		geometryChanged();
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
		setPath(wpoly);
	}

}
