package edu.cnu.mdi.desktop;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.UIManager;

/**
 * DesktopManager that deterministically draws and erases OUTLINE_DRAG_MODE
 * rectangles, eliminating "ghost" outlines when mouseReleased is missed.
 * <p>
 * IMPORTANT: In OUTLINE_DRAG_MODE, the outline drag normally does not move the
 * real frame until mouse release. Since we draw the outline ourselves, we must
 * also apply the final bounds on endDraggingFrame.
 * </p>
 */
@SuppressWarnings("serial")
public final class GhostBustingDesktopManager extends DefaultDesktopManager {

    private final JDesktopPane desktop;

    // Last outline we drew (in desktop coordinates)
    private Rectangle lastOutline = null;
    private boolean outlineDrawn = false;

    public GhostBustingDesktopManager(JDesktopPane desktop) {
        this.desktop = desktop;
    }

    /** Force-erase any outline we may have drawn. Safe to call anytime. */
    public void cancelOutline() {
        if (outlineDrawn && lastOutline != null) {
            xorOutline(lastOutline); // XOR toggle erase
        }
        outlineDrawn = false;
        lastOutline = null;
    }

    @Override
    public void beginDraggingFrame(JComponent f) {
        cancelOutline();
        super.beginDraggingFrame(f);
    }

    @Override
    public void dragFrame(JComponent f, int newX, int newY) {

        if (desktop.getDragMode() == JDesktopPane.OUTLINE_DRAG_MODE) {

            Rectangle newRect = new Rectangle(newX, newY, f.getWidth(), f.getHeight());

            // Erase old outline (XOR toggle)
            if (outlineDrawn && lastOutline != null) {
                xorOutline(lastOutline);
            }

            // Draw new outline
            xorOutline(newRect);

            lastOutline = newRect;
            outlineDrawn = true;
            return;
        }

        // Not outline mode: default behavior (live dragging)
        super.dragFrame(f, newX, newY);
    }

    @Override
    public void endDraggingFrame(JComponent f) {

        // In OUTLINE_DRAG_MODE, we must move the actual frame to the final outline.
        if (desktop.getDragMode() == JDesktopPane.OUTLINE_DRAG_MODE) {
            Rectangle r = lastOutline;

            // Erase our outline before moving, so we don't leave XOR artifacts behind.
            if (outlineDrawn && r != null) {
                xorOutline(r);
            }

            outlineDrawn = false;

            // Apply final bounds to the real component.
            if (r != null) {
                try {
                    setBoundsForFrame(f, r.x, r.y, r.width, r.height);
                } catch (Exception ignored) {
                    // Fallback: direct bounds set
                    try {
                        f.setBounds(r);
                    } catch (Exception ignored2) {
                    }
                }
            }

            lastOutline = null;

            // Let the default manager finish any housekeeping.
            super.endDraggingFrame(f);

            // Strong repaint helps with stubborn compositing cases.
            desktop.repaint();
            try {
                desktop.paintImmediately(0, 0, desktop.getWidth(), desktop.getHeight());
            } catch (Exception ignored) {
            }
            return;
        }

        // Non-outline mode: default behavior.
        super.endDraggingFrame(f);
    }

    @Override
    public void closeFrame(JInternalFrame f) {
        cancelOutline();
        super.closeFrame(f);
    }

    private void xorOutline(Rectangle r) {
        Graphics g = desktop.getGraphics();
        if (g == null) {
            return;
        }
        try {
            Color outline = UIManager.getColor("DesktopManager.dragOutline");
            if (outline == null) {
                outline = Color.WHITE;
            }

            // XOR against desktop background; drawing same rect twice erases it.
            g.setColor(outline);
            g.setXORMode(desktop.getBackground());
            g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        } finally {
            g.dispose();
        }
    }
}
