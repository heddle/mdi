package edu.cnu.mdi.properties;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.swing.JComponent;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.rubberband.ARubberband;
import edu.cnu.mdi.graphics.style.LineStyle;
import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.util.Environment;

public class PropertyUtils {

	public static final String ASPECT = "ASPECT";
	public static final String BACKGROUND = "BACKGROUND";
	public static final String BACKGROUNDIMAGE = "BACKGROUNDIMAGE";
	public static final String BOXZOOMRBPOLICY = "BOXZOOMRBPOLICY";
	public static final String CLOSABLE = "CLOSABLE";
	public static final String CONNECTABLE = "CONNECTABLE";
	public static final String CONTAINER = "CONTAINER";
	public static final String CONTAINERCLASS = "CONTAINERCLASS";
	public static final String DELETABLE = "DELETABLE";
	public static final String DOUBLECLICKABLE = "DOUBLECLICKABLE";
	public static final String DRAGGABLE = "DRAGGABLE";
	public static final String FILLCOLOR = "FILLCOLOR";
	public static final String FRACTION = "FRACTION";
	public static final String HEIGHT = "HEIGHT";
	public static final String ICONIFIABLE = "ICONIFIABLE";
	public static final String INFOBUTTON = "INFOBUTTON";
	public static final String LEFT = "LEFT";
	public static final String LINECOLOR = "LINECOLOR";
	public static final String LINESTYLE = "LINESTYLE";
	public static final String LINEWIDTH = "LINEWIDTH";
	public static final String MAXIMIZE = "MAXIMIZE";
	public static final String MAXIMIZABLE = "MAXIMIZABLE";
	public static final String LOCKED = "LOCKED";
	public static final String RADIUS = "RADIUS";
	public static final String RESIZABLE = "RESIZABLE";
	public static final String RIGHTCLICKABLE = "RIGHTCLICKABLE";
	public static final String ROTATABLE = "ROTATABLE";
	public static final String SCROLLABLE = "SCROLLABLE";
	public static final String SPLITWESTCOMPONENT = "SPLITWESTCOMPONENT";
	public static final String STANDARDVIEWDECORATIONS = "STANDARDVIEWDECORATIONS";
	public static final String SYMBOL = "SYMBOL";
	public static final String SYMBOLSIZE = "SYMBOLSIZE";
	public static final String TEXTCOLOR = "TEXTCOLOR";
	public static final String TITLE = "TITLE";
	public static final String TOOLBARBITS = "TOOLBARBITS";
	public static final String TOP = "TOP";
	public static final String USERDATA = "USERDATA";
	public static final String VISIBLE = "VISIBLE";
	public static final String WHEELZOOM = "WHEELZOOM";
	public static final String WIDTH = "WIDTH";
	public static final String WORLDSYSTEM = "WORLDSYSTEM";

	// properties for 3D views
	public static final String ANGLE_X = "ANGLEX";
	public static final String ANGLE_Y = "ANGLEY";
	public static final String ANGLE_Z = "ANGLEZ";
	public static final String DIST_X = "DISTX";
	public static final String DIST_Y = "DISTY";
	public static final String DIST_Z = "DISTZ";
	
