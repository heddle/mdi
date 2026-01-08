package edu.cnu.mdi.item;

import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;

/**
 * A connection between two connectable items, drawn as an open polyline with an editable elbow.
 * <p>
 * Geometry model:
 * <ul>
 *   <li>Vertex 0: anchored to {@code startItem}</li>
 *   <li>Vertex 1: elbow (user draggable)</li>
 *   <li>Vertex 2: anchored to {@code endItem}</li>
 * </ul>
 * Endpoints are recomputed from the items each time geometry is updated, so the connection
 * follows its endpoints when nodes move.
 */
public class ConnectorItem extends PolylineItem {

    private final AItem startItem;
    private final AItem endItem;

    /**
     * Create a connector with a single elbow.
     *
     * @param layer     layer to add the connector to (recommended: a connection layer below nodes)
     * @param startItem first endpoint item (must not be null)
     * @param endItem   second endpoint item (must not be null)
     */
    public ConnectorItem(Layer layer, AItem startItem, AItem endItem) {
        super(layer, initialPoints(layer.getContainer(), startItem, endItem));

        this.startItem = java.util.Objects.requireNonNull(startItem, "startItem");
        this.endItem = java.util.Objects.requireNonNull(endItem, "endItem");

        // Connections should usually be selectable (for elbow editing) but not rotatable.
        setDraggable(false);     // dragging the whole connector is optional; start with false
        setResizable(true);      // vertex-based reshape uses "resize" pathway in your framework
        setRotatable(false);
        setDeletable(true);
        setSelectable(true);
        setLocked(false);

        setDisplayName("Connector");

        // Optional: make connections not "connectable" themselves
        setConnectable(false);

        // Make it visually line-like
        getStyleSafe().setFillColor(null);

        // Ensure endpoints match item anchors at creation
        syncAnchorsToItems();
    }

    @Override
    public boolean shouldDraw(Graphics g, IContainer container) {
        // Always recompute anchors before drawing so connection follows moved items.
        syncAnchorsToItems();
        return super.shouldDraw(g, container);
    }

    @Override
    public void drawItem(Graphics g, IContainer container) {
        // Keep anchors synced; then draw like a normal polyline.
        syncAnchorsToItems();
        super.drawItem(g, container);
    }

    /**
     * Get the starting item this connector is anchored to.
     * @return the start item
     */
	public AItem getStartItem() {
		return startItem;
	}

	/**
	 * Get the ending item this connector is anchored to.
	 * @return the end item
	 */
	public AItem getEndItem() {
		return endItem;
	}

	@Override
	public void modify() {
		if (_modification == null) {
			return;
		}

        // Let the PolylineItem reshape logic move vertices…
        super.modify();

        // …but then force endpoints back onto their anchored items.
        // This preserves elbow edits while keeping endpoints attached.
        syncAnchorsToItems();
        setDirty(true);
    }

    @Override
    public Rectangle2D.Double getWorldBounds() {
        // Ensure anchors are current before bounds queries (picking/visibility)
        syncAnchorsToItems();
        return super.getWorldBounds();
    }

    /**
     * Recompute endpoint vertices (0 and last) from the items.
     * Keeps elbow vertices unchanged.
     */
    private void syncAnchorsToItems() {
        if (_path == null) {
			return;
		}

        Point2D.Double[] wpoly = WorldGraphicsUtils.pathToWorldPolygon(_path);
        if (wpoly == null || wpoly.length < 2) {
			return;
		}

        Point2D.Double a = anchorWorld(startItem);
        Point2D.Double b = anchorWorld(endItem);
        if (a == null || b == null) {
			return;
		}

        wpoly[0] = a;
        wpoly[wpoly.length - 1] = b;

        _path = WorldGraphicsUtils.worldPolygonToPath(wpoly);
        updateFocus();
    }

    /**
     * Choose an anchor point on an item in world coordinates.
     * For now: use the item's focus point. Later: intersect boundary in direction of other endpoint.
     */
    private static Point2D.Double anchorWorld(AItem item) {
        Point2D.Double f = item.getFocus();
        return (f == null) ? null : new Point2D.Double(f.x, f.y);
    }

    /**
     * Initial L-shape: A -> (A.x, B.y) -> B.
     * You can swap to (B.x, A.y) based on which looks cleaner.
     */
    private static Point2D.Double[] initialPoints(IContainer c, AItem start, AItem end) {
        Point2D.Double a = anchorWorld(start);
        Point2D.Double b = anchorWorld(end);

        if (a == null) {
			a = new Point2D.Double(0, 0);
		}
        if (b == null) {
			b = new Point2D.Double(1, 1);
		}

        Point2D.Double elbow = new Point2D.Double(a.x, b.y);

        return new Point2D.Double[] {
                new Point2D.Double(a.x, a.y),
                elbow,
                new Point2D.Double(b.x, b.y)
        };
    }
}
