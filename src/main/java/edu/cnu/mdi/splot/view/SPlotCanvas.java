package edu.cnu.mdi.splot.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

import javax.swing.JComponent;

import edu.cnu.mdi.splot.model.BoundsPolicy;
import edu.cnu.mdi.splot.model.Plot2D;

/**
 * Canvas component that renders a {@link Plot2D} using {@link Plot2DRenderer}
 * and provides plot interactions (mouse location feedback, rubberband zoom,
 * recenter, legend dragging).
 *
 * <p>This is intentionally a view/controller class; the data model remains
 * in {@code edu.cnu.mdi.splot.model}.</p>
 */
@SuppressWarnings("serial")
public class SPlotCanvas extends JComponent implements MouseListener, MouseMotionListener {

    private final Plot2D plot;
    private final Plot2DRenderer renderer;

    // Active drawing bounds inside the component (plot area)
    private final Rectangle activeBounds = new Rectangle();

    // Transforms for mapping between pixels and world/view coordinates
    private AffineTransform localToWorld;
    private AffineTransform worldToLocal;

    // Legend drag state (screen/pixel space)
    private final LegendPlacement legendPlacement = new LegendPlacement();

    // Feedback string like your old status line
    private String locationString = " ";

    // Interaction mode (keep simple; hook into mdi toolbar later)
    public enum Mode { POINTER, BOX_ZOOM, CENTER }
    private Mode mode = Mode.POINTER;

    // Rubberband rectangle in pixel coordinates (simple internal rubberband)
    private Rectangle rubberband;
    private Point rubberbandStart;

    public SPlotCanvas(Plot2D plot, Plot2DRenderer renderer) {
        this.plot = Objects.requireNonNull(plot, "plot");
        this.renderer = Objects.requireNonNull(renderer, "renderer");

        setOpaque(true);

        addMouseListener(this);
        addMouseMotionListener(this);

        // reasonable default legend placement (top-right inside plot area)
        legendPlacement.anchorX = 0.98;
        legendPlacement.anchorY = 0.05;
    }

