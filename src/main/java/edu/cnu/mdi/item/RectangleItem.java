package edu.cnu.mdi.item;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;
import edu.cnu.mdi.util.Point2DSupport;

public class RectangleItem extends PolygonItem {

    /**
     * Create a world rectangle object.
     *
     * @param itemList the list this item is on.
     * @param wr       the initial bounds of the item.
     */
    public RectangleItem(ItemList itemList, Rectangle2D.Double wr) {
        super(itemList, WorldGraphicsUtils.getPoints(wr));
    }

    /**
     * Reshape the item based on the modification. Keep in mind that if control or
     * shift was pressed, the item will scale rather than coming here.
     */
    @Override
    protected void reshape() {

        if (_modification == null) {
            return;
        }

        final int j = _modification.getSelectIndex();
        if (j < 0 || j > 3) {
            return;
        }

        final Point2D.Double pp = _modification.getCurrentWorldPoint();
        if (pp == null) {
            return;
        }

        // start from the original geometry at the start of the gesture
        Path2D.Double startPath = _modification.getStartPath();
        if (startPath == null) {
            return;
        }

        _path = (Path2D.Double) startPath.clone();
        Point2D.Double[] wpoly = WorldGraphicsUtils.pathToWorldPolygon(_path);
        if (wpoly == null || wpoly.length < 4) {
            return;
        }

        // Indices: j is dragged corner; i and k are adjacent corners; (j+2)%4 is the opposite corner
        final int i = (j + 3) % 4;
        final int k = (j + 1) % 4;

        Point2D.Double pj = wpoly[j];
        Point2D.Double pi = wpoly[i];
        Point2D.Double pk = wpoly[k];

        // Vector from old corner pj to new corner pp
        Point2D.Double vj = Point2DSupport.pointDelta(pj, pp);

        // Edge directions from pj to adjacent corners
        Point2D.Double a = Point2DSupport.pointDelta(pi, pj);
        Point2D.Double b = Point2DSupport.pointDelta(pk, pj);

        // Project the corner move onto each edge direction
        Point2D.Double vi = Point2DSupport.project(vj, b);
        Point2D.Double vk = Point2DSupport.project(vj, a);

        // Apply updates
        wpoly[j] = pp;
        wpoly[i].x += vi.x;
        wpoly[i].y += vi.y;
        wpoly[k].x += vk.x;
        wpoly[k].y += vk.y;

        // Rebuild path and FORCE closure (critical)
        Path2D.Double newPath = WorldGraphicsUtils.worldPolygonToPath(wpoly);
        if (newPath != null) {
            newPath.closePath();
        }
        _path = newPath;

        // Mark geometry updated
        geometryChanged(); // preferred over updateFocus() + setDirty(true)
    }
}
