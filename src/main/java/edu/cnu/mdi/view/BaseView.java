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
import java.awt.geom.Rectangle2D;
import java.beans.PropertyVetoException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
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
import edu.cnu.mdi.graphics.rubberband.ARubberband;
import edu.cnu.mdi.graphics.toolbar.AToolBar;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.properties.PropertySupport;
import edu.cnu.mdi.ui.menu.ViewPopupMenu;

/**
 * Base class for all MDI views.
 * <p>
 * A {@code BaseView} is a {@link JInternalFrame} hosted on the application's
 * {@link Desktop}. Most views contain an {@link IContainer} that provides a
 * "world" coordinate system and drawing/interaction support. Some views (e.g.
 * log/config views) can be container-less.
 * </p>
 *
 * <h2>Refactoring goals</h2>
 * <ul>
 * <li>Keep a true drop-in API: existing subclasses should compile
 * unchanged.</li>
 * <li>Centralize property reading: parse once, then act.</li>
 * <li>Split responsibilities into small helper components (nested classes for
 * low friction; can be moved to separate files later).</li>
 * <li>Improve persistence stability: use {@link #VIEWPROPNAME} (not frame
 * title) as the persistence key prefix.</li>
 * <li>Maintain backward compatibility for legacy persistence keys (including
 * the historical misspelling {@code "maxmized"}).</li>
 * </ul>
 *
 * <h2>FlatLaf note</h2>
 * <p>
 * Previous versions repainted the desktop on move/resize to work around
 * look-and-feel quirks. With FlatLaf this is typically unnecessary, so those
 * "repaint quirks" were removed; the component listener methods remain as
 * no-ops for API continuity.
 * </p>
 */
