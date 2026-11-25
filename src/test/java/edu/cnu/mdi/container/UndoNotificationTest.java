package edu.cnu.mdi.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.toolbar.GestureContext;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemChangeType;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.item.RectangleItem;
import edu.cnu.mdi.undo.ItemEdits;

/**
 * Undo and redo must tell a layer's listeners what they changed, just as the
 * original gesture did. An application that keeps its own model in step with its
 * items (by listening for {@code MOVED} and the like) otherwise disagrees with
 * the canvas after an undo.
 */
class UndoNotificationTest {

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
		item.setResizable(true);
		item.setRotatable(true);
		item.setSelectable(true);
		return item;
	}

	/** Records the geometry-change events a layer reports. */
	private static List<String> record(Layer layer, AItem... named) {
		List<String> events = new ArrayList<>();
		layer.addItemChangeListener((l, item, type) -> {
			if (type == ItemChangeType.MOVED || type == ItemChangeType.RESIZED || type == ItemChangeType.ROTATED
					|| type == ItemChangeType.MODIFIED) {
				for (int i = 0; i < named.length; i++) {
					if (named[i] == item) {
						events.add(type + " " + i);
					}
				}
			}
		});
		return events;
	}

	private static MouseEvent mouse(BaseContainer c, int id, Point p) {
		return new MouseEvent(c.getComponent(), id, System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK, p.x,
				p.y, 1, false, MouseEvent.BUTTON1);
	}

	private static void gesture(BaseContainer c, AItem primary, Point press, int dx, int dy) {
		BaseToolHandler handler = new BaseToolHandler(c);
		BaseToolBar toolbar = new BaseToolBar(c.getComponent(), handler, 0L);
		Point to = new Point(press.x + dx, press.y + dy);
		MouseEvent pressEvent = mouse(c, MouseEvent.MOUSE_PRESSED, press);
		MouseEvent dragEvent = mouse(c, MouseEvent.MOUSE_DRAGGED, to);
		handler.beginDragObject(new GestureContext(toolbar, c.getComponent(), primary, press, pressEvent));
		handler.dragObjectBy(new GestureContext(toolbar, c.getComponent(), primary, to, dragEvent), dx, dy);
		handler.endDragObject(new GestureContext(toolbar, c.getComponent(), primary, to, dragEvent));
	}

	@Test
	void aModelThatFollowsMovedEventsStaysInStepThroughUndoAndRedo() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = rectangle(c.getDefaultLayer(), 1, 1);
			Map<AItem, Point2D.Double> model = new HashMap<>();
			model.put(item, item.getFocus());
			c.getDefaultLayer().addItemChangeListener((layer, changed, type) -> {
				if (type == ItemChangeType.MOVED) {
					model.put(changed, changed.getFocus());
				}
			});
			double x0 = item.getFocus().x;

			gesture(c, item, item.getFocusPoint(c), 40, 0);
			assertEquals(x0 + 2, model.get(item).x, EPS);

			c.undo();
			assertEquals(x0, item.getFocus().x, EPS);
			assertEquals(x0, model.get(item).x, EPS, "the model heard about the undo");

			c.redo();
			assertEquals(x0 + 2, model.get(item).x, EPS, "the model heard about the redo");
		});
	}

	@Test
	void undoingAMoveReportsMoved() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = rectangle(c.getDefaultLayer(), 1, 1);
			gesture(c, item, item.getFocusPoint(c), 40, 0);
			List<String> events = record(c.getDefaultLayer(), item);

			c.undo();
			c.redo();

			assertEquals(List.of("MOVED 0", "MOVED 0"), events);
		});
	}

	@Test
	void undoingAResizeReportsResized() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = rectangle(c.getDefaultLayer(), 2, 1);
			item.setSelected(true);
			gesture(c, item, item.getSelectionPoints(c)[0], -20, 10);
			assertEquals("Undo Resize", c.getUndoPresentationName());
			List<String> events = record(c.getDefaultLayer(), item);

			c.undo();

			assertEquals(List.of("RESIZED 0"), events);
		});
	}

	@Test
	void undoingARotationReportsRotated() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = rectangle(c.getDefaultLayer(), 4, 2);
			item.setSelected(true);
			gesture(c, item, item.getRotatePoint(c), 25, 25);
			assertEquals("Undo Rotate", c.getUndoPresentationName());
			List<String> events = record(c.getDefaultLayer(), item);

			c.undo();

			assertEquals(List.of("ROTATED 0"), events);
		});
	}

	@Test
	void undoingAGroupDragReportsEveryItemAfterAllAreRestored() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem a = rectangle(c.getDefaultLayer(), 1, 1);
			RectangleItem b = rectangle(c.getDefaultLayer(), 4, 1);
			a.setSelected(true);
			b.setSelected(true);
			double ax = a.getFocus().x;
			double bx = b.getFocus().x;
			gesture(c, a, a.getFocusPoint(c), 20, 0);
			List<String> events = record(c.getDefaultLayer(), a, b);
			List<Boolean> bothRestored = new ArrayList<>();
			c.getDefaultLayer().addItemChangeListener((layer, item, type) -> {
				if (type == ItemChangeType.MOVED) {
					bothRestored.add(Math.abs(a.getFocus().x - ax) < EPS && Math.abs(b.getFocus().x - bx) < EPS);
				}
			});

			c.undo();

			assertEquals(2, events.size());
			assertTrue(events.contains("MOVED 0") && events.contains("MOVED 1"), events.toString());
			assertEquals(List.of(true, true), bothRestored);
		});
	}

	@Test
	void undoingAStyleChangeReportsModified() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = rectangle(c.getDefaultLayer(), 1, 1);
			var items = List.of(item);
			var before = ItemEdits.capture(items);
			item.getStyleSafe().setFillColor(Color.green);
			c.recordEdit(ItemEdits.changedStates("Style", c, items, before));
			List<String> events = record(c.getDefaultLayer(), item);

			c.undo();
			c.redo();

			assertEquals(List.of("MODIFIED 0", "MODIFIED 0"), events);
		});
	}

	@Test
	void aListenerThatRecordsAnEditDuringUndoDoesNotDisturbTheHistory() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = rectangle(c.getDefaultLayer(), 1, 1);
			gesture(c, item, item.getFocusPoint(c), 40, 0);
			c.getDefaultLayer().addItemChangeListener((layer, changed, type) -> {
				if (type == ItemChangeType.MOVED) {
					var items = List.of(changed);
					var before = ItemEdits.capture(items);
					changed.getStyleSafe().setFillColor(Color.pink);
					c.recordEdit(ItemEdits.changedStates("Echo", c, items, before));
				}
			});

			c.undo();

			assertTrue(c.canRedo(), "the redo survived");
			assertEquals("Redo Move", c.getRedoPresentationName());
		});
	}
}
