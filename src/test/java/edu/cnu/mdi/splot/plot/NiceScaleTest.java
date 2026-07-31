package edu.cnu.mdi.splot.plot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NiceScaleTest {

	@Test
	void createsExpectedNiceScale() {
		NiceScale scale = new NiceScale(1.1, 9.1, 6);
		assertEquals(2, scale.getTickSpacing());
		assertEquals(0, scale.getNiceMin());
		assertEquals(10, scale.getNiceMax());
		assertEquals(6, scale.getTickCount());
	}

	@Test
	void handlesReversedDegenerateAndNonFiniteRanges() {
		NiceScale reversed = new NiceScale(10, -2, 5);
		assertTrue(reversed.getNiceMin() <= -2);
		assertTrue(reversed.getNiceMax() >= 10);

		NiceScale degenerate = new NiceScale(5, 5);
		assertTrue(degenerate.getNiceMin() < 5);
		assertTrue(degenerate.getNiceMax() > 5);

		NiceScale nonFinite = new NiceScale(Double.NaN, Double.POSITIVE_INFINITY);
		assertEquals(0, nonFinite.getNiceMin());
		assertEquals(1, nonFinite.getNiceMax());
	}
}
