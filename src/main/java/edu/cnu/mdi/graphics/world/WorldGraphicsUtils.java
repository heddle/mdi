package edu.cnu.mdi.graphics.world;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.LineStyle;
import edu.cnu.mdi.util.Point2DSupport;

/**
 * Static utility methods for drawing and geometry operations in world
 * coordinate space.
 *
 * <h2>Coordinate systems</h2>
 * <p>
 * The MDI framework maintains two coordinate systems for each view:
 * </p>
 * <ul>
 *   <li><b>World coordinates</b> — the application's logical coordinate
 *       space (kilometres, degrees, arbitrary units, etc.), represented by
 *       {@code double} values and {@link Point2D.Double}.</li>
 *   <li><b>Local (screen/pixel) coordinates</b> — the pixel space of the
 *       rendered component, represented by {@code int} values and
 *       {@link Point}.</li>
 * </ul>
 * <p>
 * Conversion between the two spaces is handled by
 * {@link IContainer#worldToLocal} and {@link IContainer#localToWorld}.
 * Every drawing method in this class accepts world coordinates and performs
 * the conversion internally before issuing Java2D drawing calls.
 * </p>
 *
 * <h2>Graphics context</h2>
 * <p>
 * All methods that perform drawing accept a {@link Graphics2D} context
 * directly. No internal casting from {@link java.awt.Graphics} is performed.
 * The sole exception is {@link #drawImageOnQuad}, which calls
 * {@link Graphics2D#create()} — a method declared on
 * {@link java.awt.Graphics} that returns {@link java.awt.Graphics} — and
 * therefore requires a cast back to {@link Graphics2D} for the child context.
 * This is unavoidable and is the only cast in the class.
 * </p>
 *
 * <h2>Null colors</h2>
 * <p>
 * Where fill and line colors are accepted separately, passing {@code null}
 * suppresses that part of the drawing. For example, a {@code null} fill
 * color produces an unfilled (outline-only) shape, and a {@code null} line
 * color produces a filled shape with no outline.
 * </p>
 *
 * <h2>Path support</h2>
 * <p>
 * Several methods operate on {@link Path2D.Double} objects. These methods
 * support only straight-segment paths composed of
 * {@code SEG_MOVETO}, {@code SEG_LINETO}, and {@code SEG_CLOSE} segments.
 * If a path contains curve segments ({@code SEG_QUADTO} or
 * {@code SEG_CUBICTO}) an {@link IllegalArgumentException} is thrown rather
 * than producing silently incorrect results.
 * </p>
 */
public class WorldGraphicsUtils {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Light color used for the bright half of an etched line. */
    private static final Color ETCH_LIGHT = Color.white;

    /** Dark color used for the shadow half of an etched line. */
    private static final Color ETCH_DARK = Color.black;

    /**
     * Threshold below which a value is treated as effectively zero.
     * Used to guard against division by zero and degenerate geometry.
     */
    private static final double TINY = 1.0e-8;

    /**
     * Number of line segments used to approximate a full circle or arc.
     * Higher values produce smoother curves at the cost of more vertices.
     */
    private static final int NUMCIRCSTEP = 60;

    /**
     * Default stroke: 1 pixel wide, solid style.
     * <p>
     * Applied by {@link #setStyleDefaults(Graphics2D)} and used as a
     * fallback wherever no explicit stroke is specified.
     * </p>
     */
    public static final Stroke DEFAULTSTROKE =
            GraphicsUtils.getStroke(1, LineStyle.SOLID);

    // -----------------------------------------------------------------------
    // Etched lines
    // -----------------------------------------------------------------------

    /**
     * Draw a horizontal "etched" line in world coordinates.
     * <p>
     * An etched line is rendered as two parallel pixel-offset lines in
     * contrasting colors to simulate a bevelled groove cut into a surface.
     * </p>
     *
     * @param g2         the graphics context
     * @param container  the container defining the world-to-screen transform
     * @param x          world X coordinate of the left end of the line
     * @param y          world Y coordinate of the line
     * @param w          width of the line in world units
     * @param lightAbove if {@code true}, the light color is drawn on top
     *                   (simulating illumination from above); if {@code false},
     *                   the dark color is on top
     */
    public static void drawEtchedHorizontalWorldLine(Graphics2D g2,
            IContainer container, double x, double y, double w,
            boolean lightAbove) {

        Point p1 = new Point();
        Point p2 = new Point();
        container.worldToLocal(p1, x, y);
        container.worldToLocal(p2, x + w, y);

        if (lightAbove) {
            g2.setColor(ETCH_LIGHT);
            g2.drawLine(p1.x, p1.y,     p2.x, p2.y);
            g2.setColor(ETCH_DARK);
            g2.drawLine(p1.x, p1.y + 1, p2.x, p2.y + 1);
        } else {
            g2.setColor(ETCH_DARK);
            g2.drawLine(p1.x, p1.y,     p2.x, p2.y);
            g2.setColor(ETCH_LIGHT);
            g2.drawLine(p1.x, p1.y + 1, p2.x, p2.y + 1);
        }
    }

    /**
     * Draw a vertical "etched" line in world coordinates.
     * <p>
     * An etched line is rendered as two parallel pixel-offset lines in
     * contrasting colors to simulate a bevelled groove cut into a surface.
     * </p>
     *
     * @param g2        the graphics context
     * @param container the container defining the world-to-screen transform
     * @param x         world X coordinate of the vertical line
     * @param y         world Y coordinate of the top end of the line
     * @param h         height of the line in world units
     * @param lightLeft if {@code true}, the light color is drawn on the left
     *                  (simulating illumination from the left); if {@code false},
     *                  the dark color is on the left
     */
    public static void drawEtchedVerticalWorldLine(Graphics2D g2,
            IContainer container, double x, double y, double h,
            boolean lightLeft) {

        Point p1 = new Point();
        Point p2 = new Point();
        container.worldToLocal(p1, x, y);
        container.worldToLocal(p2, x, y + h);

        if (lightLeft) {
            g2.setColor(ETCH_LIGHT);
            g2.drawLine(p1.x,     p1.y, p2.x,     p2.y);
            g2.setColor(ETCH_DARK);
            g2.drawLine(p1.x + 1, p1.y, p2.x + 1, p2.y);
        } else {
            g2.setColor(ETCH_DARK);
            g2.drawLine(p1.x,     p1.y, p2.x,     p2.y);
            g2.setColor(ETCH_LIGHT);
            g2.drawLine(p1.x + 1, p1.y, p2.x + 1, p2.y);
        }
    }

    // -----------------------------------------------------------------------
    // Lines
    // -----------------------------------------------------------------------

    /**
     * Draw a styled world line between two {@link Point2D.Double} endpoints.
     * <p>
     * color, width, and dash pattern are taken from {@code style}.
     * </p>
     *
     * @param g2        the graphics context
     * @param container the container defining the world-to-screen transform
     * @param wp0       first endpoint in world coordinates
     * @param wp1       second endpoint in world coordinates
     * @param style     source of line color, width, and style
     */
    public static void drawWorldLine(Graphics2D g2, IContainer container,
            Point2D.Double wp0, Point2D.Double wp1, IStyled style) {
        drawWorldLine(g2, container, wp0.x, wp0.y, wp1.x, wp1.y, style);
    }

    /**
     * Draw a styled world line between two coordinate pairs.
     * <p>
     * color, width, and dash pattern are taken from {@code style}.
     * </p>
     *
     * @param g2        the graphics context
     * @param container the container defining the world-to-screen transform
     * @param x1        world X of the first endpoint
     * @param y1        world Y of the first endpoint
     * @param x2        world X of the second endpoint
     * @param y2        world Y of the second endpoint
     * @param style     source of line color, width, and style
     */
    public static void drawWorldLine(Graphics2D g2, IContainer container,
            double x1, double y1, double x2, double y2, IStyled style) {
        drawWorldLine(g2, container, x1, y1, x2, y2,
                style.getLineColor(), style.getLineWidth(), style.getLineStyle());
    }

    /**
     * Draw a world line with explicit color, width, and dash style.
     * <p>
     * If {@code lineColor} is {@code null} the method returns immediately
     * without drawing.
     * </p>
     *
     * @param g2        the graphics context
     * @param container the container defining the world-to-screen transform
     * @param x1        world X of the first endpoint
     * @param y1        world Y of the first endpoint
     * @param x2        world X of the second endpoint
     * @param y2        world Y of the second endpoint
     * @param lineColor line color; {@code null} suppresses drawing
     * @param lineWidth line width in pixels
     * @param lineStyle dash style
     */
    public static void drawWorldLine(Graphics2D g2, IContainer container,
            double x1, double y1, double x2, double y2,
            Color lineColor, float lineWidth, LineStyle lineStyle) {

        if (lineColor == null) {
            return;
        }

        Point p1 = new Point();
        Point p2 = new Point();
        container.worldToLocal(p1, x1, y1);
        container.worldToLocal(p2, x2, y2);

        Stroke oldStroke = g2.getStroke();
        g2.setStroke(GraphicsUtils.getStroke(lineWidth, lineStyle));
        g2.setColor(lineColor);
        g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        g2.setStroke(oldStroke);
    }

