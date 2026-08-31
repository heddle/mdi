package edu.cnu.mdi.mapping.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

/**
 * Regression coverage for {@link Etopo5Loader}'s elevation lookups, which had
 * zero prior test coverage. Uses the real bundled ETOPO5.DAT resource with
 * well-known geographic sanity checks, since the loader's constructor is
 * private (no way to inject a synthetic grid).
 */
class Etopo5LoaderTest {

	private static Etopo5Loader loader;

	@BeforeAll
	static void loadGrid() throws IOException {
		loader = Etopo5Loader.loadDefaultResource();
	}

	@Test
	void nearestNeighborElevationIsWellAboveSeaLevelNearEverest() {
		int elevation = loader.getElevationMeters(28.0, 87.0);
		assertTrue(elevation > 2000, "expected a high-mountain elevation near Everest, got " + elevation);
	}

	@Test
	void nearestNeighborElevationIsWellBelowSeaLevelNearTheMarianaTrench() {
		int elevation = loader.getElevationMeters(11.0, 142.0);
		assertTrue(elevation < -2000, "expected a deep-ocean elevation near the Mariana Trench, got " + elevation);
	}

	@Test
	void nearestNeighborElevationIsNegativeOverOpenOcean() {
		int elevation = loader.getElevationMeters(0.0, -30.0);
		assertTrue(elevation < 0, "expected a below-sea-level elevation over open ocean, got " + elevation);
	}

	@Test
	void interpolatedElevationIsCloseToTheNearestNeighborValue() {
		int nearest = loader.getElevationMeters(28.0, 87.0);
		double interpolated = loader.getInterpolatedElevationMeters(28.05, 87.05);
		// A small offset (a fraction of the 5' grid spacing) should not move the
		// interpolated value far from the nearest-neighbor value at a nearby grid point.
		assertTrue(Math.abs(interpolated - nearest) < 1000,
				"interpolated (" + interpolated + ") should stay close to nearest-neighbor (" + nearest + ")");
	}

	@Test
	void longitudeWrapsAtTheZeroDegreeSeamForNearestNeighbor() {
		// -0.02 degrees normalizes to 359.98 degrees; both must hit the same column.
		assertEquals(
				loader.getElevationMeters(10.0, 359.98),
				loader.getElevationMeters(10.0, -0.02));
	}

	@Test
	void interpolationAcrossTheZeroDegreeSeamIsContinuousNotDiscontinuous() {
		// Two points 0.04 degrees apart, straddling the seam, should interpolate
		// to close values -- not jump because column indices wrapped incorrectly.
		double justBefore = loader.getInterpolatedElevationMeters(10.0, 359.98);
		double justAfter = loader.getInterpolatedElevationMeters(10.0, 0.02);
		assertTrue(Math.abs(justBefore - justAfter) < 50,
				"expected near-continuous interpolation across the seam, got "
						+ justBefore + " vs " + justAfter);
	}

	@Test
	void nonFiniteCoordinatesAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> loader.getElevationMeters(Double.NaN, 0.0));
		assertThrows(IllegalArgumentException.class, () -> loader.getElevationMeters(0.0, Double.POSITIVE_INFINITY));
		assertThrows(IllegalArgumentException.class,
				() -> loader.getInterpolatedElevationMeters(Double.NaN, 0.0));
	}

	@Test
	void latitudeIsClampedToTheValidRangeRatherThanThrowing() {
		// Should not throw for out-of-range latitude; clamps to the pole row instead.
		int atPole = loader.getElevationMeters(90.0, 0.0);
		int beyondPole = loader.getElevationMeters(150.0, 0.0);
		assertEquals(atPole, beyondPole);
	}
}
