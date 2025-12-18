package edu.cnu.mdi.item;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.graphics.text.Snippet;
import edu.cnu.mdi.graphics.text.UnicodeSupport;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * A drawable item that renders text with a rectangular background/border.
 * <p>
 * Key invariants:
 * <ul>
 *   <li>{@link #_focus} is the <b>center</b> of the text box.</li>
 *   <li>When the item is being modified (drag/resize/rotate), we do <b>not</b>
 *       rebuild the path from font metrics. We rely on the affine transforms in
 *       {@link PathBasedItem#modify()} so the rectangle and handles stay
 *       lockstep with the mouse.</li>
 *   <li>The text baseline point is tracked as a {@link #_secondaryPoints}
 *       entry so it transforms together with the rectangle.</li>
 *   <li>When the modification ends, we “snap” back to a clean metrics-derived
 *       rectangle for stable serialization and consistent redraw.</li>
 * </ul>
 * </p>
 */
public class TextItem extends RectangleItem {

    /** Pixel margin around text bounds. */
    private static final int MARGIN = 4;

    /** Twice the margin. */
    private static final int MARGIN2 = 2 * MARGIN;

    /** Default font. */
    private static final Font DEFAULT_FONT = Fonts.commonFont(Font.PLAIN, 12);

    /** Font used to render the text. */
    private Font _font = DEFAULT_FONT;

    /** The text to render. */
    private String _text;

    /** Text foreground color. */
    private Color _textColor = Color.black;


    /** Font size at the moment a modification begins (used for smooth scaling). */
    private int _startFontSize = -1;

    /**
     * Create a text item.
     *
     * @param layer  the z layer which will hold the item
     * @param location  world location of the text baseline start (left/baseline) used to seed initial focus
     * @param font      font to use (null -> default)
     * @param text      text to display
     * @param textColor text foreground (null -> black)
     * @param fillColor background fill (may be null)
     * @param lineColor border color (may be null)
     */
    public TextItem(Layer layer,
                    Point2D.Double location,
                    Font font,
                    String text,
                    Color textColor,
                    Color fillColor,
                    Color lineColor) {

        // Temporary rectangle; real geometry is computed from metrics + focus.
        super(layer, new Rectangle2D.Double(location.x, location.y, 1, 1));

        setFont(font);
        setText(text);

        _textColor = (textColor != null) ? textColor : Color.black;
        _style.setFillColor(fillColor);
        _style.setLineColor(lineColor);

        // Seed focus at the baseline point; first draw will recenter correctly.
        _focus = new Point2D.Double(location.x, location.y);

        // Text boxes should scale uniformly.
        _resizePolicy = ResizePolicy.SCALEONLY;

        setResizable(true);
        setRotatable(true);
        setDraggable(true);

        // Track baseline as a secondary point so it transforms with the path.
        _secondaryPoints = new Point2D.Double[] { new Point2D.Double(location.x, location.y) };
    }

    /**
     * Called only when a modification starts; we capture the starting font size
     * so scaling uses the same reference throughout the drag.
     *
     * @param itemModification the modification record
     */
    @Override
    public void setModificationItem(ItemModification itemModification) {
        super.setModificationItem(itemModification);
        _startFontSize = (_font != null) ? _font.getSize() : -1;

        // Ensure we have a clean geometry before transforms begin.
        if (_path == null && itemModification != null) {
            rebuildGeometry(itemModification.getContainer());
        }
    }

    /**
     * Draw rectangle then draw text.
     */
    @Override
    public void drawItem(Graphics2D g, IContainer container) {


        // IMPORTANT:
        // If we are actively modifying, do NOT rebuild from metrics (causes “faster than mouse”).
        // Let PathBasedItem.modify() keep _path and _secondaryPoints in sync with the mouse.
        if (_modification == null) {
            rebuildGeometry(container);
        }

        // Draw the rectangle via PathBasedItem rendering.
        super.drawItem(g, container);

        if (_text == null || container == null) {
            return;
        }

        // Baseline is stored in secondary point #0 (kept in sync during transforms).
        Point2D.Double baselineWorld = (_secondaryPoints != null && _secondaryPoints.length > 0)
                ? _secondaryPoints[0]
                : _focus;

        Point pBase = new Point();
        container.worldToLocal(pBase, baselineWorld);

        g.setColor((_textColor != null) ? _textColor : Color.black);

        // Delegate rotation direction convention to your existing snippet renderer.
        GraphicsUtils.drawSnippets(g, pBase.x, pBase.y, _font, _text, container.getComponent(), getAzimuth());
    }

    /**
     * Selection points are the 4 rectangle corners, computed from the current path.
     */
    @Override
    public Point[] getSelectionPoints(IContainer container) {
        if (container == null) {
            return null;
        }
        if (_path == null && _modification == null) {
            rebuildGeometry(container);
        }
        if (_path == null) {
            return null;
        }

        Polygon poly = WorldGraphicsUtils.pathToPolygon(container, _path);
        if (poly == null || poly.npoints < 4) {
            return null;
        }

        Point[] pts = new Point[4];
        for (int i = 0; i < 4; i++) {
            pts[i] = new Point(poly.xpoints[i], poly.ypoints[i]);
        }
        return pts;
    }

    /**
     * Use {@link PathBasedItem}'s rotate handle logic (outward from focus along azimuth).
     */
    @Override
    public Point getRotatePoint(IContainer container) {
        if (_path == null && container != null && _modification == null) {
            rebuildGeometry(container);
        }
        return super.getRotatePoint(container);
    }

    /**
     * Scale-only resizing:
     * <ul>
     *   <li>Let {@link PathBasedItem#scale()} do the geometric affine scale of the path and secondary point.</li>
     *   <li>Adjust the font size smoothly from the stored start font size using the same scale factor.</li>
     * </ul>
     * This keeps the rectangle/handles locked to the mouse (path drives geometry),
     * while the text size tracks the drag without rebuilding the box mid-drag.
     */
    @Override
    protected void scale() {
        if (_modification == null) {
            return;
        }

        // Compute scale factor exactly as PathBasedItem does (relative to focus).
        if (_focus == null) {
            updateFocus();
            if (_focus == null) {
                return;
            }
        }

        Point2D.Double startPoint = _modification.getStartWorldPoint();
        Point2D.Double currentPoint = _modification.getCurrentWorldPoint();

        double denom = startPoint.distance(_focus);
        if (denom < 1.0e-12) {
            return;
        }

        double s = currentPoint.distance(_focus) / denom;

        // 1) Geometrically scale the path and secondary point lockstep with mouse.
        super.scale(); // uses _modification startPath + secondaryPath -> keeps handles “attached”

        // 2) Update font size proportionally (but do NOT rebuild geometry mid-drag).
        if (_startFontSize <= 0) {
            _startFontSize = (_font != null) ? _font.getSize() : 12;
        }

        int newSize = (int) Math.round(_startFontSize * s);
        newSize = Math.max(2, Math.min(newSize, 256));
        _font = new Font(_font.getFontName(), _font.getStyle(), newSize);
    }

    /**
     * When a modification ends, snap geometry back to a clean rectangle derived from metrics.
     * This prevents tiny transform noise from accumulating across many edits.
     */
    @Override
    public void stopModification() {
        super.stopModification();
        _startFontSize = -1;

        if (getContainer() != null) {
            rebuildGeometry(getContainer());
        }
    }

    /**
     * Rebuild rectangle path and baseline point from current focus/font/text/azimuth.
     * <p>
     * This is called only when NOT actively modifying, so it cannot cause
     * “mouse drift” during a drag.
     * </p>
     *
     * @param container the rendering container
     */
    private void rebuildGeometry(IContainer container) {
        if (container == null || container.getComponent() == null || _focus == null || _text == null) {
            return;
        }

        Component comp = container.getComponent();
        FontMetrics fm = comp.getFontMetrics(_font);

        Dimension size = sizeText(comp, _font, _text);
        if (size == null) {
            return;
        }

        // Center in pixels from world focus.
        Point pCenter = new Point();
        container.worldToLocal(pCenter, _focus);

        // Unrotated rectangle in pixels centered at focus.
        int rectX = pCenter.x - size.width / 2;
        int rectY = pCenter.y - size.height / 2;

        Rectangle rectPix = new Rectangle(rectX, rectY, size.width, size.height);

        // Baseline (left/baseline) in pixels for the *unrotated* rectangle.
        int baseX = rectX + MARGIN;
        int baseY = rectY + MARGIN + fm.getAscent();

        // Convert rectangle corners to world (unrotated).
        Point2D.Double[] corners = new Point2D.Double[4];
        for (int i = 0; i < 4; i++) {
            corners[i] = new Point2D.Double();
        }

        Point p0 = new Point(rectPix.x, rectPix.y);
        Point p1 = new Point(rectPix.x, rectPix.y + rectPix.height);
        Point p2 = new Point(rectPix.x + rectPix.width, rectPix.y + rectPix.height);
        Point p3 = new Point(rectPix.x + rectPix.width, rectPix.y);

        container.localToWorld(p0, corners[0]);
        container.localToWorld(p1, corners[1]);
        container.localToWorld(p2, corners[2]);
        container.localToWorld(p3, corners[3]);

        // Baseline to world (unrotated).
        Point pBase = new Point(baseX, baseY);
        if (_secondaryPoints == null || _secondaryPoints.length < 1) {
            _secondaryPoints = new Point2D.Double[] { new Point2D.Double() };
        }
        container.localToWorld(pBase, _secondaryPoints[0]);

        // Rotate corners and baseline about focus to match your framework azimuth convention.
        double az = getAzimuth();
        if (Math.abs(az) > 1.0e-6) {
            AffineTransform at = AffineTransform.getRotateInstance(Math.toRadians(-az), _focus.x, _focus.y);
            for (Point2D.Double wp : corners) {
                at.transform(wp, wp);
            }
            at.transform(_secondaryPoints[0], _secondaryPoints[0]);
        }

        _path = WorldGraphicsUtils.worldPolygonToPath(corners);

        // Ensure focus stays exactly centered.
        updateFocus();
    }

    /**
     * Computes the pixel size of the rendered text including margin.
     *
     * @param component the component used for font metrics/snippet layout
     * @param font      the font
     * @param text      the text
     * @return the pixel size including margins, or null
     */
    public static Dimension sizeText(Component component, Font font, String text) {
        if (text == null || component == null || font == null) {
            return null;
        }

        int width = 0;
        int height = 0;
        ArrayList<Snippet> snippets = Snippet.getSnippets(font, text, component);
        for (Snippet s : snippets) {
            Dimension sz = s.size(component);
            width = Math.max(width, s.getDeltaX() + sz.width);
            height = Math.max(height, s.getDeltaY() + sz.height);
        }

        return new Dimension(width + MARGIN2, height + MARGIN2);
    }

    /** @return the font used for rendering (never null) */
    public Font getFont() {
        return _font;
    }

    /**
     * Set the rendering font.
     *
     * @param font the font (null -> default)
     */
    public void setFont(Font font) {
        _font = (font != null) ? font : DEFAULT_FONT;
    }

    /** @return the text being rendered */
    public String getText() {
        return _text;
    }

    /**
     * Set the text being rendered.
     *
     * @param text the text (may be null)
     */
    public void setText(String text) {
        _text = (text != null) ? UnicodeSupport.specialCharReplace(text) : null;
    }

    /** @return the text foreground color */
    public Color getTextColor() {
        return _textColor;
    }

    /**
     * Set the text foreground color.
     *
     * @param textForeground the new text foreground color
     */
    public void setTextColor(Color textForeground) {
        _textColor = textForeground;
    }

    /**
     * Update focus to be the rectangle center (centroid of the box path).
     */
    @Override
    protected void updateFocus() {
        if (_path != null) {
            _focus = WorldGraphicsUtils.getCentroid(_path);
        }
    }
}
