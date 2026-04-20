package edu.cnu.mdi.mapping.projection;

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
import edu.cnu.mdi.mapping.theme.MapTheme;

/**
 * Mollweide equal-area pseudocylindrical projection (spherical Earth, R = 1).
 *
 * <h2>Forward equations (Snyder)</h2>
 * <p>Solve for the auxiliary angle θ using Newton-Raphson iteration:</p>
 * <pre>
 *   2θ + sin(2θ) = π sin φ
 *
 *   x = (2√2 / π) · (λ - λ₀) · cos θ
 *   y = √2 · sin θ
 * </pre>
 *
 * <h2>Inverse equations</h2>
 * <pre>
 *   θ = arcsin(y / √2)
 *   φ = arcsin((2θ + sin 2θ) / π)
 *   λ = λ₀ + π · x / (2√2 · cos θ)
 * </pre>
 *
 * <h2>Domain</h2>
 * <p>The entire globe maps to an ellipse with semi-axes
 * A = 2√2 (x) and B = √2 (y). Latitude is clamped to ±89.999° for
 * numerical safety.</p>
 *
 * <h2>Seam handling</h2>
 * <p>Like Mercator, the Mollweide projection has a seam at λ₀ ± π.
 * {@link #crossesSeam} is overridden to detect polygon edges that straddle
 * the seam so they can be split before rendering.</p>
 *
 * <h2>Default central meridian</h2>
 * <p>The central longitude λ₀ defaults to -70° (western Atlantic). This
 * choice is intentional: it positions the Americas near the centre for an
 * aesthetically balanced view when the control panel occupies the right side
 * of the window. It can be changed via {@link #setCentralLongitude(double)}.</p>
 */
public class MollweideProjection implements IMapProjection {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final double R     = 1.0;
    private static final double SQRT2 = Math.sqrt(2.0);

    /** Semi-major (x) axis of the Mollweide ellipse. */
    private static final double A = 2.0 * SQRT2;

    /** Semi-minor (y) axis of the Mollweide ellipse. */
    private static final double B = SQRT2;

    private static final int    MAX_NEWTON_ITERS = 20;
    private static final double NEWTON_TOL       = 1e-12;

    private static final double MAX_LAT = Math.toRadians(89.999);
    private static final double MIN_LAT = -MAX_LAT;

    private static final int NUM_SEGMENTS = 360;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /**
     * Central meridian λ₀ in radians.
     *
     * <p>Defaults to -70° (western Atlantic) — see the class-level note on
     * the default central meridian. Change via
     * {@link #setCentralLongitude(double)}.</p>
     */
    private double lambda0 = Math.toRadians(-70.0);

    /** Active rendering theme. */
    private MapTheme theme;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a Mollweide projection with the supplied theme and the default
     * central meridian of -70°.
     *
     * @param theme the map theme; must not be {@code null}
     */
    public MollweideProjection(MapTheme theme) {
        setTheme(theme);
    }

    // -------------------------------------------------------------------------
    // Private math helpers
    // -------------------------------------------------------------------------

    /**
     * Solves {@code 2θ + sin(2θ) = π sin φ} for θ using Newton-Raphson.
     *
     * <p>The iteration converges quickly (typically 3-5 steps) for all
     * latitudes except the poles, where the formula degenerates and the
     * clamping above prevents pathological inputs.</p>
     *
     * @param phi latitude φ in radians; should already be clamped to
     *            [{@code MIN_LAT}, {@code MAX_LAT}]
     * @return the auxiliary angle θ in radians
     */
    private static double solveTheta(double phi) {
        phi = Math.max(MIN_LAT, Math.min(MAX_LAT, phi));
        if (Math.abs(phi) < 1e-12) return 0.0;

        double target = Math.PI * Math.sin(phi);
        double theta  = phi; // good initial guess

        for (int i = 0; i < MAX_NEWTON_ITERS; i++) {
            double twoTheta = 2.0 * theta;
            double delta    = (twoTheta + Math.sin(twoTheta) - target) / (2.0 + 2.0 * Math.cos(twoTheta));
            theta -= delta;
            if (Math.abs(delta) < NEWTON_TOL) break;
        }
        return theta;
    }

