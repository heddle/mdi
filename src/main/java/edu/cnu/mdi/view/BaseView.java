package edu.cnu.mdi.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import edu.cnu.mdi.app.BaseMDIApplication;
import edu.cnu.mdi.component.MagnifyWindow;
import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.container.LayerInspectorDialog;
import edu.cnu.mdi.desktop.Desktop;
import edu.cnu.mdi.feedback.FeedbackControl;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.feedback.IFeedbackProvider;
import edu.cnu.mdi.format.DoubleFormat;
import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.graphics.rubberband.ARubberband;
import edu.cnu.mdi.graphics.toolbar.AToolBar;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.transfer.IFileDropHandler;
import edu.cnu.mdi.ui.menu.ViewPopupMenu;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Base class for all MDI views.
 *
 * <p>A {@code BaseView} is a {@link JInternalFrame} hosted on the application's
 * {@link Desktop}. Most views contain an {@link IContainer} that provides a
 * "world" coordinate system and drawing/interaction support. Container-less
 * views (e.g. log or config panels) are also supported.</p>
 *
 * <h2>Persistence key ({@code VIEWPROPNAME})</h2>
 * <p>
 * Each view owns a stable string prefix used as the namespace for its
 * persisted properties (position, size, visibility, etc.).  The prefix is
 * derived automatically from the view's title at construction time by
 * {@link #sanitizeForKey(String)}.  Subclasses that need a hand-chosen,
 * title-independent key should override {@link #getPropertyName()}.
 * </p>
 * <p>
 * <strong>Why not use the title directly?</strong> Titles may contain spaces
 * and special characters that are awkward inside XML property keys, and they
 * can change between releases.  The sanitised form is stable and safe.
 * </p>
 *
 * <h2>Persistence format</h2>
 * <p>
 * Properties are written using dotted keys: {@code prefix.x},
 * {@code prefix.y}, {@code prefix.w}, {@code prefix.h},
 * {@code prefix.visible}, {@code prefix.maximized}.  When reading back,
 * the legacy non-dotted variants (e.g. {@code prefixx}, {@code prefixvisible})
 * are also accepted so that files saved by older versions of the framework
 * continue to work.
 * </p>
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>All property parsing happens once in {@link ViewInitConfig#from},
 *       keeping the constructor body straightforward.</li>
 *   <li>Nested static helpers ({@link FrameConfigurer}, {@link ViewContentBuilder},
 *       etc.) keep each responsibility isolated without requiring separate
 *       compilation units.</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class BaseView extends JInternalFrame
        implements FocusListener, MouseListener, ComponentListener,
                   IFeedbackProvider, IFileDropHandler {

    // -----------------------------------------------------------------------
    // Persistence key
    // -----------------------------------------------------------------------

    /**
     * Stable prefix used to namespace this view's persisted properties.
     *
     * <p>Set during construction from the sanitised view title.  Subclasses
     * that want an explicit, title-independent key should override
     * {@link #getPropertyName()} rather than writing to this field.</p>
     */
    protected String VIEWPROPNAME;

    // -----------------------------------------------------------------------
    // UI resources (shared across all instances)
    // -----------------------------------------------------------------------

    /** Lazily-shown dialog displaying view information. */
    protected JDialog infoDialog;

    /** Icon displayed in the floating info button. */
    protected static final Icon infoIcon;

    static {
        String path = ToolBits.getResourcePath(ToolBits.INFO);
        infoIcon = ImageManager.getInstance().loadUiIcon(
                path,
                BaseToolBar.DEFAULT_ICON_SIZE,
                BaseToolBar.DEFAULT_ICON_SIZE);
    }

    // -----------------------------------------------------------------------
    // Instance state
    // -----------------------------------------------------------------------

    /**
     * Properties supplied at construction time.  Also used as a lightweight
     * per-view key-value store by {@link #setBooleanProperty} etc.
     */
    protected final Properties properties;

    /** The desktop pane that hosts this internal frame. */
    protected final JDesktopPane desktop;

    /** Optional world-coordinate container; {@code null} for container-less views. */
    protected IContainer container;

    /** Optional scroll pane wrapping the container component. */
    protected JScrollPane scrollPane;

    /** Lazily resolved top-level frame ancestor. */
    private JFrame parentFrame;

    /** Right-click popup menu for this view. */
    protected final ViewPopupMenu viewPopupMenu;

    /** Optional toolbar (present only when toolbar bits were specified). */
    private BaseToolBar toolBar;

    /**
     * Optional thumbnail item shown in the {@link VirtualView}.
     * Set externally by the virtual desktop machinery.
     */
    protected VirtualWindowItem virtualItem;

    /** Optional drag-and-drop file filter. */
    private Predicate<File> fileFilter;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Construct a {@code BaseView} from alternating key/value property pairs.
     *
     * @param keyVals alternating {@link PropertyUtils} key/value pairs
     */
    public BaseView(Object... keyVals) {
        this(PropertyUtils.fromKeyValues(keyVals));
    }

    /**
     * Construct a {@code BaseView} from a pre-built {@link Properties} object.
     *
     * @param props configuration properties; must not be {@code null}
     */
    public BaseView(Properties props) {
        this.properties    = (props != null) ? props : new Properties();
        this.desktop       = Desktop.getInstance();
        this.viewPopupMenu = new ViewPopupMenu(this);

        setLayout(new BorderLayout(-1, -1));

        // Parse all configuration exactly once.
        final ViewInitConfig cfg = ViewInitConfig.from(this.properties);

        // Derive the persistence key from the sanitised title.
        // Subclasses may override getPropertyName() if they need a fixed key.
        VIEWPROPNAME = sanitizeForKey(cfg.title);

        // Apply frame decorations, title, close operation, initial location.
        FrameConfigurer.apply(this, cfg);

        // Register listeners.
        addMouseListener(this);
        addFocusListener(this);
        addComponentListener(this);

        // Register with the manager before the view becomes visible.
        ViewManager.getInstance().add(this);

        // Build content (container, toolbar, scroll pane, etc.)
        if (cfg.useContainer && cfg.hasWorldSystem()) {
            this.container = resolveContainer(cfg.worldSystem);
            this.container.setView(this);
            ViewContentBuilder.build(this, cfg);
            installViewPopupTrigger();
            ViewKeyBindings.installDeleteBinding(this);
            pack();
        } else {
            FrameConfigurer.applyNoContainerBounds(this, cfg);
        }

        // Add to the desktop.
        if (desktop != null) {
            desktop.add(this, 0);
        }

        if (this.container instanceof BaseContainer) {
            getViewPopupMenu().add(LayerInspectorDialog.createMenuItem(this));
        }

        // Defer setVisible to avoid flicker / z-order issues during startup.
        if (cfg.visible) {
            SwingUtilities.invokeLater(() -> setVisible(true));
        }
    }

    // -----------------------------------------------------------------------
    // Persistence key helpers
    // -----------------------------------------------------------------------

    /**
     * Convert a view title into a safe property-key prefix.
     *
     * <p>Trims whitespace and replaces every character that is not an ASCII
     * letter, digit, hyphen, or underscore with an underscore.  The result is
     * therefore safe for use in XML property files and is stable across
     * platforms.</p>
     *
     * <p>Examples:</p>
     * <pre>
     *   "Sample 2D Map View"  →  "Sample_2D_Map_View"
     *   "TSP Demo View"       →  "TSP_Demo_View"
     *   null / blank          →  "view"
     * </pre>
     *
     * @param title the view title; may be {@code null}
     * @return a non-empty, XML-safe property-key prefix
     */
    private static String sanitizeForKey(String title) {
        if (title == null || title.isBlank()) {
            return "view";
        }
        return title.trim().replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    // -----------------------------------------------------------------------
    // Container creation
    // -----------------------------------------------------------------------

    /**
     * Resolve the {@link IContainer} for this view.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>An {@link IContainer} instance stored directly in the properties
     *       under {@link PropertyUtils#CONTAINER}.</li>
     *   <li>A {@link Class} stored under {@link PropertyUtils#CONTAINERCLASS},
     *       instantiated via its {@code (Rectangle2D.Double)} constructor.</li>
     *   <li>A default {@link BaseContainer}.</li>
     * </ol>
     *
     * @param worldSystem the initial world coordinate rectangle
     * @return a non-null container
     */
    protected IContainer resolveContainer(Rectangle2D.Double worldSystem) {
        return ContainerFactory.resolveContainer(this.properties, worldSystem);
    }

    // -----------------------------------------------------------------------
    // First-realization lifecycle hook
    // -----------------------------------------------------------------------

    /**
     * Called exactly once, after the view has been fully constructed and
     * registered with the {@link ViewManager}.
     *
     * <p>For eagerly-created views this occurs during normal startup. For
     * lazily-created views (see {@link ViewConfiguration}) it occurs the first
     * time the user selects the view from the Views menu.</p>
     *
     * <p>The default implementation is a no-op. Subclasses may override to
     * reconcile with shared application state or perform other one-time
     * post-construction work.</p>
     */
    public void onFirstRealize() {
        // no-op
    }

    // -----------------------------------------------------------------------
    // Standard accessors
    // -----------------------------------------------------------------------

    /**
     * Slightly reduces the bottom inset to match legacy appearance.
     *
     * @return adjusted insets
     */
    @Override
    public Insets getInsets() {
        Insets def = super.getInsets();
        return new Insets(def.top, def.left, 2, def.right);
    }

    /**
     * Called when the application is about to exit.
     *
     * <p>Delegates to {@link IContainer#prepareForExit()} if a container is
     * present, allowing simulations or background tasks to stop cleanly.</p>
     */
    public void prepareForExit() {
        IContainer cont = getIContainer();
        if (cont != null) {
            cont.prepareForExit();
        }
    }

    /**
     * Returns the top-level {@link JFrame} that hosts the desktop, resolving
     * it lazily on first call.
     *
     * @return the parent frame, or {@code null} if not yet in a realized
     *         hierarchy
     */
    public JFrame getParentFrame() {
        if (parentFrame == null) {
            parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(Frame.class, this);
        }
        return parentFrame;
    }

    /**
     * Returns the container hosted by this view.
     *
     * @return the container, or {@code null} for container-less views
     */
    public IContainer getIContainer() {
        return container;
    }

    /**
     * Set the drag-and-drop file filter for this view.
     *
     * @param filter predicate that accepts only the desired file types; pass
     *               {@code null} to accept all files
     */
    @Override
    public void setFileFilter(Predicate<File> filter) {
        this.fileFilter = filter;
    }

    /**
     * Returns the drag-and-drop file filter, or {@code null} if none is set.
     *
     * @return the file filter
     */
    @Override
    public Predicate<File> getFileFilter() {
        return fileFilter;
    }

    /**
     * Called when files are dropped on this view.  The default is a no-op;
     * see {@code PlotView} and {@code DrawingView} for example overrides.
     *
     * @param files the dropped files
     */
    @Override
    public void filesDropped(List<File> files) {
        // no-op
    }

    /**
     * Returns the view title (legacy: {@link JInternalFrame#getName()} maps to
     * the title for backward compatibility).
     *
     * @return the title
     */
    @Override
    public String getName() {
        return getTitle();
    }

    /**
     * Returns the stable property-key prefix used to namespace this view's
     * persisted state.
     *
     * <p>By default this is derived automatically from the view title via
     * {@link #sanitizeForKey(String)}.  Subclasses that require a fixed,
     * title-independent key should override this method:</p>
     * <pre>
     * &#64;Override
     * public String getPropertyName() { return "myMapView"; }
     * </pre>
     *
     * @return the persistence key prefix; never {@code null}
     */
    public String getPropertyName() {
        return VIEWPROPNAME;
    }

    /**
     * Returns the right-click popup menu for this view.
     *
     * @return the popup menu; never {@code null}
     */
    public ViewPopupMenu getViewPopupMenu() {
        return viewPopupMenu;
    }

    /**
     * Install a right-click popup trigger on the view's canvas.
     *
     * <p>Handles platform differences: the popup trigger may fire on
     * {@code mousePressed} (macOS) or {@code mouseReleased} (Windows/Linux).
     * Item-level right-click handlers take precedence — the view popup is
     * shown only when the click lands on empty canvas.</p>
     */
    private void installViewPopupTrigger() {
        if (container == null) {
            return;
        }
        final Component canvas = container.getComponent();
        if (canvas == null) {
            return;
        }

        canvas.addMouseListener(new MouseAdapter() {
            private void maybeShow(MouseEvent e) {
                if (e == null || !e.isPopupTrigger()
                        || container.getItemAtPoint(e.getPoint()) != null) {
                    return;
                }
                ViewPopupMenu menu = getViewPopupMenu();
                if (menu != null) {
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            @Override public void mousePressed (MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
        });

        if (scrollPane != null && scrollPane.getViewport() != null) {
            final Component viewport = scrollPane.getViewport();
            viewport.addMouseListener(new MouseAdapter() {
                private void maybeShow(MouseEvent e) {
                    if (e == null || !e.isPopupTrigger()) {
						return;
					}
                    Point p = SwingUtilities.convertPoint(viewport, e.getPoint(), canvas);
                    if (container.getItemAtPoint(p) != null) {
						return;
					}
                    ViewPopupMenu menu = getViewPopupMenu();
                    if (menu != null) {
                        menu.show(viewport, e.getX(), e.getY());
                    }
                }
                @Override public void mousePressed (MouseEvent e) { maybeShow(e); }
                @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
            });
        }
    }

    /**
     * Returns {@code true} if this view wraps its container in a scroll pane.
     *
     * @return {@code true} if scrollable
     */
    public boolean isScrollable() {
        return scrollPane != null;
    }

    /**
     * Returns the scroll pane, if this view was configured as scrollable.
     *
     * @return the scroll pane, or {@code null}
     */
    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    /**
     * Optional hook for subclasses: called on a right-click.  The default
     * returns {@code false} (not handled).
     *
     * @param mouseEvent the triggering event
     * @return {@code true} if the event was handled and should not be
     *         processed further
     */
    public boolean rightClicked(MouseEvent mouseEvent) {
        return false;
    }

    /**
     * Refresh the view by delegating to the container, if the view is
     * currently visible.
     */
    public void refresh() {
        if (isViewVisible() && container != null) {
            container.refresh();
        }
    }

    /**
     * Returns {@code true} if any part of this internal frame's bounds
     * intersects the parent desktop's bounds, indicating that it is on-screen.
     *
     * @return {@code true} if visible in the desktop
     */
    public boolean isViewVisible() {
        if (getParent() == null) {
            return false;
        }
        return getParent().getBounds().intersects(getBounds());
    }

    /**
     * Returns {@code true} if this view is at the top of the desktop's
     * z-order.
     *
     * @return {@code true} if on top
     */
    public boolean isOnTop() {
        if (isSelected()) {
            return true;
        }
        if (!isShowing() || desktop == null) {
            return false;
        }
        for (JInternalFrame frame : desktop.getAllFrames()) {
            if (frame.isShowing()) {
                return frame == this;
            }
        }
        return false;
    }

    /**
     * Returns an optional clip shape used by the rendering pipeline.
     * The default returns {@code null}.
     *
     * @return a clip shape, or {@code null}
     */
    public Shape getSpecialClip() {
        return null;
    }

    /**
     * Move this view by the specified pixel offset.
     *
     * @param dh horizontal offset in pixels (positive = right)
     * @param dv vertical offset in pixels (positive = down)
     */
    public void offset(int dh, int dv) {
        Rectangle b = getBounds();
        b.x += dh;
        b.y += dv;
        setBounds(b);
    }

    /**
     * center this view within its parent frame.
     * <p>
     * This is a no-op if the parent frame cannot be determined.
     * </p>
     */
    public void center() {
        JFrame parent = getParentFrame();
        if (parent != null) {
            Dimension ps = parent.getSize();
            Dimension ms = getSize();
            setLocation((ps.width - ms.width) / 2, (ps.height - ms.height) / 2);
        }
    }

    /**
     * Returns the toolbar for this view, or {@code null} if none was
     * configured.
     *
     * @return the toolbar
     */
    public AToolBar getToolBar() {
        return toolBar;
    }

    // -----------------------------------------------------------------------
    // Feedback integration
    // -----------------------------------------------------------------------

    /**
     * Create and install a feedback pane with default colors (cyan on black,
     * 9 pt font).
     *
     * @return the created feedback pane
     */
    protected FeedbackPane initFeedback() {
        return initFeedback(Color.cyan, Color.black, 9);
    }

    /**
     * Create and install a feedback pane with custom colors and font size.
     *
     * @param fg       foreground color
     * @param bg       background color
     * @param fontSize font size in points
     * @return the created feedback pane
     * @throws IllegalStateException if this view has no container
     */
    protected FeedbackPane initFeedback(Color fg, Color bg, int fontSize) {
        if (getIContainer() == null) {
            throw new IllegalStateException(
                    "initFeedback requires a container-backed view.");
        }
        FeedbackControl fbc = getIContainer().getFeedbackControl();
        fbc.addFeedbackProvider(this);

        FeedbackPane fbp = new FeedbackPane(fg, bg, fontSize);
        fbp.setBorder(null);
        IContainer cont = getIContainer();
        cont.setFeedbackPane(fbp);

        cont.getComponent().addMouseMotionListener(new MouseMotionListener() {
            @Override public void mouseMoved  (MouseEvent e) { cont.feedbackTrigger(e, false); }
            @Override public void mouseDragged(MouseEvent e) { cont.feedbackTrigger(e, true);  }
        });
        return fbp;
    }

    /**
     * Default feedback implementation: no-op.
     * Subclasses override to provide coordinate or status strings.
     */
    @Override
    public void getFeedbackStrings(IContainer container, Point pp,
            java.awt.geom.Point2D.Double wp, List<String> feedbackStrings) {
        // no-op
    }

    // -----------------------------------------------------------------------
    // Virtual desktop
    // -----------------------------------------------------------------------

    /**
     * Returns the virtual-desktop thumbnail item associated with this view, or
     * {@code null} if the virtual desktop is not active.
     *
     * @return the virtual window item
     */
    public VirtualWindowItem getVirtualItem() {
        return virtualItem;
    }

    /**
     * Set the virtual-desktop thumbnail item.
     *
     * @param virtualItem the item to associate with this view
     */
    public void setVirtualItem(VirtualWindowItem virtualItem) {
        this.virtualItem = virtualItem;
    }

    // -----------------------------------------------------------------------
    // Focus-fix helper
    // -----------------------------------------------------------------------

    /**
     * Install a mouse-enter listener on {@code menu} that selects {@code view}
     * when the mouse enters the menu.
     *
     * <p>This works around a focus issue on some platforms where the view's
     * menu bar flashes or loses focus when a menu is opened. Selecting the
     * internal frame explicitly prevents the flash.</p>
     *
     * @param menu the menu to attach the listener to
     * @param view the view to select on mouse entry
     */
    public static void applyFocusFix(JMenu menu, BaseView view) {
        menu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                try {
                    if (view.isIcon()) {
                        view.setIcon(false);
                    }
                    if (!view.isSelected()) {
                        view.setSelected(true);
                    }
                } catch (Exception ex) {
                    // Vetoed — ignore.
                }
            }
        });
    }

    // -----------------------------------------------------------------------
    // Magnify support
    // -----------------------------------------------------------------------

    /**
     * Handle a magnify (modifier + mouse) request.
     * <p>
     * Dispatches to the EDT if not already on it.
     * </p>
     *
     * @param me the triggering mouse event
     */
    public void handleMagnify(final MouseEvent me) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> handleMagnify(me));
            return;
        }
        IContainer cont = getIContainer();
        if (cont instanceof BaseContainer) {
            MagnifyWindow.magnify((BaseContainer) cont, me);
        }
        if (container != null) {
            container.refresh();
        }
    }

    // -----------------------------------------------------------------------
    // Info dialog
    // -----------------------------------------------------------------------

    /**
     * Show the view information dialog.
     *
     * <p>If the subclass provides an {@link AbstractViewInfo} via
     * {@link #getViewInfo()} that is displayed in a dialog; otherwise a
     * generic "no information available" message is shown.</p>
     */
    public void viewInfo() {
        AbstractViewInfo info = getViewInfo();
        if (info != null) {
            if (infoDialog != null && infoDialog.isShowing()) {
                infoDialog.toFront();
                return;
            }
            infoDialog = InfoDialogHelper.showInfoDialog(this, info);
        } else {
            JOptionPane.showMessageDialog(this,
                    "No detailed information is available for this view.",
                    "View Info",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Override to supply structured information for the info dialog.
     * The default returns {@code null} (no info available).
     *
     * @return view info, or {@code null}
     */
    public AbstractViewInfo getViewInfo() {
        return null;
    }

    // -----------------------------------------------------------------------
    // Persistence — public API
    // -----------------------------------------------------------------------

    /**
     * Restore this view's state from a properties map previously produced by
     * {@link #getConfigurationProperties()}.
     *
     * <p>Both the current dotted key format ({@code prefix.x}) and the legacy
     * non-dotted format ({@code prefixx}) are recognised, so files written by
     * older versions of the framework load correctly.</p>
     *
     * @param properties the source properties; may be {@code null}
     */
    public void setFromProperties(Properties properties) {
        ViewPersistence.applyToView(this, properties);
    }

    /**
     * Capture this view's current state into a {@link Properties} object.
     *
     * <p>Keys use the dotted format: {@code prefix.x}, {@code prefix.y},
     * {@code prefix.w}, {@code prefix.h}, {@code prefix.visible},
     * {@code prefix.maximized}. The prefix is {@link #getPropertyName()}.</p>
     *
     * @return a non-null properties object containing this view's state
     */
    public Properties getConfigurationProperties() {
        return ViewPersistence.captureFromView(this);
    }

    /**
     * Returns the properties that were used to construct this view.
     *
     * @return the construction-time properties; never {@code null}
     */
    public Properties getProperties() {
        return properties;
    }

    // -----------------------------------------------------------------------
    // Per-view property convenience methods
    // -----------------------------------------------------------------------

    /**
     * Read a boolean property from this view's internal properties.
     *
     * @param key the property key
     * @return the value, or {@code false} if absent or unparseable
     */
    public boolean checkBooleanProperty(String key) {
        return PropertyUtils.getBoolean(properties, key, false);
    }

    /**
     * Store a boolean property in this view's internal properties.
     *
     * @param key the property key
     * @param val the value
     */
    public void setBooleanProperty(String key, boolean val) {
        properties.put(key, Boolean.toString(val));
    }

    /**
     * Read an integer property from this view's internal properties.
     *
     * @param key the property key
     * @return the value, or {@code -1} if absent or unparseable
     */
    public int getIntProperty(String key) {
        return PropertyUtils.getInt(properties, key, -1);
    }

    /**
     * Store an integer property in this view's internal properties.
     *
     * @param key the property key
     * @param val the value
     */
    public void setIntProperty(String key, int val) {
        properties.put(key, Integer.toString(val));
    }

    // -----------------------------------------------------------------------
    // Listener implementations (API compatibility stubs)
    // -----------------------------------------------------------------------

    @Override public void focusGained(FocusEvent e) { /* no-op */ }
    @Override public void focusLost  (FocusEvent e) { /* no-op */ }

    @Override public void mouseClicked (MouseEvent e) { /* no-op */ }
    @Override public void mousePressed (MouseEvent e) { /* no-op */ }
    @Override public void mouseReleased(MouseEvent e) { /* no-op */ }
    @Override public void mouseEntered (MouseEvent e) { /* no-op */ }

    /** Close the magnify window when the mouse leaves any view canvas. */
    @Override
    public void mouseExited(MouseEvent e) {
        MagnifyWindow.closeMagnifyWindow();
    }

    @Override public void componentHidden(ComponentEvent e) { /* no-op */ }
    @Override public void componentShown (ComponentEvent e) { /* no-op */ }

    /**
     * No-op: FlatLaf repaints correctly on move without explicit desktop
     * repaint calls. Retained for API continuity.
     */
    @Override public void componentMoved  (ComponentEvent e) { /* no-op */ }

    /**
     * No-op: FlatLaf repaints correctly on resize. Retained for API continuity.
     */
    @Override public void componentResized(ComponentEvent e) { /* no-op */ }

    // ======================================================================
    // Nested helpers
    // ======================================================================

    // -----------------------------------------------------------------------
    // ViewInitConfig — parse all properties once
    // -----------------------------------------------------------------------

    /**
     * Immutable snapshot of the configuration parsed from the construction
     * properties.  All {@link PropertyUtils} reads happen here so the
     * constructor body can act on a clean, typed object.
     */
    private static final class ViewInitConfig {

        final String title;
        final boolean standardDecorations;
        final boolean iconifiable;
        final boolean maximizable;
        final boolean resizable;
        final boolean closable;
        final boolean addWheelZoom;
        final boolean scrollable;
        final boolean useContainer;
        final boolean visible;
        final int left;
        final int top;
        final int width;
        final int height;
        final Rectangle2D.Double worldSystem;
        final Color background;
        final JComponent splitWestComponent;
        final long toolBits;
        final ARubberband.Policy boxZoomPolicy;

        private ViewInitConfig(String title, boolean standardDecorations,
                boolean iconifiable, boolean maximizable,
                boolean resizable, boolean closable, boolean scrollable,
                boolean visible, int left, int top, int width, int height,
                Rectangle2D.Double worldSystem, Color background,
                JComponent splitWestComponent, long toolBits,
                ARubberband.Policy boxZoomPolicy, boolean addWheelZoom,
                boolean useContainer) {
            this.title               = title;
            this.standardDecorations = standardDecorations;
            this.iconifiable         = iconifiable;
            this.maximizable         = maximizable;
            this.resizable           = resizable;
            this.closable            = closable;
            this.scrollable          = scrollable;
            this.visible             = visible;
            this.left                = left;
            this.top                 = top;
            this.width               = width;
            this.height              = height;
            this.worldSystem         = worldSystem;
            this.background          = background;
            this.splitWestComponent  = splitWestComponent;
            this.toolBits            = toolBits;
            this.boxZoomPolicy       = boxZoomPolicy;
            this.addWheelZoom        = addWheelZoom;
            this.useContainer        = useContainer;
        }

        /** Returns {@code true} if a world coordinate system was specified. */
        boolean hasWorldSystem() {
            return worldSystem != null;
        }

        /**
         * Parse a {@link Properties} object into a {@code ViewInitConfig}.
         *
         * @param props the construction-time properties
         * @return a fully initialized config
         */
        static ViewInitConfig from(Properties props) {
            String title = PropertyUtils.getTitle(props);
            if (title == null || title.equals(PropertyUtils.unknownString)) {
                title = "A View";
            }

            boolean standardDecorations = PropertyUtils.getStandardViewDecorations(props);
            boolean iconifiable         = PropertyUtils.getIconifiable(props);
            boolean maximizable         = PropertyUtils.getMaximizable(props);
            boolean resizable           = PropertyUtils.getResizable(props);
            boolean closable            = PropertyUtils.getClosable(props);
            boolean scrollable          = PropertyUtils.getScrollable(props);
            boolean useContainer        = PropertyUtils.getUseContainer(props);
            boolean visible             = PropertyUtils.getVisible(props);

            int left   = PropertyUtils.getLeft(props);
            int top    = PropertyUtils.getTop(props);

            // Explicit WIDTH/HEIGHT take highest priority.
            int explicitWidth  = PropertyUtils.getWidth(props);
            int explicitHeight = PropertyUtils.getHeight(props);

            int width;
            int height;

            if (explicitWidth > 0 && explicitHeight > 0) {
                // Caller gave an exact pixel size — honour it directly.
                width  = Math.max(100, explicitWidth);
                height = Math.max(100, explicitHeight);
            } else {
                // Fall back to FRACTION + optional ASPECT sizing relative to
                // the application main window (not the screen).
                width  = 400; // safe fallback
                height = 300;
                double fraction = PropertyUtils.getFraction(props);
                if (Double.isFinite(fraction) && fraction > 0.0 && fraction < 1.0) {
                    BaseMDIApplication app = BaseMDIApplication.getApplication();
                    if (app != null) {
                        Dimension appSize = app.getSize();
                        double aspect = PropertyUtils.getAspectRatio(props);
                        height = (int) (fraction * appSize.height);
                        if (aspect > 0.001) {
                            // Width derived from requested height and aspect ratio.
                            width = (int) (height * aspect);
                        } else {
                            // No aspect given: match the height fraction on width too.
                            width = (int) (fraction * appSize.width);
                        }
                    }
                }
            }

            // Cascading placement when explicit left/top are not given.
            Point p = ViewStackingPolicy.computeInitialLocation(left, top);
            left = p.x;
            top  = p.y;

            Rectangle2D.Double worldSystem     = PropertyUtils.getWorldSystem(props);
            Color               background     = PropertyUtils.getBackground(props);
            JComponent          west           = PropertyUtils.getSplitWestComponent(props);
            long                bits           = PropertyUtils.getToolbarBits(props);
            ARubberband.Policy  policy         = PropertyUtils.getBoxZoomRubberbandPolicy(props);
            boolean             addWheelZoom   = PropertyUtils.addWheelZoom(props);

            return new ViewInitConfig(title, standardDecorations,
                    iconifiable, maximizable, resizable, closable, scrollable,
                    visible, left, top, width, height, worldSystem, background,
                    west, bits, policy, addWheelZoom, useContainer);
        }
    }

    // -----------------------------------------------------------------------
    // ViewStackingPolicy — cascade placement
    // -----------------------------------------------------------------------

    /**
     * Tracks the next default location for a view when explicit left/top
     * coordinates are not provided, producing a classic "staircase" cascade.
     */
    private static final class ViewStackingPolicy {

        private static int LASTLEFT = 0;
        private static int LASTTOP  = 0;
        private static final int DEL_H = 40;
        private static final int DEL_V = 20;

        /**
         * Return the initial placement point, advancing the cascade counters
         * if {@code left} or {@code top} were not explicitly specified (≤ 0).
         *
         * @param left explicitly requested left position, or 0 if not set
         * @param top  explicitly requested top position, or 0 if not set
         * @return the resolved initial location
         */
        static Point computeInitialLocation(int left, int top) {
            int x = left;
            int y = top;
            if (x < 1) { x = LASTLEFT; LASTLEFT += DEL_H; }
            if (y < 1) { y = LASTTOP;  LASTTOP  += DEL_V; }
            return new Point(x, y);
        }
    }

    // -----------------------------------------------------------------------
    // FrameConfigurer — apply JInternalFrame decoration settings
    // -----------------------------------------------------------------------

    /**
     * Applies frame-level configuration: title, decorations, close operation,
     * and initial location.
     */
    private static final class FrameConfigurer {

        /**
         * Apply title, decoration flags, close policy, and initial position.
         *
         * @param view the view to configure
         * @param cfg  the parsed configuration
         */
        static void apply(BaseView view, ViewInitConfig cfg) {
            view.setTitle(cfg.title);
            view.setIconifiable (cfg.standardDecorations || cfg.iconifiable);
            view.setMaximizable (cfg.standardDecorations || cfg.maximizable);
            view.setResizable   (cfg.standardDecorations || cfg.resizable);
            view.setClosable    (cfg.standardDecorations || cfg.closable);
            view.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            view.setFrameIcon(null);
            view.setLocation(cfg.left, cfg.top);
        }

        /**
         * Apply explicit bounds to a container-less view.
         *
         * @param view the view to configure
         * @param cfg  the parsed configuration
         */
        static void applyNoContainerBounds(BaseView view, ViewInitConfig cfg) {
            if (cfg.width > 0 && cfg.height > 0) {
                view.setBounds(cfg.left, cfg.top, cfg.width, cfg.height);
            } else {
                view.setLocation(cfg.left, cfg.top);
            }
        }
    }

    // -----------------------------------------------------------------------
    // ContainerFactory — resolve / instantiate the IContainer
    // -----------------------------------------------------------------------

    /**
     * Resolves the {@link IContainer} for a view from its properties.
     */
    private static final class ContainerFactory {

    	   /**
         * Resolve a container from the given properties and world system.
         *
         * <p>Resolution order:</p>
         * <ol>
         *   <li>An {@link IContainer} instance stored directly in the properties
         *       under {@link PropertyUtils#CONTAINER}.</li>
         *   <li>A {@link edu.cnu.mdi.view.ContainerFactory} stored under
         *       {@link PropertyUtils#CONTAINERFACTORY} — the preferred approach
         *       for new code because it is type-safe and IDE-navigable.</li>
         *   <li>A {@link Class} stored under {@link PropertyUtils#CONTAINERCLASS},
         *       instantiated via its {@code (Rectangle2D.Double)} constructor —
         *       retained for backward compatibility.</li>
         *   <li>A default {@link BaseContainer}.</li>
         * </ol>
         *
         * @param props       the view's construction properties
         * @param worldSystem the initial world coordinate rectangle
         * @return a non-null container
         */
        static IContainer resolveContainer(Properties props,
                Rectangle2D.Double worldSystem) {
     
            // 1. Explicit container instance takes highest priority.
            IContainer fromProps = PropertyUtils.getContainer(props);
            if (fromProps != null) {
                return fromProps;
            }
     
            // 2. ContainerFactory functional interface — preferred over Class.
            //    Use the fully-qualified name to disambiguate from this inner class.
            Object rawFactory = props.get(PropertyUtils.CONTAINERFACTORY);
            if (rawFactory instanceof edu.cnu.mdi.view.ContainerFactory) {
                return ((edu.cnu.mdi.view.ContainerFactory) rawFactory)
                        .create(worldSystem);
            }
     
            // 3. Container class via reflection — legacy path, kept for
            //    backward compatibility with existing call sites.
            Object rawClass = props.get(PropertyUtils.CONTAINERCLASS);
            if (rawClass instanceof Class<?>
                    && IContainer.class.isAssignableFrom((Class<?>) rawClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends IContainer> cc =
                        (Class<? extends IContainer>) rawClass;
                return instantiateContainer(cc, worldSystem);
            }
     
            // 4. Fall back to the default container.
            return new BaseContainer(worldSystem);
        }
        /**
         * Instantiate a container class via its {@code (Rectangle2D.Double)}
         * constructor.
         *
         * @param containerClass the class to instantiate
         * @param worldSystem    the world coordinate rectangle
         * @return the new container
         * @throws IllegalArgumentException if instantiation fails
         */
        static IContainer instantiateContainer(
                Class<? extends IContainer> containerClass,
                Rectangle2D.Double worldSystem) {
            try {
                Constructor<? extends IContainer> ctor =
                        containerClass.getConstructor(Rectangle2D.Double.class);
                return ctor.newInstance(worldSystem);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Failed to instantiate container class "
                        + containerClass.getName()
                        + ". Expected a constructor (Rectangle2D.Double).", e);
            }
        }
    }

	// -------------------------------------------------------------------------
	// Post-placement panel additions
	// -------------------------------------------------------------------------

	/**
	 * Adds a component to the {@code WEST} slot of this view's content pane,
	 * expanding the frame width to accommodate it.
	 *
	 * <p>
	 * Uses a double {@code invokeLater} to guarantee execution after all
	 * construction-time {@code invokeLater} calls have been processed —
	 * including {@link BaseView}'s deferred {@code setVisible} and
	 * {@link edu.cnu.mdi.view.ViewConfiguration}'s placement call.  A single
	 * {@code invokeLater} is not sufficient because the placement work is
	 * itself queued at the same EDT level as a naive single defer.
	 * </p>
	 *
	 * @param panel the component to place in the WEST slot; must not be null
	 */
	public void addWestPanel(javax.swing.JComponent panel) {
		// Outer invokeLater: runs after the BaseView constructor's setVisible defer.
		SwingUtilities.invokeLater(() ->
			// Inner invokeLater: runs after VirtualView placement, which is itself
			// queued in an invokeLater inside ViewConfiguration.realizeView().
			SwingUtilities.invokeLater(() -> {
				getContentPane().add(panel, BorderLayout.WEST);
				int extra = panel.getPreferredSize().width;
				java.awt.Rectangle r = getBounds();
				setBounds(r.x, r.y, r.width + extra, r.height);
				revalidate();
				repaint();
			})
		);
	}
    // -----------------------------------------------------------------------
    // ViewContentBuilder — compose Swing content for container-backed views
    // -----------------------------------------------------------------------

    /**
     * Builds the Swing component hierarchy for a container-backed view:
     * background, preferred size, optional scroll pane, optional info-button
     * overlay, optional split pane, and optional toolbar.
     */
    private static final class ViewContentBuilder {

        /**
         * Build and install the view's content.
         *
         * @param view the view to populate
         * @param cfg  the parsed configuration
         * @throws IllegalStateException if the view has no container
         */
        static void build(BaseView view, ViewInitConfig cfg) {
            IContainer container = view.container;
            if (container == null) {
                throw new IllegalStateException(
                        "ViewContentBuilder requires a container-backed view.");
            }

            // Background color.
            if (cfg.background != null && container.getComponent() != null) {
                container.getComponent().setBackground(cfg.background);
            }

            // ------------------------------------------------------------------
            // Pin the container component to the requested canvas size.
            //
            // Both preferredSize AND minimumSize are set so that:
            //   • pack() sizes the frame around the requested canvas area
            //     (accounting for insets and toolbar height automatically).
            //   • Later additions of EAST/WEST panels cause the frame to
            //     *expand* rather than squishing the canvas below its
            //     requested size.
            // ------------------------------------------------------------------
            if (cfg.width > 0 && cfg.height > 0
                    && container.getComponent() != null) {
                Dimension canvasSize = new Dimension(cfg.width, cfg.height);
                container.getComponent().setPreferredSize(canvasSize);
                container.getComponent().setMinimumSize(canvasSize);
            }

            // Center component — optionally wrapped in a scroll pane.
            Component center = container.getComponent();
            if (cfg.scrollable && center != null) {
                view.scrollPane = new JScrollPane(center);
                center = view.scrollPane;
            }

            // ------------------------------------------------------------------
            // Add the toolbar FIRST, then the center content.
            // This ensures pack() (called by the constructor after build())
            // measures all child components — toolbar height is included in the
            // final frame height so the canvas is never shrunk by it.
            // ------------------------------------------------------------------

            // Optional toolbar (added before center so pack() sees full height).
            if (cfg.toolBits > 0) {
                view.toolBar = new BaseToolBar(
                        container.getComponent(), null,
                        cfg.toolBits,
                        ARubberband.Policy.RECTANGLE,
                        cfg.boxZoomPolicy);
                view.getContentPane().add(view.toolBar, BorderLayout.NORTH);
                if (container instanceof BaseContainer) {
                    ((BaseContainer) container).setToolBar(view.toolBar);
                }
            }

            // Optional split pane west component.
            if (cfg.splitWestComponent != null) {
                JSplitPane split = new JSplitPane(
                        JSplitPane.HORIZONTAL_SPLIT, false,
                        cfg.splitWestComponent, center);
                split.setResizeWeight(0.0);
                view.getContentPane().add(split, BorderLayout.CENTER);
            } else {
                view.getContentPane().add(center, BorderLayout.CENTER);
            }

            if (cfg.addWheelZoom) {
                installWheelZoom(container);
            }
        }

        /**
         * Install a mouse-wheel zoom listener on the container's component.
         *
         * @param container the container to add wheel zoom to
         */
        private static void installWheelZoom(IContainer container) {
            if (container == null) {
				return;
			}
            Component comp = container.getComponent();
            if (comp == null) {
				return;
			}
            comp.addMouseWheelListener(e -> {
                double r    = e.getPreciseWheelRotation();
                double base = e.isControlDown() || e.isMetaDown() ? 1.04
                            : e.isShiftDown()                     ? 1.20
                            :                                        1.12;
                double factor = Math.max(0.2, Math.min(5.0, Math.pow(base, r)));
                container.scale(factor);
                e.consume();
            });
        }
    }


    // -----------------------------------------------------------------------
    // ViewKeyBindings — keyboard shortcuts
    // -----------------------------------------------------------------------

    /**
     * Installs keyboard bindings on the view's root pane.
     * Currently: Delete / Backspace → delete selected items.
     */
    private static final class ViewKeyBindings {

        /**
         * Install Delete and Backspace bindings that delete selected items in
         * the view's container.
         *
         * @param view the view on which to install the bindings
         */
        static void installDeleteBinding(BaseView view) {
            JComponent target = view.getRootPane();
            InputMap  im = target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = target.getActionMap();

            AbstractAction deleteAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    IContainer container = view.container;
                    if (container == null) {
						return;
					}
                    // Prefer the toolbar's delete handler when available.
                    if (container.getToolBar() instanceof BaseToolBar baseTb
                            && baseTb.hasDeleteTool()) {
                        baseTb.invokeDelete();
                        return;
                    }
                    container.deleteSelectedItems();
                    container.refresh();
                }
            };

            am.put("mdi.delete", deleteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "mdi.delete");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,     0), "mdi.delete");
        }
    }

    // -----------------------------------------------------------------------
    // ViewPersistence — save / restore view state
    // -----------------------------------------------------------------------

    /**
     * Handles serialisation of a view's runtime state (position, size,
     * visibility, maximized flag) to and from a flat {@link Properties} map.
     *
     * <h2>Key format</h2>
     * <p>
     * Keys are written using the <em>dotted</em> format:
     * {@code prefix.x}, {@code prefix.y}, {@code prefix.w},
     * {@code prefix.h}, {@code prefix.visible}, {@code prefix.maximized}.
     * The world-window extents are written as {@code prefix.xmin},
     * {@code prefix.ymin}, {@code prefix.xmax}, {@code prefix.ymax}.
     * </p>
     *
     * <h2>Backward compatibility (read-side only)</h2>
     * <p>
     * When reading back, the legacy <em>non-dotted</em> variants
     * ({@code prefixx}, {@code prefixvisible}, {@code prefixmaxmized}, etc.)
     * are also checked as fallbacks.  This allows files written by older
     * versions of the framework to load without conversion.  The legacy
     * variants are <strong>not</strong> written; the file always contains only
     * the modern dotted keys.
     * </p>
     */
    private static final class ViewPersistence {

        // -------------------------------------------------------------------
        // Restore
        // -------------------------------------------------------------------

        /**
         * Apply persisted properties to {@code view}.
         *
         * <p>Each property is applied only if a value is actually present in
         * {@code props} — missing keys are silently skipped so that a partial
         * or freshly-created configuration file does not reset views to
         * unexpected defaults.</p>
         *
         * @param view  the target view; ignored if {@code null}
         * @param props the source properties; ignored if {@code null} or empty
         */
        static void applyToView(BaseView view, Properties props) {
            if (view == null || props == null || props.isEmpty()) {
                return;
            }

            String prefix = view.getPropertyName();
            String dotted = prefix + ".";

            // ---- Visibility ----
            Boolean vis = getBooleanIfPresent(props,
                    dotted + "visible", prefix + "visible");
            if (vis != null) {
                view.setVisible(vis);
            }

            // ---- Bounds ----
            Integer x = getIntIfPresent(props, dotted + "x", prefix + "x");
            Integer y = getIntIfPresent(props, dotted + "y", prefix + "y");
            // Support both "w" (new) and "width" (also written in older code).
            Integer w = getIntIfPresent(props,
                    dotted + "w", dotted + "width", prefix + "w");
            Integer h = getIntIfPresent(props,
                    dotted + "h", dotted + "height", prefix + "h");

            if (x != null && y != null && w != null && h != null && w > 0 && h > 0) {
                view.setBounds(x, y, w, h);
            }

            // ---- Maximized ----
            Boolean max = getBooleanIfPresent(props,
                    dotted + "maximized",
                    dotted  + "maxmized",   // historical typo — read-side only
                    prefix  + "maximized",
                    prefix  + "maxmized");
            if (max != null) {
                try {
                    view.setMaximum(max);
                } catch (Exception ignored) {
                }
            }

            // ---- On top ----
            Boolean ontop = getBooleanIfPresent(props,
                    dotted + "ontop", prefix + "ontop");
            if (Boolean.TRUE.equals(ontop)) {
                try {
                    view.moveToFront();
                    view.setSelected(true);
                } catch (Exception ignored) {
                }
            }
        }

        // -------------------------------------------------------------------
        // Capture
        // -------------------------------------------------------------------

        /**
         * Capture {@code view}'s current runtime state into a new
         * {@link Properties} object.
         *
         * <p>Only dotted keys are written. The world-window extents are
         * included when the view has a container.</p>
         *
         * @param view the view to capture; must not be {@code null}
         * @return a non-null properties object
         */
        static Properties captureFromView(BaseView view) {
            Properties props = new Properties();

            String prefix = view.getPropertyName();
            if (prefix == null || prefix.isBlank()) {
                // Fallback: sanitise the title directly.
                prefix = sanitizeForKey(view.getTitle());
            }

            final String dotted = prefix + ".";

            // Frame state.
            props.put(dotted + "visible",   Boolean.toString(view.isVisible()));
            props.put(dotted + "maximized", Boolean.toString(view.isMaximum()));
            props.put(dotted + "ontop",     Boolean.toString(view.isOnTop()));

            // Bounds.
            Rectangle vr = view.getBounds();
            props.put(dotted + "x", Integer.toString(vr.x));
            props.put(dotted + "y", Integer.toString(vr.y));
            props.put(dotted + "w", Integer.toString(vr.width));
            props.put(dotted + "h", Integer.toString(vr.height));

            // World-window extents (when container is present).
            if (view.container != null
                    && view.container.getComponent() != null) {
                Rectangle b = view.container.getComponent().getBounds();
                b.x = 0;
                b.y = 0;
                Rectangle2D.Double wr = new Rectangle2D.Double();
                view.container.localToWorld(b, wr);
                props.put(dotted + "xmin", DoubleFormat.doubleFormat(wr.x,         8));
                props.put(dotted + "ymin", DoubleFormat.doubleFormat(wr.y,         8));
                props.put(dotted + "xmax", DoubleFormat.doubleFormat(wr.getMaxX(), 8));
                props.put(dotted + "ymax", DoubleFormat.doubleFormat(wr.getMaxY(), 8));
            }

            return props;
        }

        // -------------------------------------------------------------------
        // Helpers
        // -------------------------------------------------------------------

        /**
         * Return the value of the first key in {@code keys} that exists in
         * {@code props}, or {@code null} if none is present.
         *
         * @param props  the source properties
         * @param keys   candidate keys, tried in order
         * @return the first matching value, or {@code null}
         */
        private static String firstPresent(Properties props, String... keys) {
            if (props == null) {
				return null;
			}
            for (String k : keys) {
                String v = props.getProperty(k);
                if (v != null) {
					return v;
				}
            }
            return null;
        }

        /**
         * Parse the first present key as a {@link Boolean}, or return
         * {@code null} if none of the keys are present.
         *
         * @param props  the source properties
         * @param keys   candidate keys
         * @return parsed boolean, or {@code null}
         */
        private static Boolean getBooleanIfPresent(Properties props,
                String... keys) {
            String v = firstPresent(props, keys);
            return v == null ? null : Boolean.parseBoolean(v.trim());
        }

        /**
         * Parse the first present key as an {@link Integer}, or return
         * {@code null} if none of the keys are present or the value is not a
         * valid integer.
         *
         * @param props  the source properties
         * @param keys   candidate keys
         * @return parsed integer, or {@code null}
         */
        private static Integer getIntIfPresent(Properties props,
                String... keys) {
            String v = firstPresent(props, keys);
            if (v == null) {
				return null;
			}
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }
}