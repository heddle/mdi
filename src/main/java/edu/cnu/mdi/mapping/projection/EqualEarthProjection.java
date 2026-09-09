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
import edu.cnu.mdi.mapping.graphics.MapGraphics;
import edu.cnu.mdi.mapping.theme.MapTheme;

/**
 * Equal Earth equal-area pseudocylindrical projection for a unit sphere.
 *
 * <p>The implementation follows Bojan Šavrič, Bernhard Jenny, and Tom
 * Patterson, “The Equal Earth map projection,” 2018. It maps the whole globe,
 * preserves relative area, and has gently curved meridians with straight
 * parallels. Geographic coordinates and the central meridian are expressed in
 * radians.</p>
 *
 * @see <a href="https://doi.org/10.1080/13658816.2018.1504949">The Equal Earth
 *      map projection</a>
 */
public class EqualEarthProjection implements IMapProjection {

    private static final double A1 = 1.340264;
    private static final double A2 = -0.081106;
    private static final double A3 = 0.000893;
    private static final double A4 = 0.003796;
    private static final double SQRT3 = Math.sqrt(3.0);
    private static final double THETA_MAX = Math.PI / 3.0;
    private static final double DOMAIN_TOLERANCE = 1.0e-9;
    private static final double NEWTON_TOLERANCE = 1.0e-13;
    private static final int MAX_NEWTON_ITERATIONS = 12;
    private static final int NUM_SEGMENTS = 360;

    private static final double Y_MAX = ordinate(THETA_MAX);
    private static final double X_MAX = abscissa(Math.PI, 0.0);

    private double lambda0 = Math.toRadians(-70.0);
    private MapTheme theme;

    /**
     * Creates an Equal Earth projection with a central meridian of -70 degrees.
     *
     * @param theme drawing theme; must not be {@code null}
     */
    public EqualEarthProjection(MapTheme theme) {
        setTheme(theme);
    }

    /** @return the central meridian in radians */
    public double getCentralLongitude() {
        return lambda0;
    }

    /**
     * Sets the central meridian.
     *
     * @param centralLongitude central longitude in radians
     */
    public void setCentralLongitude(double centralLongitude) {
        lambda0 = wrapLongitude(centralLongitude);
    }

    private static double polynomial(double theta) {
        double theta2 = theta * theta;
        double theta6 = theta2 * theta2 * theta2;
        return A1 + A2 * theta2 + A3 * theta6 + A4 * theta6 * theta2;
    }

    private static double derivativePolynomial(double theta) {
        double theta2 = theta * theta;
        double theta6 = theta2 * theta2 * theta2;
        return A1 + 3.0 * A2 * theta2 + 7.0 * A3 * theta6
                + 9.0 * A4 * theta6 * theta2;
    }

    private static double ordinate(double theta) {
        return theta * polynomial(theta);
    }

    private static double abscissa(double deltaLongitude, double theta) {
        return 2.0 * SQRT3 * deltaLongitude * Math.cos(theta)
                / (3.0 * derivativePolynomial(theta));
    }

    private static double thetaFromY(double y) {
        double theta = Math.max(-THETA_MAX, Math.min(THETA_MAX, y / A1));
        for (int i = 0; i < MAX_NEWTON_ITERATIONS; i++) {
            double correction = (ordinate(theta) - y) / derivativePolynomial(theta);
            theta -= correction;
            if (Math.abs(correction) < NEWTON_TOLERANCE) {
                break;
            }
        }
        return theta;
    }

    @Override
    public void latLonToXY(Point2D.Double latLon, Point2D.Double xy) {
        if (!Double.isFinite(latLon.x) || !Double.isFinite(latLon.y)) {
            xy.setLocation(Double.NaN, Double.NaN);
            return;
        }
        double latitude = Math.max(-Math.PI / 2.0, Math.min(Math.PI / 2.0, latLon.y));
        double theta = Math.asin((SQRT3 / 2.0) * Math.sin(latitude));
        double deltaLongitude = wrapLongitude(latLon.x - lambda0);
        xy.setLocation(abscissa(deltaLongitude, theta), ordinate(theta));
    }

