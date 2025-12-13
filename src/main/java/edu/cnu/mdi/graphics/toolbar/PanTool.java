package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

/**
 * Tool that pans the active {@link edu.cnu.mdi.container.IContainer}.
 * <p>
 * This tool delegates the mechanics of panning to a {@link PanBehavior}
 * strategy. This eliminates container-specific hacks (e.g. "standard panning")
 * and allows map views or specialized containers to supply custom panning rules
 * without subclassing UI buttons.
 * </p>
 * <p>
 * Typical behaviors include:
 * </p>
 * <ul>
 *   <li>{@link DirectPanBehavior} for continuous transform updates</li>
 *   <li>{@link PreviewImagePanBehavior} for snapshot-preview panning with a
 *       single commit on release</li>
 * </ul>
 *
 * @author heddle
 */
public class PanTool implements ITool {

    private final PanBehavior behavior;

    /**
     * Create a pan tool using the given panning strategy.
     *
     * @param behavior the panning behavior implementation (must not be null).
     */
    public PanTool(PanBehavior behavior) {
        this.behavior = behavior;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String id() {
        return "pan";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toolTip() {
        return "Pan the view";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor cursor(ToolContext ctx) {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }

    /**
     * Begin panning on button-1 press.
     *
     * @param ctx the tool context.
     * @param e   the mouse event.
     */
    @Override
    public void mousePressed(ToolContext ctx, MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            behavior.begin(ctx, e);
        }
    }

    /**
     * Update panning during drag.
     *
     * @param ctx the tool context.
     * @param e   the mouse event.
     */
    @Override
    public void mouseDragged(ToolContext ctx, MouseEvent e) {
        behavior.drag(ctx, e);
    }

    /**
     * Commit/finish panning on button-1 release.
     *
     * @param ctx the tool context.
     * @param e   the mouse event.
     */
    @Override
    public void mouseReleased(ToolContext ctx, MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            behavior.end(ctx, e);
        }
    }
}
