package edu.cnu.mdi.splot.pdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.event.EventListenerList;

/**
 * A lightweight, UI-agnostic container for the data backing a plot.
 * <p>
 * {@code PlotData} owns one or more {@link ACurve} instances (including
 * histogram-backed curves such as {@link HistoCurve}, and ordinary XY curves
 * such as {@link Curve}). Views typically register a single
 * {@link DataChangeListener} on the {@code PlotData} and repaint when notified.
 * </p>
 *
 * <h3>Notifications</h3> There are two kinds of changes:
 * <ul>
 * <li><b>Structural</b> changes to the model (curves added/removed/cleared)
 * &mdash; fired by {@code PlotData}.</li>
 * <li><b>Data</b> changes within a curve (points appended, histogram filled,
 * etc.) &mdash; fired by the curve, and forwarded by {@code PlotData} so the
 * view can listen in one place.</li>
 * </ul>
 *
 * <h3>Threading</h3> {@code PlotData} is not synchronized. If curves are
 * updated from a background sampler (e.g. {@link StripChartCurve}), the view
 * should use its existing snapshot/copy strategy when rendering.
 *
 * @author heddle
 */
public class PlotData implements CurveChangeListener {

	/** The curves owned by this plot-data instance. */
	private final ArrayList<ACurve> curves = new ArrayList<>();

	/** PlotData change listeners. */
	private final EventListenerList listenerList = new EventListenerList();

	/** Plot data set type. */
	private final PlotDataType type;
	
	/** 
	 * 2D histogram data, if applicable. 
	 * The only type to be cached because it has no curves.
	 */
	private Histo2DData histo2DData;

	/**
	 * Create plot data from one or more 1D histogram data objects. Each
	 * {@link HistoData} is wrapped in a {@link HistoCurve} and added to this model.
	 *
	 * @param histos one or more histogram data objects (must be non-null and
	 *               contain no nulls)
	 * @throws PlotDataException if there is a problem creating the data set
	 */
	public PlotData(HistoData... histos) throws PlotDataException {
		if (histos == null || histos.length < 1) {
			throw new PlotDataException("Must supply at least one histogram data object.");
		}
		type = PlotDataType.H1D;

		for (int i = 0; i < histos.length; i++) {
			HistoData hd = histos[i];
			if (hd == null) {
				throw new PlotDataException("Histogram data object " + i + " is null.");
			}
			HistoCurve hc = new HistoCurve(hd.name(), hd);
			addCurve(hc);

		}
	}
	
	/**
	 * Create plot data for a 2D histogram.
	 *
	 * @param histo2D the 2D histogram backing data (non-null)
	 * @throws PlotDataException if the histogram data is null
	 */
	public PlotData(Histo2DData histo2D) throws PlotDataException {
		if (histo2D == null) {
			throw new PlotDataException("Must supply a non-null 2D histogram data object.");
		}
		type = PlotDataType.H2D;
		// there are no curves for 2D histograms which is why we cache the data
		this.histo2DData = histo2D;
	}

	/**
	 * Create plot data for a strip chart (time series / streamed XY).
	 *
	 * @param stripData the strip chart backing data (non-null)
	 * @throws PlotDataException if the strip data does not provide a curve
	 */
	public PlotData(StripChartCurve stripData) throws PlotDataException {
		if (stripData == null) {
			throw new IllegalArgumentException("StripData object is null.");
		}
		type = PlotDataType.STRIP;
		addCurve(stripData);
	}

