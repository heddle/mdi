package edu.cnu.mdi.mapping.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.LineStyle;
import edu.cnu.mdi.mapping.MapView2D;
import edu.cnu.mdi.mapping.container.MapContainer;
import edu.cnu.mdi.mapping.projection.IMapProjection;

/**
 * Static utility methods for drawing and geometry operations on geographic
 * data in the MDI mapping subsystem.
 *
 * <h2>Purpose</h2>
 * <p>
 * This class is the geographic analogue of
 * {@code edu.cnu.mdi.graphics.world.WorldGraphicsUtils}. Whereas the world
 * graphics utilities assume the input vertices are already expressed in the
 * container's world coordinate system, {@code MapGraphics} accepts vertices in
 * geographic coordinates and handles the additional steps needed for map-aware
 * rendering:
 * </p>
 * <ol>
 *   <li>Great-circle interpolation between consecutive geographic vertices.</li>
 *   <li>Forward projection from geographic coordinates (longitude, latitude)
 *       to projection-space (x, y) coordinates via {@link IMapProjection}.</li>
 *   <li>Seam-aware splitting of projected paths so that segments crossing a
 *       longitude wrap boundary are not drawn as spurious streaks across the
 *       full map.</li>
 *   <li>Conversion from projection-space coordinates to device pixels through
 *       the owning {@link MapContainer}.</li>
 * </ol>
 *
 * <h2>Coordinate conventions</h2>
 * <ul>
 *   <li>All geographic coordinates are in <em>radians</em>.</li>
 *   <li>A geographic point is represented by a {@link Point2D.Double} with
 *       {@code x = λ} (longitude) and {@code y = φ} (latitude).</li>
 *   <li>Projected coordinates are ordinary map projection coordinates in the
 *       {@link IMapProjection} XY plane, <em>not</em> device pixels.</li>
 *   <li>Device pixels are produced only at the final rendering stage via
 *       {@link MapContainer#worldToLocal(java.awt.Point, java.awt.geom.Point2D.Double)}.</li>
 * </ul>
 *
 * <h2>Seam handling</h2>
 * <p>
 * Global projections with longitude wrapping, such as Mercator and Mollweide,
 * may split a single geographic polyline or polygon into multiple projected
 * subpaths. The decision is delegated to
 * {@link IMapProjection#crossesSeam(double, double)}. When a seam crossing is
 * detected, the current subpath is terminated and a new subpath is started.
 * </p>
 *
 * <p>
 * This class does not attempt to compute the exact seam intersection point.
 * Instead it relies on sufficiently fine great-circle sampling to place the
 * break very near the seam. That strategy keeps the implementation projection-
 * independent and works well in practice for map drawing and annotation.
 * </p>
 *
 * <h2>Typical use</h2>
 * <pre>{@code
 * Point2D.Double[] latLon = ...; // radians, x=lon, y=lat
 * MapGraphics.drawMapPolygon(g2, mapContainer, latLon,
 *         new Color(255, 200, 0, 80), Color.ORANGE, 1.5f, LineStyle.SOLID);
 * }</pre>
 *
 * <h2>Intended scope</h2>
 * <p>
 * This class is designed as the low-level rendering and geometry engine for
 * future map-native items such as {@code MapPolylineItem} and
 * {@code MapPolygonItem}. Those items can store their vertices in geographic
 * coordinates and delegate projection, seam handling, bounds, and hit testing
 * to this utility.
 * </p>
 */
public final class MapGraphics {

    /**
     * Default maximum angular step, in degrees, used when approximating a
     * great-circle segment by sampled intermediate points.
     *
     * <p>
     * Smaller values produce smoother curves and more accurate seam placement
     * at the cost of more projected vertices.
     * </p>
     */
    public static final double DEFAULT_MAX_GREAT_CIRCLE_STEP_DEG = 2.0;

    /**
     * Minimum allowed line width in device pixels.
     *
     * <p>
     * This prevents zero-width lines from disappearing completely.
     * </p>
     */
    public static final float MIN_LINE_WIDTH = 0.5f;

    /**
     * Small threshold used in floating-point comparisons.
     */
    private static final double TINY = 1.0e-12;

    /**
     * Hidden constructor for a static utility class.
     */
    private MapGraphics() {
    }

    // -------------------------------------------------------------------------
    // Public drawing API
    // -------------------------------------------------------------------------

