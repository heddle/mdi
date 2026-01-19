package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Objects;

import javax.swing.JToggleButton;

/**
 * Toggle tool that drives a press-drag-release gesture.
 * <p>
 * Unlike {@link ARubberbandButton}, this class does not draw a rubberband.
 * It simply tracks a gesture and calls abstract callbacks.
 * </p>
 *
 * <h2>Containment</h2>
 * <p>
 * This tool ignores drags that leave the canvas bounds (as the original version did).
 * </p>
 */
@SuppressWarnings("serial")
public abstract class ADragButton extends JToggleButton implements MouseListener, MouseMotionListener {

    /** Canvas component (non-null). */
    protected final Component canvas;

    /** Owning toolbar (non-null). */
    protected final AToolBar toolBar;

    /** Current gesture context (null when idle). */
    protected GestureContext gesture;

    /** True if we have moved at least once while pressed. */
    private boolean dragging;

    /**
     * Create a drag tool.
     *
     * @param canvas  canvas component (non-null)
     * @param toolBar owning toolbar (non-null)
     */
    public ADragButton(Component canvas, AToolBar toolBar) {
        this.canvas = Objects.requireNonNull(canvas, "canvas");
        this.toolBar = Objects.requireNonNull(toolBar, "toolBar");
    }

    @Override public void mouseClicked(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }
    @Override public void mouseMoved(MouseEvent e) { }

    @Override
    public void mousePressed(MouseEvent e) {
        gesture = new GestureContext(toolBar, canvas, null, e.getPoint(), e);
        dragging = false;
        startDrag(gesture);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (gesture == null) {
            return;
        }
        if (!contained(e.getPoint())) {
            return;
        }

        dragging = true;
        gesture.update(e);
        updateDrag(gesture);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (gesture == null) {
            return;
        }

        gesture.update(e);
        if (dragging) {
            doneDrag(gesture);
        } else {
            // still treat as a completed gesture (tools may ignore)
            doneDrag(gesture);
        }

        gesture = null;
        dragging = false;
    }

    /**
     * Called once on mouse press.
     *
     * @param gc gesture context (non-null)
     */
    public abstract void startDrag(GestureContext gc);

    /**
     * Called during dragging (after at least one drag event).
     *
     * @param gc gesture context (non-null)
     */
    public abstract void updateDrag(GestureContext gc);

    /**
     * Called on release.
     *
     * @param gc gesture context (non-null)
     */
    public abstract void doneDrag(GestureContext gc);

    /**
     * Check if the point is within the canvas bounds.
     */
    private boolean contained(Point p) {
        return (p.x >= 0 && p.y >= 0 && p.x < canvas.getWidth() && p.y < canvas.getHeight());
    }
}
