package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * Click-based 2-point line gesture:
 * - first click anchors
 * - second click ends
 * Approval hooks allow TWO_CLICK_LINE behavior without policy switches.
 */
public abstract class ALineClickRubberband extends AClickRubberband {

	protected ALineClickRubberband(Component component, IRubberbanded rubberbanded, Policy policy) {
		super(component, rubberbanded, policy);
	}

	/** Hook for approval (e.g., TWO_CLICK_LINE). Default accepts. */
	protected boolean approvePoint(Point p, boolean isFirstPoint) {
		return true;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (!isActive()) return;

		Point p = e.getPoint();

		// First click
		if (tempPoly == null) {
			if (!approvePoint(p, true)) {
				endRubberbanding(null);
				return;
			}
			if (!ensureStarted(p)) {
				return;
			}
			return;
		}

		// Second click
		if (!approvePoint(p, false)) {
			endRubberbanding(null);
			return;
		}

		addPoint(tempPoly, p.x, p.y);
		if (tempPoly.npoints >= 2) {
			endRubberbanding(p);
		}
	}
}
