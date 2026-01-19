package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import edu.cnu.mdi.graphics.GraphicsUtils;

public class RubberLine extends ALineClickRubberband {

	public RubberLine(Component component, IRubberbanded rubberbanded) {
		super(component, rubberbanded, Policy.LINE);
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
