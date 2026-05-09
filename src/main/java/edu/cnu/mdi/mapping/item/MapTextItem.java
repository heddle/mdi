package edu.cnu.mdi.mapping.item;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.SwingConstants;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.dialog.TextEditDialog;
import edu.cnu.mdi.item.ITextEditable;
import edu.cnu.mdi.item.ItemModification;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.item.ResizePolicy;
import edu.cnu.mdi.mapping.container.MapContainer;
import edu.cnu.mdi.swing.WindowPlacement;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.TextUtils;
import edu.cnu.mdi.util.UnicodeUtils;

/**
 * A map-native text label whose anchor is stored as a geographic coordinate
 * (longitude/latitude in radians).
 *
 * <h2>Distinction from {@code TextItem}</h2>
 * <p>
 * {@code TextItem} anchors its bounding box to a world-space (projection-space)
 * point. When the map projection changes or the view recenters, the text box
 * drifts because the anchor is in a coordinate system that moves relative to the
 * Earth's surface. {@code MapTextItem} stores the anchor as a geographic point;
 * the anchor is re-projected on every draw so the label stays above the same
 * geographic feature regardless of the active projection.
 * </p>
 *
 * <h2>Rendering model</h2>
 * <p>
 * The text box is always axis-aligned in screen space. Only the anchor moves
 * with the projection; the box itself is a screen-space rectangle computed from
 * font metrics on each draw, exactly as in {@code TextItem}. This means rotation
 * of the text box is not supported (and is meaningless for a geographic label).
 * </p>
 *
 * <h2>Drag</h2>
 * <p>
 * Drag converts both the start and current mouse positions independently to
 * geographic coordinates and moves the anchor by the difference. This is exact
 * at all latitudes (no raw lon/lat approximation).
 * </p>
 *
 * <h2>Resize</h2>
 * <p>
 * Not applicable — the box size is determined by the font and text content.
 * {@link ResizePolicy#SCALEONLY} is set so that shift+drag scales rather than
 * attempting a shape-preserving reshape.
 * </p>
 *
 * <h2>Rotation</h2>
 * <p>
 * Not supported. Geographic labels are conventionally upright; rotating a text
 * label relative to the map surface has no standard geographic meaning.
 * </p>
 *
 * <h2>Focus</h2>
 * <p>
 * {@link #_focus} holds the geographic anchor ({@code x = longitude, y = latitude}
 * in radians). {@link AMapItem#getFocusPoint} projects it to device pixels for
 * the selection indicator. The bounding box is then built screen-space around
 * that pixel anchor, matching {@code TextItem}'s screen-space round-trip.
 * </p>
 */
public class MapTextItem extends AMapItem implements ITextEditable {

    private static final int   MARGIN   = 4;
    private static final float LINESIZE = 1.0f;

