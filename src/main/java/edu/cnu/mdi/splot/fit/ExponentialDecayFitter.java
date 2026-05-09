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
 * Nonlinear least-squares fitter for exponential decay with a constant offset:
 *
 * <pre>
 * y(x) = A * exp(-x / &tau;) + C
 * </pre>
 *
 * <p>Parameter ordering</p>
 * <ul>
 *   <li>{@code params[0] = A}  &mdash; amplitude</li>
 *   <li>{@code params[1] = τ}  &mdash; decay constant (enforced {@code > 0})</li>
 *   <li>{@code params[2] = C}  &mdash; baseline offset</li>
 * </ul>
 *
 * <p>Typical applications</p>
 * <p>Radioactive decay, RC circuit discharge, fluorescence lifetime,
 * exponential growth (negative τ via sign of A).</p>
 *
 * <p>Initial guess strategy</p>
 * <p>The offset {@code C} is estimated from the tail of the data. The
 * amplitude {@code A} comes from the first point minus the offset. The
 * decay constant {@code τ} is estimated by log-linearising the
 * offset-subtracted data and fitting a line through the log-transformed
 * values.</p>
 *
 * <p>Constraint</p>
 * <p>{@code τ >= DEFAULT_MIN_TAU} is enforced by a clamping parameter
 * validator to prevent division by zero in the Jacobian.</p>
 *
 * @author heddle
 */
public final class ExponentialDecayFitter extends ALeastSquaresFitter {

	// -----------------------------------------------------------------------
	// Parameter indices (public for callers that need to index params[])
	// -----------------------------------------------------------------------

	/** Index of amplitude parameter {@code A}. */
	public static final int IDX_A   = 0;
	/** Index of decay-constant parameter {@code τ}. */
	public static final int IDX_TAU = 1;
	/** Index of baseline-offset parameter {@code C}. */
	public static final int IDX_C   = 2;

	/** Display names used in legend strings and fit summaries. */
	public static final String[] PARAM_NAMES = { "A", "\u03c4", "C" };

	/** Minimum decay constant to avoid divide-by-zero in the Jacobian. */
	public static final double DEFAULT_MIN_TAU = 1e-12;

	// -----------------------------------------------------------------------
	// Constructors
	// -----------------------------------------------------------------------

	/**
	 * Creates a fitter with the default Levenberg-Marquardt optimizer.
	 */
	public ExponentialDecayFitter() {
		this(new LevenbergMarquardtOptimizer());
	}

	/**
	 * Creates a fitter with a custom optimizer.
	 *
	 * @param optimizer the least-squares optimizer to use (non-null)
	 */
	public ExponentialDecayFitter(LeastSquaresOptimizer optimizer) {
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
		return new ExpDecayModel(x);
	}

	@Override
	protected double[] defaultInitialGuess(double[] x, double[] y, double[] weights) {
		return InitialGuess.guess(x, y);
	}

