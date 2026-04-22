package edu.cnu.mdi.mapping.item;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.SymbolDraw;
import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.item.ItemModification;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.container.MapContainer;

/**
 * A map-native point item whose location is stored as a geographic coordinate
 * (longitude/latitude in radians) rather than a world-space point.
 *
 * <h2>Use cases</h2>
 * <p>
 * Cities, airports, GPS waypoints, observation stations, or any location marker
 * that should remain at the same geographic position when the projection changes.
 * </p>
 *
 * <h2>Rendering</h2>
 * <p>
 * On each draw, the geographic location is projected through the active
 * {@link edu.cnu.mdi.mapping.projection.IMapProjection} to device pixels.
 * If the point is on the far side of the globe (non-finite projection result,
 * e.g. orthographic back-hemisphere), the item is not drawn.
 * </p>
 * <p>
 * The item can display either an {@link ImageIcon} or a symbol from
 * {@link edu.cnu.mdi.graphics.style.SymbolType}, controlled by the style.
 * Alignment for icon drawing follows {@link SwingUtilities} constants
 * ({@code LEFT/CENTER/RIGHT} horizontally, {@code TOP/CENTER/BOTTOM} vertically).
 * </p>
 *
 * <h2>Interaction</h2>
 * <ul>
 *   <li><b>Drag</b> — moves the geographic location by the screen-space delta
 *       converted through the map projection. Unlike the raw lon/lat delta used
 *       by multi-point items, this conversion is exact at all latitudes because
 *       both the start and current pixel positions are independently inverse-projected
 *       to geographic coordinates.</li>
 *   <li><b>Resize / Rotate</b> — not applicable to a point; silently ignored.</li>
 * </ul>
 *
 * <h2>Focus</h2>
 * <p>
 * The {@link #_focus} field holds the geographic location directly
 * ({@code x = longitude, y = latitude} in radians). {@link AMapItem#getFocusPoint}
 * handles the projection to device pixels.
 * </p>
 */
public class MapPointItem extends AMapItem {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /**
     * Optional icon to display instead of a symbol. When non-null, icon drawing
     * takes precedence over the style's {@link SymbolType}.
     */
    protected ImageIcon icon;

    /** Horizontal icon alignment: {@link SwingUtilities#LEFT/CENTER/RIGHT}. */
    private int _xAlignment = SwingUtilities.CENTER;