    private static final Font _defaultFont = Fonts.plainFontDelta(0);

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private Font     _font      = _defaultFont;
    private String[] _lines;
    private int      _alignment = SwingConstants.LEFT;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a map-native text label.
     *
     * @param layer     the layer; must not be {@code null}
     * @param location  geographic anchor in radians ({@code x = longitude,
     *                  y = latitude})
     * @param font      font to use; if {@code null} the default font is used
     * @param text      text to display; must not be {@code null} or empty
     * @param lineColor border color; {@code null} for no border
     * @param fillColor background color; {@code null} for transparent background
     * @param textColor text foreground color; {@code null} defaults to black
     */
    public MapTextItem(Layer layer, Point2D.Double location,
                       Font font, String text,
                       Color lineColor, Color fillColor, Color textColor) {
        super(layer);
        setFont(font);
        setText(text);
        _style.setFillColor(fillColor);
        _style.setLineColor(lineColor);
        _style.setTextColor(textColor != null ? textColor : Color.BLACK);
        _focus = (location == null)
                ? new Point2D.Double(Double.NaN, Double.NaN)
                : new Point2D.Double(location.x, location.y);
        _resizePolicy = ResizePolicy.SCALEONLY;
        setDisplayName("Text");
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    /**
     * Projects the geographic anchor to device pixels and draws the text box
     * (background fill, border, and text) in screen space around that pixel.
     *
     * <p>Returns immediately if the anchor is off-map (non-finite projection).
     * No path-based geometry is maintained; the bounding box is rebuilt from
     * font metrics on every draw.</p>
     *
     * @param g2        the graphics context
     * @param container the rendering container; must be a {@link MapContainer}
     */
    @Override
    public void drawItem(Graphics2D g2, IContainer container) {
        Point cp = getFocusPoint(container);
        if (cp == null) return;     // unplaced or off-map

        FontMetrics fm = container.getComponent().getFontMetrics(_font);
        Rectangle r = TextUtils.textBounds(getText(), fm,
                MARGIN, MARGIN, MARGIN, MARGIN, LINESIZE);
        int hw = r.width  / 2;
        int hh = r.height / 2;

        // Draw background fill
        if (_style.getFillColor() != null) {
            g2.setColor(_style.getFillColor());
            g2.fillRect(cp.x - hw, cp.y - hh, r.width, r.height);
        }

        // Draw border
        if (_style.getLineColor() != null) {
            g2.setColor(_style.getLineColor());
            g2.drawRect(cp.x - hw, cp.y - hh, r.width, r.height);
        }

        // Draw text (no rotation — geographic labels are always upright)
        TextUtils.drawRotatedText(g2, cp, getText(), _font,
                _style.getTextColor(), 0.0, _alignment);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code false} if the anchor is unplaced or the bounding
     * rectangle does not intersect the visible component area.</p>
     */
    @Override
    public boolean shouldDraw(Graphics2D g2, IContainer container) {
        Rectangle r = getBounds(container);
        if (r == null) return false;
        Rectangle b = container.getComponent().getBounds();
        b.x = 0; b.y = 0;
        return b.intersects(r);
    }

    // -------------------------------------------------------------------------
    // Bounds and hit-testing
    // -------------------------------------------------------------------------

    /**
     * Returns the screen-space bounding rectangle of the text box.
     *
     * <p>Computed from font metrics centred on the projected anchor. Returns
     * {@code null} if the anchor is off-map.</p>
     *
     * @param container the rendering container
     * @return bounding rectangle in device coordinates, or {@code null}
     */
    @Override
    public Rectangle getBounds(IContainer container) {
        Point cp = getFocusPoint(container);
        if (cp == null) return null;
        FontMetrics fm = container.getComponent().getFontMetrics(_font);
        Rectangle r = TextUtils.textBounds(getText(), fm,
                MARGIN, MARGIN, MARGIN, MARGIN, LINESIZE);
        return new Rectangle(cp.x - r.width / 2, cp.y - r.height / 2,
                             r.width, r.height);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code null} — a geographic label has no world-space extent.</p>
     */
    @Override
    public Rectangle2D.Double getWorldBounds() {
        return null;
    }

    // -------------------------------------------------------------------------
    // Modification lifecycle
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Records the geographic start anchor and its screen projection for
     * subsequent drag computation.</p>
     */
    @Override
    public void startModification() {
        _modification.setStartFocus(
                (_focus == null) ? null : new Point2D.Double(_focus.x, _focus.y));
        _modification.setStartFocusPoint(getFocusPoint(_modification.getContainer()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>DRAG: independently inverse-projects the start and current mouse
     * positions to geographic coordinates and shifts the anchor by the
     * difference. This is exact at all latitudes.
     * RESIZE / ROTATE: silently ignored.</p>
     */
    @Override
    public void modify() {
        if (_modification == null) return;
        if (_modification.getType() != ItemModification.ModificationType.DRAG) return;
        if (!(_modification.getContainer() instanceof MapContainer mc)) return;

        Point startFocusPt = _modification.getStartFocusPoint();
        if (startFocusPt == null) return;

        Point2D.Double startFocusLL = new Point2D.Double();
        mc.localToLatLon(startFocusPt, startFocusLL);

        Point2D.Double startMouseLL = new Point2D.Double();
        mc.localToLatLon(_modification.getStartMousePoint(), startMouseLL);

        Point2D.Double currentMouseLL = new Point2D.Double();
        mc.localToLatLon(_modification.getCurrentMousePoint(), currentMouseLL);

        _focus = new Point2D.Double(
                startFocusLL.x + (currentMouseLL.x - startMouseLL.x),
                startFocusLL.y + (currentMouseLL.y - startMouseLL.y));
        mc.refresh();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Moves the geographic anchor by raw lon/lat offsets. See
     * {@link MapLineItem} for the high-latitude caveat. A no-op if the anchor
     * is unplaced.</p>
     */
    @Override
    public void translateWorld(double dx, double dy) {
        if (_focus == null || Double.isNaN(_focus.x)) return;
        if (Math.abs(dx) < 1.0e-12 && Math.abs(dy) < 1.0e-12) return;
        _focus.x += dx;
        _focus.y += dy;
        setDirty(true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stores the geographic location directly.
     * NaN coordinates represent an unplaced label.</p>
     */
    @Override
    public void setFocus(Point2D.Double location) {
        if (_focus == null) _focus = new Point2D.Double(Double.NaN, Double.NaN);
        if (location == null) {
            _focus.x = Double.NaN; _focus.y = Double.NaN;
        } else {
            _focus.x = location.x; _focus.y = location.y;
        }
    }

    // -------------------------------------------------------------------------
    // Edit dialog
    // -------------------------------------------------------------------------

    /**
     * Opens the text-edit dialog and applies the result.
     *
     * <p>Rebuilding the path (as {@code TextItem} does for rotation) is
     * unnecessary here because {@code MapTextItem} has no stored path.</p>
     */
    public void edit() {
        TextEditDialog dialog = new TextEditDialog(this);
        WindowPlacement.centerComponent(dialog);
        dialog.setVisible(true);
        if (dialog.isCancelled()) return;
        dialog.updateTextItem(this);
        IContainer container = getContainer();
        if (container != null) container.refresh();
    }

    // -------------------------------------------------------------------------
    // Feedback
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Reports the text content and geographic anchor when the cursor is
     * over the label.</p>
     */
    @Override
    public void getFeedbackStrings(IContainer container, Point pp,
            Point2D.Double wp, List<String> feedbackStrings) {
        if (feedbackStrings == null || !contains(container, pp)) return;
        if (_focus == null || Double.isNaN(_focus.x)) return;
        feedbackStrings.add("$yellow$" + getDisplayName() + ": \"" + getText() + "\"");
        feedbackStrings.add(String.format("anchor: lon %.4f°, lat %.4f°",
                Math.toDegrees(_focus.x), Math.toDegrees(_focus.y)));
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** @return the current text alignment */
    public int getAlignment()           { return _alignment; }
    /** @param align text alignment constant from {@link SwingConstants} */
    public void setAlignment(int align) { _alignment = align; }

    /** @return the current font */
    public Font getFont()               { return _font; }

    /**
     * Sets the display font.
     *
     * @param font the font to use; if {@code null} the default font is restored
     */
    public void setFont(Font font)      { _font = (font != null) ? font : _defaultFont; }

    /**
     * Returns the full text content as a single string with embedded newlines.
     *
     * @return the text; never {@code null}
     */
    public String getText() {
        return (_lines == null || _lines.length == 0) ? "" : String.join("\n", _lines);
    }

    /**
     * Sets the text content, replacing special characters via
     * {@link UnicodeUtils#specialCharReplace}.
     *
     * @param text the new text; {@code null} is treated as an empty string
     */
    public void setText(String text) {
        if (text == null) text = "";
        text = UnicodeUtils.specialCharReplace(text);
        _lines = text.lines().toArray(String[]::new);
    }
}