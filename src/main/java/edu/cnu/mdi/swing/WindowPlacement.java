package edu.cnu.mdi.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;

/**
 * Utility class for common window and component placement tasks in Swing
 * applications.
 *
 * <p>
 * This class centralizes logic for determining the "main" display screen,
 * centering windows, sizing components relative to the display, and retrieving
 * top-level parent containers. It also includes optional Mac-specific UI hints
 * (Harbor/JRS properties) for component styling.
 * </p>
 *
 * <p>
 * The class is {@code final} and has a private constructor because it is purely
 * a static utility.
 * </p>
 */
public final class WindowPlacement {

	/** Private constructor to prevent instantiation. */
	private WindowPlacement() {
	}

	/**
	 * Returns the bounds of the "main" screen.
	 *
	 * <p>
	 * The method tries the following logic in order:
	 * </p>
	 * <ol>
	 * <li>Return the bounds of the first screen whose {@code x} coordinate is
	 * {@code 0}. This is typically the primary monitor.</li>
	 * <li>If no such screen exists, return the bounds of the largest (by pixel
	 * area) display.</li>
	 * <li>If screen information is unavailable, fall back to the default Toolkit
	 * screen size.</li>
	 * </ol>
	 *
	 * @return the rectangle representing the main screen's bounds; never
	 *         {@code null}.
	 */
	public static Rectangle boundsOfMainScreen() {
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] allScreens = env.getScreenDevices();

		if (allScreens != null) {
			for (GraphicsDevice screen : allScreens) {
				GraphicsConfiguration gc = screen.getDefaultConfiguration();
				Rectangle b = gc.getBounds();
				if (b.x == 0) {
					return b;
				}
			}
		}

		GraphicsDevice bigScreen = null;
		int maxArea = -1;
		if (allScreens != null) {
			for (GraphicsDevice screen : allScreens) {
				GraphicsConfiguration gc = screen.getDefaultConfiguration();
				Rectangle b = gc.getBounds();
				int area = b.width * b.height;
				if (area > maxArea) {
					maxArea = area;
					bigScreen = screen;
				}
			}
		}

		if (bigScreen != null) {
			return bigScreen.getDefaultConfiguration().getBounds();
		}
		return new Rectangle(0, 0, Toolkit.getDefaultToolkit().getScreenSize().width,
				Toolkit.getDefaultToolkit().getScreenSize().height);
	}

	/**
	 * Centers the given component on the main screen.
	 *
	 * <p>
	 * This is equivalent to calling {@link #centerComponent(Component, int, int)}
	 * with zero offsets.
	 * </p>
	 *
	 * @param component the component to center; ignored if {@code null}.
	 */
	public static void centerComponent(Component component) {
		centerComponent(component, 0, 0);
	}

	/**
	 * Centers a component on the main screen, optionally adjusting the final
	 * position by horizontal and vertical offsets.
	 *
	 * <p>
	 * The method ensures that the component does not exceed the screen size,
	 * shrinking its calculated size if necessary, but it does not resize the
	 * component itself—only its placement rectangle.
	 * </p>
	 *
	 * @param component the component to place; ignored if {@code null}.
	 * @param dh        horizontal offset from true center (positive → right).
	 * @param dv        vertical offset from true center (positive → down).
	 */
	public static void centerComponent(Component component, int dh, int dv) {
		if (component == null) {
			return;
		}

		Rectangle bounds = boundsOfMainScreen();
		Dimension size = component.getSize();
		if (size.height > bounds.height) {
			size.height = bounds.height;
		}
		if (size.width > bounds.width) {
			size.width = bounds.width;
		}

		int x = bounds.x + ((bounds.width - size.width) / 2) + dh;
		int y = bounds.y + ((bounds.height - size.height) / 2) + dv;

		component.setLocation(x, y);
	}

	/**
	 * Returns a {@link Dimension} representing the given fraction of the total main
	 * screen size.
	 *
	 * <p>
	 * For example, {@code screenFraction(0.5)} returns a dimension equal to half
	 * the screen width and half the screen height.
	 * </p>
	 *
	 * @param fraction the fraction of the screen size (typically between 0 and 1).
	 * @return a dimension containing {@code fraction * screenWidth} and
	 *         {@code fraction * screenHeight}.
	 */
	public static Dimension screenFraction(double fraction) {
		Dimension d = getDisplaySize();
		d.width = (int) (fraction * d.width);
		d.height = (int) (fraction * d.height);
		return d;
	}

	/**
	 * Returns the width and height of the main display as a {@link Dimension}.
	 *
	 * @return the display dimensions; never {@code null}.
	 */
	public static Dimension getDisplaySize() {
		Rectangle bounds = boundsOfMainScreen();
		return new Dimension(bounds.width, bounds.height);
	}

	/**
	 * Sizes a {@link JFrame} to a fraction of the main screen and centers it.
	 *
	 * @param frame          the frame to size and center.
	 * @param fractionalSize the fraction (e.g., {@code 0.8} for 80% of screen
	 *                       size).
	 */
	public static void sizeToScreen(JFrame frame, double fractionalSize) {
		Dimension d = screenFraction(fractionalSize);
		frame.setSize(d);
		centerComponent(frame);
	}

	/**
	 * Returns the top-level Swing/AWT container associated with the given
	 * component.
	 *
	 * <p>
	 * The search walks up the parent hierarchy until it finds one of:
	 * </p>
	 * <ul>
	 * <li>{@link JInternalFrame}</li>
	 * <li>{@link JFrame}</li>
	 * <li>{@link JDialog}</li>
	 * <li>{@link Window}</li>
	 * </ul>
	 *
	 * <p>
	 * If no such ancestor exists, or if {@code component} is {@code null}, the
	 * method returns {@code null}.
	 * </p>
	 *
	 * @param component the component whose top-level container should be found.
	 * @return the top-level container, or {@code null} if none exists.
	 */
	public static Container getParentContainer(Component component) {
		if (component == null) {
			return null;
		}

		Container container = component.getParent();
		while (container != null) {
			if (container instanceof JInternalFrame || container instanceof JFrame || container instanceof JDialog
					|| container instanceof Window) {
				return container;
			}
			container = container.getParent();
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// Optional UI hints (mostly recognized on macOS with Aqua LAF)
	// -------------------------------------------------------------------------

	/**
	 * Applies the macOS "small" size variant to a Swing component.
	 *
	 * @param component the component to modify.
	 */
	public static void setSizeSmall(JComponent component) {
		component.putClientProperty("JComponent.sizeVariant", "small");
	}

	/**
	 * Applies the macOS "mini" size variant to a Swing component.
	 *
	 * @param component the component to modify.
	 */
	public static void setSizeMini(JComponent component) {
		component.putClientProperty("JComponent.sizeVariant", "mini");
	}

	/**
	 * Marks the given button as a Mac-style square button, if supported by the
	 * installed Look and Feel.
	 *
	 * @param button the button to modify.
	 */
	public static void setSquareButton(AbstractButton button) {
		button.putClientProperty("JButton.buttonType", "square");
	}

	/**
	 * Marks the given button as a Mac-style textured button.
	 *
	 * @param button the button to modify.
	 */
	public static void setTexturedButton(AbstractButton button) {
		button.putClientProperty("JButton.buttonType", "textured");
	}
}
