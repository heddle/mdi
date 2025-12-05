package edu.cnu.mdi.mapping;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;

/**
 * Orthographic map projection (spherical Earth).
 * <p>
 * Displays the hemisphere centered at (lambda0, phi0) as a perspective
 * projection onto a plane. The Earth is modeled as a unit sphere, so
 * projected coordinates (x, y) lie within the unit disk:
 * x^2 + y^2 <= 1.
 * </p>
 */
public class OrthographicProjection implements IMapProjection {

    /** Default sampling resolution for graticule polylines. */
    private static final int NUM_SEGMENTS = 180;

    /** Center longitude λ₀ in radians. */
    private double lambda0;

    /** Center latitude φ₀ in radians. */
    private double phi0;

    /** Precomputed sines and cosines for the center. */
    private double sinPhi0;
    private double cosPhi0;

    /** Active theme used for rendering. */
    private MapTheme theme;

    /**
     * Creates an orthographic projection centered on (λ₀ = 0, φ₀ = 0)
     * using the default light theme.
     */
    public OrthographicProjection() {
        this(0.0, 0.0, MapTheme.light());
    }

    /**
     * Creates an orthographic projection centered on the given
     * latitude/longitude using the default light theme.
     *
     * @param center center of projection
     */
    public OrthographicProjection(LatLon center) {
        this(center.lambda(), center.phi(), MapTheme.light());
    }

    /**
     * Creates an orthographic projection centered at (lambda0, phi0)
     * using the specified theme.
     *
     * @param lambda0 center longitude in radians
     * @param phi0    center latitude in radians
     * @param theme   map theme (must not be {@code null})
     */
    public OrthographicProjection(double lambda0, double phi0, MapTheme theme) {
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
     * Returns the center of the projection.
     *
     * @return a {@link LatLon} representing the projection center
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

        // Visible hemisphere test (dot product >= 0)
        double cosC = sinPhi0 * sinPhi + cosPhi0 * cosPhi * cosDLambda;
        if (cosC < 0.0) {
            // Point is on the far side; you may choose to set NaN or
            // leave as-is. Here we project anyway; caller should use
            // isPointVisible for visibility decisions.
        }

        double x = cosPhi * sinDLambda;
        double y = cosPhi0 * sinPhi - sinPhi0 * cosPhi * cosDLambda;

        xy.set(x, y);
    }

    @Override
    public void xyToLatLon(XY xy, LatLon latLon) {
        double x = xy.x();
        double y = xy.y();

        double rho2 = x * x + y * y;
        if (rho2 > 1.0) {
            // Outside projection disk; result undefined.
            // We still compute but caller should check isPointOnMap.
        }

        double rho = Math.sqrt(rho2);
        double c = Math.asin(Math.min(1.0, rho)); // central angle

        if (rho == 0.0) {
            // Center point maps to projection center
            latLon.set(lambda0, phi0);
            return;
        }

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
        // The visible Earth is a unit circle in projection space:
        // x^2 + y^2 = 1

        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();

        g2.setColor(theme.getOutlineColor());
        g2.setStroke(new BasicStroke(theme.getOutlineStrokeWidth()));

        Point2D.Double world = new Point2D.Double();
        Point prev = new Point();
        Point first = new Point();

        // Approximate the circle with a polyline
        int n = NUM_SEGMENTS;
        for (int i = 0; i <= n; i++) {
            double theta = (2.0 * Math.PI * i) / n;
            double x = Math.cos(theta);
            double y = Math.sin(theta);

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
        return x * x + y * y <= 1.0 + 1e-9; // small epsilon
    }

    @Override
    public void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude) {
        // We draw a polyline by sampling longitude.
        // Only points on the near hemisphere are drawn.

        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();

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
                // Break the line when we go to far side
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
        // Sample latitude from -π/2 to +π/2.
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();

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
        // Visible if central angle <= 90°, i.e. cos(c) >= 0
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
        return EProjection.ORTHOGRAPHIC;
    }

    @Override
    public Rectangle2D.Double getXYBounds() {
        // Projection is the unit disk; extents are [-1, 1] in both x and y.
        return new Rectangle2D.Double(-1.0, -1.0, 2.0, 2.0);
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
