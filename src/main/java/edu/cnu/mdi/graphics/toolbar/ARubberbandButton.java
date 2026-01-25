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

import edu.cnu.mdi.graphics.rubberband.ARubberband;
import edu.cnu.mdi.graphics.rubberband.IRubberbanded;
import edu.cnu.mdi.graphics.rubberband.RubberbandFactory;

/**
 * Toggle tool that drives a rubberband gesture on a canvas.
 * <p>
 * This class manages the lifetime of the underlying {@link ARubberband} and
 * a {@link GestureContext} for the gesture.
 * </p>
 *
 * <h2>Start policy</h2>
 * <p>
 * Some policies are "drag-started" (rectangle policies) and delay rubberband
 * creation until a drag occurs. Others are click-based and start immediately
 * on press.
 * </p>
 */
@SuppressWarnings("serial")
public abstract class ARubberbandButton extends JToggleButton
        implements MouseMotionListener, MouseListener, IRubberbanded {

    private final int minSizePx;

    protected final Component canvas;
    protected final AToolBar toolBar;
    protected final ARubberband.Policy policy;

    protected ARubberband rubberband;
    protected final boolean startOnDrag;

    /** Gesture context for the current gesture (null when idle). */
    protected GestureContext gesture;

    /**
     * Create a rubberband tool.
     *
     * @param canvas    canvas component (non-null)
     * @param toolBar   owning toolbar (non-null)
     * @param policy    rubberband policy (non-null)
     * @param minSizePx minimum size (pixels) required for a valid gesture
     */
    protected ARubberbandButton(Component canvas, AToolBar toolBar, ARubberband.Policy policy, int minSizePx) {
        this.canvas = Objects.requireNonNull(canvas, "canvas");
        this.toolBar = Objects.requireNonNull(toolBar, "toolBar");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.minSizePx = Math.max(1, minSizePx);

        this.startOnDrag = (policy == ARubberband.Policy.RECTANGLE
                || policy == ARubberband.Policy.RECTANGLE_PRESERVE_ASPECT);
    }

    /** @return the configured policy */
    protected ARubberband.Policy rubberbandPolicy() {
        return policy;
    }

    /** @return cursor to use when this tool is active */
    protected Cursor activeCursor() {
        return (policy == ARubberband.Policy.NONE)
                ? Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                : Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    /**
     * Called when a valid rubberband gesture completes.
     *
     * @param gesture  gesture context captured for this gesture (never null)
     * @param bounds   rubberband bounds (never null)
     * @param vertices rubberband vertices (never null)
     */
    public abstract void rubberbanding(GestureContext gesture, Rectangle bounds, Point[] vertices);

    @Override
    public final void doneRubberbanding() {
        if (policy == ARubberband.Policy.NONE) {
            return;
        }

        final ARubberband rb = rubberband;
        final GestureContext gc = gesture;

        rubberband = null;
        gesture = null;

        if (rb == null || gc == null || !rb.isGestureValid(minSizePx)) {
            return;
        }

        Rectangle bounds = rb.getRubberbandBounds();
        Point[] vertices = rb.getRubberbandVertices();

        if (gc != null && rb instanceof edu.cnu.mdi.graphics.rubberband.IRubberbandAngleProvider ap) {
            gc.setRubberbandAngleDeg(ap.getRubberbandAngleDeg());
        }

        rubberbanding(gc, bounds, vertices);
        canvas.repaint();
       canvas.repaint();
    }

    /**
     * Cancel an in-progress rubberband gesture, if any.
     */
    protected final void cancelRubberband() {
        ARubberband rb = rubberband;
        rubberband = null;
        gesture = null;
        if (rb != null) {
            rb.cancel();
        }
    }

    @Override public void mouseClicked(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }
    @Override public void mouseMoved(MouseEvent e) { }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (gesture != null) {
            gesture.update(e);
        }
        if (startOnDrag && rubberband == null) {
            init(e);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (policy == ARubberband.Policy.NONE) {
            return;
        }

        // Start a gesture (target is tool-defined; typically null for rubberbands)
        gesture = new GestureContext(toolBar, canvas, null, e.getPoint(), e);

        if (!startOnDrag && rubberband == null) {
            init(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (gesture != null) {
            gesture.update(e);
        }
    }

    private void init(MouseEvent e) {
        if ((policy == ARubberband.Policy.NONE) || (gesture == null) || (rubberband != null)) {
			return;
		}

        ARubberband.Policy pol = Objects.requireNonNull(rubberbandPolicy(), "rubberbandPolicy");
        rubberband = RubberbandFactory.create(canvas, this, pol);
        if (rubberband == null) {
			return;
		}

        rubberband.setActive(true);

        // Click-based tools must see the click that created them.
        if (rubberband.isClickBased()) {
            rubberband.mousePressed(e);
        } else {
            rubberband.begin(gesture.getPressPoint());
        }
    }
}
