package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import edu.cnu.mdi.graphics.GraphicsUtils;

/**
 * Rubberband "XONLY" policy: a vertical band spanning the full component height,
 * with x determined by the drag.
 *
 * This mirrors the old Rubberband behavior:
 * - anchor y is set to component top on start
 * - current y is forced to component bottom during drag
 */
public class RubberXOnly extends ADragRubberband {

	public RubberXOnly(Component component, IRubberbanded rubberbanded) {
		super(component, rubberbanded, Policy.XONLY);
	}

	@Override
	protected void modifyCurrentPoint(Point cp) {
		super.modifyCurrentPoint(cp); // clamp x/y first

		Rectangle b = component.getBounds();
		// Force the band to full height.
		cp.y = b.y + b.height - 1;

		// no need to re-clamp x; it was clamped above
	}

	@Override
	protected void draw(Graphics2D g) {
		Rectangle rect = getRubberbandBounds();
		g.fillRect(rect.x, rect.y, rect.width, rect.height);
		GraphicsUtils.drawHighlightedRectangle(g, rect, highlightColor1, highlightColor2);
	}

	@Override
	public Rectangle getRubberbandBounds() {
		return new Rectangle(
				(currentPt.x < startPt.x) ? currentPt.x : startPt.x,
				(currentPt.y < startPt.y) ? currentPt.y : startPt.y,
				Math.abs(currentPt.x - startPt.x),
				Math.abs(currentPt.y - startPt.y));
	}
}
