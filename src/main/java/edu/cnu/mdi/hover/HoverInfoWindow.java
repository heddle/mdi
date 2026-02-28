package edu.cnu.mdi.hover;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JWindow;

import edu.cnu.mdi.ui.fonts.Fonts;

@SuppressWarnings("serial")
public class HoverInfoWindow extends JWindow {
    private final JLabel label;

    /**
	 * Create a hover info window. The owner parameter is used to ensure the window
	 * is associated with the correct parent frame, which can help with focus and
	 * z-ordering. The window is set up to be transparent and always on top, making
	 * it suitable for displaying hover information without interfering with user
	 * interactions.
	 *
	 * @param owner the component that owns this hover info window, typically the main application frame
	 */
    public HoverInfoWindow(Component owner) {
        super(javax.swing.FocusManager.getCurrentManager().getActiveWindow());
        
        // Setup transparency (Requires the OS/Windowing system to support it)
        setBackground(new Color(0, 0, 0, 128)); // Black with opacity
        
        label = new JLabel();
        label.setForeground(Color.WHITE);
        label.setFont(Fonts.plainFontDelta(0));
        label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        
        add(label);
        setAlwaysOnTop(true);
    }

    /**
	 * Show the hover info window with the specified text at the given screen position.
	 * The window will be positioned slightly offset from the cursor to avoid
	 * obscuring the hovered component. The pack() method is called to ensure the
	 * window resizes to fit the text content, and setVisible(true) is called to
	 * display the window.
	 */
    public void showMessage(String text, Point screenPos) {
        label.setText(text);
        pack(); // Resize to fit text
        
        // Offset slightly so it's not directly under the cursor
        setLocation(screenPos.x + 10, screenPos.y + 10);
        setVisible(true);
    }

    /**
	 * Hide the hover info window. This method simply sets the window's visibility
	 * to false, which will hide it from view. The window can be shown again later
	 * by calling showMessage() with new text and position.
	 * Note that the window is not disposed, so it can be reused for future hover events without needing to recreate it.
	 * This allows for efficient reuse of the hover info window across multiple hover interactions.
	 */
    public void hideWindow() {
        setVisible(false);
    }
}