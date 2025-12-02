package edu.cnu.mdi.graphics;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ArrowPainter}.
 *
 * <p>These tests verify that:</p>
 * <ul>
 *     <li>Arrows are drawn onto an image without throwing exceptions.</li>
 *     <li>Very short arrow requests result in no drawing.</li>
 *     <li>Highlighted arrows delegate to {@link HighlightPainter} and use
 *         the specified highlight colors.</li>
 * </ul>
 */
public class ArrowPainterTest {

    /**
     * Creates a blank ARGB image initialized to fully transparent pixels.
     *
     * @param width  image width in pixels
     * @param height image height in pixels
     * @return a new {@link BufferedImage} with type {@link BufferedImage#TYPE_INT_ARGB}
     */
    private BufferedImage createBlankImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Counts the number of pixels in the image that are not fully transparent.
     *
     * @param img the image to scan
     * @return number of pixels with a non-zero ARGB value
     */
    private int countNonTransparentPixels(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int count = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (img.getRGB(x, y) != 0x00000000) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Tests that a basic, non-highlighted arrow call draws something onto
     * the image and does not throw an exception.
     */
    @Test
    void drawArrow_simpleArrow_drawsOnImage() {
        BufferedImage img = createBlankImage(200, 100);
        Graphics2D g2 = img.createGraphics();

        // Draw a simple horizontal arrow
        ArrowPainter.drawArrow(g2, 20, 50, 180, 50);
        g2.dispose();

        int nonTransparent = countNonTransparentPixels(img);
        assertTrue(nonTransparent > 0, "Simple arrow should modify some pixels");
    }

    /**
     * Tests that very short arrows (with endpoints within 3 pixels in both
     * x and y) are ignored as specified by {@link ArrowPainter}'s early exit
     * condition.
     */
    @Test
    void drawArrow_tooShort_doesNotDraw() {
        BufferedImage img = createBlankImage(50, 50);
        Graphics2D g2 = img.createGraphics();

        // This should be considered too short: |dx| < 4 and |dy| < 4
        ArrowPainter.drawArrow(g2, 10, 10, 12, 12);
        g2.dispose();

        int nonTransparent = countNonTransparentPixels(img);
        assertEquals(0, nonTransparent, "Too-short arrow should not draw any pixels");
    }

    /**
     * Tests that a highlighted arrow draws something on the image and uses
     * the overlay highlight color (via {@link HighlightPainter}).
     */
    @Test
    void drawArrow_withHighlight_usesHighlightColors() {
        BufferedImage img = createBlankImage(300, 150);
        Graphics2D g2 = img.createGraphics();

        Color c1 = Color.CYAN;
        Color c2 = Color.MAGENTA;

        // Draw a diagonal highlighted arrow
        ArrowPainter.drawArrow(g2, 30, 30, 270, 120, true, c1, c2);
        g2.dispose();

        int nonTransparent = countNonTransparentPixels(img);
        assertTrue(nonTransparent > 0, "Highlighted arrow should modify some pixels");

        // We expect at least some pixels with the overlay color c2,
        // since HighlightPainter draws the thinner overlay with this color.
        boolean foundOverlay = false;
        int overlayRGB = c2.getRGB();
        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y < h && !foundOverlay; y++) {
            for (int x = 0; x < w && !foundOverlay; x++) {
                if (img.getRGB(x, y) == overlayRGB) {
                    foundOverlay = true;
                }
            }
        }

        assertTrue(foundOverlay, "Overlay highlight color (c2) should appear in the arrow shaft");
    }

    /**
     * Tests that calling the convenience overload
     * {@link ArrowPainter#drawArrow(java.awt.Graphics, int, int, int, int)}
     * (without highlight parameters) functions correctly and draws content.
     */
    @Test
    void drawArrow_convenienceOverload_drawsArrow() {
        BufferedImage img = createBlankImage(200, 200);
        Graphics2D g2 = img.createGraphics();

        // Use the 4-argument overload (no highlight)
        ArrowPainter.drawArrow(g2, 50, 150, 150, 50);
        g2.dispose();

        int nonTransparent = countNonTransparentPixels(img);
        assertTrue(nonTransparent > 0, "Convenience overload should draw an arrow");
    }
}
