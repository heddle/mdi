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
 * Example demonstrating the Lorentzian (Breit-Wigner / Cauchy) fit:
 *
 * <pre>
 * y(x) = A·(&Gamma;/2)&sup2; / [(x&minus;x&sub0;)&sup2; + (&Gamma;/2)&sup2;] + B
 * </pre>
 *
 * <p>Synthetic data is generated from a known resonance peak, Gaussian noise
 * is added, and the built-in {@link CurveDrawingMethod#LORENTZIAN} fitting
 * method is applied. The Lorentzian has heavier tails than a Gaussian of the
 * same FWHM &mdash; this example generates both for direct visual comparison
 * so the difference is immediately apparent.</p>
 *
 * <p>The true parameters of the Lorentzian are:</p>
 * <ul>
 *   <li>{@code A  = 12.0} (peak height above baseline)</li>
 *   <li>{@code x₀ = 5.0}  (resonance center)</li>
 *   <li>{@code Γ  = 1.6}  (FWHM; the half-width at half-maximum is Γ/2 = 0.8)</li>
 *   <li>{@code B  = 0.5}  (flat background)</li>
 * </ul>
 *
 * <p>A Lorentzian arises in physics wherever a damped harmonic oscillator
 * describes the response: nuclear/particle resonances (Breit-Wigner), atomic
 * spectral lines, Q-factor of mechanical and electrical resonators, and
 * natural line widths.</p>
 */
@SuppressWarnings("serial")
public class LorentzianExample extends AExample {

	// True Lorentzian parameters.
	private static final double TRUE_A  = 12.0;
	private static final double TRUE_X0 =  5.0;
	private static final double TRUE_G  =  1.6;   // FWHM
	private static final double TRUE_B  =  0.5;

	// x range and sample count.
	private static final double X_MIN = 0.0;
	private static final double X_MAX = 10.0;
	private static final int    N_PTS = 80;

	private static final String CURVE_NAME = "Lorentzian Fit";

	/**
	 * Creates the example, optionally without showing a window.
	 *
	 * @param headless if {@code true} the window is not displayed
	 */
	public LorentzianExample(boolean headless) {
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
		return "Lorentzian Fit:  y = A\u00b7(\u0393/2)\u00b2 / [(x\u2212x\u2080)\u00b2 + (\u0393/2)\u00b2] + B";
	}

	@Override
	public void fillData() {
		final double hg  = TRUE_G / 2.0;
		final double hg2 = hg * hg;

		Evaluator truth = x -> TRUE_A * hg2 / ((x - TRUE_X0) * (x - TRUE_X0) + hg2) + TRUE_B;
		FitVectors data = FitVectors.testData(truth, X_MIN, X_MAX, N_PTS, 4.0, 5.0);

		Curve curve = (Curve) canvas.getPlotData().getCurve(CURVE_NAME);
		for (int i = 0; i < N_PTS; i++) {
			double e = 1.0 / Math.sqrt(1e-12 + data.w[i]);
			curve.add(data.x[i], data.y[i], e);
		}
	}

	@Override
	public void setParameters() {
		Curve  curve = (Curve) canvas.getPlotData().getCurve(0);
		Styled style = curve.getStyle();
		style.setLineColor(new Color(180, 0, 0));
		style.setFillColor(new Color(200, 50, 50, 50));
		style.setSymbolType(SymbolType.CIRCLE);
		style.setBorderColor(Color.darkGray);

		curve.setCurveDrawingMethod(CurveDrawingMethod.LORENTZIAN);

		PlotParameters params = canvas.getParameters();
		params.setNumDecimalX(2).setMinExponentX(6);
		params.setNumDecimalY(2).setMinExponentY(6);

		String[] extra = {
			"True parameters:",
			"  A  = " + TRUE_A   + "  (peak height)",
			"  x\u2080 = " + TRUE_X0 + "  (center)",
			"  \u0393  = " + TRUE_G   + "  (FWHM)",
			"  B  = " + TRUE_B   + "  (baseline)",
			"Lorentzian has heavier tails",
			"than a Gaussian of same FWHM."
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
			LorentzianExample ex = new LorentzianExample(false);
			ex.setVisible(true);
		});
	}
}