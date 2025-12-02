package edu.cnu.mdi.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PropertySupport}.
 *
 * <p>These tests verify that {@link PropertySupport} provides predictable,
 * robust access to {@link Properties} values across a variety of types,
 * including integers, longs, doubles, floats, booleans, strings, enums, and
 * the helper methods for converting to and from key/value arrays.</p>
 */
public class PropertySupportTest {

    /**
     * Simple enum used to test {@link PropertySupport#getEnum(Properties, String, Class, Enum)}.
     */
    private enum TestEnum {
        ALPHA,
        BETA,
        GAMMA
    }

    // -------------------------------------------------------------------------
    // fromKeyValues
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link PropertySupport#fromKeyValues(Object...)} returns null
     * for null or too-short input arrays.
     */
    @Test
    void fromKeyValues_nullOrTooShort_returnsNull() {
        assertNull(PropertySupport.fromKeyValues((Object[]) null),
                "Null keyValues should produce null Properties");

        assertNull(PropertySupport.fromKeyValues("onlyKey"),
                "Single-element keyValues should produce null Properties");
    }

    /**
     * Verifies that {@link PropertySupport#fromKeyValues(Object...)} builds a
     * {@link Properties} instance from alternating key/value pairs, and that
     * null keys are skipped.
     */
    @Test
    void fromKeyValues_buildsPropertiesAndSkipsNullKeys() {
        Properties props = PropertySupport.fromKeyValues(
                "width", 800,
                null, "ignoredValue",
                "visible", true,
                "title", "Main Window",
                "lastKey" // odd trailing element, ignored
        );

        assertNotNull(props, "Properties should be created");
        assertEquals(3, props.size(), "Only three non-null keys should be stored");

        assertEquals(800, props.get("width"));
        assertEquals(Boolean.TRUE, props.get("visible"));
        assertEquals("Main Window", props.get("title"));
        assertNull(props.get("ignoredValue"));
        assertNull(props.get("lastKey"));
    }

    // -------------------------------------------------------------------------
    // toObjectArray
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link PropertySupport#toObjectArray(Properties)} returns
     * null for null or empty properties.
     */
    @Test
    void toObjectArray_nullOrEmpty_returnsNull() {
        assertNull(PropertySupport.toObjectArray(null),
                "Null Properties should yield null array");

        Properties props = new Properties();
        assertNull(PropertySupport.toObjectArray(props),
                "Empty Properties should yield null array");
    }

    /**
     * Verifies that {@link PropertySupport#toObjectArray(Properties)} returns a
     * key/value array of the expected size, and that all entries are present.
     *
     * <p>Order is not guaranteed, so we reconstruct a map from the array.</p>
     */
    @Test
    void toObjectArray_containsAllEntries() {
        Properties props = new Properties();
        props.put("width", 800);
        props.put("visible", Boolean.TRUE);
        props.put("title", "Main");

        Object[] array = PropertySupport.toObjectArray(props);

        assertNotNull(array);
        assertEquals(2 * props.size(), array.length, "Array length must be 2 * number of properties");

        Map<Object, Object> map = new HashMap<>();
        for (int i = 0; i < array.length; i += 2) {
            Object key = array[i];
            Object value = array[i + 1];
            map.put(key, value);
        }

        assertEquals(3, map.size());
        assertEquals(800, map.get("width"));
        assertEquals(Boolean.TRUE, map.get("visible"));
        assertEquals("Main", map.get("title"));
    }

    // -------------------------------------------------------------------------
    // getString
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link PropertySupport#getString(Properties, String, String)}
     * returns stored strings and falls back to the default for missing or
     * non-string values.
     */
    @Test
    void getString_returnsStoredStringOrDefault() {
        Properties props = new Properties();
        props.put("str", "hello");
        props.put("num", 42);

        assertEquals("hello",
                PropertySupport.getString(props, "str", "default"));
        assertEquals("default",
                PropertySupport.getString(props, "missing", "default"));
        assertEquals("default",
                PropertySupport.getString(props, "num", "default"));
    }

    // -------------------------------------------------------------------------
    // getInt
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link PropertySupport#getInt(Properties, String, int)} handles
     * Integer, Number, and String inputs, and falls back to default for invalid data.
     */
    @Test
    void getInt_handlesIntegerNumberAndString() {
        Properties props = new Properties();
        props.put("intVal", 10);
        props.put("doubleVal", 3.9);
        props.put("strVal", "123");
        props.put("badStr", "notAnInt");

        assertEquals(10, PropertySupport.getInt(props, "intVal", -1));
        assertEquals(3, PropertySupport.getInt(props, "doubleVal", -1));
        assertEquals(123, PropertySupport.getInt(props, "strVal", -1));
        assertEquals(-1, PropertySupport.getInt(props, "missing", -1));
        assertEquals(-1, PropertySupport.getInt(props, "badStr", -1));
    }

    // -------------------------------------------------------------------------
    // getLong
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link PropertySupport#getLong(Properties, String, long)} handles
     * Long, Number, and String inputs correctly.
     */
    @Test
    void getLong_handlesLongNumberAndString() {
        Properties props = new Properties();
        props.put("longVal", 100L);
        props.put("intVal", 42);
        props.put("strVal", "9876543210");
        props.put("badStr", "NaN");

        assertEquals(100L, PropertySupport.getLong(props, "longVal", -1L));
        assertEquals(42L, PropertySupport.getLong(props, "intVal", -1L));
        assertEquals(9876543210L, PropertySupport.getLong(props, "strVal", -1L));
        assertEquals(-1L, PropertySupport.getLong(props, "missing", -1L));
        assertEquals(-1L, PropertySupport.getLong(props, "badStr", -1L));
    }

