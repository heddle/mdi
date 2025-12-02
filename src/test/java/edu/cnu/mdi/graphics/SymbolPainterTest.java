package edu.cnu.mdi.graphics;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SymbolPainter}.
 *
 * <p>These tests render into an off-screen {@link BufferedImage} and inspect
 * pixels to verify that the expected shapes and colors are produced in
 * approximate regions. They are intentionally simple and robust rather than
 * pixel-perfect visual regression tests.</p>
 */
public class SymbolPainterTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private BufferedImage createImage(int w, int h, Color bg) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);
        g2.dispose();
        return img;
    }

    private boolean regionHasColor(BufferedImage img, Rectangle r, Color color) {
        int target = color.getRGB();
        int x0 = Math.max(0, r.x);
        int y0 = Math.max(0, r.y);
        int x1 = Math.min(img.getWidth(), r.x + r.width);
        int y1 = Math.min(img.getHeight(), r.y + r.height);

        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if (img.getRGB(x, y) == target) {
                    return true;
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void fillAndFrameRect_fillsInteriorAndDrawsFrame() {
        Color bg = Color.BLACK;
        Color fill = Color.RED;
        Color frame = Color.GREEN;

        BufferedImage img = createImage(100, 100, bg);
        Graphics2D g = img.createGraphics();

        Rectangle r = new Rectangle(10, 10, 30, 20);
        SymbolPainter.fillAndFrameRect(g, r, fill, frame);
        g.dispose();

        // Interior point
        int interior = img.getRGB(25, 20);
        assertEquals(fill.getRGB(), interior, "Interior pixel should have fill color");

        // Border point (top-left corner)
        int border = img.getRGB(10, 10);
        assertEquals(frame.getRGB(), border, "Border pixel should have frame color");

        // Outside area
        int outside = img.getRGB(5, 5);
        assertEquals(bg.getRGB(), outside, "Outside area should remain background");
    }

    @Test
    void drawRectangle_centeredRectangleUsesFillAndOutline() {
        Color bg = Color.BLACK;
        Color fill = Color.BLUE;
        Color line = Color.YELLOW;

        BufferedImage img = createImage(100, 100, bg);
        Graphics2D g = img.createGraphics();

        int x = 50;
        int y = 40;
        int w2 = 10;
        int h2 = 5;

        SymbolPainter.drawRectangle(g, x, y, w2, h2, line, fill);
        g.dispose();

        // Interior point
        assertEquals(fill.getRGB(), img.getRGB(x, y), "Center pixel should be filled");

        // Edge point (top center)
        assertEquals(line.getRGB(), img.getRGB(x, y - h2),
                "Top edge should have outline color");
    }

    @Test
    void drawOval_centeredOvalUsesFillAndOutline() {
        Color bg = Color.BLACK;
        Color fill = Color.CYAN;
        Color line = Color.MAGENTA;

        BufferedImage img = createImage(100, 100, bg);
        Graphics2D g = img.createGraphics();

        int x = 50;
        int y = 50;
        int w2 = 10;
        int h2 = 8;

        SymbolPainter.drawOval(g, x, y, w2, h2, line, fill);
        g.dispose();

        // Center should be filled
        assertEquals(fill.getRGB(), img.getRGB(x, y), "Center of oval should be fill color");

        // Some point near the right edge should be outline
        assertTrue(regionHasColor(img,
                new Rectangle(x + w2 - 1, y - 1, 2, 3),
                line),
                "Right edge region should contain outline color");
    }

    @Test
    void drawUpTriangle_drawsTriangleWithFillAndOutline() {
        Color bg = Color.BLACK;
        Color fill = Color.ORANGE;
        Color line = Color.WHITE;

        BufferedImage img = createImage(100, 100, bg);
        Graphics2D g = img.createGraphics();

        int x = 50;
        int y = 50;
        int s2 = 10;

        SymbolPainter.drawUpTriangle(g, x, y, s2, line, fill);
        g.dispose();

        // Check some interior point near bottom center
        int interior = img.getRGB(x, y + 3);
        assertEquals(fill.getRGB(), interior, "Interior of triangle should be fill color");

        // Check some point on top edge for outline-ish behavior
        assertTrue(regionHasColor(img,
                new Rectangle(x - s2, y - s2, 2 * s2 + 1, 2),
                line),
                "Top edge region should contain outline color");
    }

    @Test
    void drawDownTriangle_drawsTriangleWithFillAndOutline() {
        Color bg = Color.BLACK;
        Color fill = Color.PINK;
        Color line = Color.GREEN;

        BufferedImage img = createImage(100, 100, bg);
        Graphics2D g = img.createGraphics();

        int x = 50;
        int y = 50;
        int s2 = 10;

        SymbolPainter.drawDownTriangle(g, x, y, s2, line, fill);
        g.dispose();

        // Interior near bottom
        int interior = img.getRGB(x, y + 5);
        assertEquals(fill.getRGB(), interior, "Interior of down triangle should be fill color");

        // Check some point on top edge
        assertTrue(regionHasColor(img,
                new Rectangle(x - s2, y - s2, 2 * s2 + 1, 2),
                line),
                "Top edge region should contain outline color");
    }

    @Test
    void drawCross_drawsPlusShape() {
        Color bg = Color.BLACK;
        Color line = Color.WHITE;

        BufferedImage img = createImage(50, 50, bg);
        Graphics2D g = img.createGraphics();

        int x = 25;
        int y = 25;
        int s2 = 5;

        SymbolPainter.drawCross(g, x, y, s2, line);
        g.dispose();

        // Horizontal center
        assertEquals(line.getRGB(), img.getRGB(x + s2, y));
        assertEquals(line.getRGB(), img.getRGB(x - s2, y));

        // Vertical center
        assertEquals(line.getRGB(), img.getRGB(x, y + s2));
        assertEquals(line.getRGB(), img.getRGB(x, y - s2));
    }

    @Test
    void drawX_drawsDiagonalCross() {
        Color bg = Color.BLACK;
        Color line = Color.YELLOW;

        BufferedImage img = createImage(50, 50, bg);
        Graphics2D g = img.createGraphics();

        int x = 25;
        int y = 25;
        int s2 = 5;

        SymbolPainter.drawX(g, x, y, s2, line);
        g.dispose();

        // Diagonal 1
        assertEquals(line.getRGB(), img.getRGB(x - s2, y - s2));
        assertEquals(line.getRGB(), img.getRGB(x + s2, y + s2));

        // Diagonal 2
        assertEquals(line.getRGB(), img.getRGB(x - s2, y + s2));
        assertEquals(line.getRGB(), img.getRGB(x + s2, y - s2));
    }

    @Test
    void drawSimple3DRect_raisedAndInsetHaveDifferentEdgeColors() {
        Color bg = Color.GRAY;
        Color fill = Color.BLUE;

        Rectangle r = new Rectangle(10, 10, 30, 20);

        // Raised
        BufferedImage imgRaised = createImage(80, 60, bg);
        Graphics2D gR = imgRaised.createGraphics();
        SymbolPainter.drawSimple3DRect(gR, r, fill, true);
        gR.dispose();

        // Inset
        BufferedImage imgInset = createImage(80, 60, bg);
        Graphics2D gI = imgInset.createGraphics();
        SymbolPainter.drawSimple3DRect(gI, r, fill, false);
        gI.dispose();

        int topCenterX = r.x + r.width / 2;
        int topCenterY = r.y;

        int raisedTop = imgRaised.getRGB(topCenterX, topCenterY);
        int insetTop = imgInset.getRGB(topCenterX, topCenterY);

        assertNotEquals(raisedTop, insetTop,
                "Raised vs inset should use different top edge colors");
    }

    @Test
    void drawSimple3DOval_drawsSomethingWithinBounds() {
        Color bg = Color.BLACK;
        Color fill = Color.RED;
        Color inner = Color.ORANGE;

        Rectangle r = new Rectangle(10, 10, 40, 30);

        BufferedImage img = createImage(80, 60, bg);
        Graphics2D g = img.createGraphics();
        SymbolPainter.drawSimple3DOval(g, r, fill, inner, true);
        g.dispose();

        // Expect non-background pixels inside bounding rectangle
        assertTrue(regionHasColor(img, r, fill),
                "Outer fill color should appear inside bounding rectangle");
        assertTrue(regionHasColor(img,
                new Rectangle(r.x + 3, r.y + 3, r.width - 7, r.height - 7),
                inner),
                "Inner fill color should appear in inner oval region");
    }

    @Test
    void drawSimple3DDiamond_drawsDiamondInsideBounds() {
        Color bg = Color.BLACK;
        Color fill = Color.CYAN;

        Rectangle r = new Rectangle(10, 10, 30, 30);

        BufferedImage img = createImage(80, 80, bg);
        Graphics2D g = img.createGraphics();
        SymbolPainter.drawSimple3DDiamond(g, r, fill, true);
        g.dispose();

        // Diamond center should be filled
        int xc = r.x + r.width / 2;
        int yc = r.y + r.height / 2;

        assertEquals(fill.getRGB(), img.getRGB(xc, yc),
                "Center of diamond should be filled with fill color");

        // There should be some non-background pixels inside bounding box
        assertTrue(regionHasColor(img, r, fill),
                "Diamond fill should appear within bounding rectangle");
    }
}

