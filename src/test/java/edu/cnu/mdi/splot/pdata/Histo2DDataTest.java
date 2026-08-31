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

	private static Histo2DData grid3x3() {
		Histo2DData h = new Histo2DData("heat", 0, 3, 3, 0, 3, 3);
		h.fill(0.5, 0.5, 1);
		h.fill(1.5, 0.5, 2);
		h.fill(2.5, 0.5, 3);
		h.fill(0.5, 1.5, 4);
		h.fill(1.5, 1.5, 5);
		h.fill(2.5, 1.5, 6);
		h.fill(0.5, 2.5, 7);
		h.fill(1.5, 2.5, 8);
		h.fill(2.5, 2.5, 9);
		return h;
	}

	@Test
	void localMean3x3AveragesTheFullNeighborhoodAtTheCenterBin() {
		Histo2DData h = grid3x3();
		// Center bin's 3x3 neighborhood is the entire grid: mean of 1..9.
		assertEquals(5.0, h.localMean3x3(1.5, 1.5), 1.0e-12);
	}

	@Test
	void localMean3x3ClipsTheNeighborhoodAtACornerBin() {
		Histo2DData h = grid3x3();
		// Corner bin (0,0): only the 2x2 block of in-range neighbors counts.
		assertEquals((1.0 + 4.0 + 2.0 + 5.0) / 4.0, h.localMean3x3(0.5, 0.5), 1.0e-12);
	}

	@Test
	void localMean3x3IsZeroOutsideTheGrid() {
		Histo2DData h = grid3x3();
		assertEquals(0.0, h.localMean3x3(-10.0, -10.0), 1.0e-12);
	}

	@Test
	void percentileRanksBinsAgainstAllOccupiedBins() {
		Histo2DData h = grid3x3();
		assertEquals(100.0 * 9 / 9, h.percentile(2.5, 2.5), 1.0e-9); // bin value 9 (max)
		assertEquals(100.0 * 1 / 9, h.percentile(0.5, 0.5), 1.0e-9); // bin value 1 (min)
		assertEquals(100.0 * 5 / 9, h.percentile(1.5, 1.5), 1.0e-9); // bin value 5 (median)
	}

	@Test
	void percentileIsZeroForAnEmptyBinOrOutOfRangePoint() {
		Histo2DData h = new Histo2DData("heat", 0, 3, 3, 0, 3, 3);
		h.fill(1.5, 1.5, 5); // only the center bin is occupied

		assertEquals(0.0, h.percentile(0.5, 0.5), 1.0e-12, "an empty (zero-value) bin ranks at 0");
		assertEquals(0.0, h.percentile(-10.0, -10.0), 1.0e-12, "an out-of-range point ranks at 0");
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