    /**
     * Draws a single great-circle map segment between two geographic endpoints.
     *
     * @param g2        the graphics context
     * @param container the map container that owns the projection and supplies
     *                  the world-to-device transform
     * @param ll0       first endpoint in radians ({@code x=λ, y=φ})
     * @param ll1       second endpoint in radians ({@code x=λ, y=φ})
     * @param lineColor the line color; {@code null} suppresses drawing
     * @param lineWidth the line width in pixels
     * @param lineStyle the line dash style; if {@code null},
     *                  {@link LineStyle#SOLID} is used
     */
    public static void drawMapLine(Graphics2D g2, MapContainer container,
            Point2D.Double ll0, Point2D.Double ll1,
            Color lineColor, float lineWidth, LineStyle lineStyle) {

        if (ll0 == null || ll1 == null) {
            return;
        }

        drawMapPolyline(g2, container,
                new Point2D.Double[] { ll0, ll1 },
                lineColor, lineWidth, lineStyle);
    }

    /**
     * Draws a great-circle map polyline from a geographic vertex array.
     *
     * @param g2           the graphics context
     * @param container    the owning map container
     * @param latLonPoints geographic vertices in radians; at least two points
     *                     are required
     * @param lineColor    the line color; {@code null} suppresses drawing
     * @param lineWidth    the line width in pixels
     * @param lineStyle    the line dash style; if {@code null},
     *                     {@link LineStyle#SOLID} is used
     */
    public static void drawMapPolyline(Graphics2D g2, MapContainer container,
            Point2D.Double[] latLonPoints,
            Color lineColor, float lineWidth, LineStyle lineStyle) {

        if (lineColor == null) {
            return;
        }

        ProjectedMapShape shape = buildProjectedPolyline(container, latLonPoints,
                DEFAULT_MAX_GREAT_CIRCLE_STEP_DEG);
        drawProjectedShape(g2, shape, null, lineColor, lineWidth, lineStyle);
    }

    /**
     * Draws a great-circle map polygon from a geographic vertex array.
     *
     * <p>
     * The polygon is automatically closed by connecting the last vertex back to
     * the first using a great-circle segment.
     * </p>
     *
     * @param g2           the graphics context
     * @param container    the owning map container
     * @param latLonPoints geographic polygon vertices in radians; at least
     *                     three points are required
     * @param fillColor    the fill color; {@code null} suppresses fill
     * @param lineColor    the outline color; {@code null} suppresses outline
     * @param lineWidth    the outline width in pixels
     * @param lineStyle    the outline dash style; if {@code null},
     *                     {@link LineStyle#SOLID} is used
     */
    public static void drawMapPolygon(Graphics2D g2, MapContainer container,
            Point2D.Double[] latLonPoints,
            Color fillColor, Color lineColor,
            float lineWidth, LineStyle lineStyle) {

        ProjectedMapShape shape = buildProjectedPolygon(container, latLonPoints,
                DEFAULT_MAX_GREAT_CIRCLE_STEP_DEG);
        drawProjectedShape(g2, shape, fillColor, lineColor, lineWidth, lineStyle);
    }

    /**
     * Draws a great-circle map polyline using style parameters from an
     * {@link IStyled} source.
     *
     * @param g2           the graphics context
     * @param container    the owning map container
     * @param latLonPoints geographic vertices in radians
     * @param style        the style source; must not be {@code null}
     */
    public static void drawMapPolyline(Graphics2D g2, MapContainer container,
            Point2D.Double[] latLonPoints, IStyled style) {

        if (style == null) {
            return;
        }

        drawMapPolyline(g2, container, latLonPoints,
                style.getLineColor(), style.getLineWidth(), style.getLineStyle());
    }

    /**
     * Draws a great-circle map polygon using style parameters from an
     * {@link IStyled} source.
     *
     * @param g2           the graphics context
     * @param container    the owning map container
     * @param latLonPoints geographic polygon vertices in radians
     * @param style        the style source; must not be {@code null}
     */
    public static void drawMapPolygon(Graphics2D g2, MapContainer container,
            Point2D.Double[] latLonPoints, IStyled style) {

        if (style == null) {
            return;
        }

        drawMapPolygon(g2, container, latLonPoints,
                style.getFillColor(), style.getLineColor(),
                style.getLineWidth(), style.getLineStyle());
    }

    // -------------------------------------------------------------------------
    // Public path-building API
    // -------------------------------------------------------------------------

    /**
     * Builds a seam-aware projected polyline from geographic vertices using the
     * default great-circle sampling step.
     *
     * @param container    the owning map container
     * @param latLonPoints geographic vertices in radians
     * @return a seam-split projected shape; never {@code null}
     */
    public static ProjectedMapShape buildProjectedPolyline(
            MapContainer container, Point2D.Double[] latLonPoints) {
        return buildProjectedPolyline(container, latLonPoints,
                DEFAULT_MAX_GREAT_CIRCLE_STEP_DEG);
    }

    /**
     * Builds a seam-aware projected polygon from geographic vertices using the
     * default great-circle sampling step.
     *
     * @param container    the owning map container
     * @param latLonPoints geographic polygon vertices in radians
     * @return a seam-split projected shape; never {@code null}
     */
    public static ProjectedMapShape buildProjectedPolygon(
            MapContainer container, Point2D.Double[] latLonPoints) {
        return buildProjectedPolygon(container, latLonPoints,
                DEFAULT_MAX_GREAT_CIRCLE_STEP_DEG);
    }

