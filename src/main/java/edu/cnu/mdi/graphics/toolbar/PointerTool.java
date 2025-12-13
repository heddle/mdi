package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.rubberband.IRubberbanded;
import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.graphics.style.Styled;
import edu.cnu.mdi.graphics.style.ui.StyleEditorDialog;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemModification;
import edu.cnu.mdi.util.Environment;

/**
 * Default "pointer" tool used for selection and direct manipulation of items.
 * <p>
 * Responsibilities:
 * </p>
 * <ul>
 *   <li>Single-click selection (with Ctrl-modified multi-select behavior)</li>
 *   <li>Rubber-band selection when clicking empty space</li>
 *   <li>Direct manipulation of a trackable item (drag/resize/rotate) via
 *       {@link ItemModification} and {@link AItem#modify()}</li>
 *   <li>Forwarding double-clicks to items</li>
 * </ul>
 *
 * <h2>Selection behavior</h2>
 * <ul>
 *   <li>Left-click on an unlocked item selects it (Ctrl enables additive selection)</li>
 *   <li>Left-click empty space starts a rubber-band rectangle selection</li>
 *   <li>Clicking a selected or locked item does not change selection</li>
 * </ul>
 *
 * <h2>Manipulation behavior</h2>
 * <p>
 * When the press occurs on a trackable item, dragging begins a modification gesture
 * after a minimum movement threshold ({@link #MIN_DRAG_STEP}). Once modifying, the
 * tool updates the {@link ItemModification} current mouse point and calls
 * {@link AItem#modify()} on each drag event. On release, the modification ends with
 * {@link AItem#stopModification()}.
 * </p>
 *
 * <h2>Rubber-band selection</h2>
 * <p>
 * Rubber-band interaction is delegated to {@link Rubberband}, which installs
 * temporary mouse listeners on the canvas and calls back to
 * {@link #doneRubberbanding()} when complete.
 * </p>
 *
 * <h2>Popup support</h2>
 * <p>
 * In the new framework, popup triggering should be handled centrally by
 * {@link BaseToolBar} (or a dedicated popup router). This tool therefore does not
 * perform popup work; it simply ignores right-click presses.
 * </p>
 *
 * @author heddle
 */
public class PointerTool implements ITool, IRubberbanded {

    /** Tool id used by {@link ToolController}. */
    public static final String ID = "pointer";

    /** Must move at least this many pixels to be considered a drag gesture. */
    private static final int MIN_DRAG_STEP = 2;

    /** Item being modified (dragged/rotated/resized), or null if none. */
    private AItem modifiedItem;

    /** Starting mouse position for drag gestures. */
    private final Point startPoint = new Point();

    /** Previous mouse position (used for drag thresholding). */
    private final Point prevPoint = new Point();

    /** Current mouse position. */
    private final Point currentPoint = new Point();

    /** Timestamp of last press (used for drag delay behavior). */
    private long lastPressTimeMillis;

    /** Optional drag delay (ms) to reduce accidental drags; 0 means no delay. */
    private long dragDelayMillis;

    /** True when actively modifying an item. */
    private boolean modifying;

    /** Active rubber-band selection, if any. */
    private Rubberband rubberband;

    /** Container for the current interaction session. */
    private IContainer owner;

    /** Context while this tool is active (needed for toolbar callbacks). */
    private ToolContext activeCtx;

    /** Create a pointer tool. */
    public PointerTool() {
    }

