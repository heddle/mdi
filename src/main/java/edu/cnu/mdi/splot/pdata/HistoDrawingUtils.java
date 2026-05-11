package edu.cnu.mdi.splot.pdata;

import java.awt.Point;
import java.awt.Polygon;

import edu.cnu.mdi.format.DoubleFormat;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotParameters;

/**
 * Stateless utility class for rendering-related histogram helpers.
 *
 * <p>This class holds the two static methods that were previously embedded in
 * {@link HistoData} but depend on the {@code plot} package &mdash; creating a
 * downward dependency from the data model into the view layer. Moving them here
 * removes that dependency from {@code HistoData} itself while keeping the
 * functionality in a single, discoverable place.</p>
 *
 * <p>Usage</p>
 * <pre>{@code
 * // Build the screen polygon for hit-testing or filled drawing
 * Polygon poly = HistoDrawingUtils.buildScreenPolygon(canvas, histoData);
 *
 * // Get the mouse-position status string
 * String status = HistoDrawingUtils.statusString(canvas, histoData, mousePoint, worldPoint);
 * }</pre>
 *
 * @author heddle (original), refactored for extraction
 * @see HistoData
 */
public final class HistoDrawingUtils {

	/** Not instantiable. */
	private HistoDrawingUtils() {}

	// -----------------------------------------------------------------------
	// Public API
	// -----------------------------------------------------------------------

	/**
	 * Builds the screen-space {@link Polygon} that outlines the histogram bars.
	 *
	 * <p>The polygon traces the top of every bar and the baseline, so it is
	 * suitable for both hit-testing ({@link Polygon#contains}) and filled
	 * rendering. Bins whose corners cannot be projected onto the screen (e.g.
	 * because transforms are not yet initialised) are silently skipped.</p>
	 *
	 * @param canvas the plot canvas that owns the coordinate transforms
	 *               (non-null)
	 * @param histo  the histogram data to outline (non-null)
	 * @return a {@link Polygon} in screen (component) coordinates; may be empty
	 *         if the canvas has no valid transforms
	 */
	public static Polygon buildScreenPolygon(PlotCanvas canvas, HistoData histo) {
		Polygon       poly = new Polygon();
		long[]        counts = histo.getCounts();
		Point         pp   = new Point();
		Point.Double  wp   = new Point.Double();

		int nbin = histo.getNumberBins();
		for (int bin = 0; bin < nbin; bin++) {
			double xmin = histo.getBinMinX(bin);
			double xmax = histo.getBinMaxX(bin);
			double y    = counts[bin];

			// First bin: add the bottom-left corner at y = 0.
			if (bin == 0) {
				wp.setLocation(xmin, 0.0);
				if (canvas.dataToScreen(pp, wp)) {
					poly.addPoint(pp.x, pp.y);
				}
			}

			// Top-left of bar.
			wp.setLocation(xmin, y);
			if (canvas.dataToScreen(pp, wp)) {
				poly.addPoint(pp.x, pp.y);
			}

			// Top-right of bar.
			wp.setLocation(xmax, y);
			if (canvas.dataToScreen(pp, wp)) {
				poly.addPoint(pp.x, pp.y);
			}

			// Last bin: add the bottom-right corner at y = 0.
			if (bin == nbin - 1) {
				wp.setLocation(xmax, 0.0);
				if (canvas.dataToScreen(pp, wp)) {
					poly.addPoint(pp.x, pp.y);
				}
			}
		}
		return poly;
	}

	/**
	 * Builds the status string shown in the feedback pane when the mouse is
	 * over a histogram.
	 *
	 * <p>Returns the bin range and count under the cursor, formatted using the
	 * canvas's current decimal and exponent settings. Returns an empty string
	 * when the mouse is outside the histogram polygon.</p>
	 *
	 * @param canvas     the plot canvas (provides coordinate transforms and
	 *                   formatting parameters) (non-null)
	 * @param histo      the histogram data being queried (non-null)
	 * @param mousePoint current mouse position in screen (component) coordinates
	 *                   (non-null)
	 * @param wp         current mouse position in data coordinates (non-null)
	 * @return a formatted status string, or {@code ""} if the cursor is outside
	 *         the histogram
	 */
	public static String statusString(PlotCanvas canvas,
	                                  HistoData histo,
	                                  Point mousePoint,
	                                  Point.Double wp) {
		Polygon poly = buildScreenPolygon(canvas, histo);
		if (!poly.contains(mousePoint)) {
			return "";
		}

		int bin = histo.getBin(wp.x);

		PlotParameters params  = canvas.getParameters();
		String         minStr  = DoubleFormat.doubleFormat(
				histo.getBinMinX(bin),
				params.getNumDecimalX(),
				params.getMinExponentX());
		String         maxStr  = DoubleFormat.doubleFormat(
				histo.getBinMaxX(bin),
				params.getNumDecimalX(),
				params.getMinExponentX());

		String name = histo.name();
		String tag  = (name != null && !name.isEmpty()) ? "[" + name + "] " : "";

		return tag + "bin: " + bin
				+ " [" + minStr + " \u2013 " + maxStr + "]"
				+ "  counts: " + histo.getCount(bin);
	}
}