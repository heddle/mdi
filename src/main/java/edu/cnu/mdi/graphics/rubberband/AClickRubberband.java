package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseEvent;

/**
 * Minimal base for click-collected gestures.
 * No policy switches; subclasses decide how to accumulate/end.
 */
public abstract class AClickRubberband extends ARubberband {

	protected AClickRubberband(Component component, IRubberbanded rubberbanded, Policy policy) {
		super(component, rubberbanded, policy);
	}

	@Override
	public final boolean isClickBased() {
		return true;
	}

	/**
	 * Ensure started + tempPoly exists and contains the start point.
	 * Returns false if start was rejected/cancelled.
	 */
	protected final boolean ensureStarted(Point p) {
		if (tempPoly != null) {
			return true;
		}

		startRubberbanding(p);
		if (!started) {
			return false;
		}

		tempPoly = new Polygon();
		addPoint(tempPoly, startPt.x, startPt.y);
		return true;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (!isActive()) {
			return;
		}
		if (tempPoly != null) {
			setCurrent(e.getPoint());
		}
	}
}