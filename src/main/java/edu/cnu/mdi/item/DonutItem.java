package edu.cnu.mdi.item;

import java.awt.Point;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;

public class DonutItem extends PolygonItem {

    public DonutItem(ItemList itemList,
                     Point2D.Double wpc,
                     double radiusInner,
                     double radiusOuter,
                     double startAngle,
                     double arcAngle) {

        super(itemList, WorldGraphicsUtils.getDonutPoints(wpc, radiusInner, radiusOuter, startAngle, arcAngle));
        setAzimuth(90 - startAngle - arcAngle / 2.0);
        setFocus(wpc);
    }

    @Override
    public Point[] getSelectionPoints(IContainer container) {
        if (_path == null || container == null) {
            return null;
        }

        Point2D.Double[] wp = WorldGraphicsUtils.pathToWorldPolygon(_path);
        if (wp == null || wp.length < 4) {
            return null;
        }

        int n = wp.length;
        // Donut is NOT closed by duplicating the first point, so no "drop last" here.

        int n2 = n / 2;
        if (n2 < 2) {
            return null;
        }

        Point[] pp = new Point[4];
        pp[0] = new Point(); container.worldToLocal(pp[0], wp[0]);        // inner start
        pp[1] = new Point(); container.worldToLocal(pp[1], wp[n2 - 1]);   // inner end
        pp[2] = new Point(); container.worldToLocal(pp[2], wp[n2]);       // outer end
        pp[3] = new Point(); container.worldToLocal(pp[3], wp[n - 1]);    // outer start
        return pp;
    }
}
