package edu.cnu.mdi.item;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.IFeedbackProvider;
import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.graphics.drawable.IDrawable;
import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.Styled;
import edu.cnu.mdi.item.ItemModification.ModificationType;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.BaseView;

/**
 * Abstract base class for all interactive graphical items rendered on an
 * {@link IContainer}.
 *
 * <h2>Role in the MDI item model</h2>
 * <p>
 * An {@code AItem} is the fundamental unit of interactive content in MDI. It
 * lives on a {@link Layer}, which in turn belongs to an {@link IContainer}.
 * Items are drawn by the container's rendering pipeline, hit-tested by the
 * interaction layer, and can be dragged, resized, rotated, selected, or
 * deleted according to a set of independently-controllable behavior flags.
 * </p>
 *
 * <h2>Drawing pipeline</h2>
 * <p>
 * The public entry point for drawing is {@link #draw(Graphics2D, IContainer)},
 * which is called by the container. Subclasses must <em>not</em> override
 * {@code draw}; instead they implement the two abstract methods:
 * </p>
 * <ul>
 *   <li>{@link #drawItem(Graphics2D, IContainer)} — performs the actual
 *       rendering of the item's geometry.</li>
 *   <li>{@link #shouldDraw(Graphics2D, IContainer)} — returns {@code true} if
 *       the item should be drawn at all (e.g. it intersects the visible
 *       area).</li>
 * </ul>
 *
 * <h2>Behavior flags and the lock</h2>
 * <p>
 * Each interactive behavior (dragging, resizing, rotating, deleting) is
 * individually controlled by a boolean flag. However, the {@link #_locked}
 * flag is a master override: when {@code true} it suppresses dragging,
 * resizing, rotating, and deleting <em>regardless</em> of the individual flags.
 * Locking does not suppress visibility or selection display.
 * </p>
 * <p>
 * <strong>Items are locked by default ({@code _locked = true}).</strong>
 * Call {@link #setLocked(boolean) setLocked(false)} or supply
 * {@link PropertyUtils#LOCKED LOCKED=false} in the constructor key-value pairs
 * to make an item interactive.
 * </p>
 *
 * <h2>Property-based construction</h2>
 * <p>
 * The constructor accepts an optional varargs list of alternating
 * {@link PropertyUtils} key/value pairs. These are parsed and applied by
 * {@link #applyProperties(Properties)}, which covers behavior flags, style
 * attributes, and display name. Subclasses that need geometry parameters
 * should override {@link #applyGeometryProperties(Properties)}.
 * </p>
 *
 * <h2>Geometry</h2>
 * <p>
 * {@code AItem} provides three geometry fields that subclasses may use:
 * </p>
 * <ul>
 *   <li>{@link #_path} — a world-coordinate {@link Path2D.Double} for
 *       polygon-like items.</li>
 *   <li>{@link #_line} — a world-coordinate {@link Line2D.Double} for line
 *       items.</li>
 *   <li>{@link #_focus} — a world-coordinate {@link Point2D.Double} that
 *       represents the item's conceptual center.</li>
 * </ul>
 *
 * <h2>Modification lifecycle</h2>
 * <ol>
 *   <li>{@link #setModification(ItemModification)} — attaches the context.</li>
 *   <li>{@link #startModification()} — classifies the operation.</li>
 *   <li>{@link #modify()} — called repeatedly during the gesture.</li>
 *   <li>{@link #stopModification()} — finalises, notifies, clears.</li>
 * </ol>
 */
public abstract class AItem implements IDrawable, IFeedbackProvider {

    // -----------------------------------------------------------------------
    // Shared visual constants
    // -----------------------------------------------------------------------

    /** Semi-transparent grey fill for the focus-point indicator. */
    protected static final Color _FOCUSFILL = new Color(128, 128, 128, 128);

    /** Pixel size of each selection-handle hit rectangle. */
    protected static final int SPSIZE  = 10;
    /** Half of {@link #SPSIZE}. */
    protected static final int SPSIZE2 = SPSIZE / 2;

    /** Pixel size of the rotation-handle hit rectangle. */
    protected static final int RPSIZE  = 14;
    /** Half of {@link #RPSIZE}. */
    protected static final int RPSIZE2 = RPSIZE / 2;

    /** Icon rendered inside the rotation handle when the item is selected. */
    protected static Icon rotateIcon =
            ImageManager.getInstance().loadUiIcon(
                    Environment.MDI_RESOURCE_PATH + "images/svg/rotate.svg", 16);

    private static final Color _selectFill = Color.white;
    private static final Color _selectLine = Color.black;
    private static final Color _rotateFill = X11Colors.getX11Color("yellow", 64);

    // -----------------------------------------------------------------------
    // Geometry fields (protected — subclass use)
    // -----------------------------------------------------------------------

    /** World-coordinate path for polygon-like items; may be {@code null}. */
    protected Path2D.Double _path;

    /** World-coordinate line for line items; may be {@code null}. */
    protected Line2D.Double _line;

    /** Optional secondary world-coordinate control points; may be {@code null}. */
    protected Point2D.Double[] _secondaryPoints;

    /**
     * World-coordinate focus — typically the centroid or anchor.
     * Drawn as a small square when the item is selected.
     */
    protected Point2D.Double _focus;

