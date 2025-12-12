package edu.cnu.mdi.mapping;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Renders a collection of {@link GeoJsonCityLoader.CityFeature} instances
 * as points (and optional labels) using an {@link IMapProjection},
 * {@link MapTheme}, and {@link IContainer}.
 * <p>
 * The renderer uses the projection's {@link IMapProjection#getXYBounds()}
 * to construct a world-to-screen transform via the container, so that
 * multiple layers (countries, graticule, cities) line up.
 * </p>
 * <p>
 * Conventions:
 * <ul>
 *   <li>Input city coordinates are in radians (lon/lat) as provided by
 *       {@link GeoJsonCityLoader.CityFeature}.</li>
 *   <li>They are stored in a {@link Point2D.Double}
 *       with {@code x = λ} (longitude) and {@code y = φ} (latitude).</li>
 *   <li>Projection-space coordinates are likewise represented as
 *       {@code Point2D.Double} in the projection's XY plane.</li>
 * </ul>
 * </p>
 */
public class CityPointRenderer {

    private final List<GeoJsonCityLoader.CityFeature> cities;
    private final IMapProjection projection;

    // Rendering options
    private boolean drawLabels = true;
    private boolean useAntialias = true;

    /** Minimum population to draw; &lt;= 0 means no population filter. */
    private long minPopulation = 0L;

    /**
     * Maximum scalerank to draw; negative means no scalerank filter.
     * If set to 0 only the most important cities are drawn.
     */
    private int maxScalerank = -1;

    /**
     * Maximum scalerank to draw a label for. Negative means labels are
     * not restricted by scalerank (beyond whatever {@link #maxScalerank}
     * enforces).
     * <p>
     * By default this is set to a modest value, so only relatively
     * important cities receive labels, even if smaller towns still
     * get dots.
     * </p>
     */
    private int maxLabelScalerank = 1;

    /** Radius of city marker in screen pixels. */
    private double pointRadius = 2.5;

    /** Horizontal offset in pixels from point to label. */
    private int labelOffsetX = 4;

    /** Vertical offset in pixels from point to label baseline. */
    private int labelOffsetY = -2;

    /** Font for labels. */
    private Font labelFont = Fonts.smallFont;

    /**
     * Create a renderer for the given cities and projection.
     *
     * @param cities     immutable list of city features to render
     * @param projection map projection used for coordinate transforms
     */
    public CityPointRenderer(List<GeoJsonCityLoader.CityFeature> cities,
                             IMapProjection projection) {
        this.cities = Objects.requireNonNull(cities, "cities");
        this.projection = Objects.requireNonNull(projection, "projection");
    }

    /** Enable or disable drawing of city labels. */
    public void setDrawLabels(boolean drawLabels) {
        this.drawLabels = drawLabels;
    }

    /** Enable or disable antialiasing. */
    public void setUseAntialias(boolean useAntialias) {
        this.useAntialias = useAntialias;
    }

    /** Set the minimum population for a city to be drawn; &lt;= 0 disables. */
    public void setMinPopulation(long minPopulation) {
        this.minPopulation = minPopulation;
    }

    /**
     * Only draw cities with {@code scalerank <= maxScalerank}.
     * Set to a negative value to disable this filter entirely.
     */
    public void setMaxScalerank(int maxScalerank) {
        this.maxScalerank = maxScalerank;
    }

    /**
     * Only draw labels for cities with {@code scalerank <= maxLabelScalerank}.
     * <p>
     * This is applied in addition to {@link #maxScalerank} and population
     * filtering. A negative value disables additional label filtering, so
     * any city that is drawn can be labeled (subject to {@link #setDrawLabels}).
     *
     * @param maxLabelScalerank maximum scalerank allowed for labels, or
     *                          a negative value to disable label filtering
     *                          by scalerank.
     */
    public void setMaxLabelScalerank(int maxLabelScalerank) {
        this.maxLabelScalerank = maxLabelScalerank;
    }

