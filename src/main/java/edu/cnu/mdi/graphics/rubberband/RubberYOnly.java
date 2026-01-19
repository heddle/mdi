package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import edu.cnu.mdi.graphics.GraphicsUtils;

/**
 * Rubberband "YONLY" policy: a horizontal band spanning the full component width,
 * with y determined by the drag.
 *
 * This mirrors the old Rubberband behavior:
 * - anchor x is set to component left on start
 * - current x is forced to component right during drag
 */
public class RubberYOnly extends ADragRubberband {

	public RubberYOnly(Component component, IRubberbanded rubberbanded) {
		super(component, rubberbanded, Policy.YONLY);
	}

	@Override
	protected void modifyCurrentPoint(Point cp) {
		super.modifyCurrentPoint(cp); // clamp x/y first

		Rectangle b = component.getBounds();
		// Force the band to full width.
		cp.x = b.x + b.width - 1;

		// no need to re-clamp y; it was clamped above
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
