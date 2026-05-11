package edu.cnu.mdi.mapping.item;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemModification;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.MapConstants;
import edu.cnu.mdi.mapping.container.MapContainer;
import edu.cnu.mdi.mapping.graphics.MapGraphics;
import edu.cnu.mdi.mapping.graphics.MapGraphics.ProjectedMapShape;

/**
 * A map-native line item defined by two geographic endpoints and rendered as a
 * seam-aware great-circle route.
 *
 * <h2>Conceptual model</h2>
 * <p>
 * Unlike {@code edu.cnu.mdi.item.LineItem}, which stores a straight segment in
 * ordinary world (projection-space) coordinates, {@code MapLineItem} stores its
 * geometry in geographic coordinates:
 * </p>
 * <ul>
 *   <li>{@code _startLatLon} — geographic start point in radians
 *       ({@code x = longitude, y = latitude})</li>
 *   <li>{@code _endLatLon} — geographic end point in radians
 *       ({@code x = longitude, y = latitude})</li>
 * </ul>
 * <p>
 * When drawn, the item is projected and sampled as a great-circle route through
 * {@link MapGraphics}, giving the familiar "airline route" appearance on a map.
 * </p>
 *
 * <h2>Interaction model</h2>
 * <ul>
 *   <li><strong>Creation</strong> — use the ordinary straight-line rubberband.
 *       The tool converts its two screen endpoints to geographic coordinates via
 *       {@link MapContainer#localToLatLon} and constructs a
 *       {@code MapLineItem}.</li>
 *   <li><strong>Drag</strong> — moves both geographic endpoints by the
 *       great-circle displacement implied by the mouse movement, preserving
 *       great-circle length and initial bearing exactly.</li>
 *   <li><strong>Resize</strong> — replaces the selected endpoint with the
 *       current geographic cursor location.</li>
 *   <li><strong>Rotate</strong> — rotates both endpoints around the geodesic
 *       midpoint by the screen-space angle dragged, preserving great-circle
 *       length from the midpoint to each endpoint. See
 *       <a href="#rotation">Rotation semantics</a> below.</li>
 * </ul>
 *
 * <h2>Focus and selection handles</h2>
 * <p>
 * The item focus is the exact geodesic midpoint of the arc (computed via
 * {@link MapGraphics#greatCircleEndPoint} at half the arc length). The two
 * selection handles are the projected screen positions of the geographic
 * endpoints.
 * </p>
 *
 * <h2><a id="rotation">Rotation semantics</a></h2>
 * <p>
 * Rotation is well-defined and physically meaningful for a great-circle line.
 * The pivot is the geodesic midpoint (the item focus). The angular delta is
 * the screen-space three-point angle dragged by the user (identical to how
 * the base {@code PathBasedItem} handles rotation). The two half-lengths from
 * the midpoint to each endpoint are preserved; only the bearings change.
 * </p>
 * <p>
 * Because a great-circle has two distinct initial bearings (from start→midpoint
 * and from end→midpoint), both must be rotated independently using
 * {@link MapGraphics#greatCircleAzimuth} and {@link MapGraphics#greatCircleEndPoint}.
 * This keeps the arc symmetric around its pivot for any rotation angle.
 * </p>
 * <p>
 * The {@link AItem#_azimuth} field inherited from {@code AItem} is used as the
 * cumulative screen-space rotation angle (in degrees) for display purposes
 * (e.g. the rotation handle position). The geographic geometry is the
 * authoritative state; {@code _azimuth} is purely a display aid that accumulates
 * across multiple rotate gestures.
 * </p>
 *
 * <h2>Geometry and caching</h2>
 * <p>
 * The projected route is cached as a {@link ProjectedMapShape} after drawing.
 * That cached shape is used for hit testing and bounds. The cache is cleared
 * whenever the item is marked dirty.
 * </p>
 *
 * <h2>Note on {@code getWorldBounds()}</h2>
 * <p>
 * The inherited {@link AItem#getWorldBounds()} contract requires a
 * projection-space bounding rectangle. For a great-circle arc this rectangle
 * is not computable without a projection reference, so this method returns
 * a simple longitude/latitude bounding box of the two <em>endpoints</em>.
 * This is conservative: it may miss the arc's apex if the arc bulges
 * significantly above or below the chord (e.g. a trans-polar route).
 * All visibility and hit-testing that matters uses the screen-space
 * {@link #getBounds(IContainer)} path through the cached projected shape,
 * which is exact.
 * </p>
 */
