/**
 *
 */
package edu.cnu.mdi.item;

import java.awt.Point;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;

/**
 * @author heddle
 *
 */
public class EllipseItem extends PolygonItem {
	/**
	 * Create a world rectangle object.
	 *
	 * @param itemList the list this item is on.
	 * @param w        the width of the ellipse
	 * @param h        the height of the ellipse
	 * @param azimuth  the rotation of the ellipse in degrees. 0 is north, 90 east,
	 *                 etc.
	 * @param center   the center of the ellipse.
	 */
	public EllipseItem(ItemList itemList, double w, double h, double azimuth, Point2D.Double center) {
		super(itemList, WorldGraphicsUtils.getEllipsePoints(w, h, azimuth, 10, center));
	}

	/**
	 * Obtain the selection points used to indicate this item is selected.
	 *
	 * @return the selection points used to indicate this item is selected.
	 */
	@Override
	public Point[] getSelectionPoints(IContainer container) {
		// if the item cached a last drawn polygon lets use it--it better be
		// right!

		if ((_lastDrawnPolygon != null) && (_lastDrawnPolygon.npoints > 1)) {
			Point pp[] = new Point[4];
			int del = _lastDrawnPolygon.npoints / 4;
			for (int i = 0; i < 4; i++) {
				int j = i * del;
				pp[i] = new Point(_lastDrawnPolygon.xpoints[j], _lastDrawnPolygon.ypoints[j]);
			}
			return pp;
		}

		return null;
	}

}
