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
public class EnergyVsStep extends PlotPanel {

	private static final String TITLE = "Potential (pseudo)Energy vs. Step";
	private static final String XLABEL = "Simulation Step";
	private static final String YLABEL = "pseudoEnergy";

	private static final String SPRING_CURVE = "Spring";
	private static final String REPULSION_CURVE = "Repulsion";
	private static final String CENTER_CURVE = "Center";
	private static final String TOTAL_CURVE = "Total";

	private static final String[] CURVE_NAMES = {
			SPRING_CURVE,
			REPULSION_CURVE,
			CENTER_CURVE,
			TOTAL_CURVE
	};

	// the curves
	private final Curve springCurve;
	private final Curve repulsionCurve;
	private final Curve centerCurve;
	private final Curve totalCurve;

	/**
	 * Create the Energy vs Step plot panel.
	 */
	public EnergyVsStep() {
		super(createCanvas());

		PlotData plotData = getPlotCanvas().getPlotData();
		this.springCurve = (Curve) plotData.getCurve(SPRING_CURVE);
		this.repulsionCurve = (Curve) plotData.getCurve(REPULSION_CURVE);
		this.centerCurve = (Curve) plotData.getCurve(CENTER_CURVE);
		this.totalCurve = (Curve) plotData.getCurve(TOTAL_CURVE);

		//set the plot parameters for plot and curves
		setParameters();
	}

	// set plot parameters
	private void setParameters() {
		PlotParameters params = getPlotCanvas().getParameters();

		//x axis is logarithmic scale
		params.setXScale(PlotParameters.AxisScale.LOG10);
		params.setNumDecimalX(0);
		params.setTitleFont(Fonts.plainFontDelta(2));

		setCurveStyle(springCurve, SymbolType.CIRCLE,
				X11Colors.getX11Color("Cadet Blue"), Color.blue);

		setCurveStyle(repulsionCurve, SymbolType.CIRCLE,
				X11Colors.getX11Color("Light Coral"), Color.red);

		setCurveStyle(centerCurve, SymbolType.CIRCLE,
				X11Colors.getX11Color("Dark Goldenrod"), Color.black);

		setCurveStyle(totalCurve, SymbolType.SQUARE,
				X11Colors.getX11Color("Dark Olive Green"), Color.green.darker());
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
		springCurve.add(d.step, d.Uspring);
		repulsionCurve.add(d.step, d.Urepulsion);
		centerCurve.add(d.step, d.Ucenter);
		totalCurve.add(d.step, d.total());
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
