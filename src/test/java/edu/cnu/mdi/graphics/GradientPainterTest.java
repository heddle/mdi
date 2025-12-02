package edu.cnu.mdi.graphics;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GradientPainter}.
 *
 * <p>These tests use an off-screen {@link BufferedImage} and inspect pixel
 * contents to verify that the gradient circle rendering behaves as expected.</p>
 */
public class GradientPainterTest {

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
     * @return the number of pixels whose ARGB value is not zero
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
     * Checks whether at least one pixel in a small square neighborhood around
     * a given point matches the specified color exactly.
     *
     * @param img   the image to inspect
     * @param cx    center x-coordinate
     * @param cy    center y-coordinate
     * @param color the color to search for
     * @return {@code true} if the color is found in the neighborhood
     */
    private boolean neighborhoodContainsColor(BufferedImage img,
                                              int cx,
                                              int cy,
                                              Color color) {
        int w = img.getWidth();
        int h = img.getHeight();
        int target = color.getRGB();

        for (int y = cy - 1; y <= cy + 1; y++) {
            if (y < 0 || y >= h) {
                continue;
            }
            for (int x = cx - 1; x <= cx + 1; x++) {
                if (x < 0 || x >= w) {
                    continue;
                }
                if (img.getRGB(x, y) == target) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verifies that drawing a gradient circle actually modifies some pixels
     * in the target image (i.e., the result is not completely transparent).
     */
    @Test
    void drawGradientCircle_drawsSomething() {
        BufferedImage img = createBlankImage(200, 200);
        Graphics2D g2 = img.createGraphics();

        float radius = 50f;
        Point2D.Float center = new Point2D.Float(100f, 100f);
        Color fill = Color.BLUE;

        GradientPainter.drawGradientCircle(g2, radius, fill, null, center, 1);
        g2.dispose();

        int nonTransparent = countNonTransparentPixels(img);
        assertTrue(nonTransparent > 0, "Gradient circle should modify some pixels");
    }

    /**
     * Verifies that when a non-null frame color is supplied, an outline in that
     * color appears somewhere near the circle rim.
     */
    @Test
    void drawGradientCircle_withFrameColor_drawsOutline() {
        BufferedImage img = createBlankImage(200, 200);
        Graphics2D g2 = img.createGraphics();

        float radius = 60f;
        Point2D.Float center = new Point2D.Float(100f, 100f);
        Color fill = Color.GREEN;
        Color frame = Color.MAGENTA;

        GradientPainter.drawGradientCircle(g2, radius, fill, frame, center, 2);
        g2.dispose();

        // Pick a point on the rightmost edge of the circle
        int sampleX = Math.round(center.x + radius);
        int sampleY = Math.round(center.y);

        boolean hasFrameColor = neighborhoodContainsColor(img, sampleX, sampleY, frame);
        assertTrue(hasFrameColor, "Frame color should appear near the circle rim");
    }

    /**
     * Ensures that the three different rendering modes produce visibly
     * different pixel patterns for the same center, radius, and fill color.
     */
    @Test
    void drawGradientCircle_modesProduceDifferentImages() {
        int size = 200;
        float radius = 60f;
        Point2D.Float center = new Point2D.Float(100f, 100f);
        Color fill = new Color(100, 160, 220);

        BufferedImage img1 = createBlankImage(size, size);
        BufferedImage img2 = createBlankImage(size, size);
        BufferedImage img3 = createBlankImage(size, size);

        Graphics2D g1 = img1.createGraphics();
        Graphics2D g2 = img2.createGraphics();
        Graphics2D g3 = img3.createGraphics();

        GradientPainter.drawGradientCircle(g1, radius, fill, null, center, 1);
        GradientPainter.drawGradientCircle(g2, radius, fill, null, center, 2);
        GradientPainter.drawGradientCircle(g3, radius, fill, null, center, 3);

        g1.dispose();
        g2.dispose();
        g3.dispose();

        // Compare images pixel by pixel.
        boolean diff12 = imagesDiffer(img1, img2);
        boolean diff23 = imagesDiffer(img2, img3);

        assertTrue(diff12, "Mode 1 and mode 2 should produce different images");
        assertTrue(diff23, "Mode 2 and mode 3 should produce different images");
    }

    /**
     * Compares two images pixel by pixel and returns {@code true} if at least
     * one pixel differs.
     *
     * @param a first image
     * @param b second image
     * @return {@code true} if any pixel differs; {@code false} otherwise
     */
    private boolean imagesDiffer(BufferedImage a, BufferedImage b) {
        assertEquals(a.getWidth(), b.getWidth(), "Image widths must match");
        assertEquals(a.getHeight(), b.getHeight(), "Image heights must match");

        int w = a.getWidth();
        int h = a.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }
}
