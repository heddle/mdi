package edu.cnu.mdi.item;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.event.EventListenerList;

import edu.cnu.mdi.container.IContainer;


@SuppressWarnings("serial")
public class Layer extends ArrayList<AItem> {

	// the owner container
	protected IContainer container;
	
	// the name of the layer
    protected String name;
	
    // listener list for item change events
	private EventListenerList listenerList = new EventListenerList();

	// visibility flag for this layer
	protected boolean _visible = true;
	
	// lock flag for this layer
	protected boolean _locked = true;
	
	/**
	 * Create a z layer for holding items.
	 *
	 * @param name the name of the layer.
	 */
	public Layer(IContainer container, String name) {
		super();
		this.name = name;
		this.container = container;
		this.container.addLayer(this);
	}
	

	/**
	 * Adds an <code>AItem</code> to this layer.
	 *
	 * @param item the <code>AItem</code> to add.
	 * @return <code>true</code> (as specified by Collection.add(E))
	 */
	@Override
	public boolean add(AItem item) {
		synchronized (this) {
			super.add(item);
			notifyItemChangeListeners(item, ItemChangeType.ADDED);
		}
		return true;
	}

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
	 * Removes all items from this layer.
	 */
	@Override
	public void clear() {
		// no op
	}

	/**
	 * Draw all the <code>AItem</code> objects on this layer.
	 *
	 * @param g2         the graphics context.
	 * @param container the graphic container being rendered.
	 */
	public void draw(Graphics2D g2, IContainer container) {

		// draw nothing if we are not visible.
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
	 * Add all the items that enclose a given point to a collection of
	 * items.
	 *
	 * @param items       the collection we are adding to.
	 * @param container   the graphical container rendering the item.
	 * @param screenPoint the point in question.
	 */
	public void addItemsAtPoint(ArrayList<AItem> items, IContainer container, Point screenPoint) {
		ArrayList<AItem> itemsAtPoint = getItemsAtPoint(container, screenPoint);
		if (itemsAtPoint != null) {
			items.addAll(itemsAtPoint);
		}
	}

	/**
	 * Add all the selected items to an Items collection.
	 *
	 * @param items the list to which we will add all selected items on this
	 *              layer.
	 */
	public void addSelectedItems(ArrayList<AItem> items) {
		ArrayList<AItem> selectedItems = getSelectedItems();
		if (selectedItems != null) {
			items.addAll(selectedItems);
		}
	}

	/**
	 * Clears all the items. Not as simple as it appears. The main gotcha is that
	 * items were probably added to the container's feedback control as feedback
	 * providers. If they are not removed they will continue to produce feedback
	 * (and will not be garbage collected). So, we must remove them from the feedback control.
	 * @param container
	 */
	public void clearAllItems(IContainer container) {
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

	    // Do NOT call clear() here: it would remove any remaining non-deletable items
	    // without detaching feedback, and it also emits LISTCLEARED which can be misleading.
	}

	/**
	 * Deletes all selected (visible) items. Deleting simply means
	 * removing them from the layer. They will no longer be drawn. Items that are not
	 * deletable are not removed.
	 *
	 * @param container the container they lived on.
	 */
	public void deleteSelectedItems(IContainer container) {

	    if (!isVisible()) {
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
	 * Delete a single item, making sure to remove it from the feedback listener
	 * list.
	 *
	 * @param item the item to remove
	 */
	public void deleteItem(AItem item) {
	    synchronized (this) {
	        deleteInternal(item, (item != null) ? item.getContainer() : container);
	    }
	}
	
	/**
	 * Detach an item from container-managed services (feedback, reference items),
	 * then remove it from this layer.
	 */
	private void deleteInternal(AItem item, IContainer container) {
	    if (item == null) {
	        return;
	    }

	    IContainer c = (container != null) ? container : container;
	    if (c != null) {
	        c.getFeedbackControl().removeFeedbackProvider(item);
	    }

	    remove(item); // calls prepareForRemoval() + notifies REMOVED
	}


	/**
	 * Find the topmost item, if any, at the point, probably a mouse location.
	 *
	 * @param container   the graphical container rendering the item.
	 * @param screenPoint the point in question.
	 * @return the topmost item at that location, or <code>null</code>.
	 */
	public AItem getItemAtPoint(IContainer container, Point screenPoint) {

		synchronized (this) {

			for (int i = size() - 1; i >= 0; i--) {
				AItem item = (AItem) get(i);
				if (item.isVisible() && item.contains(container, screenPoint)) {
					return item;
				}
			}

		}
		return null;
	}

	/**
	 * Returns all items that contain the given point. The items are returned in
	 * reverse order, from top to bottom.
	 *
	 * @param container the graphical container rendering the item.
	 * @param lp        the point in question.
	 * @return all items that contain the given point. If any, the topmost will be
	 *         the first entry.
	 */
	public ArrayList<AItem> getItemsAtPoint(IContainer container, Point lp) {

		ArrayList<AItem> locitems = null;

		synchronized (this) {

			for (int i = size() - 1; i >= 0; i--) {
				AItem item = (AItem) get(i);
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
	 * Count how many items are selected.
	 *
	 * @return the number of selected items.
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
	 * Check whether at least one item is selected.
	 *
	 * @return <code>true</code> if at least one item is selected.
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
	 * Obtain a collection of selected items.
	 *
	 * @return all selected items.
	 */
	public ArrayList<AItem> getSelectedItems() {

		ArrayList<AItem> selitems = new ArrayList<>();

		synchronized (this) {
			for (AItem item : this) {
				if (item.isSelected() && item.isVisible()) {
					selitems.add((AItem) item);
				}
			}
		}
		return selitems;
	}

	/**
	 * Get all the items into a collection
	 */
	public ArrayList<AItem> getAllItems() {

		ArrayList<AItem> allitems = new ArrayList<>();

		synchronized (this) {
			for (AItem item : this) {
				allitems.add(item);
			}
		}
		return allitems;
	}

	/**
	 * Select (or deselect) all items.
	 *
	 * @param select if <code>true</code> select, otherwise deselect.
	 */
	public void selectAllItems(boolean select) {
		selectAllItems(select, null);
	}

	/**
	 * Select (or deselect) all item excepting a single specified item.
	 *
	 * @param select       if <code>true</code> select, otherwise deselect.
	 * @param excludedItem optional Item to be excluded from the operation, may be
	 *                     null.
	 */
	public void selectAllItems(boolean select, AItem excludedItem) {
		for (AItem item : this) {
			if ((!item.isLocked()) && (item != excludedItem)) {

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
	 * Select or deselect a single item and send the notification.
	 *
	 * @param item   the item in question.
	 * @param select the new select state.
	 */
	public void selectItem(AItem item, boolean select) {
		if ((item != null) && (!item.isLocked())) {
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
	 * Add all the enclosed items to a collection
	 *
	 * @param container the container being rendered.
	 * @param items     the collection we are adding to.
	 * @param rect      the enclosing rectangle.
	 */
	public void addEnclosedItems(IContainer container, ArrayList<AItem> items, Rectangle rect) {

		synchronized (this) {
			if (size() > 0) {
				items.addAll(getEnclosedItems(container, rect));
			}
		}
	}

	/**
	 * Get all the items enclosed by a rectangle.
	 *
	 * @param container the container being rendered.
	 * @param rect      the rectangle in question.
	 */
	public ArrayList<AItem> getEnclosedItems(IContainer container, Rectangle rect) {

		synchronized (this) {
			if (size() > 0) {
				ArrayList<AItem> encitems = new ArrayList<>();
				for (AItem item : this) {
					if (item.isVisible() && item.isEnclosed(container, rect)) {
						encitems.add(item);
					}
				}
				return encitems;
			}
			return null;
		}
	}

	/**
	 * Get the container for this list.
	 *
	 * @return the container.
	 */
	public IContainer getContainer() {
		return container;
	}

	
	/**
	 * Add an <code>ItemChangeListener</code>.
	 *
	 * @see ItemChangeListener
	 * @param itemListener the <code>ItemChangeListener</code> to add.
	 */
	public void addItemChangeListener(ItemChangeListener itemListener) {
		
		if (itemListener == null) {
			return;
		}

		// avoid adding duplicates
		removeItemChangeListener(itemListener);
		listenerList.add(ItemChangeListener.class, itemListener);
	}

	/**
	 * Remove an <code>ItemChangeListener</code>.
	 *
	 * @see ItemChangeListener
	 * @param itemListener the <code>ItemChangeListener</code> to remove.
	 */
	public void removeItemChangeListener(ItemChangeListener itemListener) {
		
		if (itemListener == null) {
			return;
		}

		listenerList.remove(ItemChangeListener.class, itemListener);
	}

	/**
	 * Notify interested parties that an <code>AItem</code> has changed
	 *
	 * @param item the <code>AItem</code> in question.
	 * @param type the type of change, e.g. one of the enum constants in the
	 *             <code>ItemChangeType</code> class.
	 */
	public void notifyItemChangeListeners(AItem item, ItemChangeType type) {

		if (listenerList == null) {
			System.out.println("Layer: No listeners to notify.");
			return;
		}

		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();

		// Process the listeners last to first, notifying
		// those that are interested in this event

		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == ItemChangeListener.class) {
				((ItemChangeListener) listeners[i + 1]).itemChanged(this, item, type);
			}
		}
	}
	

	/**
	 * Move an item upward in the list which has the effect of sending it
	 * backward when drawn.
	 *
	 * @param item the item to move backward.
	 */
	public void sendBackward(AItem item) {

		synchronized (this) {
			int src = indexOf(item);
			if (src <= 0) {
				return;
			}
			// use super methods so that notifiers are not called
			super.remove(item);
			super.add(src - 1, item);
		}
	}

	/**
	 * Move an item downward in the list which has the effect of sending it
	 * forward when drawn.
	 *
	 * @param item the item to move backward.
	 */
	public void sendForward(AItem item) {

		synchronized (this) {
			int src = indexOf(item);
			if (src == -1 || src == (size() - 1)) {
				return;
			}
			// use super methods so that notifiers are not called
			super.remove(item);
			super.add(src + 1, item);
		}
	}

	/**
	 * Put an <code>AItem</code> at the top of the list, which has the effect of
	 * sending to the back when drawn.
	 *
	 * @param item the <code>AItem</code> to put at the beginning of the
	 *                 list, which will result in being drawn on the bottom.
	 */
	public void sendToBack(AItem item) {

		synchronized (this) {
			// use super methods so that notifiers are not called
			super.remove(item);
			super.add(0, item);
		}
	}

	/**
	 * Put an <code>AItem</code> at the bottom of the list, which has the effect
	 * of sending to the front when drawn.
	 *
	 * @param item the <code>AItem</code> to put at the end of the list,
	 *                 which will result in being drawn on top.
	 */
	public void sendToFront(AItem item) {

		synchronized (this) {
			// use super methods so that notifiers are not called
			super.remove(item);
			super.add(item);
		}
	}

	/**
	 * Check whether this list is marked as visible.
	 *
	 * @return <code>true</code> is this list is marked as visible.
	 */
	public boolean isVisible() {
		return _visible;
	}

	/**
	 * Sets the visibility flag.
	 *
	 * @param visible the new value of the flag.
	 */
	public void setVisible(boolean visible) {
		if (visible == this._visible) {
			return;
		}
		this._visible = visible;
	}

	/**
	 * Check whether this list is marked as locked.
	 *
	 * @return <code>true</code> is this list is marked as locked.
	 */
	public boolean isLocked() {
		return _locked;
	}

	/**
	 * Sets the lock flag.
	 *
	 * @param locked the new value of the flag
	 */
	public void setLocked(boolean locked) {
		this._locked = locked;
	}

	/**
	 * Set the name for this layer.
	 *
	 * @param name the name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Get the name of this z layer.
	 *
	 * @return the name of the layer.
	 */
	public String getName() {
	    return name;
	}

	/**
	 * Convenience routine to set the dirty property for all items on this layer. A
	 * dirty state is a signal that some cached calculations relevant for display
	 * need to be redone. By careful use of the dirty states, expensive calculations
	 * can be performed only when needed. The danger is that something that makes
	 * the items "dirty" gets missed.
	 *
	 * @param dirty the value to set for the <code>dirty</code> flag.
	 */
	public void setDirty(boolean dirty) {

		synchronized (this) {
			for (AItem item : this) {
				item.setDirty(dirty);
			}
		}
	}


	/**
	 * Equality check.
	 *
	 * @return <code>true</code> if objects are equal.
	 */
	@Override
	public boolean equals(Object o) {

		if ((o != null) && (o instanceof Layer)) {
			return (this == o);
		}
		return false;
	}

}
