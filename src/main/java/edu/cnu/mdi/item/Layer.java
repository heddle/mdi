package edu.cnu.mdi.item;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.event.EventListenerList;

import edu.cnu.mdi.container.IContainer;

/**
 * A z-layer (drawing layer) that owns an ordered list of {@link AItem} objects.
 *
 * <h2>Role in the MDI rendering model</h2>
 * <pre>
 *   View → Container → Layers → Items
 * </pre>
 * <p>
 * Layers are rendered bottom-to-top by the owning container. The layer manages
 * item ordering, selection state, hit-testing, and notifies registered
 * {@link ItemChangeListener}s when items are added, removed, or selected.
 * </p>
 *
 * <h2>Visibility vs. locking</h2>
 * <ul>
 *   <li><b>Visible</b> layers are drawn and participate in hit-testing.</li>
 *   <li><b>Locked</b> layers are still drawn (if visible) but their items are
 *       treated as non-interactive: hit-testing returns {@code null}, selection
 *       helpers are no-ops, and deletion helpers skip locked layers.</li>
 * </ul>
 *
 * <h2>Why delegation instead of inheritance</h2>
 * <p>
 * This class delegates to an internal {@link ArrayList} rather than extending
 * one. Extending {@code ArrayList} would expose the full {@code List} mutation
 * API ({@code add(int, E)}, {@code set()}, {@code addAll()}, etc.) to callers,
 * making it trivial to bypass the change-notification logic in {@link #add} and
 * {@link #remove}. The delegation approach exposes only the operations this
 * layer actually supports, each of which fires the correct event.
 * </p>
 *
 * <h2>Thread safety</h2>
 * <p>
 * Mutating operations synchronize on {@code this} to provide basic safety when
 * background threads modify items while the Swing EDT is painting.
 * </p>
 *
 * @see AItem
 * @see ItemChangeListener
 * @see IContainer
 */
public class Layer {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** The owning container — never {@code null} after construction. */
    private final IContainer container;

    /** Human-readable name shown in layer inspectors and menus. */
    private String name;

    /**
     * The ordered list of items on this layer. Items are drawn and iterated
     * bottom-to-top (index 0 is drawn first, the last index is on top).
     * <p>
     * All accesses that read or mutate this list must synchronize on
     * {@code this}.
     */
    private final ArrayList<AItem> items = new ArrayList<>();

    /** Registered listeners for item change events. */
    private final EventListenerList listenerList = new EventListenerList();

    /** Whether this layer is drawn and hit-tested. */
    private boolean visible = true;

