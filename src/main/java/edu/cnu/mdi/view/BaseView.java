package edu.cnu.mdi.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyVetoException;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import edu.cnu.mdi.component.MagnifyWindow;
import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.DrawingContainer;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.desktop.Desktop;
import edu.cnu.mdi.format.DoubleFormat;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.toolbar.ToolBarBits;
import edu.cnu.mdi.properties.PropertySupport;
import edu.cnu.mdi.ui.menu.ViewPopupMenu;

/**
 * Base class for all mdi "views".
 * <p>
 * A view is implemented as a {@link JInternalFrame} hosted on the shared
 * application {@link Desktop}. Most views wrap an {@link IContainer} which
 * provides world and local coordinate systems and optional drawing behavior.
 * Other views (e.g. log views) do not require a container and simply use the
 * internal frame content area directly.
 * </p>
 * <p>
 * Instances of {@code BaseView} are typically created using a variable-length
 * list of key–value pairs built on {@link PropertySupport}. These properties
 * control:
 * </p>
 * <ul>
 *     <li>Basic frame attributes (title, size, location, visibility).</li>
 *     <li>Standard decorations (closable, iconifiable, resizable, maximizable).</li>
 *     <li>Container creation and configuration (world system, margins, background).</li>
 *     <li>Optional scroll pane and toolbar configuration.</li>
 * </ul>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class BaseView extends JInternalFrame
        implements FocusListener, MouseListener, ComponentListener {

    /**
     * Name used as a prefix for reading and writing persistent properties for this
     * view. This can be different from the visible frame title.
     */
    protected String VIEWPROPNAME = "?";

    // ------------------------------------------------------------------------
    // Simple stacking support for new views
    // ------------------------------------------------------------------------

    /** Last used left position when stacking newly created views. */
    private static int LASTLEFT = 0;

    /** Last used top position when stacking newly created views. */
    private static int LASTTOP = 0;

    /** Horizontal offset between subsequently created views. */
    private static final int DEL_H = 40;

    /** Vertical offset between subsequently created views. */
    private static final int DEL_V = 20;

    // ------------------------------------------------------------------------
    // Instance state
    // ------------------------------------------------------------------------

    /**
     * The parent application frame hosting the desktop, lazily resolved.
     */
    private JFrame parentFrame;

    /**
     * The desktop that owns this view (internal frame). Typically obtained from
     * {@link Desktop#getInstance()}.
     */
    private final JDesktopPane desktop;

    /**
     * The container used by this view, if any. Some views (such as a log view)
     * may not use a container.
     */
    private IContainer container;

    /**
     * The popup menu associated with this view (e.g. quick zoom menu).
     */
    private final ViewPopupMenu viewPopupMenu;

    /**
     * Properties originally used to construct this view, typically obtained from
     * {@link PropertySupport#fromKeyValues(Object...)}.
     */
    protected final Properties properties;

    /**
     * Optional virtual window item used when this view participates in a virtual
     * overview (miniature) display.
     */
    protected VirtualWindowItem virtualItem;

    /**
     * Optional scroll pane wrapping the container component.
     */
    private JScrollPane scrollPane;

    /**
     * The initial upper-left location that this view was placed at creation time.
     */
    private final Point startingLocation = new Point();

    // ========================================================================
    // Construction
    // ========================================================================
    
    public BaseView(Object... keyVals) {
       this(PropertySupport.fromKeyValues(keyVals));
    }

    /**
     * Constructs a new {@code BaseView}.
     * <p>
     * The constructor accepts an optional variable-length list of key–value
     * pairs. These are converted into a {@link Properties} instance via
     * {@link PropertySupport#fromKeyValues(Object...)}. Recognized properties
     * control frame decorations, world system, margins, background color, toolbar,
     * scrollability, initial size, and visibility.
     * </p>
     * <p>
     * Common keys include (but are not limited to):
     * </p>
     * <ul>
     *     <li>{@link PropertySupport#TITLE}</li>
     *     <li>{@link PropertySupport#WORLD_SYSTEM}</li>
     *     <li>{@link PropertySupport#SCROLLABLE}</li>
     *     <li>{@link PropertySupport#TOOLBAR}</li>
     *     <li>Margin keys such as {@link PropertySupport#LEFTMARGIN}</li>
     * </ul>
     *
     * @param keyVals an optional variable-length list of property name–value pairs.
     *                The number of arguments must be even (name, value,
     *                name, value, ...). If the length is odd, an error is
     *                reported to {@code System.err}.
     */
    public BaseView(Properties props) {

        properties = props;

        // create the view popup menu
        viewPopupMenu = new ViewPopupMenu(this);

        desktop = Desktop.getInstance();


        // --------------------------------------------------------------------
        // Read general view properties
        // --------------------------------------------------------------------

        String title = PropertySupport.getTitle(properties);

        // view decorations
        boolean standardDecorations = PropertySupport.getStandardViewDecorations(properties);
        boolean iconifiable = PropertySupport.getIconifiable(properties);
        boolean maximizable = PropertySupport.getMaximizable(properties);
        boolean resizable = PropertySupport.getResizable(properties);
        boolean closable = PropertySupport.getClosable(properties);

        // behavior flags
        boolean scrollable = PropertySupport.getScrollable(properties);
        boolean visible = PropertySupport.getVisible(properties);

        // apply frame properties
        setTitle((title != null) ? title : "A View");
        setIconifiable(standardDecorations || iconifiable);
        setMaximizable(standardDecorations || maximizable);
        setResizable(standardDecorations || resizable);
        setClosable(standardDecorations || closable);
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        VIEWPROPNAME = PropertySupport.getPropName(properties);

        int left = PropertySupport.getLeft(properties);
        int top = PropertySupport.getTop(properties);
        int width = PropertySupport.getWidth(properties);
        int height = PropertySupport.getHeight(properties);

        if (left < 1) {
            left = LASTLEFT;
            LASTLEFT += DEL_H;
        }
        if (top < 1) {
            top = LASTTOP;
            LASTTOP += DEL_V;
        }

        addMouseListener(this);
        addFocusListener(this);
        setFrameIcon(null);
        ViewManager.getInstance().add(this);

        // --------------------------------------------------------------------
        // Container creation and configuration
        // --------------------------------------------------------------------

        Rectangle2D.Double worldSystem = PropertySupport.getWorldSystem(properties);
        if (worldSystem != null) {
            // view includes a container
            setLocation(left, top);

            // container provided or default
            container = PropertySupport.getContainer(properties);
            if (container == null) {
                String contType = PropertySupport.getContainerType(properties);
                if ((contType != null) && contType.equalsIgnoreCase("drawing")) {
                    container = new DrawingContainer(this, worldSystem);
                } else {
                    container = new BaseContainer(this, worldSystem);
                }
            } else {
                container.setView(this);
            }

            if (container instanceof BaseContainer) {
                int lmargin = PropertySupport.getLeftMargin(properties);
                int tmargin = PropertySupport.getTopMargin(properties);
                int rmargin = PropertySupport.getRightMargin(properties);
                int bmargin = PropertySupport.getBottomMargin(properties);
                container.setLeftMargin(lmargin);
                container.setTopMargin(tmargin);
                container.setRightMargin(rmargin);
                container.setBottomMargin(bmargin);
            }

            // optional scroll pane
            if (scrollable && (container.getComponent() != null)) {
                scrollPane = new JScrollPane(container.getComponent());
            }

            // background color applies to the container
            Color background = PropertySupport.getBackground(properties);
            if (background != null && container.getComponent() != null) {
                container.getComponent().setBackground(background);
            }

            if ((width > 0) && (height > 0) && (container.getComponent() != null)) {
                container.getComponent().setPreferredSize(new Dimension(width, height));
            }

            // optional west component (e.g., file tree) in a split pane
            JComponent westComponent = PropertySupport.getSplitWestComponent(properties);

            Component centerComponent = (scrollPane == null)
                    ? container.getComponent()
                    : scrollPane;

            if (westComponent != null) {
                JSplitPane splitPane = new JSplitPane(
                        JSplitPane.HORIZONTAL_SPLIT,
                        false,
                        westComponent,
                        centerComponent);

                splitPane.setResizeWeight(0.0);
                add(splitPane, BorderLayout.CENTER);
            } else {
                add(centerComponent, BorderLayout.CENTER);
            }

            // optional toolbar
            boolean addToolBar = PropertySupport.getToolbar(properties);
            if (addToolBar) {
                long bits = PropertySupport.getToolbarBits(properties);
                if (bits == Long.MIN_VALUE) {
                    bits = ToolBarBits.EVERYTHING;
                }
                BaseToolBar toolBar = new BaseToolBar(container, bits);
                add(toolBar, BorderLayout.NORTH);
            } else {
                // "hack": create a non-visible toolbar so that the pointer tool is
                // initialized and available.
                new BaseToolBar(container, 1);
            }

            pack();
        } else {
            // view without a container (e.g., log view)
            if ((width > 0) && (height > 0)) {
                setBounds(left, top, width, height);
            } else {
                setLocation(left, top);
            }
        }

        startingLocation.setLocation(left, top);

        // --------------------------------------------------------------------
        // Add to desktop and schedule initial visibility
        // --------------------------------------------------------------------

        if (desktop != null) {
            desktop.add(this, 0);
        }

        if (visible) {
            // set visible on the AWT event dispatch thread
            EventQueue.invokeLater(() -> setVisible(true));
        }

        addComponentListener(this);
    }

    // ========================================================================
    // Basic accessors
    // ========================================================================

    /**
     * Returns the initial upper-left location of this view at creation time.
     *
     * @return the starting location of the view.
     */
    public Point getStartingLocation() {
        return startingLocation;
    }

    /**
     * Returns the insets of this internal frame, slightly reducing the bottom
     * inset to avoid unwanted extra space.
     *
     * @return the insets for this view.
     */
    @Override
    public Insets getInsets() {
        Insets def = super.getInsets();
        return new Insets(def.top, def.left, 2, def.right);
    }

    /**
     * Returns the parent {@link JFrame} hosting the desktop that contains this
     * view.
     * <p>
     * The result is lazily looked up using
     * {@link SwingUtilities#getAncestorOfClass(Class, java.awt.Component)} and
     * cached.
     * </p>
     *
     * @return the parent application {@link JFrame}, or {@code null} if this view
     *         has not yet been attached to a frame hierarchy.
     */
    public JFrame getParentFrame() {
        if (parentFrame == null) {
            parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(Frame.class, this);
        }
        return parentFrame;
    }

    /**
     * Checks whether this view is currently the "top" internal frame on the desktop.
     * <p>
     * A view is considered on top if:
     * </p>
     * <ul>
     *     <li>it is currently selected, or</li>
     *     <li>it is the first visible frame among the desktop's internal frames.</li>
     * </ul>
     *
     * @return {@code true} if this view is on top; {@code false} otherwise.
     */
    public boolean isOnTop() {
        // is it active?
        if (isSelected()) {
            return true;
        }

        // is it showing?
        if (!isShowing()) {
            return false;
        }

        if (desktop == null) {
            return false;
        }

        JInternalFrame[] frames = desktop.getAllFrames();
        for (JInternalFrame frame : frames) {
            if (frame.isShowing()) {
                return frame == this;
            }
        }
        return false;
    }

    /**
     * Returns the container associated with this view, if any.
     *
     * @return the view's {@link IContainer}, or {@code null} if this view has no
     *         container.
     */
    public IContainer getContainer() {
        return container;
    }

    /**
     * Returns the name of this view. The base implementation returns the
     * current title of the internal frame.
     *
     * @return the title of the view, used as its name.
     */
    @Override
    public String getName() {
        return getTitle();
    }

    /**
     * Returns the logical property name used when storing or loading properties
     * for this view.
     *
     * @return the property name for this view.
     */
    public String getPropertyName() {
        return VIEWPROPNAME;
    }

    // ========================================================================
    // Interaction hooks
    // ========================================================================

    /**
     * Called by a container when a right-click is not otherwise handled
     * (typically when the click is on an "inert" region).
     * <p>
     * Subclasses may override to provide custom context menu behavior.
     * </p>
     *
     * @param mouseEvent the originating mouse event.
     * @return {@code true} if the event was consumed; {@code false} otherwise.
     */
    public boolean rightClicked(MouseEvent mouseEvent) {
        return false;
    }

    /**
     * Called when a "control panel" toggle button is pressed for this view.
     * <p>
     * The base implementation simply logs to {@code System.err}. Subclasses
     * should override to show or hide a view-specific control panel.
     * </p>
     */
    public void controlPanelButtonHit() {
        System.err.println("Control Panel Button Hit");
    }

    // ========================================================================
    // Properties / configuration
    // ========================================================================

    /**
     * Applies layout and world configuration stored in the given properties
     * instance.
     * <p>
     * This method expects properties that were previously produced by
     * {@link #getConfigurationProperties()} and stored externally.
     * </p>
     *
     * @param properties the properties to read from; may be {@code null}.
     */
    public void setFromProperties(Properties properties) {
        if (properties == null) {
            return;
        }

        String name = getName();
        if (name == null) {
            return;
        }

        name = name + ".";

        // Frame bounds
        Rectangle viewRect = rectangleFromProperties(properties);
        if (viewRect != null) {
            setBounds(viewRect);

            // World bounds and zoom
            if (container != null) {
                Rectangle2D.Double wr = worldRectangleFromProperties(properties);
                if (wr != null) {
                    container.zoom(wr.getMinX(), wr.getMaxX(), wr.getMinY(), wr.getMaxY());
                }
            }
        }

        boolean vis = getBoolean(name + "visible", properties, false);
        boolean ontop = getBoolean(name + "ontop", properties, false);
        boolean maximized = getBoolean(name + "maxmized", properties, false); // note spelling in legacy key
        setVisible(vis);

        if (ontop) {
            toFront();
        }

        if (maximized) {
            try {
                setMaximum(true);
            } catch (PropertyVetoException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Utility to read a boolean property from the given {@link Properties} with
     * a default value.
     *
     * @param key        the property key.
     * @param properties the properties instance.
     * @param defVal     the default value if the property is not present.
     * @return the parsed boolean value, or {@code defVal} if the property is
     *         absent.
     */
    private boolean getBoolean(String key, Properties properties, boolean defVal) {
        String str = properties.getProperty(key);
        return (str == null) ? defVal : Boolean.parseBoolean(str);
    }

    /**
     * Returns the properties that were used to construct this view.
     *
     * @return the underlying properties of this view; never {@code null}.
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Convenience method to read a boolean property from this view's internal
     * properties.
     *
     * @param key the property key.
     * @return the parsed boolean value, or {@code false} if missing or invalid.
     */
    public boolean checkBooleanProperty(String key) {
        return PropertySupport.getBoolean(properties, key, false);
    }

    /**
     * Stores a boolean property in this view's internal properties.
     *
     * @param key the key associated with the boolean value.
     * @param val the value to store.
     */
    public void setBooleanProperty(String key, boolean val) {
        properties.put(key, val ? "true" : "false");
    }

    /**
     * Reads an integer property from this view's internal properties.
     *
     * @param key the property key.
     * @return the parsed integer value, or {@code -1} if missing or invalid.
     */
    public int getIntProperty(String key) {
        return PropertySupport.getInt(properties, key, -1);
    }

    /**
     * Stores an integer property in this view's internal properties.
     *
     * @param key the key associated with the integer value.
     * @param val the value to store.
     */
    public void setIntProperty(String key, int val) {
        properties.put(key, val);
    }

    /**
     * Produces a set of properties that describe the current configuration of
     * this view, suitable for persisting to a file or other storage.
     * <p>
     * Stored properties include:
     * </p>
     * <ul>
     *     <li>Visibility, iconified, on-top, and maximized flags.</li>
     *     <li>Frame bounds in pixels.</li>
     *     <li>World coordinate bounds (xmin, ymin, xmax, ymax) if a container is
     *     present.</li>
     * </ul>
     *
     * @return a new {@link Properties} instance capturing the current configuration.
     */
    public Properties getConfigurationProperties() {
        Properties props = new Properties();
        String name = getName();

        if (name != null) {
            name = name + ".";

            props.put(name + "visible", "" + isVisible());
            props.put(name + "closed", "" + isClosed());
            props.put(name + "icon", "" + isIcon());
            props.put(name + "ontop", "" + isOnTop());
            props.put(name + "maximized", "" + isMaximum());

            Rectangle viewRect = getBounds();
            props.put(name + "x", "" + viewRect.x);
            props.put(name + "y", "" + viewRect.y);
            props.put(name + "width", "" + viewRect.width);
            props.put(name + "height", "" + viewRect.height);

            if (container != null && container.getComponent() != null) {
                Rectangle b = container.getComponent().getBounds();
                b.x = 0;
                b.y = 0;
                Rectangle2D.Double wr = new Rectangle2D.Double();
                container.localToWorld(b, wr);
                props.put(name + "xmin", DoubleFormat.doubleFormat(wr.x, 8));
                props.put(name + "ymin", DoubleFormat.doubleFormat(wr.y, 8));
                props.put(name + "xmax", DoubleFormat.doubleFormat(wr.getMaxX(), 8));
                props.put(name + "ymax", DoubleFormat.doubleFormat(wr.getMaxY(), 8));
            }
        }

        return props;
    }

    /**
     * Reconstructs a world-coordinate rectangle from the given properties.
     *
     * @param properties the properties to read from.
     * @return a new {@link Rectangle2D.Double} describing the world bounds, or
     *         {@code null} if insufficient data are present or parsing fails.
     */
    private Rectangle2D.Double worldRectangleFromProperties(Properties properties) {

        String name = getName() + "."; // prefix

        String xminStr = properties.getProperty(name + "xmin");
        if (xminStr != null) {
            String yminStr = properties.getProperty(name + "ymin");
            if (yminStr != null) {
                String xmaxStr = properties.getProperty(name + "xmax");
                if (xmaxStr != null) {
                    String ymaxStr = properties.getProperty(name + "ymax");
                    if (ymaxStr != null) {
                        try {
                            double xmin = Double.parseDouble(xminStr);
                            double ymin = Double.parseDouble(yminStr);
                            double xmax = Double.parseDouble(xmaxStr);
                            double ymax = Double.parseDouble(ymaxStr);
                            return new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Reconstructs a pixel-based rectangle from the given properties.
     *
     * @param properties the properties to read from.
     * @return a new {@link Rectangle} if the stored width and height are
     *         greater than two pixels; {@code null} otherwise.
     */
    private Rectangle rectangleFromProperties(Properties properties) {

        String name = getName() + "."; // prefix

        String xStr = properties.getProperty(name + "x");
        if (xStr != null) {
            String yStr = properties.getProperty(name + "y");
            if (yStr != null) {
                String wStr = properties.getProperty(name + "width");
                if (wStr != null) {
                    String hStr = properties.getProperty(name + "height");
                    if (hStr != null) {
                        try {
                            int x = Integer.parseInt(xStr);
                            int y = Integer.parseInt(yStr);
                            int w = Integer.parseInt(wStr);
                            int h = Integer.parseInt(hStr);

                            if ((w > 2) && (h > 2)) {
                                return new Rectangle(x, y, w, h);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }

    // ========================================================================
    // Virtual window integration
    // ========================================================================

    /**
     * Returns the {@link VirtualWindowItem} associated with this view, if any.
     * <p>
     * The virtual window item is used when the view participates in a virtual
     * overview representation of the desktop.
     * </p>
     *
     * @return the associated {@link VirtualWindowItem}, or {@code null} if none.
     */
    protected VirtualWindowItem getVirtualItem() {
        return virtualItem;
    }

    /**
     * Sets the {@link VirtualWindowItem} associated with this view.
     *
     * @param virtualItem the virtual window item to associate; may be {@code null}.
     */
    protected void setVirtualItem(VirtualWindowItem virtualItem) {
        this.virtualItem = virtualItem;
    }

    // ========================================================================
    // Geometry utilities
    // ========================================================================

    /**
     * Offsets the location of this view by the specified amounts.
     *
     * @param dh the horizontal increment in pixels (positive moves right).
     * @param dv the vertical increment in pixels (positive moves down).
     */
    public void offset(int dh, int dv) {
        Rectangle b = getBounds();
        b.x += dh;
        b.y += dv;
        setBounds(b);
    }

    /**
     * Returns a special clipping shape to be used when rendering this view, if
     * any.
     * <p>
     * The base implementation returns {@code null}, indicating the default
     * clipping behavior should be used. Subclasses may override to provide
     * non-rectangular or otherwise custom clipping behavior.
     * </p>
     *
     * @return a custom clip {@link Shape}, or {@code null} if no special
     *         clipping is required.
     */
    public Shape getSpecialClip() {
        return null;
    }

    // ========================================================================
    // Toolbar helpers
    // ========================================================================

    /**
     * Returns the view's toolbar, if one exists.
     *
     * @return the {@link BaseToolBar} associated with this view, or {@code null}
     *         if no toolbar exists.
     */
    public BaseToolBar getToolBar() {
        if (getContainer() != null) {
            return getContainer().getToolBar();
        }
        return null;
    }

    // ========================================================================
    // Magnification support
    // ========================================================================

    /**
     * Handles a magnification request initiated by a mouse event.
     * <p>
     * The actual magnification is delegated to {@link MagnifyWindow} for
     * containers of type {@link BaseContainer}. The work is queued to run on
     * the AWT event dispatch thread via {@link SwingUtilities#invokeLater(Runnable)}.
     * </p>
     *
     * @param me the source mouse event, typically a gesture indicating "magnify".
     */
    public void handleMagnify(final MouseEvent me) {
        final BaseView bview = this;

        Runnable magRun = () -> {
            IContainer cont = bview.getContainer();
            if (cont instanceof BaseContainer) {
                MagnifyWindow.magnify((BaseContainer) cont, me);
            }
        };

        SwingUtilities.invokeLater(magRun);

        if (container != null) {
            container.refresh();
        }
    }

    // ========================================================================
    // Popup / quick zoom
    // ========================================================================

    /**
     * Returns the {@link ViewPopupMenu} associated with this view.
     *
     * @return the view's popup menu.
     */
    public ViewPopupMenu getViewPopupMenu() {
        return viewPopupMenu;
    }

    /**
     * Adds a named quick-zoom region to this view's popup menu.
     *
     * @param title the display title of the quick zoom entry.
     * @param xmin  minimum world x-coordinate of the zoom region.
     * @param ymin  minimum world y-coordinate of the zoom region.
     * @param xmax  maximum world x-coordinate of the zoom region.
     * @param ymax  maximum world y-coordinate of the zoom region.
     */
    public void addQuickZoom(String title,
                             final double xmin,
                             final double ymin,
                             final double xmax,
                             final double ymax) {

        viewPopupMenu.addQuickZoom(title, xmin, ymin, xmax, ymax);
    }

    // ========================================================================
    // Scroll support
    // ========================================================================

    /**
     * Indicates whether this view is scrollable.
     *
     * @return {@code true} if this view uses a {@link JScrollPane}; {@code false}
     *         otherwise.
     */
    public boolean isScrollable() {
        return (scrollPane != null);
    }

    /**
     * Returns the scroll pane used by this view, if any.
     *
     * @return the {@link JScrollPane} wrapping the container component, or
     *         {@code null} if this view is not scrollable.
     */
    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    // ========================================================================
    // FocusListener
    // ========================================================================

    @Override
    public void focusGained(FocusEvent e) {
        // no-op by default; subclasses may override
    }

    @Override
    public void focusLost(FocusEvent e) {
        // no-op by default; subclasses may override
    }

    // ========================================================================
    // Cloning / refresh
    // ========================================================================

    /**
     * Refreshes the view by refreshing its container, if present, provided that
     * the view is currently visible.
     */
    public void refresh() {
        if (isViewVisible() && (container != null)) {
            container.refresh();
        }
    }

    // ========================================================================
    // MouseListener
    // ========================================================================

    @Override
    public void mouseClicked(MouseEvent e) {
        // no-op by default; subclasses may override
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // no-op by default; subclasses may override
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // no-op by default; subclasses may override
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // no-op by default; subclasses may override
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // no-op by default; subclasses may override
    }

    // ========================================================================
    // ComponentListener
    // ========================================================================

    @Override
    public void componentHidden(ComponentEvent e) {
        // no-op by default; subclasses may override
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        // Some look-and-feels and desktops require an explicit repaint to avoid
        // artifacts after a move.
        if (desktop != null) {
            desktop.repaint();
        }
    }

    @Override
    public void componentResized(ComponentEvent e) {
        // Some look-and-feels and desktops require an explicit repaint to avoid
        // artifacts after resizing.
        if (desktop != null) {
            desktop.repaint();
        }
    }

    @Override
    public void componentShown(ComponentEvent e) {
        // no-op by default; subclasses may override
    }

    // ========================================================================
    // Visibility helpers
    // ========================================================================

    /**
     * Checks whether this view is at least partially visible within the bounds of
     * its parent container.
     * <p>
     * This method does not consider z-order or occlusion by other internal
     * frames. It simply tests whether the frame's bounds intersect the bounds of
     * its parent component.
     * </p>
     *
     * @return {@code true} if the view's bounds intersect the parent's bounds;
     *         {@code false} otherwise or if there is no parent.
     */
    public boolean isViewVisible() {
        if (getParent() == null) {
            return false;
        }

        Rectangle frameBounds = getBounds();
        Rectangle parentBounds = getParent().getBounds();

        return parentBounds.intersects(frameBounds);
    }
}
