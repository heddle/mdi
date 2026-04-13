package edu.cnu.mdi.splot.fit;

import java.util.Objects;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.fitting.leastsquares.ParameterValidator;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

import edu.cnu.mdi.splot.pdata.FitVectors;

/**
 * Nonlinear least-squares fitter for a Lorentzian (Cauchy / Breit-Wigner)
 * peak with a constant baseline:
 *
 * <pre>
 * y(x) = A * (&Gamma;/2)&sup2; / [(x - x&sub0;)&sup2; + (&Gamma;/2)&sup2;] + B
 * </pre>
 *
 * <h3>Normalisation</h3>
 * <p>The parameter {@code A} is the peak height above baseline (i.e.
 * {@code y(x₀) = A + B}), not the area. This matches the Gaussian fitter
 * convention used elsewhere in splot, making it easy to switch between the
 * two.</p>
 *
 * <h3>Parameter ordering</h3>
 * <ul>
 *   <li>{@code params[0] = A}   &mdash; peak height above baseline</li>
 *   <li>{@code params[1] = x₀}  &mdash; peak centre</li>
 *   <li>{@code params[2] = Γ}   &mdash; full-width at half-maximum (FWHM);
 *       enforced {@code >= DEFAULT_MIN_GAMMA}</li>
 *   <li>{@code params[3] = B}   &mdash; baseline offset</li>
 * </ul>
 *
 * <h3>Relationship to Gaussian</h3>
 * <p>A Lorentzian has heavier tails than a Gaussian of the same FWHM. For a
 * Gaussian, {@code FWHM = 2√(2 ln 2)·σ ≈ 2.355·σ}. Both models have the same
 * peak-height parameter {@code A} and baseline {@code B}, so they can be
 * compared directly.</p>
 *
 * <h3>Typical applications</h3>
 * <p>Resonance peaks in nuclear/particle physics (Breit-Wigner), spectral
 * line shapes in atomic spectroscopy, quality-factor measurements in
 * mechanical or electrical resonators, natural line widths.</p>
 *
 * @author heddle
 */
public final class LorentzianFitter extends ALeastSquaresFitter {

	// -----------------------------------------------------------------------
	// Parameter indices
	// -----------------------------------------------------------------------

	/** Index of peak-height parameter {@code A}. */
	public static final int IDX_A  = 0;
	/** Index of peak-centre parameter {@code x₀}. */
	public static final int IDX_X0 = 1;
	/** Index of FWHM parameter {@code Γ}. */
	public static final int IDX_G  = 2;
	/** Index of baseline parameter {@code B}. */
	public static final int IDX_B  = 3;

	/** Display names used in legend strings and fit summaries. */
	public static final String[] PARAM_NAMES = { "A", "x\u2080", "\u0393", "B" };

	/** Minimum FWHM to avoid divide-by-zero in the Jacobian. */
	public static final double DEFAULT_MIN_GAMMA = 1e-12;

	// -----------------------------------------------------------------------
	// Constructors
	// -----------------------------------------------------------------------

	/**
	 * Creates a fitter with the default Levenberg-Marquardt optimizer.
	 */
	public LorentzianFitter() {
		this(new LevenbergMarquardtOptimizer());
	}

	/**
	 * Creates a fitter with a custom optimizer.
	 *
	 * @param optimizer the least-squares optimizer to use (non-null)
	 */
	public LorentzianFitter(LeastSquaresOptimizer optimizer) {
		super(Objects.requireNonNull(optimizer, "optimizer"),
		      (x, y, w) -> InitialGuess.guess(x, y));
	}

	// -----------------------------------------------------------------------
	// ALeastSquaresFitter contract
	// -----------------------------------------------------------------------

	@Override
	protected int getParameterCount() {
		return 4;
	}

	@Override
	protected MultivariateJacobianFunction model(double[] x) {
		return new LorentzianModel(x);
	}

	@Override
	protected double[] defaultInitialGuess(double[] x, double[] y, double[] weights) {
		return InitialGuess.guess(x, y);
	}

