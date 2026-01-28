package edu.cnu.mdi.splot.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.event.EventListenerList;

import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.splot.edit.PlotPreferencesDialog;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.CurveChangeType;
import edu.cnu.mdi.splot.pdata.DataChangeListener;
import edu.cnu.mdi.splot.pdata.HistoCurve;
import edu.cnu.mdi.splot.pdata.HistoData;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.pdata.Snapshot;
import edu.cnu.mdi.util.Environment;

@SuppressWarnings("serial")
public class PlotCanvas extends JComponent implements MouseListener, MouseMotionListener, DataChangeListener {

	public static final String DONEDRAWINGPROP = "Done Drawing";
	public static final String TITLETEXTCHANGE = "Title Text";
	public static final String TITLEFONTCHANGE = "Title Font";
	public static final String XLABELTEXTCHANGE = "X Label Text";
	public static final String YLABELTEXTCHANGE = "Y Label Text";
	public static final String AXESFONTCHANGE = "Axes Font";
	public static final String STATUSFONTCHANGE = "Status Font";

	// List of plot change listeners
	private EventListenerList _listenerList;

	// used to fire property changes. Transient.
	private long drawCount = 0;

	// for saving files
	private static String _dataFilePath;

	// the bounds of the plot
	private Rectangle _activeBounds;

	// redraw check for dynamic data adding
	private boolean _needsRedraw;
	private boolean _needsRescale;

	// the world system of the active area
	private Rectangle2D.Double _worldSystem = new Rectangle2D.Double(0, 0, 1, 1);

	// If true, log scaling is actually active (requested + valid range)
	private boolean _xLogActive = false;
	private boolean _yLogActive = false;

	// convert from screen to data
	protected AffineTransform _localToWorld;
	protected AffineTransform _worldToLocal;

	// dataset being plotted
	protected PlotData _plotData;

	// plot parameters
	protected PlotParameters _parameters;

	// plot ticks
	private PlotTicks _plotTicks;

	// if this has a parent (optional), print applies to the
	// parent. For example, the parent might be a PlotPanel
	private Component _parent;

	// legend and floating label dragging
	private Legend _legend;

	// extra and floating label dragging
	private ExtraText _extra;

	// data drawer
	private DataDrawer _dataDrawer;

	// toolbar that owns this canvas
	private BaseToolBar _toolBar;

	// swing timer for fixing world system
	private final Timer _timer;

