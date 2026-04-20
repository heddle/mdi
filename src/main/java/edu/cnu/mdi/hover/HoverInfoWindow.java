package edu.cnu.mdi.hover;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JWindow;

import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Simple hover popup window for displaying hover text near the cursor.
 */
@SuppressWarnings("serial")
public class HoverInfoWindow extends JWindow {

    private final JLabel label;

    public HoverInfoWindow(Window owner) {
        super(owner);

        setBackground(new Color(0, 0, 0, 0));  // IMPORTANT

        label = new JLabel();
        label.setFont(Fonts.plainFontDelta(-2));
        label.setOpaque(false);
        label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        label.setForeground(Color.white);

        JComponent content = (JComponent) getContentPane();
        content.setLayout(new BorderLayout());
        content.add(label, BorderLayout.CENTER);

        content.setBackground(new Color(0, 0, 0, 128));  // translucent black
        content.setOpaque(true);

        content.setBorder(BorderFactory.createLineBorder(
                new Color(0, 0, 0, 200)
        ));

        setAlwaysOnTop(true);
        setFocusableWindowState(false);    }

 
    public void showMessage(String message, Point screenPoint) {
        label.setText(message == null ? "" : message);
        pack();

        int x = screenPoint.x + 12;
        int y = screenPoint.y + 18;

        // Find the GraphicsConfiguration for whichever monitor contains
        // the target point — this is correct even after the window has been
        // dragged to a different screen.
        GraphicsConfiguration gc = getGraphicsConfigurationFor(screenPoint);

        Insets insets = getToolkit().getScreenInsets(gc);
        Rectangle bounds = gc.getBounds();

        int minX = bounds.x + insets.left;
        int minY = bounds.y + insets.top;
        int maxX = bounds.x + bounds.width  - insets.right  - getWidth();
        int maxY = bounds.y + bounds.height - insets.bottom - getHeight();

        if (x < minX) x = minX;
        if (y < minY) y = minY;
        if (x > maxX) x = maxX;
        if (y > maxY) y = maxY;

        setLocation(x, y);
        setVisible(true);
    }

    /**
     * Returns the {@link GraphicsConfiguration} for the screen device that
     * contains (or is nearest to) the given point in screen coordinates.
     *
     * <p>This is the correct way to determine which monitor a popup should
     * appear on after the owning window has been dragged to a secondary
     * display. Asking {@code this.getGraphicsConfiguration()} is wrong
     * because that returns the configuration of the device the popup window
     * itself was created on, which does not update when the owner moves.</p>
     */
    private static GraphicsConfiguration getGraphicsConfigurationFor(Point screenPoint) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice device : ge.getScreenDevices()) {
            GraphicsConfiguration gc = device.getDefaultConfiguration();
            if (gc.getBounds().contains(screenPoint)) {
                return gc;
            }
        }
        // Point is not within any screen (e.g. between monitors in a gap).
        // Fall back to the default screen.
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
    }
    /**
	 * Hide the hover message.
	 */
    public void hideMessage() {
        setVisible(false);
    }
}