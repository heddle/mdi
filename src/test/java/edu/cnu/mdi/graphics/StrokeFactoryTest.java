package edu.cnu.mdi.graphics;

import edu.cnu.mdi.graphics.style.LineStyle;
import org.junit.jupiter.api.Test;

import java.awt.BasicStroke;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StrokeFactory}.
 *
 * <p>This test suite validates the caching behavior, dash-array correctness,
 * line-width handling, and the behavior of {@link StrokeFactory#copyWithNewWidth}.
 * Tests are intentionally concise, quiet, and deterministic.</p>
 */
public class StrokeFactoryTest {

    /**
     * Repeated calls with the same width/style should return the same cached instance.
     */
    @Test
    void get_returnsCachedInstance() {
        BasicStroke a = StrokeFactory.get(2.0f, LineStyle.DASH);
        BasicStroke b = StrokeFactory.get(2.0f, LineStyle.DASH);
        assertSame(a, b, "Expected cached instance for repeated get()");
    }

    /**
     * Different line styles must not collide in the cache.
     */
    @Test
    void get_differentStylesReturnDifferentInstances() {
        BasicStroke solid = StrokeFactory.get(1.0f, LineStyle.SOLID);
        BasicStroke dash = StrokeFactory.get(1.0f, LineStyle.DASH);
        assertNotSame(solid, dash, "Different styles should produce different strokes");
    }

    /**
     * The returned stroke should correctly reflect the requested width.
     */
    @Test
    void get_widthIsAppliedCorrectly() {
        for (float width : new float[] {0.5f, 1.0f, 3.5f}) {
            BasicStroke s = StrokeFactory.get(width, LineStyle.DOT);
            assertEquals(width, s.getLineWidth(), 1e-6f, "Stroke width should match request");
        }
    }

    /**
     * Solid strokes should have no dash array; dashed styles should have a dash pattern.
     */
    @Test
    void get_dashArrayCorrectness() {
        assertNull(
                StrokeFactory.get(2.0f, LineStyle.SOLID).getDashArray(),
                "SOLID strokes should not have dash array"
        );

        assertNotNull(
                StrokeFactory.get(2.0f, LineStyle.DASH).getDashArray(),
                "DASH strokes should have dash array"
        );

        assertNotNull(
                StrokeFactory.get(2.0f, LineStyle.DOT_DASH).getDashArray(),
                "DOT_DASH strokes should have dash array"
        );

        assertNotNull(
                StrokeFactory.get(2.0f, LineStyle.DOT).getDashArray(),
                "DOT strokes should have dash array"
        );
    }

    /**
     * copyWithNewWidth should preserve everything except the width.
     */
    @Test
    void copyWithNewWidth_preservesAttributesExceptWidth() {
        BasicStroke orig = StrokeFactory.get(2.0f, LineStyle.LONG_DASH);
        BasicStroke copy = StrokeFactory.copyWithNewWidth(orig, 6.0f);

        assertEquals(6.0f, copy.getLineWidth(), 1e-6f, "Width should be changed");
        assertEquals(orig.getEndCap(), copy.getEndCap());
        assertEquals(orig.getLineJoin(), copy.getLineJoin());
        assertEquals(orig.getMiterLimit(), copy.getMiterLimit());
        assertEquals(orig.getDashPhase(), copy.getDashPhase(), 1e-6f);

        if (orig.getDashArray() == null) {
            assertNull(copy.getDashArray());
        } else {
            assertArrayEquals(orig.getDashArray(), copy.getDashArray(), "Dash arrays must match");
        }
    }

    /**
     * Predefined highlight/dash strokes should be non-null.
     */
    @Test
    void predefinedStrokes_areNotNull() {
        assertNotNull(StrokeFactory.HIGHLIGHT_DASH1);
        assertNotNull(StrokeFactory.HIGHLIGHT_DASH2);
        assertNotNull(StrokeFactory.HIGHLIGHT_DASH1_2);
        assertNotNull(StrokeFactory.HIGHLIGHT_DASH2_2);
        assertNotNull(StrokeFactory.HIGHLIGHT_DASH1_2T);
        assertNotNull(StrokeFactory.HIGHLIGHT_DASH2_2T);
        assertNotNull(StrokeFactory.SIMPLE_DASH);
        assertNotNull(StrokeFactory.SIMPLE_DASH2);
    }
}
