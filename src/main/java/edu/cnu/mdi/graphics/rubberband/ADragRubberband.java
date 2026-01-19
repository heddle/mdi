package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * Base class for "drag to stretch" rubberband gestures (rectangles, ovals, etc.).
 * <p>
 * Mirrors the old Rubberband "normalMode()" behavior:
 * press -> start, drag -> modify+setCurrent, release -> end.
 * </p>
 */
public abstract class ADragRubberband extends ARubberband {

	protected ADragRubberband(Component component, IRubberbanded rubberbanded, Policy policy) {
		super(component, rubberbanded, policy);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (!isActive()) {
			return;
		}
		startRubberbanding(e.getPoint());
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!isActive()) {
			return;
		}
		Point cp = e.getPoint();
		modifyCurrentPoint(cp);
		setCurrent(cp);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (!isActive()) {
			return;
		}
		endRubberbanding(e.getPoint());
	}
}