	/**
	 * Create a plot canvas for plotting a dataset
	 *
	 * @param plotData  the dataset to plot. It might contain many curves
	 * @param plotTitle the plot title
	 * @param xLabel    the x axis label
	 * @param yLabel    the y axis label
	 */
	public PlotCanvas(PlotData plotData, String plotTitle, String xLabel, String yLabel) {

		if (plotData == null) {
			plotData = PlotData.emptyData();
		}
		if (_dataFilePath == null) {
			_dataFilePath = Environment.getInstance().getHomeDirectory();
		}

		setBackground(Color.white);
		_parameters = new PlotParameters(this);
		_parameters.setPlotTitle(plotTitle);
		_parameters.setXLabel(xLabel);
		_parameters.setYLabel(yLabel);
		_plotTicks = new PlotTicks(this);

		setPlotData(plotData);

		ComponentAdapter componentAdapter = new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent ce) {
				setAffineTransforms();
				repaint();
			}
		};

		_legend = new Legend(this);
		_extra = new ExtraText(this);
		_dataDrawer = new DataDrawer(this);

		addComponentListener(componentAdapter);
		addMouseListener(this);
		addMouseMotionListener(this);

		int delay = 1000; // milliseconds
		_timer = new Timer(delay, evt -> onTimer());
		_timer.setCoalesce(true); // good for bursty updates
	}

	// timer action
	private void onTimer() {
		if (_needsRedraw) {
			if (_needsRescale) {
				setWorldSystem();
			}
			repaint();
			_needsRedraw = false;
			_needsRescale = false;
		}
	}

	@Override
	public void addNotify() {
		super.addNotify();
		// optional: start here rather than constructor
	}

	@Override
	public void removeNotify() {
		if (_timer != null)
			_timer.stop();
		super.removeNotify();
	}

	/**
	 * Get the PlotData type
	 *
	 * @return the plot data type
	 */
	public PlotDataType getType() {
		return _plotData.getType();
	}

	/**
	 * Get the plot title
	 *
	 * @return the plot title
	 */
	public String getTitle() {
		return _parameters.getPlotTitle();
	}

	/**
	 * Get the plot parameters
	 *
	 * @return the plot parameters
	 */
	public PlotParameters getParameters() {
		return _parameters;
	}

	/**
	 * Set the parent component, probably a PlotPanel
	 *
	 * @param parent the optional parent component
	 */
	public void setParent(Component parent) {
		_parent = parent;
	}

	/**
	 * Set a new data set for the canvas
	 *
	 * @param plotData the new dataset
	 */
	protected void setPlotData(PlotData plotData) {
		if (plotData == null) {
			plotData = PlotData.emptyData();
		}

		// remove from old
		if (_plotData != null) {
			_plotData.removeDataChangeListener(this);
		}

		_plotData = plotData;

		// add to new
		_plotData.addDataChangeListener(this);

		setWorldSystem();
		repaint();
	}

	/**
	 * Get the underlying plot data
	 *
	 * @return the underlying plot data
	 */
	public PlotData getPlotData() {
		return _plotData;
	}

	/**
	 * Clear all data from all curves Use with caution!
	 */
	public void clearData() {
		for (ACurve curve : getPlotData().getCurves()) {
			curve.clearData();
		}
	}

	/**
	 * Get the world boundary
	 *
	 * @return the world boundary
	 */
	public Rectangle.Double getWorld() {
		return _worldSystem;
	}

	public void setWorldSystem() {

		if (_worldSystem == null) {
			return;
		}

		if (_plotData == null) {
			_worldSystem.setFrame(0, 0, 1, 1);
			return;
		}

		double xmin = _plotData.xMin();
		if (Double.isNaN(xmin)) {
			return;
		}

		double xmax = _plotData.xMax();
		double ymin = _plotData.yMin();
		double ymax = _plotData.yMax();

		PlotParameters params = getParameters();

		LimitsMethod xMethod = params.getXLimitsMethod();
		LimitsMethod yMethod = params.getYLimitsMethod();

		final boolean xLogRequested = (params.getXScale() == PlotParameters.AxisScale.LOG10);
		final boolean yLogRequested = (params.getYScale() == PlotParameters.AxisScale.LOG10);

		// -------------------------------------------------------------
		// X bounds: if log requested, use positive-only data bounds and
		// DO NOT call NiceScale (which often rounds down to 0).
		// -------------------------------------------------------------
		switch (xMethod) {
		case MANUALLIMITS:
			xmin = params.getManualXMin();
			xmax = params.getManualXMax();
			break;

		case ALGORITHMICLIMITS:
			if (xLogRequested) {
				double[] mm = positiveDataMinMax(true);
				if (mm != null) {
					xmin = mm[0];
					xmax = mm[1];
				} // else keep fallback values; log will deactivate later
			} else {
				NiceScale ns = new NiceScale(xmin, xmax, _plotTicks.getNumMajorTickX() + 2, params.includeXZero());
				xmin = ns.getNiceMin();
				xmax = ns.getNiceMax();
			}
			break;

		case USEDATALIMITS:
			if (xLogRequested) {
				double[] mm = positiveDataMinMax(true);
				if (mm != null) {
					xmin = mm[0];
					xmax = mm[1];
				}
			}
			break;
		}

		// -------------------------------------------------------------
		// Y bounds: same logic
		// -------------------------------------------------------------
		switch (yMethod) {
		case MANUALLIMITS:
			ymin = params.getManualYMin();
			ymax = params.getManualYMax();
			break;

		case ALGORITHMICLIMITS:
			if (yLogRequested) {
				double[] mm = positiveDataMinMax(false);
				if (mm != null) {
					ymin = mm[0];
					ymax = mm[1];
				}
			} else {
				NiceScale ns = new NiceScale(ymin, ymax, _plotTicks.getNumMajorTickY() + 2, params.includeYZero());
				ymin = ns.getNiceMin();
				ymax = ns.getNiceMax();
			}
			break;

		case USEDATALIMITS:
			if (yLogRequested) {
				double[] mm = positiveDataMinMax(false);
				if (mm != null) {
					ymin = mm[0];
					ymax = mm[1];
				}
			}
			break;
		}

		// Guard invalids
		if (!Double.isFinite(xmin) || !Double.isFinite(xmax) || !Double.isFinite(ymin) || !Double.isFinite(ymax)) {
			_worldSystem.setFrame(0, 0, 1, 1);
			return;
		}

		// Ensure min <= max
		if (xmax < xmin) {
			double t = xmin;
			xmin = xmax;
			xmax = t;
		}
		if (ymax < ymin) {
			double t = ymin;
			ymin = ymax;
			ymax = t;
		}

		// Expand any zero/near-zero range (log-safe)
		double dx = xmax - xmin;
		if (Math.abs(dx) < 1.0e-12) {
			if (xLogRequested && xmin > 0.0 && xmax > 0.0) {
				xmin /= 1.1;
				xmax *= 1.1;
			} else {
				double pad = (Math.abs(xmin) > 0) ? (0.01 * Math.abs(xmin)) : 1.0;
				xmin -= pad;
				xmax += pad;
			}
		}

		double dy = ymax - ymin;
		if (Math.abs(dy) < 1.0e-12) {
			if (yLogRequested && ymin > 0.0 && ymax > 0.0) {
				ymin /= 1.1;
				ymax *= 1.1;
			} else {
				double pad = (Math.abs(ymin) > 0) ? (0.01 * Math.abs(ymin)) : 1.0;
				ymin -= pad;
				ymax += pad;
			}
		}

		// Enable log only if final data range is strictly positive
		_xLogActive = xLogRequested && (xmin > 0.0) && (xmax > 0.0);
		_yLogActive = yLogRequested && (ymin > 0.0) && (ymax > 0.0);

		// Convert bounds to world space (log space if active)
		double wxmin = _xLogActive ? Math.floor(log10(xmin)) : xmin;
		double wxmax = _xLogActive ? Math.ceil(log10(xmax)) : xmax;
		double wymin = _yLogActive ? Math.floor(log10(ymin)) : ymin;
		double wymax = _yLogActive ? Math.ceil(log10(ymax)) : ymax;

		if (!Double.isFinite(wxmin) || !Double.isFinite(wxmax) || !Double.isFinite(wymin) || !Double.isFinite(wymax)) {
			_worldSystem.setFrame(0, 0, 1, 1);
			return;
		}
		if (wxmax <= wxmin)
			wxmax = wxmin + 1.0;
		if (wymax <= wymin)
			wymax = wymin + 1.0;

		_worldSystem.setFrame(wxmin, wymin, wxmax - wxmin, wymax - wymin);

		setAffineTransforms();
	}

	/**
	 * Paint the canvas
	 *
	 * @param g the graphics context
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Rectangle b = getBounds();

		g.setColor(getBackground());
		g.fillRect(0, 0, b.width, b.height);

		// super.paintComponent(g);
		setAffineTransforms();

		// draw the data
		_dataDrawer.draw(g, _plotData);

		// frame the active area
		g.setColor(Color.black);
		g.drawRect(_activeBounds.x, _activeBounds.y, _activeBounds.width, _activeBounds.height);

		// draw the ticks and legend
		_plotTicks.draw(g);

		if (_parameters.isLegendDrawn()) {
			_legend.draw(g);
		}

		if (_parameters.extraDrawing()) {
			_extra.draw(g);
		}

		firePropertyChange(DONEDRAWINGPROP, drawCount, ++drawCount);

	}

	/**
	 * Data is being added, possibly very quickly, so lets schedule a redraw
	 *
	 * @param rescale if <code>true</code> the world system will also be rescaled
	 */
	public void needsRedraw(boolean rescale) {
		_needsRedraw = true;
		_needsRescale = _needsRescale || rescale;
	}

	/**
	 * Get the active plot area
	 *
	 * @return the active plot area
	 */
	public Rectangle getActiveBounds() {
		return _activeBounds;
	}

	// set the active bounds from the component bounds and the margins
	private void setActiveBounds() {
		Rectangle bounds = getBounds();
		if (bounds == null) {
			_activeBounds = null;
		} else {
			int left = 0;
			int top = 0;
			int right = left + bounds.width;
			int bottom = top + bounds.height;

			int bottomMargin = 25;
			int leftMargin = 25;

			if (_parameters.getAxesFont() != null) {
				FontMetrics fm = getFontMetrics(_parameters.getAxesFont());
				bottomMargin = 6 + fm.getHeight();
				leftMargin = 6 + fm.getHeight();
			}

			left += leftMargin;
			// default values for margins
			int _topMargin = 10;
			top += _topMargin;
			int _rightMargin = 10;
			right -= _rightMargin;
			bottom -= bottomMargin;

			if (_activeBounds == null) {
				_activeBounds = new Rectangle();
			}
			_activeBounds.setBounds(left, top, right - left, bottom - top);
		}

	}

	// check if x axis is reversed, so xmin on right insteat of left
	private boolean reverseX() {
		return _parameters.isReverseXaxis();
	}

	// check if y axis is reversed, so ymin on top instead of bottom
	private boolean reverseY() {
		return _parameters.isReverseYaxis();
	}

	private static double log10(double v) {
		return Math.log(v) / Math.log(10.0);
	}

	public boolean isXLogActive() {
		return _xLogActive;
	}

	public boolean isYLogActive() {
		return _yLogActive;
	}


	/**
	 * Get the visible bounds in *data* space (not log space). Used by tick
	 * generation and any UI that wants real values.
	 */
	public Rectangle2D.Double getDataWorld() {
		if (_worldSystem == null) {
			return null;
		}
		double xmin = xWorldToData(_worldSystem.getMinX());
		double xmax = xWorldToData(_worldSystem.getMaxX());
		double ymin = yWorldToData(_worldSystem.getMinY());
		double ymax = yWorldToData(_worldSystem.getMaxY());

		double x0 = Math.min(xmin, xmax);
		double y0 = Math.min(ymin, ymax);
		double w = Math.abs(xmax - xmin);
		double h = Math.abs(ymax - ymin);

		return new Rectangle2D.Double(x0, y0, w, h);
	}

	/**
	 * Find min/max strictly positive values on an axis across all curves.
	 * 
	 * @param xAxis true for x, false for y
	 * @return {minPos, maxPos} or null if no positive finite values exist
	 */
	private double[] positiveDataMinMax(boolean xAxis) {
		if (_plotData == null) {
			return null;
		}

		double minPos = Double.POSITIVE_INFINITY;
		double maxPos = Double.NEGATIVE_INFINITY;

		for (ACurve c : _plotData.getCurves()) {
			Snapshot s = c.snapshot();
			double[] arr = xAxis ? s.x : s.y;
			if (arr == null)
				continue;

			for (double v : arr) {
				if (v > 0.0 && Double.isFinite(v)) {
					if (v < minPos)
						minPos = v;
					if (v > maxPos)
						maxPos = v;
				}
			}
		}

		if (minPos == Double.POSITIVE_INFINITY || maxPos == Double.NEGATIVE_INFINITY) {
			return null;
		}
		return new double[] { minPos, maxPos };
	}

	// Get the transforms for world to local and vice versa
	protected void setAffineTransforms() {
		Rectangle bounds = getBounds();

		if ((bounds == null) || (bounds.width < 1) || (bounds.height < 1)) {
			_localToWorld = null;
			_worldToLocal = null;
			_activeBounds = null;
			return;
		}

		setActiveBounds();

		if (_worldSystem == null || _activeBounds == null || _activeBounds.width < 1 || _activeBounds.height < 1) {
			return;
		}

		final boolean rx = reverseX();
		final boolean ry = reverseY();

		// Magnitudes
		final double sxMag = _worldSystem.width / _activeBounds.width;
		final double syMag = _worldSystem.height / _activeBounds.height;

		// Choose which world edge maps to the local top-left of the active plot area
		final double tx = rx ? _worldSystem.getMaxX() : _worldSystem.getMinX();
		final double ty = ry ? _worldSystem.getMinY() : _worldSystem.getMaxY();

		// Sign controls axis direction on screen
		final double sx = rx ? -sxMag : sxMag;
		final double sy = ry ? syMag : -syMag;

		_localToWorld = AffineTransform.getTranslateInstance(tx, ty);
		_localToWorld.concatenate(AffineTransform.getScaleInstance(sx, sy));
		_localToWorld.concatenate(AffineTransform.getTranslateInstance(-_activeBounds.x, -_activeBounds.y));

		try {
			_worldToLocal = _localToWorld.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
			_worldToLocal = null;
		}
	}

	/**
	 * The mouse has been dragged over the plot canvas
	 *
	 * @param e the mouseEvent
	 */
	@Override
	public void mouseDragged(MouseEvent e) {
		if (_legend.isDraggingPrimed()) {
			_legend.setDragging(true);
		}

		if (_legend.isDragging()) {
			int dx = e.getX() - _legend.getCurrentPoint().x;
			int dy = e.getY() - _legend.getCurrentPoint().y;
			_legend.x += dx;
			_legend.y += dy;
			_legend.setCurrentPoint(e.getPoint());
			repaint();
		}

		if (_extra.isDraggingPrimed()) {
			_extra.setDragging(true);
		}

		if (_extra.isDragging()) {
			int dx = e.getX() - _extra.getCurrentPoint().x;
			int dy = e.getY() - _extra.getCurrentPoint().y;
			_extra.x += dx;
			_extra.y += dy;
			_extra.setCurrentPoint(e.getPoint());
			repaint();
		}
	}

	/**
	 * The mouse has moved over the plot canvas
	 *
	 * @param e the mouseEvent
	 */
	@Override
	public void mouseMoved(MouseEvent e) {

		// System.err.println("Plot Canvas MMoved");
		if (_plotData == null) {
			return;
		}

		PlotPanel plotPanel = plotPanel();
		if (plotPanel == null) {
			return;
		}

		FeedbackPane feedback = plotPanel().getFeedbackPane();

		if ((_activeBounds == null) || (_worldSystem == null)) {
			return;
		}

		feedback.clear();

		Point pp = e.getPoint();
		Point2D.Double dataPoint = new Point2D.Double();
		localToData(pp, dataPoint);
		feedback.append(String.format("(x, y) = (%7.2g, %-7.2g)", dataPoint.x, dataPoint.y));

		if (_plotData.isXYData()) {
			List<ACurve> curves = _plotData.getVisibleCurves();

			for (ACurve curve : curves) {
				String cStr = String.format("%s: %d points ", curve.name(), curve.length());

				String fitSummary = curve.getFitSummary();
				if (fitSummary != null) {
					cStr += " " + fitSummary;
				}
				feedback.append(cStr);

			}
		}

		if (_plotData.isHistoData()) {
			List<ACurve> curves = _plotData.getVisibleCurves();

			for (ACurve curve : curves) {
				HistoCurve hc = (HistoCurve) curve;
				HistoData hd = hc.getHistoData();
				String cStr = HistoData.statusString(this, hd, pp, dataPoint);
				String fitSummary = curve.getFitSummary();
				if (fitSummary != null) {
					cStr += " " + fitSummary;
				}
				feedback.append(cStr);
			}
		}
	}

	/**
	 * Add a plot change listener
	 *
	 * @param listener the listener to add
	 */
	public void addPlotChangeListener(PlotChangeListener listener) {

		if (_listenerList == null) {
			_listenerList = new EventListenerList();
		}

		// avoid adding duplicates
		_listenerList.remove(PlotChangeListener.class, listener);
		_listenerList.add(PlotChangeListener.class, listener);
	}

	/**
	 * Remove a PlotChangeListener.
	 *
	 * @param listener the PlotChangeListener to remove.
	 */

	public void removePlotChangeListener(PlotChangeListener listener) {

		if ((listener == null) || (_listenerList == null)) {
			return;
		}

		_listenerList.remove(PlotChangeListener.class, listener);
	}

	// notify listeners of message
	private void notifyListeners(PlotChangeType event) {

		if (_listenerList == null) {
			return;
		}

		// Guaranteed to return a non-null array
		Object[] listeners = _listenerList.getListenerList();

		// This weird loop is the bullet proof way of notifying all listeners.
		// for (int i = listeners.length - 2; i >= 0; i -= 2) {
		// order is flipped so it goes in order as added
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == PlotChangeListener.class) {
				PlotChangeListener listener = (PlotChangeListener) listeners[i + 1];
				listener.plotChanged(event);
			}
		}
	}

	/**
	 * The plot is being shutdown
	 */
	public void shutDown() {
		if (_timer != null)
			_timer.stop();
		notifyListeners(PlotChangeType.SHUTDOWN);
	}

	/**
	 * The plot is being stood up
	 */
	public void standUp() {
		if (_timer != null)
			_timer.start();
		notifyListeners(PlotChangeType.STOODUP);
	}

	// check if pointer tool is active
	private boolean isPointer() {
		return (_toolBar == null) || _toolBar.isPointerActive();
	}

	public void setToolBar(BaseToolBar toolBar) {
		_toolBar = toolBar;
	}

	/**
	 * The mouse has been pressed on plot canvas
	 *
	 * @param e the mouseEvent
	 */
	@Override
	public void mousePressed(MouseEvent e) {

		if (isPointer() && _parameters.isLegendDrawn() && _legend.contains(e.getPoint())) {
			_legend.setDraggingPrimed(true);
			_legend.setCurrentPoint(e.getPoint());
		} else if (isPointer() && _parameters.extraDrawing() && _extra.contains(e.getPoint())) {
			_extra.setDraggingPrimed(true);
			_extra.setCurrentPoint(e.getPoint());
		}
	}

	/**
	 * The mouse has been released on plot canvas. A release comes before the click
	 *
	 * @param e the mouseEvent
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		_legend.setDragging(false);
		_legend.setDraggingPrimed(false);
		_legend.setCurrentPoint(null);
		_extra.setDragging(false);
		_extra.setDraggingPrimed(false);
		_extra.setCurrentPoint(null);
	}

	/**
	 * The mouse has entered the area of the plot canvas
	 *
	 * @param e the mouseEvent
	 */
	@Override
	public void mouseEntered(MouseEvent e) {

	}

	/**
	 * The mouse has exited the area of the plot canvas
	 *
	 * @param e the mouseEvent
	 */
	@Override
	public void mouseExited(MouseEvent e) {
	}

	/**
	 * Zoom to a given rectangle in local coordinates
	 *
	 * @param rbrect the rectangle in local coordinates
	 */
	public void zoomToRect(Rectangle rbrect) {
		if ((rbrect.width < 15) || (rbrect.height < 15)) {
			return;
		}

		localToWorldRaw(rbrect, _worldSystem);
		setAffineTransforms();
		repaint();
	}

	/**
	 * Scale the canvas by a given amount
	 *
	 * @param amount the factor to scale by
	 */
	public void scale(double amount) {
		double xc = _worldSystem.getCenterX();
		double w = _worldSystem.width * amount;
		double x = xc - w / 2;

		double h;
		double y;

		h = _worldSystem.height * amount;
		double yc = _worldSystem.getCenterY();
		y = yc - h / 2;
		_worldSystem.setFrame(x, y, w, h);
		repaint();
	}

	/**
	 * Show the plot preferences dialog
	 */
	public void showPreferencesEditor() {
		PlotPreferencesDialog prefEditor = new PlotPreferencesDialog(this);
		prefEditor.setVisible(true);
		prefEditor.toFront();
	}

	/**
	 * Used so another object can tell the plot canvas to fire a property change
	 * event
	 *
	 * @param propName the property name
	 * @param oldValue the old value
	 * @param newValue the new value
	 */
	public void remoteFirePropertyChange(String propName, Object oldValue, Object newValue) {
		firePropertyChange(propName, oldValue, newValue);
	}

	/**
	 * Get the canavas's plot ticks
	 *
	 * @return the plot ticks
	 */
	public PlotTicks getPlotTicks() {
		return _plotTicks;
	}

	public PlotPanel plotPanel() {
		if ((_parent != null) && (_parent instanceof PlotPanel)) {
			return (PlotPanel) _parent;
		}
		return null;
	}

	@Override
	public void dataSetChanged(PlotData plotData, ACurve curve, CurveChangeType type) {
		setWorldSystem();
		needsRedraw(true);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	/*
	 * ========================= Coordinate systems in PlotCanvas =========================
	 *
	 * PlotCanvas uses three coordinate systems:
	 *
	 * 1) Local (pixel) coordinates
	 *    - Swing component coordinates: (0,0) is the top-left of the component.
	 *    - Mouse events and painting happen in this space.
	 *
	 * 2) Raw World coordinates (internal world)
	 *    - This is the coordinate system stored in _worldSystem and used by the affine transforms.
	 *    - IMPORTANT: when log axes are active, raw world is in log10(data) space.
	 *      Example: if x-data is 100, raw-world x is log10(100) = 2.
	 *    - This is the correct space for rubber-band zooming and for storing the visible window.
	 *
	 * 3) Data coordinates
	 *    - These are the "real" data values shown to the user and stored in curves.
	 *    - Data coordinates are ALWAYS in linear units (even when log axes are active).
	 *    - Conversion between data and raw-world happens at the boundary:
	 *        data -> raw world : log10(data)    (only if log active)
	 *        raw world -> data : pow(10, world) (only if log active)
	 *
	 * Naming rule used below:
	 *   - *Raw* methods produce/consume raw-world coordinates (log10-space if active).
	 *   - *Data* methods produce/consume data coordinates (always linear values).
	 *
	 * Usage rules:
	 *   - For rubber-band zoom: localToWorldRaw(Rectangle, ...) then assign into _worldSystem.
	 *   - For mouse readout / status text: localToData(Point, ...) so you display real values.
	 *   - For drawing curves: worldToLocal(Point, dataPoint) where the input is data; the method
	 *     handles the log conversion internally.
	 * ===================================================================================
	 */

	// -------- transformation methods ---------

	/**
	 * Convert a local (pixel) point to the internal <b>raw world</b> coordinates.
	 * <p>
	 * Raw world is the coordinate system used by {@link #_worldSystem}. When log scaling
	 * is active, raw world coordinates are in log10(data) space.
	 * </p>
	 * <p>
	 * Use this when you need to manipulate {@link #_worldSystem} directly (e.g. zoom),
	 * or when you need the internal world-space for debugging.
	 * </p>
	 *
	 * @param p  local pixel point (component coordinates)
	 * @param wp output point filled with raw world coordinates (log10-space if active)
	 */
	public void localToWorldRaw(Point p, Point2D.Double wp) {
	    if (wp == null) {
	        throw new IllegalArgumentException("wp must not be null");
	    }
	    if (_localToWorld == null || p == null) {
	        return;
	    }
	    _localToWorld.transform(p, wp);
	}

	/**
	 * Convert a local (pixel) rectangle to an internal <b>raw world</b> rectangle.
	 * <p>
	 * This is the correct conversion for rubberband zooming because {@link #_worldSystem}
	 * is stored in raw world coordinates (log10-space when log axes are active).
	 * </p>
	 *
	 * @param pr local (pixel) rectangle (component coordinates)
	 * @param wr output rectangle filled with raw world bounds (log10-space if active)
	 */
	public void localToWorldRaw(Rectangle pr, Rectangle2D.Double wr) {
	    if (wr == null) {
	        throw new IllegalArgumentException("wr must not be null");
	    }
	    if (_localToWorld == null || pr == null) {
	        return;
	    }

	    // Two corners in local space
	    Point p0 = new Point(pr.x, pr.y);
	    Point p1 = new Point(pr.x + pr.width, pr.y + pr.height);

	    Point2D.Double w0 = new Point2D.Double();
	    Point2D.Double w1 = new Point2D.Double();

	    _localToWorld.transform(p0, w0);
	    _localToWorld.transform(p1, w1);

	    double xmin = Math.min(w0.x, w1.x);
	    double xmax = Math.max(w0.x, w1.x);
	    double ymin = Math.min(w0.y, w1.y);
	    double ymax = Math.max(w0.y, w1.y);

	    wr.setFrame(xmin, ymin, xmax - xmin, ymax - ymin);
	}

	/**
	 * Convert a local (pixel) point to <b>data</b> coordinates.
	 * <p>
	 * Data coordinates are the real values (always linear units). When log axes are active,
	 * this method converts from raw-world log10 space back into data space.
	 * </p>
	 * <p>
	 * Use this for mouse readouts, hit tests in data space, and any UI that reports
	 * "real" x/y values.
	 * </p>
	 *
	 * @param p  local pixel point (component coordinates)
	 * @param dp output point filled with data coordinates (linear units)
	 */
	public void localToData(Point p, Point2D.Double dp) {
	    if (dp == null) {
	        throw new IllegalArgumentException("dp must not be null");
	    }
	    if (_localToWorld == null || p == null) {
	        return;
	    }

	    // dp becomes RAW world (log10-space if active)
	    _localToWorld.transform(p, dp);

	    // RAW world -> DATA
	    dp.x = xWorldToData(dp.x);
	    dp.y = yWorldToData(dp.y);
	}

	/**
	 * Convert a local (pixel) rectangle to <b>data</b> coordinates.
	 * <p>
	 * This converts the pixel rectangle to raw-world first (log10-space if active),
	 * then converts the bounds back into data space.
	 * </p>
	 * <p>
	 * Use this when you want to interpret a rubberband selection in terms of data values.
	 * Do <b>not</b> use this to update {@link #_worldSystem}; for zoom you want raw world.
	 * </p>
	 *
	 * @param pr local (pixel) rectangle (component coordinates)
	 * @param dr output rectangle filled with data-space bounds (linear units)
	 */
	public void localToData(Rectangle pr, Rectangle2D.Double dr) {
	    if (dr == null) {
	        throw new IllegalArgumentException("dr must not be null");
	    }
	    if (_localToWorld == null || pr == null) {
	        return;
	    }

	    Rectangle2D.Double wr = new Rectangle2D.Double();
	    localToWorldRaw(pr, wr); // RAW world (log10-space if active)

	    double xmin = xWorldToData(wr.getMinX());
	    double xmax = xWorldToData(wr.getMaxX());
	    double ymin = yWorldToData(wr.getMinY());
	    double ymax = yWorldToData(wr.getMaxY());

	    dr.x = Math.min(xmin, xmax);
	    dr.y = Math.min(ymin, ymax);
	    dr.width  = Math.abs(xmax - xmin);
	    dr.height = Math.abs(ymax - ymin);
	}

	/**
	 * Convert a <b>data</b> coordinate to local (pixel) coordinates.
	 * <p>
	 * Input is data-space (linear units). If log axes are active, the method converts
	 * data -> raw-world via log10(data) before applying the affine transform.
	 * </p>
	 *
	 * @param pp output local pixel point (component coordinates)
	 * @param dp input data point (linear units)
	 * @return true if conversion succeeded; false if log axes are active and input
	 *         contains non-positive values (invalid for log scale) or transforms are unavailable
	 */
	public boolean worldToLocal(Point pp, Point2D.Double dp) {
	    if (_worldToLocal == null || pp == null || dp == null) {
	        return false;
	    }

	    // Reject invalid data for log axes
	    if ((_xLogActive && dp.x <= 0.0) || (_yLogActive && dp.y <= 0.0)) {
	        return false;
	    }

	    Point2D.Double wsrc = new Point2D.Double(xDataToWorld(dp.x), yDataToWorld(dp.y));
	    _worldToLocal.transform(wsrc, pp);
	    return true;
	}

	/**
	 * Convert a <b>data</b> rectangle to a local (pixel) rectangle.
	 * <p>
	 * The input rectangle is in data-space (linear units). If log axes are active,
	 * conversion uses log10 internally.
	 * </p>
	 * <p>
	 * IMPORTANT: This method can fail in log mode if any bound is non-positive. In that case
	 * the return value is false and {@code r} is left unchanged.
	 * </p>
	 *
	 * @param r  output local pixel rectangle (component coordinates)
	 * @param dr input data rectangle (linear units)
	 * @return true if conversion succeeded; false if invalid for log scale or transforms missing
	 */
	public boolean worldToLocal(Rectangle r, Rectangle2D.Double dr) {
	    if (r == null || dr == null) {
	        return false;
	    }

	    Point2D.Double d0 = new Point2D.Double(dr.getMinX(), dr.getMinY());
	    Point2D.Double d1 = new Point2D.Double(dr.getMaxX(), dr.getMaxY());

	    Point p0 = new Point();
	    Point p1 = new Point();

	    if (!worldToLocal(p0, d0) || !worldToLocal(p1, d1)) {
	        return false;
	    }

	    int x = Math.min(p0.x, p1.x);
	    int y = Math.min(p0.y, p1.y);
	    int w = Math.abs(p1.x - p0.x);
	    int h = Math.abs(p1.y - p0.y);

	    r.setBounds(x, y, w, h);
	    return true;
	}

	/**
	 * Data -> raw-world conversion for X.
	 * <p>
	 * When log axes are active, raw world is log10(data).
	 * </p>
	 */
	private double xDataToWorld(double x) {
	    return _xLogActive ? log10(x) : x;
	}

	/**
	 * Data -> raw-world conversion for Y.
	 * <p>
	 * When log axes are active, raw world is log10(data).
	 * </p>
	 */
	private double yDataToWorld(double y) {
	    return _yLogActive ? log10(y) : y;
	}

	/**
	 * Raw-world -> data conversion for X.
	 * <p>
	 * When log axes are active, data is 10^(rawWorld).
	 * </p>
	 */
	private double xWorldToData(double wx) {
	    return _xLogActive ? Math.pow(10.0, wx) : wx;
	}

	/**
	 * Raw-world -> data conversion for Y.
	 * <p>
	 * When log axes are active, data is 10^(rawWorld).
	 * </p>
	 */
	private double yWorldToData(double wy) {
	    return _yLogActive ? Math.pow(10.0, wy) : wy;
	}

}
