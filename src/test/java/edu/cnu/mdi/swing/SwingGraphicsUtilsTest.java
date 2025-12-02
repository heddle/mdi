package edu.cnu.mdi.swing;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SwingGraphicsUtils}.
 *
 * <p>These tests validate buffer creation, image types (RGB vs ARGB),
 * null and zero-size handling, and that components can be painted into
 * the offscreen buffers.</p>
 */
public class SwingGraphicsUtilsTest {

    /**
     * Simple test component that paints its entire area with a known color.
     */
    private static class TestComponent extends Component {
        private final Color paintColor;

        TestComponent(Color paintColor, int width, int height) {
            this.paintColor = paintColor;
            setSize(width, height);
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(paintColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * Utility to check if a region of the image contains at least one pixel
     * of the specified color.
     */
    private boolean regionHasColor(BufferedImage img, int x, int y, int w, int h, Color color) {
        int target = color.getRGB();
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(img.getWidth(), x + w);
        int y1 = Math.min(img.getHeight(), y + h);

        for (int yy = y0; yy < y1; yy++) {
            for (int xx = x0; xx < x1; xx++) {
                if (img.getRGB(xx, yy) == target) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * createComponentImageBuffer should return {@code null} for a null component.
     */
    @Test
    void createComponentImageBuffer_nullComponent_returnsNull() {
        assertNull(SwingGraphicsUtils.createComponentImageBuffer(null),
                "Null component should yield null buffer");
    }

    /**
     * createComponentImageBuffer should return {@code null} if component
     * has zero size.
     */
    @Test
    @SuppressWarnings("deprecation")
    void createComponentImageBuffer_zeroSize_returnsNull() {
        Component c = new TestComponent(Color.BLACK, 0, 0);
        assertEquals(new Dimension(0, 0), c.getSize(), "Expected size 0x0");

        assertNull(SwingGraphicsUtils.createComponentImageBuffer(c),
                "Zero-size component should yield null buffer");
    }

    /**
     * For a valid-sized component, createComponentImageBuffer should create
     * an RGB image with matching dimensions.
     */
    @Test
    void createComponentImageBuffer_validComponent_createsRgbImage() {
        Component c = new TestComponent(Color.BLACK, 50, 30);

        BufferedImage img = SwingGraphicsUtils.createComponentImageBuffer(c);

        assertNotNull(img, "Buffer should not be null for valid size");
        assertEquals(50, img.getWidth(), "Width should match component size");
        assertEquals(30, img.getHeight(), "Height should match component size");
        assertEquals(BufferedImage.TYPE_INT_RGB, img.getType(), "Image type should be TYPE_INT_RGB");
    }

    /**
     * createComponentTranslucentImageBuffer should behave similarly but
     * with ARGB image type.
     */
    @Test
    void createComponentTranslucentImageBuffer_createsArgbImage() {
        Component c = new TestComponent(Color.BLACK, 40, 20);

        BufferedImage img = SwingGraphicsUtils.createComponentTranslucentImageBuffer(c);

        assertNotNull(img, "Buffer should not be null for valid size");
        assertEquals(40, img.getWidth());
        assertEquals(20, img.getHeight());
        assertEquals(BufferedImage.TYPE_INT_ARGB, img.getType(), "Image type should be TYPE_INT_ARGB");
    }

    /**
     * paintComponentOnImage should be a no-op for null component or null image.
     */
    @Test
    void paintComponentOnImage_nullArguments_isNoOp() {
        // These calls should not throw.
        SwingGraphicsUtils.paintComponentOnImage(null, null);

        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        SwingGraphicsUtils.paintComponentOnImage(null, img);
    }

    /**
     * paintComponentOnImage should cause our test component's color to appear
     * in the buffer.
     */
    @Test
    void paintComponentOnImage_rendersComponentIntoBuffer() {
        Color paintColor = Color.MAGENTA;
        TestComponent c = new TestComponent(paintColor, 40, 30);

        BufferedImage img = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);

        // Fill image with a different color first
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.setRGB(x, y, Color.BLACK.getRGB());
            }
        }

        SwingGraphicsUtils.paintComponentOnImage(c, img);

        assertTrue(regionHasColor(img, 0, 0, 40, 30, paintColor),
                "Rendered image should contain the component's paint color");
    }

    /**
     * getComponentImage is a convenience that combines buffer creation and painting
     * for opaque images.
     */
    @Test
    void getComponentImage_createsAndPaintsOpaqueImage() {
        Color paintColor = Color.RED;
        TestComponent c = new TestComponent(paintColor, 30, 20);

        BufferedImage img = SwingGraphicsUtils.getComponentImage(c);

        assertNotNull(img, "Image should not be null for valid component");
        assertEquals(30, img.getWidth());
        assertEquals(20, img.getHeight());
        assertEquals(BufferedImage.TYPE_INT_RGB, img.getType());

        assertTrue(regionHasColor(img, 0, 0, 30, 20, paintColor),
                "Opaque snapshot should contain the component's paint color");
    }

    /**
     * getComponentTranslucentImage creates an ARGB buffer and paints the component
     * into it.
     */
    @Test
    void getComponentTranslucentImage_createsAndPaintsArgbImage() {
        Color paintColor = Color.GREEN;
        TestComponent c = new TestComponent(paintColor, 25, 15);

        BufferedImage img = SwingGraphicsUtils.getComponentTranslucentImage(c);

        assertNotNull(img, "Image should not be null for valid component");
        assertEquals(25, img.getWidth());
        assertEquals(15, img.getHeight());
        assertEquals(BufferedImage.TYPE_INT_ARGB, img.getType());

        assertTrue(regionHasColor(img, 0, 0, 25, 15, paintColor),
                "Translucent snapshot should contain the component's paint color");
    }

    /**
     * getComponentImage and getComponentTranslucentImage should return null when
     * the component size is zero or negative.
     */
    @Test
    void getComponentImage_zeroSize_returnsNull() {
        TestComponent c = new TestComponent(Color.BLUE, 0, 0);

        assertNull(SwingGraphicsUtils.getComponentImage(c),
                "Zero-size component should yield null opaque snapshot");
        assertNull(SwingGraphicsUtils.getComponentTranslucentImage(c),
                "Zero-size component should yield null translucent snapshot");
    }
}