public class MapLineItem extends AMapItem {

    // -------------------------------------------------------------------------
    // Geographic geometry
    // -------------------------------------------------------------------------

    /**
     * Geographic start endpoint in radians ({@code x = longitude, y = latitude}).
     */
    protected Point2D.Double _startLatLon;

    /**
     * Geographic end endpoint in radians ({@code x = longitude, y = latitude}).
     */
    protected Point2D.Double _endLatLon;

    // -------------------------------------------------------------------------
    // Projection cache
    // -------------------------------------------------------------------------

    /**
     * Cached projected route produced during the most recent draw or bounds
     * query.
     *
     * <p>Invalidated whenever the item is marked dirty via {@link #setDirty}.
     * Rebuilt lazily by {@link #ensureProjectedShape}.</p>
     */
    protected transient ProjectedMapShape _projectedShape;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a map-native great-circle line item.
     *
     * @param layer       the layer this item lives on; must not be {@code null}
     * @param startLatLon geographic start point in radians
     *                    ({@code x = longitude, y = latitude})
     * @param endLatLon   geographic end point in radians
     *                    ({@code x = longitude, y = latitude})
     */
    public MapLineItem(Layer layer, Point2D.Double startLatLon,
                       Point2D.Double endLatLon) {
        this(layer, startLatLon, endLatLon, (Object[]) null);
    }

