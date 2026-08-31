package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import edu.cnu.mdi.graphics.GraphicsUtils;

public class RubberLine extends ALineClickRubberband {

	public RubberLine(Component component, IRubberbanded rubberbanded) {
		this(component, rubberbanded, Policy.LINE);
	}

	/**
	 * Constructor for subclasses (e.g. {@link RubberTwoClickLine}) that share
	 * this class's line-gesture behavior but must be registered under a
	 * distinct {@link Policy} so {@code isClickBased()}/{@code policy}-aware
	 * callers see the correct policy for the instance.
	 *
	 * @param component    the component this rubberband operates on
	 * @param rubberbanded the completion callback
	 * @param policy       the policy this instance reports
	 */
	protected RubberLine(Component component, IRubberbanded rubberbanded, Policy policy) {
		super(component, rubberbanded, policy);
	}

	@Override
	public boolean isGestureValid(int minSizePx) {
		int dx = currentPt.x - startPt.x;
		int dy = currentPt.y - startPt.y;
		return dx * dx + dy * dy >= minSizePx * minSizePx;
	}

	@Override
	protected Point[] computeVertices() {
		return new Point[] {
				new Point(startPt.x, startPt.y),
				new Point(currentPt.x, currentPt.y)
		};
	}

	@Override
	protected void draw(Graphics2D g) {
		GraphicsUtils.drawHighlightedLine(g,
				startPt.x, startPt.y,
				currentPt.x, currentPt.y,
				highlightColor1, highlightColor2);
	}

	@Override
	public Rectangle getRubberbandBounds() {
		int x = Math.min(startPt.x, currentPt.x);
		int y = Math.min(startPt.y, currentPt.y);
		int w = Math.abs(currentPt.x - startPt.x);
		int h = Math.abs(currentPt.y - startPt.y);
		return new Rectangle(x, y, w, h);
	}
}
