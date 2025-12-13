package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import edu.cnu.mdi.component.MagnifyWindow;
import edu.cnu.mdi.view.BaseView;

/**
 * Tool that drives the magnification overlay/window.
 * <p>
 * Behavior matches the legacy {@code MagnifyButton}:
 * </p>
 * <ul>
 *   <li>On mouse move: delegates to {@link BaseView#handleMagnify(MouseEvent)}.</li>
 *   <li>On mouse exit: closes the {@link MagnifyWindow}.</li>
 *   <li>On tool deselect: closes the {@link MagnifyWindow} to avoid orphan windows.</li>
 * </ul>
 *
 * @author heddle
 */
public class MagnifyTool implements ITool {

    /** Tool id used by {@link ToolController}. */
    public static final String ID = "magnify";

    /** Cursor image to use while magnifying (same as legacy magnify button). */
    private static final String CURSOR_IMAGE = "images/box_zoomcursor.gif";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String toolTip() {
        return "Magnification";
    }

    @Override
    public Cursor cursor(ToolContext ctx) {
        // Use your CursorManager (cached, platform-safe).
        return ctx.cursors().custom(CURSOR_IMAGE, 0, 0);
    }

    @Override
    public void mouseMoved(ToolContext ctx, MouseEvent e) {
        BaseView view = (ctx.container() == null) ? null : ctx.container().getView();
        if (view != null) {
            view.handleMagnify(e);
        }
    }

    @Override
    public void mouseExited(ToolContext ctx, MouseEvent e) {
        MagnifyWindow.closeMagnifyWindow();
    }

    @Override
    public void onDeselected(ToolContext ctx) {
        // Important: switching tools should also shut the magnify window.
        MagnifyWindow.closeMagnifyWindow();
    }
}
