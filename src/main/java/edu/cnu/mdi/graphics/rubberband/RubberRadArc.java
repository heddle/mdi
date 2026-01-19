package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import edu.cnu.mdi.graphics.GraphicsUtils;

public class RubberRadArc extends AClickRubberband {

	public RubberRadArc(Component component, IRubberbanded rubberbanded) {
		super(component, rubberbanded, Policy.RADARC);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (!isActive()) return;

		final Point p = e.getPoint();

		// First click: center
		if (tempPoly == null) {
			if (!ensureStarted(p)) return;
			return;
		}

		// Second click: first leg endpoint
		if (tempPoly.npoints == 1) {
			addPoint(tempPoly, p.x, p.y);
			return;
		}

		// Third click: SCALE to r1 before storing and ending
		if (tempPoly.npoints == 2) {
			Point scaled = scaleToFirstRadius(p);
			addPoint(tempPoly, scaled.x, scaled.y);
			currentPt.setLocation(scaled);
			endRubberbanding(scaled);
		}
	}

	private Point scaleToFirstRadius(Point p) {
		double xc = tempPoly.xpoints[0];
		double yc = tempPoly.ypoints[0];
		double x1 = tempPoly.xpoints[1];
		double y1 = tempPoly.ypoints[1];

		double dx1 = x1 - xc;
		double dy1 = y1 - yc;
		double r1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);

		double dx2 = p.x - xc;
		double dy2 = p.y - yc;
		double r2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);

		if (r1 < 0.99 || r2 < 0.99) {
			return new Point(p);
		}

		double scale = r1 / r2;
		double xs = xc + dx2 * scale;
		double ys = yc + dy2 * scale;

		return new Point((int) Math.round(xs), (int) Math.round(ys));
	}

	@Override
	public boolean isGestureValid(int minSizePx) {
		if (poly == null || poly.npoints < 3) return false;
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

		if (tempPoly.npoints == 1) {
			GraphicsUtils.drawHighlightedLine(g,
					tempPoly.xpoints[0], tempPoly.ypoints[0],
					currentPt.x, currentPt.y,
					highlightColor1, highlightColor2);
			return;
		}

		if (tempPoly.npoints == 2) {

			double xc = tempPoly.xpoints[0];
			double yc = tempPoly.ypoints[0];
			double x1 = tempPoly.xpoints[1];
			double y1 = tempPoly.ypoints[1];

			Point scaled = scaleToFirstRadius(currentPt);
			double x2 = scaled.x;
			double y2 = scaled.y;

			double dx1 = x1 - xc;
			double dy1 = y1 - yc;
			double dx2 = x2 - xc;
			double dy2 = y2 - yc;

			double r1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
			double r2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);

			if (r1 < 0.99 || r2 < 0.99) return;

			double sAngle = Math.atan2(-dy1, dx1);

			double aAngle = Math.acos((dx1 * dx2 + dy1 * dy2) / (r1 * r2));
			if ((dx1 * dy2 - dx2 * dy1) > 0.0) {
				aAngle = -aAngle;
			}

			int startAngle = (int) Math.toDegrees(sAngle);
			int arcAngle = (int) Math.toDegrees(aAngle);

			int pixrad = (int) r1;
			int size = (int) (2 * r1);

			g.fillArc((int) xc - pixrad, (int) yc - pixrad, size, size, startAngle, arcAngle);
			GraphicsUtils.drawHighlightedArc(g,
					(int) xc - pixrad, (int) yc - pixrad, size, size,
					startAngle, arcAngle,
					highlightColor1, highlightColor2);

			GraphicsUtils.drawHighlightedLine(g,
					(int) xc, (int) yc, (int) x1, (int) y1,
					highlightColor1, highlightColor2);
			GraphicsUtils.drawHighlightedLine(g,
					(int) xc, (int) yc, (int) x2, (int) y2,
					highlightColor1, highlightColor2);
		}
	}

	@Override
	public Rectangle getRubberbandBounds() {
		return (poly != null) ? poly.getBounds() : (tempPoly != null ? tempPoly.getBounds() : null);
	}
}
