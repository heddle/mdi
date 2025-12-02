package edu.cnu.mdi.swing.view;

import edu.cnu.mdi.util.PropertySupport;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ViewPropertySupport}.
 *
 * <p>These tests verify that view-related properties are correctly interpreted
 * from a {@link Properties} instance, including booleans, numeric layout
 * parameters, world-coordinate rectangles, colors, fonts, Swing components,
 * and arbitrary user data.</p>
 */
public class ViewPropertySupportTest {

    /**
     * Creates a fresh {@link Properties} instance with no predefined values.
     *
     * @return empty properties object
     */
    private Properties emptyProps() {
        return new Properties();
    }

    // -------------------------------------------------------------------------
    // String and basic boolean/int/double properties
    // -------------------------------------------------------------------------

    /**
     * Verifies that title and property name fall back to {@link ViewPropertySupport#UNKNOWN_STRING}
     * when not provided, and that property values are respected when set.
     */
    @Test
    void titleAndPropName_useDefaultsWhenMissingAndValuesWhenPresent() {
        Properties props = emptyProps();

        // Defaults
        assertEquals(ViewPropertySupport.UNKNOWN_STRING,
                ViewPropertySupport.getTitle(props));
        assertEquals(ViewPropertySupport.UNKNOWN_STRING,
                ViewPropertySupport.getPropName(props));

        // Set explicit values
        props.setProperty(ViewPropertyKeys.TITLE, "My View");
        props.setProperty(ViewPropertyKeys.PROP_NAME, "myViewInternal");

        assertEquals("My View", ViewPropertySupport.getTitle(props));
        assertEquals("myViewInternal", ViewPropertySupport.getPropName(props));
    }

    /**
     * Verifies default boolean behavior and that explicit values override defaults.
     */
    @Test
    void booleanFlags_defaultValuesAndOverrides() {
        Properties props = emptyProps();

        // Defaults
        assertTrue(ViewPropertySupport.isVisible(props));
        assertFalse(ViewPropertySupport.isScrollable(props));
        assertTrue(ViewPropertySupport.useToolbar(props));
        assertTrue(ViewPropertySupport.useStandardViewDecorations(props));
        assertTrue(ViewPropertySupport.isClosable(props));
        assertTrue(ViewPropertySupport.isIconifiable(props));
        assertTrue(ViewPropertySupport.isResizable(props));
        assertFalse(ViewPropertySupport.isDraggable(props));
        assertFalse(ViewPropertySupport.isMaximize(props));
        assertTrue(ViewPropertySupport.isMaximizable(props));
        assertFalse(ViewPropertySupport.isRotatable(props));
        assertTrue(ViewPropertySupport.isLocked(props));

        // Overrides
        props.setProperty(ViewPropertyKeys.VISIBLE, "false");
        props.setProperty(ViewPropertyKeys.SCROLLABLE, "true");
        props.setProperty(ViewPropertyKeys.TOOLBAR, "false");
        props.setProperty(ViewPropertyKeys.STANDARD_VIEW_DECOR, "false");
        props.setProperty(ViewPropertyKeys.CLOSABLE, "false");
        props.setProperty(ViewPropertyKeys.ICONIFIABLE, "false");
        props.setProperty(ViewPropertyKeys.RESIZABLE, "false");
        props.setProperty(ViewPropertyKeys.DRAGGABLE, "true");
        props.setProperty(ViewPropertyKeys.MAXIMIZE, "true");
        props.setProperty(ViewPropertyKeys.MAXIMIZABLE, "false");
        props.setProperty(ViewPropertyKeys.ROTATABLE, "true");
        props.setProperty(ViewPropertyKeys.LOCKED, "false");

        assertFalse(ViewPropertySupport.isVisible(props));
        assertTrue(ViewPropertySupport.isScrollable(props));
        assertFalse(ViewPropertySupport.useToolbar(props));
        assertFalse(ViewPropertySupport.useStandardViewDecorations(props));
        assertFalse(ViewPropertySupport.isClosable(props));
        assertFalse(ViewPropertySupport.isIconifiable(props));
        assertFalse(ViewPropertySupport.isResizable(props));
        assertTrue(ViewPropertySupport.isDraggable(props));
        assertTrue(ViewPropertySupport.isMaximize(props));
        assertFalse(ViewPropertySupport.isMaximizable(props));
        assertTrue(ViewPropertySupport.isRotatable(props));
        assertFalse(ViewPropertySupport.isLocked(props));
    }

