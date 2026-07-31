package edu.cnu.mdi.util;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.graphics.style.LineStyle;
import edu.cnu.mdi.graphics.style.SymbolType;

public class PropertyUtilsTest {

	private static final double TOL = 1.0e-12;

	@Test
	public void testFromKeyValuesWithNoArgumentsReturnsEmptyProperties() {
		Properties props = PropertyUtils.fromKeyValues();

		assertNotNull(props);
		assertTrue(props.isEmpty());
	}

	@Test
	public void testFromKeyValuesWithNullArgumentsReturnsEmptyProperties() {
		Properties props = PropertyUtils.fromKeyValues((Object[]) null);

		assertNotNull(props);
		assertTrue(props.isEmpty());
	}

	@Test
	public void testFromKeyValuesStoresKnownTypedValues() {
		Properties props = PropertyUtils.fromKeyValues(
				PropertyUtils.TITLE, "My View",
				PropertyUtils.WIDTH, 800,
				PropertyUtils.HEIGHT, 600,
				PropertyUtils.VISIBLE, false,
				PropertyUtils.ASPECT, 1.5,
				PropertyUtils.LINEWIDTH, 2.5f,
				PropertyUtils.TOOLBARBITS, 123L,
				PropertyUtils.BACKGROUND, Color.BLUE,
				PropertyUtils.LINESTYLE, LineStyle.DASH,
				PropertyUtils.SYMBOL, SymbolType.CIRCLE);

		assertEquals("My View", PropertyUtils.getTitle(props));
		assertEquals(800, PropertyUtils.getWidth(props));
		assertEquals(600, PropertyUtils.getHeight(props));
		assertFalse(PropertyUtils.getVisible(props));
		assertEquals(1.5, PropertyUtils.getAspectRatio(props), TOL);
		assertEquals(2.5f, PropertyUtils.getLineWidth(props), 1.0e-6f);
		assertEquals(123L, PropertyUtils.getToolbarBits(props));
		assertEquals(Color.BLUE, PropertyUtils.getBackground(props));
		assertEquals(LineStyle.DASH, PropertyUtils.getLineStyle(props));
		assertEquals(SymbolType.CIRCLE, PropertyUtils.getSymbol(props));
	}

	@Test
	public void testFromKeyValuesRejectsOddNumberOfArguments() {
		assertThrows(IllegalArgumentException.class,
				() -> PropertyUtils.fromKeyValues(PropertyUtils.TITLE));
	}

	@Test
	public void testFromKeyValuesRejectsNonStringKey() {
		assertThrows(IllegalArgumentException.class,
				() -> PropertyUtils.fromKeyValues(123, "bad key"));
	}

	@Test
	public void testFromKeyValuesRejectsWrongTypeForKnownKey() {
		assertThrows(IllegalArgumentException.class,
				() -> PropertyUtils.fromKeyValues(PropertyUtils.WIDTH, "800"));

		assertThrows(IllegalArgumentException.class,
				() -> PropertyUtils.fromKeyValues(PropertyUtils.VISIBLE, "true"));

		assertThrows(IllegalArgumentException.class,
				() -> PropertyUtils.fromKeyValues(PropertyUtils.BACKGROUND, "red"));
	}

	@Test
	public void testFromKeyValuesRejectsNullValuesClearly() {
		assertThrows(IllegalArgumentException.class,
				() -> PropertyUtils.fromKeyValues(PropertyUtils.TITLE, null));
		assertThrows(IllegalArgumentException.class,
				() -> PropertyUtils.fromKeyValues("UNKNOWN_NULL", null));
	}

	@Test
	public void testFromKeyValuesAllowsUnknownKey() {
		java.io.PrintStream originalErr = System.err;
		java.io.ByteArrayOutputStream errBytes = new java.io.ByteArrayOutputStream();

		try {
			System.setErr(new java.io.PrintStream(errBytes));

			Properties props = PropertyUtils.fromKeyValues("CUSTOM_UNKNOWN_KEY", "custom value");

			assertEquals("custom value", props.get("CUSTOM_UNKNOWN_KEY"));
			assertTrue(errBytes.toString().contains("Warning: Unknown property key: CUSTOM_UNKNOWN_KEY"));
		} finally {
			System.setErr(originalErr);
		}
	}
	
