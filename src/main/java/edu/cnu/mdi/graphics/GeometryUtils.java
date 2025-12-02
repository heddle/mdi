package edu.cnu.mdi.graphics;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Utility methods for common 2D geometric operations used by the MDI graphics
 * subsystem.  <p>
 *
 * This class includes helpers for:
 * <ul>
 *     <li>Converting rectangles to point arrays</li>
 *     <li>Constructing rectangles from two corner points</li>
 *     <li>Enforcing fixed aspect ratios while dragging</li>
 *     <li>Point-to-line hit testing with configurable tolerance</li>
 * </ul>
 *
 * <p>These helpers are primitive building blocks used by view containers,
 * rubber-band selection, drawing tools, and interactive geometry components.</p>
 *
 * <p>This class is {@code final} and cannot be instantiated.</p>
 */
public final class GeometryUtils {

    /**
     * Default tolerance (in pixels) for {@link #pointOnLine} hit testing.
     * Values within this distance of the ideal line segment are considered a hit.
     */
    private static final double DEFAULT_SELECT_RES = 3.01;

    /**
     * Private constructor to prevent instantiation.
     */
    private GeometryUtils() { }

    /**
     * Converts a {@link Rectangle} into an array of its four corner points.
     * <p>
     * The points are returned in clockwise order:
     * <ol>
     *     <li>Top-left</li>
     *     <li>Top-right</li>
     *     <li>Bottom-right</li>
     *     <li>Bottom-left</li>
     * </ol>
     *
     * @param rect the rectangle to convert; must not be {@code null}
     * @return an array of exactly four {@link Point} objects
     */
    public static Point[] rectangleToPoints(Rectangle rect) {
        Point[] pp = new Point[4];
        int l = rect.x;
        int t = rect.y;
        int r = l + rect.width;
        int b = t + rect.height;

        pp[0] = new Point(l, t);
        pp[1] = new Point(r, t);
        pp[2] = new Point(r, b);
        pp[3] = new Point(l, b);

        return pp;
    }

    /**
     * Constructs a {@link Rectangle} that spans between two corner points.
     * <p>
     * This method automatically determines the upper-left corner and computes
     * width and height using absolute differences.
     *
     * <p>Behavior with null inputs:</p>
     * <ul>
     *     <li>If both {@code p1} and {@code p2} are {@code null}, returns {@code null}</li>
     *     <li>If exactly one point is {@code null}, returns a zero-sized rectangle at the other</li>
     * </ul>
     *
     * @param p1 the first point; may be {@code null}
     * @param p2 the second point; may be {@code null}
     * @return a {@link Rectangle} spanning {@code p1} and {@code p2},
     *         or {@code null} if both points are {@code null}
     */
    public static Rectangle rectangleFromPoints(Point p1, Point p2) {
        if (p1 == null && p2 == null) {
            return null;
        }
        if (p1 == null) {
            return new Rectangle(p2.x, p2.y, 0, 0);
        }
        if (p2 == null) {
            return new Rectangle(p1.x, p1.y, 0, 0);
        }

        int w = Math.abs(p2.x - p1.x);
        int h = Math.abs(p2.y - p1.y);
        int x = Math.min(p1.x, p2.x);
        int y = Math.min(p1.y, p2.y);
        return new Rectangle(x, y, w, h);
    }

    /**
     * Adjusts a drag endpoint so that the rectangle defined by
     * <code>(p0, p)</code> matches the aspect ratio of the reference rectangle
     * {@code r}. This is used in tools such as fixed-aspect zoom boxes or
     * constrained shape creation.
     *
     * <p>The point {@code p} is modified in place.</p>
     *
     * <p>Aspect ratio rules:</p>
     * <ul>
     *     <li>If {@code r} is taller than wide, constrain width based on height</li>
     *     <li>If {@code r} is wider than tall, constrain height based on width</li>
     *     <li>Sign of the drag is preserved so the rectangle grows in the
     *         correct direction</li>
     * </ul>
     *
     * @param r  a reference rectangle whose aspect ratio will be enforced
     * @param p0 fixed anchor point (the first corner)
     * @param p  mutable drag point (the second corner), modified in place
     */
    public static void rectangleARFixedAdjust(Rectangle r, Point p0, Point p) {
        if (r == null) {
            return;
        }

        double rw = r.getWidth();
        double rh = r.getHeight();

        // If reference rectangle is taller, adjust width based on height.
        if (rw < rh) {
            int signX = (p.x > p0.x) ? 1 : -1;
            double ar = rw / rh;
            double pw = signX * ar * Math.abs(p.y - p0.y);
            p.x = p0.x + (int) pw;
        }
        // Otherwise adjust height based on width.
        else {
            int signY = (p.y > p0.y) ? 1 : -1;
            double ar = rh / rw;
            double ph = signY * ar * Math.abs(p.x - p0.x);
            p.y = p0.y + (int) ph;
        }
    }

