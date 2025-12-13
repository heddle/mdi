package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.Point;

import edu.cnu.mdi.container.IAnnotationSupport;
import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemList;

/**
 * Tool that creates a polyline item by vertex rubber-banding.
 * <p>
 * Drop-in replacement for the legacy {@code PolylineButton}.
 * </p>
 * <ul>
 *   <li>Press starts {@link Rubberband.Policy#POLYLINE} vertex capture.</li>
 *   <li>Completion creates a polyline item in the container's annotation list.</li>
 *   <li>After creation: clears selection, resets to default tool, refreshes.</li>
 * </ul>
 *
 * @author heddle
 */
public class PolylineTool extends AbstractVertexRubberbandTool {

    /** Tool id used by {@link ToolController} for registration/selection. */
    public static final String ID = "polyline";

    /**
     * Create a polyline tool.
     * <p>
     * Polylines require at least 2 vertices (matches the legacy check).
     * </p>
     */
    public PolylineTool() {
        super(2);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String toolTip() {
        return "Create a polyline";
    }

    @Override
    protected Cursor activeCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    protected Rubberband.Policy rubberbandPolicy() {
        return Rubberband.Policy.POLYLINE;
    }

    @Override
    protected AItem createItem(IAnnotationSupport owner, ItemList list, Point[] vertices) {
        return owner.createPolylineItem(list, vertices);
    }

    @Override
    protected void configureItem(AItem item) {
        item.setRightClickable(true);
        item.setDraggable(true);
        item.setRotatable(true);
        item.setResizable(true);
        item.setDeletable(true);
        item.setLocked(false);
    }
}
