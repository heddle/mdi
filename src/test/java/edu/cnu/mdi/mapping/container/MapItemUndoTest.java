package edu.cnu.mdi.mapping.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.toolbar.GestureContext;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ItemState;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.MapView2D;
import edu.cnu.mdi.mapping.item.MapLineItem;
import edu.cnu.mdi.mapping.item.MapMilSymbolItem;
import edu.cnu.mdi.mapping.item.MapPointItem;
import edu.cnu.mdi.mapping.item.MapPolygonItem;
import edu.cnu.mdi.mapping.item.MapPolylineItem;
import edu.cnu.mdi.mapping.item.MapTextItem;
import edu.cnu.mdi.mapping.milsym.MilSymbolDescriptor;
import edu.cnu.mdi.undo.ItemEdits;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Undo/redo of editing map-native items: moving and resizing them, driven
 * through the real {@link MapToolHandler} gesture sequence, and the snapshot
 * each item class keeps so that a restore is exact.
 */
class MapItemUndoTest {

	private static final double EPS = 1.0e-12;

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

	private static MapContainer newContainer() {
		return onEdt(() -> {
			MapView2D view = new MapView2D(PropertyUtils.VISIBLE, Boolean.FALSE);
			MapContainer c = (MapContainer) view.getIContainer();
			c.setBounds(0, 0, 800, 400);
			c.setDirty(true);
			return c;
		});
	}

	private static MouseEvent mouse(MapContainer c, int id, Point p) {
		return new MouseEvent(c.getComponent(), id, System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK, p.x,
				p.y, 1, false, MouseEvent.BUTTON1);
	}

	/** Presses at {@code press}, drags by (dx, dy) pixels and releases, through the real handler. */
	private static void gesture(MapContainer c, AItem primary, Point press, int dx, int dy) {
		MapToolHandler handler = new MapToolHandler(c);
		BaseToolBar toolbar = new BaseToolBar(c.getComponent(), handler, 0L);
		Point to = new Point(press.x + dx, press.y + dy);
		MouseEvent pressEvent = mouse(c, MouseEvent.MOUSE_PRESSED, press);
		MouseEvent dragEvent = mouse(c, MouseEvent.MOUSE_DRAGGED, to);
		handler.beginDragObject(new GestureContext(toolbar, c.getComponent(), primary, press, pressEvent));
		handler.dragObjectBy(new GestureContext(toolbar, c.getComponent(), primary, to, dragEvent), dx, dy);
		handler.endDragObject(new GestureContext(toolbar, c.getComponent(), primary, to, dragEvent));
	}

	/** Makes {@code item} selected, draggable and resizable, as the map tools do for new items. */
	private static <T extends AItem> T ready(T item) {
		item.setDraggable(true);
		item.setResizable(true);
		item.setRotatable(true);
		item.setSelectable(true);
		item.setLocked(false);
		item.setSelected(true);
		return item;
	}

	/**
	 * A point on a line item that is neither one of its end handles nor its
	 * rotate handle (which sits at the middle), so a press there starts a drag.
	 */
	private static Point grabPoint(MapContainer c, MapLineItem line) {
		Point[] handles = line.getSelectionPoints(c);
		Point middle = line.getFocusPoint(c);
		return new Point((handles[0].x + middle.x) / 2, (handles[0].y + middle.y) / 2);
	}

	private static Point2D.Double ll(double lonDegrees, double latDegrees) {
		return new Point2D.Double(Math.toRadians(lonDegrees), Math.toRadians(latDegrees));
	}

	private static Point2D.Double[] triangle() {
		return new Point2D.Double[] { ll(-20, -10), ll(20, -10), ll(0, 25) };
	}

	private static void assertSameLatLon(Point2D.Double expected, Point2D.Double actual) {
		assertEquals(expected.x, actual.x, EPS);
		assertEquals(expected.y, actual.y, EPS);
	}

	// ----------------------------------------------------------------- lines

