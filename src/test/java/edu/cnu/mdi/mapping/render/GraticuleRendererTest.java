package edu.cnu.mdi.mapping.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GraticuleRendererTest {

    @Test
    void fullProjectedWidthDetectionAllowsBoundaryInset() {
        assertTrue(GraticuleRenderer.coversFullProjectedWidth(6.25, 2.0 * Math.PI));
        assertTrue(GraticuleRenderer.coversFullProjectedWidth(100.0, 100.0));
        assertFalse(GraticuleRenderer.coversFullProjectedWidth(95.0, 100.0));
        assertFalse(GraticuleRenderer.coversFullProjectedWidth(Double.NaN, 100.0));
        assertFalse(GraticuleRenderer.coversFullProjectedWidth(100.0, 0.0));
    }
}
