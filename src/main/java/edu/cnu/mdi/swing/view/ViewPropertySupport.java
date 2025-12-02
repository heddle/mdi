package edu.cnu.mdi.swing.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import java.util.Properties;

import javax.swing.JComponent;

import edu.cnu.mdi.util.PropertySupport;

/**
 * Utility class providing Swing-oriented helpers for reading view properties
 * from a {@link Properties} object.
 *
 * <p>This class acts as a UI-layer on top of the generic
 * {@link edu.cnu.mdi.util.PropertySupport} utility. It interprets properties
 * as Swing-specific concepts: colors, fonts, world-coordinate rectangles,
 * visibility flags, margins, tools, layout constraints, and user-data hooks.</p>
 *
 * <p>No state is maintained; all methods are {@code static}. The class is
 * {@code final} and has a private constructor to enforce its utility role.</p>
 *
 * <h3>Property Formats</h3>
 * <p>Certain property values may be supplied in multiple formats:</p>
 * <ul>
 *   <li>{@link Color} instances stored directly in the properties map</li>
 *   <li>Hex strings: <code>#RRGGBB</code> or <code>#RRGGBBAA</code></li>
 *   <li>Decimal triplets: <code>r,g,b</code> where each 0–255</li>
 *   <li>{@link Rectangle2D.Double} stored directly for world-coordinate systems</li>
 *   <li>Booleans: “true”, “false” (case-insensitive)</li>
 *   <li>Integers and doubles via {@link PropertySupport}</li>
 * </ul>
 *
 * <p>If a property is missing or invalid, defined defaults are used.</p>
 */
public final class ViewPropertySupport {

    // -------------------------------------------------------------------------
    // Default Values (used when properties are missing)
    // -------------------------------------------------------------------------

    /** Default semi-transparent fill color for views. */
    public static final Color DEFAULT_FILL_COLOR  = new Color(208, 208, 208, 128);

    /** Default outline/line color for view items. */
    public static final Color DEFAULT_LINE_COLOR  = Color.BLACK;

    /** Default text color for annotation overlay. */
    public static final Color DEFAULT_TEXT_COLOR  = Color.BLACK;

