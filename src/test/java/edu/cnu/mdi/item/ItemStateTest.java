package edu.cnu.mdi.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.style.Styled;

/**
 * {@link ItemState} snapshots, each item type's opt-in to them, and
 * {@link Layer#detach(AItem)} / {@link Layer#reattach(AItem, int)}, which undoable
 * deletion is built on.
 */
class ItemStateTest {

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

	private static BaseContainer newContainer() {
		return onEdt(() -> {
			BaseContainer c = new BaseContainer(new Rectangle2D.Double(0, 0, 10, 5));
			c.setBounds(0, 0, 200, 100);
			c.setDirty(true);
			return c;
		});
	}

	private static Path2D.Double triangle(double x) {
		Path2D.Double p = new Path2D.Double();
		p.moveTo(x, 0);
		p.lineTo(x + 1, 0);
		p.lineTo(x, 1);
		p.closePath();
		return p;
	}

	/** An item that does not opt in to snapshots. */
	private static final class PlainItem extends AItem {
		PlainItem(Layer layer) {
			super(layer);
		}

		@Override
		public void drawItem(Graphics2D g2, IContainer container) { }

		@Override
		public boolean shouldDraw(Graphics2D g2, IContainer container) {
			return true;
		}

		@Override
		public Rectangle2D.Double getWorldBounds() {
			return new Rectangle2D.Double(0, 0, 1, 1);
		}

		@Override
		public void translateWorld(double dx, double dy) { }

		@Override
		public void modify() { }
	}

	// ------------------------------------------------------------- ItemState

	@Test
	void aSnapshotIsIndependentOfTheObjectsItWasMadeFrom() {
		Point2D.Double focus = new Point2D.Double(1, 2);
		Path2D.Double path = triangle(0);
		Line2D.Double line = new Line2D.Double(0, 0, 1, 1);
		Styled style = new Styled();
		style.setLineColor(Color.RED);

		ItemState state = new ItemState(focus, 10, path, line, style, null);
		focus.x = 99;
		path.reset();
		line.x2 = 99;
		style.setLineColor(Color.GREEN);

		assertEquals(1, state.focus().x, EPS);
		assertEquals(triangle(0).getBounds2D(), state.path().getBounds2D());
		assertEquals(1, state.line().x2, EPS);
		assertEquals(Color.RED, state.style().getLineColor());
	}

	@Test
	void accessorsHandOutCopiesNotTheStoredObjects() {
		ItemState state = new ItemState(new Point2D.Double(1, 2), 0, triangle(0), new Line2D.Double(0, 0, 1, 1),
				new Styled(), null);

		state.focus().x = 99;
		state.path().reset();
		state.line().x2 = 99;

		assertEquals(1, state.focus().x, EPS);
		assertEquals(3, countSegments(state.path()));
		assertEquals(1, state.line().x2, EPS);
		assertNotSame(state.style(), state.style());
	}

	private static int countSegments(Path2D path) {
		int n = 0;
		for (var it = path.getPathIterator(null); !it.isDone(); it.next()) {
			n++;
		}
		return n == 0 ? 0 : n - 1; // do not count the close
	}

	@Test
	void identicalSnapshotsAreTheSameAndNullIsNot() {
		ItemState a = new ItemState(new Point2D.Double(1, 2), 30, triangle(0), null, new Styled(), "x");
		ItemState b = new ItemState(new Point2D.Double(1, 2), 30, triangle(0), null, new Styled(), "x");

		assertTrue(a.sameAs(b));
		assertTrue(a.sameAs(a));
		assertFalse(a.sameAs(null));
	}