	// a map of known keys and their expected value types. 
	//This is used for error checking and documentation purposes.
	private static final Map<String, Class<?>> KNOWN_KEYS = Map.ofEntries(
			Map.entry(ANGLE_X, Float.class),
			Map.entry(ANGLE_Y, Float.class),
			Map.entry(ANGLE_Z, Float.class),
			Map.entry(ASPECT, Double.class), 
			Map.entry(BACKGROUND, Color.class),
			Map.entry(BACKGROUNDIMAGE, String.class),
		    Map.entry(BOXZOOMRBPOLICY, ARubberband.Policy.class),
		    Map.entry(CLOSABLE, Boolean.class),
		    Map.entry(CONNECTABLE, Boolean.class),
		    Map.entry(CONTAINER, IContainer.class),
		    Map.entry(CONTAINERCLASS, Class.class),
		    Map.entry(DELETABLE, Boolean.class),
		    Map.entry(DIST_X, Float.class),
		    Map.entry(DIST_Y, Float.class),
		    Map.entry(DIST_Z, Float.class),
		    Map.entry(DOUBLECLICKABLE, Boolean.class),
		    Map.entry(DRAGGABLE, Boolean.class),
		    Map.entry(FILLCOLOR, Color.class),
		    Map.entry(FRACTION, Double.class),
		    Map.entry(HEIGHT, Integer.class),
		    Map.entry(ICONIFIABLE, Boolean.class),
		    Map.entry(INFOBUTTON, Boolean.class),
		    Map.entry(LEFT, Integer.class),
		    Map.entry(LINECOLOR, Color.class),
		    Map.entry(LINESTYLE, LineStyle.class),
		    Map.entry(LINEWIDTH, Float.class),
		    Map.entry(LOCKED, Boolean.class),
		    Map.entry(MAXIMIZE, Boolean.class),
		    Map.entry(MAXIMIZABLE, Boolean.class),
		    Map.entry(RADIUS, Double.class),
		    Map.entry(RESIZABLE, Boolean.class),
		    Map.entry(RIGHTCLICKABLE, Boolean.class),
		    Map.entry(ROTATABLE, Boolean.class),
		    Map.entry(SCROLLABLE, Boolean.class),
		    Map.entry(SPLITWESTCOMPONENT, JComponent.class),
		    Map.entry(STANDARDVIEWDECORATIONS, Boolean.class),
		    Map.entry(SYMBOL, SymbolType.class),
		    Map.entry(SYMBOLSIZE, Integer.class),
		    Map.entry(TEXTCOLOR, Color.class),
		    Map.entry(TITLE, String.class),
		    Map.entry(TOOLBARBITS, Long.class),
		    Map.entry(TOP, Integer.class),
		    Map.entry(USERDATA, Object.class),
		    Map.entry(VISIBLE, Boolean.class),
		    Map.entry(WHEELZOOM, Boolean.class),
		    Map.entry(WIDTH, Integer.class),
		    Map.entry(WORLDSYSTEM, Rectangle2D.Double.class)
		);

	// default fill color a gray
	public static Color defaultFillColor = new Color(208, 208, 208, 128);

	// default line color black
	public static Color defaultLineColor = Color.black;

	// default text color black
	public static Color defaultTextColor = Color.black;

	// default data directory
	public static String defaultDataDir = Environment.getInstance().getCurrentWorkingDirectory() + File.separator
			+ "data";

	// a default "unknown" string
	public static final String unknownString = "???";

	// a default world rectangle
	public static final Rectangle2D.Double defaultWorldRect = new Rectangle2D.Double(0, 0, 1, 1);

	/**
	 * Create a set of properties from the key values
	 *
	 * @param keyValues the set of key values
	 * @return a set of properties
	 */
	public static Properties fromKeyValues(Object... keyValues) {

	    Properties props = new Properties();

	    if (keyValues == null || keyValues.length == 0) {
	        return props;
	    }

	    if ((keyValues.length % 2) != 0) {
	        throw new IllegalArgumentException(
	            "Key/value arguments must come in pairs.");
	    }

	    for (int i = 0; i < keyValues.length; i += 2) {

	        Object keyObj = keyValues[i];
	        Object value = keyValues[i + 1];

	        if (!(keyObj instanceof String key)) {
	            throw new IllegalArgumentException(
	                "Property key must be a String at index " + i);
	        }

	        // --- Unknown key check ---
	        if (!KNOWN_KEYS.containsKey(key)) {
	            System.err.println("Warning: Unknown property key: " + key);
	            props.put(key, value);
	            continue;
	        }

	        // --- Type safety check ---
	        Class<?> expectedType = KNOWN_KEYS.get(key);
	        if (value != null && !expectedType.isInstance(value)) {
	            throw new IllegalArgumentException(
	                "Property '" + key + "' expects type " +
	                expectedType.getSimpleName() +
	                " but got " +
	                value.getClass().getSimpleName());
	        }

	        props.put(key, value);
	    }

	    return props;
	}
	
	/**
	 * Register a new key and its expected type. This allows for extensibility while maintaining some level of type safety.
	 *
	 * @param key          the property key to register
	 * @param expectedType the expected type of the value associated with this key
	 * @throws NullPointerException if key or expectedType is null
	 */
	public static void registerKey(String key, Class<?> expectedType) {
	    Objects.requireNonNull(key, "key");
	    Objects.requireNonNull(expectedType, "expectedType");
	    KNOWN_KEYS.put(key, expectedType);
	}

