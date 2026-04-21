package edu.cnu.mdi.mapping.item;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.MapConstants;
import edu.cnu.mdi.mapping.container.MapContainer;
import edu.cnu.mdi.mapping.graphics.MapGraphics;
import edu.cnu.mdi.mapping.graphics.MapGraphics.ProjectedMapShape;

/**
 * Abstract base for map-native items whose geometry is an ordered array of
 * geographic vertices (longitude/latitude in radians).
 *
 * <h2>Shared behaviour</h2>
 * <p>
 * Provides vertex storage, modification lifecycle (drag / vertex resize),
 * hit-testing, bounds, selection handles, projection caching, and feedback.
 * Subclasses ({@link MapPolylineItem}, {@link MapPolygonItem}) supply only
 * {@link #drawItem} and the {@link #buildShape} factory method.
 * </p>
 *
 * <h2>Vertex indexing</h2>
 * <p>
 * Vertices are in {@link #_latLons} in construction order. For a closed polygon
 * the path is implicitly closed — no duplicate of vertex 0 is stored.
 * </p>
 *
 * <h2>Drag</h2>
 * <p>
 * Raw lon/lat delta from drag-start to current cursor is applied to all vertices
 * from their snapshot positions (no incremental accumulation). Same high-latitude
 * caveat as {@link MapLineItem}: at high latitudes a purely northward drag may
 * introduce a small longitudinal error.
 * </p>
 *
 * <h2>Resize</h2>
 * <p>
 * Moves the vertex at {@link edu.cnu.mdi.item.ItemModification#getSelectIndex()}
 * to the current geographic cursor location.
 * </p>
 *
 * <h2>Rotation</h2>
 * <p>
 * Rotation is silently ignored for arbitrary multi-vertex geographic shapes
 * because there is no single unambiguous spherical rotation centre or
 * "bearing change" for non-line shapes. Subclasses with a well-defined centre
 * (e.g. a geodesic circle) may override {@link #modify()} to add it.
 * </p>
 */
public abstract class AMapMultiPointItem extends AMapItem {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /**
     * Geographic vertices in radians ({@code x = longitude, y = latitude}).
     * Never {@code null} after construction; length ≥ 2.
     */
    protected Point2D.Double[] _latLons;

    /**
     * Cached projected shape. Cleared by {@link #setDirty} and rebuilt lazily
     * by {@link #ensureProjectedShape}.
     */
    protected transient ProjectedMapShape _projectedShape;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * @param layer   the layer; must not be {@code null}
     * @param latLons geographic vertices in radians; must not be {@code null},
     *                length ≥ 2
     * @param keyVals optional {@link edu.cnu.mdi.util.PropertyUtils} key/value pairs
     * @throws IllegalArgumentException if {@code latLons} has fewer than 2 points
     */
    protected AMapMultiPointItem(Layer layer, Point2D.Double[] latLons,
                                 Object... keyVals) {
        super(layer, keyVals);
        if (latLons == null || latLons.length < 2)
            throw new IllegalArgumentException("latLons must contain at least 2 points");
        _latLons = copyVertices(latLons);
        updateFocus();
    }

    // -------------------------------------------------------------------------
    // Vertex accessors
    // -------------------------------------------------------------------------

    /** @return a defensive copy of the vertex array; never {@code null} */
    public Point2D.Double[] getLatLons() { return copyVertices(_latLons); }

    /**
     * Replace all vertices and mark dirty.
     *
     * @param latLons new vertices; must not be {@code null}, length ≥ 2
     */
    public void setLatLons(Point2D.Double[] latLons) {
        if (latLons == null || latLons.length < 2)
            throw new IllegalArgumentException("latLons must contain at least 2 points");
        _latLons = copyVertices(latLons);
        geometryChanged();
    }

    /** @return number of vertices (≥ 2) */
    public int getVertexCount() { return _latLons.length; }

    // -------------------------------------------------------------------------
    // Abstract — subclass supplies drawing and shape building
    // -------------------------------------------------------------------------

    /**
     * Build the projected shape from the current vertices.
     *
     * <p>Use {@link MapGraphics#buildProjectedPolyline} for open items,
     * {@link MapGraphics#buildProjectedPolygon} for closed items.</p>
     *
     * @param mc the owning map container
     * @return projected shape; never {@code null}
     */
    protected abstract ProjectedMapShape buildShape(MapContainer mc);

