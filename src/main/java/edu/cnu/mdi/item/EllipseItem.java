package edu.cnu.mdi.item;


import java.awt.Point;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;

public class EllipseItem extends PolygonItem {

    private double _w;
    private double _h;
    private double _azimuth;
    private Point2D.Double _center;

    public EllipseItem(Layer layer, double w, double h, double azimuth, Point2D.Double center) {
        super(layer, null);
        _w = w;
        _h = h;
        _azimuth = azimuth;
        _center = (center == null) ? null : (Point2D.Double) center.clone();
        rebuildPath();
    }

    private void rebuildPath() {
        if (_center == null) {
            _path = null;
            return;
        }
        setPath(WorldGraphicsUtils.getEllipsePoints(_w, _h, _azimuth, 10, _center));
        // setPath sets _path and _focus via centroid; for ellipse we *want* center:
        setFocus(_center);
    }

    @Override
    protected void updateFocus() {
        setFocus(_center);
    }

    @Override
    protected void reshape() {
        if (_modification == null || _center == null) {
            return;
        }

        int j = _modification.getSelectIndex(); // 0..3 from your getSelectionPoints()
        Point2D.Double wp = _modification.getCurrentWorldPoint();
        if (wp == null) {
            return;
        }

        // Interpret handles as: 0=top, 1=right, 2=bottom, 3=left (your code uses quarter points)
        // We update width/height by projecting onto ellipse axes.
        double dx = wp.x - _center.x;
        double dy = wp.y - _center.y;

        // Convert world delta into ellipse-local coordinates (rotate by -azimuth)
        double th = Math.toRadians(_azimuth);
        double cos = Math.cos(th);
        double sin = Math.sin(th);

        double localX =  dx * cos + dy * sin;
        double localY = -dx * sin + dy * cos;

        // For top/bottom handles adjust height; for left/right adjust width
        if (j == 0 || j == 2) {
            _h = Math.max(1.0e-9, 2.0 * Math.abs(localY));
        } else {
            _w = Math.max(1.0e-9, 2.0 * Math.abs(localX));
        }

        rebuildPath();
    }

    @Override
    public Point[] getSelectionPoints(IContainer container) {
        if (container == null || _center == null) {
            return null;
        }

        double a = _w / 2.0;   // semi-width
        double b = _h / 2.0;   // semi-height

        // Your azimuth convention: 0=north, 90=east (WorldGraphicsUtils.project uses that)
        Point2D.Double top    = new Point2D.Double();
        Point2D.Double right  = new Point2D.Double();
        Point2D.Double bottom = new Point2D.Double();
        Point2D.Double left   = new Point2D.Double();

        WorldGraphicsUtils.project(_center, b, 0.0   + _azimuth, top);
        WorldGraphicsUtils.project(_center, a, 90.0  + _azimuth, right);
        WorldGraphicsUtils.project(_center, b, 180.0 + _azimuth, bottom);
        WorldGraphicsUtils.project(_center, a, 270.0 + _azimuth, left);

        Point[] pp = new Point[4];
        pp[0] = new Point(); container.worldToLocal(pp[0], top);
        pp[1] = new Point(); container.worldToLocal(pp[1], right);
        pp[2] = new Point(); container.worldToLocal(pp[2], bottom);
        pp[3] = new Point(); container.worldToLocal(pp[3], left);
        return pp;
    }

}

