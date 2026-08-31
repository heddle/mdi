package edu.cnu.mdi.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import javax.swing.SwingConstants;

/** Text measurement, comparison, and rotated drawing helpers. */
public final class TextUtils {

	private TextUtils() {
	}

	public static void drawGhostText(Graphics g, String text, int x, int y) {
		drawGhostText(g, text, x, y, Color.WHITE, Color.BLACK);
	}

	public static void drawGhostText(Graphics g, String text, int x, int y, Color foreground, Color background) {
		if (g == null || text == null || foreground == null || background == null) {
			return;
		}
		g.setColor(foreground);
		g.drawString(text, x, y + 1);
		g.setColor(background);
		g.drawString(text, x, y);
	}

	public static void drawHaloText(Graphics g, String text, int x, int y) {
		drawHaloText(g, text, x, y, Color.BLACK, Color.WHITE);
	}

	public static void drawHaloText(Graphics g, String text, int x, int y, Color textColor, Color haloColor) {
		if (g == null || text == null || textColor == null || haloColor == null) {
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

	public static Rectangle sizeText(Component component, Point basePoint, String text, Font font) {
		FontMetrics metrics = component.getFontMetrics(font);
		return new Rectangle(basePoint.x, basePoint.y - metrics.getAscent(), metrics.stringWidth(text),
				metrics.getAscent() + metrics.getDescent());
	}

	public static Font nextSmallerFont(Font font, int stepDown) {
		return (font == null) ? null : font.deriveFont((float) (font.getSize() - stepDown));
	}

	public static Font nextBiggerFont(Font font, int stepUp) {
		return (font == null) ? null : font.deriveFont((float) (font.getSize() + stepUp));
	}

	public static String[] tokens(String text, String delimiters) {
		StringTokenizer tokenizer = new StringTokenizer(text, delimiters);
		String[] result = new String[tokenizer.countTokens()];
		for (int i = 0; i < result.length; i++) {
			result[i] = tokenizer.nextToken();
		}
		return result;
	}

	public static String[] commaSeparatedToArray(String text) {
		if (text == null) {
			return null;
		}
		String compact = text.replaceAll("\\s", "");
		return compact.isEmpty() ? null : tokens(compact, ",");
	}

	public static String arrayToCommaSeparated(String[] values) {
		return (values == null) ? "" : String.join(", ", values);
	}

	/**
	 * Draws rotated multi-line text with specified alignment.
	 * <p>
	 * This method creates its own Graphics2D copy so that the translate/rotate
	 * transforms and the dispose() call never affect the caller's context.
	 * The original {@code g2} is left completely unmodified.
	 * </p>
	 *
	 * @param g2    the Graphics context (never modified or disposed)
	 * @param cp    the center point for the text block and the rotation pivot
	 * @param s     the text to draw (may contain newlines)
	 * @param font  the font to use
	 * @param tcolor the text color
	 * @param theta the rotation angle in degrees
	 * @param align Use SwingConstants.LEFT, CENTER, or RIGHT
	 */
	public static void drawRotatedText(Graphics2D g2, Point cp, String s,
	        Font font, Color tcolor, double theta, int align) {
		Objects.requireNonNull(g2, "g2");
		Objects.requireNonNull(cp, "cp");
		Objects.requireNonNull(s, "text");
		Objects.requireNonNull(font, "font");
		Objects.requireNonNull(tcolor, "text color");

		// Work on a copy so that translate/rotate/dispose never touch the caller's g2.
		// This is the critical fix: the original code called g2.dispose() on the shared
		// pipeline context, silently killing all subsequent drawing (e.g. selection
		// handles) for the rest of that paint cycle.
		Graphics2D g = (Graphics2D) g2.create();
		try {
		    g.setFont(font);
		    g.setColor(tcolor);
		    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		    String[] lines = textLines(s);
		    FontMetrics fm = g.getFontMetrics();
		    int lineHeight = fm.getHeight();
		    int totalHeight = lineHeight * lines.length;

		    g.translate(cp.x, cp.y);
		    g.rotate(Math.toRadians(theta));

		    // Calculate max width for the whole block
		    int maxWidth = 0;
		    for (String line : lines) {
		        maxWidth = Math.max(maxWidth, fm.stringWidth(line));
		    }

		    // Calculate the top-most baseline position
		    float startY = -totalHeight / 2.0f + fm.getAscent();

		    for (int i = 0; i < lines.length; i++) {
		        String line = lines[i];
		        int lineWidth = fm.stringWidth(line);
		        float x;

		        switch (align) {
		            case SwingConstants.LEFT:
		                x = -maxWidth / 2.0f;
		                break;
		            case SwingConstants.RIGHT:
		                x = maxWidth / 2.0f - lineWidth;
		                break;
		            case SwingConstants.CENTER:
		            default:
		                x = -lineWidth / 2.0f;
		                break;
		        }

		        float y = startY + (i * lineHeight);
		        g.drawString(line, x, y);
		    }
		} finally {
		    g.dispose();   // dispose the COPY, not the original
		}
	}

	/**
	 * Calculate the bounding rectangle for a block of text with given margins and line spacing.
	 *
	 * @param text        the text content which can contain multiple lines
	 * @param fm          the FontMetrics object for measuring text
	 * @param leftMargin  the left margin in pixels; silently clamped to
	 *                    {@code [0, 500]}
	 * @param rightMargin the right margin in pixels; silently clamped to
	 *                    {@code [0, 500]}
	 * @param topMargin   the top margin in pixels; silently clamped to
	 *                    {@code [0, 500]}
	 * @param bottomMargin the bottom margin in pixels; silently clamped to
	 *                    {@code [0, 500]}
	 * @param lineSpacing the line spacing factor (1.0 = normal, >1.0 = more
	 *                    space); silently clamped to {@code [0.5, 3.0]}
	 * @return the bounding Rectangle for the text block
	 * @throws IllegalArgumentException if {@code lineSpacing} is not finite
	 *                                  (NaN or infinite)
	 */
	public static Rectangle textBounds(String text, FontMetrics fm,
            int leftMargin, int rightMargin,
            int topMargin, int bottomMargin,
            float lineSpacing) {
		Objects.requireNonNull(text, "text");
		String lines[] = textLines(text);
		return textBounds(lines, fm, leftMargin, rightMargin, topMargin, bottomMargin, lineSpacing);
	}

	/**
	 * Calculate the bounding rectangle for a block of text with given margins and line spacing.
	 *
	 * @param lines       the array of text lines
	 * @param fm          the FontMetrics object for measuring text
	 * @param leftMargin  the left margin in pixels; silently clamped to
	 *                    {@code [0, 500]}
	 * @param rightMargin the right margin in pixels; silently clamped to
	 *                    {@code [0, 500]}
	 * @param topMargin   the top margin in pixels; silently clamped to
	 *                    {@code [0, 500]}
	 * @param bottomMargin the bottom margin in pixels; silently clamped to
	 *                    {@code [0, 500]}
	 * @param lineSpacing the line spacing factor (1.0 = normal, >1.0 = more
	 *                    space); silently clamped to {@code [0.5, 3.0]}
	 * @return the bounding Rectangle for the text block
	 * @throws IllegalArgumentException if {@code lineSpacing} is not finite
	 *                                  (NaN or infinite)
	 */
	public static Rectangle textBounds(String[] lines, FontMetrics fm,
	                            int leftMargin, int rightMargin,
	                            int topMargin, int bottomMargin,
	                            float lineSpacing) {

		Objects.requireNonNull(fm, "font metrics");
	    leftMargin   = Math.max(0, Math.min(leftMargin,   500));
	    rightMargin  = Math.max(0, Math.min(rightMargin,  500));
	    topMargin    = Math.max(0, Math.min(topMargin,    500));
	    bottomMargin = Math.max(0, Math.min(bottomMargin, 500));
		if (!Float.isFinite(lineSpacing)) {
			throw new IllegalArgumentException("lineSpacing must be finite");
		}
	    lineSpacing  = Math.max(0.5f, Math.min(lineSpacing, 3.0f));

	    if (lines == null || lines.length == 0) {
	        return new Rectangle(0, 0, leftMargin + rightMargin, topMargin + bottomMargin);
	    }

	    int maxWidth = 0;
	    for (String line : lines) {
	        if (line != null) {
	            maxWidth = Math.max(maxWidth, fm.stringWidth(line));
	        }
	    }

	    int fontHeight = fm.getHeight();
	    int totalTextHeight = 0;
	    if (lines.length > 0) {
	        int interLineHeight = Math.round(fontHeight * lineSpacing);
	        totalTextHeight = (interLineHeight * (lines.length - 1)) + fontHeight;
	    }

	    return new Rectangle(
	        0,
	        0,
	        maxWidth + leftMargin + rightMargin,
	        totalTextHeight + topMargin + bottomMargin
	    );
	}

	/**
	 * Check to see if two vectors of strings are equal.
	 *
	 * @param list1 the first String vector.
	 * @param list2 the other String vector.
	 * @return {@code true} if they are equal.
	 */
	public static boolean equalStringLists(List<String> list1, List<String> list2) {
		if ((list1 == null) && (list2 == null)) {
			return true;
		}
		if ((list1 == null) || (list2 == null) || (list1.size() != list2.size())) {
			return false;
		}
		for (int i = 0; i < list1.size(); i++) {
			String s1 = list1.get(i);
			String s2 = list2.get(i);
			if (((s1 == null) && (s2 != null)) || ((s1 != null) && (s2 == null))) {
				return false;
			}
			if ((s1 != null) && !(s1.equals(s2))) {
				return false;
			}
		}
		return true;
	}

	private static String[] textLines(String text) {
		return text.split("\\R", -1);
	}
}
