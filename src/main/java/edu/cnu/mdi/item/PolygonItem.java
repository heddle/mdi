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
	 * Create a world polygon item.
	 *
	 * @param itemList the list this item is on.
	 * @param points   the points of the polygon (world coordinates).
	 */
	public PolygonItem(ItemList itemList, Point2D.Double[] points) {
		super(itemList);
		if (points != null) {
			setPath(points);
		}
	}

	/**
	 * Create an empty world polygon item (path may be set later).
	 *
	 * @param itemList the list this item is on.
	 */
	public PolygonItem(ItemList itemList) {
		super(itemList);
	}

	/**
	 * Set the path from a world polygon.
	 *
	 * @param points the points of the polygon (world coordinates).
	 */
	public void setPath(Point2D.Double[] points) {
		_path = WorldGraphicsUtils.worldPolygonToPath(points);
		geometryChanged(); // updates focus + marks dirty
	}

	@Override
	protected void updateFocus() {
		_focus = (_path == null) ? null : WorldGraphicsUtils.getCentroid(_path);
	}

	/**
	 * Reshape the polygon by moving the selected vertex to the current world point.
	 * <p>
	 * If Ctrl/Shift is held, scaling logic happens elsewhere (so this method is not used).
	 * </p>
	 */
	@Override
	protected void reshape() {
		if (_path == null || _modification == null) {
			return;
		}

		int index = _modification.getSelectIndex();
		Point2D.Double[] wpoly = WorldGraphicsUtils.pathToWorldPolygon(_path);
		if (wpoly == null || index < 0 || index >= wpoly.length) {
			return;
		}

		wpoly[index] = _modification.getCurrentWorldPoint();
		setPath(wpoly);
	}

	@Override
	protected boolean isClosedPath() {
		return true;
	}
}
