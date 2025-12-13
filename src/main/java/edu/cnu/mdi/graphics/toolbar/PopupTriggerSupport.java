package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemPopupManager;
import edu.cnu.mdi.ui.menu.ViewPopupMenu;
import edu.cnu.mdi.view.BaseView;

/**
 * Utility for handling platform-specific popup triggers (context menus) on a view/canvas.
 * <p>
 * The policy mirrors the legacy {@code ToolBarToggleButton.popupTrigger(...)} behavior:
 * </p>
 * <ol>
 *   <li>If an item exists under the cursor and is right-clickable, show the item popup.</li>
 *   <li>Otherwise, allow the view a chance to handle the right-click via
 *       {@link BaseView#rightClicked(MouseEvent)}.</li>
 *   <li>Otherwise, show the view's {@link ViewPopupMenu} (if enabled).</li>
 * </ol>
 *
 * <h2>Invoker component</h2>
 * <p>
 * Swing popups need an "invoker" component for coordinate space and ownership.
 * In the old code this was the button ("this"). In the new framework it should be the
 * container's canvas component (or any component in the same hierarchy).
 * </p>
 *
 * <h2>Recommended usage</h2>
 * <pre>{@code
 * if (PopupTriggerSupport.isPopupTrigger(e)) {
 *     PopupTriggerSupport.showPopup(container, e);
 *     return;
 * }
 * }</pre>
 *
 * @author heddle
 */
public final class PopupTriggerSupport {

    private PopupTriggerSupport() {
        // static utility
    }

    /**
     * Determine whether the given mouse event should trigger a context menu.
     * <p>
     * On some platforms the popup trigger occurs on press; on others it occurs on release.
     * Call this check from both handlers if you want fully correct behavior.
     * </p>
     *
     * @param e the mouse event.
     * @return true if the event is a popup trigger.
     */
    public static boolean isPopupTrigger(MouseEvent e) {
        return e != null && e.isPopupTrigger();
    }

    /**
     * Show the appropriate popup for a right-click/popup-trigger event.
     * <p>
     * Uses the container's view, item-under-cursor, and view popup menu according to
     * the legacy policy.
     * </p>
     *
     * @param container the container owning the canvas/view.
     * @param e         the popup-trigger mouse event.
     */
    public static void showPopup(IContainer container, MouseEvent e) {
        if (container == null || e == null) {
            return;
        }

        BaseView view = container.getView();
        if (view == null) {
            return;
        }

        Point p = e.getPoint();

        // Item under cursor?
        AItem item = container.getItemAtPoint(p);
        if (item != null && item.isRightClickable()) {
            ItemPopupManager.prepareForPopup(item, container, p);
            return;
        }

        // Give the view a chance to handle it.
        if (view.rightClicked(e)) {
            return;
        }

        // Otherwise show the view popup menu if enabled.
        ViewPopupMenu vmenu = view.getViewPopupMenu();
        if (vmenu == null || !vmenu.isEnabled()) {
            return;
        }

        Component invoker = container.getComponent();
        if (invoker == null) {
            return;
        }

        vmenu.show(invoker, e.getX(), e.getY());
    }
}