    /** Vertical icon alignment: {@link SwingUtilities#TOP/CENTER/BOTTOM}. */
    private int _yAlignment = SwingUtilities.CENTER;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates an unplaced map point item. The focus is initialised to
     * ({@link Double#NaN}, {@link Double#NaN}); call {@link #setFocus} before
     * the item is drawn.
     *
     * @param layer the layer; must not be {@code null}
     */
    public MapPointItem(Layer layer) {
        super(layer);
        _focus = new Point2D.Double(Double.NaN, Double.NaN);
        setDisplayName("Point");
    }

    /**
     * Creates a map point item at the given geographic location.
     *
     * @param layer    the layer; must not be {@code null}
     * @param location geographic location in radians ({@code x = longitude,
     *                 y = latitude})
     */
    public MapPointItem(Layer layer, Point2D.Double location) {
        this(layer, location, (Object[]) null);
    }

    /**
     * Creates a map point item at the given geographic location, with optional
     * property key/value pairs.
     *
     * @param layer    the layer; must not be {@code null}
     * @param location geographic location in radians
     * @param keyVals  optional {@link edu.cnu.mdi.util.PropertyUtils} key/value pairs
     */
    public MapPointItem(Layer layer, Point2D.Double location, Object... keyVals) {
        super(layer, keyVals);
        _focus = (location == null)
                ? new Point2D.Double(Double.NaN, Double.NaN)
                : new Point2D.Double(location.x, location.y);
        setDisplayName("Point");
    }

    /**
     * Creates a map point item that displays an icon.
     *
     * @param layer    the layer; must not be {@code null}
     * @param location geographic location in radians
     * @param icon     the icon to display; if {@code null} the style symbol is used
     */
    public MapPointItem(Layer layer, Point2D.Double location, ImageIcon icon) {
        this(layer, location);
        this.icon = icon;
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    /**
     * Draws the point at its projected screen location.
     *
     * <p>The item is not drawn if the focus is unplaced (NaN) or projects to
     * a non-finite coordinate. Icon drawing takes priority over symbol drawing.</p>
     *
     * @param g2        the graphics context
     * @param container the rendering container; must be a {@link MapContainer}
     */
    @Override
    public void drawItem(Graphics2D g2, IContainer container) {
        Point p = getFocusPoint(container);
        if (p == null) return;     // unplaced or off-map

        if (icon != null) {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            int x = alignedX(p.x, w, _xAlignment);
            int y = alignedY(p.y, h, _yAlignment);
            g2.drawImage(icon.getImage(), x, y, container.getComponent());
        } else {
            if (_style.getSymbolType() != SymbolType.NOSYMBOL) {
                Rectangle r = getBounds(container);
                if (r != null) {
                    SymbolDraw.drawSymbol(g2, r.x + r.width / 2, r.y + r.height / 2, _style);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code false} if the focus is unplaced or the projected bounds
     * do not intersect the visible component area.</p>
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
     * Returns the device-space bounding rectangle for this point item.
     *
     * <p>For icon items: the rectangle is positioned according to the
     * alignment settings. For symbol items: a square of size
     * {@code style.getSymbolSize()} centred on the focus point.</p>
     *
     * <p>Returns {@code null} if the focus is unplaced or off-map.</p>
     *
     * @param container the rendering container
     * @return bounding rectangle in device coordinates, or {@code null}
     */
    @Override
    public Rectangle getBounds(IContainer container) {
        Point p = getFocusPoint(container);
        if (p == null) return null;

        if (icon != null) {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            return new Rectangle(alignedX(p.x, w, _xAlignment),
                                 alignedY(p.y, h, _yAlignment), w, h);
        }

        int sz  = _style.getSymbolSize();
        int sz2 = sz / 2;
        return new Rectangle(p.x - sz2, p.y - sz2, sz, sz);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a {@code null} world bounding rectangle — a geographic point
     * has no extent in world space.</p>
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
     * <p>Records the geographic start location so drag can compute an absolute
     * delta from the snapshot rather than accumulating incremental moves.</p>
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
     * <p>DRAG: converts the start and current mouse positions independently to
     * geographic coordinates and moves the focus by the difference. This gives
     * an exact result at all latitudes — no raw lon/lat approximation.</p>
     *
     * <p>RESIZE / ROTATE: silently ignored for a point item.</p>
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

        // Delta in geographic space
        double dLon = currentMouseLL.x - startMouseLL.x;
        double dLat = currentMouseLL.y - startMouseLL.y;

        _focus = new Point2D.Double(startFocusLL.x + dLon, startFocusLL.y + dLat);
        setDirty(true);
        mc.refresh();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stores the geographic location directly, with NaN used to represent
     * an unplaced item.</p>
     */
    @Override
    public void setFocus(Point2D.Double location) {
        if (_focus == null) _focus = new Point2D.Double(Double.NaN, Double.NaN);
        if (location == null) {
            _focus.x = Double.NaN;
            _focus.y = Double.NaN;
        } else {
            _focus.x = location.x;
            _focus.y = location.y;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds the lon/lat offset to the stored geographic location. A no-op
     * if the focus is unplaced (NaN) or the delta is negligible.</p>
     */
    @Override
    public void translateWorld(double dx, double dy) {
        if (_focus == null) _focus = new Point2D.Double(Double.NaN, Double.NaN);
        if (Double.isNaN(_focus.x) || Double.isNaN(_focus.y)) return;
        if (Math.abs(dx) < 1.0e-12 && Math.abs(dy) < 1.0e-12) return;
        _focus.x += dx;
        _focus.y += dy;
        setDirty(true);
    }

    // -------------------------------------------------------------------------
    // Feedback
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Reports the geographic coordinates of this point when the cursor is
     * near it.</p>
     */
    @Override
    public void getFeedbackStrings(IContainer container, Point pp,
            Point2D.Double wp, List<String> feedbackStrings) {
        if (feedbackStrings == null || !contains(container, pp)) return;
        if (_focus == null || Double.isNaN(_focus.x)) return;
        feedbackStrings.add("$yellow$" + getDisplayName());
        feedbackStrings.add(String.format("lon %.4f°, lat %.4f°",
                Math.toDegrees(_focus.x), Math.toDegrees(_focus.y)));
    }

    // -------------------------------------------------------------------------
    // Alignment accessors
    // -------------------------------------------------------------------------

    /**
     * @return horizontal alignment for icon drawing
     *         ({@link SwingUtilities#LEFT}, {@link SwingUtilities#CENTER},
     *         or {@link SwingUtilities#RIGHT})
     */
    public int getAlignmentH() { return _xAlignment; }

    /**
     * @param xAlignment horizontal alignment constant from {@link SwingUtilities}
     */
    public void setAlignmentH(int xAlignment) { _xAlignment = xAlignment; }

    /**
     * @return vertical alignment for icon drawing
     *         ({@link SwingUtilities#TOP}, {@link SwingUtilities#CENTER},
     *         or {@link SwingUtilities#BOTTOM})
     */
    public int getAlignmentV() { return _yAlignment; }

    /**
     * @param yAlignment vertical alignment constant from {@link SwingUtilities}
     */
    public void setAlignmentV(int yAlignment) { _yAlignment = yAlignment; }

    // -------------------------------------------------------------------------
    // Private alignment helpers
    // -------------------------------------------------------------------------

    private static int alignedX(int cx, int w, int alignment) {
        return switch (alignment) {
            case SwingUtilities.LEFT  -> cx;
            case SwingUtilities.RIGHT -> cx - w;
            default                  -> cx - w / 2;   // CENTER
        };
    }

    private static int alignedY(int cy, int h, int alignment) {
        return switch (alignment) {
            case SwingUtilities.TOP    -> cy;
            case SwingUtilities.BOTTOM -> cy - h;
            default                    -> cy - h / 2; // CENTER
        };
    }
}