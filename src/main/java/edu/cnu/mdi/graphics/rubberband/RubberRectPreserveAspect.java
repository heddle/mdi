package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import edu.cnu.mdi.graphics.GraphicsUtils;

/**
 * Rubberband rectangle that preserves aspect ratio (square in screen space).
 * Matches the old Rubberband Policy.RECTANGLE_PRESERVE_ASPECT behavior.
 */
public class RubberRectPreserveAspect extends ADragRubberband {

	public RubberRectPreserveAspect(Component component, IRubberbanded rubberbanded) {
		super(component, rubberbanded, Policy.RECTANGLE_PRESERVE_ASPECT);
	}

	@Override
	protected void modifyCurrentPoint(Point cp) {
		super.modifyCurrentPoint(cp); // clamp first

		int dx = cp.x - startPt.x;
		int dy = cp.y - startPt.y;

		int adx = Math.abs(dx);
		int ady = Math.abs(dy);

		// Make |dx| == |dy| while preserving the quadrant.
		if (adx >= ady) {
			// Drive y from x
			int sy = (dy < 0) ? -1 : 1;
			cp.y = startPt.y + sy * adx;
		} else {
			// Drive x from y
			int sx = (dx < 0) ? -1 : 1;
			cp.x = startPt.x + sx * ady;
		}

		// Re-clamp after adjustment (important when the square would extend off component)
		super.modifyCurrentPoint(cp);
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
