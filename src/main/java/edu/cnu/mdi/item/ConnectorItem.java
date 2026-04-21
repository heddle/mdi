package edu.cnu.mdi.item;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;

/**
 * A connection between two connectable items, drawn as an open polyline with an
 * editable elbow.
 *
 * <h2>Geometry model</h2>
 * <ul>
 *   <li>Vertex 0 — anchored to {@code startItem}.</li>
 *   <li>Vertex 1 — elbow (user-draggable).</li>
 *   <li>Vertex 2 — anchored to {@code endItem}.</li>
 * </ul>
 * <p>
 * Endpoints are recomputed from the connected items each time the connector's
 * geometry needs to be validated, so the connection follows its endpoints when
 * nodes move.  The elbow vertex is preserved across anchor sync operations.
 * </p>
 *
 * <h2>Anchor synchronisation</h2>
 * <p>
 * The connector compares the current endpoint world positions against the
 * last-known anchor positions ({@link #lastStart} and {@link #lastEnd}) before
 * redrawing.  Synchronisation is skipped when the anchors have not moved,
 * preventing the connector from being perpetually marked dirty on every frame.
 * </p>
 */
public class ConnectorItem extends PolylineItem {

    // -----------------------------------------------------------------------
    // Endpoint items
    // -----------------------------------------------------------------------

    /** The item this connector originates from; never {@code null}. */
    private final AItem startItem;

    /** The item this connector terminates at; never {@code null}. */
    private final AItem endItem;

    // -----------------------------------------------------------------------
    // Cached anchor positions (for change detection)
    // -----------------------------------------------------------------------

    /**
     * World position of the start anchor as of the last sync.
     * Initialised to {@code null} to force a sync on the first draw.
     */
    private Point2D.Double lastStart;

    /**
     * World position of the end anchor as of the last sync.
     * Initialised to {@code null} to force a sync on the first draw.
     */
    private Point2D.Double lastEnd;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Create a connector with a single elbow between two items.
     *
     * <p>The initial geometry is an L-shape: start anchor → elbow at
     * {@code (startAnchor.x, endAnchor.y)} → end anchor.</p>
     *
     * @param layer     the layer to add the connector to; recommended to use a
     *                  connection layer drawn below node layers
     * @param startItem the item the connector originates from; must not be
     *                  {@code null}
     * @param endItem   the item the connector terminates at; must not be
     *                  {@code null}
     * @throws NullPointerException if either item argument is {@code null}
     */
    public ConnectorItem(Layer layer, AItem startItem, AItem endItem) {
        // Note: AItem's constructor (called via the super chain) immediately
        // adds the item to the layer and registers feedback. By the time the
        // lines below run the item is already in the layer, but that is
        // harmless because ConnectorItem holds no geometry state that
        // listeners would query on ADDED — they wait for the first draw.
        super(layer, initialPoints(
                java.util.Objects.requireNonNull(startItem, "startItem"),
                java.util.Objects.requireNonNull(endItem,   "endItem")));

        this.startItem = startItem;
        this.endItem   = endItem;

        // Interaction flags
        setDraggable(false);     // dragging the connector as a whole is opt-in
        setResizable(true);      // vertex-based reshape uses the resize pathway
        setRotatable(false);
        setDeletable(true);
        setSelectable(true);
        setLocked(false);
        setConnectable(false);   // connections should not themselves be connectable
        setDisplayName("Connector");

        // Visual: open polyline, no fill
        getStyleSafe().setFillColor(null);

        // Cache the initial anchor positions so the first draw does not
        // unnecessarily mark the connector dirty.
        this.lastStart = anchorWorld(startItem);
        this.lastEnd   = anchorWorld(endItem);
    }

