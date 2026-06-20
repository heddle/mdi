package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import edu.cnu.mdi.graphics.GraphicsUtils;

/**
 * Rubberband "XONLY" policy: a vertical band spanning the full component height,
 * with x determined by the drag.
 */
public class RubberXOnly extends ADragRubberband {

	public RubberXOnly(Component component, IRubberbanded rubberbanded) {
		super(component, rubberbanded, Policy.XONLY);
	}

	@Override
	protected void startRubberbanding(Point anchorPt) {
		super.startRubberbanding(anchorPt);

		Rectangle b = getLocalBounds();

		// Preserve the clicked x anchor, but force the y anchor to the top.
		startPt.y = b.y;

		// Also initialize current y consistently.
		currentPt.y = b.y + b.height - 1;
	}

	@Override
	protected void modifyCurrentPoint(Point cp) {
		super.modifyCurrentPoint(cp);

		Rectangle b = getLocalBounds();

		// Force the band to the bottom.
		cp.y = b.y + b.height - 1;
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

		int x = Math.min(currentPt.x, startPt.x);
		int w = Math.abs(currentPt.x - startPt.x);

		return new Rectangle(
				x,
				b.y,
				w,
				b.height - 1);
	}

	// Get the bounds of the component in local coordinates.
	private Rectangle getLocalBounds() {
		return new Rectangle(0, 0, component.getWidth(), component.getHeight());
	}
}