	@Override
	protected ParameterValidator defaultValidator() {
		double[] lo = {
			Double.NEGATIVE_INFINITY,
			Double.NEGATIVE_INFINITY,
			DEFAULT_MIN_GAMMA,
			Double.NEGATIVE_INFINITY
		};
		double[] hi = {
			Double.POSITIVE_INFINITY,
			Double.POSITIVE_INFINITY,
			Double.POSITIVE_INFINITY,
			Double.POSITIVE_INFINITY
		};
		return clampingValidator(lo, hi);
	}

	// -----------------------------------------------------------------------
	// Evaluator
	// -----------------------------------------------------------------------

	/**
	 * Returns an {@link Evaluator} that computes
	 * {@code A * (Γ/2)² / [(x − x₀)² + (Γ/2)²] + B}.
	 *
	 * @param fit fit result from a previous call to {@link #fit}
	 * @return evaluator for the fitted Lorentzian
	 * @throws NullPointerException     if {@code fit} is {@code null}
	 * @throws IllegalArgumentException if the result has the wrong parameter count
	 */
	@Override
	public Evaluator asEvaluator(FitResult fit) {
		Objects.requireNonNull(fit, "fit");
		if (fit.params.length != getParameterCount()) {
			throw new IllegalArgumentException(
					"LorentzianFitter: expected " + getParameterCount()
					+ " parameters, got " + fit.params.length);
		}
		final double A  = fit.params[IDX_A];
		final double x0 = fit.params[IDX_X0];
		final double G  = fit.params[IDX_G];
		final double B  = fit.params[IDX_B];
		final double hg = G / 2.0;
		final double hg2 = hg * hg;
		return x -> {
			double dx = x - x0;
			return A * hg2 / (dx * dx + hg2) + B;
		};
	}

	// -----------------------------------------------------------------------
	// IFitStringGetter
	// -----------------------------------------------------------------------

	@Override
	public String modelName() {
		return "Lorentzian";
	}

	@Override
	public String functionForm() {
		// y(x) = A·(Γ/2)² / [(x−x₀)² + (Γ/2)²] + B
		return "y(x) = A\u00b7(\u0393/2)\u00b2 / [(x\u2212x\u2080)\u00b2 + (\u0393/2)\u00b2] + B";
	}

	@Override
	public String parameterName(int index) {
		if (index < 0 || index >= getParameterCount()) {
			throw new IllegalArgumentException("bad parameter index: " + index);
		}
		return PARAM_NAMES[index];
	}

	@Override
	public IFitStringGetter getStringGetter() {
		return this;
	}

	// -----------------------------------------------------------------------
	// Analytic model + Jacobian
	// -----------------------------------------------------------------------

	/**
	 * Analytic model and Jacobian for the Lorentzian.
	 *
	 * <p>Let {@code h = Γ/2}, {@code d = x − x₀}, {@code D = d² + h²}. Then:</p>
	 * <ul>
	 *   <li>∂y/∂A  = h²/D</li>
	 *   <li>∂y/∂x₀ = 2A·h²·d / D²</li>
	 *   <li>∂y/∂Γ  = A·h·(D − h²) / D²  =  A·h·d² / D²</li>
	 *   <li>∂y/∂B  = 1</li>
	 * </ul>
	 */
	private static final class LorentzianModel implements MultivariateJacobianFunction {

		private final double[] x;

		LorentzianModel(double[] x) {
			this.x = x.clone();
		}

		@Override
		public Pair<RealVector, RealMatrix> value(RealVector point) {
			final double[] p  = point.toArray();
			final double   A  = p[IDX_A];
			final double   x0 = p[IDX_X0];
			final double   G  = p[IDX_G];
			final double   B  = p[IDX_B];

			final double hg  = G / 2.0;
			final double hg2 = hg * hg;

			final int        n   = x.length;
			final double[]   val = new double[n];
			final double[][] jac = new double[n][4];

			for (int i = 0; i < n; i++) {
				final double d  = x[i] - x0;
				final double d2 = d * d;
				final double D  = d2 + hg2;
				final double D2 = D * D;

				val[i] = A * hg2 / D + B;

				jac[i][IDX_A]  = hg2 / D;
				jac[i][IDX_X0] = 2.0 * A * hg2 * d / D2;
				jac[i][IDX_G]  = A * hg * d2 / D2;   // ∂/∂Γ = (∂/∂h)*(1/2), h=Γ/2
				jac[i][IDX_B]  = 1.0;
			}

			return new Pair<>(new ArrayRealVector(val, false),
			                  new Array2DRowRealMatrix(jac, false));
		}
	}

