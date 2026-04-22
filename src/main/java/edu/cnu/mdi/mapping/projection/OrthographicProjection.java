package edu.cnu.mdi.mapping.projection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.mapping.theme.MapTheme;

/**
 * Orthographic azimuthal projection (spherical Earth, R = 1).
 *
 * <h2>Forward equations</h2>
 * <pre>
 *   x = R · cos φ · sin(λ - λ₀)
 *   y = R · (cos φ₀ · sin φ − sin φ₀ · cos φ · cos(λ - λ₀))
 * </pre>
 *
 * <p>A point is only projected when the dot product of the surface normal with
 * the view direction is positive (z &gt; 0). Points on the far hemisphere
 * yield {@code (NaN, NaN)} from {@link #latLonToXY} and {@code false} from
 * {@link #isPointVisible}.</p>
 *
 * <h2>Inverse equations</h2>
 * <pre>
 *   ρ = √(x² + y²),   c = arcsin(ρ / R)
 *   φ = arcsin(cos c · sin φ₀ + (y · sin c · cos φ₀) / ρ)
 *   λ = λ₀ + atan2(x · sin c, ρ · cos φ₀ · cos c − y · sin φ₀ · sin c)
 * </pre>
 *
 * <h2>Domain</h2>
 * <p>The unit disk: x² + y² ≤ 1. Only the visible hemisphere is rendered.</p>
 *
 * <h2>No seam</h2>
 * <p>Unlike cylindrical projections the orthographic projection has no
 * antimeridian seam; the default {@link IMapProjection#crossesSeam} returning
 * {@code false} is correct for this class.</p>
 */
public class OrthographicProjection implements IMapProjection {

    private static final int    NUM_SEGMENTS = 180;
    private static final double R            = 1.0;
    private static final double MAX_LAT      = Math.toRadians(89.999);
    private static final double MIN_LAT      = -MAX_LAT;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Central longitude λ₀ in radians. */
    private double centerLon;

    /** Central latitude φ₀ in radians. */
    private double centerLat;

