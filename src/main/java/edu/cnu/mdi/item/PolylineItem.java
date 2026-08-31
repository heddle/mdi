package edu.cnu.mdi.item;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;

/**
 * An open, stroked-only polyline defined in world coordinates.
 * <p>
 * Unlike {@link PolygonItem}, the path is not closed back to its first
 * point, and the fill color is forced to {@code null} so only the connecting
 * segments are drawn. The geometry is stored as a world-coordinate
 * {@link java.awt.geom.Path2D}; selection/reshape modifies vertices of that
 * path.
 * </p>
 */
public class PolylineItem extends PathBasedItem {

	/**
	 * Create a world polyline item
	 *
	 * @param layer the z layer  this item is on.
	 * @param points   the points of the polyline
	 */
	public PolylineItem(Layer layer, Point2D.Double points[]) {
		this(layer, points, (Object[]) null);
	}

	/**
	 * Create a world polyline item
	 *
	 * @param layer the z layer  this item is on.
	 * @param points   the points of the polyline
	 * @param keyVals  optional key value pairs for styling the item.
	 * See {@link AItem#AItem(Layer, Object...)} for more details.
	 */
	public PolylineItem(Layer layer, Point2D.Double points[], Object... keyVals) {
		super(layer, keyVals);

		// get the path
		_path = WorldGraphicsUtils.worldPolygonToPath(points);
		_focus = WorldGraphicsUtils.getCentroid(_path);

		_style.setFillColor(null);
	}


	/**
	 * Custom drawer for the item.
	 *
	 * @param g2        the graphics context.
	 * @param container the graphical container being rendered.
	 */
	@Override
	public void drawItem(Graphics2D g2, IContainer container) {
		_lastDrawnPolygon = WorldGraphicsUtils.drawPath2D(g2, container, _path, _style, false);

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