    /**
     * Verifies integer layout parameters (left, top, width, height, margins)
     * use documented defaults and respect explicit settings.
     */
    @Test
    void integerLayoutProperties_defaultsAndOverrides() {
        Properties props = emptyProps();

        // Defaults
        assertEquals(0, ViewPropertySupport.getLeft(props));
        assertEquals(0, ViewPropertySupport.getTop(props));
        assertEquals(Integer.MIN_VALUE, ViewPropertySupport.getWidth(props));
        assertEquals(Integer.MIN_VALUE, ViewPropertySupport.getHeight(props));
        assertEquals(0, ViewPropertySupport.getTopMargin(props));
        assertEquals(0, ViewPropertySupport.getBottomMargin(props));
        assertEquals(0, ViewPropertySupport.getLeftMargin(props));
        assertEquals(0, ViewPropertySupport.getRightMargin(props));
        assertEquals(Double.NaN, ViewPropertySupport.getFraction(props), 0.0);

        // Overrides
        props.setProperty(ViewPropertyKeys.LEFT, "10");
        props.setProperty(ViewPropertyKeys.TOP, "20");
        props.setProperty(ViewPropertyKeys.WIDTH, "800");
        props.setProperty(ViewPropertyKeys.HEIGHT, "600");
        props.setProperty(ViewPropertyKeys.TOP_MARGIN, "1");
        props.setProperty(ViewPropertyKeys.BOTTOM_MARGIN, "2");
        props.setProperty(ViewPropertyKeys.LEFT_MARGIN, "3");
        props.setProperty(ViewPropertyKeys.RIGHT_MARGIN, "4");
        props.setProperty(ViewPropertyKeys.FRACTION, "0.75");

        assertEquals(10, ViewPropertySupport.getLeft(props));
        assertEquals(20, ViewPropertySupport.getTop(props));
        assertEquals(800, ViewPropertySupport.getWidth(props));
        assertEquals(600, ViewPropertySupport.getHeight(props));
        assertEquals(1, ViewPropertySupport.getTopMargin(props));
        assertEquals(2, ViewPropertySupport.getBottomMargin(props));
        assertEquals(3, ViewPropertySupport.getLeftMargin(props));
        assertEquals(4, ViewPropertySupport.getRightMargin(props));
        assertEquals(0.75, ViewPropertySupport.getFraction(props), 1e-9);
    }

    // -------------------------------------------------------------------------
    // World / geometry properties
    // -------------------------------------------------------------------------

    /**
     * Verifies that the world system falls back to {@link ViewPropertySupport#DEFAULT_WORLD_RECT}
     * if no rectangle is defined.
     */
    @Test
    void getWorldSystem_usesDefaultWhenMissing() {
        Properties props = emptyProps();
        Rectangle2D.Double world = ViewPropertySupport.getWorldSystem(props);

        assertNotNull(world);
        assertSame(ViewPropertySupport.DEFAULT_WORLD_RECT, world,
                "World system should default to DEFAULT_WORLD_RECT");
    }

    /**
     * Verifies that a {@link Rectangle2D.Double} stored directly in properties
     * is returned by {@link ViewPropertySupport#getWorldRectangle}.
     */
    @Test
    void getWorldRectangle_returnsStoredRectangleWhenPresent() {
        Properties props = emptyProps();

        Rectangle2D.Double rect = new Rectangle2D.Double(1.0, 2.0, 3.0, 4.0);
        props.put(ViewPropertyKeys.WORLDSYSTEM, rect);

        Rectangle2D.Double result = ViewPropertySupport.getWorldSystem(props);

        assertSame(rect, result, "World system should return stored rectangle instance");
        assertEquals(1.0, result.x, 1e-9);
        assertEquals(2.0, result.y, 1e-9);
        assertEquals(3.0, result.width, 1e-9);
        assertEquals(4.0, result.height, 1e-9);
    }

    // -------------------------------------------------------------------------
    // Color properties and parsing
    // -------------------------------------------------------------------------

    /**
     * Verifies that colors stored directly as {@link Color} instances are returned unchanged,
     * and that missing values fall back to defaults.
     */
    @Test
    void getColor_directColorAndDefaults() {
        Properties props = emptyProps();

        // No colors set: expect defaults
        assertNull(ViewPropertySupport.getBackgroundColor(props));
        assertEquals(ViewPropertySupport.DEFAULT_TEXT_COLOR,
                ViewPropertySupport.getTextColor(props));
        assertEquals(ViewPropertySupport.DEFAULT_FILL_COLOR,
                ViewPropertySupport.getFillColor(props));
        assertEquals(ViewPropertySupport.DEFAULT_LINE_COLOR,
                ViewPropertySupport.getLineColor(props));

        // Direct Color instances
        Color bg = new Color(10, 20, 30);
        Color text = new Color(40, 50, 60);
        Color fill = new Color(70, 80, 90);
        Color line = new Color(100, 110, 120);

        props.put(ViewPropertyKeys.BACKGROUND, bg);
        props.put(ViewPropertyKeys.TEXT_COLOR, text);
        props.put(ViewPropertyKeys.FILL_COLOR, fill);
        props.put(ViewPropertyKeys.LINE_COLOR, line);

        assertEquals(bg, ViewPropertySupport.getBackgroundColor(props));
        assertEquals(text, ViewPropertySupport.getTextColor(props));
        assertEquals(fill, ViewPropertySupport.getFillColor(props));
        assertEquals(line, ViewPropertySupport.getLineColor(props));
    }

