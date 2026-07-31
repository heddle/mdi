package edu.cnu.mdi.ui.fonts;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
