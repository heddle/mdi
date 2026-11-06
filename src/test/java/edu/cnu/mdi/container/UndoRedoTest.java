package edu.cnu.mdi.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.graphics.connection.ConnectionManager;
import edu.cnu.mdi.graphics.style.Styled;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.toolbar.GestureContext;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemState;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.item.RectangleItem;
import edu.cnu.mdi.item.TextItem;
import edu.cnu.mdi.undo.ItemEdits;

/**
 * Undo and redo of interactive edits on a {@link BaseContainer}, driven through
 * the real {@link BaseToolHandler}.
 *
 * <p>The container maps a 10 x 5 world onto 200 x 100 pixels, so 20 pixels is
 * one world unit.</p>
 */
class UndoRedoTest {

	private static final double EPS = 1.0e-9;

	private static <T> T onEdt(Supplier<T> body) {
		Object[] result = new Object[1];
		try {
			SwingUtilities.invokeAndWait(() -> result[0] = body.get());
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException re) throw re;
			if (cause instanceof Error err) throw err;
			throw new AssertionError(cause);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while running Swing test", e);
		}
		@SuppressWarnings("unchecked")
		T value = (T) result[0];
		return value;
	}

	private static void onEdt(Runnable body) {
		onEdt(() -> {
			body.run();
			return null;
		});
	}

	private static BaseContainer newContainer() {
		return onEdt(() -> {
			BaseContainer c = new BaseContainer(new Rectangle2D.Double(0, 0, 10, 5));
			c.setBounds(0, 0, 200, 100);
			c.setDirty(true);
			return c;
		});
	}

	private static RectangleItem rectangle(Layer layer, double x, double y) {
		RectangleItem item = new RectangleItem(layer, new Rectangle2D.Double(x, y, 1, 1));
		item.setLocked(false);
		item.setDraggable(true);
		item.setDeletable(true);
		item.setSelectable(true);
		return item;
	}

	private static MouseEvent mouse(BaseContainer c, int id, Point p) {
		return new MouseEvent(c.getComponent(), id, System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK, p.x,
				p.y, 1, false, MouseEvent.BUTTON1);
	}

	/** Drag {@code primary} by (dx, dy) pixels through the real gesture sequence. */
	private static void drag(BaseContainer c, AItem primary, int dx, int dy) {
		BaseToolHandler handler = new BaseToolHandler(c);
		BaseToolBar toolbar = new BaseToolBar(c.getComponent(), handler, 0L);
		Point press = primary.getFocusPoint(c);
		Point to = new Point(press.x + dx, press.y + dy);
		MouseEvent pressEvent = mouse(c, MouseEvent.MOUSE_PRESSED, press);
		MouseEvent dragEvent = mouse(c, MouseEvent.MOUSE_DRAGGED, to);
		handler.beginDragObject(new GestureContext(toolbar, c.getComponent(), primary, press, pressEvent));
		handler.dragObjectBy(new GestureContext(toolbar, c.getComponent(), primary, to, dragEvent), dx, dy);
		handler.endDragObject(new GestureContext(toolbar, c.getComponent(), primary, to, dragEvent));
	}

	// -------------------------------------------------------------- moving

	@Test
	void aDragIsOneUndoableStepThatCanBeRedone() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = rectangle(c.getDefaultLayer(), 1, 1);
			double x0 = item.getFocus().x;
			double y0 = item.getFocus().y;
			assertFalse(c.canUndo());

			drag(c, item, 20, 0);
			assertEquals(x0 + 1, item.getFocus().x, EPS);
			assertTrue(c.canUndo());
			assertEquals("Undo Move", c.getUndoPresentationName());

			assertTrue(c.undo());
			assertEquals(x0, item.getFocus().x, EPS);
			assertEquals(y0, item.getFocus().y, EPS);
			assertFalse(c.canUndo());
			assertTrue(c.canRedo());
			assertEquals("Redo Move", c.getRedoPresentationName());

			assertTrue(c.redo());
			assertEquals(x0 + 1, item.getFocus().x, EPS);
			assertFalse(c.canRedo());
		});
	}

	@Test
	void undoPutsBackTheExactGeometryNotJustTheFocus() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = rectangle(c.getDefaultLayer(), 1, 1);
			ItemState before = item.captureState();

			drag(c, item, 40, 20);
			assertFalse(before.sameAs(item.captureState()));

			c.undo();

			assertTrue(before.sameAs(item.captureState()));
		});
	}

	@Test
	void draggingASelectionMovesAndUndoesAllItemsTogether() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem a = rectangle(c.getDefaultLayer(), 1, 1);
			RectangleItem b = rectangle(c.getDefaultLayer(), 5, 2);
			a.setSelected(true);
			b.setSelected(true);
			double ax = a.getFocus().x;
			double bx = b.getFocus().x;

			drag(c, a, 20, 0);
			assertEquals(ax + 1, a.getFocus().x, EPS);
			assertEquals(bx + 1, b.getFocus().x, EPS);

			assertTrue(c.undo());

			assertEquals(ax, a.getFocus().x, EPS);
			assertEquals(bx, b.getFocus().x, EPS);
			assertFalse(c.canUndo(), "the whole group drag was a single step");
		});
	}

	@Test
	void aDragThatMovesNothingRecordsNothing() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = rectangle(c.getDefaultLayer(), 1, 1);

			drag(c, item, 0, 0);

			assertFalse(c.canUndo());
		});
	}

	@Test
	void successiveDragsUndoNewestFirst() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = rectangle(c.getDefaultLayer(), 1, 1);
			double x0 = item.getFocus().x;
			drag(c, item, 20, 0);
			drag(c, item, 20, 0);
			assertEquals(x0 + 2, item.getFocus().x, EPS);

			c.undo();
			assertEquals(x0 + 1, item.getFocus().x, EPS);
			c.undo();
			assertEquals(x0, item.getFocus().x, EPS);
			assertFalse(c.undo(), "history exhausted");
		});
	}

	@Test
	void anEditAfterAnUndoDiscardsWhatCouldHaveBeenRedone() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = rectangle(c.getDefaultLayer(), 1, 1);
			drag(c, item, 20, 0);
			c.undo();
			assertTrue(c.canRedo());

			drag(c, item, 0, 20);

			assertFalse(c.canRedo());
		});
	}

	@Test
	void undoAndRedoWithNothingToDoReturnFalse() {
		BaseContainer c = newContainer();

		assertFalse(onEdt(() -> c.undo()));
		assertFalse(onEdt(() -> c.redo()));
		assertEquals("Undo", c.getUndoPresentationName().strip().split(" ")[0]);
	}

	// ------------------------------------------------------------ restyling

	@Test
	void aStyleChangeUndoesAndRedoes() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem a = rectangle(c.getDefaultLayer(), 1, 1);
			RectangleItem b = rectangle(c.getDefaultLayer(), 5, 2);
			List<AItem> both = List.of(a, b);
			Color original = a.getStyle().getLineColor();
			List<ItemState> before = ItemEdits.capture(both);
			for (AItem item : both) {
				Styled s = new Styled();
				s.setLineColor(Color.MAGENTA);
				item.setStyle(s);
			}
			c.recordEdit(ItemEdits.changedStates("Style", c, both, before));
			assertEquals("Undo Style", c.getUndoPresentationName());

			c.undo();
			assertEquals(original, a.getStyle().getLineColor());
			assertEquals(original, b.getStyle().getLineColor());

			c.redo();
			assertEquals(Color.MAGENTA, a.getStyle().getLineColor());
			assertEquals(Color.MAGENTA, b.getStyle().getLineColor());
		});
	}

	@Test
	void anEditThatChangedNothingIsNotRecorded() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem a = rectangle(c.getDefaultLayer(), 1, 1);
			List<AItem> items = List.of(a);
			List<ItemState> before = ItemEdits.capture(items);

			assertNull(ItemEdits.changedStates("Nothing", c, items, before));
		});
	}

	@Test
	void textEditsUndoTheTextAndFont() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			TextItem text = new TextItem(c.getDefaultLayer(), new Point2D.Double(3, 2),
					new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12), "before", Color.BLACK, Color.WHITE,
					Color.BLACK);
			List<AItem> items = List.of(text);
			List<ItemState> before = ItemEdits.capture(items);
			text.setText("after");
			text.setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 18));
			c.recordEdit(ItemEdits.changedStates("Edit Text", c, items, before));

			c.undo();
			assertEquals("before", text.getText());
			assertEquals(12, text.getFont().getSize());

			c.redo();
			assertEquals("after", text.getText());
			assertEquals(18, text.getFont().getSize());
		});
	}

	// ------------------------------------------------------------- creation

	@Test
	void aCreatedItemCanBeUndoneAndRedoneAsTheSameObject() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			BaseToolHandler handler = new BaseToolHandler(c);
			Layer layer = c.getAnnotationLayer();

			handler.createRectangle(null, new Rectangle(20, 20, 40, 40));
			assertEquals(1, layer.size());
			AItem created = layer.getAllItems().get(0);
			assertEquals("Undo Create Rectangle", c.getUndoPresentationName());

			c.undo();
			assertEquals(0, layer.size());

			c.redo();
			assertEquals(1, layer.size());
			assertSame(created, layer.getAllItems().get(0));
		});
	}

	@Test
	void everyCreationToolIsUndoable() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			BaseToolHandler handler = new BaseToolHandler(c);
			Layer layer = c.getAnnotationLayer();
			Point[] tri = { new Point(20, 20), new Point(60, 20), new Point(40, 60) };

			handler.createRectangle(null, new Rectangle(20, 20, 40, 40));
			handler.createEllipse(null, new Rectangle(20, 20, 40, 40));
			handler.createPolygon(null, tri);
			handler.createPolyline(null, tri);
			handler.createLine(null, new Point(10, 10), new Point(90, 40));
			handler.createRadArc(null, new Point[] { new Point(50, 50), new Point(80, 50), new Point(50, 20) });

			assertEquals(6, layer.size());
			for (int i = 0; i < 6; i++) {
				assertTrue(c.undo(), "step " + i);
			}
			assertEquals(0, layer.size());
			assertFalse(c.canUndo());
		});
	}

	@Test
	void creatingAfterAnUndoDisposesTheItemThatCouldHaveBeenRedone() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			BaseToolHandler handler = new BaseToolHandler(c);
			Layer layer = c.getAnnotationLayer();
			handler.createRectangle(null, new Rectangle(20, 20, 40, 40));
			AItem parked = layer.getAllItems().get(0);
			c.undo();
			assertNotNull(parked.getLayer(), "parked, not disposed, while redo is possible");

			handler.createEllipse(null, new Rectangle(60, 20, 40, 40));

			assertFalse(c.canRedo());
			assertNull(parked.getLayer(), "disposed once it can no longer be redone");
		});
	}

	// ------------------------------------------------------------- deletion

	@Test
	void deletionIsUndoneByPuttingTheSameItemsBackInTheSameOrder() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			RectangleItem a = rectangle(layer, 0, 0);
			RectangleItem b = rectangle(layer, 3, 1);
			RectangleItem keep = rectangle(layer, 6, 2);
			RectangleItem d = rectangle(layer, 8, 3);
			b.setSelected(true);
			d.setSelected(true);
			ItemState bState = b.captureState();

			c.deleteSelectedItemsUndoably();

			assertEquals(List.of(a, keep), new ArrayList<>(layer.getAllItems()));
			assertEquals("Undo Delete 2 Items", c.getUndoPresentationName());

			c.undo();
			assertEquals(List.of(a, b, keep, d), new ArrayList<>(layer.getAllItems()));
			assertTrue(bState.sameAs(b.captureState()), "restored intact");
			assertTrue(b.isSelected());

			c.redo();
			assertEquals(List.of(a, keep), new ArrayList<>(layer.getAllItems()));
		});
	}

	@Test
	void theToolbarDeleteToolIsUndoable() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			RectangleItem a = rectangle(layer, 1, 1);
			a.setSelected(true);
			BaseToolHandler handler = new BaseToolHandler(c);
			BaseToolBar toolbar = new BaseToolBar(c.getComponent(), handler, 0L);
			Point p = a.getFocusPoint(c);

			handler.delete(new GestureContext(toolbar, c.getComponent(), a, p, mouse(c, MouseEvent.MOUSE_CLICKED, p)));
			assertFalse(layer.contains(a));

			c.undo();
			assertTrue(layer.contains(a));
		});
	}

	@Test
	void deletingOneItemIsNamedDelete() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem a = rectangle(c.getDefaultLayer(), 1, 1);
			a.setSelected(true);

			c.deleteSelectedItemsUndoably();

			assertEquals("Undo Delete", c.getUndoPresentationName());
		});
	}

	@Test
	void undoableDeleteSkipsWhatOrdinaryDeleteSkips() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer open = c.getDefaultLayer();
			Layer locked = new Layer(c, "Locked");
			Layer hidden = new Layer(c, "Hidden");
			RectangleItem doomed = rectangle(open, 0, 0);
			RectangleItem unselected = rectangle(open, 3, 0);
			RectangleItem protectedItem = rectangle(open, 6, 0);
			protectedItem.setDeletable(false);
			RectangleItem inLocked = rectangle(locked, 0, 3);
			RectangleItem inHidden = rectangle(hidden, 3, 3);
			doomed.setSelected(true);
			protectedItem.setSelected(true);
			inLocked.setSelected(true);
			inHidden.setSelected(true);
			locked.setLocked(true);
			hidden.setVisible(false);

			c.deleteSelectedItemsUndoably();

			assertFalse(open.contains(doomed));
			assertTrue(open.contains(unselected));
			assertTrue(open.contains(protectedItem));
			assertTrue(locked.contains(inLocked));
			assertTrue(hidden.contains(inHidden));
		});
	}

	@Test
	void deletingNothingRecordsNothing() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			rectangle(c.getDefaultLayer(), 1, 1); // not selected

			c.deleteSelectedItemsUndoably();

			assertFalse(c.canUndo());
		});
	}

	@Test
	void aDeletedItemIsDisposedOnlyWhenItsEntryLeavesTheHistory() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			RectangleItem kept = rectangle(layer, 1, 1);
			RectangleItem gone = rectangle(layer, 5, 2);
			kept.setSelected(true);
			c.deleteSelectedItemsUndoably();
			assertNotNull(kept.getLayer(), "deleted but still undoable, so not disposed");

			c.undo(); // back on the layer
			c.clearUndoHistory();
			assertNotNull(kept.getLayer(), "undone deletions are live items, so clearing must not dispose them");
			assertTrue(layer.contains(kept));

			gone.setSelected(true);
			c.deleteSelectedItemsUndoably();
			c.clearUndoHistory();
			assertNull(gone.getLayer(), "a deletion forgotten while still done is disposed");
		});
	}

	@Test
	void theOldestEntriesFallOffAtTheHistoryLimit() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			RectangleItem victim = rectangle(layer, 1, 1);
			victim.setSelected(true);
			c.deleteSelectedItemsUndoably();
			assertNotNull(victim.getLayer());

			BaseToolHandler handler = new BaseToolHandler(c);
			for (int i = 0; i < BaseContainer.UNDO_LIMIT; i++) {
				handler.createRectangle(null, new Rectangle(20, 20, 10, 10));
			}

			assertNull(victim.getLayer(), "its entry was trimmed, so the deleted item is released");
			int undone = 0;
			while (c.undo()) {
				undone++;
			}
			assertEquals(BaseContainer.UNDO_LIMIT, undone);
		});
	}

	// ----------------------------------- deletions the history did not make

	@Test
	void deletingAnItemBehindTheHistorysBackClearsTheHistory() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			RectangleItem a = rectangle(layer, 1, 1);
			RectangleItem b = rectangle(layer, 5, 2);
			drag(c, a, 20, 0);
			assertTrue(c.canUndo());

			layer.remove(b); // programmatic hard delete: disposes b

			assertFalse(c.canUndo(), "history could now mention a disposed item");
			assertFalse(c.canRedo());
		});
	}

	@Test
	void undoingAndRedoingNeverClearTheHistoryThemselves() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			BaseToolHandler handler = new BaseToolHandler(c);
			handler.createRectangle(null, new Rectangle(20, 20, 40, 40));
			handler.createEllipse(null, new Rectangle(60, 20, 40, 40));

			c.undo(); // detaches: fires DELETED
			assertTrue(c.canUndo());
			assertTrue(c.canRedo());
			c.redo(); // reattaches
			assertTrue(c.canUndo());
		});
	}

	@Test
	void connectedItemsAreDeletedForGoodAndTheHistoryIsCleared() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			RectangleItem a = rectangle(layer, 1, 1);
			RectangleItem b = rectangle(layer, 6, 2);
			a.setConnectable(true);
			b.setConnectable(true);
			drag(c, a, 20, 0);
			assertTrue(c.canUndo());
			assertNotNull(ConnectionManager.getInstance().connect(c.getConnectionLayer(), a, b));
			a.setSelected(true);

			c.deleteSelectedItemsUndoably();

			assertFalse(layer.contains(a));
			assertNull(a.getLayer(), "removed the ordinary way, so disposed");
			assertEquals(0, c.getConnectionLayer().size(), "its connector went with it");
			assertTrue(layer.contains(b));
			assertFalse(c.canUndo(), "the history was cleared");
		});
	}

	// ---------------------------------------------------------- API surface

	@Test
	void theUndoManagerIsExposedWithTheDocumentedLimit() {
		BaseContainer c = newContainer();

		assertEquals(BaseContainer.UNDO_LIMIT, c.getUndoManager().getLimit());
		assertEquals(100, BaseContainer.UNDO_LIMIT);
	}

	@Test
	void recordingANullEditIsIgnored() {
		BaseContainer c = newContainer();

		c.recordEdit(null);

		assertFalse(c.canUndo());
	}
}
