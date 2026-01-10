package edu.cnu.mdi.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Utility methods for creating offscreen images of Swing/AWT components.
 *
 * <p>
 * These helpers simplify the process of generating snapshots of components for
 * thumbnails, drag images, buffered painting, effects, and testing. Unlike
 * {@link Component#createImage(int, int)}, these methods return a
 * {@link BufferedImage} that can be manipulated, saved, or inspected.
 * </p>
 *
 * <p>
 * All methods are null-safe and return {@code null} if the given component is
 * {@code null} or has an invalid size.
 * </p>
 *
 * <p>
 * <strong>Important:</strong> The component must have a meaningful size
 * (usually after layout or manual sizing) before rendering can occur.
 * </p>
 */
public final class SwingGraphicsUtils {

	/** Prevent instantiation (static utility class). */
	private SwingGraphicsUtils() {
	}

	// -------------------------------------------------------------------------
	// Buffer creation: opaque (RGB)
	// -------------------------------------------------------------------------

	/**
	 * Creates an offscreen opaque RGB buffer sized to match the component.
	 *
	 * <p>
	 * This method does not paint anything; it only allocates a
	 * {@link BufferedImage} of type {@link BufferedImage#TYPE_INT_RGB}. Use
	 * {@link #paintComponentOnImage(Component, BufferedImage)} to draw the
	 * component.
	 * </p>
	 *
	 * @param c the component whose size determines the buffer dimensions
	 * @return a new opaque image buffer, or {@code null} if the component is
	 *         {@code null} or has zero/negative width or height
	 */
	public static BufferedImage createComponentImageBuffer(Component c) {
		if (c == null) {
			return null;
		}
		Dimension size = c.getSize();
		if (size.width < 1 || size.height < 1) {
			return null;
		}
		return new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
	}

	// -------------------------------------------------------------------------
	// Buffer creation: translucent (ARGB)
	// -------------------------------------------------------------------------

	/**
	 * Creates a translucent offscreen buffer sized to match the component.
	 *
	 * <p>
	 * This uses {@link BufferedImage#TYPE_INT_ARGB}, which preserves alpha and
	 * allows semi-transparent rendering. Useful for drag images, effects, or
	 * components with non-opaque backgrounds.
	 * </p>
	 *
	 * @param c the component whose size determines the buffer dimensions
	 * @return a new ARGB image, or {@code null} if the component is invalid
	 */
	public static BufferedImage createComponentTranslucentImageBuffer(Component c) {
		if (c == null) {
			return null;
		}
		Dimension size = c.getSize();
		if (size.width < 1 || size.height < 1) {
			return null;
		}
		return new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
	}

	// -------------------------------------------------------------------------
	// Painting onto an existing buffer
	// -------------------------------------------------------------------------

	/**
	 * Paints a component into an existing {@link BufferedImage}.
	 *
	 * <p>
	 * This uses {@link Component#paint(java.awt.Graphics)} which performs the
	 * component's full paint cycle (background, border, children, etc.). The
	 * component does not need to be visible on screen.
	 * </p>
	 *
	 * @param c     the component to render
	 * @param image the target image buffer
	 */
	public static void paintComponentOnImage(Component c, BufferedImage image) {
		if (c == null || image == null) {
			return;
		}

		Dimension size = c.getSize();
		if (size.width < 1 || size.height < 1) {
			return;
		}

		Graphics2D g2 = image.createGraphics();
		try {
			c.paint(g2);
		} finally {
			g2.dispose();
		}
	}

	// -------------------------------------------------------------------------
	// Convenience wrappers
	// -------------------------------------------------------------------------

	/**
	 * Convenience method that creates an opaque buffer and immediately paints the
	 * component into it.
	 *
	 * <p>
	 * Useful for quickly generating snapshots for saving or testing.
	 * </p>
	 *
	 * @param c the component to render
	 * @return a fully painted opaque image, or {@code null} on error
	 */
	public static BufferedImage getComponentImage(Component c) {
		BufferedImage img = createComponentImageBuffer(c);
		paintComponentOnImage(c, img);
		return img;
	}

	/**
	 * Convenience method that creates a translucent ARGB buffer and immediately
	 * paints the component into it.
	 *
	 * <p>
	 * This preserves transparency and is appropriate for overlays, effects, or
	 * drag-and-drop representations.
	 * </p>
	 *
	 * @param c the component to render
	 * @return a fully painted ARGB image, or {@code null} if invalid
	 */
	public static BufferedImage getComponentTranslucentImage(Component c) {
		BufferedImage img = createComponentTranslucentImageBuffer(c);
		paintComponentOnImage(c, img);
		return img;
	}
}
