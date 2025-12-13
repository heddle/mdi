package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import edu.cnu.mdi.item.YouAreHereItem;

/**
 * Tool that sets/updates the container "range" reference point (anchor) by
 * placing or moving a {@link YouAreHereItem}.
 * <p>
 * Legacy behavior (from {@code RangeButton}):
 * </p>
 * <ul>
 *   <li>On mouse press, converts the mouse point to world coordinates.</li>
 *   <li>If a {@link YouAreHereItem} already exists, calls {@link YouAreHereItem#setFocus(Point2D.Double)}.</li>
 *   <li>Otherwise creates a new item via {@link YouAreHereItem#createYouAreHereItem(edu.cnu.mdi.container.IContainer, Point2D.Double)}.</li>
 *   <li>After updating/creating, clears the tool selection back to the default tool and refreshes.</li>
 * </ul>
 *
 * <p>
 * This tool intentionally does not reference toolbar button classes; it uses
 * {@link ToolContext} to access the container and to reset to the default tool.
 * </p>
 *
 * @author heddle
 */
public class RangeTool implements ITool {

    /** Tool id used for registration/selection. */
    public static final String ID = "range";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String toolTip() {
        return "Range";
    }

    @Override
    public Cursor cursor(ToolContext ctx) {
        // You can swap in a custom cursor later if desired.
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    /**
     * On press, set/move the YouAreHereItem focus to the clicked world point,
     * then return to the default tool.
     */
    @Override
    public void mousePressed(ToolContext ctx, MouseEvent e) {
        if (ctx == null || e == null || ctx.container() == null) {
            return;
        }

        Point2D.Double wp = new Point2D.Double();
        ctx.container().localToWorld(e.getPoint(), wp);

        YouAreHereItem item = ctx.container().getYouAreHereItem();
        if (item != null) {
            item.setFocus(wp);
        } else {
            YouAreHereItem.createYouAreHereItem(ctx.container(), wp);
        }

        ctx.resetToDefaultTool();
        ctx.container().refresh();
    }
}
