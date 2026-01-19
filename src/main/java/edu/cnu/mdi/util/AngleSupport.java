package edu.cnu.mdi.util;

import java.awt.geom.Point2D;

/**
 * Angle helper utilities used by radial arc tools/items.
 * <p>
 * The main convenience is to compute a unique CCW sweep angle in the range
 * {@code [0, 360)} from two vectors using the robust {@code atan2(cross, dot)}
 * formulation.
 * </p>
 *
 * <h2>Conventions</h2>
 * <ul>
 *   <li>Angles are in degrees unless otherwise stated.</li>
 *   <li>CCW sweep is reported in {@code [0, 360)}.</li>
 *   <li>The caller controls coordinate handedness by the vectors it provides.
 *       (For screen coordinates where +y is down, callers usually flip y.)</li>
 * </ul>
 */
public final class AngleSupport {

    private AngleSupport() {
    }

    /**
     * Compute the signed angle from v1 to v2 in degrees using atan2(cross, dot).
     * Range is {@code (-180, 180]}.
     *
     * @param v1 first vector
     * @param v2 second vector
     * @return signed angle in degrees in {@code (-180, 180]}
     */
    public static double signedAngleDeg(Point2D.Double v1, Point2D.Double v2) {
        final double dot = v1.x * v2.x + v1.y * v2.y;
        final double cross = v1.x * v2.y - v1.y * v2.x;
        return Math.toDegrees(Math.atan2(cross, dot));
    }

    /**
     * Compute the CCW sweep from v1 to v2 in degrees.
     * <p>
     * This returns a unique angle in {@code [0, 360)}. In particular, a signed
     * angle of {@code -90} becomes {@code 270}.
     * </p>
     *
     * @param v1 first vector
     * @param v2 second vector
     * @return CCW sweep in degrees in {@code [0, 360)}
     */
    public static double ccwSweepDeg(Point2D.Double v1, Point2D.Double v2) {
        double a = signedAngleDeg(v1, v2); // (-180,180]
        if (a < 0.0) {
            a += 360.0;
        }
        // Map +360 back to 0 if ever produced by numerical noise
        if (a >= 360.0) {
            a -= 360.0;
        }
        return a;
    }

    /**
     * Convenience: vector from center to point.
     *
     * @param center center point
     * @param p      other point
     * @return vector p - center
     */
    public static Point2D.Double vec(Point2D.Double center, Point2D.Double p) {
        return new Point2D.Double(p.x - center.x, p.y - center.y);
    }

    /**
     * Same as {@link #vec(Point2D.Double, Point2D.Double)} but flips Y.
     * <p>
     * Useful when converting screen-space deltas (where +y is down) into
     * math-space vectors (where +y is up).
     * </p>
     *
     * @param center center point
     * @param p      other point
     * @return vector (dx, -dy)
     */
    public static Point2D.Double vecFlipY(Point2D.Double center, Point2D.Double p) {
        return new Point2D.Double(p.x - center.x, -(p.y - center.y));
    }
}
