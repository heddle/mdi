package edu.cnu.mdi.container;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.item.Layer;

public class BaseContainerTest {

    private static final double EPS = 1.0e-9;

    private static BaseContainer newContainer() {
        AtomicReference<BaseContainer> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                // World: x in [0,10], y in [0,5].
                BaseContainer c = new BaseContainer(new Rectangle2D.Double(0, 0, 10, 5));
                c.setBounds(0, 0, 200, 100);
                c.setDirty(true);
                result.set(c);
            });
        } catch (Exception e) {
            throw new AssertionError("Could not create container on the EDT", e);
        }
        return result.get();
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
        BaseContainer c = newContainer();

        // Per setAffineTransforms():
        // local (0,0) -> world (minX, maxY) :contentReference[oaicite:11]{index=11}
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
    }

    @Test
    void panRecentersWorldAtExpectedLocalPoint() {
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
    }

    @Test
    void resetWorldSystemDoesNotAliasCallerRectangle() {
        BaseContainer c = newContainer();
        Rectangle2D.Double supplied = new Rectangle2D.Double(1, 2, 20, 10);

        c.resetWorldSystem(supplied);
        supplied.setRect(100, 200, 1, 1);

        assertEquals(new Rectangle2D.Double(1, 2, 20, 10), c.getWorldSystem());
    }
}
