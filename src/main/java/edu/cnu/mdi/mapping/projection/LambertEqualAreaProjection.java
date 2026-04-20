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
 * Lambert azimuthal equal-area projection (spherical Earth, R = 1).
 *
 * <h2>Forward equations (Snyder, spherical form)</h2>
 * <pre>
 *   k = √(2 / (1 + sin φ₀ sin φ + cos φ₀ cos φ cos(λ - λ₀)))
 *
 *   x = k · cos φ · sin(λ - λ₀)
 *   y = k · (cos φ₀ · sin φ − sin φ₀ · cos φ · cos(λ - λ₀))
 * </pre>
 *
 * <h2>Inverse equations</h2>
 * <pre>
 *   ρ = √(x² + y²),   c = 2 arcsin(ρ / 2)
 *
 *   φ = arcsin(cos c · sin φ₀ + (y · sin c · cos φ₀) / ρ)
 *   λ = λ₀ + atan2(x · sin c, ρ · cos φ₀ · cos c − y · sin φ₀ · sin c)
 * </pre>
 *
 * <h2>Domain</h2>
 * <p>The entire sphere maps to a disk of radius 2 (for R = 1). The antipode
 * of the center point maps to the outer boundary circle. The bounding box is
 * [-2, -2] to [2, 2].</p>
 *
 * <h2>Antipode singularity</h2>
 * <p>When the denominator {@code 1 + cos γ} approaches zero (point is the
 * antipode of the center), the formula is regularized by clamping to a point
 * on the outer boundary of the disk at angle 0. This is geometrically correct
 * in the limit and prevents division by zero or NaN propagation.</p>
 *
 * <h2>No seam</h2>
 * <p>The Lambert projection has no antimeridian seam because the mapping from
 * globe to disk is continuous. The default {@link IMapProjection#crossesSeam}
 * returning {@code false} is correct for this class.</p>
 */
public class LambertEqualAreaProjection implements IMapProjection {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final double R           = 1.0;
    private static final double MAX_LAT     = Math.toRadians(89.999);
    private static final double MIN_LAT     = -MAX_LAT;

    /**
     * Maximum radial distance in projection space (antipode of center
     * maps to the circle boundary at ρ = 2R).
     */
    private static final double RHO_MAX     = 2.0 * R;

    /** Number of segments for the circular outline and clip path. */
    private static final int    NUM_SEGMENTS = 360;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Central longitude λ₀ in radians. */
    private double centerLon;

    /** Central latitude φ₀ in radians. */
    private double centerLat;

    /** Active rendering theme. */
    private MapTheme theme;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a Lambert equal-area projection centered at the given geographic
     * point. The {@link MapTheme} must be set via {@link #setTheme(MapTheme)}
     * before any drawing methods are called, or use the three-argument
     * constructor to set it atomically.
     *
     * @param centerLon central longitude λ₀ in radians
     * @param centerLat central latitude φ₀ in radians
     */
    public LambertEqualAreaProjection(double centerLon, double centerLat) {
        this.centerLon = centerLon;
        this.centerLat = centerLat;
    }

    /**
     * Convenience constructor centered at (0°, 0°). The theme must still be
     * set before drawing.
     */
    public LambertEqualAreaProjection() {
        this(0.0, 0.0);
    }

    /**
     * Convenience constructor that sets both the center and the theme
     * atomically, preventing the NullPointerException that arises when drawing
     * methods are called before the theme is set.
     *
     * @param centerLon central longitude λ₀ in radians
     * @param centerLat central latitude φ₀ in radians
     * @param theme     map theme; must not be {@code null}
     */
    public LambertEqualAreaProjection(double centerLon, double centerLat, MapTheme theme) {
        this(centerLon, centerLat);
        setTheme(theme);
    }

    /**
     * Convenience constructor centered at (0°, 0°) with the given theme.
     *
     * @param theme map theme; must not be {@code null}
     */
    public LambertEqualAreaProjection(MapTheme theme) {
        this(0.0, 0.0, theme);
    }

    // -------------------------------------------------------------------------
    // Center accessor
    // -------------------------------------------------------------------------

    /**
     * Changes the center of the projection. The next repaint will show the
     * globe centred on the new point.
     *
     * @param centerLon central longitude λ₀ in radians
     * @param centerLat central latitude φ₀ in radians
     */
    public void setCenter(double centerLon, double centerLat) {
        this.centerLon = centerLon;
        this.centerLat = centerLat;
    }

    // -------------------------------------------------------------------------
    // IMapProjection — transforms
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Latitude is clamped to [{@code MIN_LAT}, {@code MAX_LAT}] before
     * projection. When the input point is the antipode of the center (the
     * denominator {@code 1 + cos γ ≈ 0}), the output is set to the boundary
     * point {@code (RHO_MAX, 0)} rather than NaN.</p>
     */
    @Override
    public void latLonToXY(Point2D.Double latLon, Point2D.Double xy) {
        double lon  = latLon.x;
        double lat  = Math.max(MIN_LAT, Math.min(MAX_LAT, latLon.y));
        double dLon = lon - centerLon;

        double sinLat  = Math.sin(lat);
        double cosLat  = Math.cos(lat);
        double sinLat0 = Math.sin(centerLat);
        double cosLat0 = Math.cos(centerLat);
        double cosDLon = Math.cos(dLon);

        double denom = 1.0 + sinLat0 * sinLat + cosLat0 * cosLat * cosDLon;

        if (denom <= 1e-15) {
            // Antipode: map to a canonical boundary point.
            xy.x = RHO_MAX;
            xy.y = 0.0;
            return;
        }

        double k = Math.sqrt(2.0 / denom);
        xy.setLocation(
            k * cosLat  * Math.sin(dLon),
            k * (cosLat0 * sinLat - sinLat0 * cosLat * cosDLon)
        );
    }

    /** {@inheritDoc} */
    @Override
    public void latLonFromXY(Point2D.Double latLon, Point2D.Double xy) {
        double x    = xy.x;
        double y    = xy.y;
        double rho  = Math.sqrt(x * x + y * y);

        if (rho > RHO_MAX + 1e-9) {
            latLon.setLocation(Double.NaN, Double.NaN);
            return;
        }
        if (rho < 1e-15) {
            latLon.x = centerLon;
            latLon.y = centerLat;
            return;
        }

        double sinLat0 = Math.sin(centerLat);
        double cosLat0 = Math.cos(centerLat);
        double c       = 2.0 * Math.asin(rho / (2.0 * R));
        double sinC    = Math.sin(c);
        double cosC    = Math.cos(c);

        latLon.y = Math.asin(cosC * sinLat0 + (y * sinC * cosLat0) / rho);
        latLon.x = wrapLongitude(
            centerLon + Math.atan2(x * sinC, rho * cosLat0 * cosC - y * sinLat0 * sinC));
    }

    // -------------------------------------------------------------------------
    // IMapProjection — visibility
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>The Lambert projection maps the entire globe, so every point in the
     * valid latitude range is visible.</p>
     */
    @Override
    public boolean isPointVisible(Point2D.Double latLon) {
        return latLon.y >= MIN_LAT && latLon.y <= MAX_LAT;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A point is on the map when it lies within or on the boundary of the
     * disk: x² + y² ≤ RHO_MAX².</p>
     */
    @Override
    public boolean isPointOnMap(Point2D.Double xy) {
        return xy.x * xy.x + xy.y * xy.y <= RHO_MAX * RHO_MAX + 1e-9;
    }

    // -------------------------------------------------------------------------
    // IMapProjection — drawing
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Draws the circular boundary of the projection disk (radius
     * {@code RHO_MAX = 2}) using the theme's outline color and stroke
     * width.</p>
     */
    @Override
    public void drawMapOutline(Graphics2D g2, IContainer container) {
        Path2D path = new Path2D.Double();
        Point2D.Double world = new Point2D.Double();
        Point screen = new Point();

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double theta = 2.0 * Math.PI * i / NUM_SEGMENTS;
            world.setLocation(RHO_MAX * Math.cos(theta), RHO_MAX * Math.sin(theta));
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
     * <p>Samples 360 evenly-spaced longitudes at the given latitude and
     * connects the projected screen points. NaN results (e.g. near the
     * antipode boundary) are skipped. Uses
     * {@link MapTheme#getGraticuleColor()} so the line color reflects the
     * active theme.</p>
     */
    @Override
    public void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude) {
        double lat = Math.max(MIN_LAT, Math.min(MAX_LAT, latitude));

        Path2D path = new Path2D.Double();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy     = new Point2D.Double();
        Point screen          = new Point();

        latLon.y = lat;
        for (int i = 0; i <= 360; i++) {
            latLon.x = -Math.PI + 2.0 * Math.PI * i / 360;
            latLonToXY(latLon, xy);
            if (Double.isNaN(xy.x) || Double.isNaN(xy.y) || !isPointOnMap(xy)) continue;
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
     * <p>Samples 180 evenly-spaced latitudes at the given longitude and
     * connects the projected screen points. Uses
     * {@link MapTheme#getGraticuleColor()} for the line color.</p>
     */
    @Override
    public void drawLongitudeLine(Graphics2D g2, IContainer container, double longitude) {
        double lon = wrapLongitude(longitude);

        Path2D path = new Path2D.Double();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy     = new Point2D.Double();
        Point screen          = new Point();

        latLon.x = lon;
        for (int i = 0; i <= 180; i++) {
            double t   = (double) i / 180;
            latLon.y   = MIN_LAT + (MAX_LAT - MIN_LAT) * t;
            latLonToXY(latLon, xy);
            if (Double.isNaN(xy.x) || Double.isNaN(xy.y) || !isPointOnMap(xy)) continue;
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
    public EProjection getProjection() { return EProjection.LAMBERT_EQUAL_AREA; }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the bounding square of the disk: [-RHO_MAX, -RHO_MAX] to
     * [RHO_MAX, RHO_MAX], i.e. [-2, -2] to [2, 2] for a unit sphere.</p>
     */
    @Override
    public Rectangle2D.Double getXYBounds() {
        return new Rectangle2D.Double(-RHO_MAX, -RHO_MAX, 2.0 * RHO_MAX, 2.0 * RHO_MAX);
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
     * <p>Returns a circular path in device coordinates matching the disk
     * boundary (radius {@code RHO_MAX}), used as a clip region for ocean fill
     * and land rendering.</p>
     */
    @Override
    public Shape createClipShape(IContainer container) {
        Path2D path = new Path2D.Double();
        Point2D.Double world = new Point2D.Double();
        Point screen = new Point();

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double theta = 2.0 * Math.PI * i / NUM_SEGMENTS;
            world.setLocation(RHO_MAX * Math.cos(theta), RHO_MAX * Math.sin(theta));
            container.worldToLocal(screen, world);
            if (i == 0) path.moveTo(screen.x, screen.y);
            else        path.lineTo(screen.x, screen.y);
        }
        path.closePath();
        return path;
    }
}
