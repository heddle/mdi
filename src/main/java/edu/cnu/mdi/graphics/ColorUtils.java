package edu.cnu.mdi.graphics;

import java.awt.Color;

/**
 * Utility methods for converting between {@link Color} objects and
 * hexadecimal string representations.  <p>
 *
 * This class supports bidirectional conversion:
 * <ul>
 *     <li>{@link #toHexRgba(Color)} produces strings of the form
 *     <code>#rrggbbaa</code></li>
 *     <li>{@link #fromHexRgba(String)} parses strings of the form
 *     <code>#rrggbbaa</code>, with optional alpha and optional leading '#'</li>
 * </ul>
 *
 * Hex values follow standard two-digit lowercase hexadecimal notation for
 * each component:
 * <pre>
 *   rr = red   (00–ff)
 *   gg = green (00–ff)
 *   bb = blue  (00–ff)
 *   aa = alpha (00–ff)
 * </pre>
 *
 * <p>This class is {@code final} and cannot be instantiated.</p>
 */
public final class ColorUtils {

    /**
     * Private constructor to prevent instantiation.
     */
    private ColorUtils() { }

    /**
     * Converts a {@link Color} object to a hexadecimal RGBA string in the form
     * <code>#rrggbbaa</code>. All hex digits are lowercase.
     *
     * <p>If {@code color} is {@code null}, this method returns the default
     * opaque black string <code>#000000ff</code>.</p>
     *
     * <p><b>Examples:</b></p>
     * <pre>
     *   ColorUtils.toHexRgba(new Color(255, 0, 0, 128));  // "#ff000080"
     *   ColorUtils.toHexRgba(Color.BLACK);                // "#000000ff"
     * </pre>
     *
     * @param color the color to convert; may be {@code null}
     * @return a lowercase hexadecimal string in the form <code>#rrggbbaa</code>
     */
    public static String toHexRgba(Color color) {
        if (color == null) {
            return "#000000ff";
        }
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();
        return String.format("#%02x%02x%02x%02x", r, g, b, a);
    }

    /**
     * Parses a hexadecimal color string and returns the corresponding
     * {@link Color}. The input may be of the form:
     * <ul>
     *   <li><code>#rrggbbaa</code> (full specification)</li>
     *   <li><code>#rrggbb</code> (alpha defaults to <code>ff</code>)</li>
     *   <li><code>rrggbbaa</code> (leading '#' optional)</li>
     *   <li><code>rrggbb</code> (alpha defaults to <code>ff</code>)</li>
     * </ul>
     *
     * <p>If fewer than 6 hex digits are provided, missing digits are padded
     * with zeros. If the alpha component is missing, it is padded to "ff".</p>
     *
     * <p><b>Examples:</b></p>
     * <pre>
     *   ColorUtils.fromHexRgba("#80ff00ff")  // lime green, opaque
     *   ColorUtils.fromHexRgba("ff0000")     // red, alpha defaults to ff
     *   ColorUtils.fromHexRgba("abc")        // "abc000ff" → parses safely
     * </pre>
     *
     * <p>If parsing fails for any reason, this method returns
     * {@link Color#black}.</p>
     *
     * @param hex the hex string to parse; may be {@code null}
     * @return a {@link Color} representing the parsed RGBA value, or
     *         {@link Color#black} on error
     */
    public static Color fromHexRgba(String hex) {
        if (hex == null) {
            return Color.black;
        }

        // Remove leading '#' if present
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        // Ensure at least RGB (6 chars)
        while (hex.length() < 6) {
            hex += "0";
        }

        // Ensure RGBA (8 chars)
        while (hex.length() < 8) {
            hex += "f";
        }

        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            int a = Integer.parseInt(hex.substring(6, 8), 16);
            return new Color(r, g, b, a);
        } catch (Exception e) {
            e.printStackTrace();
            return Color.black;
        }
    }
}
