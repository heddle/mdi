package edu.cnu.mdi.item;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;

public class PathBasedItem extends AItem {

    // workspace
    protected final Point2D.Double workPoint = new Point2D.Double();

    /**
     * Create an object that is based on a java Path2D object.
     *
     * @param layer the z layer this item is on.
     */
    public PathBasedItem(Layer layer) {
        super(layer);
    }

    @Override
    public void drawItem(Graphics2D g, IContainer container) {
        boolean closed = isClosedPath();
        _lastDrawnPolygon = WorldGraphicsUtils.drawPath2D(g, container, _path, getStyleSafe(), closed);
    }

    /**
     * Is the path closed (i.e. a polygon) or open (i.e. a polyline)?
     *
     * @return true if the path is closed.
     */
    protected boolean isClosedPath() {
        return true; // override for polylines
    }

    /**
     * Pick tolerance in world units (approx. a fixed number of screen pixels).
     *
     * @param c the container being rendered
     * @return pick tolerance in world units
     */
    protected double pickToleranceWorld(IContainer c) {
        final double tolPx = getPickTolerancePx(c); // AItem default is 6.0 unless overridden
        double pixPerWorld = WorldGraphicsUtils.getMeanPixelDensity(c);

        if (pixPerWorld > 1.0e-12) {
            return tolPx / pixPerWorld;
        }

        // Fallback: if density can't be computed (component not realized, etc.),
        // return a small non-zero tolerance in world units so picking still works.
        Rectangle2D.Double wr = getWorldBounds();
        if (wr != null) {
            double diag = Math.hypot(wr.width, wr.height);
            if (diag > 1.0e-12) {
                return 0.01 * diag; // 1% of item diagonal
            }
        }
        return 1.0; // absolute fallback in world units (better than “never pick”)
    }

    @Override
    public boolean shouldDraw(Graphics g, IContainer container) {

        if (_path == null) {
            _lastDrawnPolygon = null;
            return false;
        }

        int count = WorldGraphicsUtils.getPathPointCount(_path);

        if (count == 1) {
            Rectangle spr = singlePointBounds(container);
            Rectangle b = container.getComponent().getBounds();
            b.x = 0;
            b.y = 0;
            return (spr != null) && b.intersects(spr);
        }

        Rectangle2D.Double wr = getWorldBounds();
        if (wr == null || wr.isEmpty()) {
            _lastDrawnPolygon = null;
            return false;
        }

        Rectangle r = new Rectangle();
        container.worldToLocal(r, wr);

        Rectangle b = container.getComponent().getBounds();
        b.x = 0;
        b.y = 0;

        boolean shouldDraw = b.intersects(r);
        if (!shouldDraw) {
            _lastDrawnPolygon = null;
        }

        return shouldDraw;
    }

    @Override
    public boolean contains(IContainer container, Point screenPoint) {

        if (_path == null) {
            return false;
        }
        if (inASelectRect(container, screenPoint)) {
            return true;
        }

        container.localToWorld(screenPoint, workPoint);

        if (isClosedPath()) {
            // true area containment
            return _path.contains(workPoint);
        }

        // open path: “near path” containment with tolerance
        double tolWorld = pickToleranceWorld(container);
        return WorldGraphicsUtils.isPointNearPath(_path, workPoint, tolWorld);
    }

    @Override
    public Rectangle getBounds(IContainer container) {

        if (_path == null) {
            return null;
        }

        int count = WorldGraphicsUtils.getPathPointCount(_path);
        if (count < 1) {
            return null;
        }
        if (count == 1) {
            return singlePointBounds(container);
        }

        Rectangle2D.Double wr = getWorldBounds();
        if (wr == null || wr.isEmpty()) {
            return null;
        }

        Rectangle r = new Rectangle();
        container.worldToLocal(r, wr);
        return r;
    }

    @Override
    public Point getRotatePoint(IContainer container) {

        if (!isRotatable() || _path == null) {
            return null;
        }
        if (_focus == null) {
            updateFocus();
            if (_focus == null) {
                return null;
            }
        }

        Point2D.Double wp = WorldGraphicsUtils.polygonIntersection(_focus, getAzimuth(), _path);
        if (wp == null) {
            return null;
        }

        // extend by ~15 pixels
        double dist = _focus.distance(wp);
        Point pf = getFocusPoint(container);
        Point p1 = new Point();
        container.worldToLocal(p1, wp);

        double dx = p1.x - pf.x;
        double dy = p1.y - pf.y;
        double pixlen = Math.sqrt(dx * dx + dy * dy);

        if (pixlen > 1) {
            dist += 15.0 * dist / pixlen;
            WorldGraphicsUtils.project(_focus, dist, getAzimuth(), wp);
        }

        Point pp = new Point();
        container.worldToLocal(pp, wp);
        return pp;
    }