    /**
     * Draw a 1-pixel solid world line between two {@link Point2D.Double}
     * endpoints.
     *
     * @param g2        the graphics context
     * @param container the container defining the world-to-screen transform
     * @param wp0       first endpoint in world coordinates
     * @param wp1       second endpoint in world coordinates
     * @param lineColor line color; {@code null} suppresses drawing
     */
    public static void drawWorldLine(Graphics2D g2, IContainer container,
            Point2D.Double wp0, Point2D.Double wp1, Color lineColor) {
        drawWorldLine(g2, container,
                wp0.x, wp0.y, wp1.x, wp1.y, lineColor, 1, LineStyle.SOLID);
    }

    /**
     * Draw a 1-pixel solid world line between two coordinate pairs.
     *
     * @param g2        the graphics context
     * @param container the container defining the world-to-screen transform
     * @param x1        world X of the first endpoint
     * @param y1        world Y of the first endpoint
     * @param x2        world X of the second endpoint
     * @param y2        world Y of the second endpoint
     * @param lineColor line color; {@code null} suppresses drawing
     */
    public static void drawWorldLine(Graphics2D g2, IContainer container,
            double x1, double y1, double x2, double y2, Color lineColor) {
        drawWorldLine(g2, container,
                x1, y1, x2, y2, lineColor, 1, LineStyle.SOLID);
    }

    // -----------------------------------------------------------------------
    // Style defaults
    // -----------------------------------------------------------------------

    /**
     * Reset the graphics context to the MDI default drawing style.
     * <p>
     * Sets the color to {@link Color#black} and the stroke to
     * {@link #DEFAULTSTROKE} (1 pixel wide, solid). Useful at the start of a
     * draw method that does not receive explicit style parameters.
     * </p>
     *
     * @param g2 the graphics context to reset
     */
    public static void setStyleDefaults(Graphics2D g2) {
        g2.setColor(Color.black);
        g2.setStroke(DEFAULTSTROKE);
    }

    // -----------------------------------------------------------------------
    // Coordinate helpers
    // -----------------------------------------------------------------------

    /**
     * Convert two world endpoints to their pixel equivalents.
     * <p>
     * The results are written into the pre-allocated {@link Point} objects
     * {@code p0} and {@code p1} to avoid allocating new objects on the
     * critical drawing path.
     * </p>
     *
     * @param container the container defining the world-to-screen transform
     * @param p0        receives the pixel coordinates of {@code wp0}
     * @param p1        receives the pixel coordinates of {@code wp1}
     * @param wp0       first world endpoint
     * @param wp1       second world endpoint
     */
    public static void getPixelEnds(IContainer container,
            Point p0, Point p1,
            Point2D.Double wp0, Point2D.Double wp1) {
        container.worldToLocal(p0, wp0.x, wp0.y);
        container.worldToLocal(p1, wp1.x, wp1.y);
    }

    /**
     * Compute the pixel bounding rectangle for a line between two world points.
     * <p>
     * The returned rectangle is expanded to a minimum size of 2 × 2 pixels so
     * that nearly-horizontal and nearly-vertical lines still have a usable
     * hit-test area.
     * </p>
     *
     * @param container the container defining the world-to-screen transform
     * @param wp0       first world endpoint
     * @param wp1       second world endpoint
     * @return pixel bounding rectangle with a minimum dimension of 2
     */
    public static Rectangle getBounds(IContainer container,
            Point2D.Double wp0, Point2D.Double wp1) {
        Point p0 = new Point();
        Point p1 = new Point();
        getPixelEnds(container, p0, p1, wp0, wp1);
        Rectangle r = GraphicsUtils.rectangleFromPoints(p0, p1);
        if (r.height < 2) { r.y -= 1; r.height += 2; }
        if (r.width  < 2) { r.x -= 1; r.width  += 2; }
        return r;
    }

    /**
     * Compute the pixel bounding rectangle for a line between two world
     * coordinate pairs.
     * <p>
     * The returned rectangle is expanded to a minimum size of 2 × 2 pixels so
     * that nearly-horizontal and nearly-vertical lines still have a usable
     * hit-test area.
     * </p>
     *
     * @param container the container defining the world-to-screen transform
     * @param x1        world X of the first endpoint
     * @param y1        world Y of the first endpoint
     * @param x2        world X of the second endpoint
     * @param y2        world Y of the second endpoint
     * @return pixel bounding rectangle with a minimum dimension of 2
     */
    public static Rectangle getBounds(IContainer container,
            double x1, double y1, double x2, double y2) {
        Point p0 = new Point();
        Point p1 = new Point();
        container.worldToLocal(p0, x1, y1);
        container.worldToLocal(p1, x2, y2);
        Rectangle r = GraphicsUtils.rectangleFromPoints(p0, p1);
        if (r.height < 2) { r.y -= 1; r.height += 2; }
        if (r.width  < 2) { r.x -= 1; r.width  += 2; }
        return r;
    }

    // -----------------------------------------------------------------------
    // Angle helpers
    // -----------------------------------------------------------------------

    /**
     * Compute the signed sweep angle in degrees from vector
     * {@code (wpc→wp1)} to vector {@code (wpc→wp2)}.
     * <p>
     * Uses {@code atan2(cross, dot)} so the result carries the correct sign:
     * positive for a counter-clockwise rotation, negative for clockwise.
     * </p>
     *
     * @param wpc the common origin of the two vectors (pivot point)
     * @param wp1 tip of the first vector; defines the start direction
     * @param wp2 tip of the second vector; defines the end direction
     * @return signed sweep angle in degrees, in the range {@code (-180, 180]}
     */
    public static double signedSweepDeg(Point2D.Double wpc,
            Point2D.Double wp1, Point2D.Double wp2) {
        double x1 = wp1.x - wpc.x;
        double y1 = wp1.y - wpc.y;
        double x2 = wp2.x - wpc.x;
        double y2 = wp2.y - wpc.y;
        return Math.toDegrees(Math.atan2(x1 * y2 - y1 * x2, x1 * x2 + y1 * y2));
    }

    /**
     * Compute the counter-clockwise (CCW) sweep angle in degrees from vector
     * {@code (wpc→wp1)} to vector {@code (wpc→wp2)}.
     * <p>
     * Unlike {@link #signedSweepDeg}, this method always returns a value in
     * {@code [0, 360)} so that major arcs are representable. For example, a
     * signed angle of −60° becomes 300°.
     * </p>
     *
     * @param wpc the common origin of the two vectors (pivot point)
     * @param wp1 tip of the first vector; defines the start direction
     * @param wp2 tip of the second vector; defines the end direction
     * @return CCW sweep angle in degrees, in the range {@code [0, 360)}
     */
    public static double ccwSweepDeg(Point2D.Double wpc,
            Point2D.Double wp1, Point2D.Double wp2) {
        double x1 = wp1.x - wpc.x;
        double y1 = wp1.y - wpc.y;
        double x2 = wp2.x - wpc.x;
        double y2 = wp2.y - wpc.y;
        double a = Math.toDegrees(
                Math.atan2(x1 * y2 - y1 * x2, x1 * x2 + y1 * y2));
        if (a < 0.0)    a += 360.0;
        if (a >= 360.0) a -= 360.0;
        return a;
    }

    /**
     * Unwrap a signed angle measurement so it varies continuously relative to
     * a previous unwrapped angle, eliminating ±180° discontinuities.
     * <p>
     * This is useful when accumulating a running sweep angle during an
     * interactive drag: without unwrapping, crossing the ±180° boundary
     * causes the accumulated angle to jump by 360°.
     * </p>
     *
     * @param prevUnwrapped the previous accumulated (unwrapped) angle, which
     *                      may lie outside {@code (-180, 180]}
     * @param signedNow     the new raw measurement from
     *                      {@link #signedSweepDeg}, in {@code (-180, 180]}
     * @return the new unwrapped angle, adjusted to be continuous with
     *         {@code prevUnwrapped}
     */
    public static double unwrapSweepDeg(double prevUnwrapped, double signedNow) {
        double prevWrapped = prevUnwrapped % 360.0;
        if (prevWrapped <= -180.0) prevWrapped += 360.0;
        if (prevWrapped >   180.0) prevWrapped -= 360.0;

        double delta = signedNow - prevWrapped;
        if (delta >  180.0) delta -= 360.0;
        if (delta < -180.0) delta += 360.0;

        return prevUnwrapped + delta;
    }

    // -----------------------------------------------------------------------
    // Arcs and radar arcs
    // -----------------------------------------------------------------------