    // -------------------------------------------------------------------------
    // Central longitude accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the central meridian λ₀ in radians.
     *
     * @return central longitude in radians
     */
    public double getCentralLongitude() { return lambda0; }

    /**
     * Sets the central meridian λ₀.
     *
     * <p>The value is normalized to (-π, π] before storage.</p>
     *
     * @param centralLongitude desired central longitude in radians
     */
    public void setCentralLongitude(double centralLongitude) {
        this.lambda0 = wrapLongitude(centralLongitude);
    }

    // -------------------------------------------------------------------------
    // IMapProjection — seam
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code true} when the two longitudes — each wrapped relative
     * to λ₀ — differ by more than π radians, indicating an antimeridian
     * crossing.</p>
     */
    @Override
    public boolean crossesSeam(double lon1, double lon2) {
        double d1 = wrapLongitude(lon1 - lambda0);
        double d2 = wrapLongitude(lon2 - lambda0);
        return Math.abs(d1 - d2) > Math.PI;
    }

    // -------------------------------------------------------------------------
    // IMapProjection — transforms
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Latitude is clamped to [{@code MIN_LAT}, {@code MAX_LAT}] before
     * solving for θ. The relative longitude (λ - λ₀) is wrapped to [-π, π)
     * to handle inputs that span the seam.</p>
     */
    @Override
    public void latLonToXY(Point2D.Double latLon, Point2D.Double xy) {
        double lat    = Math.max(MIN_LAT, Math.min(MAX_LAT, latLon.y));
        double theta  = solveTheta(lat);
        double dLon   = wrapLongitude(latLon.x - lambda0);

        xy.x = 2.0 * SQRT2 * R * dLon * Math.cos(theta) / Math.PI;
        xy.y = SQRT2 * R * Math.sin(theta);
    }

    /** {@inheritDoc} */
    @Override
    public void latLonFromXY(Point2D.Double latLon, Point2D.Double xy) {
        double x     = xy.x / R;
        double y     = xy.y / R;
        double theta = Math.asin(y / SQRT2);

        double argument = Math.max(-1.0, Math.min(1.0, (2.0 * theta + Math.sin(2.0 * theta)) / Math.PI));
        double phi      = Math.asin(argument);

        double cosTheta = Math.cos(theta);
        double lambda   = lambda0;
        if (Math.abs(cosTheta) > 1e-12) {
            lambda += Math.PI * x / (2.0 * SQRT2 * cosTheta);
        }

        latLon.x = wrapLongitude(lambda);
        latLon.y = phi;
    }

    // -------------------------------------------------------------------------
    // IMapProjection — visibility
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>The entire globe maps to the Mollweide ellipse, so every point within
     * the latitude range is visible.</p>
     */
    @Override
    public boolean isPointVisible(Point2D.Double latLon) {
        return latLon.y >= MIN_LAT && latLon.y <= MAX_LAT;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A point is on the map when it lies within or on the boundary of the
     * Mollweide ellipse: x²/A² + y²/B² ≤ 1.</p>
     */
    @Override
    public boolean isPointOnMap(Point2D.Double xy) {
        return (xy.x * xy.x) / (A * A) + (xy.y * xy.y) / (B * B) <= 1.0 + 1e-9;
    }

    // -------------------------------------------------------------------------
    // IMapProjection — drawing
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Draws the Mollweide ellipse boundary (x²/A² + y²/B² = 1) using the
     * theme's outline color and stroke width.</p>
     */
    @Override
    public void drawMapOutline(Graphics2D g2, IContainer container) {
        Path2D path = new Path2D.Double();
        Point2D.Double world = new Point2D.Double();
        Point screen = new Point();

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double t = 2.0 * Math.PI * i / NUM_SEGMENTS;
            world.setLocation(A * Math.cos(t), B * Math.sin(t));
            container.worldToLocal(screen, world);
            if (i == 0) path.moveTo(screen.x, screen.y);
            else        path.lineTo(screen.x, screen.y);
        }
        path.closePath();

        Color  oldColor  = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(theme.getOutlineColor());
        g2.setStroke(new BasicStroke(theme.getOutlineStrokeWidth()));
        g2.draw(path);
        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Samples 360 evenly spaced longitudes at the given latitude and
     * connects the projected screen points. Uses
     * {@link MapTheme#getGraticuleColor()} — <em>not</em> a hardcoded color —
     * so the line color correctly reflects the active theme.</p>
     */
    @Override
    public void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude) {
        double lat = Math.max(MIN_LAT, Math.min(MAX_LAT, latitude));

        Path2D path = new Path2D.Double();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy     = new Point2D.Double();
        Point screen          = new Point();

        latLon.y = lat;
        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            latLon.x = -Math.PI + 2.0 * Math.PI * i / NUM_SEGMENTS;
            latLonToXY(latLon, xy);
            container.worldToLocal(screen, xy);
            if (i == 0) path.moveTo(screen.x, screen.y);
            else        path.lineTo(screen.x, screen.y);
        }

        Color oldColor = g2.getColor();
        g2.setColor(theme.getGraticuleColor());   // fixed: was Color.LIGHT_GRAY
        g2.draw(path);
        g2.setColor(oldColor);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Samples 360 evenly spaced latitudes at the given longitude and
     * connects the projected screen points. Uses
     * {@link MapTheme#getGraticuleColor()} so the color respects theme
     * switching.</p>
     */
    @Override
    public void drawLongitudeLine(Graphics2D g2, IContainer container, double longitude) {
        double lon = wrapLongitude(longitude);

        Path2D path = new Path2D.Double();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy     = new Point2D.Double();
        Point screen          = new Point();

        latLon.x = lon;
        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            latLon.y = MIN_LAT + (MAX_LAT - MIN_LAT) * i / NUM_SEGMENTS;
            latLonToXY(latLon, xy);
            container.worldToLocal(screen, xy);
            if (i == 0) path.moveTo(screen.x, screen.y);
            else        path.lineTo(screen.x, screen.y);
        }

        Color oldColor = g2.getColor();
        g2.setColor(theme.getGraticuleColor());   // fixed: was Color.LIGHT_GRAY
        g2.draw(path);
        g2.setColor(oldColor);
    }

    // -------------------------------------------------------------------------
    // IMapProjection — metadata
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public EProjection getProjection() { return EProjection.MOLLWEIDE; }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the bounding rectangle of the Mollweide ellipse:
     * [-A, -B] to [A, B], i.e.
     * {@code [-2√2, -√2] to [2√2, √2]}.</p>
     */
    @Override
    public Rectangle2D.Double getXYBounds() {
        return new Rectangle2D.Double(-A, -B, 2.0 * A, 2.0 * B);
    }

    /** {@inheritDoc} */
    @Override
    public MapTheme getTheme() { return theme; }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code theme} is {@code null}
     */
    @Override
    public void setTheme(MapTheme theme) {
        if (theme == null) throw new IllegalArgumentException("MapTheme must not be null");
        this.theme = theme;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns an elliptical path in device coordinates that matches the
     * Mollweide boundary, used as a clip region for ocean fill and land
     * rendering.</p>
     */
    @Override
    public Shape createClipShape(IContainer container) {
        Path2D path = new Path2D.Double();
        Point2D.Double world = new Point2D.Double();
        Point screen = new Point();

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double t = 2.0 * Math.PI * i / NUM_SEGMENTS;
            world.setLocation(A * Math.cos(t), B * Math.sin(t));
            container.worldToLocal(screen, world);
            if (i == 0) path.moveTo(screen.x, screen.y);
            else        path.lineTo(screen.x, screen.y);
        }
        path.closePath();
        return path;
    }
}