    /**
     * Active rendering theme.
     *
     * <p>Unlike some other projections, the two-argument constructor does
     * <em>not</em> accept a theme; callers must set it via
     * {@link #setTheme(MapTheme)} or use the three-argument convenience
     * constructor {@link #OrthographicProjection(double, double, MapTheme)}.
     * Forgetting to set the theme before calling {@link #drawMapOutline} will
     * cause a {@link NullPointerException}; callers should always go through
     * {@link ProjectionFactory} to avoid this.</p>
     */
    private MapTheme theme;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates an orthographic projection centered at the given geographic
     * point on a unit sphere. The {@link MapTheme} must be supplied separately
     * via {@link #setTheme(MapTheme)} before any drawing methods are called.
     *
     * <p>Prefer the three-argument constructor or {@link ProjectionFactory} to
     * ensure the theme is set atomically with the center.</p>
     *
     * @param centerLon central longitude λ₀ in radians
     * @param centerLat central latitude φ₀ in radians
     */
    public OrthographicProjection(double centerLon, double centerLat) {
        this.centerLon = centerLon;
        this.centerLat = centerLat;
    }

    /**
     * Convenience constructor that sets both the center and the theme in a
     * single step, eliminating the NullPointerException risk of calling
     * {@link #drawMapOutline} before the theme is set.
     *
     * @param centerLon central longitude λ₀ in radians
     * @param centerLat central latitude φ₀ in radians
     * @param theme     map theme; must not be {@code null}
     */
    public OrthographicProjection(double centerLon, double centerLat, MapTheme theme) {
        this(centerLon, centerLat);
        setTheme(theme);
    }

    // -------------------------------------------------------------------------
    // Center accessor
    // -------------------------------------------------------------------------

    /**
     * Changes the center of the projection. The next repaint will show the
     * globe as seen from above the new center point.
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
     * <p>If the point is on the far hemisphere (dot product z ≤ 0) the output
     * is set to {@code (NaN, NaN)} and the caller should treat it as not
     * visible. Latitude is clamped to [{@code MIN_LAT}, {@code MAX_LAT}] for
     * numerical stability near the poles.</p>
     */
    @Override
    public void latLonToXY(Point2D.Double latLon, Point2D.Double xy) {
        double lat      = Math.max(MIN_LAT, Math.min(MAX_LAT, latLon.y));
        double deltaLon = latLon.x - centerLon;

        double cosLat     = Math.cos(lat);
        double sinLat     = Math.sin(lat);
        double cosDeltaLon = Math.cos(deltaLon);
        double sinDeltaLon = Math.sin(deltaLon);
        double cosLat0    = Math.cos(centerLat);
        double sinLat0    = Math.sin(centerLat);

        double z = sinLat0 * sinLat + cosLat0 * cosLat * cosDeltaLon;
        if (z <= 0.0) {
            xy.setLocation(Double.NaN, Double.NaN);
        } else {
            xy.setLocation(
                R * cosLat * sinDeltaLon,
                R * (cosLat0 * sinLat - sinLat0 * cosLat * cosDeltaLon)
            );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void latLonFromXY(Point2D.Double latLon, Point2D.Double xy) {
        double x    = xy.x;
        double y    = xy.y;
        double rho2 = x * x + y * y;

        if (rho2 > R * R) {
            latLon.setLocation(Double.NaN, Double.NaN);
            return;
        }

        double rho     = Math.sqrt(rho2);
        if (rho == 0.0) {
            latLon.x = centerLon;
            latLon.y = centerLat;
            return;
        }

        double c       = Math.asin(rho / R);
        double sinC    = Math.sin(c);
        double cosC    = Math.cos(c);
        double cosLat0 = Math.cos(centerLat);
        double sinLat0 = Math.sin(centerLat);

        latLon.y = Math.asin(cosC * sinLat0 + (y * sinC * cosLat0) / rho);
        latLon.x = wrapLongitude(centerLon
                + Math.atan2(x * sinC, rho * cosLat0 * cosC - y * sinLat0 * sinC));
    }

    // -------------------------------------------------------------------------
    // IMapProjection — visibility
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>A point is visible when the dot product of the surface normal with
     * the view direction is positive (z &gt; 0), i.e. the point lies on the
     * near hemisphere.</p>
     */
    @Override
    public boolean isPointVisible(Point2D.Double latLon) {
        double deltaLon = latLon.x - centerLon;
        double z = Math.sin(centerLat) * Math.sin(latLon.y)
                 + Math.cos(centerLat) * Math.cos(latLon.y) * Math.cos(deltaLon);
        return z > 0.0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A point is on the map when it lies within or on the unit disk
     * (x² + y² ≤ R²).</p>
     */
    @Override
    public boolean isPointOnMap(Point2D.Double xy) {
        return xy.x * xy.x + xy.y * xy.y <= R * R + 1e-9;
    }

    // -------------------------------------------------------------------------
    // IMapProjection — drawing
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Draws the unit-circle boundary of the projection disk using the
     * theme's outline color and stroke width.</p>
     */
    @Override
    public void drawMapOutline(Graphics2D g2, IContainer container) {
        Path2D path = new Path2D.Double();
        Point2D.Double world = new Point2D.Double();
        Point screen = new Point();

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double theta = 2.0 * Math.PI * i / NUM_SEGMENTS;
            world.setLocation(R * Math.cos(theta), R * Math.sin(theta));
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
     * <p>Samples {@link #NUM_SEGMENTS} evenly-spaced longitudes at the given
     * latitude. Invisible points (on the far hemisphere) are skipped so the
     * path is not drawn across the globe's limb. Uses
     * {@link MapTheme#getGraticuleColor()} rather than a hardcoded color.</p>
     */
    @Override
    public void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude) {
        double lat = Math.max(MIN_LAT, Math.min(MAX_LAT, latitude));

        GeneralPath path = new GeneralPath();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy     = new Point2D.Double();
        Point screen          = new Point();

        latLon.y = lat;
        double step = 2.0 * Math.PI / NUM_SEGMENTS;

        boolean penDown = false;

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            latLon.x = -Math.PI + i * step;

            if (!isPointVisible(latLon)) {
                penDown = false;
                continue;
            }

            latLonToXY(latLon, xy);
            if (Double.isNaN(xy.x) || Double.isNaN(xy.y)) {
                penDown = false;
                continue;
            }

            container.worldToLocal(screen, xy);

            if (!penDown) {
                path.moveTo(screen.x, screen.y);
                penDown = true;
            } else {
                path.lineTo(screen.x, screen.y);
            }
        }

        Color oldColor = g2.getColor();
        g2.setColor(theme.getGraticuleColor());
        g2.draw(path);
        g2.setColor(oldColor);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Samples {@link #NUM_SEGMENTS} evenly-spaced latitudes across the
     * visible latitude band for the current center latitude. Invisible points
     * are skipped. Uses {@link MapTheme#getGraticuleColor()} for the line
     * color.</p>
     */
    @Override
    public void drawLongitudeLine(Graphics2D g2, IContainer container, double longitude) {
        double lon = wrapLongitude(longitude);

        GeneralPath path = new GeneralPath();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy     = new Point2D.Double();
        Point screen          = new Point();

        latLon.x = lon;
        List<double[]> ranges = getVisibleLatitudeRanges(centerLat);

        for (double[] range : ranges) {
            boolean penDown = false;

            for (int i = 0; i <= NUM_SEGMENTS; i++) {
                double t = (double) i / NUM_SEGMENTS;
                latLon.y = range[0] + t * (range[1] - range[0]);

                if (!isPointVisible(latLon)) {
                    penDown = false;
                    continue;
                }

                latLonToXY(latLon, xy);
                if (Double.isNaN(xy.x) || Double.isNaN(xy.y)) {
                    penDown = false;
                    continue;
                }

                container.worldToLocal(screen, xy);

                if (!penDown) {
                    path.moveTo(screen.x, screen.y);
                    penDown = true;
                } else {
                    path.lineTo(screen.x, screen.y);
                }
            }
        }

        Color oldColor = g2.getColor();
        g2.setColor(theme.getGraticuleColor());
        g2.draw(path);
        g2.setColor(oldColor);
    }
    // -------------------------------------------------------------------------
    // IMapProjection — metadata
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public EProjection getProjection() { return EProjection.ORTHOGRAPHIC; }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the bounding square of the unit disk: [-R, -R] to [R, R],
     * i.e. [-1, -1] to [1, 1].</p>
     */
    @Override
    public Rectangle2D.Double getXYBounds() {
        return new Rectangle2D.Double(-R, -R, 2.0 * R, 2.0 * R);
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
     * <p>Returns a circular path in device coordinates matching the unit-disk
     * boundary, used as a clip region for ocean fill and land rendering.</p>
     */
    @Override
    public Shape createClipShape(IContainer container) {
        Path2D path = new Path2D.Double();
        Point2D.Double world = new Point2D.Double();
        Point screen = new Point();

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double theta = 2.0 * Math.PI * i / NUM_SEGMENTS;
            world.setLocation(R * Math.cos(theta), R * Math.sin(theta));
            container.worldToLocal(screen, world);
            if (i == 0) path.moveTo(screen.x, screen.y);
            else        path.lineTo(screen.x, screen.y);
        }
        path.closePath();
        return path;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Computes the visible latitude range(s) for a meridian given the current
     * center latitude.
     *
     * <p>For the orthographic projection the visible latitude band is
     * {@code [φ₀ - π/2, φ₀ + π/2]} intersected with the valid range
     * {@code [-π/2, π/2]}. The result always contains exactly one segment.</p>
     *
     * @param cLat center latitude φ₀ in radians
     * @return a single-element list containing {@code [minLat, maxLat]}
     */
    private List<double[]> getVisibleLatitudeRanges(double cLat) {
        double minLat = Math.max(cLat - Math.PI / 2.0, -Math.PI / 2.0);
        double maxLat = Math.min(cLat + Math.PI / 2.0,  Math.PI / 2.0);
        List<double[]> ranges = new ArrayList<>();
        ranges.add(new double[]{ minLat, maxLat });
        return ranges;
    }
}
