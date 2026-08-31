package edu.cnu.mdi.container;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.item.RectangleItem;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.toolbar.GestureContext;

public class BaseContainerTest {

    private static final double EPS = 1.0e-9;

    private static BaseContainer newContainer() {
        if (SwingUtilities.isEventDispatchThread()) {
            return createContainer();
        }
        AtomicReference<BaseContainer> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(createContainer()));
        } catch (Exception e) {
            throw new AssertionError("Could not create container on the EDT", e);
        }
        return result.get();
    }

    private static BaseContainer createContainer() {
        BaseContainer c = new BaseContainer(new Rectangle2D.Double(0, 0, 10, 5));
        c.setBounds(0, 0, 200, 100);
        c.setDirty(true);
        return c;
    }

    private static void runOnEdt(Runnable testBody) {
        try {
            SwingUtilities.invokeAndWait(testBody);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            if (cause instanceof Error error) throw error;
            throw new AssertionError(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while running Swing test", e);
        }
    }

    @Test
    void constructorCreatesStandardLayersAndProtectsThemFromUserList() {
        BaseContainer c = newContainer();

        assertNotNull(c.getConnectionLayer());
        assertNotNull(c.getAnnotationLayer());
        assertNotNull(c.getDefaultLayer());

        // Protected layers should NOT be discoverable by getLayerByName(), since it
        // searches only user layers.
        assertNull(c.getLayerByName("Connections"));
        assertNull(c.getLayerByName("Annotations"));

        // Default content layer should be a user layer and discoverable.
        assertSame(c.getDefaultLayer(), c.getLayerByName("Content"));

        // Draw order should be: Connections, user layers..., Annotations.
        List<Layer> all = c.getAllLayers();
        assertTrue(all.size() >= 3);
        assertSame(c.getConnectionLayer(), all.get(0));
        assertSame(c.getAnnotationLayer(), all.get(all.size() - 1));
        assertSame(c.getDefaultLayer(), all.get(1)); // with only default user layer, it should be right after Connections
    }

    @Test
    void addingUserLayersAffectsDrawOrderAndHitTestOrder() {
        BaseContainer c = newContainer();

        Layer l1 = new Layer(c, "L1"); // Layer constructor auto-registers itself.
        Layer l2 = new Layer(c, "L2");

        // Draw order: Connections, Content, L1, L2, Annotations
        List<Layer> draw = c.getAllLayers();
        assertEquals(List.of(c.getConnectionLayer(), c.getDefaultLayer(), l1, l2, c.getAnnotationLayer()), draw);

        // Hit test order: Annotations, L2, L1, Content, Connections.
        List<Layer> hit = c.getAllLayersForHitTesting();
        assertEquals(List.of(c.getAnnotationLayer(), l2, l1, c.getDefaultLayer(), c.getConnectionLayer()), hit);

        // getLayerByName only searches user layers.
        assertSame(l1, c.getLayerByName("L1"));
        assertSame(l2, c.getLayerByName("L2"));
    }

    @Test
    void localWorldTransformsHaveExpectedOrientationAndRoundTrip() {
        runOnEdt(() -> {
        BaseContainer c = newContainer();

        // Per setAffineTransforms():
        // local (0,0) -> world (minX, maxY)
        Point2D.Double w = new Point2D.Double();
        c.localToWorld(new Point(0, 0), w);
        assertEquals(0.0, w.x, EPS);
        assertEquals(5.0, w.y, EPS);

        // world (0,5) -> local (0,0)
        Point p = new Point();
        c.worldToLocal(p, new Point2D.Double(0, 5));
        assertEquals(0, p.x);
        assertEquals(0, p.y);

        // Check the far corner:
        // world (10,0) should map to local (200,100) for bounds 200x100 and world 10x5.
        c.worldToLocal(p, new Point2D.Double(10, 0));
        assertEquals(200, p.x);
        assertEquals(100, p.y);

        // Round-trip sanity on an arbitrary point
        Point local = new Point(37, 61);
        c.localToWorld(local, w);
        Point back = new Point();
        c.worldToLocal(back, w);
        assertEquals(local.x, back.x);
        assertEquals(local.y, back.y);
        });
    }

    @Test
    void negativeWorldWidthReversesTheXAxis() {
        runOnEdt(() -> {
        BaseContainer c = new BaseContainer(new Rectangle2D.Double(20, -20, -40, 40));
        c.setSize(400, 400);
        c.setAffineTransforms();

        Point left = new Point();
        Point right = new Point();
        c.worldToLocal(left, new Point2D.Double(20, 0));
        c.worldToLocal(right, new Point2D.Double(-20, 0));
        assertEquals(0, left.x);
        assertEquals(400, right.x);

        Point2D.Double world = new Point2D.Double();
        c.localToWorld(new Point(100, 200), world);
        assertEquals(10.0, world.x, EPS);
        assertEquals(0.0, world.y, EPS);
        });
    }

    @Test
    void panRecentersWorldAtExpectedLocalPoint() {
        runOnEdt(() -> {
        BaseContainer c = newContainer();

        // pan(dh,dv) recenters at (centerX - dh, centerY - dv) in local coords.
        int dh = 10;
        int dv = 0;

        int centerX = c.getBounds().width / 2;  // 100
        int centerY = c.getBounds().height / 2; // 50
        Point targetLocal = new Point(centerX - dh, centerY - dv);

        // Expected world center is whatever world point corresponds to that local point
        Point2D.Double expectedCenter = new Point2D.Double();
        c.localToWorld(targetLocal, expectedCenter);

        c.pan(dh, dv);

        Rectangle2D.Double ws = c.getWorldSystem();
        assertEquals(expectedCenter.x, ws.getCenterX(), 1e-9);
        assertEquals(expectedCenter.y, ws.getCenterY(), 1e-9);
        });
    }

    @Test
    void resetWorldSystemDoesNotAliasCallerRectangle() {
        BaseContainer c = newContainer();
        Rectangle2D.Double supplied = new Rectangle2D.Double(1, 2, 20, 10);

        c.resetWorldSystem(supplied);
        supplied.setRect(100, 200, 1, 1);

        assertEquals(new Rectangle2D.Double(1, 2, 20, 10), c.getWorldSystem());
    }

	@Test
	void draggingSelectedItemMovesOnlyEligibleSelectedItems() {
		runOnEdt(() -> {
			BaseContainer container = newContainer();
			Layer layer = container.getDefaultLayer();
			RectangleItem primary = draggableRectangle(layer, 1.0);
			RectangleItem companion = draggableRectangle(layer, 4.0);
			RectangleItem locked = draggableRectangle(layer, 6.0);
			RectangleItem notDraggable = draggableRectangle(layer, 8.0);
			locked.setLocked(true);
			notDraggable.setDraggable(false);

			primary.setSelected(true);
			companion.setSelected(true);
			locked.setSelected(true);
			notDraggable.setSelected(true);

			double primaryStartX = primary.getFocus().x;
			double companionStartX = companion.getFocus().x;
			double lockedStartX = locked.getFocus().x;
			double notDraggableStartX = notDraggable.getFocus().x;

			BaseToolHandler handler = new BaseToolHandler(container);
			BaseToolBar toolbar = new BaseToolBar(container.getComponent(), handler, 0L);
			Point press = primary.getFocusPoint(container);
			Point dragged = new Point(press.x + 20, press.y);
			MouseEvent pressEvent = mouseEvent(container, MouseEvent.MOUSE_PRESSED, press);
			MouseEvent dragEvent = mouseEvent(container, MouseEvent.MOUSE_DRAGGED, dragged);

			handler.beginDragObject(new GestureContext(toolbar, container.getComponent(), primary,
					press, pressEvent));
			handler.dragObjectBy(new GestureContext(toolbar, container.getComponent(), primary,
					dragged, dragEvent), 20, 0);
			handler.endDragObject(new GestureContext(toolbar, container.getComponent(), primary,
					dragged, dragEvent));

			assertEquals(primaryStartX + 1.0, primary.getFocus().x, EPS);
			assertEquals(companionStartX + 1.0, companion.getFocus().x, EPS);
			assertEquals(lockedStartX, locked.getFocus().x, EPS);
			assertEquals(notDraggableStartX, notDraggable.getFocus().x, EPS);
		});
	}

	@Test
	void degenerateComponentSizeLeavesConversionsAsSafeNoOps() {
		runOnEdt(() -> {
			BaseContainer c = createContainer();
			c.setBounds(0, 0, 0, 0); // 0x0 component
			c.setAffineTransforms();

			Point2D.Double w = new Point2D.Double(-1, -1);
			c.localToWorld(new Point(5, 5), w);
			assertEquals(-1, w.x, EPS, "localToWorld must leave the output untouched, not throw");
			assertEquals(-1, w.y, EPS);

			Point p = new Point(-1, -1);
			c.worldToLocal(p, new Point2D.Double(3, 4));
			assertEquals(-1, p.x, "worldToLocal must leave the output untouched, not throw");
			assertEquals(-1, p.y);
		});
	}

	@Test
	void degenerateWorldSystemLeavesConversionsAsSafeNoOps() {
		runOnEdt(() -> {
			BaseContainer c = createContainer(); // valid 200x100 bounds
			c.setWorldSystem(new Rectangle2D.Double(0, 0, 0, 5)); // zero width
			c.setAffineTransforms();

			Point2D.Double w = new Point2D.Double(-1, -1);
			c.localToWorld(new Point(5, 5), w);
			assertEquals(-1, w.x, EPS, "a zero-extent world system must leave transforms null (no-op)");
			assertEquals(-1, w.y, EPS);
		});
	}

	private static RectangleItem draggableRectangle(Layer layer, double x) {
		RectangleItem item = new RectangleItem(layer, new Rectangle2D.Double(x, 1.0, 1.0, 1.0));
		item.setLocked(false);
		item.setDraggable(true);
		item.setSelectable(true);
		return item;
	}

	private static MouseEvent mouseEvent(BaseContainer container, int id, Point point) {
		return new MouseEvent(container.getComponent(), id, System.currentTimeMillis(),
				MouseEvent.BUTTON1_DOWN_MASK, point.x, point.y, 1, false, MouseEvent.BUTTON1);
	}
}
