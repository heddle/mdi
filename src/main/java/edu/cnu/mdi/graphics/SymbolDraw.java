package edu.cnu.mdi.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;

import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.LineStyle;
import edu.cnu.mdi.graphics.style.SymbolType;

/**
 * Static utility methods for drawing MDI point symbols at pixel coordinates.
 *
 * <h2>Symbol catalogue</h2>
 * <p>
 * The supported symbol types are defined by {@link SymbolType}:
 * {@code SQUARE}, {@code CIRCLE}, {@code DIAMOND}, {@code CROSS},
 * {@code UPTRIANGLE}, {@code DOWNTRIANGLE}, {@code BOWTIE}, {@code X}, and
 * {@code STAR}. {@code NOSYMBOL} suppresses drawing entirely.
 * </p>
 *
 * <h2>Color conventions</h2>
 * <p>
 * Most symbols accept separate line (outline) and fill colors. Passing
 * {@code null} for either color suppresses that part of the drawing. Where a
 * line color is {@code null} but a fill color is provided, the fill color
 * is used for the outline as well so that the symbol is always visible.
 * </p>
 *
 * <h2>Graphics context</h2>
 * <p>
 * All methods accept {@link Graphics2D} directly. No casts from
 * {@link java.awt.Graphics} are performed. Callers that receive a
 * {@link java.awt.Graphics} context (e.g. inside an overridden
 * {@link java.awt.Component#paint} method) should cast once at the call site:
 * </p>
 * <pre>
 * &#64;Override
 * public void paint(Graphics g) {
 *     Graphics2D g2 = (Graphics2D) g;
 *     SymbolDraw.drawSymbol(g2, x, y, style);
 * }
 * </pre>
 *
 * <h2>Stroke management</h2>
 * <p>
 * {@link #drawSymbol(Graphics2D, int, int, SymbolType, int, Color, Color)}
 * temporarily sets a 1-pixel solid stroke for drawing and restores the
 * previous stroke afterwards. Individual shape-drawing helpers do not manage
 * the stroke; they rely on whatever stroke is current.
 * </p>
 */
public class SymbolDraw {

    // -----------------------------------------------------------------------
    // Top-level dispatch
    // -----------------------------------------------------------------------

    /**
     * Draw the symbol described by {@code style} at the given screen position.
     * <p>
     * Symbol type, size, border color, and fill color are all read from
     * {@code style}.
     * </p>
     *
     * @param g2    the graphics context
     * @param x     x pixel coordinate of the symbol center
     * @param y     y pixel coordinate of the symbol center
     * @param style the drawing style; must not be {@code null}
     */
    public static void drawSymbol(Graphics2D g2, int x, int y, IStyled style) {
        drawSymbol(g2, x, y,
                style.getSymbolType(), style.getSymbolSize(),
                style.getBorderColor(), style.getFillColor());
    }

    /**
     * Draw a "ghost" symbol — three stacked copies of the symbol with slight
     * vertical offsets — to produce a shadow/emboss effect.
     * <p>
     * The three passes are:
     * </p>
     * <ol>
     *   <li>At {@code (x, y+1)} with a white outline (shadow).</li>
     *   <li>At {@code (x, y)} with the style's normal line color.</li>
     *   <li>At {@code (x, y-1)} with the style's normal line color
     *       (highlight).</li>
     * </ol>
     *
     * @param g2    the graphics context
     * @param x     x pixel coordinate of the symbol center
     * @param y     y pixel coordinate of the symbol center
     * @param style the drawing style; must not be {@code null}
     */
    public static void drawGhostSymbol(Graphics2D g2, int x, int y,
            IStyled style) {
        drawSymbol(g2, x, y + 1,
                style.getSymbolType(), style.getSymbolSize(),
                Color.white, style.getFillColor());
        drawSymbol(g2, x, y,
                style.getSymbolType(), style.getSymbolSize(),
                style.getLineColor(), style.getFillColor());
        drawSymbol(g2, x, y - 1,
                style.getSymbolType(), style.getSymbolSize(),
                style.getLineColor(), style.getFillColor());
    }

