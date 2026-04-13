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
 * Nonlinear least-squares fitter for a power law with a constant offset:
 *
 * <pre>
 * y(x) = A * x^n + C
 * </pre>
 *
 * <h3>Parameter ordering</h3>
 * <ul>
 *   <li>{@code params[0] = A}  &mdash; amplitude</li>
 *   <li>{@code params[1] = n}  &mdash; exponent (real-valued, unconstrained)</li>
 *   <li>{@code params[2] = C}  &mdash; baseline offset</li>
 * </ul>
 *
 * <h3>Typical applications</h3>
 * <p>Log-log scaling laws, stellar flux vs distance, turbulence spectra,
 * fractal relationships, and any data that appears linear on a log-log plot.</p>
 *
 * <h3>Important: domain restriction</h3>
 * <p>The model {@code A * x^n} is only defined for {@code x > 0}. Any data
 * point with {@code x <= 0} will cause the fit to fail. If your data includes
 * non-positive x values, shift or truncate before fitting.</p>
 *
 * <h3>Initial guess strategy</h3>
 * <p>The baseline {@code C} is estimated from the smallest-y points. The
 * offset-subtracted data is log-log linearised: fitting a line to
 * {@code log(y − C)} vs {@code log(x)} gives {@code n} as the slope and
 * {@code log(A)} as the intercept.</p>
 *
 * @author heddle
 */
public final class PowerLawFitter extends ALeastSquaresFitter {

	// -----------------------------------------------------------------------
	// Parameter indices
	// -----------------------------------------------------------------------

	/** Index of amplitude parameter {@code A}. */
	public static final int IDX_A = 0;
	/** Index of exponent parameter {@code n}. */
	public static final int IDX_N = 1;
	/** Index of baseline-offset parameter {@code C}. */
	public static final int IDX_C = 2;

	/** Display names used in legend strings and fit summaries. */
	public static final String[] PARAM_NAMES = { "A", "n", "C" };

	/** Minimum absolute value of x permitted in the model; guards log(x). */
	private static final double MIN_X = 1e-300;

	// -----------------------------------------------------------------------
	// Constructors
	// -----------------------------------------------------------------------

	/**
	 * Creates a fitter with the default Levenberg-Marquardt optimizer.
	 */
	public PowerLawFitter() {
		this(new LevenbergMarquardtOptimizer());
	}

	/**
	 * Creates a fitter with a custom optimizer.
	 *
	 * @param optimizer the least-squares optimizer to use (non-null)
	 */
	public PowerLawFitter(LeastSquaresOptimizer optimizer) {
		super(Objects.requireNonNull(optimizer, "optimizer"),
		      (x, y, w) -> InitialGuess.guess(x, y));
	}

	// -----------------------------------------------------------------------
	// ALeastSquaresFitter contract
	// -----------------------------------------------------------------------

	@Override
	protected int getParameterCount() {
		return 3;
	}

	@Override
	protected MultivariateJacobianFunction model(double[] x) {
		return new PowerLawModel(x);
	}

	@Override
	protected double[] defaultInitialGuess(double[] x, double[] y, double[] weights) {
		return InitialGuess.guess(x, y);
	}

	@Override
	protected ParameterValidator defaultValidator() {
		// n is unconstrained; A and C are unconstrained.
		// No hard bounds needed: the model handles all real n for x > 0.
		return null;
	}

	// -----------------------------------------------------------------------
	// Evaluator
	// -----------------------------------------------------------------------

	/**
	 * Returns an {@link Evaluator} that computes {@code A * x^n + C}.
	 *
	 * @param fit fit result from a previous call to {@link #fit}
	 * @return evaluator for the fitted function
	 * @throws NullPointerException     if {@code fit} is {@code null}
	 * @throws IllegalArgumentException if the result has the wrong parameter count
	 */
	@Override
	public Evaluator asEvaluator(FitResult fit) {
		Objects.requireNonNull(fit, "fit");
		if (fit.params.length != getParameterCount()) {
			throw new IllegalArgumentException(
					"PowerLawFitter: expected " + getParameterCount()
					+ " parameters, got " + fit.params.length);
		}
		final double A = fit.params[IDX_A];
		final double n = fit.params[IDX_N];
		final double C = fit.params[IDX_C];
		return x -> A * Math.pow(x, n) + C;
	}

	// -----------------------------------------------------------------------
	// IFitStringGetter
	// -----------------------------------------------------------------------

	@Override
	public String modelName() {
		return "Power Law";
	}

