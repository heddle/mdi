package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Objects;

import javax.swing.JToggleButton;

/**
 * Toggle tool that drives a hover-move gesture (no press required).
 * <p>
 * This is commonly used for magnify/inspect tools where the action follows the
 * mouse while it is inside the canvas and stops when it exits.
 * </p>
 *
 * <h2>Gesture lifetime</h2>
 * <ul>
 *   <li>On the first {@code mouseMoved} while idle, a new {@link GestureContext} is created.</li>
 *   <li>Each subsequent move updates the context and calls {@link #updateMove(GestureContext)}.</li>
 *   <li>On {@code mouseExited}, {@link #doneMove(GestureContext)} is called and the gesture ends.</li>
 * </ul>
 */
@SuppressWarnings("serial")
public abstract class AMoveButton extends JToggleButton implements MouseMotionListener, MouseListener {

    /** Canvas component (non-null). */
    protected final Component canvas;

    /** Owning toolbar (non-null). */
    protected final AToolBar toolBar;

    /** Current move gesture context (null when idle). */
    protected GestureContext gesture;

    /**
     * Create a hover-move tool.
     *
     * @param canvas  canvas component (non-null)
     * @param toolBar owning toolbar (non-null)
     */
    public AMoveButton(Component canvas, AToolBar toolBar) {
        this.canvas = Objects.requireNonNull(canvas, "canvas");
        this.toolBar = Objects.requireNonNull(toolBar, "toolBar");
    }

    @Override public void mouseClicked(MouseEvent e) { }
    @Override public void mousePressed(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) {
        if (gesture != null) {
            gesture.update(e);
            doneMove(gesture);
            gesture = null;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // This tool is hover-driven; ignore drags.
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (gesture == null) {
            gesture = new GestureContext(toolBar, canvas, null, e.getPoint(), e);
            startMove(gesture);
        } else {
            gesture.update(e);
            updateMove(gesture);
        }
    }

    /**
     * Called once at the start of the move gesture.
     *
     * @param gc gesture context (non-null)
     */
    public abstract void startMove(GestureContext gc);

    /**
     * Called on each move update after start.
     *
     * @param gc gesture context (non-null)
     */
    public abstract void updateMove(GestureContext gc);

    /**
     * Called when the move gesture ends (mouse exits canvas).
     *
     * @param gc gesture context (non-null)
     */
    public abstract void doneMove(GestureContext gc);
}
