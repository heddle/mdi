package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import edu.cnu.mdi.graphics.GraphicsUtils;

public class RubberOval extends ADragRubberband {

	public RubberOval(Component component, IRubberbanded rubberbanded) {
		super(component, rubberbanded, Policy.OVAL);
	}

	@Override
	protected void draw(Graphics2D g) {
		Rectangle rect = getRubberbandBounds();
		g.fillOval(rect.x, rect.y, rect.width, rect.height);
		GraphicsUtils.drawHighlightedOval(g, rect, highlightColor1, highlightColor2);
	}

	@Override
	public Rectangle getRubberbandBounds() {
		return new Rectangle((currentPt.x < startPt.x) ? currentPt.x : startPt.x,
				(currentPt.y < startPt.y) ? currentPt.y : startPt.y,
				Math.abs(currentPt.x - startPt.x),
				Math.abs(currentPt.y - startPt.y));
	}
}
