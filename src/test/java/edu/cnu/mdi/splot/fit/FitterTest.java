package edu.cnu.mdi.splot.fit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FitterTest {

	@Test
	void polynomialFitRecoversExactCoefficients() {
		double[] x = { -3, -2, -1, 0, 1, 2, 3 };
		double[] y = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			y[i] = 2.0 - 3.0 * x[i] + 0.5 * x[i] * x[i];
		}

		FitResult result = PolynomialFitter.fit(2, x, y);
		assertEquals(2.0, result.param(0), 1.0e-10);
		assertEquals(-3.0, result.param(1), 1.0e-10);
		assertEquals(0.5, result.param(2), 1.0e-10);
		assertEquals(0.0, result.chiSquare, 1.0e-18);
		assertEquals(y[5], result.evaluator.value(x[5]), 1.0e-10);
	}

	@Test
	void fitValidationRejectsInvalidObservationsAndWeights() {
		PolynomialFitter fitter = new PolynomialFitter(1);
		assertThrows(IllegalArgumentException.class,
				() -> fitter.fit(new double[] { 0, 1 }, new double[] { 1 }));
		assertThrows(IllegalArgumentException.class,
				() -> fitter.fit(new double[] { 0, Double.NaN }, new double[] { 1, 2 }));
		assertThrows(IllegalArgumentException.class,
				() -> fitter.fit(new double[] { 0, 1 }, new double[] { 1, 2 },
						new double[] { 1, -1 }));
		assertThrows(IllegalArgumentException.class,
				() -> new PolynomialFitter(3).fit(
						new double[] { 0, 1, 2 }, new double[] { 0, 1, 4 }));
		assertThrows(IllegalArgumentException.class,
				() -> fitter.fit(new double[] { 0, 1 }, new double[] { 1, 2 },
						null, new double[] { 0, Double.NaN }, null));
	}

	@Test
	void sigmaConversionProducesInverseVarianceWeights() {
		double[] weights = ALeastSquaresFitter.weightsFromSigmaY(
				new double[] { 0.5, 2.0 });
		assertEquals(4.0, weights[0]);
		assertEquals(0.25, weights[1]);
		assertThrows(IllegalArgumentException.class,
				() -> ALeastSquaresFitter.weightsFromSigmaY(new double[] { 0.0 }));
	}

	@Test
	void cubicSplineInterpolatesKnotsAndFindsRoots() {
		CubicSpline spline = new CubicSpline(
				new double[] { -2, -1, 0, 1, 2 },
				new double[] { 4, 1, 0, 1, 4 });
		assertEquals(1.0, spline.value(-1.0), 1.0e-12);
		assertEquals(4.0, spline.value(10.0), 1.0e-12);
		double[] roots = spline.findRoots(-2, 2);
		assertEquals(1, roots.length);
		assertEquals(0.0, roots[0], 1.0e-9);
	}

	@Test
	void cubicSplineRejectsNonFiniteCoordinates() {
		assertThrows(IllegalArgumentException.class,
				() -> new CubicSpline(new double[] { 0, 1 },
						new double[] { 0, Double.NaN }));
		assertTrue(new CubicSpline(new double[] { 0, 1 },
				new double[] { 0, 1 }).isValid());
	}
}
