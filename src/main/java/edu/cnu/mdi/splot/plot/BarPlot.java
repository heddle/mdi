package edu.cnu.mdi.splot.plot;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Objects;

import javax.swing.JFrame;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatIntelliJLaf;

import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.ui.fonts.Fonts;

public class BarPlot {


	/**
	 * Create a simple bar plot. Actually creates an XY plot to mimic a bar plot.
	 *
	 * @param title      the plot title
	 * @param values     the bar values
	 * @param categories the bar categories
	 * @param xLabel     the x axis label
	 * @param yLabel     the y axis label
	 * @return the plot data
	 * @throws PlotDataException if there is a problem creating the plot data
	 */
	public static PlotPanel createBarPlot(String title,
			double[] values,
			String[] categories,
			String xLabel,
			String yLabel) throws PlotDataException {

		Objects.requireNonNull(values, "Values array cannot be null");
		Objects.requireNonNull(categories, "Categories array cannot be null");
		if (values.length != categories.length) {
			throw new IllegalArgumentException("Values and categories arrays must have the same length");
		}
		if (values.length == 0) {
			throw new IllegalArgumentException("Values and categories arrays cannot be empty");
		}

		PlotData plotData = new PlotData(PlotDataType.XYXY, categories, null);
		PlotCanvas canvas = new PlotCanvas(plotData, title, xLabel, yLabel);
		PlotPanel plotPanel = new PlotPanel(canvas);

		List<ACurve> curves = plotData.getCurves();
		assignStyles(canvas, curves, values);
		forceBehavior(canvas, curves.size());
		return plotPanel;
	}

	// Assign styles to the curves and plot to mimic bars
	private static void assignStyles(PlotCanvas canvas, List<ACurve> curves, double[] values) {

		//store some client properties for later use
		canvas.putClientProperty("barplot.values", values.clone());
		canvas.putClientProperty("barplot.count", Integer.valueOf(values.length));

		ScientificColorMap palette = ScientificColorMap.VIRIDIS;


		double u = 1.0/(3*curves.size() + 1);
		for (int i = 0; i < curves.size(); i++) {
			ACurve acurve = curves.get(i);
			Curve curve = (Curve) acurve;
			double x = u * (3*i + 1);
			Color color = palette.colorAt(x);
			curve.getStyle().setLineColor(color.darker());
			curve.getStyle().setFillColor(color);
			curve.getStyle().setLineWidth(1f);
			curve.getStyle().setSymbolType(null); // no symbols
			curve.setCurveDrawingMethod(CurveDrawingMethod.CONNECT);

			curve.add(x, 0.0);
			curve.add(x, values[i]);
			curve.add(x+2*u, values[i]);
			curve.add(x+2*u, 0.0);
		}

	}

	/**
	 * Reset the plot lines to default for bar plots
	 * @param canvas
	 */
	public static void resetPlotLines(PlotCanvas canvas) {
		PlotParameters params = canvas.getParameters();
		params.clearPlotLines();
		Rectangle2D.Double world = canvas.getDataWorld();
		if (!canvas.isYLogActive()) {
			params.addPlotLine(new HorizontalLine(canvas, 0));

			// add horizontal grid lines
			double ymax = world.getMaxY();
			double ymin = Math.min(0, world.getMinY());
			double yRange = ymax - ymin;
			double yStep = 2 * Math.pow(10, Math.floor(Math.log10(yRange)));
			double yStart = Math.floor(ymin / yStep) * yStep;
			double y = yStart;
			while (y < ymax) {
				params.addPlotLine(new HorizontalLine(canvas, y));
				y += yStep;
			}
		}
		else {
		 //add horizontal grid lines at visible decades
			double ymax = world.getMaxY();
			double ymin = world.getMinY();
			double decadeStart = Math.pow(10, Math.ceil(Math.log10(ymin)));
			double decadeEnd = Math.pow(10, Math.floor(Math.log10(ymax)));
			for (double d = decadeStart; d <= decadeEnd; d *= 10)
				{
				params.addPlotLine(new HorizontalLine(canvas, d));
			}
		}
	}

