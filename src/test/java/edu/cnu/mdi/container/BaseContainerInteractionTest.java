package edu.cnu.mdi.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.item.RectangleItem;

/**
 * Container-level hit testing, selection, deletion and zoom behavior.
 *
 * <p>The container maps a 10 x 5 world onto 200 x 100 pixels (20 pixels per
 * world unit, world y up, local y down). Every item below is a 2 x 1 world
 * rectangle, so a rectangle whose lower-left corner is world (x, y) covers
 * local x in [20x, 20x + 40] and local y in [100 - 20(y + 1), 100 - 20y].</p>
 */
class BaseContainerInteractionTest {

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

	private static RectangleItem rectangle(Layer layer, double x, double y) {
		RectangleItem item = new RectangleItem(layer, new Rectangle2D.Double(x, y, 2, 1));
		item.setLocked(false);
		item.setDeletable(true);
		return item;
	}

	// ------------------------------------------------------------ hit testing

	@Test
	void topmostLayerWinsWhereItemsOverlap() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			Layer upper = new Layer(c, "Upper");
			RectangleItem below = rectangle(c.getDefaultLayer(), 1, 1);
			RectangleItem above = rectangle(upper, 1, 1);
			Point inBoth = new Point(40, 70);

			assertSame(above, c.getItemAtPoint(inBoth));
			assertEquals(List.of(above, below), c.getItemsAtPoint(inBoth), "top to bottom");
		});
	}

	@Test
	void hitTestingFindsNothingAwayFromItems() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			rectangle(c.getDefaultLayer(), 1, 1);
			Point empty = new Point(180, 10);

			assertNull(c.getItemAtPoint(empty));
			assertTrue(c.getItemsAtPoint(empty).isEmpty());
		});
	}

	@Test
	void hitTestingToleratesANullPoint() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			rectangle(c.getDefaultLayer(), 1, 1);

			assertNull(c.getItemAtPoint(null));
			assertTrue(c.getItemsAtPoint(null).isEmpty());
		});
	}

	@Test
	void hiddenAndLockedLayersAreSkippedByHitTesting() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			Layer hidden = new Layer(c, "Hidden");
			Layer locked = new Layer(c, "Locked");
			RectangleItem base = rectangle(c.getDefaultLayer(), 1, 1);
			rectangle(hidden, 1, 1);
			rectangle(locked, 1, 1);
			Point p = new Point(40, 70);
			hidden.setVisible(false);
			locked.setLocked(true);

			assertSame(base, c.getItemAtPoint(p));
			assertEquals(List.of(base), c.getItemsAtPoint(p));
		});
	}

	@Test
	void enclosedItemsAreThoseEntirelyInsideTheRubberBand() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			Layer other = new Layer(c, "Other");
			RectangleItem left = rectangle(c.getDefaultLayer(), 1, 1);   // local x 20..60
			RectangleItem right = rectangle(other, 6, 1);                // local x 120..160

			assertEquals(List.of(left), c.getEnclosedItems(new Rectangle(0, 0, 100, 100)));
			assertEquals(2, c.getEnclosedItems(new Rectangle(0, 0, 200, 100)).size());
			assertTrue(c.getEnclosedItems(new Rectangle(0, 0, 200, 100)).contains(right));
			assertTrue(c.getEnclosedItems(new Rectangle(30, 0, 60, 100)).isEmpty(),
					"a band that clips an item does not enclose it");
			assertNull(c.getEnclosedItems(null));
		});
	}

	@Test
	void enclosedItemsSkipHiddenLayers() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			Layer hidden = new Layer(c, "Hidden");
			rectangle(hidden, 1, 1);
			hidden.setVisible(false);

			assertTrue(c.getEnclosedItems(new Rectangle(0, 0, 200, 100)).isEmpty());
		});
	}

	// -------------------------------------------------------------- selection

	@Test
	void selectAllItemsSpansLayersAndRespectsLockedLayers() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			Layer open = new Layer(c, "Open");
			Layer locked = new Layer(c, "Locked");
			RectangleItem a = rectangle(c.getDefaultLayer(), 0, 0);
			RectangleItem b = rectangle(open, 3, 1);
			RectangleItem inLocked = rectangle(locked, 6, 2);
			locked.setLocked(true);

			assertFalse(c.anySelectedItems());

			c.selectAllItems(true);

			assertTrue(c.anySelectedItems());
			assertTrue(a.isSelected());
			assertTrue(b.isSelected());
			assertFalse(inLocked.isSelected(), "locked layers are not selectable in bulk");
			assertEquals(2, c.getSelectedItems().size());

			c.selectAllItems(false);

			assertFalse(c.anySelectedItems());
			assertTrue(c.getSelectedItems().isEmpty());
		});
	}

	@Test
	void selectedItemsAreReportedEvenFromAHiddenLayer() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			Layer layer = new Layer(c, "L");
			RectangleItem item = rectangle(layer, 1, 1);
			item.setSelected(true);
			layer.setVisible(false);

			assertTrue(c.anySelectedItems());
			assertEquals(List.<AItem>of(item), c.getSelectedItems());
		});
	}

	@Test
	void deleteSelectedItemsRemovesOnlySelectedDeletableItemsInOpenLayers() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
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

			c.deleteSelectedItems();

			assertFalse(open.contains(doomed));
			assertTrue(open.contains(unselected));
			assertTrue(open.contains(protectedItem), "non-deletable items survive");
			assertTrue(locked.contains(inLocked), "locked layer is untouched");
			assertTrue(hidden.contains(inHidden), "hidden layer is untouched");
		});
	}

	// ------------------------------------------------------------------- zoom

	@Test
	void zoomSetsTheWorldAndUndoRestoresThePreviousOne() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			Rectangle2D.Double original = new Rectangle2D.Double(0, 0, 10, 5);

			c.zoom(2, 6, 1, 3);

			assertEquals(new Rectangle2D.Double(2, 1, 4, 2), c.getWorldSystem());

			c.undoLastZoom();

			assertEquals(original, c.getWorldSystem());
		});
	}

	@Test
	void undoingTwiceTogglesBetweenTheTwoWorlds() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			c.zoom(2, 6, 1, 3);
			Rectangle2D.Double zoomed = c.getWorldSystem();

			c.undoLastZoom();
			c.undoLastZoom();

			assertEquals(zoomed, c.getWorldSystem());
		});
	}

	@Test
	void zoomTransformsFollowTheNewWorld() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();

			c.zoom(2, 6, 1, 3); // 4 x 2 world on 200 x 100 px: 50 px per unit

			Point p = new Point();
			c.worldToLocal(p, new java.awt.geom.Point2D.Double(2, 3));
			assertEquals(0, p.x);
			assertEquals(0, p.y);
			c.worldToLocal(p, new java.awt.geom.Point2D.Double(6, 1));
			assertEquals(200, p.x);
			assertEquals(100, p.y);
		});
	}

	@Test
	void scaleResizesAboutTheCenterAndUndoReverts() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();

			c.scale(0.5);

			Rectangle2D.Double ws = c.getWorldSystem();
			assertEquals(5.0, ws.width, EPS);
			assertEquals(2.5, ws.height, EPS);
			assertEquals(5.0, ws.getCenterX(), EPS);
			assertEquals(2.5, ws.getCenterY(), EPS);

			c.undoLastZoom();

			assertEquals(new Rectangle2D.Double(0, 0, 10, 5), c.getWorldSystem());
		});
	}

	@Test
	void restoreDefaultWorldReturnsToTheConstructionWorld() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			c.zoom(2, 6, 1, 3);
			c.scale(2);

			c.restoreDefaultWorld();

			assertEquals(new Rectangle2D.Double(0, 0, 10, 5), c.getWorldSystem());
		});
	}

	@Test
	void restoreDefaultWorldHandsBackACopyNotTheStoredDefault() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			c.restoreDefaultWorld();
			Rectangle2D.Double first = c.getWorldSystem();
			first.width = 999; // caller mutates what it was given

			c.zoom(1, 2, 1, 2);
			c.restoreDefaultWorld();

			assertEquals(new Rectangle2D.Double(0, 0, 10, 5), c.getWorldSystem());
			assertNotSame(first, c.getWorldSystem());
		});
	}

	@Test
	void resetWorldSystemMakesTheNewWorldTheDefault() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			c.resetWorldSystem(new Rectangle2D.Double(-5, -5, 20, 10));
			c.zoom(0, 1, 0, 1);

			c.restoreDefaultWorld();

			assertEquals(new Rectangle2D.Double(-5, -5, 20, 10), c.getWorldSystem());
		});
	}

	@Test
	void rubberBandSmallerThanTenPixelsDoesNotZoom() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();

			c.rubberBanded(new Rectangle(10, 10, 9, 50));
			c.rubberBanded(new Rectangle(10, 10, 50, 9));

			assertEquals(new Rectangle2D.Double(0, 0, 10, 5), c.getWorldSystem());
		});
	}

	@Test
	void rubberBandZoomsToTheEnclosedWorldRegion() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();

			// Top-left quarter of the component: world x 0..5, y 2.5..5.
			c.rubberBanded(new Rectangle(0, 0, 100, 50));

			Rectangle2D.Double ws = c.getWorldSystem();
			assertEquals(0.0, ws.x, EPS);
			assertEquals(2.5, ws.y, EPS);
			assertEquals(5.0, ws.width, EPS);
			assertEquals(2.5, ws.height, EPS);
		});
	}

	// ---------------------------------------------------------- dirty marking

	@Test
	void settingDirtyMarksItemsOnEveryLayerIncludingProtectedOnes() {
		runOnEdt(() -> {
			BaseContainer c = newContainer();
			Layer other = new Layer(c, "Other");
			RectangleItem a = rectangle(c.getDefaultLayer(), 0, 0);
			RectangleItem b = rectangle(other, 3, 3);
			RectangleItem annotation = rectangle(c.getAnnotationLayer(), 6, 1);
			a.setDirty(false);
			b.setDirty(false);
			annotation.setDirty(false);

			c.setDirty(true);

			assertTrue(a.isDirty());
			assertTrue(b.isDirty());
			assertTrue(annotation.isDirty());
		});
	}
}
