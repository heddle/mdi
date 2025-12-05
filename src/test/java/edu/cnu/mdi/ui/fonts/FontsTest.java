package edu.cnu.mdi.ui.fonts;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Font;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Fonts}.
 */
class FontsTest {

    @Test
    @DisplayName("commonFont returns non-null font with correct size and style")
    void testCommonFontBasicProperties() {
        int style = Font.BOLD;
        int size = 16;

        Font font = Fonts.commonFont(style, size);

        assertNotNull(font, "commonFont should not return null");
        assertEquals(size, font.getSize(), "Font size should match requested size");
        assertEquals(style, font.getStyle(), "Font style should match requested style");

        // Family should be either Lucida Grande (if installed) or SansSerif fallback.
        String family = font.getFamily();
        assertTrue(
            "Lucida Grande".equalsIgnoreCase(family) ||
            "SansSerif".equalsIgnoreCase(family),
            () -> "Font family should be Lucida Grande or SansSerif, but was: " + family
        );
    }

    @Test
    @DisplayName("commonFont caches instances for same family/style/size")
    void testCommonFontCaching() {
        Font f1 = Fonts.commonFont(Font.PLAIN, 12);
        Font f2 = Fonts.commonFont(Font.PLAIN, 12);

        assertSame(f1, f2, "Fonts with same style and size should be cached and identical");
    }

    @Test
    @DisplayName("scaleFont scales size by factor and keeps style and family")
    void testScaleFont() {
        Font base = Fonts.commonFont(Font.BOLD, 12);
        float factor = 1.5f;

        Font scaled = Fonts.scaleFont(base, factor);

        assertNotNull(scaled, "Scaled font should not be null");
        assertEquals(Font.BOLD, scaled.getStyle(), "Scaled font should retain style");
        assertEquals(base.getFamily(), scaled.getFamily(), "Scaled font should retain family");

        float expectedSize = base.getSize2D() * factor;
        assertEquals(expectedSize, scaled.getSize2D(), 0.001,
                "Scaled font size should be base size multiplied by factor");
    }

    @Test
    @DisplayName("scaleFont throws IllegalArgumentException on null font")
    void testScaleFontNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Fonts.scaleFont(null, 1.2f),
                "scaleFont should throw IllegalArgumentException when font is null");
    }

    @Test
    @DisplayName("Public monospaced fonts use a monospaced family")
    void testMonospacedFonts() {
        Font[] monos = {
                Fonts.defaultMono,
                Fonts.mono,
                Fonts.smallMono,
                Fonts.tinyMono
        };

        for (Font f : monos) {
            assertNotNull(f, "Monospaced font should not be null");
            assertEquals(Font.MONOSPACED, f.getFamily(),
                    "Monospaced font family should be Font.MONOSPACED");
        }
    }

    @Test
    @DisplayName("Predefined mediumItalicBoldFont has correct size and style")
    void testMediumItalicBoldFontProperties() {
        Font f = Fonts.mediumItalicBoldFont;

        assertNotNull(f, "mediumItalicBoldFont should not be null");
        assertEquals(11, f.getSize(), "mediumItalicBoldFont should have size 11");
        assertEquals(Font.ITALIC | Font.BOLD, f.getStyle(),
                "mediumItalicBoldFont should be italic and bold");
    }

    @Test
    @DisplayName("Predefined constants are cached (identity) if requested again")
    void testConstantsUseCachedFonts() {
        Font fromConstant = Fonts.defaultFont;
        Font fromFactory = Fonts.commonFont(Font.PLAIN, 12);

        assertSame(fromConstant, fromFactory,
                "Accessing the same size/style via constant and factory should yield the same cached instance");
    }
}
