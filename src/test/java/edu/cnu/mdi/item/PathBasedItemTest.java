package edu.cnu.mdi.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;
import edu.cnu.mdi.item.ItemModification.ModificationType;

/**
 * Regression coverage for {@link PathBasedItem#modify()}'s DRAG case, which
 * had zero prior test coverage.
 */
class PathBasedItemTest {

	private static final double EPS = 1.0e-9;

	private static BaseContainer newContainer() {
		AtomicReference<BaseContainer> result = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				// World (0,0)-(10,10) mapped onto a 100x100 pixel container:
				// 1 world unit == 10 pixels.
				BaseContainer container = new BaseContainer(
						new Rectangle2D.Double(0, 0, 10, 10));
				container.setSize(100, 100);
				container.setDirty(true);
				result.set(container);
			});
		} catch (Exception e) {
			throw new AssertionError("Could not create container on the EDT", e);
		}
		return result.get();
	}

	@Test
	void dragTranslatesEveryVertexByTheWorldSpaceMouseDelta() {
		BaseContainer container = newContainer();
		Layer layer = container.getDefaultLayer();

		Point2D.Double[] startPoints = {
				new Point2D.Double(2, 2),
				new Point2D.Double(4, 2),
				new Point2D.Double(3, 4)
		};
		PolygonItem item = new PolygonItem(layer, startPoints);
		item.setLocked(false);
		item.setDraggable(true);

		// Press at local (10,10) -> world (1,9); drag to local (30,30) -> world (3,7).
		// Expected world-space delta: (+2, -2).
		ItemModification modification = new ItemModification(
				item, container, new Point(10, 10), new Point(30, 30), false, false);
		modification.setType(ModificationType.DRAG);
		item.setModification(modification);

		item.modify();

		Point2D.Double[] result = WorldGraphicsUtils.pathToWorldPolygon(item.getPath());
		for (int i = 0; i < startPoints.length; i++) {
			assertEquals(startPoints[i].x + 2.0, result[i].x, EPS, "vertex " + i + " x");
			assertEquals(startPoints[i].y - 2.0, result[i].y, EPS, "vertex " + i + " y");
		}
	}

	@Test
	void dragIsANoOpWhenTheItemIsNotDraggable() {
		BaseContainer container = newContainer();
		Layer layer = container.getDefaultLayer();

		Point2D.Double[] startPoints = { new Point2D.Double(2, 2), new Point2D.Double(4, 2), new Point2D.Double(3, 4) };
		PolygonItem item = new PolygonItem(layer, startPoints);
		item.setLocked(false);
		item.setDraggable(false);

		ItemModification modification = new ItemModification(
				item, container, new Point(10, 10), new Point(30, 30), false, false);
		modification.setType(ModificationType.DRAG);
		item.setModification(modification);

		item.modify();

		Point2D.Double[] result = WorldGraphicsUtils.pathToWorldPolygon(item.getPath());
		for (int i = 0; i < startPoints.length; i++) {
			assertEquals(startPoints[i].x, result[i].x, EPS, "vertex " + i + " x unchanged");
			assertEquals(startPoints[i].y, result[i].y, EPS, "vertex " + i + " y unchanged");
		}
	}
}
