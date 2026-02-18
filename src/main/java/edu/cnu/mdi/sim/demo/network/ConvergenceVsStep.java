package edu.cnu.mdi.sim.demo.network;

import java.awt.Color;

import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotPanel;
import edu.cnu.mdi.splot.plot.PlotParameters;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.ui.fonts.Fonts;

@SuppressWarnings("serial")
public class ConvergenceVsStep extends PlotPanel {
	private static final String TITLE = "Convergence Metrics vs. Step";
	private static final String XLABEL = "Simulation Step";
	private static final String YLABEL = "Metrics";

	private static final String SPEED_CURVE = "Average Speed";
	private static final String FRMS_CURVE = "Force RMS";
	private static final String RATIO_CURVE = "Force RMS / Avg Speed (diagnostic)";

	private static final String[] CURVE_NAMES = { SPEED_CURVE, FRMS_CURVE, RATIO_CURVE };

	// the curves
	private final Curve speedCurve;
	private final Curve frmsCurve;
	private final Curve ratioCurve;

	/**
	 * Create the Convergence metrics vs Step plot panel.
	 */
	public ConvergenceVsStep() {
		super(createCanvas());

		PlotData plotData = getPlotCanvas().getPlotData();
		this.speedCurve = (Curve) plotData.getCurve(SPEED_CURVE);
		this.frmsCurve = (Curve) plotData.getCurve(FRMS_CURVE);
		this.ratioCurve = (Curve) plotData.getCurve(RATIO_CURVE);

		//set the plot parameters for plot and curves
		setParameters();

	}

	// set plot parameters
	private void setParameters() {
		PlotParameters params = getPlotCanvas().getParameters();

		// x axis is logarithmic scale
		params.setXScale(PlotParameters.AxisScale.LOG10);
		params.setYScale(PlotParameters.AxisScale.LOG10);
		params.setNumDecimalX(0);
		params.setTitleFont(Fonts.plainFontDelta(2));

		setCurveStyle(speedCurve, SymbolType.CIRCLE, null, Color.black);
		setCurveStyle(frmsCurve, SymbolType.SQUARE, X11Colors.getX11Color("Light Coral"), Color.red);
		setCurveStyle(ratioCurve, SymbolType.DIAMOND, X11Colors.getX11Color("Medium Sea Green", 128), Color.green);
	}

	//helper method to set curve style
	private void setCurveStyle(Curve curve, SymbolType stype, Color symbolColor, Color symbolBorder) {
		IStyled style = curve.getStyle();
		style.setSymbolType(stype);
		style.setFillColor(symbolColor);
		style.setBorderColor(symbolBorder);
	}

	/**
	 * Update the plot with new diagnostic data.
	 *
	 * @param d diagnostic data (from the simulation)
	 */
	protected void updatePlot(NetworkDeclutterSimulation.Diagnostics d) {
		speedCurve.add(d.step, d.avgSpeed);
		frmsCurve.add(d.step, d.Frms);
		ratioCurve.add(d.step, d.Frms / (1.0e-12 + d.avgSpeed));
	}

	// create the plot canvas
	private static PlotCanvas createCanvas() {

		PlotData plotData;
		try {
			plotData = new PlotData(PlotDataType.XYEXYE, CURVE_NAMES, null);
			return new PlotCanvas(plotData, TITLE, XLABEL, YLABEL);
		} catch (PlotDataException e) {
			e.printStackTrace();
		}
		return null;
	}


}