    @Override
    public void latLonFromXY(Point2D.Double latLon, Point2D.Double xy) {
        if (!isPointOnMap(xy)) {
            latLon.setLocation(Double.NaN, Double.NaN);
            return;
        }

        double theta = thetaFromY(xy.y);
        double sinLatitude = 2.0 * Math.sin(theta) / SQRT3;
        double latitude = Math.asin(Math.max(-1.0, Math.min(1.0, sinLatitude)));
        double deltaLongitude = 3.0 * xy.x * derivativePolynomial(theta)
                / (2.0 * SQRT3 * Math.cos(theta));
        latLon.setLocation(wrapLongitude(lambda0 + deltaLongitude), latitude);
    }

    @Override
    public boolean isPointVisible(Point2D.Double latLon) {
        return latLon != null && Double.isFinite(latLon.x) && Double.isFinite(latLon.y)
                && latLon.y >= -Math.PI / 2.0 && latLon.y <= Math.PI / 2.0;
    }

    @Override
    public boolean isPointOnMap(Point2D.Double xy) {
        if (xy == null || !Double.isFinite(xy.x) || !Double.isFinite(xy.y)
                || Math.abs(xy.y) > Y_MAX + DOMAIN_TOLERANCE) {
            return false;
        }
        double theta = thetaFromY(Math.max(-Y_MAX, Math.min(Y_MAX, xy.y)));
        double halfWidth = Math.abs(abscissa(Math.PI, theta));
        return Math.abs(xy.x) <= halfWidth + DOMAIN_TOLERANCE;
    }

    @Override
    public boolean crossesSeam(double lon1, double lon2) {
        double d1 = wrapLongitude(lon1 - lambda0);
        double d2 = wrapLongitude(lon2 - lambda0);
        return Math.abs(d1 - d2) > Math.PI;
    }

    @Override
    public boolean isLongitudePeriodic() {
        return true;
    }

    private Path2D createBoundary(IContainer container) {
        Path2D path = new Path2D.Double();
        Point2D.Double world = new Point2D.Double();
        Point screen = new Point();

        for (int side = 0; side < 2; side++) {
            double deltaLongitude = (side == 0) ? Math.PI : -Math.PI;
            for (int i = 0; i <= NUM_SEGMENTS; i++) {
                double fraction = (side == 0) ? i / (double) NUM_SEGMENTS
                        : 1.0 - i / (double) NUM_SEGMENTS;
                double latitude = -Math.PI / 2.0 + Math.PI * fraction;
                double theta = Math.asin((SQRT3 / 2.0) * Math.sin(latitude));
                world.setLocation(abscissa(deltaLongitude, theta), ordinate(theta));
                container.worldToLocal(screen, world);
                if (side == 0 && i == 0) {
                    path.moveTo(screen.x, screen.y);
                } else {
                    path.lineTo(screen.x, screen.y);
                }
            }
        }
        path.closePath();
        return path;
    }

    @Override
    public void drawMapOutline(Graphics2D g2, IContainer container) {
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(theme.getOutlineColor());
        g2.setStroke(new BasicStroke(theme.getOutlineStrokeWidth()));
        g2.draw(createBoundary(container));
        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    @Override
    public void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude) {
        if (latitude >= -Math.PI / 2.0 && latitude <= Math.PI / 2.0) {
            MapGraphics.drawHorizontalLatitudeLine(g2, container, this, latitude,
                    lambda0, theme);
        }
    }

    @Override
    public void drawLongitudeLine(Graphics2D g2, IContainer container, double longitude) {
        Path2D path = new Path2D.Double();
        Point2D.Double latLon = new Point2D.Double(longitude, 0.0);
        Point2D.Double world = new Point2D.Double();
        Point screen = new Point();
        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            latLon.y = -Math.PI / 2.0 + Math.PI * i / NUM_SEGMENTS;
            latLonToXY(latLon, world);
            container.worldToLocal(screen, world);
            if (i == 0) {
                path.moveTo(screen.x, screen.y);
            } else {
                path.lineTo(screen.x, screen.y);
            }
        }
        Color oldColor = g2.getColor();
        g2.setColor(theme.getGraticuleColor());
        g2.draw(path);
        g2.setColor(oldColor);
    }

    @Override
    public Shape createClipShape(IContainer container) {
        return createBoundary(container);
    }

    @Override
    public EProjection getProjection() {
        return EProjection.EQUAL_EARTH;
    }

    @Override
    public Rectangle2D.Double getXYBounds() {
        return new Rectangle2D.Double(-X_MAX, -Y_MAX, 2.0 * X_MAX, 2.0 * Y_MAX);
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