    /**
     * Returns a new rectangle using the aspect-ratio-fixed adjustment applied
     * to point {@code p}. This is a convenience wrapper that calls
     * {@link #rectangleARFixedAdjust(Rectangle, Point, Point)} and then
     * constructs a new rectangle from the two points.
     *
     * @param r  the reference rectangle whose aspect ratio is to be preserved
     * @param p0 fixed anchor point (first corner)
     * @param p  drag point (second corner), modified in place
     * @return a new rectangle honoring the aspect ratio of {@code r}
     */
    public static Rectangle rectangleARFixed(Rectangle r, Point p0, Point p) {
        rectangleARFixedAdjust(r, p0, p);
        return rectangleFromPoints(p0, p);
    }

    /**
     * Tests whether a point lies on or very near a finite line segment,
     * using a default tolerance of {@link #DEFAULT_SELECT_RES}.
     *
     * <p>This is useful for hit-testing drawn line segments in GUI tools.</p>
     *
     * @param p     the point to test; may be {@code null}
     * @param start the start of the line segment; may be {@code null}
     * @param end   the end of the line segment; may be {@code null}
     * @return {@code true} if the point is within tolerance of the segment;
     *         {@code false} otherwise
     */
    public static boolean pointOnLine(Point p, Point start, Point end) {
        if (p == null || start == null || end == null) {
            return false;
        }
        return pointOnLine(p.x, p.y, start.x, start.y, end.x, end.y, DEFAULT_SELECT_RES);
    }

    /**
     * Tests whether a point <code>(px,py)</code> lies on or near the line
     * segment connecting <code>(sx,sy)</code> to <code>(ex,ey)</code>,
     * within a specified tolerance measured in pixels. <p>
     *
     * <p>Algorithm:</p>
     * <ul>
     *     <li>Normalize parameter <code>t</code> along the major axis</li>
     *     <li>Reject immediately if <code>t</code> is outside [0,1]</li>
     *     <li>Compute the corresponding point on the line</li>
     *     <li>Check perpendicular distance &lt; tolerance</li>
     * </ul>
     *
     * <p>This method is optimized for interactive drawing tools and uses
     * simple arithmetic to avoid unnecessary divisions where possible.</p>
     *
     * @param px x-coordinate of the test point
     * @param py y-coordinate of the test point
     * @param sx start x-coordinate of the line
     * @param sy start y-coordinate of the line
     * @param ex end x-coordinate of the line
     * @param ey end y-coordinate of the line
     * @param tolerance how far (in pixels) from the ideal line counts as a hit
     * @return {@code true} if the test point is near the segment; {@code false} otherwise
     */
    public static boolean pointOnLine(int px, int py,
                                      int sx, int sy,
                                      int ex, int ey,
                                      double tolerance) {

        int delx = ex - sx;
        int dely = ey - sy;
        int fdelx = Math.abs(delx);
        int fdely = Math.abs(dely);

        // Very tiny segments are ignored
        if (fdelx < 2 && fdely < 2) {
            return false;
        }

        double x = px;
        double y = py;
        double x1 = sx;
        double y1 = sy;
        double dx = delx;
        double dy = dely;

        double t;
        // Parameterize along major axis (avoids divide-by-zero)
        if (fdelx > fdely) {
            t = (x - x1) / dx;
            if (t < 0.0 || t > 1.0) {
                return false;
            }
            double yt = y1 + t * dy;
            return Math.abs(yt - y) < tolerance;
        } else {
            t = (y - y1) / dy;
            if (t < 0.0 || t > 1.0) {
                return false;
            }
            double xt = x1 + t * dx;
            return Math.abs(xt - x) < tolerance;
        }
    }
}