    /**
     * Set an optional delay that must elapse after mouse press before a drag
     * modification may start. This can reduce accidental drags.
     *
     * @param millis delay in milliseconds (0 disables delay).
     */
    public void setDragDelayMillis(long millis) {
        dragDelayMillis = Math.max(0, millis);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String toolTip() {
        return "Make selections";
    }

    @Override
    public Cursor cursor(ToolContext ctx) {
        return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }

    /**
     * Cache context and container when this tool becomes active.
     *
     * @param ctx the tool context (never null).
     */
    @Override
    public void onSelected(ToolContext ctx) {
        activeCtx = ctx;
        owner = (ctx == null) ? null : ctx.container();
        reset();
    }

    /**
     * Cancel any in-progress rubber-band selection and clear modification state
     * when the tool is deselected.
     *
     * @param ctx the tool context (never null).
     */
    @Override
    public void onDeselected(ToolContext ctx) {
        try {
            if (rubberband != null) {
                rubberband.cancel();
                rubberband = null;
            }
        } finally {
            owner = null;
            activeCtx = null;
            reset();
            Environment.getInstance().setDragging(false);
        }
    }

    /**
     * Handle mouse press for selection and potential modification.
     *
     * @param ctx tool context (never null).
     * @param e   mouse event.
     */
    @Override
    public void mousePressed(ToolContext ctx, MouseEvent e) {

        // Keep a stable owner for the duration of this gesture.
        if (owner == null) {
            owner = ctx.container();
        }
        if (owner == null) {
            return;
        }

        // Popups handled centrally by BaseToolBar.
        if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
            return;
        }

        // If a rubber-band is already active, ignore new presses.
        if (rubberband != null) {
            return;
        }

        // Find topmost item under pointer (ignore disabled).
        modifiedItem = owner.getItemAtPoint(e.getPoint());
        if (modifiedItem != null && !modifiedItem.isEnabled()) {
            modifiedItem = null;
        }

        // Empty-space press => begin rubber-band selection.
        if (modifiedItem == null) {
            startRubberbandSelection(e);
            return;
        }

        // Selection behavior
        selectItemsFromClick(modifiedItem, e);

        // Ignore untrackable items for modification.
        if (modifiedItem != null && !modifiedItem.isTrackable()) {
            modifiedItem = null;
        }

        // Initialize drag state
        currentPoint.setLocation(e.getPoint());
        startPoint.setLocation(e.getPoint());
        prevPoint.setLocation(e.getPoint());

        modifying = false;
        lastPressTimeMillis = System.currentTimeMillis();
    }

    /**
     * Begin rectangle rubber-band selection from the given press point.
     *
     * @param e mouse press event.
     */
    private void startRubberbandSelection(MouseEvent e) {
        Environment.getInstance().setDragging(true);

        rubberband = new Rubberband(owner, this, Rubberband.Policy.RECTANGLE);
        rubberband.setActive(true);
        rubberband.startRubberbanding(e.getPoint());
    }

    /**
     * Handle dragging for item modification.
     * <p>
     * Rubberband selection is driven by {@link Rubberband}'s internal listeners, so
     * this method focuses on item manipulation only.
     * </p>
     *
     * @param ctx tool context (never null).
     * @param e   mouse event.
     */
    @Override
    public void mouseDragged(ToolContext ctx, MouseEvent e) {

        Environment.getInstance().setDragging(true);

        // Rubberband handles its own drag events.
        if (rubberband != null) {
            return;
        }

        if (owner == null) {
            owner = ctx.container();
        }
        if (owner == null) {
            return;
        }

        // Ignore drags outside the component bounds.
        if (owner.getComponent() != null && !owner.getComponent().getBounds().contains(e.getPoint())) {
            return;
        }

        currentPoint.setLocation(e.getPoint());

        if (modifiedItem == null) {
            prevPoint.setLocation(currentPoint);
            return;
        }

        int dx = Math.abs(currentPoint.x - prevPoint.x);
        int dy = Math.abs(currentPoint.y - prevPoint.y);
        if (dx < MIN_DRAG_STEP && dy < MIN_DRAG_STEP) {
            return;
        }

        if (modifying) {
            // Continue modification
            ItemModification mod = modifiedItem.getItemModification();
            if (mod != null) {
                mod.setCurrentMousePoint(currentPoint);
            }
            modifiedItem.modify();

            // Propagate drag modifications to descendants (drag only).
            propagateDragToDescendantsIfNeeded(modifiedItem, currentPoint);

        } else {
            // Start modification once delay has elapsed.
            modifying = (System.currentTimeMillis() - lastPressTimeMillis) > dragDelayMillis;
            if (modifying) {
                ItemModification mod = new ItemModification(
                        modifiedItem, owner, startPoint, currentPoint,
                        e.isShiftDown(), e.isControlDown()
                );
                modifiedItem.setModificationItem(mod);
                modifiedItem.startModification();

                // If the type is DRAG, prepare descendant modifications.
                if (mod.getType() == ItemModification.ModificationType.DRAG) {
                    prepareDescendantDrag(modifiedItem, e);
                }
            }
        }

        prevPoint.setLocation(currentPoint);
    }

    /**
     * Stop modification on release.
     *
     * @param ctx tool context (never null).
     * @param e   mouse event.
     */
    @Override
    public void mouseReleased(ToolContext ctx, MouseEvent e) {
        try {
            // Rubberband completion is handled internally and will call doneRubberbanding().
            if (rubberband != null) {
                return;
            }

            if (modifying && modifiedItem != null) {
                ItemModification mod = modifiedItem.getItemModification();
                if (mod != null) {
                    mod.setCurrentMousePoint(currentPoint);
                }
                modifiedItem.stopModification();
            }
        } finally {
            reset();
            Environment.getInstance().setDragging(false);
        }
    }

