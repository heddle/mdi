package edu.cnu.mdi.splot.pdata;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HistoDataTest {

	@Test
	void binsIncludeBothEndpointsAndTrackOutOfRangeValues() {
		HistoData histogram = new HistoData("h", 0.0, 10.0, 10);
		histogram.addAll(new double[] { -1.0, 0.0, 0.5, 10.0, 11.0 });

		assertEquals(2, histogram.getCount(0));
		assertEquals(1, histogram.getCount(9));
		assertEquals(1, histogram.getUnderCount());
		assertEquals(1, histogram.getOverCount());
		assertEquals(5, histogram.getTotalCount());
	}

	@Test
	void nonFiniteSamplesAreIgnoredAndLimitsAreRejected() {
		HistoData histogram = new HistoData("h", 0.0, 1.0, 2);
		histogram.addAll(new double[] {
				Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY });
		assertEquals(0, histogram.getTotalCount());

		assertThrows(IllegalArgumentException.class,
				() -> new HistoData("h", Double.NEGATIVE_INFINITY, 1.0, 2));
		assertThrows(IllegalArgumentException.class,
				() -> new HistoData("h", new double[] { 0.0, Double.NaN }));
		assertThrows(IllegalArgumentException.class,
				() -> new Histo2DData("h", 0, Double.POSITIVE_INFINITY, 2, 0, 1, 2));
	}

	@Test
	void statisticsResultCannotCorruptTheCache() {
		HistoData histogram = new HistoData("h", 0.0, 2.0, 2);
		histogram.add(0.5);
		histogram.add(1.5);
		double[] expected = histogram.getBasicStatistics();
		double[] exposed = histogram.getBasicStatistics();
		exposed[0] = 999.0;

		assertArrayEquals(expected, histogram.getBasicStatistics());
		assertThrows(IllegalArgumentException.class,
				() -> histogram.setCount(0.5, -1));
	}
}