    /**
     * Layer-level lock.  When {@code true} all items on this layer behave as
     * non-interactive regardless of their own {@link AItem#isLocked()} flag.
     * The layer is still drawn if {@link #visible} is {@code true}.
     */
    private boolean locked = false;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Create a z-layer and register it with the owning container.
     * <p>
     * The layer auto-registers itself via {@link IContainer#addLayer(Layer)}.
     * </p>
     *
     * @param container the owning container; must not be {@code null}
     * @param name      a human-readable layer name; must not be {@code null}
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public Layer(IContainer container, String name) {
        if (container == null) throw new IllegalArgumentException("container cannot be null");
        if (name == null)      throw new IllegalArgumentException("name cannot be null");
        this.container = container;
        this.name      = name;
        container.addLayer(this);
    }

    // -------------------------------------------------------------------------
    // Item collection — mutators
    // -------------------------------------------------------------------------

    /**
     * Add an item to the top of this layer and notify listeners.
     *
     * <p><b>Note:</b> items normally add themselves to their layer inside
     * {@link AItem#AItem(Layer, Object...)}. Calling this method directly is
     * only needed for unusual container-level moves; prefer the two-step
     * {@link #addRaw} + {@link #notifyItemChangeListeners} pair in those cases
     * to control whether an event is fired.</p>
     *
     * @param item the item to add; should not be {@code null}
     */
    public void add(AItem item) {
        synchronized (this) {
            items.add(item);
        }
        notifyItemChangeListeners(item, ItemChangeType.ADDED);
    }

    /**
     * Remove an item from this layer.
     * <p>
     * {@link AItem#prepareForRemoval()} is called on the item before it is
     * removed from the backing list, and a {@link ItemChangeType#DELETED} event
     * is fired after successful removal.
     * </p>
     *
     * @param item the item to remove; if {@code null} or not on this layer the
     *             call is a no-op
     * @return {@code true} if the item was found and removed
     */
    public boolean remove(AItem item) {
        if (item == null) return false;
        boolean removed;
        synchronized (this) {
            item.prepareForRemoval();
            removed = items.remove(item);
        }
        if (removed) {
            notifyItemChangeListeners(item, ItemChangeType.DELETED);
        }
        return removed;
    }

    /**
     * Delete all deletable items from this layer, properly detaching each from
     * container-managed services (feedback control, etc.).
     * <p>
     * If this layer is locked the call is a no-op.
     * </p>
     *
     * @param container the owning container; if {@code null} falls back to
     *                  this layer's own container reference
     */
    public void clearAllItems(IContainer container) {
        if (locked) return;
        synchronized (this) {
            List<AItem> snapshot = new ArrayList<>(items);
            for (AItem item : snapshot) {
                if (item.isDeletable()) {
                    deleteInternal(item, container);
                }
            }
        }
    }

    /**
     * Delete all currently selected, deletable items.
     * <p>
     * If this layer is locked or not visible the call is a no-op.
     * </p>
     *
     * @param container the owning container
     */
    public void deleteSelectedItems(IContainer container) {
        if (!visible || locked) return;
        synchronized (this) {
            List<AItem> snapshot = new ArrayList<>(items);
            for (AItem item : snapshot) {
                if (item.isSelected() && item.isDeletable()) {
                    deleteInternal(item, container);
                }
            }
        }
    }

    /**
     * Delete a single item from this layer.
     * <p>
     * If this layer is locked the call is a no-op. The item is detached from
     * the container's feedback control and {@link AItem#prepareForRemoval()} is
     * called before removal.
     * </p>
     *
     * @param item the item to delete; if {@code null} the call is a no-op
     */
    public void deleteItem(AItem item) {
        if (locked || item == null) return;
        synchronized (this) {
            deleteInternal(item, item.getContainer());
        }
    }

    /**
     * Internal deletion: detaches the item from the container's feedback
     * control, then delegates to {@link #remove(AItem)}.
     */
    private void deleteInternal(AItem item, IContainer c) {
        IContainer effective = (c != null) ? c : this.container;
        if (effective != null && effective.getFeedbackControl() != null) {
            effective.getFeedbackControl().removeFeedbackProvider(item);
        }
        remove(item);   // fires DELETED + calls prepareForRemoval
    }

    // -------------------------------------------------------------------------
    // Item collection — read-only queries
    // -------------------------------------------------------------------------

    /**
     * Return the number of items on this layer.
     *
     * @return item count (≥ 0)
     */
    public int size() {
        synchronized (this) { return items.size(); }
    }

    /**
     * Return {@code true} if this layer contains no items.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        synchronized (this) { return items.isEmpty(); }
    }

    /**
     * Return {@code true} if this layer contains the given item.
     *
     * @param item the item to test; if {@code null} returns {@code false}
     * @return {@code true} if {@code item} is on this layer
     */
    public boolean contains(AItem item) {
        if (item == null) return false;
        synchronized (this) { return items.contains(item); }
    }

    /**
     * Return a snapshot of all items on this layer, in draw order
     * (bottom-to-top).
     * <p>
     * The returned list is a defensive copy — modifications to it do not affect
     * this layer.
     * </p>
     *
     * @return a new, mutable {@link ArrayList} of all items; never {@code null}
     */
    public ArrayList<AItem> getAllItems() {
        synchronized (this) { return new ArrayList<>(items); }
    }

    /**
     * Return an unmodifiable view of the items list for read-only iteration.
     * <p>
     * Unlike {@link #getAllItems()} this does <em>not</em> copy the list, but
     * callers must not call methods that would mutate the layer while iterating.
     * Prefer {@link #getAllItems()} when iteration may interleave with mutation.
     * </p>
     *
     * @return an unmodifiable view of the item list
     */
    public List<AItem> itemsView() {
        return Collections.unmodifiableList(items);
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    /**
     * Draw all items on this layer in bottom-to-top order.
     * <p>
     * If this layer is not visible the method returns immediately.
     * </p>
     *
     * @param g2        the graphics context
     * @param container the container being rendered
     */
    public void draw(Graphics2D g2, IContainer container) {
        if (!visible) return;
        synchronized (this) {
            for (AItem item : items) {
                item.draw(g2, container);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Hit testing
    // -------------------------------------------------------------------------

    /**
     * Return the topmost visible item that contains the given screen point.
     * <p>
     * Returns {@code null} if this layer is not visible or is locked.
     * </p>
     *
     * @param container   the container performing the hit test
     * @param screenPoint the pixel location to test
     * @return the topmost matching item, or {@code null}
     */
    public AItem getItemAtPoint(IContainer container, Point screenPoint) {
        if (!visible || locked) return null;
        synchronized (this) {
            for (int i = items.size() - 1; i >= 0; i--) {
                AItem item = items.get(i);
                if (item.isVisible() && item.contains(container, screenPoint)) {
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * Return all visible items that contain the given screen point, topmost
     * first.
     * <p>
     * Returns {@code null} if this layer is not visible or is locked, or if no
     * items match.
     * </p>
     *
     * @param container   the container performing the hit test
     * @param screenPoint the pixel location to test
     * @return matching items (topmost first), or {@code null}
     */
    public ArrayList<AItem> getItemsAtPoint(IContainer container, Point screenPoint) {
        if (!visible || locked) return null;
        ArrayList<AItem> result = null;
        synchronized (this) {
            for (int i = items.size() - 1; i >= 0; i--) {
                AItem item = items.get(i);
                if (item.isVisible() && item.contains(container, screenPoint)) {
                    if (result == null) result = new ArrayList<>();
                    result.add(item);
                }
            }
        }
        return result;
    }

    /**
     * Append all visible items contained within the given rectangle to
     * {@code dest}.
     * <p>
     * Does nothing if this layer is locked or not visible.
     * </p>
     *
     * @param container the container performing the test
     * @param dest      destination list; items are appended to it
     * @param rect      the enclosing rectangle in screen coordinates
     */
    public void addEnclosedItems(IContainer container, ArrayList<AItem> dest, Rectangle rect) {
        if (!visible || locked) return;
        synchronized (this) {
            for (AItem item : items) {
                if (item.isVisible() && item.enclosed(container, rect)) {
                    dest.add(item);
                }
            }
        }
    }

    /**
     * Append all visible items at the given screen point to {@code dest}.
     *
     * @param dest        destination list; items are appended to it
     * @param container   the container performing the hit test
     * @param screenPoint the pixel location to test
     */
    public void addItemsAtPoint(ArrayList<AItem> dest, IContainer container, Point screenPoint) {
        ArrayList<AItem> atPoint = getItemsAtPoint(container, screenPoint);
        if (atPoint != null) dest.addAll(atPoint);
    }

    // -------------------------------------------------------------------------
    // Selection
    // -------------------------------------------------------------------------

    /**
     * Return {@code true} if at least one item on this layer is selected.
     *
     * @return {@code true} if any item is selected
     */
    public boolean anySelected() {
        synchronized (this) {
            for (AItem item : items) {
                if (item.isSelected()) return true;
            }
        }
        return false;
    }

    /**
     * Return the number of selected items on this layer.
     *
     * @return selected item count (≥ 0)
     */
    public int getSelectedCount() {
        int count = 0;
        synchronized (this) {
            for (AItem item : items) {
                if (item.isSelected()) count++;
            }
        }
        return count;
    }

    /**
     * Return a snapshot of all selected, visible items on this layer.
     *
     * @return selected items; never {@code null} but may be empty
     */
    public ArrayList<AItem> getSelectedItems() {
        ArrayList<AItem> result = new ArrayList<>();
        synchronized (this) {
            for (AItem item : items) {
                if (item.isSelected() && item.isVisible()) result.add(item);
            }
        }
        return result;
    }

    /**
     * Append all selected, visible items to {@code dest}.
     *
     * @param dest destination list; items are appended to it
     */
    public void addSelectedItems(ArrayList<AItem> dest) {
        dest.addAll(getSelectedItems());
    }

    /**
     * Select or deselect all items on this layer.
     * <p>
     * If this layer is locked the call is a no-op.
     * </p>
     *
     * @param select {@code true} to select, {@code false} to deselect
     */
    public void selectAllItems(boolean select) {
        selectAllItems(select, null);
    }

    /**
     * Select or deselect all items on this layer, optionally excluding one.
     * <p>
     * Both the layer lock and each item's own lock are respected.
     * If this layer is locked the call is a no-op.
     * </p>
     *
     * @param select       new selection state
     * @param excludedItem item to skip; may be {@code null}
     */
    public void selectAllItems(boolean select, AItem excludedItem) {
        if (locked) return;
        for (AItem item : getAllItems()) {    // snapshot — safe to iterate
            if (item.isLocked() || item == excludedItem) continue;
            if (select && !item.isSelected()) {
                item.setSelected(true);
                notifyItemChangeListeners(item, ItemChangeType.SELECTED);
            } else if (!select && item.isSelected()) {
                item.setSelected(false);
                notifyItemChangeListeners(item, ItemChangeType.DESELECTED);
            }
        }
    }

    /**
     * Select or deselect a single item and notify listeners.
     * <p>
     * If this layer is locked, or the item itself is locked, the call is a
     * no-op.
     * </p>
     *
     * @param item   the target item; if {@code null} the call is a no-op
     * @param select new selection state
     */
    public void selectItem(AItem item, boolean select) {
        if (locked || item == null || item.isLocked()) return;
        if (select && !item.isSelected()) {
            item.setSelected(true);
            notifyItemChangeListeners(item, ItemChangeType.SELECTED);
        } else if (!select && item.isSelected()) {
            item.setSelected(false);
            notifyItemChangeListeners(item, ItemChangeType.DESELECTED);
        }
    }

    // -------------------------------------------------------------------------
    // Z-ordering
    // -------------------------------------------------------------------------

    /**
     * Bring an item to the very front (top draw order) of this layer.
     *
     * @param item the item to move; no-op if not on this layer
     */
    public void sendToFront(AItem item) {
        synchronized (this) {
            if (items.remove(item)) items.add(item);
        }
    }

    /**
     * Send an item to the very back (bottom draw order) of this layer.
     *
     * @param item the item to move; no-op if not on this layer
     */
    public void sendToBack(AItem item) {
        synchronized (this) {
            if (items.remove(item)) items.add(0, item);
        }
    }

    /**
     * Move an item one step toward the front (higher draw order).
     *
     * @param item the item to move; no-op if not on this layer or already at front
     */
    public void sendForward(AItem item) {
        synchronized (this) {
            int src = items.indexOf(item);
            if (src < 0 || src == items.size() - 1) return;
            items.remove(src);
            items.add(src + 1, item);
        }
    }

    /**
     * Move an item one step toward the back (lower draw order).
     *
     * @param item the item to move; no-op if not on this layer or already at back
     */
    public void sendBackward(AItem item) {
        synchronized (this) {
            int src = items.indexOf(item);
            if (src <= 0) return;
            items.remove(src);
            items.add(src - 1, item);
        }
    }

    // -------------------------------------------------------------------------
    // Dirty / refresh helpers
    // -------------------------------------------------------------------------

    /**
     * Mark all items on this layer dirty or clean.
     *
     * @param dirty new dirty state for each item
     */
    public void setDirty(boolean dirty) {
        synchronized (this) {
            for (AItem item : items) item.setDirty(dirty);
        }
    }

    // -------------------------------------------------------------------------
    // Change listener management
    // -------------------------------------------------------------------------

    /**
     * Register an {@link ItemChangeListener}.
     * <p>
     * Duplicate registrations are silently ignored (the listener is removed
     * then re-added to avoid duplicates).
     * </p>
     *
     * @param listener the listener to add; if {@code null} the call is a no-op
     */
    public void addItemChangeListener(ItemChangeListener listener) {
        if (listener == null) return;
        listenerList.remove(ItemChangeListener.class, listener);   // dedupe
        listenerList.add(ItemChangeListener.class, listener);
    }

    /**
     * Remove a previously registered {@link ItemChangeListener}.
     *
     * @param listener the listener to remove; if {@code null} the call is a no-op
     */
    public void removeItemChangeListener(ItemChangeListener listener) {
        if (listener == null) return;
        listenerList.remove(ItemChangeListener.class, listener);
    }

    /**
     * Notify all registered listeners that an item changed.
     *
     * @param item the affected item; may be {@code null} for layer-level events
     * @param type the type of change
     */
    public void notifyItemChangeListeners(AItem item, ItemChangeType type) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ItemChangeListener.class) {
                ((ItemChangeListener) listeners[i + 1]).itemChanged(this, item, type);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Return the owning container.
     *
     * @return the container that owns this layer; never {@code null}
     */
    public IContainer getContainer() { return container; }

    /**
     * Return the human-readable name of this layer.
     *
     * @return the layer name; never {@code null}
     */
    public String getName() { return name; }

    /**
     * Set the human-readable name of this layer.
     *
     * @param name the new name; must not be {@code null}
     * @throws IllegalArgumentException if {@code name} is {@code null}
     */
    public void setName(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        this.name = name;
    }

    /**
     * Return {@code true} if this layer is visible.
     *
     * @return the visibility flag
     */
    public boolean isVisible() { return visible; }

    /**
     * Set the visibility of this layer.
     *
     * @param visible {@code true} to make the layer visible
     */
    public void setVisible(boolean visible) { this.visible = visible; }

    /**
     * Return {@code true} if this layer is locked (edit-locked).
     * <p>
     * A locked layer is still drawn if {@link #isVisible()} returns
     * {@code true}, but its items are treated as non-interactive.
     * </p>
     *
     * @return the locked flag
     */
    public boolean isLocked() { return locked; }

    /**
     * Set the locked state of this layer.
     *
     * @param locked {@code true} to lock the layer
     */
    public void setLocked(boolean locked) { this.locked = locked; }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    /**
     * Identity-based equality: two layer references are equal only when they
     * are the same object.
     *
     * @param o the object to compare
     * @return {@code true} only if {@code o == this}
     */
    @Override
    public boolean equals(Object o) { return this == o; }

    /** {@inheritDoc} */
    @Override
    public int hashCode() { return System.identityHashCode(this); }

    /**
     * Return a human-readable description of this layer.
     *
     * @return a string of the form {@code "Layer[name, N items]"}
     */
    @Override
    public String toString() {
        return "Layer[" + name + ", " + size() + " items]";
    }

    // -------------------------------------------------------------------------
    // Package-private helpers for container-level layer moves
    // -------------------------------------------------------------------------

    /**
     * Add an item directly to the backing list <em>without</em> firing a change
     * notification or calling {@link AItem#prepareForRemoval()}.
     * <p>
     * <b>For use only by container-level code</b> that moves items between
     * layers atomically (e.g. changing layer assignment) and fires its own
     * events. Do not use in application code.
     * </p>
     *
     * @param item the item to add silently
     */
    void addRaw(AItem item) {
        synchronized (this) { items.add(item); }
    }

    /**
     * Remove an item directly from the backing list <em>without</em> firing a
     * change notification or calling {@link AItem#prepareForRemoval()}.
     * <p>
     * <b>For use only by container-level code</b> that moves items between
     * layers atomically. Do not use in application code.
     * </p>
     *
     * @param item the item to remove silently
     * @return {@code true} if the item was present and removed
     */
    boolean removeRaw(AItem item) {
        synchronized (this) { return items.remove(item); }
    }
}