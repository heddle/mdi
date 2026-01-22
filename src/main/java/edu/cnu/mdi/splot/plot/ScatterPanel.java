package edu.cnu.mdi.splot.plot;

import java.awt.Color;

import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;

public class ScatterPanel extends AReadyPlotPanel {
	
	private String title;
	private String xLabel;
	private String yLabel;
	
	private static final String ACCEPTED_CURVE = "Accepted";
	private static final String BEST_CURVE = "Best";
	
	private volatile Curve accepted;
	private volatile Curve best;
	
	public ScatterPanel(String title, String xLabel, String yLabel) {
		super();
		this.title = title;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		dataSetup();
	}

	@Override
	public void plotChanged(PlotChangeType event) {
	}

	@Override
	protected PlotData createPlotData() throws PlotDataException {
		String[] curveNames = { ACCEPTED_CURVE, BEST_CURVE };
		return new PlotData(PlotDataType.XYXY, curveNames, null);
	}

	@Override
	protected String getXAxisLabel() {
		return xLabel;
	}

	@Override
	protected String getYAxisLabel() {
			return yLabel;
	}

	@Override
	protected String getPlotTitle() {
		// TODO Auto-generated method stub
		return title;
	}

	@Override
	public void fillData() {
		// no-op
	}
	
	public void clearData() {
		for (ACurve curve : canvas.getPlotData().getCurves()) {
			((Curve)curve).clearData();
		}
		canvas.repaint();
	}
	
	public void addAccepted(double x, double y) {
		if (accepted != null) {
			accepted.add(x, y);
		}
	}
	
	public void addBest(double x, double y) {
		if (best != null) {
			best.add(x, y);
		}
	}

	@Override
	public void setParameters() {
		Color acceptedColor = new Color(128, 128, 128, 10);
		Color bestColor = Color.red;

		PlotData plotData = canvas.getPlotData();
		
		accepted = (Curve) plotData.getCurve(ACCEPTED_CURVE);
		best = (Curve) plotData.getCurve(BEST_CURVE);
		

		best.setCurveMethod(CurveDrawingMethod.CONNECT);
		best.getStyle().setSymbolType(SymbolType.SQUARE);
		best.getStyle().setSymbolSize(6);
		best.getStyle().setFillColor(bestColor);
		best.getStyle().setLineColor(Color.black);
		
		accepted.setCurveMethod(CurveDrawingMethod.NONE);
		accepted.getStyle().setSymbolType(SymbolType.CIRCLE);
		accepted.getStyle().setSymbolSize(2);
		accepted.getStyle().setFillColor(acceptedColor);
		accepted.getStyle().setLineColor(null);


		PlotParameters params = canvas.getParameters();
		params.mustIncludeXZero(true);
		params.mustIncludeYZero(true);
		params.setLegendDrawing(false);
	}

}