    /** Set the radius of city markers in screen pixels (minimum 0.5). */
    public void setPointRadius(double pointRadius) {
        this.pointRadius = Math.max(0.5, pointRadius);
    }

    /**
     * Set the font used for city labels.
     *
     * @param font label font; if {@code null}, a default will be used
     */
    public void setLabelFont(Font font) {
        this.labelFont = (font != null) ? font : Fonts.smallFont;
    }

    /**
     * Render all cities that pass the current filters.
     *
     * @param g2        graphics context
     * @param container container providing the drawing surface and coordinate
     *                  transforms
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
        Font oldFont = g2.getFont();

        g2.setFont(labelFont);
        MapTheme theme = projection.getTheme();
        Color pointColor = theme.getCityColor() != null
                ? theme.getCityColor()
                : theme.getLabelColor();
        Color labelColor = theme.getLabelColor() != null
                ? theme.getLabelColor()
                : pointColor;

        FontMetrics fm = g2.getFontMetrics();

        double r = pointRadius;
        Ellipse2D.Double marker = new Ellipse2D.Double();

        Point screen = new Point();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy = new Point2D.Double();

        for (GeoJsonCityLoader.CityFeature city : cities) {

            if (!passesFilters(city)) {
                continue;
            }

            double lonRad = city.getLongitude();
            double latRad = city.getLatitude();

            latLon.setLocation(lonRad, latRad);

            if (!projection.isPointVisible(latLon)) {
                continue;
            }

            projection.latLonToXY(latLon, xy);
            if (!projection.isPointOnMap(xy)) {
                continue;
            }

            container.worldToLocal(screen, xy);

            double cx = screen.x;
            double cy = screen.y;

            // Draw the point marker
            marker.setFrame(cx - r, cy - r, 2 * r, 2 * r);
            g2.setColor(pointColor);
            g2.fill(marker);

            // Optional label (with additional scalerank check)
            if (drawLabels && city.getName() != null && labelPassesScalerank(city)) {
                g2.setColor(labelColor);
                int textX = (int) Math.round(cx + labelOffsetX);
                int textY = (int) Math.round(cy + labelOffsetY);
                // Ensure label baseline is visible (simple clipping guard)
                if (textY >= fm.getAscent()) {
                    g2.drawString(city.getName(), textX, textY);
                }
            }
        }

        // restore graphics state
        g2.setColor(oldColor);
        g2.setFont(oldFont);
        resetAntialias(g2, oldAA);
    }

    /**
     * Return whether the city passes the population and global scalerank
     * filters used for drawing the point itself.
     */
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

    /**
     * Return whether the city passes the more restrictive label scalerank
     * filter (if enabled). This is evaluated only after the city has already
     * passed {@link #passesFilters(GeoJsonCityLoader.CityFeature)}.
     */
    private boolean labelPassesScalerank(GeoJsonCityLoader.CityFeature city) {
        if (maxLabelScalerank < 0) {
            // no extra label filter
            return true;
        }
        int sr = city.getScalerank();
        if (sr < 0) {
            // unknown scalerank: allow it (or change this if you prefer)
            return true;
        }
        return sr <= maxLabelScalerank;
    }

    /**
     * Find the nearest city to the given mouse position (in local/screen
     * coordinates) that passes the current filters.
     *
     * @param mouseLocal mouse position in local coordinates
     * @param container  drawing container
     * @return closest city within a small picking radius, or {@code null} if
     *         none are close enough.
     */
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

        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy = new Point2D.Double();

        for (GeoJsonCityLoader.CityFeature city : cities) {

            if (!passesFilters(city)) {
                continue;
            }

            double lonRad = city.getLongitude();
            double latRad = city.getLatitude();

            latLon.setLocation(lonRad, latRad);

            if (!projection.isPointVisible(latLon)) {
                continue;
            }

            projection.latLonToXY(latLon, xy);
            if (!projection.isPointOnMap(xy)) {
                continue;
            }

            // World (projection plane) coordinate
            world.setLocation(xy.x, xy.y);

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