    /**
     * Draw a radar arc (annular sector) with explicit style parameters.
     * <p>
     * A radar arc is defined by inner and outer radii ({@code rmin},
     * {@code rmax}) and an angular sweep from {@code startAngle} to
     * {@code stopAngle}. The resulting shape is the region between the two
     * arcs, closed by two radial edges.
     * </p>
     *
     * @param g2         the graphics context
     * @param container  the container defining the world-to-screen transform
     * @param xc         world X of the arc center
     * @param yc         world Y of the arc center
     * @param rmin       inner radius in world units
     * @param rmax       outer radius in world units
     * @param startAngle start angle in degrees
     * @param stopAngle  stop angle in degrees
     * @param fillColor  fill color; {@code null} suppresses fill
     * @param lineColor  outline color; {@code null} suppresses outline
     * @param lineWidth  outline width in pixels
     * @param lineStyle  outline dash style
     */
    public static void drawWorldRadArc(Graphics2D g2, IContainer container,
            double xc, double yc, double rmin, double rmax,
            double startAngle, double stopAngle,
            Color fillColor, Color lineColor,
            float lineWidth, LineStyle lineStyle) {

        Polygon p0 = createWorldArc(container, xc, yc, rmin, startAngle, stopAngle);
        Polygon p1 = createWorldArc(container, xc, yc, rmax, stopAngle, startAngle);

        GeneralPath path = new GeneralPath(Path2D.WIND_EVEN_ODD);
        path.append(p0, false);
        path.append(p1, false);

        if (fillColor != null) {
            g2.setColor(fillColor);
            g2.fill(path);
        }

        if (lineColor != null) {
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(GraphicsUtils.getStroke(lineWidth, lineStyle));
            g2.setColor(lineColor);
            g2.draw(path);
            g2.setStroke(oldStroke);
        }
    }

    /**
     * Draw a radar arc (annular sector) with a 1-pixel solid outline.
     *
     * @param g2         the graphics context
     * @param container  the container defining the world-to-screen transform
     * @param xc         world X of the arc center
     * @param yc         world Y of the arc center
     * @param rmin       inner radius in world units
     * @param rmax       outer radius in world units
     * @param startAngle start angle in degrees
     * @param stopAngle  stop angle in degrees
     * @param fillColor  fill color; {@code null} suppresses fill
     * @param lineColor  outline color; {@code null} suppresses outline
     */
    public static void drawWorldRadArc(Graphics2D g2, IContainer container,
            double xc, double yc, double rmin, double rmax,
            double startAngle, double stopAngle,
            Color fillColor, Color lineColor) {
        drawWorldRadArc(g2, container, xc, yc, rmin, rmax,
                startAngle, stopAngle, fillColor, lineColor, 1, LineStyle.SOLID);
    }

    /**
     * Draw a radar arc (annular sector) using an {@link IStyled} source for
     * fill color, line color, line width, and line style.
     *
     * @param g2         the graphics context
     * @param container  the container defining the world-to-screen transform
     * @param xc         world X of the arc center
     * @param yc         world Y of the arc center
     * @param rmin       inner radius in world units
     * @param rmax       outer radius in world units
     * @param startAngle start angle in degrees
     * @param stopAngle  stop angle in degrees
     * @param style      source of fill color, line color, width, and style
     */
    public static void drawWorldRadArc(Graphics2D g2, IContainer container,
            double xc, double yc, double rmin, double rmax,
            double startAngle, double stopAngle, IStyled style) {
        drawWorldRadArc(g2, container, xc, yc, rmin, rmax,
                startAngle, stopAngle,
                style.getFillColor(), style.getLineColor(),
                style.getLineWidth(), style.getLineStyle());
    }

    // -----------------------------------------------------------------------
    // Image on quad
    // -----------------------------------------------------------------------

    /**
     * Draw a {@link BufferedImage} mapped onto an arbitrary world-coordinate
     * quadrilateral.
     * <p>
     * The image is mapped so that its top-left pixel aligns with
     * {@code wpoly[0]}, and the two edges adjacent to that corner determine
     * the width and height directions. The method automatically selects which
     * adjacent corner ({@code wpoly[1]} or {@code wpoly[3]}) is the width
     * direction by matching the quad's aspect ratio to the image's aspect
     * ratio.
     * </p>
     * <p>
     * If the affine mapping has a negative determinant (i.e. the quad is
     * mirror-reflected in local coordinates) the v-axis of the image mapping
     * is flipped to prevent the image from being drawn upside down.
     * </p>
     * <p>
     * <strong>Note on casting:</strong> this method calls
     * {@link Graphics2D#create()}, which is declared on
     * {@link java.awt.Graphics} and returns {@link java.awt.Graphics}. A cast
     * to {@link Graphics2D} is therefore required for the child context. This
     * is the only unavoidable cast in {@code WorldGraphicsUtils}.
     * </p>
     *
     * @param g2        the graphics context
     * @param image     the image to draw; ignored if {@code null}
     * @param wpoly     four world-coordinate corners in order:
     *                  {@code [w0, wa, w2, wb]} where {@code wa} and
     *                  {@code wb} are the corners adjacent to {@code w0};
     *                  ignored if {@code null} or fewer than 4 elements
     * @param container the container defining the world-to-screen transform
     */
    public static void drawImageOnQuad(Graphics2D g2, BufferedImage image,
            Point2D.Double[] wpoly, IContainer container) {

        if (image == null || wpoly == null || wpoly.length < 4) {
            return;
        }

        Point2D.Double w0 = wpoly[0];
        Point2D.Double wa = wpoly[1];
        Point2D.Double wb = wpoly[3];

        double imgW = image.getWidth();
        double imgH = image.getHeight();
        double imgRatio = imgW / imgH;

        double ra = w0.distance(wa);
        double rb = w0.distance(wb);

        // Select width/height directions by matching aspect ratio to image.
        Point2D.Double wW = wb;
        Point2D.Double wH = wa;
        if (Math.abs((ra / rb) - imgRatio) < Math.abs((rb / ra) - imgRatio)) {
            wW = wa;
            wH = wb;
        }

        Point p0 = new Point();
        Point pW = new Point();
        Point pH = new Point();
        container.worldToLocal(p0, w0);
        container.worldToLocal(pW, wW);
        container.worldToLocal(pH, wH);

        double x0 = p0.x, y0 = p0.y;
        double x1 = pW.x, y1 = pW.y;
        double x2 = pH.x, y2 = pH.y;

        // If the affine basis is mirrored, flip the v-axis to avoid drawing
        // the image upside-down.
        double det = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0);
        if (det < 0) {
            double ox0 = x0, oy0 = y0;
            double ox1 = x1, oy1 = y1;
            double ox2 = x2, oy2 = y2;
            x0 = ox2; y0 = oy2;
            x2 = ox0; y2 = oy0;
            x1 = ox1 + (ox2 - ox0);
            y1 = oy1 + (oy2 - oy0);
        }

        double m00 = (x1 - x0) / imgW;
        double m10 = (y1 - y0) / imgW;
        double m01 = (x2 - x0) / imgH;
        double m11 = (y2 - y0) / imgH;