	@Override
	protected ParameterValidator defaultValidator() {
		double[] lo = { Double.NEGATIVE_INFINITY, DEFAULT_MIN_TAU, Double.NEGATIVE_INFINITY };
		double[] hi = { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
		return clampingValidator(lo, hi);
	}

	// -----------------------------------------------------------------------
	// Evaluator
	// -----------------------------------------------------------------------

	/**
	 * Returns an {@link Evaluator} that computes
	 * {@code A * exp(-x / τ) + C} from the supplied fit result.
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
					"ExponentialDecayFitter: expected " + getParameterCount()
					+ " parameters, got " + fit.params.length);
		}
		final double A   = fit.params[IDX_A];
		final double tau = fit.params[IDX_TAU];
		final double C   = fit.params[IDX_C];
		return x -> A * Math.exp(-x / tau) + C;
	}

	// -----------------------------------------------------------------------
	// IFitStringGetter
	// -----------------------------------------------------------------------

	@Override
	public String modelName() {
		return "Exponential Decay";
	}

	@Override
	public String functionForm() {
		return "y(x) = A\u00b7exp(-x/\u03c4) + C";
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
	 * Analytic model and Jacobian for {@code A * exp(-x/τ) + C}.
	 *
	 * <p>Partial derivatives:</p>
	 * <ul>
	 *   <li>∂y/∂A   = exp(-x/τ)</li>
	 *   <li>∂y/∂τ   = A * x / τ² * exp(-x/τ)</li>
	 *   <li>∂y/∂C   = 1</li>
	 * </ul>
	 */
	private static final class ExpDecayModel implements MultivariateJacobianFunction {

		private final double[] x;

		ExpDecayModel(double[] x) {
			this.x = x.clone();
		}

		@Override
		public Pair<RealVector, RealMatrix> value(RealVector point) {
			final double[] p   = point.toArray();
			final double   A   = p[IDX_A];
			final double   tau = p[IDX_TAU];
			final double   C   = p[IDX_C];

			final int      n   = x.length;
			final double[] val = new double[n];
			final double[][] jac = new double[n][3];

			final double invTau  = 1.0 / tau;
			final double invTau2 = invTau * invTau;

			for (int i = 0; i < n; i++) {
				final double xi = x[i];
				final double e  = Math.exp(-xi * invTau);

				val[i] = A * e + C;

				jac[i][IDX_A]   = e;
				jac[i][IDX_TAU] = A * xi * invTau2 * e;
				jac[i][IDX_C]   = 1.0;
			}

			return new Pair<>(new ArrayRealVector(val, false),
			                  new Array2DRowRealMatrix(jac, false));
		}
	}

	// -----------------------------------------------------------------------
	// Heuristic initial guess
	// -----------------------------------------------------------------------

	/**
	 * Heuristic initial-guess strategy for exponential decay parameters.
	 *
	 * <ol>
	 *   <li>Estimate the baseline {@code C} as the average of the last 10% of
	 *       points (sorted by x), representing the tail of the decay.</li>
	 *   <li>Subtract the baseline and estimate {@code A} from the value at the
	 *       smallest x.</li>
	 *   <li>Log-linearise the offset-subtracted data and fit a line through
	 *       log(y − C) vs x to extract {@code τ = −1/slope}.</li>
	 * </ol>
	 */
	static final class InitialGuess {

		private InitialGuess() {}

		static double[] guess(double[] x, double[] y) {
			final int n = x.length;

			if (n == 0) {
				return new double[] { 1.0, 1.0, 0.0 };
			}
			if (n == 1) {
				return new double[] { y[0], 1.0, 0.0 };
			}

			// Sort indices by x.
			Integer[] idx = new Integer[n];
			for (int i = 0; i < n; i++) idx[i] = i;
			java.util.Arrays.sort(idx, (a, b) -> Double.compare(x[a], x[b]));

			// Baseline: mean of the last 10% of points (at least 1).
			int tailLen = Math.max(1, n / 10);
			double sumTail = 0.0;
			for (int k = n - tailLen; k < n; k++) {
				sumTail += y[idx[k]];
			}
			double C = sumTail / tailLen;

			// Amplitude: first point minus baseline.
			double A = y[idx[0]] - C;
			if (A == 0.0) A = 1.0;

			// Decay constant: log-linear fit on positive (y - C) values.
			double tau = estimateTau(x, y, C, idx, A);

			return new double[] { A, Math.max(tau, DEFAULT_MIN_TAU), C };
		}

		/**
		 * Estimates τ by fitting a line to log(y − C) vs x, using only points
		 * where (y − C) has the same sign as A (i.e. the signal is above the
		 * baseline for decay, below for growth).
		 */
		private static double estimateTau(double[] x, double[] y, double C,
		                                  Integer[] idx, double A) {
			// Collect log-transformed points.
			double sumX = 0, sumLogY = 0, sumXX = 0, sumXLogY = 0;
			int count = 0;

			for (int k : idx) {
				double yShifted = y[k] - C;
				// Only use points on the same side of the baseline as A.
				if (A > 0 ? yShifted > 1e-30 : yShifted < -1e-30) {
					double logY = Math.log(Math.abs(yShifted));
					double xi   = x[k];
					sumX      += xi;
					sumLogY   += logY;
					sumXX     += xi * xi;
					sumXLogY  += xi * logY;
					count++;
				}
			}

			if (count < 2) {
				// Fallback: use x-range as a rough scale.
				double xRange = Math.abs(x[idx[idx.length - 1]] - x[idx[0]]);
				return Math.max(DEFAULT_MIN_TAU, xRange / 3.0);
			}

			// Linear regression: log(|y - C|) = log|A| + (-1/τ)*x
			double denom = count * sumXX - sumX * sumX;
			if (Math.abs(denom) < 1e-30) {
				return Math.max(DEFAULT_MIN_TAU, 1.0);
			}
			double slope = (count * sumXLogY - sumX * sumLogY) / denom;

			// slope ≈ -1/τ  =>  τ ≈ -1/slope
			if (Math.abs(slope) < 1e-30) {
				return Math.max(DEFAULT_MIN_TAU, 1.0);
			}
			return Math.max(DEFAULT_MIN_TAU, -1.0 / slope);
		}
	}

	// -----------------------------------------------------------------------
	// Stand-alone test
	// -----------------------------------------------------------------------

	/**
	 * Quick smoke test: fits synthetic decay data and prints the result.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		final double A   = 5.0;
		final double tau = 2.0;
		final double C   = 0.8;
		final int    n   = 60;

		FitVectors data = FitVectors.testData(
				x -> A * Math.exp(-x / tau) + C,
				0.0, 10.0, n, 3.0, 5.0);

		ExponentialDecayFitter fitter = new ExponentialDecayFitter();
		FitResult result = fitter.fit(data.x, data.y, data.w);

		System.out.println("True: A=" + A + "  τ=" + tau + "  C=" + C);
		System.out.println(result);
	}
}