    /**
     * Forward a double-click to the item under the cursor (if any).
     *
     * @param ctx tool context (never null).
     * @param e   mouse event.
     */
    @Override
    public void mouseDoubleClicked(ToolContext ctx, MouseEvent e) {
        if (owner == null) {
            owner = ctx.container();
        }
        if (owner == null) {
            return;
        }
     // inside PointerTool.mouseDoubleClicked(...)
        AItem item = ctx.container().getItemAtPoint(e.getPoint());
        if (item != null) {
            // edit the clicked item’s style
            Styled edited = StyleEditorDialog.edit(ctx.container().getComponent(), item.getStyleSafe(), false);
            if (edited != null) {
                item.setStyle(edited);
                item.setDirty(true);
                ctx.container().refresh();
            }
            return;
        }
    }

    /**
     * Called by {@link Rubberband} when rubber-banding completes.
     * <p>
     * Selects items enclosed by the rubber-band rectangle.
     * </p>
     */
    @Override
    public void doneRubberbanding() {
        try {
            if (rubberband == null || owner == null) {
                return;
            }

            Rectangle b = rubberband.getRubberbandBounds();
            rubberband = null;

            owner.selectAllItems(false);

            ArrayList<AItem> enclosed = owner.getEnclosedItems(b);
            if (enclosed != null) {
                for (AItem item : enclosed) {
                    if (item != null && !item.isLocked()) {
                        item.getItemList().selectItem(item, true);
                    }
                }
            }

            requestButtonStateUpdate();
            owner.refresh();

        } finally {
            Environment.getInstance().setDragging(false);
        }
    }

    /**
     * Select items based on a click.
     *
     * @param item clicked item (must not be null).
     * @param e    mouse event.
     */
    private void selectItemsFromClick(AItem item, MouseEvent e) {

        // Only left-click participates in selection changes.
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }

        // Clicking a locked or already-selected item: do nothing.
        if (item.isLocked() || item.isSelected()) {
            return;
        }

        // If Ctrl not held, deselect all first.
        if (!e.isControlDown()) {
            owner.selectAllItems(false);
        }

        // Select the clicked item.
        item.getItemList().selectItem(item, true);

        requestButtonStateUpdate();
        owner.setDirty(true);
        owner.refresh();
    }

    /**
     * Request that the toolbar refresh any selection-dependent enable/disable state
     * (e.g., delete button enabled if any items are selected).
     */
    private void requestButtonStateUpdate() {
        if (activeCtx == null) {
            return;
        }
        BaseToolBar tb = activeCtx.toolBar();
        if (tb != null) {
            tb.updateButtonState();
        }
    }

    /**
     * Reset state for item modification.
     */
    private void reset() {
        modifying = false;
        modifiedItem = null;
    }

    /**
     * If the current modification is a drag, propagate the current mouse point to
     * descendants so they move with the parent.
     *
     * @param item      the modified item.
     * @param currentPt current mouse location.
     */
    private void propagateDragToDescendantsIfNeeded(AItem item, Point currentPt) {
        ItemModification mod = item.getItemModification();
        if (mod == null || mod.getType() != ItemModification.ModificationType.DRAG) {
            return;
        }

        ArrayList<AItem> descendants = item.getAllDescendants();
        if (descendants == null) {
            return;
        }

        for (AItem d : descendants) {
            if (d == null) {
                continue;
            }
            ItemModification dm = d.getItemModification();
            if (dm != null) {
                dm.setCurrentMousePoint(currentPt);
                d.modify();
            }
        }
    }

    /**
     * When beginning a drag modification on a parent item, pre-create drag modifications
     * for descendants so they move together.
     *
     * @param item parent item.
     * @param e    current mouse event (for modifier keys).
     */
    private void prepareDescendantDrag(AItem item, MouseEvent e) {
        ItemModification mod = item.getItemModification();
        if (mod == null || mod.getType() != ItemModification.ModificationType.DRAG) {
            return;
        }

        ArrayList<AItem> descendants = item.getAllDescendants();
        if (descendants == null) {
            return;
        }

        for (AItem d : descendants) {
            if (d == null) {
                continue;
            }
            ItemModification dm = new ItemModification(
                    d, owner, startPoint, currentPoint,
                    e.isShiftDown(), e.isControlDown()
            );
            dm.setType(ItemModification.ModificationType.DRAG);
            d.setModificationItem(dm);
        }
    }
}
