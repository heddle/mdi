package edu.cnu.mdi.util;

import java.util.Enumeration;
import java.util.Objects;
import java.util.Properties;

/**
 * A lightweight utility class providing type-safe accessors for
 * {@link java.util.Properties}.
 *
 * <p>This class is intentionally UI-agnostic and suitable for use throughout
 * the core MDI framework. Its role is to simplify common operations such as
 * reading integers, floating-point values, booleans, strings, and enums from
 * a {@link Properties} structure while maintaining predictable behavior in the
 * face of missing keys, unexpected value types, or malformed data.</p>
 *
 * <p>Each typed getter follows the same rules:</p>
 * <ul>
 *     <li>If {@code props} is {@code null}, a {@link NullPointerException} is thrown.</li>
 *     <li>If the key is absent or the value cannot be converted, the supplied
 *         default value is returned.</li>
 *     <li>Supported conversions include:
 *         <ul>
 *             <li>Number → numeric primitive types</li>
 *             <li>String → parsed number or boolean</li>
 *             <li>Enum → via {@link Enum#valueOf(Class, String)}</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <p>The class also includes helpers for converting between a
 * {@link Properties} instance and an alternating key/value {@code Object[]}
 * array, used frequently in MDI view and layout configuration.</p>
 *
 * <p>The class is {@code final} with a private constructor because it is
 * intended strictly as a static utility.</p>
 */
public final class PropertySupport {

    /**
     * Private constructor preventing instantiation.
     * Utility class: use static methods only.
     */
    private PropertySupport() {}

    // ---------------------------------------------------------------------
    // Construction helpers
    // ---------------------------------------------------------------------

    /**
     * Creates a {@link Properties} object from an even-length array of
     * alternating key/value pairs.
     *
     * <p>Example usage:</p>
     * <pre>
     * Properties p = PropertySupport.fromKeyValues(
     *     "width", 800,
     *     "height", 600,
     *     "title", "Main Window"
     * );
     * </pre>
     *
     * <p>Keys are converted to strings through {@code toString()}. Values may
     * be any object and are stored directly.</p>
     *
     * @param keyValues an array of alternating key/value entries.
     * @return a populated {@link Properties} object, or {@code null} if
     *         {@code keyValues} is {@code null} or has fewer than 2 elements.
     */
    public static Properties fromKeyValues(Object... keyValues) {
        if (keyValues == null || keyValues.length < 2) {
            return null;
        }

        Properties props = new Properties();

        for (int i = 0; i < keyValues.length - 1; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];

            if (key == null) {
                // Null keys are ignored
                continue;
            }

            props.put(key.toString(), value);
        }

        return props;
    }

    /**
     * Converts a {@link Properties} instance into an alternating key/value
     * {@code Object[]} array.
     *
     * <p>Example output structure:</p>
     * <pre>
     * ["width", 800, "visible", true, "title", "Main"]
     * </pre>
     *
     * @param props the properties to convert.
     * @return an array of size {@code 2 * props.size()}, or {@code null} if
     *         {@code props} is null or empty.
     */
    public static Object[] toObjectArray(Properties props) {
        if (props == null || props.isEmpty()) {
            return null;
        }

        Object[] array = new Object[2 * props.size()];
        int i = 0;

        for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            Object value = props.get(key);
            array[i] = key;
            array[i + 1] = value;
            i += 2;
        }

        return array;
    }

    // ---------------------------------------------------------------------
    // Typed getters
    // ---------------------------------------------------------------------

    /**
     * Retrieves a String property value.
     *
     * @param props the properties source (must not be {@code null}).
     * @param key the property name.
     * @param defaultValue returned if key is missing or not a String.
     * @return the stored String or {@code defaultValue}.
     */
    public static String getString(Properties props, String key, String defaultValue) {
        Objects.requireNonNull(props, "props must not be null");
        Object val = props.get(key);
        return (val instanceof String) ? (String) val : defaultValue;
    }

    /**
     * Retrieves an integer property. Accepts:
     * <ul>
     *     <li>{@link Integer}</li>
     *     <li>Any {@link Number}</li>
     *     <li>{@link String} parseable as an integer</li>
     * </ul>
     *
     * @param props the properties source.
     * @param key the lookup key.
     * @param defaultValue used when conversion fails.
     * @return the integer value or {@code defaultValue}.
     */
    public static int getInt(Properties props, String key, int defaultValue) {
        Objects.requireNonNull(props, "props must not be null");
        Object val = props.get(key);

        if (val instanceof Integer) {
            return (Integer) val;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * Retrieves a long property.
     *
     * @param props the properties source.
     * @param key the lookup key.
     * @param defaultValue returned on failure.
     * @return the long value or {@code defaultValue}.
     */
    public static long getLong(Properties props, String key, long defaultValue) {
        Objects.requireNonNull(props, "props must not be null");
        Object val = props.get(key);

        if (val instanceof Long) {
            return (Long) val;
        }
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val instanceof String) {
            try {
                return Long.parseLong((String) val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * Retrieves a double property.
     *
     * @param props the properties source.
     * @param key the lookup key.
     * @param defaultValue returned when conversion fails.
     * @return the double value or {@code defaultValue}.
     */
    public static double getDouble(Properties props, String key, double defaultValue) {
        Objects.requireNonNull(props, "props must not be null");
        Object val = props.get(key);

        if (val instanceof Double) {
            return (Double) val;
        }
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        if (val instanceof String) {
            try {
                return Double.parseDouble((String) val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * Retrieves a float property.
     *
     * @param props the properties source.
     * @param key the lookup key.
     * @param defaultValue returned when conversion fails.
     * @return the float value or {@code defaultValue}.
     */
    public static float getFloat(Properties props, String key, float defaultValue) {
        Objects.requireNonNull(props, "props must not be null");
        Object val = props.get(key);

        if (val instanceof Float) {
            return (Float) val;
        }
        if (val instanceof Number) {
            return ((Number) val).floatValue();
        }
        if (val instanceof String) {
            try {
                return Float.parseFloat((String) val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * Retrieves a boolean property. Accepts:
     * <ul>
     *   <li>{@link Boolean}</li>
     *   <li>{@link String} — parsed via {@link Boolean#parseBoolean(String)}</li>
     * </ul>
     *
     * @param props the properties source.
     * @param key the lookup key.
     * @param defaultValue returned on failure.
     * @return the boolean value or {@code defaultValue}.
     */
    public static boolean getBoolean(Properties props, String key, boolean defaultValue) {
        Objects.requireNonNull(props, "props must not be null");
        Object val = props.get(key);

        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }
        return defaultValue;
    }

    /**
     * Retrieves an enum constant by name.
     *
     * <p>Accepts either an enum instance or a matching enum name in string
     * form. Returns {@code defaultValue} if conversion fails.</p>
     *
     * @param <E> enum type
     * @param props the properties source.
     * @param key the lookup key.
     * @param enumClass the enum class.
     * @param defaultValue fallback value.
     * @return the enum constant or {@code defaultValue}.
     */
    public static <E extends Enum<E>> E getEnum(
            Properties props,
            String key,
            Class<E> enumClass,
            E defaultValue) {

        Objects.requireNonNull(props, "props must not be null");
        Objects.requireNonNull(enumClass, "enumClass must not be null");

        Object val = props.get(key);

        if (enumClass.isInstance(val)) {
            return enumClass.cast(val);
        }

        if (val instanceof String) {
            try {
                return Enum.valueOf(enumClass, (String) val);
            } catch (IllegalArgumentException ignored) {}
        }

        return defaultValue;
    }
}
