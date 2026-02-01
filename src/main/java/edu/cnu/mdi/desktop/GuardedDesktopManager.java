package edu.cnu.mdi.desktop;

import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;

public class GuardedDesktopManager extends DefaultDesktopManager {
	
	private static final int TOP_MARGIN = 5; // A little breathing room
    
    @Override
    public void dragFrame(JComponent f, int newX, int newY) {
        // Prevent the title bar from going above the top edge (y < 0)
        if (newY < TOP_MARGIN) {
            newY = TOP_MARGIN;
        }

        // Optional: Prevent dragging off the left, right, or bottom
        // int maxW = f.getParent().getWidth() - 30; // keep 30px visible
        // int maxH = f.getParent().getHeight() - 30;
        // newX = Math.max(-f.getWidth() + 30, Math.min(newX, maxW));
        // newY = Math.min(newY, maxH);

        super.dragFrame(f, newX, newY);
    }
}