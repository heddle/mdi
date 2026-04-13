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
 * Example demonstrating the power-law fit ({@code y(x) = A·x^n + C}) on
 * log-log axes.
 *
 * <p>Synthetic data is generated from a known power law, log-normal
 * multiplicative scatter is added, and the built-in
 * {@link CurveDrawingMethod#POWER_LAW} fitting method is applied. Displaying
 * on log-log axes makes the power law appear as a straight line, while the
 * nonlinear fitter works in linear space and handles the constant offset
 * correctly.</p>
 *
 * <p>The true parameters used are:</p>
 * <ul>
 *   <li>{@code A = 2.0}  (amplitude)</li>
 *   <li>{@code n = 1.7}  (exponent; the slope on a log-log plot)</li>
 *   <li>{@code C = 0.0}  (baseline offset — zero keeps the log-log line
 *       perfectly straight, which makes the fit easy to judge visually)</li>
 * </ul>
 *
 * <p>Power laws arise in scaling relationships across science and engineering:
 * stellar luminosity vs mass, turbulence energy spectra, fractal dimensions,
 * and Kepler's third law, among others.</p>
 */
@SuppressWarnings("serial")
public class PowerLawExample extends AExample {

	// True model parameters.
	private static final double TRUE_A = 2.0;
	private static final double TRUE_N = 1.7;
	private static final double TRUE_C = 0.0;

	// x range: log-spaced from 0.1 to 100.
	private static final double LOG_X_MIN  = -1.0;   // 10^-1 = 0.1
	private static final double LOG_X_MAX  =  2.0;   // 10^2  = 100
	private static final int    N_PTS      = 40;

	// Scatter and error parameters.
	private static final double LOG_JITTER_SIGMA = 0.06;  // ~15% multiplicative scatter
	private static final double REL_ERROR        = 0.10;  // 10% relative error bars

	private static final String CURVE_NAME = "Power Law Fit";

	/**
	 * Creates the example, optionally without showing a window.
	 *
	 * @param headless if {@code true} the window is not displayed
	 */
	public PowerLawExample(boolean headless) {
		super(headless);
	}

	// -----------------------------------------------------------------------
	// AExample contract
	// -----------------------------------------------------------------------

	@Override
	protected PlotData createPlotData() throws PlotDataException {
		return new PlotData(PlotDataType.XYEXYE, new String[] { CURVE_NAME }, null);
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
		return "<html>Power Law Fit:  y = A\u00b7x<sup>n</sup> + C";
	}

	@Override
	public void fillData() {
		Curve  curve = (Curve) canvas.getPlotData().getCurve(CURVE_NAME);
		Random rand  = new Random(20260201L);

		for (int i = 0; i < N_PTS; i++) {
			// Log-spaced x values.
			double t    = (double) i / (N_PTS - 1);
			double logx = LOG_X_MIN + t * (LOG_X_MAX - LOG_X_MIN);
			logx += 0.015 * rand.nextGaussian();   // tiny x jitter in log space
			double x = Math.pow(10.0, logx);

			// True y then multiplicative scatter.
			double yTrue = TRUE_A * Math.pow(x, TRUE_N) + TRUE_C;
			double y     = yTrue * Math.pow(10.0, LOG_JITTER_SIGMA * rand.nextGaussian());

			// Error bar proportional to the scattered value.
			double sig = Math.max(1e-30, REL_ERROR * Math.abs(y));

			curve.add(x, y, sig);
		}
	}

	@Override
	public void setParameters() {
		Curve curve = (Curve) canvas.getPlotData().getCurve(0);

		curve.getStyle().setLineColor(new Color(0, 150, 80));
		curve.getStyle().setFillColor(new Color(0, 150, 80, 50));
		curve.getStyle().setBorderColor(new Color(0, 100, 50));
		curve.setCurveDrawingMethod(CurveDrawingMethod.POWER_LAW);

		PlotParameters params = canvas.getParameters();

		// Log-log axes: the power law plots as a straight line here.
		params.setXScale(PlotParameters.AxisScale.LOG10);
		params.setYScale(PlotParameters.AxisScale.LOG10);
		params.includeXZero(false);
		params.includeYZero(false);
		params.setMinExponentX(6).setNumDecimalX(2);
		params.setMinExponentY(6).setNumDecimalY(2);

		String[] extra = {
			"True parameters:",
			"  A = " + TRUE_A,
			"  n = " + TRUE_N,
			"  C = " + TRUE_C,
			"On log-log axes, y = Ax\u207f",
			"plots as a straight line."
		};
		params.setExtraStrings(extra);

		canvas.setWorldSystem();
	}

	// -----------------------------------------------------------------------
	// Entry point
	// -----------------------------------------------------------------------

	/**
	 * Launches the example as a standalone application.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(() -> {
			PowerLawExample ex = new PowerLawExample(false);
			ex.setVisible(true);
		});
	}
}