	/**
	 * Register a new key with an expected type of Object. This is a more permissive registration that allows any type of value for the key.
	 *
	 * @param key the property key to register
	 * @throws NullPointerException if key is null
	 */
	public static void registerKey(String key) {
	    registerKey(key, Object.class); // means “known, any type”
	}

	/**
	 * Get the full path of an image file to be used as a background image.
	 *
	 * @param props the properties
	 * @return the file path. On error return <code>null</code>.
	 */
	public static String getBackgroundImage(Properties props) {
		return getString(props, BACKGROUNDIMAGE, null);
	}

	/**
	 * Get the left location in pixels.
	 *
	 * @param props the properties
	 * @return the left location. On error, return 0.
	 */
	public static int getLeft(Properties props) {
		return getInt(props, LEFT, 0);
	}

	/**
	 * Get the top location in pixels.
	 *
	 * @param props the properties
	 * @return the top location. On error, return 0.
	 */
	public static int getTop(Properties props) {
		return getInt(props, TOP, 0);
	}

	/**
	 * Get the "is visible" boolean flag.
	 *
	 * @param props the properties
	 * @return the visible flag. On error, return true.
	 */
	public static boolean getVisible(Properties props) {
		return getBoolean(props, VISIBLE, true);
	}

	/**
	 * Get the "scrollable" boolean flag.
	 *
	 * @param props the properties
	 * @return the scrollable flag. On error, return false.
	 */
	public static boolean getScrollable(Properties props) {
		return getBoolean(props, SCROLLABLE, false);
	}
	
	/**
	 * Get the "wheel zoom" boolean flag.
	 * 
	 * @param props the properties
	 * @return the wheel zoom flag. On error, return false.
	 */
	public static boolean addWheelZoom(Properties props) {
		return getBoolean(props, WHEELZOOM, false);
	}

	/**
	 * Get the tool bar bits.
	 *
	 * @param props the properties
	 * @return the toolbar bits. On error, return 0.
	 */
	public static long getToolbarBits(Properties props) {
		return getLong(props, TOOLBARBITS, 0);
	}

	/**
	 * Get the "use standard view decorations" boolean flag.
	 *
	 * @param props the properties
	 * @return the decorations flag. On error, return true.
	 */
	public static boolean getStandardViewDecorations(Properties props) {
		return getBoolean(props, STANDARDVIEWDECORATIONS, true);
	}

	/**
	 * Get the closable boolean flag.
	 *
	 * @param props the properties
	 * @return the closable flag. On error, return true.
	 */
	public static boolean getClosable(Properties props) {
		return getBoolean(props, CLOSABLE, true);
	}

	/**
	 * Get the draggable boolean flag.
	 *
	 * @param props the properties
	 * @return the draggable flag. On error, return false.
	 */
	public static boolean getDraggable(Properties props) {
		return getBoolean(props, DRAGGABLE, false);
	}

	/**
	 * Get the view iconifiable boolean flag.
	 *
	 * @param props the properties
	 * @return the iconifiable flag. On error, return true.
	 */
	public static boolean getIconifiable(Properties props) {
		return getBoolean(props, ICONIFIABLE, true);
	}

	/**
	 * Get the view info button boolean flag.
	 *
	 * @param props the properties
	 * @return the info button flag. On error, return false.
	 */
	public static boolean getInfoButton(Properties props) {
		return getBoolean(props, INFOBUTTON, false);
	}

	/**
	 * Get the view maximize boolean flag. For views.
	 *
	 * @param props the properties
	 * @return the maximize flag. On error, return false.
	 */
	public static boolean getMaximize(Properties props) {
		return getBoolean(props, MAXIMIZE, false);
	}

	/**
	 * Get the view maximizable boolean flag. For views.
	 *
	 * @param props the properties
	 * @return the maximizable flag. On error, return true.
	 */
	public static boolean getMaximizable(Properties props) {
		return getBoolean(props, MAXIMIZABLE, true);
	}
	
	/**
	 * Get the item connectable boolean flag. 
	 *
	 * @param props the properties
	 * @return the connectable flag. On error, return false.
	 */
	public static boolean getConnectable(Properties props) {
		return getBoolean(props, CONNECTABLE, false);
	}
	
	/**
	 * Get the view double-clickable boolean flag. For views and items.
	 *
	 * @param props the properties
	 * @return the double-clickable flag. On error, return false.
	 */
	public static boolean getDoubleClickable(Properties props) {
		return getBoolean(props, DOUBLECLICKABLE, false);
	}
	
