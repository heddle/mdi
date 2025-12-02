package edu.cnu.mdi.graphics;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ColorUtils}.
 *
 * <p>These tests verify correct behavior of the conversion methods between
 * {@link Color} objects and hexadecimal RGBA string representations.</p>
 */
public class ColorUtilsTest {

    /**
     * Verifies that {@link ColorUtils#toHexRgba(Color)} returns the default
     * value for a {@code null} input color.
     */
    @Test
    void toHexRgba_nullColor_returnsDefaultOpaqueBlack() {
        String hex = ColorUtils.toHexRgba(null);
        assertEquals("#000000ff", hex, "Null color should map to default #000000ff");
    }

    /**
     * Verifies that {@link ColorUtils#toHexRgba(Color)} produces a string in the
     * expected format for a non-null color, and that the format is lowercase.
     */
    @Test
    void toHexRgba_nonNullColor_hasCorrectFormatAndLowercase() {
        Color c = new Color(0xAB, 0xCD, 0xEF, 0x12);
        String hex = ColorUtils.toHexRgba(c);

        assertEquals("#abcdef12", hex, "Hex output should match expected RGBA string");
        assertEquals(hex.toLowerCase(), hex, "Hex string should be lowercase");
        assertTrue(hex.startsWith("#"), "Hex string should start with '#'");
        assertEquals(9, hex.length(), "Hex string should be in the form #rrggbbaa");
    }

    /**
     * Confirms that converting a color to hex and back again yields an equal
     * {@link Color} object (same RGBA components).
     */
    @Test
    void toHexRgba_and_fromHexRgba_roundTripPreservesComponents() {
        Color original = new Color(10, 20, 30, 40);

        String hex = ColorUtils.toHexRgba(original);
        Color parsed = ColorUtils.fromHexRgba(hex);

        assertNotNull(parsed, "Parsed color should not be null");
        assertEquals(original.getRed(), parsed.getRed(), "Red component should round-trip");
        assertEquals(original.getGreen(), parsed.getGreen(), "Green component should round-trip");
        assertEquals(original.getBlue(), parsed.getBlue(), "Blue component should round-trip");
        assertEquals(original.getAlpha(), parsed.getAlpha(), "Alpha component should round-trip");
    }

    /**
     * Verifies that {@link ColorUtils#fromHexRgba(String)} returns black when
     * the input string is {@code null}.
     */
    @Test
    void fromHexRgba_nullString_returnsBlack() {
        Color c = ColorUtils.fromHexRgba(null);
        assertEquals(Color.black, c, "Null hex string should yield Color.black");
    }

    /**
     * Verifies that {@link ColorUtils#fromHexRgba(String)} correctly parses a
     * full eight-digit RGBA string including a leading '#'.
     */
    @Test
    void fromHexRgba_fullRgbaWithHash_parsesCorrectly() {
        Color c = ColorUtils.fromHexRgba("#11223344");

        assertEquals(0x11, c.getRed(), "Red component mismatch");
        assertEquals(0x22, c.getGreen(), "Green component mismatch");
        assertEquals(0x33, c.getBlue(), "Blue component mismatch");
        assertEquals(0x44, c.getAlpha(), "Alpha component mismatch");
    }

    /**
     * Verifies that {@link ColorUtils#fromHexRgba(String)} correctly parses a
     * six-digit RGB string and pads the alpha channel to 255 (opaque).
     */
    @Test
    void fromHexRgba_rgbOnly_padsAlphaToOpaque() {
        Color c = ColorUtils.fromHexRgba("A1B2C3");

        assertEquals(0xA1, c.getRed(), "Red component mismatch");
        assertEquals(0xB2, c.getGreen(), "Green component mismatch");
        assertEquals(0xC3, c.getBlue(), "Blue component mismatch");
        assertEquals(0xFF, c.getAlpha(), "Alpha should default to 255");
    }

    /**
     * Verifies that short hex strings are padded as documented: first to at
     * least six digits for RGB, then to eight digits for alpha.
     */
    @Test
    void fromHexRgba_shortString_isPaddedToRgbAndAlpha() {
        // "abc" is padded internally to "abc000ff":
        //   "ab" -> red, "c0" -> green, "00" -> blue, "ff" -> alpha,
        // given the implementation's simple zero/f padding strategy.
        Color c = ColorUtils.fromHexRgba("abc");

        assertEquals(0xAB, c.getRed(), "Red component should correspond to first two hex digits");
        assertEquals(0xC0, c.getGreen(), "Green component should be based on padded string");
        assertEquals(0x00, c.getBlue(), "Blue component should be based on padded string");
        assertEquals(0xFF, c.getAlpha(), "Alpha should be padded to 255");
    }

    /**
     * Verifies that invalid hex strings (non-hex characters or insufficient
     * characters after padding) are handled by returning {@link Color#black}.
     */
    @Test
    void fromHexRgba_invalidString_returnsBlack() {
        Color c1 = ColorUtils.fromHexRgba("zzzzzzzz");
        Color c2 = ColorUtils.fromHexRgba("this-is-not-hex");

        assertEquals(Color.black, c1, "Invalid hex should yield Color.black");
        assertEquals(Color.black, c2, "Invalid hex should yield Color.black");
    }

    /**
     * Confirms that leading '#' characters are ignored by
     * {@link ColorUtils#fromHexRgba(String)} and that behavior is otherwise
     * identical to passing the same hex value without the '#'.
     */
    @Test
    void fromHexRgba_leadingHash_isIgnored() {
        Color withHash = ColorUtils.fromHexRgba("#44556677");
        Color withoutHash = ColorUtils.fromHexRgba("44556677");

        assertEquals(withoutHash.getRed(), withHash.getRed(), "Red component should match");
        assertEquals(withoutHash.getGreen(), withHash.getGreen(), "Green component should match");
        assertEquals(withoutHash.getBlue(), withHash.getBlue(), "Blue component should match");
        assertEquals(withoutHash.getAlpha(), withHash.getAlpha(), "Alpha component should match");
    }
}
