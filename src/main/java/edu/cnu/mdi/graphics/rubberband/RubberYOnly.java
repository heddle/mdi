package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import edu.cnu.mdi.graphics.GraphicsUtils;

/**
 * Rubberband "YONLY" policy: a horizontal band spanning the full component width,
 * with y determined by the drag.
 */
public class RubberYOnly extends ADragRubberband {

	public RubberYOnly(Component component, IRubberbanded rubberbanded) {
		super(component, rubberbanded, Policy.YONLY);
	}

	@Override
	protected void startRubberbanding(Point anchorPt) {
		super.startRubberbanding(anchorPt);

		Rectangle b = getLocalBounds();

		// Preserve clicked y anchor, but force x anchor to the left.
		startPt.x = b.x;

		// Initialize current x consistently at the right.
		currentPt.x = b.x + b.width - 1;
	}

	@Override
	protected void modifyCurrentPoint(Point cp) {
		super.modifyCurrentPoint(cp);

		Rectangle b = getLocalBounds();

		// Force the band to full width.
		cp.x = b.x + b.width - 1;
	}

	@Override
	protected void draw(Graphics2D g) {
		Rectangle rect = getRubberbandBounds();
		g.fillRect(rect.x, rect.y, rect.width, rect.height);
		GraphicsUtils.drawHighlightedRectangle(g, rect, highlightColor1, highlightColor2);
	}

	@Override
	public Rectangle getRubberbandBounds() {
		Rectangle b = getLocalBounds();

		int y = Math.min(currentPt.y, startPt.y);
		int h = Math.abs(currentPt.y - startPt.y);

		return new Rectangle(
				b.x,
				y,
				b.width - 1,
				h);
	}

	private Rectangle getLocalBounds() {
		return new Rectangle(0, 0, component.getWidth(), component.getHeight());
	}
}