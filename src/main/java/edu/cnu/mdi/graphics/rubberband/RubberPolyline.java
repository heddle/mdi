package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import edu.cnu.mdi.graphics.GraphicsUtils;

public class RubberPolyline extends APolyClickRubberband {

	public RubberPolyline(Component component, IRubberbanded rubberbanded) {
		super(component, rubberbanded, Policy.POLYLINE);
	}

	@Override
	protected int minPointsToAccept() {
		return 2;
	}

	@Override
	public boolean isGestureValid(int minSizePx) {
		if (poly == null || poly.npoints < 2) return false;
		Rectangle b = poly.getBounds();
		return Math.max(b.width, b.height) >= minSizePx;
	}

	@Override
	protected Point[] computeVertices() {
		Polygon p = (poly != null) ? poly : tempPoly;
		if (p == null) return null;

		Point[] pts = new Point[p.npoints];
		for (int i = 0; i < p.npoints; i++) {
			pts[i] = new Point(p.xpoints[i], p.ypoints[i]);
		}
		return pts;
	}

	@Override
	protected void draw(Graphics2D g) {
		if (tempPoly == null || tempPoly.npoints < 1) return;

		Polygon tpoly = new Polygon(tempPoly.xpoints, tempPoly.ypoints, tempPoly.npoints);
		addPoint(tpoly, currentPt.x, currentPt.y);

		GraphicsUtils.drawHighlightedPolyline(g,
				tpoly.xpoints, tpoly.ypoints, tpoly.npoints,
				highlightColor1, highlightColor2);
	}

	@Override
	public Rectangle getRubberbandBounds() {
		return (poly != null) ? poly.getBounds() : (tempPoly != null ? tempPoly.getBounds() : null);
	}
}