    /**
     * Builds a seam-aware projected polyline from geographic vertices.
     *
     * @param container    the owning map container
     * @param latLonPoints geographic vertices in radians
     * @param maxStepDeg   maximum angular sampling step in degrees along each
     *                     great-circle segment; values less than or equal to
     *                     zero are replaced by
     *                     {@link #DEFAULT_MAX_GREAT_CIRCLE_STEP_DEG}
     * @return a seam-split projected shape; never {@code null}
     */
    public static ProjectedMapShape buildProjectedPolyline(
            MapContainer container, Point2D.Double[] latLonPoints,
            double maxStepDeg) {
        return buildProjectedShape(container, latLonPoints, false, maxStepDeg);
    }

    /**
     * Builds a seam-aware projected polygon from geographic vertices.
     *
     * @param container    the owning map container
     * @param latLonPoints geographic polygon vertices in radians
     * @param maxStepDeg   maximum angular sampling step in degrees along each
     *                     great-circle segment; values less than or equal to
     *                     zero are replaced by
     *                     {@link #DEFAULT_MAX_GREAT_CIRCLE_STEP_DEG}
     * @return a seam-split projected shape; never {@code null}
     */
    public static ProjectedMapShape buildProjectedPolygon(
            MapContainer container, Point2D.Double[] latLonPoints,
            double maxStepDeg) {
        return buildProjectedShape(container, latLonPoints, true, maxStepDeg);
    }

    // -------------------------------------------------------------------------
    // Public geometry helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the pixel bounds of a projected map shape.
     *
     * @param shape the projected map shape; may be {@code null}
     * @return the union of all path bounds, or an empty rectangle if the shape
     *         is {@code null} or empty
     */
    public static Rectangle getBounds(ProjectedMapShape shape) {
        return (shape == null) ? new Rectangle() : shape.getBounds();
    }

