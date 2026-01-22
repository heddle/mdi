package edu.cnu.mdi.splot.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import edu.cnu.mdi.util.Environment;

@SuppressWarnings("serial")
public class PlotCanvas extends JComponent
		implements MouseListener, MouseMotionListener, DataChangeListener {

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

	// default values for margins
	private int _topMargin = 10;
	private int _rightMargin = 10;

	// for saving files
	private static String _dataFilePath;

	// the bounds of the plot
	private Rectangle _activeBounds;

	// redraw check for dynamic data adding
	private boolean _needsRedraw;
	private boolean _needsRescale;

	// the world system of the active area
	private Rectangle2D.Double _worldSystem = new Rectangle2D.Double(0, 0, 1, 1);

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

	// reusable point
	public Point2D.Double _workPoint = new Point2D.Double();

	// toolbar that owns this canvas
	private BaseToolBar _toolBar;

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

		// every canvas has a swing timer
		int delay = 1000; // milliseconds
		ActionListener taskPerformer = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (_needsRedraw) {
					if (_needsRescale) {
						setWorldSystem();
					}
					repaint();
				}
				_needsRescale = false;
				_needsRedraw = false;
			}
		};
		new Timer(delay, taskPerformer).start();

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

		_plotData = plotData;

		plotData.removeDataChangeListener(this);
		plotData.addDataChangeListener(this);
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
	 * Get the world boundary
	 *
	 * @return the world boundary
	 */
	public Rectangle.Double getWorld() {
		return _worldSystem;
	}

	/**
	 * Set the world system based on the plot data This is where the plot limits are
	 * set.
	 */
	public void setWorldSystem() {

		if (_worldSystem == null) {
			_worldSystem = new Rectangle2D.Double();
		}

		// watch for no plot data
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

		// the limits methods are in the plot parameters
		// defaults are ALGORITHMICLIMITS
		PlotParameters params = getParameters();

		LimitsMethod xMethod = params.getXLimitsMethod();
		LimitsMethod yMethod = params.getYLimitsMethod();

		switch (xMethod) {
		case MANUALLIMITS:
			xmin = params.getManualXMin();
			xmax = params.getManualXMax();
			break;

		case ALGORITHMICLIMITS:
			NiceScale ns = new NiceScale(xmin, xmax, _plotTicks.getNumMajorTickX() + 2, _parameters.includeXZero());
			xmin = ns.getNiceMin();
			xmax = ns.getNiceMax();
			break;

		case USEDATALIMITS: // do nothing
			break;
		}

		switch (yMethod) {
		case MANUALLIMITS:
			ymin = params.getManualYMin();
			ymax = params.getManualYMax();
			break;

		case ALGORITHMICLIMITS:
			NiceScale ns = new NiceScale(ymin, ymax, _plotTicks.getNumMajorTickY() + 2, _parameters.includeYZero());
			ymin = ns.getNiceMin();
			ymax = ns.getNiceMax();
			break;

		case USEDATALIMITS: // do nothing
			break;
		}

		// ------------------------------------------------------------------
		// Guard against degenerate or invalid ranges.
		//
		// With USEDATALIMITS (common for strip charts), the very first sample
		// can yield xmin == xmax and/or ymin == ymax. That creates a world
		// rectangle with zero width/height, which in turn produces a non-
		// invertible AffineTransform (scale=0) in setAffineTransforms().
		//
		// We expand zero (or near-zero) ranges slightly to keep transforms
		// invertible and allow the plot to draw immediately.
		// ------------------------------------------------------------------
		if (!Double.isFinite(xmin) || !Double.isFinite(xmax) || !Double.isFinite(ymin) || !Double.isFinite(ymax)) {
			_worldSystem.setFrame(0, 0, 1, 1);
			return;
		}

		// Ensure min <= max
		if (xmax < xmin) {
			double tmp = xmin;
			xmin = xmax;
			xmax = tmp;
		}
		if (ymax < ymin) {
			double tmp = ymin;
			ymin = ymax;
			ymax = tmp;
		}

		double dx = xmax - xmin;
		double dy = ymax - ymin;

		// Expand any zero/near-zero range
		if (Math.abs(dx) < 1.0e-12) {
			double pad = (Math.abs(xmin) > 0) ? (0.01 * Math.abs(xmin)) : 1.0;
			xmin -= pad;
			xmax += pad;
		}
		if (Math.abs(dy) < 1.0e-12) {
			double pad = (Math.abs(ymin) > 0) ? (0.01 * Math.abs(ymin)) : 1.0;
			ymin -= pad;
			ymax += pad;
		}

		_worldSystem.setFrame(xmin, ymin, xmax - xmin, ymax - ymin);
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
			top += _topMargin;
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

	    if (_worldSystem == null || _activeBounds == null ||
	        _activeBounds.width < 1 || _activeBounds.height < 1) {
	        return;
	    }

	    final boolean rx = reverseX();
	    final boolean ry = reverseY();

	    // Magnitudes
	    final double sxMag = _worldSystem.width  / _activeBounds.width;
	    final double syMag = _worldSystem.height / _activeBounds.height;

	    // Choose which world edge maps to the local top-left of the active plot area
	    final double tx = rx ? _worldSystem.getMaxX() : _worldSystem.getMinX();
	    final double ty = ry ? _worldSystem.getMinY() : _worldSystem.getMaxY();

	    // Sign controls axis direction on screen
	    final double sx = rx ? -sxMag :  sxMag;
	    final double sy = ry ?  syMag : -syMag;

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

	
	// Get the transforms for world to local and vice versa
	protected void XsetAffineTransforms() {
		Rectangle bounds = getBounds();

		if ((bounds == null) || (bounds.width < 1) || (bounds.height < 1)) {
			_localToWorld = null;
			_worldToLocal = null;
			_activeBounds = null;
			return;
		}

		setActiveBounds();

		if (_worldSystem == null) {
			return;
		}

		double scaleX = _worldSystem.width / _activeBounds.width;
		double scaleY = _worldSystem.height / _activeBounds.height;

		_localToWorld = AffineTransform.getTranslateInstance(_worldSystem.x, _worldSystem.getMaxY());
		_localToWorld.concatenate(AffineTransform.getScaleInstance(scaleX, -scaleY));
		_localToWorld.concatenate(AffineTransform.getTranslateInstance(-_activeBounds.x, -_activeBounds.y));

		try {
			_worldToLocal = _localToWorld.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
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

		Point pp = e.getPoint();
		FeedbackPane feedback = plotPanel().getFeedbackPane();

		if ((_activeBounds == null) || (_worldSystem == null)) {
			return;
		}

		feedback.clear();
		// location string

		pp.x -= _activeBounds.x;
		pp.y -= _activeBounds.y;
		localToWorld(pp, _workPoint);
		feedback.append(String.format("(x, y) = (%7.2g, %-7.2g)", _workPoint.x, _workPoint.y));

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
				String cStr = HistoData.statusString(this, hd, pp, _workPoint);
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
		notifyListeners(PlotChangeType.SHUTDOWN);
	}
	/**
	 * The plot is being stood up
	 */
	public void standUp() {
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
	 * This converts a screen or pixel point to a world point.
	 *
	 * @param pp contains the local (screen-pixel) point.
	 * @param wp will hold the resultant world point.
	 */
	public void localToWorld(Point pp, Point.Double wp) {
		if (_localToWorld != null) {
			_localToWorld.transform(pp, wp);
		}
	}

	/**
	 * This converts a screen or pixel rectangle to a world rectangle.
	 *
	 * @param pr contains the local (screen-pixel) rectangle.
	 * @param wr will hold the resultant world rectangle.
	 */
	public void localToWorld(Rectangle pr, Rectangle2D.Double wr) {
		if (_localToWorld != null) {
			int l = pr.x;
			int t = pr.y;
			int r = l + pr.width;
			int b = t + pr.height;
			Point pplt = new Point(l, t);
			Point pprb = new Point(r, b);
			Point.Double wplt = new Point.Double();
			Point.Double wprb = new Point.Double();
			_localToWorld.transform(pplt, wplt);
			_localToWorld.transform(pprb, wprb);
			wr.x = Math.min(wplt.x, wprb.x);
			wr.y = Math.min(wplt.y, wprb.y);
			wr.width = Math.abs(wplt.x - wprb.x);
			wr.height = Math.abs(wplt.y - wprb.y);
		}
	}

	/**
	 * This converts a world point to a screen or pixel point.
	 *
	 * @param pp will hold the resultant local (screen-pixel) point.
	 * @param wp contains world point.
	 */
	public void worldToLocal(Point pp, Point.Double wp) {
		if (_worldToLocal != null) {
			_worldToLocal.transform(wp, pp);
		}
	}

	public void worldToLocal(Rectangle r, Rectangle.Double wr) {
		// New version to accommodate world with x decreasing right
		Point2D.Double wp0 = new Point2D.Double(wr.getMinX(), wr.getMinY());
		Point2D.Double wp1 = new Point2D.Double(wr.getMaxX(), wr.getMaxY());
		Point p0 = new Point();
		Point p1 = new Point();
		worldToLocal(p0, wp0);
		worldToLocal(p1, wp1);

		int x = Math.min(p0.x, p1.x);
		int y = Math.min(p0.y, p1.y);
		int w = Math.abs(p1.x - p0.x);
		int h = Math.abs(p1.y - p0.y);
		r.setBounds(x, y, w, h);
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

		localToWorld(rbrect, _worldSystem);
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
		needsRedraw(false);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

}
