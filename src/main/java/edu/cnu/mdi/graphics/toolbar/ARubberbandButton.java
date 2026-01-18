package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Objects;

import javax.swing.JToggleButton;

import edu.cnu.mdi.graphics.rubberband.IRubberbanded;
import edu.cnu.mdi.graphics.rubberband.Rubberband;

/**
 * Base class for tools using a {@link Rubberband} bounds-based gesture (e.g.
 * rectangle, ellipse) on a component.
 *
 * <p>
 * This version uses {@link GestureContext} to preserve the true press point and
 * to provide modifier state to subclasses at completion time.
 * </p>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public abstract class ARubberbandButton extends JToggleButton
        implements MouseMotionListener, MouseListener, IRubberbanded {

    /** Minimum width/height in pixels for a creation gesture to be accepted. */
    private final int minSizePx;

    /** Component that owns the current gesture (null when idle). */
    protected Component canvas;

    /** Toolbar that owns this tool. */
    protected AToolBar toolBar;

    /** Rubberband policy to use. */
    protected Rubberband.Policy policy;

    /** Cached rubber band (null when idle). */
    protected Rubberband rubberband;

    /** If true, rubberbanding starts on drag instead of press. */
    protected boolean startOnDrag;

    /** Context for the current gesture (null when idle). */
    protected GestureContext gesture;

    /**
     * Create a rubber-band based tool.
     *
     * @param canvas    component on which rubber-banding occurs.
     * @param toolBar   toolbar that owns this tool.
     * @param policy    rubber-band policy to use (e.g., {@link Rubberband.Policy#OVAL}).
     * @param minSizePx minimum pixel size for bounds to be considered valid.
     */
    protected ARubberbandButton(Component canvas, AToolBar toolBar, Rubberband.Policy policy, int minSizePx) {
        Objects.requireNonNull(canvas, "canvas");
        Objects.requireNonNull(toolBar, "toolBar");
        Objects.requireNonNull(policy, "policy");
        this.canvas = canvas;
        this.toolBar = toolBar;
        this.policy = policy;
        this.minSizePx = Math.max(1, minSizePx);

        startOnDrag = (policy == Rubberband.Policy.RECTANGLE || policy == Rubberband.Policy.RECTANGLE_PRESERVE_ASPECT);
    }

    /**
     * @return rubber-band policy to use (e.g., {@link Rubberband.Policy#OVAL}).
     */
    protected Rubberband.Policy rubberbandPolicy() {
        return policy;
    }

    /**
     * @return cursor to use while active. Default is crosshair.
     */
    protected Cursor activeCursor() {
        if (policy == Rubberband.Policy.NONE) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    /**
     * Called by {@link Rubberband} when the gesture completes.
     */
    @Override
    public final void doneRubberbanding() {
        if (policy == Rubberband.Policy.NONE) {
            return;
        }

        // Snapshot fields first, then clear instance state early.
        final Rubberband rb = this.rubberband;
        final GestureContext gc = this.gesture;
        
        this.rubberband = null;
        this.gesture = null;
        
        if (rb == null) {
            return;
        }

        Point[] vertices = rb.getRubberbandVertices();
        Rectangle bounds = rb.getRubberbandBounds();

        if (!isValidBounds(bounds)) {
           return;
        }

        rubberbanding(gc, bounds, vertices);
        this.gesture = null;
        canvas.repaint();
    }

    /**
     * Cancel an in-progress rubber-band gesture.
     */
    protected final void cancelRubberband() {
        if (policy == Rubberband.Policy.NONE) {
            return;
        }
        Rubberband rb = rubberband;
        rubberband = null;
        gesture = null;
        if (rb != null) {
            rb.cancel();
        }
    }

    // checks minimum size of rubberband bounds
    private boolean isValidBounds(Rectangle b) {
        return (b != null) && (b.width >= minSizePx) && (b.height >= minSizePx);
    }

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }

    @Override
    public void mouseMoved(MouseEvent e) { }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Keep gesture current (modifiers may change mid-gesture)
        if (gesture != null) {
            gesture.update(e);
        }

        // For start-on-drag tools, initialize on first drag
        if (startOnDrag && rubberband == null) {
            init();
        }
    }

    // initialize rubberbanding, starting from the true press point
    private void init() {
        if (policy == Rubberband.Policy.NONE) {
            return;
        }

        if (gesture == null) {
            // This can happen if we never saw a press (unlikely), or if caller canceled early.
            // Best effort: start from current mouse location isn't available here, so bail.
            return;
        }

        if (rubberband == null) {
            Rubberband.Policy pol = Objects.requireNonNull(rubberbandPolicy(), "rubberbandPolicy");
            rubberband = new Rubberband(canvas, this, pol);
        }

        rubberband.setActive(true);

        // Critical: start from the press point, not the first drag point.
        rubberband.startRubberbanding(gesture.getPressPoint());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (policy == Rubberband.Policy.NONE) {
            return;
        }

        // Create context at press time (true press point + initial modifiers).
        // Rubberband creation gestures don't have a "target object", so target = null.
        gesture = new GestureContext(toolBar, canvas, null, e.getPoint(), e);

        if (!startOnDrag && rubberband == null) {
            init();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Keep final modifiers/current point available to doneRubberbanding subclass handler.
        if (gesture != null) {
            gesture.update(e);
        }
    }

    /**
     * Handle a completed rubber-band gesture with valid bounds.
     *
     * @param gesture  the gesture context captured at press time (may be null if gesture was lost/canceled)
     * @param bounds   the rubber-band bounds
     * @param vertices the rubber-band vertices
     */
    public abstract void rubberbanding(GestureContext gesture, Rectangle bounds, Point[] vertices);
}
