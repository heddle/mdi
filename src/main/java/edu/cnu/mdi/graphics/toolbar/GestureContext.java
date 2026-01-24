package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Objects;

/**
 * Encapsulates the state of a single user interaction ("gesture") on a canvas.
 * <p>
 * A gesture typically begins on a mouse press (or the first meaningful move for
 * hover-driven tools), proceeds through zero or more updates, and ends on
 * release or exit. This class packages:
 * </p>
 * <ul>
 * <li>The owning {@link AToolBar}</li>
 * <li>The canvas {@link Component}</li>
 * <li>The press-time target object (may be {@code null})</li>
 * <li>The press point (immutable)</li>
 * <li>The previous and current points (updated as events arrive)</li>
 * <li>Modifier key state (shift/ctrl/alt/meta) as of the most recent
 * update</li>
 * </ul>
 *
 * <h2>Immutability and copies</h2>
 * <p>
 * The press-time values are immutable, and this class returns defensive copies
 * for all {@link Point} accessors to avoid accidental external mutation.
 * </p>
 */
public final class GestureContext {

	private final AToolBar toolBar;
	private final Component canvas;
	private final Object target;
	private final Point pressPoint;

	private Point previousPoint;
	private Point currentPoint;

	private boolean shift;
	private boolean ctrl;
	private boolean alt;
	private boolean meta;

	private MouseEvent sourceEvent;
	private MouseEvent recentEvent;

	// Add near the other fields
	private Double rubberbandAngleDeg; // optional, for tools like RADARC

	/** Package-private: set by toolbar/rubberband code. */
	void setRubberbandAngleDeg(Double angleDeg) {
		this.rubberbandAngleDeg = angleDeg;
	}

	/**
	 * Optional extra angle for rubberband tools (e.g. RADARC) in degrees.
	 * <p>
	 * For RADARC this is the signed, unwrapped sweep (can exceed 180 in magnitude).
	 * </p>
	 *
	 * @return the angle in degrees, or null if not provided
	 */
	public Double getRubberbandAngleDeg() {
		return rubberbandAngleDeg;
	}

	/**
	 * Create a context at gesture start using a mouse event.
	 *
	 * @param toolBar    owning toolbar (non-null)
	 * @param canvas     canvas component (non-null)
	 * @param target     press-time target (may be null)
	 * @param pressPoint press point (non-null)
	 * @param e          initiating mouse event (may be null)
	 */
	public GestureContext(AToolBar toolBar, Component canvas, Object target, Point pressPoint, MouseEvent e) {
		this.toolBar = Objects.requireNonNull(toolBar, "toolBar");
		this.canvas = Objects.requireNonNull(canvas, "canvas");
		this.target = target;
		this.pressPoint = new Point(Objects.requireNonNull(pressPoint, "pressPoint"));
		this.previousPoint = new Point(this.pressPoint);
		this.currentPoint = new Point(this.pressPoint);
		this.sourceEvent = e;
		if (e != null) {
			updateModifiers(e);
		}
		this.recentEvent = e;
	}

	/**
	 * Update the context using the event's point and modifier keys.
	 *
	 * @param e mouse event (non-null)
	 */
	public void update(MouseEvent e) {
		Objects.requireNonNull(e, "e");
		update(e.getPoint(), e);
	}

	/**
	 * Update the context using an explicit point and an event for modifier keys.
	 *
	 * @param current new current point (non-null)
	 * @param e       mouse event used to update modifiers (may be null)
	 */
	public void update(Point current, MouseEvent e) {
		previousPoint = currentPoint;
		currentPoint = new Point(Objects.requireNonNull(current, "current"));
		recentEvent = e;
		updateModifiers(e);
	}

	private void updateModifiers(MouseEvent e) {
		if (e == null) {
			return;
		}
		shift = e.isShiftDown();
		ctrl = e.isControlDown();
		alt = e.isAltDown();
		meta = e.isMetaDown();
	}

	/** @return source mouse event (may be null) */
	public MouseEvent getSourceEvent() {
		return sourceEvent;
	}

	/** @return most recent mouse event (may be null) */
	public MouseEvent getRecentEvent() {
		return recentEvent;
	}

	/** @return owning toolbar (never null) */
	public AToolBar getToolBar() {
		return toolBar;
	}

	/** @return canvas component (never null) */
	public Component getCanvas() {
		return canvas;
	}

	/** @return press-time target object (may be null) */
	public Object getTarget() {
		return target;
	}

	/** @return copy of the press point */
	public Point getPressPoint() {
		return new Point(pressPoint);
	}

	/** @return copy of the previous point */
	public Point getPreviousPoint() {
		return new Point(previousPoint);
	}

	/** @return copy of the current point */
	public Point getCurrentPoint() {
		return new Point(currentPoint);
	}

	/** @return incremental delta x (current - previous) */
	public int deltaX() {
		return currentPoint.x - previousPoint.x;
	}

	/** @return incremental delta y (current - previous) */
	public int deltaY() {
		return currentPoint.y - previousPoint.y;
	}

	/** @return total delta x (current - press) */
	public int totalDeltaX() {
		return currentPoint.x - pressPoint.x;
	}

	/** @return total delta y (current - press) */
	public int totalDeltaY() {
		return currentPoint.y - pressPoint.y;
	}

	/** @return true if shift is down at last update */
	public boolean isShiftDown() {
		return shift;
	}

	/** @return true if control is down at last update */
	public boolean isControlDown() {
		return ctrl;
	}

	/** @return true if alt is down at last update */
	public boolean isAltDown() {
		return alt;
	}

	/** @return true if meta/command is down at last update */
	public boolean isMetaDown() {
		return meta;
	}
}
