package edu.cnu.mdi.splot.example;

import java.awt.Color;

import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.PlotParameters;

@SuppressWarnings("serial")
public class StraightLine extends AExample {

	static double x[] = { 3, 2.5, 3.5, 4, 5 };
	static double y[] = { 238.0, 280.0, 310, 321.0, 420.0 };
	static double sig[] = { 14.7, 12.1, 13.1, 20.0, 8.0 };

	@Override
	protected PlotData createPlotData() throws PlotDataException {
		String[] curveNames = { "Linear Fit" };
		int[] fitOrders = { 1 }; // linear fit
		return new PlotData(PlotDataType.XYEXYE, curveNames, fitOrders);
	}

	@Override
	protected String getXAxisLabel() {
		return "<html>x data  X<SUB>M</SUB><SUP>2</SUP>";
	}

	@Override
	protected String getYAxisLabel() {
		return "<html>y data  Y<SUB>Q</SUB><SUP>2</SUP>";
	}

	@Override
	protected String getPlotTitle() {
		return "<html>Sample Plot X<SUP>2</SUP> vs. Q<SUP>2</SUP>";
	}

	@Override
	public void fillData() {
		PlotData plotData = canvas.getPlotData();
		Curve curve = (Curve) plotData.getCurve(0);
		for (int i = 0; i < x.length; i++) {
			curve.add(x[i], y[i], sig[i]);
		}
	}

	@Override
	public void setParameters() {
		PlotData plotData = canvas.getPlotData();

		//symbol fill color
		plotData.getCurve(0).getStyle().setFillColor(new Color(32, 32, 32, 64));

		//symbol border color
		Curve curve = (Curve) plotData.getCurve(0);
		curve.getStyle().setBorderColor(Color.darkGray);
		curve.setCurveMethod(CurveDrawingMethod.POLYNOMIAL);
		PlotParameters params = canvas.getParameters();
		params.setMinExponentY(6)
		.setNumDecimalY(2)
		.setMinExponentX(6)
		.setNumDecimalX(3);
	}

	//--------------------------------------------------------------
	public static void main(String arg[]) {

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				StraightLine example = new StraightLine();
				example.setVisible(true);
			}
		});

	}

}
