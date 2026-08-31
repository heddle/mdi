package edu.cnu.mdi.graphics.style;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StyledTest {

    @Test
    void negativeStyleIndexIsSupportedAsDocumented() {
        assertDoesNotThrow(() -> new Styled(-1));
    }

    @Test
    void styleValuesAreNormalized() {
        Styled style = new Styled();
        style.setLineWidth(-2);
        style.setAuxLineWidth(-3);
        style.setSymbolSize(-4);
        style.setAuxLineStyle(null);

        assertEquals(0, style.getLineWidth());
        assertEquals(0, style.getAuxLineWidth());
        assertEquals(0, style.getSymbolSize());
        assertEquals(LineStyle.SOLID, style.getAuxLineStyle());
        assertThrows(IllegalArgumentException.class, () -> style.setLineWidth(Float.NaN));
    }
}
