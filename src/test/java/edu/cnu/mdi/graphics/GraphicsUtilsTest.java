package edu.cnu.mdi.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.graphics.style.LineStyle;

class GraphicsUtilsTest {

    @Test
    void zeroWidthUsesDocumentedOnePixelStrokeAndCache() {
        BasicStroke zero = GraphicsUtils.getStroke(0, LineStyle.SOLID);
        BasicStroke one = GraphicsUtils.getStroke(1, LineStyle.SOLID);

        assertEquals(1.0f, zero.getLineWidth());
        assertSame(one, zero);
    }

    @Test
    void rejectsInvalidStrokeWidths() {
        assertThrows(IllegalArgumentException.class,
                () -> GraphicsUtils.getStroke(-1, LineStyle.SOLID));
        assertThrows(IllegalArgumentException.class,
                () -> GraphicsUtils.getStroke(Float.NaN, LineStyle.SOLID));
        assertThrows(NullPointerException.class,
                () -> GraphicsUtils.getStroke(1, null));
    }

    @Test
    void rectangleConstructionDoesNotOverflow() {
        Rectangle rectangle = GraphicsUtils.rectangleFromPoints(
                new Point(Integer.MIN_VALUE, Integer.MIN_VALUE),
                new Point(Integer.MAX_VALUE, Integer.MAX_VALUE));

        assertEquals(Integer.MAX_VALUE, rectangle.width);
        assertEquals(Integer.MAX_VALUE, rectangle.height);
    }

    @Test
    void colorToHexRoundTripsThroughColorFromHex() {
        Color original = new Color(0x1A, 0x2B, 0x3C, 0x4D);
        String hex = GraphicsUtils.colorToHex(original);
        assertEquals("#1a2b3c4d", hex);
        assertEquals(original, GraphicsUtils.colorFromHex(hex));
    }

    @Test
    void colorToHexOfNullIsOpaqueBlack() {
        assertEquals("#000000ff", GraphicsUtils.colorToHex(null));
    }

    @Test
    void colorFromHexAcceptsSixDigitFormsAndDefaultsToOpaque() {
        assertEquals(new Color(0x11, 0x22, 0x33, 0xff), GraphicsUtils.colorFromHex("#112233"));
        assertEquals(new Color(0x11, 0x22, 0x33, 0xff), GraphicsUtils.colorFromHex("112233"));
    }

    @Test
    void colorFromHexOfNullOrUnparseableIsBlack() {
        assertEquals(Color.black, GraphicsUtils.colorFromHex(null));
        assertEquals(Color.black, GraphicsUtils.colorFromHex("not-a-color"));
    }

    @Test
    void pointOnLineIsTrueWithinToleranceOfASegment() {
        // Horizontal segment from (0,0) to (100,0); point 2px above its midpoint.
        assertTrue(GraphicsUtils.pointOnLine(50, 2, 0, 0, 100, 0));
        // Well outside tolerance.
        assertFalse(GraphicsUtils.pointOnLine(50, 20, 0, 0, 100, 0));
        // Beyond the segment's endpoints (t out of [0,1]).
        assertFalse(GraphicsUtils.pointOnLine(150, 0, 0, 0, 100, 0));
    }

    @Test
    void pointOnLineRejectsANearDegenerateSegment() {
        assertFalse(GraphicsUtils.pointOnLine(0, 0, 0, 0, 1, 1));
    }

    @Test
    void pointOnLinePointOverloadIsNullSafe() {
        assertFalse(GraphicsUtils.pointOnLine(null, new Point(0, 0), new Point(1, 1)));
        assertFalse(GraphicsUtils.pointOnLine(new Point(0, 0), null, new Point(1, 1)));
        assertFalse(GraphicsUtils.pointOnLine(new Point(0, 0), new Point(1, 1), null));
    }

    @Test
    void rotatedTextDoesNotAlterCallerTransform() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        AffineTransform before = graphics.getTransform();
        try {
            GraphicsUtils.drawRotatedText(graphics, "test", graphics.getFont(),
                    20, 20, 35);
            assertEquals(before, graphics.getTransform());
        } finally {
            graphics.dispose();
        }
    }
}
