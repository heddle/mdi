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
 * Spherical Mercator projection.
 * <p>
 * Uses a practical latitude cutoff (±85°) to avoid infinite Y at the poles.
 * Projection-space coordinates:
 * <pre>
 *     x = λ
 *     y = ln(tan(π/4 + φ/2))
 * </pre>
 * where λ is longitude and φ is latitude in radians.
 */
public class MercatorProjection implements IMapProjection {

    /** Maximum latitude (in radians) to avoid infinite Y at the poles. */
    private static final double MAX_LAT_DEG = 85.0;
    private static final double MAX_LAT = Math.toRadians(MAX_LAT_DEG);

    /** Longitudinal extent (whole world). */
    private static final double MIN_LON = -Math.PI;
    private static final double MAX_LON = Math.PI;

    /** Y extents corresponding to ±MAX_LAT. */
    private static final double MIN_Y = mercatorY(-MAX_LAT);
    private static final double MAX_Y = mercatorY(MAX_LAT);

    /** Active theme used for rendering. */
    private MapTheme theme;
    
    /**
     * Central longitude (λ₀) in radians. Longitude values are mapped to
     * projection X as {@code x = wrap(λ - λ₀)}, so λ₀ is the longitude that
     * appears at x = 0 in projection space.
     */
    private double centralLongitude = 0;

    /**
     * Create a Mercator projection with the given theme.
     *
     * @param theme the map theme; must not be {@code null}
     */
    public MercatorProjection(MapTheme theme) {
        setTheme(theme);
    }

    /**
     * Forward Mercator Y formula: {@code y = ln(tan(π/4 + φ/2))}.
     */
    private static double mercatorY(double latitude) {
        return Math.log(Math.tan((Math.PI / 4.0) + (latitude / 2.0)));
    }
    
    /**
     * Get the central longitude λ₀ (in radians).
     *
     * @return the central longitude in radians
     */
    public double getCentralLongitude() {
        return centralLongitude;
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
        this.centralLongitude = wrapLongitude(centralLongitude);
    }

    @Override
    public void latLonToXY(Point2D.Double latLon, Point2D.Double xy) {
        double lon = latLon.x;
        double lat = latLon.y;

        // Clamp latitude to avoid undefined values at poles
        lat = Math.max(-MAX_LAT, Math.min(MAX_LAT, lat));

        xy.x = wrapLongitude(lon - centralLongitude); // λ
        xy.y = mercatorY(lat);
    }

    @Override
    public void latLonFromXY(Point2D.Double latLon, Point2D.Double xy) {
        double x = wrapLongitude(xy.x + centralLongitude);
        double y = xy.y;

        latLon.x = x; // λ
        latLon.y = 2.0 * Math.atan(Math.exp(y)) - Math.PI / 2.0; // φ
    }

    @Override
    public void drawMapOutline(Graphics2D g2, IContainer container) {
        Rectangle2D.Double r = getXYBounds();

        Point2D.Double world = new Point2D.Double();
        Point pLL = new Point();
        Point pLR = new Point();
        Point pUR = new Point();
        Point pUL = new Point();

        // lower-left
        world.setLocation(r.x, r.y);
        container.worldToLocal(pLL, world);

        // lower-right
        world.setLocation(r.x + r.width, r.y);
        container.worldToLocal(pLR, world);

        // upper-right
        world.setLocation(r.x + r.width, r.y + r.height);
        container.worldToLocal(pUR, world);

        // upper-left
        world.setLocation(r.x, r.y + r.height);
        container.worldToLocal(pUL, world);

        Color oldColor = g2.getColor();
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

    @Override
    public boolean isPointOnMap(Point2D.Double xy) {
        double x = xy.x;
        double y = xy.y;
        return (x >= MIN_LON && x <= MAX_LON && y >= MIN_Y && y <= MAX_Y);
    }

    @Override
    public void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude) {
        // Clamp to allowed range
        double lat = Math.max(-MAX_LAT, Math.min(MAX_LAT, latitude));

        int numSegments = 360;
        double dLon = (MAX_LON - MIN_LON) / numSegments;

        Path2D path = new Path2D.Double();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy = new Point2D.Double();
        Point screen = new Point();

        latLon.y = lat;

        for (int i = 0; i <= numSegments; i++) {
            double lon = MIN_LON + i * dLon;
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

        int numSegments = 360;
        double dLat = (2.0 * MAX_LAT) / numSegments;

        Path2D path = new Path2D.Double();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy = new Point2D.Double();
        Point screen = new Point();

        latLon.x = lon;

        for (int i = 0; i <= numSegments; i++) {
            double lat = -MAX_LAT + i * dLat;
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
        double lat = latLon.y;
        return lat >= -MAX_LAT && lat <= MAX_LAT;
    }

    @Override
    public EProjection getProjection() {
        return EProjection.MERCATOR;
    }

    @Override
    public Rectangle2D.Double getXYBounds() {
        return new Rectangle2D.Double(MIN_LON, MIN_Y,
                                      MAX_LON - MIN_LON,
                                      MAX_Y - MIN_Y);
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
        Rectangle2D.Double r = getXYBounds();

        Point2D.Double world = new Point2D.Double();
        Point p = new Point();
        Path2D path = new Path2D.Double();

        // lower-left
        world.setLocation(r.x, r.y);
        container.worldToLocal(p, world);
        path.moveTo(p.x, p.y);

        // lower-right
        world.setLocation(r.x + r.width, r.y);
        container.worldToLocal(p, world);
        path.lineTo(p.x, p.y);

        // upper-right
        world.setLocation(r.x + r.width, r.y + r.height);
        container.worldToLocal(p, world);
        path.lineTo(p.x, p.y);

        // upper-left
        world.setLocation(r.x, r.y + r.height);
        container.worldToLocal(p, world);
        path.lineTo(p.x, p.y);

        path.closePath();
        return path;
    }
}