    /**
     * Draw a symbol at the given screen position with explicit style
     * parameters.
     * <p>
     * A 1-pixel solid stroke is set for the duration of the call and the
     * previous stroke is restored afterwards. If {@code symbol} is
     * {@link SymbolType#NOSYMBOL} the method returns immediately without
     * drawing.
     * </p>
     *
     * @param g2         the graphics context
     * @param x          x pixel coordinate of the symbol center
     * @param y          y pixel coordinate of the symbol center
     * @param symbol     the symbol type to draw
     * @param symbolSize total symbol size in pixels (half-size is derived
     *                   internally as {@code symbolSize / 2})
     * @param lineColor  outline color; {@code null} suppresses the outline
     *                   (or causes the fill color to be used as the outline
     *                   for symbols that require one)
     * @param fillColor  fill color; {@code null} suppresses fill (not
     *                   relevant for line-only symbols such as {@code CROSS},
     *                   {@code X}, and {@code STAR})
     */
    public static void drawSymbol(Graphics2D g2, int x, int y,
            SymbolType symbol, int symbolSize,
            Color lineColor, Color fillColor) {

        if (symbol == SymbolType.NOSYMBOL) {
            return;
        }

        Stroke oldStroke = g2.getStroke();
        g2.setStroke(GraphicsUtils.getStroke(1, LineStyle.SOLID));

        int s2 = symbolSize / 2;

        switch (symbol) {
            case SQUARE:
                drawRectangle(g2, x, y, s2, s2, lineColor, fillColor);
                break;
            case CIRCLE:
                drawOval(g2, x, y, s2, s2, lineColor, fillColor);
                break;
            case DIAMOND:
                drawDiamond(g2, x, y, s2, lineColor, fillColor);
                break;
            case CROSS:
                drawCross(g2, x, y, s2, lineColor);
                break;
            case UPTRIANGLE:
                drawUpTriangle(g2, x, y, s2, lineColor, fillColor);
                break;
            case DOWNTRIANGLE:
                drawDownTriangle(g2, x, y, s2, lineColor, fillColor);
                break;
            case BOWTIE:
                drawDavid(g2, x, y, s2, lineColor, fillColor);
                break;
            case X:
                drawX(g2, x, y, s2, lineColor);
                break;
            case STAR:
                drawStar(g2, x, y, s2, lineColor);
                break;  // was missing — caused fall-through to NOSYMBOL
            case NOSYMBOL:
                break;
        }

        g2.setStroke(oldStroke);
    }

    // -----------------------------------------------------------------------
    // Individual symbol drawing methods
    // -----------------------------------------------------------------------

    /**
     * Draw a star symbol composed of four crossing lines (horizontal,
     * vertical, and two diagonals) meeting at the center.
     *
     * @param g2 the graphics context
     * @param x  x pixel coordinate of the center
     * @param y  y pixel coordinate of the center
     * @param s2 half-size (arm length) in pixels
     * @param lc line color; no-op if {@code null}
     */
    public static void drawStar(Graphics2D g2, int x, int y,
            int s2, Color lc) {
        if (lc != null) {
            g2.setColor(lc);
            g2.drawLine(x - s2, y,      x + s2, y);
            g2.drawLine(x,      y - s2, x,      y + s2);
            g2.drawLine(x - s2, y - s2, x + s2, y + s2);
            g2.drawLine(x - s2, y + s2, x + s2, y - s2);
        }
    }

    /**
     * Draw a centerd rectangle symbol.
     * <p>
     * If {@code lc} is {@code null} the fill color is used for the outline
     * so the symbol boundary remains visible.
     * </p>
     *
     * @param g2 the graphics context
     * @param x  x pixel coordinate of the center
     * @param y  y pixel coordinate of the center
     * @param w2 half-width in pixels
     * @param h2 half-height in pixels
     * @param lc outline color; defaults to {@code fc} if {@code null}
     * @param fc fill color; {@code null} suppresses fill
     */
    public static void drawRectangle(Graphics2D g2, int x, int y,
            int w2, int h2, Color lc, Color fc) {
        if (lc == null) lc = fc;
        if (fc != null) {
            g2.setColor(fc);
            g2.fillRect(x - w2, y - h2, 2 * w2, 2 * h2);
        }
        if (lc != null) {
            g2.setColor(lc);
            g2.drawRect(x - w2, y - h2, 2 * w2, 2 * h2);
        }
    }

    /**
     * Draw a centered oval symbol.
     * <p>
     * If {@code lc} is {@code null} the fill color is used for the outline.
     * </p>
     *
     * @param g2 the graphics context
     * @param x  x pixel coordinate of the center
     * @param y  y pixel coordinate of the center
     * @param w2 half-width in pixels
     * @param h2 half-height in pixels
     * @param lc outline color; defaults to {@code fc} if {@code null}
     * @param fc fill color; {@code null} suppresses fill
     */
    public static void drawOval(Graphics2D g2, int x, int y,
            int w2, int h2, Color lc, Color fc) {
        if (lc == null) lc = fc;
        if (fc != null) {
            g2.setColor(fc);
            g2.fillOval(x - w2, y - h2, 2 * w2, 2 * h2);
        }
        if (lc != null) {
            g2.setColor(lc);
            g2.drawOval(x - w2, y - h2, 2 * w2, 2 * h2);
        }
    }

    /**
     * Draw a centered upward-pointing triangle symbol.
     * <p>
     * The triangle has its apex at the top and its base at the bottom.
     * If {@code lc} is {@code null} the fill color is used for the outline.
     * </p>
     *
     * @param g2 the graphics context
     * @param x  x pixel coordinate of the center
     * @param y  y pixel coordinate of the center
     * @param s2 half-size in pixels (applies to both width and height)
     * @param lc outline color; defaults to {@code fc} if {@code null}
     * @param fc fill color; {@code null} suppresses fill
     */
    public static void drawUpTriangle(Graphics2D g2, int x, int y,
            int s2, Color lc, Color fc) {
        if (lc == null) lc = fc;
        Polygon poly = new Polygon();
        poly.addPoint(x - s2, y + s2); // bottom-left
        poly.addPoint(x,      y - s2); // apex
        poly.addPoint(x + s2, y + s2); // bottom-right
        if (fc != null) { g2.setColor(fc); g2.fillPolygon(poly); }
        if (lc != null) { g2.setColor(lc); g2.drawPolygon(poly); }
    }

