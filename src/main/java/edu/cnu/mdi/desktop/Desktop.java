package edu.cnu.mdi.desktop;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;

import edu.cnu.mdi.graphics.drawable.Drawable;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.util.IPing;
import edu.cnu.mdi.util.Ping;


/**
 * Main MDI desktop for mdi applications.
 * <p>
 * This class extends {@link JDesktopPane} and provides:
 * <ul>
 *     <li>Optional tiled background image support.</li>
 *     <li>Simple configuration persistence for {@link BaseView} instances
 *         using {@link java.util.Properties} stored as XML.</li>
 *     <li>Convenience access to the "top" internal frame.</li>
 *     <li>An optional "after draw" hook that can draw on top of the desktop
 *         after the background has been rendered.</li>
 *     <li>A workaround for a Swing bug in which mouse release events are
 *         occasionally missed while dragging internal frames.</li>
 * </ul>
 *
 * <p>
 * The desktop is managed as a singleton; use {@link #createDesktop(Color, String)}
 * to construct and {@link #getInstance()} to retrieve the shared instance.
 * </p>
 */
@SuppressWarnings("serial")
public final class Desktop extends JDesktopPane implements MouseListener, MouseMotionListener {

    /**
     * Constant indicating that an operation should apply to all views
     * currently hosted on this desktop.
     * <p>
     * This constant is not used internally by {@code Desktop} but is provided
     * for clients that need a convenient flag when iterating over views.
     */
    public static final int ALL_VIEWS = 0;

    /**
     * Constant indicating that an operation should apply only to the "top"
     * (front-most) view on this desktop.
     * <p>
     * This constant is not used internally by {@code Desktop} but is provided
     * for clients that need a convenient flag when addressing a single view.
     */
    public static final int TOP_VIEW = 1;

    /**
     * View-configuration properties used to restore window layout.
     * These are typically loaded from and stored to an XML configuration file
     * via {@link #loadConfigurationFile()} and {@link #writeConfigurationFile()}.
     */
    private Properties properties;

    /**
     * Flag indicating whether the desktop background should be tiled with
     * the configured image icon.
     */
    private boolean tile;

    /**
     * The image used to tile the desktop background when {@link #tile} is {@code true}.
     */
    private ImageIcon tileIcon;

    /**
     * The size of a single tile of the background image.
     */
    private Dimension tileSize;

    /**
     * The singleton {@code Desktop} instance.
     */
    private static Desktop instance;

    /**
     * Optional drawable that is invoked after the normal desktop painting
     * has completed. This can be used for overlays or debugging visuals.
     */
    private Drawable afterDraw;

    /**
     * Helper ping/timer used to detect a missed mouse-release event
     * during internal frame dragging.
     */
    private static Ping ping;

    /**
     * The internal frame currently being dragged, if any.
     */
    private JInternalFrame dragFrame;

    /**
     * Timestamp (in milliseconds since the epoch) of the last drag event
     * associated with {@link #dragFrame}.
     */
    private long lastDragTime = Long.MAX_VALUE;

    /**
     * Most recent X coordinate (in the frame's coordinate system) observed
     * during dragging of {@link #dragFrame}.
     */
    private int lastX;

    /**
     * Most recent Y coordinate (in the frame's coordinate system) observed
     * during dragging of {@link #dragFrame}.
     */
    private int lastY;

    /**
     * Constructs a new {@code Desktop} instance.
     * <p>
     * This constructor is private because the desktop is managed as a singleton;
     * use {@link #createDesktop(Color, String)} to create the shared instance.
     * </p>
     *
     * @param background      optional background color. If {@code null}, a default
     *                        X11 "royal blue" is used.
     * @param backgroundImage optional background image resource path. If non-null
     *                        and successfully loaded, the image is tiled across
     *                        the desktop background.
     */
    private Desktop(Color background, String backgroundImage) {

        // Use outline drag mode for better performance when moving internal frames.
        setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        setDoubleBuffered(true);

        if (background != null) {
            setBackground(background);
        } else {
            setBackground(X11Colors.getX11Color("royal blue"));
        }

        // Attempt to enable background tiling.
        tile = false;
        if (backgroundImage != null) {
            tileIcon = ImageManager.getInstance().loadImageIcon(backgroundImage);
            if (tileIcon != null) {
                tileSize = new Dimension(tileIcon.getIconWidth(), tileIcon.getIconHeight());
                tile = (tileSize.width >= 2) && (tileSize.height >= 2);
            }
        }

        // Set up ping-based heartbeat to detect missed mouse-release events
        // while dragging internal frames.
        ping = new Ping(1000); // ping every 1 second
        IPing pingListener = new IPing() {

            @Override
            public void ping() {
                heartbeat();
            }
        };
        ping.addPingListener(pingListener);
    }