    // -------------------------------------------------------------------------
    // Visibility / bounds / hit-testing
    // -------------------------------------------------------------------------

    @Override
    public boolean shouldDraw(Graphics2D g2, IContainer container) {
        Rectangle r = getBounds(container);
        if (r == null) return false;
        Rectangle b = container.getComponent().getBounds();
        b.x = 0; b.y = 0;
        return b.intersects(r);
    }

    @Override
    public Rectangle getBounds(IContainer container) {
        ensureProjectedShape(container);
        Rectangle r = MapGraphics.getBounds(_projectedShape);
        return (r == null || r.isEmpty()) ? null : r;
    }

    @Override
    public boolean contains(IContainer container, Point screenPoint) {
        if (inASelectRect(container, screenPoint)) return true;
        ensureProjectedShape(container);
        return containsPoint(_projectedShape, screenPoint);
    }

    /**
     * Tests whether a screen point hits the projected shape.
     *
     * <p>Default: 6-pixel proximity (correct for open polylines).
     * {@link MapPolygonItem} overrides to also test interior containment.</p>
     *
     * @param shape       projected shape (may be {@code null})
     * @param screenPoint device-space test point
     * @return {@code true} if the point hits the shape
     */
    protected boolean containsPoint(ProjectedMapShape shape, Point screenPoint) {
        return MapGraphics.isPointNear(shape, screenPoint, 6.0);
    }

    // -------------------------------------------------------------------------
    // Selection handles
    // -------------------------------------------------------------------------

    /**
     * Returns all vertices projected to device-space handles, omitting any
     * that are off-map (non-finite projection result).
     *
     * @return handle positions, or {@code null} if none are visible
     */
    @Override
    public Point[] getSelectionPoints(IContainer container) {
        if (!(container instanceof MapContainer mc)) return null;
        Point[] pts = new Point[_latLons.length];
        int count = 0;
        for (Point2D.Double ll : _latLons) {
            Point p = projectLatLon(mc, ll);
            if (p != null) pts[count++] = p;
        }
        return (count == 0) ? null : Arrays.copyOf(pts, count);
    }

    // -------------------------------------------------------------------------
    // Modification lifecycle
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Stores a {@link MultiPointStartState} snapshot of all vertex positions
     * in the modification's user-object slot.</p>
     */
    @Override
    public void startModification() {
        super.startModification();
        if (_modification != null)
            _modification.setUserObject(new MultiPointStartState(_latLons));
    }

    /**
     * {@inheritDoc}
     *
     * <p>DRAG: shifts all vertices by the lon/lat delta from the start snapshot.
     * RESIZE: moves the selected vertex to the cursor. ROTATE: silently ignored.</p>
     */
    @Override
    public void modify() {
        if (_modification == null) return;
        if (!(_modification.getContainer() instanceof MapContainer mc)) return;

        Point startMouse   = _modification.getStartMousePoint();
        Point currentMouse = _modification.getCurrentMousePoint();

        switch (_modification.getType()) {

            case DRAG -> {
                if (!isDraggable()) return;
                Point2D.Double startLL   = localToLatLon(mc, startMouse);
                Point2D.Double currentLL = localToLatLon(mc, currentMouse);
                if (startLL == null || currentLL == null) return;
                MultiPointStartState state =
                        (MultiPointStartState) _modification.getUserObject();
                double dLon = currentLL.x - startLL.x;
                double dLat = currentLL.y - startLL.y;
                for (int i = 0; i < _latLons.length; i++) {
                    _latLons[i].x = state.vertices[i].x + dLon;
                    _latLons[i].y = state.vertices[i].y + dLat;
                }
            }

            case RESIZE -> {
                Point2D.Double currentLL = localToLatLon(mc, currentMouse);
                if (currentLL == null) return;
                int idx = _modification.getSelectIndex();
                if (idx >= 0 && idx < _latLons.length)
                    _latLons[idx] = new Point2D.Double(currentLL.x, currentLL.y);
            }

            case ROTATE -> { return; }  // silently ignored — see class Javadoc
        }

        geometryChanged();
        _modification.getContainer().refresh();
    }