    /**
     * Creates a map-native great-circle line item with optional item property
     * key/value pairs.
     *
     * @param layer       the layer this item lives on; must not be {@code null}
     * @param startLatLon geographic start point in radians
     * @param endLatLon   geographic end point in radians
     * @param keyVals     optional {@link edu.cnu.mdi.util.PropertyUtils}
     *                    key/value pairs
     */
    public MapLineItem(Layer layer, Point2D.Double startLatLon,
                       Point2D.Double endLatLon, Object... keyVals) {
        super(layer, keyVals);
        _startLatLon = copy(startLatLon);
        _endLatLon   = copy(endLatLon);
        updateFocus();
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns a defensive copy of the geographic start endpoint.
     *
     * @return the start point in radians; never {@code null} after construction
     */
    public Point2D.Double getStartLatLon() { return copy(_startLatLon); }

    /**
     * Returns a defensive copy of the geographic end endpoint.
     *
     * @return the end point in radians; never {@code null} after construction
     */
    public Point2D.Double getEndLatLon() { return copy(_endLatLon); }

    /**
     * Replaces the geographic start endpoint and marks the item dirty.
     *
     * @param startLatLon the new start point in radians
     */
    public void setStartLatLon(Point2D.Double startLatLon) {
        _startLatLon = copy(startLatLon);
        geometryChanged();
    }

    /**
     * Replaces the geographic end endpoint and marks the item dirty.
     *
     * @param endLatLon the new end point in radians
     */
    public void setEndLatLon(Point2D.Double endLatLon) {
        _endLatLon = copy(endLatLon);
        geometryChanged();
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    /**
     * Draws this item as a seam-aware great-circle route.
     *
     * <p>The rendered path is built through {@link MapGraphics} and cached in
     * {@link #_projectedShape} for subsequent hit-testing and bounds queries.</p>
     *
     * @param g2        the graphics context
     * @param container the rendering container; must be a {@link MapContainer}
     */
    @Override
    public void drawItem(Graphics2D g2, IContainer container) {
        if (!(container instanceof MapContainer mapContainer)) return;

        // Rebuild and cache the projected shape for this frame.
        _projectedShape = MapGraphics.buildProjectedPolyline(mapContainer,
                new Point2D.Double[]{ copy(_startLatLon), copy(_endLatLon) });
        _lastDrawnPolygon = null; // hit-testing uses _projectedShape, not a polygon

        MapGraphics.drawMapLine(g2, mapContainer,
                _startLatLon, _endLatLon,
                getStyleSafe().getLineColor(),
                getStyleSafe().getLineWidth(),
                getStyleSafe().getLineStyle());
    }

    /**
     * Returns {@code true} if this item's projected pixel bounds intersect the
     * visible component area.
     *
     * @param g2        the graphics context (not used directly)
     * @param container the rendering container
     * @return {@code true} if the route is (at least partially) visible
     */
    @Override
    public boolean shouldDraw(Graphics2D g2, IContainer container) {
        Rectangle r = getBounds(container);
        if (r == null) return false;
        Rectangle b = container.getComponent().getBounds();
        b.x = 0;
        b.y = 0;
        return b.intersects(r);
    }

    // -------------------------------------------------------------------------
    // Hit testing
    // -------------------------------------------------------------------------

    /**
     * Tests whether the rendered route or its selection handles contain the
     * given screen point.
     *
     * <p>Handle hit-testing is checked first so endpoint handles remain easy
     * to select. If no handle is hit, the test falls through to a pixel
     * proximity check against the projected great-circle route.</p>
     *
     * @param container   the rendering container
     * @param screenPoint the device-space point to test
     * @return {@code true} if the point is near the route or on a handle
     */
    @Override
    public boolean contains(IContainer container, Point screenPoint) {
        if (inASelectRect(container, screenPoint)) return true;
        ensureProjectedShape(container);
        return MapGraphics.isPointNear(_projectedShape, screenPoint, 6.0);
    }

    /**
     * Returns the device-space bounds of the projected route.
     *
     * @param container the rendering container
     * @return the pixel-space bounding rectangle, or {@code null} if
     *         unavailable or empty
     */
    @Override
    public Rectangle getBounds(IContainer container) {
        ensureProjectedShape(container);
        Rectangle r = MapGraphics.getBounds(_projectedShape);
        return (r == null || r.isEmpty()) ? null : r;
    }

    // -------------------------------------------------------------------------
    // Selection handles and rotation handle
    // -------------------------------------------------------------------------

    /**
     * Returns the two endpoint handle positions in device coordinates.
     *
     * <p>Unlike a polyline, a map line exposes only its two geographic
     * endpoints as resize handles, even though the rendered route between
     * them is curved.</p>
     *
     * @param container the rendering container
     * @return the two endpoint handle positions, or {@code null} if either
     *         endpoint cannot be projected
     */
    @Override
    public Point[] getSelectionPoints(IContainer container) {
        if (!(container instanceof MapContainer mc)) return null;
        Point p0 = projectLatLon(mc, _startLatLon);
        Point p1 = projectLatLon(mc, _endLatLon);
        if (p0 == null || p1 == null) return null;
        return new Point[]{ p0, p1 };
    }

    /**
     * Returns the rotation handle position in device coordinates.
     *
     * <p>The handle is placed at the projected screen position of the geodesic
     * midpoint (the item focus), which is the pivot around which rotation
     * occurs. This gives the user a natural handle: grab it and drag in an arc
     * around the line's midpoint.</p>
     *
     * @param container the rendering container
     * @return the screen position of the rotation handle, or {@code null} if
     *         the focus cannot be projected
     */
    @Override
    public Point getRotatePoint(IContainer container) {
        if (!(container instanceof MapContainer mc)) return null;
        return projectLatLon(mc, _focus);
    }

    // -------------------------------------------------------------------------
    // Modification lifecycle
    // -------------------------------------------------------------------------

    /**
     * Continues an interactive modification.
     *
     * <p>Drag</p>
     * <p>Moves the start endpoint by the great-circle displacement between the
     * drag start and current mouse positions (converted to geographic
     * coordinates). The end endpoint is then recomputed from the new start
     * using the preserved great-circle azimuth and length, keeping the arc's
     * shape invariant across latitudes.</p>
     *
     * <p>Resize</p>
     * <p>Replaces the selected endpoint (index 0 = start, 1 = end) with the
     * current geographic cursor location. The opposite endpoint is unchanged.</p>
     *
     * <p>Rotate</p>
     * <p>Rotates both endpoints around the geodesic midpoint (the item focus)
     * by the screen-space three-point angle dragged by the user. Both half-arc
     * lengths (midpoint→start and midpoint→end) are preserved; only the
     * bearings change. The {@link AItem#_azimuth} field accumulates the
     * total screen-space rotation for display purposes.</p>
     */
    @Override
    public void modify() {
        if (_modification == null) return;

        if (!(_modification.getContainer() instanceof MapContainer mc)) return;

        Point startMouse   = _modification.getStartMousePoint();
        Point currentMouse = _modification.getCurrentMousePoint();

        ItemModification.ModificationType type = _modification.getType();

        switch (type) {

            case DRAG -> {
                if (!isDraggable()) return;

                Point2D.Double startLL   = localToLatLon(mc, startMouse);
                Point2D.Double currentLL = localToLatLon(mc, currentMouse);
                if (startLL == null || currentLL == null) return;

                MapLineStartState state = (MapLineStartState) _modification.getUserObject();
                double dLon = currentLL.x - startLL.x;
                double dLat = currentLL.y - startLL.y;

                _startLatLon.x = state.start.x + dLon;
                _startLatLon.y = state.start.y + dLat;
                _endLatLon = MapGraphics.greatCircleEndPoint(
                        _startLatLon, state.azimuth, state.length);
            }

            case RESIZE -> {
                Point2D.Double currentLL = localToLatLon(mc, currentMouse);
                if (currentLL == null) return;
                int index = _modification.getSelectIndex();
                if (index == 0) {
                    _startLatLon = copy(currentLL);
                } else {
                    _endLatLon = copy(currentLL);
                }
            }

            case ROTATE -> {
                if (!isRotatable()) return;

                /*
                 * The rotation angle is the screen-space three-point angle at the
                 * focus between the start-of-drag mouse ray and the current mouse
                 * ray. The same threePointAngle() computation that PathBasedItem
                 * uses is applied here — the pivot is the focus screen point, which
                 * is the geodesic midpoint.
                 *
                 * Both endpoints are then recomputed from the focus using the
                 * preserved half-lengths and the new bearings.
                 */
                MapLineStartState state = (MapLineStartState) _modification.getUserObject();

                Point vertex = _modification.getStartFocusPoint();
                if (vertex == null) return;

                double angleDeg = threePointAngle(startMouse, vertex, currentMouse);
                if (Double.isNaN(angleDeg)) return;

                // Snap to integer degrees for consistency with base items.
                angleDeg = Math.round(angleDeg);

                // Cumulative screen-space azimuth (for the rotation handle display).
                double newAzimuthDeg = state.startAzimuthDeg + angleDeg;
                setAzimuth(newAzimuthDeg);

                // The signed screen-space rotation maps to a bearing change.
                // Screen CW positive (matching the base item convention) means the
                // initial bearing from the midpoint to each endpoint increases.
                double deltaRad = Math.toRadians(angleDeg);

                // Restore the start-of-drag focus as the stable pivot.
                Point2D.Double pivot = state.focus;

                // Half-arc from pivot to start endpoint (measured start→pivot at
                // drag-start, reversed so we can reconstruct the start endpoint from
                // the pivot).
                double startBearing   = state.bearingPivotToStart + deltaRad;
                double endBearing     = state.bearingPivotToEnd   + deltaRad;

                _startLatLon = MapGraphics.greatCircleEndPoint(
                        pivot, startBearing, state.halfLengthToStart);
                _endLatLon   = MapGraphics.greatCircleEndPoint(
                        pivot, endBearing,   state.halfLengthToEnd);
            }
        }

        geometryChanged();
        _modification.getContainer().refresh();
    }

    // -------------------------------------------------------------------------
    // AItem abstract implementations
    // -------------------------------------------------------------------------

    /**
     * Returns an approximate world-space bounding rectangle.
     *
     * <p>This returns a simple longitude/latitude bounding box of the two
     * <em>endpoints</em>. It is provided only to satisfy the {@link AItem}
     * contract. It may under-estimate the true extent of high-latitude arcs
     * that bulge significantly above or below the chord between the endpoints.
     * All visibility and hit-testing that matters uses the exact screen-space
     * bounds available through {@link #getBounds(IContainer)}.</p>
     *
     * @return a lon/lat bounding rectangle in radians, or {@code null} if
     *         either endpoint is absent
     */
    @Override
    public Rectangle2D.Double getWorldBounds() {
        if (_startLatLon == null || _endLatLon == null) return null;
        double xmin = Math.min(_startLatLon.x, _endLatLon.x);
        double xmax = Math.max(_startLatLon.x, _endLatLon.x);
        double ymin = Math.min(_startLatLon.y, _endLatLon.y);
        double ymax = Math.max(_startLatLon.y, _endLatLon.y);
        return new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    /**
     * Translates the item by raw longitude and latitude offsets in radians.
     *
     * <p>This is a pragmatic translation: for small distances and mid-latitude
     * items it works well. At high latitudes the longitudinal scale diverges
     * from the latitudinal scale, so a northward mouse drag may introduce a
     * small longitudinal error. A future implementation could convert the pixel
     * delta to a geodesic bearing and length, then call
     * {@link MapGraphics#greatCircleEndPoint}, but that requires a
     * {@link MapContainer} reference not available here.</p>
     *
     * @param dx longitude offset in radians
     * @param dy latitude offset in radians
     */
    @Override
    public void translateWorld(double dx, double dy) {
        if (_startLatLon == null || _endLatLon == null) return;
        if (Math.abs(dx) < 1.0e-12 && Math.abs(dy) < 1.0e-12) return;
        _startLatLon.x += dx;
        _startLatLon.y += dy;
        _endLatLon.x   += dx;
        _endLatLon.y   += dy;
        geometryChanged();
    }

    // -------------------------------------------------------------------------
    // Feedback
    // -------------------------------------------------------------------------

    /**
     * Provides mouse-over feedback showing the great-circle length and the
     * geographic coordinates of both endpoints.
     *
     * @param container       the rendering container
     * @param pp              current mouse position in device coordinates
     * @param wp              current mouse position in container world
     *                        coordinates (not used here)
     * @param feedbackStrings the list to populate
     */
    @Override
    public void getFeedbackStrings(IContainer container, Point pp,
            Point2D.Double wp, List<String> feedbackStrings) {
        if (feedbackStrings == null || !contains(container, pp)) return;

        double length = MapConstants.RADIUS_EARTH_KM
                * MapGraphics.greatCircleLength(_startLatLon, _endLatLon);
        feedbackStrings.add("$yellow$" + getDisplayName()
                + " GC length: " + String.format("%.1f km", length));

        if (_startLatLon != null) {
            feedbackStrings.add(String.format("start: lon %.2f°, lat %.2f°",
                    Math.toDegrees(_startLatLon.x),
                    Math.toDegrees(_startLatLon.y)));
        }
        if (_endLatLon != null) {
            feedbackStrings.add(String.format("end: lon %.2f°, lat %.2f°",
                    Math.toDegrees(_endLatLon.x),
                    Math.toDegrees(_endLatLon.y)));
        }
    }

    // -------------------------------------------------------------------------
    // updateFocus / dirty / removal
    // -------------------------------------------------------------------------

    /**
     * Recomputes the item focus as the exact geodesic midpoint of the arc.
     *
     * <p>The midpoint is obtained by projecting half the total great-circle
     * arc length along the initial bearing from the start endpoint.</p>
     */
    @Override
    protected void updateFocus() {
        if (_startLatLon == null || _endLatLon == null) return;
        double length  = MapGraphics.greatCircleLength(_startLatLon, _endLatLon);
        double azimuth = MapGraphics.greatCircleAzimuth(_startLatLon, _endLatLon);
        if (Double.isNaN(azimuth)) {
            _focus = copy(_startLatLon);
        } else {
            _focus = MapGraphics.greatCircleEndPoint(_startLatLon, azimuth, length / 2.0);
        }
    }

    /**
     * Marks the item dirty and clears the cached projected route.
     *
     * @param dirty whether the item should be marked dirty
     */
    @Override
    public void setDirty(boolean dirty) {
        super.setDirty(dirty);
        if (dirty) _projectedShape = null;
    }

    /**
     * Prepares this item for removal, releasing geographic geometry references.
     */
    @Override
    public void prepareForRemoval() {
        _projectedShape = null;
        _startLatLon    = null;
        _endLatLon      = null;
        super.prepareForRemoval();
    }

    // -------------------------------------------------------------------------
    // Protected helpers
    // -------------------------------------------------------------------------

    /**
     * Ensures that {@link #_projectedShape} is populated, building it from the
     * current geographic endpoints if necessary.
     *
     * @param container the rendering container
     */
    protected void ensureProjectedShape(IContainer container) {
        if (_projectedShape != null) return;
        if (!(container instanceof MapContainer mc)) return;
        if (_startLatLon == null || _endLatLon == null) return;
        _projectedShape = MapGraphics.buildProjectedPolyline(mc,
                new Point2D.Double[]{ copy(_startLatLon), copy(_endLatLon) });
    }

    /**
     * Projects a geographic point ({@code x = longitude, y = latitude} in
     * radians) to a device-space pixel coordinate.
     *
     * <p>Returns {@code null} if the point projects to a non-finite coordinate
     * (e.g. the point is on the far hemisphere of an orthographic projection).
     * </p>
     *
     * @param mc     the map container
     * @param latLon geographic point in radians
     * @return the device-space point, or {@code null} if projection fails
     */
    protected Point projectLatLon(MapContainer mc, Point2D.Double latLon) {
        if (mc == null || latLon == null) return null;
        Point2D.Double xy = new Point2D.Double();
        ((edu.cnu.mdi.mapping.MapView2D) mc.getView())
                .getProjection().latLonToXY(latLon, xy);
        if (!Double.isFinite(xy.x) || !Double.isFinite(xy.y)) return null;
        Point p = new Point();
        mc.worldToLocal(p, xy);
        return p;
    }

    /**
     * Converts a device-space point to a geographic longitude/latitude in
     * radians.
     *
     * @param mc the map container
     * @param p  the device-space point
     * @return the geographic point in radians, or {@code null} if the
     *         container or point is null
     */
    protected Point2D.Double localToLatLon(MapContainer mc, Point p) {
        if (mc == null || p == null) return null;
        Point2D.Double ll = new Point2D.Double();
        mc.localToLatLon(p, ll);
        return ll;
    }

    /**
     * Returns a defensive copy of a geographic point.
     *
     * @param p the source point; may be {@code null}
     * @return a copied point, or {@code null} if the source is {@code null}
     */
    protected static Point2D.Double copy(Point2D.Double p) {
        return (p == null) ? null : new Point2D.Double(p.x, p.y);
    }

    /**
     * Computes the screen-space three-point angle at {@code vertex} between
     * rays to {@code p1} and {@code p2}, in degrees.
     *
     * <p>Positive values indicate a clockwise rotation from the p1 ray to the
     * p2 ray in screen space (y-down), matching the convention used by
     * {@code PathBasedItem.threePointAngle}.</p>
     *
     * @param p1     start of the first ray (typically the drag start point)
     * @param vertex the pivot (typically the focus screen point)
     * @param p2     start of the second ray (typically the current mouse point)
     * @return the angle in degrees, or {@link Double#NaN} if either ray is
     *         degenerate
     */
    private double threePointAngle(Point p1, Point vertex, Point p2) {
        double ax = p1.x - vertex.x;
        double ay = p1.y - vertex.y;
        double bx = p2.x - vertex.x;
        double by = p2.y - vertex.y;
        double a = Math.sqrt(ax * ax + ay * ay);
        if (a < 1.0e-10) return Double.NaN;
        double b = Math.sqrt(bx * bx + by * by);
        if (b < 1.0e-10) return Double.NaN;
        double adotb  = ax * bx + ay * by;
        double acrossb = ax * by - ay * bx;
        double ang = Math.toDegrees(Math.acos(
                Math.max(-1.0, Math.min(1.0, adotb / (a * b)))));
        if (acrossb < 0.0) ang = 360.0 - ang;
        return ang;
    }

    // -------------------------------------------------------------------------
    // Start-of-modification snapshot
    // -------------------------------------------------------------------------

    /**
     * Immutable snapshot of the item's geodesic state at the beginning of an
     * interactive modification gesture.
     *
     * <p>All bearings and lengths are computed once at drag-start and reused
     * throughout the gesture, preventing the drift that would occur if they
     * were recomputed incrementally on each mouse move.</p>
     */
    private static final class MapLineStartState {

        /** Start endpoint at drag-start, in radians. */
        final Point2D.Double start;

        /** Geodesic midpoint (focus) at drag-start, in radians. */
        final Point2D.Double focus;

        /** Full great-circle length from start to end, in radians. */
        final double length;

        /** Initial great-circle bearing from start to end, in radians. */
        final double azimuth;

        /** Great-circle half-length from the midpoint to the start endpoint, in radians. */
        final double halfLengthToStart;

        /** Great-circle half-length from the midpoint to the end endpoint, in radians. */
        final double halfLengthToEnd;

        /** Initial great-circle bearing from the midpoint to the start endpoint, in radians. */
        final double bearingPivotToStart;

        /** Initial great-circle bearing from the midpoint to the end endpoint, in radians. */
        final double bearingPivotToEnd;

        /**
         * Cumulative screen-space azimuth (degrees) at drag-start, captured
         * from {@link AItem#getAzimuth()} so that the running total of
         * on-screen rotation is preserved across sequential rotate gestures.
         */
        final double startAzimuthDeg;

        MapLineStartState(Point2D.Double start, Point2D.Double end) {
            this.start   = copy(start);
            this.length  = MapGraphics.greatCircleLength(start, end);
            this.azimuth = MapGraphics.greatCircleAzimuth(start, end);

            // Geodesic midpoint.
            double az = Double.isNaN(this.azimuth) ? 0.0 : this.azimuth;
            this.focus = MapGraphics.greatCircleEndPoint(start, az, this.length / 2.0);

            // Half-lengths from pivot to each endpoint.
            this.halfLengthToStart = MapGraphics.greatCircleLength(focus, start);
            this.halfLengthToEnd   = MapGraphics.greatCircleLength(focus, end);

            // Bearings from pivot to each endpoint.
            double bps = MapGraphics.greatCircleAzimuth(focus, start);
            double bpe = MapGraphics.greatCircleAzimuth(focus, end);
            this.bearingPivotToStart = Double.isNaN(bps) ? 0.0 : bps;
            this.bearingPivotToEnd   = Double.isNaN(bpe) ? Math.PI : bpe;

            // Screen-space azimuth is read from the item itself via the outer
            // class's getAzimuth(); this field carries its value at drag-start.
            this.startAzimuthDeg = 0.0; // overwritten by the item below
        }

        /**
         * Create a copy with a specific {@code startAzimuthDeg} value.
         * Used by the item's {@code startModification} to record the running
         * screen-space rotation total before this gesture begins.
         */
        private MapLineStartState(MapLineStartState src, double startAzimuthDeg) {
            this.start             = src.start;
            this.focus             = src.focus;
            this.length            = src.length;
            this.azimuth           = src.azimuth;
            this.halfLengthToStart = src.halfLengthToStart;
            this.halfLengthToEnd   = src.halfLengthToEnd;
            this.bearingPivotToStart = src.bearingPivotToStart;
            this.bearingPivotToEnd   = src.bearingPivotToEnd;
            this.startAzimuthDeg   = startAzimuthDeg;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overridden to attach the {@link MapLineStartState} snapshot with the
     * current screen-space azimuth baked in, so that sequential rotate gestures
     * accumulate correctly.</p>
     */
    @Override
    public void startModification() {
        super.startModification();
        if (_modification != null) {
            MapLineStartState base = new MapLineStartState(_startLatLon, _endLatLon);
            _modification.setUserObject(
                    new MapLineStartState(base, getAzimuth()));
        }
    }
}