	@Test
	void everyPartOfTheStateMattersToSameAs() {
		ItemState base = new ItemState(new Point2D.Double(1, 2), 30, triangle(0), new Line2D.Double(0, 0, 1, 1),
				new Styled(), "x");
		Styled other = new Styled();
		other.setLineWidth(5f);

		assertFalse(base.sameAs(new ItemState(new Point2D.Double(1, 2.5), 30, triangle(0), new Line2D.Double(0, 0, 1, 1),
				new Styled(), "x")), "focus");
		assertFalse(base.sameAs(new ItemState(new Point2D.Double(1, 2), 31, triangle(0), new Line2D.Double(0, 0, 1, 1),
				new Styled(), "x")), "azimuth");
		assertFalse(base.sameAs(new ItemState(new Point2D.Double(1, 2), 30, triangle(0.5), new Line2D.Double(0, 0, 1, 1),
				new Styled(), "x")), "path");
		assertFalse(base.sameAs(new ItemState(new Point2D.Double(1, 2), 30, triangle(0), new Line2D.Double(0, 0, 1, 2),
				new Styled(), "x")), "line");
		assertFalse(base.sameAs(new ItemState(new Point2D.Double(1, 2), 30, triangle(0), new Line2D.Double(0, 0, 1, 1),
				other, "x")), "style");
		assertFalse(base.sameAs(new ItemState(new Point2D.Double(1, 2), 30, triangle(0), new Line2D.Double(0, 0, 1, 1),
				new Styled(), "y")), "extra");
		assertFalse(base.sameAs(new ItemState(null, 30, triangle(0), new Line2D.Double(0, 0, 1, 1), new Styled(), "x")),
				"missing focus");
		assertFalse(base.sameAs(new ItemState(new Point2D.Double(1, 2), 30, null, new Line2D.Double(0, 0, 1, 1),
				new Styled(), "x")), "missing path");
	}

	@Test
	void pathsOfDifferentShapeButEqualBoundsAreNotTheSame() {
		Path2D.Double a = triangle(0);
		Path2D.Double b = new Path2D.Double();
		b.moveTo(0, 0);
		b.lineTo(1, 0);
		b.lineTo(1, 1); // same bounds as a, different shape
		b.closePath();

		assertEquals(a.getBounds2D(), b.getBounds2D());
		assertFalse(new ItemState(null, 0, a, null, null, null).sameAs(new ItemState(null, 0, b, null, null, null)));
	}

	@Test
	void nanCoordinatesCompareAsTheSameAsEachOther() {
		// A PointItem with no location keeps its focus at NaN, NaN.
		Point2D.Double nan = new Point2D.Double(Double.NaN, Double.NaN);

		assertTrue(new ItemState(nan, 0, null, null, null, null).sameAs(new ItemState(nan, 0, null, null, null, null)));
	}

	// -------------------------------------------- per-item opt-in and restore

	/** Restoring a state captured from a different item of the same kind must reproduce it exactly. */
	private static void assertRestoresFromAnotherInstance(AItem target, AItem source) {
		ItemState wanted = source.captureState();
		assertNotNull(wanted);
		assertFalse(wanted.sameAs(target.captureState()), "test needs two different items");

		target.restoreState(wanted);

		assertTrue(wanted.sameAs(target.captureState()), target.getClass().getSimpleName());
		assertTrue(target.isDirty());
	}

