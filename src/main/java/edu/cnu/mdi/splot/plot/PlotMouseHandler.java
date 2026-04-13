package edu.cnu.mdi.splot.plot;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;

import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.Histo2DData;
import edu.cnu.mdi.splot.pdata.HistoCurve;
import edu.cnu.mdi.splot.pdata.HistoData;
import edu.cnu.mdi.splot.pdata.HistoDrawingUtils;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.Snapshot;
import edu.cnu.mdi.util.UnicodeUtils;

/**
 * Handles all mouse interaction for a {@link PlotCanvas}.
 *
 * <p>This class was extracted from {@code PlotCanvas} to keep that class
 * focused on coordinate geometry, painting, and data management. All
 * {@link MouseListener}, {@link MouseMotionListener}, and
 * {@link MouseWheelListener} logic lives here.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Legend and extra-text dragging ({@link #mousePressed},
 *       {@link #mouseDragged}, {@link #mouseReleased})</li>
 *   <li>Mouse-move feedback: coordinate readout, histogram bin info, XY curve
 *       summaries, bar-plot hit testing, 2D histogram bin info
 *       ({@link #mouseMoved})</li>
 *   <li>Scroll-wheel zoom ({@link #mouseWheelMoved})</li>
 * </ul>
 *
 * <h3>Canvas coupling</h3>
 * <p>The handler holds a reference back to its owning {@code PlotCanvas} for
 * coordinate transforms, data access, and repaint requests. It does
 * <em>not</em> hold any state that could not be reconstructed from the
 * canvas.</p>
 *
 * @author heddle (original), refactored for extraction
 * @see PlotCanvas
 */