	/**
	 * Get the view deletable boolean flag. For views and items.
	 *
	 * @param props the properties
	 * @return the deletable flag. On error, return true.
	 */
	public static boolean getDeletable(Properties props) {
		return getBoolean(props, DELETABLE, true);
	}

	/**
	 * Get the view resizable boolean flag. For views.
	 *
	 * @param props the properties
	 * @return the resizable flag. On error, return true.
	 */
	public static boolean getResizable(Properties props) {
		return getBoolean(props, RESIZABLE, true);
	}
	
	/**
	 * Get the view right-clickable boolean flag. For views and items.
	 *
	 * @param props the properties
	 * @return the right-clickable flag. On error, return true.
	 */
	public static boolean getRightClickable(Properties props) {
		return getBoolean(props, RIGHTCLICKABLE, true);
	}

	/**
	 * Get the item rotatable boolean flag. For views.
	 *
	 * @param props the properties
	 * @return the rotatable flag. On error, return true.
	 */
	public static boolean getRotatable(Properties props) {
		return getBoolean(props, ROTATABLE, false);
	}

	/**
	 * Get a world coordinate system
	 *
	 * @param props the properties
	 * @return a world rectangle. On error, return defaultWorldRect
	 */
	public static Rectangle2D.Double getWorldSystem(Properties props) {
		return getWorldRectangle(props, WORLDSYSTEM, defaultWorldRect);
	}

	/**
	 * Get a title
	 *
	 * @param props the properties
	 * @return a title On error return unknownString.
	 */
	public static String getTitle(Properties props) {
		return getString(props, TITLE, unknownString);
	}


	/**
	 * Get a container from the properties
	 *
	 * @param props the properties
	 * @return an IContainer, on error return null
	 */
	public static IContainer getContainer(Properties props) {
		Object val = props.get(CONTAINER);
		if ((val == null) || !(val instanceof IContainer)) {
			return null;
		}
		return (IContainer) val;
	}

	/**
	 * Get a fraction (maybe of screen, maybe of app) from the properties
	 *
	 * @param props the properties
	 * @return a (screen) fraction, by default return Double.NaN.
	 */
	public static double getFraction(Properties props) {
		return getDouble(props, FRACTION, Double.NaN);
	}

	/**
	 * Get an aspect ratio from the properties
	 *
	 * @param props the properties
	 * @return an aspect ratio used (optionally) to size views
	 */
	public static double getAspectRatio(Properties props) {
		return getDouble(props, ASPECT, 1);
	}

	/**
	 * Get an optional component used to split the west part of a view
	 *
	 * @param props the properties
	 * @return the component
	 */
	public static JComponent getSplitWestComponent(Properties props) {
		return getJComponent(props, SPLITWESTCOMPONENT);
	}

	/**
	 * Get a symbol from the properties
	 *
	 * @param props the properties
	 * @return a SymbolType, on error return SymbolType.SQUARE
	 */
	public static SymbolType getSymbol(Properties props) {
		Object val = props.get(SYMBOL);
		if ((val == null) || !(val instanceof SymbolType)) {
			return SymbolType.SQUARE;
		}
		return (SymbolType) val;
	}

	/**
	 * Get the symbol size in pixels
	 *
	 * @param props the properties
	 * @return get the symbol size (width and height) in pixels. On error return 8.
	 */
	public static int getSymbolSize(Properties props) {
        return getInt(props, SYMBOLSIZE, 8);
	}

	/**
	 * Get the optional user data. This is any object that the user wants to attach
	 * to the shape.
	 *
	 * @param props the properties
	 * @return the user data (might be <code>null</code>).
	 */
	public static Object getUserData(Properties props) {
		return props.get(USERDATA);
	}

	/**
	 * Get the background color from the properties
	 *
	 * @param props the properties
	 * @return the background color. On error return null.
	 */
	public static Color getBackground(Properties props) {
		return getColor(props, BACKGROUND, null);
	}

	/**
	 * Get the text color from the properties
	 *
	 * @param props the properties
	 * @return the text color. On error return _defaultTextColor.
	 */
	public static Color getTextColor(Properties props) {
		return getColor(props, TEXTCOLOR, defaultTextColor);
	}

