package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import edu.cnu.mdi.graphics.GraphicsUtils;

public class RubberRadArc extends AClickRubberband implements IRubberbandAngleProvider {

    // Unwrapped signed sweep (deg). Positive = CCW in math coords (y up).
    private double sweepDeg = 0.0;
    private double lastSignedDeg = 0.0;
    private boolean sweepInit = false;

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
            sweepInit = false;
            return;
        }

        // Second click: first leg endpoint
        if (tempPoly.npoints == 1) {
            addPoint(tempPoly, p.x, p.y);
            sweepInit = false; // start tracking sweep after second click
            return;
        }

        // Third click: SCALE to r1 before storing and ending
        if (tempPoly.npoints == 2) {
            Point scaled = scaleToFirstRadius(p);
            addPoint(tempPoly, scaled.x, scaled.y);
            currentPt.setLocation(scaled);

            // Ensure sweepDeg reflects the final point.
            updateSweepForCurrentPoint(scaled);

            endRubberbanding(scaled);
        }
    }

    @Override
    public double getRubberbandAngleDeg() {
        return sweepDeg;
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

    private void updateSweepForCurrentPoint(Point current) {
        if (tempPoly == null || tempPoly.npoints != 2) return;

        double xc = tempPoly.xpoints[0];
        double yc = tempPoly.ypoints[0];
        double x1 = tempPoly.xpoints[1];
        double y1 = tempPoly.ypoints[1];

        Point scaled = scaleToFirstRadius(current);
        double x2 = scaled.x;
        double y2 = scaled.y;

        // Screen deltas
        double dx1s = x1 - xc;
        double dy1s = y1 - yc;
        double dx2s = x2 - xc;
        double dy2s = y2 - yc;

        double r1 = Math.hypot(dx1s, dy1s);
        double r2 = Math.hypot(dx2s, dy2s);
        if (r1 < 0.99 || r2 < 0.99) return;

        // Convert to math coords (flip Y) so CCW is standard.
        double dx1 = dx1s;
        double dy1 = -dy1s;
        double dx2 = dx2s;
        double dy2 = -dy2s;

        double dot = dx1 * dx2 + dy1 * dy2;
        double cross = dx1 * dy2 - dy1 * dx2;

        double signed = Math.toDegrees(Math.atan2(cross, dot)); // (-180, 180]

        if (!sweepInit) {
            sweepInit = true;
            sweepDeg = signed;
            lastSignedDeg = signed;
            return;
        }

        // Unwrap across the -180/180 discontinuity for continuous dragging.
        double delta = signed - lastSignedDeg;
        if (delta > 180.0) delta -= 360.0;
        if (delta < -180.0) delta += 360.0;

        sweepDeg += delta;
        lastSignedDeg = signed;
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

            double r1 = Math.hypot(dx1, dy1);
            if (r1 < 0.99) return;

            // Update sweep tracking based on current mouse point
            updateSweepForCurrentPoint(currentPt);

            // Start angle for Java2D: keep your original convention
            double sAngle = Math.atan2(-dy1, dx1);
            int startAngle = (int) Math.round(Math.toDegrees(sAngle));

            // Use the unwrapped signed sweep (CW shows small CW sector, not complement)
            int arcAngle = (int) Math.round(sweepDeg);

            int pixrad = (int) r1;
            int size = (int) Math.round(2 * r1);

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
