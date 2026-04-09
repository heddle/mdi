package edu.cnu.mdi.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.graphics.style.LineStyle;

/**
 * Static utility methods for general-purpose 2D graphics operations.
 *
 * <h2>Graphics context</h2>
 * <p>
 * Every drawing method in this class accepts a {@link Graphics2D} context
 * directly. The legacy {@link java.awt.Graphics} parameter type has been
 * removed throughout; no internal casts are performed.
 * </p>
 * <p>
 * <strong>Note for callers that override {@link Component#paint}:</strong>
 * Java AWT declares {@code paint(Graphics g)} with a fixed signature. Inside
 * that override, cast once at the entry point and pass the result to these
 * utilities:
 * </p>
 * <pre>
 * &#64;Override
 * public void paint(Graphics g) {
 *     Graphics2D g2 = (Graphics2D) g;
 *     GraphicsUtils.drawHighlightedRectangle(g2, rect);
 * }
 * </pre>
 *
 * <h2>Stroke caching</h2>
 * <p>
 * {@link BasicStroke} objects are expensive to construct and are immutable.
 * {@link #getStroke(float, LineStyle)} caches every stroke it creates in
 * {@link #strokes} so that repeated calls with the same parameters return the
 * same object without allocation.
 * </p>
 *
 * <h2>Highlighted drawing</h2>
 * <p>
 * "Highlighted" shapes (rectangles, ovals, polylines, etc.) are drawn twice
 * with complementary dashed strokes — {@link #dash1}/{@link #dash2} or
 * {@link #dash1_2}/{@link #dash2_2} — in two alternating colors. The result
 * is a marching-ants selection indicator. Default colors are
 * {@link #highlightColor1} (red) and {@link #highlightColor2} (yellow).
 * </p>
 */
public class GraphicsUtils {

    // -----------------------------------------------------------------------
    // Highlight colors
    // -----------------------------------------------------------------------

    /**
     * Primary color used for highlighted (marching-ants) drawing.
     * Defaults to {@link Color#red}.
     */
    public static final Color highlightColor1 = Color.red;

    /**
     * Secondary color used for highlighted (marching-ants) drawing.
     * Defaults to {@link Color#yellow}.
     */
    public static final Color highlightColor2 = Color.yellow;

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /**
     * Pixel tolerance used when hit-testing whether a point lies on a line.
     * A point is considered "on" the line if it is within this many pixels.
     */
    private static final double SELECTRES = 3.01;

    /**
     * Dash pattern array shared by all highlighted-drawing strokes.
     * Produces alternating 8-pixel on/off segments.
     */
    private static final float[] DASH = { 8.0f };

    // -----------------------------------------------------------------------
    // Highlighted-drawing strokes
    // -----------------------------------------------------------------------

