package edu.cnu.mdi.desktop;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.view.BaseView;
import edu.cnu.mdi.view.VirtualView;

/**
 * The MDI desktop pane that hosts all {@link BaseView} internal frames.
 *
 * <h2>Background rendering</h2>
 * <p>
 * An optional background color and/or tiled background image can be supplied
 * at creation time via {@link #createDesktop(Color, String)}.
 * </p>
 *
 * <h2>Layout persistence</h2>
 * <p>
 * The desktop can save and restore the position, size, and visibility of every
 * registered {@link BaseView} to/from an XML properties file whose location is
 * determined by {@link Environment#getConfigurationFile()}.
 * </p>
 * <p>
 * Two save methods are provided:
 * </p>
 * <ul>
 *   <li>{@link #writeConfigurationFile()} — writes silently, suitable for
 *       automatic saves on exit or via toolbar actions.</li>
 *   <li>{@link #writeConfigurationFileInteractive()} — prompts the user to
 *       confirm before overwriting an existing file, suitable for an explicit
 *       "Save Layout…" menu action.</li>
 * </ul>
 */
@SuppressWarnings("serial")
public final class Desktop extends JDesktopPane {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /**
     * Sentinel passed to operate-on-views methods to target all views.
     */
    public static final int ALL_VIEWS = 0;

    /**
     * Sentinel passed to operate-on-views methods to target only the
     * top-most view.
     */
    public static final int TOP_VIEW = 1;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /**
     * Properties loaded from the configuration file by
     * {@link #loadConfigurationFile()}, and applied by
     * {@link #configureViews()}.  {@code null} when no configuration has been
     * loaded or the load failed.
     */
    private Properties _properties;

    /** If {@code true} the background image is tiled across the desktop. */
    private boolean tile;

    /** The tiled background image icon, or {@code null} if not configured. */
    private ImageIcon _icon;

    /** Pixel dimensions of a single tile. */
    private Dimension _tileSize;

