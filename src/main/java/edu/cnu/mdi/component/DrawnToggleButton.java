package edu.cnu.mdi.component;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;

/**
 * A {@link JToggleButton} whose visible content is drawn by a {@link ButtonPainter}.
 * <p>
 * The selected, rollover, pressed, and enabled states remain available through the
 * normal Swing button model. A painter can query those states from the supplied button
 * and render them however it chooses.
 * </p>
 */
@SuppressWarnings("serial")
public class DrawnToggleButton extends JToggleButton {

    private final DrawnComponentSupport support;

    /**
     * Creates a drawn toggle button.
     *
     * @param animated {@code true} to repaint this button on the shared animation timer
     * @param painter the painter used to draw the button contents; may be {@code null}
     */
    public DrawnToggleButton(boolean animated, ButtonPainter painter) {
        super();
        support = new DrawnComponentSupport(this, animated, painter);
        configureForCustomPainting();
    }
    
    /**
	 * Creates a drawn toggle button that is not animated.
	 *
	 * @param painter the painter used to draw the button contents; may be {@code null}
	 */
    public DrawnToggleButton(ButtonPainter painter) {
        this(false, painter);
    }

    /**
     * Returns the current button painter.
     *
     * @return the current painter, or {@code null}
     */
    public ButtonPainter getPainter() {
        return support.getPainter();
    }

    /**
     * Sets the painter used to draw this button.
     *
     * @param painter the new painter, or {@code null} for no custom drawing
     */
    public void setPainter(ButtonPainter painter) {
        support.setPainter(painter);
    }

    /**
     * Returns whether this button uses the shared animation timer.
     *
     * @return {@code true} if animated
     */
    public boolean isAnimated() {
        return support.isAnimated();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        support.addNotify();
    }

    @Override
    public void removeNotify() {
        support.removeNotify();
        super.removeNotify();
    }

    @Override
    public Dimension getPreferredSize() {
        return support.getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        support.paint(g);
    }

	// Configure the button for custom painting by disabling default 
	// painting and focus behavior
	private void configureForCustomPainting() {
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setFocusable(false);
		setOpaque(false);

		setBorder(BorderFactory.createEmptyBorder());
		setMargin(new Insets(0, 0, 0, 0));
	}
}