    /**
     * Periodic heartbeat method invoked from the {@link #ping} timer.
     * <p>
     * The intent is to work around a Swing bug where the mouse-released event
     * is occasionally not delivered to an internal frame during outline dragging.
     * If a drag appears to have been in progress for too long without a release,
     * this method synthesizes and dispatches a {@link MouseEvent#MOUSE_RELEASED}
     * event to the affected frame.
     * </p>
     */
    private void heartbeat() {
        if (dragFrame == null) {
            return;
        }

        long deltaMillis = System.currentTimeMillis() - lastDragTime;
        if ((deltaMillis > 1000) && (dragFrame != null)) {
            System.err.println("Mouse released missed; synthesizing release event.");

            MouseEvent mouseEvent = new MouseEvent(
                    dragFrame,             // target component
                    MouseEvent.MOUSE_RELEASED,
                    System.currentTimeMillis(),
                    0,                     // no modifiers
                    lastX,
                    lastY,
                    1,                     // click count
                    false,                 // not a popup trigger
                    MouseEvent.BUTTON1     // left button
            );

            dragFrame.dispatchEvent(mouseEvent);
        }
    }

    /**
     * Creates (if necessary) and returns the singleton {@code Desktop} instance.
     * <p>
     * The first call to this method constructs the desktop and configures its
     * background color and optional background image. Subsequent calls return
     * the previously created instance and ignore the arguments.
     * </p>
     *
     * @param background      optional background color. If {@code null}, a default
     *                        X11 "royal blue" is used.
     * @param backgroundImage optional background image resource path. If non-null
     *                        and successfully loaded, the image is tiled across
     *                        the desktop background.
     * @return the singleton {@code Desktop} instance.
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
     * @return the shared desktop, or {@code null} if it has not yet been created
     *         via {@link #createDesktop(Color, String)}.
     */
    public static Desktop getInstance() {
        return instance;
    }

    /**
     * Paints the desktop component.
     * <p>
     * If background tiling is enabled, the configured image is tiled across
     * the entire bounds of the desktop. Otherwise, the default background
     * painting of {@link JDesktopPane} is used. After the background is drawn,
     * any configured "after draw" drawable (see {@link #setAfterDraw(IDrawable)})
     * is invoked.
     * </p>
     *
     * @param g the {@link Graphics} context to use for painting.
     */
    @Override
    public void paintComponent(Graphics g) {

        if (tile) {
            tileBackground(g);
        } else {
            super.paintComponent(g);
        }

        if (afterDraw != null) {
            // Note: the container parameter is unused by Desktop and is supplied as null.
            afterDraw.draw(g, null);
        }
    }

    /**
     * Sets a drawable to be invoked after the desktop background is rendered.
     * <p>
     * This hook can be used for drawing overlays, diagnostics, or global
     * decorations on top of all views.
     * </p>
     *
     * @param afterDraw the drawable invoked at the end of {@link #paintComponent(Graphics)},
     *                  or {@code null} to disable the hook.
     */
    public void setAfterDraw(Drawable afterDraw) {
        this.afterDraw = afterDraw;
    }

