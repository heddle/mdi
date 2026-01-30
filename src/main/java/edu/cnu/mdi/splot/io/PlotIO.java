package edu.cnu.mdi.splot.io;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.swing.SwingUtilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.HistoCurve;
import edu.cnu.mdi.splot.pdata.HistoData;
import edu.cnu.mdi.splot.pdata.Histo2DData;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.pdata.Snapshot;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotParameters;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * Read/write PlotSpec JSON files.
 */
public final class PlotIO {

	private PlotIO() {
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

	/** Save a plot canvas to JSON. Safe to call from any thread. */
	public static void save(PlotCanvas canvas, File file) throws IOException {
		Objects.requireNonNull(canvas, "canvas");
		Objects.requireNonNull(file, "file");

		PlotSpec spec = toSpec(canvas);

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			bw.write(GSON.toJson(spec));
		}
	}

	/** Load a PlotSpec JSON file. */
	public static PlotSpec loadSpec(File file) throws IOException {
		Objects.requireNonNull(file, "file");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			return GSON.fromJson(br, PlotSpec.class);
		}
	}

	/**
	 * Convenience: load a plot file and build a fresh PlotCanvas.
	 * <p>
	 * Ensures construction and population occur on the EDT to avoid queued-drain
	 * behaviors.
	 * </p>
	 */
	public static PlotCanvas loadCanvas(File file) throws IOException, PlotDataException {
		PlotSpec spec = loadSpec(file);

		final PlotCanvas[] out = new PlotCanvas[1];
		final Exception[] err = new Exception[1];

		Runnable build = () -> {
			try {
				PlotData data = createPlotData(spec);
				PlotParametersSpec ps = spec.parameters;

				// initial labels used in constructor; we still apply full parameters afterwards.
				String title = (ps != null && ps.plotTitle != null) ? ps.plotTitle : "No Plot";
				String xl = (ps != null && ps.xLabel != null) ? ps.xLabel : "X Data";
				String yl = (ps != null && ps.yLabel != null) ? ps.yLabel : "Y Data";

				PlotCanvas canvas = new PlotCanvas(data, title, xl, yl);
				applyParameters(spec, canvas.getParameters());
				out[0] = canvas;
			} catch (Exception e) {
				err[0] = e;
			}
		};

		if (SwingUtilities.isEventDispatchThread()) {
			build.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(build);
			} catch (Exception e) {
				throw new IOException("Failed to build plot on EDT.", e);
			}
		}

		if (err[0] != null) {
			if (err[0] instanceof PlotDataException) {
				throw (PlotDataException) err[0];
			}
			if (err[0] instanceof IOException) {
				throw (IOException) err[0];
			}
			throw new IOException("Failed to load plot.", err[0]);
		}

		return out[0];
	}

	// ---------------------------
	// Spec <-> live objects
	// ---------------------------

	private static PlotSpec toSpec(PlotCanvas canvas) {
		PlotSpec spec = new PlotSpec();

		PlotData data = canvas.getPlotData();
		PlotParameters params = canvas.getParameters();

		spec.plotDataType = data.getType();
		spec.parameters = fromParameters(params);

		if (spec.plotDataType == PlotDataType.H2D) {
			Histo2DData h2d = data.getHisto2DData();
			if (h2d != null) {
				spec.histo2d = fromHisto2D(h2d);
			}
		} else {
			for (ACurve c : data.getCurves()) {
				spec.curves.add(fromCurve(c, data.getType()));
			}
		}

		return spec;
	}
	
	// ---------------------------
	// Heatmap (Histo2DData) <-> spec
	// ---------------------------

	private static Histo2DSpec fromHisto2D(Histo2DData h2d) {
	    if (h2d == null) {
	        return null;
	    }

	    Histo2DSpec s = new Histo2DSpec();

	    // Name + geometry
	    s.name = h2d.name();
	    s.nx   = h2d.nx();
	    s.ny   = h2d.ny();
	    s.xmin = h2d.xMin();
	    s.xmax = h2d.xMax();
	    s.ymin = h2d.yMin();
	    s.ymax = h2d.yMax();

	    // Bin contents (snapshot/copy)
	    s.bins = h2d.snapshotBins();

	    // Counts
	    s.goodCount     = h2d.getGoodCount();

	    s.xUnderCount   = h2d.getXUnderCount();
	    s.xOverCount    = h2d.getXOverCount();
	    s.yUnderCount   = h2d.getYUnderCount();
	    s.yOverCount    = h2d.getYOverCount();

	    s.xUnder_yUnder = h2d.getXUnderYUnderCount();
	    s.xUnder_yOver  = h2d.getXUnderYOverCount();
	    s.xOver_yUnder  = h2d.getXOverYUnderCount();
	    s.xOver_yOver   = h2d.getXOverYOverCount();

	    return s;
	}


	private static PlotParametersSpec fromParameters(PlotParameters p) {
		PlotParametersSpec s = new PlotParametersSpec();

		s.plotTitle = p.getPlotTitle();
		s.xLabel = p.getXLabel();
		s.yLabel = p.getYLabel();

		s.reverseXaxis = p.isReverseXaxis();
		s.reverseYaxis = p.isReverseYaxis();

		// --- NEW: axis scales ---
		s.xScale = p.getXScale();
		s.yScale = p.getYScale();

		s.includeXZero = p.includeXZero();
		s.includeYZero = p.includeYZero();

		s.xLimitsMethod = p.getXLimitsMethod();
		s.yLimitsMethod = p.getYLimitsMethod();

		double xmin = p.getManualXMin();
		double xmax = p.getManualXMax();
		double ymin = p.getManualYMin();
		double ymax = p.getManualYMax();

		s.manualXmin = Double.isNaN(xmin) ? null : xmin;
		s.manualXmax = Double.isNaN(xmax) ? null : xmax;
		s.manualYmin = Double.isNaN(ymin) ? null : ymin;
		s.manualYmax = Double.isNaN(ymax) ? null : ymax;

		s.numDecimalX = p.getNumDecimalX();
		s.minExponentX = p.getMinExponentX();
		s.numDecimalY = p.getNumDecimalY();
		s.minExponentY = p.getMinExponentY();

		s.drawLegend = p.isLegendDrawn();
		s.legendBorder = p.isLegendBorder();
		s.legendLineLength = p.getLegendLineLength();

		s.drawExtra = p.extraDrawing();
		s.extraBorder = p.extraBorder();
		s.extraStrings = p.getExtraStrings();

		s.logZ = p.isLogZ();
		s.zColorMap = (p.getColorMap() == null) ? null : p.getColorMap().name();
		s.showEmptyBins = p.showEmptyBins();

		return s;
	}

	private static CurveSpec fromCurve(ACurve curve, PlotDataType type) {
		CurveSpec cs = new CurveSpec();
		cs.name = curve.name();
		cs.visible = curve.isVisible();
		cs.drawingMethod = curve.getCurveDrawingMethod();
		cs.fitOrder = curve.getFitOrder();
		cs.style = fromStyle(curve.getStyle());

		if (curve instanceof HistoCurve hc) {
			HistoData hd = hc.getHistoData();
			HistoSpec hs = new HistoSpec();
			hs.grid = hd.getGridCopy();
			hs.counts = hd.getCountsCopy();
			hs.underCount = hd.getUnderCount();
			hs.overCount = hd.getOverCount();
			cs.histo = hs;
			return cs;
		}


		// Heatmap / 2D histogram (best-effort via reflection to avoid tight coupling)
		try {
			Object h2 = invokeGetter(curve, "getHisto2DData");
			if (h2 instanceof Histo2DData h2d) {
				Histo2DSpec hs2 = new Histo2DSpec();
				hs2.nx = h2d.nx();
				hs2.ny = h2d.ny();
				hs2.xmin = h2d.xMin();
				hs2.xmax = h2d.xMax();
				hs2.ymin = h2d.yMin();
				hs2.ymax = h2d.yMax();
				hs2.bins = h2d.snapshotBins();
				hs2.goodCount = h2d.getGoodCount();
				hs2.xUnderCount = h2d.getXUnderCount();
				hs2.xOverCount = h2d.getXOverCount();
				hs2.yUnderCount = h2d.getYUnderCount();
				hs2.yOverCount = h2d.getYOverCount();
				hs2.xUnder_yUnder = h2d.getXUnderYUnderCount();
				hs2.xUnder_yOver = h2d.getXUnderYOverCount();
				hs2.xOver_yUnder = h2d.getXOverYUnderCount();
				hs2.xOver_yOver = h2d.getXOverYOverCount();
				cs.histo2d = hs2;
				return cs;
			}
		} catch (Exception ignore) {
			// not a heatmap curve
		}
		if (curve instanceof Curve xy) {
			Snapshot snap = xy.snapshot();
			cs.x = snap.x;
			cs.y = snap.y;
			cs.e = snap.e;
			return cs;
		}

		// StripChartCurve etc: treat as XY snapshot
		Snapshot snap = curve.snapshot();
		cs.x = snap.x;
		cs.y = snap.y;
		cs.e = snap.e;

		return cs;
	}

	private static StyleSpec fromStyle(IStyled st) {
		if (st == null) {
			return null;
		}

		StyleSpec ss = new StyleSpec();
		ss.symbolType = st.getSymbolType();
		ss.lineStyle = st.getLineStyle();
		ss.fillColorARGB = (st.getFillColor() == null) ? null : st.getFillColor().getRGB();
		ss.borderColorARGB = (st.getBorderColor() == null) ? null : st.getBorderColor().getRGB();
		ss.lineColorARGB = (st.getLineColor() == null) ? null : st.getLineColor().getRGB();
		ss.symbolSize = st.getSymbolSize();
		ss.lineWidth = st.getLineWidth();
		return ss;
	}

	/** Build PlotData from a spec (must be called on EDT). */
	private static PlotData createPlotData(PlotSpec spec) throws PlotDataException {
		Objects.requireNonNull(spec, "spec");
	    Objects.requireNonNull(spec.plotDataType, "spec.plotDataType");

	    PlotDataType type = spec.plotDataType;

	    // Handle H2D first (no curves expected)
	    if (type == PlotDataType.H2D) {
	        Histo2DData h2d = buildH2D(spec);      // helper shown below
	        return newPlotDataForH2D(h2d);         // your existing helper
	    }

	    // For everything else, curves are required
	    List<CurveSpec> curves = spec.curves;
	    if (curves == null || curves.isEmpty()) {
	        return PlotData.emptyData();
	    }
		switch (type) {
		case H1D: {
			// One HistoData per curve entry
			HistoData[] histos = new HistoData[curves.size()];
			for (int i = 0; i < curves.size(); i++) {
				CurveSpec cs = curves.get(i);
				if (cs.histo == null || cs.histo.grid == null || cs.histo.counts == null) {
					throw new PlotDataException("Missing histogram data for curve " + i);
				}
				String name = (cs.name != null) ? cs.name : ("Histo " + (i + 1));
				HistoData hd = new HistoData(name, cs.histo.grid);
				hd.setCountsForDeserialization(cs.histo.counts, cs.histo.underCount, cs.histo.overCount);
				histos[i] = hd;
			}

			PlotData pd = new PlotData(histos);

			// Apply per-curve props (visibility, style, method)
			applyCurveProps(pd, curves);
			return pd;
		}


		case H2D: {
			Histo2DSpec hs2 = spec.histo2d;
			// Backward compatibility: older format stored heatmap in curves[0].histo2d
			if (hs2 == null && spec.curves != null && !spec.curves.isEmpty()) {
				hs2 = spec.curves.get(0).histo2d;
				if (hs2 != null && hs2.name == null) {
					hs2.name = spec.curves.get(0).name;
				}
			}
			if (hs2 == null || hs2.bins == null) {
				throw new PlotDataException("Missing 2D histogram data.");
			}
			String name = (hs2.name != null) ? hs2.name : "Heatmap";
			Histo2DData h2d = new Histo2DData(name, hs2.xmin, hs2.xmax, hs2.nx, hs2.ymin, hs2.ymax, hs2.ny);
			h2d.setBinsForDeserialization(hs2.bins, hs2.goodCount,
				hs2.xUnderCount, hs2.xOverCount,
				hs2.yUnderCount, hs2.yOverCount,
				hs2.xUnder_yUnder, hs2.xUnder_yOver,
				hs2.xOver_yUnder, hs2.xOver_yOver);

			PlotData pd = new PlotData(h2d);
			return pd;
		}

		case XYXY:
		case XYEXYE: {
			String[] names = new String[curves.size()];
			int[] fitOrders = new int[curves.size()];
			for (int i = 0; i < curves.size(); i++) {
				CurveSpec cs = curves.get(i);
				names[i] = (cs.name != null) ? cs.name : ("Curve " + (i + 1));
				fitOrders[i] = cs.fitOrder;
			}

			PlotData pd = new PlotData(type, names, fitOrders);

			// Fill data + style
			for (int i = 0; i < curves.size(); i++) {
				ACurve c = pd.getCurves().get(i);
				CurveSpec cs = curves.get(i);

				if (c instanceof Curve xy) {
					if (cs.x != null && cs.y != null) {
						if (type == PlotDataType.XYEXYE && cs.e != null) {
							xy.addAll(cs.x, cs.y, cs.e);
						} else {
							xy.addAll(cs.x, cs.y);
						}
					}
				}

				c.setName(names[i]);
				c.setVisible(cs.visible);
				c.setFitOrder(cs.fitOrder);
				c.setCurveDrawingMethod(cs.drawingMethod != null ? cs.drawingMethod : CurveDrawingMethod.NONE);
				applyStyle(cs.style, c.getStyle());
			}

			return pd;
		}

		case STRIP:
			throw new PlotDataException("STRIP persistence not supported in v1 (use XY snapshot instead).");
		}

		throw new PlotDataException("Unsupported plot type: " + type);
	}

	// Helper to build Histo2DData from spec (with backward compatibility)
	private static Histo2DData buildH2D(PlotSpec spec) throws PlotDataException {
	    Histo2DSpec hs2 = spec.histo2d;

	    // Backward compatibility: older format stored heatmap in curves[0].histo2d
	    if (hs2 == null && spec.curves != null && !spec.curves.isEmpty()) {
	        hs2 = spec.curves.get(0).histo2d;
	        if (hs2 != null && hs2.name == null) {
	            hs2.name = spec.curves.get(0).name;
	        }
	    }
	    if (hs2 == null || hs2.bins == null) {
	        throw new PlotDataException("Missing 2D histogram data.");
	    }

	    String name = (hs2.name != null) ? hs2.name : "Heatmap";

	    Histo2DData h2d = new Histo2DData(name,
	            hs2.xmin, hs2.xmax, hs2.nx,
	            hs2.ymin, hs2.ymax, hs2.ny);

	    h2d.setBinsForDeserialization(hs2.bins, hs2.goodCount,
	            hs2.xUnderCount, hs2.xOverCount,
	            hs2.yUnderCount, hs2.yOverCount,
	            hs2.xUnder_yUnder, hs2.xUnder_yOver,
	            hs2.xOver_yUnder, hs2.xOver_yOver);

	    return h2d;
	}


	private static void applyCurveProps(PlotData pd, List<CurveSpec> curves) {
		for (int i = 0; i < curves.size(); i++) {
			ACurve c = pd.getCurves().get(i);
			CurveSpec cs = curves.get(i);
			c.setVisible(cs.visible);
			c.setFitOrder(cs.fitOrder);
			c.setCurveDrawingMethod(cs.drawingMethod != null ? cs.drawingMethod : CurveDrawingMethod.NONE);
			applyStyle(cs.style, c.getStyle());
		}
	}

	
	/** Best-effort reflection helper: invoke a no-arg getter. */
	private static Object invokeGetter(Object target, String method) {
		try {
			return target.getClass().getMethod(method).invoke(target);
		} catch (Exception e) {
			return null;
		}
	}

	/** Construct PlotData for an H2D plot while tolerating constructor variations. */
	private static PlotData newPlotDataForH2D(Histo2DData h2d) throws PlotDataException {
		try {
			// Most likely: new PlotData(Histo2DData)
			return PlotData.class.getConstructor(Histo2DData.class).newInstance(h2d);
		} catch (Exception ignore) {
			// Next: new PlotData(Histo2DData[])
			try {
				return PlotData.class.getConstructor(Histo2DData[].class).newInstance((Object) new Histo2DData[] { h2d });
			} catch (Exception ignore2) {
				throw new PlotDataException("No PlotData constructor supports Histo2DData. Add PlotData(Histo2DData) or PlotData(Histo2DData[])");
			}
		}
	}


	/** Apply spec parameters onto a live PlotParameters. */
	private static void applyParameters(PlotSpec spec, PlotParameters p) {
		PlotParametersSpec s = spec.parameters;
		if (s == null) {
			return;
		}

		if (s.plotTitle != null) {
			p.setPlotTitle(s.plotTitle);
		}
		if (s.xLabel != null) {
			p.setXLabel(s.xLabel);
		}
		if (s.yLabel != null) {
			p.setYLabel(s.yLabel);
		}

		p.setReverseXaxis(s.reverseXaxis);
		p.setReverseYaxis(s.reverseYaxis);

		// --- NEW: apply axis scales early so subsequent logic (include-zero, limits) is consistent ---
		PlotParameters.AxisScale xs = (s.xScale != null) ? s.xScale : PlotParameters.AxisScale.LINEAR;
		PlotParameters.AxisScale ys = (s.yScale != null) ? s.yScale : PlotParameters.AxisScale.LINEAR;
		p.setXScale(xs);
		p.setYScale(ys);

		p.includeXZero(s.includeXZero);
		p.includeYZero(s.includeYZero);

		if (s.xLimitsMethod != null) {
			p.setXLimitsMethod(s.xLimitsMethod);
		}
		if (s.yLimitsMethod != null) {
			p.setYLimitsMethod(s.yLimitsMethod);
		}

		if (s.manualXmin != null && s.manualXmax != null) {
			p.setXRange(s.manualXmin, s.manualXmax);
		}
		if (s.manualYmin != null && s.manualYmax != null) {
			p.setYRange(s.manualYmin, s.manualYmax);
		}

		p.setNumDecimalX(s.numDecimalX);
		p.setMinExponentX(s.minExponentX);
		p.setNumDecimalY(s.numDecimalY);
		p.setMinExponentY(s.minExponentY);

		p.setLegendDrawing(s.drawLegend);
		p.setLegendBorder(s.legendBorder);
		p.setLegendLineLength(s.legendLineLength);

		p.setExtraDrawing(s.drawExtra);
		p.setExtraBorder(s.extraBorder);
		p.setExtraStrings(s.extraStrings);

		// Heatmap knobs
		p.setLogZ(s.logZ);
		p.setShowEmptyBins(s.showEmptyBins);
		if (s.zColorMap != null) {
			try {
				p.setColorMap(ScientificColorMap.valueOf(s.zColorMap));
			} catch (Exception ignore) {
				// keep default
			}
		}
	}

	/**
	 * Apply style using reflection-based setters to avoid tight coupling to Styled's
	 * mutator API.
	 */
	private static void applyStyle(StyleSpec spec, Object styled) {
		if (spec == null || styled == null) {
			return;
		}

		Color fill = (spec.fillColorARGB == null) ? null : new Color(spec.fillColorARGB, true);
		Color border = (spec.borderColorARGB == null) ? null : new Color(spec.borderColorARGB, true);
		Color line = (spec.lineColorARGB == null) ? null : new Color(spec.lineColorARGB, true);

		tryInvoke(styled, "setSymbolType",
				new Class<?>[] { spec.symbolType != null ? spec.symbolType.getClass() : Object.class },
				spec.symbolType);
		tryInvoke(styled, "setLineStyle",
				new Class<?>[] { spec.lineStyle != null ? spec.lineStyle.getClass() : Object.class }, spec.lineStyle);

		tryInvoke(styled, "setFillColor", new Class<?>[] { Color.class }, fill);
		tryInvoke(styled, "setBorderColor", new Class<?>[] { Color.class }, border);
		tryInvoke(styled, "setLineColor", new Class<?>[] { Color.class }, line);

		if (spec.symbolSize != null) {
			tryInvoke(styled, "setSymbolSize", new Class<?>[] { int.class }, spec.symbolSize.intValue());
			tryInvoke(styled, "setSymbolSize", new Class<?>[] { Integer.class }, spec.symbolSize);
		}

		if (spec.lineWidth != null) {
			double lw = spec.lineWidth.doubleValue();
			tryInvoke(styled, "setLineWidth", new Class<?>[] { double.class }, lw);
			tryInvoke(styled, "setLineWidth", new Class<?>[] { float.class }, (float) lw);
			tryInvoke(styled, "setLineWidth", new Class<?>[] { Double.class }, spec.lineWidth);
		}
	}

	private static void tryInvoke(Object target, String methodName, Class<?>[] sig, Object arg) {
		try {
			var m = target.getClass().getMethod(methodName, sig);
			m.invoke(target, arg);
		} catch (Exception ignored) {
			// Intentionally ignored: style setter names vary across implementations.
		}
	}
}
