package edu.cnu.mdi.mapping;

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

/**
 * Orthographic azimuthal projection (spherical Earth).
 * <p>
 * Displays the hemisphere centered at (λ₀, φ₀) as a perspective projection
 * onto a plane. Earth is modeled as a unit sphere so the projected domain is
 * the unit disk:
 * <pre>
 *     x = R cos φ sin(λ - λ₀)
 *     y = R (cos φ₀ sin φ - sin φ₀ cos φ cos(λ - λ₀))
 * </pre>
 * with {@code x^2 + y^2 ≤ R^2}.
 */
public class OrthographicProjection implements IMapProjection {

    /** Number of segments used for boundary and graticule approximations. */
    private static final int NUM_SEGMENTS = 180;

    /** Central longitude and latitude (in radians). */
    private double centerLon;
    private double centerLat;

    /** Sphere radius in projection space. */
    private static final double R = 1.0;

    private static final double MAX_LAT = Math.toRadians(89.999);
    private static final double MIN_LAT = -MAX_LAT;

    /** Theme for outline drawing. */
    private MapTheme theme;

    /**
     * Create a new orthographic projection centered at the given geographic
     * location on a unit sphere.
     *
     * @param centerLon central longitude λ₀ in radians
     * @param centerLat central latitude  φ₀ in radians
     */
    public OrthographicProjection(double centerLon, double centerLat) {
        this.centerLon = centerLon;
        this.centerLat = centerLat;
    }

    /**
     * Set the center of the projection.
     *
     * @param centerLon central longitude λ₀ in radians
     * @param centerLat central latitude  φ₀ in radians
     */
    public void setCenter(double centerLon, double centerLat) {
        this.centerLat = centerLat;
        this.centerLon = centerLon;
    }

    @Override
    public void latLonToXY(Point2D.Double latLon, Point2D.Double xy) {
        double lon = latLon.x;
        double lat = latLon.y;

        // Clamp latitude slightly away from the poles for numerical stability
        lat = Math.max(MIN_LAT, Math.min(MAX_LAT, lat));

        double deltaLon = lon - centerLon;

        double cosLat = Math.cos(lat);
        double sinLat = Math.sin(lat);
        double cosDeltaLon = Math.cos(deltaLon);
        double sinDeltaLon = Math.sin(deltaLon);

        double cosLat0 = Math.cos(centerLat);
        double sinLat0 = Math.sin(centerLat);

        double x = R * cosLat * sinDeltaLon;
        double y = R * (cosLat0 * sinLat - sinLat0 * cosLat * cosDeltaLon);

        // z is used to test visibility: dot product with view direction
        double z = sinLat0 * sinLat + cosLat0 * cosLat * cosDeltaLon;

        if (z <= 0.0) {
            // Point is on the far hemisphere; mark as not visible
            xy.setLocation(Double.NaN, Double.NaN);
        } else {
            xy.setLocation(x, y);
        }
    }

    @Override
    public void latLonFromXY(Point2D.Double latLon, Point2D.Double xy) {
        double x = xy.x;
        double y = xy.y;

        double rho2 = x * x + y * y;
        if (rho2 > R * R) {
            // Outside unit disk: no valid inverse; mark as NaN
            latLon.setLocation(Double.NaN, Double.NaN);
            return;
        }

        double rho = Math.sqrt(rho2);
        if (rho == 0.0) {
            // Center of the projection
            latLon.x = centerLon;
            latLon.y = centerLat;
            return;
        }

        double c = Math.asin(rho / R);
        double sinC = Math.sin(c);
        double cosC = Math.cos(c);

        double cosLat0 = Math.cos(centerLat);
        double sinLat0 = Math.sin(centerLat);

        double phi = Math.asin(cosC * sinLat0 + (y * sinC * cosLat0) / rho);
        double lambda = centerLon + Math.atan2(x * sinC,
                                               rho * cosLat0 * cosC - y * sinLat0 * sinC);

        latLon.x = wrapLongitude(lambda);
        latLon.y = phi;
    }