        // create() returns Graphics, so the cast is unavoidable here.
        Graphics2D g2d = (Graphics2D) g2.create();
        try {
            g2d.drawImage(image,
                    new AffineTransform(m00, m10, m01, m11, x0, y0), null);
        } finally {
            g2d.dispose();
        }
    }

    // -----------------------------------------------------------------------
    // Circles, donuts, arcs — polygon factories
    // -----------------------------------------------------------------------

    /**
     * Build a screen {@link Polygon} approximating a world-coordinate circle.
     * <p>
     * The circle is approximated by {@value #NUMCIRCSTEP} line segments.
     * </p>
     *
     * @param container the container defining the world-to-screen transform
     * @param xc        world X of the center
     * @param yc        world Y of the center
     * @param radius    radius in world units
     * @return polygon approximating the circle in pixel coordinates
     */
    public static Polygon createWorldCircle(IContainer container,
            double xc, double yc, double radius) {
        int[] x = new int[NUMCIRCSTEP];
        int[] y = new int[NUMCIRCSTEP];
        double delAng = (2.0 * Math.PI) / (NUMCIRCSTEP - 1);
        Point pp = new Point();
        for (int i = 0; i < NUMCIRCSTEP; i++) {
            double theta = i * delAng;
            container.worldToLocal(pp,
                    xc + radius * Math.cos(theta),
                    yc + radius * Math.sin(theta));
            x[i] = pp.x;
            y[i] = pp.y;
        }
        return new Polygon(x, y, NUMCIRCSTEP);
    }

    /**
     * Build a screen {@link Polygon} approximating a world-coordinate donut
     * (annulus).
     * <p>
     * If {@code minRadius} is less than {@link #TINY} the inner ring is
     * degenerate and the result degrades gracefully to a plain circle at
     * {@code maxRadius}.
     * </p>
     *
     * @param container the container defining the world-to-screen transform
     * @param xc        world X of the center
     * @param yc        world Y of the center
     * @param minRadius inner radius in world units
     * @param maxRadius outer radius in world units
     * @return polygon approximating the annulus in pixel coordinates
     */
    public static Polygon createWorldDonut(IContainer container,
            double xc, double yc, double minRadius, double maxRadius) {

        if (minRadius < TINY) {
            return createWorldCircle(container, xc, yc, maxRadius);
        }

        int n2 = 2 * NUMCIRCSTEP;
        int[] x = new int[n2];
        int[] y = new int[n2];
        double delAng = (2.0 * Math.PI) / (NUMCIRCSTEP - 1);
        Point pp = new Point();

        for (int i = 0; i < NUMCIRCSTEP; i++) {
            int j = i + NUMCIRCSTEP;
            double theta = i * delAng;
            double cos = Math.cos(theta);
            double sin = Math.sin(theta);

            container.worldToLocal(pp, xc + minRadius * cos, yc + minRadius * sin);
            x[i] = pp.x; y[i] = pp.y;

            container.worldToLocal(pp, xc + maxRadius * cos, yc + maxRadius * sin);
            x[j] = pp.x; y[j] = pp.y;
        }

        return new Polygon(x, y, n2);
    }

    /**
     * Build a screen {@link Polygon} approximating a world-coordinate arc
     * (a portion of a circle's circumference).
     * <p>
     * If the angular span {@code |stopAngle - startAngle|} is at least
     * 360° − {@link #TINY}, the full circle is returned via
     * {@link #createWorldCircle}.
     * </p>
     *
     * @param container  the container defining the world-to-screen transform
     * @param xc         world X of the center
     * @param yc         world Y of the center
     * @param radius     radius in world units
     * @param startAngle start angle in degrees
     * @param stopAngle  stop angle in degrees
     * @return polygon approximating the arc in pixel coordinates
     */
    public static Polygon createWorldArc(IContainer container,
            double xc, double yc, double radius,
            double startAngle, double stopAngle) {

        if (Math.abs(stopAngle - startAngle) > (360.0 - TINY)) {
            return createWorldCircle(container, xc, yc, radius);
        }

        int[] x = new int[NUMCIRCSTEP];
        int[] y = new int[NUMCIRCSTEP];
        double rad0 = Math.toRadians(startAngle);
        double rad1 = Math.toRadians(stopAngle);
        double delAng = (rad1 - rad0) / (NUMCIRCSTEP - 1);
        Point pp = new Point();

        for (int i = 0; i < NUMCIRCSTEP; i++) {
            double theta = rad0 + i * delAng;
            container.worldToLocal(pp,
                    xc + radius * Math.cos(theta),
                    yc + radius * Math.sin(theta));
            x[i] = pp.x;
            y[i] = pp.y;
        }

        return new Polygon(x, y, NUMCIRCSTEP);
    }

    // -----------------------------------------------------------------------
    // Ovals
    // -----------------------------------------------------------------------

    /**
     * Draw a world oval with explicit style parameters.
     * <p>
     * The oval is inscribed within the axis-aligned world rectangle
     * {@code worldRectangle}.
     * </p>
     *
     * @param g2             the graphics context
     * @param container      the container defining the world-to-screen transform
     * @param worldRectangle bounding world rectangle of the oval
     * @param fillColor      fill color; {@code null} suppresses fill
     * @param lineColor      outline color; {@code null} suppresses outline
     * @param lineWidth      outline width in pixels
     * @param lineStyle      outline dash style
     */
    public static void drawWorldOval(Graphics2D g2, IContainer container,
            Rectangle2D.Double worldRectangle,
            Color fillColor, Color lineColor,
            float lineWidth, LineStyle lineStyle) {

        Rectangle r = new Rectangle();
        container.worldToLocal(r, worldRectangle);

        if (fillColor != null) {
            g2.setColor(fillColor);
            g2.fillOval(r.x, r.y, r.width, r.height);
        }

        if (lineColor != null) {
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(GraphicsUtils.getStroke(lineWidth, lineStyle));
            g2.setColor(lineColor);
            g2.drawOval(r.x, r.y, r.width, r.height);
            g2.setStroke(oldStroke);
        }
    }

    /**
     * Draw a world oval with a 1-pixel solid outline.
     *
     * @param g2             the graphics context
     * @param container      the container defining the world-to-screen transform
     * @param worldRectangle bounding world rectangle of the oval
     * @param fillColor      fill color; {@code null} suppresses fill
     * @param lineColor      outline color; {@code null} suppresses outline
     */
    public static void drawWorldOval(Graphics2D g2, IContainer container,
            Rectangle2D.Double worldRectangle,
            Color fillColor, Color lineColor) {
        drawWorldOval(g2, container, worldRectangle,
                fillColor, lineColor, 1, LineStyle.SOLID);
    }

    /**
     * Draw a world oval using an {@link IStyled} source for style properties.
     *
     * @param g2             the graphics context
     * @param container      the container defining the world-to-screen transform
     * @param worldRectangle bounding world rectangle of the oval
     * @param style          source of fill color, line color, width, and style
     */
    public static void drawWorldOval(Graphics2D g2, IContainer container,
            Rectangle2D.Double worldRectangle, IStyled style) {
        drawWorldOval(g2, container, worldRectangle,
                style.getFillColor(), style.getLineColor(),
                style.getLineWidth(), style.getLineStyle());
    }

    // -----------------------------------------------------------------------
    // Rectangles
    // -----------------------------------------------------------------------

    /**
     * Draw a world rectangle with explicit style parameters.
     *
     * @param g2             the graphics context
     * @param container      the container defining the world-to-screen transform
     * @param worldRectangle the world rectangle to draw
     * @param fillColor      fill color; {@code null} suppresses fill
     * @param lineColor      outline color; {@code null} suppresses outline
     * @param lineWidth      outline width in pixels
     * @param lineStyle      outline dash style
     */
    public static void drawWorldRectangle(Graphics2D g2, IContainer container,
            Rectangle2D.Double worldRectangle,
            Color fillColor, Color lineColor,
            float lineWidth, LineStyle lineStyle) {

        Rectangle r = new Rectangle();
        container.worldToLocal(r, worldRectangle);

        if (fillColor != null) {
            g2.setColor(fillColor);
            g2.fillRect(r.x, r.y, r.width, r.height);
        }

        if (lineColor != null) {
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(GraphicsUtils.getStroke(lineWidth, lineStyle));
            g2.setColor(lineColor);
            g2.drawRect(r.x, r.y, r.width, r.height);
            g2.setStroke(oldStroke);
        }
    }

    /**
     * Draw a world rectangle with a 1-pixel solid outline.
     *
     * @param g2             the graphics context
     * @param container      the container defining the world-to-screen transform
     * @param worldRectangle the world rectangle to draw
     * @param fillColor      fill color; {@code null} suppresses fill
     * @param lineColor      outline color; {@code null} suppresses outline
     */
    public static void drawWorldRectangle(Graphics2D g2, IContainer container,
            Rectangle2D.Double worldRectangle,
            Color fillColor, Color lineColor) {
        drawWorldRectangle(g2, container, worldRectangle,
                fillColor, lineColor, 1, LineStyle.SOLID);
    }

    /**
     * Draw a world rectangle using an {@link IStyled} source for style
     * properties.
     *
     * @param g2             the graphics context
     * @param container      the container defining the world-to-screen transform
     * @param worldRectangle the world rectangle to draw
     * @param style          source of fill color, line color, width, and style
     */
    public static void drawWorldRectangle(Graphics2D g2, IContainer container,
            Rectangle2D.Double worldRectangle, IStyled style) {
        drawWorldRectangle(g2, container, worldRectangle,
                style.getFillColor(), style.getLineColor(),
                style.getLineWidth(), style.getLineStyle());
    }

    /**
     * Draw the outline of a highlighted (marching-ants style) world rectangle.
     * <p>
     * The outline alternates between {@code color1} and {@code color2} on
     * successive pixels to produce a high-visibility selection indicator.
     * </p>
     *
     * @param g2        the graphics context
     * @param container the container defining the world-to-screen transform
     * @param wr        the world rectangle to highlight
     * @param color1    first alternating color
     * @param color2    second alternating color
     */
    public static void drawHighlightedRectangle(Graphics2D g2,
            IContainer container, Rectangle2D.Double wr,
            Color color1, Color color2) {
        Rectangle r = new Rectangle();
        container.worldToLocal(r, wr);
        GraphicsUtils.drawHighlightedRectangle(g2, r, color1, color2);
    }

    // -----------------------------------------------------------------------
    // Polygons
    // -----------------------------------------------------------------------

    /**
     * Draw a world polygon with a 1-pixel solid outline.
     *
     * @param g2           the graphics context
     * @param container    the container defining the world-to-screen transform
     * @param worldPolygon the polygon in world coordinates
     * @param fillColor    fill color; {@code null} suppresses fill
     * @param lineColor    outline color; {@code null} suppresses outline
     * @param lineWidth    outline width in pixels
     */
    public static void drawWorldPolygon(Graphics2D g2, IContainer container,
            WorldPolygon worldPolygon,
            Color fillColor, Color lineColor, float lineWidth) {
        drawWorldPolygon(g2, container, worldPolygon,
                fillColor, lineColor, lineWidth, LineStyle.SOLID);
    }

    /**
     * Draw a world polygon with explicit style parameters.
     *
     * @param g2           the graphics context
     * @param container    the container defining the world-to-screen transform
     * @param worldPolygon the polygon in world coordinates
     * @param fillColor    fill color; {@code null} suppresses fill
     * @param lineColor    outline color; {@code null} suppresses outline
     * @param lineWidth    outline width in pixels
     * @param lineStyle    outline dash style
     */
    public static void drawWorldPolygon(Graphics2D g2, IContainer container,
            WorldPolygon worldPolygon,
            Color fillColor, Color lineColor,
            float lineWidth, LineStyle lineStyle) {

        Polygon polygon = new Polygon();
        container.worldToLocal(polygon, worldPolygon);

        if (fillColor != null) {
            g2.setColor(fillColor);
            g2.fillPolygon(polygon);
        }

        if (lineColor != null) {
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(GraphicsUtils.getStroke(lineWidth, lineStyle));
            g2.setColor(lineColor);
            g2.drawPolygon(polygon);
            g2.setStroke(oldStroke);
        }
    }

    // -----------------------------------------------------------------------
    // Text
    // -----------------------------------------------------------------------

    /**
     * Draw a string anchored at a world coordinate with a pixel offset.
     * <p>
     * The text is drawn using the current font and color of the graphics
     * context. The pixel offset {@code (dh, dv)} is applied after the
     * world-to-screen conversion, allowing fine-grained label placement
     * relative to a world point.
     * </p>
     *
     * @param g2        the graphics context
     * @param container the container defining the world-to-screen transform
     * @param x         world X of the text anchor
     * @param y         world Y of the text anchor (baseline)
     * @param text      the string to draw; ignored if {@code null}
     * @param dh        additional horizontal pixel offset (positive = right)
     * @param dv        additional vertical pixel offset (positive = down)
     */
    public static void drawWorldText(Graphics2D g2, IContainer container,
            double x, double y, String text, int dh, int dv) {
        if (text == null) {
            return;
        }
        Point p = new Point();
        container.worldToLocal(p, x, y);
        g2.drawString(text, p.x + dh, p.y + dv);
    }

    // -----------------------------------------------------------------------
    // Path drawing
    // -----------------------------------------------------------------------

    /**
     * Draw a straight-segment world path as either a closed polygon or an
     * open polyline, deriving style from an {@link IStyled} object.
     *
     * @param g2        the graphics context
     * @param container the container defining the world-to-screen transform
     * @param path      the world path (straight segments only)
     * @param style     source of fill color, line color, width, and style
     * @param closed    if {@code true} the path is drawn as a filled/outlined
     *                  polygon; if {@code false} as an open polyline
     * @return the pixel {@link Polygon} that was drawn, or {@code null} if
     *         the path had no vertices
     */
    public static Polygon drawPath2D(Graphics2D g2, IContainer container,
            Path2D.Double path, IStyled style, boolean closed) {
        return drawPath2D(g2, container, path,
                style.getFillColor(), style.getLineColor(),
                style.getLineWidth(), style.getLineStyle(), closed);
    }

    /**
     * Draw a straight-segment world path as either a closed polygon or an
     * open polyline, with explicit style parameters.
     * <p>
     * Anti-aliasing is enabled for the duration of this call to produce
     * smooth diagonal edges. If {@code lineWidth} is zero it is clamped to
     * 0.5 so the path remains visible at all zoom levels.
     * </p>
     *
     * @param g2        the graphics context
     * @param container the container defining the world-to-screen transform
     * @param path      the world path (straight segments only)
     * @param fillColor fill color; {@code null} suppresses fill (only
     *                  meaningful when {@code closed} is {@code true})
     * @param lineColor outline color; {@code null} suppresses outline
     * @param lineWidth outline width in pixels; clamped to 0.5 if zero
     * @param lineStyle outline dash style
     * @param closed    if {@code true} the path is drawn as a filled/outlined
     *                  polygon; if {@code false} as an open polyline
     * @return the pixel {@link Polygon} that was drawn, or {@code null} if
     *         the path had no vertices
     */
    public static Polygon drawPath2D(Graphics2D g2, IContainer container,
            Path2D.Double path, Color fillColor, Color lineColor,
            float lineWidth, LineStyle lineStyle, boolean closed) {

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        float effectiveWidth = (lineWidth == 0) ? 0.5f : lineWidth;

        Polygon poly = pathToPolygon(container, path);
        if (poly == null) {
            return null;
        }

        if (fillColor != null && closed) {
            g2.setColor(fillColor);
            g2.fillPolygon(poly);
        }

        if (lineColor != null) {
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(GraphicsUtils.getStroke(effectiveWidth, lineStyle));
            g2.setColor(lineColor);
            if (closed) {
                g2.drawPolygon(poly);
            } else {
                g2.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);
            }
            g2.setStroke(oldStroke);
        }

        return poly;
    }

    // -----------------------------------------------------------------------
    // Path inspection and conversion
    // -----------------------------------------------------------------------

    /**
     * Count the explicit vertex points in a straight-segment {@link Path2D.Double}.
     * <p>
     * Vertices are contributed by {@code SEG_MOVETO} and {@code SEG_LINETO}
     * segments only. {@code SEG_CLOSE} is ignored.
     * </p>
     *
     * @param path the path to inspect; {@code null} returns 0
     * @return the number of MOVETO + LINETO vertices
     * @throws IllegalArgumentException if the path contains curve segments
     */
    public static int getPathPointCount(Path2D.Double path) {
        if (path == null) {
            return 0;
        }
        int count = 0;
        double[] coords = new double[6];
        PathIterator pi = path.getPathIterator(null);
        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    count++;
                    break;
                case PathIterator.SEG_CLOSE:
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Path contains curve segments; "
                            + "only MOVE/LINE/CLOSE are supported.");
            }
            pi.next();
        }
        return count;
    }

    /**
     * Return the capacity (vertex count) needed to store a straight-segment
     * path in a flat array.
     * <p>
     * This is a convenience alias for {@link #getPathPointCount(Path2D.Double)},
     * intended for use at array-allocation sites to make the intent clear.
     * </p>
     *
     * @param path the path; {@code null} returns 0
     * @return number of MOVETO + LINETO vertices
     * @throws IllegalArgumentException if the path contains curve segments
     */
    public static int neededCapacity(Path2D.Double path) {
        return getPathPointCount(path);
    }

    /**
     * Extract the vertices of a straight-segment {@link Path2D} as a list.
     * <p>
     * Vertices from {@code SEG_MOVETO} and {@code SEG_LINETO} are included.
     * Iteration stops when a {@code SEG_CLOSE} is encountered if only one
     * sub-path has been seen; multiple sub-paths are not supported.
     * </p>
     *
     * @param path the world path; must not be {@code null}
     * @return a list of world vertices in path order
     * @throws IllegalArgumentException if the path contains curve segments or
     *                                  multiple sub-paths
     */
    public static ArrayList<Point2D.Double> getPoints(Path2D path) {
        ArrayList<Point2D.Double> points = new ArrayList<>();
        double[] coords = new double[6];
        int numSubPaths = 0;
        for (PathIterator pi = path.getPathIterator(null);
                !pi.isDone(); pi.next()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    points.add(new Point2D.Double(coords[0], coords[1]));
                    numSubPaths++;
                    break;
                case PathIterator.SEG_LINETO:
                    points.add(new Point2D.Double(coords[0], coords[1]));
                    break;
                case PathIterator.SEG_CLOSE:
                    if (numSubPaths > 1) {
                        throw new IllegalArgumentException(
                                "Path contains multiple sub-paths.");
                    }
                    return points;
                default:
                    throw new IllegalArgumentException(
                            "Path contains curve segments.");
            }
        }
        return points;
    }

    /**
     * Return the world-coordinate vertex at the specified zero-based index in
     * a straight-segment path.
     * <p>
     * Only {@code SEG_MOVETO} and {@code SEG_LINETO} segments contribute
     * vertices. {@code SEG_CLOSE} is ignored.
     * </p>
     *
     * @param path  the world path; {@code null} returns {@code null}
     * @param index zero-based vertex index
     * @return the vertex at {@code index}, or {@code null} if the index is
     *         out of range
     * @throws IllegalArgumentException if the path contains curve segments
     */
    public static Point2D.Double getPathPointAt(Path2D.Double path, int index) {
        if (path == null || index < 0) {
            return null;
        }
        double[] coords = new double[6];
        PathIterator pi = path.getPathIterator(null);
        int count = 0;
        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    if (count == index) {
                        return new Point2D.Double(coords[0], coords[1]);
                    }
                    count++;
                    break;
                case PathIterator.SEG_CLOSE:
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Path contains curve segments; "
                            + "only MOVE/LINE/CLOSE are supported.");
            }
            pi.next();
        }
        return null;
    }

    /**
     * Convert a straight-segment world path to a screen {@link Polygon}.
     * <p>
     * Each {@code SEG_MOVETO} and {@code SEG_LINETO} vertex is transformed
     * from world to screen coordinates. {@code SEG_CLOSE} contributes no new
     * vertex.
     * </p>
     *
     * @param container the container defining the world-to-screen transform;
     *                  {@code null} returns {@code null}
     * @param path      the world path; {@code null} returns {@code null}
     * @return screen polygon, or {@code null} if there are no vertices
     * @throws IllegalArgumentException if the path contains curve segments
     */
    public static Polygon pathToPolygon(IContainer container,
            Path2D.Double path) {
        if (container == null || path == null) {
            return null;
        }
        int n = neededCapacity(path);
        if (n <= 0) {
            return null;
        }
        int[] x = new int[n];
        int[] y = new int[n];
        int count = 0;
        Point pp = new Point();
        double[] coords = new double[6];
        PathIterator pi = path.getPathIterator(null);
        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    container.worldToLocal(pp, coords[0], coords[1]);
                    x[count] = pp.x;
                    y[count] = pp.y;
                    count++;
                    break;
                case PathIterator.SEG_CLOSE:
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Path contains curve segments; "
                            + "only MOVE/LINE/CLOSE are supported.");
            }
            pi.next();
        }
        return (count > 0) ? new Polygon(x, y, count) : null;
    }

    /**
     * Extract the world-coordinate vertices of a straight-segment path as a
     * new array.
     *
     * @param path the world path; {@code null} returns {@code null}
     * @return array of world vertices, or {@code null} if the path has none
     * @throws IllegalArgumentException if the path contains curve segments
     */
    public static Point2D.Double[] pathToWorldPolygon(Path2D.Double path) {
        if (path == null) {
            return null;
        }
        ArrayList<Point2D.Double> list = new ArrayList<>();
        double[] coords = new double[6];
        PathIterator pi = path.getPathIterator(null);
        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    list.add(new Point2D.Double(coords[0], coords[1]));
                    break;
                case PathIterator.SEG_CLOSE:
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Path contains curve segments; "
                            + "only MOVE/LINE/CLOSE are supported.");
            }
            pi.next();
        }
        return list.isEmpty() ? null : list.toArray(new Point2D.Double[0]);
    }

    /**
     * Copy the world-coordinate vertices of a straight-segment path into a
     * pre-allocated array.
     * <p>
     * Each element of {@code wp} must already be a non-null
     * {@link Point2D.Double}. Iteration stops early if {@code wp} is full.
     * </p>
     *
     * @param path the world path; no-op if {@code null}
     * @param wp   destination array of pre-allocated {@link Point2D.Double}
     *             objects; must not be {@code null}
     * @throws IllegalArgumentException if the path contains curve segments
     */
    public static void pathToWorldPolygon(Path2D.Double path,
            Point2D.Double[] wp) {
        if (path == null) {
            return;
        }
        double[] coords = new double[6];
        PathIterator pi = path.getPathIterator(null);
        int index = 0;
        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    wp[index].setLocation(coords[0], coords[1]);
                    if (++index >= wp.length) return;
                    break;
                case PathIterator.SEG_CLOSE:
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Path contains curve segments; "
                            + "only MOVE/LINE/CLOSE are supported.");
            }
            pi.next();
        }
    }

    // -----------------------------------------------------------------------
    // Path / polygon factories
    // -----------------------------------------------------------------------

    /**
     * Convert an axis-aligned world rectangle into a closed
     * {@link Path2D.Double}.
     * <p>
     * The path visits the four corners in order: top-left → bottom-left →
     * bottom-right → top-right, then closes.
     * </p>
     *
     * @param wr the world rectangle; must not be {@code null}
     * @return a closed path tracing the rectangle boundary
     */
    public static Path2D.Double worldRectangleToPath(Rectangle2D.Double wr) {
        double xmax = wr.getMaxX();
        double ymax = wr.getMaxY();
        Path2D.Double path = new Path2D.Double();
        path.moveTo(wr.x,  wr.y);
        path.lineTo(wr.x,  ymax);
        path.lineTo(xmax,  ymax);
        path.lineTo(xmax,  wr.y);
        path.closePath();
        return path;
    }

    /**
     * Convert an array of world points into an open {@link Path2D.Double}.
     * <p>
     * The first point becomes a {@code moveTo}; subsequent points become
     * {@code lineTo} calls. The path is not closed.
     * </p>
     *
     * @param points world vertices; {@code null} returns {@code null}
     * @return open world path, or {@code null} if {@code points} is
     *         {@code null}
     */
    public static Path2D.Double worldPolygonToPath(Point2D.Double[] points) {
        if (points == null) {
            return null;
        }
        Path2D.Double path = null;
        for (Point2D.Double wp : points) {
            if (path == null) {
                path = new Path2D.Double();
                path.moveTo(wp.x, wp.y);
            } else {
                path.lineTo(wp.x, wp.y);
            }
        }
        return path;
    }

    /**
     * Return the four corners of a world rectangle as a point array.
     * <p>
     * Corners are returned in order: top-left, bottom-left, bottom-right,
     * top-right.
     * </p>
     *
     * @param wr the world rectangle; must not be {@code null}
     * @return array of four {@link Point2D.Double} corners
     */
    public static Point2D.Double[] getPoints(Rectangle2D.Double wr) {
        double x1 = wr.x;
        double x2 = x1 + wr.width;
        double y1 = wr.y;
        double y2 = y1 + wr.height;
        return new Point2D.Double[] {
            new Point2D.Double(x1, y1),
            new Point2D.Double(x1, y2),
            new Point2D.Double(x2, y2),
            new Point2D.Double(x2, y1)
        };
    }

    // -----------------------------------------------------------------------
    // Geometry — intersections, centroids, areas
    // -----------------------------------------------------------------------

    /**
     * Compute the intersection point of two line segments.
     * <p>
     * Each segment is defined by two endpoints. Returns {@code null} if the
     * segments are parallel (denominator near zero) or if the intersection
     * lies outside either segment (parametric parameter outside [0, 1]).
     * </p>
     *
     * @param p1 first endpoint of segment 1
     * @param p2 second endpoint of segment 1
     * @param q1 first endpoint of segment 2
     * @param q2 second endpoint of segment 2
     * @return the intersection point, or {@code null} if there is none within
     *         both segments
     */
    public static Point2D.Double intersection(Point2D.Double p1,
            Point2D.Double p2, Point2D.Double q1, Point2D.Double q2) {

        double denom = (q2.y - q1.y) * (p2.x - p1.x)
                     - (q2.x - q1.x) * (p2.y - p1.y);
        if (Math.abs(denom) < 1.0e-12) {
            return null; // parallel
        }

        double t1 = ((q2.x - q1.x) * (p1.y - q1.y)
                   - (q2.y - q1.y) * (p1.x - q1.x)) / denom;
        if (t1 < 0.0 || t1 > 1.0) return null;

        double t2 = ((p2.x - p1.x) * (p1.y - q1.y)
                   - (p2.y - p1.y) * (p1.x - q1.x)) / denom;
        if (t2 < 0.0 || t2 > 1.0) return null;

        return new Point2D.Double(
                p1.x + t1 * (p2.x - p1.x),
                p1.y + t1 * (p2.y - p1.y));
    }

    /**
     * Test whether a world point lies inside a polygon.
     *
     * @param poly the polygon vertices; must not be {@code null}
     * @param wp   the point to test; must not be {@code null}
     * @return {@code true} if {@code wp} is inside {@code poly}
     */
    public static boolean contains(Point2D.Double[] poly, Point2D.Double wp) {
        Path2D.Double path = worldPolygonToPath(poly);
        return path != null && path.contains(wp);
    }

    /**
     * Compute the signed area of a polygon using the shoelace formula.
     * <p>
     * A positive result indicates a counter-clockwise winding; negative
     * indicates clockwise. Returns 0 for degenerate or collinear polygons.
     * </p>
     *
     * @param poly the polygon vertices; {@code null} or fewer than 3 vertices
     *             returns 0
     * @return signed area in world-coordinate square units
     */
    public static double area(Point2D.Double[] poly) {
        if (poly == null || poly.length < 3) {
            return 0.0;
        }
        double sum = 0;
        for (int i = 0; i < poly.length; i++) {
            Point2D.Double pi = poly[i];
            Point2D.Double pj = poly[(i + 1) % poly.length];
            sum += (pi.x * pj.y - pj.x * pi.y);
        }
        return 0.5 * sum;
    }

    /**
     * Compute the centroid of a polygon.
     * <p>
     * Uses the standard area-weighted formula. Returns {@code null} for
     * degenerate polygons whose area is effectively zero.
     * </p>
     *
     * @param poly the polygon vertices; {@code null} returns {@code null}
     * @return the centroid, or {@code null} if the polygon is degenerate
     */
    public static Point2D.Double getCentroid(Point2D.Double[] poly) {
        double area = area(poly);
        if (Math.abs(area) < 1.0e-12) {
            return null;
        }
        double sumx = 0;
        double sumy = 0;
        for (int i = 0; i < poly.length; i++) {
            Point2D.Double pi = poly[i];
            Point2D.Double pj = poly[(i + 1) % poly.length];
            double cp = pi.x * pj.y - pj.x * pi.y;
            sumx += (pi.x + pj.x) * cp;
            sumy += (pi.y + pj.y) * cp;
        }
        double sixa = 6.0 * area;
        return new Point2D.Double(sumx / sixa, sumy / sixa);
    }

    /**
     * Compute the centroid of a straight-segment path.
     *
     * @param path the world path; {@code null} returns {@code null}
     * @return the centroid, or {@code null} if the path is degenerate
     */
    public static Point2D.Double getCentroid(Path2D.Double path) {
        return getCentroid(pathToWorldPolygon(path));
    }

    /**
     * Compute the distance from {@code focus} to the farthest vertex of a
     * polygon.
     * <p>
     * This is used internally by {@link #polygonIntersection} to determine
     * how long a ray must be to guarantee it crosses the polygon boundary.
     * </p>
     *
     * @param focus the reference point; {@code null} returns {@link Double#NaN}
     * @param poly  the polygon vertices; {@code null} or empty returns
     *              {@link Double#NaN}
     * @return the longest vertex distance, or {@link Double#NaN} if inputs
     *         are invalid
     */
    public static double longestDiagonal(Point2D.Double focus,
            Point2D.Double[] poly) {
        if (focus == null || poly == null || poly.length < 1) {
            return Double.NaN;
        }
        double longest = 0.0;
        for (Point2D.Double wp : poly) {
            double d = focus.distanceSq(wp);
            if (d > longest) longest = d;
        }
        return Math.sqrt(longest);
    }

    // -----------------------------------------------------------------------
    // Polygon / path intersection
    // -----------------------------------------------------------------------

    /**
     * Find the first intersection of a ray with a world path.
     * <p>
     * The ray starts at {@code p0} and radiates in the direction of
     * {@code azimuth}. This is a convenience overload that converts the path
     * to a polygon array before delegating to
     * {@link #polygonIntersection(Point2D.Double, double, Point2D.Double[])}.
     * </p>
     *
     * @param p0      the start point (typically inside the polygon)
     * @param azimuth direction of the ray in degrees (azimuth convention:
     *                0 = north, 90 = east)
     * @param path    the world path to intersect
     * @return the first intersection point, or {@code null} if none found
     */
    public static Point2D.Double polygonIntersection(Point2D.Double p0,
            double azimuth, Path2D.Double path) {
        return polygonIntersection(p0, azimuth, pathToWorldPolygon(path));
    }

    /**
     * Find the first intersection of a ray with a world polygon.
     * <p>
     * The ray starts at {@code p0} and extends twice the length of the
     * longest vertex distance to ensure it crosses the boundary. The first
     * segment intersection found is returned.
     * </p>
     * <p>
     * This works reliably when {@code p0} is inside a convex or mildly
     * concave polygon. For very irregular polygons the "first" intersection
     * may not be the geometrically nearest one.
     * </p>
     *
     * @param p0      the start point (typically inside the polygon)
     * @param azimuth direction of the ray in degrees (azimuth convention:
     *                0 = north, 90 = east)
     * @param poly    the polygon vertices
     * @return the first intersection point, or {@code null} if none found
     */
    public static Point2D.Double polygonIntersection(Point2D.Double p0,
            double azimuth, Point2D.Double[] poly) {
        double longest = longestDiagonal(p0, poly);
        if (Double.isNaN(longest)) {
            return null;
        }
        Point2D.Double p1 = new Point2D.Double();
        project(p0, 2.0 * longest, azimuth, p1);

        int n = poly.length;
        for (int i = 0; i < n; i++) {
            Point2D.Double q0 = poly[i];
            Point2D.Double q1 = poly[(i + 1) % n];
            Point2D.Double hit = intersection(p0, p1, q0, q1);
            if (hit != null) return hit;
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Polar projection
    // -----------------------------------------------------------------------

    /**
     * Project a world point by a distance and azimuth angle.
     * <p>
     * The azimuth follows the compass convention: 0° is north (+Y), 90° is
     * east (+X), 180° is south (−Y), 270° is west (−X).
     * </p>
     *
     * @param focus    the origin point
     * @param distance the projection distance in world units
     * @param angle    the direction in degrees (azimuth: 0 = north, 90 = east)
     * @param wp       receives the projected point; must not be {@code null}
     */
    public static void project(Point2D.Double focus, double distance,
            double angle, Point2D.Double wp) {
        double rad = Math.toRadians(angle);
        wp.setLocation(focus.x + distance * Math.sin(rad),
                       focus.y + distance * Math.cos(rad));
    }

    /**
     * Create a world point at a given distance and polar angle from a center.
     * <p>
     * Unlike {@link #project}, this method uses the standard mathematical
     * polar angle convention (0° = east, 90° = north).
     * </p>
     *
     * @param center the origin point; must not be {@code null}
     * @param radius the distance from the center in world units
     * @param theta  the angle in degrees (standard polar: 0 = east, 90 = north)
     * @return the new world point
     */
    public static Point2D.Double radiusPoint(Point2D.Double center,
            double radius, double theta) {
        double rad = Math.toRadians(theta);
        return new Point2D.Double(
                center.x + radius * Math.cos(rad),
                center.y + radius * Math.sin(rad));
    }

    // -----------------------------------------------------------------------
    // Arc point arrays
    // -----------------------------------------------------------------------

    /**
     * Build an array of world points approximating a radar-arc sweep.
     * <p>
     * The array begins at the center {@code wpc}, proceeds to {@code wp1}
     * (the first leg endpoint), then sweeps counter-clockwise by
     * {@code arcAngle} degrees, using {@value #NUMCIRCSTEP} interpolation
     * steps. The first element is always the center; the remaining elements
     * trace the arc.
     * </p>
     *
     * @param wpc      the center of the arc
     * @param wp1      the first leg endpoint; the distance {@code wpc→wp1}
     *                 defines the radius
     * @param arcAngle the CCW sweep angle in degrees
     * @return array of {@value #NUMCIRCSTEP}{@code + 2} world points
     *         (center + wp1 + arc samples)
     */
    public static Point2D.Double[] getRadArcPoints(Point2D.Double wpc,
            Point2D.Double wp1, double arcAngle) {

        double dTheta = Math.toRadians(arcAngle);
        double x = wp1.x - wpc.x;
        double y = wp1.y - wpc.y;
        double del = dTheta / NUMCIRCSTEP;

        Point2D.Double[] wp = new Point2D.Double[NUMCIRCSTEP + 2];
        wp[0] = wpc;
        wp[1] = wp1;

        for (int i = 1; i <= NUMCIRCSTEP; i++) {
            double ang  = i * del;
            double cosT = Math.cos(ang);
            double sinT = Math.sin(ang);
            wp[i + 1] = new Point2D.Double(
                    wpc.x + x * cosT - y * sinT,
                    wpc.y + x * sinT + y * cosT);
        }

        return wp;
    }

    /**
     * Build an array of world points approximating a simple arc sweep.
     * <p>
     * Similar to {@link #getRadArcPoints} but does not include the center
     * point. The array begins at {@code wp1} and sweeps counter-clockwise by
     * {@code arcAngle} degrees.
     * </p>
     *
     * @param wpc      the center of the arc (not included in the output)
     * @param wp1      the first leg endpoint; the distance {@code wpc→wp1}
     *                 defines the radius
     * @param arcAngle the CCW sweep angle in degrees
     * @return array of {@value #NUMCIRCSTEP}{@code + 1} world points tracing
     *         the arc from {@code wp1}
     */
    public static Point2D.Double[] getArcPoints(Point2D.Double wpc,
            Point2D.Double wp1, double arcAngle) {

        double dTheta = Math.toRadians(arcAngle);
        double x = wp1.x - wpc.x;
        double y = wp1.y - wpc.y;
        double del = dTheta / NUMCIRCSTEP;

        Point2D.Double[] wp = new Point2D.Double[NUMCIRCSTEP + 1];
        wp[0] = wp1;

        for (int i = 0; i < NUMCIRCSTEP; i++) {
            double ang  = i * del;
            double cosT = Math.cos(ang);
            double sinT = Math.sin(ang);
            wp[i + 1] = new Point2D.Double(
                    wpc.x + x * cosT - y * sinT,
                    wpc.y + x * sinT + y * cosT);
        }

        return wp;
    }

    // -----------------------------------------------------------------------
    // Ellipse point array
    // -----------------------------------------------------------------------

    /**
     * Build an array of world points approximating an ellipse.
     * <p>
     * The ellipse is defined by its half-widths ({@code w/2}, {@code h/2})
     * and an azimuth rotation. Four primary compass points are computed first,
     * the centroid is recomputed from those, and then {@code 4 * numFill}
     * equally-spaced angular samples are generated. A closing point equal to
     * the first is appended so the returned polygon is explicitly closed.
     * </p>
     *
     * @param w       full width of the ellipse in world units
     * @param h       full height of the ellipse in world units
     * @param azimuth rotation of the ellipse in degrees (azimuth convention:
     *                0 = north, 90 = east)
     * @param numFill number of sample points per quadrant; the total returned
     *                is {@code 4 * numFill + 1}
     * @param center  the center of the ellipse
     * @return array of world points approximating the ellipse, closed at the
     *         last element
     */
    public static Point2D.Double[] getEllipsePoints(double w, double h,
            double azimuth, int numFill, Point2D.Double center) {

        double w2 = w / 2.0;
        double h2 = h / 2.0;

        Point2D.Double[] wpv = new Point2D.Double[4];
        for (int i = 0; i < 4; i++) wpv[i] = new Point2D.Double();

        project(center, h2,   0.0, wpv[0]);
        project(center, w2,  90.0, wpv[1]);
        project(center, h2, 180.0, wpv[2]);
        project(center, w2, 270.0, wpv[3]);
        center = getCentroid(wpv);

        int n = 4 * numFill;
        Point2D.Double[] bp = new Point2D.Double[n + 1];

        double ww = wpv[1].distance(wpv[3]);
        double hh = wpv[0].distance(wpv[2]);
        double rx = ww / 2.0;
        double ry = hh / 2.0;
        double deltheta = 90.0 / numFill;

        for (int i = 0; i < n; i++) {
            bp[i] = new Point2D.Double();
            double theta = Math.toRadians(i * deltheta);
            double xx =  rx * Math.sin(theta);
            double yy =  ry * Math.cos(theta);
            if (xx > 0.0) project(center,  xx,  90.0, bp[i]);
            else          project(center, -xx, 270.0, bp[i]);
            if (yy > 0.0) project(bp[i],  yy,   0.0, bp[i]);
            else          project(bp[i], -yy, 180.0, bp[i]);
        }
        bp[n] = new Point2D.Double(bp[0].x, bp[0].y); // force closed

        if (Math.abs(azimuth) > 1.0e-12) {
            for (Point2D.Double tmp : bp) {
                double gcd = center.distance(tmp);
                double az2 = Point2DSupport.azimuth(center, tmp);
                project(center, gcd, az2 + azimuth, tmp);
            }
        }

        return bp;
    }

    // -----------------------------------------------------------------------
    // Pixel density
    // -----------------------------------------------------------------------

    /**
     * Estimate the pixel density of the container in pixels per world unit.
     * <p>
     * Computed as the diagonal pixel length divided by the diagonal world
     * length across the full component bounds. The bigger this number is,
     * the more zoomed in the view is. Returns 0 if the component is too
     * small to produce a meaningful estimate.
     * </p>
     *
     * @param container the container to measure; must not be {@code null}
     * @return approximate pixels per world unit, or 0 if the component is
     *         too small
     */
    public static double getMeanPixelDensity(IContainer container) {
        Rectangle b = container.getComponent().getBounds();
        if (b == null || b.width < 3 || b.height < 3) {
            return 0.0;
        }
        Point p0 = new Point(0, 0);
        Point p1 = new Point(b.width, b.height);
        Point2D.Double wp0 = new Point2D.Double();
        Point2D.Double wp1 = new Point2D.Double();
        container.localToWorld(p0, wp0);
        container.localToWorld(p1, wp1);
        double worldDist = Math.max(TINY, wp0.distance(wp1));
        double pixDist   = Math.sqrt((double) b.width * b.width
                                   + (double) b.height * b.height);
        return pixDist / worldDist;
    }

    // -----------------------------------------------------------------------
    // Hit testing
    // -----------------------------------------------------------------------

    /**
     * Test whether a world point lies within a given tolerance of any segment
     * in a straight-segment {@link Path2D.Double}.
     * <p>
     * Proximity to path vertices is also tested (a point exactly on a vertex
     * counts as a hit). {@code SEG_CLOSE} is treated as a segment from the
     * last point back to the most recent {@code SEG_MOVETO} point.
     * </p>
     *
     * @param path     the world path to test; {@code null} returns
     *                 {@code false}
     * @param wp       the world point to test; {@code null} returns
     *                 {@code false}
     * @param tolWorld tolerance distance in world units; must be ≥ 0
     * @return {@code true} if {@code wp} is within {@code tolWorld} of any
     *         segment or vertex in {@code path}
     * @throws IllegalArgumentException if the path contains curve segments
     */
    public static boolean isPointNearPath(Path2D.Double path,
            Point2D.Double wp, double tolWorld) {

        if (path == null || wp == null || tolWorld < 0) {
            return false;
        }

        final double tol2 = tolWorld * tolWorld;
        double[] c = new double[6];
        PathIterator pi = path.getPathIterator(null);
        Point2D.Double last = null;
        Point2D.Double subpathStart = null;

        while (!pi.isDone()) {
            switch (pi.currentSegment(c)) {
                case PathIterator.SEG_MOVETO:
                    last = new Point2D.Double(c[0], c[1]);
                    subpathStart = last;
                    if (last.distanceSq(wp) <= tol2) return true;
                    break;

                case PathIterator.SEG_LINETO: {
                    Point2D.Double cur = new Point2D.Double(c[0], c[1]);
                    if (last != null) {
                        if (distanceSqPointToSegment(last, cur, wp) <= tol2)
                            return true;
                    } else {
                        if (cur.distanceSq(wp) <= tol2) return true;
                    }
                    last = cur;
                    break;
                }

                case PathIterator.SEG_CLOSE:
                    if (last != null && subpathStart != null) {
                        if (distanceSqPointToSegment(last, subpathStart, wp)
                                <= tol2) return true;
                        last = subpathStart;
                    }
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Path contains curve segments; "
                            + "isPointNearPath only supports line segments.");
            }
            pi.next();
        }
        return false;
    }

    /**
     * Return the point on segment {@code a→b} that is closest to {@code p}.
     * <p>
     * If the segment is degenerate (zero length) the method returns a copy of
     * {@code a}.
     * </p>
     *
     * @param p1 one end of the segment
     * @param p2 the other end of the segment
     * @param wp the query point
     * @return the closest point on the segment
     */
    public static Point2D.Double closestPointOnSegment(Point2D.Double p1,
            Point2D.Double p2, Point2D.Double wp) {
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        double len2 = dx * dx + dy * dy;
        if (len2 < 1.0e-20) {
            return new Point2D.Double(p1.x, p1.y);
        }
        double t = Math.max(0.0, Math.min(1.0,
                ((wp.x - p1.x) * dx + (wp.y - p1.y) * dy) / len2));
        return new Point2D.Double(p1.x + t * dx, p1.y + t * dy);
    }

    // -----------------------------------------------------------------------
    // Private geometry helpers
    // -----------------------------------------------------------------------

    /**
     * Return the squared distance from point {@code p} to the segment
     * {@code a→b}.
     * <p>
     * Using squared distance avoids a square-root and is sufficient for
     * tolerance comparisons against {@code tol²}.
     * </p>
     *
     * @param a first endpoint of the segment
     * @param b second endpoint of the segment
     * @param p the query point
     * @return squared distance from {@code p} to the nearest point on
     *         segment {@code a→b}
     */
    private static double distanceSqPointToSegment(Point2D.Double a,
            Point2D.Double b, Point2D.Double p) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double len2 = dx * dx + dy * dy;
        if (len2 < 1.0e-20) {
            return a.distanceSq(p);
        }
        double t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / len2;
        if (t <= 0.0) return a.distanceSq(p);
        if (t >= 1.0) return b.distanceSq(p);
        double cx = a.x + t * dx;
        double cy = a.y + t * dy;
        return (p.x - cx) * (p.x - cx) + (p.y - cy) * (p.y - cy);
    }
}