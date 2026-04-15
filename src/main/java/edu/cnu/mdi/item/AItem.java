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
 * <p>
 * {@code draw} handles visibility, optional special clipping from the owning
 * view, stroke save/restore, and the overlay of selection and rotation
 * handles.
 * </p>
 *
 * <h2>Behavior flags and the lock</h2>
 * <p>
 * Each interactive behavior (dragging, resizing, rotating, deleting) is
 * individually controlled by a boolean flag. However, the {@link #_locked}
 * flag is a master override: when {@code true} it suppresses dragging,
 * resizing, rotating, and deleting <em>regardless</em> of the individual
 * flags. Locking does not suppress visibility or selection display.
 * </p>
 * <p>
 * <strong>Important:</strong> items are locked by default ({@code _locked =
 * true}). Applications must explicitly call {@link #setLocked(boolean) setLocked(false)}
 * (or supply {@link edu.cnu.mdi.util.PropertyUtils#LOCKED LOCKED} in the
 * constructor key-value pairs) to make an item interactive.
 * </p>
 *
 * <h2>Property-based construction</h2>
 * <p>
 * The constructor accepts an optional varargs list of alternating
 * {@link PropertyUtils} key/value pairs. These are parsed and applied by
 * {@link #applyProperties(Properties)}, which covers behavior flags, style
 * attributes, and display name. Subclasses that need geometry parameters
 * (e.g. radius, width) should override {@link #applyGeometryProperties(Properties)}
 * rather than the constructor.
 * </p>
 *
 * <h2>Geometry</h2>
 * <p>
 * {@code AItem} provides three geometry fields that subclasses may use:
 * </p>
 * <ul>
 *   <li>{@link #_path} — a world-coordinate {@link Path2D.Double} for
 *       polygon-like items.</li>
 *   <li>{@link #_line} — a world-coordinate {@link Line2D.Double} for
 *       line items.</li>
 *   <li>{@link #_focus} — a world-coordinate {@link Point2D.Double} that
 *       represents the item's conceptual center (centroid, anchor point,
 *       etc.).</li>
 * </ul>
 * <p>
 * Not every subclass uses all three. The focus is drawn as a small filled
 * square when the item is selected.
 * </p>
 *
 * <h2>Modification lifecycle</h2>
 * <p>
 * Interactive modifications (drag, resize, rotate) follow a three-step
 * lifecycle managed by the container's interaction layer:
 * </p>
 * <ol>
 *   <li>{@link #setModification(ItemModification)} — attaches an
 *       {@link ItemModification} record describing the operation.</li>
 *   <li>{@link #startModification()} — classifies the operation (drag,
 *       resize, or rotate) based on which part of the item was clicked.</li>
 *   <li>{@link #modify()} — called repeatedly during the gesture to update
 *       the item's geometry.</li>
 *   <li>{@link #stopModification()} — finalises the operation, notifies
 *       listeners, and clears the modification record.</li>
 * </ol>
 */
public abstract class AItem implements IDrawable, IFeedbackProvider {

    // -----------------------------------------------------------------------
    // Shared visual constants
    // -----------------------------------------------------------------------

    /**
     * Semi-transparent grey fill used to draw the focus point indicator when
     * the item is selected.
     */
    protected static final Color _FOCUSFILL = new Color(128, 128, 128, 128);

    /**
     * Pixel size of the square hit-test area centerd on each selection handle.
     *
     * @see #SPSIZE2
     */
    protected static final int SPSIZE = 10;

    /**
     * Half of {@link #SPSIZE}, used to center selection handles on their
     * nominal points.
     */
    protected static final int SPSIZE2 = SPSIZE / 2;

    /**
     * Pixel size of the square hit-test area centerd on the rotation handle.
     *
     * @see #RPSIZE2
     */
    protected static final int RPSIZE = 14;

    /**
     * Half of {@link #RPSIZE}, used to center the rotation handle on its
     * nominal point.
     */
    protected static final int RPSIZE2 = RPSIZE / 2;

    /**
     * Icon rendered inside the rotation handle when the item is selected and
     * rotatable. Loaded lazily from the MDI SVG resource bundle.
     */
    protected static Icon rotateIcon =
            ImageManager.getInstance().loadUiIcon(
                    Environment.MDI_RESOURCE_PATH + "images/svg/rotate.svg",
                    16);

    /** Fill color used for selection handle ovals. */
    private static final Color _selectFill = Color.white;

    /** Outline color used for selection handle ovals. */
    private static final Color _selectLine = Color.black;

    /** Fill color used for the rotation handle oval. */
    private static final Color _rotateFill =
            X11Colors.getX11Color("yellow", 64);

    // -----------------------------------------------------------------------
    // Geometry fields
    // -----------------------------------------------------------------------

    /**
     * Optional world-coordinate path used by polygon-like items.
     * Not all subclasses use this field.
     */
    protected Path2D.Double _path;

    /**
     * Optional world-coordinate line used by line-based items.
     * Not all subclasses use this field.
     */
    protected Line2D.Double _line;

    /**
     * Optional secondary world-coordinate points (e.g. internal control
     * points). Not all subclasses use this field.
     */
    protected Point2D.Double[] _secondaryPoints;

    /**
     * The world-coordinate focus of this item — typically its center,
     * centroid, or anchor point. Drawn as a small filled square when the
     * item is selected. May be {@code null} if the item has no defined focus.
     */
    protected Point2D.Double _focus;

    // -----------------------------------------------------------------------
    // Layer and style
    // -----------------------------------------------------------------------

    /**
     * The {@link Layer} on which this item lives. Set at construction and
     * nulled out by {@link #prepareForRemoval()}.
     */
    protected Layer _layer;

    /**
     * The drawing style for this item (fill color, line color, line style,
     * symbol type, etc.). Initialized to a default {@link Styled} instance;
     * never {@code null} under normal operation but may be {@code null} after
     * {@link #prepareForRemoval()}.
     *
     * @see #getStyleSafe()
     */
    protected IStyled _style = new Styled();

    // -----------------------------------------------------------------------
    // Behavior flags
    // -----------------------------------------------------------------------

    /**
     * Resize policy applied when this item is resized interactively.
     */
    protected ResizePolicy _resizePolicy = ResizePolicy.NORMAL;

    /**
     * Controls whether this item is rendered. Invisible items are completely
     * skipped by the drawing pipeline, including selection handles.
     */
    protected boolean _visible = true;

    /**
     * Controls whether this item can be dragged by the user.
     * Suppressed by {@link #_locked}.
     */
    protected boolean _draggable = false;

    /**
     * Controls whether this item can be rotated by the user.
     * Suppressed by {@link #_locked}.
     */
    protected boolean _rotatable = false;

    /**
     * Controls whether this item responds to a right-click gesture.
     */
    protected boolean _rightClickable = true;

    /**
     * Controls whether this item can be selected by the user.
     */
    protected boolean _selectable = true;

    /**
     * Controls whether this item can be connected to other items.
     */
    protected boolean _connectable = false;

    /**
     * Controls whether this item responds to a double-click gesture.
     */
    protected boolean _doubleClickable = false;

    /**
     * Master lock flag. When {@code true}, the item cannot be dragged,
     * rotated, resized, or deleted, regardless of the individual behavior
     * flags. Locking does not affect visibility or selection display.
     *
     * <p><strong>Default is {@code true}.</strong> Newly created items are
     * locked; call {@link #setLocked(boolean) setLocked(false)} or supply
     * {@link PropertyUtils#LOCKED LOCKED=false} in the constructor to enable
     * interaction.</p>
     */
    protected boolean _locked = true;

    /**
     * Controls whether this item can be resized by the user.
     * Suppressed by {@link #_locked}.
     */
    private boolean _resizable = true;

    /**
     * Controls whether this item can be deleted by the user.
     * Suppressed by {@link #_locked}.
     */
    protected boolean _deletable = false;

    /**
     * Whether this item is currently selected.
     * Selected items display selection handles and a focus indicator.
     */
    protected boolean _selected = false;

    /**
     * Whether this item is enabled. Disabled items are inert — they do not
     * respond to interaction — and may be rendered in a ghosted style.
     */
    protected boolean _enabled = true;

    /**
     * Whether this item's cached rendering data is stale.
     * <p>
     * When {@code true} the item must be redrawn from scratch on the next
     * paint cycle. Items that cache intermediate geometry (e.g. a
     * pre-computed screen polygon) use this flag to decide whether to
     * recompute. Setting dirty to {@code true} also clears
     * {@link #_lastDrawnPolygon}.
     * </p>
     */
    protected boolean _dirty = true;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /**
     * The screen polygon produced by the most recent {@link #drawItem} call,
     * or {@code null} if the item has not yet been drawn, has been marked
     * dirty, or does not cache a polygon.
     * <p>
     * Used for hit-testing ({@link #contains}) and rubber-band selection
     * ({@link #enclosed}).
     * </p>
     */
    protected Polygon _lastDrawnPolygon;

    /**
     * The active modification record during an interactive drag, resize, or
     * rotate gesture. {@code null} when no modification is in progress.
     */
    protected ItemModification _modification;

    /**
     * Reference rotation angle in degrees, in the range {@code (-180, 180]}.
     * Normalised automatically by {@link #setAzimuth(double)}.
     */
    private double _azimuth = 0.0;

    /**
     * Human-readable display name for this item. Used in feedback strings,
     * popup menus, and debugging. Defaults to {@code "no name"}.
     */
    protected String _displayName = "no name";

    /**
     * The right-click popup menu for this item. Created lazily by
     * {@link #createPopupMenu()} on first access via {@link #getPopupMenu()}.
     */
    protected JPopupMenu _popupMenu;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Construct an item on the given layer, optionally configuring it from
     * property key-value pairs.
     *
     * <p>The item is added to {@code layer} immediately and registered as a
     * feedback provider with the container's feedback control. If
     * {@code keyVals} are supplied, they are parsed and applied via
     * {@link #applyProperties(Properties)}, which sets behavior flags,
     * style, and display name. Subclass geometry can be initialized by
     * overriding {@link #applyGeometryProperties(Properties)}.</p>
     *
     * @param layer   the layer to add this item to; must not be {@code null}
     * @param keyVals optional alternating {@link PropertyUtils} key/value
     *                pairs (may be empty)
     */
    public AItem(Layer layer, Object... keyVals) {
        _layer = layer;
        _layer.add(this);
        _layer.getContainer().getFeedbackControl().addFeedbackProvider(this);

        Properties props = PropertyUtils.fromKeyValues(keyVals);
        applyProperties(props);
    }

    // -----------------------------------------------------------------------
    // Property application
    // -----------------------------------------------------------------------

    /**
     * Apply a set of properties to this item's behavior flags, style, name,
     * and geometry.
     *
     * <p>This method is called by the constructor but can also be called at
     * any later time to reconfigure the item. The application order is:
     * behavior flags → display name → style → geometry. Subclasses that
     * need to apply additional properties should override
     * {@link #applyGeometryProperties(Properties)} for geometry, or call
     * {@code super.applyProperties(props)} before their own logic for
     * anything else.</p>
     *
     * @param props the properties to apply; a no-op if {@code null}
     */
    protected void applyProperties(Properties props) {
        if (props == null) return;

        // Behavior flags
        setVisible(PropertyUtils.getBoolean(props, PropertyUtils.VISIBLE, _visible));
        setLocked(PropertyUtils.getLocked(props));
        setDraggable(PropertyUtils.getDraggable(props));
        setResizable(PropertyUtils.getResizable(props));
        setRightClickable(PropertyUtils.getRightClickable(props));
        setDeletable(PropertyUtils.getDeletable(props));
        setConnectable(PropertyUtils.getConnectable(props));
        setDoubleClickable(PropertyUtils.getDoubleClickable(props));
        setRotatable(PropertyUtils.getRotatable(props));

        // Display name
        String title = PropertyUtils.getTitle(props);
        if (title != null) _displayName = title;

        // Style and geometry (delegated)
        applyStyleProperties(props);
        applyGeometryProperties(props);
    }

    /**
     * Apply style-related properties (colors, line style, line width, symbol
     * type and size) from {@code props}.
     *
     * <p>Each property is applied only if it is actually present in
     * {@code props}, so missing keys leave the current style unchanged. The
     * style object is accessed via {@link #getStyleSafe()} so a missing
     * {@code _style} is recreated rather than causing a
     * {@link NullPointerException}.</p>
     *
     * <p>Subclasses that support additional style properties should override
     * this method and call {@code super.applyStyleProperties(props)} first.</p>
     *
     * @param props the properties to read style values from; must not be
     *              {@code null}
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
     * <p>The default implementation is a no-op. Subclasses that accept
     * geometric constructor parameters (radius, width, endpoint coordinates,
     * etc.) should override this method to read those values from
     * {@code props} rather than adding constructor parameters.</p>
     *
     * @param props the properties to read geometry values from; never
     *              {@code null}
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
     * Subclasses must <em>not</em> override it; they implement
     * {@link #drawItem(Graphics2D, IContainer)} instead.</p>
     *
     * <p>The method performs the following steps, in order:</p>
     * <ol>
     *   <li>Skip everything if {@link #_visible} is {@code false}.</li>
     *   <li>Call {@link #shouldDraw} — skip {@code drawItem} if it returns
     *       {@code false}.</li>
     *   <li>Install any special clip provided by the owning
     *       {@link BaseView}.</li>
     *   <li>Save and restore the {@link java.awt.Stroke} around
     *       {@link #drawItem}, so subclasses can freely change the stroke
     *       without affecting other items.</li>
     *   <li>Call {@link #drawSelections} to overlay selection handles and the
     *       focus indicator, even if {@code shouldDraw} returned
     *       {@code false} (because a selected item may still need its handles
     *       drawn when it is temporarily out of the visible area).</li>
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
     * <p>This method is called by {@link #draw} after visibility and clip
     * setup. Subclasses implement their drawing here. The stroke is saved and
     * restored by {@code draw}, so subclasses may freely set any stroke
     * without cleaning up.</p>
     *
     * @param g2        the graphics context
     * @param container the container being rendered
     */
    public abstract void drawItem(Graphics2D g2, IContainer container);

    /**
     * Determine whether this item should be drawn on the current frame.
     *
     * <p>This is a secondary visibility check, beyond the simple
     * {@link #_visible} flag. A typical implementation checks whether the
     * item's screen bounds intersect the current clip rectangle. The method
     * is called by {@link #draw} before {@link #drawItem} is invoked.</p>
     *
     * @param g2        the graphics context (may be used to read clip bounds)
     * @param container the container being rendered
     * @return {@code true} if {@link #drawItem} should be called for this
     *         frame
     */
    public abstract boolean shouldDraw(Graphics2D g2, IContainer container);

    // -----------------------------------------------------------------------
    // Drawing — selection and focus overlays
    // -----------------------------------------------------------------------

    /**
     * Draw the selection handles and focus indicator for this item.
     *
     * <p>Called unconditionally by {@link #draw} whenever the item is
     * visible, even if {@link #shouldDraw} returned {@code false}. This
     * ensures handles remain visible for selected items that are partially
     * scrolled out of view.</p>
     *
     * <p>If the item is not selected this method returns immediately without
     * drawing anything. When selected it draws:</p>
     * <ul>
     *   <li>A small white oval at each selection point (from
     *       {@link #getSelectionPoints}).</li>
     *   <li>A yellow rotation-handle oval plus the {@link #rotateIcon} at
     *       the rotation point (from {@link #getRotatePoint}), if the item
     *       is rotatable.</li>
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
     * Draw the focus indicator — a small filled square at the item's screen
     * focus point.
     *
     * <p>Only drawn when {@link #getFocus()} returns a non-null point.
     * The indicator is a 6×6 pixel square filled with {@link #_FOCUSFILL}.
     * </p>
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
     * <p>The test uses the best available geometry, in priority order:</p>
     * <ol>
     *   <li>{@link #_lastDrawnPolygon} — if the item cached a screen polygon
     *       from its last draw, use that for an exact hit test.</li>
     *   <li>{@link #getBounds(IContainer)} — fall back to the axis-aligned
     *       bounding rectangle.</li>
     *   <li>{@link #inASelectRect} — as a last resort, check the selection
     *       and rotation handle rectangles.</li>
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
     * <p>Used as a last-resort hit-test by {@link #contains} when neither the
     * cached polygon nor the bounding rectangle contains the point. This
     * ensures that selection handles themselves are always clickable.</p>
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
     * Test whether the screen point falls inside the hit-test rectangle
     * around the rotation handle.
     *
     * <p>Returns {@code false} immediately if the item is not rotatable or
     * not selected.</p>
     *
     * @param container   the container rendering this item
     * @param screenPoint the pixel point to test
     * @return {@code true} if the point is inside the rotation handle area
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
     * {@code checkResizable} is {@code true} and the item is not resizable.
     * </p>
     *
     * @param container      the container rendering this item
     * @param screenPoint    the pixel point to test
     * @param checkResizable if {@code true}, skip the test when the item is
     *                       not resizable
     * @return the zero-based index of the hit selection point, or {@code -1}
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
     * Check whether a rubber-band rectangle completely encloses this item.
     *
     * <p>Used during rubber-band selection. The default implementation tests
     * the cached screen polygon (if available) or the bounding rectangle.
     * Subclasses with non-rectangular shapes may override for a more precise
     * test.</p>
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
     * override to return a meaningful bounding box, which is used for
     * hit-testing and rubber-band selection when no cached polygon is
     * available.</p>
     *
     * @param container the container rendering this item
     * @return the pixel bounding rectangle, or {@code null} if unavailable
     */
    public Rectangle getBounds(IContainer container) {
        return null;
    }

    /**
     * Return the world-coordinate bounding rectangle of this item.
     *
     * @return the world-coordinate bounding rectangle; must not be
     *         {@code null} in a concrete implementation
     */
    public abstract Rectangle2D.Double getWorldBounds();

    /**
     * Return the selection handles for this item in screen coordinates.
     *
     * <p>The default implementation uses the vertices of
     * {@link #_lastDrawnPolygon} if it exists and has more than one point,
     * otherwise falls back to the four corners of {@link #getBounds}.</p>
     *
     * <p>Subclasses may override to return a custom set of handles (e.g. for
     * a line item: just the two endpoints).</p>
     *
     * @param container the container rendering this item
     * @return array of selection handle points, or {@code null} if bounds
     *         are unavailable
     */
    public Point[] getSelectionPoints(IContainer container) {
        if (_lastDrawnPolygon != null && _lastDrawnPolygon.npoints > 1) {
            Point[] pp = new Point[_lastDrawnPolygon.npoints];
            for (int i = 0; i < _lastDrawnPolygon.npoints; i++) {
                pp[i] = new Point(
                        _lastDrawnPolygon.xpoints[i],
                        _lastDrawnPolygon.ypoints[i]);
            }
            return pp;
        }

        Rectangle r = getBounds(container);
        if (r == null) return null;

        int right  = r.x + r.width;
        int bottom = r.y + r.height;
        return new Point[] {
            new Point(r.x,    r.y),
            new Point(r.x,    bottom),
            new Point(right,  bottom),
            new Point(right,  r.y)
        };
    }

    /**
     * Return the screen-coordinate rotation handle point for this item.
     *
     * <p>The default implementation returns {@code null}, meaning the item
     * has no rotation handle. Subclasses that support rotation should
     * override to return a meaningful point — typically near one corner of
     * the item's bounding box.</p>
     *
     * @param container the container rendering this item
     * @return the rotation handle point, or {@code null} if not applicable
     */
    public Point getRotatePoint(IContainer container) {
        return null;
    }

    /**
     * Return the last screen polygon produced by {@link #drawItem}, or
     * {@code null} if the item has not been drawn, is dirty, or does not
     * cache a polygon.
     *
     * @return the last drawn polygon, or {@code null}
     */
    public Polygon getLastDrawnPolygon() {
        return _lastDrawnPolygon;
    }

    /**
     * Return the world-coordinate path used by this item, or {@code null} if
     * the item does not use a path.
     *
     * @return the world-coordinate path, or {@code null}
     */
    public Path2D.Double getPath() {
        return _path;
    }

    /**
     * Return the world-coordinate line used by this item, or {@code null} if
     * the item does not use a line.
     *
     * @return the world-coordinate line, or {@code null}
     */
    public Line2D.Double getLine() {
        return _line;
    }

    /**
     * Return the optional secondary world-coordinate points (e.g. internal
     * control points), or {@code null} if none are defined.
     *
     * @return the secondary points array, or {@code null}
     */
    public Point2D.Double[] getSecondaryPoints() {
        return _secondaryPoints;
    }

    /**
     * Return the world-coordinate focus of this item.
     *
     * <p>The focus is the item's conceptual center: for point-like items it
     * is their location; for polygon items it is typically the centroid.
     * Returns {@code null} if no focus has been set.</p>
     *
     * @return the world-coordinate focus, or {@code null}
     */
    public Point2D.Double getFocus() {
        return _focus;
    }

    /**
     * Set the world-coordinate focus of this item.
     *
     * <p>Subclasses should override this to enforce any constraints on the
     * focus (e.g. ensuring it stays within the item's bounding geometry).
     * The base implementation simply stores the point.</p>
     *
     * @param wp the new focus; may be {@code null} to clear
     */
    public void setFocus(Point2D.Double wp) {
        _focus = wp;
    }

    /**
     * Return the screen-coordinate (pixel) location of this item's focus
     * point, or {@code null} if the focus is not set.
     *
     * @param container the container rendering this item (used for the
     *                  world-to-screen transform)
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
     * Hook called when geometry changes and the focus point should be
     * recomputed.
     *
     * <p>The default implementation is a no-op. Subclasses that maintain a
     * derived focus (e.g. a centroid) should override this to recompute it
     * from the current geometry.</p>
     */
    protected void updateFocus() {
        // default: nothing to do
    }

    /**
     * Notify the item that its geometry has changed.
     *
     * <p>Calls {@link #updateFocus()} to recompute the focus, then marks the
     * item dirty so it is redrawn from scratch on the next paint cycle.</p>
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
     * <p>All concrete subclasses must implement this to move their geometry
     * accordingly.</p>
     *
     * @param dx the world-coordinate offset along the X axis
     * @param dy the world-coordinate offset along the Y axis
     */
    public abstract void translateWorld(double dx, double dy);

    /**
     * Translate this item by {@code (dx, dy)} in screen (pixel) coordinates.
     *
     * <p>The pixel deltas are converted to world-coordinate deltas using the
     * current container transform, then delegated to
     * {@link #translateWorld(double, double)}. This method is a no-op if
     * both deltas are zero or if the container is unavailable.</p>
     *
     * @param dx the horizontal pixel offset (positive = right)
     * @param dy the vertical pixel offset (positive = down)
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
     * Attach an {@link ItemModification} record to this item, signalling that
     * an interactive modification is about to begin.
     *
     * <p>Called by the container's interaction layer before
     * {@link #startModification()}. The modification record carries the
     * container reference, the mouse start point, and will receive the
     * resolved {@link ModificationType} from {@link #startModification()}.
     * </p>
     *
     * @param itemModification the modification context; may be {@code null}
     *                         to cancel any pending modification
     */
    public void setModification(ItemModification itemModification) {
        _modification = itemModification;
    }

    /**
     * Return the current modification record, or {@code null} if no
     * modification is in progress.
     *
     * @return the active {@link ItemModification}, or {@code null}
     */
    public ItemModification getItemModification() {
        return _modification;
    }

    /**
     * Classify and begin a drag, resize, or rotate modification.
     *
     * <p>Called once, immediately after {@link #setModification}, to
     * determine which kind of modification the mouse-down position implies.
     * Priority order is:</p>
     * <ol>
     *   <li><strong>Rotate</strong> — if the start point is inside the
     *       rotation handle.</li>
     *   <li><strong>Resize</strong> — if the start point is inside a
     *       selection handle and the item is resizable.</li>
     *   <li><strong>Drag</strong> — otherwise.</li>
     * </ol>
     * <p>
     * This method is a no-op if {@link #_modification} is {@code null}.
     * </p>
     */
    public void startModification() {
        if (_modification == null) return;

        IContainer container = _modification.getContainer();
        Point smp = _modification.getStartMousePoint();

        _modification.setType(ModificationType.DRAG); // defensive default

        if (inRotatePoint(container, smp)) {
            _modification.setType(ModificationType.ROTATE);
            return;
        }

        int index = inSelectPoint(container, smp, true);
        if (index >= 0) {
            _modification.setSelectIndex(index);
            _modification.setType(ModificationType.RESIZE);
            return;
        }

        _modification.setType(ModificationType.DRAG);
    }

    /**
     * Update this item's geometry in response to a mouse movement during an
     * active modification.
     *
     * <p>Called repeatedly by the container's interaction layer while the
     * user is dragging, resizing, or rotating. The current mouse position
     * is available through {@link #_modification}. Subclasses implement
     * this to move, resize, or rotate their geometry accordingly.</p>
     */
    public abstract void modify();

    /**
     * Finalise the current modification, notify layer listeners, and clear
     * the modification record.
     *
     * <p>This method is a no-op if {@link #_modification} is {@code null}.
     * After notifying listeners the modification record is set to
     * {@code null} so subsequent calls are safe.</p>
     */
    public void stopModification() {
        if (_modification == null) return;

        switch (_modification.getType()) {
            case DRAG:
                _layer.notifyItemChangeListeners(this, ItemChangeType.MOVED);
                break;
            case ROTATE:
                _layer.notifyItemChangeListeners(this, ItemChangeType.ROTATED);
                break;
            case RESIZE:
                _layer.notifyItemChangeListeners(this, ItemChangeType.RESIZED);
                break;
        }
        _modification = null;
    }

    // -----------------------------------------------------------------------
    // Behavior flags — getters and setters
    // -----------------------------------------------------------------------

    /**
     * Return {@code true} if this item is visible.
     *
     * @return the visibility flag
     */
    @Override
    public boolean isVisible() { return _visible; }

    /**
     * Set the visibility of this item. Invisible items are completely skipped
     * by the drawing pipeline, including their selection handles.
     *
     * @param visible {@code true} to make the item visible
     */
    @Override
    public void setVisible(boolean visible) { _visible = visible; }

    /**
     * Return {@code true} if this item can be dragged.
     *
     * <p>Returns {@code false} if the item is locked, regardless of the
     * individual drag flag.</p>
     *
     * @return {@code true} if the item can be dragged
     */
    public boolean isDraggable() {
        return !_locked && _draggable;
    }

    /**
     * Set whether this item can be dragged.
     *
     * @param draggable {@code true} to allow dragging
     */
    public void setDraggable(boolean draggable) { _draggable = draggable; }

    /**
     * Return {@code true} if this item can be deleted.
     *
     * <p>Returns {@code false} if the item is locked.</p>
     *
     * @return {@code true} if the item can be deleted
     */
    public boolean isDeletable() {
        return !_locked && _deletable;
    }

    /**
     * Set whether this item can be deleted.
     *
     * @param deletable {@code true} to allow deletion
     */
    public void setDeletable(boolean deletable) { _deletable = deletable; }

    /**
     * Return {@code true} if this item can be resized.
     *
     * <p>Returns {@code false} if the item is locked.</p>
     *
     * @return {@code true} if the item can be resized
     */
    public boolean isResizable() {
        return !_locked && _resizable;
    }

    /**
     * Set whether this item can be resized.
     *
     * @param resizable {@code true} to allow resizing
     */
    public void setResizable(boolean resizable) { _resizable = resizable; }

    /**
     * Return {@code true} if this item can be rotated.
     *
     * <p>Returns {@code false} if the item is locked.</p>
     *
     * @return {@code true} if the item can be rotated
     */
    public boolean isRotatable() {
        return !_locked && _rotatable;
    }

    /**
     * Set whether this item can be rotated.
     *
     * @param rotatable {@code true} to allow rotation
     */
    public void setRotatable(boolean rotatable) { _rotatable = rotatable; }

    /**
     * Return {@code true} if this item can be connected to other items.
     *
     * @return {@code true} if connectable
     */
    public boolean isConnectable() { return _connectable; }

    /**
     * Set whether this item can be connected to other items.
     *
     * @param connectable {@code true} to allow connections
     */
    public void setConnectable(boolean connectable) {
        _connectable = connectable;
    }

    /**
     * Return {@code true} if this item responds to double-click gestures.
     *
     * @return {@code true} if double-clickable
     */
    public boolean isDoubleClickable() { return _doubleClickable; }

    /**
     * Set whether this item responds to double-click gestures.
     *
     * @param doubleClickable {@code true} to enable double-click response
     */
    public void setDoubleClickable(boolean doubleClickable) {
        _doubleClickable = doubleClickable;
    }

    /**
     * Return {@code true} if this item can be selected.
     *
     * @return {@code true} if selectable
     */
    public boolean isSelectable() { return _selectable; }

    /**
     * Set whether this item can be selected.
     *
     * @param selectable {@code true} to allow selection
     */
    public void setSelectable(boolean selectable) {
        _selectable = selectable;
    }

    /**
     * Return {@code true} if this item is locked.
     *
     * <p>A locked item ignores drag, resize, rotate, and delete operations
     * regardless of its individual behavior flags. See the class-level
     * documentation for the default-locked note.</p>
     *
     * @return {@code true} if the item is locked
     */
    public boolean isLocked() { return _locked; }

    /**
     * Set the locked state of this item.
     *
     * @param locked {@code true} to lock the item; {@code false} to enable
     *               interactive behavior
     */
    public void setLocked(boolean locked) { _locked = locked; }

    /**
     * Return {@code true} if this item responds to right-click gestures.
     *
     * @return {@code true} if right-clickable
     */
    public boolean isRightClickable() { return _rightClickable; }

    /**
     * Set whether this item responds to right-click gestures.
     *
     * @param rightClickable {@code true} to enable right-click response
     */
    public void setRightClickable(boolean rightClickable) {
        _rightClickable = rightClickable;
    }

    /**
     * Return {@code true} if this item is currently selected.
     *
     * @return {@code true} if selected
     */
    public boolean isSelected() { return _selected; }

    /**
     * Set the selected state of this item and request a container refresh.
     *
     * @param selected {@code true} to select the item
     */
    public void setSelected(boolean selected) {
        _selected = selected;
        _layer.getContainer().refresh();
    }

    /**
     * Return {@code true} if this item is enabled.
     *
     * <p>Disabled items are inert — they do not respond to interaction — and
     * may be rendered differently (e.g. ghosted).</p>
     *
     * @return {@code true} if the item is enabled
     */
    @Override
    public boolean isEnabled() { return _enabled; }

    /**
     * Set whether this item is enabled.
     *
     * @param enabled {@code true} to enable the item
     */
    @Override
    public void setEnabled(boolean enabled) { _enabled = enabled; }

    /**
     * Return {@code true} if this item's cached rendering data is stale.
     *
     * <p>When dirty, the item must be redrawn from scratch on the next paint
     * cycle. Items that cache intermediate geometry use this flag to decide
     * whether to recompute.</p>
     *
     * @return {@code true} if the item is dirty
     */
    public boolean isDirty() { return _dirty; }

    /**
     * Set whether this item's cached rendering data is stale.
     *
     * <p>Setting dirty to {@code true} also clears {@link #_lastDrawnPolygon}
     * so that subsequent hit-testing falls back to the bounding rectangle
     * rather than stale polygon data.</p>
     *
     * @param dirty {@code true} to mark the item dirty
     */
    @Override
    public void setDirty(boolean dirty) {
        _dirty = dirty;
        if (dirty) _lastDrawnPolygon = null;
    }

    /**
     * Return {@code true} if this item is "trackable" — that is, eligible to
     * receive drag, resize, or rotate gestures.
     *
     * <p>An item is trackable only when it is unlocked, enabled, and at least
     * one of draggable, rotatable, or resizable is set.</p>
     *
     * @return {@code true} if the item should be tracked during mouse gestures
     */
    public boolean isTrackable() {
        if (_locked || !_enabled) return false;
        return _rotatable || _draggable || _resizable;
    }

    // -----------------------------------------------------------------------
    // Resize policy
    // -----------------------------------------------------------------------

    /**
     * Return the resize policy applied when this item is resized
     * interactively.
     *
     * @return the current {@link ResizePolicy}
     */
    public ResizePolicy getResizePolicy() { return _resizePolicy; }

    /**
     * Set the resize policy applied when this item is resized interactively.
     *
     * @param resizePolicy the new policy; must not be {@code null}
     */
    public void setResizePolicy(ResizePolicy resizePolicy) {
        _resizePolicy = resizePolicy;
    }

    // -----------------------------------------------------------------------
    // Azimuth (rotation angle)
    // -----------------------------------------------------------------------

    /**
     * Return the reference rotation angle in degrees, in the range
     * {@code (-180, 180]}.
     *
     * @return the azimuth in degrees
     */
    public double getAzimuth() { return _azimuth; }

    /**
     * Set the reference rotation angle and normalise it to
     * {@code (-180, 180]}.
     *
     * @param azimuth the new azimuth in degrees (any value is accepted and
     *                will be normalised)
     */
    public void setAzimuth(double azimuth) {
        _azimuth = azimuth;
        while (_azimuth >  180.0) _azimuth -= 360.0;
        while (_azimuth < -180.0) _azimuth += 360.0;
    }

    // -----------------------------------------------------------------------
    // Display name
    // -----------------------------------------------------------------------

    /**
     * Return the human-readable display name of this item.
     *
     * @return the display name; never {@code null}
     */
    public String getDisplayName() { return _displayName; }

    /**
     * Set the human-readable display name of this item.
     *
     * @param name the new name; {@code null} is accepted but discouraged
     */
    public void setDisplayName(String name) { _displayName = name; }

    // -----------------------------------------------------------------------
    // Style
    // -----------------------------------------------------------------------

    /**
     * Return the drawing style for this item, creating a default
     * {@link Styled} instance if {@link #_style} is {@code null}.
     *
     * <p>This "safe" accessor is used internally (e.g. in
     * {@link #applyStyleProperties}) to avoid null-pointer exceptions if the
     * style was cleared by {@link #prepareForRemoval()}. Prefer
     * {@link #getStyle()} in normal usage.</p>
     *
     * @return the style object; never {@code null}
     */
    public IStyled getStyleSafe() {
        if (_style == null) _style = new Styled();
        return _style;
    }

    /**
     * Return the drawing style for this item.
     *
     * <p>Through this object fill color, line color, line style, line
     * width, symbol type, and symbol size can all be read and changed.</p>
     *
     * @return the style; may be {@code null} after {@link #prepareForRemoval}
     */
    public IStyled getStyle() { return _style; }

    /**
     * Replace the drawing style for this item.
     *
     * @param style the new style; {@code null} is accepted but will cause
     *              null-pointer exceptions unless {@link #getStyleSafe()} is
     *              used subsequently
     */
    public void setStyle(IStyled style) { _style = style; }

    // -----------------------------------------------------------------------
    // Container / layer / view
    // -----------------------------------------------------------------------

    /**
     * Return the {@link IContainer} that hosts this item.
     *
     * @return the container; may be {@code null} after
     *         {@link #prepareForRemoval}
     */
    public IContainer getContainer() {
        return getLayer().getContainer();
    }

    /**
     * Return the {@link Layer} on which this item lives.
     *
     * @return the layer; may be {@code null} after {@link #prepareForRemoval}
     */
    public Layer getLayer() { return _layer; }

    /**
     * Return the {@link BaseView} that owns the container hosting this item,
     * or {@code null} if the container or view is unavailable.
     *
     * @return the owning view, or {@code null}
     */
    public BaseView getView() {
        IContainer container = getContainer();
        return container != null ? container.getView() : null;
    }

    // -----------------------------------------------------------------------
    // Popup menu
    // -----------------------------------------------------------------------

    /**
     * Return the popup menu for this item, creating it lazily on first call.
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
     * <p>The default menu contains an item-ordering sub-menu and a
     * "Locked" checkbox that toggles {@link #isLocked()}. Subclasses may
     * override this method to add additional menu items, but should call
     * {@code super.createPopupMenu()} first and then add to the returned
     * menu.</p>
     *
     * @return the newly created popup menu (also stored in
     *         {@link #_popupMenu})
     */
    protected JPopupMenu createPopupMenu() {
        _popupMenu = new JPopupMenu();
        _popupMenu.add(ItemOrderingMenu.getItemOrderingMenu(this, true));

        JCheckBoxMenuItem cbitem =
                new JCheckBoxMenuItem("Locked", isLocked());
        cbitem.addItemListener(e -> {
            setLocked(cbitem.isSelected());
            if (isLocked()) {
                setSelected(false);
                IContainer cont = getLayer().getContainer();
                if (cont != null) getContainer().refresh();
            }
        });
        _popupMenu.add(cbitem);
        return _popupMenu;
    }

    /**
     * Show this item's popup menu at the given screen point.
     *
     * <p>Calls {@link #getPopupMenu()} (creating it lazily if needed) and
     * shows it relative to the container's component. This is a no-op if the
     * container is unavailable.</p>
     *
     * @param pp the screen point at which to show the menu; must not be
     *           {@code null}
     * @throws NullPointerException if {@code pp} is {@code null}
     */
    public void prepareForPopup(Point pp) {
        java.util.Objects.requireNonNull(pp, "Popup location cannot be null");
        IContainer container = getContainer();
        if (container == null) return;
        JPopupMenu menu = getPopupMenu();
        if (menu != null) {
            menu.show(container.getComponent(), pp.x, pp.y);
        }
    }

    // -----------------------------------------------------------------------
    // Double-click
    // -----------------------------------------------------------------------

    /**
     * Called when this item receives a double-click.
     *
     * <p>The default implementation is a no-op. Subclasses that want to
     * respond to double-clicks (e.g. to open an editor dialog) should
     * override this method.</p>
     *
     * @param mouseEvent the double-click event
     */
    public void doubleClicked(MouseEvent mouseEvent) {
        // default: nothing to do
    }

    // -----------------------------------------------------------------------
    // Feedback
    // -----------------------------------------------------------------------

    /**
     * Populate the feedback string list with any information this item wants
     * to contribute when the mouse is near it.
     *
     * <p>The default implementation is a no-op. Subclasses should override
     * to add status-bar or tooltip strings relevant to the item's state.</p>
     *
     * @param container       the container rendering this item
     * @param pp              the current mouse position in screen coordinates
     * @param wp              the corresponding world-coordinate position
     * @param feedbackStrings the list to add strings to
     */
    @Override
    public void getFeedbackStrings(IContainer container, Point pp,
            Point2D.Double wp, List<String> feedbackStrings) {
        // default: nothing to add
    }

    // -----------------------------------------------------------------------
    // Equality
    // -----------------------------------------------------------------------

    /**
     * Identity-based equality.
     *
     * <p>Two {@code AItem} references are equal only when they refer to the
     * same object ({@code this == o}). This is the correct semantic for items,
     * since each item is a unique graphical entity even if two items have
     * identical geometry and style.</p>
     *
     * @param o the object to compare
     * @return {@code true} only if {@code o} is this exact object
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof AItem) && (this == o);
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Prepare this item for removal from its layer.
     *
     * <p>Called by the layer when the item is being deleted. Nulls out all
     * heavyweight references (geometry, style, layer) to assist garbage
     * collection and prevent stale state from being accessed after removal.
     * After this call the item should not be used.</p>
     */
    @Override
    public void prepareForRemoval() {
        _focus            = null;
        _lastDrawnPolygon = null;
        _layer            = null;
        _path             = null;
        _secondaryPoints  = null;
        _style            = null;
    }
}