    // -----------------------------------------------------------------------
    // Drawing
    // -----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Recomputes endpoint anchors before drawing if either endpoint item
     * has moved since the last draw. Only performs the sync (and marks the
     * item dirty) when a change is detected, avoiding a perpetual
     * dirty-every-frame cycle.</p>
     */
    @Override
    public boolean shouldDraw(Graphics2D g2, IContainer container) {
        syncAnchorsIfMoved();
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Syncs anchors if needed, then delegates to {@link PolylineItem}.</p>
     */
    @Override
    public void drawItem(Graphics2D g2, IContainer container) {
        syncAnchorsIfMoved();
        super.drawItem(g2, container);
    }

    // -----------------------------------------------------------------------
    // Modification
    // -----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Lets {@link PolylineItem} reshape the elbow vertex, then forces
     * endpoints back onto their anchored items so the connection cannot be
     * accidentally detached by dragging a handle.</p>
     */
    @Override
    public void modify() {
        if (_modification == null) return;

        super.modify();          // vertex reshape (elbow move)
        forceAnchors();          // snap endpoints back to items
        setDirty(true);
    }

    // -----------------------------------------------------------------------
    // Bounds
    // -----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Ensures anchors are current before bounds queries are used for
     * picking and visibility tests.</p>
     */
    @Override
    public Rectangle2D.Double getWorldBounds() {
        syncAnchorsIfMoved();
        return super.getWorldBounds();
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * Return the item this connector originates from.
     *
     * @return the start item; never {@code null}
     */
    public AItem getStartItem() { return startItem; }

    /**
     * Return the item this connector terminates at.
     *
     * @return the end item; never {@code null}
     */
    public AItem getEndItem() { return endItem; }

    // -----------------------------------------------------------------------
    // Anchor synchronisation
    // -----------------------------------------------------------------------

    /**
     * Recompute endpoint vertices only when one or both anchor items have
     * moved since the last sync.
     *
     * <p>Movement is detected by comparing the current anchor world positions
     * against {@link #lastStart} and {@link #lastEnd}.  When no movement is
     * detected the path is left unchanged and the connector is <em>not</em>
     * marked dirty, preventing the superfluous repaint cycle that arose when
     * {@code syncAnchorsToItems()} was called unconditionally from
     * {@link #shouldDraw} and {@link #drawItem}.</p>
     */
    private void syncAnchorsIfMoved() {
        Point2D.Double a = anchorWorld(startItem);
        Point2D.Double b = anchorWorld(endItem);

        boolean startMoved = !samePoint(a, lastStart);
        boolean endMoved   = !samePoint(b, lastEnd);

        if (!startMoved && !endMoved) return;

        lastStart = a;
        lastEnd   = b;
        applyAnchors(a, b);
        setDirty(true);
    }

    /**
     * Unconditionally snap endpoint vertices to their anchor items.
     *
     * <p>Used from {@link #modify()} where we know the path has already
     * changed (so dirty state is appropriate) and we must re-snap regardless
     * of whether the items have moved.</p>
     */
    private void forceAnchors() {
        Point2D.Double a = anchorWorld(startItem);
        Point2D.Double b = anchorWorld(endItem);
        lastStart = a;
        lastEnd   = b;
        applyAnchors(a, b);
    }

    /**
     * Write the given world positions into vertices 0 and N-1 of {@link #_path},
     * leaving all intermediate (elbow) vertices unchanged.
     *
     * @param a new world position for vertex 0 (start anchor)
     * @param b new world position for the last vertex (end anchor)
     */
    private void applyAnchors(Point2D.Double a, Point2D.Double b) {
        if (_path == null || a == null || b == null) return;

        Point2D.Double[] wpoly = WorldGraphicsUtils.pathToWorldPolygon(_path);
        if (wpoly == null || wpoly.length < 2) return;

        wpoly[0]              = new Point2D.Double(a.x, a.y);
        wpoly[wpoly.length - 1] = new Point2D.Double(b.x, b.y);
        _path = WorldGraphicsUtils.worldPolygonToPath(wpoly);
        updateFocus();
    }

    // -----------------------------------------------------------------------
    // Static helpers
    // -----------------------------------------------------------------------

    /**
     * Return the world-coordinate anchor point for an item.
     *
     * <p>Currently returns the item's focus point. A future enhancement could
     * intersect the item boundary in the direction of the opposite endpoint for
     * a cleaner visual connection.</p>
     *
     * @param item the item to anchor to
     * @return the anchor in world coordinates, or {@code null} if the item has
     *         no focus
     */
    private static Point2D.Double anchorWorld(AItem item) {
        Point2D.Double f = item.getFocus();
        return (f == null) ? null : new Point2D.Double(f.x, f.y);
    }

    /**
     * Compute the initial three-vertex L-shape path (start → elbow → end).
     *
     * <p>The elbow is placed at {@code (startAnchor.x, endAnchor.y)}.
     * Callers may swap the elbow to {@code (endAnchor.x, startAnchor.y)}
     * if that orientation looks cleaner for a particular layout.</p>
     *
     * @param start the start item (focus used as anchor)
     * @param end   the end item (focus used as anchor)
     * @return a three-element world-coordinate point array
     */
    private static Point2D.Double[] initialPoints(AItem start, AItem end) {
        Point2D.Double a = anchorWorld(start);
        Point2D.Double b = anchorWorld(end);
        if (a == null) a = new Point2D.Double(0, 0);
        if (b == null) b = new Point2D.Double(1, 1);
        return new Point2D.Double[] {
            new Point2D.Double(a.x, a.y),
            new Point2D.Double(a.x, b.y),   // elbow
            new Point2D.Double(b.x, b.y)
        };
    }

    /**
     * Test whether two nullable world points are equal to within floating-point
     * precision.
     *
     * @param p1 first point; may be {@code null}
     * @param p2 second point; may be {@code null}
     * @return {@code true} if both are {@code null}, or if both are non-null
     *         and their coordinates differ by less than {@code 1e-9}
     */
    private static boolean samePoint(Point2D.Double p1, Point2D.Double p2) {
        if (p1 == null && p2 == null) return true;
        if (p1 == null || p2 == null) return false;
        return Math.abs(p1.x - p2.x) < 1e-9
            && Math.abs(p1.y - p2.y) < 1e-9;
    }
}