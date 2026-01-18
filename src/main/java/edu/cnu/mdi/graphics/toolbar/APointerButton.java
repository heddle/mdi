package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.Objects;

import javax.swing.JToggleButton;

import edu.cnu.mdi.graphics.rubberband.IRubberbanded;
import edu.cnu.mdi.graphics.rubberband.Rubberband;

@SuppressWarnings("serial")
public abstract class APointerButton extends JToggleButton
		implements MouseMotionListener, MouseListener, IRubberbanded {

	private final int minSizePx;

	protected Component canvas;
	protected AToolBar toolBar;
	protected Rubberband.Policy policy;
	protected Rubberband rubberband;

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

	protected APointerButton(Component canvas, AToolBar toolBar, Rubberband.Policy policy, int minSizePx) {
		Objects.requireNonNull(canvas, "canvas");
		Objects.requireNonNull(toolBar, "toolBar");
		Objects.requireNonNull(policy, "policy");
		this.canvas = canvas;
		this.toolBar = toolBar;
		this.policy = policy;
		this.minSizePx = Math.max(1, minSizePx);
	}

	/** Get the rubberband policy for this tool. */
	protected Rubberband.Policy rubberbandPolicy() {
		return policy;
	}
	
	/** Get the active cursor for this tool. */
	protected Cursor activeCursor() {
		if (policy == Rubberband.Policy.NONE) {
			return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
		}
		return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}

	/** Called when a rubberband gesture completes with valid bounds. */
	public abstract void rubberbanding(Rectangle bounds, Point[] vertices);


	/** Cancel any ongoing rubberband gesture. */
	protected final void cancelRubberband() {
		if (policy == Rubberband.Policy.NONE) {
			return;
		}
		Rubberband rb = rubberband;
		rubberband = null;
		if (rb != null) {
			rb.cancel();
		}
	}

	// ------ IRubberbanded interface methods ------
	
	// Validate rubberband bounds.
	private boolean isValidBounds(Rectangle b) {
		return (b != null) && (b.width >= minSizePx) && (b.height >= minSizePx);
	}

	// Check whether drag has passed threshold.
	private boolean pastThreshold(Point a, Point b) {
		int dx = b.x - a.x;
		int dy = b.y - a.y;
		int thr = dragThresholdPx;
		return dx * dx + dy * dy >= thr * thr;
	}

	@Override
	public final void doneRubberbanding() {
		if (policy == Rubberband.Policy.NONE) {
			return;
		}

		final Rubberband rb = this.rubberband;
		this.rubberband = null;

		mode = Mode.IDLE;

		if (rb == null) {
			return;
		}

		Rectangle bounds = rb.getRubberbandBounds();
		if (!isValidBounds(bounds)) {
			return;
		}

		Point[] vertices = rb.getRubberbandVertices();
		rubberbanding(bounds, vertices);

		if (canvas != null) {
			canvas.repaint();
		}
	}

	// ---------------- MouseListener / MouseMotionListener ----------------

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (policy == Rubberband.Policy.NONE || canvas == null) {
			return;
		}

		// Double-click handling lives here.
		if (e.getClickCount() != 2) {
			return;
		}
		;
// Ignore double-clicks once a drag gesture is underway.
		if (mode == Mode.MOVING || mode == Mode.RUBBERBANDING) {
			return;
		}

		Object dc = hitTest(e.getPoint());
		doubleClickObject(dc, e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (policy == Rubberband.Policy.NONE || canvas == null) {
			return;
		}

		pressPt = e.getPoint();
		lastPt = pressPt;

		hitObject = hitTest(pressPt);
		
		if ((hitObject != null) && e.isPopupTrigger()) {
			// Right-click on object: treat as click, not drag.
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
		if (policy == Rubberband.Policy.NONE || canvas == null || pressPt == null) {
			return;
		}

		Point p = e.getPoint();

		// Not committed to a drag yet? Check threshold.
		if (mode == Mode.PRESS_ON_OBJECT || mode == Mode.PRESS_ON_EMPTY) {
			if (!pastThreshold(pressPt, p)) {
				return; // still a click candidate
			}

			// Promote to a drag mode.
			if (mode == Mode.PRESS_ON_OBJECT) {
				if (doNotDrag(hitObject,  e)) {
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

		// While RUBBERBANDING, Rubberband itself tracks/paints.
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (policy == Rubberband.Policy.NONE || canvas == null) {
			return;
		}

		try {
			if (mode == Mode.PRESS_ON_OBJECT) {
				clickObject(hitObject, e);
			} else if (mode == Mode.PRESS_ON_EMPTY) {
				clickObject(null, e); // optional: treat empty click as "background click"
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

	// start rubberbanding
	private void beginRubberband(MouseEvent e) {
		if (policy == Rubberband.Policy.NONE) {
			return;
		}
		if (rubberband == null) {
			Rubberband.Policy pol = Objects.requireNonNull(rubberbandPolicy(), "rubberbandPolicy");
			rubberband = new Rubberband(canvas, this, pol);
		}
		rubberband.setActive(true);
		rubberband.startRubberbanding(pressPt != null ? pressPt : e.getPoint());
	}
	
	

	// ------ Interface methods for subclasses to override ------

	/** Single-click hit test (press). */
	protected Object hitTest(Point p) {
		return null;
	}

	/** Click on an object (obj may be null for background). */
	protected void clickObject(Object obj, MouseEvent e) {
	}

	/** Double-click on an object. */
	protected void doubleClickObject(Object obj, MouseEvent e) {
	}
	
	/** Drag promoted to MOVING. (Dragging an object or objects) */
	protected void beginDragObject(Object obj, Point pressPoint, MouseEvent e) {
	}

	/** Move selection by pixel delta. */
	protected void dragObjectBy(Object object, int dx, int dy, MouseEvent e) {
	}

	/** End move. */
	protected void endDragObject(Object object, MouseEvent e) {
	}
	
	/** Return true to prevent dragging of the given object. */
	protected boolean doNotDrag(Object object, MouseEvent e) {
		return false;
	}

}
