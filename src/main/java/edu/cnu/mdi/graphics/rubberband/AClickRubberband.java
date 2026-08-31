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
	 * Ensure the gesture has started and {@code tempPoly} exists, seeded with
	 * the start point.
	 * <p>
	 * A no-op (returning {@code true} immediately) if {@code tempPoly} is
	 * already non-null. Otherwise delegates to
	 * {@link ARubberband#startRubberbanding(Point)}, which always sets
	 * {@code started = true} for a non-null point (it has no rejection path),
	 * so this method currently always returns {@code true}. The
	 * {@code boolean} return is kept as a defensive contract in case a future
	 * {@code startRubberbanding} override introduces a genuine rejection path.
	 * </p>
	 *
	 * @param p the point to seed the gesture with; must not be {@code null}
	 * @return {@code true} once the gesture has started and {@code tempPoly}
	 *         is non-null
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