package edu.cnu.mdi.splot.example;

import java.awt.Color;

import edu.cnu.mdi.graphics.style.Styled;
import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.fit.Evaluator;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.FitVectors;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.PlotParameters;

@SuppressWarnings("serial")
public class ThreeGaussians extends AExample {

	private static final String curveName = "3 Gaussian Fit";

	public ThreeGaussians(boolean headless) {
		super(headless);
	}

	@Override
	protected PlotData createPlotData() throws PlotDataException {
		String[] curveNames = { curveName };
		int[] fitOrders = { 3 }; // fit to 3 gaussians
		return new PlotData(PlotDataType.XYEXYE, curveNames, fitOrders);
	}

	@Override
	protected String getXAxisLabel() {
		return "<html>x <b>data</b>";
	}

	@Override
	protected String getYAxisLabel() {
		return "<html>y <b>data</b>";
	}

	@Override
	protected String getPlotTitle() {
		return "<html>Fit to Three Gaussians";
	}

	@Override
	public void fillData() {
		final double[] mu = { 1.2, 3.3, 5.3 };
		final double[] sigma = { 0.5, 0.5, 0.4 };
		final double[] A = { 2.0, 1.5, 1.1 };
		final double B = 0.5;
		int n = 100;
		int numGauss = A.length;

		Evaluator eval = (double x) -> {
			double sum = 0;
			for (int k = 0; k < numGauss; k++) {
				double dx = x - mu[k];
				double z = dx / sigma[k];
				sum += A[k] * Math.exp(-0.5 * z * z);
			}
			sum += B;
			return sum;
		};

		FitVectors testData = FitVectors.testData(eval, -1.0, 7.0, n, 6.0, 7.0);
		for (int i = 0; i < n; i++) {
			double x = testData.x[i];
			double y = testData.y[i];
			double w = testData.w[i];

			// convert weight to error
			double e = 1.0 / Math.sqrt(1.0e-12 + w);

			Curve curve = (Curve) canvas.getPlotData().getCurve(curveName);

			// since we are on the EDT thread direct add is safe
			curve.add(x, y, e);

		}

	}

	@Override
	public void setParameters() {
		PlotData plotData = canvas.getPlotData();

		Curve curve = (Curve) plotData.getCurve(curveName);
		Styled style = curve.getStyle();
		// symbol fill color
		style.setFillColor(new Color(32, 32, 32, 64));
		style.setSymbolType(SymbolType.CIRCLE);
		style.setBorderColor(Color.darkGray);
		style.setLineColor(Color.black);
		
		curve.setCurveDrawingMethod(CurveDrawingMethod.GAUSSIANS);
		PlotParameters params = canvas.getParameters();
		params.setMinExponentY(6).setNumDecimalY(2);
		params.includeXZero(true);
		params.includeYZero(true);

		String extra[] = { "This is an extra string", "This is a longer extra string",
				"This is an even longer extra string", "This box, like the Legend, is draggable." };
		params.setExtraStrings(extra);

	}

	public static void main(String arg[]) {

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ThreeGaussians example = new ThreeGaussians(false);
				example.setVisible(true);
			}
		});

	}

}
