package edu.cnu.mdi.mapping;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;

/**
 * Mollweide equal-area projection (spherical Earth, R = 1).
 * <p>
 * Forward equations (Snyder):
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
 */
public class MollweideProjection implements IMapProjection {

    /** Sphere radius (normalized). */
    private static final double R = 1.0;

    /** Numerical constants. */
    private static final double SQRT2 = Math.sqrt(2.0);
    private static final double A = 2.0 * SQRT2;  // semi-major axis (x extent)
    private static final double B = SQRT2;        // semi-minor axis (y extent)

    /** Iteration parameters for solving θ. */
    private static final int MAX_NEWTON_ITERS = 20;
    private static final double NEWTON_TOL = 1e-12;

    /** Maximum latitude allowed (avoid pathological inputs). */
    private static final double MAX_LAT = Math.toRadians(89.999);
    private static final double MIN_LAT = -MAX_LAT;

    /** Central meridian λ₀ in radians. */
    private double lambda0 = Math.toRadians(-70);

    /** Active theme. */
    private MapTheme theme;

    /** Number of segments for approximating curves. */
    private static final int NUM_SEGMENTS = 360;


    /**
     * Creates a Mollweide projection with the given central meridian and theme.
     *
     * @param lambda0 central meridian in radians
     * @param theme   map theme; must not be {@code null}
     */
    public MollweideProjection(MapTheme theme) {
        setTheme(theme);
    }

    /**
     * Solve for θ from {@code 2θ + sin(2θ) = π sin φ} using Newton-Raphson.
     *
     * @param phi latitude φ in radians
     * @return θ in radians
     */
    private static double solveTheta(double phi) {
        // Clamp phi for numerical safety
        phi = Math.max(MIN_LAT, Math.min(MAX_LAT, phi));

        if (Math.abs(phi) < 1e-12) {
            return 0.0;
        }

        double target = Math.PI * Math.sin(phi);
        double theta = phi; // reasonable initial guess

        for (int i = 0; i < MAX_NEWTON_ITERS; i++) {
            double twoTheta = 2.0 * theta;
            double f = twoTheta + Math.sin(twoTheta) - target;
            double fp = 2.0 + 2.0 * Math.cos(twoTheta);

            double delta = f / fp;
            theta -= delta;

            if (Math.abs(delta) < NEWTON_TOL) {
                break;
            }
        }

        return theta;
    }


    /**
     * Get the central longitude λ₀ (in radians).
     *
     * @return the central longitude in radians
     */
    public double getCentralLongitude() {
        return lambda0;
    }

    /**
     * Set the central longitude λ₀ (in radians).
     * <p>
     * The value is normalized using {@code wrapLongitude} so that it lies
     * within the conventional range (-π, π].
     *
     * @param centralLongitude the desired central longitude in radians
     */
    public void setCentralLongitude(double centralLongitude) {
        // Use the same normalization helper used elsewhere
        this.lambda0 = wrapLongitude(centralLongitude);
    }

    /**
     * Test to see if the line between two longitudes crosses the
     * seam (the line at the central longitude). This is a test
     * for the dreaded wrapping problem.
     * @param lon1 one longitude in radians
     * @param lon2 the other longitude in radians
     * @return {@code true} if the line between the two longitudes
     * 	   crosses the seam; {@code false} otherwise
     */
    @Override
    public boolean crossesSeam(double lon1, double lon2) {
		double d1 = lon1 - lambda0;
		double d2 = lon2 - lambda0;

		d1 = wrapLongitude(d1);
		d2 = wrapLongitude(d2);


//		return (d1 * d2 < 0) && (Math.abs(d1 - d2) > Math.PI);
		return Math.abs(d1 - d2) > Math.PI;

    }


    @Override
    public void latLonToXY(Point2D.Double latLon, Point2D.Double xy) {
        double lon = latLon.x;
        double lat = latLon.y;

        lat = Math.max(MIN_LAT, Math.min(MAX_LAT, lat));

        double theta = solveTheta(lat);
        double dLambda = wrapLongitude(lon - lambda0); // <-- critical

        xy.x = 2.0 * SQRT2 * R * dLambda * Math.cos(theta) / Math.PI;
        xy.y = SQRT2 * R * Math.sin(theta);
    }

