package edu.cnu.mdi.mapping.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.MapView2D;
import edu.cnu.mdi.undo.ItemEdits;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Undo/redo on a map: creating and deleting map items is undoable. Editing a map
 * item's geometry is not recorded yet, because its geographic coordinates are not
 * part of the item snapshot, so undoing it would restore the wrong thing.
 */
class MapUndoTest {

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

	/** A map container with a real map view behind it, which supplies the projection. */
	private static MapContainer newContainer() {
		return onEdt(() -> {
			MapView2D view = new MapView2D(PropertyUtils.VISIBLE, Boolean.FALSE);
			MapContainer c = (MapContainer) view.getIContainer();
			c.setBounds(0, 0, 800, 400);
			c.setDirty(true);
			return c;
		});
	}

	@Test
	void createdMapLinesPolylinesAndPolygonsAreUndoable() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapToolHandler handler = new MapToolHandler(c);
			Layer layer = c.getAnnotationLayer();
			Point[] vertices = { new Point(100, 100), new Point(300, 120), new Point(200, 250) };

			handler.createLine(null, new Point(100, 100), new Point(400, 200));
			handler.createPolyline(null, vertices);
			handler.createPolygon(null, vertices);
			assertEquals(3, layer.size());
			assertEquals("Undo Create Polygon", c.getUndoPresentationName());

			assertTrue(c.undo());
			assertEquals(2, layer.size());
			assertTrue(c.undo());
			assertTrue(c.undo());
			assertEquals(0, layer.size());
			assertFalse(c.canUndo());

			assertTrue(c.redo());
			assertEquals(1, layer.size());
			assertEquals("Redo Create Polyline", c.getRedoPresentationName());
		});
	}

	@Test
	void aRedoneMapItemIsTheSameObject() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapToolHandler handler = new MapToolHandler(c);
			Layer layer = c.getAnnotationLayer();
			handler.createLine(null, new Point(100, 100), new Point(400, 200));
			AItem line = layer.getAllItems().get(0);

			c.undo();
			c.redo();

			assertSame(line, layer.getAllItems().get(0));
		});
	}

	@Test
	void aDeletedMapItemComesBackOnUndo() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapToolHandler handler = new MapToolHandler(c);
			Layer layer = c.getAnnotationLayer();
			handler.createLine(null, new Point(100, 100), new Point(400, 200));
			AItem line = layer.getAllItems().get(0);
			line.setSelected(true);

			c.deleteSelectedItemsUndoably();
			assertEquals(0, layer.size());

			c.undo();
			assertEquals(1, layer.size());
			assertSame(line, layer.getAllItems().get(0));
			assertNotNull(line.getLayer());
		});
	}

	@Test
	void mapItemGeometryIsNotSnapshottedSoItsEditsAreNotRecorded() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapToolHandler handler = new MapToolHandler(c);
			handler.createLine(null, new Point(100, 100), new Point(400, 200));
			AItem line = c.getAnnotationLayer().getAllItems().get(0);

			assertNull(line.captureState());
			var items = List.of(line);
			var before = ItemEdits.capture(items);
			assertNull(before.get(0));
			assertNull(ItemEdits.changedStates("Move", c, items, before));
		});
	}
}