	@Test
	public void testRegisterKeyAllowsCustomTypedKey() {
		String key = "TEST_CUSTOM_STRING_" + System.nanoTime();

		PropertyUtils.registerKey(key, String.class);

		Properties props = PropertyUtils.fromKeyValues(key, "hello");

		assertEquals("hello", props.get(key));
	}

	@Test
	public void testRegisterKeyRejectsDuplicateRegistration() {
		String key = "TEST_DUPLICATE_" + System.nanoTime();

		PropertyUtils.registerKey(key, String.class);

		assertThrows(IllegalStateException.class,
				() -> PropertyUtils.registerKey(key, String.class));
	}

	@Test
	public void testRegisterKeyOverwriteAllowsChangingType() {
		String key = "TEST_OVERWRITE_" + System.nanoTime();

		PropertyUtils.registerKey(key, String.class);
		PropertyUtils.registerKeyOverwrite(key, Integer.class);

		Properties props = PropertyUtils.fromKeyValues(key, 42);

		assertEquals(42, props.get(key));
	}

	@Test
	public void testRegisterKeyRejectsBadArguments() {
		assertThrows(IllegalArgumentException.class,
				() -> PropertyUtils.registerKey(null, String.class));

		assertThrows(IllegalArgumentException.class,
				() -> PropertyUtils.registerKey("   ", String.class));

		assertThrows(IllegalArgumentException.class,
				() -> PropertyUtils.registerKey("BAD_TYPE_" + System.nanoTime(), null));

		assertThrows(IllegalArgumentException.class,
				() -> PropertyUtils.registerKeyOverwrite(null, String.class));

		assertThrows(IllegalArgumentException.class,
				() -> PropertyUtils.registerKeyOverwrite("   ", String.class));

		assertThrows(IllegalArgumentException.class,
				() -> PropertyUtils.registerKeyOverwrite("BAD_TYPE_OVERWRITE_" + System.nanoTime(), null));
	}

	@Test
	public void testRepresentativeDefaultsForMissingValues() {
		Properties props = new Properties();

		assertTrue(PropertyUtils.getUseContainer(props));
		assertFalse(PropertyUtils.getConsoleLog(props));
		assertNull(PropertyUtils.getBackgroundImage(props));
		assertEquals(0, PropertyUtils.getLeft(props));
		assertEquals(0, PropertyUtils.getTop(props));
		assertTrue(PropertyUtils.getVisible(props));
		assertFalse(PropertyUtils.getScrollable(props));
		assertFalse(PropertyUtils.addWheelZoom(props));
		assertEquals(0L, PropertyUtils.getToolbarBits(props));
		assertFalse(PropertyUtils.getDraggable(props));
		assertFalse(PropertyUtils.getMaximize(props));
		assertFalse(PropertyUtils.getConnectable(props));
		assertFalse(PropertyUtils.getDoubleClickable(props));
		assertTrue(PropertyUtils.getDeletable(props));
		assertTrue(PropertyUtils.getResizable(props));
		assertTrue(PropertyUtils.getRightClickable(props));
		assertFalse(PropertyUtils.getRotatable(props));
		assertFalse(PropertyUtils.getStyleEditable(props));
		assertEquals(PropertyUtils.unknownString, PropertyUtils.getTitle(props));
		assertEquals(Double.NaN, PropertyUtils.getFraction(props));
		assertEquals(0.0, PropertyUtils.getAspectRatio(props), TOL);
		assertEquals(SymbolType.SQUARE, PropertyUtils.getSymbol(props));
		assertEquals(8, PropertyUtils.getSymbolSize(props));
		assertNull(PropertyUtils.getUserData(props));
		assertNull(PropertyUtils.getBackground(props));
		assertEquals(PropertyUtils.defaultTextColor, PropertyUtils.getTextColor(props));
		assertEquals(PropertyUtils.defaultFillColor, PropertyUtils.getFillColor(props));
		assertEquals(PropertyUtils.defaultLineColor, PropertyUtils.getLineColor(props));
		assertEquals(LineStyle.SOLID, PropertyUtils.getLineStyle(props));
		assertEquals(0.0f, PropertyUtils.getLineWidth(props), 1.0e-6f);
		assertEquals(Integer.MIN_VALUE, PropertyUtils.getWidth(props));
		assertEquals(Integer.MIN_VALUE, PropertyUtils.getHeight(props));
		assertTrue(PropertyUtils.getLocked(props));
		assertNull(PropertyUtils.getContainer(props));
		assertNull(PropertyUtils.getSplitWestComponent(props));
	}

