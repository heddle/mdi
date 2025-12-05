package edu.cnu.mdi.mapping;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;

/**
 * Lambert azimuthal equal-area projection (spherical Earth).
 * <p>
 * Shows one hemisphere centered on (λ₀, φ₀) as a disk of radius √2.
 * Equal-area but not conformal.
 * </p>
 * <p>
 * Forward equations (Snyder, spherical Earth, R = 1):
 * <pre>
 *   k = sqrt(2 / (1 + sin φ₀ sin φ + cos φ₀ cos φ cos Δλ))
 *
 *   x = k cos φ sin Δλ
 *   y = k (cos φ₀ sin φ - sin φ₀ cos φ cos Δλ)
 * </pre>
 * where Δλ = λ - λ₀ and only the near hemisphere (cos c ≥ 0) is visible.
 * </p>
 */
public class LambertEqualAreaProjection implements IMapProjection {

    /** Center longitude λ₀ in radians. */
    private double lambda0;

    /** Center latitude φ₀ in radians. */
    private double phi0;

    /** Precomputed sines and cosines for the center. */
    private double sinPhi0;
    private double cosPhi0;

    /** Active theme used for rendering. */
    private MapTheme theme;

    /** Number of segments used for approximating curves. */
    private static final int NUM_SEGMENTS = 180;

    /** Maximum radius in projection space for visible hemisphere (ρ ≤ √2). */
    private static final double R_MAX = Math.sqrt(2.0);

    /**
     * Creates a Lambert equal-area projection centered at (0, 0)
     * with the default light theme.
     */
    public LambertEqualAreaProjection() {
        this(0.0, 0.0, MapTheme.light());
    }

    /**
     * Creates a Lambert equal-area projection centered at the given location
     * with the default light theme.
     *
     * @param center projection center (λ, φ) in radians
     */
    public LambertEqualAreaProjection(LatLon center) {
        this(center.lambda(), center.phi(), MapTheme.light());
    }

    /**
     * Creates a Lambert equal-area projection centered at (λ₀, φ₀) with
     * the given theme.
     *
     * @param lambda0 center longitude in radians
     * @param phi0    center latitude in radians
     * @param theme   map theme (must not be {@code null})
     */
    public LambertEqualAreaProjection(double lambda0, double phi0, MapTheme theme) {
        setCenter(lambda0, phi0);
        setTheme(theme);
    }

    /**
     * Sets the center of the projection.
     *
     * @param lambda0 center longitude in radians
     * @param phi0    center latitude in radians
     */
    public final void setCenter(double lambda0, double phi0) {
        this.lambda0 = lambda0;
        this.phi0 = phi0;
        this.sinPhi0 = Math.sin(phi0);
        this.cosPhi0 = Math.cos(phi0);
    }

    /**
     * Returns the projection center.
     *
     * @return a new {@link LatLon} representing the center
     */
    public LatLon getCenter() {
        return new LatLon(lambda0, phi0);
    }

    @Override
    public void latLonToXY(LatLon latLon, XY xy) {
        double lambda = latLon.lambda();
        double phi    = latLon.phi();

        double sinPhi = Math.sin(phi);
        double cosPhi = Math.cos(phi);
        double dLambda = lambda - lambda0;
        double cosDLambda = Math.cos(dLambda);
        double sinDLambda = Math.sin(dLambda);

        double denom = 1.0 + sinPhi0 * sinPhi + cosPhi0 * cosPhi * cosDLambda;
        // For the antipode, denom → 0; that point is not visible anyway.
        double k = Math.sqrt(2.0 / denom);

        double x = k * cosPhi * sinDLambda;
        double y = k * (cosPhi0 * sinPhi - sinPhi0 * cosPhi * cosDLambda);

        xy.set(x, y);
    }

    @Override
    public void xyToLatLon(XY xy, LatLon latLon) {
        double x = xy.x();
        double y = xy.y();

        double rho2 = x * x + y * y;
        double rho = Math.sqrt(rho2);

        if (rho < 1e-12) {
            // Center point maps to projection center
            latLon.set(lambda0, phi0);
            return;
        }

        // c is the central angle from the projection center
        // ρ^2 = 2(1 - cos c) => cos c = 1 - ρ^2 / 2, sin c from ρ = 2 sin(c/2)
        double c = 2.0 * Math.asin(Math.min(1.0, rho / 2.0));
        double sinC = Math.sin(c);
        double cosC = Math.cos(c);

        double phi = Math.asin(
                cosC * sinPhi0 + (y * sinC * cosPhi0 / rho)
        );

        double lambda = lambda0 + Math.atan2(
                x * sinC,
                rho * cosPhi0 * cosC - y * sinPhi0 * sinC
        );

        latLon.set(lambda, phi);
    }

