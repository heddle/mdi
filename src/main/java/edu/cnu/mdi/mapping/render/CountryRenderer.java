package edu.cnu.mdi.mapping.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;
import edu.cnu.mdi.graphics.world.WorldPolygon;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.projection.EProjection;
import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.theme.MapTheme;

/**
 * Renders a collection of {@link GeoJsonCountryLoader.CountryFeature} instances
 * onto an {@link IContainer} using a supplied {@link IMapProjection} and its
 * associated {@link MapTheme}.
 *
 * <h2>Rendering pipeline</h2>
 * <ol>
 *   <li>Iterate over all country features.</li>
 *   <li>For each polygon ring, split it at any antimeridian crossing (detected
 *       via {@link IMapProjection#crossesSeam}) into at most two
 *       {@link WorldPolygon} objects in world (projection-space)
 *       coordinates.</li>
 *   <li>Draw each polygon via
 *       {@link WorldGraphicsUtils#drawWorldPolygon}, which converts world
 *       coordinates to screen space using the container's transform.</li>
 *   <li>Cache the resulting polygons for later hit-testing by
 *       {@link #pickCountry}.</li>
 * </ol>
 *
 * <h2>Seam splitting</h2>
 * <p>Cylindrical projections (Mercator, Mollweide) have a seam at the edge of
 * the map. Polygon edges that cross the seam would otherwise be drawn as a
 * long horizontal line across the entire map. The renderer detects such
 * crossings via {@link IMapProjection#crossesSeam(double, double)} and routes
 * subsequent points into an "other side" polygon so each half renders
 * correctly.</p>
 *
 * <h2>Hit-testing and cache dependency</h2>
 * <p>{@link #pickCountry(Point, IContainer)} uses a cache of
 * {@link WorldPolygon} objects that is built during {@link #render}. This
 * means {@code pickCountry} must be called <em>after</em> the first
 * {@link #render} call, and will return {@code null} for all countries if
 * called before any render. In practice this is safe because a mouse-over
 * event cannot occur before the first paint, but callers should be aware of
 * the dependency. The cache is fully rebuilt on every {@code render} call so
 * it always reflects the most recent projection state.</p>
 *
 * <h2>Border stroke</h2>
 * <p>The border color and stroke are taken from the theme via
 * {@link MapTheme#getBorderColor()} and {@link MapTheme#getBorderStroke()}.
 * These are passed through to
 * {@link WorldGraphicsUtils#drawWorldPolygon} so that theme switching is
 * fully respected.</p>
 */
public class CountryRenderer {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Country features to render, as loaded from GeoJSON. */
    private final List<GeoJsonCountryLoader.CountryFeature> countryFeatures;

    /** Projection used to convert lon/lat to world XY coordinates. */
    private final IMapProjection projection;

    // Rendering flags
    private boolean fillLand     = true;
    private boolean drawBorders  = true;
    private boolean useAntialias = true;

