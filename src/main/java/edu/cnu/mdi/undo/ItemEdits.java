package edu.cnu.mdi.undo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemState;
import edu.cnu.mdi.item.Layer;

/**
 * Factories for the undoable edits an interactive container records: changing
 * items' state (move, resize, rotate, restyle, edit text), creating an item,
 * and deleting items. Each is a {@link javax.swing.undo.UndoableEdit} for a
 * {@link javax.swing.undo.UndoManager}.
 *
 * <h2>Deletion keeps the item alive</h2>
 * <p>Undoable deletion takes an item out of its layer
 * ({@link Layer#detach(AItem)}) but does not dispose it, so undoing puts back
 * the very same object with its geometry, style and identity intact. The item
 * is disposed ({@link AItem#prepareForRemoval()}) only when its edit leaves the
 * history for good, such as when the history is cleared or trimmed to its
 * limit, so nothing leaks. Creation works the same way in reverse: undoing a
 * creation detaches the new item and keeps it for redo.</p>
 *
 * <p>All edits must be made and undone on the Swing event thread.</p>
 */
public final class ItemEdits {

	private ItemEdits() {
	}

	// ------------------------------------------------------------------
	// State changes
	// ------------------------------------------------------------------

	/**
	 * Snapshots the state of each item, for later use with
	 * {@link #changedStates}.
	 *
	 * @param items the items to snapshot
	 * @return one entry per item, in order; an entry is {@code null} for an item
	 *         that does not support snapshots ({@link AItem#captureState()})
	 */
	public static List<ItemState> capture(List<? extends AItem> items) {
		List<ItemState> states = new ArrayList<>(items.size());
		for (AItem item : items) {
			states.add(item.captureState());
		}
		return states;
	}