    // -------------------------------------------------------------------------
    // AItem abstract implementations
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns the axis-aligned lon/lat bounding box of the vertex set.
     * May under-estimate arcs that bulge beyond the chord; use
     * {@link #getBounds(IContainer)} for exact pixel bounds.</p>
     */
    @Override
    public Rectangle2D.Double getWorldBounds() {
        if (_latLons == null || _latLons.length == 0) return null;
        double xmin = _latLons[0].x, xmax = xmin;
        double ymin = _latLons[0].y, ymax = ymin;
        for (Point2D.Double ll : _latLons) {
            xmin = Math.min(xmin, ll.x); xmax = Math.max(xmax, ll.x);
            ymin = Math.min(ymin, ll.y); ymax = Math.max(ymax, ll.y);
        }
        return new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Applies raw lon/lat offsets to all vertices. See {@link MapLineItem}
     * for the high-latitude caveat.</p>
     */
    @Override
    public void translateWorld(double dx, double dy) {
        if (Math.abs(dx) < 1.0e-12 && Math.abs(dy) < 1.0e-12) return;
        for (Point2D.Double ll : _latLons) { ll.x += dx; ll.y += dy; }
        geometryChanged();
    }

    // -------------------------------------------------------------------------
    // Feedback
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Adds vertex count and total great-circle perimeter when the cursor
     * is over the item.</p>
     */
    @Override
    public void getFeedbackStrings(IContainer container, Point pp,
            Point2D.Double wp, List<String> feedbackStrings) {
        if (feedbackStrings == null || !contains(container, pp)) return;
        feedbackStrings.add("$yellow$" + getDisplayName()
                + " (" + _latLons.length + " vertices)");
        double totalRad = 0.0;
        for (int i = 0; i < _latLons.length - 1; i++)
            totalRad += MapGraphics.greatCircleLength(_latLons[i], _latLons[i + 1]);
        feedbackStrings.add(String.format("perimeter: %.1f km",
                MapConstants.RADIUS_EARTH_KM * totalRad));
    }

    // -------------------------------------------------------------------------
    // Focus / dirty / removal
    // -------------------------------------------------------------------------

    /** Recomputes the focus as the geographic mean of all vertices. */
    @Override
    protected void updateFocus() {
        if (_latLons == null || _latLons.length == 0) return;
        double sumLon = 0.0, sumLat = 0.0;
        for (Point2D.Double ll : _latLons) { sumLon += ll.x; sumLat += ll.y; }
        _focus = new Point2D.Double(sumLon / _latLons.length,
                                    sumLat / _latLons.length);
    }

    @Override
    public void setDirty(boolean dirty) {
        super.setDirty(dirty);
        if (dirty) _projectedShape = null;
    }

    @Override
    public void prepareForRemoval() {
        _projectedShape = null;
        _latLons        = null;
        super.prepareForRemoval();
    }

    // -------------------------------------------------------------------------
    // Protected helpers
    // -------------------------------------------------------------------------

    /** Rebuilds {@link #_projectedShape} if null. */
    protected void ensureProjectedShape(IContainer container) {
        if (_projectedShape != null) return;
        if (!(container instanceof MapContainer mc)) return;
        if (_latLons == null) return;
        _projectedShape = buildShape(mc);
    }

    /**
     * Projects a geographic point to device pixels.
     *
     * @return device-space point, or {@code null} if off-map
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
     * Converts a device-space point to geographic lon/lat in radians.
     *
     * @return geographic point, or {@code null} if either argument is {@code null}
     */
    protected Point2D.Double localToLatLon(MapContainer mc, Point p) {
        if (mc == null || p == null) return null;
        Point2D.Double ll = new Point2D.Double();
        mc.localToLatLon(p, ll);
        return ll;
    }

    /** Deep defensive copy of a vertex array. */
    protected static Point2D.Double[] copyVertices(Point2D.Double[] src) {
        Point2D.Double[] dst = new Point2D.Double[src.length];
        for (int i = 0; i < src.length; i++)
            dst[i] = (src[i] == null) ? null : new Point2D.Double(src[i].x, src[i].y);
        return dst;
    }

    // -------------------------------------------------------------------------
    // Start-of-modification snapshot
    // -------------------------------------------------------------------------

    /**
     * Immutable snapshot of all vertex positions at drag-start.
     * Stored in the {@link edu.cnu.mdi.item.ItemModification} user-object slot.
     */
    protected static final class MultiPointStartState {
        final Point2D.Double[] vertices;
        MultiPointStartState(Point2D.Double[] src) { this.vertices = copyVertices(src); }
    }
}