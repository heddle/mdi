package edu.cnu.mdi.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.IContainer;

/**
 * Behavior of {@link AItem}'s flags, hit testing, geometry helpers and
 * lifecycle, independent of the modification-classification coverage in
 * {@link AItemTest}.
 *
 * <p>The container maps a 10 x 5 world onto 200 x 100 pixels, i.e. 20 pixels per
 * world unit, with world y pointing up and local y pointing down.</p>
 */
class AItemBehaviorTest {

	private static final double EPS = 1.0e-9;

	private static void runOnEdt(Runnable body) {
		try {
			SwingUtilities.invokeAndWait(body);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException re) throw re;
			if (cause instanceof Error err) throw err;
			throw new AssertionError(cause);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while running Swing test", e);
		}
	}

	private static BaseContainer newContainer() {
		BaseContainer c = new BaseContainer(new Rectangle2D.Double(0, 0, 10, 5));
		c.setBounds(0, 0, 200, 100);
		c.setDirty(true);
		return c;
	}

	/** A rectangle at world (1,1) of size 2 x 1: local x 20..60, y 60..80. */
	private static RectangleItem rectangle(Layer layer) {
		RectangleItem item = new RectangleItem(layer, new Rectangle2D.Double(1, 1, 2, 1));
		item.setLocked(false);
		return item;
	}

	/** Minimal concrete item, used to observe the untouched defaults. */
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

	// ---------------------------------------------------------------- flags

	@Test
	void newItemsAreLockedAndInertByDefault() {
		runOnEdt(() -> {
			PlainItem item = new PlainItem(newContainer().getDefaultLayer());

			assertTrue(item.isLocked());
			assertFalse(item.isDraggable());
			assertFalse(item.isDeletable());
			assertFalse(item.isRotatable());
			assertFalse(item.isResizable());
			assertFalse(item.isTrackable());
			assertTrue(item.isVisible());
			assertTrue(item.isEnabled());
			assertTrue(item.isSelectable());
			assertFalse(item.isSelected());
		});
	}

	@Test
	void lockingOverridesEveryEditingFlag() {
		runOnEdt(() -> {
			PlainItem item = new PlainItem(newContainer().getDefaultLayer());
			item.setLocked(false);
			item.setDraggable(true);
			item.setDeletable(true);
			item.setRotatable(true);
			item.setResizable(true);
			assertTrue(item.isDraggable() && item.isDeletable()
					&& item.isRotatable() && item.isResizable());

			item.setLocked(true);

			assertFalse(item.isDraggable());
			assertFalse(item.isDeletable());
			assertFalse(item.isRotatable());
			assertFalse(item.isResizable());
		});
	}

	@Test
	void trackableNeedsUnlockedEnabledAndAtLeastOneEditingFlag() {
		runOnEdt(() -> {
			PlainItem item = new PlainItem(newContainer().getDefaultLayer());
			item.setLocked(false);
			item.setResizable(false);
			assertFalse(item.isTrackable(), "no editing flag set");

			item.setDraggable(true);
			assertTrue(item.isTrackable());

			item.setEnabled(false);
			assertFalse(item.isTrackable(), "disabled");
			item.setEnabled(true);

			item.setLocked(true);
			assertFalse(item.isTrackable(), "locked");
		});
	}

	@Test
	void azimuthIsNormalisedToMinus180Exclusive180Inclusive() {
		runOnEdt(() -> {
			PlainItem item = new PlainItem(newContainer().getDefaultLayer());

			item.setAzimuth(190);
			assertEquals(-170.0, item.getAzimuth(), EPS);
			item.setAzimuth(-190);
			assertEquals(170.0, item.getAzimuth(), EPS);
			item.setAzimuth(180);
			assertEquals(180.0, item.getAzimuth(), EPS);
			item.setAzimuth(-180);
			assertEquals(180.0, item.getAzimuth(), EPS);
			item.setAzimuth(720 + 45);
			assertEquals(45.0, item.getAzimuth(), EPS);
		});
	}

	@Test
	void equalityIsIdentity() {
		runOnEdt(() -> {
			Layer layer = newContainer().getDefaultLayer();
			PlainItem a = new PlainItem(layer);
			PlainItem b = new PlainItem(layer);

			assertEquals(a, a);
			assertEquals(a.hashCode(), a.hashCode());
			assertNotEquals(a, b);
		});
	}

	// ---------------------------------------------------------- hit testing

	@Test
	void containsHitsTheBodyAndMissesOutsideIt() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			RectangleItem item = rectangle(c.getDefaultLayer());

			assertTrue(item.contains(c, new Point(40, 70)));
			assertFalse(item.contains(c, new Point(120, 20)));
		});
	}

	@Test
	void selectionHandlesExtendTheHitAreaOnlyWhileSelected() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			RectangleItem item = rectangle(c.getDefaultLayer());
			Point[] handles = item.getSelectionPoints(c);
			assertNotNull(handles);
			Point first = handles[0];
			// Just outside the body but inside the 10 px handle square.
			Point justOutside = new Point(first.x, first.y - 4);
			assertFalse(item.getBounds(c).contains(justOutside), "test point must be off the body");

			assertFalse(item.contains(c, justOutside));
			item.setSelected(true);
			assertTrue(item.contains(c, justOutside));
		});
	}

	@Test
	void inSelectPointReportsTheHandleIndexOrMinusOne() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			RectangleItem item = rectangle(c.getDefaultLayer());
			Point[] handles = item.getSelectionPoints(c);

			assertEquals(-1, item.inSelectPoint(c, handles[1], false), "not selected");

			item.setSelected(true);
			assertEquals(1, item.inSelectPoint(c, handles[1], false));
			assertEquals(-1, item.inSelectPoint(c, new Point(handles[1].x + 50, handles[1].y), false));

			item.setResizable(false);
			assertEquals(-1, item.inSelectPoint(c, handles[1], true), "checkResizable on a fixed-size item");
			assertEquals(1, item.inSelectPoint(c, handles[1], false));
		});
	}

	@Test
	void inRotatePointRequiresRotatableAndSelected() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			RectangleItem item = rectangle(c.getDefaultLayer());
			item.setRotatable(true);
			Point rp = item.getRotatePoint(c);
			assertNotNull(rp);

			assertFalse(item.inRotatePoint(c, rp), "not selected");
			item.setSelected(true);
			assertTrue(item.inRotatePoint(c, rp));
			assertFalse(item.inRotatePoint(c, new Point(rp.x + 40, rp.y)));

			item.setRotatable(false);
			assertFalse(item.inRotatePoint(c, rp), "not rotatable");
		});
	}

	@Test
	void enclosedRequiresTheWholeItemInsideTheRubberBand() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			RectangleItem item = rectangle(c.getDefaultLayer());

			assertTrue(item.enclosed(c, new Rectangle(0, 0, 200, 100)));
			assertTrue(item.enclosed(c, new Rectangle(20, 60, 40, 20)), "exactly the item's bounds");
			assertFalse(item.enclosed(c, new Rectangle(20, 60, 39, 20)), "one pixel short");
			assertFalse(item.enclosed(c, new Rectangle(100, 0, 100, 100)), "disjoint");
		});
	}

	// ---------------------------------------------------------- geometry

	@Test
	void translateWorldMovesBoundsAndFocusTogether() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			RectangleItem item = rectangle(c.getDefaultLayer());
			Rectangle2D.Double before = item.getWorldBounds();
			double focusX = item.getFocus().x;
			double focusY = item.getFocus().y;

			item.translateWorld(2, -1);

			Rectangle2D.Double after = item.getWorldBounds();
			assertEquals(before.x + 2, after.x, EPS);
			assertEquals(before.y - 1, after.y, EPS);
			assertEquals(before.width, after.width, EPS);
			assertEquals(before.height, after.height, EPS);
			assertEquals(focusX + 2, item.getFocus().x, EPS);
			assertEquals(focusY - 1, item.getFocus().y, EPS);
		});
	}

	@Test
	void translateLocalConvertsPixelsToWorldUnitsWithYInverted() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			RectangleItem item = rectangle(c.getDefaultLayer());
			Rectangle2D.Double before = item.getWorldBounds();

			item.translateLocal(20, 20); // 1 unit right, 1 unit down (world y decreases)

			Rectangle2D.Double after = item.getWorldBounds();
			assertEquals(before.x + 1, after.x, EPS);
			assertEquals(before.y - 1, after.y, EPS);
		});
	}

	@Test
	void translateLocalByZeroDoesNothing() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			RectangleItem item = rectangle(c.getDefaultLayer());
			Rectangle2D.Double before = item.getWorldBounds();

			item.translateLocal(0, 0);

			assertEquals(before, item.getWorldBounds());
		});
	}

	@Test
	void focusPointIsTheFocusInPixels() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			RectangleItem item = rectangle(c.getDefaultLayer());

			// Rectangle centre is world (2, 1.5) -> local (40, 70).
			assertEquals(new Point(40, 70), item.getFocusPoint(c));

			item.setFocus(null);
			assertNull(item.getFocusPoint(c));
		});
	}

	// ---------------------------------------------------------- dirty state

	@Test
	void settingDirtyDiscardsTheCachedDrawnPolygon() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			RectangleItem item = rectangle(c.getDefaultLayer());
			BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = image.createGraphics();
			try {
				item.draw(g2, c);
			} finally {
				g2.dispose();
			}
			assertNotNull(item.getLastDrawnPolygon(), "drawing caches the screen polygon");

			item.setDirty(true);

			assertTrue(item.isDirty());
			assertNull(item.getLastDrawnPolygon());
		});
	}

	@Test
	void invisibleItemsAreNotDrawn() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			RectangleItem item = rectangle(c.getDefaultLayer());
			item.setVisible(false);
			BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = image.createGraphics();
			try {
				item.draw(g2, c);
			} finally {
				g2.dispose();
			}

			assertNull(item.getLastDrawnPolygon());
		});
	}

	// ---------------------------------------------------------- lifecycle

	@Test
	void deletingAnItemReleasesItsReferencesAndRemovesItFromTheLayer() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			Layer layer = c.getDefaultLayer();
			RectangleItem item = rectangle(layer);
			assertTrue(layer.contains(item));
			assertEquals(layer, item.getLayer());

			layer.deleteItem(item);

			assertFalse(layer.contains(item));
			assertNull(item.getLayer());
			assertNull(item.getFocus());
			assertNull(item.getStyle());
		});
	}
}