@SuppressWarnings("serial")
public class BaseView extends JInternalFrame
		implements FocusListener, MouseListener, ComponentListener, IFeedbackProvider {

	/**
	 * Stable property name prefix used for persistence.
	 * <p>
	 * IMPORTANT: persistence keys should not be derived from the window title
	 * because titles change. This value is set from
	 * {@link PropertySupport#getPropName(Properties)} when available, otherwise a
	 * reasonable fallback is used.
	 * </p>
	 */
	protected String VIEWPROPNAME = "?";

	/**
	 * Properties provided at construction time (and used as the view's
	 * configuration bag).
	 */
	protected final Properties properties;

	/** The owning desktop (JDesktopPane) hosting internal frames. */
	protected final JDesktopPane desktop;

	/** Optional container hosted by this view. */
	protected IContainer container;

	/** Optional scroll pane used when the view is configured as scrollable. */
	protected JScrollPane scrollPane;

	/** Lazily resolved parent JFrame containing the desktop. */
	private JFrame parentFrame;

	/** View popup menu  */
	protected final ViewPopupMenu viewPopupMenu;
	
	/** Optional toolbar (if configured). */
	private BaseToolBar toolBar;

	/** Optional virtual window item (used by overview/minimap type views). */
	protected VirtualWindowItem virtualItem;


	// --------------------------------------------------------------------
	// Construction
	// --------------------------------------------------------------------

	/**
	 * Construct a BaseView from key/value pairs consumed by
	 * {@link PropertySupport#fromKeyValues(Object...)}.
	 *
	 * @param keyVals alternating key/value pairs.
	 */
	public BaseView(Object... keyVals) {
		this(PropertySupport.fromKeyValues(keyVals));
	}

	/**
	 * Construct a BaseView using the provided properties.
	 *
	 * @param props configuration properties; must be non-null.
	 */
	public BaseView(Properties props) {
		this.properties = (props != null) ? props : new Properties();
		this.desktop = Desktop.getInstance();
		this.viewPopupMenu = new ViewPopupMenu(this);

		setLayout(new BorderLayout(-1, -1));
		// Parse configuration exactly once.
		final ViewInitConfig cfg = ViewInitConfig.from(this.properties);

		// Establish a stable persistence prefix early.
		// Use the configured propName if present; otherwise fall back to title; then
		// class name.
		this.VIEWPROPNAME = stablePropName(cfg);

		// Frame decorations and basic state.
		FrameConfigurer.apply(this, cfg);

		// Listeners (kept for API continuity; some are no-ops).
		addMouseListener(this);
		addFocusListener(this);
		addComponentListener(this);

		// Register the view with the manager before showing.
		ViewManager.getInstance().add(this);

		// Build view content.
		if (cfg.hasWorldSystem()) {
			this.container = resolveContainer(cfg.worldSystem);
			this.container.setView(this);
			ViewContentBuilder.build(this, cfg);
			ViewKeyBindings.installDeleteBinding(this);
			pack();
		} else {
			// Container-less views: we still position/bounds as configured.
			FrameConfigurer.applyNoContainerBounds(this, cfg);
		}

		// Add to desktop.
		if (desktop != null) {
			desktop.add(this, 0);
		}

		if ((this.container != null) && this.container instanceof BaseContainer) {
			getViewPopupMenu().addSeparator();
			getViewPopupMenu().add(LayerInspectorDialog.createMenuItem(this));
		}

		// Visibility: schedule after add to desktop to avoid flicker/ordering issues.
		if (cfg.visible) {
			SwingUtilities.invokeLater(() -> setVisible(true));
		}
	}

	/**
	 * Choose a stable property-name prefix for persistence.
	 * <p>
	 * Preference order: explicit propName property, then title, then simple class
	 * name.
	 * </p>
	 */
	private String stablePropName(ViewInitConfig cfg) {
		String pn = cfg.propName;
		if (pn != null && !pn.isBlank()) {
			return pn.trim();
		}
		String t = cfg.title;
		if (t != null && !t.isBlank()) {
			return t.trim();
		}
		return getClass().getSimpleName();
	}

	// --------------------------------------------------------------------
	// Container creation (public/protected API preserved)
	// --------------------------------------------------------------------

	/**
	 * Resolve the container for this view using the view's {@link #properties}.
	 * <p>
	 * The container may be provided directly via properties, or via a container
	 * class property, otherwise a {@link BaseContainer} is used.
	 * </p>
	 *
	 * @param worldSystem the world-system rectangle.
	 * @return a non-null container instance.
	 */
	protected IContainer resolveContainer(Rectangle2D.Double worldSystem) {
		return ContainerFactory.resolveContainer(this.properties, worldSystem);
	}

	// --------------------------------------------------------------------
	// Standard accessors
	// --------------------------------------------------------------------

	/**
	 * Override the insets slightly to match legacy look.
	 */
	@Override
	public Insets getInsets() {
		Insets defInsets = super.getInsets();
		return new Insets(defInsets.top, defInsets.left, 2, defInsets.right);
	}

	/**
	 * Get the owning parent frame that hosts the desktop.
	 *
	 * @return the parent {@link JFrame} or {@code null} if not found.
	 */
	public JFrame getParentFrame() {
		if (parentFrame == null) {
			parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(Frame.class, this);
		}
		return parentFrame;
	}

	/**
	 * Get the container hosted by this view (if any).
	 *
	 * @return the container, or {@code null} for container-less views.
	 */
	public IContainer getContainer() {
		return container;
	}

	/**
	 * Legacy behavior: view "name" equals its title.
	 * <p>
	 * NOTE: persistence no longer uses {@code getName()} because titles can change.
	 * Persistence uses {@link #getPropertyName()}.
	 * </p>
	 */
	@Override
	public String getName() {
		return getTitle();
	}

	/**
	 * Returns the stable property name used for persistence.
	 *
	 * @return the stable property name (never null after construction).
	 */
	public String getPropertyName() {
		return VIEWPROPNAME;
	}

	/**
	 * Returns the popup menu for this view.
	 *
	 * @return the view popup menu; never null.
	 */
	public ViewPopupMenu getViewPopupMenu() {
		return viewPopupMenu;
	}

	/**
	 * Add a "quick zoom" preset to the view popup menu.
	 *
	 * @param title the menu label.
	 * @param xmin  world xmin.
	 * @param ymin  world ymin.
	 * @param xmax  world xmax.
	 * @param ymax  world ymax.
	 */
	public void addQuickZoom(String title, double xmin, double ymin, double xmax, double ymax) {
		viewPopupMenu.addQuickZoom(title, xmin, ymin, xmax, ymax);
	}

	/**
	 * @return {@code true} if this view wraps its container in a
	 *         {@link JScrollPane}.
	 */
	public boolean isScrollable() {
		return (scrollPane != null);
	}

	/**
	 * @return the scroll pane if configured scrollable, otherwise null.
	 */
	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	/**
	 * Optional hook for subclasses: called when a right click is detected. Default
	 * returns false (not handled).
	 *
	 * @param mouseEvent the mouse event.
	 * @return true if handled.
	 */
	public boolean rightClicked(MouseEvent mouseEvent) {
		return false;
	}

	/**
	 * Refresh the view (delegates to the container if present and visible).
	 */
	public void refresh() {
		if (isViewVisible() && container != null) {
			container.refresh();
		}
	}

	/**
	 * Returns whether any portion of the internal frame is visible on the desktop.
	 *
	 * @return true if the frame bounds intersect the parent bounds.
	 */
	public boolean isViewVisible() {
		if (getParent() == null) {
			return false;
		}
		Rectangle frameBounds = getBounds();
		Rectangle parentBounds = getParent().getBounds();
		return parentBounds.intersects(frameBounds);
	}

	/**
	 * Returns whether this view is on top of the desktop's z-order.
	 * <p>
	 * This matches the legacy logic: if selected, true; otherwise checks the
	 * desktop's visible frames list ordering.
	 * </p>
	 *
	 * @return true if this frame is on top among showing frames.
	 */
	public boolean isOnTop() {
		if (isSelected()) {
			return true;
		}
		if (!isShowing() || desktop == null) {
			return false;
		}
		JInternalFrame[] allFrames = desktop.getAllFrames();
		for (JInternalFrame frame : allFrames) {
			if (frame.isShowing()) {
				return frame == this;
			}
		}
		return false;
	}

	/**
	 * Optional hook used by some views to provide a clip shape.
	 *
	 * @return a special clip shape, or null.
	 */
	public Shape getSpecialClip() {
		return null;
	}

	/**
	 * Convenience method to move the view by an offset.
	 *
	 * @param dh delta x.
	 * @param dv delta y.
	 */
	public void offset(int dh, int dv) {
		Rectangle b = getBounds();
		b.x += dh;
		b.y += dv;
		setBounds(b);
	}

	// --------------------------------------------------------------------
	// Feedback integration
	// --------------------------------------------------------------------

	/**
	 * Create and install a feedback pane with default colors and font size.
	 *
	 * @return the created feedback pane.
	 */
	protected FeedbackPane initFeedback() {
		return initFeedback(Color.cyan, Color.black, 9);
	}

	/**
	 * Create and install a feedback pane with provided colors and font size.
	 * <p>
	 * The feedback pane is registered with the container's {@link FeedbackControl}.
	 * </p>
	 *
	 * @param fg       foreground color.
	 * @param bg       background color.
	 * @param fontSize feedback font size.
	 * @return the created feedback pane.
	 */
	protected FeedbackPane initFeedback(Color fg, Color bg, int fontSize) {
		if (getContainer() == null) {
			throw new IllegalStateException("initFeedback requires a container-backed view.");
		}
		FeedbackControl fbc = getContainer().getFeedbackControl();
		fbc.addFeedbackProvider(this);

		FeedbackPane fbp = new FeedbackPane(fg, bg, fontSize);
		fbp.setBorder(null);
		getContainer().setFeedbackPane(fbp);
		return fbp;
	}

	/**
	 * Default feedback implementation: no-op.
	 */
	@Override
	public void getFeedbackStrings(IContainer container, Point pp, java.awt.geom.Point2D.Double wp,
			List<String> feedbackStrings) {
		// no-op
	}

	/**
	 * Get the optional virtual window item associated with this view.
	 * <p>
	 * Some overview/minimap-style views keep a {@link VirtualWindowItem} that
	 * reflects or controls the visible world window of this view.
	 * </p>
	 *
	 * @return the virtual window item, or {@code null} if none is set.
	 */
	public VirtualWindowItem getVirtualItem() {
		return virtualItem;
	}
	
	/**
	 * Apply a focus-fix mouse listener to the given menu that selects the view
	 * when the mouse enters the menu. This works around focus issues on some platforms
	 * and look-and-feels. The symptom is that the view does not gain focus when the
	 * menu is shown and you get a menu "flash".
	 * 
	 * @param menu the menu to apply the fix to.
	 * @param view the view to select.
	 */
	public static void applyFocusFix(JMenu menu, BaseView view) {
	    menu.addMouseListener(new MouseAdapter() {
	        @Override
	        public void mouseEntered(MouseEvent e) {
	            try {
	                if (view.isIcon()) view.setIcon(false); // Ensure it's not minimized
	                if (!view.isSelected()) view.setSelected(true);
	            } catch (Exception ex) { /* Vetoed */ }
	        }
	    });
	}


	// --------------------------------------------------------------------
	// Magnify support
	// --------------------------------------------------------------------

	/**
	 * Handle a magnify request (typically on a modifier + mouse).
	 *
	 * @param me the triggering mouse event.
	 */
	public void handleMagnify(final MouseEvent me) {
		
		//check if on EDT thread
		
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> handleMagnify(me));
			return;
		}
		
		IContainer cont = getContainer();
		if (cont instanceof BaseContainer) {
			MagnifyWindow.magnify((BaseContainer) cont, me);
		}

		if (container != null) {
			container.refresh();
		}
	}

	// --------------------------------------------------------------------
	// Persistence (API preserved; behavior improved)
	// --------------------------------------------------------------------

	/**
	 * Restore view state from previously captured properties.
	 * <p>
	 * This method supports both the newer "dotted" keys (e.g.
	 * {@code prefix.visible}) and the legacy non-dotted keys (e.g.
	 * {@code prefixvisible}). It also supports the historical misspelling
	 * {@code maxmized}.
	 * </p>
	 *
	 * @param properties the properties to read from; may be null.
	 */
	public void setFromProperties(Properties properties) {
		ViewPersistence.applyToView(this, properties);
	}

	/**
	 * Capture view configuration state to a {@link Properties} object.
	 * <p>
	 * Uses {@link #getPropertyName()} as the stable prefix. Writes both the newer
	 * dotted keys and a small set of legacy keys for backward compatibility.
	 * </p>
	 *
	 * @return properties representing this view's persistent state (never null).
	 */
	public Properties getConfigurationProperties() {
		return ViewPersistence.captureFromView(this);
	}

	/**
	 * Returns the properties that were used to construct this view.
	 *
	 * @return the underlying properties of this view; never null.
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * Convenience method to read a boolean property from this view's internal
	 * properties.
	 *
	 * @param key the property key.
	 * @return the parsed boolean value, or false if missing/invalid.
	 */
	public boolean checkBooleanProperty(String key) {
		return PropertySupport.getBoolean(properties, key, false);
	}

	/**
	 * Store a boolean property into this view's internal properties.
	 *
	 * @param key the property key.
	 * @param val the value to store.
	 */
	public void setBooleanProperty(String key, boolean val) {
		properties.put(key, val ? "true" : "false");
	}

	/**
	 * Reads an integer property from this view's internal properties.
	 *
	 * @param key the property key.
	 * @return the parsed integer value, or -1 if missing/invalid.
	 */
	public int getIntProperty(String key) {
		return PropertySupport.getInt(properties, key, -1);
	}

	/**
	 * Stores an integer property into this view's internal properties.
	 *
	 * @param key the property key.
	 * @param val the value.
	 */
	public void setIntProperty(String key, int val) {
		properties.put(key, Integer.toString(val));
	}

	/**
	 * Set the optional virtual item for this view.
	 *
	 * @param virtualItem the virtual item.
	 */
	public void setVirtualItem(VirtualWindowItem virtualItem) {
		this.virtualItem = virtualItem;
	}

	// --------------------------------------------------------------------
	// Listener implementations (kept for compatibility)
	// --------------------------------------------------------------------

	@Override
	public void focusGained(FocusEvent e) {
		// no-op
	}

	@Override
	public void focusLost(FocusEvent e) {
		// no-op
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// no-op
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// no-op
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// no-op
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// no-op
	}

	@Override
	public void mouseExited(MouseEvent e) {
		MagnifyWindow.closeMagnifyWindow();
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		// no-op
	}

	@Override
	public void componentShown(ComponentEvent e) {
		// no-op
	}

	/**
	 * With FlatLaf, we no longer force desktop repaints on move. Kept as a no-op
	 * for API continuity.
	 */
	@Override
	public void componentMoved(ComponentEvent e) {
		// no-op
	}

	/**
	 * With FlatLaf, we no longer force desktop repaints on resize. Kept as a no-op
	 * for API continuity.
	 */
	@Override
	public void componentResized(ComponentEvent e) {
		// no-op
	}

	// =====================================================================================
	// Helper components (nested for drop-in refactor)
	// =====================================================================================

	/**
	 * Immutable parsed configuration for initializing a {@link BaseView}.
	 * <p>
	 * All reads from {@link PropertySupport} happen here, so the rest of the
	 * constructor can act on a stable configuration object.
	 * </p>
	 */
	private static final class ViewInitConfig {
		final String title;
		final String propName;

		final boolean standardDecorations;
		final boolean iconifiable;
		final boolean maximizable;
		final boolean resizable;
		final boolean closable;

		final boolean scrollable;
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

		private ViewInitConfig(String title, String propName, boolean standardDecorations, boolean iconifiable,
				boolean maximizable, boolean resizable, boolean closable, boolean scrollable, boolean visible, int left,
				int top, int width, int height, Rectangle2D.Double worldSystem, Color background,
				JComponent splitWestComponent, long toolBits, ARubberband.Policy boxZoomPolicy) {
			this.title = title;
			this.propName = propName;
			this.standardDecorations = standardDecorations;
			this.iconifiable = iconifiable;
			this.maximizable = maximizable;
			this.resizable = resizable;
			this.closable = closable;
			this.scrollable = scrollable;
			this.visible = visible;
			this.left = left;
			this.top = top;
			this.width = width;
			this.height = height;
			this.worldSystem = worldSystem;
			this.background = background;
			this.splitWestComponent = splitWestComponent;
			this.toolBits = toolBits;
			this.boxZoomPolicy = boxZoomPolicy;
		}

		boolean hasWorldSystem() {
			return worldSystem != null;
		}

		static ViewInitConfig from(Properties props) {
			String title = PropertySupport.getTitle(props);
			if (title == null) {
				title = "A View";
			}

			boolean standardDecorations = PropertySupport.getStandardViewDecorations(props);
			boolean iconifiable = PropertySupport.getIconifiable(props);
			boolean maximizable = PropertySupport.getMaximizable(props);
			boolean resizable = PropertySupport.getResizable(props);
			boolean closable = PropertySupport.getClosable(props);

			boolean scrollable = PropertySupport.getScrollable(props);
			boolean visible = PropertySupport.getVisible(props);

			String propName = PropertySupport.getPropName(props);

			int left = PropertySupport.getLeft(props);
			int top = PropertySupport.getTop(props);

			int width = Math.max(100, PropertySupport.getWidth(props));
			int height = Math.max(100, PropertySupport.getHeight(props));

			// Support "fraction + aspect ratio" sizing (legacy feature).
			double fraction = PropertySupport.getFraction(props);
			if (Double.isFinite(fraction) && (fraction > 0.0) && (fraction < 1.0)) {
				BaseMDIApplication app = BaseMDIApplication.getApplication();
				if (app != null) {
					Dimension appSize = app.getSize();
					double aspect = PropertySupport.getAspectRatio(props);
					height = (int) (fraction * appSize.height);
					width = (int) (height * aspect);
				}
			}

			// Cascading/stacking policy if left/top not specified.
			Point p = ViewStackingPolicy.computeInitialLocation(left, top);
			left = p.x;
			top = p.y;

			Rectangle2D.Double worldSystem = PropertySupport.getWorldSystem(props);

			Color background = PropertySupport.getBackground(props);

			JComponent west = PropertySupport.getSplitWestComponent(props);

			long bits = PropertySupport.getToolbarBits(props);
			ARubberband.Policy policy = PropertySupport.getBoxZoomRubberbandPolicy(props);

			return new ViewInitConfig(title, propName, standardDecorations, iconifiable, maximizable, resizable,
					closable, scrollable, visible, left, top, width, height, worldSystem, background, west, bits,
					policy);
		}
	}

	/**
	 * Cascading placement policy used when left/top are not explicitly provided.
	 * Kept as static fields to preserve legacy "stacking" behavior across created
	 * views.
	 */
	private static final class ViewStackingPolicy {
		private static int LASTLEFT = 0;
		private static int LASTTOP = 0;
		private static final int DEL_H = 40;
		private static final int DEL_V = 20;

		static Point computeInitialLocation(int left, int top) {
			int x = left;
			int y = top;

			if (x < 1) {
				x = LASTLEFT;
				LASTLEFT += DEL_H;
			}
			if (y < 1) {
				y = LASTTOP;
				LASTTOP += DEL_V;
			}
			return new Point(x, y);
		}
	}

	/**
	 * Applies frame-level configuration (decorations, close operation, title,
	 * etc.).
	 */
	private static final class FrameConfigurer {

		static void apply(BaseView view, ViewInitConfig cfg) {
			view.setTitle(cfg.title);

			// Standard decorations override individual flags.
			view.setIconifiable(cfg.standardDecorations || cfg.iconifiable);
			view.setMaximizable(cfg.standardDecorations || cfg.maximizable);
			view.setResizable(cfg.standardDecorations || cfg.resizable);
			view.setClosable(cfg.standardDecorations || cfg.closable);

			view.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
			view.setFrameIcon(null);

			// For container-backed views: location is applied here; bounds/pack handled
			// later.
			view.setLocation(cfg.left, cfg.top);
		}

		static void applyNoContainerBounds(BaseView view, ViewInitConfig cfg) {
			if (cfg.width > 0 && cfg.height > 0) {
				view.setBounds(cfg.left, cfg.top, cfg.width, cfg.height);
			} else {
				view.setLocation(cfg.left, cfg.top);
			}
		}
	}

	/**
	 * Creates/resolves the container instance based on properties.
	 */
	private static final class ContainerFactory {

		static IContainer resolveContainer(Properties props, Rectangle2D.Double worldSystem) {
			// If a container instance is explicitly provided, use it.
			IContainer fromProps = PropertySupport.getContainer(props);
			if (fromProps != null) {
				return fromProps;
			}

			// If a container class is specified, instantiate it using the expected
			// constructor signature.
			Object containerClassObj = props.get(PropertySupport.CONTAINERCLASS);
			if (containerClassObj instanceof Class<?> rawClass && IContainer.class.isAssignableFrom(rawClass)) {

				@SuppressWarnings("unchecked")
				Class<? extends IContainer> containerClass = (Class<? extends IContainer>) rawClass;
				return instantiateContainer(containerClass, worldSystem);
			}

			// Default container.
			return new BaseContainer(worldSystem);
		}

		static IContainer instantiateContainer(Class<? extends IContainer> containerClass,
				Rectangle2D.Double worldSystem) {
			try {
				Constructor<? extends IContainer> ctor = containerClass.getConstructor(Rectangle2D.Double.class);
				return ctor.newInstance(worldSystem);
			} catch (Exception e) {
				throw new IllegalArgumentException("Failed to instantiate container class " + containerClass.getName()
						+ ". Expected a constructor (Rectangle2D.Double).", e);
			}
		}
	}

	/**
	 * Composes the Swing content for container-backed views: margins, background,
	 * preferred size, scroll pane, optional split pane, optional toolbar.
	 */
	private static final class ViewContentBuilder {

		static void build(BaseView view, ViewInitConfig cfg) {
			IContainer container = view.container;

			if (container == null) {
				throw new IllegalStateException("ViewContentBuilder requires a container-backed view.");
			}

			// Configure background.
			if (cfg.background != null && container.getComponent() != null) {
				container.getComponent().setBackground(cfg.background);
			}

			// Preferred size (applies to container component).
			if (cfg.width > 0 && cfg.height > 0 && container.getComponent() != null) {
				container.getComponent().setPreferredSize(new Dimension(cfg.width, cfg.height));
			}

			// Center component (container component optionally wrapped in scroll pane).
			Component center = container.getComponent();
			if (cfg.scrollable && center != null) {
				view.scrollPane = new JScrollPane(center);
				center = view.scrollPane;
			}

			// Optional split pane west component.
			if (cfg.splitWestComponent != null) {
				JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, cfg.splitWestComponent, center);
				split.setResizeWeight(0.0);
				view.getContentPane().add(split, BorderLayout.CENTER);
			} else {
				view.getContentPane().add(center, BorderLayout.CENTER);
			}

			// Optional toolbar.
			if (cfg.toolBits > 0) {
				view.toolBar = new BaseToolBar(container.getComponent(), null, 
						cfg.toolBits, ARubberband.Policy.RECTANGLE, cfg.boxZoomPolicy);
				view.getContentPane().add(view.toolBar, BorderLayout.NORTH);
				if (container instanceof BaseContainer baseCont) {
					baseCont.setToolBar(view.toolBar);
				}
			}
		}
	}
	
	/**
	 * Returns the optional toolbar for this view.
	 *
	 * @return the toolbar, or null if none is configured.
	 */
	public AToolBar getToolBar() {
		return toolBar;
	}

	/**
	 * Installs key bindings for this view (currently delete/backspace -> delete
	 * selected items).
	 */
	private static final class ViewKeyBindings {

		static void installDeleteBinding(BaseView view) {
			JComponent target = view.getRootPane();
			InputMap im = target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
			ActionMap am = target.getActionMap();

			AbstractAction deleteAction = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					IContainer container = view.container;
					if (container == null) {
						return;
					}

					// Preferred path: toolbar's delete if present (keeps behavior consistent with
					// UI).
					if (container.getToolBar() instanceof BaseToolBar baseTb && baseTb.hasDeleteTool()) {
						baseTb.invokeDelete();
						return;
					}

					// Fallback: container deletion.
					container.deleteSelectedItems();
					container.refresh();
				}
			};

			am.put("mdi.delete", deleteAction);
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "mdi.delete");
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "mdi.delete");
		}
	}

	/**
	 * Captures/restores persistent properties for a view.
	 * <p>
	 * Uses the stable prefix {@link BaseView#getPropertyName()}. Supports old key
	 * variants for backward compatibility.
	 * </p>
	 */
	private static final class ViewPersistence {

		static void applyToView(BaseView view, Properties props) {
			if (props == null) {
				return;
			}

			String prefixBase = view.getPropertyName();
			if (prefixBase == null || prefixBase.isBlank()) {
				// last-resort fallback (shouldn't happen after construction)
				prefixBase = view.getTitle();
			}
			if (prefixBase == null || prefixBase.isBlank()) {
				return;
			}

			String dotted = prefixBase + ".";

			// Bounds
			Rectangle viewRect = rectangleFromProperties(dotted, props);
			if (viewRect == null) {
				// legacy without dot (very old)
				viewRect = rectangleFromProperties(prefixBase, props);
			}
			if (viewRect != null) {
				view.setBounds(viewRect);

				// World window
				if (view.container != null) {
					Rectangle2D.Double wr = worldRectangleFromProperties(dotted, props);
					if (wr == null) {
						wr = worldRectangleFromProperties(prefixBase, props);
					}
					if (wr != null) {
						view.container.zoom(wr.getMinX(), wr.getMaxX(), wr.getMinY(), wr.getMaxY());
					}
				}
			}

			boolean vis = readBoolean(props, dotted + "visible", prefixBase + "visible", false);

			boolean ontop = readBoolean(props, dotted + "ontop", prefixBase + "ontop", false);

			// Support both correct and legacy misspelling
			boolean maximized = readBoolean(props, dotted + "maximized", dotted + "maxmized", false);
			maximized = maximized || readBoolean(props, prefixBase + "maximized", prefixBase + "maxmized", false);

			view.setVisible(vis);

			if (ontop) {
				view.toFront();
			}
			if (maximized) {
				try {
					view.setMaximum(true);
				} catch (PropertyVetoException e) {
					e.printStackTrace();
				}
			}
		}

		static Properties captureFromView(BaseView view) {
			Properties props = new Properties();

			String prefixBase = view.getPropertyName();
			if (prefixBase == null || prefixBase.isBlank()) {
				prefixBase = view.getTitle();
			}
			if (prefixBase == null || prefixBase.isBlank()) {
				return props;
			}

			String dotted = prefixBase + ".";

			// Basic frame state (new dotted keys)
			props.put(dotted + "visible", Boolean.toString(view.isVisible()));
			props.put(dotted + "closed", Boolean.toString(view.isClosed()));
			props.put(dotted + "icon", Boolean.toString(view.isIcon()));
			props.put(dotted + "ontop", Boolean.toString(view.isOnTop()));
			props.put(dotted + "maximized", Boolean.toString(view.isMaximum()));

			// Backward-compat: legacy no-dot keys for the booleans that historically used
			// them
			props.put(prefixBase + "visible", Boolean.toString(view.isVisible()));
			props.put(prefixBase + "ontop", Boolean.toString(view.isOnTop()));
			// Write both correct and misspelled maximized keys so old readers keep working
			props.put(prefixBase + "maximized", Boolean.toString(view.isMaximum()));
			props.put(prefixBase + "maxmized", Boolean.toString(view.isMaximum()));
			props.put(dotted + "maxmized", Boolean.toString(view.isMaximum()));

			// Bounds (dotted keys)
			Rectangle vr = view.getBounds();
			props.put(dotted + "x", Integer.toString(vr.x));
			props.put(dotted + "y", Integer.toString(vr.y));
			props.put(dotted + "width", Integer.toString(vr.width));
			props.put(dotted + "height", Integer.toString(vr.height));

			// World window (dotted keys)
			if (view.container != null && view.container.getComponent() != null) {
				Rectangle b = view.container.getComponent().getBounds();
				b.x = 0;
				b.y = 0;

				Rectangle2D.Double wr = new Rectangle2D.Double();
				view.container.localToWorld(b, wr);

				props.put(dotted + "xmin", DoubleFormat.doubleFormat(wr.x, 8));
				props.put(dotted + "ymin", DoubleFormat.doubleFormat(wr.y, 8));
				props.put(dotted + "xmax", DoubleFormat.doubleFormat(wr.getMaxX(), 8));
				props.put(dotted + "ymax", DoubleFormat.doubleFormat(wr.getMaxY(), 8));
			}

			return props;
		}

		private static boolean readBoolean(Properties props, String key1, String key2, boolean defVal) {
			String v = props.getProperty(key1);
			if (v == null) {
				v = props.getProperty(key2);
			}
			return (v == null) ? defVal : Boolean.parseBoolean(v);
		}

		private static Rectangle2D.Double worldRectangleFromProperties(String prefix, Properties properties) {
			String xminStr = properties.getProperty(prefix + "xmin");
			String yminStr = properties.getProperty(prefix + "ymin");
			String xmaxStr = properties.getProperty(prefix + "xmax");
			String ymaxStr = properties.getProperty(prefix + "ymax");

			if (xminStr == null || yminStr == null || xmaxStr == null || ymaxStr == null) {
				return null;
			}

			try {
				double xmin = Double.parseDouble(xminStr);
				double ymin = Double.parseDouble(yminStr);
				double xmax = Double.parseDouble(xmaxStr);
				double ymax = Double.parseDouble(ymaxStr);
				return new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		private static Rectangle rectangleFromProperties(String prefix, Properties properties) {
			String xStr = properties.getProperty(prefix + "x");
			String yStr = properties.getProperty(prefix + "y");
			String wStr = properties.getProperty(prefix + "width");
			String hStr = properties.getProperty(prefix + "height");

			if (xStr == null || yStr == null || wStr == null || hStr == null) {
				return null;
			}

			try {
				int x = Integer.parseInt(xStr);
				int y = Integer.parseInt(yStr);
				int w = Integer.parseInt(wStr);
				int h = Integer.parseInt(hStr);

				if (w <= 2 || h <= 2) {
					return null;
				}
				return new Rectangle(x, y, w, h);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}
}