    /**
     * Verifies that hex color strings in the form "#RRGGBB" are parsed correctly.
     */
    @Test
    void getColor_parsesHexRrgBbb() {
        Properties props = emptyProps();

        props.setProperty(ViewPropertyKeys.TEXT_COLOR, "#00FF00");
        Color c = ViewPropertySupport.getTextColor(props);

        assertNotNull(c);
        assertEquals(0, c.getRed());
        assertEquals(255, c.getGreen());
        assertEquals(0, c.getBlue());
    }

    /**
     * Verifies that decimal triplets "r,g,b" are parsed as colors.
     */
    @Test
    void getColor_parsesDecimalTriplet() {
        Properties props = emptyProps();

        props.setProperty(ViewPropertyKeys.FILL_COLOR, "10, 20, 30");
        Color c = ViewPropertySupport.getFillColor(props);

        assertNotNull(c);
        assertEquals(10, c.getRed());
        assertEquals(20, c.getGreen());
        assertEquals(30, c.getBlue());
    }

    /**
     * Verifies that invalid color strings cause the default to be returned.
     */
    @Test
    void getColor_invalidStringFallsBackToDefault() {
        Properties props = emptyProps();

        // Set invalid value for text color; expect default to be used.
        props.setProperty(ViewPropertyKeys.TEXT_COLOR, "not_a_color");

        Color text = ViewPropertySupport.getTextColor(props);
        assertEquals(ViewPropertySupport.DEFAULT_TEXT_COLOR, text);

        // Invalid triplet
        props.setProperty(ViewPropertyKeys.LINE_COLOR, "1,2");
        Color line = ViewPropertySupport.getLineColor(props);
        assertEquals(ViewPropertySupport.DEFAULT_LINE_COLOR, line);
    }

    /**
     * Verifies that background image path is returned as a string, or null if absent.
     */
    @Test
    void getBackgroundImagePath_returnsStringOrNull() {
        Properties props = emptyProps();

        assertNull(ViewPropertySupport.getBackgroundImagePath(props));

        props.setProperty(ViewPropertyKeys.BACKGROUND_IMAGE, "/path/to/image.png");
        assertEquals("/path/to/image.png",
                ViewPropertySupport.getBackgroundImagePath(props));
    }

    // -------------------------------------------------------------------------
    // Font and Swing component properties
    // -------------------------------------------------------------------------

    /**
     * Verifies that the default font is returned when none is set, and that
     * an explicit {@link Font} instance overrides the default.
     */
    @Test
    void getFont_defaultAndStoredFont() {
        Properties props = emptyProps();

        // Default
        Font fDefault = ViewPropertySupport.getFont(props);
        assertNotNull(fDefault);
        assertEquals(ViewPropertySupport.DEFAULT_FONT, fDefault);

        // Override
        Font custom = new Font("Monospaced", Font.BOLD, 14);
        props.put(ViewPropertyKeys.FONT, custom);

        Font fCustom = ViewPropertySupport.getFont(props);
        assertSame(custom, fCustom, "Should return stored Font instance");
    }

    /**
     * Verifies that a {@link JComponent} stored under split-west key is returned,
     * and that {@code null} is returned when no component is present.
     */
    @Test
    void getSplitWestComponent_andGetJComponent_behavior() {
        Properties props = emptyProps();

        assertNull(ViewPropertySupport.getSplitWestComponent(props));

        JComponent panel = new JPanel();
        props.put(ViewPropertyKeys.SPLIT_WEST_COMPONENT, panel);

        assertSame(panel, ViewPropertySupport.getSplitWestComponent(props),
                "Should retrieve stored JComponent instance");

        // Generic accessor test
        assertSame(panel, ViewPropertySupport.getJComponent(props, ViewPropertyKeys.SPLIT_WEST_COMPONENT));
        assertNull(ViewPropertySupport.getJComponent(props, "nonexistentKey"));
    }

    // -------------------------------------------------------------------------
    // User data
    // -------------------------------------------------------------------------

    /**
     * Verifies that arbitrary user data stored under {@link ViewPropertyKeys#USER_DATA}
     * can be retrieved as an {@link Object}.
     */
    @Test
    void getUserData_returnsStoredObjectOrNull() {
        Properties props = emptyProps();

        assertNull(ViewPropertySupport.getUserData(props),
                "No user data should yield null");

        Object userData = new Object();
        props.put(ViewPropertyKeys.USER_DATA, userData);

        assertSame(userData, ViewPropertySupport.getUserData(props),
                "Should return the exact stored user-data object");
    }
}

