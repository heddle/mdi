package edu.cnu.mdi.component;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.AbstractButton;

/**
 * Paints the vector contents of a {@link DrawnButton} or {@link DrawnToggleButton}.
 * <p>
 * A painter is deliberately independent of image files. It is given the target button,
 * the drawable bounds inside the button's insets, and an animation frame value. The
 * same painter instance may be shared by many buttons, so implementations should not
 * store per-button mutable drawing state unless they do so carefully.
 * </p>
 */
@FunctionalInterface
public interface ButtonPainter {

    /** The default drawing canvas used when no explicit size is supplied. */
    Dimension DEFAULT_PREFERRED_SIZE = new Dimension(48, 48);

    /**
     * Draws the button contents.
     *
     * @param g2 the graphics context; the caller owns and disposes this copy
     * @param button the button being painted
     * @param bounds the drawable area inside the button insets
     * @param frameCount the current animation frame, or {@code 0} for non-animated buttons
     */
    void draw(Graphics2D g2, AbstractButton button, Rectangle bounds, long frameCount);

    /**
     * Returns the natural canvas size for buttons using this painter.
     * <p>
     * Override this for non-square or detailed drawings. The returned dimension is copied
     * by the button, so callers may safely return a shared constant.
     * </p>
     *
     * @return the preferred drawing size
     */
    default Dimension getPreferredSize() {
        return DEFAULT_PREFERRED_SIZE;
    }
}