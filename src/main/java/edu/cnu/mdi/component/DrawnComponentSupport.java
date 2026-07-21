package edu.cnu.mdi.component;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Objects;

import javax.swing.AbstractButton;

/**
 * Shared implementation used by drawn push buttons and drawn toggle buttons.
 * <p>
 * This class exists because {@link DrawnButton} must extend {@code JButton} and
 * {@link DrawnToggleButton} must extend {@code JToggleButton}. Java cannot share a
 * superclass implementation in that situation, so the common behavior lives here.
 * </p>
 */
final class DrawnComponentSupport {

    private final AbstractButton button;
    private final boolean animated;
    private final Rectangle drawBounds = new Rectangle();

    private ButtonPainter painter;

    DrawnComponentSupport(AbstractButton button, boolean animated, ButtonPainter painter) {
        this.button = Objects.requireNonNull(button, "button");
        this.animated = animated;
        this.painter = painter;
    }

    boolean isAnimated() {
        return animated;
    }

    ButtonPainter getPainter() {
        return painter;
    }

    void setPainter(ButtonPainter painter) {
        this.painter = painter;
        button.revalidate();
        button.repaint();
    }

    Dimension getPreferredSize() {
        if (button.isPreferredSizeSet()) {
            return button.getPreferredSize();
        }

        Dimension size = (painter == null) ? ButtonPainter.DEFAULT_PREFERRED_SIZE : painter.getPreferredSize();
        return new Dimension(size);
    }

    void paint(Graphics g) {
        if (painter == null) {
            return;
        }

        Insets insets = button.getInsets();
        int width = Math.max(0, button.getWidth() - insets.left - insets.right);
        int height = Math.max(0, button.getHeight() - insets.top - insets.bottom);
        if ((width <= 0) || (height <= 0)) {
            return;
        }

        drawBounds.setBounds(insets.left, insets.top, width, height);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            long frame = animated ? DrawnComponentManager.getInstance().getFrameCount() : 0L;
            painter.draw(g2, button, drawBounds, frame);
        } finally {
            g2.dispose();
        }
    }

    void addNotify() {
        if (animated) {
            DrawnComponentManager.getInstance().register(button);
        }
    }

    void removeNotify() {
        if (animated) {
            DrawnComponentManager.getInstance().unregister(button);
        }
    }
}