package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Objects;

import edu.cnu.mdi.container.IAnnotationSupport;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.rubberband.IRubberbanded;
import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemList;
import edu.cnu.mdi.util.Environment;

/**
 * Base class for tools that create an {@link AItem} from a multi-vertex
 * {@link Rubberband} gesture (e.g. polygon, polyline, radial arc).
 * <p>
 * This tool base is container-agnostic: it requires the active container to
 * implement {@link IAnnotationSupport}, but it does not require a specific
 * container subclass (e.g. {@code DrawingContainer}).
 * </p>
 *
 * <h2>Lifecycle note</h2>
 * {@link #doneRubberbanding()} is invoked by {@link Rubberband} without a
 * {@link ToolContext}. Also, calling {@link ToolController#resetToDefault()}
 * can synchronously deselect this tool (invoking {@link #onDeselected(ToolContext)})
 * while we are still finishing. Therefore this class snapshots required state
 * first and clears fields early to avoid NPEs.
 *
 * @author heddle
 */
public abstract class AbstractVertexRubberbandTool implements ITool, IRubberbanded {

    /** Minimum number of vertices required to accept the gesture. */
    private final int minVertices;

    /** Active rubber-band session (null when idle). */
    private Rubberband rubberband;

    /** Container that owns the current gesture (null when idle). */
    private IContainer owner;

    /** Creation capability for the current gesture (null when idle). */
    private IAnnotationSupport creator;

    /** Cached controller so we can reset tool selection from {@link #doneRubberbanding()}. */
    private ToolController controller;

    /**
     * Create a vertex-based rubberband tool.
     *
     * @param minVertices minimum number of vertices required to create an item (>= 1).
     */
    protected AbstractVertexRubberbandTool(int minVertices) {
        this.minVertices = Math.max(1, minVertices);
    }

    /** @return the rubberband policy used to collect vertices. */
    protected abstract Rubberband.Policy rubberbandPolicy();

    /**
     * Create the item from the collected vertices (screen coords).
     *
     * @param creator  container annotation-creation capability (never null)
     * @param list     the list the new item should be added to (never null)
     * @param vertices the collected vertices (never null; length >= {@link #minVertices})
     * @return the created item, or null
     */
    protected abstract AItem createItem(IAnnotationSupport creator, ItemList list, Point[] vertices);

    /** Hook for subclasses to configure the created item (draggable/resizable/etc). */
    protected void configureItem(AItem item) {
        // no-op
    }

    /** @return cursor to use while active. Default is crosshair. */
    protected Cursor activeCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public Cursor cursor(ToolContext ctx) {
        return activeCursor();
    }

    @Override
    public void onSelected(ToolContext ctx) {
        controller = safeController(ctx);
    }

    @Override
    public final void mousePressed(ToolContext ctx, MouseEvent e) {
        if (ctx == null || e == null) {
            return;
        }

        IContainer c = ctx.container();
        if (!(c instanceof IAnnotationSupport)) {
            return;
        }

        if (rubberband != null) {
            return;
        }

        owner = c;
        creator = (IAnnotationSupport) c;

        // Defensive: temporary override tools may not get onSelected().
        if (controller == null) {
            controller = safeController(ctx);
        }

        Environment.getInstance().setDragging(true);

        Rubberband.Policy policy = Objects.requireNonNull(rubberbandPolicy(), "rubberbandPolicy");
        rubberband = new Rubberband(owner, this, policy);
        rubberband.setActive(true);
        rubberband.startRubberbanding(e.getPoint());
    }

    @Override
    public final void doneRubberbanding() {

        // SNAPSHOT first: resetToDefault() can synchronously deselect this tool.
        final Rubberband rb = rubberband;
        final IContainer c = owner;
        final IAnnotationSupport cap = creator;
        final ToolController tc = controller;

        // Clear instance state immediately.
        rubberband = null;
        owner = null;
        creator = null;
        controller = null;

        try {
            if (rb == null || c == null || cap == null) {
                return;
            }

            Point[] vertices = rb.getRubberbandVertices();
            if (!isValid(vertices)) {
                return;
            }

            ItemList list = c.getAnnotationList();
            AItem item = createItem(cap, list, vertices);

            if (item != null) {
                configureItem(item);
            }

            c.selectAllItems(false);

            if (tc != null) {
                tc.resetToDefault();
            }

            c.refresh();

        } finally {
            Environment.getInstance().setDragging(false);
        }
    }

    @Override
    public void onDeselected(ToolContext ctx) {
        cancelRubberband();
        owner = null;
        creator = null;
        controller = null;
        Environment.getInstance().setDragging(false);
    }

    /** Cancel the rubberband interaction (if any). */
    protected final void cancelRubberband() {
        Rubberband rb = rubberband;
        rubberband = null;
        if (rb != null) {
            rb.cancel();
        }
    }

    private boolean isValid(Point[] vertices) {
        return vertices != null && vertices.length >= minVertices;
    }

    /**
     * ToolContext#controller() can throw if ToolContext has no toolbar; tools should
     * treat that as "no controller" rather than crashing.
     */
    private static ToolController safeController(ToolContext ctx) {
        try {
            return (ctx == null) ? null : ctx.controller();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
