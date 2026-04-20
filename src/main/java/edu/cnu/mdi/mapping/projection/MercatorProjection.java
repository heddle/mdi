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
import edu.cnu.mdi.mapping.render.CountryRenderer;
import edu.cnu.mdi.mapping.theme.MapTheme;

/**
 * Spherical Mercator projection (EPSG:3857 style, unit sphere).
 *
 * <h2>Forward equations</h2>
 * <pre>
 *   x = λ - λ₀          (relative longitude, wrapped to [-π, π))
 *   y = ln(tan(π/4 + φ/2))
 * </pre>
 *
 * <h2>Inverse equations</h2>
 * <pre>
 *   λ = wrap(x + λ₀)
 *   φ = 2 atan(exp(y)) - π/2
 * </pre>
 *
 * <h2>Domain</h2>
 * <p>Latitude is clamped to ±89° to avoid infinite Y values at the poles. The
 * projection space bounding box is therefore
 * {@code [-π, π] × [mercatorY(-89°), mercatorY(89°)]}.</p>
 *
 * <h2>Seam handling</h2>
 * <p>The Mercator projection has a seam at longitude {@code λ₀ ± π}. The
 * {@link #crossesSeam(double, double)} override detects when a polygon edge
 * spans more than π radians of relative longitude, which signals that the edge
 * wraps around the seam and must be split by {@link CountryRenderer}.</p>
 */
public class MercatorProjection implements IMapProjection {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /**
     * Maximum latitude (radians) accepted before Y becomes impractical.
     * Corresponds to approximately ±85.05° (Web Mercator standard cutoff
     * is ~85.05°; here we use a slightly wider ±89°).
     */
    private static final double MAX_LAT = Math.toRadians(89.0);
    private static final double MIN_LAT = -MAX_LAT;

    private static final double MIN_LON = -Math.PI;
    private static final double MAX_LON =  Math.PI;

    /** Projected Y at {@link #MIN_LAT}. */
    private static final double MIN_Y = mercatorY(MIN_LAT);

    /** Projected Y at {@link #MAX_LAT}. */
    private static final double MAX_Y = mercatorY(MAX_LAT);

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /**
     * Central longitude λ₀ in radians.
     *
     * <p>The projected X coordinate of a geographic point is
     * {@code wrap(λ - λ₀)}, so λ₀ is the longitude that appears at x = 0
     * (the centre of the view). A value of -70° (approximately the Americas)
     * gives a visually balanced initial view when the data panel is placed on
     * the right-hand side of the window; it can be changed at any time via
     * {@link #setCentralLongitude(double)}.</p>
     */
    private double lambda0 = Math.toRadians(-70.0);

    /** Active rendering theme; set via {@link #setTheme(MapTheme)}. */
    private MapTheme theme;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a Mercator projection with the supplied theme and the default
     * central longitude of -70° (centred on the western Atlantic).
     *
     * @param theme the map theme to use; must not be {@code null}
     */
    public MercatorProjection(MapTheme theme) {
        setTheme(theme);
    }

    // -------------------------------------------------------------------------
    // Private math helpers
    // -------------------------------------------------------------------------

    /**
     * Computes the Mercator Y value for the given latitude.
     *
     * @param latitude latitude φ in radians
     * @return {@code ln(tan(π/4 + φ/2))}
     */
    private static double mercatorY(double latitude) {
        return Math.log(Math.tan(Math.PI / 4.0 + latitude / 2.0));
    }

    // -------------------------------------------------------------------------
    // Central longitude accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the central longitude λ₀ in radians.
     *
     * @return current central longitude (radians)
     */
    public double getCentralLongitude() {
        return lambda0;
    }

