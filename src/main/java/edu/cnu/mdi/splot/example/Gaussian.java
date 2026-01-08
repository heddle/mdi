package edu.cnu.mdi.splot.example;

import java.awt.Color;

import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.fit.Evaluator;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.FitVectors;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.PlotParameters;

@SuppressWarnings("serial")
public class Gaussian extends AExample {

	private static final String curveName = "Gaussian Fit";

	@Override
	protected PlotData createPlotData() throws PlotDataException {
		String[] curveNames = {curveName };
		return new PlotData(PlotDataType.XYEXYE, curveNames, null);
	}

	@Override
	protected String getXAxisLabel() {
		return "x";
	}

	@Override
	protected String getYAxisLabel() {
		return "y";
	}

	@Override
	protected String getPlotTitle() {
		return "Sample Gaussian Fit";
	}

	@Override
	public void fillData() {

		final double mu = 1.2;
 		final double sigma = 0.3;
 		final double A = 2.0;
 		final double B = 0.5;
 		int n = 50;


 		Evaluator eval = (double x) -> {
 			double z = (x - mu) / sigma;
 			return A * Math.exp(-0.5 * z * z) + B;
 		};

 		FitVectors testData = FitVectors.testData(eval, -1.0, 3.0, n, 4.0, 40.0);

		for (int i = 0; i < n; i++) {
			double x = testData.x[i];
			double y = testData.y[i];
			double w = testData.w[i];

			//convert weight to error
	    	double e = 1.0 / Math.sqrt(1.0e-12 + w);

	    	Curve curve = (Curve) canvas.getPlotData().getCurve(curveName);

	    	//since we are on the EDT thread direct add is safe
			curve.add(x, y, e);

		}

	}

	@Override
	public void setParameters() {
		PlotData plotData = canvas.getPlotData();

		//symbol fill color
		plotData.getCurve(0).getStyle().setFillColor(new Color(32, 32, 32, 64));

		//symbol border color
		plotData.getCurve(0).getStyle().setBorderColor(Color.darkGray);
		plotData.getCurve(0).setCurveMethod(CurveDrawingMethod.GAUSSIAN);
		PlotParameters params = canvas.getParameters();
		params.setMinExponentY(6).setNumDecimalY(2);
	}

	// Since we are using Swing, we need to start things off on the
	// event dispatch thread (EDT).
	public static void main(String arg[]) {

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Gaussian example = new Gaussian();
				example.setVisible(true);
			}
		});

	}

}
