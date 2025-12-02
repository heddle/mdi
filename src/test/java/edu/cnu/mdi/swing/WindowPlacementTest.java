package edu.cnu.mdi.swing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WindowPlacement}.
 *
 * <p>
 * These tests focus on verifying the logical behavior of screen-bound
 * calculations, component centering, sizing relative to the display, parent
 * container lookup, and the application of macOS-oriented UI hints via
 * client properties.
 * </p>
 *
 * <p>
 * Tests that require an actual top-level {@link JFrame} are guarded by a
 * headless-environment check using {@link GraphicsEnvironment#isHeadless()}.
 * This allows the suite to run in CI environments that do not have a display.
 * </p>
 */
public class WindowPlacementTest {

    /**
     * Verifies that {@link WindowPlacement#boundsOfMainScreen()} returns a
     * non-null rectangle with positive width and height.
     */
    @Test
    void boundsOfMainScreen_returnsNonNullPositiveBounds() {
        Rectangle bounds = WindowPlacement.boundsOfMainScreen();

        assertNotNull(bounds, "Bounds must not be null");
        assertTrue(bounds.width > 0, "Screen width should be positive");
        assertTrue(bounds.height > 0, "Screen height should be positive");
    }

    /**
     * Verifies that {@link WindowPlacement#getDisplaySize()} corresponds to
     * the width and height of {@link WindowPlacement#boundsOfMainScreen()}.
     */
    @Test
    void getDisplaySize_matchesBoundsOfMainScreen() {
        Rectangle bounds = WindowPlacement.boundsOfMainScreen();
        Dimension displaySize = WindowPlacement.getDisplaySize();

        assertEquals(bounds.width, displaySize.width, "Display width should match bounds");
        assertEquals(bounds.height, displaySize.height, "Display height should match bounds");
    }

    /**
     * Verifies that {@link WindowPlacement#screenFraction(double)} returns a
     * {@link Dimension} that is a scaled version of the display size.
     */
    @Test
    void screenFraction_returnsScaledDisplayDimension() {
        Dimension full = WindowPlacement.getDisplaySize();
        double fraction = 0.5;

        Dimension half = WindowPlacement.screenFraction(fraction);

        assertEquals((int) (full.width * fraction), half.width, "Width should be scaled by fraction");
        assertEquals((int) (full.height * fraction), half.height, "Height should be scaled by fraction");
    }

    /**
     * Verifies that {@link WindowPlacement#centerComponent(Component, int, int)}
     * places a component at the expected center coordinates of the main screen
     * when no offsets are applied.
     *
     * <p>
     * This test does not depend on absolute screen size, only that the method
     * follows its own centering math.
     * </p>
     */
    @Test
    void centerComponent_placesComponentAtExpectedCenter() {
        Rectangle bounds = WindowPlacement.boundsOfMainScreen();

        JPanel panel = new JPanel();
        // Give the component a known size smaller than the screen
        Dimension size = new Dimension(bounds.width / 2, bounds.height / 2);
        panel.setSize(size);

        // Center with no offsets
        WindowPlacement.centerComponent(panel, 0, 0);

        int expectedX = bounds.x + (bounds.width - size.width) / 2;
        int expectedY = bounds.y + (bounds.height - size.height) / 2;

        assertEquals(expectedX, panel.getX(), "Component X coordinate should match computed center");
        assertEquals(expectedY, panel.getY(), "Component Y coordinate should match computed center");
    }

    /**
     * Verifies that offsets in {@link WindowPlacement#centerComponent(Component, int, int)}
     * are applied correctly.
     */
    @Test
    void centerComponent_appliesOffsetsCorrectly() {
        Rectangle bounds = WindowPlacement.boundsOfMainScreen();

        JPanel panel = new JPanel();
        Dimension size = new Dimension(bounds.width / 4, bounds.height / 4);
        panel.setSize(size);

        int dh = 10;
        int dv = -20;

        WindowPlacement.centerComponent(panel, dh, dv);

        int expectedX = bounds.x + (bounds.width - size.width) / 2 + dh;
        int expectedY = bounds.y + (bounds.height - size.height) / 2 + dv;

        assertEquals(expectedX, panel.getX(), "Component X coordinate should include horizontal offset");
        assertEquals(expectedY, panel.getY(), "Component Y coordinate should include vertical offset");
    }

    /**
     * Verifies that {@link WindowPlacement#getParentContainer(Component)} walks
     * up the component hierarchy and returns the appropriate top-level container,
     * here a {@link JInternalFrame}.
     */
    @Test
    void getParentContainer_returnsTopLevelInternalFrame() {
        JInternalFrame internalFrame = new JInternalFrame();
        JPanel innerPanel = new JPanel();
        JPanel child = new JPanel();

        // Build a small hierarchy: internalFrame -> innerPanel -> child
        internalFrame.setContentPane(innerPanel);
        innerPanel.add(child);

        Container parent = WindowPlacement.getParentContainer(child);

        assertSame(internalFrame, parent, "Top-level container should be the JInternalFrame");
    }

    /**
     * Verifies that {@link WindowPlacement#getParentContainer(Component)} returns
     * {@code null} when there is no parent chain that matches the accepted types
     * or when the component itself is {@code null}.
     */
    @Test
    void getParentContainer_returnsNullWhenNoTopLevelOrNullComponent() {
        assertNull(WindowPlacement.getParentContainer(null), "Null component should return null");

        JPanel lonePanel = new JPanel();
        assertNull(WindowPlacement.getParentContainer(lonePanel),
                "Panel with no ancestors should return null");
    }

    /**
     * Tests {@link WindowPlacement#sizeToScreen(JFrame, double)} which both sizes
     * and centers a frame relative to the main display.
     *
     * <p>
     * This test is skipped in headless environments where creating frames is not
     * supported.
     * </p>
     */
    @Test
    void sizeToScreen_sizesAndCentersFrame() {
        // Skip this test entirely if running headless
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires non-headless environment");

        JFrame frame = new JFrame();
        double fraction = 0.6;

        WindowPlacement.sizeToScreen(frame, fraction);

        Dimension expectedSize = WindowPlacement.screenFraction(fraction);
        assertEquals(expectedSize.width, frame.getWidth(), "Frame width should match fractional screen width");
        assertEquals(expectedSize.height, frame.getHeight(), "Frame height should match fractional screen height");

        // Check centered location
        Rectangle bounds = WindowPlacement.boundsOfMainScreen();
        int expectedX = bounds.x + (bounds.width - expectedSize.width) / 2;
        int expectedY = bounds.y + (bounds.height - expectedSize.height) / 2;

        assertEquals(expectedX, frame.getX(), "Frame X coordinate should match centered position");
        assertEquals(expectedY, frame.getY(), "Frame Y coordinate should match centered position");

        frame.dispose();
    }

    /**
     * Verifies that {@link WindowPlacement#setSizeSmall(JComponent)} sets the
     * "sizeVariant" client property to "small".
     */
    @Test
    void setSizeSmall_setsClientPropertyOnComponent() {
        JComponent comp = new JPanel();

        WindowPlacement.setSizeSmall(comp);

        Object value = comp.getClientProperty("JComponent.sizeVariant");
        assertEquals("small", value, "sizeVariant should be set to 'small'");
    }

    /**
     * Verifies that {@link WindowPlacement#setSizeMini(JComponent)} sets the
     * "sizeVariant" client property to "mini".
     */
    @Test
    void setSizeMini_setsClientPropertyOnComponent() {
        JComponent comp = new JPanel();

        WindowPlacement.setSizeMini(comp);

        Object value = comp.getClientProperty("JComponent.sizeVariant");
        assertEquals("mini", value, "sizeVariant should be set to 'mini'");
    }

    /**
     * Verifies that {@link WindowPlacement#setSquareButton(javax.swing.AbstractButton)}
     * sets the appropriate client property to mark the button as "square".
     */
    @Test
    void setSquareButton_setsSquareClientProperty() {
        AbstractButton button = new JButton();

        WindowPlacement.setSquareButton(button);

        Object value = button.getClientProperty("JButton.buttonType");
        assertEquals("square", value, "buttonType should be set to 'square'");
    }

    /**
     * Verifies that {@link WindowPlacement#setTexturedButton(javax.swing.AbstractButton)}
     * sets the appropriate client property to mark the button as "textured".
     */
    @Test
    void setTexturedButton_setsTexturedClientProperty() {
        AbstractButton button = new JButton();

        WindowPlacement.setTexturedButton(button);

        Object value = button.getClientProperty("JButton.buttonType");
        assertEquals("textured", value, "buttonType should be set to 'textured'");
    }
}