	/**
	 * Rebuild the bar rectangles based on current values and log state.
	 *
	 * @param canvas the plot canvas
	 */
	public static void rebuildBars(PlotCanvas canvas) {
		    Object vv = canvas.getClientProperty("barplot.values");
	    if (!(vv instanceof double[])) {
	        return;
	    }
	    double[] values = (double[]) vv;

	    PlotData plotData = canvas.getPlotData();
	    if (plotData == null) {
	        return;
	    }
	    List<ACurve> curves = plotData.getCurves();
	    if (curves == null || curves.isEmpty()) {
	        return;
	    }

	    PlotParameters params = canvas.getParameters();

	    // use the *active* log state (PlotCanvas disables log if range not strictly positive)
	    boolean logY = canvas.isYLogActive();

	    // baseline rules
	    final double baseline = logY ? computeLogBaseline(values) : 0.0;

	    // keep includeYZero consistent with what the user sees
	    params.includeYZero(!logY);

	    // recompute layout exactly as assignStyles did
	    double u = 1.0 / (3 * curves.size() + 1);

	    for (int i = 0; i < curves.size() && i < values.length; i++) {
	        ACurve ac = curves.get(i);
	        if (!(ac instanceof Curve)) {
	            continue;
	        }
	        Curve c = (Curve) ac;

	        // clear the old rectangle geometry
	        c.clearData();

	        double v = values[i];

	        // On log axis we cannot draw non-positive values
	        if (logY && v <= 0.0) {
	            continue;
	        }

	        double x0 = u * (3 * i + 1);
	        double x1 = x0 + 2 * u;

	        double y0 = baseline;
	        double y1 = v;

	        // for linear, allow negatives (your existing behavior)
	        // for log, y0 and y1 are positive by construction

	        c.add(x0, y0);
	        c.add(x0, y1);
	        c.add(x1, y1);
	        c.add(x1, y0);
	    }
	    resetPlotLines(canvas);
	}

	private static double computeLogBaseline(double[] values) {
	    double minPos = Double.POSITIVE_INFINITY;
	    for (double v : values) {
	        if (v > 0.0 && v < minPos) {
	            minPos = v;
	        }
	    }
	    if (!Double.isFinite(minPos)) {
	        return 1.0; // fallback
	    }
	    // one decade below smallest positive bar top
	    double decade = Math.floor(Math.log10(minPos));
	    return Math.pow(10.0, decade - 1.0);
	}


	// Force behavior to mimic a bar plot
	private static void forceBehavior(PlotCanvas canvas, int numCurves) {

		PlotParameters params = canvas.getParameters();
		params.addRenderHint(RenderHint.BARPLOT);
		params.setXScale(PlotParameters.AxisScale.LINEAR);
		params.includeXZero(true);
		params.setLogZ(false);
		params.addPlotLine(new HorizontalLine(canvas, 0));
		params.setLegendDrawing(false);

		PlotTicks ticks = canvas.getPlotTicks();
		ticks.setNumMajorTickY(4);
		ticks.setNumMinorTickY(0);
		ticks.setNumMajorTickX(numCurves);
		ticks.setNumMinorTickX(0);

		resetPlotLines(canvas);

	}


	/**
	 * A demo bar plot
	 *
	 * @return the plot panel
	 * @throws PlotDataException if there is a problem creating the plot data
	 */
	public static PlotPanel demoBarPlot() throws PlotDataException {
		String title = "Sample Bar Plot";
		double[] values = { 1.0, 2.5, 3.0, 4.5, -2.0, 0.4, 2.6};
		String[] categories = { "Category A", "Category B",
				"Category C", "Category D", "Category E",
				"Category F", "Category G"};
		String xLabel = "Categories";
		String yLabel = "Values";

		return createBarPlot(title, values, categories, xLabel, yLabel);
	}


}
