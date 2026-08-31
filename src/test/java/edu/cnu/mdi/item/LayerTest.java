package edu.cnu.mdi.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.IContainer;

class LayerTest {

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

    @Test
    void removingItemFromWrongLayerDoesNotDetachIt() {
        BaseContainer container = newContainer();
        Layer owner = new Layer(container, "owner");
        Layer other = new Layer(container, "other");
        PointItem item = new PointItem(owner, new Point2D.Double(2, 3));

        assertFalse(other.remove(item));
        assertSame(owner, item.getLayer());
        assertSame(container, item.getContainer());
        assertEquals(1, owner.size());
    }

    @Test
    void addRejectsForeignItemsAndIgnoresDuplicates() {
        BaseContainer container = newContainer();
        Layer owner = new Layer(container, "owner");
        Layer other = new Layer(container, "other");
        PointItem item = new PointItem(owner, new Point2D.Double(2, 3));

        owner.add(item);
        assertEquals(1, owner.size());
        assertThrows(IllegalArgumentException.class, () -> other.add(item));
        assertEquals(0, other.size());
    }

    @Test
    void drawRestoresGraphicsStateWhenItemRenderingFails() {
        BaseContainer container = newContainer();
        Layer layer = new Layer(container, "items");
        PointItem item = new PointItem(layer, new Point2D.Double(2, 3)) {
            @Override
            public void drawItem(Graphics2D g2, IContainer ignored) {
                g2.setStroke(new BasicStroke(9));
                g2.setClip(new Rectangle(20, 20, 5, 5));
                throw new IllegalStateException("test failure");
            }

            @Override
            public boolean shouldDraw(Graphics2D g2, IContainer ignored) {
                return true;
            }
        };

        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            BasicStroke originalStroke = new BasicStroke(2);
            Shape originalClip = new Rectangle(1, 2, 80, 70);
            g2.setStroke(originalStroke);
            g2.setClip(originalClip);

            assertThrows(IllegalStateException.class, () -> item.draw(g2, container));
            assertSame(originalStroke, g2.getStroke());
            assertEquals(originalClip.getBounds(), g2.getClip().getBounds());
        } finally {
            g2.dispose();
        }
    }

    @Test
    void modificationDefaultsNullMousePointsBeforeCoordinateConversion() {
        BaseContainer container = newContainer();
        PointItem item = new PointItem(container.getDefaultLayer(), new Point2D.Double(2, 3));

        ItemModification modification = new ItemModification(
                item, container, null, null, false, false);

        assertEquals(new java.awt.Point(0, 0), modification.getStartMousePoint());
        assertEquals(new java.awt.Point(0, 0), modification.getCurrentMousePoint());
        assertEquals(0.0, modification.getStartWorldPoint().x, 1.0e-12);
        assertEquals(10.0, modification.getStartWorldPoint().y, 1.0e-12);
        assertEquals(modification.getStartWorldPoint(), modification.getCurrentWorldPoint());
    }

    @Test
    void modificationRejectsMissingRequiredOwners() {
        BaseContainer container = newContainer();
        PointItem item = new PointItem(container.getDefaultLayer(), new Point2D.Double(2, 3));

        assertThrows(NullPointerException.class,
                () -> new ItemModification(null, container, null, null, false, false));
        assertThrows(NullPointerException.class,
                () -> new ItemModification(item, null, null, null, false, false));
    }

    @Test
    void zOrderingMovesItemsWithinTheLayersDrawOrder() {
        BaseContainer container = newContainer();
        Layer layer = new Layer(container, "items");
        PointItem a = new PointItem(layer, new Point2D.Double(1, 1));
        PointItem b = new PointItem(layer, new Point2D.Double(2, 2));
        PointItem c = new PointItem(layer, new Point2D.Double(3, 3));
        // Draw order is back-to-front: [a, b, c].
        assertEquals(java.util.List.of(a, b, c), layer.getAllItems());

        layer.sendToFront(a);
        assertEquals(java.util.List.of(b, c, a), layer.getAllItems());

        layer.sendToBack(c);
        assertEquals(java.util.List.of(c, b, a), layer.getAllItems());

        layer.sendForward(c);
        assertEquals(java.util.List.of(b, c, a), layer.getAllItems());

        layer.sendBackward(a);
        assertEquals(java.util.List.of(b, a, c), layer.getAllItems());

        // No-ops at the boundaries.
        layer.sendForward(c);
        assertEquals(java.util.List.of(b, a, c), layer.getAllItems());
        layer.sendBackward(b);
        assertEquals(java.util.List.of(b, a, c), layer.getAllItems());
    }

    @Test
    void lockedLayerRejectsNewSelectionButStillReportsAlreadySelectedItems() {
        BaseContainer container = newContainer();
        Layer layer = new Layer(container, "items");
        PointItem alreadySelected = new PointItem(layer, new Point2D.Double(1, 1));
        PointItem other = new PointItem(layer, new Point2D.Double(2, 2));
        // Items are locked by default; unlock them so layer-level locking is
        // the only thing under test.
        alreadySelected.setLocked(false);
        other.setLocked(false);

        // Select while unlocked, then lock the layer.
        layer.selectItem(alreadySelected, true);
        assertTrue(alreadySelected.isSelected());
        layer.setLocked(true);

        // New selection attempts on a locked layer are no-ops.
        layer.selectItem(other, true);
        assertFalse(other.isSelected());
        layer.selectAllItems(true);
        assertFalse(other.isSelected());
        layer.selectItem(alreadySelected, false);
        assertTrue(alreadySelected.isSelected(), "deselection is also a no-op while locked");

        // But an item selected before the lock is still reported as selected.
        assertEquals(java.util.List.of(alreadySelected), layer.getSelectedItems());
    }

    @Test
    void pointItemStartModificationIsANoOpWithoutAnActiveModification() {
        BaseContainer container = newContainer();
        PointItem item = new PointItem(container.getDefaultLayer(), new Point2D.Double(2, 3));

        // No modification attached; must not throw (matches the AItem contract).
        item.startModification();
    }

    @Test
    void lineCullingUsesComponentLocalCoordinates() {
        BaseContainer container = newContainer();
        container.setBounds(500, 400, 100, 100);
        container.setDirty(true);
        LineItem line = new LineItem(container.getDefaultLayer(),
                new Point2D.Double(1, 1), new Point2D.Double(9, 9));

        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            assertTrue(line.shouldDraw(g2, container));
        } finally {
            g2.dispose();
        }
    }
}