    /**
     * Draw a centered downward-pointing triangle symbol.
     * <p>
     * The triangle has its apex at the bottom and its base at the top.
     * If {@code lc} is {@code null} the fill color is used for the outline.
     * </p>
     *
     * @param g2 the graphics context
     * @param x  x pixel coordinate of the center
     * @param y  y pixel coordinate of the center
     * @param s2 half-size in pixels (applies to both width and height)
     * @param lc outline color; defaults to {@code fc} if {@code null}
     * @param fc fill color; {@code null} suppresses fill
     */
    public static void drawDownTriangle(Graphics2D g2, int x, int y,
            int s2, Color lc, Color fc) {
        if (lc == null) lc = fc;
        Polygon poly = new Polygon();
        poly.addPoint(x - s2, y - s2); // top-left
        poly.addPoint(x + s2, y - s2); // top-right
        poly.addPoint(x,      y + s2); // apex
        if (fc != null) { g2.setColor(fc); g2.fillPolygon(poly); }
        if (lc != null) { g2.setColor(lc); g2.drawPolygon(poly); }
    }

    /**
     * Draw a bowtie (hourglass) symbol.
     * <p>
     * The bowtie is a six-vertex polygon: two corners on the left and right
     * edges, and one pinched point on each of the left and right sides of
     * the center. 
     * </p>
     *
     * @param g2 the graphics context
     * @param x  x pixel coordinate of the center
     * @param y  y pixel coordinate of the center
     * @param s2 half-size in pixels
     * @param lc outline color; {@code null} suppresses outline
     * @param fc fill color; {@code null} suppresses fill
     */
    public static void drawDavid(Graphics2D g2, int x, int y,
            int s2, Color lc, Color fc) {
        Polygon poly = new Polygon();
        poly.addPoint(x - s2, y - s2); // top-left
        poly.addPoint(x + s2, y - s2); // top-right
        poly.addPoint(x + 1,  y);      // right pinch
        poly.addPoint(x + s2, y + s2); // bottom-right
        poly.addPoint(x - s2, y + s2); // bottom-left
        poly.addPoint(x - 1,  y);      // left pinch
        if (fc != null) { g2.setColor(fc); g2.fillPolygon(poly); }
        if (lc != null) { g2.setColor(lc); g2.drawPolygon(poly); }
    }

    /**
     * Draw a centered cross (+) symbol.
     *
     * @param g2 the graphics context
     * @param x  x pixel coordinate of the center
     * @param y  y pixel coordinate of the center
     * @param s2 half-size (arm length) in pixels
     * @param lc line color; no-op if {@code null}
     */
    public static void drawCross(Graphics2D g2, int x, int y,
            int s2, Color lc) {
        if (lc != null) {
            g2.setColor(lc);
            g2.drawLine(x - s2, y,      x + s2, y);
            g2.drawLine(x,      y - s2, x,      y + s2);
        }
    }

    /**
     * Draw a centered X (×) symbol.
     *
     * @param g2 the graphics context
     * @param x  x pixel coordinate of the center
     * @param y  y pixel coordinate of the center
     * @param s2 half-size (arm length) in pixels
     * @param lc line color; no-op if {@code null}
     */
    public static void drawX(Graphics2D g2, int x, int y,
            int s2, Color lc) {
        if (lc != null) {
            g2.setColor(lc);
            g2.drawLine(x - s2, y - s2, x + s2, y + s2);
            g2.drawLine(x - s2, y + s2, x + s2, y - s2);
        }
    }

    /**
     * Draw a centered diamond symbol.
     * <p>
     * The diamond has four vertices at the top, bottom, left, and right
     * extremes of the bounding box defined by {@code s2}.
     * If {@code lc} is {@code null} the fill color is used for the outline.
     * </p>
     *
     * @param g2 the graphics context
     * @param x  x pixel coordinate of the center
     * @param y  y pixel coordinate of the center
     * @param s2 half-size in pixels (applies to all four arms)
     * @param lc outline colou=r; defaults to {@code fc} if {@code null}
     * @param fc fill color; {@code null} suppresses fill
     */
    public static void drawDiamond(Graphics2D g2, int x, int y,
            int s2, Color lc, Color fc) {
        if (lc == null) lc = fc;
        Polygon poly = new Polygon();
        poly.addPoint(x - s2, y);      // left
        poly.addPoint(x,      y - s2); // top
        poly.addPoint(x + s2, y);      // right
        poly.addPoint(x,      y + s2); // bottom
        if (fc != null) { g2.setColor(fc); g2.fillPolygon(poly); }
        if (lc != null) { g2.setColor(lc); g2.drawPolygon(poly); }
    }
}