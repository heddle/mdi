package edu.cnu.mdi.graphics;

import org.junit.jupiter.api.Test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HighlightPainter}.
 *
 * <p>These tests verify that:</p>
 * <ul>
 *     <li>The highlighted line drawing routine modifies pixels on the image.</li>
 *     <li>The specified colors are actually used in the rendered output.</li>
 *     <li>The graphics state (color and stroke) is restored after drawing.</li>
 * </ul>
 */
public class HighlightPainterTest {

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
     * Verifies that {@link HighlightPainter#drawHighlightedLine(Graphics2D, int, int, int, int, Color, Color)}
     * actually draws onto the image (i.e., some pixels become non-transparent).
     */
    @Test
    void drawHighlightedLine_drawsSomethingOnImage() {
        BufferedImage img = createBlankImage(200, 100);
        Graphics2D g2 = img.createGraphics();

        Color c1 = Color.RED;
        Color c2 = Color.BLUE;

        HighlightPainter.drawHighlightedLine(g2, 10, 50, 190, 50, c1, c2);
        g2.dispose();

        int nonTransparent = countNonTransparentPixels(img);
        assertTrue(nonTransparent > 0, "Highlighted line should modify some pixels");
    }

    /**
     * Verifies that the overlay color (second color argument) appears in the
     * rendered line. Since the overlay stroke is drawn last and thinner, at
     * least some pixels should match {@code c2}.
     */
    @Test
    void drawHighlightedLine_usesOverlayColor() {
        BufferedImage img = createBlankImage(200, 100);
        Graphics2D g2 = img.createGraphics();

        Color c1 = Color.YELLOW;
        Color c2 = Color.MAGENTA;

        HighlightPainter.drawHighlightedLine(g2, 20, 20, 180, 80, c1, c2);
        g2.dispose();

        boolean foundOverlayColor = false;
        int w = img.getWidth();
        int h = img.getHeight();
        int overlayRGB = c2.getRGB();

        for (int y = 0; y < h && !foundOverlayColor; y++) {
            for (int x = 0; x < w && !foundOverlayColor; x++) {
                if (img.getRGB(x, y) == overlayRGB) {
                    foundOverlayColor = true;
                }
            }
        }

        assertTrue(foundOverlayColor, "Overlay color (c2) should appear in the rendered line");
    }

    /**
     * Ensures that the graphics context's color and stroke are restored to
     * their original values after calling the highlight drawing method.
     */
    @Test
    void drawHighlightedLine_restoresGraphicsState() {
        BufferedImage img = createBlankImage(200, 100);
        Graphics2D g2 = img.createGraphics();

        Color originalColor = Color.GREEN;
        Stroke originalStroke = new BasicStroke(1.0f);

        g2.setColor(originalColor);
        g2.setStroke(originalStroke);

        HighlightPainter.drawHighlightedLine(g2, 10, 10, 190, 90,
                HighlightPainter.DEFAULT_COLOR1,
                HighlightPainter.DEFAULT_COLOR2);

        // Verify that both color and stroke were restored
        assertEquals(originalColor, g2.getColor(), "Graphics color should be restored after drawing");
        assertSame(originalStroke, g2.getStroke(), "Graphics stroke should be restored after drawing");

        g2.dispose();
    }
}
