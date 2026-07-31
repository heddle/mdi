package edu.cnu.mdi.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.BasicStroke;
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