	/**
	 * Get the fill color from the properties
	 *
	 * @param props the properties
	 * @return the fill color. On error return _defaultFillColor.
	 */
	public static Color getFillColor(Properties props) {
		return getColor(props, FILLCOLOR, defaultFillColor);
	}

	/**
	 * Get the line color from the properties
	 *
	 * @param props the properties
	 * @return the line color. On error return _defaultLineColor.
	 */
	public static Color getLineColor(Properties props) {
		return getColor(props, LINECOLOR, defaultLineColor);
	}

	/**
	 * Get the line style from the properties.
	 *
	 * @param props the properties.
	 * @return the linestyle. On error return LineStyle.SOLID.
	 */
	public static LineStyle getLineStyle(Properties props) {
		LineStyle lineStyle = LineStyle.SOLID;
		Object val = props.get(LINESTYLE);

		if ((val != null) && (val instanceof LineStyle)) {
			return (LineStyle) val;
		}
		return lineStyle;
	}

	/**
	 * Get a line width from the properties
	 *
	 * @param props the properties
	 * @return the width. On error return 0.
	 */
	public static float getLineWidth(Properties props) {
		return getFloat(props, LINEWIDTH, 0);
	}

	/**
	 * Get a width from the properties
	 *
	 * @param props the properties
	 * @return the width. On error return Integer.MIN_VALUE (-2^31 = -2147483648)
	 */
	public static int getWidth(Properties props) {
		return getInt(props, WIDTH, Integer.MIN_VALUE);
	}

	/**
	 * Get a height from the properties
	 *
	 * @param props the properties
	 * @return the height. On error return Integer.MIN_VALUE (-2^31 = -2147483648)
	 */
	public static int getHeight(Properties props) {
		return getInt(props, HEIGHT, Integer.MIN_VALUE);
	}

	/**
	 * Get the locked flag. (Default is true)
	 *
	 * @param props the properties
	 * @return the locked flag
	 */
	public static boolean getLocked(Properties props) {
		return getBoolean(props, LOCKED, true);
	}

	/**
	 * Get a world rectangle
	 *
	 * @param props        the properties
	 * @param key          the key
	 * @param defaultValue the default value
	 * @return the world rectangle, or defaultValue upon failure.
	 */
	public static Rectangle2D.Double getWorldRectangle(Properties props, String key, Rectangle2D.Double defaultValue) {
		Object val = props.get(key);
		if (val == null) {
			return defaultValue;
		}

		if (val instanceof Rectangle2D.Double) {
			return (Rectangle2D.Double) val;
		}
		return null;
	}

	/**
	 * Get a color from a properties. Tries to handle both a String (from the X11
	 * database, e.g. "coral", "red", "powder blue") and a Java Color value.
	 *
	 * @param props        the properties
	 * @param key          the key
	 * @param defaultValue the default value
	 * @return the color, or defaultValue upon failure.
	 */
	public static Color getColor(Properties props, String key, Color defaultValue) {
		Object val = props.get(key);
		if (val == null) {
			return defaultValue;
		}

		if (val instanceof String) {
			return X11Colors.getX11Color((String) val);
		}

		if (val instanceof Color) {
			return (Color) val;
		}
		return defaultValue;
	}

	/**
	 * Get a JComponent from properties.
	 *
	 * @param props        the properties
	 * @param key          the key
	 * @param defaultValue the default value
	 * @return the JComponent value, or on error <code>null</code>.
	 */
	public static JComponent getJComponent(Properties props, String key) {
		Object val = props.get(key);
		if ((val != null) && (val instanceof JComponent)) {
			return (JComponent) val;
		}
		return null;
	}

	/**
	 * Get the box-zoom rubberband policy from properties.
	 *
	 * @param props the properties
	 * @return the rubberband policy. On error return null.
	 */
	public static ARubberband.Policy getBoxZoomRubberbandPolicy(Properties props) {
		Object val = props.get(BOXZOOMRBPOLICY);
		if ((val != null) && (val instanceof ARubberband.Policy)) {
			return (ARubberband.Policy) val;
		}
		return ARubberband.Policy.RECTANGLE_PRESERVE_ASPECT;
	}

	/**
	 * Get a String from properties.
	 *
	 * @param props        the properties
	 * @param key          the key
	 * @param defaultValue the default value
	 * @return the String value, or on error the default value.
	 */
	public static String getString(Properties props, String key, String defaultValue) {
		Object val = props.get(key);
		if (val == null) {
			return defaultValue;
		}

		if (val instanceof String) {
			return (String) val;
		}
		return defaultValue;
	}

