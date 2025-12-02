package edu.cnu.mdi.graphics;

import java.awt.*;

/**
 * Utility methods for drawing 2D arrows onto a {@link Graphics} or
 * {@link Graphics2D} context. <p>
 *
 * This class provides a simple arrow-drawing primitive consisting of:
 * <ul>
 *   <li>a line segment from a start point to the base of the arrowhead</li>
 *   <li>a filled triangular arrowhead with an outline</li>
 * </ul>
 *
 * The arrowhead geometry is computed from a fixed width and angle,
 * producing visually consistent arrows regardless of direction or length.
 * Optionally, a highlighted line segment can be drawn using
 * {@link HighlightPainter}, enabling effects such as soft glows or
 * two-tone emphasis used elsewhere in the MDI graphics framework.
 *
 * <p>This class is {@code final} and cannot be instantiated.</p>
 */
public final class ArrowPainter {

    /**
     * Private constructor to prevent instantiation.
     */
    private ArrowPainter() { }

    /**
     * Draws a simple arrow from (x, y) to (xx, yy) without highlighting.
     * <p>
     * Equivalent to calling
     * {@link #drawArrow(Graphics, int, int, int, int, boolean, Color, Color)}
     * with {@code highlight == false}.
     *
     * @param g1  the graphics context (must be drawable; will be cast to {@link Graphics2D})
     * @param x   starting x-coordinate
     * @param y   starting y-coordinate
     * @param xx  ending x-coordinate (tip of the arrowhead)
     * @param yy  ending y-coordinate (tip of the arrowhead)
     */
    public static void drawArrow(Graphics g1, int x, int y, int xx, int yy) {
        drawArrow(g1, x, y, xx, yy, false, null, null);
    }

    /**
     * Draws an arrow between two points with optional highlighting.
     *
     * <p>The arrow consists of a straight line segment from the start point
     * to the base of the arrowhead, plus a triangular arrowhead whose tip
     * is located at {@code (xx, yy)}. The arrowhead width and opening angle
     * are fixed constants that provide a visually pleasing shape.
     *
     * <p>If {@code highlight} is {@code true}, the line portion is drawn
     * using {@link HighlightPainter#drawHighlightedLine(Graphics2D, int, int, int, int, Color, Color)}.
     * The highlight colors {@code c1} and {@code c2} may be {@code null},
     * in which case {@link HighlightPainter#DEFAULT_COLOR1} and
     * {@link HighlightPainter#DEFAULT_COLOR2} are used.
     *
     * @param g1         the graphics context; will be cast to {@link Graphics2D}
     * @param x          starting x-coordinate
     * @param y          starting y-coordinate
     * @param xx         ending x-coordinate (arrow tip)
     * @param yy         ending y-coordinate (arrow tip)
     * @param highlight  whether to draw the shaft using a highlight effect
     * @param c1         first highlight color (may be {@code null})
     * @param c2         second highlight color (may be {@code null})
     *
     * @throws ClassCastException if {@code g1} is not an instance of {@link Graphics2D}
     */
    public static void drawArrow(Graphics g1, int x, int y, int xx, int yy,
                                 boolean highlight, Color c1, Color c2) {

        // Avoid degenerately short arrows.
        if (Math.abs(x - xx) < 4 && Math.abs(y - yy) < 4) {
            return;
        }

        Graphics2D g = (Graphics2D) g1;

        // Arrowhead geometry constants
        float arrowWidth = 8.0f;
        float theta = 0.423f;  // Approximate arrowhead angle

        int[] xPoints = new int[3];
        int[] yPoints = new int[3];

        // Vector from start to end
        float[] vecLine = new float[2];
        // Perpendicular vector for arrowhead width
        float[] vecLeft = new float[2];

        float fLength;
        float th;
        float ta;
        float baseX, baseY;

        // Arrow tip
        xPoints[0] = xx;
        yPoints[0] = yy;

        // Direction vector
        vecLine[0] = (float) xPoints[0] - x;
        vecLine[1] = (float) yPoints[0] - y;

        // Left perpendicular
        vecLeft[0] = -vecLine[1];
        vecLeft[1] = vecLine[0];

        // Length of the shaft (full arrow minus head)
        fLength = (float) Math.sqrt(vecLine[0] * vecLine[0] + vecLine[1] * vecLine[1]);

        // Normalize scale factors for arrowhead geometry
        th = arrowWidth / (2.0f * fLength);
        ta = arrowWidth / (2.0f * ((float) Math.tan(theta) / 2.0f) * fLength);

        // Base of the arrowhead
        baseX = (xPoints[0] - ta * vecLine[0]);
        baseY = (yPoints[0] - ta * vecLine[1]);

        // Compute the two base corners of the arrowhead polygon
        xPoints[1] = (int) (baseX + th * vecLeft[0]);
        yPoints[1] = (int) (baseY + th * vecLeft[1]);
        xPoints[2] = (int) (baseX - th * vecLeft[0]);
        yPoints[2] = (int) (baseY - th * vecLeft[1]);

        // Draw line portion
        if (highlight) {
            Color hc1 = (c1 == null) ? HighlightPainter.DEFAULT_COLOR1 : c1;
            Color hc2 = (c2 == null) ? HighlightPainter.DEFAULT_COLOR2 : c2;
            HighlightPainter.drawHighlightedLine(g, x, y, (int) baseX, (int) baseY, hc1, hc2);
        } else {
            g.drawLine(x, y, (int) baseX, (int) baseY);
        }

        // Draw arrowhead (filled and outlined)
        g.fillPolygon(xPoints, yPoints, 3);
        g.setColor(g.getColor().darker());
        g.drawPolygon(xPoints, yPoints, 3);
    }
}