    /**
     * Tiles the configured background image across the desktop.
     *
     * @param g the {@link Graphics} context to use for painting.
     */
    private void tileBackground(Graphics g) {
        if (tileIcon == null || tileSize == null) {
            return;
        }

        Rectangle bounds = getBounds();
        int ncol = bounds.width / tileSize.width + 1;
        int nrow = bounds.height / tileSize.height + 1;

        for (int i = 0; i < ncol; i++) {
            int x = i * tileSize.width;
            for (int j = 0; j < nrow; j++) {
                int y = j * tileSize.height;
                g.drawImage(tileIcon.getImage(), x, y, this);
            }
        }
    }

    /**
     * Adds a component to the desktop at the specified index.
     * <p>
     * If the component is a {@link JInternalFrame}, it is made non-opaque and
     * registered for mouse and mouse-motion events so that the drag hack
     * (see {@link #mouseDragged(MouseEvent)}) can function correctly.
     * </p>
     *
     * @param comp  the component to add.
     * @param index the position at which to insert the component, or {@code -1}
     *              to append it.
     * @return the added component.
     */
    @Override
    public Component add(Component comp, int index) {

        if (comp instanceof JInternalFrame) {
            JInternalFrame frame = (JInternalFrame) comp;
            frame.setOpaque(false);
            frame.addMouseListener(this);
            frame.addMouseMotionListener(this);
        }

        return super.add(comp, index);
    }

    /**
     * Returns the "top" internal frame on the desktop.
     * <p>
     * The top frame is defined as the frame with the smallest component Z-order
     * (i.e., the one visually in front of the others).
     * </p>
     *
     * @return the top {@link JInternalFrame}, or {@code null} if the desktop
     *         does not currently contain any internal frames.
     */
    public JInternalFrame getTopFrame() {

        int minIndex = -1;
        int minZorder = Integer.MAX_VALUE;

        JInternalFrame[] frames = getAllFrames();
        if (frames != null) {
            for (int index = 0; index < frames.length; index++) {
                int z = getComponentZOrder(frames[index]);
                if (z < minZorder) {
                    minZorder = z;
                    minIndex = index;
                }
            }
        }

        if (minIndex < 0) {
            return null;
        }
        return frames[minIndex];
    }