	@Test
	void draggingAMapLineIsUndoableAndRedoable() {
		MapContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getAnnotationLayer();
			MapLineItem line = ready(new MapLineItem(layer, ll(-30, 0), ll(30, 10)));
			Point2D.Double start0 = line.getStartLatLon();
			Point2D.Double end0 = line.getEndLatLon();

			gesture(c, line, grabPoint(c, line), 40, 25);
			Point2D.Double start1 = line.getStartLatLon();
			Point2D.Double end1 = line.getEndLatLon();
			assertTrue(Math.abs(start1.x - start0.x) > 1e-6, "the line moved");
			assertEquals("Undo Move", c.getUndoPresentationName());

			assertTrue(c.undo());
			assertSameLatLon(start0, line.getStartLatLon());
			assertSameLatLon(end0, line.getEndLatLon());
			assertFalse(c.canUndo());

			assertTrue(c.redo());
			assertSameLatLon(start1, line.getStartLatLon());
			assertSameLatLon(end1, line.getEndLatLon());
		});
	}

	@Test
	void undoingALineDragPutsTheFocusBackToo() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapLineItem line = ready(new MapLineItem(c.getAnnotationLayer(), ll(-30, 0), ll(30, 10)));
			Point2D.Double focus0 = line.getFocus();
			ItemState before = line.captureState();

			gesture(c, line, grabPoint(c, line), 40, 25);
			assertFalse(before.sameAs(line.captureState()));

			c.undo();

			assertTrue(before.sameAs(line.captureState()));
			assertSameLatLon(focus0, line.getFocus());
		});
	}

	@Test
	void resizingALineByAnEndpointIsUndoable() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapLineItem line = ready(new MapLineItem(c.getAnnotationLayer(), ll(-30, 0), ll(30, 10)));
			Point2D.Double start0 = line.getStartLatLon();
			Point2D.Double end0 = line.getEndLatLon();
			Point[] handles = line.getSelectionPoints(c);
			assertNotNull(handles);

			gesture(c, line, handles[handles.length - 1], 30, -20);

			assertEquals("Undo Resize", c.getUndoPresentationName());
			c.undo();
			assertSameLatLon(start0, line.getStartLatLon());
			assertSameLatLon(end0, line.getEndLatLon());
		});
	}

	@Test
	void rotatingALineIsUndoableAndRestoresItsAzimuth() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapLineItem line = ready(new MapLineItem(c.getAnnotationLayer(), ll(-30, 0), ll(30, 10)));
			Point2D.Double start0 = line.getStartLatLon();
			Point2D.Double end0 = line.getEndLatLon();
			double azimuth0 = line.getAzimuth();
			Point rotate = line.getRotatePoint(c);
			assertNotNull(rotate);

			// Grab the handle slightly off its centre, which is the pivot, so the
			// drag has an angle.
			gesture(c, line, new Point(rotate.x + 3, rotate.y), 0, 60);
			assertEquals("Undo Rotate", c.getUndoPresentationName());
			assertTrue(Math.abs(line.getAzimuth() - azimuth0) > 0 || Math.abs(line.getStartLatLon().y - start0.y) > 1e-9,
					"the line rotated");

			c.undo();
			assertSameLatLon(start0, line.getStartLatLon());
			assertSameLatLon(end0, line.getEndLatLon());
			assertEquals(azimuth0, line.getAzimuth(), EPS);
		});
	}

	// ------------------------------------------------------ polylines/polygons

	@Test
	void draggingAPolylineIsUndoable() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapPolylineItem item = ready(new MapPolylineItem(c.getAnnotationLayer(), triangle()));
			Point2D.Double[] before = item.getLatLons();

			gesture(c, item, item.getFocusPoint(c), 30, 20);
			Point2D.Double[] moved = item.getLatLons();
			assertTrue(Math.abs(moved[0].x - before[0].x) > 1e-6);

			c.undo();
			Point2D.Double[] undone = item.getLatLons();
			for (int i = 0; i < before.length; i++) {
				assertSameLatLon(before[i], undone[i]);
			}

			c.redo();
			Point2D.Double[] redone = item.getLatLons();
			for (int i = 0; i < moved.length; i++) {
				assertSameLatLon(moved[i], redone[i]);
			}
		});
	}

	@Test
	void movingOneVertexOfAPolygonIsUndoable() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapPolygonItem item = ready(new MapPolygonItem(c.getAnnotationLayer(), triangle()));
			Point2D.Double[] before = item.getLatLons();
			Point[] handles = item.getSelectionPoints(c);
			assertEquals(3, handles.length);

			gesture(c, item, handles[1], 25, -30);
			Point2D.Double[] after = item.getLatLons();
			assertSameLatLon(before[0], after[0]);
			assertTrue(Math.abs(after[1].x - before[1].x) > 1e-6, "only vertex 1 moved");
			assertSameLatLon(before[2], after[2]);
			assertEquals("Undo Resize", c.getUndoPresentationName());

			c.undo();
			Point2D.Double[] undone = item.getLatLons();
			for (int i = 0; i < before.length; i++) {
				assertSameLatLon(before[i], undone[i]);
			}
		});
	}

	@Test
	void anUndoneVertexMoveRestoresTheFocusSoTheItemCanBeDraggedAgain() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapPolygonItem item = ready(new MapPolygonItem(c.getAnnotationLayer(), triangle()));
			Point2D.Double focus0 = item.getFocus();
			Point[] handles = item.getSelectionPoints(c);

			gesture(c, item, handles[1], 25, -30);
			assertTrue(Math.abs(item.getFocus().x - focus0.x) > 1e-9, "the centroid followed the vertex");

			c.undo();
			assertSameLatLon(focus0, item.getFocus());
		});
	}

	// ----------------------------------------------------------------- points

	@Test
	void draggingAMapPointIsUndoable() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapPointItem item = ready(new MapPointItem(c.getAnnotationLayer(), ll(10, 20)));
			Point2D.Double focus0 = item.getFocus();

			gesture(c, item, item.getFocusPoint(c), 50, 30);
			Point2D.Double focus1 = item.getFocus();
			assertTrue(Math.abs(focus1.x - focus0.x) > 1e-6);

			c.undo();
			assertSameLatLon(focus0, item.getFocus());
			c.redo();
			assertSameLatLon(focus1, item.getFocus());
		});
	}

	@Test
	void aMapPointKeepsItsAlignmentThroughUndo() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapPointItem item = new MapPointItem(c.getAnnotationLayer(), ll(10, 20));
			item.setAlignmentH(SwingConstants.LEFT);
			item.setAlignmentV(SwingConstants.TOP);
			ItemState before = item.captureState();

			item.setAlignmentH(SwingConstants.RIGHT);
			item.setAlignmentV(SwingConstants.BOTTOM);
			assertFalse(before.sameAs(item.captureState()));

			item.restoreState(before);

			assertEquals(SwingConstants.LEFT, item.getAlignmentH());
			assertEquals(SwingConstants.TOP, item.getAlignmentV());
			assertTrue(before.sameAs(item.captureState()));
		});
	}

	@Test
	void aMilSymbolRestoresItsDrawSizeAndLocation() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapMilSymbolItem item = new MapMilSymbolItem(c.getAnnotationLayer(), ll(5, 5),
					null, null);
			item.setDrawSize(40);
			ItemState before = item.captureState();

			item.setDrawSize(64);
			item.setFocus(ll(30, 30));
			assertFalse(before.sameAs(item.captureState()));

			item.restoreState(before);

			assertEquals(40, item.getDrawSize());
			assertSameLatLon(ll(5, 5), item.getFocus());
			assertTrue(before.sameAs(item.captureState()));
		});
	}

	// ------------------------------------------------------------------- text

	private static MapTextItem text(MapContainer c) {
		return ready(new MapTextItem(c.getAnnotationLayer(), ll(-10, 15), new Font("SansSerif", Font.PLAIN, 14),
				"hello\nworld", Color.black, Color.white, Color.blue));
	}

	@Test
	void draggingMapTextIsUndoable() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapTextItem item = text(c);
			Point2D.Double focus0 = item.getFocus();

			gesture(c, item, item.getFocusPoint(c), 40, 10);
			assertTrue(Math.abs(item.getFocus().x - focus0.x) > 1e-6);

			c.undo();
			assertSameLatLon(focus0, item.getFocus());
			assertEquals("hello\nworld", item.getText());
		});
	}

	@Test
	void editedMapTextComesBackWithItsOldContentFontAndAlignment() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapTextItem item = text(c);
			Font font0 = item.getFont();
			int align0 = item.getAlignment();
			ItemState before = item.captureState();

			item.setText("completely different");
			item.setFont(new Font("Serif", Font.BOLD, 30));
			item.setAlignment(SwingConstants.CENTER);
			ItemState after = item.captureState();
			assertFalse(before.sameAs(after));

			item.restoreState(before);
			assertEquals("hello\nworld", item.getText());
			assertEquals(font0, item.getFont());
			assertEquals(align0, item.getAlignment());

			item.restoreState(after);
			assertEquals("completely different", item.getText());
			assertEquals(SwingConstants.CENTER, item.getAlignment());
		});
	}

	@Test
	void anUnplacedMapTextCanBeSnapshottedAndRestored() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapTextItem item = new MapTextItem(c.getAnnotationLayer(), null, new Font("SansSerif", Font.PLAIN, 12),
					"x", Color.black, Color.white, Color.blue);
			ItemState state = item.captureState();
			assertNotNull(state);
			item.restoreState(state);
			assertTrue(state.sameAs(item.captureState()));
		});
	}

	// ---------------------------------------------------------- style/identity

	@Test
	void aMapItemsStyleIsPartOfItsSnapshot() {
		MapContainer c = newContainer();
		onEdt(() -> {
			MapLineItem line = ready(new MapLineItem(c.getAnnotationLayer(), ll(-30, 0), ll(30, 10)));
			line.getStyleSafe().setLineColor(Color.red);
			ItemState before = line.captureState();

			line.getStyleSafe().setLineColor(Color.green);
			c.recordEdit(ItemEdits.changedStates("Style", c, List.of(line), java.util.Collections.singletonList(before)));

			assertEquals("Undo Style", c.getUndoPresentationName());
			c.undo();
			assertEquals(Color.red, line.getStyleSafe().getLineColor());
		});
	}

	@Test
	void everyMapItemTypeIsSnapshottable() {
		MapContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getAnnotationLayer();
			assertNotNull(new MapLineItem(layer, ll(0, 0), ll(10, 10)).captureState());
			assertNotNull(new MapPolylineItem(layer, triangle()).captureState());
			assertNotNull(new MapPolygonItem(layer, triangle()).captureState());
			assertNotNull(new MapPointItem(layer, ll(1, 1)).captureState());
			assertNotNull(text(c).captureState());
		});
	}

	@Test
	void aRestoredItemIsTheSameObjectAndReprojectsFreshly() {
		MapContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getAnnotationLayer();
			MapLineItem line = ready(new MapLineItem(layer, ll(-30, 0), ll(30, 10)));
			java.awt.Rectangle boundsBefore = line.getBounds(c);

			gesture(c, line, grabPoint(c, line), 60, 40);
			java.awt.Rectangle boundsMoved = line.getBounds(c);
			assertFalse(boundsMoved.equals(boundsBefore));

			c.undo();

			assertSame(line, layer.getAllItems().get(0));
			assertEquals(boundsBefore, line.getBounds(c), "the cached projection was rebuilt");
		});
	}

	// ------------------------------------------------------------ symbol drop

	@Test
	void placingASymbolIsUndoableAndRedoable() {
		MapContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getAnnotationLayer();
			MilSymbolDescriptor descriptor = new MilSymbolDescriptor("id", "Test Symbol", "cat", "none",
					new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)));

			c.placeSymbol(descriptor, new Point(100, 100));
			assertEquals(1, layer.size());
			AItem symbol = layer.getAllItems().get(0);
			assertEquals("Undo Create Symbol", c.getUndoPresentationName());

			assertTrue(c.undo());
			assertEquals(0, layer.size());
			assertTrue(c.redo());
			assertSame(symbol, layer.getAllItems().get(0));
		});
	}

	@Test
	void placingNothingRecordsNothing() {
		MapContainer c = newContainer();
		onEdt(() -> {
			c.placeSymbol(null, new Point(100, 100));
			assertEquals(0, c.getAnnotationLayer().size());
			assertFalse(c.canUndo());
		});
	}
}
