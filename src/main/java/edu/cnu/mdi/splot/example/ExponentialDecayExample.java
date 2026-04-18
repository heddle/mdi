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

/**
 * Example demonstrating the exponential decay fit
 * ({@code y(x) = A·exp(-x/τ) + C}).
 *
 * <p>Synthetic data is generated from a known decay, Gaussian noise is added,
 * and the built-in {@link CurveDrawingMethod#EXPONENTIAL_DECAY} fitting method
 * is applied. The fit overlay is drawn automatically by the canvas.</p>
 *
 * <p>The true parameters used are:</p>
 * <ul>
 *   <li>{@code A = 8.0}  (initial amplitude)</li>
 *   <li>{@code τ = 2.5}  (decay constant; the signal falls to 1/e ≈ 37% at
 *       x = τ)</li>
 *   <li>{@code C = 1.0}  (asymptotic baseline)</li>
 * </ul>
 *
 * <p>This shape arises in radioactive decay, RC circuit discharge,
 * fluorescence lifetime measurements, and many other physical systems.</p>
 */
@SuppressWarnings("serial")
public class ExponentialDecayExample extends AExample {

	// True model parameters used to generate the synthetic data.
	private static final double TRUE_A   = 8.0;
	private static final double TRUE_TAU = 2.5;
	private static final double TRUE_C   = 1.0;

	// x range for the demo.
	private static final double X_MIN  = 0.0;
	private static final double X_MAX  = 10.0;
	private static final int    N_PTS  = 60;

	private static final String CURVE_NAME = "Exponential Decay";

	/**
	 * Creates the example, optionally without showing a window.
	 *
	 * @param headless if {@code true} the window is not displayed (useful for
	 *                 embedding in {@link SplotDemoView})
	 */
	public ExponentialDecayExample(boolean headless) {
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
		return "x";
	}

	@Override
	protected String getYAxisLabel() {
		return "y";
	}

	@Override
	protected String getPlotTitle() {
		return "<html>Exponential Decay Fit:  y = A·e<SUP>-x/τ</SUP> + C";
	}

	@Override
	public void fillData() {
		Evaluator truth = x -> TRUE_A * Math.exp(-x / TRUE_TAU) + TRUE_C;
		FitVectors data = FitVectors.testData(truth, X_MIN, X_MAX, N_PTS, 5.0, 6.0);

		Curve curve = (Curve) canvas.getPlotData().getCurve(CURVE_NAME);
		for (int i = 0; i < N_PTS; i++) {
			double e = 1.0 / Math.sqrt(1e-12 + data.w[i]);
			curve.add(data.x[i], data.y[i], e);
		}
	}

	@Override
	public void setParameters() {
		Curve curve = (Curve) canvas.getPlotData().getCurve(0);
		Styled style = curve.getStyle();
		style.setLineColor(Color.blue);
		style.setSymbolType(SymbolType.CIRCLE);
		style.setFillColor(new Color(0, 120, 200, 60));

		curve.setCurveDrawingMethod(CurveDrawingMethod.EXPONENTIAL_DECAY);

		PlotParameters params = canvas.getParameters();
		params.setNumDecimalY(2).setMinExponentY(6);
		params.setNumDecimalX(2).setMinExponentX(6);

		String[] extra = {
			"True parameters:",
			"  A = " + TRUE_A,
			"  \u03c4 = " + TRUE_TAU,
			"  C = " + TRUE_C,
			"Fit result shown on legend."
		};
		params.setExtraStrings(extra);
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
			ExponentialDecayExample ex = new ExponentialDecayExample(false);
			ex.setVisible(true);
		});
	}
}