	/**
	 * Builds the edit that takes the items from the given earlier states to their
	 * current states, leaving out items that cannot be snapshotted and items that
	 * did not change.
	 *
	 * @param name      the edit's name as shown in an Undo menu, such as "Move"
	 * @param container the container to repaint after undo/redo
	 * @param items     the items, in the same order as {@code before}
	 * @param before    the states captured earlier with {@link #capture}
	 * @return the edit, or {@code null} if nothing changed
	 */
	public static UndoableEdit changedStates(String name, IContainer container, List<? extends AItem> items,
			List<ItemState> before) {
		Objects.requireNonNull(items, "items");
		Objects.requireNonNull(before, "before");
		if (before.size() != items.size()) {
			throw new IllegalArgumentException("before must have one entry per item");
		}

		List<AItem> changed = new ArrayList<>();
		List<ItemState> was = new ArrayList<>();
		List<ItemState> now = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			AItem item = items.get(i);
			ItemState b = before.get(i);
			ItemState a = (b == null) ? null : item.captureState();
			if (a != null && !a.sameAs(b)) {
				changed.add(item);
				was.add(b);
				now.add(a);
			}
		}
		return changed.isEmpty() ? null : new StateChange(name, container, changed, was, now);
	}

	private static final class StateChange extends AbstractUndoableEdit {
		private static final long serialVersionUID = 1L;

		private final String name;
		private final transient IContainer container;
		private final transient List<AItem> items;
		private final transient List<ItemState> before;
		private final transient List<ItemState> after;

		StateChange(String name, IContainer container, List<AItem> items, List<ItemState> before,
				List<ItemState> after) {
			this.name = name;
			this.container = container;
			this.items = items;
			this.before = before;
			this.after = after;
		}

		@Override
		public void undo() throws CannotUndoException {
			super.undo();
			apply(before);
		}

		@Override
		public void redo() throws CannotRedoException {
			super.redo();
			apply(after);
		}

		private void apply(List<ItemState> states) {
			for (int i = 0; i < items.size(); i++) {
				items.get(i).restoreState(states.get(i));
			}
			refresh(container);
		}

		@Override
		public String getPresentationName() {
			return name;
		}
	}

	// ------------------------------------------------------------------
	// Creation
	// ------------------------------------------------------------------

	/**
	 * Builds the edit for an item that has just been created on its layer.
	 * Undoing detaches the item and keeps it for redo.
	 *
	 * @param name      the edit's name, such as "Create Rectangle"
	 * @param container the container to repaint after undo/redo
	 * @param item      the new item, already on its layer
	 * @return the edit
	 */
	public static UndoableEdit created(String name, IContainer container, AItem item) {
		Objects.requireNonNull(item, "item");
		return new Created(name, container, item);
	}

	private static final class Created extends AbstractUndoableEdit {
		private static final long serialVersionUID = 1L;

		private final String name;
		private final transient IContainer container;
		private final transient AItem item;
		private final transient Layer layer;
		private int index = -1;

		Created(String name, IContainer container, AItem item) {
			this.name = name;
			this.container = container;
			this.item = item;
			this.layer = item.getLayer();
		}

		@Override
		public void undo() throws CannotUndoException {
			super.undo();
			index = layer.detach(item);
			refresh(container);
		}

		@Override
		public void redo() throws CannotRedoException {
			super.redo();
			layer.reattach(item, index);
			refresh(container);
		}

		@Override
		public void die() {
			boolean detached = canRedo(); // undone: the item is parked, off its layer
			super.die();
			if (detached) {
				item.prepareForRemoval();
			}
		}

		@Override
		public String getPresentationName() {
			return name;
		}
	}

	// ------------------------------------------------------------------
	// Deletion
	// ------------------------------------------------------------------

	/**
	 * Deletes the items from their layers undoably and returns the edit that
	 * brings them back. The items are detached, not disposed (see the class
	 * description).
	 *
	 * @param container the container to repaint after undo/redo
	 * @param items     the items to delete; each is deleted from its own layer
	 * @return the edit, or {@code null} if none of the items was on a layer
	 */
	public static UndoableEdit delete(IContainer container, List<? extends AItem> items) {
		Objects.requireNonNull(items, "items");
		List<Removal> removed = new ArrayList<>();
		for (AItem item : items) {
			Layer layer = item.getLayer();
			int index = (layer == null) ? -1 : layer.detach(item);
			if (index >= 0) {
				removed.add(new Removal(item, layer, index));
			}
		}
		return removed.isEmpty() ? null : new Deleted(container, removed);
	}

	/** Where a deleted item sat, so it can be put back in the same place. */
	private static final class Removal {
		final AItem item;
		final Layer layer;
		int index;

		Removal(AItem item, Layer layer, int index) {
			this.item = item;
			this.layer = layer;
			this.index = index;
		}
	}

	private static final class Deleted extends AbstractUndoableEdit {
		private static final long serialVersionUID = 1L;

		private final transient IContainer container;
		private final transient List<Removal> removals;

		Deleted(IContainer container, List<Removal> removals) {
			this.container = container;
			this.removals = removals;
		}

		@Override
		public void undo() throws CannotUndoException {
			super.undo();
			// Put items back in the reverse of the order they were taken out, so
			// each lands at the index it had when it was removed.
			for (int i = removals.size() - 1; i >= 0; i--) {
				Removal r = removals.get(i);
				r.layer.reattach(r.item, r.index);
			}
			refresh(container);
		}

		@Override
		public void redo() throws CannotRedoException {
			super.redo();
			for (Removal r : removals) {
				int index = r.layer.detach(r.item);
				if (index >= 0) {
					r.index = index;
				}
			}
			refresh(container);
		}

		@Override
		public void die() {
			boolean detached = canUndo(); // done: the items are parked, off their layers
			super.die();
			if (detached) {
				for (Removal r : removals) {
					r.item.prepareForRemoval();
				}
			}
		}

		@Override
		public String getPresentationName() {
			return removals.size() == 1 ? "Delete" : "Delete " + removals.size() + " Items";
		}
	}

	private static void refresh(IContainer container) {
		if (container != null) {
			container.setDirty(true);
			container.refresh();
		}
	}
}