    // -----------------------------------------------------------------------
    // Layer and style
    // -----------------------------------------------------------------------

    /**
     * The layer this item lives on. Set at construction; nulled by
     * {@link #prepareForRemoval()}.
     */
    protected Layer _layer;

    /**
     * The drawing style (colors, line style, symbol type, etc.).
     * Never {@code null} under normal operation; may be {@code null} after
     * {@link #prepareForRemoval()}.
     *
     * @see #getStyleSafe()
     */
    protected IStyled _style = new Styled();

    // -----------------------------------------------------------------------
    // Behavior flags
    // -----------------------------------------------------------------------

    /** Resize policy applied during interactive resizing. */
    protected ResizePolicy _resizePolicy = ResizePolicy.NORMAL;

    /** Whether this item is rendered. */
    protected boolean _visible      = true;

    /** Whether this item can be dragged. Suppressed by {@link #_locked}. */
    protected boolean _draggable    = false;

    /** Whether this item can be rotated. Suppressed by {@link #_locked}. */
    protected boolean _rotatable    = false;
    
    /** Whether this item can be styled via the style editor. */
    protected boolean _styleEditable = true;

    /** Whether this item responds to right-click. */
    protected boolean _rightClickable = true;

    /** Whether this item can be selected. */
    protected boolean _selectable   = true;

    /** Whether this item can be connected to other items. */
    protected boolean _connectable  = false;

    /** Whether this item responds to double-click. */
    protected boolean _doubleClickable = false;

    /**
     * Master lock flag.  When {@code true} drag, resize, rotate, and delete
     * are all suppressed, regardless of the individual flags.
     * <p><b>Default is {@code true}.</b></p>
     */
    protected boolean _locked       = true;

    /**
     * Whether this item can be resized.  Suppressed by {@link #_locked}.
     * <p>
     * This field is {@code protected} (not {@code private}) so that
     * subclasses can read the raw flag value independently of the lock — for
     * example, to include it in a serialised representation.  The effective
     * resizability is always exposed through {@link #isResizable()}, which
     * also checks {@link #_locked}.
     * </p>
     */
    protected boolean _resizable    = true;

    /** Whether this item can be deleted. Suppressed by {@link #_locked}. */
    protected boolean _deletable    = false;

    /** Whether this item is currently selected. */
    protected boolean _selected     = false;

    /** Whether this item is enabled (responds to interaction). */
    protected boolean _enabled      = true;

    /**
     * Whether the item's cached rendering data is stale.
     * Setting this to {@code true} also clears {@link #_lastDrawnPolygon}.
     */
    protected boolean _dirty        = true;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /**
     * The screen polygon from the most recent {@link #drawItem} call.
     * {@code null} when dirty or not yet drawn. Used for hit-testing and
     * rubber-band selection.
     */
    protected Polygon _lastDrawnPolygon;

    /** Active modification during a drag/resize/rotate gesture. */
    protected ItemModification _modification;

    /**
     * Rotation angle in degrees, range {@code (-180, 180]}.
     * Normalised by {@link #setAzimuth(double)}.
     */
    private double _azimuth = 0.0;

    /** Human-readable display name for feedback and menus. */
    protected String _displayName = "no name";