    // -------------------------------------------------------------------------
    // getDouble
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link PropertySupport#getDouble(Properties, String, double)}
     * handles Double, Number, and String inputs.
     */
    @Test
    void getDouble_handlesDoubleNumberAndString() {
        Properties props = new Properties();
        props.put("doubleVal", 1.5);
        props.put("intVal", 2);
        props.put("strVal", "3.14");
        props.put("badStr", "pi");

        assertEquals(1.5, PropertySupport.getDouble(props, "doubleVal", -1.0), 1e-9);
        assertEquals(2.0, PropertySupport.getDouble(props, "intVal", -1.0), 1e-9);
        assertEquals(3.14, PropertySupport.getDouble(props, "strVal", -1.0), 1e-9);
        assertEquals(-1.0, PropertySupport.getDouble(props, "missing", -1.0), 1e-9);
        assertEquals(-1.0, PropertySupport.getDouble(props, "badStr", -1.0), 1e-9);
    }

    // -------------------------------------------------------------------------
    // getFloat
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link PropertySupport#getFloat(Properties, String, float)}
     * handles Float, Number, and String inputs.
     */
    @Test
    void getFloat_handlesFloatNumberAndString() {
        Properties props = new Properties();
        props.put("floatVal", 1.25f);
        props.put("doubleVal", 2.5);
        props.put("strVal", "3.5");
        props.put("badStr", "x");

        assertEquals(1.25f, PropertySupport.getFloat(props, "floatVal", -1.0f), 1e-6);
        assertEquals(2.5f, PropertySupport.getFloat(props, "doubleVal", -1.0f), 1e-6);
        assertEquals(3.5f, PropertySupport.getFloat(props, "strVal", -1.0f), 1e-6);
        assertEquals(-1.0f, PropertySupport.getFloat(props, "missing", -1.0f), 1e-6);
        assertEquals(-1.0f, PropertySupport.getFloat(props, "badStr", -1.0f), 1e-6);
    }

    // -------------------------------------------------------------------------
    // getBoolean
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link PropertySupport#getBoolean(Properties, String, boolean)}
     * handles Boolean and String ("true"/"false") inputs correctly.
     */
    @Test
    void getBoolean_handlesBooleanAndString() {
        Properties props = new Properties();
        props.put("boolVal", Boolean.TRUE);
        props.put("strTrue", "true");
        props.put("strFalse", "false");
        props.put("strOther", "yes");

        assertTrue(PropertySupport.getBoolean(props, "boolVal", false));
        assertTrue(PropertySupport.getBoolean(props, "strTrue", false));
        assertFalse(PropertySupport.getBoolean(props, "strFalse", true));

        // "yes" will be parsed as false by Boolean.parseBoolean
        assertFalse(PropertySupport.getBoolean(props, "strOther", true));

        // Missing key uses default
        assertTrue(PropertySupport.getBoolean(props, "missing", true));
    }

    // -------------------------------------------------------------------------
    // getEnum
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link PropertySupport#getEnum(Properties, String, Class, Enum)}
     * returns stored enum instances directly when already the correct type.
     */
    @Test
    void getEnum_returnsStoredEnumInstance() {
        Properties props = new Properties();
        props.put("e", TestEnum.BETA);

        TestEnum result = PropertySupport.getEnum(props, "e", TestEnum.class, TestEnum.ALPHA);
        assertEquals(TestEnum.BETA, result);
    }

    /**
     * Verifies that enum values can be parsed from string names.
     */
    @Test
    void getEnum_parsesEnumFromString() {
        Properties props = new Properties();
        props.put("e", "GAMMA");

        TestEnum result = PropertySupport.getEnum(props, "e", TestEnum.class, TestEnum.ALPHA);
        assertEquals(TestEnum.GAMMA, result);
    }

    /**
     * Verifies that invalid enum names or missing keys cause the default enum
     * value to be returned.
     */
    @Test
    void getEnum_invalidOrMissing_returnsDefault() {
        Properties props = new Properties();
        props.put("e", "NO_SUCH_CONSTANT");

        TestEnum resultInvalid = PropertySupport.getEnum(props, "e", TestEnum.class, TestEnum.ALPHA);
        TestEnum resultMissing = PropertySupport.getEnum(props, "missing", TestEnum.class, TestEnum.ALPHA);

        assertEquals(TestEnum.ALPHA, resultInvalid);
        assertEquals(TestEnum.ALPHA, resultMissing);
    }

    // -------------------------------------------------------------------------
    // Null handling
    // -------------------------------------------------------------------------

    /**
     * Verifies that most getters throw {@link NullPointerException} when
     * called with a null Properties reference, as they use
     * {@link java.util.Objects#requireNonNull(Object)}.
     */
    @Test
    void getters_throwOnNullProperties() {
        Properties nullProps = null;

        assertThrows(NullPointerException.class,
                () -> PropertySupport.getString(nullProps, "k", "d"));
        assertThrows(NullPointerException.class,
                () -> PropertySupport.getInt(nullProps, "k", 0));
        assertThrows(NullPointerException.class,
                () -> PropertySupport.getLong(nullProps, "k", 0L));
        assertThrows(NullPointerException.class,
                () -> PropertySupport.getDouble(nullProps, "k", 0.0));
        assertThrows(NullPointerException.class,
                () -> PropertySupport.getFloat(nullProps, "k", 0.0f));
        assertThrows(NullPointerException.class,
                () -> PropertySupport.getBoolean(nullProps, "k", true));
        assertThrows(NullPointerException.class,
                () -> PropertySupport.getEnum(nullProps, "k", TestEnum.class, TestEnum.ALPHA));
    }
}