	@Test
	void rectangleRestoresPositionAndRotation() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			RectangleItem a = new RectangleItem(layer, new Rectangle2D.Double(1, 1, 2, 1));
			RectangleItem b = new RectangleItem(layer, new Rectangle2D.Double(5, 3, 2, 1));
			b.setAzimuth(40);
			assertRestoresFromAnotherInstance(a, b);
			assertEquals(40, a.getAzimuth(), EPS);
		});
	}

	@Test
	void rectangleBackToItsOwnEarlierState() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = new RectangleItem(c.getDefaultLayer(), new Rectangle2D.Double(1, 1, 2, 1));
			ItemState before = item.captureState();
			Rectangle2D.Double boundsBefore = item.getWorldBounds();

			item.translateWorld(3, 1);
			item.setAzimuth(25);
			assertFalse(before.sameAs(item.captureState()));

			item.restoreState(before);

			assertTrue(before.sameAs(item.captureState()));
			assertEquals(boundsBefore, item.getWorldBounds());
			assertEquals(0, item.getAzimuth(), EPS);
		});
	}

	@Test
	void ellipseRestoresItsSize() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			EllipseItem small = new EllipseItem(layer, 2, 1, 0, new Point2D.Double(2, 2));
			EllipseItem big = new EllipseItem(layer, 6, 3, 20, new Point2D.Double(6, 3));
			assertRestoresFromAnotherInstance(small, big);
			assertEquals(20, small.getAzimuth(), EPS);
		});
	}

	@Test
	void radArcRestoresItsDefiningPointsAndAngle() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			RadArcItem a = new RadArcItem(layer, new Point2D.Double(2, 2), new Point2D.Double(3, 2), 60);
			RadArcItem b = new RadArcItem(layer, new Point2D.Double(6, 3), new Point2D.Double(6, 4.5), 120);
			assertRestoresFromAnotherInstance(a, b);
		});
	}

	@Test
	void textRestoresItsContentFontAndStyle() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			TextItem a = new TextItem(layer, new Point2D.Double(2, 2), new Font("SansSerif", Font.PLAIN, 12), "one",
					Color.BLACK, Color.WHITE, Color.BLACK);
			TextItem b = new TextItem(layer, new Point2D.Double(7, 3), new Font("Serif", Font.BOLD, 20), "two\nlines",
					Color.RED, Color.YELLOW, Color.BLUE);
			assertRestoresFromAnotherInstance(a, b);
			assertEquals("two\nlines", a.getText());
			assertEquals(new Font("Serif", Font.BOLD, 20), a.getFont());
			assertEquals(Color.BLUE, a.getStyle().getTextColor());
		});
	}

	@Test
	void lineRestoresBothEnds() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			LineItem a = new LineItem(layer, new Point2D.Double(1, 1), new Point2D.Double(2, 2));
			LineItem b = new LineItem(layer, new Point2D.Double(5, 1), new Point2D.Double(8, 4));
			assertRestoresFromAnotherInstance(a, b);
			assertEquals(8, a.getLine().x2, EPS);
		});
	}

	@Test
	void pointRestoresItsLocation() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			PointItem a = new PointItem(layer, new Point2D.Double(1, 1));
			PointItem b = new PointItem(layer, new Point2D.Double(7, 3));
			assertRestoresFromAnotherInstance(a, b);
			assertEquals(7, a.getFocus().x, EPS);
		});
	}

	@Test
	void polygonPolylineAndImageRestoreTheirPaths() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			Point2D.Double[] tri1 = { new Point2D.Double(0, 0), new Point2D.Double(1, 0), new Point2D.Double(0, 1) };
			Point2D.Double[] tri2 = { new Point2D.Double(5, 2), new Point2D.Double(7, 2), new Point2D.Double(5, 4) };
			assertRestoresFromAnotherInstance(new PolygonItem(layer, tri1), new PolygonItem(layer, tri2));
			assertRestoresFromAnotherInstance(new PolylineItem(layer, tri1), new PolylineItem(layer, tri2));

			BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
			assertRestoresFromAnotherInstance(new ImageItem(layer, new Rectangle2D.Double(1, 1, 2, 1), image),
					new ImageItem(layer, new Rectangle2D.Double(5, 2, 2, 1), image));
		});
	}

	@Test
	void restoringAStylePutsTheOldStyleBack() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = new RectangleItem(c.getDefaultLayer(), new Rectangle2D.Double(1, 1, 2, 1));
			ItemState before = item.captureState();
			Styled changed = new Styled();
			changed.setLineColor(Color.MAGENTA);
			changed.setLineWidth(7f);
			item.setStyle(changed);
			ItemState after = item.captureState();
			assertFalse(before.sameAs(after), "a style change is a state change");

			item.restoreState(before);

			assertFalse(Color.MAGENTA.equals(item.getStyle().getLineColor()));
			assertTrue(before.sameAs(item.captureState()));

			item.restoreState(after);
			assertEquals(Color.MAGENTA, item.getStyle().getLineColor());
			assertEquals(7f, item.getStyle().getLineWidth(), 0f);
		});
	}

	@Test
	void restoredItemsDoNotShareGeometryWithTheSnapshot() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem item = new RectangleItem(c.getDefaultLayer(), new Rectangle2D.Double(1, 1, 2, 1));
			ItemState before = item.captureState();
			item.restoreState(before);

			item.translateWorld(5, 5); // must not leak into the snapshot

			assertEquals(1, before.path().getBounds2D().getX(), EPS);
		});
	}

	@Test
	void itemsThatDoNotOptInReportNoStateAndRefuseARestore() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			PlainItem plain = new PlainItem(layer);
			assertNull(plain.captureState());
			ItemState someState = new ItemState(null, 0, null, null, null, null);
			assertThrows(UnsupportedOperationException.class, () -> plain.restoreState(someState));
		});
	}

	@Test
	void connectorsAreNeverRestoredFromSnapshotsBecauseTheirGeometryIsDerived() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			RectangleItem a = new RectangleItem(c.getDefaultLayer(), new Rectangle2D.Double(1, 1, 1, 1));
			RectangleItem b = new RectangleItem(c.getDefaultLayer(), new Rectangle2D.Double(6, 3, 1, 1));
			ConnectorItem connector = new ConnectorItem(c.getConnectionLayer(), a, b);

			assertNull(connector.captureState());
		});
	}

	// ------------------------------------------------------ Layer detach/reattach

	private static List<AItem> itemsOf(Layer layer) {
		return new ArrayList<>(layer.getAllItems());
	}

	@Test
	void detachTakesTheItemOutWithoutDisposingItAndReportsItsPlace() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			RectangleItem first = new RectangleItem(layer, new Rectangle2D.Double(1, 1, 1, 1));
			RectangleItem second = new RectangleItem(layer, new Rectangle2D.Double(3, 1, 1, 1));
			RectangleItem third = new RectangleItem(layer, new Rectangle2D.Double(5, 1, 1, 1));

			int index = layer.detach(second);

			assertEquals(1, index);
			assertEquals(List.of(first, third), itemsOf(layer));
			assertFalse(layer.contains(second));
			assertSame(layer, second.getLayer(), "not disposed");
			assertNotNull(second.getFocus());
			assertNotNull(second.getStyle());
		});
	}

	@Test
	void reattachRestoresTheDrawOrder() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			RectangleItem first = new RectangleItem(layer, new Rectangle2D.Double(1, 1, 1, 1));
			RectangleItem second = new RectangleItem(layer, new Rectangle2D.Double(3, 1, 1, 1));
			RectangleItem third = new RectangleItem(layer, new Rectangle2D.Double(5, 1, 1, 1));
			int index = layer.detach(second);

			assertTrue(layer.reattach(second, index));

			assertEquals(List.of(first, second, third), itemsOf(layer));
		});
	}

	@Test
	void reattachClampsAnOutOfRangeIndex() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			RectangleItem a = new RectangleItem(layer, new Rectangle2D.Double(1, 1, 1, 1));
			RectangleItem b = new RectangleItem(layer, new Rectangle2D.Double(3, 1, 1, 1));
			layer.detach(a);
			layer.detach(b);

			assertTrue(layer.reattach(a, 99));
			assertTrue(layer.reattach(b, -5));

			assertEquals(List.of(b, a), itemsOf(layer));
		});
	}

	@Test
	void detachAndReattachRefuseWhatTheyCannotDo() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			Layer other = new Layer(c, "Other");
			RectangleItem mine = new RectangleItem(layer, new Rectangle2D.Double(1, 1, 1, 1));
			RectangleItem theirs = new RectangleItem(other, new Rectangle2D.Double(1, 1, 1, 1));

			assertEquals(-1, layer.detach(null));
			assertEquals(-1, layer.detach(theirs), "not on this layer");
			assertTrue(other.contains(theirs));

			assertFalse(layer.reattach(null, 0));
			assertFalse(layer.reattach(mine, 0), "already present");
			assertFalse(layer.reattach(theirs, 0), "belongs to another layer");
			assertEquals(List.of(mine), itemsOf(layer));
		});
	}

	@Test
	void detachAndReattachNotifyListenersAsDeletedAndAdded() {
		BaseContainer c = newContainer();
		onEdt(() -> {
			Layer layer = c.getDefaultLayer();
			RectangleItem item = new RectangleItem(layer, new Rectangle2D.Double(1, 1, 1, 1));
			List<String> events = new ArrayList<>();
			layer.addItemChangeListener((l, i, type) -> {
				if (i == item) {
					events.add(type.name());
				}
			});

			int index = layer.detach(item);
			layer.reattach(item, index);

			assertEquals(List.of("DELETED", "ADDED"), events);
		});
	}
}
