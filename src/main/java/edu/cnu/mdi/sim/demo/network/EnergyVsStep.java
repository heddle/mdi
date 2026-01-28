package edu.cnu.mdi.sim.demo.network;

import java.awt.Color;

import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotPanel;
import edu.cnu.mdi.splot.plot.PlotParameters;

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
	
	private final Curve springCurve;
	
	private final Curve repulsionCurve;
	
	private final Curve centerCurve;
	
	private final Curve totalCurve;
	
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
	
	public void setParameters() {
		PlotData plotData = getPlotCanvas().getPlotData();
		PlotParameters params = getPlotCanvas().getParameters();
		params.setXScale(PlotParameters.AxisScale.LOG10);

//		// symbol fill color
//		plotData.getCurve(0).getStyle().setFillColor(new Color(32, 32, 32, 64));
//
//		// symbol border color
//		plotData.getCurve(0).getStyle().setBorderColor(Color.darkGray);
//		plotData.getCurve(0).setCurveDrawingMethod(CurveDrawingMethod.GAUSSIAN);
//		PlotParameters params = canvas.getParameters();
//		params.setMinExponentY(6).setNumDecimalY(2);
//		String extra[] = { "Use the Edit Plot -> Curves..", "to change fit, colors, etc.",
//				"This box, like the Legend, is draggable." };
//		params.setExtraStrings(extra);
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
