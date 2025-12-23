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

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import edu.cnu.mdi.desktop.Desktop;
import edu.cnu.mdi.properties.PropertySupport;
import edu.cnu.mdi.swing.WindowPlacement;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.ui.menu.FileMenu;
import edu.cnu.mdi.ui.menu.MenuManager;
import edu.cnu.mdi.ui.menu.OptionMenu;
import edu.cnu.mdi.view.ViewManager;

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
                
        _properties = PropertySupport.fromKeyValues(keyVals);

        // --------------------------------------------------------------------
        // Menu bar and menu manager
        // --------------------------------------------------------------------
        setJMenuBar(new JMenuBar());
        MenuManager menuManager = MenuManager.createMenuManager(getJMenuBar());

        // Exit on close (applications can override via WindowListener if needed)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        // --------------------------------------------------------------------
        // Frame attributes
        // --------------------------------------------------------------------
        Color background = PropertySupport.getBackground(_properties);
        String backgroundImage = PropertySupport.getBackgroundImage(_properties);
        String title = PropertySupport.getTitle(_properties);
        boolean maximize = PropertySupport.getMaximize(_properties);
        double screenFraction = PropertySupport.getFraction(_properties);
        int width = PropertySupport.getWidth(_properties);
        int height = PropertySupport.getHeight(_properties);

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
        menuManager.addMenu(new OptionMenu());
        menuManager.addMenu(ViewManager.getInstance().getViewMenu());

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        instance = this;
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