    @Override
    public void drawMapOutline(Graphics2D g2, IContainer container) {
        // Boundary is the unit circle x^2 + y^2 = R^2
        Path2D path = new Path2D.Double();
        Point2D.Double world = new Point2D.Double();
        Point screen = new Point();

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double theta = 2.0 * Math.PI * i / NUM_SEGMENTS;
            double x = R * Math.cos(theta);
            double y = R * Math.sin(theta);

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
        return x * x + y * y <= R * R + 1e-9;
    }

    @Override
    public void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude) {
        double lat = Math.max(MIN_LAT, Math.min(MAX_LAT, latitude));

        GeneralPath path = new GeneralPath();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy = new Point2D.Double();
        Point screen = new Point();

        latLon.y = lat;

        int numPoints = NUM_SEGMENTS;
        double step = 2.0 * Math.PI / numPoints;

        for (int i = 0; i <= numPoints; i++) {
            double lon = -Math.PI + i * step;
            latLon.x = lon;

            if (!isPointVisible(latLon)) {
                continue;
            }

            latLonToXY(latLon, xy);
            if (Double.isNaN(xy.x) || Double.isNaN(xy.y)) {
                continue;
            }

            container.worldToLocal(screen, xy);

            if (path.getCurrentPoint() == null) {
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

        GeneralPath path = new GeneralPath();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy = new Point2D.Double();
        Point screen = new Point();

        latLon.x = lon;

        // Determine visible latitude range for this central latitude
        List<double[]> ranges = getVisibleLatitudeRanges(centerLat);

        int numPoints = NUM_SEGMENTS;

        for (double[] range : ranges) {
            double latMin = range[0];
            double latMax = range[1];

            for (int i = 0; i <= numPoints; i++) {
                double t = (double) i / numPoints;
                double lat = latMin + t * (latMax - latMin);
                latLon.y = lat;

                if (!isPointVisible(latLon)) {
                    continue;
                }

                latLonToXY(latLon, xy);
                if (Double.isNaN(xy.x) || Double.isNaN(xy.y)) {
                    continue;
                }

                container.worldToLocal(screen, xy);

                if (path.getCurrentPoint() == null) {
                    path.moveTo(screen.x, screen.y);
                } else {
                    path.lineTo(screen.x, screen.y);
                }
            }
        }

        Color oldColor = g2.getColor();
        g2.setColor(Color.LIGHT_GRAY);
        g2.draw(path);
        g2.setColor(oldColor);
    }

    @Override
    public boolean isPointVisible(Point2D.Double latLon) {
        double lon = latLon.x;
        double lat = latLon.y;

        double deltaLon = lon - centerLon;

        double cosLat = Math.cos(lat);
        double sinLat = Math.sin(lat);
        double cosDeltaLon = Math.cos(deltaLon);

        double cosLat0 = Math.cos(centerLat);
        double sinLat0 = Math.sin(centerLat);

        // z = dot(viewDirection, surface normal)
        double z = sinLat0 * sinLat + cosLat0 * cosLat * cosDeltaLon;
        return z > 0.0;
    }

    @Override
    public EProjection getProjection() {
        return EProjection.ORTHOGRAPHIC;
    }

    @Override
    public Rectangle2D.Double getXYBounds() {
        // Projection is the unit disk centered at (0, 0)
        return new Rectangle2D.Double(-R, -R, 2.0 * R, 2.0 * R);
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
        Path2D path = new Path2D.Double();
        Point2D.Double world = new Point2D.Double();
        Point screen = new Point();

        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            double theta = 2.0 * Math.PI * i / NUM_SEGMENTS;
            double x = R * Math.cos(theta);
            double y = R * Math.sin(theta);

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

    /**
     * Compute the visible latitude range(s) for a given central latitude.
     * For the orthographic projection the visible band is simply
     * {@code [centerLat - π/2, centerLat + π/2]} intersected with
     * {@code [-π/2, π/2]}.
     *
     * @param centerLat central latitude in radians
     * @return list containing one [minLat, maxLat] segment
     */
    private List<double[]> getVisibleLatitudeRanges(double centerLat) {
        double minLat = centerLat - Math.PI / 2.0;
        double maxLat = centerLat + Math.PI / 2.0;

        minLat = Math.max(minLat, -Math.PI / 2.0);
        maxLat = Math.min(maxLat, Math.PI / 2.0);

        List<double[]> ranges = new ArrayList<>();
        ranges.add(new double[] { minLat, maxLat });
        return ranges;
    }
}
