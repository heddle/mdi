package edu.cnu.mdi.properties;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.util.PropertyUtils;

class PropertyUtilsTest {

    @Test
    void fromKeyValuesNullOrEmptyReturnsEmptyProperties() {
        Properties p1 = PropertyUtils.fromKeyValues((Object[]) null);
        assertNotNull(p1);
        assertTrue(p1.isEmpty());

        Properties p2 = PropertyUtils.fromKeyValues();
        assertNotNull(p2);
        assertTrue(p2.isEmpty());
    }

    @Test
    void fromKeyValuesRejectsOddNumberOfArguments() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class,
                        () -> PropertyUtils.fromKeyValues(PropertyUtils.DRAGGABLE));
        assertTrue(ex.getMessage().toLowerCase().contains("pairs"));
    }

    @Test
    void fromKeyValuesRejectsNonStringKeys() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class,
                        () -> PropertyUtils.fromKeyValues(123, true));
        assertTrue(ex.getMessage().contains("Property key must be a String"));
    }

    @Test
    void fromKeyValuesAcceptsKnownKeyWithCorrectType() {
        Properties props = PropertyUtils.fromKeyValues(PropertyUtils.DRAGGABLE, Boolean.TRUE);
        assertEquals(Boolean.TRUE, props.get(PropertyUtils.DRAGGABLE));
    }

    @Test
    void fromKeyValuesRejectsKnownKeyWithWrongType() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class,
                        () -> PropertyUtils.fromKeyValues(PropertyUtils.DRAGGABLE, "yes"));
        assertTrue(ex.getMessage().contains(PropertyUtils.DRAGGABLE));
        assertTrue(ex.getMessage().toLowerCase().contains("expects type"));
    }

    @Test
    void fromKeyValuesAllowsUnknownKeyAndWarnsToStderr() {
        PrintStream oldErr = System.err;
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errBytes));
        try {
            String key = "MY_CUSTOM_KEY";
            Properties props = PropertyUtils.fromKeyValues(key, 42);

            assertEquals(42, props.get(key));

            String err = errBytes.toString();
            assertTrue(err.contains("Warning: Unknown property key: " + key));
        } finally {
            System.setErr(oldErr);
        }
    }

    @Test
    void registerKeyMakesKeyKnownAndEnforcesType() {
        // This test will currently FAIL with UnsupportedOperationException
        // because KNOWN_KEYS is built with Map.ofEntries(...) (unmodifiable). :contentReference[oaicite:1]{index=1}
        PropertyUtils.registerKey("MY_TYPED_KEY", Integer.class);

        Properties ok = PropertyUtils.fromKeyValues("MY_TYPED_KEY", 7);
        assertEquals(7, ok.get("MY_TYPED_KEY"));

        assertThrows(IllegalArgumentException.class,
                () -> PropertyUtils.fromKeyValues("MY_TYPED_KEY", "nope"));
    }
    
    @Test
    void registerKeyRefusesOverwrite() {
        String key = "MY_DUP_KEY";

        PropertyUtils.registerKey(key, Integer.class);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> PropertyUtils.registerKey(key, Double.class));

        assertTrue(ex.getMessage().contains("already registered"));
    }
    
    @Test
    void registerKeyOverwriteReplacesType() {
        String key = "MY_OVERWRITE_KEY";

        PropertyUtils.registerKey(key, Integer.class);
        PropertyUtils.registerKeyOverwrite(key, Double.class);

        // now should accept Double and reject Integer
        assertEquals(3.14, PropertyUtils.fromKeyValues(key, 3.14).get(key));
        assertThrows(IllegalArgumentException.class,
            () -> PropertyUtils.fromKeyValues(key, 7));
    }
    
}