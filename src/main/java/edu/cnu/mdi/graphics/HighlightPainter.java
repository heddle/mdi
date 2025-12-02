package edu.cnu.mdi.graphics;

import java.awt.*;

/**
 * Utility class for rendering visually emphasized (highlighted) line
 * segments with a two-tone effect.  <p>
 *
 * A highlighted line is drawn in two passes:
 * <ol>
 *     <li>A thicker, softer-colored "underlay" stroke</li>
 *     <li>A thinner, sharper-colored "overlay" stroke</li>
 * </ol>
 *
 * This produces an appearance similar to a glowing or embossed line,
 * and is used by higher-level drawing utilities such as
 * {@link ArrowPainter} to produce highlighted arrows or selection lines.
 *
 * <p>The highlight colors are customizable, though convenient defaults
 * {@link #DEFAULT_COLOR1} and {@link #DEFAULT_COLOR2} are provided.</p>
 *
 * <p>This class is {@code final} and cannot be instantiated.</p>
 */
public final class HighlightPainter {

    /**
     * Default outer highlight color (typically a soft, light color).
     */
    public static final Color DEFAULT_COLOR1 = new Color(255, 255, 180);

    /**
     * Default inner highlight color (typically stronger and slightly darker).
     */
    public static final Color DEFAULT_COLOR2 = new Color(255, 200, 80);

    /**
     * Private constructor to prevent instantiation.
     */
    private HighlightPainter() { }

    /**
     * Draws a highlighted line between two points using a two-tone rendering
     * technique. The result is a line with a subtle glow or emphasis, useful
     * for indicating selection, focus, or importance in a graphical view.
     *
     * <p>The line is drawn in two passes:</p>
     * <ul>
     *   <li><b>Pass 1:</b> A wider stroke using {@code c1}</li>
     *   <li><b>Pass 2:</b> A narrower stroke using {@code c2}</li>
     * </ul>
     *
     * @param g2 the graphics context; must be an instance of {@link Graphics2D}
     * @param x1 starting x-coordinate
     * @param y1 starting y-coordinate
     * @param x2 ending x-coordinate
     * @param y2 ending y-coordinate
     * @param c1 outer (underlay) highlight color
     * @param c2 inner (overlay) highlight color
     *
     * @throws NullPointerException if {@code g2}, {@code c1}, or {@code c2}
     *         is {@code null}
     * @throws ClassCastException if {@code g2} is not a {@link Graphics2D}
     */
    public static void drawHighlightedLine(Graphics2D g2, int x1, int y1, int x2, int y2,
                                           Color c1, Color c2) {

        // Pass 1: wider glow
        Stroke oldStroke = g2.getStroke();
        Color oldColor = g2.getColor();

        g2.setColor(c1);
        g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x1, y1, x2, y2);

        // Pass 2: inner sharp line
        g2.setColor(c2);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x1, y1, x2, y2);

        // Restore original state
        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }
}
