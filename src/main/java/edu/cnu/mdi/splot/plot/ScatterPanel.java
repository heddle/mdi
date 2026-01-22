package edu.cnu.mdi.splot.plot;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicInteger;

import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;

public class ScatterPanel extends AReadyPlotPanel {
	
	private String title;
	private String xLabel;
	private String yLabel;
	
	private volatile Curve curve;
	
	public ScatterPanel(String title, String xLabel, String yLabel) {
		super();
		this.title = title;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		dataSetup();
	}

	@Override
	public void plotChanged(PlotChangeType event) {
		// TODO Auto-generated method stub

	}

	@Override
	protected PlotData createPlotData() throws PlotDataException {
		String[] curveNames = { "Data" };
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
		if (curve != null) {
			curve.clearData();
		}
		canvas.repaint();
	}
	
	public void add(double x, double y) {
		if (curve != null) {
			curve.add(x, y);
		}
	}

	@Override
	public void setParameters() {
		Color fillColor = Color.red;

		PlotData plotData = canvas.getPlotData();
		final Curve dc = (Curve) plotData.getFirstCurve();
		curve = dc;

		dc.setCurveMethod(CurveDrawingMethod.NONE);
		dc.getStyle().setSymbolType(SymbolType.CIRCLE);
		dc.getStyle().setSymbolSize(2);
		dc.getStyle().setFillColor(fillColor);
		dc.getStyle().setLineColor(null);

		PlotParameters params = canvas.getParameters();
		params.mustIncludeXZero(true);
		params.mustIncludeYZero(true);
		params.setLegendDrawing(false);
	}

}
