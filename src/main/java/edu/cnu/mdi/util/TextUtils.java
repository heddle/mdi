package edu.cnu.mdi.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.StringTokenizer;

public class TextUtils {

    /** Minimum font size returned by sizing helpers. */
    public static final int MIN_FONT_SIZE = 4;

    /** Maximum font size returned by sizing helpers (defensive upper bound). */
    public static final int MAX_FONT_SIZE = 512;
	/**
	 * Draws "ghosted" text for the specified foreground and background colors.
	 *
	 * @param g    the graphics context.
	 * @param text the string to draw.
	 * @param x    the horizontal (left) position.
	 * @param y    the vertical (baseline) position.
	 */
	public static void drawGhostText(Graphics g, String text, int x, int y) {
		drawGhostText(g, text, x, y, Color.white, Color.black);
	}

	/**
	 * Draws "ghosted" text for the specified foreground and background colors.
	 *
	 * @param g    the graphics context.
	 * @param text the string to draw.
	 * @param x    the horizontal (left) position.
	 * @param y    the vertical (baseline) position.
	 * @param fg   the foreground color.
	 * @param bg   the background color.
	 */
	public static void drawGhostText(Graphics g, String text, int x, int y, Color fg, Color bg) {

		if (g == null || bg == null) {
			return;
		}
		g.setColor(fg);
		g.drawString(text, x, y + 1);
		g.setColor(bg);
		g.drawString(text, x, y);
	}

	/**
	 * Draw halo text that is black with a white halo.
	 *
	 * @param g    the graphics context
	 * @param text the text to draw
	 * @param x    the horizontal (left) position.
	 * @param y    the vertical (baseline) position.
	 */
	public static void drawHaloText(Graphics g, String text, int x, int y) {
		drawHaloText(g, text, x, y, Color.black, Color.white);
	}

	/**
	 * Draw halo text (text that is outlined in a halo color)
	 *
	 * @param g         the graphics context
	 * @param text      the text to draw
	 * @param x         the horizontal (left) position.
	 * @param y         the vertical (baseline) position.
	 * @param textColor the color of the text.
	 * @param haloColor the color of the halo outline.
	 */
	public static void drawHaloText(Graphics g, String text, int x, int y, Color textColor, Color haloColor) {
		if (text == null) {
			return;
		}
		g.setColor(haloColor);
		g.drawString(text, x + 1, y);
		g.drawString(text, x - 1, y);
		g.drawString(text, x, y + 1);
		g.drawString(text, x, y - 1);

		g.setColor(textColor);
		g.drawString(text, x, y);
	}

	/**
	 * Return a string's bounding box.
	 *
	 * @param c    the component where the string will be rendered.
	 * @param pp   the pixel location of the base point.
	 * @param s    the string being rendered.
	 * @param font the font being used.
	 * @return the bounding rectangle.
	 */
	public static Rectangle sizeText(Component c, Point pp, String s, Font font) {
		FontMetrics fm = c.getFontMetrics(font);
		int descent = fm.getDescent();
		int ascent = fm.getAscent();
		int sw = fm.stringWidth(s);
		return new Rectangle(pp.x, pp.y - ascent, sw, ascent + descent);
	}

	/**
	 * Check to see if two vectors of strings are equal. Used by feedback to avoid
	 * redrawing identical strings.
	 *
	 * @param list1 the first String vector.
	 * @param list2 the other String vector.
	 * @return <code>true</code> if they are equal.
	 */
	public static boolean equalStringLists(List<String> list1, List<String> list2) {
		if ((list1 == null) && (list2 == null)) {
			return true;
		}

		// if just one is null, not equal
		// must have the same size
		if ((list1 == null) || (list2 == null) || (list1.size() != list2.size())) {
			return false;
		}

		// all strings must be equal
		for (int i = 0; i < list1.size(); i++) {
			String s1 = list1.get(i);
			String s2 = list2.get(i);

			if (((s1 == null) && (s2 != null)) || ((s1 != null) && (s2 == null))) {
				return false;
			}

			if ((s1 != null) && (s2 != null) && !(s1.equals(s2))) {
				return false;
			}

		}

		return true;
	}

	/**
     * Get the next smaller font. This method clamps the resulting size so that
     * it never returns a font with a non-positive size.
     *
     * @param font     the base font
     * @param stepdown the step down (e.g., if stepdown is 2 and the font size is 28,
     *                 a font of size 26 is returned).
     * @return a font a little smaller, or null if {@code font} is null.
     */
    public static Font nextSmallerFont(Font font, int stepdown) {
        if (font == null) {
            return null;
        }
        int step = Math.max(1, stepdown);
        int newSize = Math.max(MIN_FONT_SIZE, font.getSize() - step);
        return (newSize == font.getSize()) ? font : new Font(font.getFontName(), font.getStyle(), newSize);
    }

    /**
     * Get the next bigger font. This method clamps the resulting size to a
     * defensive maximum.
     *
     * @param font   the base font
     * @param stepup the step up (e.g., if stepup is 2 and the font size is 28,
     *               a font of size 30 is returned).
     * @return a font a little bigger, or null if {@code font} is null.
     */
    public static Font nextBiggerFont(Font font, int stepup) {
        if (font == null) {
            return null;
        }
        int step = Math.max(1, stepup);
        int newSize = Math.min(MAX_FONT_SIZE, font.getSize() + step);
        return (newSize == font.getSize()) ? font : new Font(font.getFontName(), font.getStyle(), newSize);
    }
	/**
	 * This method breaks a string into an array of tokens.
	 *
	 * @param str   the string to decompose.
	 * @param token the token
	 * @return an array of tokens
	 */

	public static String[] tokens(String str, String token) {

		StringTokenizer t = new StringTokenizer(str, token);
		int num = t.countTokens();
		String lines[] = new String[num];

		for (int i = 0; i < num; i++) {
			lines[i] = t.nextToken();
		}

		return lines;
	}

	/**
	 * Convert a comma separated string to an array of strings,
	 * removing white space
	 * @param s the commas separated string
	 * @return the array of strings, no white space
	 */
	public static String[] cssToStringArray(String s) {

		if (s == null) {
			return null;
		}
		s = s.replaceAll("\\s", "");

		if (s.length() == 0) {
			return null;
		}

		return tokens(s, ",");
	}

	/**
	 * Convert an array of strings to a single comma separated string
	 * @param array the string array of matches
	 * @return a comma separated string
	 */
	public static String stringArrayToString(String[] array) {
		if (array == null) {
			return "";
		}

		int len = array.length;
		if (len == 0) {
			return "";
		}

		StringBuffer sb = new StringBuffer(256);

		for (int i = 0; i < (len-1); i++) {
			sb.append(array[i] + ", ");
		}
		sb.append(array[len-1]);
		return sb.toString();

	}


}
