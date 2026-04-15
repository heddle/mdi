package edu.cnu.mdi.splot.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.event.EventListenerList;

import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.splot.edit.PlotPreferencesDialog;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.CurveChangeType;
import edu.cnu.mdi.splot.pdata.DataChangeListener;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.pdata.Snapshot;

/**
 * The primary Swing component for rendering a 2-D scientific plot.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Owning the coordinate-system geometry ({@link #_worldSystem},
 *       {@link #setWorldSystem()}, affine transforms)</li>
 *   <li>Painting: delegating to {@link DataDrawer}, {@link PlotTicks},
 *       {@link Legend}, and {@link ExtraText}</li>
 *   <li>Notifying {@link PlotChangeListener}s on lifecycle events</li>
 *   <li>Providing the public coordinate-conversion API (screen ↔ raw-world
 *       ↔ data)</li>
 *   <li>Data management: {@link #setPlotData}, {@link #clearData}</li>
 * </ul>
 *
 * <h3>Mouse handling</h3>
 * <p>All mouse interaction (legend dragging, feedback readout, scroll-wheel
 * zoom) is delegated to {@link PlotMouseHandler}, which is wired up in the
 * constructor. {@code PlotCanvas} no longer implements any mouse listener
 * interfaces directly.</p>
 *
 * <h3>Coordinate systems</h3>
 * <p>Three coordinate spaces are in play:</p>
 * <ol>
 *   <li><b>Screen (local)</b> &mdash; Swing pixel coordinates,
 *       {@code (0,0)} at top-left.</li>
 *   <li><b>Raw world</b> &mdash; stored in {@link #_worldSystem};
 *       <em>log₁₀(data)</em> when a log axis is active.</li>
 *   <li><b>Data</b> &mdash; the real values in the curves, always linear.</li>
 * </ol>
 * <p>Conversion methods are named {@code screen*}, {@code *RawWorld*}, and
 * {@code *Data*} to make the direction explicit.</p>
 *
 * @author heddle
 * @see PlotMouseHandler
 * @see DataDrawer
 * @see PlotTicks
 */
@SuppressWarnings("serial")
public class PlotCanvas extends JComponent implements DataChangeListener {

	// -----------------------------------------------------------------------
	// Property-change event names (fired via firePropertyChange)
	// -----------------------------------------------------------------------

	/** Fired after every completed paint pass. */
	public static final String DONEDRAWINGPROP   = "Done Drawing";
	/** Fired when the plot title text changes. */
	public static final String TITLETEXTCHANGE   = "Title Text";
	/** Fired when the plot title font changes. */
	public static final String TITLEFONTCHANGE   = "Title Font";
	/** Fired when the x-axis label text changes. */
	public static final String XLABELTEXTCHANGE  = "X Label Text";
	/** Fired when the y-axis label text changes. */
	public static final String YLABELTEXTCHANGE  = "Y Label Text";
	/** Fired when the axes font changes. */
	public static final String AXESFONTCHANGE    = "Axes Font";
	/** Fired when the status/feedback font changes. */
	public static final String STATUSFONTCHANGE  = "Status Font";
	/** Fired when log-Z mode changes (2D histograms). */
	public static final String LOGZCHANGE        = "Log Z";
	/** Fired when the color map changes (2D histograms / heatmaps). */
	public static final String COLORMAPCHANGE    = "Colormap";

	// -----------------------------------------------------------------------
	// Fields
	// -----------------------------------------------------------------------

	/** Listeners for {@link PlotChangeType} lifecycle events. */
	private EventListenerList _listenerList;

	/** Monotonically increasing counter used to tag {@link #DONEDRAWINGPROP} events. */
	private long drawCount = 0;


	/** Pixel bounds of the active (data) area inside the component. */
	private Rectangle _activeBounds;

	/** Signals that a redraw is pending (set by background data producers). */
	private boolean _needsRedraw;

	/** Signals that the world system should also be recomputed on the next redraw. */
	private boolean _needsRescale;

	/**
	 * The visible region in <em>raw world</em> coordinates.
	 * When log axes are active this is in log₁₀(data) space.
	 */
	private Rectangle2D.Double _worldSystem = new Rectangle2D.Double(0, 0, 1, 1);

	/** {@code true} when log-10 x-scaling is both requested and valid (range > 0). */
	private boolean _xLogActive = false;

	/** {@code true} when log-10 y-scaling is both requested and valid (range > 0). */
	private boolean _yLogActive = false;

	/** Transforms screen → raw-world (null when component has no valid size). */
	protected AffineTransform _screenToRawWorld;

	/** Transforms raw-world → screen (null when component has no valid size). */
	protected AffineTransform _rawWorldToScreen;

	/** The data backing this canvas. */
	protected PlotData _plotData;