    /** Right-click popup menu; created lazily by {@link #createPopupMenu()}. */
    protected JPopupMenu _popupMenu;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Construct an item on the given layer, optionally configuring it from
     * property key-value pairs, and immediately register it.
     *
     * <p>Registration adds the item to its layer (firing an
     * {@link ItemChangeType#ADDED} event) and registers it as a feedback
     * provider with the container's feedback control. Registration happens
     * inside the constructor so that callers — including factory methods such
     * as those in {@code CreationSupport} — do not need to take any extra
     * step after construction.</p>
     *
     * <h3>Construction-time event caveat</h3>
     * <p>
     * Because registration fires the {@code ADDED} event before any subclass
     * constructor body has run, a listener that immediately queries the item's
     * geometry on {@code ADDED} will see only the state that
     * {@link #applyProperties} has set — not any geometry a subclass sets
     * after {@code super()} returns. In practice, MDI listeners that care
     * about geometry wait for a subsequent draw or modification event, so
     * this has not been a problem. If a future listener does need to read
     * geometry on {@code ADDED}, the creation call-site should set geometry
     * before adding the listener, or the listener should guard against
     * uninitialized state.
     * </p>
     *
     * @param layer   the layer to add this item to; must not be {@code null}
     * @param keyVals optional alternating {@link PropertyUtils} key/value
     *                pairs (may be empty or {@code null})
     * @throws IllegalArgumentException if {@code layer} is {@code null}
     */
    public AItem(Layer layer, Object... keyVals) {
        if (layer == null) throw new IllegalArgumentException("layer cannot be null");
        _layer = layer;
        Properties props = PropertyUtils.fromKeyValues(keyVals);
        applyProperties(props);
        _layer.add(this);
        _layer.getContainer().getFeedbackControl().addFeedbackProvider(this);
    }

    // -----------------------------------------------------------------------
    // Property application
    // -----------------------------------------------------------------------

    /**
     * Apply a set of properties to this item's behavior flags, style, name,
     * and geometry.
     *
     * <p>Called from the Phase-1 constructor. May also be called later to
     * reconfigure the item. Order: flags → name → style → geometry.</p>
     *
     * @param props properties to apply; a no-op if {@code null}
     */
    protected void applyProperties(Properties props) {
        if (props == null) return;

        setVisible(PropertyUtils.getBoolean(props, PropertyUtils.VISIBLE, _visible));
        setLocked(PropertyUtils.getLocked(props));
        setDraggable(PropertyUtils.getDraggable(props));
        setResizable(PropertyUtils.getResizable(props));
        setRightClickable(PropertyUtils.getRightClickable(props));
        setDeletable(PropertyUtils.getDeletable(props));
        setConnectable(PropertyUtils.getConnectable(props));
        setDoubleClickable(PropertyUtils.getDoubleClickable(props));
        setRotatable(PropertyUtils.getRotatable(props));
        setStyleEditable(PropertyUtils.getStyleEditable(props));

        String title = PropertyUtils.getTitle(props);
        if (title != null) _displayName = title;

        applyStyleProperties(props);
        applyGeometryProperties(props);
    }

    /**
     * Apply style-related properties from {@code props}.
     *
     * <p>Each property is applied only when the key is present; missing keys
     * leave the current style unchanged. Subclasses that support additional
     * style properties should override this method and call
     * {@code super.applyStyleProperties(props)} first.</p>
     *
     * @param props the properties to read; must not be {@code null}
     */
    protected void applyStyleProperties(Properties props) {
        IStyled s = getStyleSafe();
        if (props.containsKey(PropertyUtils.FILLCOLOR))  s.setFillColor(PropertyUtils.getFillColor(props));
        if (props.containsKey(PropertyUtils.LINECOLOR))  s.setLineColor(PropertyUtils.getLineColor(props));
        if (props.containsKey(PropertyUtils.LINESTYLE))  s.setLineStyle(PropertyUtils.getLineStyle(props));
        if (props.containsKey(PropertyUtils.LINEWIDTH))  s.setLineWidth(PropertyUtils.getLineWidth(props));
        if (props.containsKey(PropertyUtils.SYMBOL))     s.setSymbolType(PropertyUtils.getSymbol(props));
        if (props.containsKey(PropertyUtils.SYMBOLSIZE)) s.setSymbolSize(PropertyUtils.getSymbolSize(props));
        if (props.containsKey(PropertyUtils.TEXTCOLOR))  s.setTextColor(PropertyUtils.getTextColor(props));
    }

    /**
     * Apply geometry-specific properties from {@code props}.
     *
     * <p>Default implementation is a no-op. Subclasses with geometric
     * constructor parameters (radius, width, endpoint coordinates, etc.)
     * should override this rather than adding constructor parameters.</p>
     *
     * @param props the properties to read; never {@code null}
     */
    protected void applyGeometryProperties(Properties props) {
        // default: nothing to do
    }

    // -----------------------------------------------------------------------
    // Drawing — public pipeline entry point
    // -----------------------------------------------------------------------

    /**
     * Draw this item onto the container.
     *
     * <p>This is the method called by the container's rendering pipeline.
     * Subclasses must <em>not</em> override it; implement
     * {@link #drawItem(Graphics2D, IContainer)} instead.</p>
     *
     * <p>Steps performed:</p>
     * <ol>
     *   <li>Skip if {@link #_visible} is {@code false}.</li>
     *   <li>Call {@link #shouldDraw} — skip {@link #drawItem} if
     *       {@code false}.</li>
     *   <li>Install any special clip from the owning {@link BaseView}.</li>
     *   <li>Save and restore the {@link Stroke} around {@link #drawItem}.</li>
     *   <li>Call {@link #drawSelections} unconditionally (selected items need
     *       handles even when scrolled partially out of view).</li>
     * </ol>
     *
     * @param g2        the graphics context
     * @param container the container being rendered
     */
    @Override
    public void draw(Graphics2D g2, IContainer container) {
        if (!_visible) return;

        if (shouldDraw(g2, container)) {
            Shape oldClip = g2.getClip();
            BaseView bview = container.getView();
            if (bview != null) {
                Shape clip = bview.getSpecialClip();
                if (clip != null) g2.setClip(clip);
            }

            Stroke oldStroke = g2.getStroke();
            drawItem(g2, container);
            setDirty(false);
            g2.setStroke(oldStroke);
            g2.setClip(oldClip);
        }

        drawSelections(g2, container);
    }

    // -----------------------------------------------------------------------
    // Drawing — subclass hooks (abstract)
    // -----------------------------------------------------------------------

    /**
     * Perform the actual rendering of this item's geometry.
     *
     * <p>Called by {@link #draw} after visibility and clip setup. The stroke
     * is saved and restored by {@code draw}, so subclasses may freely change
     * the stroke without cleanup.</p>
     *
     * @param g2        the graphics context
     * @param container the container being rendered
     */
    public abstract void drawItem(Graphics2D g2, IContainer container);

    /**
     * Return {@code true} if this item should be drawn on the current frame.
     *
     * <p>This is a secondary visibility check beyond the simple
     * {@link #_visible} flag. A typical implementation checks whether the
     * item's screen bounds intersect the current clip rectangle.</p>
     *
     * @param g2        the graphics context (may be used to read clip bounds)
     * @param container the container being rendered
     * @return {@code true} if {@link #drawItem} should be called
     */
    public abstract boolean shouldDraw(Graphics2D g2, IContainer container);

    // -----------------------------------------------------------------------
    // Drawing — selection and focus overlays
    // -----------------------------------------------------------------------

    /**
     * Draw selection handles and the focus indicator for this item.
     *
     * <p>Called unconditionally by {@link #draw} whenever the item is visible,
     * even when {@link #shouldDraw} returned {@code false}, so handles remain
     * visible for selected items that are partially scrolled out of view.</p>
     *
     * <p>When selected, draws:</p>
     * <ul>
     *   <li>A white oval at each selection point.</li>
     *   <li>A yellow rotation-handle oval plus {@link #rotateIcon}, if
     *       rotatable.</li>
     *   <li>The focus indicator via {@link #focusFill}.</li>
     * </ul>
     *
     * @param g2        the graphics context
     * @param container the container being rendered
     */
    public void drawSelections(Graphics2D g2, IContainer container) {
        if (!isSelected()) return;

        Point[] selectPoints = getSelectionPoints(container);
        if (selectPoints != null) {
            for (Point p : selectPoints) {
                g2.setColor(_selectFill);
                g2.fillOval(p.x - SPSIZE2, p.y - SPSIZE2, SPSIZE, SPSIZE);
                g2.setColor(_selectLine);
                g2.drawOval(p.x - SPSIZE2, p.y - SPSIZE2, SPSIZE, SPSIZE);
            }
        }

        if (isRotatable()) {
            Point rp = getRotatePoint(container);
            if (rp != null && rotateIcon != null) {
                g2.setColor(_rotateFill);
                g2.fillOval(rp.x - RPSIZE2, rp.y - RPSIZE2, RPSIZE, RPSIZE);
                rotateIcon.paintIcon(container.getComponent(), g2,
                        rp.x - rotateIcon.getIconHeight() / 2,
                        rp.y - rotateIcon.getIconHeight() / 2);
            }
        }

        focusFill(g2, container);
    }

    /**
     * Draw a small filled square at the item's focus point.
     *
     * <p>Only drawn when {@link #getFocus()} returns a non-null point.
     * The indicator is a 6×6 pixel square filled with {@link #_FOCUSFILL}.</p>
     *
     * @param g2        the graphics context
     * @param container the container being rendered
     */
    protected void focusFill(Graphics2D g2, IContainer container) {
        Point pp = getFocusPoint(container);
        if (pp != null) {
            g2.setColor(_FOCUSFILL);
            g2.fillRect(pp.x - 3, pp.y - 3, 6, 6);
        }
    }

    // -----------------------------------------------------------------------
    // Hit testing
    // -----------------------------------------------------------------------

    /**
     * Test whether this item, as rendered, contains the given screen point.
     *
     * <p>Priority order:</p>
     * <ol>
     *   <li>{@link #_lastDrawnPolygon} — exact hit test.</li>
     *   <li>{@link #getBounds(IContainer)} — axis-aligned bounding rectangle.</li>
     *   <li>{@link #inASelectRect} — selection/rotation handle rectangles.</li>
     * </ol>
     *
     * @param container   the container rendering this item
     * @param screenPoint the pixel point to test
     * @return {@code true} if the item contains {@code screenPoint}
     */
    public boolean contains(IContainer container, Point screenPoint) {
        if (_lastDrawnPolygon != null) {
            if (_lastDrawnPolygon.contains(screenPoint)) return true;
        } else {
            Rectangle r = getBounds(container);
            if (r != null && r.contains(screenPoint)) return true;
        }
        return inASelectRect(container, screenPoint);
    }

    /**
     * Test whether the screen point falls inside any selection or rotation
     * handle rectangle.
     *
     * @param container   the container rendering this item
     * @param screenPoint the pixel point to test
     * @return {@code true} if the point is inside any handle rectangle
     */
    protected boolean inASelectRect(IContainer container, Point screenPoint) {
        return inSelectPoint(container, screenPoint, false) >= 0
                || inRotatePoint(container, screenPoint);
    }

    /**
     * Test whether the screen point falls inside the rotation-handle rectangle.
     *
     * <p>Returns {@code false} immediately if the item is not rotatable or not
     * selected.</p>
     *
     * @param container   the container rendering this item
     * @param screenPoint the pixel point to test
     * @return {@code true} if the point is inside the rotation handle
     */
    public boolean inRotatePoint(IContainer container, Point screenPoint) {
        if (!isRotatable() || !isSelected()) return false;
        Point p = getRotatePoint(container);
        if (p == null) return false;
        return new Rectangle(p.x - RPSIZE2, p.y - RPSIZE2, RPSIZE, RPSIZE)
                .contains(screenPoint);
    }

    /**
     * Return the index of the selection handle whose hit-test rectangle
     * contains the given screen point, or {@code -1} if none does.
     *
     * <p>Returns {@code -1} immediately if the item is not selected, or if
     * {@code checkResizable} is {@code true} and the item is not resizable.</p>
     *
     * @param container      the container rendering this item
     * @param screenPoint    the pixel point to test
     * @param checkResizable if {@code true}, skip the test when not resizable
     * @return the zero-based handle index, or {@code -1}
     */
    public int inSelectPoint(IContainer container, Point screenPoint,
            boolean checkResizable) {
        if ((checkResizable && !isResizable()) || !isSelected()) return -1;

        Point[] pp = getSelectionPoints(container);
        if (pp == null) return -1;

        int index = 0;
        for (Point lp : pp) {
            Rectangle r = new Rectangle(
                    lp.x - SPSIZE2, lp.y - SPSIZE2, SPSIZE, SPSIZE);
            if (r.contains(screenPoint)) return index;
            index++;
        }
        return -1;
    }

    /**
     * Test whether a rubber-band rectangle completely encloses this item.
     *
     * <p>The default implementation uses the cached screen polygon if
     * available, otherwise the bounding rectangle. Subclasses with
     * non-rectangular shapes may override for a more precise test.</p>
     *
     * @param container the rendering container
     * @param r         the rubber-band rectangle in screen coordinates
     * @return {@code true} if {@code r} completely contains this item
     */
    public boolean enclosed(IContainer container, Rectangle r) {
        if (_lastDrawnPolygon != null) {
            return r.contains(_lastDrawnPolygon.getBounds());
        }
        Rectangle myBounds = getBounds(container);
        return myBounds != null && r.contains(myBounds);
    }

    // -----------------------------------------------------------------------
    // Geometry accessors
    // -----------------------------------------------------------------------

    /**
     * Return the screen-coordinate bounding rectangle of this item.
     *
     * <p>The default implementation returns {@code null}. Subclasses should
     * override to return a meaningful bounding box used for hit-testing and
     * rubber-band selection.</p>
     *
     * @param container the container rendering this item
     * @return the pixel bounding rectangle, or {@code null} if unavailable
     */
    public Rectangle getBounds(IContainer container) { return null; }

    /**
     * Return the world-coordinate bounding rectangle of this item.
     *
     * @return the world-coordinate bounding rectangle
     */
    public abstract Rectangle2D.Double getWorldBounds();

    /**
     * Return the selection handles for this item in screen coordinates.
     *
     * <p>The default uses the vertices of {@link #_lastDrawnPolygon} if
     * available, otherwise the four corners of {@link #getBounds}.</p>
     *
     * @param container the container rendering this item
     * @return array of handle points, or {@code null} if bounds are unavailable
     */
    public Point[] getSelectionPoints(IContainer container) {
        if (_lastDrawnPolygon != null && _lastDrawnPolygon.npoints > 1) {
            Point[] pp = new Point[_lastDrawnPolygon.npoints];
            for (int i = 0; i < _lastDrawnPolygon.npoints; i++) {
                pp[i] = new Point(_lastDrawnPolygon.xpoints[i],
                                  _lastDrawnPolygon.ypoints[i]);
            }
            return pp;
        }
        Rectangle r = getBounds(container);
        if (r == null) return null;
        int right  = r.x + r.width;
        int bottom = r.y + r.height;
        return new Point[] {
            new Point(r.x,   r.y),
            new Point(r.x,   bottom),
            new Point(right, bottom),
            new Point(right, r.y)
        };
    }

    /**
     * Return the screen-coordinate rotation handle point, or {@code null} if
     * the item does not support rotation.
     *
     * @param container the container rendering this item
     * @return the rotation handle point, or {@code null}
     */
    public Point getRotatePoint(IContainer container) { return null; }

    /** @return the last drawn polygon, or {@code null} */
    public Polygon getLastDrawnPolygon() { return _lastDrawnPolygon; }

    /** @return the world-coordinate path, or {@code null} */
    public Path2D.Double getPath() { return _path; }

    /** @return the world-coordinate line, or {@code null} */
    public Line2D.Double getLine() { return _line; }

    /** @return the secondary world-coordinate points, or {@code null} */
    public Point2D.Double[] getSecondaryPoints() { return _secondaryPoints; }

    /**
     * Return the world-coordinate focus of this item.
     *
     * <p>The focus is the item's conceptual center: for point items it is
     * their location; for polygon items it is typically the centroid.
     * Returns {@code null} if no focus has been set.</p>
     *
     * @return the world-coordinate focus, or {@code null}
     */
    public Point2D.Double getFocus() { return _focus; }

    /**
     * Set the world-coordinate focus of this item.
     *
     * @param wp the new focus; may be {@code null} to clear
     */
    public void setFocus(Point2D.Double wp) { _focus = wp; }

    /**
     * Return the screen-coordinate location of the focus, or {@code null} if
     * the focus is not set.
     *
     * @param container the container (used for world-to-screen transform)
     * @return the pixel location of the focus, or {@code null}
     */
    public Point getFocusPoint(IContainer container) {
        Point2D.Double wp = getFocus();
        if (wp == null) return null;
        Point pp = new Point();
        container.worldToLocal(pp, wp);
        return pp;
    }

    /**
     * Hook called when geometry changes and the focus should be recomputed.
     * The default implementation is a no-op.
     */
    protected void updateFocus() { }

    /**
     * Notify the item that its geometry has changed.
     * Recomputes the focus and marks the item dirty.
     */
    public void geometryChanged() {
        updateFocus();
        setDirty(true);
    }

    // -----------------------------------------------------------------------
    // Translation
    // -----------------------------------------------------------------------

    /**
     * Translate this item by {@code (dx, dy)} in world coordinates.
     *
     * @param dx the world-coordinate offset along the X axis
     * @param dy the world-coordinate offset along the Y axis
     */
    public abstract void translateWorld(double dx, double dy);

    /**
     * Translate this item by {@code (dx, dy)} in screen (pixel) coordinates.
     *
     * <p>Pixel deltas are converted to world deltas via the container
     * transform, then delegated to {@link #translateWorld}.</p>
     *
     * @param dx horizontal pixel offset (positive = right)
     * @param dy vertical pixel offset (positive = down)
     */
    public void translateLocal(int dx, int dy) {
        if (dx == 0 && dy == 0) return;
        IContainer container = getContainer();
        if (container == null) return;
        Point2D.Double w0 = new Point2D.Double();
        Point2D.Double w1 = new Point2D.Double();
        container.localToWorld(new Point(0,  0),  w0);
        container.localToWorld(new Point(dx, dy), w1);
        translateWorld(w1.x - w0.x, w1.y - w0.y);
    }

    // -----------------------------------------------------------------------
    // Modification lifecycle
    // -----------------------------------------------------------------------

    /**
     * Attach a modification context to begin an interactive gesture.
     *
     * @param itemModification the modification context; {@code null} to cancel
     */
    public void setModification(ItemModification itemModification) {
        _modification = itemModification;
    }

    /** @return the active modification context, or {@code null} */
    public ItemModification getItemModification() { return _modification; }

    /**
     * Classify and begin a drag, resize, or rotate modification.
     *
     * <p>Priority: rotate (if start point is in rotation handle) → resize (if
     * start point is in a selection handle) → drag (otherwise).</p>
     *
     * <p>This method is a no-op if {@link #_modification} is {@code null}.</p>
     */
    public void startModification() {
        if (_modification == null) return;

        IContainer container = _modification.getContainer();
        Point smp = _modification.getStartMousePoint();

        _modification.setType(ModificationType.DRAG);   // safe default

        if (inRotatePoint(container, smp)) {
            _modification.setType(ModificationType.ROTATE);
            return;
        }
        int index = inSelectPoint(container, smp, true);
        if (index >= 0) {
            _modification.setSelectIndex(index);
            _modification.setType(ModificationType.RESIZE);
        }
    }

    /**
     * Update this item's geometry in response to a mouse movement during an
     * active modification.
     *
     * <p>Called repeatedly while the user is dragging, resizing, or rotating.
     * The current mouse position is available through {@link #_modification}.</p>
     */
    public abstract void modify();

    /**
     * Finalise the current modification, notify layer listeners, and clear the
     * modification record.
     *
     * <p>Fires {@link ItemChangeType#MOVED}, {@link ItemChangeType#ROTATED}, or
     * {@link ItemChangeType#RESIZED} depending on the modification type.
     * This method is a no-op if {@link #_modification} is {@code null}.</p>
     */
    public void stopModification() {
        if (_modification == null) return;

        switch (_modification.getType()) {
            case DRAG:   _layer.notifyItemChangeListeners(this, ItemChangeType.MOVED);   break;
            case ROTATE: _layer.notifyItemChangeListeners(this, ItemChangeType.ROTATED); break;
            case RESIZE: _layer.notifyItemChangeListeners(this, ItemChangeType.RESIZED); break;
        }
        _modification = null;
    }

    // -----------------------------------------------------------------------
    // Behavior flags — getters and setters
    // -----------------------------------------------------------------------

    /** @return {@code true} if this item is visible */
    @Override public boolean isVisible() { return _visible; }

    /** @param visible {@code true} to make the item visible */
    @Override public void setVisible(boolean visible) { _visible = visible; }

    /**
     * Return {@code true} if this item can be dragged.
     * Returns {@code false} when locked, regardless of the drag flag.
     *
     * @return effective draggability
     */
    public boolean isDraggable()           { return !_locked && _draggable; }
    /** @param draggable {@code true} to allow dragging */
    public void setDraggable(boolean draggable)     { _draggable = draggable; }

    /**
     * Return {@code true} if this item can be deleted.
     * Returns {@code false} when locked.
     *
     * @return effective deletability
     */
    public boolean isDeletable()           { return !_locked && _deletable; }
    /** @param deletable {@code true} to allow deletion */
    public void setDeletable(boolean deletable)     { _deletable = deletable; }

    /**
     * Return {@code true} if this item can be resized.
     * Returns {@code false} when locked.
     *
     * @return effective resizability
     */
    public boolean isResizable()           { return !_locked && _resizable; }
    /** @param resizable {@code true} to allow resizing */
    public void setResizable(boolean resizable)     { _resizable = resizable; }

    /**
     * Return {@code true} if this item can be rotated.
     * Returns {@code false} when locked.
     *
     * @return effective rotatability
     */
    public boolean isRotatable()           { return !_locked && _rotatable; }
    /** @param rotatable {@code true} to allow rotation */
    public void setRotatable(boolean rotatable)     { _rotatable = rotatable; }
    
    /** @return {@code true} if this item can be styled via the style editor */
    public boolean isStyleEditable()       { return _styleEditable; }
    /** @param styleEditable {@code true} to allow styling via the style editor */
    public void setStyleEditable(boolean styleEditable) { _styleEditable = styleEditable; }

    /** @return {@code true} if this item can be connected to other items */
    public boolean isConnectable()         { return _connectable; }
    /** @param connectable {@code true} to allow connections */
    public void setConnectable(boolean connectable) { _connectable = connectable; }

    /** @return {@code true} if this item responds to double-click */
    public boolean isDoubleClickable()     { return _doubleClickable; }
    /** @param doubleClickable {@code true} to enable double-click */
    public void setDoubleClickable(boolean doubleClickable) { _doubleClickable = doubleClickable; }

    /** @return {@code true} if this item can be selected */
    public boolean isSelectable()          { return _selectable; }
    /** @param selectable {@code true} to allow selection */
    public void setSelectable(boolean selectable) { _selectable = selectable; }

    /**
     * Return {@code true} if this item is locked.
     *
     * <p>A locked item ignores drag, resize, rotate, and delete operations
     * regardless of its individual flags. Items are locked by default.</p>
     *
     * @return {@code true} if locked
     */
    public boolean isLocked()              { return _locked; }
    /** @param locked {@code true} to lock the item */
    public void setLocked(boolean locked)  { _locked = locked; }

    /** @return {@code true} if this item responds to right-click */
    public boolean isRightClickable()      { return _rightClickable; }
    /** @param rightClickable {@code true} to enable right-click */
    public void setRightClickable(boolean rightClickable) { _rightClickable = rightClickable; }

    /** @return {@code true} if this item is currently selected */
    public boolean isSelected()            { return _selected; }

    /**
     * Set the selected state of this item and request a container refresh.
     *
     * @param selected {@code true} to select the item
     */
    public void setSelected(boolean selected) {
        _selected = selected;
        _layer.getContainer().refresh();
    }

    /** @return {@code true} if this item is enabled */
    public boolean isEnabled()             { return _enabled; }
    /** @param enabled {@code true} to enable the item */
    public void setEnabled(boolean enabled) { _enabled = enabled; }

    /**
     * Return {@code true} if this item's cached rendering data is stale.
     *
     * @return the dirty flag
     */
    public boolean isDirty()              { return _dirty; }

    /**
     * Set the dirty flag.  Setting to {@code true} also clears
     * {@link #_lastDrawnPolygon}.
     *
     * @param dirty new dirty state
     */
    public void setDirty(boolean dirty) {
        _dirty = dirty;
        if (dirty) _lastDrawnPolygon = null;
    }

    // -----------------------------------------------------------------------
    // Trackability
    // -----------------------------------------------------------------------

    /**
     * Return {@code true} if this item is "trackable" — that is, eligible to
     * receive drag, resize, or rotate gestures from the interaction layer.
     *
     * <p>An item is trackable only when it is unlocked, enabled, and at least
     * one of {@link #_draggable}, {@link #_rotatable}, or {@link #_resizable}
     * is {@code true}.  The interaction layer uses this as a fast pre-check
     * before performing the more expensive hit-test against individual
     * selection and rotation handles.</p>
     *
     * @return {@code true} if the item should be tracked during mouse gestures
     */
    public boolean isTrackable() {
        if (_locked || !_enabled) return false;
        return _draggable || _rotatable || _resizable;
    }

    // -----------------------------------------------------------------------
    // Style
    // -----------------------------------------------------------------------

    /**
     * Return this item's style object.
     *
     * @return the style; may be {@code null} after {@link #prepareForRemoval()}
     */
    public IStyled getStyle() { return _style; }

    /**
     * Replace the drawing style for this item.
     *
     * <p>Passing {@code null} is accepted but will cause
     * {@link NullPointerException}s in the drawing pipeline unless
     * {@link #getStyleSafe()} is used for all subsequent style accesses.</p>
     *
     * @param style the new style; {@code null} is technically permitted but
     *              discouraged
     */
    public void setStyle(IStyled style) { _style = style; }

    /**
     * Return this item's style, recreating it as a default {@link Styled}
     * instance if {@link #_style} is {@code null}.
     *
     * <p>Used internally (e.g. in {@link #applyStyleProperties}) to guard
     * against the style having been cleared by {@link #prepareForRemoval()}.
     * Prefer {@link #getStyle()} in normal usage.</p>
     *
     * @return the style object; never {@code null}
     */
    public IStyled getStyleSafe() {
        if (_style == null) _style = new Styled();
        return _style;
    }

    // -----------------------------------------------------------------------
    // Azimuth
    // -----------------------------------------------------------------------

    /**
     * Return the rotation angle in degrees, range {@code (-180, 180]}.
     *
     * @return the azimuth in degrees
     */
    public double getAzimuth()            { return _azimuth; }

    /**
     * Set the rotation angle, normalising the value to {@code (-180, 180]}.
     *
     * @param azimuth the new azimuth in degrees
     */
    public void setAzimuth(double azimuth) {
        while (azimuth >  180.0) azimuth -= 360.0;
        while (azimuth <= -180.0) azimuth += 360.0;
        _azimuth = azimuth;
    }

    // -----------------------------------------------------------------------
    // Display name
    // -----------------------------------------------------------------------

    /** @return the human-readable display name */
    public String getDisplayName()              { return _displayName; }
    /** @param name the new display name */
    public void setDisplayName(String name)     { _displayName = name; }

    // -----------------------------------------------------------------------
    // Resize policy
    // -----------------------------------------------------------------------

    /** @return the resize policy */
    public ResizePolicy getResizePolicy()              { return _resizePolicy; }
    /** @param policy the new resize policy */
    public void setResizePolicy(ResizePolicy policy)   { _resizePolicy = policy; }

    // -----------------------------------------------------------------------
    // Container / layer / view
    // -----------------------------------------------------------------------

    /**
     * Return the {@link IContainer} that hosts this item's layer.
     *
     * @return the container; may be {@code null} after {@link #prepareForRemoval()}
     */
    public IContainer getContainer() {
        return (_layer != null) ? _layer.getContainer() : null;
    }

    /**
     * Return the {@link Layer} on which this item lives.
     *
     * @return the layer; may be {@code null} after {@link #prepareForRemoval()}
     */
    public Layer getLayer() { return _layer; }

    /**
     * Return the {@link BaseView} that owns the container hosting this item,
     * or {@code null} if unavailable.
     *
     * @return the owning view, or {@code null}
     */
    public BaseView getView() {
        IContainer container = getContainer();
        return (container != null) ? container.getView() : null;
    }

    // -----------------------------------------------------------------------
    // Popup menu
    // -----------------------------------------------------------------------

    /**
     * Return this item's popup menu, creating it lazily on first call.
     *
     * @return the popup menu; never {@code null}
     */
    public JPopupMenu getPopupMenu() {
        if (_popupMenu == null) createPopupMenu();
        return _popupMenu;
    }

    /**
     * Build the basic popup menu for this item.
     *
     * <p>The default menu contains an item-ordering sub-menu and a "Locked"
     * checkbox. Subclasses may override and call {@code super.createPopupMenu()}
     * before adding their own items.</p>
     *
     * @return the newly created popup menu
     */
    protected JPopupMenu createPopupMenu() {
        _popupMenu = new JPopupMenu();
        _popupMenu.add(ItemOrderingMenu.forItem(this, true));

        JCheckBoxMenuItem cbitem = new JCheckBoxMenuItem("Locked", isLocked());
        cbitem.addItemListener(e -> {
            setLocked(cbitem.isSelected());
            if (isLocked()) {
                setSelected(false);
                IContainer cont = getContainer();
                if (cont != null) cont.refresh();
            }
        });
        _popupMenu.add(cbitem);
        return _popupMenu;
    }

    /**
     * Show this item's popup menu at the given screen point.
     *
     * @param pp the screen point; must not be {@code null}
     */
    public void prepareForPopup(Point pp) {
        java.util.Objects.requireNonNull(pp, "Popup location cannot be null");
        IContainer container = getContainer();
        if (container == null) return;
        JPopupMenu menu = getPopupMenu();
        if (menu != null) menu.show(container.getComponent(), pp.x, pp.y);
    }

    // -----------------------------------------------------------------------
    // Double-click
    // -----------------------------------------------------------------------

    /**
     * Called when this item receives a double-click.
     * The default implementation is a no-op.
     *
     * @param mouseEvent the double-click event
     */
    public void doubleClicked(MouseEvent mouseEvent) { }

    // -----------------------------------------------------------------------
    // Feedback
    // -----------------------------------------------------------------------

    /**
     * Populate the feedback string list with information about this item.
     * The default implementation is a no-op.
     *
     * @param container       the container rendering this item
     * @param pp              the current mouse position in screen coordinates
     * @param wp              the corresponding world-coordinate position
     * @param feedbackStrings the list to add strings to
     */
    @Override
    public void getFeedbackStrings(IContainer container, Point pp,
            Point2D.Double wp, List<String> feedbackStrings) { }

    // -----------------------------------------------------------------------
    // Equality
    // -----------------------------------------------------------------------

    /**
     * Identity-based equality: two {@code AItem} references are equal only
     * when they are the same object.
     *
     * @param o the object to compare
     * @return {@code true} only if {@code o == this}
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof AItem) && (this == o);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() { return System.identityHashCode(this); }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Prepare this item for removal from its layer.
     *
     * <p>Performs two operations:</p>
     * <ol>
     *   <li><b>Feedback de-registration.</b> The item unregisters itself from
     *       the container's {@link edu.cnu.mdi.feedback.FeedbackControl} so
     *       that it is no longer polled after removal. The container reference
     *       is captured before {@link #_layer} is nulled.</li>
     *   <li><b>Reference nulling.</b> Heavyweight references are set to
     *       {@code null} to assist garbage collection and to cause fast,
     *       obvious {@link NullPointerException}s if the item is used
     *       after removal.</li>
     * </ol>
     *
     * <p>Subclasses holding additional heavyweight references should override
     * this method, null those fields, and call {@code super.prepareForRemoval()}
     * either first or last.</p>
     */
    @Override
    public void prepareForRemoval() {
        IContainer cont = (_layer != null) ? _layer.getContainer() : null;
        if (cont != null) {
            cont.getFeedbackControl().removeFeedbackProvider(this);
        }
        _focus            = null;
        _lastDrawnPolygon = null;
        _layer            = null;
        _path             = null;
        _secondaryPoints  = null;
        _style            = null;
    }
}