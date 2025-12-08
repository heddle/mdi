package edu.cnu.mdi.mapping;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;

/**
 * Renders a collection of {@link GeoJsonCountryLoader.CountryFeature} instances
 * onto an {@link IContainer} using a supplied {@link IMapProjection} and
 * {@link MapTheme}.
 * <p>
 * This renderer uses a {@link CountryShapeCache} to store the projected
 * country polygons in world (projection XY) coordinates. At render time,
 * the cached world paths are transformed to screen space using the
 * container's {@code worldToLocal} transform so that zoom and pan are
 * automatically respected.
 * </p>
 * <p>
 * The same cached shapes are also used for hit-testing, enabling efficient
 * mouseover feedback (e.g., "which country is under the mouse?").
 * </p>
 */
public class CountryFeatureRenderer {

    /** Original country features as loaded from GeoJSON. */
    private final List<GeoJsonCountryLoader.CountryFeature> countryFeatures;

    /** Projection used to convert lon/lat into world XY coordinates. */
    private final IMapProjection projection;

    /** Visual theme used for land fill and border styling. */
    private final MapTheme theme;

    /** Cache of projected country polygons in world (XY) coordinates. */
    private final CountryShapeCache shapeCache;

    // Rendering flags
    private boolean fillLand = true;
    private boolean drawBorders = true;
    private boolean useAntialias = true;

    /**
     * Construct a new renderer for the given country features and projection.
     * The current theme is obtained from the supplied projection.
     *
     * @param countryFeatures the country features to render
     * @param projection      the map projection to use for rendering
     */
    public CountryFeatureRenderer(List<GeoJsonCountryLoader.CountryFeature> countryFeatures,
                                  IMapProjection projection) {
        this.countryFeatures = Objects.requireNonNull(countryFeatures, "countryFeatures");
        this.projection = Objects.requireNonNull(projection, "projection");
        this.theme = this.projection.getTheme();
        this.shapeCache = new CountryShapeCache(countryFeatures);
    }

    /**
     * Enable or disable filling country polygons with the land color.
     *
     * @param fillLand {@code true} to fill land areas; {@code false} to draw
     *                 only borders
     */
    public void setFillLand(boolean fillLand) {
        this.fillLand = fillLand;
    }

    /**
     * Enable or disable drawing country borders.
     *
     * @param drawBorders {@code true} to draw borders; {@code false} to skip
     *                    border strokes
     */
    public void setDrawBorders(boolean drawBorders) {
        this.drawBorders = drawBorders;
    }

    /**
     * Enable or disable antialiasing during rendering.
     *
     * @param useAntialias {@code true} to enable antialiasing;
     *                     {@code false} to use the existing hint
     */
    public void setUseAntialias(boolean useAntialias) {
        this.useAntialias = useAntialias;
    }

    /**
     * Invalidate the cached country shapes, forcing the next render or
     * hit-test to rebuild the cache for the current projection.
     * <p>
     * Call this whenever the projection's geometry changes in a way that
     * affects the projected coordinates (for example, if the projection
     * type changes, or parameters such as the central meridian are updated).
     * </p>
     */
    public void invalidateCache() {
        shapeCache.invalidate();
    }

    /**
     * Render all configured country features onto the given graphics context
     * within the supplied container.
     *
     * @param g2        graphics context to draw into
     * @param container container providing the world-to-local transform
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

        Stroke oldStroke = g2.getStroke();
        Color oldColor = g2.getColor();

        Color landColor = theme.getLandColor();
        Color borderColor = theme.getBorderColor();
        Stroke borderStroke = theme.getBorderStroke();
        if (borderStroke == null) {
            borderStroke = new BasicStroke(0.5f);
        }

        // Use cached world-space shapes, and map to screen for drawing
        List<CountryShapeCache.CountryShape> shapes = shapeCache.getShapes(projection);
        for (CountryShapeCache.CountryShape shape : shapes) {
            drawCountryShape(g2, container, shape, landColor, borderColor, borderStroke);
        }

        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
        resetAntialias(g2, oldAA);
    }

    /**
     * Draw a single cached country shape. The shape is stored in world
     * coordinates; this method converts it to a screen-space path using
     * the container's {@code worldToLocal} transform.
     *
     * @param g2           graphics context
     * @param container    container providing world-to-local transform
     * @param shape        cached world-space country shape
     * @param landColor    fill color for land
     * @param borderColor  stroke color for borders
     * @param borderStroke stroke used for borders
     */
    private void drawCountryShape(Graphics2D g2,
                                  IContainer container,
                                  CountryShapeCache.CountryShape shape,
                                  Color landColor,
                                  Color borderColor,
                                  Stroke borderStroke) {

        Path2D.Double worldPath = shape.getWorldPath();
        if (worldPath == null) {
            return;
        }

        // Build a screen-space path from the world-space path
        Path2D.Double screenPath = new Path2D.Double(Path2D.WIND_NON_ZERO);
        PathIterator it = worldPath.getPathIterator(null);
        double[] coords = new double[6];
        Point screenPoint = new Point();

        while (!it.isDone()) {
            int segType = it.currentSegment(coords);

            switch (segType) {
                case PathIterator.SEG_MOVETO: {
                    double wx = coords[0];
                    double wy = coords[1];
                    container.worldToLocal(screenPoint, wx, wy);
                    screenPath.moveTo(screenPoint.x, screenPoint.y);
                    break;
                }
                case PathIterator.SEG_LINETO: {
                    double wx = coords[0];
                    double wy = coords[1];
                    container.worldToLocal(screenPoint, wx, wy);
                    screenPath.lineTo(screenPoint.x, screenPoint.y);
                    break;
                }
                case PathIterator.SEG_CLOSE: {
                    screenPath.closePath();
                    break;
                }
                default:
                    // worldPath only uses move/line/close; other types are not expected
                    break;
            }

            it.next();
        }

        if (screenPath.getBounds2D().isEmpty()) {
            return;
        }

        if (fillLand && landColor != null) {
            g2.setColor(landColor);
            g2.fill(screenPath);
        }

        if (drawBorders && borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(borderStroke);
            g2.draw(screenPath);
        }
    }

    /**
     * Perform a hit-test on the cached country shapes to determine which
     * country (if any) lies under the given mouse position.
     *
     * @param mouseLocal mouse position in the container's local coordinate space
     * @param container  container providing the local-to-world transform
     * @return the first {@link GeoJsonCountryLoader.CountryFeature} whose
     *         projected polygon contains the mouse position, or {@code null}
     *         if no country was hit
     */
    public GeoJsonCountryLoader.CountryFeature pickCountry(Point mouseLocal,
                                                           IContainer container) {
        Objects.requireNonNull(mouseLocal, "mouseLocal");
        Objects.requireNonNull(container, "container");
        return shapeCache.pickCountry(mouseLocal, container, projection);
    }

    /**
     * Restore the antialiasing hint after rendering.
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
