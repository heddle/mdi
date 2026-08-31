package edu.cnu.mdi.splot.pdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Regression coverage for {@link HistoFitPrep}'s peak-finding methods, which
 * had zero prior test coverage.
 */
class HistoFitPrepTest {

	/** Builds a 10-bin [0,10) histogram with the given exact per-bin counts. */
	private static HistoData histoWithCounts(int... counts) {
		HistoData h = new HistoData("h", 0.0, 10.0, counts.length);
		for (int bin = 0; bin < counts.length; bin++) {
			for (int k = 0; k < counts[bin]; k++) {
				h.add(bin + 0.5);
			}
		}
		return h;
	}

	@Test
	void findPeakBinReturnsTheBinWithTheHighestRawCount() {
		HistoData h = histoWithCounts(1, 2, 10, 3, 1, 0, 0, 5, 2, 1);
		assertEquals(2, HistoFitPrep.findPeakBin(h, 0, 9));
	}

	@Test
	void findPeakBinSmoothedCanPreferAClusterOverATallerIsolatedSpike() {
		// bin2 has the single tallest raw count (7), but bins 5-7 form a
		// smoother cluster (4,5,4) whose 3-bin moving average beats bin2's.
		HistoData h = histoWithCounts(0, 0, 7, 0, 0, 4, 5, 4, 0, 0);

		assertEquals(2, HistoFitPrep.findPeakBin(h, 0, 9), "raw peak is the isolated spike");
		assertEquals(2, HistoFitPrep.findPeakBinSmoothed(h, 0, 9, 0, false),
				"radius 0 smoothing degenerates to the raw peak");
		assertEquals(6, HistoFitPrep.findPeakBinSmoothed(h, 0, 9, 1, false),
				"radius 1 smoothing should prefer the cluster's center bin");
	}

	@Test
	void findPeakBinSmoothedTieBreaksOnHigherRawCount() {
		// bin0 and bin9 both average 2.5 with radius=1 (clipped at the grid
		// edges), but bin9's raw count (5) is higher than bin0's (2), so the
		// documented tie-break rule must prefer bin9.
		HistoData h = histoWithCounts(2, 3, 0, 0, 4, 0, 0, 0, 0, 5);
		assertEquals(9, HistoFitPrep.findPeakBinSmoothed(h, 0, 9, 1, false));
	}

	@Test
	void findPeakBinSmoothedCanIgnoreZeroBins() {
		HistoData h = histoWithCounts(0, 0, 0, 6, 0, 0, 0, 0, 0, 0);
		// With ignoreZeroBins=true and a search range that excludes bin3,
		// there are no eligible candidates, so it falls back to findPeakBin
		// over that same (now all-zero) range.
		assertEquals(0, HistoFitPrep.findPeakBinSmoothed(h, 0, 2, 0, true));
	}

	@Test
	void findPeakBinSmoothedRejectsNegativeRadius() {
		HistoData h = histoWithCounts(1, 2, 3);
		assertThrows(IllegalArgumentException.class,
				() -> HistoFitPrep.findPeakBinSmoothed(h, 0, 2, -1, false));
	}

	@Test
	void convenienceOverloadSearchesTheFullHistogramRange() {
		HistoData h = histoWithCounts(1, 2, 10, 3, 1, 0, 0, 5, 2, 1);
		assertEquals(2, HistoFitPrep.findPeakBinSmoothed(h, 0, false));
	}
}