    @Override
    public void drawMapOutline(Graphics2D g2, IContainer container) {
        // Visible hemisphere is a disk of radius √2 in projection space.
        Stroke oldStroke = g2.getStroke();
        java.awt.Color oldColor = g2.getColor();

        g2.setColor(theme.getOutlineColor());
        g2.setStroke(new BasicStroke(theme.getOutlineStrokeWidth()));

        Point2D.Double world = new Point2D.Double();
        Point prev = new Point();
        Point first = new Point();

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double theta = 2.0 * Math.PI * i / NUM_SEGMENTS;
            double x = R_MAX * Math.cos(theta);
            double y = R_MAX * Math.sin(theta);

            world.setLocation(x, y);
            Point p = new Point();
            container.worldToLocal(p, world);

            if (i == 0) {
                first.setLocation(p);
                prev.setLocation(p);
            } else {
                g2.drawLine(prev.x, prev.y, p.x, p.y);
                prev.setLocation(p);
            }
        }

        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    @Override
    public boolean isPointOnMap(XY xy) {
        double x = xy.x();
        double y = xy.y();
        double rho2 = x * x + y * y;
        return rho2 <= (R_MAX * R_MAX + 1e-9);
    }

    @Override
    public void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude) {
        Stroke oldStroke = g2.getStroke();
        java.awt.Color oldColor = g2.getColor();

        g2.setColor(theme.getGraticuleColor());
        g2.setStroke(new BasicStroke(theme.getGraticuleStrokeWidth()));

        LatLon latLon = new LatLon();
        XY xy = new XY();
        Point2D.Double world = new Point2D.Double();
        Point prevPoint = null;

        double lonMin = -Math.PI;
        double lonMax =  Math.PI;

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double t = (double) i / NUM_SEGMENTS;
            double lambda = lonMin + t * (lonMax - lonMin);

            latLon.set(lambda, latitude);

            if (!isPointVisible(latLon)) {
                prevPoint = null;
                continue;
            }

            latLonToXY(latLon, xy);
            if (!isPointOnMap(xy)) {
                prevPoint = null;
                continue;
            }

            world.setLocation(xy.x(), xy.y());
            Point p = new Point();
            container.worldToLocal(p, world);

            if (prevPoint != null) {
                g2.drawLine(prevPoint.x, prevPoint.y, p.x, p.y);
            }
            prevPoint = p;
        }

        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    @Override
    public void drawLongitudeLine(Graphics2D g2, IContainer container, double longitude) {
        Stroke oldStroke = g2.getStroke();
        java.awt.Color oldColor = g2.getColor();

        g2.setColor(theme.getGraticuleColor());
        g2.setStroke(new BasicStroke(theme.getGraticuleStrokeWidth()));

        LatLon latLon = new LatLon();
        XY xy = new XY();
        Point2D.Double world = new Point2D.Double();
        Point prevPoint = null;

        double latMin = -Math.PI / 2.0;
        double latMax =  Math.PI / 2.0;

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double t = (double) i / NUM_SEGMENTS;
            double phi = latMin + t * (latMax - latMin);

            latLon.set(longitude, phi);

            if (!isPointVisible(latLon)) {
                prevPoint = null;
                continue;
            }

            latLonToXY(latLon, xy);
            if (!isPointOnMap(xy)) {
                prevPoint = null;
                continue;
            }

            world.setLocation(xy.x(), xy.y());
            Point p = new Point();
            container.worldToLocal(p, world);

            if (prevPoint != null) {
                g2.drawLine(prevPoint.x, prevPoint.y, p.x, p.y);
            }
            prevPoint = p;
        }

        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    @Override
    public boolean isPointVisible(LatLon latLon) {
        // Visible if central angle c ≤ π/2, i.e. cos c >= 0.
        double lambda = latLon.lambda();
        double phi    = latLon.phi();

        double sinPhi = Math.sin(phi);
        double cosPhi = Math.cos(phi);
        double dLambda = lambda - lambda0;
        double cosDLambda = Math.cos(dLambda);

        double cosC = sinPhi0 * sinPhi + cosPhi0 * cosPhi * cosDLambda;
        return cosC >= 0.0;
    }

    @Override
    public EProjection getProjection() {
        return EProjection.LAMBERT_EQUAL_AREA;
    }

    @Override
    public Rectangle2D.Double getXYBounds() {
        // Disk radius √2 ⇒ square [-√2, √2] × [-√2, √2].
        return new Rectangle2D.Double(-R_MAX, -R_MAX, 2.0 * R_MAX, 2.0 * R_MAX);
    }

    @Override
    public MapTheme getTheme() {
        return theme;
    }

    @Override
    public void setTheme(MapTheme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("MapTheme must not be null");
        }
        this.theme = theme;
    }
}
