package edu.cnu.mdi.splot.example;

import java.awt.Color;
import java.util.Random;

import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.PlotParameters;

/**
 * Example power-law data with log-log axes:
 * y = A x^3 with multiplicative jitter and y-proportional errors.
 */
@SuppressWarnings("serial")
public class CubicLogLog extends AExample {

	public CubicLogLog(boolean headless) {
		super(headless);
	}

	@Override
	protected PlotData createPlotData() throws PlotDataException {
		String[] curveNames = { "Cubic fit (order 3)" };
		int[] fitOrders = { 3 };
		return new PlotData(PlotDataType.XYEXYE, curveNames, fitOrders);
	}

	@Override
	protected String getXAxisLabel() {
		return "<html>x (log<sub>10</sub> scale)";
	}

	@Override
	protected String getYAxisLabel() {
		return "<html>y (log<sub>10</sub> scale)";
	}

	@Override
	protected String getPlotTitle() {
		return "<html>Log-Log Example: y = A x<sup>3</sup> with jitter and errors";
	}

	@Override
	public void fillData() {
		PlotData plotData = canvas.getPlotData();
		Curve curve = (Curve) plotData.getCurve(0);

		// Deterministic randomness for repeatable plots
		Random rand = new Random(20260125L);

		final int n = 30;

		// Choose x values roughly log-spaced between 10^-1 and 10^1 (0.1 to 10)
		double logMin = -1.0;
		double logMax =  1.0;

		// Model
		final double A = 0.8;

		// Multiplicative jitter strength (in log10 units)
		// e.g. 0.08 means ~20% typical multiplicative scatter (10^0.08 ≈ 1.20)
		final double logJitterSigma = 0.08;

		// Relative measurement uncertainty (used as the plotted error bar)
		final double relError = 0.12;

		for (int i = 0; i < n; i++) {

			// nominal log-spaced x
			double t = (double) i / (n - 1);
			double logx = logMin + t * (logMax - logMin);

			// small random jitter in log space (keeps x positive)
			logx += 0.02 * rand.nextGaussian();
			double x = Math.pow(10.0, logx);

			// true y
			double yTrue = A * x * x * x;

			// multiplicative jitter in y (log-normal-ish)
			double y = yTrue * Math.pow(10.0, logJitterSigma * rand.nextGaussian());

			// error bar proportional to y (always positive)
			double sig = relError * y;

			curve.add(x, y, sig);
		}
	}

	@Override
	public void setParameters() {
		PlotData plotData = canvas.getPlotData();
		Curve curve = (Curve) plotData.getCurve(0);

		// Style
		curve.getStyle().setFillColor(new Color(32, 32, 32, 64));
		curve.getStyle().setBorderColor(Color.darkGray);
		curve.setCurveDrawingMethod(CurveDrawingMethod.POLYNOMIAL);

		PlotParameters params = canvas.getParameters();

		// Log-log axes
		params.setXScale(PlotParameters.AxisScale.LOG10);
		params.setYScale(PlotParameters.AxisScale.LOG10);

		// Tick label formatting: let your log ticks show cleanly
		// (These still matter for non-decade labels and any fallback formatting.)
		params.setMinExponentX(6).setNumDecimalX(3);
		params.setMinExponentY(6).setNumDecimalY(3);

		// For log axes, include-zero is meaningless; keep it off
		params.includeXZero(false);
		params.includeYZero(false);

		// Recompute world system now that we changed scale
		canvas.setWorldSystem();
	}

	// --------------------------------------------------------------
	public static void main(String arg[]) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				CubicLogLog example = new CubicLogLog(false);
				example.setVisible(true);
			}
		});
	}
}