    /**
     * Sets the central longitude λ₀ in radians.
     *
     * <p>The supplied value is normalized to (-π, π] via
     * {@link #wrapLongitude(double)} before storage, so callers may pass any
     * radian value without pre-normalizing.</p>
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
     * <p>Returns {@code true} when the absolute difference of the two
     * longitudes — each wrapped relative to {@code λ₀} — exceeds π radians,
     * indicating that the segment crosses the antimeridian seam at
     * {@code λ₀ ± π}.</p>
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
     * projection. Longitude is wrapped relative to the central longitude so
     * that the result always lies in [-π, π).</p>
     */
    @Override
    public void latLonToXY(Point2D.Double latLon, Point2D.Double xy) {
        double lat = Math.max(MIN_LAT, Math.min(MAX_LAT, latLon.y));
        xy.x = wrapLongitude(latLon.x - lambda0);
        xy.y = mercatorY(lat);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The inverse Mercator un-wraps X by adding λ₀, then uses the
     * standard Gudermannian inverse to recover latitude.</p>
     */
    @Override
    public void latLonFromXY(Point2D.Double latLon, Point2D.Double xy) {
        latLon.x = wrapLongitude(xy.x + lambda0);
        latLon.y = 2.0 * Math.atan(Math.exp(xy.y)) - Math.PI / 2.0;
    }

    // -------------------------------------------------------------------------
    // IMapProjection — visibility
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code true} for all latitudes in [{@code MIN_LAT},
     * {@code MAX_LAT}]; the poles are excluded because the Mercator Y
     * diverges there.</p>
     */
    @Override
    public boolean isPointVisible(Point2D.Double latLon) {
        return latLon.y >= MIN_LAT && latLon.y <= MAX_LAT;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The Mercator map domain is the rectangle
     * [{@code MIN_LON}, {@code MAX_LON}] × [{@code MIN_Y}, {@code MAX_Y}].</p>
     */
    @Override
    public boolean isPointOnMap(Point2D.Double xy) {
        return xy.x >= MIN_LON && xy.x <= MAX_LON
            && xy.y >= MIN_Y  && xy.y <= MAX_Y;
    }

    // -------------------------------------------------------------------------
    // IMapProjection — drawing
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Draws the rectangular bounding box of the projection domain using the
     * theme's outline color and stroke width.</p>
     */
    @Override
    public void drawMapOutline(Graphics2D g2, IContainer container) {
        Rectangle2D.Double r = getXYBounds();

        Point2D.Double world = new Point2D.Double();
        Point pLL = new Point(), pLR = new Point(), pUR = new Point(), pUL = new Point();

        world.setLocation(r.x,           r.y);           container.worldToLocal(pLL, world);
        world.setLocation(r.x + r.width, r.y);           container.worldToLocal(pLR, world);
        world.setLocation(r.x + r.width, r.y + r.height);container.worldToLocal(pUR, world);
        world.setLocation(r.x,           r.y + r.height);container.worldToLocal(pUL, world);

        Color  oldColor  = g2.getColor();
        Stroke oldStroke = g2.getStroke();

        g2.setColor(theme.getOutlineColor());
        g2.setStroke(new BasicStroke(theme.getOutlineStrokeWidth()));
        g2.drawLine(pLL.x, pLL.y, pLR.x, pLR.y);
        g2.drawLine(pLR.x, pLR.y, pUR.x, pUR.y);
        g2.drawLine(pUR.x, pUR.y, pUL.x, pUL.y);
        g2.drawLine(pUL.x, pUL.y, pLL.x, pLL.y);

        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Draws a horizontal line at the given latitude by sampling 360
     * evenly-spaced longitudes across the full longitude range and connecting
     * the projected screen points. Uses {@link MapTheme#getGraticuleColor()}
     * so the color respects theme switching.</p>
     */
    @Override
    public void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude) {
        double lat = Math.max(MIN_LAT, Math.min(MAX_LAT, latitude));

        Path2D path = new Path2D.Double();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy     = new Point2D.Double();
        Point screen          = new Point();

        latLon.y = lat;
        double dLon = (MAX_LON - MIN_LON) / 360;

        for (int i = 0; i <= 360; i++) {
            latLon.x = MIN_LON + i * dLon;
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
     * <p>Draws a vertical line at the given longitude by sampling 360
     * evenly-spaced latitudes and connecting the projected screen points.
     * Uses {@link MapTheme#getGraticuleColor()} so the color respects theme
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
        double dLat = (MAX_LAT - MIN_LAT) / 360;

        for (int i = 0; i <= 360; i++) {
            latLon.y = MIN_LAT + i * dLat;
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
    public EProjection getProjection() { return EProjection.MERCATOR; }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code [-π, π] × [MIN_Y, MAX_Y]} where the Y bounds
     * correspond to the Mercator projection of ±89°.</p>
     */
    @Override
    public Rectangle2D.Double getXYBounds() {
        return new Rectangle2D.Double(MIN_LON, MIN_Y, MAX_LON - MIN_LON, MAX_Y - MIN_Y);
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
     * <p>Returns a rectangular clip path matching {@link #getXYBounds()},
     * converted to device coordinates via the container transform. Used by
     * {@link #fillOcean} to restrict the ocean fill to the map rectangle.</p>
     */
    @Override
    public Shape createClipShape(IContainer container) {
        Rectangle2D.Double r = getXYBounds();
        Point2D.Double world = new Point2D.Double();
        Point p = new Point();
        Path2D path = new Path2D.Double();

        world.setLocation(r.x,           r.y);            container.worldToLocal(p, world); path.moveTo(p.x, p.y);
        world.setLocation(r.x + r.width, r.y);            container.worldToLocal(p, world); path.lineTo(p.x, p.y);
        world.setLocation(r.x + r.width, r.y + r.height); container.worldToLocal(p, world); path.lineTo(p.x, p.y);
        world.setLocation(r.x,           r.y + r.height); container.worldToLocal(p, world); path.lineTo(p.x, p.y);
        path.closePath();
        return path;
    }
}
