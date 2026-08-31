package edu.cnu.mdi.splot.plot;

import java.awt.Point;
import java.awt.Rectangle;

/** Rectangle with the interaction state required for draggable plot overlays. */
@SuppressWarnings("serial")
public class DraggableRectangle extends Rectangle implements Draggable {

	// are we being dragged
	protected boolean _dragging;

	// is dragging primed
	protected boolean _draggingPrimed;

	// current point
	protected Point _currentPoint;

	protected boolean _beenMoved;

	@Override
	public boolean isDraggingPrimed() {
		return _draggingPrimed;
	}

	@Override
	public boolean isDragging() {
		return _dragging;
	}

	@Override
	public void setDraggingPrimed(boolean primed) {
		_draggingPrimed = primed;
	}

	@Override
	public void setDragging(boolean dragging) {
		_dragging = dragging;

		// Only a transition into the dragging state means the overlay has
		// actually begun moving. Clearing dragging (e.g. on every mouse
		// release, even a plain click that never primed a drag) must not
		// mark it as moved, or ExtraText's "keep at default location until
		// dragged" logic breaks after the very first click anywhere on the
		// canvas.
		if (dragging) {
			_beenMoved = true;
		}
	}

	@Override
	public void setCurrentPoint(Point p) {
		if (p == null) {
			_currentPoint = null;
		} else {
			_currentPoint = new Point(p);
		}
	}

	@Override
	public Point getCurrentPoint() {
		return (_currentPoint == null) ? null : new Point(_currentPoint);
	}

}
