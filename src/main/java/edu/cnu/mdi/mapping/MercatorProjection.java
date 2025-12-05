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
 * Mercator map projection implementation (spherical).
 * Uses a practical latitude cutoff (±85°) to avoid infinite Y at the poles.
 */
public class MercatorProjection implements IMapProjection {

    private static final double MAX_LAT_DEG = 85.0;
    private static final double MAX_LAT = Math.toRadians(MAX_LAT_DEG);

    private static final double MIN_LON = -Math.PI;
    private static final double MAX_LON =  Math.PI;

    private static final double MIN_Y = mercatorY(-MAX_LAT);
    private static final double MAX_Y = mercatorY(MAX_LAT);

    /** Active theme used for rendering. */
    private MapTheme theme;

    /**
     * Creates a Mercator projection using the default light theme.
     */
    public MercatorProjection() {
        this(MapTheme.light());
    }

    /**
     * Creates a Mercator projection with the specified {@link MapTheme}.
     *
     * @param theme the theme to use (must not be {@code null})
     */
    public MercatorProjection(MapTheme theme) {
        setTheme(theme);
    }

    /** Forward Mercator Y formula: y = ln(tan(pi/4 + φ/2)) */
    private static double mercatorY(double phi) {
        return Math.log(Math.tan((Math.PI / 4.0) + (phi / 2.0)));
    }

    @Override
    public void latLonToXY(LatLon latLon, XY xy) {
        double lambda = latLon.lambda();
        double phi    = latLon.phi();

        // Clamp to avoid singularity at poles
        phi = Math.max(-MAX_LAT, Math.min(MAX_LAT, phi));

        xy.set(lambda, mercatorY(phi));
    }

    @Override
    public void xyToLatLon(XY xy, LatLon latLon) {
        double x = xy.x();
        double y = xy.y();

        double lambda = x;
        double phi = 2.0 * Math.atan(Math.exp(y)) - (Math.PI / 2.0);

        latLon.set(lambda, phi);
    }

    @Override
    public void drawMapOutline(Graphics2D g2, IContainer container) {
        Rectangle2D.Double r = getXYBounds();

        Point2D.Double world = new Point2D.Double();
        Point pLL = new Point();
        Point pLR = new Point();
        Point pUR = new Point();
        Point pUL = new Point();

        // Lower-left
        world.setLocation(r.x, r.y);
        container.worldToLocal(pLL, world);

        // Lower-right
        world.setLocation(r.x + r.width, r.y);
        container.worldToLocal(pLR, world);

        // Upper-right
        world.setLocation(r.x + r.width, r.y + r.height);
        container.worldToLocal(pUR, world);

        // Upper-left
        world.setLocation(r.x, r.y + r.height);
        container.worldToLocal(pUL, world);

        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();

        g2.setColor(theme.getOutlineColor());
        g2.setStroke(new BasicStroke(theme.getOutlineStrokeWidth()));

        // Draw edges
        g2.drawLine(pLL.x, pLL.y, pLR.x, pLR.y);
        g2.drawLine(pLR.x, pLR.y, pUR.x, pUR.y);
        g2.drawLine(pUR.x, pUR.y, pUL.x, pUL.y);
        g2.drawLine(pUL.x, pUL.y, pLL.x, pLL.y);

        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    @Override
    public boolean isPointOnMap(XY xy) {
        double x = xy.x();
        double y = xy.y();
        return (x >= MIN_LON && x <= MAX_LON &&
                y >= MIN_Y  && y <= MAX_Y);
    }

    @Override
    public void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude) {
        if (Math.abs(latitude) > MAX_LAT) {
            return;
        }

        LatLon latLon = new LatLon();
        XY xy = new XY();
        Point2D.Double world = new Point2D.Double();
        Point p0 = new Point();
        Point p1 = new Point();

        // West end
        latLon.set(MIN_LON, latitude);
        latLonToXY(latLon, xy);
        world.setLocation(xy.x(), xy.y());
        container.worldToLocal(p0, world);

        // East end
        latLon.set(MAX_LON, latitude);
        latLonToXY(latLon, xy);
        world.setLocation(xy.x(), xy.y());
        container.worldToLocal(p1, world);

        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();

        g2.setColor(theme.getGraticuleColor());
        g2.setStroke(new BasicStroke(theme.getGraticuleStrokeWidth()));
        g2.drawLine(p0.x, p0.y, p1.x, p1.y);

        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    @Override
    public void drawLongitudeLine(Graphics2D g2, IContainer container, double longitude) {
        if (longitude < MIN_LON || longitude > MAX_LON) {
            return;
        }

        LatLon latLon = new LatLon();
        XY xy = new XY();
        Point2D.Double world = new Point2D.Double();
        Point p0 = new Point();
        Point p1 = new Point();

        // North end
        latLon.set(longitude, MAX_LAT);
        latLonToXY(latLon, xy);
        world.setLocation(xy.x(), xy.y());
        container.worldToLocal(p0, world);

        // South end
        latLon.set(longitude, -MAX_LAT);
        latLonToXY(latLon, xy);
        world.setLocation(xy.x(), xy.y());
        container.worldToLocal(p1, world);

        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();

        g2.setColor(theme.getGraticuleColor());
        g2.setStroke(new BasicStroke(theme.getGraticuleStrokeWidth()));
        g2.drawLine(p0.x, p0.y, p1.x, p1.y);

        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    @Override
    public boolean isPointVisible(LatLon latLon) {
        double phi = latLon.phi();
        return (phi >= -MAX_LAT && phi <= MAX_LAT);
    }

    @Override
    public EProjection getProjection() {
        return EProjection.MERCATOR;
    }

    @Override
    public Rectangle2D.Double getXYBounds() {
        return new Rectangle2D.Double(
                MIN_LON,
                MIN_Y,
                MAX_LON - MIN_LON,
                MAX_Y  - MIN_Y
        );
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