	/**
	 * Get an int from properties. Tries to handle both a String (e.g., "67") and
	 * Integer value.
	 *
	 * @param props        the properties
	 * @param key          the key
	 * @param defaultValue the default value
	 * @return the integer value, or on error the default value.
	 */
	public static int getInt(Properties props, String key, int defaultValue) {
		Object val = props.get(key);
		if (val == null) {
			return defaultValue;
		}

		if (val instanceof String) {
			try {
				return Integer.parseInt((String) val);
			} catch (Exception e) {
				return defaultValue;
			}

		}

		if (val instanceof Integer) {
			return (Integer) val;
		}
		return defaultValue;

	}

	/**
	 * Get a long from properties. Tries to handle both a String (e.g., "67") and
	 * Long value.
	 *
	 * @param props        the properties
	 * @param key          the key
	 * @param defaultValue the default value
	 * @return the integer value, or on error the default value.
	 */
	public static long getLong(Properties props, String key, long defaultValue) {
		Object val = props.get(key);
		if (val == null) {
			return defaultValue;
		}

		if (val instanceof String) {
			try {
				return Integer.parseInt((String) val);
			} catch (Exception e) {
				return defaultValue;
			}
		}

		if (val instanceof Long) {
			return (Long) val;
		}
		return defaultValue;

	}

	/**
	 * Get a double from properties. Tries to handle both a String (e.g., "67.0")
	 * and Double value.
	 *
	 * @param props the properties
	 * @param key   the key
	 * @return the double value, or on error return Double.NaN.
	 */
	public static double getDouble(Properties props, String key) {
		return getDouble(props, key, Double.NaN);
	}

	/**
	 * Get a double from properties. Tries to handle both a String (e.g., "67.0")
	 * and Double value.
	 *
	 * @param props        the properties
	 * @param key          the key
	 * @param defaultValue the default value
	 * @return the double value, or on error return defaultValue
	 */
	public static double getDouble(Properties props, String key, double defaultValue) {
		Object val = props.get(key);
		if (val == null) {
			return defaultValue;
		}

		if (val instanceof String) {
			try {
				return Double.parseDouble((String) val);
			} catch (Exception e) {
				return defaultValue;
			}

		}

		if (val instanceof Double) {
			return (Double) val;
		}

		if (val instanceof Float) {
			return ((Float) val);
		}

		if (val instanceof Integer) {
			return ((Integer) val);
		}

		return defaultValue;
	}

	/**
	 * Get a float from properties. Tries to handle both a String (e.g., "67.0") and
	 * Float value.
	 *
	 * @param props the properties
	 * @param key   the key
	 * @return the double value, or on error return Float.NaN.
	 */
	public static float getFloat(Properties props, String key) {
		return getFloat(props, key, Float.NaN);
	}

	/**
	 * Get a float from properties. Tries to handle both a String (e.g., "67.0") and
	 * Float value.
	 *
	 * @param props        the properties
	 * @param key          the key
	 * @param defaultValue the default value
	 * @return the double value, or on error return defaultValue
	 */
	public static float getFloat(Properties props, String key, float defaultValue) {
		Object val = props.get(key);
		if (val == null) {
			return defaultValue;
		}

		if (val instanceof String) {
			try {
				return Float.parseFloat((String) val);
			} catch (Exception e) {
				return defaultValue;
			}

		}

		if (val instanceof Float) {
			return (Float) val;
		}

		if (val instanceof Integer) {
			return ((Integer) val);
		}

		return defaultValue;

	}

	/**
	 * Get a boolean from properties. Tries to handle both a String (e.g., "true")
	 * and Boolean value.
	 *
	 * @param props   the properties
	 * @param key     the key
	 * @param boolean defaultVal
	 * @return the boolean value, or on error the default
	 */
	public static boolean getBoolean(Properties props, String key, boolean defaultVal) {
		Object val = props.get(key);
		if (val == null) {
			return defaultVal;
		}

		if (val instanceof String) {
			try {
				return Boolean.parseBoolean((String) val);
			} catch (Exception e) {
				return defaultVal;
			}

		}

		if (val instanceof Boolean) {
			return (Boolean) val;
		}
		return defaultVal;

	}

}
