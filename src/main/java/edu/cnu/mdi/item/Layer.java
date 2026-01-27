package edu.cnu.mdi.item;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.event.EventListenerList;

import edu.cnu.mdi.container.IContainer;

/**
 * A z-layer (drawing layer) that owns a list of {@link AItem} objects.
 * <p>
 * MDI rendering model:
 *
 * <pre>
 * View -> Container -> Layers -> Items
 * </pre>
 *
 * Layers are rendered from bottom to top by the owning container.
 *
 * <h2>Visibility vs. locking</h2>
 * <ul>
 * <li><b>Visible</b> layers are drawn and participate in hit-testing.</li>
 * <li><b>Locked</b> layers are still drawn (if visible), but their items are
 * treated as non-interactive: hit-testing returns {@code null}, selection
 * helpers ignore them, and deletion helpers no-op.</li>
 * </ul>
 *
 * <p>
 * <b>Threading:</b> mutating operations synchronize on {@code this} to provide
 * basic safety when tools are mutating items while the UI thread is drawing.
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class Layer extends ArrayList<AItem> {

	/** The owning container. */
	protected IContainer container;

	/** Human-readable name shown in UI (e.g., a layer inspector). */
	protected String name;

	/** Listener list for item change events. */
	private final EventListenerList listenerList = new EventListenerList();

	/** Visibility flag for this layer. */
	protected boolean _visible = true;

	/**
	 * Layer-level lock. When {@code true}, the layer's items are treated as
	 * non-interactive.
	 * <p>
	 * Note: item-level locks still apply independently (see
	 * {@link AItem#isLocked()}).
	 */
	protected boolean _locked = false;

	/**
	 * Create a z-layer for holding items.
	 * <p>
	 * The layer auto-registers itself with the owning container via
	 * {@link IContainer#addLayer(Layer)}.
	 *
	 * @param container the owning container (non-null)
	 * @param name      the name of the layer (non-null)
	 */
	public Layer(IContainer container, String name) {
		super();
		if (container == null) {
			throw new IllegalArgumentException("container cannot be null");
		}
		if (name == null) {
			throw new IllegalArgumentException("name cannot be null");
		}
		this.name = name;
		this.container = container;
		this.container.addLayer(this);
	}

	/**
	 * Add an {@link AItem} to this layer and notify listeners.
	 *
	 * @param item the item to add (may be null, though typically not desired)
	 * @return {@code true} as specified by {@link java.util.Collection#add(Object)}
	 */
	@Override
	public boolean add(AItem item) {
		synchronized (this) {
			super.add(item);
			notifyItemChangeListeners(item, ItemChangeType.ADDED);
		}
		return true;
	}

	/**
	 * Remove an {@link AItem} from this layer and notify listeners.
	 * <p>
	 * If {@code o} is an {@link AItem}, {@link AItem#prepareForRemoval()} is called
	 * first.
	 */
	@Override
	public boolean remove(Object o) {
		if (o instanceof AItem) {
			synchronized (this) {
				AItem item = (AItem) o;
				item.prepareForRemoval();
				boolean result = super.remove(item);
				if (result) {
					notifyItemChangeListeners(item, ItemChangeType.DELETED);
				}
				return result;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Intentionally a no-op. Use {@link #clearAllItems(IContainer)} if you want a
	 * safe "clear" that detaches feedback providers and honors deletable flags.
	 */
	@Override
	public void clear() {
		// no-op by design
	}

	/**
	 * Draw all items on this layer.
	 *
	 * @param g2        the graphics context
	 * @param container the container being rendered
	 */
	public void draw(Graphics2D g2, IContainer container) {
		if (!_visible) {
			return;
		}
		synchronized (this) {
			for (AItem item : this) {
				item.draw(g2, container);
			}
		}
	}

	/**
	 * Add all items at a point (top to bottom) to a supplied list.
	 *
	 * @param items       destination list
	 * @param container   container rendering the item
	 * @param screenPoint point in screen/pixel coordinates
	 */
	public void addItemsAtPoint(ArrayList<AItem> items, IContainer container, Point screenPoint) {
		ArrayList<AItem> itemsAtPoint = getItemsAtPoint(container, screenPoint);
		if (itemsAtPoint != null) {
			items.addAll(itemsAtPoint);
		}
	}

	/**
	 * Add all selected (visible) items on this layer to a supplied list.
	 *
	 * @param items destination list
	 */
	public void addSelectedItems(ArrayList<AItem> items) {
		ArrayList<AItem> selectedItems = getSelectedItems();
		if (selectedItems != null) {
			items.addAll(selectedItems);
		}
	}

	/**
	 * Clear all deletable items from this layer, properly detaching feedback
	 * providers.
	 * <p>
	 * This is safer than {@link #clear()} because items may also be registered with
	 * container-managed services (e.g., feedback control).
	 *
	 * @param container the owning container (may be null; falls back to this
	 *                  layer's container)
	 */
	public void clearAllItems(IContainer container) {
		if (isLocked()) {
			return;
		}

		synchronized (this) {
			ArrayList<AItem> allItems = getAllItems();
			if (allItems == null) {
				clear();
				return;
			}

			for (AItem item : allItems) {
				if (item.isDeletable()) {
					deleteInternal(item, container);
				}
			}
		}
		// do NOT call clear(): would remove non-deletable items without proper detach
	}

	/**
	 * Delete all selected deletable items on this layer.
	 *
	 * @param container the owning container
	 */
	public void deleteSelectedItems(IContainer container) {
		if (!isVisible() || isLocked()) {
			return;
		}

		synchronized (this) {
			ArrayList<AItem> selitems = getSelectedItems();
			if (selitems != null) {
				for (AItem item : selitems) {
					if (item.isDeletable()) {
						deleteInternal(item, container);
					}
				}
			}
		}
	}

	/**
	 * Delete a single item from this layer (and detach feedback provider).
	 *
	 * @param item the item to delete
	 */
	public void deleteItem(AItem item) {
		if (isLocked()) {
			return;
		}
		synchronized (this) {
			deleteInternal(item, (item != null) ? item.getContainer() : container);
		}
	}

	/**
	 * Detach an item from container-managed services (feedback, reference items),
	 * then remove it from this layer.
	 */
	private void deleteInternal(AItem item, IContainer c0) {
		if (item == null) {
			return;
		}

		// Prefer the provided container; otherwise fall back to this layer's container.
		IContainer c = (c0 != null) ? c0 : this.container;

		if (c != null && c.getFeedbackControl() != null) {
			c.getFeedbackControl().removeFeedbackProvider(item);
		}

		remove(item); // calls prepareForRemoval() + notifies DELETED
	}

	/**
	 * Find the topmost item (if any) at the point.
	 * <p>
	 * If this layer is not visible or is locked, returns {@code null}.
	 *
	 * @param container   container rendering the item
	 * @param screenPoint screen/pixel point
	 * @return the topmost item at that location, or {@code null}
	 */
	public AItem getItemAtPoint(IContainer container, Point screenPoint) {
		if (!_visible || _locked) {
			return null;
		}

		synchronized (this) {
			for (int i = size() - 1; i >= 0; i--) {
				AItem item = get(i);
				if (item.isVisible() && item.contains(container, screenPoint)) {
					return item;
				}
			}
		}
		return null;
	}

	/**
	 * Return all items that contain the given point (top-to-bottom).
	 * <p>
	 * If this layer is not visible or is locked, returns {@code null}.
	 *
	 * @param container container rendering the item
	 * @param lp        screen/pixel point
	 * @return matching items (topmost first), or {@code null}
	 */
	public ArrayList<AItem> getItemsAtPoint(IContainer container, Point lp) {
		if (!_visible || _locked) {
			return null;
		}

		ArrayList<AItem> locitems = null;
		synchronized (this) {
			for (int i = size() - 1; i >= 0; i--) {
				AItem item = get(i);
				if (item.isVisible() && item.contains(container, lp)) {
					if (locitems == null) {
						locitems = new ArrayList<>(25);
					}
					locitems.add(item);
				}
			}
		}
		return locitems;
	}

	/**
	 * @return number of selected items on this layer
	 */
	public int getSelectedCount() {
		int count = 0;
		synchronized (this) {
			for (AItem item : this) {
				if (item.isSelected()) {
					++count;
				}
			}
		}
		return count;
	}

	/**
	 * @return {@code true} if at least one item is selected
	 */
	public boolean anySelected() {
		synchronized (this) {
			for (AItem item : this) {
				if (item.isSelected()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Obtain selected items (visible only).
	 * <p>
	 * Note: if the layer is locked, returns an empty list.
	 *
	 * @return selected visible items (never null)
	 */
	public ArrayList<AItem> getSelectedItems() {
		ArrayList<AItem> selitems = new ArrayList<>();
		if (_locked) {
			return selitems;
		}

		synchronized (this) {
			for (AItem item : this) {
				if (item.isSelected() && item.isVisible()) {
					selitems.add(item);
				}
			}
		}
		return selitems;
	}

	/**
	 * Get all items in this layer.
	 *
	 * @return all items (never null)
	 */
	public ArrayList<AItem> getAllItems() {
		ArrayList<AItem> allitems = new ArrayList<>();
		synchronized (this) {
			allitems.addAll(this);
		}
		return allitems;
	}

	/**
	 * Select or deselect all items on this layer.
	 * <p>
	 * If this layer is locked, this is a no-op.
	 *
	 * @param select if {@code true}, select; otherwise deselect
	 */
	public void selectAllItems(boolean select) {
		selectAllItems(select, null);
	}

	/**
	 * Select or deselect all items on this layer except one excluded item.
	 * <p>
	 * If this layer is locked, this is a no-op.
	 *
	 * @param select       new selection state
	 * @param excludedItem optional excluded item (may be null)
	 */
	public void selectAllItems(boolean select, AItem excludedItem) {
		if (_locked) {
			return;
		}
		for (AItem item : this) {
			// respect item-level locks too
			if (!item.isLocked() && item != excludedItem) {
				if (select && !item.isSelected()) {
					item.setSelected(true);
					notifyItemChangeListeners(item, ItemChangeType.SELECTED);
				} else if (!select && item.isSelected()) {
					item.setSelected(false);
					notifyItemChangeListeners(item, ItemChangeType.DESELECTED);
				}
			}
		}
	}

	/**
	 * Select or deselect a single item and notify listeners.
	 * <p>
	 * If this layer is locked, this is a no-op.
	 *
	 * @param item   target item
	 * @param select new selection state
	 */
	public void selectItem(AItem item, boolean select) {

		if (_locked) {
			return;
		}
		if (item != null && !item.isLocked()) {
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
	 * Add all enclosed items within a rectangle to a supplied list.
	 * <p>
	 * If this layer is locked or not visible, nothing is added.
	 *
	 * @param container container rendering the item
	 * @param items     destination list
	 * @param rect      enclosing rectangle
	 */
	public void addEnclosedItems(IContainer container, ArrayList<AItem> items, Rectangle rect) {
		if (!_visible || _locked) {
			return;
		}
		synchronized (this) {
			if (!isEmpty()) {
				ArrayList<AItem> enclosed = getEnclosedItems(container, rect);
				if (enclosed != null) {
					items.addAll(enclosed);
				}
			}
		}
	}

	/**
	 * Get all items enclosed by a rectangle.
	 * <p>
	 * If this layer is locked or not visible, returns {@code null}.
	 *
	 * @param container container rendering the item
	 * @param rect      enclosing rectangle
	 * @return enclosed items, or {@code null}
	 */
	public ArrayList<AItem> getEnclosedItems(IContainer container, Rectangle rect) {
		if (!_visible || _locked) {
			return null;
		}

		synchronized (this) {
			if (!isEmpty()) {
				ArrayList<AItem> encitems = new ArrayList<>();
				for (AItem item : this) {
					if (item.isVisible() && item.enclosed(container, rect)) {
						encitems.add(item);
					}
				}
				return encitems;
			}
			return null;
		}
	}

	/**
	 * @return the owning container
	 */
	public IContainer getContainer() {
		return container;
	}

	/**
	 * Add an {@link ItemChangeListener}. Duplicate listeners are avoided.
	 *
	 * @param itemListener listener to add (may be null)
	 */
	public void addItemChangeListener(ItemChangeListener itemListener) {
		if (itemListener == null) {
			return;
		}
		removeItemChangeListener(itemListener);
		listenerList.add(ItemChangeListener.class, itemListener);
	}

	/**
	 * Remove an {@link ItemChangeListener}.
	 *
	 * @param itemListener listener to remove (may be null)
	 */
	public void removeItemChangeListener(ItemChangeListener itemListener) {
		if (itemListener == null) {
			return;
		}
		listenerList.remove(ItemChangeListener.class, itemListener);
	}

	/**
	 * Notify listeners that an item changed.
	 *
	 * @param item item in question (may be null for some events)
	 * @param type change type
	 */
	public void notifyItemChangeListeners(AItem item, ItemChangeType type) {
		Object[] listeners = listenerList.getListenerList();
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == ItemChangeListener.class) {
				((ItemChangeListener) listeners[i + 1]).itemChanged(this, item, type);
			}
		}
	}

	/**
	 * Move an item one step toward the back (lower draw order).
	 *
	 * @param item item to move
	 */
	public void sendBackward(AItem item) {
		synchronized (this) {
			int src = indexOf(item);
			if (src <= 0) {
				return;
			}
			super.remove(item);
			super.add(src - 1, item);
		}
	}

	/**
	 * Move an item one step toward the front (higher draw order).
	 *
	 * @param item item to move
	 */
	public void sendForward(AItem item) {
		synchronized (this) {
			int src = indexOf(item);
			if (src == -1 || src == (size() - 1)) {
				return;
			}
			super.remove(item);
			super.add(src + 1, item);
		}
	}

	/**
	 * Send an item to the very back (bottom) of this layer.
	 *
	 * @param item item to move
	 */
	public void sendToBack(AItem item) {
		synchronized (this) {
			super.remove(item);
			super.add(0, item);
		}
	}

	/**
	 * Bring an item to the very front (top) of this layer.
	 *
	 * @param item item to move
	 */
	public void sendToFront(AItem item) {
		synchronized (this) {
			super.remove(item);
			super.add(item);
		}
	}

	/**
	 * @return {@code true} if the layer is visible
	 */
	public boolean isVisible() {
		return _visible;
	}

	/**
	 * Set layer visibility.
	 *
	 * @param visible new visibility
	 */
	public void setVisible(boolean visible) {
		this._visible = visible;
	}

	/**
	 * @return {@code true} if the layer is locked (edit locked)
	 */
	public boolean isLocked() {
		return _locked;
	}

	/**
	 * Set layer lock state.
	 *
	 * @param locked new lock state
	 */
	public void setLocked(boolean locked) {
		this._locked = locked;
	}

	/**
	 * Set the display name of this layer.
	 *
	 * @param name new name (non-null)
	 */
	public void setName(String name) {
		if (name == null) {
			throw new IllegalArgumentException("name cannot be null");
		}
		this.name = name;
	}

	/**
	 * @return layer name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Convenience: mark all items dirty/clean.
	 *
	 * @param dirty new dirty state
	 */
	public void setDirty(boolean dirty) {
		synchronized (this) {
			for (AItem item : this) {
				item.setDirty(dirty);
			}
		}
	}

	/**
	 * Equality check: layers are identity objects.
	 */
	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	// -------------------------------------------------------------------------
	// Package-private helpers used by container-level layer management
	// (e.g., moving items between layers without firing ADDED/DELETED events).
	// -------------------------------------------------------------------------

	/**
	 * Add an item without firing change notifications.
	 * <p>
	 * Intended for container-level operations like moving items between layers.
	 */
	void addSilently(AItem item) {
		synchronized (this) {
			super.add(item);
		}
	}

	/**
	 * Remove an item without firing change notifications or calling
	 * prepareForRemoval().
	 * <p>
	 * Intended for container-level operations like moving items between layers.
	 */
	boolean removeSilently(AItem item) {
		synchronized (this) {
			return super.remove(item);
		}
	}
}