	@Override
	public String functionForm() {
		return "y(x) = A\u00b7x\u207f + C";
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
	 * Analytic model and Jacobian for {@code A * x^n + C}.
	 *
	 * <p>Partial derivatives:</p>
	 * <ul>
	 *   <li>∂y/∂A = x^n</li>
	 *   <li>∂y/∂n = A * x^n * ln(x)</li>
	 *   <li>∂y/∂C = 1</li>
	 * </ul>
	 *
	 * <p>Points with {@code x <= 0} yield model value {@code C} and zero
	 * Jacobian entries, effectively excluding them from the fit direction.</p>
	 */
	private static final class PowerLawModel implements MultivariateJacobianFunction {

		private final double[] x;

		PowerLawModel(double[] x) {
			this.x = x.clone();
		}

		@Override
		public Pair<RealVector, RealMatrix> value(RealVector point) {
			final double[] p   = point.toArray();
			final double   A   = p[IDX_A];
			final double   n   = p[IDX_N];
			final double   C   = p[IDX_C];

			final int        sz  = x.length;
			final double[]   val = new double[sz];
			final double[][] jac = new double[sz][3];

			for (int i = 0; i < sz; i++) {
				final double xi = x[i];

				if (xi <= 0.0) {
					// Can't evaluate x^n for non-positive x when n is non-integer.
					// Return C and zero gradient to let the optimizer move away.
					val[i]       = C;
					jac[i][IDX_C] = 1.0;
					continue;
				}

				final double xn   = Math.pow(xi, n);
				final double lnx  = Math.log(xi);

				val[i] = A * xn + C;

				jac[i][IDX_A] = xn;
				jac[i][IDX_N] = A * xn * lnx;
				jac[i][IDX_C] = 1.0;
			}

			return new Pair<>(new ArrayRealVector(val, false),
			                  new Array2DRowRealMatrix(jac, false));
		}
	}

	// -----------------------------------------------------------------------
	// Heuristic initial guess
	// -----------------------------------------------------------------------

	/**
	 * Heuristic initial-guess strategy for power-law parameters.
	 *
	 * <ol>
	 *   <li>Estimate baseline {@code C} from the smallest-y values (bottom 10%).
	 *       For pure power laws without offset this will be near zero.</li>
	 *   <li>Remove non-positive x points and log-transform the remainder:
	 *       {@code log(y − C)} vs {@code log(x)}.</li>
	 *   <li>Fit a line (OLS) to the log-log data. Slope → {@code n},
	 *       intercept → {@code log(A)} → {@code A = exp(intercept)}.</li>
	 * </ol>
	 */
	static final class InitialGuess {

		private InitialGuess() {}

		static double[] guess(double[] x, double[] y) {
			final int n = x.length;

			if (n == 0) return new double[] { 1.0, 1.0, 0.0 };
			if (n == 1) return new double[] { y[0], 1.0, 0.0 };

			// Sort by y to estimate baseline from the bottom 10%.
			Integer[] yIdx = new Integer[n];
			for (int i = 0; i < n; i++) yIdx[i] = i;
			java.util.Arrays.sort(yIdx, (a, b) -> Double.compare(y[a], y[b]));

			int tailLen = Math.max(1, n / 10);
			double sumTail = 0.0;
			for (int k = 0; k < tailLen; k++) sumTail += y[yIdx[k]];
			double C = sumTail / tailLen;

			// Log-log linearisation on x > 0 points.
			double sumLX = 0, sumLY = 0, sumLX2 = 0, sumLXLY = 0;
			int count = 0;

			for (int i = 0; i < n; i++) {
				double xi = x[i];
				double yShifted = y[i] - C;
				if (xi > MIN_X && Math.abs(yShifted) > 1e-30) {
					double lx = Math.log(xi);
					double ly = Math.log(Math.abs(yShifted));
					sumLX   += lx;
					sumLY   += ly;
					sumLX2  += lx * lx;
					sumLXLY += lx * ly;
					count++;
				}
			}

			if (count < 2) {
				return new double[] { y[0] - C, 1.0, C };
			}

			double denom = count * sumLX2 - sumLX * sumLX;
			double slope, intercept;
			if (Math.abs(denom) < 1e-30) {
				slope     = 1.0;
				intercept = Math.log(Math.max(1e-30, Math.abs(y[0] - C)));
			} else {
				slope     = (count * sumLXLY - sumLX * sumLY) / denom;
				intercept = (sumLY - slope * sumLX) / count;
			}

			double A = Math.exp(intercept);
			// Preserve sign of the data relative to the baseline.
			if (y[yIdx[n - 1]] < C) A = -A;

			return new double[] { A, slope, C };
		}
	}

	// -----------------------------------------------------------------------
	// Stand-alone test
	// -----------------------------------------------------------------------

	/**
	 * Quick smoke test: fits synthetic power-law data and prints the result.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		final double A = 3.0;
		final double n = 2.5;
		final double C = 1.0;

		FitVectors data = FitVectors.testData(
				x -> A * Math.pow(x, n) + C,
				0.5, 5.0, 50, 3.0, 5.0);

		PowerLawFitter fitter = new PowerLawFitter();
		FitResult result = fitter.fit(data.x, data.y, data.w);

		System.out.println("True: A=" + A + "  n=" + n + "  C=" + C);
		System.out.println(result);
	}
}