    /** Singleton instance. */
    private static Desktop instance;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Private constructor.
     *
     * @param background      optional background color; defaults to royal blue
     *                        if {@code null}
     * @param backgroundImage optional resource path of an image to tile across
     *                        the desktop background; ignored if {@code null} or
     *                        the resource cannot be loaded
     */
    private Desktop(Color background, String backgroundImage) {
        setDesktopManager(new GuardedDesktopManager());
        setDragMode(JDesktopPane.LIVE_DRAG_MODE);

        setBackground(background != null
                ? background
                : X11Colors.getX11Color("royal blue"));

        tile = false;
        if (backgroundImage != null) {
            _icon = ImageManager.getInstance().loadImageIcon(backgroundImage);
            if (_icon != null) {
                _tileSize = new Dimension(_icon.getIconWidth(), _icon.getIconHeight());
                tile = (_tileSize.width >= 2) && (_tileSize.height >= 2);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Singleton factory / access
    // -----------------------------------------------------------------------

    /**
     * Create the singleton desktop pane.
     * <p>
     * If the singleton already exists this method returns the existing instance
     * unchanged; the arguments are ignored on subsequent calls.
     * </p>
     *
     * @param background      optional background color
     * @param backgroundImage optional path of a tiled background image resource
     * @return the singleton {@code Desktop}
     */
    public static Desktop createDesktop(Color background, String backgroundImage) {
        if (instance == null) {
            instance = new Desktop(background, backgroundImage);
        }
        return instance;
    }

    /**
     * Returns the singleton {@code Desktop} instance.
     *
     * @return the singleton, or {@code null} if
     *         {@link #createDesktop(Color, String)} has not yet been called
     */
    public static Desktop getInstance() {
        return instance;
    }

    // -----------------------------------------------------------------------
    // Painting
    // -----------------------------------------------------------------------

    /**
     * Paints the desktop background, tiling the background image if one was
     * configured.
     *
     * @param g the graphics context
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (tile) {
            tileBackground(g);
        }
    }

    /**
     * Fill the desktop bounds by repeating the background image in a grid.
     *
     * @param g the graphics context
     */
    private void tileBackground(Graphics g) {
        Rectangle bounds = getBounds();
        int ncol = bounds.width  / _tileSize.width  + 1;
        int nrow = bounds.height / _tileSize.height + 1;
        for (int col = 0; col < ncol; col++) {
            for (int row = 0; row < nrow; row++) {
                g.drawImage(_icon.getImage(),
                        col * _tileSize.width,
                        row * _tileSize.height,
                        this);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Layout persistence — load
    // -----------------------------------------------------------------------

    /**
     * Load the layout configuration file into memory.
     * <p>
     * The file path is determined by {@link Environment#getConfigurationFile()}.
     * If the file does not exist, cannot be read, cannot be parsed, or is
     * empty, this method is a no-op (the internal property set is cleared so
     * that a subsequent {@link #configureViews()} call is also a no-op).
     * </p>
     * <p>
     * Call {@link #configureViews()} after this method to apply the loaded
     * state to the registered views.
     * </p>
     */
    public void loadConfigurationFile() {
        Log log = Log.getInstance();
        
        final File file = Environment.getInstance().getConfigurationFile();

        if (file == null || !file.exists() || !file.isFile() || !file.canRead()) {
            _properties = null;
            if (log != null) {
                log.info("No saved layout file found; using defaults.");
            }
            return;
        }

        final Properties loaded = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            loaded.loadFromXML(fis);
        } catch (Exception e) {
            _properties = null;
            if (log != null) {
                log.warning("Failed to load layout configuration from ["
                        + file.getPath() + "]: " + e.getMessage());
            }
            return;
        }

        if (loaded.isEmpty()) {
            _properties = null;
            if (log != null) {
                log.info("Layout configuration file was empty; ignoring ["
                        + file.getPath() + "]");
            }
            return;
        }

        _properties = loaded;
        if (log != null) {
            log.info("Loaded layout configuration from [" + file.getPath() + "]");
        }
    }

    /**
     * Returns the properties loaded by the most recent call to
     * {@link #loadConfigurationFile()}, or {@code null} if no configuration
     * has been loaded.
     * <p>
     * This is used by {@link edu.cnu.mdi.view.ViewConfiguration} to apply
     * saved state to lazily-created views at the moment they are realized,
     * since those views do not exist as frames when {@link #configureViews()}
     * runs at startup.
     * </p>
     * <p>
     * The returned object is the live internal instance; callers must not
     * modify it.
     * </p>
     *
     * @return the loaded configuration properties, or {@code null}
     */
    public Properties getSavedProperties() {
        return _properties;
    }
    /*
     * Apply the loaded configuration properties to all registered views.
     * <p>
     * Each {@link JInternalFrame} on the desktop that is a {@link BaseView}
     * has its state restored via {@link BaseView#setFromProperties(Properties)}.
     * This is a no-op if {@link #loadConfigurationFile()} has not been called
     * or if the load produced no usable properties.
     * </p>
     * <p>
     * {@link VirtualView} is intentionally excluded: its position is always
     * fixed at (0, 0) upper-left by the framework and must not be overridden
     * by a saved layout.
     * </p>
     */
    public void configureViews() {
        if (_properties == null || _properties.isEmpty()) {
            return;
        }

        JInternalFrame[] frames = getAllFrames();
        if (frames == null) {
            return;
        }

        for (JInternalFrame frame : frames) {
            if (frame instanceof BaseView && !(frame instanceof VirtualView)) {
                ((BaseView) frame).setFromProperties(_properties);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Layout persistence — save (silent)
    // -----------------------------------------------------------------------

    /**
     * Write the current view layout to the configuration file without
     * prompting the user.
     * <p>
     * This is the preferred path for automatic saves (e.g. on application
     * exit or via a toolbar button). It overwrites any existing file silently.
     * For a save that confirms before overwriting, use
     * {@link #writeConfigurationFileInteractive()}.
     * </p>
     * <p>
     * The file path is determined by {@link Environment#getConfigurationFile()}.
     * If the path cannot be determined, or if the write fails, the failure is
     * logged and this method returns without throwing.
     * </p>
     */
    public void writeConfigurationFile() {
        File file = Environment.getInstance().getConfigurationFile();
        if (file == null) {
            Log.getInstance().warning(
                    "Cannot save layout: configuration file path is unknown.");
            return;
        }

        Properties properties = collectViewProperties();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            properties.storeToXML(fos, null);
            Log.getInstance().info(
                    "Layout saved to [" + file.getAbsolutePath() + "]");
        } catch (IOException e) {
            Log.getInstance().warning(
                    "Failed to save layout to ["
                    + file.getPath() + "]: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Layout persistence — save (interactive)
    // -----------------------------------------------------------------------

    /**
     * Write the current view layout to the configuration file, prompting the
     * user to confirm before overwriting an existing file.
     * <p>
     * This method is intended for an explicit "Save Layout…" menu action where
     * user confirmation is appropriate. For automatic/silent saves use
     * {@link #writeConfigurationFile()} instead.
     * </p>
     * <p>
     * On success a confirmation dialog informs the user of the saved path.
     * </p>
     */
    public void writeConfigurationFileInteractive() {
        File file = Environment.getInstance().getConfigurationFile();
        if (file == null) {
            JOptionPane.showMessageDialog(null,
                    "Cannot determine the configuration file location.",
                    "Save Layout", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(null,
                    file.getAbsolutePath() + " already exists.\n"
                    + "Do you want to overwrite it?",
                    "Overwrite Existing Layout?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (answer != JFileChooser.APPROVE_OPTION) {
                return;
            }
        }

        writeConfigurationFile();

        // Show confirmation only for the interactive path.
        if (file.exists()) {
            JOptionPane.showMessageDialog(null,
                    "Your layout was saved to:\n" + file.getAbsolutePath(),
                    "Layout Saved",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // -----------------------------------------------------------------------
    // Layout persistence — delete
    // -----------------------------------------------------------------------

    /**
     * Delete the layout configuration file after confirming with the user.
     * <p>
     * This is a no-op if no configuration file path can be determined or if
     * the file does not exist.
     * </p>
     */
    public void deleteConfigurationFile() {
        File file = Environment.getInstance().getConfigurationFile();
        if (file == null || !file.exists()) {
            return;
        }

        int answer = JOptionPane.showConfirmDialog(null,
                "Confirm delete (this cannot be undone).",
                "Delete Layout?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (answer != JFileChooser.APPROVE_OPTION) {
            return;
        }

        if (file.delete()) {
            JOptionPane.showMessageDialog(null,
                    "Layout configuration file deleted.",
                    "Deleted",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null,
                    "Could not delete: " + file.getAbsolutePath(),
                    "Delete Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // -----------------------------------------------------------------------
    // Exit hook
    // -----------------------------------------------------------------------

    /**
     * Notify all registered {@link BaseView}s that the application is about
     * to exit, giving each view a chance to stop running tasks, flush state,
     * etc.
     * <p>
     * If this method is called from a thread other than the EDT it re-dispatches
     * itself via {@link SwingUtilities#invokeLater(Runnable)}.
     * </p>
     */
    public void prepareForExit() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::prepareForExit);
            return;
        }

        JInternalFrame[] frames = getAllFrames();
        if (frames == null) {
            return;
        }

        for (JInternalFrame frame : frames) {
            if (frame instanceof BaseView) {
                ((BaseView) frame).prepareForExit();
            }
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Collect the current persistent state of every {@link BaseView} on the
     * desktop into a single {@link Properties} object.
     * <p>
     * Each view contributes its own prefix-namespaced keys via
     * {@link BaseView#getConfigurationProperties()}.
     * </p>
     * <p>
     * {@link VirtualView} is excluded: its position is always fixed
     * programmatically and must never be saved or restored from a file.
     * </p>
     * <p>
     * <strong>Column-offset normalisation:</strong> the virtual desktop works
     * by physically shifting all internal-frame pixel X positions when the
     * user navigates between columns. A view nominally in column 1 may
     * therefore have a negative X coordinate on screen when column 2 is
     * active. To make saved layouts column-independent, this method adds the
     * current column's pixel offset back to every saved X value, producing
     * coordinates as they would appear if the desktop were at column 0. On
     * restore the session always starts at column 0, so the values apply
     * directly without further adjustment.
     * </p>
     *
     * @return a merged {@code Properties} containing all view states; never
     *         {@code null} but may be empty if there are no frames
     */
    private Properties collectViewProperties() {
        Properties merged = new Properties();
        JInternalFrame[] frames = getAllFrames();
        if (frames == null) {
            return merged;
        }

        // Current column pixel offset: how far left views have been shifted
        // from their column-0 positions. Adding this back normalises every
        // saved X to column-0 coordinates.
        VirtualView vv = VirtualView.getInstance();
        final int colOffset = (vv != null) ? vv.getCurrentColumnPixelOffset() : 0;

        for (JInternalFrame frame : frames) {
            if (!(frame instanceof BaseView) || frame instanceof VirtualView) {
                continue;
            }
            BaseView bv = (BaseView) frame;
            Properties vprops = bv.getConfigurationProperties();

            if (colOffset != 0) {
                String xKey = bv.getPropertyName() + ".x";
                String xVal = vprops.getProperty(xKey);
                if (xVal != null) {
                    try {
                        int normalisedX = Integer.parseInt(xVal.trim()) + colOffset;
                        vprops.put(xKey, Integer.toString(normalisedX));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            merged.putAll(vprops);
        }
        return merged;
    }
}