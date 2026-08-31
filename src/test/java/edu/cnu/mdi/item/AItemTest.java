package edu.cnu.mdi.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.ItemModification.ModificationType;

/**
 * Regression coverage for {@link AItem#startModification()}'s classification
 * priority: rotate handle &gt; resize (selection) handle &gt; drag.
 */
class AItemTest {

	private static BaseContainer newContainer() {
		AtomicReference<BaseContainer> result = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
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

	/**
	 * A minimal item, exercising the generic {@link AItem#startModification()}
	 * classification (not overridden, unlike e.g. {@link PointItem}), with
	 * fixed, overridable rotate/selection handle positions.
	 */
	private static final class HandleTestItem extends AItem {
		private Point rotatePoint;
		private Point[] selectionPoints;

		HandleTestItem(Layer layer) {
			super(layer);
			setLocked(false);
			setRotatable(true);
			setResizable(true);
			setSelected(true);
		}

		@Override
		public Point getRotatePoint(IContainer container) {
			return rotatePoint;
		}

		@Override
		public Point[] getSelectionPoints(IContainer container) {
			return selectionPoints;
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

	private static ItemModification modificationAt(HandleTestItem item, BaseContainer container, Point pressPoint) {
		return new ItemModification(item, container, pressPoint, pressPoint, false, false);
	}

	@Test
	void rotateHandleTakesPriorityOverAnOverlappingSelectionHandle() {
		BaseContainer container = newContainer();
		HandleTestItem item = new HandleTestItem(container.getDefaultLayer());
		Point handle = new Point(50, 50);
		item.rotatePoint = handle;
		item.selectionPoints = new Point[] { handle }; // deliberately overlapping

		item.setModification(modificationAt(item, container, handle));
		item.startModification();

		assertEquals(ModificationType.ROTATE, item.getItemModification().getType());
	}

	@Test
	void selectionHandleWinsWhenTheRotateHandleIsNotHit() {
		BaseContainer container = newContainer();
		HandleTestItem item = new HandleTestItem(container.getDefaultLayer());
		item.rotatePoint = new Point(50, 50);
		Point handle = new Point(200, 200);
		item.selectionPoints = new Point[] { new Point(1, 1), handle };

		item.setModification(modificationAt(item, container, handle));
		item.startModification();

		assertEquals(ModificationType.RESIZE, item.getItemModification().getType());
		assertEquals(1, item.getItemModification().getSelectIndex());
	}

	@Test
	void neitherHandleHitDefaultsToDrag() {
		BaseContainer container = newContainer();
		HandleTestItem item = new HandleTestItem(container.getDefaultLayer());
		item.rotatePoint = new Point(50, 50);
		item.selectionPoints = new Point[] { new Point(200, 200) };

		item.setModification(modificationAt(item, container, new Point(500, 500)));
		item.startModification();

		assertEquals(ModificationType.DRAG, item.getItemModification().getType());
	}

	@Test
	void startModificationIsANoOpWithoutAnActiveModification() {
		BaseContainer container = newContainer();
		HandleTestItem item = new HandleTestItem(container.getDefaultLayer());

		// No modification attached; must not throw.
		item.startModification();
	}
}
