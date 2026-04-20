package edu.cnu.mdi.mapping.render;

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
import edu.cnu.mdi.mapping.loader.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.theme.MapTheme;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Renders a collection of {@link GeoJsonCityLoader.CityFeature} instances as
 * marker dots and optional text labels using an {@link IMapProjection},
 * {@link MapTheme}, and {@link IContainer}.
 *
 * <h2>Coordinate conventions</h2>
 * <p>City coordinates stored in {@link GeoJsonCityLoader.CityFeature} are in
 * radians (λ = longitude, φ = latitude), as required by
 * {@link IMapProjection#latLonToXY}. All coordinate intermediate values use
 * the same {@code x = λ, y = φ} convention.</p>
 *
 * <h2>Filtering</h2>
 * <p>Two independent filter levels control what is drawn:
 * <ol>
 *   <li><b>Point filter</b> — controlled by {@link #setMinPopulation(long)}
 *       and {@link #setMaxScalerank(int)}. Cities that fail this filter are
 *       not drawn at all.</li>
 *   <li><b>Label filter</b> — an additional scalerank threshold controlled by
 *       {@link #setMaxLabelScalerank(int)}. Cities that pass the point filter
 *       but not the label filter still get a dot, but no text label.</li>
 * </ol>
 *
 * <h2>Picking</h2>
 * <p>{@link #pickCity(Point, IContainer)} performs a brute-force linear scan
 * over all visible cities to find the nearest one within a small pick radius.
 * This is called on every mouse-move event via the feedback mechanism, so the
 * city list should remain small (the default population filter achieves this
 * in practice). Unlike {@link CountryRenderer}, city picking does not depend
 * on a previous render call — it reprojects cities on demand.</p>
 *
 * <h2>Thread safety</h2>
 * <p>This class is not thread-safe. All method calls must be made on the
 * Event Dispatch Thread.</p>
 *
 * <h2>City list ownership</h2>
 * <p>The city list passed to the constructor is stored by reference without
 * defensive copying. Callers must ensure that the list is effectively
 * immutable after construction (or is not modified while rendering is in
 * progress). The list returned by {@link GeoJsonCityLoader} is already
 * unmodifiable, so in normal usage this is safe.</p>
 */
public class CityPointRenderer {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /**
     * Unmodifiable list of city features to render, stored by reference.
     * See the class-level note on list ownership.
     */
    private final List<GeoJsonCityLoader.CityFeature> cities;

    /** Projection used for coordinate transforms and visibility tests. */
    private final IMapProjection projection;

    // Rendering options
    private boolean drawLabels   = true;
    private boolean useAntialias = true;

    /**
     * Minimum population for a city to be drawn.
     * A value &le; 0 disables population filtering.
     */
    private long minPopulation = 0L;

    /**
     * Maximum scalerank for a city to be drawn.
     * A negative value disables scalerank filtering.
     * Scalerank 0 = most important; higher = less important.
     */
    private int maxScalerank = -1;

    /**
     * Maximum scalerank for a label to be drawn (applied in addition to
     * {@link #maxScalerank}). A negative value disables additional label
     * filtering, allowing any drawn city to also receive a label.
     *
     * <p>The default is 1, so only the most prominent cities (rank 0 or 1)
     * receive labels even if less prominent cities still get dots.</p>
     */
    private int maxLabelScalerank = 1;

    /** Radius of city marker dots in screen pixels. */
    private double pointRadius = 2.5;

    /** Horizontal offset in pixels from the marker center to the label. */
    private int labelOffsetX = 4;

    /** Vertical offset in pixels from the marker center to the label baseline. */
    private int labelOffsetY = -2;

    /** Font used for city name labels. */
    private Font labelFont = Fonts.smallFont;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a renderer for the given cities and projection.
     *
     * <p>The city list is stored by reference; see the class-level note on
     * list ownership for thread-safety considerations.</p>
     *
     * @param cities     the city features to render; must not be {@code null};
     *                   should be effectively immutable after this call
     * @param projection the map projection; must not be {@code null}
     */
    public CityPointRenderer(List<GeoJsonCityLoader.CityFeature> cities,
                             IMapProjection projection) {
        this.cities     = Objects.requireNonNull(cities,     "cities");
        this.projection = Objects.requireNonNull(projection, "projection");
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /**
     * Enables or disables drawing of city name labels.
     *
     * @param drawLabels {@code true} to draw labels; {@code false} to draw
     *                   dots only
     */
    public void setDrawLabels(boolean drawLabels) { this.drawLabels = drawLabels; }

    /**
     * Enables or disables antialiased rendering.
     *
     * @param useAntialias {@code true} to enable antialiasing
     */
    public void setUseAntialias(boolean useAntialias) { this.useAntialias = useAntialias; }

    /**
     * Sets the minimum population a city must have to be drawn.
     *
     * <p>Cities with {@code population >= 0 && population < minPopulation}
     * are hidden. Cities whose population is unknown ({@code population == -1})
     * are always shown regardless of this setting. Set to &le; 0 to disable
     * population filtering entirely.</p>
     *
     * @param minPopulation minimum population (inclusive), or &le; 0 to disable
     */
    public void setMinPopulation(long minPopulation) { this.minPopulation = minPopulation; }

    /**
     * Sets the maximum scalerank for a city to be drawn.
     *
     * <p>Cities with {@code scalerank > maxScalerank} are hidden. Set to a
     * negative value to disable scalerank filtering. Scalerank 0 denotes
     * the largest / most prominent cities; higher values denote progressively
     * less prominent ones.</p>
     *
     * @param maxScalerank maximum scalerank (inclusive), or negative to disable
     */
    public void setMaxScalerank(int maxScalerank) { this.maxScalerank = maxScalerank; }

    /**
     * Sets the maximum scalerank for which a label is drawn, <em>in addition
     * to</em> the point filter ({@link #setMaxScalerank}).
     *
     * <p>A city that passes the point filter but has
     * {@code scalerank > maxLabelScalerank} still receives a dot but no label.
     * Set to a negative value to disable this additional label filter so that
     * every drawn city also receives a label.</p>
     *
     * @param maxLabelScalerank maximum scalerank for labels (inclusive), or
     *                          negative to disable label scalerank filtering
     */
    public void setMaxLabelScalerank(int maxLabelScalerank) {
        this.maxLabelScalerank = maxLabelScalerank;
    }

    /**
     * Sets the radius of city marker dots in screen pixels.
     *
     * @param pointRadius dot radius in pixels; clamped to a minimum of 0.5
     */
    public void setPointRadius(double pointRadius) {
        this.pointRadius = Math.max(0.5, pointRadius);
    }

    /**
     * Sets the font used for city name labels.
     *
     * @param font label font; if {@code null} the default small font is used
     */
    public void setLabelFont(Font font) {
        this.labelFont = (font != null) ? font : Fonts.smallFont;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /**
     * Renders all cities that pass the current filters.
     *
     * <p>For each visible city that passes {@link #passesFilters}:
     * <ol>
     *   <li>The geographic coordinates are projected to world space and then
     *       to screen space.</li>
     *   <li>A circular dot is drawn at the screen position using the city
     *       color from the theme.</li>
     *   <li>If {@link #drawLabels} is {@code true} and the city also passes
     *       {@link #labelPassesScalerank}, the city name is drawn to the
     *       right of the dot.</li>
     * </ol>
     *
     * <p>The graphics state (color, font, antialiasing hint) is saved before
     * rendering and restored on return, even if an exception occurs.</p>
     *
     * @param g2        graphics context to draw into; must not be {@code null}
     * @param container container providing the world-to-local transform;
     *                  must not be {@code null}
     */
    public void render(Graphics2D g2, IContainer container) {
        Objects.requireNonNull(g2,        "g2");
        Objects.requireNonNull(container, "container");

        Component comp = container.getComponent();
        if (comp.getWidth() <= 0 || comp.getHeight() <= 0) return;

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
        Font  oldFont  = g2.getFont();

        g2.setFont(labelFont);
        MapTheme theme      = projection.getTheme();
        Color    pointColor = theme.getCityColor()  != null ? theme.getCityColor()  : theme.getLabelColor();
        Color    labelColor = theme.getLabelColor() != null ? theme.getLabelColor() : pointColor;

        FontMetrics    fm     = g2.getFontMetrics();
        double         r      = pointRadius;
        Ellipse2D.Double marker = new Ellipse2D.Double();

        // Workspace objects declared once per render call to avoid per-city allocation.
        Point          screen = new Point();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy     = new Point2D.Double();

        for (GeoJsonCityLoader.CityFeature city : cities) {
            if (!passesFilters(city)) continue;

            latLon.setLocation(city.getLongitude(), city.getLatitude());
            if (!projection.isPointVisible(latLon)) continue;

            projection.latLonToXY(latLon, xy);
            if (!projection.isPointOnMap(xy)) continue;

            container.worldToLocal(screen, xy);
            double cx = screen.x;
            double cy = screen.y;

            // Draw the marker dot.
            marker.setFrame(cx - r, cy - r, 2 * r, 2 * r);
            g2.setColor(pointColor);
            g2.fill(marker);

            // Draw an optional label.
            if (drawLabels && city.getName() != null && labelPassesScalerank(city)) {
                g2.setColor(labelColor);
                int textX = (int) Math.round(cx + labelOffsetX);
                int textY = (int) Math.round(cy + labelOffsetY);
                // Only draw if the label baseline is within the component bounds.
                if (textY >= fm.getAscent()) {
                    g2.drawString(city.getName(), textX, textY);
                }
            }
        }

        g2.setColor(oldColor);
        g2.setFont(oldFont);
        resetAntialias(g2, oldAA);
    }

    // -------------------------------------------------------------------------
    // Hit-testing
    // -------------------------------------------------------------------------

    /**
     * Returns the closest visible city within a small picking radius of the
     * given screen position, or {@code null} if no city is nearby.
     *
     * <p>Unlike {@link CountryRenderer#pickCountry}, this method does not
     * rely on any cached state from a previous render call. It reprojects
     * every city from scratch on each invocation, which is acceptable because
     * the population filter keeps the number of visible cities small.</p>
     *
     * <p>The pick radius is {@link #pointRadius} + 2 pixels. If multiple
     * cities fall within that radius the one closest to the cursor is
     * returned.</p>
     *
     * @param mouseLocal mouse position in the container's local (screen)
     *                   coordinate space; must not be {@code null}
     * @param container  container providing the world-to-local transform;
     *                   must not be {@code null}
     * @return the nearest city within the pick radius, or {@code null}
     */
    public GeoJsonCityLoader.CityFeature pickCity(Point mouseLocal, IContainer container) {
        Objects.requireNonNull(mouseLocal, "mouseLocal");
        Objects.requireNonNull(container,  "container");

        double pickRadiusSq = (pointRadius + 2.0) * (pointRadius + 2.0);

        Point          screen = new Point();
        Point2D.Double latLon = new Point2D.Double();
        Point2D.Double xy     = new Point2D.Double();

        GeoJsonCityLoader.CityFeature best      = null;
        double                        bestDistSq = Double.MAX_VALUE;

        for (GeoJsonCityLoader.CityFeature city : cities) {
            if (!passesFilters(city)) continue;

            latLon.setLocation(city.getLongitude(), city.getLatitude());
            if (!projection.isPointVisible(latLon)) continue;

            projection.latLonToXY(latLon, xy);
            if (!projection.isPointOnMap(xy)) continue;

            container.worldToLocal(screen, xy);
            double dx     = mouseLocal.x - screen.x;
            double dy     = mouseLocal.y - screen.y;
            double distSq = dx * dx + dy * dy;

            if (distSq <= pickRadiusSq && distSq < bestDistSq) {
                bestDistSq = distSq;
                best       = city;
            }
        }
        return best;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the city satisfies the population and scalerank
     * thresholds that control whether a dot is drawn.
     *
     * <p>Population filtering: skips the city only when a positive
     * {@link #minPopulation} is set <em>and</em> the city has a known
     * (non-negative) population that falls below the threshold. Cities with
     * an unknown population ({@code -1}) always pass.</p>
     *
     * <p>Scalerank filtering: skips the city only when a non-negative
     * {@link #maxScalerank} is set <em>and</em> the city has a known
     * (non-negative) scalerank that exceeds the threshold. Cities with an
     * unknown scalerank ({@code -1}) always pass.</p>
     *
     * @param city city to test; must not be {@code null}
     * @return {@code true} if the city should be rendered
     */
    private boolean passesFilters(GeoJsonCityLoader.CityFeature city) {
        if (minPopulation > 0
                && city.getPopulation() >= 0
                && city.getPopulation() < minPopulation) {
            return false;
        }
        if (maxScalerank >= 0
                && city.getScalerank() >= 0
                && city.getScalerank() > maxScalerank) {
            return false;
        }
        return true;
    }

    /**
     * Returns {@code true} if the city passes the additional scalerank filter
     * that controls whether a label is drawn.
     *
     * <p>This is evaluated only after the city has already passed
     * {@link #passesFilters}. When {@link #maxLabelScalerank} is negative,
     * every drawn city also receives a label.</p>
     *
     * @param city city to test; must not be {@code null}
     * @return {@code true} if a label should be drawn for this city
     */
    private boolean labelPassesScalerank(GeoJsonCityLoader.CityFeature city) {
        if (maxLabelScalerank < 0) return true; // label filtering disabled
        int sr = city.getScalerank();
        if (sr < 0)               return true; // unknown scalerank: allow label
        return sr <= maxLabelScalerank;
    }

    /**
     * Restores the antialiasing rendering hint to its previous value.
     *
     * @param g2    graphics context
     * @param oldAA previous antialiasing hint (may be {@code null})
     */
    private void resetAntialias(Graphics2D g2, Object oldAA) {
        if (oldAA != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }
    }
}