    /** Set current interaction mode (e.g., driven by a toolbar). */
    public void setMode(Mode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    /** @return HTML-ish or plain location string (caller decides how to show it). */
    public String getLocationString() {
        return locationString;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        updateActiveBounds();
        updateTransforms();

        Graphics2D g2 = (Graphics2D) g;

        // Provide legend placement to renderer (simple setter you add)
        renderer.setLegendPlacement(legendPlacement);

        // Render plot
        renderer.render(g2, plot, new Rectangle2D.Double(0, 0, getWidth(), getHeight()));

        // Optional: draw rubberband (if active)
        if (rubberband != null) {
            g2.draw(rubberband);
        }
    }

    private void updateActiveBounds() {
        Rectangle pa = renderer.getLastPlotArea();
        if (pa != null) {
            activeBounds.setBounds(pa);
        } else {
            // first paint hasn't happened yet; fall back to renderer margins:
            activeBounds.setBounds(renderer.getPlotArea(getBounds()));
        }
    }

    private void updateTransforms() {
        Rectangle2D.Double view = renderer.getEffectiveViewBounds(plot);
        if (view == null || !(view.width > 0) || !(view.height > 0)) {
            localToWorld = null;
            worldToLocal = null;
            return;
        }

        double scaleX = view.width / activeBounds.width;
        double scaleY = view.height / activeBounds.height;

        localToWorld = AffineTransform.getTranslateInstance(view.x, view.getMaxY());
        localToWorld.concatenate(AffineTransform.getScaleInstance(scaleX, -scaleY));
        localToWorld.concatenate(AffineTransform.getTranslateInstance(-activeBounds.x, -activeBounds.y));

        try {
            worldToLocal = localToWorld.createInverse();
        } catch (NoninvertibleTransformException e) {
            worldToLocal = null;
        }
    }

    private boolean inActiveBounds(Point p) {
        return activeBounds.contains(p);
    }

    private void localToWorld(Point p, Point2D.Double out) {
        if (localToWorld != null) {
            localToWorld.transform(p, out);
        }
    }

    private Rectangle2D.Double localToWorld(Rectangle r) {
        if (localToWorld == null) {
            return new Rectangle2D.Double(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }

        Point2D.Double w0 = new Point2D.Double();
        Point2D.Double w1 = new Point2D.Double();

        localToWorld.transform(new Point(r.x, r.y), w0);
        localToWorld.transform(new Point(r.x + r.width, r.y + r.height), w1);

        double xmin = Math.min(w0.x, w1.x);
        double xmax = Math.max(w0.x, w1.x);
        double ymin = Math.min(w0.y, w1.y);
        double ymax = Math.max(w0.y, w1.y);

        return new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    /** Zoom about center by a factor (e.g., 0.85 zoom-in, 1/0.85 zoom-out). */
    public void scale(double factor) {
    	Rectangle2D.Double v = renderer.getEffectiveViewBounds(plot);
        double cx = v.getCenterX();
        double cy = v.getCenterY();
        double w = v.width * factor;
        double h = v.height * factor;

        plot.setBoundsPolicy(BoundsPolicy.MANUAL);
        plot.setViewBounds(new Rectangle2D.Double(cx - w / 2, cy - h / 2, w, h));
        repaint();
    }

    /** Recenter at a clicked point without changing zoom. */
    public void recenterAt(Point click) {
        Point2D.Double wp = new Point2D.Double();
        localToWorld(click, wp);

        Rectangle2D.Double v = renderer.getEffectiveViewBounds(plot);
        plot.setBoundsPolicy(BoundsPolicy.MANUAL);
        plot.setViewBounds(new Rectangle2D.Double(wp.x - v.width / 2, wp.y - v.height / 2, v.width, v.height));
        repaint();
    }

    // ---------------- Mouse handling ----------------

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!inActiveBounds(e.getPoint()) || localToWorld == null) {
            locationString = " ";
            return;
        }

        Point2D.Double wp = new Point2D.Double();
        localToWorld(e.getPoint(), wp);

        // Base coordinates
        String base = String.format("(x, y) = (%7.3g, %7.3g)", wp.x, wp.y);

        // Try curve hit-test
        HoverInfo h = renderer.hitTest(plot, e.getPoint(), 8);

        if (h != null) {
            if (h.fit != null) {
                locationString = base + "   " + h.curveName + "   " +
                        h.fit.formatSummaryOneLine();
            } else {
                locationString = base + "   " + h.curveName +
                        String.format("  [%.3g, %.3g]", h.x, h.y);
            }
        } else {
            locationString = base;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (mode == Mode.POINTER) {

            // Right-click → fit details
            if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                HoverInfo h = renderer.hitTest(plot, e.getPoint(), 8);
                if (h != null && h.fit != null) {
                    new FitResultDialog(
                            (javax.swing.JFrame)
                                    javax.swing.SwingUtilities.getWindowAncestor(this),
                            h.fit
                    ).setVisible(true);
                    return;
                }
            }

            // Legend drag prime
            renderer.beginLegendDrag(e.getPoint(), legendPlacement);
        }
        else if (mode == Mode.BOX_ZOOM) {
            rubberbandStart = e.getPoint();
            rubberband = new Rectangle(rubberbandStart);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (mode == Mode.POINTER) {
            if (renderer.isLegendDragging()) {
            	renderer.dragLegend(e.getPoint(), legendPlacement);
            	repaint();
           }
        }
        else if (mode == Mode.BOX_ZOOM && rubberband != null && rubberbandStart != null) {
            int x = Math.min(rubberbandStart.x, e.getX());
            int y = Math.min(rubberbandStart.y, e.getY());
            int w = Math.abs(e.getX() - rubberbandStart.x);
            int h = Math.abs(e.getY() - rubberbandStart.y);
            rubberband.setBounds(x, y, w, h);
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (mode == Mode.POINTER) {
            renderer.endLegendDrag();
        }
        else if (mode == Mode.BOX_ZOOM && rubberband != null) {
            if (rubberband.width >= 10 && rubberband.height >= 10) {
                Rectangle clipped = rubberband.intersection(activeBounds);
                Rectangle2D.Double worldRect = localToWorld(clipped);

                plot.setBoundsPolicy(BoundsPolicy.MANUAL);
                plot.setViewBounds(worldRect);
            }
            rubberband = null;
            rubberbandStart = null;
            repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (mode == Mode.CENTER && inActiveBounds(e.getPoint())) {
            recenterAt(e.getPoint());
        }
    }

    // unused
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}