public class PlotMouseHandler
        implements MouseListener, MouseMotionListener, MouseWheelListener {

	/** The canvas this handler serves. */
	private final PlotCanvas canvas;

	/**
	 * Creates a {@code PlotMouseHandler} bound to the given canvas.
	 *
	 * @param canvas the owning {@link PlotCanvas} (non-null)
	 */
	public PlotMouseHandler(PlotCanvas canvas) {
		if (canvas == null) {
			throw new IllegalArgumentException("canvas must not be null");
		}
		this.canvas = canvas;
	}

	// -----------------------------------------------------------------------
	// MouseMotionListener
	// -----------------------------------------------------------------------

	/**
	 * Handles drag events: moves the legend or the extra-text label if either
	 * is in the "dragging" state.
	 *
	 * @param e the mouse event
	 */
	@Override
	public void mouseDragged(MouseEvent e) {
		Legend    legend = canvas.getLegend();
		ExtraText extra  = canvas.getExtra();

		if (legend.isDraggingPrimed()) {
			legend.setDragging(true);
		}
		if (legend.isDragging()) {
			int dx = e.getX() - legend.getCurrentPoint().x;
			int dy = e.getY() - legend.getCurrentPoint().y;
			legend.x += dx;
			legend.y += dy;
			legend.setCurrentPoint(e.getPoint());
			canvas.repaint();
		}

		if (extra.isDraggingPrimed()) {
			extra.setDragging(true);
		}
		if (extra.isDragging()) {
			int dx = e.getX() - extra.getCurrentPoint().x;
			int dy = e.getY() - extra.getCurrentPoint().y;
			extra.x += dx;
			extra.y += dy;
			extra.setCurrentPoint(e.getPoint());
			canvas.repaint();
		}
	}

	/**
	 * Handles mouse-move events, updating the feedback pane with coordinate
	 * readouts and curve-specific information.
	 *
	 * <p>Delegates to specialised private helpers depending on the current
	 * data type (2D histogram, bar plot, XY curves, 1D histogram).</p>
	 *
	 * @param e the mouse event
	 */
	@Override
	public void mouseMoved(MouseEvent e) {
		PlotData plotData = canvas.getPlotData();
		if (plotData == null) {
			return;
		}

		PlotPanel plotPanel = canvas.plotPanel();
		if (plotPanel == null) {
			return;
		}

		FeedbackPane feedback = plotPanel.getFeedbackPane();
		if (canvas.getActiveBounds() == null || canvas.getWorld() == null) {
			return;
		}

		feedback.clear();

		Point            pp        = e.getPoint();
		Point2D.Double   dataPoint = new Point2D.Double();
		canvas.screenToData(pp, dataPoint);

		// 2D histogram — handled separately, returns immediately.
		if (plotData.isHisto2DData()) {
			Histo2DData h2d = plotData.getHisto2DData();
			if (h2d != null) {
				histo2DFeedback(h2d, dataPoint, feedback);
			}
			return;
		}

		feedback.append(String.format("(x, y) = (%7.2g, %-7.2g)", dataPoint.x, dataPoint.y));

		if (plotData.isXYData()) {
			if (canvas.isBarPlot()) {
				barPlotFeedback(dataPoint, feedback, pp);
				return;
			}
			for (ACurve curve : plotData.getVisibleCurves()) {
				String cStr = String.format("%s: %d points ", curve.name(), curve.length());
				String fitSummary = curve.getFitSummary();
				if (fitSummary != null) {
					cStr += " " + fitSummary;
				}
				feedback.append(cStr);
			}
		}

		if (plotData.isHistoData()) {
			for (ACurve curve : plotData.getVisibleCurves()) {
				HistoCurve hc   = (HistoCurve) curve;
				HistoData  hd   = hc.getHistoData();
				String     cStr = HistoDrawingUtils.statusString(canvas, hd, pp, dataPoint);
				String fitSummary = curve.getFitSummary();
				if (fitSummary != null) {
					cStr += " " + fitSummary;
				}
				feedback.append(cStr);
			}
		}
	}

	// -----------------------------------------------------------------------
	// MouseListener
	// -----------------------------------------------------------------------

	/**
	 * Primes the legend or extra-text label for dragging when the pointer tool
	 * is active and the mouse is pressed inside one of them.
	 *
	 * @param e the mouse event
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		Legend    legend = canvas.getLegend();
		ExtraText extra  = canvas.getExtra();
		PlotParameters params = canvas.getParameters();

		if (canvas.isPointer() && params.isLegendDrawn() && legend.contains(e.getPoint())) {
			legend.setDraggingPrimed(true);
			legend.setCurrentPoint(e.getPoint());
		} else if (canvas.isPointer() && params.extraDrawing() && extra.contains(e.getPoint())) {
			extra.setDraggingPrimed(true);
			extra.setCurrentPoint(e.getPoint());
		}
	}

	/**
	 * Ends any active drag on mouse release.
	 *
	 * @param e the mouse event
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		Legend    legend = canvas.getLegend();
		ExtraText extra  = canvas.getExtra();

		legend.setDragging(false);
		legend.setDraggingPrimed(false);
		legend.setCurrentPoint(null);

		extra.setDragging(false);
		extra.setDraggingPrimed(false);
		extra.setCurrentPoint(null);
	}

	/** No action. */
	@Override
	public void mouseClicked(MouseEvent e) {}

	/** No action. */
	@Override
	public void mouseEntered(MouseEvent e) {}

	/** No action. */
	@Override
	public void mouseExited(MouseEvent e) {}

	// -----------------------------------------------------------------------
	// MouseWheelListener
	// -----------------------------------------------------------------------

	/**
	 * Zooms the canvas in or out using the scroll wheel.
	 *
	 * <p>Base zoom factor is {@code 1.12} per notch. Holding Ctrl/Meta gives
	 * finer control ({@code 1.04}); holding Shift gives coarser control
	 * ({@code 1.20}). The computed factor is clamped to {@code [0.2, 5.0]}
	 * as a safety net against runaway zooming.</p>
	 *
	 * @param e the mouse-wheel event
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		double r    = e.getPreciseWheelRotation();
		double base = 1.12;
		if (e.isControlDown() || e.isMetaDown()) {
			base = 1.04;
		} else if (e.isShiftDown()) {
			base = 1.20;
		}

		double factor = Math.pow(base, r);
		factor = Math.max(0.2, Math.min(5.0, factor));

		canvas.scale(factor);
		e.consume();
	}

	// -----------------------------------------------------------------------
	// Private feedback helpers
	// -----------------------------------------------------------------------

	/**
	 * Populates the feedback pane for a bar plot: identifies the bar closest to
	 * the mouse and reports its category and height.
	 *
	 * @param dataPoint current mouse position in data coordinates
	 * @param feedback  the pane to append text to
	 * @param pp        current mouse position in screen coordinates (unused
	 *                  here but kept for symmetry with histogram helper)
	 */
	private void barPlotFeedback(Point2D.Double dataPoint,
	                             FeedbackPane feedback,
	                             Point pp) {
		PlotData plotData = canvas.getPlotData();
		double   mouseX   = dataPoint.x;
		double   closestBarX  = Double.NaN;
		Curve    closestCurve = null;
		double   minDist  = Double.POSITIVE_INFINITY;

		for (ACurve curve : plotData.getVisibleCurves()) {
			Snapshot s    = curve.snapshot();
			double[] xArr = s.x;
			double[] yArr = s.y;
			if (xArr == null || yArr == null) {
				continue;
			}

			for (double xVal : xArr) {
				double dist = Math.abs(xVal - mouseX);
				if (dist < minDist) {
					minDist      = dist;
					closestBarX  = xVal;
					closestCurve = (Curve) curve;
				}
			}
		}

		if ((closestCurve == null) || closestCurve.yData().size() < 2) {
			return;
		}

		if (!Double.isNaN(closestBarX)) {
			Snapshot s = closestCurve.snapshot();
			String name = closestCurve.name();
			// Height is the span of the y-data for this bar entry.
			
			double height = closestCurve.yData().get(1) - closestCurve.yData().get(0);
			for (double xVal : s.x) {
				if (xVal == closestBarX) {
					feedback.append(String.format(
							"Category %s at x=%.2f has height %.4f",
							name, closestBarX, height));
					break;
				}
			}
		}
	}

	/**
	 * Populates the feedback pane for a 2D histogram: reports bin coordinates,
	 * count, log-Z (if enabled), Z/Zmax percentage, percentile, and local
	 * 3×3 mean.
	 *
	 * @param h2d       the 2D histogram data
	 * @param dataPoint current mouse position in data coordinates
	 * @param feedback  the pane to append text to
	 */
	private void histo2DFeedback(Histo2DData h2d,
	                             Point2D.Double dataPoint,
	                             FeedbackPane feedback) {
		boolean logZ  = canvas.getParameters().isLogZ();
		int     count = (int) h2d.bin(dataPoint.x, dataPoint.y);

		double[] xbr = h2d.xBinRange(dataPoint.x);
		double[] ybr = h2d.yBinRange(dataPoint.y);
		if (xbr == null || ybr == null) {
			feedback.append(String.format(
					"(x, y) = (%.2f, %.2f) out of range",
					dataPoint.x, dataPoint.y));
			return;
		}

		String s = String.format(
				"x \u03b5 [%.1f , %.1f),  y \u03b5 [%.1f , %.1f) Z = %d",
				xbr[0], xbr[1], ybr[0], ybr[1], count);

		if (logZ) {
			double logCount = (count > 0) ? Math.log10(count) : 0.0;
			s += String.format("  (%sZ = %.2f)", UnicodeUtils.LOG10, logCount);
		}
		feedback.append(s);

		double maxCount = h2d.maxBin();
		if (maxCount > 0) {
			feedback.append(String.format(
					"Z / Zmax: %.1f%%", 100.0 * count / maxCount));
		}

		double percentile = h2d.percentile(dataPoint.x, dataPoint.y);
		double mean3x3    = h2d.localMean3x3(dataPoint.x, dataPoint.y);
		feedback.append(String.format(
				"Percentile: %dth    Local avg (3x3): %d",
				Math.round(percentile), (int) mean3x3));
	}
}