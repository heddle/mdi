package edu.cnu.mdi.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatIntelliJLaf;

import edu.cnu.mdi.desktop.Desktop;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.properties.PropertyUtils;
import edu.cnu.mdi.swing.WindowPlacement;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.ui.menu.FileMenu;
import edu.cnu.mdi.ui.menu.MenuManager;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.view.ViewManager;
import edu.cnu.mdi.view.VirtualView;

/**
 * Base class for all MDI applications.
 * <p>
 * {@code BaseMDIApplication} provides the top-level {@link JFrame} that hosts:
 * <ul>
 *   <li>The {@link Desktop} (MDI workspace)</li>
 *   <li>Standard application menus</li>
 *   <li>View and plugin menus</li>
 * </ul>
 *
 * <h2>Design goals</h2>
 * <ul>
 *   <li>Hide Swing lifecycle complexity from application code</li>
 *   <li>Provide a consistent, single-instance application frame</li>
 *   <li>Offer optional, framework-level support for a "virtual desktop"</li>
 * </ul>
 *
 * <p>
 * Subclasses typically:
 * <ul>
 *   <li>Call {@link #prepareForVirtualDesktop()} if virtual desktop support is desired</li>
 *   <li>Create views in their constructor or factory method</li>
 *   <li>Override virtual-desktop hooks if needed</li>
 * </ul>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class BaseMDIApplication extends JFrame {

    /** Parsed application properties created from constructor key-value pairs. */
    protected final Properties _properties;

    /** Singleton instance: MDI framework supports exactly one application frame. */
    private static volatile BaseMDIApplication instance;

    /**
     * Protected constructor.
     * <p>
     * Applications must subclass {@code BaseMDIApplication}. The constructor
     * accepts a variable-length list of property key-value pairs used to
     * configure frame size, title, background, etc.
     *
     * @param keyVals property key-value pairs
     */
    protected BaseMDIApplication(Object... keyVals) {

        // Enforce singleton application model
        if (instance != null) {
            System.err.println("Singleton violation in BaseMDIApplication");
            System.exit(1);
        }

        // Initialize FlatLaf LookAndFeel
        UIInit();

        //set the application name
        String applicationId = getApplicationId();
        Environment.setApplicationName(applicationId);

        _properties = PropertyUtils.fromKeyValues(keyVals);

        // --------------------------------------------------------------------
        // Menu bar and menu manager
        // --------------------------------------------------------------------
        setJMenuBar(new JMenuBar());
        MenuManager menuManager = MenuManager.createMenuManager(getJMenuBar());

        // Exit on close (applications can override via WindowListener if needed)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
            	prepareForShutdown();
                System.exit(0);
            }
        });

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
        }
        else if (!Double.isNaN(screenFraction)) {
            screenFraction = Math.max(0.25, Math.min(1.0, screenFraction));
            setSize(WindowPlacement.screenFraction(screenFraction));
        }
        else {
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

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        instance = this;
    }

    /** Prepare for application shutdown. Subclasses may override to add
	 * custom shutdown behavior. They should call super.prepareForShutdown()
	 * to ensure the desktop gets a chance to notify views.
	 */
    protected void prepareForShutdown() {
 		Desktop.getInstance().prepareForExit();
	}

    /** Return a stable application ID for use in persistence, etc. By default
     * this is the fully-qualified class name of the class containing
     * main(), which is presumably a subclass.The subclass may override this to
	 * provide a different (simpler) ID if desired.
     */
    protected String getApplicationId() {
        return getClass().getName(); // fully-qualified is most stable/unique
    }

    //initialize the FlatLaf UI
	private void UIInit() {
		FlatIntelliJLaf.setup();
		UIManager.put("Component.focusWidth", 1);
		UIManager.put("Component.arc", 6);
		UIManager.put("Button.arc", 6);
		UIManager.put("TabbedPane.showTabSeparators", true);
		Fonts.refresh();
	}

    /**
     * @return the singleton application frame
     */
    public static BaseMDIApplication getApplication() {
        return instance;
    }

    // ======================================================================
    // Virtual Desktop Support
    // ======================================================================

	// in BaseMDIApplication
	protected void standardVirtualDesktopReady(VirtualView vv, Runnable defaultLayout, boolean applySavedLayout) {
		if (vv != null) {
			vv.reconfigure();
			if (defaultLayout != null) {
				defaultLayout.run();
			}
		}
		if (applySavedLayout) {
			Desktop.getInstance().loadConfigurationFile();
			Desktop.getInstance().configureViews();
		}
		Log.getInstance().info(vv != null ? "Virtual desktop enabled" : "Virtual desktop disabled");
	}

	protected void standardVirtualDesktopRelayout(VirtualView vv) {
		if (vv != null) {
            vv.reconfigure();
        }

    }

    /**
     * Enable framework-level support for a "virtual desktop" layout strategy.
     * <p>
     * This installs:
     * <ul>
     *   <li>A one-shot callback when the frame is first shown</li>
     *   <li>A debounced callback for subsequent resizes/moves</li>
     * </ul>
     *
     * <p>
     * Subclasses opt in by calling this method (typically in their constructor)
     * and overriding {@link #onVirtualDesktopReady()} and/or
     * {@link #onVirtualDesktopRelayout()}.
     *
     * @param debounceMs debounce delay (milliseconds) for resize/move events
     */
    protected void prepareForVirtualDesktop(int debounceMs) {

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
     * Called once, after the application frame is first shown and Swing layout
     * has stabilized.
     * <p>
     * Typical uses:
     * <ul>
     *   <li>Reconfigure a {@code VirtualView}</li>
     *   <li>Place default view locations</li>
     *   <li>Load and apply persisted desktop configuration</li>
     * </ul>
     * Default implementation does nothing.
     */
    protected void onVirtualDesktopReady() {
        // override in subclass if needed
    }

    /**
     * Called after the application frame is resized or moved (debounced).
     * <p>
     * Intended for lightweight geometry updates such as:
     * <pre>
     * virtualView.reconfigure();
     * </pre>
     * Default implementation does nothing.
     */
    protected void onVirtualDesktopRelayout() {
        // override in subclass if needed
    }
}