    @Override
    public void modify() {

        if (_modification == null) {
            return;
        }

        boolean keymod = _modification.isShift() || _modification.isControl();

        switch (_modification.getType()) {

        case DRAG:
            if (!isDraggable()) {
                break;
            }
            _path = (Path2D.Double) _modification.getStartPath().clone();

            Point2D.Double swp = _modification.getStartWorldPoint();
            Point2D.Double cwp = _modification.getCurrentWorldPoint();
            double dx = cwp.x - swp.x;
            double dy = cwp.y - swp.y;

            AffineTransform at = AffineTransform.getTranslateInstance(dx, dy);
            _path.transform(at);

            if (_secondaryPoints != null) {
                Path2D.Double path2 = (Path2D.Double) _modification.getSecondaryPath().clone();
                path2.transform(at);
                WorldGraphicsUtils.pathToWorldPolygon(path2, _secondaryPoints);
            }
            break;

        case ROTATE:
            if (WorldGraphicsUtils.getPathPointCount(_path) < 2) {
                break;
            }

            _path = (Path2D.Double) _modification.getStartPath().clone();

            Point p1 = _modification.getStartMousePoint();
            Point vertex = _modification.getStartFocusPoint();
            Point p2 = _modification.getCurrentMousePoint();
            Point2D.Double anchor = _modification.getStartFocus();

            double angle = threePointAngle(p1, vertex, p2);
            if (Double.isNaN(angle)) {
                break;
            }

            angle = (int) angle; // keep your “snap to int degrees” behavior

            at = AffineTransform.getRotateInstance(Math.toRadians(-angle), anchor.x, anchor.y);
            _path.transform(at);

            if (_secondaryPoints != null) {
                Path2D.Double path2 = (Path2D.Double) _modification.getSecondaryPath().clone();
                path2.transform(at);
                WorldGraphicsUtils.pathToWorldPolygon(path2, _secondaryPoints);
            }

            setAzimuth(_modification.getStartAzimuth() + angle);
            break;

        case RESIZE:
            if (WorldGraphicsUtils.getPathPointCount(_path) < 2) {
                break;
            }

            if (keymod || (_resizePolicy == ResizePolicy.SCALEONLY)) {
                scale();
            } else {
                reshape();
            }
            break;
        }

        geometryChanged(); // updates focus + marks dirty
        _modification.getContainer().refresh();
    }

    public void rotate(double angle) {
        if (_path == null || Math.abs(angle) < 0.05) {
            return;
        }

        double azim = getAzimuth();
        Point2D.Double anchor = WorldGraphicsUtils.getCentroid(_path);
        if (anchor == null) {
            return;
        }

        AffineTransform at = AffineTransform.getRotateInstance(Math.toRadians(-angle), anchor.x, anchor.y);
        _path.transform(at);

        setAzimuth(azim + angle);
        geometryChanged();
    }

    protected void scale() {
        _path = (Path2D.Double) _modification.getStartPath().clone();

        if (_focus == null) {
            updateFocus();
            if (_focus == null) {
                return;
            }
        }

        Point2D.Double startPoint = _modification.getStartWorldPoint();
        Point2D.Double currentPoint = _modification.getCurrentWorldPoint();

        double denom = startPoint.distance(_focus);
        if (denom < 1.0e-12) {
            return;
        }

        double scale = currentPoint.distance(_focus) / denom;

        AffineTransform at = AffineTransform.getTranslateInstance(_focus.x, _focus.y);
        at.concatenate(AffineTransform.getScaleInstance(scale, scale));
        at.concatenate(AffineTransform.getTranslateInstance(-_focus.x, -_focus.y));
        _path.transform(at);

        if (_secondaryPoints != null) {
            Path2D.Double path2 = (Path2D.Double) _modification.getSecondaryPath().clone();
            path2.transform(at);
            WorldGraphicsUtils.pathToWorldPolygon(path2, _secondaryPoints);
        }
    }

    protected void reshape() {
        scale();
    }

    protected void updateFocus() {
        setFocus((_path == null) ? null : WorldGraphicsUtils.getCentroid(_path));
    }

    protected double threePointAngle(Point p1, Point vertex, Point p2) {
        double ax = p1.x - vertex.x;
        double ay = p1.y - vertex.y;
        double bx = p2.x - vertex.x;
        double by = p2.y - vertex.y;

        double a = Math.sqrt(ax * ax + ay * ay);
        if (a < 1.0e-10) {
            return Double.NaN;
        }
        double b = Math.sqrt(bx * bx + by * by);
        if (b < 1.0e-10) {
            return Double.NaN;
        }

        double adotb = ax * bx + ay * by;
        double acrossb = ax * by - ay * bx;

        double cos = adotb / (a * b);
        cos = Math.max(-1.0, Math.min(1.0, cos)); // numeric safety
        double ang = Math.toDegrees(Math.acos(cos));

        if (acrossb < 0.0) {
            ang = 360.0 - ang;
        }
        return ang;
    }

    @Override
    public Rectangle2D.Double getWorldBounds() {
        if (_path == null) {
            return null;
        }
        Rectangle2D r2d = _path.getBounds2D();
        return new Rectangle2D.Double(r2d.getX(), r2d.getY(), r2d.getWidth(), r2d.getHeight());
    }

    protected Rectangle singlePointBounds(IContainer container) {
        if (_path == null) {
            return null;
        }
        Point2D.Double wp = WorldGraphicsUtils.getPathPointAt(0, _path);
        if (wp == null) {
            return null;
        }
        Point pp = new Point();
        container.worldToLocal(pp, wp);
        return new Rectangle(pp.x - 8, pp.y - 8, 16, 16);
    }
}
