package edu.cnu.mdi.hover;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Point;
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

 
    /**
     * Show a message near the given screen coordinate.
     *
     * @param message the message text
     * @param screenPoint a point in SCREEN coordinates
     */
    public void showMessage(String message, Point screenPoint) {
        label.setText(message == null ? "" : message);
        pack();

        // Offset slightly so we don't sit exactly under the cursor.
        int x = screenPoint.x + 12;
        int y = screenPoint.y + 18;

        // Optional: keep on-screen (simple clamp).
        Insets insets = getToolkit().getScreenInsets(getGraphicsConfiguration());
        int minX = insets.left;
        int minY = insets.top;
        int maxX = getGraphicsConfiguration().getBounds().x
                + getGraphicsConfiguration().getBounds().width - insets.right - getWidth();
        int maxY = getGraphicsConfiguration().getBounds().y
                + getGraphicsConfiguration().getBounds().height - insets.bottom - getHeight();

        if (x < minX) x = minX;
        if (y < minY) y = minY;
        if (x > maxX) x = maxX;
        if (y > maxY) y = maxY;

        setLocation(x, y);
        setVisible(true);
    }

    public void hideMessage() {
        setVisible(false);
    }
}