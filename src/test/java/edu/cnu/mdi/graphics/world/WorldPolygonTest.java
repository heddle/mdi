package edu.cnu.mdi.graphics.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Regression coverage for {@link WorldPolygon#contains(double, double)}'s
 * ray-casting point-in-polygon test, which had zero prior test coverage.
 */
class WorldPolygonTest {

	private static WorldPolygon square() {
		return new WorldPolygon(
				new double[] { 0, 10, 10, 0 },
				new double[] { 0, 0, 10, 10 },
				4);
	}

	/** An L-shaped concave polygon: the top-right quadrant is a notch. */
	private static WorldPolygon lShape() {
		return new WorldPolygon(
				new double[] { 0, 10, 10, 5, 5, 0 },
				new double[] { 0, 0, 5, 5, 10, 10 },
				6);
	}

	@Test
	void containsIsTrueWellInsideASimpleSquare() {
		assertTrue(square().contains(5.0, 5.0));
	}

	@Test
	void containsIsFalseWellOutsideASimpleSquare() {
		WorldPolygon square = square();
		assertFalse(square.contains(15.0, 15.0));
		assertFalse(square.contains(-5.0, -5.0));
	}

	@Test
	void containsRespectsTheNotchOfAConcavePolygon() {
		WorldPolygon l = lShape();

		// Inside the notch (top-right quadrant) -- must be excluded.
		assertFalse(l.contains(7.0, 7.0));

		// Inside the two legs of the L.
		assertTrue(l.contains(2.0, 2.0), "bottom-left leg");
		assertTrue(l.contains(2.0, 8.0), "top-left leg");
		assertTrue(l.contains(8.0, 2.0), "bottom-right leg");
	}

	@Test
	void containsIsFalseForADegeneratePolygon() {
		// A "polygon" with fewer than 3 points can't enclose any area.
		WorldPolygon line = new WorldPolygon(new double[] { 0, 10 }, new double[] { 0, 10 }, 2);
		assertFalse(line.contains(5.0, 5.0));

		WorldPolygon empty = new WorldPolygon();
		assertFalse(empty.contains(0.0, 0.0));
	}

	@Test
	void intContainsDelegatesToDoubleContains() {
		WorldPolygon square = square();
		assertTrue(square.contains(5, 5));
		assertFalse(square.contains(15, 15));
	}
}