    /**
     * Tests whether a device-space point lies inside any closed projected
     * subpath of a map shape.
     *
     * <p>
     * This method is intended for filled polygons, not open polylines.
     * </p>
     *
     * @param shape the projected map shape
     * @param p     the test point in device coordinates
     * @return {@code true} if any closed subpath contains the point;
     *         {@code false} otherwise
     */
    public static boolean contains(ProjectedMapShape shape, Point p) {
        if (shape == null || p == null || !shape.isClosed()) {
            return false;
        }

        for (Path2D.Double path : shape.getPaths()) {
            if (path.contains(p)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether a device-space point lies within a specified tolerance of
     * any projected path segment.
     *
     * <p>
     * This method is useful for hit testing open polylines and polygon
     * outlines.
     * </p>
     *
     * @param shape  the projected map shape
     * @param p      the test point in device coordinates
     * @param tolPix the tolerance in pixels; values less than zero are treated
     *               as zero
     * @return {@code true} if the point is within {@code tolPix} of any
     *         segment; {@code false} otherwise
     */
    public static boolean isPointNear(ProjectedMapShape shape, Point p,
            double tolPix) {

        if (shape == null || p == null) {
            return false;
        }

        double tol2 = Math.max(0.0, tolPix) * Math.max(0.0, tolPix);

        for (Path2D.Double path : shape.getPaths()) {
            if (isPointNearPath(path, p, tol2)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Normalizes a geographic point by wrapping its longitude to the
     * projection's canonical range and clamping latitude to [-π/2, π/2].
     *
     * @param projection the active map projection; must not be {@code null}
     * @param latLon     the geographic point in radians; must not be
     *                   {@code null}
     * @return a normalized copy of the input point
     */
    public static Point2D.Double normalizeLatLon(IMapProjection projection,
            Point2D.Double latLon) {

        if (projection == null) {
            throw new IllegalArgumentException("projection must not be null");
        }
        if (latLon == null) {
            throw new IllegalArgumentException("latLon must not be null");
        }

        double lon = projection.wrapLongitude(latLon.x);
        double lat = Math.max(-0.5 * Math.PI,
                              Math.min(0.5 * Math.PI, latLon.y));
        return new Point2D.Double(lon, lat);
    }

    // -------------------------------------------------------------------------
    // Private rendering helpers
    // -------------------------------------------------------------------------

    /**
     * Draws a projected map shape using fill and outline parameters.
     *
     * @param g2        the graphics context
     * @param shape     the projected shape
     * @param fillColor fill color; {@code null} suppresses fill
     * @param lineColor outline color; {@code null} suppresses outline
     * @param lineWidth outline width in pixels
     * @param lineStyle outline dash style; if {@code null},
     *                  {@link LineStyle#SOLID} is used
     */
    private static void drawProjectedShape(Graphics2D g2,
            ProjectedMapShape shape,
            Color fillColor, Color lineColor,
            float lineWidth, LineStyle lineStyle) {

        if (g2 == null || shape == null || shape.isEmpty()) {
            return;
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        if (fillColor != null && shape.isClosed()) {
            g2.setColor(fillColor);
            for (Path2D.Double path : shape.getPaths()) {
                g2.fill(path);
            }
        }

        if (lineColor != null) {
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(GraphicsUtils.getStroke(
                    Math.max(MIN_LINE_WIDTH, lineWidth),
                    (lineStyle == null) ? LineStyle.SOLID : lineStyle));
            g2.setColor(lineColor);
            for (Path2D.Double path : shape.getPaths()) {
                g2.draw(path);
            }
            g2.setStroke(oldStroke);
        }
    }

    // -------------------------------------------------------------------------
    // Private projected-shape builder
    // -------------------------------------------------------------------------

    /**
     * Core implementation shared by polyline and polygon builders.
     *
     * @param container    the owning map container
     * @param latLonPoints geographic input vertices in radians
     * @param closed       whether the last vertex should connect back to the
     *                     first
     * @param maxStepDeg   maximum great-circle sampling step in degrees
     * @return a seam-aware projected shape; never {@code null}
     */
    private static ProjectedMapShape buildProjectedShape(MapContainer container,
            Point2D.Double[] latLonPoints, boolean closed, double maxStepDeg) {

        if (container == null) {
            throw new IllegalArgumentException("container must not be null");
        }

        if (latLonPoints == null || latLonPoints.length < (closed ? 3 : 2)) {
            return new ProjectedMapShape(Collections.emptyList(), closed);
        }

        IMapProjection projection = getProjection(container);
        double stepDeg = (maxStepDeg > 0.0)
                ? maxStepDeg : DEFAULT_MAX_GREAT_CIRCLE_STEP_DEG;

        int n = latLonPoints.length;
        int segmentCount = closed ? n : (n - 1);

        ArrayList<Path2D.Double> paths = new ArrayList<>();
        Path2D.Double currentPath = null;

        Point2D.Double prevSample = null;
        boolean prevVisible = false;
        Point prevPixel = null;

        for (int i = 0; i < segmentCount; i++) {

            Point2D.Double ll0 = normalizeLatLon(projection, latLonPoints[i]);
            Point2D.Double ll1 = normalizeLatLon(projection,
                    latLonPoints[(i + 1) % n]);

            List<Point2D.Double> samples = sampleGreatCircle(ll0, ll1, stepDeg);

            for (int j = 0; j < samples.size(); j++) {
                Point2D.Double ll = normalizeLatLon(projection, samples.get(j));

                boolean seamBreak = false;
                if (prevSample != null && projection.crossesSeam(prevSample.x, ll.x)) {
                    seamBreak = true;
                }

                Point2D.Double xy = projectLatLon(projection, ll);
                boolean visible = isProjectedPointDrawable(projection, ll, xy);

                if (seamBreak) {
                    currentPath = null;
                    prevPixel = null;
                }

                if (!visible) {
                    prevSample = ll;
                    prevVisible = false;
                    prevPixel = null;
                    currentPath = null;
                    continue;
                }

                Point pixel = projectedToPixel(container, xy);

                if (!prevVisible || currentPath == null) {
                    currentPath = new Path2D.Double();
                    currentPath.moveTo(pixel.x, pixel.y);
                    paths.add(currentPath);
                } else if (!samePixel(prevPixel, pixel)) {
                    currentPath.lineTo(pixel.x, pixel.y);
                }

                prevSample = ll;
                prevVisible = true;
                prevPixel = pixel;
            }

            /*
             * Reset continuity between source segments. This avoids a false
             * connection between the end of one sampled segment and the start
             * of the next when they happen to begin after a seam break or an
             * invisible region.
             */
            prevSample = null;
            prevVisible = false;
            prevPixel = null;
            currentPath = null;
        }

        if (closed) {
            for (Path2D.Double path : paths) {
                path.closePath();
            }
        }

        return new ProjectedMapShape(paths, closed);
    }

    /**
     * Obtains the active map projection from the container's owning
     * {@link MapView2D}.
     *
     * @param container the map container
     * @return the active map projection
     * @throws IllegalStateException if the container is not attached to a
     *                               {@link MapView2D}
     */
    private static IMapProjection getProjection(MapContainer container) {
        if (!(container.getView() instanceof MapView2D mapView)) {
            throw new IllegalStateException(
                    "MapContainer is not attached to a MapView2D");
        }
        return mapView.getProjection();
    }

    /**
     * Forward-projects a geographic point using the active projection.
     *
     * @param projection the map projection
     * @param latLon     geographic point in radians
     * @return the projected point in projection-space coordinates
     */
    private static Point2D.Double projectLatLon(IMapProjection projection,
            Point2D.Double latLon) {

        Point2D.Double xy = new Point2D.Double();
        projection.latLonToXY(latLon, xy);
        return xy;
    }

    /**
     * Tests whether a projected point is drawable.
     *
     * <p>
     * A point is considered drawable only if:
     * </p>
     * <ol>
     *   <li>the geographic point is visible in the projection,</li>
     *   <li>the forward projection produced finite coordinates, and</li>
     *   <li>the projected point lies on the projection domain.</li>
     * </ol>
     *
     * @param projection the active map projection
     * @param latLon     the source geographic point
     * @param xy         the projected point
     * @return {@code true} if the point can be drawn; {@code false} otherwise
     */
    private static boolean isProjectedPointDrawable(IMapProjection projection,
            Point2D.Double latLon, Point2D.Double xy) {

        if (!projection.isPointVisible(latLon)) {
            return false;
        }
        if (!isFinite(xy)) {
            return false;
        }
        return projection.isPointOnMap(xy);
    }

    /**
     * Converts a projection-space point into a device-space pixel.
     *
     * @param container the owning map container
     * @param xy        the projected point in map XY space
     * @return the corresponding device-space point
     */
    private static Point projectedToPixel(MapContainer container,
            Point2D.Double xy) {

        Point pixel = new Point();
        container.worldToLocal(pixel, xy);
        return pixel;
    }

    /**
     * Tests whether a projected point contains only finite coordinates.
     *
     * @param p the point to test
     * @return {@code true} if both coordinates are finite
     */
    private static boolean isFinite(Point2D.Double p) {
        return Double.isFinite(p.x) && Double.isFinite(p.y);
    }

    /**
     * Tests whether two pixel coordinates are identical.
     *
     * @param p1 the first point
     * @param p2 the second point
     * @return {@code true} if both points are non-null and have identical
     *         integer coordinates
     */
    private static boolean samePixel(Point p1, Point p2) {
        return p1 != null && p2 != null && p1.x == p2.x && p1.y == p2.y;
    }

    // -------------------------------------------------------------------------
    // Great-circle sampling
    // -------------------------------------------------------------------------

    /**
     * Samples the shorter great-circle arc between two geographic endpoints.
     *
     * <p>
     * The result includes both endpoints. Sampling is performed by spherical
     * linear interpolation (slerp) on the unit sphere, then converting each
     * interpolated Cartesian point back to longitude/latitude.
     * </p>
     *
     * <p>
     * If the endpoints are nearly identical, a two-point list containing the
     * normalized endpoints is returned. If the endpoints are nearly antipodal,
     * the interpolation still proceeds, but note that the great circle is then
     * not unique. In that rare case the result depends on floating-point
     * roundoff in the two endpoint vectors.
     * </p>
     *
     * @param ll0        first endpoint in radians
     * @param ll1        second endpoint in radians
     * @param maxStepDeg maximum angular step in degrees
     * @return sampled great-circle points including both endpoints
     */
    private static List<Point2D.Double> sampleGreatCircle(
            Point2D.Double ll0, Point2D.Double ll1, double maxStepDeg) {

        ArrayList<Point2D.Double> out = new ArrayList<>();

        Vec3 a = Vec3.fromLatLon(ll0);
        Vec3 b = Vec3.fromLatLon(ll1);

        double dot = clamp(a.dot(b), -1.0, 1.0);
        double omega = Math.acos(dot);

        if (omega < TINY) {
            out.add(new Point2D.Double(ll0.x, ll0.y));
            out.add(new Point2D.Double(ll1.x, ll1.y));
            return out;
        }

        double step = (maxStepDeg > 0.0) ? maxStepDeg
                                         : DEFAULT_MAX_GREAT_CIRCLE_STEP_DEG;
        int nstep = Math.max(1,
                (int) Math.ceil(Math.toDegrees(omega) / step));

        double sinOmega = Math.sin(omega);

        for (int i = 0; i <= nstep; i++) {
            double t = (double) i / nstep;

            double w0;
            double w1;

            if (Math.abs(sinOmega) < TINY) {
                /*
                 * Fallback for extremely small or nearly antipodal cases. A
                 * linear blend followed by renormalization is less exact than
                 * slerp but numerically stable.
                 */
                w0 = 1.0 - t;
                w1 = t;
            } else {
                w0 = Math.sin((1.0 - t) * omega) / sinOmega;
                w1 = Math.sin(t * omega) / sinOmega;
            }

            Vec3 p = a.scale(w0).add(b.scale(w1)).normalize();
            out.add(p.toLatLon());
        }

        return out;
    }

    /**
     * Clamps a value to a closed interval.
     *
     * @param x  the input value
     * @param lo lower bound
     * @param hi upper bound
     * @return {@code x} clamped to [{@code lo}, {@code hi}]
     */
    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    // -------------------------------------------------------------------------
    // Path hit-testing helpers
    // -------------------------------------------------------------------------

    /**
     * Tests whether a point lies within a squared tolerance of any segment of a
     * device-space path.
     *
     * @param path the device-space path
     * @param p    the test point
     * @param tol2 the squared tolerance in pixels squared
     * @return {@code true} if the point lies within tolerance of any path
     *         segment; {@code false} otherwise
     */
    private static boolean isPointNearPath(Path2D.Double path, Point p,
            double tol2) {

        if (path == null || p == null) {
            return false;
        }

        double[] c = new double[6];
        PathIterator pi = path.getPathIterator(null);

        Point2D.Double last = null;
        Point2D.Double subpathStart = null;

        Point2D.Double test = new Point2D.Double(p.x, p.y);

        while (!pi.isDone()) {
            switch (pi.currentSegment(c)) {
                case PathIterator.SEG_MOVETO -> {
                    last = new Point2D.Double(c[0], c[1]);
                    subpathStart = last;
                    if (last.distanceSq(test) <= tol2) {
                        return true;
                    }
                }

                case PathIterator.SEG_LINETO -> {
                    Point2D.Double cur = new Point2D.Double(c[0], c[1]);
                    if (last != null) {
                        if (distanceSqPointToSegment(last, cur, test) <= tol2) {
                            return true;
                        }
                    } else if (cur.distanceSq(test) <= tol2) {
                        return true;
                    }
                    last = cur;
                }

                case PathIterator.SEG_CLOSE -> {
                    if (last != null && subpathStart != null) {
                        if (distanceSqPointToSegment(last, subpathStart, test) <= tol2) {
                            return true;
                        }
                        last = subpathStart;
                    }
                }

                default -> throw new IllegalArgumentException(
                        "Path contains unsupported curve segments");
            }

            pi.next();
        }

        return false;
    }

    /**
     * Returns the squared distance from a point to a line segment in
     * device-space.
     *
     * @param a segment start
     * @param b segment end
     * @param p test point
     * @return squared distance from {@code p} to the nearest point on segment
     *         {@code a→b}
     */
    private static double distanceSqPointToSegment(Point2D.Double a,
            Point2D.Double b, Point2D.Double p) {

        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double len2 = dx * dx + dy * dy;

        if (len2 < TINY) {
            return a.distanceSq(p);
        }

        double t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / len2;
        if (t <= 0.0) {
            return a.distanceSq(p);
        }
        if (t >= 1.0) {
            return b.distanceSq(p);
        }

        double cx = a.x + t * dx;
        double cy = a.y + t * dy;
        double ex = p.x - cx;
        double ey = p.y - cy;
        return ex * ex + ey * ey;
    }

    // -------------------------------------------------------------------------
    // Inner helper classes
    // -------------------------------------------------------------------------

    /**
     * Immutable representation of a projected map shape that may consist of
     * multiple subpaths.
     *
     * <p>
     * Multiple subpaths arise naturally when a geographic polyline or polygon
     * crosses the seam of a wrapped map projection. Each path is already
     * expressed in device coordinates and is therefore ready for drawing and
     * hit testing.
     * </p>
     */
    public static final class ProjectedMapShape {

        /**
         * The projected subpaths in device coordinates.
         */
        private final List<Path2D.Double> paths;

        /**
         * Whether the original geographic input represented a closed polygon.
         */
        private final boolean closed;

        /**
         * Creates a new projected map shape.
         *
         * @param paths  the projected subpaths; a defensive copy is made
         * @param closed whether the originating shape was closed
         */
        public ProjectedMapShape(List<Path2D.Double> paths, boolean closed) {
            this.paths = (paths == null)
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(paths));
            this.closed = closed;
        }

        /**
         * Returns the projected subpaths in device coordinates.
         *
         * @return an unmodifiable list of subpaths
         */
        public List<Path2D.Double> getPaths() {
            return paths;
        }

        /**
         * Returns whether the original geographic shape was closed.
         *
         * @return {@code true} for polygons, {@code false} for polylines
         */
        public boolean isClosed() {
            return closed;
        }

        /**
         * Returns whether this projected shape has no subpaths.
         *
         * @return {@code true} if empty; {@code false} otherwise
         */
        public boolean isEmpty() {
            return paths.isEmpty();
        }

        /**
         * Computes the union of the bounds of all projected subpaths.
         *
         * @return the bounding rectangle in device coordinates
         */
        public Rectangle getBounds() {
            Rectangle r = null;
            for (Path2D.Double path : paths) {
                Rectangle pr = path.getBounds();
                if (r == null) {
                    r = new Rectangle(pr);
                } else {
                    r.add(pr);
                }
            }
            return (r == null) ? new Rectangle() : r;
        }
    }
    
    /**
     * Computes the central-angle length of the shorter great-circle arc between
     * two geographic points on the unit sphere.
     *
     * <p>
     * The input points use the standard MDI geographic convention:
     * {@code x = longitude} and {@code y = latitude}, both in radians.
     * The returned value is also in radians. On a sphere of radius {@code R},
     * the physical arc length is therefore {@code R * greatCircleLength(...)}.
     * </p>
     *
     * <p>
     * The computation uses the spherical law of cosines in a numerically safe
     * form by clamping the cosine argument to {@code [-1, 1]} before calling
     * {@link Math#acos(double)}.
     * </p>
     *
     * @param startLatLon the start geographic point in radians
     *                    ({@code x = longitude, y = latitude}); must not be
     *                    {@code null}
     * @param endLatLon   the end geographic point in radians
     *                    ({@code x = longitude, y = latitude}); must not be
     *                    {@code null}
     * @return the shorter great-circle arc length in radians on the unit sphere,
     *         in the range {@code [0, π]}
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public static double greatCircleLength(Point2D.Double startLatLon,
            Point2D.Double endLatLon) {

        if (startLatLon == null) {
            throw new IllegalArgumentException("startLatLon must not be null");
        }
        if (endLatLon == null) {
            throw new IllegalArgumentException("endLatLon must not be null");
        }

        double lon1 = startLatLon.x;
        double lat1 = startLatLon.y;
        double lon2 = endLatLon.x;
        double lat2 = endLatLon.y;

        double sin1 = Math.sin(lat1);
        double sin2 = Math.sin(lat2);
        double cos1 = Math.cos(lat1);
        double cos2 = Math.cos(lat2);
        double dLon = lon2 - lon1;

        double cosOmega = sin1 * sin2 + cos1 * cos2 * Math.cos(dLon);
        cosOmega = clamp(cosOmega, -1.0, 1.0);

        return Math.acos(cosOmega);
    }
    
    /**
     * Computes the initial great-circle azimuth (bearing) from a start
     * geographic point to an end geographic point on the unit sphere.
     *
     * <p>
     * The input points use the standard MDI geographic convention:
     * {@code x = longitude} and {@code y = latitude}, both in radians.
     * The returned azimuth is also in radians and uses the navigation
     * convention:
     * </p>
     * <ul>
     *   <li>{@code 0} = north</li>
     *   <li>{@code π/2} = east</li>
     *   <li>{@code π} = south</li>
     *   <li>{@code 3π/2} = west</li>
     * </ul>
     *
     * <p>
     * The value is normalized to the half-open range {@code [0, 2π)}.
     * This is the <em>initial</em> azimuth at the start point. Along a
     * great-circle path the local heading generally changes continuously,
     * so the azimuth at the end point will usually be different.
     * </p>
     *
     * <p>
     * If the two points are coincident, the azimuth is undefined. In that
     * degenerate case this method returns {@code Double.NaN}.
     * </p>
     *
     * @param startLatLon the start geographic point in radians
     *                    ({@code x = longitude, y = latitude}); must not be
     *                    {@code null}
     * @param endLatLon   the end geographic point in radians
     *                    ({@code x = longitude, y = latitude}); must not be
     *                    {@code null}
     * @return the initial great-circle azimuth in radians, normalized to
     *         {@code [0, 2π)}, or {@code Double.NaN} if the two points are
     *         coincident
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public static double greatCircleAzimuth(Point2D.Double startLatLon,
            Point2D.Double endLatLon) {

        if (startLatLon == null) {
            throw new IllegalArgumentException("startLatLon must not be null");
        }
        if (endLatLon == null) {
            throw new IllegalArgumentException("endLatLon must not be null");
        }

        double lon1 = startLatLon.x;
        double lat1 = startLatLon.y;
        double lon2 = endLatLon.x;
        double lat2 = endLatLon.y;

        double dLon = lon2 - lon1;

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        if (Math.abs(x) < TINY && Math.abs(y) < TINY) {
            return Double.NaN;
        }

        double azimuth = Math.atan2(y, x);
        if (azimuth < 0.0) {
            azimuth += 2.0 * Math.PI;
        }

        return azimuth;
    }

    /**
     * Computes the endpoint of a great-circle path on the unit sphere from
     * a start geographic point, an initial azimuth, and a normalized arc
     * length.
     *
     * <p>
     * The input start point uses the standard MDI geographic convention:
     * {@code x = longitude} and {@code y = latitude}, both in radians.
     * The azimuth is also in radians and uses the navigation convention:
     * </p>
     * <ul>
     *   <li>{@code 0} = north</li>
     *   <li>{@code π/2} = east</li>
     *   <li>{@code π} = south</li>
     *   <li>{@code 3π/2} = west</li>
     * </ul>
     *
     * <p>
     * The {@code normalizedLength} parameter is the angular arc length on the
     * unit sphere, in radians. Thus:
     * </p>
     * <ul>
     *   <li>{@code 0} returns the start point</li>
     *   <li>{@code π/2} is a quarter great circle</li>
     *   <li>{@code π} reaches the antipode</li>
     * </ul>
     *
     * <p>
     * The returned longitude is wrapped to {@code (-π, π]} using the same
     * canonical convention used elsewhere in the mapping subsystem. The
     * returned latitude is naturally in {@code [-π/2, π/2]}.
     * </p>
     *
     * @param startPoint       the start geographic point in radians
     *                         ({@code x = longitude, y = latitude}); must not
     *                         be {@code null}
     * @param azimuth          the initial azimuth in radians, using the
     *                         navigation convention
     * @param normalizedLength the angular arc length on the unit sphere in
     *                         radians
     * @return the endpoint geographic point in radians
     *         ({@code x = longitude, y = latitude})
     * @throws IllegalArgumentException if {@code startPoint} is {@code null}
     */
    public static Point2D.Double greatCircleEndPoint(Point2D.Double startPoint,
            double azimuth, double normalizedLength) {

        if (startPoint == null) {
            throw new IllegalArgumentException("startPoint must not be null");
        }

        double lon1 = startPoint.x;
        double lat1 = startPoint.y;

        double sinLat1 = Math.sin(lat1);
        double cosLat1 = Math.cos(lat1);

        double sinD = Math.sin(normalizedLength);
        double cosD = Math.cos(normalizedLength);

        double sinLat2 = sinLat1 * cosD + cosLat1 * sinD * Math.cos(azimuth);
        sinLat2 = clamp(sinLat2, -1.0, 1.0);

        double lat2 = Math.asin(sinLat2);

        double y = Math.sin(azimuth) * sinD * cosLat1;
        double x = cosD - sinLat1 * sinLat2;

        double lon2 = lon1 + Math.atan2(y, x);

        // Canonical wrap to (-π, π]
        while (lon2 <= -Math.PI) {
            lon2 += 2.0 * Math.PI;
        }
        while (lon2 > Math.PI) {
            lon2 -= 2.0 * Math.PI;
        }

        return new Point2D.Double(lon2, lat2);
    }

    /**
     * Small immutable 3D vector used internally for great-circle interpolation
     * on the unit sphere.
     */
    private static final class Vec3 {

        /**
         * The x component.
         */
        final double x;

        /**
         * The y component.
         */
        final double y;

        /**
         * The z component.
         */
        final double z;

        /**
         * Constructs a vector from Cartesian components.
         *
         * @param x x component
         * @param y y component
         * @param z z component
         */
        Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /**
         * Builds a unit-sphere Cartesian vector from a geographic point in
         * radians.
         *
         * @param latLon geographic point ({@code x=λ, y=φ}) in radians
         * @return the corresponding unit vector
         */
        static Vec3 fromLatLon(Point2D.Double latLon) {
            double lon = latLon.x;
            double lat = latLon.y;
            double cosLat = Math.cos(lat);
            return new Vec3(
                    cosLat * Math.cos(lon),
                    cosLat * Math.sin(lon),
                    Math.sin(lat));
        }

        /**
         * Converts this vector back to longitude/latitude in radians.
         *
         * @return geographic point ({@code x=λ, y=φ}) in radians
         */
        Point2D.Double toLatLon() {
            double lon = Math.atan2(y, x);
            double lat = Math.atan2(z, Math.sqrt(x * x + y * y));
            return new Point2D.Double(lon, lat);
        }

        /**
         * Computes the dot product with another vector.
         *
         * @param other the other vector
         * @return the dot product
         */
        double dot(Vec3 other) {
            return x * other.x + y * other.y + z * other.z;
        }

        /**
         * Returns the vector sum of this vector and another.
         *
         * @param other the other vector
         * @return the vector sum
         */
        Vec3 add(Vec3 other) {
            return new Vec3(x + other.x, y + other.y, z + other.z);
        }

        /**
         * Returns this vector scaled by a scalar factor.
         *
         * @param s the scalar factor
         * @return the scaled vector
         */
        Vec3 scale(double s) {
            return new Vec3(s * x, s * y, s * z);
        }

        /**
         * Returns a normalized copy of this vector.
         *
         * <p>
         * If the vector magnitude is extremely small, this vector is returned
         * unchanged.
         * </p>
         *
         * @return a unit-length vector, or this vector if normalization is not
         *         numerically meaningful
         */
        Vec3 normalize() {
            double mag = Math.sqrt(x * x + y * y + z * z);
            if (mag < TINY) {
                return this;
            }
            return new Vec3(x / mag, y / mag, z / mag);
        }
    }
}