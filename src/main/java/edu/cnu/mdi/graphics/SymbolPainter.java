package edu.cnu.mdi.graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;

/**
 * Utility class for drawing small symbolic markers and simple 3D-styled shapes.
 *
 * <p>This class centralizes drawing primitives commonly used in visualization
 * applications, including rectangles, ovals, triangles, crosses, diamonds, and
 * basic pseudo-3D versions of rectangles and ellipses. All operations are
 * performed directly on the supplied {@link Graphics} context, using simple
 * integer geometry and optional fill/outline colors.</p>
 *
 * <p>The class is {@code final} and non-instantiable since all operations
 * are stateless static helpers.</p>
 */
public final class SymbolPainter {

    /** Prevent instantiation. */
    private SymbolPainter() { }

    // -------------------------------------------------------------------------
    // Basic filled + framed rectangle
    // -------------------------------------------------------------------------

    /**
     * Fills and frames a rectangle using optional fill and outline colors.
     *
     * <p>If {@code fill} is non-null, the rectangle is filled first.  
     * If {@code frame} is non-null, a rectangular outline is drawn afterward.</p>
     *
     * @param g     the graphics context
     * @param r     the rectangle bounds (x, y, width, height)
     * @param fill  fill color, or {@code null} for no fill
     * @param frame outline color, or {@code null} for no outline
     */
    public static void fillAndFrameRect(Graphics g, Rectangle r,
                                        Color fill, Color frame) {
        Color old = g.getColor();

        if (fill != null) {
            g.setColor(fill);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
        if (frame != null) {
            g.setColor(frame);
            g.drawRect(r.x, r.y, r.width, r.height);
        }

        g.setColor(old);
    }

    // -------------------------------------------------------------------------
    // Centered rectangle and oval markers
    // -------------------------------------------------------------------------

    /**
     * Draws a filled and/or outlined rectangle centered at ({@code x}, {@code y})
     * with half-width {@code w2} and half-height {@code h2}.
     *
     * <p>If {@code fc} is non-null, the rectangle is filled first.  
     * If {@code lc} is {@code null}, it is substituted by {@code fc}.</p>
     *
     * @param g   the graphics context
     * @param x   center x-coordinate
     * @param y   center y-coordinate
     * @param w2  half-width (rectangle extends ±w2 horizontally)
     * @param h2  half-height (rectangle extends ±h2 vertically)
     * @param lc  line (outline) color, or {@code null} to use fill color
     * @param fc  fill color, or {@code null} for no fill
     */
    public static void drawRectangle(Graphics g, int x, int y, int w2, int h2,
                                     Color lc, Color fc) {
        if (lc == null) {
            lc = fc;
        }
        if (fc != null) {
            g.setColor(fc);
            g.fillRect(x - w2, y - h2, 2 * w2, 2 * h2);
        }
        if (lc != null) {
            g.setColor(lc);
            g.drawRect(x - w2, y - h2, 2 * w2, 2 * h2);
        }
    }

    /**
     * Draws a filled and/or outlined oval centered at ({@code x}, {@code y})
     * with half-width {@code w2} and half-height {@code h2}.
     *
     * @param g   the graphics context
     * @param x   center x-coordinate
     * @param y   center y-coordinate
     * @param w2  half-width in x
     * @param h2  half-height in y
     * @param lc  outline color (if {@code null}, defaults to fill color)
     * @param fc  fill color (or {@code null} for no fill)
     */
    public static void drawOval(Graphics g, int x, int y, int w2, int h2,
                                Color lc, Color fc) {
        if (lc == null) {
            lc = fc;
        }
        if (fc != null) {
            g.setColor(fc);
            g.fillOval(x - w2, y - h2, 2 * w2, 2 * h2);
        }
        if (lc != null) {
            g.setColor(lc);
            g.drawOval(x - w2, y - h2, 2 * w2, 2 * h2);
        }
    }

    // -------------------------------------------------------------------------
    // Triangles (up / down)
    // -------------------------------------------------------------------------

    /**
     * Draws an upward-pointing filled or outlined triangle centered at
     * ({@code x}, {@code y}) with half-size {@code s2}.
     *
     * <p>The vertices are:</p>
     * <ul>
     *   <li>bottom-left:  (x − s2, y + s2)</li>
     *   <li>top:          (x,       y − s2)</li>
     *   <li>bottom-right: (x + s2, y + s2)</li>
     * </ul>
     *
     * @param g   the graphics context
     * @param x   center x-coordinate
     * @param y   center y-coordinate
     * @param s2  half-size of the triangle
     * @param lc  outline color (null uses fill color)
     * @param fc  fill color (null means no fill)
     */
    public static void drawUpTriangle(Graphics g, int x, int y, int s2,
                                      Color lc, Color fc) {
        if (lc == null) {
            lc = fc;
        }

        int l = x - s2;
        int t = y - s2;
        int r = x + s2;
        int b = y + s2;

        Polygon poly = new Polygon();
        poly.addPoint(l, b);
        poly.addPoint(x, t);
        poly.addPoint(r, b);

        if (fc != null) {
            g.setColor(fc);
            g.fillPolygon(poly);
        }
        if (lc != null) {
            g.setColor(lc);
            g.drawPolygon(poly);
        }
    }

    /**
     * Draws a downward-pointing filled or outlined triangle.
     *
     * <p>Vertices:</p>
     * <ul>
     *   <li>top-left:  (x − s2, y − s2)</li>
     *   <li>top-right: (x + s2, y − s2)</li>
     *   <li>bottom:    (x,       y + s2)</li>
     * </ul>
     *
     * @param g   the graphics context
     * @param x   center x-coordinate
     * @param y   center y-coordinate
     * @param s2  half-size
     * @param lc  outline color
     * @param fc  fill color
     */
    public static void drawDownTriangle(Graphics g, int x, int y, int s2,
                                        Color lc, Color fc) {
        if (lc == null) {
            lc = fc;
        }

        int l = x - s2;
        int t = y - s2;
        int r = x + s2;
        int b = y + s2;

        Polygon poly = new Polygon();
        poly.addPoint(l, t);
        poly.addPoint(r, t);
        poly.addPoint(x, b);

        if (fc != null) {
            g.setColor(fc);
            g.fillPolygon(poly);
        }
        if (lc != null) {
            g.setColor(lc);
            g.drawPolygon(poly);
        }
    }

    // -------------------------------------------------------------------------
    // Cross and X markers
    // -------------------------------------------------------------------------

    /**
     * Draws a simple “plus” marker centered at ({@code x}, {@code y}) extending
     * {@code s2} pixels in all four directions.
     *
     * @param g   the graphics context
     * @param x   center x
     * @param y   center y
     * @param s2  half-length of cross arms
     * @param lc  line color (ignored if null)
     */
    public static void drawCross(Graphics g, int x, int y, int s2, Color lc) {
        if (lc != null) {
            g.setColor(lc);
            g.drawLine(x - s2, y, x + s2, y);
            g.drawLine(x, y - s2, x, y + s2);
        }
    }

    /**
     * Draws an “X” marker centered at ({@code x}, {@code y}).
     *
     * @param g   the graphics context
     * @param x   center x
     * @param y   center y
     * @param s2  half-length along each diagonal
     * @param lc  line color, or null for no drawing
     */
    public static void drawX(Graphics g, int x, int y, int s2, Color lc) {
        if (lc != null) {
            g.setColor(lc);
            g.drawLine(x - s2, y - s2, x + s2, y + s2);
            g.drawLine(x - s2, y + s2, x + s2, y - s2);
        }
    }

    // -------------------------------------------------------------------------
    // Simple 3D rectangle
    // -------------------------------------------------------------------------

    /**
     * Draws a simple pseudo-3D rectangle, optionally filled, using white/black
     * edges to create a raised or inset appearance.
     *
     * @param g      the graphics context
     * @param x      top-left x
     * @param y      top-left y
     * @param w      width
     * @param h      height
     * @param fill   fill color (null for none)
     * @param outsie true for raised (light on top/left), false for inset
     */
    public static void drawSimple3DRect(Graphics g, int x, int y, int w, int h,
                                        Color fill, boolean outsie) {
        Color old = g.getColor();

        if (fill != null) {
            g.setColor(fill);
            g.fillRect(x, y, w, h);
        }

        Color tc = outsie ? Color.white : Color.black;
        Color bc = outsie ? Color.black : Color.white;

        int x2 = x + w;
        int y2 = y + h;

        g.setColor(tc);
        g.drawLine(x, y, x2, y);
        g.drawLine(x, y, x, y2);

        g.setColor(bc);
        g.drawLine(x, y2, x2, y2);
        g.drawLine(x2, y2, x2, y);

        g.setColor(old);
    }

    /**
     * Convenience wrapper for drawing a pseudo-3D rectangle using a {@link Rectangle}.
     *
     * @param g      the graphics context
     * @param r      rectangle bounds
     * @param fc     fill color
     * @param outsie raised/inset flag
     */
    public static void drawSimple3DRect(Graphics g, Rectangle r,
                                        Color fc, boolean outsie) {
        drawSimple3DRect(g, r.x, r.y, r.width - 1, r.height - 1, fc, outsie);
    }

    // -------------------------------------------------------------------------
    // Simple 3D oval
    // -------------------------------------------------------------------------

    /**
     * Draws a simple pseudo-3D oval (ellipse) with optional inner color and
     * shading arcs to imply depth.
     *
     * @param g      the graphics context
     * @param r      bounding rectangle
     * @param fc     fill color for the main oval (null = none)
     * @param ic     inner-oval color (null = none)
     * @param outsie true for raised shading, false for inset
     */
    public static void drawSimple3DOval(Graphics g, Rectangle r,
                                        Color fc, Color ic, boolean outsie) {
        Color tc = outsie ? Color.white : Color.black;
        Color bc = outsie ? Color.black : Color.white;

        if (fc != null) {
            g.setColor(fc);
            g.fillArc(r.x, r.y, r.width - 1, r.height - 1, 0, 360);
        }

        if (ic != null) {
            g.setColor(ic);
            g.fillArc(r.x + 3, r.y + 3, r.width - 7, r.height - 7, 0, 360);
            g.setColor(Color.gray);
            g.drawArc(r.x + 3, r.y + 3, r.width - 7, r.height - 7, 0, 360);
        }

        // Top-left highlight arc
        g.setColor(tc);
        g.drawArc(r.x, r.y, r.width - 1, r.height - 1, 45, 180);

        // Bottom-right shadow arc
        g.setColor(bc);
        g.drawArc(r.x, r.y, r.width - 1, r.height - 1, 45, -180);
    }

    // -------------------------------------------------------------------------
    // Simple 3D diamond
    // -------------------------------------------------------------------------

    /**
     * Draws a simple 3D diamond shape centered in rectangle {@code r}, with
     * highlight/shadow edges and a filled interior.
     *
     * <p>The diamond consists of four vertices: left, top, right, bottom.
     * Two triangular edge segments are drawn with highlight and shadow colors
     * to simulate raised/inset appearance.</p>
     *
     * @param g      the graphics context
     * @param r      bounding rectangle
     * @param fc     fill color for the diamond interior
     * @param outsie true for raised highlight (white top-left), false for inset
     */
    public static void drawSimple3DDiamond(Graphics g, Rectangle r,
                                           Color fc, boolean outsie) {

        Color tc = outsie ? Color.white : Color.black;
        Color bc = outsie ? Color.black : Color.white;

        int xc = r.x + r.width / 2;
        int yc = r.y + r.height / 2;

        // diamond vertices
        int[] x = { r.x, xc, r.x + r.width, xc };
        int[] y = { yc, r.y, yc, r.y + r.height };

        // top-left highlight triangle
        int[] xp = { r.x, xc, r.x + r.width };
        int[] yp = { yc, r.y, yc };

        g.setColor(tc);
        g.drawPolygon(xp, yp, 3);

        // bottom-right shadow triangle
        xp[0] = r.x + r.width; yp[0] = yc;
        xp[1] = xc;            yp[1] = r.y + r.height;
        xp[2] = r.x;           yp[2] = yc;

        g.setColor(bc);
        g.drawPolygon(xp, yp, 3);

        g.setColor(fc);
        g.fillPolygon(x, y, 4);
    }
}