    @Override
    public void latLonFromXY(Point2D.Double latLon, Point2D.Double xy) {
        double x = xy.x / (R);
        double y = xy.y / (R);

        double theta = Math.asin(y / SQRT2);

        // φ from θ
        double argument = (2.0 * theta + Math.sin(2.0 * theta)) / Math.PI;
        argument = Math.max(-1.0, Math.min(1.0, argument));
        double phi = Math.asin(argument);

        // λ from x and θ
        double cosTheta = Math.cos(theta);
        double lambda = lambda0;
        if (Math.abs(cosTheta) > 1e-12) {
            lambda += Math.PI * x / (2.0 * SQRT2 * cosTheta);
        }

        latLon.x = wrapLongitude(lambda);
        latLon.y = phi;
    }

    @Override
    public void drawMapOutline(Graphics2D g2, IContainer container) {
        // Elliptical boundary: x^2 / A^2 + y^2 / B^2 = 1
        Path2D path = new Path2D.Double();
        Point2D.Double world = new Point2D.Double();
        Point screen = new Point();

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double t = 2.0 * Math.PI * i / NUM_SEGMENTS;
            double x = A * Math.cos(t);
            double y = B * Math.sin(t);

            world.setLocation(x, y);
            container.worldToLocal(screen, world);

            if (i == 0) {
                path.moveTo(screen.x, screen.y);
            } else {
                path.lineTo(screen.x, screen.y);
            }
        }
        path.closePath();

        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();

        g2.setColor(theme.getOutlineColor());
        g2.setStroke(new BasicStroke(theme.getOutlineStrokeWidth()));
        g2.draw(path);

        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    @Override
    public boolean isPointOnMap(Point2D.Double xy) {
        double x = xy.x;
        double y = xy.y;
        double value = (x * x) / (A * A) + (y * y) / (B * B);
        return value <= 1.0 + 1e-9;
    }

    @Override
    public void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude) {
        double lat = Math.max(MIN_LAT, Math.min(MAX_LAT, latitude));

        Path2D path = new Path2D.Double();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy = new Point2D.Double();
        Point screen = new Point();

        latLon.y = lat;

        int n = NUM_SEGMENTS;
        for (int i = 0; i <= n; i++) {
            double lon = -Math.PI + 2.0 * Math.PI * i / n;
            latLon.x = lon;

            latLonToXY(latLon, xy);
            container.worldToLocal(screen, xy);

            if (i == 0) {
                path.moveTo(screen.x, screen.y);
            } else {
                path.lineTo(screen.x, screen.y);
            }
        }

        Color oldColor = g2.getColor();
        g2.setColor(Color.LIGHT_GRAY);
        g2.draw(path);
        g2.setColor(oldColor);
    }

    @Override
    public void drawLongitudeLine(Graphics2D g2, IContainer container, double longitude) {
        double lon = wrapLongitude(longitude);

        Path2D path = new Path2D.Double();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy = new Point2D.Double();
        Point screen = new Point();

        latLon.x = lon;

        int n = NUM_SEGMENTS;
        for (int i = 0; i <= n; i++) {
            double lat = MIN_LAT + (MAX_LAT - MIN_LAT) * i / n;
            latLon.y = lat;

            latLonToXY(latLon, xy);
            container.worldToLocal(screen, xy);

            if (i == 0) {
                path.moveTo(screen.x, screen.y);
            } else {
                path.lineTo(screen.x, screen.y);
            }
        }

        Color oldColor = g2.getColor();
        g2.setColor(Color.LIGHT_GRAY);
        g2.draw(path);
        g2.setColor(oldColor);
    }

    @Override
    public boolean isPointVisible(Point2D.Double latLon) {
        // Whole globe is visible; just enforce numerical latitude range.
        double lat = latLon.y;
        return lat >= MIN_LAT && lat <= MAX_LAT;
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

    @Override
    public Shape createClipShape(IContainer container) {
        // Same ellipse as the outline, but in device coordinates
        Path2D path = new Path2D.Double();
        Point2D.Double world = new Point2D.Double();
        Point screen = new Point();

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double t = 2.0 * Math.PI * i / NUM_SEGMENTS;
            double x = A * Math.cos(t);
            double y = B * Math.sin(t);

            world.setLocation(x, y);
            container.worldToLocal(screen, world);

            if (i == 0) {
                path.moveTo(screen.x, screen.y);
            } else {
                path.lineTo(screen.x, screen.y);
            }
        }
        path.closePath();
        return path;
    }
}
