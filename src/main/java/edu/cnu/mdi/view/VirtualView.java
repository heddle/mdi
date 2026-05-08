package edu.cnu.mdi.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.graphics.drawable.IDrawable;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * A "virtual desktop" overview view.
 *
 * <p>A {@code VirtualView} displays a small thumbnail representation of each
 * open {@link BaseView} and supports a simple column-based virtual desktop:
 * internally-managed views can be arranged into {@code N} virtual columns
 * (cells). Clicking a column in the {@code VirtualView} shifts all views
 * horizontally so that the chosen column becomes the visible "current"
 * column.</p>
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li>This view needs a reference to the top-level {@link JFrame} in order
 *       to know the real window dimensions and respond to frame resizes.</li>
 *   <li>In Swing, the window ancestor is not reliably available inside the
 *       constructor. Therefore, the {@code JFrame} hook is installed in
 *       {@link #addNotify()} (when this component is actually attached to a
 *       realized containment hierarchy).</li>
 * </ul>
 *
 * <h2>Singleton</h2>
 * <p>
 * The singleton reference ({@link #_instance}) is assigned by the factory
 * method {@link #createVirtualView(int)} <em>after</em> the constructor
 * returns, so no code reachable from the constructor can observe a
 * partially-constructed instance through the singleton reference.
 * </p>
 */
@SuppressWarnings("serial")
public class VirtualView extends BaseView
        implements InternalFrameListener, IViewListener, MouseMotionListener, MouseListener {

    // ------------------------------------------------------------------------
    // Static/constant data
    // ------------------------------------------------------------------------

    /** Map of views to their component listeners (for geometry tracking). */
    private final Map<BaseView, ComponentListener> viewComponentListeners = new HashMap<>();

    /** Margin used when constraining view placement inside a column. */
    private static final int _SLOP = 10;

    /** Title shown in the internal frame. */
    private static final String VVTITLE = "Desktop";

    /** Background colors for the overview. */
    private static final Color _bg   = Color.gray;
    private static final Color _fill = X11Colors.getX11Color("alice blue");

    /** Virtual-window item color. */
    private static final Color _vwfill = new Color(255, 200, 120, 128);

    /**
     * Minimum height hack (prevents the internal frame from collapsing too
     * small).
     */
    private static final int MINHEIGHT = 60;

    // ------------------------------------------------------------------------
    // Column/constraint constants
    // ------------------------------------------------------------------------

    /** Constrain placement to upper-left of the target cell. */
    public static final int UPPERLEFT = 0;
    /** Constrain placement to upper-right of the target cell. */
    public static final int UPPERRIGHT = 1;
    /** Constrain placement to bottom-left of the target cell. */
    public static final int BOTTOMLEFT = 2;
    /** Constrain placement to bottom-right of the target cell. */
    public static final int BOTTOMRIGHT = 3;
    /** Constrain placement to top center of the target cell. */
    public static final int TOPCENTER = 4;
    /** Constrain placement to bottom center of the target cell. */
    public static final int BOTTOMCENTER = 5;
    /** Constrain placement to center of the target cell (special-cases). */
    public static final int CENTER = 6;
    /** Constrain placement to center-left of the target cell. */
    public static final int CENTERLEFT = 7;
    /** Constrain placement to center-right of the target cell. */
    public static final int CENTERRIGHT = 8;

    /**
     * Vertical correction (pixels) applied when placing a view with the
     * {@link #CENTERRIGHT} constraint.
     *
     * <p>Geometrically, centering a view vertically within a column places its
     * title bar near the mid-point of the desktop area, where it is easily
     * obscured by views docked in the upper half. This constant nudges the
     * view upward from the geometric centre so the title bar is fully visible
     * without manual repositioning.</p>
     *
     * <p>The value was chosen empirically on a 1080p display with default view
     * decoration height. It may need adjustment on high-DPI screens or when
     * custom decorations change the title-bar height significantly.</p>
     */
    private static final int CENTERRIGHT_VERTICAL_NUDGE_PX = -20;

    // ------------------------------------------------------------------------
    // Singleton access (convenience, application-wide)
    // ------------------------------------------------------------------------

    /**
     * Global singleton instance.
     *
     * <p>Assigned by {@link #createVirtualView(int)} after the constructor
     * returns so that no partially-constructed instance is ever visible to
     * other threads via this reference.</p>
     */
    private static VirtualView _instance;

    // ------------------------------------------------------------------------
    // Instance state
    // ------------------------------------------------------------------------

    /** Parent application frame; resolved lazily in {@link #addNotify()}. */
    private JFrame _parent;

    /** Views managed by this virtual desktop (all views except {@code this}). */
    private final ArrayList<BaseView> _views = new ArrayList<>();

    /**
     * Number of virtual columns (cells).
     *
     * <p>This field is {@code static} because the original design treated the
     * virtual desktop as a global singleton with a single column count shared
     * across the entire application. The field is set exactly once by
     * {@link #createVirtualView(int)} before the singleton instance is
     * published, so there is no write-race between that assignment and
     * subsequent reads.</p>
     *
     * <p>If multi-instance support is ever needed this should become a plain
     * instance field; the static declaration would then be incorrect.</p>
     */
    private static int _numcol = 8;

    /** Currently visible column. */
    private int _currentCol = 0;

    /** Column offsets in world coordinates (used when shifting all views). */
    private Point[] _offsets = new Point[_numcol];

    /** Scratch world point used during mouse motion. */
    private final Point2D.Double _wp = new Point2D.Double();

    // ------------------------------------------------------------------------
    // Parent-frame hook bookkeeping (prevents constructor-time ancestor issues)
    // ------------------------------------------------------------------------

    /** True once we have attached a listener to the parent frame. */
    private boolean parentHooked = false;

    /**
     * Listener installed on the parent frame to keep the virtual desktop in
     * sync with frame size.
     */
    private ComponentListener parentResizeListener;

    // ------------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------------

    /**
     * Create the virtual desktop view.
     *
     * <p>Use {@link #createVirtualView(int)} to instantiate; do not call this
     * constructor directly. The singleton reference ({@link #_instance}) is
     * intentionally <em>not</em> assigned here — it is set by the factory
     * method after this constructor returns, ensuring that no code path
     * reachable from the constructor body can observe a partially-constructed
     * instance through the singleton reference (a "this-escape" hazard).</p>
     *
     * @param keyVals variable set of property key-value arguments
     */
    private VirtualView(Object... keyVals) {
        super(PropertyUtils.fromKeyValues(keyVals));

        // Track views added/removed after we are created.
        ViewManager.getInstance().addViewListener(this);
        
        // VirtualView is a fixed, non-interactive overview; disable all internal frame interactions.
        setMaximizable(false);
        setIconifiable(false);
        setResizable(false);
        setClosable(false);


        // Add items representing currently existing views.
        addItems();

        // Visual defaults.
        setBackground(_bg);
        getIContainer().getComponent().setBackground(_bg);

        // Mouse interactions (hover title, column switch on click).
        getIContainer().getComponent().addMouseMotionListener(this);
        getIContainer().getComponent().addMouseListener(this);

        // Initial offsets based on the current (possibly placeholder) world system.
        setOffsets();

        // Drawers that highlight current column and show debug dividers.
        setBeforeDraw();
        setAfterDraw();

        // Headless pointer to drag VirtualWindowItems.
        new HeadlessPointerTool(this);

        // _instance is NOT assigned here — see createVirtualView().
    }

    /**
     * Create and show a new {@link VirtualView}.
     *
     * <p>The view is created with a small fixed pixel size (thumbnail-style),
     * and its world system is sized to {@code numCols × maxScreenWidth}.</p>
     *
     * <p>The parent frame is resolved later in {@link #addNotify()} so it is
     * safe to call this factory before the application frame is visible.</p>
     *
     * <p><strong>Singleton assignment:</strong> {@link #_instance} is
     * published here, <em>after</em> {@code new VirtualView(...)} returns.
     * This guarantees that no code reachable from the constructor can observe
     * a partially-constructed instance through the singleton reference.</p>
     *
     * @param numcol number of virtual columns; clamped to at least 1
     * @return the newly created virtual desktop view; never {@code null}
     */
    public static VirtualView createVirtualView(int numcol) {

        _numcol = Math.max(1, numcol);

        Rectangle2D.Double world = getWorld();

        // Small "overview" internal frame size: N tiny columns.
        int cell_width  = 40;
        int cell_height = 1 + ((9 * cell_width) / 16);
        int width       = _numcol * cell_width;
        int height      = cell_height;

        // OS-dependent decoration quirks (legacy behavior).
        if (Environment.getInstance().isLinux())   { height += 23; }
        if (Environment.getInstance().isWindows()) { height += 23; }

        VirtualView view = new VirtualView(
                PropertyUtils.WORLDSYSTEM,             world,
                PropertyUtils.WIDTH,                   width,
                PropertyUtils.HEIGHT,                  height,
                PropertyUtils.VISIBLE,                 true,
                PropertyUtils.BACKGROUND,              Color.white,
                PropertyUtils.TITLE,                   VVTITLE);

        view._offsets = new Point[_numcol];

        Insets insets = view.getInsets();
        view.setSize(width, height + insets.top);
        view.setLocation(0, 0);

        // Publish the singleton reference only after construction is complete,
        // so no partially-constructed instance is ever visible to other threads.
        _instance = view;

        return view;
    }

    /**
     * Public access to the singleton virtual view.
     *
     * @return the current virtual view instance, or {@code null} if none has
     *         been created
     */
    public static VirtualView getInstance() {
        return _instance;
    }

    // ------------------------------------------------------------------------
    // Lifecycle — addNotify / removeNotify
    // ------------------------------------------------------------------------

    /**
     * Called when this component is added to a realized containment hierarchy.
     *
     * <p>This is the correct place to safely resolve the top-level window
     * ancestor and install a resize listener on the parent {@link JFrame}.
     * Attempting to access the window ancestor in the constructor is
     * unreliable.</p>
     */
    @Override
    public void addNotify() {
        super.addNotify();
        adjustTitleFont();
        if (parentHooked) {
            return;
        }

        final Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof JFrame) {
            _parent = (JFrame) w;

            parentResizeListener = new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    reconfigure();
                }
            };
            _parent.addComponentListener(parentResizeListener);

            parentHooked = true;
            reconfigure();
        }
    }

    /**
     * Called when this component is removed from the UI hierarchy.
     *
     * <p>Removes listeners that were installed on the parent frame to avoid
     * leaks.</p>
     */
    @Override
    public void removeNotify() {
        if (parentHooked && _parent != null && parentResizeListener != null) {
            _parent.removeComponentListener(parentResizeListener);
        }
        parentHooked = false;
        parentResizeListener = null;
        _parent = null;

        super.removeNotify();
    }

    // ------------------------------------------------------------------------
    // Title font
    // ------------------------------------------------------------------------

    /**
     * Reduce the title bar font size for the virtual view.
     *
     * <p>The VirtualView is intended to be visually unobtrusive, so its title
     * is rendered slightly smaller than standard views.</p>
     */
    private void adjustTitleFont() {
        javax.swing.plaf.InternalFrameUI ifui = getUI();
        if (!(ifui instanceof javax.swing.plaf.basic.BasicInternalFrameUI ui)) {
            return;
        }

        java.awt.Component north = ui.getNorthPane();
        if (!(north instanceof javax.swing.JComponent northPane)) {
            return;
        }

        java.awt.Font newFont = Fonts.boldFontDelta(-3);
        setFontRecursively(northPane, newFont);
        northPane.revalidate();
        northPane.repaint();
    }

    private static void setFontRecursively(java.awt.Component c, java.awt.Font f) {
        c.setFont(f);

        if (c instanceof javax.swing.JLabel lbl) {
            lbl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            lbl.setFont(f);
        }

        if (c instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                setFontRecursively(child, f);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Reconfiguration
    // ------------------------------------------------------------------------

    /**
     * Reconfigure the virtual desktop world system and update all virtual
     * window items.
     *
     * <p>This should be called when the parent application frame size changes.
     * It updates the virtual desktop's world width to be
     * {@code numCols * frameWidth} (one frame-width per virtual column) and
     * updates the stored per-column offsets.</p>
     */
    public void reconfigure() {

        if (_parent == null) {
            return;
        }

        final Dimension d = _parent.getSize();

        final int width  = _numcol * d.width;
        final int height = d.height;

        Rectangle2D.Double ws = getIContainer().getWorldSystem();
        ws.width  = width;
        ws.height = height;

        setOffsets();

        for (BaseView view : _views) {
            if (view.getVirtualItem() != null) {
                view.getVirtualItem().setLocation();
            }
        }
    }

    /**
     * Compute the per-column world offsets used when switching the current
     * column.
     *
     * <p>Offsets are stored as points for historical reasons; only the X
     * component is used when shifting views.</p>
     */
    private void setOffsets() {
        Rectangle2D.Double world = getIContainer().getWorldSystem();
        double dx = world.width / _numcol;
        double dy = world.height;

        for (int col = 0; col < _numcol; col++) {
            _offsets[col] = new Point((int) (col * dx), (int) (dy));
        }
    }

    // ------------------------------------------------------------------------
    // Draw hooks
    // ------------------------------------------------------------------------

    /**
     * Configure a "before draw" hook to highlight the current column.
     */
    private void setBeforeDraw() {
        IDrawable beforeDraw = new DrawableAdapter() {
            @Override
            public void draw(Graphics2D g, IContainer container) {
                Rectangle cr = getColRect(_currentCol);
                g.setColor(_fill);
                g.fillRect(cr.x + 1, cr.y + 1, cr.width - 2, cr.height - 2);
            }
        };

        getIContainer().setBeforeDraw(beforeDraw);
    }

    /**
     * Configure an "after draw" hook.
     *
     * <p>This currently draws debug column dividers and outlines. You may want
     * to remove or theme these for production.</p>
     */
    private void setAfterDraw() {
        IDrawable afterDraw = new DrawableAdapter() {
            @Override
            public void draw(Graphics2D g, IContainer container) {

                Rectangle b = container.getComponent().getBounds();
                Rectangle2D.Double world = getIContainer().getWorldSystem();
                Point2D.Double wp = new Point2D.Double();
                Point pp = new Point();
                double dx = world.width / _numcol;

                g.setColor(Color.red);
                wp.y = world.y + world.height / 2;
                for (int i = 1; i < _numcol; i++) {
                    wp.x = i * dx + 1;
                    container.worldToLocal(pp, wp);
                    g.drawLine(pp.x, 0, pp.x, b.height);
                }

                g.setColor(Color.red);
                g.drawRect(0, 0, b.width, b.height);
            }
        };

        getIContainer().setAfterDraw(afterDraw);
    }

    // ------------------------------------------------------------------------
    // View tracking
    // ------------------------------------------------------------------------

    /**
     * Add virtual window items for all current views (except this
     * {@link VirtualView}).
     */
    private void addItems() {
        for (BaseView view : ViewManager.getInstance()) {
            addView(view);
        }
    }

    /**
     * Returns the horizontal pixel offset currently applied to all managed
     * views — that is, how far left they have been shifted from their
     * column-0 positions.
     *
     * <p>This is used by {@link edu.cnu.mdi.desktop.Desktop#writeConfigurationFile()}
     * to normalise saved view bounds back to column-0 coordinates, so that
     * they restore correctly regardless of which column was active when the
     * layout was saved.</p>
     *
     * @return current horizontal pixel offset (non-negative; zero when at
     *         column 0)
     */
    public int getCurrentColumnPixelOffset() {
        if (_offsets == null || _currentCol < 0 || _currentCol >= _offsets.length
                || _offsets[_currentCol] == null) {
            return 0;
        }
        return _offsets[_currentCol].x;
    }

    /**
     * @return number of virtual columns (cells)
     */
    public int getNumCol() {
        return _numcol;
    }

    /**
     * Compute an initial world system based on the largest monitor.
     *
     * <p>The virtual desktop uses a single world width that is
     * {@code numCols * maxMonitorWidth}. The parent frame size will later
     * refine the effective visible column width via {@link #reconfigure()}.</p>
     */
    private static Rectangle2D.Double getWorld() {
        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = g.getScreenDevices();

        int maxw = 0;
        int maxh = 0;

        for (GraphicsDevice element : devices) {
            maxw = Math.max(maxw, element.getDisplayMode().getWidth());
            maxh = Math.max(maxh, element.getDisplayMode().getHeight());
        }

        int width  = _numcol * maxw;
        int height = maxh;
        return new Rectangle2D.Double(0, 0, width, height);
    }

    // ------------------------------------------------------------------------
    // InternalFrameListener: keep thumbnails in sync with view lifecycle
    // ------------------------------------------------------------------------

    @Override
    public void internalFrameOpened(InternalFrameEvent e) {
        Object source = e.getSource();
        if (source instanceof BaseView) {
            BaseView view = (BaseView) source;
            if (view.getVirtualItem() != null) {
                view.getVirtualItem().setLocation();
                view.getVirtualItem().setVisible(true);
            }
        }
    }

    @Override
    public void internalFrameClosing(InternalFrameEvent e) {
        // no-op
    }

    @Override
    public void internalFrameClosed(InternalFrameEvent e) {
        Object source = e.getSource();
        if (source instanceof BaseView) {
            BaseView view = (BaseView) source;
            if (view.getVirtualItem() != null) {
                view.getVirtualItem().setVisible(false);
            }
        }
    }

    @Override
    public void internalFrameIconified(InternalFrameEvent e) {
        Object source = e.getSource();
        if (source instanceof BaseView) {
            BaseView view = (BaseView) source;
            if (view.getVirtualItem() != null) {
                view.getVirtualItem().setVisible(false);
            }
        }
    }

    @Override
    public void internalFrameDeiconified(InternalFrameEvent e) {
        Object source = e.getSource();
        if (source instanceof BaseView) {
            BaseView view = (BaseView) source;
            if (view.getVirtualItem() != null) {
                view.getVirtualItem().setVisible(true);
            }
        }
    }

    @Override
    public void internalFrameActivated(InternalFrameEvent e) {
        Object source = e.getSource();
        if (source instanceof BaseView) {
            BaseView view = (BaseView) source;
            if (view.getVirtualItem() != null) {
                view.getVirtualItem().setVisible(true);
                getIContainer().getAnnotationLayer().sendToFront(view.getVirtualItem());
            }
        }
    }

    @Override
    public void internalFrameDeactivated(InternalFrameEvent e) {
        Object source = e.getSource();
        if (source instanceof BaseView) {
            BaseView view = (BaseView) source;
            if (view.getVirtualItem() != null) {
                view.getVirtualItem().setVisible(true);
            }
        }
    }

    // ------------------------------------------------------------------------
    // IViewListener: respond to views added/removed from ViewManager
    // ------------------------------------------------------------------------

    @Override
    public void viewAdded(BaseView view) {
        addView(view);
    }

    @Override
    public void viewRemoved(BaseView view) {
        if (_views.contains(view)) {
            view.removeInternalFrameListener(this);

            if (view.getVirtualItem() != null) {
                getIContainer().getAnnotationLayer().remove(view.getVirtualItem());
            }

            _views.remove(view);

            ComponentListener cl = viewComponentListeners.remove(view);
            if (cl != null) {
                view.removeComponentListener(cl);
            }
        }
    }

    /**
     * Add a view and create the {@link VirtualWindowItem} that represents it.
     *
     * <p>This does not add {@code this} view, and will not add duplicates.</p>
     *
     * @param view the view to manage
     */
    private void addView(BaseView view) {

        if (view == null || view == this || _views.contains(view)) {
            return;
        }

        _views.add(view);

        final VirtualWindowItem vitem = new VirtualWindowItem(this, view);
        vitem.getStyle().setFillColor(_vwfill);
        vitem.getStyle().setLineColor(Color.black);

        view.addInternalFrameListener(this);

        ComponentListener cl = new ComponentListener() {
            @Override public void componentResized(ComponentEvent e) { vitem.setLocation(); }
            @Override public void componentMoved  (ComponentEvent e) { vitem.setLocation(); }
            @Override public void componentShown  (ComponentEvent e) { vitem.setLocation(); }
            @Override public void componentHidden (ComponentEvent e) { vitem.setLocation(); }
        };

        viewComponentListeners.put(view, cl);
        view.addComponentListener(cl);
    }

    // ------------------------------------------------------------------------
    // Mouse handlers: hover title + click to change current column
    // ------------------------------------------------------------------------

    @Override
    public void mouseDragged(MouseEvent e) {
        // no-op (reserved for future drag interactions)
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        getIContainer().localToWorld(e.getPoint(), _wp);

        AItem item = getIContainer().getItemAtPoint(e.getPoint());
        if (item instanceof VirtualWindowItem) {
            VirtualWindowItem vvi = (VirtualWindowItem) item;
            setTitle(vvi.getBaseView().getTitle());
        } else {
            setTitle(VVTITLE);
        }
    }

    /**
     * Virtual view: no offsetting.
     *
     * <p>A {@code VirtualView} itself stays fixed; it offsets other views.</p>
     */
    @Override
    public void offset(int dh, int dv) {
        // no-op
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        switch (mouseEvent.getButton()) {
        case MouseEvent.BUTTON1:
            if (mouseEvent.getClickCount() == 1) {
                handleClick(mouseEvent);
            }
            return;
        default:
            return;
        }
    }

    /**
     * Go to a specific column, making it visible.
     *
     * @param col the column to go to
     */
    public void gotoColumn(int col) {

        if ((col == _currentCol) || (col < 0) || (col >= _numcol)) {
            return;
        }

        int dh = _offsets[_currentCol].x - _offsets[col].x;
        int dv = 0; // can't do dv because internal frames are not allowed at negative y

        for (BaseView view : _views) {
            view.offset(dh, dv);
        }

        _currentCol = col;
    }

    /**
     * Handle a click on the virtual desktop.
     *
     * <p>The clicked column becomes the current column by horizontally
     * shifting all views.</p>
     */
    private void handleClick(MouseEvent mouseEvent) {
        Point rc = getRowCol(mouseEvent.getPoint());
        int clickCol = rc.x;
        if (clickCol == _currentCol) {
            return;
        }

        int dh = _offsets[_currentCol].x - _offsets[clickCol].x;
        int dv = 0;

        for (BaseView view : _views) {
            view.offset(dh, dv);
        }

        _currentCol = clickCol;
    }

    /**
     * Compute the screen-space rectangle corresponding to a virtual column.
     *
     * @param col the column index
     * @return the column rectangle in local (screen) coordinates
     */
    private Rectangle getColRect(int col) {

        Rectangle2D.Double world = getIContainer().getWorldSystem();
        double dx = world.width / _numcol;
        double dy = world.height;

        double x = col * dx;
        double y = world.y + world.height - dy;

        Rectangle2D.Double wr = new Rectangle2D.Double(x, y, dx, dy);
        Rectangle r = new Rectangle();
        getIContainer().worldToLocal(r, wr);
        return r;
    }

    @Override public void mousePressed (MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered (MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) {
        setTitle(VVTITLE);
    }

    /**
     * Convert a local (screen) point into a (col, row) within the virtual
     * desktop.
     *
     * <p>Current implementation uses only columns; row is always 0.</p>
     *
     * @param p local (screen) point
     * @return point where {@code x = column}, {@code y = row (always 0)}
     */
    private Point getRowCol(Point p) {

        Point2D.Double wp = new Point2D.Double();
        getIContainer().localToWorld(p, wp);

        Rectangle2D.Double world = getIContainer().getWorldSystem();
        double dx = world.width / _numcol;

        int col = (int) (wp.x / dx);
        col = Math.max(0, Math.min(_numcol - 1, col));

        return new Point(col, 0);
    }

    /**
     * Total world offset corresponding to the current virtual column.
     *
     * @return offset in world coordinates; {@code x = currentCol*dx, y = 0}
     */
    public Point2D.Double totalOffset() {
        Rectangle2D.Double world = getIContainer().getWorldSystem();
        double dx = world.width / _numcol;
        return new Point2D.Double(_currentCol * dx, 0);
    }

    /**
     * Move a view to the centre of a specific virtual column.
     *
     * @param view the view to move; ignored if {@code null} or not managed
     * @param col  the target column index
     */
    public void moveTo(BaseView view, int col) {

        if (view == null) {
            return;
        }

        col = Math.max(0, Math.min(col, (_numcol - 1)));

        if (!_views.contains(view)) {
            return;
        }

        Rectangle2D.Double world = getIContainer().getWorldSystem();
        double dx = world.width / _numcol;
        double dy = world.height;

        double x = (col - _currentCol) * dx;
        double y = world.y + world.height - dy;

        int xc = (int) (x + dx / 2);
        int yc = (int) (y + dy / 2);

        Rectangle bounds = view.getBounds();
        int delx = xc - (bounds.x + bounds.width  / 2);
        int dely = (yc - 40) - (bounds.y + bounds.height / 2);

        view.offset(delx, dely);
    }

    /**
     * Estimate the virtual column containing a given view.
     *
     * <p>This uses the view's current bounds and the current virtual column
     * width.</p>
     *
     * @param view the view
     * @return the column index, clamped to {@code [0, numCols-1]}
     */
    public int getViewColumn(BaseView view) {

        Rectangle2D.Double world = getIContainer().getWorldSystem();
        double dx = world.width / _numcol;
        Rectangle bounds = view.getBounds();
        double xc = bounds.getCenterX();

        int col = _currentCol + (int) (xc / dx);

        if (xc < 0) {
            col -= 1;
        }

        col = Math.max(0, Math.min(col, (_numcol - 1)));
        return col;
    }

    /**
     * Returns the currently visible column index.
     *
     * @return current column index
     */
    public int getCurrentColumn() {
        return _currentCol;
    }

    /**
     * Move a view to a specific virtual cell using a placement constraint,
     * with no additional pixel offsets.
     *
     * @param view       the view to move
     * @param col        the target column
     * @param constraint placement constraint constant (e.g. {@link #UPPERLEFT})
     */
    public void moveTo(BaseView view, int col, int constraint) {
        moveTo(view, col, 0, 0, constraint);
    }

    /**
     * Move a view to a specific column, applying a placement constraint and
     * additional pixel offsets.
     *
     * <p>The constraint constants ({@link #UPPERLEFT}, {@link #UPPERRIGHT},
     * {@link #BOTTOMLEFT}, {@link #BOTTOMRIGHT}, {@link #TOPCENTER},
     * {@link #BOTTOMCENTER}, {@link #CENTERLEFT}, {@link #CENTERRIGHT}) place
     * the view at the corresponding position within the target cell. The
     * special value {@link #CENTER} delegates to {@link #moveTo(BaseView, int)}
     * for centred placement.</p>
     *
     * @param view       the view to move; ignored if {@code null} or not managed
     * @param col        the target column index
     * @param delh       additional horizontal pixel offset applied after the
     *                   constraint position is computed
     * @param delv       additional vertical pixel offset applied after the
     *                   constraint position is computed
     * @param constraint placement constraint constant
     */
    public void moveTo(BaseView view, int col, int delh, int delv, int constraint) {

        if (constraint == CENTER) {
            moveTo(view, col);
            return;
        }

        col = Math.max(0, Math.min(col, (_numcol - 1)));

        if (!_views.contains(view)) {
            return;
        }

        Rectangle2D.Double world = getIContainer().getWorldSystem();
        double dx = world.width / _numcol;
        double dy = world.height;

        double left   = (col - _currentCol) * dx;
        double top    = world.y + world.height - dy;
        double right  = left + dx;
        double bottom = top + dy;

        Rectangle bounds = view.getBounds();
        int x0 = bounds.x;
        int y0 = bounds.y;
        int dh = 0;
        int dv = 0;

        if (constraint == UPPERRIGHT) {
            int xf = (int) (right - bounds.width - 2 * _SLOP);
            int yf = (int) (top + _SLOP);
            dh = xf - x0;
            dv = yf - y0;
        } else if (constraint == UPPERLEFT) {
            int xf = (int) (left + _SLOP);
            int yf = (int) (top + _SLOP);
            dh = xf - x0;
            dv = yf - y0;
        } else if (constraint == BOTTOMLEFT) {
            int xf = (int) (left + _SLOP);
            int yf = (int) (bottom - bounds.height - 7 * _SLOP);
            dh = xf - x0;
            dv = yf - y0;
        } else if (constraint == BOTTOMRIGHT) {
            int xf = (int) (right - bounds.width - 2 * _SLOP);
            int yf = (int) (bottom - bounds.height - 7 * _SLOP);
            dh = xf - x0;
            dv = yf - y0;
        } else if (constraint == TOPCENTER) {
            int xf = (int) (left + right - bounds.width - _SLOP) / 2;
            int yf = (int) (top + _SLOP);
            dh = xf - x0;
            dv = yf - y0;
        } else if (constraint == BOTTOMCENTER) {
            int xf = (int) (left + right - bounds.width - _SLOP) / 2;
            int yf = (int) (bottom - bounds.height - 7 * _SLOP);
            dh = xf - x0;
            dv = yf - y0;
        } else if (constraint == CENTERLEFT) {
            int xf = (int) (left);
            dh = xf - x0;
        } else if (constraint == CENTERRIGHT) {
            int xf = (int) (right - bounds.width - _SLOP);
            dh = xf - x0;
            // Nudge upward so the title bar clears views in the upper half of
            // the column. See CENTERRIGHT_VERTICAL_NUDGE_PX for rationale.
            dv = CENTERRIGHT_VERTICAL_NUDGE_PX;
        }

        view.offset(dh + delh, dv + delv);
    }

    /**
     * Activate the virtual column containing the given view so that it
     * becomes visible.
     *
     * <p>This uses the view's {@link VirtualWindowItem} bounds inside the
     * virtual desktop.</p>
     *
     * @param view the view
     */
    public void activateViewCell(BaseView view) {

        if (view == null || view.getVirtualItem() == null) {
            return;
        }

        Rectangle b = view.getVirtualItem().getBounds(getIContainer());
        Point pp = new Point(b.x + b.width / 2, b.y + b.height / 2);
        Point rc = getRowCol(pp);

        int col = Math.max(0, Math.min(_numcol - 1, rc.x));
        if (col == _currentCol) {
            return;
        }

        int dh = _offsets[_currentCol].x - _offsets[col].x;
        int dv = 0;

        for (BaseView bview : _views) {
            bview.offset(dh, dv);
        }

        _currentCol = col;
    }

    /**
     * Crude visibility test: whether the view's bounds intersect the
     * application's current frame bounds.
     *
     * @param view the view to check
     * @return {@code true} if the view appears to be visible
     */
    public boolean isViewVisible(BaseView view) {
        if (view == null || view.isIcon() || _parent == null) {
            return false;
        }

        Rectangle b = view.getBounds();
        Dimension d = _parent.getSize();
        Rectangle c = new Rectangle(0, 0, d.width, d.height);
        return b.intersects(c);
    }

    /**
     * Enforce a minimum internal frame height.
     */
    @Override
    public void componentResized(ComponentEvent arg0) {
        Dimension size = getSize();
        if (size.height < MINHEIGHT) {
            size.height = MINHEIGHT;
            setSize(size);
        }
        super.componentResized(arg0);
    }
}