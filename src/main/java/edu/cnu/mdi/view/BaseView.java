package edu.cnu.mdi.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.OverlayLayout;
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
import edu.cnu.mdi.properties.PropertyUtils;
import edu.cnu.mdi.transfer.IFileDropHandler;
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
		implements FocusListener, MouseListener, ComponentListener, IFeedbackProvider, IFileDropHandler {

	/**
	 * Stable property name prefix used for persistence.
	 * <p>
	 * IMPORTANT: persistence keys should not be derived from the window title
	 * because titles change. This value is set from
	 * {@link PropertyUtils#getPropName(Properties)} when available, otherwise a
	 * reasonable fallback is used.
	 * </p>
	 */
	protected String VIEWPROPNAME = "?";

	/** Dialog used to show view information (lazily created). */
	protected JDialog infoDialog;
	protected static final Icon infoIcon;
	static {
		String path = ToolBits.getResourcePath(ToolBits.INFO);
		infoIcon = ImageManager.getInstance().loadUiIcon(path, BaseToolBar.DEFAULT_ICON_SIZE,
				BaseToolBar.DEFAULT_ICON_SIZE);
	}

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

	/** View popup menu */
	protected final ViewPopupMenu viewPopupMenu;

	/** Optional toolbar (if configured). */
	private BaseToolBar toolBar;

	/** Optional virtual window item (used by the virtual view). */
	protected VirtualWindowItem virtualItem;

	/** Optional file filter for drag-and-drop operations. */
	private Predicate<File> fileFilter = null;

	// --------------------------------------------------------------------
	// Construction
	// --------------------------------------------------------------------

	/**
	 * Construct a BaseView from key/value pairs consumed by
	 * {@link PropertyUtils#fromKeyValues(Object...)}.
	 *
	 * @param keyVals alternating key/value pairs.
	 */
	public BaseView(Object... keyVals) {
		this(PropertyUtils.fromKeyValues(keyVals));
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
			installViewPopupTrigger();
			ViewKeyBindings.installDeleteBinding(this);
			pack();
		} else {
			// No world system: skip container and related setup; just apply basic frame
			// config.
			FrameConfigurer.applyNoContainerBounds(this, cfg);
		}

		// Add to desktop.
		if (desktop != null) {
			desktop.add(this, 0);
		}

		if ((this.container != null) && this.container instanceof BaseContainer) {
			getViewPopupMenu().add(LayerInspectorDialog.createMenuItem(this));
		}

		// Visibility: schedule after add to desktop to avoid flicker/ordering issues.
		if (cfg.visible) {
			SwingUtilities.invokeLater(() -> setVisible(true));
		}
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
	 * Optional hook for views to prepare for application closing. Default does
	 * nothing.
	 */
	public void prepareForExit() {
		// Default implementation does nothing;
		// override as needed, e,g, to stop background threads or save state.
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
	 * Set a file filter, e.g., f -> f.getName().endsWith(".png"). Used for drag and
	 * drop.
	 * 
	 * @param filter the file filter
	 * @see IFileDropHandler
	 */
	@Override
	public void setFileFilter(Predicate<File> filter) {
		this.fileFilter = filter;
	}

	/**
	 * Get the file filter used for drag and drop.
	 * 
	 * @return the file filter, or null if none is set.
	 */
	@Override
	public Predicate<File> getFileFilter() {
		return this.fileFilter;
	}

	/**
	 * Handle files dropped on this view through drag and drop.
	 *
	 * @param files the dropped files.
	 */
	@Override
	public void filesDropped(List<File> files) {
		// no-op see PlotView and DrawingView for example override;
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
	 * Install a right-click popup trigger on the view's canvas so that the view
	 * popup menu appears even when no toolbar tool is selected.
	 * <p>
	 * This handles platform differences where popup triggers may fire on either
	 * {@code mousePressed} (macOS) or {@code mouseReleased} (Windows/Linux). The
	 * popup is only shown when the click is <em>not</em> on an item; item-level
	 * popups (handled elsewhere) take precedence.
	 * </p>
	 */
	private void installViewPopupTrigger() {

		if (container == null) {
			return;
		}

		final Component canvas = container.getComponent();
		if (canvas == null) {
			return;
		}

		// Canvas listener: right-click on empty space -> view popup.
		canvas.addMouseListener(new MouseAdapter() {

			private void maybeShow(MouseEvent e) {
				// If the click is on an item, let item-level logic handle it.
				if (e == null || !e.isPopupTrigger() || (container.getItemAtPoint(e.getPoint()) != null)) {
					return;
				}

				ViewPopupMenu menu = getViewPopupMenu();
				if (menu != null) {
					menu.show(e.getComponent(), e.getX(), e.getY());
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				maybeShow(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShow(e);
			}
		});

		// If scrollable, also listen on the viewport; translate point to canvas coords.
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

				@Override
				public void mousePressed(MouseEvent e) {
					maybeShow(e);
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					maybeShow(e);
				}
			});
		}
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
		IContainer container = getContainer();
		container.setFeedbackPane(fbp);
		MouseMotionListener mml = new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
				container.feedbackTrigger(e, false);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				container.feedbackTrigger(e, true);
			}
		};
		container.getComponent().addMouseMotionListener(mml);
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
	 * Apply a focus-fix mouse listener to the given menu that selects the view when
	 * the mouse enters the menu. This works around focus issues on some platforms
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
					if (view.isIcon()) {
						view.setIcon(false); // Ensure it's not minimized
					}
					if (!view.isSelected()) {
						view.setSelected(true);
					}
				} catch (Exception ex) {
					/* Vetoed */ }
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

		// check if on EDT thread

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
		return PropertyUtils.getBoolean(properties, key, false);
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
		return PropertyUtils.getInt(properties, key, -1);
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

	/**
	 * Optional hook for subclasses to provide view information content. Default is
	 * null (no info).
	 *
	 * @return the view info, or null if none.
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
			JOptionPane.showMessageDialog(this, "No detailed information is available for this view.", "View Info",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	// Subclasses can override this to provide structured information about the
	// view.
	private static void installWheelZoom(IContainer container) {

		if (container == null) {
			return;
		}

		Component comp = container.getComponent();
		if (comp == null) {
			return;
		}

		comp.addMouseWheelListener(e -> {
			double r = e.getPreciseWheelRotation();

			double base = 1.12;
			if (e.isControlDown() || e.isMetaDown()) {
				base = 1.04;
			} else if (e.isShiftDown()) {
				base = 1.20;
			}

			double factor = Math.pow(base, r);
			factor = Math.max(0.2, Math.min(5.0, factor)); // defensive clamp

			container.scale(factor);
			e.consume();
		});
	}

	/**
	 * Helper component: Wraps the view content and overlays a floating info button
	 * in the top-right corner.
	 */
	private static class ViewWrapper extends JLayeredPane {

		public ViewWrapper(JComponent viewContent, BaseView view) {
			setLayout(new OverlayLayout(this));

			// 1. The Info Button (Foreground Layer)
			JButton infoButton = new JButton();
			infoButton.setIcon(infoIcon);

			// Styling: Small, semi-transparent, flat look
			infoButton.setMargin(new Insets(0, 0, 0, 0));
			infoButton.setPreferredSize(new Dimension(infoIcon.getIconWidth(), infoIcon.getIconHeight()));
			infoButton.setFocusable(false);
			infoButton.setBorderPainted(false);
			infoButton.setContentAreaFilled(false);
			infoButton.setBackground(new Color(255, 255, 255, 160)); // Slight transparency
			infoButton.setToolTipText("View Information");
			infoButton.setBorder(BorderFactory.createLineBorder(Color.GRAY));

			infoButton.addActionListener(e -> {
				view.viewInfo();
			});

			// 2. Container to align button Top-Right
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
			buttonPanel.setOpaque(false); // Transparent so map shows through
			buttonPanel.add(infoButton);

			// 3. Add layers (First added = Topmost in OverlayLayout)
			add(buttonPanel);
			add(viewContent);
		}
	}

	// =====================================================================================
	// Helper components (nested for drop-in refactor)
	// =====================================================================================

	/**
	 * Immutable parsed configuration for initializing a {@link BaseView}.
	 * <p>
	 * All reads from {@link PropertyUtils} happen here, so the rest of the
	 * constructor can act on a stable configuration object.
	 * </p>
	 */
	private static final class ViewInitConfig {
		final String title;

		final boolean standardDecorations;
		final boolean infobutton;
		final boolean iconifiable;
		final boolean maximizable;
		final boolean resizable;
		final boolean closable;
		final boolean addWheelZoom;

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

		// Constructor is private; use the static factory method.
		private ViewInitConfig(String title, boolean standardDecorations, boolean infobutton, boolean iconifiable,
				boolean maximizable, boolean resizable, boolean closable, boolean scrollable, boolean visible, int left,
				int top, int width, int height, Rectangle2D.Double worldSystem, Color background,
				JComponent splitWestComponent, long toolBits, ARubberband.Policy boxZoomPolicy, boolean addWheelZoom) {
			this.title = title;
			this.standardDecorations = standardDecorations;
			this.infobutton = infobutton;
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
			this.addWheelZoom = addWheelZoom;
		}

		// Convenience method to check if a world system is defined.
		boolean hasWorldSystem() {
			return worldSystem != null;
		}

		// Static factory method to create a ViewInitConfig from properties.
		static ViewInitConfig from(Properties props) {
			String title = PropertyUtils.getTitle(props);
			if (title == null) {
				title = "A View";
			}

			boolean standardDecorations = PropertyUtils.getStandardViewDecorations(props);
			boolean infobutton = PropertyUtils.getInfoButton(props);
			boolean iconifiable = PropertyUtils.getIconifiable(props);
			boolean maximizable = PropertyUtils.getMaximizable(props);
			boolean resizable = PropertyUtils.getResizable(props);
			boolean closable = PropertyUtils.getClosable(props);

			boolean scrollable = PropertyUtils.getScrollable(props);
			boolean visible = PropertyUtils.getVisible(props);

			int left = PropertyUtils.getLeft(props);
			int top = PropertyUtils.getTop(props);

			int width = Math.max(100, PropertyUtils.getWidth(props));
			int height = Math.max(100, PropertyUtils.getHeight(props));

			// Support "fraction + aspect ratio" sizing (legacy feature).
			double fraction = PropertyUtils.getFraction(props);
			if (Double.isFinite(fraction) && (fraction > 0.0) && (fraction < 1.0)) {
				BaseMDIApplication app = BaseMDIApplication.getApplication();
				if (app != null) {
					Dimension appSize = app.getSize();
					double aspect = PropertyUtils.getAspectRatio(props);
					height = (int) (fraction * appSize.height);
					width = (int) (height * aspect);
				}
			}

			// Cascading/stacking policy if left/top not specified.
			Point p = ViewStackingPolicy.computeInitialLocation(left, top);
			left = p.x;
			top = p.y;

			Rectangle2D.Double worldSystem = PropertyUtils.getWorldSystem(props);

			Color background = PropertyUtils.getBackground(props);

			JComponent west = PropertyUtils.getSplitWestComponent(props);

			long bits = PropertyUtils.getToolbarBits(props);
			ARubberband.Policy policy = PropertyUtils.getBoxZoomRubberbandPolicy(props);

			boolean addWheelZoom = PropertyUtils.addWheelZoom(props);

			return new ViewInitConfig(title, standardDecorations, infobutton, iconifiable, maximizable, resizable,
					closable, scrollable, visible, left, top, width, height, worldSystem, background, west, bits,
					policy, addWheelZoom);
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
			IContainer fromProps = PropertyUtils.getContainer(props);
			if (fromProps != null) {
				return fromProps;
			}

			// If a container class is specified, instantiate it using the expected
			// constructor signature.
			Object containerClassObj = props.get(PropertyUtils.CONTAINERCLASS);
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

			// *** INTEGRATION START: Wrap with Info Button if configured ***
			if (cfg.infobutton && center instanceof JComponent) {
				// We pass the 'view' so the wrapper can call view.getViewInfo() lazily on click
				center = new ViewWrapper((JComponent) center, view);
			}
			// *** INTEGRATION END ***

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
				view.toolBar = new BaseToolBar(container.getComponent(), null, cfg.toolBits,
						ARubberband.Policy.RECTANGLE, cfg.boxZoomPolicy);
				view.getContentPane().add(view.toolBar, BorderLayout.NORTH);
				if (container instanceof BaseContainer baseCont) {
					baseCont.setToolBar(view.toolBar);
				}
			}

			if (cfg.addWheelZoom) {
				installWheelZoom(container);
			}
		}
	}

	/**
	 * Centers the view within its parent frame if possible.
	 */
	public void center() {
		JFrame parent = getParentFrame();
		if (parent != null) {
			Dimension parentSize = parent.getSize();
			Dimension mySize = getSize();
			int x = (parentSize.width - mySize.width) / 2;
			int y = (parentSize.height - mySize.height) / 2;
			setLocation(x, y);
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
	 * Optional method for subclasses to provide view-specific information for
	 * display in the info dialog and activated by the toolbar's info button.
	 * Default implementation returns null (no info).
	 *
	 * @return view information, or null if not applicable.
	 */
	public AbstractViewInfo getViewInfo() {
		return null;
	}

	/**
	 * Captures/restores persistent properties for a view.
	 * <p>
	 * Uses the stable prefix {@link BaseView#getPropertyName()}. Supports old key
	 * variants for backward compatibility.
	 * </p>
	 */
	private static final class ViewPersistence {

		public static void applyToView(BaseView view, Properties props) {
			if (view == null || props == null || props.isEmpty()) {
				return;
			}

			String prefix = view.getPropertyName(); // whatever you use
			String dotted = prefix + "."; // new style
			// old style (no dot)

			// ---- Visible: APPLY ONLY IF PRESENT ----
			Boolean vis = getBooleanIfPresent(props, dotted + "visible", prefix + "visible");
			if (vis != null) {
				view.setVisible(vis);
			}

			// ---- Bounds: apply only if all coords present ----
			Integer x = getIntIfPresent(props, dotted + "x", prefix + "x");
			Integer y = getIntIfPresent(props, dotted + "y", prefix + "y");
			Integer w = getIntIfPresent(props, dotted + "w", prefix + "w");
			Integer h = getIntIfPresent(props, dotted + "h", prefix + "h");

			if (x != null && y != null && w != null && h != null) {
				// Optional: clamp sanity to avoid off-screen / negative sizes
				if (w > 0 && h > 0) {
					view.setBounds(x, y, w, h);
				}
			}

			// ---- Maximized: support misspelling, apply only if present ----
			Boolean max = getBooleanIfPresent(props, dotted + "maximized", dotted + "maxmized", // historical typo
					prefix + "maximized", prefix + "maxmized");
			if (max != null) {
				try {
					view.setMaximum(max);
				} catch (Exception ignore) {
				}
			}

			// ---- On top (if you have it) ----
			// ---- On top (bring to front now; not "always on top") ----
			Boolean ontop = getBooleanIfPresent(props, dotted + "ontop", prefix + "ontop");
			if (Boolean.TRUE.equals(ontop)) {
				try {
					// Bring to front within the desktop
					view.moveToFront();

					// Optional: make it the selected internal frame (usually what users mean)
					view.setSelected(true);
				} catch (Exception ignore) {
					// ignore PropertyVetoException etc.
				}
			}
		}

		// Capture properties from the view's current state, using the stable prefix and
		// writing both new and legacy keys for compatibility.
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

		// Helper methods to read properties with support for multiple key variants
		// (dotted and legacy no-dot).
		private static String firstPresent(Properties p, String... keys) {
			if (p == null) {
				return null;
			}
			for (String k : keys) {
				String v = p.getProperty(k);
				if (v != null) {
					return v;
				}
			}
			return null;
		}

		private static Boolean getBooleanIfPresent(Properties p, String... keys) {
			String v = firstPresent(p, keys);
			return (v == null) ? null : Boolean.parseBoolean(v.trim());
		}

		private static Integer getIntIfPresent(Properties p, String... keys) {
			String v = firstPresent(p, keys);
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
