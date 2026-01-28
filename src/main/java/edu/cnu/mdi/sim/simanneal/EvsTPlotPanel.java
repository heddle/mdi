package edu.cnu.mdi.sim.simanneal;

import java.awt.Color;

import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.AReadyPlotPanel;
import edu.cnu.mdi.splot.plot.PlotChangeType;
import edu.cnu.mdi.splot.plot.PlotParameters;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.ui.fonts.Fonts;

@SuppressWarnings("serial")
public class EvsTPlotPanel extends AReadyPlotPanel {

	// plot title and labels
	private String title;
	private String xLabel;
	private String yLabel;

	// curve names
	private static final String ACCEPTED_CURVE = "Accepted";
	private static final String BEST_CURVE = "Best";

	// curves
	private volatile Curve accepted;
	private volatile Curve best;

	//simple throttle for accepted points
	private boolean throttleAccepted;

	//accepted count if choose to throttle accepted plot
	private long acceptedCount = 0;

	// stride for accepted points, e.g., plot every 10th accepted point
	private int acceptedStride = 1;

	/**
	 * Constructor. Uses default throttling of accepted points (true).
	 * @param title the plot title
	 * @param xLabel the x axis label
	 * @param yLabel the y axis label
	 */
	public EvsTPlotPanel(String title, String xLabel, String yLabel) {
		this(title, xLabel, yLabel, true);
	}

	/**
	 * Constructor
	 * @param title the plot title
	 * @param xLabel the x axis label
	 * @param yLabel the y axis label
	 * @param throttleAccepted true to throttle accepted points, false to plot all accepted points
	 */
	public EvsTPlotPanel(String title, String xLabel, String yLabel, boolean throttleAccepted) {
		super(true);
		this.title = title;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		this.throttleAccepted = throttleAccepted;
		dataSetup();
	}


	/**
	 * Set whether to throttle accepted points
	 * @param throttle true to throttle, false to plot all accepted points
	 */
	public void setThrottleAccepted(boolean throttle) {
		this.throttleAccepted = throttle;
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
			return title;
	}

	@Override
	public void clearData() {
		for (ACurve curve : canvas.getPlotData().getCurves()) {
			((Curve)curve).clearData();
		}
		acceptedCount = 0;
		canvas.repaint();
	}

	public void addAccepted(double x, double y) {
		if (accepted != null) {
			acceptedCount++;

			if (acceptedCount > 25000 && acceptedStride < 20) {
				acceptedStride = 20;
			} else if (acceptedCount > 12000 && acceptedStride < 10) {
				acceptedStride = 10;
			} else if (acceptedCount > 3000 && acceptedStride < 5) {
				acceptedStride = 5;
			}
			if (throttleAccepted && (acceptedCount % acceptedStride != 0)) {
				return;
			}
			accepted.add(x, y);
			canvas.repaint();
		}
	}

	public void addBest(double x, double y) {
		if (best != null) {
			best.add(x, y);
			canvas.repaint();
		}
	}

	@Override
	public void setParameters() {
		Color acceptedColor = new Color(128, 128, 128, 10);
		Color bestColor = X11Colors.getX11Color("red", 128);

		PlotData plotData = canvas.getPlotData();

		accepted = (Curve) plotData.getCurve(ACCEPTED_CURVE);
		best = (Curve) plotData.getCurve(BEST_CURVE);


		best.setCurveDrawingMethod(CurveDrawingMethod.CONNECT);
		best.getStyle().setSymbolType(SymbolType.SQUARE);
		best.getStyle().setSymbolSize(4);
		best.getStyle().setFillColor(bestColor);
		best.getStyle().setLineColor(Color.black);
		best.getStyle().setBorderColor(X11Colors.getX11Color("dark red"));

		accepted.setCurveDrawingMethod(CurveDrawingMethod.NONE);
		accepted.getStyle().setSymbolType(SymbolType.CIRCLE);
		accepted.getStyle().setSymbolSize(2);
		accepted.getStyle().setFillColor(acceptedColor);
		accepted.getStyle().setBorderColor(null);


		PlotParameters params = canvas.getParameters();
		params.includeXZero(true);
		params.includeYZero(true);
		params.setXScale(PlotParameters.AxisScale.LOG10);
		params.setTitleFont(Fonts.plainFontDelta(1));
		params.setLegendDrawing(false);
	}

}
