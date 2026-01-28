package edu.cnu.mdi.sim.demo.network;

import java.awt.Color;

import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.HorizontalLine;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotPanel;
import edu.cnu.mdi.splot.plot.PlotParameters;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.ui.fonts.Fonts;

public class MinPairwiseSeparation extends PlotPanel {

	private static final String TITLE = "Min Pairwise Separation vs. Step";
	private static final String XLABEL = "Simulation Step";
	private static final String YLABEL = "Separation";
	
	private static final String SEPARATION_CURVE = "Separation";

	private static final String[] CURVE_NAMES = { SEPARATION_CURVE };
	
	// the curves
	private final Curve separationCurve;
	
	public MinPairwiseSeparation() {
		super(createCanvas());
		
		PlotData plotData = getPlotCanvas().getPlotData();
		this.separationCurve = (Curve) plotData.getCurve(SEPARATION_CURVE);
		
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
		params.addPlotLine(new HorizontalLine(getPlotCanvas(), 1));
		
		setCurveStyle(separationCurve, SymbolType.CIRCLE, 
				X11Colors.getX11Color("Cadet Blue"), Color.blue);

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
		separationCurve.add(d.step, d.minPairwiseSeparation);
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
