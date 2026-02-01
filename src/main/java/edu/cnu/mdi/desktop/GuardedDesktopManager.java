package edu.cnu.mdi.desktop;

import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;

/**
 * A DesktopManager that prevents internal frames from being dragged
 * such that their title bars go off the top edge of the desktop.
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class GuardedDesktopManager extends DefaultDesktopManager {
	
	private static final int TOP_MARGIN = 5; // A little breathing room
    
    @Override
    public void dragFrame(JComponent f, int newX, int newY) {
        // Prevent the title bar from going above the top edge (y < 0)
        if (newY < TOP_MARGIN) {
            newY = TOP_MARGIN;
        }

        super.dragFrame(f, newX, newY);
    }
}