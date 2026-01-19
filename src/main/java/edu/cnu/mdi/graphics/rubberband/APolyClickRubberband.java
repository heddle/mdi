package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * Click-based multi-point gesture:
 * - single click adds a vertex
 * - double click ends (by default does not add a final point)
 */
public abstract class APolyClickRubberband extends AClickRubberband {

	protected APolyClickRubberband(Component component, IRubberbanded rubberbanded, Policy policy) {
		super(component, rubberbanded, policy);
	}

	/** Minimum required points before accepting end. */
	protected int minPointsToAccept() {
		return 2;
	}

	/**
	 * Whether the double-click should also add a final point.
	 * Old monolith behavior typically ended without adding.
	 */
	protected boolean addPointOnDoubleClick() {
		return false;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (!isActive()) return;

		Point p = e.getPoint();

		if (tempPoly == null) {
			if (!ensureStarted(p)) {
				return;
			}
			return;
		}

		if (e.getClickCount() == 2) {
			if (addPointOnDoubleClick()) {
				addPoint(tempPoly, p.x, p.y);
			}
			if (tempPoly.npoints >= minPointsToAccept()) {
				endRubberbanding(p);
			} else {
				endRubberbanding(null);
			}
			return;
		}

		addPoint(tempPoly, p.x, p.y);
	}
}