	/** Visual and numerical parameters (axis labels, fonts, log scale, etc.). */
	protected PlotParameters _parameters;

	/** Tick-mark generator and renderer. */
	private PlotTicks _plotTicks;

	/**
	 * Optional parent component (usually a {@link PlotPanel}).
	 * When present, print actions apply to the parent.
	 */
	private Component _parent;

	/** Draggable legend overlay. */
	private Legend _legend;

	/** Draggable extra-text overlay. */
	private ExtraText _extra;

	/** Delegates data drawing to type-specific drawers. */
	private DataDrawer _dataDrawer;

	/** Optional toolbar; used to determine whether the pointer tool is active. */
	private BaseToolBar _toolBar;

	/** Coalescing timer that drives redraws for streaming/DAQ data. */
	private final Timer _timer;

	// -----------------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------------

	/**
	 * Creates a {@code PlotCanvas} for the given dataset.
	 *
	 * @param plotData  the initial dataset ({@code null} → empty XYXY data)
	 * @param plotTitle plot title (may be {@code null})
	 * @param xLabel    x-axis label (may be {@code null})
	 * @param yLabel    y-axis label (may be {@code null})
	 */
	public PlotCanvas(PlotData plotData, String plotTitle,
	                  String xLabel, String yLabel) {

		if (plotData == null) {
			plotData = PlotData.emptyData();
		}

		setBackground(Color.white);
		_parameters = new PlotParameters(this);
		_parameters.setPlotTitle(plotTitle);
		_parameters.setXLabel(xLabel);
		_parameters.setYLabel(yLabel);
		_plotTicks  = new PlotTicks(this);

		setPlotData(plotData);

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent ce) {
				setAffineTransforms();
				repaint();
			}
		});

		_legend     = new Legend(this);
		_extra      = new ExtraText(this);
		_dataDrawer = new DataDrawer(this);

		// Delegate legend-drag, feedback readout, and scroll-wheel zoom
		// to PlotMouseHandler. The toolbar gesture handler (PlotToolHandler)
		// is wired separately by PlotPanel.
		PlotMouseHandler mouseHandler = new PlotMouseHandler(this);
		addMouseListener(mouseHandler);
		addMouseMotionListener(mouseHandler);
		addMouseWheelListener(mouseHandler);

		// Coalescing redraw timer (1 s interval).
		_timer = new Timer(1000, evt -> onTimer());
		_timer.setCoalesce(true);
	}

	// -----------------------------------------------------------------------
	// Lifecycle
	// -----------------------------------------------------------------------

	@Override
	public void removeNotify() {
		if (_timer != null) {
			_timer.stop();
		}
		super.removeNotify();
	}

	/**
	 * Starts the timer and notifies listeners that the canvas is active.
	 */
	public void standUp() {
		if (_timer != null) {
			_timer.start();
		}
		notifyListeners(PlotChangeType.STOODUP);
	}

	/**
	 * Stops the timer and notifies listeners that the canvas is shutting down.
	 */
	public void shutDown() {
		if (_timer != null) {
			_timer.stop();
		}
		notifyListeners(PlotChangeType.SHUTDOWN);
	}

	// -----------------------------------------------------------------------
	// Accessors used by PlotMouseHandler (package-visible)
	// -----------------------------------------------------------------------

	/**
	 * Returns the draggable legend overlay.
	 *
	 * @return the {@link Legend} (never {@code null} after construction)
	 */
	Legend getLegend() {
		return _legend;
	}

	/**
	 * Returns the draggable extra-text overlay.
	 *
	 * @return the {@link ExtraText} (never {@code null} after construction)
	 */
	ExtraText getExtra() {
		return _extra;
	}

	/**
	 * Returns {@code true} when the pointer (select) tool is active.
	 * <p>
	 * Used by {@link PlotMouseHandler} to decide whether mouse-press events
	 * should prime a drag.
	 * </p>
	 *
	 * @return {@code true} if there is no toolbar or the toolbar's pointer tool
	 *         is selected
	 */
	boolean isPointer() {
		return (_toolBar == null) || _toolBar.isPointerActive();
	}

	// -----------------------------------------------------------------------
	// Data management
	// -----------------------------------------------------------------------

	/**
	 * Replaces the dataset and recomputes the world system.
	 *
	 * @param plotData the new dataset ({@code null} → empty XYXY data)
	 */
	protected void setPlotData(PlotData plotData) {
		if (plotData == null) {
			plotData = PlotData.emptyData();
		}

		if (_plotData != null) {
			_plotData.removeDataChangeListener(this);
		}

		_plotData = plotData;
		_plotData.addDataChangeListener(this);
		setWorldSystem();
		repaint();
	}

	/**
	 * Returns the dataset backing this canvas.
	 *
	 * @return the current {@link PlotData} (never {@code null} after
	 *         construction)
	 */
	public PlotData getPlotData() {
		return _plotData;
	}

	/**
	 * Clears all data from every curve.
	 *
	 * @see ACurve#clearData()
	 */
	public void clearData() {
		for (ACurve curve : _plotData.getCurves()) {
			curve.clearData();
		}
	}

	/**
	 * Returns the current {@link PlotDataType}.
	 *
	 * @return data type of the current dataset
	 */
	public PlotDataType getType() {
		return _plotData.getType();
	}

	// -----------------------------------------------------------------------
	// Parameters and appearance
	// -----------------------------------------------------------------------

	/**
	 * Returns the plot parameters object.
	 *
	 * @return current {@link PlotParameters}
	 */
	public PlotParameters getParameters() {
		return _parameters;
	}

	/**
	 * Returns the plot title.
	 *
	 * @return title string (may be {@code null})
	 */
	public String getTitle() {
		return _parameters.getPlotTitle();
	}

	/**
	 * Returns {@code true} if this canvas is configured as a bar plot.
	 *
	 * @return {@code true} if the {@link RenderHint#BARPLOT} hint is set
	 */
	public boolean isBarPlot() {
		return _parameters.hasRenderHint(RenderHint.BARPLOT);
	}

	/**
	 * Sets the optional parent component (usually a {@link PlotPanel}).
	 *
	 * @param parent parent component, or {@code null}
	 */
	public void setParent(Component parent) {
		_parent = parent;
	}

	/**
	 * Sets the toolbar that owns this canvas.
	 *
	 * @param toolBar the toolbar, or {@code null} if there is no toolbar
	 */
	public void setToolBar(BaseToolBar toolBar) {
		_toolBar = toolBar;
	}

	/**
	 * Opens the plot preferences dialog.
	 */
	public void showPreferencesEditor() {
		PlotPreferencesDialog prefEditor = new PlotPreferencesDialog(this);
		prefEditor.setVisible(true);
		prefEditor.toFront();
	}

	// -----------------------------------------------------------------------
	// World-system and coordinate geometry
	// -----------------------------------------------------------------------

	/**
	 * Returns the visible region in raw-world coordinates.
	 * <p>
	 * When log axes are active, this rectangle is in log₁₀(data) space.
	 * </p>
	 *
	 * @return the world rectangle (never {@code null} after construction)
	 */
	public Rectangle2D.Double getWorld() {
		return _worldSystem;
	}

	/**
	 * Returns the pixel bounds of the active (data) area within the component.
	 *
	 * @return active bounds, or {@code null} if the component has no size yet
	 */
	public Rectangle getActiveBounds() {
		return _activeBounds;
	}

	/**
	 * Returns {@code true} if log-10 scaling is active on the x-axis.
	 *
	 * @return {@code true} when x log-scaling is both requested and valid
	 */
	public boolean isXLogActive() {
		return _xLogActive;
	}

	/**
	 * Returns {@code true} if log-10 scaling is active on the y-axis.
	 *
	 * @return {@code true} when y log-scaling is both requested and valid
	 */
	public boolean isYLogActive() {
		return _yLogActive;
	}

	/**
	 * Returns the tick-mark renderer for this canvas.
	 *
	 * @return the {@link PlotTicks} (never {@code null} after construction)
	 */
	public PlotTicks getPlotTicks() {
		return _plotTicks;
	}

	/**
	 * Returns the {@link PlotPanel} that contains this canvas, if any.
	 *
	 * @return the parent {@link PlotPanel}, or {@code null}
	 */
	public PlotPanel plotPanel() {
		if (_parent instanceof PlotPanel) {
			return (PlotPanel) _parent;
		}
		return null;
	}

	/**
	 * Signals that this canvas needs a repaint, optionally requesting a world
	 * rescale. Safe to call from any thread; the actual work happens on the
	 * next timer tick.
	 *
	 * @param rescale if {@code true}, the world system is recomputed before
	 *                repainting
	 */
	public void needsRedraw(boolean rescale) {
		_needsRedraw  = true;
		_needsRescale = _needsRescale || rescale;
	}

	/**
	 * Recomputes the world system from the current data and plot parameters.
	 *
	 * <p>Handles manual limits, algorithmic (NiceScale) limits, and log-axis
	 * bounds. Activates log scaling only when the final data range is strictly
	 * positive.</p>
	 */
	public void setWorldSystem() {

		if (_worldSystem == null || _plotData == null) {
			if (_worldSystem != null) {
				_worldSystem.setFrame(0, 0, 1, 1);
			}
			return;
		}

		double xmin = _plotData.xMin();
		if (Double.isNaN(xmin)) {
			return;
		}

		double xmax = _plotData.xMax();
		double ymin = _plotData.yMin();
		double ymax = _plotData.yMax();

		PlotParameters params       = getParameters();
		LimitsMethod   xMethod      = params.getXLimitsMethod();
		LimitsMethod   yMethod      = params.getYLimitsMethod();
		final boolean  xLogRequested = (params.getXScale() == PlotParameters.AxisScale.LOG10);
		final boolean  yLogRequested = (params.getYScale() == PlotParameters.AxisScale.LOG10);

		// --- X bounds ---
		switch (xMethod) {
		case MANUALLIMITS:
			xmin = params.getManualXMin();
			xmax = params.getManualXMax();
			break;
		case ALGORITHMICLIMITS:
			if (xLogRequested) {
				double[] mm = positiveDataMinMax(true);
				if (mm != null) { xmin = mm[0]; xmax = mm[1]; }
			} else {
				NiceScale ns = new NiceScale(xmin, xmax,
						_plotTicks.getNumMajorTickX() + 2, params.includeXZero());
				xmin = ns.getNiceMin(); xmax = ns.getNiceMax();
			}
			break;
		case USEDATALIMITS:
			if (xLogRequested) {
				double[] mm = positiveDataMinMax(true);
				if (mm != null) { xmin = mm[0]; xmax = mm[1]; }
			}
			break;
		}

		// --- Y bounds ---
		switch (yMethod) {
		case MANUALLIMITS:
			ymin = params.getManualYMin();
			ymax = params.getManualYMax();
			break;
		case ALGORITHMICLIMITS:
			if (yLogRequested) {
				double[] mm = positiveDataMinMax(false);
				if (mm != null) { ymin = mm[0]; ymax = mm[1]; }
			} else {
				NiceScale ns = new NiceScale(ymin, ymax,
						_plotTicks.getNumMajorTickY() + 2, params.includeYZero());
				ymin = ns.getNiceMin(); ymax = ns.getNiceMax();
			}
			break;
		case USEDATALIMITS:
			if (yLogRequested) {
				double[] mm = positiveDataMinMax(false);
				if (mm != null) { ymin = mm[0]; ymax = mm[1]; }
			}
			break;
		}

		// Guard non-finite values.
		if (!Double.isFinite(xmin) || !Double.isFinite(xmax)
				|| !Double.isFinite(ymin) || !Double.isFinite(ymax)) {
			_worldSystem.setFrame(0, 0, 1, 1);
			return;
		}

		// Ensure min ≤ max.
		if (xmax < xmin) { double t = xmin; xmin = xmax; xmax = t; }
		if (ymax < ymin) { double t = ymin; ymin = ymax; ymax = t; }

		// Expand degenerate (zero-width) ranges.
		if (Math.abs(xmax - xmin) < 1.0e-12) {
			if (xLogRequested && xmin > 0.0) { xmin /= 1.1; xmax *= 1.1; }
			else { double pad = (xmin != 0.0) ? (0.01 * Math.abs(xmin)) : 1.0; xmin -= pad; xmax += pad; }
		}
		if (Math.abs(ymax - ymin) < 1.0e-12) {
			if (yLogRequested && ymin > 0.0) { ymin /= 1.1; ymax *= 1.1; }
			else { double pad = (ymin != 0.0) ? (0.01 * Math.abs(ymin)) : 1.0; ymin -= pad; ymax += pad; }
		}

		// Enable log only if the final range is strictly positive.
		_xLogActive = xLogRequested && (xmin > 0.0) && (xmax > 0.0);
		_yLogActive = yLogRequested && (ymin > 0.0) && (ymax > 0.0);

		// Convert to raw-world space (log₁₀ if active).
		double wxmin = _xLogActive ? Math.floor(log10(xmin)) : xmin;
		double wxmax = _xLogActive ? Math.ceil(log10(xmax))  : xmax;
		double wymin = _yLogActive ? Math.floor(log10(ymin)) : ymin;
		double wymax = _yLogActive ? Math.ceil(log10(ymax))  : ymax;

		if (!Double.isFinite(wxmin) || !Double.isFinite(wxmax)
				|| !Double.isFinite(wymin) || !Double.isFinite(wymax)) {
			_worldSystem.setFrame(0, 0, 1, 1);
			return;
		}
		if (wxmax <= wxmin) {
			wxmax = wxmin + 1.0;
		}
		if (wymax <= wymin) {
			wymax = wymin + 1.0;
		}

		_worldSystem.setFrame(wxmin, wymin, wxmax - wxmin, wymax - wymin);
		setAffineTransforms();
	}

	/**
	 * Returns the visible region in <em>data</em> (linear) coordinates.
	 *
	 * @return data-space rectangle, or {@code null} if the world system is
	 *         uninitialised
	 */
	public Rectangle2D.Double getDataWorld() {
		if (_worldSystem == null) {
			return null;
		}

		double xmin = xRawWorldToData(_worldSystem.getMinX());
		double xmax = xRawWorldToData(_worldSystem.getMaxX());
		double ymin = yRawWorldToData(_worldSystem.getMinY());
		double ymax = yRawWorldToData(_worldSystem.getMaxY());

		return new Rectangle2D.Double(
				Math.min(xmin, xmax), Math.min(ymin, ymax),
				Math.abs(xmax - xmin), Math.abs(ymax - ymin));
	}

	// -----------------------------------------------------------------------
	// Painting
	// -----------------------------------------------------------------------

	/**
	 * Paints the canvas: background, data, frame, ticks, legend, extra text.
	 * Fires {@link #DONEDRAWINGPROP} when complete.
	 *
	 * @param g graphics context
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		// Skip drawing when the component has no size or active bounds (e.g. before
		// layout). This prevents drawing glitches during resizing and when the
		// canvas is first shown.
		if (_activeBounds == null) {
			return;
		}

		Rectangle b = getBounds();
		g.setColor(getBackground());
		g.fillRect(0, 0, b.width, b.height);

		setAffineTransforms();

		_dataDrawer.draw((Graphics2D) g, _plotData);

		g.setColor(Color.black);
		g.drawRect(_activeBounds.x, _activeBounds.y,
		           _activeBounds.width, _activeBounds.height);

		_plotTicks.draw((Graphics2D) g);

		if (_parameters.isLegendDrawn()) {
			_legend.draw((Graphics2D) g);
		}
		if (_parameters.extraDrawing()) {
			_extra.draw((Graphics2D) g);
		}

		firePropertyChange(DONEDRAWINGPROP, drawCount, ++drawCount);
	}

	// -----------------------------------------------------------------------
	// Zoom and scale
	// -----------------------------------------------------------------------

	/**
	 * Zooms to a rubber-band rectangle specified in screen (pixel) coordinates.
	 * Rectangles smaller than 15×15 pixels are ignored to prevent accidental
	 * micro-zooms.
	 *
	 * @param rbrect rubber-band rectangle in screen coordinates
	 */
	public void zoomToRect(Rectangle rbrect) {
		if ((rbrect.width < 15) || (rbrect.height < 15)) {
			return;
		}
		screenToRawWorld(rbrect, _worldSystem);
		setAffineTransforms();
		repaint();
	}

	/**
	 * Scales the visible area by {@code amount} around its center.
	 * Values greater than 1 zoom out; values less than 1 zoom in.
	 *
	 * @param amount scale factor ({@code > 0})
	 */
	public void scale(double amount) {
		double xc = _worldSystem.getCenterX();
		double yc = _worldSystem.getCenterY();
		double w  = _worldSystem.width  * amount;
		double h  = _worldSystem.height * amount;
		_worldSystem.setFrame(xc - w / 2, yc - h / 2, w, h);
		repaint();
	}

	// -----------------------------------------------------------------------
	// Plot-change listener management
	// -----------------------------------------------------------------------

	/**
	 * Registers a {@link PlotChangeListener} for lifecycle events.
	 * Duplicate registrations are silently de-duplicated.
	 *
	 * @param listener the listener to add (non-null)
	 */
	public void addPlotChangeListener(PlotChangeListener listener) {
		if (_listenerList == null) {
			_listenerList = new EventListenerList();
		}
		_listenerList.remove(PlotChangeListener.class, listener);
		_listenerList.add(PlotChangeListener.class, listener);
	}

	/**
	 * Removes a previously registered {@link PlotChangeListener}.
	 *
	 * @param listener the listener to remove (ignored if {@code null} or not
	 *                 registered)
	 */
	public void removePlotChangeListener(PlotChangeListener listener) {
		if (listener == null || _listenerList == null) {
			return;
		}
		_listenerList.remove(PlotChangeListener.class, listener);
	}

	/**
	 * Triggers a property-change event on behalf of another object (e.g. a
	 * sub-component that cannot fire its own property changes).
	 *
	 * @param propName property name
	 * @param oldValue old value
	 * @param newValue new value
	 */
	public void remoteFirePropertyChange(String propName,
	                                     Object oldValue, Object newValue) {
		firePropertyChange(propName, oldValue, newValue);
	}

	// -----------------------------------------------------------------------
	// DataChangeListener
	// -----------------------------------------------------------------------

	/**
	 * Called by the data model when curve data changes. Schedules a rescaled
	 * redraw.
	 *
	 * @param plotData the plot data that changed
	 * @param curve    the specific curve that changed
	 * @param type     the type of change
	 */
	@Override
	public void dataSetChanged(PlotData plotData, ACurve curve, CurveChangeType type) {
		setWorldSystem();
		needsRedraw(true);
	}

	/*
	 * ==========================================================================
	 * Coordinate-conversion API
	 * ==========================================================================
	 *
	 * Three coordinate spaces:
	 *
	 * 1) Screen (local) — Swing pixel coords, (0,0) = top-left.
	 * 2) Raw world      — stored in _worldSystem; log₁₀(data) when log active.
	 * 3) Data           — real values in curves, always linear.
	 *
	 * Naming rule:
	 *   *RawWorld* ↔ raw-world space (log₁₀ when active)
	 *   *Data*     ↔ data space (always linear)
	 *
	 * Zoom: use screenToRawWorld → assign into _worldSystem.
	 * Readout: use screenToData → display real values.
	 * Drawing: use dataToScreen → handles log conversion internally.
	 * ==========================================================================
	 */

	/**
	 * Converts a screen (pixel) point to raw-world coordinates.
	 * <p>
	 * Raw world is the coordinate system of {@link #_worldSystem}. When log axes
	 * are active, raw world is in log₁₀(data) space. Use this for rubber-band
	 * zoom operations.
	 * </p>
	 *
	 * @param screenPoint screen point in component coordinates (non-null)
	 * @param rawPoint    output point filled with raw-world coordinates (non-null)
	 * @throws IllegalArgumentException if either argument is {@code null}
	 */
	public void screenToRawWorld(Point screenPoint, Point2D.Double rawPoint) {
		if (rawPoint == null) {
			throw new IllegalArgumentException("rawPoint must not be null");
		}
		if (screenPoint == null) {
			throw new IllegalArgumentException("screenPoint must not be null");
		}
		if (_screenToRawWorld == null) {
			return;
		}
		_screenToRawWorld.transform(screenPoint, rawPoint);
	}

	/**
	 * Converts a screen (pixel) rectangle to a raw-world rectangle.
	 * <p>
	 * Use this for rubber-band zoom: the result can be assigned directly into
	 * {@link #_worldSystem}.
	 * </p>
	 *
	 * @param screenRect screen rectangle in component coordinates
	 * @param rawRect    output rectangle in raw-world coordinates (non-null)
	 * @throws IllegalArgumentException if {@code rawRect} is {@code null}
	 */
	public void screenToRawWorld(Rectangle screenRect, Rectangle2D.Double rawRect) {
		if (rawRect == null) {
			throw new IllegalArgumentException("rawRect must not be null");
		}
		if (_screenToRawWorld == null || screenRect == null) {
			return;
		}

		Point p0 = new Point(screenRect.x, screenRect.y);
		Point p1 = new Point(screenRect.x + screenRect.width,
		                     screenRect.y + screenRect.height);
		Point2D.Double w0 = new Point2D.Double();
		Point2D.Double w1 = new Point2D.Double();
		_screenToRawWorld.transform(p0, w0);
		_screenToRawWorld.transform(p1, w1);

		rawRect.setFrame(
				Math.min(w0.x, w1.x), Math.min(w0.y, w1.y),
				Math.abs(w1.x - w0.x), Math.abs(w1.y - w0.y));
	}

	/**
	 * Converts a screen (pixel) point to data coordinates (always linear units).
	 * <p>
	 * Use this for mouse readouts and hit-tests in data space.
	 * </p>
	 *
	 * @param screenPoint screen point in component coordinates
	 * @param dataPoint   output point in data coordinates (non-null)
	 * @throws IllegalArgumentException if {@code dataPoint} is {@code null}
	 */
	public void screenToData(Point screenPoint, Point2D.Double dataPoint) {
		if (dataPoint == null) {
			throw new IllegalArgumentException("dataPoint must not be null");
		}
		if (_screenToRawWorld == null || screenPoint == null) {
			return;
		}

		_screenToRawWorld.transform(screenPoint, dataPoint);
		dataPoint.x = xRawWorldToData(dataPoint.x);
		dataPoint.y = yRawWorldToData(dataPoint.y);
	}

	/**
	 * Converts a screen rectangle to data-space bounds (always linear units).
	 * <p>
	 * Do <em>not</em> use this to update {@link #_worldSystem}; for zoom you
	 * want raw-world coordinates.
	 * </p>
	 *
	 * @param screenRect screen rectangle in component coordinates
	 * @param dataRect   output rectangle in data coordinates (non-null)
	 * @throws IllegalArgumentException if {@code dataRect} is {@code null}
	 */
	public void screenToData(Rectangle screenRect, Rectangle2D.Double dataRect) {
		if (dataRect == null) {
			throw new IllegalArgumentException("dataRect must not be null");
		}
		if (_screenToRawWorld == null || screenRect == null) {
			return;
		}

		Rectangle2D.Double wr = new Rectangle2D.Double();
		screenToRawWorld(screenRect, wr);

		double xmin = xRawWorldToData(wr.getMinX());
		double xmax = xRawWorldToData(wr.getMaxX());
		double ymin = yRawWorldToData(wr.getMinY());
		double ymax = yRawWorldToData(wr.getMaxY());

		dataRect.x      = Math.min(xmin, xmax);
		dataRect.y      = Math.min(ymin, ymax);
		dataRect.width  = Math.abs(xmax - xmin);
		dataRect.height = Math.abs(ymax - ymin);
	}

	/**
	 * Converts a data point (linear units) to screen (pixel) coordinates.
	 * <p>
	 * Handles log-axis conversion internally (data → log₁₀ → raw-world →
	 * screen).
	 * </p>
	 *
	 * @param screenPt  output screen point (non-null)
	 * @param dataPoint input data point in linear units (non-null)
	 * @return {@code true} on success; {@code false} if the point is invalid for
	 *         the current axes (e.g. non-positive on a log axis) or transforms
	 *         are unavailable
	 */
	public boolean dataToScreen(Point screenPt, Point2D.Double dataPoint) {
		if (_rawWorldToScreen == null || screenPt == null || dataPoint == null) {
			return false;
		}
		if (!Double.isFinite(dataPoint.x) || !Double.isFinite(dataPoint.y)) {
			return false;
		}
		if ((_xLogActive && dataPoint.x <= 0.0) || (_yLogActive && dataPoint.y <= 0.0)) {
			return false;
		}

		Point2D.Double wsrc = new Point2D.Double(
				xDataToRawWorld(dataPoint.x), yDataToRawWorld(dataPoint.y));
		Point2D.Double dst = new Point2D.Double();
		_rawWorldToScreen.transform(wsrc, dst);

		screenPt.x = (int) Math.round(dst.x);
		screenPt.y = (int) Math.round(dst.y);
		return true;
	}

	/**
	 * Converts a data rectangle (linear units) to a screen rectangle.
	 *
	 * @param screenRect output screen rectangle (non-null)
	 * @param dataRect   input data rectangle in linear units (non-null)
	 * @return {@code true} on success; {@code false} if any corner is invalid for
	 *         the current axes or transforms are unavailable
	 */
	public boolean dataToScreen(Rectangle screenRect, Rectangle2D.Double dataRect) {
		if (screenRect == null || dataRect == null) {
			return false;
		}

		Point p0 = new Point(), p1 = new Point();
		if (!dataToScreen(p0, new Point2D.Double(dataRect.getMinX(), dataRect.getMinY()))
				|| !dataToScreen(p1, new Point2D.Double(dataRect.getMaxX(), dataRect.getMaxY()))) {
			return false;
		}

		int x = Math.min(p0.x, p1.x);
		int y = Math.min(p0.y, p1.y);
		int w = Math.max(Math.abs(p1.x - p0.x), 1);
		int h = Math.max(Math.abs(p1.y - p0.y), 1);
		screenRect.setBounds(x, y, w, h);
		return true;
	}

	/**
	 * Converts a data rectangle specified by two corners to a screen rectangle.
	 *
	 * @param dataX0 data-space x₀ (linear)
	 * @param dataY0 data-space y₀ (linear)
	 * @param dataX1 data-space x₁ (linear)
	 * @param dataY1 data-space y₁ (linear)
	 * @return screen rectangle, or {@code null} if any corner is invalid
	 */
	public Rectangle dataToScreen(double dataX0, double dataY0,
	                              double dataX1, double dataY1) {
		Point p0 = new Point(), p1 = new Point();
		if (!dataToScreen(p0, new Point2D.Double(dataX0, dataY0))
				|| !dataToScreen(p1, new Point2D.Double(dataX1, dataY1))) {
			return null;
		}
		int x = Math.min(p0.x, p1.x);
		int y = Math.min(p0.y, p1.y);
		return new Rectangle(x, y,
				Math.max(Math.abs(p1.x - p0.x), 1),
				Math.max(Math.abs(p1.y - p0.y), 1));
	}

	/**
	 * Converts a single data point to a screen point.
	 *
	 * @param dataX data-space x (linear)
	 * @param dataY data-space y (linear)
	 * @return screen point, or {@code null} if the point is invalid for the
	 *         current axes
	 */
	public Point dataToScreen(double dataX, double dataY) {
		Point p = new Point();
		return dataToScreen(p, new Point2D.Double(dataX, dataY)) ? p : null;
	}

	// -----------------------------------------------------------------------
	// Private coordinate helpers
	// -----------------------------------------------------------------------

	/** Data → raw-world for x (log₁₀ when x log is active). */
	private double xDataToRawWorld(double xData) {
		return _xLogActive ? log10(xData) : xData;
	}

	/** Data → raw-world for y (log₁₀ when y log is active). */
	private double yDataToRawWorld(double yData) {
		return _yLogActive ? log10(yData) : yData;
	}

	/** Raw-world → data for x (10^raw when x log is active). */
	private double xRawWorldToData(double xRaw) {
		return _xLogActive ? Math.pow(10.0, xRaw) : xRaw;
	}

	/** Raw-world → data for y (10^raw when y log is active). */
	private double yRawWorldToData(double yRaw) {
		return _yLogActive ? Math.pow(10.0, yRaw) : yRaw;
	}

	/** log₁₀(v). */
	private static double log10(double v) {
		return Math.log(v) / Math.log(10.0);
	}

	// -----------------------------------------------------------------------
	// Private / internal helpers
	// -----------------------------------------------------------------------

	/** Timer callback: redraws (and optionally rescales) if flagged. */
	private void onTimer() {
		if (_needsRedraw) {
			if (_needsRescale) {
				setWorldSystem();
			}
			repaint();
			_needsRedraw  = false;
			_needsRescale = false;
		}
	}

	/** Notifies all registered {@link PlotChangeListener}s. */
	private void notifyListeners(PlotChangeType event) {
		if (_listenerList == null) {
			return;
		}

		Object[] listeners = _listenerList.getListenerList();
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == PlotChangeListener.class) {
				((PlotChangeListener) listeners[i + 1]).plotChanged(event);
			}
		}
	}

	/**
	 * Computes the bounding box of the active (data) plot area based on the
	 * current component size and margin parameters.
	 */
	private void setActiveBounds() {
		Rectangle bounds = getBounds();
		if (bounds == null) {
			_activeBounds = null;
			return;
		}

		int left   = 0, top    = 0;
		int right  = bounds.width;
		int bottom = bounds.height;

		int bottomMargin = 25;
		int leftMargin   = 25;

		if (_parameters.getAxesFont() != null) {
			FontMetrics fm = getFontMetrics(_parameters.getAxesFont());
			bottomMargin   = 6 + fm.getHeight();
			leftMargin     = 6 + fm.getHeight();
		}

		left  += leftMargin;
		top   += 10;  // _topMargin
		right -= 10;  // _rightMargin
		bottom -= bottomMargin;

		if (_activeBounds == null) {
			_activeBounds = new Rectangle();
		}
		_activeBounds.setBounds(left, top, right - left, bottom - top);
	}

	/** Rebuilds the affine transforms from the current world system and active bounds. */
	protected void setAffineTransforms() {
		Rectangle bounds = getBounds();
		if (bounds == null || bounds.width < 1 || bounds.height < 1) {
			_screenToRawWorld = null;
			_rawWorldToScreen = null;
			_activeBounds     = null;
			return;
		}

		setActiveBounds();
		if (_worldSystem == null || _activeBounds == null
				|| _activeBounds.width < 1 || _activeBounds.height < 1) {
			return;
		}

		final boolean rx    = _parameters.isReverseXaxis();
		final boolean ry    = _parameters.isReverseYaxis();
		final double  sxMag = _worldSystem.width  / _activeBounds.width;
		final double  syMag = _worldSystem.height / _activeBounds.height;
		final double  tx    = rx ? _worldSystem.getMaxX() : _worldSystem.getMinX();
		final double  ty    = ry ? _worldSystem.getMinY() : _worldSystem.getMaxY();
		final double  sx    = rx ? -sxMag :  sxMag;
		final double  sy    = ry ?  syMag : -syMag;

		_screenToRawWorld = AffineTransform.getTranslateInstance(tx, ty);
		_screenToRawWorld.concatenate(AffineTransform.getScaleInstance(sx, sy));
		_screenToRawWorld.concatenate(
				AffineTransform.getTranslateInstance(-_activeBounds.x, -_activeBounds.y));

		try {
			_rawWorldToScreen = _screenToRawWorld.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
			_rawWorldToScreen = null;
		}
	}

	/**
	 * Finds the minimum and maximum strictly-positive values on an axis across
	 * all curves. Used when computing log-axis bounds.
	 *
	 * @param xAxis {@code true} for the x-axis; {@code false} for y
	 * @return {@code {minPos, maxPos}}, or {@code null} if no positive finite
	 *         values exist
	 */
	private double[] positiveDataMinMax(boolean xAxis) {
		if (_plotData == null) {
			return null;
		}

		double minPos = Double.POSITIVE_INFINITY;
		double maxPos = Double.NEGATIVE_INFINITY;

		for (ACurve c : _plotData.getCurves()) {
			Snapshot s   = c.snapshot();
			double[] arr = xAxis ? s.x : s.y;
			if (arr == null) {
				continue;
			}

			for (double v : arr) {
				if (v > 0.0 && Double.isFinite(v)) {
					if (v < minPos) {
						minPos = v;
					}
					if (v > maxPos) {
						maxPos = v;
					}
				}
			}
		}

		return (minPos == Double.POSITIVE_INFINITY) ? null : new double[]{minPos, maxPos};
	}
}