    /** Default font for view labels, overlays, and decorations. */
    public static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);

    /**
     * Default world-coordinate system when one is unspecified.
     * This provides a basic unit square from (0,0) to (1,1).
     */
    public static final Rectangle2D.Double DEFAULT_WORLD_RECT =
            new Rectangle2D.Double(0.0, 0.0, 1.0, 1.0);

    /** Placeholder string returned when a title or name property is missing. */
    public static final String UNKNOWN_STRING = "???";

    /** Private constructor to prevent instantiation. */
    private ViewPropertySupport() { }

    // -------------------------------------------------------------------------
    // String & Boolean Properties
    // -------------------------------------------------------------------------

    /**
     * Gets the view title. Falls back to {@link #UNKNOWN_STRING}.
     *
     * @param props property container
     * @return the title string or a placeholder if missing
     */
    public static String getTitle(Properties props) {
        return PropertySupport.getString(props, ViewPropertyKeys.TITLE, UNKNOWN_STRING);
    }

    /**
     * Gets the internal property name (distinct from title).
     *
     * @param props property container
     * @return name or placeholder if missing
     */
    public static String getPropName(Properties props) {
        return PropertySupport.getString(props, ViewPropertyKeys.PROP_NAME, UNKNOWN_STRING);
    }

    /** @return whether the view is visible by default */
    public static boolean isVisible(Properties props) {
        return PropertySupport.getBoolean(props, ViewPropertyKeys.VISIBLE, true);
    }

    /** @return whether the view supports scrolling (scrollpane embedded). */
    public static boolean isScrollable(Properties props) {
        return PropertySupport.getBoolean(props, ViewPropertyKeys.SCROLLABLE, false);
    }

    /** @return whether a toolbar should be created/used. */
    public static boolean useToolbar(Properties props) {
        return PropertySupport.getBoolean(props, ViewPropertyKeys.TOOLBAR, true);
    }

    /**
     * Returns any multi-bit toolbar feature mask.
     *
     * @param props property container
     * @return integer bitmask (default 0)
     */
    public static int getToolbarBits(Properties props) {
        return PropertySupport.getInt(props, ViewPropertyKeys.TOOLBAR_BITS, 0);
    }

    /** @return true to use standard decorations when rendering the view */
    public static boolean useStandardViewDecorations(Properties props) {
        return PropertySupport.getBoolean(props, ViewPropertyKeys.STANDARD_VIEW_DECOR, true);
    }

    /** @return true if the view may be closed by the user */
    public static boolean isClosable(Properties props) {
        return PropertySupport.getBoolean(props, ViewPropertyKeys.CLOSABLE, true);
    }

    /** @return true if the view supports iconification/minimization */
    public static boolean isIconifiable(Properties props) {
        return PropertySupport.getBoolean(props, ViewPropertyKeys.ICONIFIABLE, true);
    }

    /** @return true if resizing UI handles should be shown and active */
    public static boolean isResizable(Properties props) {
        return PropertySupport.getBoolean(props, ViewPropertyKeys.RESIZABLE, true);
    }

    /** @return true if the view can be dragged (e.g., in an MDI desktop) */
    public static boolean isDraggable(Properties props) {
        return PropertySupport.getBoolean(props, ViewPropertyKeys.DRAGGABLE, false);
    }

    /** @return true if the view should start maximized */
    public static boolean isMaximize(Properties props) {
        return PropertySupport.getBoolean(props, ViewPropertyKeys.MAXIMIZE, false);
    }

    /** @return true if the view supports runtime “maximize” actions */
    public static boolean isMaximizable(Properties props) {
        return PropertySupport.getBoolean(props, ViewPropertyKeys.MAXIMIZABLE, true);
    }

    /** @return true if rotation gestures are supported (2D/3D future views) */
    public static boolean isRotatable(Properties props) {
        return PropertySupport.getBoolean(props, ViewPropertyKeys.ROTATABLE, false);
    }

    /** @return true if the view is locked (non-interactive layout) */
    public static boolean isLocked(Properties props) {
        return PropertySupport.getBoolean(props, ViewPropertyKeys.LOCKED, true);
    }

    // -------------------------------------------------------------------------
    // Integer layout properties
    // -------------------------------------------------------------------------

    /** @return left coordinate or default 0 */
    public static int getLeft(Properties props) {
        return PropertySupport.getInt(props, ViewPropertyKeys.LEFT, 0);
    }

    /** @return top coordinate or default 0 */
    public static int getTop(Properties props) {
        return PropertySupport.getInt(props, ViewPropertyKeys.TOP, 0);
    }

    /** @return width or {@link Integer#MIN_VALUE} if unspecified */
    public static int getWidth(Properties props) {
        return PropertySupport.getInt(props, ViewPropertyKeys.WIDTH, Integer.MIN_VALUE);
    }

    /** @return height or {@link Integer#MIN_VALUE} if unspecified */
    public static int getHeight(Properties props) {
        return PropertySupport.getInt(props, ViewPropertyKeys.HEIGHT, Integer.MIN_VALUE);
    }

    /** Margin accessor methods. */
    public static int getTopMargin(Properties props) {
        return PropertySupport.getInt(props, ViewPropertyKeys.TOP_MARGIN, 0);
    }
    public static int getBottomMargin(Properties props) {
        return PropertySupport.getInt(props, ViewPropertyKeys.BOTTOM_MARGIN, 0);
    }
    public static int getLeftMargin(Properties props) {
        return PropertySupport.getInt(props, ViewPropertyKeys.LEFT_MARGIN, 0);
    }
    public static int getRightMargin(Properties props) {
        return PropertySupport.getInt(props, ViewPropertyKeys.RIGHT_MARGIN, 0);
    }

    /**
     * Reads a fractional value (e.g. 0.8 of screen size).
     *
     * @return the fraction or {@code Double.NaN} if missing
     */
    public static double getFraction(Properties props) {
        return PropertySupport.getDouble(props, ViewPropertyKeys.FRACTION, Double.NaN);
    }

    // -------------------------------------------------------------------------
    // World geometry
    // -------------------------------------------------------------------------

    /**
     * Returns the world coordinate rectangle describing the logical data space
     * for the view. If absent, a default unit square is returned.
     *
     * @param props property container
     * @return world system rectangle (never null)
     */
    public static Rectangle2D.Double getWorldSystem(Properties props) {
        return getWorldRectangle(props, ViewPropertyKeys.WORLDSYSTEM, DEFAULT_WORLD_RECT);
    }

    /**
     * Retrieves a {@link Rectangle2D.Double} stored directly in the properties map.
     *
     * @param props        property container
     * @param key          lookup key
     * @param defaultValue value to return if property missing or not a rectangle
     * @return rectangle stored for the key or default if invalid
     */
    public static Rectangle2D.Double getWorldRectangle(Properties props,
                                                       String key,
                                                       Rectangle2D.Double defaultValue) {
        Objects.requireNonNull(props, "props cannot be null");
        Objects.requireNonNull(defaultValue, "defaultValue cannot be null");

        Object val = props.get(key);
        if (val instanceof Rectangle2D.Double) {
            return (Rectangle2D.Double) val;
        }
        return defaultValue;
    }

    // -------------------------------------------------------------------------
    // Colors & Fonts
    // -------------------------------------------------------------------------

    /** @return background color or {@code null} if unspecified */
    public static Color getBackgroundColor(Properties props) {
        return getColor(props, ViewPropertyKeys.BACKGROUND, null);
    }

    /**
     * @return path (string) to a background image or {@code null}
     */
    public static String getBackgroundImagePath(Properties props) {
        return PropertySupport.getString(props, ViewPropertyKeys.BACKGROUND_IMAGE, null);
    }

    /** @return text color or default black */
    public static Color getTextColor(Properties props) {
        return getColor(props, ViewPropertyKeys.TEXT_COLOR, DEFAULT_TEXT_COLOR);
    }

    /** @return interior fill color or default semi-transparent gray */
    public static Color getFillColor(Properties props) {
        return getColor(props, ViewPropertyKeys.FILL_COLOR, DEFAULT_FILL_COLOR);
    }

    /** @return outline color or default black */
    public static Color getLineColor(Properties props) {
        return getColor(props, ViewPropertyKeys.LINE_COLOR, DEFAULT_LINE_COLOR);
    }

    /**
     * Generic color reader supporting:
     * <ul>
     *   <li>{@link Color} objects directly stored in the properties</li>
     *   <li>Hex colors (#RRGGBB or #RRGGBBAA)</li>
     *   <li>Decimal triplets (e.g., "125,200,40")</li>
     * </ul>
     *
     * @param props        property container
     * @param key          key to look up
     * @param defaultValue fallback color
     * @return parsed color or default
     */
    public static Color getColor(Properties props, String key, Color defaultValue) {
        Objects.requireNonNull(props, "props cannot be null");

        Object val = props.get(key);
        if (val == null) return defaultValue;

        if (val instanceof Color) return (Color) val;

        if (val instanceof String) {
            Color parsed = parseColorString((String) val);
            return (parsed != null) ? parsed : defaultValue;
        }

        return defaultValue;
    }

    /**
     * Parses a color string in hex or decimal triplet form.
     *
     * @param s string to parse
     * @return parsed color, or null if format invalid
     */
    private static Color parseColorString(String s) {
        String str = s.trim();
        if (str.isEmpty()) return null;

        // Hex (#RRGGBB or #RRGGBBAA)
        if (str.startsWith("#") && (str.length() == 7 || str.length() == 9)) {
            try {
                int rgb = (int) Long.parseLong(str.substring(1), 16);
                if (str.length() == 7) {
                    return new Color(rgb);
                } else {
                    // #RRGGBBAA -> ARGB shift
                    return new Color((rgb >> 8) & 0xFFFFFF, true);
                }
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        // Decimal triplet: "r,g,b"
        String[] parts = str.split(",");
        if (parts.length == 3) {
            try {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return new Color(r, g, b);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return null;
    }

    /**
     * Retrieves a {@link Font} from properties.
     *
     * @param props property container
     * @return font if stored, or {@link #DEFAULT_FONT}
     */
    public static Font getFont(Properties props) {
        Objects.requireNonNull(props, "props cannot be null");

        Object val = props.get(ViewPropertyKeys.FONT);
        if (val instanceof Font) {
            return (Font) val;
        }
        return DEFAULT_FONT;
    }

    // -------------------------------------------------------------------------
    // JComponent storage (e.g., split-pane west component)
    // -------------------------------------------------------------------------

    /**
     * Returns a component to be placed on the west side of a split layout.
     *
     * @param props property container
     * @return component or null
     */
    public static JComponent getSplitWestComponent(Properties props) {
        return getJComponent(props, ViewPropertyKeys.SPLIT_WEST_COMPONENT);
    }

    /**
     * Retrieves a stored JComponent under the given key.
     *
     * @param props property container
     * @param key   key to look up
     * @return component if stored, else null
     */
    public static JComponent getJComponent(Properties props, String key) {
        Objects.requireNonNull(props, "props cannot be null");
        Object val = props.get(key);
        return (val instanceof JComponent) ? (JComponent) val : null;
    }

    // -------------------------------------------------------------------------
    // Arbitrary user data
    // -------------------------------------------------------------------------

    /**
     * Retrieves any arbitrary user-data object stored under
     * {@link ViewPropertyKeys#USER_DATA}.
     *
     * @param props property container
     * @return object or null
     */
    public static Object getUserData(Properties props) {
        return props.get(ViewPropertyKeys.USER_DATA);
    }
}
