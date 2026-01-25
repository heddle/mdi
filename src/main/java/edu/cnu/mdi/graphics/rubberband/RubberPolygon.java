package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import edu.cnu.mdi.graphics.GraphicsUtils;

public class RubberPolygon extends APolyClickRubberband {

	public RubberPolygon(Component component, IRubberbanded rubberbanded) {
		super(component, rubberbanded, Policy.POLYGON);
	}

	@Override
	protected int minPointsToAccept() {
		return 3;
	}

	@Override
	public boolean isGestureValid(int minSizePx) {
		if (poly == null || poly.npoints < 3) {
			return false;
		}
		Rectangle b = poly.getBounds();
		return Math.max(b.width, b.height) >= minSizePx;
	}

	@Override
	protected Point[] computeVertices() {
		Polygon p = (poly != null) ? poly : tempPoly;
		if (p == null) {
			return null;
		}

		Point[] pts = new Point[p.npoints];
		for (int i = 0; i < p.npoints; i++) {
			pts[i] = new Point(p.xpoints[i], p.ypoints[i]);
		}
		return pts;
	}

	@Override
	protected void draw(Graphics2D g) {
		if (tempPoly == null || tempPoly.npoints < 1) {
			return;
		}

		Polygon tpoly = new Polygon(tempPoly.xpoints, tempPoly.ypoints, tempPoly.npoints);
		addPoint(tpoly, currentPt.x, currentPt.y);

		g.fillPolygon(tpoly);
		GraphicsUtils.drawHighlightedShape(g, tpoly, highlightColor1, highlightColor2);
	}

	@Override
	public Rectangle getRubberbandBounds() {
		return (poly != null) ? poly.getBounds() : (tempPoly != null ? tempPoly.getBounds() : null);
	}
}