	@Test
	public void testPrimitiveGettersAcceptStringsAndTypedValues() {
		Properties props = new Properties();

		props.put("INT_STRING", "67");
		props.put("INT_VALUE", 68);
		props.put("BAD_INT", "not an int");

		assertEquals(67, PropertyUtils.getInt(props, "INT_STRING", -1));
		assertEquals(68, PropertyUtils.getInt(props, "INT_VALUE", -1));
		assertEquals(-1, PropertyUtils.getInt(props, "BAD_INT", -1));
		assertEquals(-1, PropertyUtils.getInt(props, "MISSING_INT", -1));

		props.put("LONG_STRING", "123");
		props.put("LONG_VALUE", 456L);
		props.put("LONG_INT", 789);
		props.put("LONG_LARGE", "9223372036854775807");
		props.put("BAD_LONG", "not a long");

		assertEquals(123L, PropertyUtils.getLong(props, "LONG_STRING", -1L));
		assertEquals(456L, PropertyUtils.getLong(props, "LONG_VALUE", -1L));
		assertEquals(789L, PropertyUtils.getLong(props, "LONG_INT", -1L));
		assertEquals(Long.MAX_VALUE, PropertyUtils.getLong(props, "LONG_LARGE", -1L));
		assertEquals(-1L, PropertyUtils.getLong(props, "BAD_LONG", -1L));
		assertEquals(-1L, PropertyUtils.getLong(props, "MISSING_LONG", -1L));

		props.put("DOUBLE_STRING", "1.25");
		props.put("DOUBLE_VALUE", 2.5);
		props.put("DOUBLE_FLOAT", 3.5f);
		props.put("DOUBLE_INT", 4);
		props.put("BAD_DOUBLE", "not a double");

		assertEquals(1.25, PropertyUtils.getDouble(props, "DOUBLE_STRING", -1.0), TOL);
		assertEquals(2.5, PropertyUtils.getDouble(props, "DOUBLE_VALUE", -1.0), TOL);
		assertEquals(3.5, PropertyUtils.getDouble(props, "DOUBLE_FLOAT", -1.0), TOL);
		assertEquals(4.0, PropertyUtils.getDouble(props, "DOUBLE_INT", -1.0), TOL);
		assertEquals(-1.0, PropertyUtils.getDouble(props, "BAD_DOUBLE", -1.0), TOL);
		assertEquals(-1.0, PropertyUtils.getDouble(props, "MISSING_DOUBLE", -1.0), TOL);

		props.put("FLOAT_STRING", "1.25");
		props.put("FLOAT_VALUE", 2.5f);
		props.put("FLOAT_INT", 3);
		props.put("BAD_FLOAT", "not a float");

		assertEquals(1.25f, PropertyUtils.getFloat(props, "FLOAT_STRING", -1.0f), 1.0e-6f);
		assertEquals(2.5f, PropertyUtils.getFloat(props, "FLOAT_VALUE", -1.0f), 1.0e-6f);
		assertEquals(3.0f, PropertyUtils.getFloat(props, "FLOAT_INT", -1.0f), 1.0e-6f);
		assertEquals(-1.0f, PropertyUtils.getFloat(props, "BAD_FLOAT", -1.0f), 1.0e-6f);
		assertEquals(-1.0f, PropertyUtils.getFloat(props, "MISSING_FLOAT", -1.0f), 1.0e-6f);

		props.put("BOOLEAN_STRING_TRUE", "true");
		props.put("BOOLEAN_STRING_FALSE", "false");
		props.put("BOOLEAN_VALUE", true);
		props.put("BAD_BOOLEAN", 123);
		props.put("BAD_BOOLEAN_STRING", "yes");

		assertTrue(PropertyUtils.getBoolean(props, "BOOLEAN_STRING_TRUE", false));
		assertFalse(PropertyUtils.getBoolean(props, "BOOLEAN_STRING_FALSE", true));
		assertTrue(PropertyUtils.getBoolean(props, "BOOLEAN_VALUE", false));
		assertTrue(PropertyUtils.getBoolean(props, "BAD_BOOLEAN", true));
		assertTrue(PropertyUtils.getBoolean(props, "BAD_BOOLEAN_STRING", true));
		assertFalse(PropertyUtils.getBoolean(props, "MISSING_BOOLEAN", false));
	}