	/**
	 * Create plot data with specified data set type and column names.
	 *
	 * @param type       the data set type (non-null)
	 * @param curveNames a curve is created for each curve name. Must be non-null
	 *                   and contain at least one name. The names should not be
	 *                   null.
	 * @param fitOrders  optional fit orders for each curve (may be null). If
	 *                   non-null, the length must match the number of curve names.
	 *                   This fit orders are assigned to each curve via
	 *                   {@link Curve#setFitOrder(int)}. They are only relevant for
	 *                   MultiGaussian (no. of Gaussians) and Polynomial (polynomial
	 *                   degree) fits.
	 * @throws PlotDataException if there is a problem creating the data set
	 */
	public PlotData(PlotDataType type, String[] curveNames, int[] fitOrders) throws PlotDataException {

		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(curveNames, "curveNames");

		int curveCount = curveNames.length;
		if (curveCount < 1) {
			throw new PlotDataException("Must supply at least one curve name.");
		}

		// fit orders can be null, but if not null lengths must match
		if (fitOrders != null) {
			if (fitOrders.length != curveCount) {
				throw new PlotDataException(
						"If fit orders are supplied, their count must match the number of curve names.");
			}
		}

		this.type = type;

		switch (type) {

		case XYXY:
			for (int i = 0; i < curveCount; i++) {
				DataColumn xData = new DataColumn();
				DataColumn yData = new DataColumn();
				Curve curve = new Curve(curveNames[i], xData, yData, null);
				if (fitOrders != null) {
					curve.setFitOrder(fitOrders[i]);
				}
				addCurve(curve);
			}
			break;

		case XYEXYE:
			for (int i = 0; i < curveCount; i++) {
				DataColumn xData = new DataColumn();
				DataColumn yData = new DataColumn();
				DataColumn eData = new DataColumn();
				Curve curve = new Curve(curveNames[i], xData, yData, eData);
				if (fitOrders != null) {
					curve.setFitOrder(fitOrders[i]);
				}
				addCurve(curve);
			}
			break;
			
		case H2D:
			throw new PlotDataException("Use PlotData(Histo2DData) constructor for 2D histograms.");

		case H1D:
			throw new PlotDataException("Use PlotData(HistoData...) constructor for 1D histograms.");

		case STRIP:
			throw new PlotDataException("Use PlotData(StripData) constructor for STRIP data.");
		}

	}

	/**
	 * Create empty plot data with one default curve.
	 *
	 * @return empty plot data
	 */
	public static PlotData emptyData() {
		try {
			return new PlotData(PlotDataType.XYXY, new String[] { "Curve 1" }, null);
		} catch (PlotDataException e) {
			// should not happen
			throw new RuntimeException("Failed to create empty PlotData", e);
		}
	}

	/** @return the plot data type. */
	public PlotDataType getType() {
		return type;
	}
	
	/** 
	 * Get the 2D histogram data, if applicable. There are no curves 
	 * for 2D histograms so the data is cached here.
	 * @return the 2D histogram data, or null if not applicable. 
	 */
	public Histo2DData getHisto2DData() {
		return histo2DData;
	}

	/**
	 * Convenience method to determine if this is histogram plot data.
	 *
	 * @return true if histogram plot data
	 */
	public boolean isHistoData() {
		return (type == PlotDataType.H1D);
	}
	
	/**
	 * Convenience method to determine if this is 2D histogram plot data.
	 *
	 * @return true if 2D histogram plot data
	 */
	public boolean isHisto2DData() {
		return (type == PlotDataType.H2D);
	}

	public boolean isXYData() {
		return (type == PlotDataType.XYEXYE) || (type == PlotDataType.XYXY);
	}

	/**
	 * Determine if this is strip chart plot data.
	 *
	 * @return true if strip chart plot data
	 */
	public boolean isStripData() {
		return (type == PlotDataType.STRIP);
	}

	/** @return an unmodifiable view of the curves. */
	public List<ACurve> getCurves() {
		return Collections.unmodifiableList(curves);
	}

	/** @return a list of visible curves. */
	public List<ACurve> getVisibleCurves() {
		ArrayList<ACurve> visibleCurves = new ArrayList<>();
		for (ACurve curve : curves) {
			if (curve.isVisible()) {
				visibleCurves.add(curve);
			}
		}
		return visibleCurves;
	}

	/** @return curve count. */
	public int size() {
		if (isHisto2DData()) {
			return 1; // there are no curves, but we treat as one for convenience
		}
		return curves.size();
	}

