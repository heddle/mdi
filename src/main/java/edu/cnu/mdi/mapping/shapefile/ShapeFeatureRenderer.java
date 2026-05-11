package edu.cnu.mdi.mapping.shapefile;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.render.IPickable;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Renders a list of {@link ShapeFeature} instances onto an
 * {@link IContainer} using a supplied {@link IMapProjection} and
 * {@link ShapeFeatureStyle}, and supports mouse-over hit testing via
 * {@link IPickable}.
 *
 * <h2>Geometry dispatch</h2>
 * <p>Each feature is rendered according to its shape type:
 * <ul>
 *   <li><b>Polygon</b> — each ring is filled (if a fill color is set) and
 *       stroked (if a stroke color is set). The stroke sits on top of the
 *       fill. All rings are treated as filled; interior holes are not
 *       distinguished from outer shells.</li>
 *   <li><b>Polyline</b> — each part is drawn as an open stroked path. No
 *       fill is applied.</li>
 *   <li><b>Point / MultiPoint</b> — each point is drawn as a filled circle.
 *       An optional label from a configured DBF field is drawn beside the
 *       marker.</li>
 * </ul>
 *
 * <h2>Pick cache</h2>
 * <p>During each {@link #render} call this renderer builds a list of
 * {@link PickCache} records — one per feature with visible geometry —
 * that {@link #pick(Point, IContainer)} uses for hit testing without
 * re-projecting coordinates. The cache is rebuilt on every render call so
 * it always reflects the current projection state and zoom level. Calling
 * {@code pick} before the first render returns {@code null}.</p>
 *
 * <h2>Hit-testing strategy by geometry type</h2>
 * <ul>
 *   <li><b>Polygon</b> — {@link GeneralPath#contains(double, double)} on
 *       each cached closed path.</li>
 *   <li><b>Polyline</b> — minimum perpendicular distance from the cursor to
 *       each cached line segment, threshold
 *       {@value #POLYLINE_PICK_TOLERANCE_PX} pixels.</li>
 *   <li><b>Point</b> — Euclidean distance from the cursor to each cached
 *       screen coordinate, threshold
 *       {@value #POINT_PICK_TOLERANCE_PX} pixels.</li>
 * </ul>
 *
 * <h2>Tooltip text</h2>
 * <p>On a hit, {@link #pick} assembles the tooltip from
 * {@link ShapeFeatureStyle#getTooltipFields()}. If that list is empty it
 * falls back to {@link ShapeFeatureStyle#getLabelField()}. Multiple field
 * values are joined with two spaces. Fields absent from the feature are
 * silently omitted. Returns {@code null} if no tooltip configuration is
 * present.</p>
 *
 * <h2>Seam splitting</h2>
 * <p>Features that cross the antimeridian seam of cylindrical projections
 * are split into two path halves. Both halves are cached and tested during
 * picking.</p>
 *
 * <h2>Graphics state</h2>
 * <p>Color, stroke, font, and antialiasing hint are saved before rendering
 * and fully restored on return.</p>
 *
 * <h2>Thread safety</h2>
 * <p>Not thread-safe; all calls must be made on the Event Dispatch Thread.</p>
 */
public class ShapeFeatureRenderer implements IPickable {

    // -------------------------------------------------------------------------
    // Pick tolerance constants
    // -------------------------------------------------------------------------

    /**
     * Maximum distance in screen pixels from the cursor to a polyline segment
     * that counts as a hit.
     */
    private static final double POLYLINE_PICK_TOLERANCE_PX = 4.0;

    /**
     * Maximum distance in screen pixels from the cursor to a point marker
     * center that counts as a hit.
     */
    private static final double POINT_PICK_TOLERANCE_PX = 6.0;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Features to render. */
    private final List<ShapeFeature> features;

    /** Projection used for coordinate transforms and visibility tests. */
    private IMapProjection projection;

    /** Visual style applied to all features in this layer. */
    private ShapeFeatureStyle style;

    /** Font used for optional feature labels. */
    private Font labelFont = Fonts.smallFont;

    /**
     * Pick cache built during the most recent {@link #render} call.
     * Empty until the first render; rebuilt on every subsequent render.
     */
    private final List<PickCache> pickCache = new ArrayList<>();

    /**
     * Whether this layer is currently visible. When {@code false},
     * {@link #render} is a no-op and {@link #pick} returns {@code null}.
     * Controlled by the {@link ShapefileMenu} checkbox for this layer.
     */
    private boolean visible = true;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a renderer for the given features using the supplied projection
     * and style.
     *
     * @param features   the features to render; must not be {@code null}
     * @param projection the map projection; must not be {@code null}
     * @param style      the visual style; must not be {@code null}
     */
    public ShapeFeatureRenderer(List<ShapeFeature> features,
                                IMapProjection projection,
                                ShapeFeatureStyle style) {
        this.features   = Objects.requireNonNull(features,   "features");
        this.projection = Objects.requireNonNull(projection, "projection");
        this.style      = Objects.requireNonNull(style,      "style");
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /**
     * Replaces the current style. Takes effect on the next render call.
     *
     * @param style the new style; must not be {@code null}
     */
    public void setStyle(ShapeFeatureStyle style) {
        this.style = Objects.requireNonNull(style, "style");
    }

    /** Returns the current style; never {@code null}. */
    public ShapeFeatureStyle getStyle() { return style; }

    /**
     * Sets the font used for on-map feature labels.
     *
     * @param font label font; if {@code null} the default small font is used
     */
    public void setLabelFont(Font font) {
        this.labelFont = (font != null) ? font : Fonts.smallFont;
    }

    /**
     * Sets whether this layer is rendered and participates in picking.
     *
     * <p>Hidden layers ({@code visible = false}) skip all rendering and
     * return {@code null} from {@link #pick}. The layer remains in the
     * MapView2D extra-layer list so it can be re-shown instantly
     * without reloading the data.</p>
     *
     * @param visible {@code true} to show this layer; {@code false} to hide it
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (!visible) pickCache.clear(); // stale cache not needed while hidden
    }

    /**
     * Returns whether this layer is currently visible.
     *
     * @return {@code true} if the layer will be rendered and picked
     */
    public boolean isVisible() { return visible; }

    /**
     * Updates the projection used for coordinate transforms and visibility
     * tests. Must be called whenever the owning MapView2D switches
     * projections so that this layer renders in the correct coordinate space.
     *
     * <p>MapView2D setProjection(EProjection) calls this automatically
     * for every layer registered via MapView2D addLayer.</p>
     *
     * @param projection the new projection; must not be {@code null}
     */
    public void setProjection(IMapProjection projection) {
        this.projection = Objects.requireNonNull(projection, "projection");
        pickCache.clear(); // stale screen-space cache is now invalid
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /**
     * Renders all features in this layer and rebuilds the pick cache.
     *
     * <p>The pick cache is cleared at the start of each call and repopulated
     * as features are rendered, so {@link #pick} always reflects the most
     * recent projection state and zoom level.</p>
     *
     * @param g2        graphics context to draw into; must not be {@code null}
     * @param container container providing the world-to-local transform;
     *                  must not be {@code null}
     */
    public void render(Graphics2D g2, IContainer container) {
        Objects.requireNonNull(g2,        "g2");
        Objects.requireNonNull(container, "container");

        if (!visible) return; // layer is hidden

        Component comp = container.getComponent();
        if (comp.getWidth() <= 0 || comp.getHeight() <= 0) return;

        pickCache.clear();

        Object oldAA     = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Color  oldColor  = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        Font   oldFont   = g2.getFont();

        if (style.isAntialias()) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
        }

        g2.setFont(labelFont);
        FontMetrics fm = g2.getFontMetrics();

        for (ShapeFeature feature : features) {
            switch (feature.getShapeType()) {
                case ShapefileGeometryReader.TYPE_POLYGON ->
                        renderPolygon(g2, container, feature);
                case ShapefileGeometryReader.TYPE_POLYLINE ->
                        renderPolyline(g2, container, feature);
                case ShapefileGeometryReader.TYPE_POINT,
                     ShapefileGeometryReader.TYPE_MULTIPOINT ->
                        renderPoints(g2, container, feature, fm);
                default -> { /* unsupported — skip */ }
            }
        }

        g2.setFont(oldFont);
        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
        if (oldAA != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }
    }

    // -------------------------------------------------------------------------
    // IPickable
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code null} if the pick cache is empty (no render has
     * occurred yet) or no feature is within the hit threshold. The tooltip
     * string is assembled from {@link ShapeFeatureStyle#getTooltipFields()},
     * falling back to {@link ShapeFeatureStyle#getLabelField()} if no tooltip
     * fields are configured.</p>
     */
    @Override
    public String pick(Point mouseLocal, IContainer container) {
        Objects.requireNonNull(mouseLocal, "mouseLocal");
        Objects.requireNonNull(container,  "container");

        if (!visible) return null; // layer is hidden

        double mx = mouseLocal.x;
        double my = mouseLocal.y;

        for (PickCache entry : pickCache) {
            boolean hit = switch (entry.feature.getShapeType()) {
                case ShapefileGeometryReader.TYPE_POLYGON    ->
                        hitTestPolygon(entry, mx, my);
                case ShapefileGeometryReader.TYPE_POLYLINE   ->
                        hitTestPolyline(entry, mx, my);
                case ShapefileGeometryReader.TYPE_POINT,
                     ShapefileGeometryReader.TYPE_MULTIPOINT ->
                        hitTestPoints(entry, mx, my);
                default -> false;
            };

            if (hit) return buildTooltip(entry.feature);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Per-geometry-type renderers  (also populate pickCache)
    // -------------------------------------------------------------------------

    /**
     * Renders one polygon feature and caches its projected closed paths.
     *
     * <p>Each ring produces up to two {@link GeneralPath} halves (near and
     * far of the antimeridian seam). Non-empty halves are closed, drawn, and
     * added to the pick cache. Empty halves (rings entirely outside the
     * visible area) are skipped — calling {@code closePath()} on an empty
     * path would throw {@link java.awt.geom.IllegalPathStateException}.</p>
     */
    private void renderPolygon(Graphics2D g2, IContainer container,
                               ShapeFeature feature) {
        Color  fill   = style.getFillColor();
        Color  stroke = style.getStrokeColor();
        if (fill == null && stroke == null) return;

        Stroke strokeObj       = style.buildStroke();
        List<GeneralPath> paths = new ArrayList<>();

        for (List<Point2D.Double> ring : feature.getRings()) {
            GeneralPath near = new GeneralPath(GeneralPath.WIND_NON_ZERO);
            GeneralPath far  = new GeneralPath(GeneralPath.WIND_NON_ZERO);
            GeneralPath cur  = near;
            boolean first    = true;
            double  prevLon  = 0.0;

            for (Point2D.Double lonLat : ring) {
                if (!projection.isPointVisible(lonLat)) continue;

                Point2D.Double xy = new Point2D.Double();
                projection.latLonToXY(lonLat, xy);
                if (!projection.isPointOnMap(xy)) continue;

                if (!first && projection.crossesSeam(lonLat.x, prevLon)) {
                    cur = (cur == near) ? far : near;
                }
                first   = false;
                prevLon = lonLat.x;

                Point screen = new Point();
                container.worldToLocal(screen, xy);

                if (cur.getCurrentPoint() == null) cur.moveTo(screen.x, screen.y);
                else                               cur.lineTo(screen.x, screen.y);
            }

            for (GeneralPath path : new GeneralPath[]{ near, far }) {
                if (path.getCurrentPoint() == null) continue; // empty — skip
                path.closePath();
                paths.add(path);
                if (fill != null) {
                    g2.setColor(fill);
                    g2.fill(path);
                }
                if (stroke != null) {
                    g2.setColor(stroke);
                    g2.setStroke(strokeObj);
                    g2.draw(path);
                }
            }
        }

        if (!paths.isEmpty()) {
            pickCache.add(new PickCache(feature, paths, null));
        }
    }

    /**
     * Renders one polyline feature and caches its projected open paths.
     *
     * <p>Visibility breaks and antimeridian crossings cause the current path
     * to be finalized and a new one started, preventing map-wide streaks.</p>
     */
    private void renderPolyline(Graphics2D g2, IContainer container,
                                ShapeFeature feature) {
        Color stroke = style.getStrokeColor();
        if (stroke == null) return;

        g2.setColor(stroke);
        g2.setStroke(style.buildStroke());

        List<GeneralPath> paths = new ArrayList<>();

        for (List<Point2D.Double> part : feature.getRings()) {
            GeneralPath near = new GeneralPath();
            GeneralPath far  = new GeneralPath();
            GeneralPath cur  = near;
            boolean first    = true;
            double  prevLon  = 0.0;

            for (Point2D.Double lonLat : part) {
                if (!projection.isPointVisible(lonLat)) { first = true; continue; }

                Point2D.Double xy = new Point2D.Double();
                projection.latLonToXY(lonLat, xy);
                if (!projection.isPointOnMap(xy)) { first = true; continue; }

                if (!first && projection.crossesSeam(lonLat.x, prevLon)) {
                    cur = (cur == near) ? far : near;
                    first = true;
                }
                prevLon = lonLat.x;

                Point screen = new Point();
                container.worldToLocal(screen, xy);

                if (first || cur.getCurrentPoint() == null) {
                    cur.moveTo(screen.x, screen.y);
                    first = false;
                } else {
                    cur.lineTo(screen.x, screen.y);
                }
            }

            for (GeneralPath path : new GeneralPath[]{ near, far }) {
                if (path.getCurrentPoint() == null) continue;
                g2.draw(path);
                paths.add(path);
            }
        }

        if (!paths.isEmpty()) {
            pickCache.add(new PickCache(feature, paths, null));
        }
    }

    /**
     * Renders one point or multi-point feature and caches the screen
     * coordinates of each visible point.
     */
    private void renderPoints(Graphics2D g2, IContainer container,
                              ShapeFeature feature, FontMetrics fm) {
        Color  pointColor = style.getPointColor();
        double r          = style.getPointRadius();
        String labelField = style.getLabelField();
        Color  labelColor = style.getLabelColor();

        String labelText = null;
        if (labelField != null) {
            labelText = feature.getProperty(labelField);
            if (labelText != null && labelText.isEmpty()) labelText = null;
        }

        Ellipse2D.Double     marker    = new Ellipse2D.Double();
        List<Point2D.Double> screenPts = new ArrayList<>();

        for (Point2D.Double lonLat : feature.getPoints()) {
            if (!projection.isPointVisible(lonLat)) continue;

            Point2D.Double xy = new Point2D.Double();
            projection.latLonToXY(lonLat, xy);
            if (!projection.isPointOnMap(xy)) continue;

            Point screen = new Point();
            container.worldToLocal(screen, xy);

            double cx = screen.x, cy = screen.y;
            screenPts.add(new Point2D.Double(cx, cy));

            marker.setFrame(cx - r, cy - r, 2 * r, 2 * r);
            g2.setColor(pointColor);
            g2.fill(marker);

            if (labelText != null) {
                g2.setColor(labelColor);
                int tx = (int) Math.round(cx + r + 2);
                int ty = (int) Math.round(cy + fm.getAscent() / 2.0);
                if (ty >= fm.getAscent()) g2.drawString(labelText, tx, ty);
            }
        }

        if (!screenPts.isEmpty()) {
            pickCache.add(new PickCache(feature, null, screenPts));
        }
    }

    // -------------------------------------------------------------------------
    // Hit-testing helpers
    // -------------------------------------------------------------------------

    /** Polygon hit: point-in-polygon on each cached closed path. */
    private static boolean hitTestPolygon(PickCache entry, double mx, double my) {
        for (GeneralPath path : entry.paths) {
            if (path.contains(mx, my)) return true;
        }
        return false;
    }

    /**
     * Polyline hit: walks each cached path via {@link PathIterator} and tests
     * the perpendicular distance from the cursor to each line segment.
     */
    private static boolean hitTestPolyline(PickCache entry, double mx, double my) {
        double tolSq  = POLYLINE_PICK_TOLERANCE_PX * POLYLINE_PICK_TOLERANCE_PX;
        float[] coords = new float[6];

        for (GeneralPath path : entry.paths) {
            PathIterator it = path.getPathIterator(null);
            double prevX = 0, prevY = 0;
            while (!it.isDone()) {
                int type = it.currentSegment(coords);
                double x = coords[0], y = coords[1];
                if (type == PathIterator.SEG_LINETO) {
                    if (segDistSq(mx, my, prevX, prevY, x, y) <= tolSq) return true;
                }
                prevX = x;
                prevY = y;
                it.next();
            }
        }
        return false;
    }

    /** Point hit: Euclidean distance from cursor to each cached screen point. */
    private static boolean hitTestPoints(PickCache entry, double mx, double my) {
        double tolSq = POINT_PICK_TOLERANCE_PX * POINT_PICK_TOLERANCE_PX;
        for (Point2D.Double pt : entry.screenPoints) {
            double dx = mx - pt.x, dy = my - pt.y;
            if (dx * dx + dy * dy <= tolSq) return true;
        }
        return false;
    }

    /**
     * Returns the squared perpendicular distance from point {@code (px,py)}
     * to the segment from {@code (ax,ay)} to {@code (bx,by)}, clamped so
     * that the nearest point on the segment is used (not the infinite line).
     */
    private static double segDistSq(double px, double py,
                                    double ax, double ay,
                                    double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double lenSq = dx * dx + dy * dy;
        if (lenSq == 0.0) {
            double ex = px - ax, ey = py - ay;
            return ex * ex + ey * ey;
        }
        double t  = Math.max(0.0, Math.min(1.0,
                    ((px - ax) * dx + (py - ay) * dy) / lenSq));
        double cx = ax + t * dx - px, cy = ay + t * dy - py;
        return cx * cx + cy * cy;
    }

    // -------------------------------------------------------------------------
    // Tooltip assembly
    // -------------------------------------------------------------------------

    /**
     * Assembles the tooltip string for a hit feature.
     *
     * <p>Uses {@link ShapeFeatureStyle#getTooltipFields()} when set, falling
     * back to {@link ShapeFeatureStyle#getLabelField()}. Multiple values are
     * joined with two spaces. Returns {@code null} when no useful text can be
     * produced.</p>
     */
    private String buildTooltip(ShapeFeature feature) {
        List<String> fields = style.getTooltipFields();

        if (fields.isEmpty()) {
            // Fall back to labelField.
            String lf = style.getLabelField();
            if (lf == null) return null;
            String val = feature.getProperty(lf);
            return (val != null && !val.isEmpty()) ? val : null;
        }

        StringBuilder sb = new StringBuilder();
        for (String fieldName : fields) {
            String val = feature.getProperty(fieldName);
            if (val == null || val.isEmpty()) continue;
            if (sb.length() > 0) sb.append("  ");
            sb.append(val);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    // -------------------------------------------------------------------------
    // Pick cache record
    // -------------------------------------------------------------------------

    /**
     * Associates one {@link ShapeFeature} with its projected screen-space
     * geometry from the most recent render call.
     *
     * <p>For polygon and polyline features, {@code paths} is non-null and
     * {@code screenPoints} is null. For point features the reverse is true.</p>
     */
    private static final class PickCache {
        final ShapeFeature         feature;
        final List<GeneralPath>    paths;        // polygon / polyline
        final List<Point2D.Double> screenPoints; // point / multipoint

        PickCache(ShapeFeature feature,
                  List<GeneralPath> paths,
                  List<Point2D.Double> screenPoints) {
            this.feature      = feature;
            this.paths        = paths;
            this.screenPoints = screenPoints;
        }
    }
}