    /**
     * 1-pixel dashed stroke, phase 0 — first pass of a marching-ants pair.
     * Used with {@link #dash2} to draw highlighted outlines.
     */
    public static final BasicStroke dash1 = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            8.0f, DASH, 0.0f);

    /**
     * 1-pixel dashed stroke, phase 8 — second pass of a marching-ants pair.
     * Used with {@link #dash1} to draw highlighted outlines.
     */
    public static final BasicStroke dash2 = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            8.0f, DASH, DASH[0]);

    /**
     * 2-pixel dashed stroke, phase 0 — first pass of a medium-weight
     * marching-ants pair. Used with {@link #dash2_2}.
     */
    public static final BasicStroke dash1_2 = new BasicStroke(
            2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            8.0f, DASH, 0.0f);

    /**
     * 2-pixel dashed stroke, phase 8 — second pass of a medium-weight
     * marching-ants pair. Used with {@link #dash1_2}.
     */
    public static final BasicStroke dash2_2 = new BasicStroke(
            2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            8.0f, DASH, DASH[0]);

    /**
     * 4-pixel dashed stroke, phase 0 — first pass of a thick marching-ants
     * pair. Used with {@link #dash2_2t}.
     */
    public static final BasicStroke dash1_2t = new BasicStroke(
            4.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            8.0f, DASH, 0.0f);

    /**
     * 4-pixel dashed stroke, phase 8 — second pass of a thick marching-ants
     * pair. Used with {@link #dash1_2t}.
     */
    public static final BasicStroke dash2_2t = new BasicStroke(
            4.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            8.0f, DASH, DASH[0]);

    /**
     * 1-pixel dashed stroke, phase 8. A convenient general-purpose dashed
     * stroke for non-highlighted use.
     */
    public static final BasicStroke simpleDash = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            8.0f, DASH, DASH[0]);

    /**
     * 1-pixel dashed stroke, phase 0.5. A slight phase offset from
     * {@link #simpleDash} for situations where two complementary dashed lines
     * are needed without the full marching-ants machinery.
     */
    public static final BasicStroke simpleDash2 = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            8.0f, DASH, 0.5f);

    // -----------------------------------------------------------------------
    // Stroke cache
    // -----------------------------------------------------------------------

    /**
     * Cache of {@link BasicStroke} objects, keyed by a string encoding the
     * line width and {@link LineStyle}.
     * <p>
     * Strokes are immutable and relatively expensive to construct. Caching
     * ensures that the same logical stroke is never allocated more than once
     * per JVM lifetime. In practice there are typically no more than six to
     * eight distinct strokes in any application.
     * </p>
     */
    protected static final Hashtable<String, BasicStroke> strokes =
            new Hashtable<>();

    // -----------------------------------------------------------------------
    // Component size / style helpers (Swing / macOS)
    // -----------------------------------------------------------------------

    /**
     * Apply the "small" size variant to a Swing component.
     * <p>
     * This is a macOS-specific client property that instructs the native
     * look-and-feel to render the component at a reduced size. It has no
     * effect on other platforms.
     * </p>
     *
     * @param component the component to resize; must not be {@code null}
     */
    public static void setSizeSmall(JComponent component) {
        component.putClientProperty("JComponent.sizeVariant", "small");
    }

    /**
     * Apply the "mini" size variant to a Swing component.
     * <p>
     * This is a macOS-specific client property that instructs the native
     * look-and-feel to render the component at its smallest available size.
     * It has no effect on other platforms.
     * </p>
     *
     * @param component the component to resize; must not be {@code null}
     */
    public static void setSizeMini(JComponent component) {
        component.putClientProperty("JComponent.sizeVariant", "mini");
    }

    /**
     * Apply the square button style to a {@link JButton}.
     * <p>
     * On macOS this suppresses the default rounded-capsule style in favour of
     * a square-cornered button that is more compact in tight layouts. Has no
     * effect on other platforms.
     * </p>
     *
     * @param button the button to restyle; must not be {@code null}
     */
    public static void setSquareButton(JButton button) {
        button.putClientProperty("JButton.buttonType", "square");
    }

    /**
     * Apply the textured button style to a {@link JButton}.
     * <p>
     * On macOS this gives the button a brushed-metal texture suitable for
     * toolbar-adjacent controls. Has no effect on other platforms.
     * </p>
     *
     * @param button the button to restyle; must not be {@code null}
     */
    public static void setTexturedButton(JButton button) {
        button.putClientProperty("JButton.buttonType", "textured");
    }

    // -----------------------------------------------------------------------
    // Clip helpers
    // -----------------------------------------------------------------------

    /**
     * Compute the intersection of a current clip shape with a rectangle.
     * <p>
     * Returns the intersection bounding rectangle, or {@code null} if either
     * argument is {@code null}, degenerate (zero area), or if the intersection
     * is empty.
     * </p>
     *
     * @param currentClip the current clip shape (e.g. from
     *                    {@link Graphics2D#getClip()})
     * @param rect        the rectangle to intersect with the clip
     * @return the intersection as a {@link Rectangle}, or {@code null}
     */
    public static Rectangle minClip(Shape currentClip, Rectangle rect) {
        if (currentClip == null || rect == null
                || rect.width == 0 || rect.height == 0) {
            return null;
        }
        Rectangle cb = currentClip.getBounds();
        if (cb == null || cb.width == 0 || cb.height == 0) {
            return null;
        }
        SwingUtilities.computeIntersection(
                rect.x, rect.y, rect.width, rect.height, cb);
        return cb;
    }

    // -----------------------------------------------------------------------
    // Stroke factory
    // -----------------------------------------------------------------------

    /**
     * Return a cached {@link BasicStroke} for the given line width and style.
     * <p>
     * The first call for any (width, style) combination creates and caches the
     * stroke; subsequent calls return the cached instance. A width of zero is
     * treated identically to a width of one.
     * </p>
     * <p>
     * Supported styles and their dash patterns:
     * </p>
     * <ul>
     *   <li>{@link LineStyle#SOLID} — no dash (round cap/join)</li>
     *   <li>{@link LineStyle#DASH} — 10 on, 10 off</li>
     *   <li>{@link LineStyle#DOT_DASH} — 4-4-10-4</li>
     *   <li>{@link LineStyle#DOT} — 4 on, 4 off</li>
     *   <li>{@link LineStyle#DOUBLE_DASH} — 10-4-10-10</li>
     *   <li>{@link LineStyle#LONG_DASH} — 15 on, 15 off</li>
     *   <li>{@link LineStyle#LONG_DOT_DASH} — 6-4-15-4</li>
     * </ul>
     *
     * @param lineWidth the desired stroke width in pixels
     * @param lineStyle the dash pattern style; must not be {@code null}
     * @return a {@link BasicStroke} matching the requested parameters
     */
    public static BasicStroke getStroke(float lineWidth, LineStyle lineStyle) {
        String hashKey = "STROKE_LW_" + lineWidth + "_LT_" + lineStyle;
        BasicStroke stroke = strokes.get(hashKey);

        if (stroke == null) {
            if (lineStyle.equals(LineStyle.SOLID)) {
                stroke = new BasicStroke(lineWidth,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            } else if (lineStyle.equals(LineStyle.DASH)) {
                stroke = new BasicStroke(lineWidth,
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 8.0f,
                        new float[] { 10.0f, 10.0f }, 0.0f);
            } else if (lineStyle.equals(LineStyle.DOT_DASH)) {
                stroke = new BasicStroke(lineWidth,
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 8.0f,
                        new float[] { 4.0f, 4.0f, 10.0f, 4.0f }, 0.0f);
            } else if (lineStyle.equals(LineStyle.DOT)) {
                stroke = new BasicStroke(lineWidth,
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 8.0f,
                        new float[] { 4.0f, 4.0f }, 0.0f);
            } else if (lineStyle.equals(LineStyle.DOUBLE_DASH)) {
                stroke = new BasicStroke(lineWidth,
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 8.0f,
                        new float[] { 10.0f, 4.0f, 10.0f, 10.0f }, 0.0f);
            } else if (lineStyle.equals(LineStyle.LONG_DASH)) {
                stroke = new BasicStroke(lineWidth,
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 8.0f,
                        new float[] { 15.0f, 15.0f }, 0.0f);
            } else if (lineStyle.equals(LineStyle.LONG_DOT_DASH)) {
                stroke = new BasicStroke(lineWidth,
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 8.0f,
                        new float[] { 6.0f, 4.0f, 15.0f, 4.0f }, 0.0f);
            } else {
                stroke = new BasicStroke(lineWidth);
            }
            strokes.put(hashKey, stroke);
        }
        return stroke;
    }

    /**
     * Copy a {@link BasicStroke}, replacing its width with {@code newWidth}
     * while preserving all other attributes (cap, join, miter limit, dash
     * array, and dash phase).
     *
     * @param original the stroke to copy; must not be {@code null}
     * @param newWidth the replacement width in pixels
     * @return a new {@link BasicStroke} identical to {@code original} except
     *         for the width
     */
    public static BasicStroke copyWithNewWidth(BasicStroke original,
            float newWidth) {
        return new BasicStroke(newWidth,
                original.getEndCap(),
                original.getLineJoin(),
                original.getMiterLimit(),
                original.getDashArray(),
                original.getDashPhase());
    }

    // -----------------------------------------------------------------------
    // Geometry helpers
    // -----------------------------------------------------------------------

    /**
     * Return the four corners of a {@link Rectangle} as an array of
     * {@link Point} objects.
     * <p>
     * Corners are returned in order: top-left, top-right, bottom-right,
     * bottom-left. Useful for computing item selection handles.
     * </p>
     *
     * @param rect the rectangle; must not be {@code null}
     * @return array of four corner points
     */
    public static Point[] rectangleToPoints(Rectangle rect) {
        int l = rect.x;
        int t = rect.y;
        int r = l + rect.width;
        int b = t + rect.height;
        return new Point[] {
            new Point(l, t),
            new Point(r, t),
            new Point(r, b),
            new Point(l, b)
        };
    }

    /**
     * Build a {@link Rectangle} from two arbitrary corner points.
     * <p>
     * The returned rectangle always has non-negative width and height,
     * regardless of the relative positions of the two points.
     * </p>
     *
     * @param p1 one corner; if {@code null} and {@code p2} is non-null,
     *           returns a zero-size rectangle at {@code p2}
     * @param p2 the opposite corner; if {@code null} and {@code p1} is
     *           non-null, returns a zero-size rectangle at {@code p1}
     * @return the bounding rectangle, or {@code null} if both points are
     *         {@code null}
     */
    public static Rectangle rectangleFromPoints(Point p1, Point p2) {
        if (p1 == null && p2 == null) return null;
        if (p1 == null) return new Rectangle(p2.x, p2.y, 0, 0);
        if (p2 == null) return new Rectangle(p1.x, p1.y, 0, 0);
        int w = Math.abs(p2.x - p1.x);
        int h = Math.abs(p2.y - p1.y);
        int x = Math.min(p1.x, p2.x);
        int y = Math.min(p1.y, p2.y);
        return new Rectangle(x, y, w, h);
    }


   // -----------------------------------------------------------------------
    // Off-screen image helpers
    // -----------------------------------------------------------------------

    /**
     * Allocate an opaque {@link BufferedImage} large enough to hold an
     * off-screen rendering of {@code c}.
     * <p>
     * The image is not painted; call
     * {@link #paintComponentOnImage(Component, BufferedImage)} separately.
     * Returns {@code null} if {@code c} is {@code null} or has zero size.
     * </p>
     *
     * @param c the component whose size determines the image dimensions
     * @return a new {@code TYPE_INT_RGB} image, or {@code null}
     */
    public static BufferedImage getComponentImageBuffer(Component c) {
        if (c == null) return null;
        Dimension size = c.getSize();
        if (size.width < 1 || size.height < 1) return null;
        return new BufferedImage(size.width, size.height,
                BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Allocate a translucent {@link BufferedImage} large enough to hold an
     * off-screen rendering of {@code c}.
     * <p>
     * Use this instead of {@link #getComponentImageBuffer} when the component
     * may have transparent or semi-transparent regions.
     * Returns {@code null} if {@code c} is {@code null} or has zero size.
     * </p>
     *
     * @param c the component whose size determines the image dimensions
     * @return a new translucent image, or {@code null}
     */
    public static BufferedImage getComponentTranslucentImageBuffer(Component c) {
        if (c == null) return null;
        Dimension size = c.getSize();
        if (size.width < 1 || size.height < 1) return null;
        return new BufferedImage(size.width, size.height,
                Transparency.TRANSLUCENT);
    }

    /**
     * Paint {@code c} onto an existing {@link BufferedImage}.
     * <p>
     * A {@link Graphics2D} context is obtained from the image, the component
     * is painted, and the context is disposed. The image must be at least as
     * large as the component.
     * </p>
     *
     * @param c     the component to paint; no-op if {@code null}
     * @param image the target image; no-op if {@code null}
     */
    public static void paintComponentOnImage(Component c, BufferedImage image) {
        if (c == null || image == null) return;
        Dimension size = c.getSize();
        if (size.width < 1 || size.height < 1) return;
        Graphics2D g2 = image.createGraphics();
        c.paint(g2);
        g2.dispose();
    }

    /**
     * Render {@code c} to a new {@link BufferedImage} and return it.
     * <p>
     * Combines {@link #getComponentImageBuffer} and
     * {@link #paintComponentOnImage} in a single call.
     * </p>
     *
     * @param c the component to render; may return {@code null} if {@code c}
     *          is {@code null} or has zero size
     * @return an image containing the rendered component, or {@code null}
     */
    public static BufferedImage getComponentImage(Component c) {
        BufferedImage image = getComponentImageBuffer(c);
        paintComponentOnImage(c, image);
        return image;
    }

    // -----------------------------------------------------------------------
    // Text drawing
    // -----------------------------------------------------------------------

    /**
     * Draw etched text.
     * <p>
     * The string is drawn twice: first in white at {@code (x+1, y+1)} to
     * create a shadow, then in black at {@code (x, y)}. This makes the text
     * readable against any background color.
     * </p>
     *
     * @param g2   the graphics context
     * @param text the string to draw; must not be {@code null}
     * @param x    the x pixel coordinate of the text baseline anchor
     * @param y    the y pixel coordinate of the text baseline
     */
    public static void drawEtchedText(Graphics2D g2, String text, int x, int y) {
        g2.setColor(Color.white);
        g2.drawString(text, x + 1, y + 1);
        g2.setColor(Color.black);
        g2.drawString(text, x, y);
    }

    /**
     * Draw a string at the given position, rotated by {@code angleDegrees}
     * around its baseline anchor.
     * <p>
     * If the angle is less than 0.5° (effectively zero) the text is drawn
     * normally without any rotation transform.
     * </p>
     *
     * @param g2           the graphics context
     * @param s            the string to draw; must not be {@code null}
     * @param font         the font to use
     * @param x            the x pixel coordinate of the baseline anchor
     * @param y            the y pixel coordinate of the baseline anchor
     * @param angleDegrees the counter-clockwise rotation angle in degrees
     */
    public static void drawRotatedText(Graphics2D g2, String s, Font font,
            int x, int y, double angleDegrees) {
        drawRotatedText(g2, s, font, x, y, 0, 0, angleDegrees);
    }

    /**
     * Draw a string rotated around an anchor point with an additional
     * unrotated offset applied before rotation.
     * <p>
     * The offset {@code (delX, delY)} is first rotated by {@code angleDegrees}
     * and then added to the anchor {@code (xo, yo)} to determine where the
     * string baseline is drawn.
     * </p>
     * <p>
     * If the angle is less than 0.5° (effectively zero) the text is drawn
     * normally at {@code (xo + delX, yo + delY)} without any rotation
     * transform, which avoids floating-point error in the identity case.
     * </p>
     *
     * @param g2           the graphics context
     * @param s            the string to draw; must not be {@code null}
     * @param font         the font to use
     * @param xo           the x pixel coordinate of the rotation anchor
     * @param yo           the y pixel coordinate of the rotation anchor
     * @param delX         horizontal offset from the anchor, measured before
     *                     rotation is applied
     * @param delY         vertical offset from the anchor, measured before
     *                     rotation is applied
     * @param angleDegrees the counter-clockwise rotation angle in degrees
     */
    public static void drawRotatedText(Graphics2D g2, String s, Font font,
            int xo, int yo, int delX, int delY, double angleDegrees) {

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (Math.abs(angleDegrees) < 0.5) {
            g2.setFont(font);
            g2.drawString(s, xo + delX, yo + delY);
            return;
        }

        AffineTransform rotation = new AffineTransform();
        rotation.rotate(Math.toRadians(angleDegrees));

        Point2D.Double offset    = new Point2D.Double(delX, delY);
        Point2D.Double rotOffset = new Point2D.Double();
        rotation.transform(offset, rotOffset);

        AffineTransform translation = AffineTransform.getTranslateInstance(
                xo + rotOffset.x, yo + rotOffset.y);
        g2.transform(translation);

        g2.setFont(font.deriveFont(rotation));
        g2.drawString(s, 0, 0);

        try {
            g2.transform(translation.createInverse());
        } catch (NoninvertibleTransformException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------
    // Styled line
    // -----------------------------------------------------------------------

    /**
     * Draw a line with explicit color, width, and dash style.
     * <p>
     * The graphics context's stroke is restored to its previous value after
     * drawing.
     * </p>
     *
     * @param g2        the graphics context
     * @param lineColor the line color; must not be {@code null}
     * @param lineWidth the line width in pixels
     * @param lineStyle the dash style
     * @param x1        x pixel coordinate of the start point
     * @param y1        y pixel coordinate of the start point
     * @param x2        x pixel coordinate of the end point
     * @param y2        y pixel coordinate of the end point
     */
    public static void drawStyleLine(Graphics2D g2, Color lineColor,
            float lineWidth, LineStyle lineStyle,
            int x1, int y1, int x2, int y2) {
        g2.setColor(lineColor);
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(GraphicsUtils.getStroke(lineWidth, lineStyle));
        g2.drawLine(x1, y1, x2, y2);
        g2.setStroke(oldStroke);
    }


    // -----------------------------------------------------------------------
    // Highlighted shapes
    // -----------------------------------------------------------------------

    /**
     * Draw a highlighted rectangle using the default colors
     * ({@link #highlightColor1} and {@link #highlightColor2}).
     *
     * @param g2 the graphics context
     * @param r  the rectangle to highlight; no-op if {@code null}
     */
    public static void drawHighlightedRectangle(Graphics2D g2, Rectangle r) {
        drawHighlightedRectangle(g2, r, highlightColor1, highlightColor2);
    }

    /**
     * Draw a highlighted (marching-ants) rectangle outline.
     * <p>
     * The rectangle is drawn twice: once with {@link #dash1} in {@code color1}
     * and once with {@link #dash2} in {@code color2}. The complementary dash
     * phases produce the marching-ants effect.
     * </p>
     *
     * @param g2     the graphics context; no-op if {@code null}
     * @param r      the rectangle to highlight; no-op if {@code null}
     * @param color1 primary dash color
     * @param color2 secondary dash color
     */
    public static void drawHighlightedRectangle(Graphics2D g2, Rectangle r,
            Color color1, Color color2) {
        if (g2 == null || r == null) return;
        Stroke old = g2.getStroke();
        g2.setStroke(dash1); g2.setColor(color1);
        g2.drawRect(r.x, r.y, r.width, r.height);
        g2.setStroke(dash2); g2.setColor(color2);
        g2.drawRect(r.x, r.y, r.width, r.height);
        g2.setStroke(old);
    }

    /**
     * Draw a highlighted (marching-ants) line segment.
     * <p>
     * The line is drawn twice with {@link #dash1_2} and {@link #dash2_2}
     * (2-pixel width) in alternating colors.
     * </p>
     *
     * @param g2     the graphics context
     * @param x1     x pixel coordinate of the start point
     * @param y1     y pixel coordinate of the start point
     * @param x2     x pixel coordinate of the end point
     * @param y2     y pixel coordinate of the end point
     * @param color1 primary dash color
     * @param color2 secondary dash color
     */
    public static void drawHighlightedLine(Graphics2D g2,
            int x1, int y1, int x2, int y2,
            Color color1, Color color2) {
        Stroke old = g2.getStroke();
        g2.setStroke(dash1_2); g2.setColor(color1);
        g2.drawLine(x1, y1, x2, y2);
        g2.setStroke(dash2_2); g2.setColor(color2);
        g2.drawLine(x1, y1, x2, y2);
        g2.setStroke(old);
    }

    /**
     * Draw a thick highlighted (marching-ants) line segment.
     * <p>
     * Identical to {@link #drawHighlightedLine} but uses {@link #dash1_2t}
     * and {@link #dash2_2t} (4-pixel width) for a more prominent indicator.
     * </p>
     *
     * @param g2     the graphics context
     * @param x1     x pixel coordinate of the start point
     * @param y1     y pixel coordinate of the start point
     * @param x2     x pixel coordinate of the end point
     * @param y2     y pixel coordinate of the end point
     * @param color1 primary dash color
     * @param color2 secondary dash color
     */
    public static void drawThickHighlightedLine(Graphics2D g2,
            int x1, int y1, int x2, int y2,
            Color color1, Color color2) {
        Stroke old = g2.getStroke();
        g2.setStroke(dash1_2t); g2.setColor(color1);
        g2.drawLine(x1, y1, x2, y2);
        g2.setStroke(dash2_2t); g2.setColor(color2);
        g2.drawLine(x1, y1, x2, y2);
        g2.setStroke(old);
    }

    /**
     * Draw a highlighted (marching-ants) arc.
     * <p>
     * The arc is drawn twice with {@link #dash1} and {@link #dash2}
     * (1-pixel width) in alternating colors.
     * </p>
     *
     * @param g2         the graphics context
     * @param x          x pixel coordinate of the bounding rectangle's
     *                   top-left corner
     * @param y          y pixel coordinate of the bounding rectangle's
     *                   top-left corner
     * @param width      width of the bounding rectangle in pixels
     * @param height     height of the bounding rectangle in pixels
     * @param startAngle start angle of the arc in degrees (0 = 3 o'clock,
     *                   measured counter-clockwise)
     * @param arcAngle   angular extent of the arc in degrees
     * @param color1     primary dash color
     * @param color2     secondary dash color
     */
    public static void drawHighlightedArc(Graphics2D g2,
            int x, int y, int width, int height,
            int startAngle, int arcAngle,
            Color color1, Color color2) {
        Stroke old = g2.getStroke();
        g2.setStroke(dash1); g2.setColor(color1);
        g2.drawArc(x, y, width, height, startAngle, arcAngle);
        g2.setStroke(dash2); g2.setColor(color2);
        g2.drawArc(x, y, width, height, startAngle, arcAngle);
        g2.setStroke(old);
    }

    /**
     * Draw a highlighted oval using the default colors
     * ({@link #highlightColor1} and {@link #highlightColor2}).
     *
     * @param g2 the graphics context
     * @param r  the bounding rectangle of the oval; no-op if {@code null}
     */
    public static void drawHighlightedOval(Graphics2D g2, Rectangle r) {
        drawHighlightedOval(g2, r, highlightColor1, highlightColor2);
    }

    /**
     * Draw a highlighted (marching-ants) oval outline.
     * <p>
     * The oval is drawn twice with {@link #dash1} and {@link #dash2}
     * (1-pixel width) in alternating colors.
     * </p>
     *
     * @param g2     the graphics context; no-op if {@code null}
     * @param r      the bounding rectangle of the oval; no-op if {@code null}
     * @param color1 primary dash color
     * @param color2 secondary dash color
     */
    public static void drawHighlightedOval(Graphics2D g2, Rectangle r,
            Color color1, Color color2) {
        if (g2 == null || r == null) return;
        Stroke old = g2.getStroke();
        g2.setStroke(dash1); g2.setColor(color1);
        g2.drawOval(r.x, r.y, r.width, r.height);
        g2.setStroke(dash2); g2.setColor(color2);
        g2.drawOval(r.x, r.y, r.width, r.height);
        g2.setStroke(old);
    }

    /**
     * Draw a highlighted polyline using the default colors
     * ({@link #highlightColor1} and {@link #highlightColor2}).
     *
     * @param g2 the graphics context
     * @param x  x coordinate array
     * @param y  y coordinate array
     * @param n  number of points to draw
     */
    public static void drawHighlightedPolyline(Graphics2D g2,
            int[] x, int[] y, int n) {
        drawHighlightedPolyline(g2, x, y, n, highlightColor1, highlightColor2);
    }

    /**
     * Draw a highlighted (marching-ants) open polyline.
     * <p>
     * The polyline is drawn twice with {@link #dash1} and {@link #dash2}
     * (1-pixel width) in alternating colors.
     * </p>
     *
     * @param g2     the graphics context
     * @param x      x coordinate array
     * @param y      y coordinate array
     * @param n      number of points to draw
     * @param color1 primary dash color
     * @param color2 secondary dash color
     */
    public static void drawHighlightedPolyline(Graphics2D g2,
            int[] x, int[] y, int n,
            Color color1, Color color2) {
        Stroke old = g2.getStroke();
        g2.setStroke(dash1); g2.setColor(color1);
        g2.drawPolyline(x, y, n);
        g2.setStroke(dash2); g2.setColor(color2);
        g2.drawPolyline(x, y, n);
        g2.setStroke(old);
    }

    /**
     * Draw a highlighted shape using the default colors
     * ({@link #highlightColor1} and {@link #highlightColor2}).
     *
     * @param g2    the graphics context
     * @param shape the shape to highlight; no-op if {@code null}
     */
    public static void drawHighlightedShape(Graphics2D g2, Shape shape) {
        drawHighlightedShape(g2, shape, highlightColor1, highlightColor2);
    }

    /**
     * Draw a highlighted (marching-ants) arbitrary {@link Shape} outline.
     * <p>
     * The shape is drawn twice with {@link #dash1} and {@link #dash2}
     * (1-pixel width) in alternating colors.
     * </p>
     *
     * @param g2     the graphics context; no-op if {@code null}
     * @param shape  the shape to highlight; no-op if {@code null}
     * @param color1 primary dash color
     * @param color2 secondary dash color
     */
    public static void drawHighlightedShape(Graphics2D g2, Shape shape,
            Color color1, Color color2) {
        if (g2 == null || shape == null) return;
        Stroke old = g2.getStroke();
        g2.setStroke(dash1); g2.setColor(color1);
        g2.draw(shape);
        g2.setStroke(dash2); g2.setColor(color2);
        g2.draw(shape);
        g2.setStroke(old);
    }

    // -----------------------------------------------------------------------
    // Hit testing
    // -----------------------------------------------------------------------

    /**
     * Test whether a pixel point lies on a pixel line segment within the
     * tolerance defined by {@link #SELECTRES}.
     * <p>
     * The test is performed in pixel space using a parametric projection. A
     * point is considered "on" the line if:
     * </p>
     * <ul>
     *   <li>The projection of the point onto the infinite line through the
     *       segment falls within the segment ({@code t ∈ [0, 1]}), and</li>
     *   <li>The perpendicular distance from the point to the line is less than
     *       {@link #SELECTRES} pixels.</li>
     * </ul>
     * <p>
     * If the segment is nearly degenerate (both {@code |Δx|} and {@code |Δy|}
     * are less than 2 pixels) the method returns {@code false}.
     * </p>
     *
     * @param px     x coordinate of the point to test
     * @param py     y coordinate of the point to test
     * @param startx x coordinate of the segment start
     * @param starty y coordinate of the segment start
     * @param endx   x coordinate of the segment end
     * @param endy   y coordinate of the segment end
     * @return {@code true} if the point is within tolerance of the segment
     */
    public static boolean pointOnLine(int px, int py,
            int startx, int starty, int endx, int endy) {

        int delx  = endx - startx;
        int dely  = endy - starty;
        int fdelx = Math.abs(delx);
        int fdely = Math.abs(dely);

        if (fdelx < 2 && fdely < 2) {
            return false;
        }

        if (fdelx > fdely) {
            double t = ((double) px - startx) / delx;
            if (t < 0.0 || t > 1.0) return false;
            return Math.abs(starty + t * dely - py) < SELECTRES;
        } else {
            double t = ((double) py - starty) / dely;
            if (t < 0.0 || t > 1.0) return false;
            return Math.abs(startx + t * delx - px) < SELECTRES;
        }
    }

    /**
     * Test whether a pixel point lies on a pixel line segment within the
     * tolerance defined by {@link #SELECTRES}.
     *
     * @param p     the point to test; no-op returns {@code false} if
     *              {@code null}
     * @param start the start of the segment; no-op returns {@code false} if
     *              {@code null}
     * @param end   the end of the segment; no-op returns {@code false} if
     *              {@code null}
     * @return {@code true} if the point is within tolerance of the segment
     * @see #pointOnLine(int, int, int, int, int, int)
     */
    public static boolean pointOnLine(Point p, Point start, Point end) {
        if (p == null || start == null || end == null) return false;
        return pointOnLine(p.x, p.y, start.x, start.y, end.x, end.y);
    }

    // -----------------------------------------------------------------------
    // color utilities
    // -----------------------------------------------------------------------

    /**
     * Convert a {@link Color} to an 8-digit hex string in {@code #rrggbbaa}
     * format.
     * <p>
     * All four channels (red, green, blue, alpha) are included. Returns
     * {@code "#000000ff"} (opaque black) if {@code color} is {@code null}.
     * </p>
     *
     * @param color the color to convert; may be {@code null}
     * @return hex string in the form {@code #rrggbbaa}
     */
    public static String colorToHex(Color color) {
        if (color == null) return "#000000ff";
        return String.format("#%02x%02x%02x%02x",
                color.getRed(), color.getGreen(),
                color.getBlue(), color.getAlpha());
    }

    /**
     * Parse a hex color string in common internet format.
     * <p>
     * The string may be in any of these forms:
     * </p>
     * <ul>
     *   <li>{@code rrggbbaa} — 8 hex digits, fully specified</li>
     *   <li>{@code rrggbb} — 6 hex digits; alpha defaults to {@code ff}
     *       (fully opaque)</li>
     *   <li>{@code #rrggbbaa} or {@code #rrggbb} — leading {@code #}
     *       is optional</li>
     * </ul>
     * <p>
     * Strings shorter than 6 hex digits are zero-padded on the right.
     * Returns {@link Color#black} if {@code hex} is {@code null} or
     * cannot be parsed.
     * </p>
     *
     * @param hex the hex color string; may be {@code null}
     * @return the corresponding {@link Color}, or {@link Color#black} on error
     */
    public static Color colorFromHex(String hex) {
        if (hex == null) return Color.black;
        if (hex.startsWith("#")) hex = hex.substring(1);

        // Pad to at least 6 digits, then to 8 (alpha defaults to ff).
        while (hex.length() < 6) hex += "0";
        while (hex.length() < 8) hex += "f";

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

    // -----------------------------------------------------------------------
    // Component hierarchy
    // -----------------------------------------------------------------------

    /**
     * Walk up the component hierarchy and return the first ancestor that is
     * a {@link JInternalFrame}, {@link JDialog}, or {@link Window}.
     * <p>
     * Returns {@code null} if no such ancestor exists (e.g. the component
     * has not been added to a window yet) or if {@code component} is
     * {@code null}.
     * </p>
     *
     * @param component the component whose ancestry should be searched
     * @return the nearest enclosing top-level container, or {@code null}
     */
    public static Container getParentContainer(Component component) {
        if (component == null) return null;
        Container container = component.getParent();
        while (container != null) {
            if (container instanceof JInternalFrame
                    || container instanceof JDialog
                    || container instanceof Window) {
                return container;
            }
            container = container.getParent();
        }
        return null;
    }
}