package edu.cnu.mdi.mapping;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;

/**
 * Mollweide equal-area pseudocylindrical projection.
 * <p>
 * Covers the entire globe, with an elliptical boundary. The central meridian
 * is configurable and defaults to λ₀ = 0.
 * </p>
 * <p>
 * Forward equations (Snyder, spherical Earth, R = 1):
 * <pre>
 *   Solve for θ from:  2θ + sin(2θ) = π sin φ
 *
 *   x = 2√2 / π * (λ - λ₀) cos θ
 *   y = √2 * sin θ
 * </pre>
 * Inverse:
 * <pre>
 *   θ = arcsin(y / √2)
 *   φ = arcsin( (2θ + sin(2θ)) / π )
 *   λ = λ₀ + π x / (2√2 cos θ)
 * </pre>
 * </p>
 */
public class MollweideProjection implements IMapProjection {

    /** Central meridian λ₀ in radians. */
    private double lambda0;

    /** Active theme used for rendering. */
    private MapTheme theme;

    /** Constant √2. */
    private static final double SQRT2 = Math.sqrt(2.0);

    /** Semi-major axis of boundary ellipse in projection space. */
    private static final double A = 2.0 * SQRT2;  // x extent: [-A, A]

    /** Semi-minor axis of boundary ellipse in projection space. */
    private static final double B = SQRT2;        // y extent: [-B, B]

    /** Number of segments used to approximate curves. */
    private static final int NUM_SEGMENTS = 360;

    /**
     * Creates a Mollweide projection with λ₀ = 0 and the default light theme.
     */
    public MollweideProjection() {
        this(0.0, MapTheme.light());
    }

    /**
     * Creates a Mollweide projection with the given central meridian and theme.
     *
     * @param lambda0 central meridian in radians
     * @param theme   map theme (must not be {@code null})
     */
    public MollweideProjection(double lambda0, MapTheme theme) {
        setCentralMeridian(lambda0);
        setTheme(theme);
    }

    /**
     * Sets the central meridian λ₀.
     *
     * @param lambda0 central meridian in radians
     */
    public final void setCentralMeridian(double lambda0) {
        this.lambda0 = lambda0;
    }

    /**
     * Returns the central meridian λ₀ in radians.
     *
     * @return the central meridian
     */
    public double getCentralMeridian() {
        return lambda0;
    }

    @Override
    public void latLonToXY(LatLon latLon, XY xy) {
        double lambda = latLon.lambda();
        double phi    = latLon.phi();

        // Normalize lambda relative to central meridian into [-π, π]
        double dLambda = lambda - lambda0;

        // Handle both [-π, π] and [0, 2π] input longitudes robustly
        while (dLambda <= -Math.PI) {
            dLambda += 2.0 * Math.PI;
        }
        while (dLambda > Math.PI) {
            dLambda -= 2.0 * Math.PI;
        }

        // Solve 2θ + sin(2θ) = π sin φ via Newton-Raphson
        double theta = solveTheta(phi);

        double cosTheta = Math.cos(theta);
        double x = (2.0 * SQRT2 / Math.PI) * dLambda * cosTheta;
        double y = SQRT2 * Math.sin(theta);

        xy.set(x, y);
    }

    @Override
    public void xyToLatLon(XY xy, LatLon latLon) {
        double x = xy.x();
        double y = xy.y();

        // Check ellipse: x^2/a^2 + y^2/b^2 <= 1
        if (!isPointOnMap(xy)) {
            // Outside nominal map; still attempt inverse.
        }

        double theta = Math.asin(Math.max(-1.0, Math.min(1.0, y / SQRT2)));

        double twoTheta = 2.0 * theta;
        double phi = Math.asin((twoTheta + Math.sin(twoTheta)) / Math.PI);

        double cosTheta = Math.cos(theta);
        double lambda;
        if (Math.abs(cosTheta) < 1e-12) {
            lambda = lambda0; // at the poles, longitude is undefined; use central meridian
        } else {
            lambda = lambda0 + (Math.PI * x) / (2.0 * SQRT2 * cosTheta);
        }

        latLon.set(lambda, phi);
    }

    @Override
    public void drawMapOutline(Graphics2D g2, IContainer container) {
        // Elliptical boundary x^2 / A^2 + y^2 / B^2 = 1
        Stroke oldStroke = g2.getStroke();
        java.awt.Color oldColor = g2.getColor();

        g2.setColor(theme.getOutlineColor());
        g2.setStroke(new BasicStroke(theme.getOutlineStrokeWidth()));

        Point2D.Double world = new Point2D.Double();
        Point prev = new Point();
        Point first = new Point();

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double t = 2.0 * Math.PI * i / NUM_SEGMENTS;
            double x = A * Math.cos(t);
            double y = B * Math.sin(t);

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
        double value = (x * x) / (A * A) + (y * y) / (B * B);
        return value <= 1.0 + 1e-9;
    }

    @Override
    public void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude) {
        // Sample longitudes and construct a polyline.
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
        // Sample latitudes and construct a polyline.
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
        // Entire globe is visible; caller is responsible for sensible lat range.
        double phi = latLon.phi();
        return (phi >= -Math.PI / 2.0 - 1e-9) && (phi <= Math.PI / 2.0 + 1e-9);
    }

    @Override
    public EProjection getProjection() {
        return EProjection.MOLLWEIDE;
    }

    @Override
    public Rectangle2D.Double getXYBounds() {
        return new Rectangle2D.Double(-A, -B, 2.0 * A, 2.0 * B);
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

    /**
     * Solve for θ in 2θ + sin(2θ) = π sin φ using Newton-Raphson.
     *
     * @param phi latitude in radians
     * @return θ in radians
     */
    private static double solveTheta(double phi) {
        // Clamp sinφ just in case
        double s = Math.sin(phi);
        if (s >  1.0) s = 1.0;
        if (s < -1.0) s = -1.0;

        double target = Math.PI * s;
        double theta = phi; // good initial guess

        for (int i = 0; i < 10; i++) {
            double f = 2.0 * theta + Math.sin(2.0 * theta) - target;
            double fp = 2.0 + 2.0 * Math.cos(2.0 * theta); // derivative
            if (Math.abs(fp) < 1e-12) {
                break;
            }
            double delta = f / fp;
            theta -= delta;
            if (Math.abs(delta) < 1e-12) {
                break;
            }
        }

        return theta;
    }
    
    @Override
    public Shape createClipShape(IContainer container) {
        Path2D path = new Path2D.Double();
        Point2D.Double world = new Point2D.Double();
        Point p = new Point();

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double t = 2.0 * Math.PI * i / NUM_SEGMENTS;
            double x = A * Math.cos(t);
            double y = B * Math.sin(t);

            world.setLocation(x, y);
            container.worldToLocal(p, world);

            if (i == 0) {
                path.moveTo(p.x, p.y);
            } else {
                path.lineTo(p.x, p.y);
            }
        }
        path.closePath();
        return path;
    }
}
