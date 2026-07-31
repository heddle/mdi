package edu.cnu.mdi.splot.pdata;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class Histo2DDataTest {

	@Test
	void binsEndpointsAndAccountsForEveryOutOfRangeRegion() {
		Histo2DData histogram = new Histo2DData("heat", 0, 2, 2, 0, 2, 2);
		histogram.fill(0, 0);
		histogram.fill(2, 2, 2);
		histogram.fill(-1, 1);
		histogram.fill(3, 1);
		histogram.fill(1, -1);
		histogram.fill(1, 3);
		histogram.fill(-1, -1);
		histogram.fill(-1, 3);
		histogram.fill(3, -1);
		histogram.fill(3, 3);

		assertEquals(1, histogram.bin(0, 0));
		assertEquals(2, histogram.bin(1, 1));
		assertEquals(2, histogram.getGoodCount());
		assertEquals(10, histogram.getTotalCount());
		assertEquals(1, histogram.getXUnderCount());
		assertEquals(1, histogram.getXOverCount());
		assertEquals(1, histogram.getYUnderCount());
		assertEquals(1, histogram.getYOverCount());
		assertEquals(1, histogram.getXUnderYUnderCount());
		assertEquals(1, histogram.getXUnderYOverCount());
		assertEquals(1, histogram.getXOverYUnderCount());
		assertEquals(1, histogram.getXOverYOverCount());
	}

	@Test
	void snapshotsAreDefensiveAndCachesFollowMutationAndClear() {
		Histo2DData histogram = new Histo2DData("heat", 0, 2, 2, 0, 2, 2);
		histogram.fill(0.5, 0.5, 2);
		assertEquals(2, histogram.maxBin());
		assertEquals(0.5, histogram.meanBin());

		double[][] snapshot = histogram.snapshotBins();
		snapshot[0][0] = 99;
		assertEquals(2, histogram.bin(0, 0));

		histogram.fill(1.5, 1.5, 4);
		assertEquals(4, histogram.maxBin());
		assertEquals(1.5, histogram.meanBin());
		assertEquals(50, histogram.percentile(0.5, 0.5));
		assertEquals(100, histogram.percentile(1.5, 1.5));

		histogram.clear();
		assertEquals(0, histogram.getTotalCount());
		assertEquals(0, histogram.maxBin());
		assertArrayEquals(new double[] { 0, 1 }, histogram.xBinRange(0.5));
	}

	@Test
	void nonFiniteFillsAreIgnored() {
		Histo2DData histogram = new Histo2DData("heat", 0, 1, 1, 0, 1, 1);
		histogram.fill(Double.NaN, 0.5);
		histogram.fill(0.5, Double.POSITIVE_INFINITY);
		histogram.fill(0.5, 0.5, Double.NaN);
		assertEquals(0, histogram.getTotalCount());
	}
}
