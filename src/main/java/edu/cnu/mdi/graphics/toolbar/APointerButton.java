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

@SuppressWarnings("serial")
public abstract class APointerButton extends JToggleButton
        implements MouseMotionListener, MouseListener, IRubberbanded {

    private final int minSizePx;

    protected Component canvas;
    protected AToolBar toolBar;
    protected ARubberband.Policy policy;
    protected ARubberband rubberband;

    /** Drag threshold (px) to promote press into a drag gesture. */
    protected int dragThresholdPx = 5;

    private enum Mode {
        IDLE, PRESS_ON_OBJECT, PRESS_ON_EMPTY, MOVING, RUBBERBANDING
    }

    private Mode mode = Mode.IDLE;

    private Point pressPt;
    private Point lastPt;

    /** Object under cursor on press (if any). */
    private Object hitObject;

    /**
	 * Create a pointer button that supports rubberbanding.
	 *
	 * @param canvas    the component on which gestures occur
	 * @param toolBar   the toolbar that owns this tool
	 * @param policy    the rubberbanding policy for this tool
	 * @param minSizePx minimum size (in pixels) for a valid rubberband gesture
	 */
    protected APointerButton(Component canvas, AToolBar toolBar, ARubberband.Policy policy, int minSizePx) {
        Objects.requireNonNull(canvas, "canvas");
        Objects.requireNonNull(toolBar, "toolBar");
        Objects.requireNonNull(policy, "policy");
        this.canvas = canvas;
        this.toolBar = toolBar;
        this.policy = policy;
        this.minSizePx = Math.max(1, minSizePx);
    }

    /**
     * Get the rubberbanding policy for this tool.
     * @return the rubberbanding policy
     */
    protected ARubberband.Policy rubberbandPolicy() {
        return policy;
    }

    /** 
	 * @return cursor to use while active. Default is crosshair or default cursor
	 *         if no rubberbanding.
	 */
    protected Cursor activeCursor() {
        if (policy == ARubberband.Policy.NONE) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    public abstract void rubberbanding(Rectangle bounds, Point[] vertices);

    protected final void cancelRubberband() {
        if (policy == ARubberband.Policy.NONE) {
            return;
        }
        ARubberband rb = rubberband;
        rubberband = null;
        if (rb != null) {
            rb.cancel();
        }
    }


    // Check if the distance between two points exceeds the drag threshold
    private boolean pastThreshold(Point a, Point b) {
        int dx = b.x - a.x;
        int dy = b.y - a.y;
        int thr = dragThresholdPx;
        return dx * dx + dy * dy >= thr * thr;
    }

    @Override
    public final void doneRubberbanding() {
        if (policy == ARubberband.Policy.NONE) {
            return;
        }

        final ARubberband rb = this.rubberband;
        this.rubberband = null;

        mode = Mode.IDLE;

        if (rb == null) {
            return;
        }

        Rectangle bounds = rb.getRubberbandBounds();
        Point[] vertices = rb.getRubberbandVertices();

        if (!rb.isGestureValid(minSizePx)) {
            return;
        }

        rubberbanding(bounds, vertices);

        if (canvas != null) {
            canvas.repaint();
        }
    }

    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }
    @Override public void mouseMoved(MouseEvent e) { }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (policy == ARubberband.Policy.NONE || canvas == null) {
            return;
        }

        if (e.getClickCount() != 2) {
            return;
        }

        if (mode == Mode.MOVING || mode == Mode.RUBBERBANDING) {
            return;
        }

        Object dc = hitTest(e.getPoint());
        doubleClickObject(dc, e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (policy == ARubberband.Policy.NONE || canvas == null) {
            return;
        }

        pressPt = e.getPoint();
        lastPt = pressPt;

        hitObject = hitTest(pressPt);

        if ((hitObject != null) && e.isPopupTrigger()) {
            clickObject(hitObject, e);
            mode = Mode.IDLE;
            pressPt = null;
            lastPt = null;
            hitObject = null;
            return;
        }

        mode = (hitObject != null) ? Mode.PRESS_ON_OBJECT : Mode.PRESS_ON_EMPTY;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (policy == ARubberband.Policy.NONE || canvas == null || pressPt == null) {
            return;
        }

        Point p = e.getPoint();

        if (mode == Mode.PRESS_ON_OBJECT || mode == Mode.PRESS_ON_EMPTY) {
            if (!pastThreshold(pressPt, p)) {
                return;
            }

            if (mode == Mode.PRESS_ON_OBJECT) {
                if (doNotDrag(hitObject, e)) {
                    mode = Mode.IDLE;
                    return;
                }
                mode = Mode.MOVING;
                beginDragObject(hitObject, pressPt, e);
            } else {
                mode = Mode.RUBBERBANDING;
                beginRubberband(e);
            }
        }

        if (mode == Mode.MOVING) {
            int dx = p.x - lastPt.x;
            int dy = p.y - lastPt.y;
            if (dx != 0 || dy != 0) {
                dragObjectBy(hitObject, dx, dy, e);
                lastPt = p;
                canvas.repaint();
            }
        }

        // While RUBBERBANDING, ARubberband handles tracking/painting.
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (policy == ARubberband.Policy.NONE || canvas == null) {
            return;
        }

        try {
            if (mode == Mode.PRESS_ON_OBJECT) {
                clickObject(hitObject, e);
            } else if (mode == Mode.PRESS_ON_EMPTY) {
                clickObject(null, e);
            } else if (mode == Mode.MOVING) {
                endDragObject(hitObject, e);
            } else if (mode == Mode.RUBBERBANDING) {
                // Completion arrives via doneRubberbanding()
            }
        } finally {
            if (mode != Mode.RUBBERBANDING) {
                mode = Mode.IDLE;
            }
            pressPt = null;
            lastPt = null;
            hitObject = null;
        }
    }

	private void beginRubberband(MouseEvent e) {
		if (policy == ARubberband.Policy.NONE) {
			return;
		}
		if (rubberband == null) {
			ARubberband.Policy pol = Objects.requireNonNull(rubberbandPolicy(), "rubberbandPolicy");
			rubberband = RubberbandFactory.create(canvas, this, pol);
		}
		if (rubberband == null) {
			return;
		}
		rubberband.setActive(true);
		if (rubberband.isGestureValid(minSizePx)) {
			// IMPORTANT: forward the very first click that created the rubberband
			rubberband.mousePressed(e);
		} else {
			// Drag-based policies need an anchor immediately (press-point, not first drag
			// point)
			rubberband.begin(pressPt);
		}
	}

    // ------ Interface methods for subclasses to override ------

    protected Object hitTest(Point p) { return null; }

    protected void clickObject(Object obj, MouseEvent e) { }

    protected void doubleClickObject(Object obj, MouseEvent e) { }

    protected void beginDragObject(Object obj, Point pressPoint, MouseEvent e) { }

    protected void dragObjectBy(Object object, int dx, int dy, MouseEvent e) { }

    protected void endDragObject(Object object, MouseEvent e) { }

    protected boolean doNotDrag(Object object, MouseEvent e) { return false; }
}