    /**
     * Loads a configuration file that preserves the current arrangement of views.
     * <p>
     * The configuration file is obtained from {@link Environment#getConfigurationFile()}.
     * If the file exists and is readable, it is read as an XML properties file
     * and stored in {@link #properties}. If the file does not exist, a message
     * is logged and no exception is thrown.
     * </p>
     */
    public void loadConfigurationFile() {

        File file = Environment.getInstance().getConfigurationFile();

        try {
            if ((file != null) && file.exists() && file.canRead()) {
                try {
                    FileInputStream fis = new FileInputStream(file);
                    properties = new Properties();
                    try {
                        properties.loadFromXML(fis);
                    } catch (InvalidPropertiesFormatException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.getInstance().info("Loaded configuration file from [" + file.getPath() + "]");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                Log log = Log.getInstance();
                if (log != null) {
                    log.info("Did not load a configuration file (none found or not readable).");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures all {@link BaseView} instances on the desktop using the
     * properties previously loaded via {@link #loadConfigurationFile()}.
     * <p>
     * For each internal frame that is an instance of {@link BaseView},
     * {@link BaseView#setFromProperties(Properties)} is invoked.
     * </p>
     */
    public void configureViews() {
        if (properties == null) {
            return;
        }

        JInternalFrame[] frames = getAllFrames();
        if (frames != null) {
            for (JInternalFrame frame : frames) {
                if (frame instanceof BaseView) {
                    BaseView view = (BaseView) frame;
                    view.setFromProperties(properties);
                }
            }
        }
    }

    /**
     * Writes a configuration file that captures the current arrangement of views.
     * <p>
     * The configuration file is obtained from {@link Environment#getConfigurationFile()}.
     * If the file already exists, the user is prompted before overwriting.
     * All {@link BaseView} instances on the desktop contribute their individual
     * configuration properties via {@link BaseView#getConfigurationProperties()},
     * which are merged into a single {@link Properties} object and written
     * as XML.
     * </p>
     */
    public void writeConfigurationFile() {

        File file = Environment.getInstance().getConfigurationFile();

        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(
                    null,
                    file.getAbsolutePath() + " already exists.\nDo you want to overwrite it?",
                    "Overwrite Existing File?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    ImageManager.cnuIcon
            );

            if (answer != JFileChooser.APPROVE_OPTION) {
                return;
            }
        }

        Properties outProps = new Properties();
        JInternalFrame[] frames = getAllFrames();

        if (frames != null) {
            for (JInternalFrame frame : frames) {
                if (frame instanceof BaseView) {
                    BaseView view = (BaseView) frame;
                    Properties vprops = view.getConfigurationProperties();
                    if (vprops != null) {
                        outProps.putAll(vprops);
                    }
                }
            }
        }

        // Write configuration file
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                outProps.storeToXML(fos, null);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes the configuration file used to preserve the arrangement of views.
     * <p>
     * The configuration file is obtained from {@link Environment#getConfigurationFile()}.
     * If it exists, the user is prompted for confirmation before deletion.
     * </p>
     */
    public void deleteConfigurationFile() {

        File file = Environment.getInstance().getConfigurationFile();

        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(
                    null,
                    "Confirm delete operation (this cannot be undone).",
                    "Delete Configuration?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    ImageManager.cnuIcon
            );

            if (answer != JFileChooser.APPROVE_OPTION) {
                return;
            }
            // Best-effort delete; ignore result.
            file.delete();
        }
    }

    // ------------------------------------------------------------------------
    // MouseListener / MouseMotionListener implementation
    // ------------------------------------------------------------------------

    /**
     * Invoked when a mouse button is pressed on a component.
     * <p>
     * This method is used in conjunction with {@link #mouseReleased(MouseEvent)}
     * and {@link #mouseDragged(MouseEvent)} to track potential drag sequences.
     * </p>
     *
     * @param e the mouse event.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        // Nothing to do here beyond establishing that a press has occurred.
        // Drag tracking is handled in mouseDragged and mouseReleased.
    }

    /**
     * Invoked when a mouse button is released on a component.
     * <p>
     * This method clears drag tracking state when an internal frame is released.
     * It also disables the drag-frame heartbeat workaround.
     * </p>
     *
     * @param e the mouse event.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        dragFrame = null;
        lastDragTime = Long.MAX_VALUE;
    }

    /**
     * Invoked when the mouse is dragged on a component.
     * <p>
     * When the drag target is a {@link JInternalFrame}, this method records the
     * frame being dragged and updates the last drag time and coordinates.
     * This information is later used by {@link #heartbeat()} to synthesize a
     * mouse-release event if necessary.
     * </p>
     *
     * @param e the mouse event.
     */
    @Override
    public void mouseDragged(MouseEvent e) {

        Component component = e.getComponent();
        if (component instanceof JInternalFrame) {
            dragFrame = (JInternalFrame) component;
            lastDragTime = System.currentTimeMillis();
            lastX = e.getX();
            lastY = e.getY();
        }
    }

    /**
     * Invoked when the mouse moves on a component (without buttons pressed).
     * <p>
     * The {@code Desktop} does not use this event; the method is present to
     * satisfy the {@link MouseMotionListener} interface.
     * </p>
     *
     * @param e the mouse event (ignored).
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        // No-op
    }

    /**
     * Invoked when the mouse is clicked (pressed and released) on a component.
     * <p>
     * The {@code Desktop} does not use this event; the method is present to
     * satisfy the {@link MouseListener} interface.
     * </p>
     *
     * @param e the mouse event (ignored).
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        // No-op
    }

    /**
     * Invoked when the mouse enters a component.
     * <p>
     * The {@code Desktop} does not use this event; the method is present to
     * satisfy the {@link MouseListener} interface.
     * </p>
     *
     * @param e the mouse event (ignored).
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        // No-op
    }

    /**
     * Invoked when the mouse exits a component.
     * <p>
     * The {@code Desktop} does not use this event; the method is present to
     * satisfy the {@link MouseListener} interface.
     * </p>
     *
     * @param e the mouse event (ignored).
     */
    @Override
    public void mouseExited(MouseEvent e) {
        // No-op
    }
}