	/** Convenience: get a curve by index. */
	public ACurve getCurve(int index) {
		return curves.get(index);
	}

	/**
	 * Convenience: get a curve by name.
	 *
	 * @param name the curve name
	 * @return the curve with the given name, or null if not found
	 */
	public ACurve getCurve(String name) {
		for (ACurve curve : curves) {
			if (curve.name().equals(name)) {
				return curve;
			}
		}
		return null;
	}

	/**
	 * Convenience: get the first curve. Often there is only one.
	 *
	 * @return the first curve, or null if there are no curves
	 */
	public ACurve getFirstCurve() {
		if (curves.isEmpty()) {
			return null;
		}
		return curves.get(0);
	}

	/**
	 * Determine if this is histogram plot data.
	 *
	 * @return true if histogram plot data
	 */
	public boolean isHistogramData() {
		return (type == PlotDataType.H1D);
	}

	/**
	 * Add a curve to this plot model.
	 *
	 * @param curve the curve to add (non-null)
	 * @return {@code true} if added
	 */
	public boolean addCurve(ACurve curve) {
		Objects.requireNonNull(curve, "curve");
		boolean added = curves.add(curve);
		if (added) {
			curve.addCurveChangeListener(this);
		}
		return added;
	}

	/** Curve-level notification (forwarded to {@link DataChangeListener}s). */
	@Override
	public void curveChanged(ACurve curve, CurveChangeType type) {
		notifyListeners(curve, type);
	}

	/** Notify {@link DataChangeListener}s that the plot model changed. */
	public void notifyListeners(ACurve curve, CurveChangeType type) {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == DataChangeListener.class) {
				((DataChangeListener) listeners[i + 1]).dataSetChanged(this, curve, type);
			}
		}
	}

	/** Add a plot-data change listener. */
	public void addDataChangeListener(DataChangeListener listener) {
		if (listener != null) {
			listenerList.add(DataChangeListener.class, listener);
		}
	}

	/** Remove a plot-data change listener. */
	public void removeDataChangeListener(DataChangeListener listener) {
		if (listener != null) {
			listenerList.remove(DataChangeListener.class, listener);
		}
	}

	/** @return the minimum x value over all curves. */
	public double xMin() {
		
		if (isHisto2DData()) {
			return histo2DData.xMin();
		}
		
		if (curves.isEmpty()) {
			return Double.NaN;
		}
		double xmin = Double.POSITIVE_INFINITY;
		for (ACurve curve : curves) {
			double cxmin = curve.xMin();
			if (cxmin < xmin) {
				xmin = cxmin;
			}
		}
		return xmin;
	}

	/** @return the maximum x value over all curves. */
	public double xMax() {
		
		if (isHisto2DData()) {
			return histo2DData.xMax();
		}
		
		if (curves.isEmpty()) {
			return Double.NaN;
		}
		double xmax = Double.NEGATIVE_INFINITY;
		for (ACurve curve : curves) {
			double cxmax = curve.xMax();
			if (cxmax > xmax) {
				xmax = cxmax;
			}
		}
		return xmax;
	}

	/** @return the minimum y value over all curves. */
	public double yMin() {
		
		if (isHisto2DData()) {
			return histo2DData.yMin();
		}
		
		if (curves.isEmpty()) {
			return Double.NaN;
		}
		double ymin = Double.POSITIVE_INFINITY;
		for (ACurve curve : curves) {
			double cymin = curve.yMin();
			if (cymin < ymin) {
				ymin = cymin;
			}
		}
		return ymin;
	}

	/** @return the maximum y value over all curves. */
	public double yMax() {
		
		if (isHisto2DData()) {
			return histo2DData.yMax();
		}
		
		if (curves.isEmpty()) {
			return Double.NaN;
		}
		double ymax = Double.NEGATIVE_INFINITY;
		for (ACurve curve : curves) {
			double cymax = curve.yMax();
			if (cymax > ymax) {
				ymax = cymax;
			}
		}
		return ymax;
	}

}
