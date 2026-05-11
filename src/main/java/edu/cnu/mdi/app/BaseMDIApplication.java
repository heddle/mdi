package edu.cnu.mdi.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.BorderFactory;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import com.formdev.flatlaf.FlatIntelliJLaf;

import edu.cnu.mdi.desktop.Desktop;
import edu.cnu.mdi.log.ConsoleLogger;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.swing.WindowPlacement;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.ui.menu.FileMenu;
import edu.cnu.mdi.ui.menu.MenuManager;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.BaseView;
import edu.cnu.mdi.view.ViewManager;
import edu.cnu.mdi.view.VirtualView;

/**
 * Base class for all MDI applications.
 *
 * <p>
 * {@code BaseMDIApplication} is the top-level {@link JFrame} that hosts:
 * </p>
 * <ul>
 * <li>The {@link Desktop} (MDI workspace)</li>
 * <li>A standard application {@link JMenuBar}</li>
 * <li>Framework menus (File, View, etc.)</li>
 * </ul>
 *
 * <h2>Singleton Application Model</h2>
 * <p>
 * The MDI framework is designed around a single top-level application frame per
 * JVM. This class enforces that constraint using an atomic set-once singleton
 * reference.
 * </p>
 *
 * <p>
 * <strong>Thread safety:</strong> construction uses an atomic compare-and-set
 * to avoid race conditions in unusual scenarios (tests, tooling, or hosts that
 * may instantiate from multiple threads).
 * </p>
 *
 * <h2>Shutdown Policy</h2>
 * <p>
 * This framework class does <em>not</em> call {@code System.exit(...)}. Closing
 * the main frame triggers {@link #prepareForShutdown()}, then disposes the
 * frame. If the embedding application wants to force JVM termination (for
 * example, after all windows are closed), that policy belongs in the embedding
 * application's {@code main()} or launch layer, not in reusable framework code.
 * </p>
 *
 * <h2>Virtual Desktop Support</h2>
 * <p>
 * The class provides an opt-in "virtual desktop" lifecycle (one-shot "ready"
 * callback and debounced relayout callback) via
 * {@link #prepareForVirtualDesktop()}.
 * </p>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class BaseMDIApplication extends JFrame {

	// Optional reference to the framework-managed virtual view, if enabled. This is
	// not
	// required for virtual desktop support, but it’s a common enough use case that
	// we provide a convenient reference here. It’s up to the application whether to
	// use it or manage its own reference.
	private VirtualView managedVirtualView;

	/** Parsed application properties created from constructor key-value pairs. */
	protected final Properties _properties;

	/**
	 * Singleton instance: the MDI framework is designed around exactly one
	 * top-level application frame per JVM.
	 * <p>
	 * This reference is set exactly once using an atomic compare-and-set to avoid
	 * race conditions if multiple threads attempt to construct an application
	 * instance concurrently.
	 */
	private static final java.util.concurrent.atomic.AtomicReference<BaseMDIApplication> INSTANCE = new java.util.concurrent.atomic.AtomicReference<>();

	/**
	 * Protected constructor.
	 *
	 * <p>
	 * Applications subclass {@code BaseMDIApplication}. The constructor accepts a
	 * variable-length list of property key-value pairs used to configure frame
	 * size, title, background, etc. Properties are parsed via
	 * {@link PropertyUtils#fromKeyValues(Object...)}.
	 * </p>
	 *
	 * <p>
	 * <strong>Singleton enforcement:</strong> If a second instance is constructed,
	 * this constructor throws {@link IllegalStateException}. Framework code must
	 * not terminate the JVM; it should fail fast and let the caller decide what to
	 * do.
	 * </p>
	 *
	 * @param keyVals property key-value pairs
	 * @throws IllegalStateException if a second {@code BaseMDIApplication} is
	 *                               constructed
	 */
	protected BaseMDIApplication(Object... keyVals) {

		// --------------------------------------------------------------------
		// Enforce singleton application model (fail fast, do NOT terminate JVM)
		// --------------------------------------------------------------------
		// Framework code should not call System.exit(...). Instead, we throw to
		// make the problem obvious to the caller while allowing tests/hosts to
		// handle the failure gracefully.
		if (!INSTANCE.compareAndSet(null, this)) {
			throw new IllegalStateException(
					"Singleton violation: only one BaseMDIApplication may be constructed per JVM.");
		}

		try {
			// Initialize FlatLaf LookAndFeel
			UIInit();

			// set the application name
			String applicationId = getApplicationId();
			Environment.setApplicationName(applicationId);

			_properties = PropertyUtils.fromKeyValues(keyVals);

			// --------------------------------------------------------------------
			// Menu bar and menu manager
			// --------------------------------------------------------------------
			setJMenuBar(new JMenuBar());
			MenuManager menuManager = MenuManager.createMenuManager(getJMenuBar());

			// --------------------------------------------------------------------
			// Window close policy
			// --------------------------------------------------------------------
			// Closing the main frame should shut down MDI cleanly (notify views, stop
			// timers, flush state) and then dispose the frame. The framework does not
			// forcibly terminate the JVM via System.exit(...); that policy belongs to
			// the embedding application (e.g., in main()).
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent we) {
					try {
						prepareForShutdown(); // notify views, stop timers, dispose hover window, etc.
					} finally {
						for (Window w : Window.getWindows()) {
							if (w != BaseMDIApplication.this) { // don’t double-dispose the frame currently closing
								w.dispose();
							}
						}

						// Dispose the frame; if this is the last displayable window and
						// no non-daemon threads remain, the JVM will exit naturally.
						dispose();
					}
				}
			});
			
			//console logging of log events for debugging?
			boolean consoleLogger = PropertyUtils.getConsoleLog(_properties);
			if (consoleLogger) {
				Log.getInstance().addLogListener(new ConsoleLogger());
				Log.getInstance().config("Console logging enabled");
			}

			// --------------------------------------------------------------------
			// Frame attributes
			// --------------------------------------------------------------------
			Color background = PropertyUtils.getBackground(_properties);
			String backgroundImage = PropertyUtils.getBackgroundImage(_properties);
			String title = PropertyUtils.getTitle(_properties);
			boolean maximize = PropertyUtils.getMaximize(_properties);
			double screenFraction = PropertyUtils.getFraction(_properties);
			int width = PropertyUtils.getWidth(_properties);
			int height = PropertyUtils.getHeight(_properties);

			setTitle(title != null ? title : "MDI Application");

			// --------------------------------------------------------------------
			// Desktop creation
			// --------------------------------------------------------------------
			Desktop desktop = Desktop.createDesktop(background, backgroundImage);
			add(desktop, BorderLayout.CENTER);

			// --------------------------------------------------------------------
			// Frame sizing strategy (precedence order)
			// --------------------------------------------------------------------
			if (maximize) {
				setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);
			} else if (!Double.isNaN(screenFraction)) {
				screenFraction = Math.max(0.25, Math.min(1.0, screenFraction));
				setSize(WindowPlacement.screenFraction(screenFraction));
			} else {
				width = Math.max(400, Math.min(4000, width));
				height = Math.max(400, Math.min(4000, height));
				setSize(width, height);
			}

			WindowPlacement.centerComponent(this);

			// --------------------------------------------------------------------
			// Standard menus
			// --------------------------------------------------------------------
			menuManager.addMenu(new FileMenu());
			menuManager.addMenu(ViewManager.getInstance().getViewMenu());


		    // Create VirtualView early (before frame is showing) so addNotify()
		    // doesn't fire mid-construction. reconfigure() runs later in onVirtualDesktopReady().
		    int cols = getVirtualDesktopColumns();
		    if (cols > 0) {
		        managedVirtualView = VirtualView.createVirtualView(cols);
		    }

		    prepareForVirtualDesktop();
		    addInitialViews();
		    Log.getInstance().config(applicationId + " initializing");
		    Log.getInstance().info(Environment.getInstance().toString());
		} catch (RuntimeException | Error e) {
			// If construction fails, clear the singleton so a retry is possible (tests,
			// tooling).
			INSTANCE.compareAndSet(this, null);
			throw e;
		}
	}
	
	/**
	 * Create and register the initial set of views shown in the application.
	 *
	 * <p>
	 * The default implementation does not add any views. Subclasses should
	 * override to create and register their initial set of views.
	 * </p>
	 */
	protected void addInitialViews() {
		// no default views
	}

	/**
	 * Performs framework-level shutdown work prior to disposing the application
	 * frame.
	 *
	 * <p>
	 * This method is called when the user attempts to close the main application
	 * window (see the {@link java.awt.event.WindowListener} installed by this
	 * class). The default implementation delegates to the {@link Desktop} so that
	 * open views can be notified and given a chance to release resources and
	 * persist state.
	 * </p>
	 *
	 * <p>
	 * <strong>Subclassing:</strong> Applications may override to add custom
	 * shutdown behavior (stopping background workers, saving preferences, flushing
	 * logs, etc.). When overriding, call {@code super.prepareForShutdown()} as part
	 * of your implementation so the framework can notify views.
	 * </p>
	 *
	 * <p>
	 * <strong>No JVM termination:</strong> This framework method intentionally does
	 * not call {@code System.exit(...)}. If an embedding application wants to force
	 * termination after shutdown, it should do so from application code (typically
	 * in {@code main}).
	 * </p>
	 */
	protected void prepareForShutdown() {
		Desktop.getInstance().prepareForExit();
	}

	/**
	 * Override to opt in to a virtual desktop with the given column count. Return 0
	 * to disable.
	 */
	protected int getVirtualDesktopColumns() {
		return 0;
	}

	/**
	 * Returns a stable application identifier used for persistence (window
	 * placement, saved desktop configuration, etc.).
	 *
	 * <p>
	 * The default implementation returns {@code getClass().getSimpleName()}, which is
	 * the simple name of the class that contains the main program.
	 * Subclasses may override to provide a shorter
	 * or more user-friendly identifier, but it should remain stable over time to
	 * avoid breaking persisted settings.
	 * </p>
	 *
	 * @return a stable, non-null application identifier
	 */
	protected String getApplicationId() {
		return this.getClass().getSimpleName(); // fully-qualified is most stable/unique
	}

	// initialize the FlatLaf UI
	private void UIInit() {
		FlatIntelliJLaf.setup();
		UIManager.put("Component.focusWidth", 1);
		UIManager.put("Component.arc", 6);
		UIManager.put("Button.arc", 6);
		UIManager.put("TabbedPane.showTabSeparators", true);
		Fonts.refresh();
	}

	/**
	 * Returns the singleton {@link BaseMDIApplication} instance for this JVM.
	 *
	 * <p>
	 * The instance is established during construction using an atomic set-once
	 * operation. If this method is called before the application is constructed, it
	 * returns {@code null}.
	 * </p>
	 *
	 * @return the singleton application instance, or {@code null} if not yet
	 *         constructed
	 */
	public static BaseMDIApplication getApplication() {
		return INSTANCE.get();
	}
	
	/**
	 * Returns the parsed application properties created from constructor key-value
	 * pairs.
	 *
	 * @return application properties
	 */
	Properties getProperties() {
		return _properties;
	}

	// ======================================================================
	// Virtual Desktop Support
	// ======================================================================

	// in BaseMDIApplication
	protected void standardVirtualDesktopReady(VirtualView vv, Runnable defaultLayout, boolean applySavedLayout) {
		if (vv != null) {
			vv.reconfigure();
		}
		// Apply saved layout FIRST — persisted positions are the baseline.
		// defaultLayout runs second and should skip views that already have a
		// saved position by guarding each moveTo() with hasSavedLayout(view).
		if (applySavedLayout) {
			Desktop.getInstance().loadConfigurationFile();
			Desktop.getInstance().configureViews();
			// Notify developers (and users) when a saved layout is applied so
			// that "my coded placement is being ignored" confusion is avoided.
			// The toast dismisses itself after 3 seconds — no user action needed.
			if (Desktop.getInstance().getSavedProperties() != null) {
				showConfigToast();
			}
		}
		if (vv != null && defaultLayout != null) {
			defaultLayout.run();
		}
		Log.getInstance().config(vv != null ? "Virtual desktop enabled" : "Virtual desktop disabled");
	}

	/**
	 * Shows a brief self-dismissing toast notification indicating that a saved
	 * layout configuration has been found and applied.
	 *
	 * <p>The toast appears in the bottom-right corner of the screen and
	 * automatically fades out after 3 seconds. It requires no user interaction.
	 * Its primary purpose is to prevent developer confusion when coded placements
	 * in {@link #defaultViewLayout()} appear to be ignored — a symptom of a saved
	 * config silently overriding them.</p>
	 */
	private void showConfigToast() {
		final int DISPLAY_MS   = 3000;
		final int MARGIN       = 20;

		JLabel label = new JLabel("  ℹ  Saved layout configuration applied  ");
		label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
		label.setForeground(new Color(240, 240, 240));
		label.setBackground(new Color(60, 60, 60, 220));
		label.setOpaque(true);
		label.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
				BorderFactory.createEmptyBorder(6, 10, 6, 10)));

		JWindow toast = new JWindow(this);
		toast.setBackground(new Color(0, 0, 0, 0)); // transparent frame
		toast.add(label);
		toast.pack();

		// Position in the bottom-right corner of the screen.
		Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getMaximumWindowBounds();
		int x = screen.x + screen.width  - toast.getWidth()  - MARGIN;
		int y = screen.y + screen.height - toast.getHeight() - MARGIN;
		toast.setLocation(x, y);
		toast.setVisible(true);

		// Auto-dismiss after DISPLAY_MS milliseconds.
		new Timer(DISPLAY_MS, e -> {
			((Timer) e.getSource()).stop();
			toast.setVisible(false);
			toast.dispose();
		}) {{ setRepeats(false); }}.start();
	}

	/**
	 * Returns {@code true} if the desktop has a saved layout entry for the
	 * given view (i.e. it was present in the configuration file).
	 *
	 * <p>Use this in {@link #defaultViewLayout()} to skip views whose positions
	 * have already been restored from saved config:</p>
	 * <pre>
	 *   if (!hasSavedLayout(myView)) vv.moveTo(myView, col, constraint);
	 * </pre>
	 *
	 * @param view the view to check
	 * @return {@code true} if a saved position exists for this view
	 */
	protected boolean hasSavedLayout(BaseView view) {
		if (view == null) return false;
		java.util.Properties saved = Desktop.getInstance().getSavedProperties();
		if (saved == null || saved.isEmpty()) return false;
		return saved.containsKey(view.getPropertyName() + ".x");
	}

	/**
	 * Moves the given view to the specified virtual desktop column and position.
	 * @param view the view to move
	 * @param col the target virtual desktop column
	 * @param delh horizontal delta from constraint position (positive = right, negative = left)
	 * @param delv vertical delta from constraint position (positive = down, negative = up)
	 * @param constraint position constraint (e.g., VirtualView.UPPERLEFT, CENTER)
	 */
	protected void virtualViewMove(BaseView view, int col, int delh, int delv, int constraint) {
		VirtualView vv = VirtualView.getInstance();
		if ((vv == null) || (view == null) || hasSavedLayout(view)) {
			return;	
		}
		vv.moveTo(view, col, delh, delv, constraint);
	}
	
	/**
	 * Overload of {@link #virtualViewMove(BaseView, int, int, int, int)} for zero deltas.
	 */
	protected void virtualViewMove(BaseView view, int col, int constraint) {
		virtualViewMove(view, col, 0, 0, constraint);
	}

	// in BaseMDIApplication
	protected void standardVirtualDesktopRelayout(VirtualView vv) {
		if (vv != null) {
			vv.reconfigure();
		}

	}

	/**
	 * Enable framework-level support for a "virtual desktop" layout strategy.
	 *
	 * <p>
	 * This installs:
	 * </p>
	 * <ul>
	 * <li>A one-shot callback when the frame is first shown</li>
	 * <li>A debounced callback for subsequent resizes/moves</li>
	 * </ul>
	 *
	 * <p>
	 * Subclasses opt in by calling this method (typically in their constructor) and
	 * overriding {@link #onVirtualDesktopReady()} and/or
	 * {@link #onVirtualDesktopRelayout()}.
	 * </p>
	 *
	 * @param debounceMs debounce delay (milliseconds) for resize/move events
	 */
	private void prepareForVirtualDesktop(int debounceMs) {

		final int delay = Math.max(50, debounceMs);

		// ------------------------------------------------------------
		// One-shot "frame is showing" callback
		// ------------------------------------------------------------
		HierarchyListener showingListener = new HierarchyListener() {

			private boolean fired = false;

			@Override
			public void hierarchyChanged(HierarchyEvent e) {
				if (fired) {
					return;
				}
				if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
					fired = true;
					SwingUtilities.invokeLater(() -> {
						try {
							onVirtualDesktopReady();
						} finally {
							removeHierarchyListener(this);
						}
					});
				}
			}
		};
		addHierarchyListener(showingListener);

		// ------------------------------------------------------------
		// Debounced relayout on resize/move
		// ------------------------------------------------------------
		Timer debounce = new Timer(delay, ae -> {
			((Timer) ae.getSource()).stop();
			onVirtualDesktopRelayout();
		});
		debounce.setRepeats(false);

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				debounce.restart();
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				debounce.restart();
			}
		});
	}

	/**
	 * Enable virtual desktop support using a sensible default debounce delay.
	 */
	protected void prepareForVirtualDesktop() {
		prepareForVirtualDesktop(120);
	}

	/**
	 * Called once, after the application frame is first shown and Swing layout has
	 * stabilized.
	 *
	 * <p>
	 * Typical uses:
	 * </p>
	 * <ul>
	 * <li>Reconfigure a {@code VirtualView}</li>
	 * <li>Place default view locations</li>
	 * <li>Load and apply persisted desktop configuration</li>
	 * </ul>
	 *
	 * <p>
	 * Default implementation does nothing.
	 * </p>
	 */
	protected void onVirtualDesktopReady() {
	    // Frame is now showing — safe to reconfigure geometry and apply layout.
	    if (managedVirtualView != null) {
	        managedVirtualView.toFront();
	    }
		standardVirtualDesktopReady(managedVirtualView, this::defaultViewLayout, true);
	}

	/**
	 * Called after the application frame is resized or moved (debounced).
	 *
	 * <p>
	 * Intended for lightweight geometry updates such as:
	 * </p>
	 * 
	 * <pre>
	 * virtualView.reconfigure();
	 * </pre>
	 *
	 * <p>
	 * Default implementation does nothing.
	 * </p>
	 */
	protected void onVirtualDesktopRelayout() {
		standardVirtualDesktopRelayout(managedVirtualView);
	}

	/** Override to define the default view placement. */
	protected void defaultViewLayout() {
	}

	/**
	 * Attempt to determine the framework version from the manifest or
	 * pom.properties.
	 *
	 * <p>
	 * This is used in the "About" dialog and elsewhere. If the version cannot be
	 * determined, returns "(development build)".
	 * </p>
	 *
	 * @return framework version string
	 */
	public static String getFrameworkVersion() {
		// Try manifest first
		Package pkg = BaseMDIApplication.class.getPackage();
		String v = (pkg != null) ? pkg.getImplementationVersion() : null;
		if (v != null && !v.isBlank())
			return v;

		// Fallback: pom.properties (more reliable)
		String path = "/META-INF/maven/io.github.heddle/mdi/pom.properties";
		try (InputStream in = BaseMDIApplication.class.getResourceAsStream(path)) {
			if (in != null) {
				Properties p = new Properties();
				p.load(in);
				String pv = p.getProperty("version");
				if (pv != null && !pv.isBlank())
					return pv;
			}
		} catch (Exception ignored) {
		}

		return "(development build)";
	}

	/**
	 * Launch the application on the EDT using the provided factory.
	 *
	 * <p>
	 * This helper ensures Swing construction and visibility occur on the Event
	 * Dispatch Thread (EDT).
	 * </p>
	 *
	 * @param factory supplier that creates the application instance
	 */
	public static void launch(Supplier<? extends BaseMDIApplication> factory) {
		EventQueue.invokeLater(() -> {
			BaseMDIApplication app = factory.get();
			app.setVisible(true);
		});
	}
}