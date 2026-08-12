package edu.cnu.mdi.ui.fonts;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FontsTest {

    @Test
    void refreshBuildsCompleteClampedFontLadder() {
        Fonts.refresh();
        assertNotNull(Fonts.defaultFont);
        assertNotNull(Fonts.defaultBoldFont);
        assertNotNull(Fonts.defaultMono);
        assertTrue(Fonts.plainFontDelta(-10_000).getSize() >= 8);
        assertTrue(Fonts.boldFontDelta(0).isBold());
    }

    @Test
    void createsAbsoluteAndScaledCompatibilityFonts() {
        Fonts.refresh();
        assertEquals(11, Fonts.commonFont(java.awt.Font.BOLD, 11).getSize());
        assertTrue(Fonts.commonFont(java.awt.Font.BOLD, 11).isBold());
        assertEquals(18.0f, Fonts.scaleFont(Fonts.defaultFont, 1.5f).getSize2D());
        assertThrows(IllegalArgumentException.class, () -> Fonts.scaleFont(null, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> Fonts.scaleFont(Fonts.defaultFont, 0.0f));
    }
}
