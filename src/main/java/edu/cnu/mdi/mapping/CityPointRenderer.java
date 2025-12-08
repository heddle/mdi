package edu.cnu.mdi.mapping;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;

/**
 * Renders a collection of {@link GeoJsonCityLoader.CityFeature} instances
 * as points (and optional labels) using an {@link IMapProjection},
 * {@link MapTheme}, and {@link IContainer}.
 * <p>
 * The renderer uses the projection's {@link IMapProjection#getXYBounds()}
 * to construct a world-to-screen transform in the same style as
 * {@link CountryFeatureRenderer}, so both layers line up.
 */
public class CityPointRenderer {

    private final List<GeoJsonCityLoader.CityFeature> cities;
    private final IMapProjection projection;
    private final MapTheme theme;

    // Rendering options
    private boolean drawLabels = true;
    private boolean useAntialias = true;

    /** Minimum population to draw; <= 0 means no population filter. */
    private long minPopulation = 0L;

    /** Maximum scalerank to draw; negative means no scalerank filter. */
    private int maxScalerank = -1;

    /** Radius of city marker in screen pixels. */
    private double pointRadius = 2.5;

    /** Horizontal offset in pixels from point to label. */
    private int labelOffsetX = 4;

    /** Vertical offset in pixels from point to label baseline. */
    private int labelOffsetY = -2;

    public CityPointRenderer(List<GeoJsonCityLoader.CityFeature> cities,
                             IMapProjection projection) {
        this.cities = Objects.requireNonNull(cities, "cities");
        this.projection = Objects.requireNonNull(projection, "projection");
        this.theme = this.projection.getTheme();
    }

    public void setDrawLabels(boolean drawLabels) {
        this.drawLabels = drawLabels;
    }

    public void setUseAntialias(boolean useAntialias) {
        this.useAntialias = useAntialias;
    }

    /** Only draw cities with population >= {@code minPopulation}. */
    public void setMinPopulation(long minPopulation) {
        this.minPopulation = minPopulation;
    }

    /**
     * Only draw cities with {@code scalerank <= maxScalerank}.
     * Set to a negative value to disable this filter.
     */
    public void setMaxScalerank(int maxScalerank) {
        this.maxScalerank = maxScalerank;
    }

    public void setPointRadius(double pointRadius) {
        this.pointRadius = Math.max(0.5, pointRadius);
    }

    /**
     * Render all cities that pass the current filters.
     *
     * @param g2        graphics context
     * @param container container providing the drawing surface size
     */
    public void render(Graphics2D g2, IContainer container) {
        Objects.requireNonNull(g2, "g2");
        Objects.requireNonNull(container, "container");

        Component comp = container.getComponent();
        int width = comp.getWidth();
        int height = comp.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (useAntialias) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
        }

        Rectangle2D xyBounds = projection.getXYBounds();
        if (xyBounds == null || xyBounds.isEmpty()) {
            resetAntialias(g2, oldAA);
            return;
        }

 
        Color oldColor = g2.getColor();
        Color pointColor = theme.getBorderColor() != null
                ? theme.getBorderColor()
                : theme.getLabelColor();
        Color labelColor = theme.getLabelColor() != null
                ? theme.getLabelColor()
                : pointColor;

        FontMetrics fm = g2.getFontMetrics();

        double r = pointRadius;
        Ellipse2D.Double marker = new Ellipse2D.Double();

        Point screen = new Point();
        for (GeoJsonCityLoader.CityFeature city : cities) {

            if (!passesFilters(city)) {
                continue;
            }

            double lonRad = Math.toRadians(city.getLonDeg());
            double latRad = Math.toRadians(city.getLatDeg());

            LatLon ll = new LatLon(lonRad, latRad);

            if (!projection.isPointVisible(ll)) {
                continue;
            }

            XY xy = projection.latLonToXY(ll);
            if (xy == null || !projection.isPointOnMap(xy)) {
                continue;
            }

            container.worldToLocal(screen, xy.x(), xy.y());
            double cx = screen.x;
            double cy = screen.y;

            // Draw the point marker
            marker.setFrame(cx - r, cy - r, 2 * r, 2 * r);
            g2.setColor(pointColor);
            g2.fill(marker);

            // Optional label
            if (drawLabels && city.getName() != null) {
                g2.setColor(labelColor);
                int textX = (int) Math.round(cx + labelOffsetX);
                int textY = (int) Math.round(cy + labelOffsetY);
                // Ensure label baseline is visible (simple clipping guard)
                if (textY >= fm.getAscent()) {
                    g2.drawString(city.getName(), textX, textY);
                }
            }
        }

        g2.setColor(oldColor);
        resetAntialias(g2, oldAA);
    }

    private boolean passesFilters(GeoJsonCityLoader.CityFeature city) {
        if (minPopulation > 0 && city.getPopulation() >= 0 &&
            city.getPopulation() < minPopulation) {
            return false;
        }
        if (maxScalerank >= 0 && city.getScalerank() >= 0 &&
            city.getScalerank() > maxScalerank) {
            return false;
        }
        return true;
    }

    public GeoJsonCityLoader.CityFeature pickCity(Point mouseLocal,
            IContainer container) {
Objects.requireNonNull(mouseLocal, "mouseLocal");
Objects.requireNonNull(container, "container");

// How far from the point in pixels we accept as "hover"
double pickRadius = pointRadius + 2.0;
double pickRadiusSq = pickRadius * pickRadius;

Point2D.Double world = new Point2D.Double();
Point screen = new Point();

GeoJsonCityLoader.CityFeature best = null;
double bestDistSq = Double.MAX_VALUE;

for (GeoJsonCityLoader.CityFeature city : cities) {

if (!passesFilters(city)) {
continue;
}

double lonRad = Math.toRadians(city.getLonDeg());
double latRad = Math.toRadians(city.getLatDeg());

// lambda = longitude, phi = latitude
LatLon ll = new LatLon(lonRad, latRad);

if (!projection.isPointVisible(ll)) {
continue;
}

XY xy = projection.latLonToXY(ll);
if (xy == null || !projection.isPointOnMap(xy)) {
continue;
}

// World (projection plane) coordinate
world.setLocation(xy.x(), xy.y());

// Map to screen via container (respects zoom/pan)
container.worldToLocal(screen, world);

double dx = mouseLocal.x - screen.x;
double dy = mouseLocal.y - screen.y;
double distSq = dx * dx + dy * dy;

if (distSq <= pickRadiusSq && distSq < bestDistSq) {
bestDistSq = distSq;
best = city;
}
}

return best;
}

    private void resetAntialias(Graphics2D g2, Object oldAA) {
        if (oldAA != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }
    }
}
