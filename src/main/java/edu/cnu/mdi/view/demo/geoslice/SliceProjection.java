package edu.cnu.mdi.view.demo.geoslice;

import java.awt.geom.Point2D;

/**
 * Coordinate conversion utilities for the geometry slice demos.
 *
 * <p>
 * The demos use a simple constant-phi slice through 3D Cartesian geometry. The
 * 2D display convention is:
 * </p>
 *
 * <pre>
 *     slice x = z
 *     slice y = radial distance in the selected phi plane
 * </pre>
 *
 * <p>
 * These methods convert between cylindrical/spherical/Cartesian coordinates and
 * the 2D slice coordinates used by the MDI view. They are public so both the
 * 2D MDI view and the companion MDI-3D explanatory view can use the same model
 * and coordinate conventions.
 * </p>
 */
public final class SliceProjection {

    /** Utility class; not instantiable. */
    private SliceProjection() {
    }

    /**
     * Convert cylindrical coordinates to a 3D Cartesian point.
     *
     * <pre>
     *     x = r cos(phi)
     *     y = r sin(phi)
     *     z = z
     * </pre>
     *
     * @param r radial coordinate
     * @param phiDeg azimuthal angle in degrees
     * @param z z coordinate
     * @return Cartesian 3D point
     */
    public static edu.cnu.mdi.geometry.Point cylindrical(
            double r, double phiDeg, double z) {

        double phi = Math.toRadians(phiDeg);

        return new edu.cnu.mdi.geometry.Point(
                r * Math.cos(phi),
                r * Math.sin(phi),
                z);
    }

    /**
     * Project a 3D point in the selected constant-phi plane into 2D slice
     * coordinates.
     *
     * <pre>
     *     x = z
     *     y = radial coordinate in the selected phi plane
     * </pre>
     *
     * @param p 3D Cartesian point
     * @param phiDeg current slice azimuth, in degrees
     * @return 2D slice point
     */
    public static Point2D.Double projectToSlice(
            edu.cnu.mdi.geometry.Point p, double phiDeg) {

        double phi = Math.toRadians(phiDeg);
        double s = p.x * Math.cos(phi) + p.y * Math.sin(phi);

        return new Point2D.Double(p.z, s);
    }

    /**
     * Convert a 2D slice-coordinate point back into a 3D Cartesian point lying in
     * the current constant-phi plane.
     *
     * <p>
     * If the slice point is {@code (z, s)}, where {@code s} is the radial
     * coordinate in the current phi plane, then:
     * </p>
     *
     * <pre>
     *     x = s cos(phi)
     *     y = s sin(phi)
     *     z = z
     * </pre>
     *
     * @param slicePoint point in 2D slice world coordinates
     * @param phiDeg current slice azimuth, in degrees
     * @return corresponding 3D Cartesian point
     */
    public static edu.cnu.mdi.geometry.Point sliceToCartesian(
            Point2D.Double slicePoint, double phiDeg) {

        double phi = Math.toRadians(phiDeg);
        double s = slicePoint.y;

        double x = s * Math.cos(phi);
        double y = s * Math.sin(phi);
        double z = slicePoint.x;

        return new edu.cnu.mdi.geometry.Point(x, y, z);
    }

    /**
     * Compute the 3D radius from the origin.
     *
     * @param p Cartesian point
     * @return {@code sqrt(x^2 + y^2 + z^2)}
     */
    public static double radius(edu.cnu.mdi.geometry.Point p) {
        return Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z);
    }

    /**
     * Compute the spherical polar angle theta in degrees.
     *
     * <p>
     * The convention is the usual physics/detector convention: {@code theta = 0}
     * points along positive z, and {@code theta = 90} lies in the x-y plane.
     * </p>
     *
     * @param p Cartesian point
     * @return theta in degrees
     */
    public static double thetaDeg(edu.cnu.mdi.geometry.Point p) {
        double r = radius(p);
        if (r < 1.0e-12) {
            return 0.0;
        }

        double costheta = p.z / r;
        costheta = Math.max(-1.0, Math.min(1.0, costheta));

        return Math.toDegrees(Math.acos(costheta));
    }

    /**
     * Compute the spherical azimuthal angle phi in degrees.
     *
     * @param p Cartesian point
     * @return phi in degrees, normalized to {@code (-180, 180]}
     */
    public static double phiDeg(edu.cnu.mdi.geometry.Point p) {
        double phi = Math.toDegrees(Math.atan2(p.y, p.x));
        return normalizeDegrees(phi);
    }

    /**
     * Normalize an angle to the range {@code (-180, 180]}.
     *
     * @param angleDeg angle in degrees
     * @return normalized angle in degrees
     */
    public static double normalizeDegrees(double angleDeg) {
        while (angleDeg > 180.0) {
            angleDeg -= 360.0;
        }
        while (angleDeg <= -180.0) {
            angleDeg += 360.0;
        }
        return angleDeg;
    }
}