	@Test
	public void testMalformedWorldRectangleAndNullPropertiesUseDefaults() {
		Properties props = new Properties();
		props.put("WORLD", "not a rectangle");
		Rectangle2D.Double fallback = new Rectangle2D.Double(1, 2, 3, 4);
		assertSame(fallback, PropertyUtils.getWorldRectangle(props, "WORLD", fallback));
		assertSame(fallback, PropertyUtils.getWorldRectangle(null, "WORLD", fallback));
		assertEquals(17, PropertyUtils.getInt(null, "INT", 17));
		assertTrue(PropertyUtils.getBoolean(null, "BOOL", true));
	}

	@Test
	public void testStringGetter() {
		Properties props = new Properties();

		props.put("STRING_VALUE", "hello");
		props.put("NON_STRING_VALUE", 123);

		assertEquals("hello", PropertyUtils.getString(props, "STRING_VALUE", "default"));
		assertEquals("default", PropertyUtils.getString(props, "NON_STRING_VALUE", "default"));
		assertEquals("default", PropertyUtils.getString(props, "MISSING_STRING", "default"));
	}

	@Test
	public void testColorGetterAcceptsColorAndX11Name() {
		Properties props = new Properties();

		props.put("COLOR_VALUE", Color.RED);
		props.put("COLOR_NAME", "red");
		props.put("BAD_COLOR", 123);
		props.put("UNKNOWN_COLOR", "not an X11 color");

		assertEquals(Color.RED, PropertyUtils.getColor(props, "COLOR_VALUE", Color.BLACK));
		assertEquals(Color.RED, PropertyUtils.getColor(props, "COLOR_NAME", Color.BLACK));
		assertEquals(Color.BLACK, PropertyUtils.getColor(props, "BAD_COLOR", Color.BLACK));
		assertEquals(Color.BLACK, PropertyUtils.getColor(props, "UNKNOWN_COLOR", Color.BLACK));
		assertEquals(Color.BLACK, PropertyUtils.getColor(props, "MISSING_COLOR", Color.BLACK));
	}

	@Test
	public void testWorldRectangleGetter() {
		Properties props = new Properties();
		Rectangle2D.Double rect = new Rectangle2D.Double(1, 2, 3, 4);
		Rectangle2D.Double defaultRect = new Rectangle2D.Double(5, 6, 7, 8);

		props.put("RECT", rect);
		props.put("BAD_RECT", "not a rectangle");

		assertSame(rect, PropertyUtils.getWorldRectangle(props, "RECT", defaultRect));
		assertSame(defaultRect, PropertyUtils.getWorldRectangle(props, "BAD_RECT", defaultRect));
		assertSame(defaultRect, PropertyUtils.getWorldRectangle(props, "MISSING_RECT", defaultRect));
	}
}
