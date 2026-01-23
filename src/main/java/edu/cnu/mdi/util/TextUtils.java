package edu.cnu.mdi.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.SwingConstants;

public class TextUtils {
	
	/**
	 * Draws rotated multi-line text with specified alignment.
	 * @param g the Graphics context
	 * @param cp the center point for rotation
	 * @param s the text to draw (can contain newlines)
	 * @param font the font to use
	 * @param tcolor the text color
	 * @param theta the rotation angle in degrees
	 * @param align Use SwingConstants.LEFT, CENTER, or RIGHT
	 */
	public static void drawRotatedText(Graphics g, Point cp, String s, 
	        Font font, Color tcolor, double theta, int align) {
	    Graphics2D g2d = (Graphics2D) g.create();

	    g2d.setFont(font);
	    g2d.setColor(tcolor);
	    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	    String[] lines = s.split("\\R");
	    FontMetrics fm = g2d.getFontMetrics();
	    int lineHeight = fm.getHeight();
	    int totalHeight = lineHeight * lines.length;

	    // Apply transformations
	    g2d.translate(cp.x, cp.y);
	    g2d.rotate(Math.toRadians(theta));
	    
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

	        // Determine X offset based on alignment relative to the pivot (0,0)
	        switch (align) {
	            case SwingConstants.LEFT:
	                x = -maxWidth/2; // Text starts at pivot
	                break;
	            case SwingConstants.RIGHT:
	                x = -lineWidth; // Text ends at pivot
	                break;
	            case SwingConstants.CENTER:
	            default:
	                x = -lineWidth / 2.0f; // Text centered on pivot
	                break;
	        }

	        float y = startY + (i * lineHeight);
	        g2d.drawString(line, x, y);
	    }

	    g2d.dispose();
	}
	
	/**
	 * Calculate the bounding rectangle for a block of text with given margins and line spacing.
	 *
	 * @param text        the text content which can contain multiple lines
	 * @param fm          the FontMetrics object for measuring text
	 * @param leftMargin  the left margin in pixels
	 * @param rightMargin the right margin in pixels
	 * @param topMargin   the top margin in pixels
	 * @param bottomMargin the bottom margin in pixels
	 * @param lineSpacing the line spacing factor (1.0 = normal, >1.0 = more space)
	 * @return the bounding Rectangle for the text block
	 */
	public static Rectangle textBounds(String text, FontMetrics fm, 
            int leftMargin, int rightMargin, 
            int topMargin, int bottomMargin, 
            float lineSpacing) {
		String lines[] = text.lines().toArray(String[]::new);
		return textBounds(lines, fm, leftMargin, rightMargin, topMargin, bottomMargin, lineSpacing);
	}
	
	/**
	 * Calculate the bounding rectangle for a block of text with given margins and line spacing.
	 *
	 * @param lines       the array of text lines
	 * @param fm          the FontMetrics object for measuring text
	 * @param leftMargin  the left margin in pixels
	 * @param rightMargin the right margin in pixels
	 * @param topMargin   the top margin in pixels
	 * @param bottomMargin the bottom margin in pixels
	 * @param lineSpacing the line spacing factor (1.0 = normal, >1.0 = more space)
	 * @return the bounding Rectangle for the text block
	 */
	public static Rectangle textBounds(String[] lines, FontMetrics fm, 
	                            int leftMargin, int rightMargin, 
	                            int topMargin, int bottomMargin, 
	                            float lineSpacing) {
	    
	    // 1. Reasonable Clamps (Safety measures for 2026 standards)
	    leftMargin = Math.max(0, Math.min(leftMargin, 500));
	    rightMargin = Math.max(0, Math.min(rightMargin, 500));
	    topMargin = Math.max(0, Math.min(topMargin, 500));
	    bottomMargin = Math.max(0, Math.min(bottomMargin, 500));
	    
	    // Clamp lineSpacing: 1.0 is standard; below 0.5 is unreadable
	    lineSpacing = Math.max(0.5f, Math.min(lineSpacing, 3.0f));

	    if (lines == null || lines.length == 0) {
	        return new Rectangle(0, 0, leftMargin + rightMargin, topMargin + bottomMargin);
	    }

	    // 2. Calculate Width
	    int maxWidth = 0;
	    for (String line : lines) {
	        if (line != null) {
	            maxWidth = Math.max(maxWidth, fm.stringWidth(line)); //
	        }
	    }

	    // 3. Calculate Height
	    int fontHeight = fm.getHeight(); // Standard height (leading + ascent + descent)
	    int totalTextHeight = 0;

	    if (lines.length > 0) {
	        // Height of all lines except the last one (includes spacing)
	        int interLineHeight = Math.round(fontHeight * lineSpacing);
	        totalTextHeight = (interLineHeight * (lines.length - 1)) + fontHeight;
	    }

	    // 4. Return Final Bounds
	    return new Rectangle(
	        0, 
	        0, 
	        maxWidth + leftMargin + rightMargin, 
	        totalTextHeight + topMargin + bottomMargin
	    );
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

}