	// -----------------------------------------------------------------------
	// Heuristic initial guess  (mirrors GaussianFitter.InitialGuess strategy)
	// -----------------------------------------------------------------------

	/**
	 * Heuristic initial-guess strategy for Lorentzian parameters.
	 *
	 * <p>The strategy is intentionally identical to the Gaussian guesser so that
	 * users can switch between the two models and get consistent convergence:</p>
	 * <ol>
	 *   <li>Baseline {@code B}: average of the endpoint y-values.</li>
	 *   <li>Peak height {@code A}: maximum (y − B).</li>
	 *   <li>Centre {@code x₀}: x at the peak.</li>
	 *   <li>FWHM {@code Γ}: estimated from the half-maximum crossing points.
	 *       Falls back to 10% of the x-range if no crossing is found.</li>
	 * </ol>
	 */
	static final class InitialGuess {

		private InitialGuess() {}

		static double[] guess(double[] x, double[] y) {
			final int n = x.length;
			if (n == 0) return new double[] { 1.0, 0.0, 1.0, 0.0 };

			// Sort by x.
			Integer[] idx = new Integer[n];
			for (int i = 0; i < n; i++) idx[i] = i;
			java.util.Arrays.sort(idx, (a, b) -> Double.compare(x[a], x[b]));

			// Baseline: average of endpoints.
			double B = 0.5 * (y[idx[0]] + y[idx[n - 1]]);

			// Peak.
			int    imax   = 0;
			double maxVal = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < n; i++) {
				if (y[i] > maxVal) { maxVal = y[i]; imax = i; }
			}
			double A  = maxVal - B;
			double x0 = x[imax];
			if (A == 0.0) A = 1.0;

			// FWHM from half-max crossings.
			double half = B + 0.5 * A;
			double x1   = x0, x2 = x0;
			boolean foundLeft = false, foundRight = false;

			// Walk left from peak in the sorted order.
			int sortedPeak = 0;
			for (int k = 0; k < n; k++) {
				if (idx[k] == imax) { sortedPeak = k; break; }
			}
			for (int k = sortedPeak; k >= 0; k--) {
				if (y[idx[k]] <= half) { x1 = x[idx[k]]; foundLeft = true; break; }
			}
			for (int k = sortedPeak; k < n; k++) {
				if (y[idx[k]] <= half) { x2 = x[idx[k]]; foundRight = true; break; }
			}

			double gamma;
			if (foundLeft && foundRight) {
				gamma = Math.abs(x2 - x1);
			} else {
				double xRange = Math.abs(x[idx[n - 1]] - x[idx[0]]);
				gamma = 0.1 * Math.max(1e-12, xRange);
			}

			return new double[] { A, x0, Math.max(DEFAULT_MIN_GAMMA, gamma), B };
		}
	}

	// -----------------------------------------------------------------------
	// Stand-alone test
	// -----------------------------------------------------------------------

	/**
	 * Quick smoke test: fits synthetic Lorentzian data and prints the result.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		final double A  = 10.0;
		final double x0 = 5.0;
		final double G  = 1.5;   // FWHM
		final double B  = 0.5;
		final double hg = G / 2.0;
		final double hg2 = hg * hg;

		FitVectors data = FitVectors.testData(
				x -> A * hg2 / ((x - x0) * (x - x0) + hg2) + B,
				0.0, 10.0, 80, 3.0, 5.0);

		LorentzianFitter fitter = new LorentzianFitter();
		FitResult result = fitter.fit(data.x, data.y, data.w);

		System.out.println("True: A=" + A + "  x0=" + x0 + "  Γ=" + G + "  B=" + B);
		System.out.println(result);
	}
}