    /**
     * Cache of projected country polygons built during the last {@link #render}
     * call. Used by {@link #pickCountry(Point, IContainer)} for hit-testing.
     *
     * <p><b>Important:</b> this cache is rebuilt on every {@link #render}
     * call. {@link #pickCountry} must not be called before the first render
     * or the cache will be empty and all pick results will be {@code null}.</p>
     */
    private final List<CountryCache> countryCache = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a renderer for the given country features and projection.
     *
     * <p>The theme is obtained from {@code projection.getTheme()} at render
     * time, so theme changes applied to the projection are reflected
     * automatically without reconstructing the renderer.</p>
     *
     * @param countryFeatures list of country features to render;
     *                        must not be {@code null}
     * @param projection      the map projection; must not be {@code null}
     */
    public CountryRenderer(List<GeoJsonCountryLoader.CountryFeature> countryFeatures,
                           IMapProjection projection) {
        this.countryFeatures = Objects.requireNonNull(countryFeatures, "countryFeatures");
        this.projection      = Objects.requireNonNull(projection, "projection");
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /**
     * Enables or disables filling country polygons with the land color from
     * the active {@link MapTheme}.
     *
     * @param fillLand {@code true} to fill land areas; {@code false} to draw
     *                 borders only
     */
    public void setFillLand(boolean fillLand) { this.fillLand = fillLand; }

    /**
     * Enables or disables drawing country political borders.
     *
     * @param drawBorders {@code true} to draw borders; {@code false} to skip
     */
    public void setDrawBorders(boolean drawBorders) { this.drawBorders = drawBorders; }

    /**
     * Enables or disables antialiased rendering.
     *
     * @param useAntialias {@code true} to enable antialiasing
     */
    public void setUseAntialias(boolean useAntialias) { this.useAntialias = useAntialias; }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /**
     * Renders all country features onto the given graphics context.
     *
     * <p>This method rebuilds the internal hit-test cache on every call, which
     * is necessary because the projection may have been re-centered or the
     * container resized since the last render. The graphics state (color,
     * stroke, antialiasing hint) is fully saved before rendering and restored
     * afterward.</p>
     *
     * @param g2        graphics context to draw into; must not be {@code null}
     * @param container container providing the world-to-local transform;
     *                  must not be {@code null}
     */
    public void render(Graphics2D g2, IContainer container) {
        Objects.requireNonNull(g2,        "g2");
        Objects.requireNonNull(container, "container");

        // Rebuild the hit-test cache for this render pass.
        countryCache.clear();

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

        Stroke oldStroke = g2.getStroke();
        Color  oldColor  = g2.getColor();

        MapTheme theme       = projection.getTheme();
        Color    landColor   = fillLand    ? theme.getLandColor()   : null;
        Color    borderColor = drawBorders ? theme.getBorderColor() : null;

        // Use the theme's border stroke; fall back to a sensible default
        // rather than hardcoding a width here.
        Stroke borderStroke = theme.getBorderStroke();
        if (borderStroke == null) {
            borderStroke = new BasicStroke(0.5f);
        }

        for (GeoJsonCountryLoader.CountryFeature country : countryFeatures) {
            drawCountryShape(g2, container, country, landColor, borderColor, borderStroke);
        }

        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
        resetAntialias(g2, oldAA);
    }

    // -------------------------------------------------------------------------
    // Hit-testing
    // -------------------------------------------------------------------------

    /**
     * Returns the first country whose projected polygon contains the given
     * mouse position, or {@code null} if no country is hit.
     *
     * <p><b>Requires a prior render call.</b> The hit-test uses the
     * {@link WorldPolygon} cache that is built during {@link #render}. If
     * {@code render} has not been called yet (or has been called on a different
     * {@link IContainer}), this method will always return {@code null}.</p>
     *
     * @param mouseLocal mouse position in the container's local coordinate
     *                   space; must not be {@code null}
     * @param container  container providing the local-to-world transform;
     *                   must not be {@code null}
     * @return the hit country, or {@code null}
     */
    public GeoJsonCountryLoader.CountryFeature pickCountry(Point mouseLocal,
                                                           IContainer container) {
        Objects.requireNonNull(mouseLocal, "mouseLocal");
        Objects.requireNonNull(container,  "container");

        Point2D.Double worldPt = new Point2D.Double();
        container.localToWorld(mouseLocal, worldPt);

        for (CountryCache cc : countryCache) {
            if (cc.contains(worldPt)) return cc.country;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Private drawing
    // -------------------------------------------------------------------------

    /**
     * Projects and draws all polygon rings for a single country, splitting
     * each ring at antimeridian crossings.
     *
     * <p>Antarctica is skipped for projections (Mercator, Mollweide) where it
     * would produce a very large distorted polygon that dominates the map and
     * degrades performance.</p>
     *
     * @param g2           graphics context
     * @param container    container providing world-to-local transform
     * @param country      the country to draw
     * @param landColor    fill color for land polygons, or {@code null} to
     *                     skip fill
     * @param borderColor  stroke color for borders, or {@code null} to skip
     *                     border strokes
     * @param borderStroke stroke used for borders
     */
    private void drawCountryShape(Graphics2D g2, IContainer container,
                                  GeoJsonCountryLoader.CountryFeature country,
                                  Color landColor, Color borderColor,
                                  Stroke borderStroke) {

        EProjection proj = projection.getProjection();

        for (List<Point2D.Double> ring : country.getPolygons()) {

            // Skip Antarctica for projections where it distorts badly.
            if ((proj == EProjection.MERCATOR || proj == EProjection.MOLLWEIDE)
                    && country.getAdminName().toLowerCase().startsWith("antarc")) {
                continue;
            }

            WorldPolygon oneSide   = new WorldPolygon();
            WorldPolygon otherSide = new WorldPolygon();
            WorldPolygon current   = oneSide;
            boolean first          = true;
            double  prevLon        = 0.0;

            for (Point2D.Double lonLat : ring) {
                if (!projection.isPointVisible(lonLat)) continue;

                Point2D.Double xy = new Point2D.Double();
                projection.latLonToXY(lonLat, xy);

                if (first) {
                    first = false;
                } else if (projection.crossesSeam(lonLat.x, prevLon)) {
                    // Swap to the other side of the seam.
                    current = (current == oneSide) ? otherSide : oneSide;
                }

                current.addPoint(xy.x, xy.y);
                prevLon = lonLat.x;
            }

            // Draw each non-degenerate half and cache for hit-testing.
            // Pass the theme's border stroke width rather than hardcoding 0.5f.
            float strokeWidth = (borderStroke instanceof BasicStroke bs)
                    ? bs.getLineWidth() : 0.5f;

            if (oneSide.npoints > 2) {
                WorldGraphicsUtils.drawWorldPolygon(g2, container, oneSide,
                                                    landColor, borderColor, strokeWidth);
            }
            if (otherSide.npoints > 2) {
                WorldGraphicsUtils.drawWorldPolygon(g2, container, otherSide,
                                                    landColor, borderColor, strokeWidth);
            }

            countryCache.add(new CountryCache(country, oneSide, otherSide));
        }
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

    // -------------------------------------------------------------------------
    // Cache record
    // -------------------------------------------------------------------------

    /**
     * Internal record that pairs a {@link GeoJsonCountryLoader.CountryFeature}
     * with its most recently projected {@link WorldPolygon} halves.
     *
     * <p>The polygons are stored in projection (world) space, not screen space,
     * so that the hit-test is independent of the current zoom level and pan
     * offset.</p>
     */
    private static final class CountryCache {

        final GeoJsonCountryLoader.CountryFeature country;
        final WorldPolygon oneSide;
        final WorldPolygon otherSide;

        CountryCache(GeoJsonCountryLoader.CountryFeature country,
                     WorldPolygon oneSide, WorldPolygon otherSide) {
            this.country   = country;
            this.oneSide   = oneSide;
            this.otherSide = otherSide;
        }

        /**
         * Returns {@code true} if either polygon half contains the given
         * world-space point.
         *
         * @param worldPt point in projection (world) coordinates
         * @return {@code true} if the point is inside this country's shape
         */
        boolean contains(Point2D.Double worldPt) {
            return (oneSide.npoints   > 2 && oneSide.contains(worldPt.x,   worldPt.y))
                || (otherSide.npoints > 2 && otherSide.contains(worldPt.x, worldPt.y));
        }
    }
}
