package edu.cnu.mdi.splot.pdata;

import java.util.Arrays;
import java.util.Objects;

import edu.cnu.mdi.util.UnicodeUtils;

/**
 * Container for 1D histogram data.
 *
 * <p>Bin edges are stored in {@code grid[]} of length {@code (numBins + 1)}.
 * Counts are stored in {@code counts[]} of length {@code numBins}.</p>
 *
 * <p>Responsibilities</p>
 * <p>This class is responsible <em>only</em> for data storage and bin
 * arithmetic:</p>
 * <ul>
 *   <li>Filling, clearing, and querying bin counts</li>
 *   <li>Underflow and overflow tracking</li>
 *   <li>Basic statistics (mean, sigma, rms)</li>
 *   <li>Display preferences (rms vs sigma in legend, stat error flag)</li>
 * </ul>
 *
 * <p>Out-of-scope (separated into dedicated classes)</p>
 * <ul>
 *   <li>Peak finding and fit-vector preparation &mdash; see {@link HistoFitPrep}</li>
 *   <li>Screen polygon and status-string rendering &mdash; see
 *       {@link HistoDrawingUtils}</li>
 * </ul>
 *
 * @author heddle
 * @see HistoFitPrep
 * @see HistoDrawingUtils
 */
public class HistoData {

	/**
	 * Sentinel returned by {@link #getBin(double)} for values below the histogram
	 * range.
	 */
	static final int UNDERFLOW = -200;

	/**
	 * Sentinel returned by {@link #getBin(double)} for values above the histogram
	 * range.
	 */
	static final int OVERFLOW = -100;

	/**
	 * Cached statistical results: mean in index 0, standard deviation in index 1,
	 * rms in index 2. {@code null} means the cache is invalid and must be
	 * recomputed.
	 */
	private double[] stats;

	/** Histogram name (used as curve name). */
	private String name;

	/** Underflow count (values below the range). */
	private long underCount;

	/** Overflow count (values above the range). */
	private long overCount;

	/** If {@code true}, use rms in the legend; otherwise use sigma. */
	private boolean rmsInHistoLegend = true;

	/** If {@code true}, draw sqrt(n) statistical error bars. */
	private boolean statErrors;

	/** Bin edges (length = numBins + 1). Must be strictly ascending. */
	final double[] grid;

	/** Bin counts (length = numBins). */
	final long[] counts;

	// -----------------------------------------------------------------------
	// Constructors
	// -----------------------------------------------------------------------

	/**
	 * Creates a uniform-bin histogram.
	 *
	 * @param name    histogram name (used as curve label)
	 * @param valMin  range minimum (must be strictly less than {@code valMax})
	 * @param valMax  range maximum
	 * @param numBins number of bins ({@code >= 1})
	 * @throws IllegalArgumentException if {@code numBins < 1} or
	 *                                  {@code valMax <= valMin}
	 */
	public HistoData(String name, double valMin, double valMax, int numBins) {
		this(name, evenBins(valMin, valMax, numBins));
	}

	/**
	 * Creates a histogram with arbitrary (non-uniform) bin spacing.
	 *
	 * @param name histogram name (used as curve label)
	 * @param grid bin-edge array in strictly ascending order (length {@code >= 2})
	 * @throws NullPointerException     if {@code grid} is {@code null}
	 * @throws IllegalArgumentException if {@code grid.length < 2} or the array is
	 *                                  not strictly ascending
	 */
	public HistoData(String name, double[] grid) {
		this.name   = name;
		this.grid   = validateAndCopyGrid(grid);
		this.counts = new long[getNumberBins()];
		clear();
	}

	// -----------------------------------------------------------------------
	// Identity
	// -----------------------------------------------------------------------

	/**
	 * Returns the histogram name.
	 *
	 * @return histogram name (never {@code null} after construction)
	 */
	public String name() {
		return name;
	}

	/**
	 * Sets the histogram name.
	 *
	 * @param name new name
	 */
	public void setName(String name) {
		this.name = name;
	}

	// -----------------------------------------------------------------------
	// Bin geometry
	// -----------------------------------------------------------------------

	/**
	 * Returns the number of bins.
	 *
	 * @return {@code grid.length - 1}
	 */
	public int getNumberBins() {
		return grid.length - 1;
	}

	/**
	 * Returns a defensive copy of the bin-edge grid.
	 *
	 * @return copy of the {@code grid} array (length = {@link #getNumberBins()} + 1)
	 */
	public double[] getGridCopy() {
		return grid.clone();
	}

	/**
	 * Returns the left edge (minimum x) of the histogram range, i.e.
	 * {@code grid[0]}.
	 *
	 * @return minimum x value
	 */
	public double getMinX() {
		return grid[0];
	}

	/**
	 * Returns the right edge (maximum x) of the histogram range, i.e.
	 * {@code grid[grid.length - 1]}.
	 *
	 * @return maximum x value
	 */
	public double getMaxX() {
		return grid[grid.length - 1];
	}

	/**
	 * Returns the minimum y value. For histograms this is always {@code 0}.
	 *
	 * @return {@code 0.0}
	 */
	public double getMinY() {
		return 0.0;
	}

	/**
	 * Returns the maximum y value, i.e. the largest bin count.
	 * Returns at least {@code 1} so that an empty histogram still has a
	 * drawable y range.
	 *
	 * @return max bin count as a {@code double} ({@code >= 1})
	 */
	public double getMaxY() {
		long max = 1L;
		for (long c : counts) {
			max = Math.max(max, c);
		}
		return max;
	}

	/**
	 * Returns the midpoint x value of a bin.
	 *
	 * @param bin zero-based bin index
	 * @return bin midpoint, or {@link Double#NaN} if {@code bin} is out of range
	 */
	public double getBinMidValue(int bin) {
		if (bin < 0 || bin >= getNumberBins()) {
			return Double.NaN;
		}
		return 0.5 * (grid[bin] + grid[bin + 1]);
	}

	/**
	 * Returns the left edge of a bin.
	 *
	 * @param bin zero-based bin index
	 * @return left edge, or {@link Double#NaN} if {@code bin} is out of range
	 */
	public double getBinMinX(int bin) {
		if (bin < 0 || bin >= getNumberBins()) {
			return Double.NaN;
		}
		return grid[bin];
	}

	/**
	 * Returns the right edge of a bin.
	 *
	 * @param bin zero-based bin index
	 * @return right edge, or {@link Double#NaN} if {@code bin} is out of range
	 */
	public double getBinMaxX(int bin) {
		if (bin < 0 || bin >= getNumberBins()) {
			return Double.NaN;
		}
		return grid[bin + 1];
	}

	/**
	 * Returns the width of a bin ({@code right - left}).
	 *
	 * @param bin zero-based bin index
	 * @return bin width, or {@link Double#NaN} if {@code bin} is out of range
	 */
	public double getBinWidth(int bin) {
		if (bin < 0 || bin >= getNumberBins()) {
			return Double.NaN;
		}
		return grid[bin + 1] - grid[bin];
	}

	/**
	 * Returns the zero-based bin index for a given value.
	 *
	 * @param val the value to look up
	 * @return bin index in {@code [0, numBins - 1]}, or {@link #UNDERFLOW} /
	 *         {@link #OVERFLOW} for out-of-range values
	 */
	public int getBin(double val) {
		if (val < getMinX()) {
			return UNDERFLOW;
		}
		if (val > getMaxX()) {
			return OVERFLOW;
		}

		int index = Arrays.binarySearch(grid, val);
		if (index < 0) {
			index = -(index + 1); // insertion point
		}
		int bin = index - 1;
		return Math.max(0, Math.min(grid.length - 2, bin));
	}

	// -----------------------------------------------------------------------
	// Filling and clearing
	// -----------------------------------------------------------------------

	/** Resets all counts and invalidates the statistics cache. */
	public void clear() {
		underCount = 0L;
		overCount  = 0L;
		stats      = null;
		Arrays.fill(counts, 0L);
	}

	/**
	 * Adds a single value to the histogram.
	 *
	 * @param value the value to fill
	 */
	public void add(double value) {
		stats = null;
		int bin = getBin(value);
		if (bin == UNDERFLOW) {
			underCount++;
		} else if (bin == OVERFLOW) {
			overCount++;
		} else {
			counts[bin]++;
		}
	}

	/**
	 * Adds many values to the histogram in bulk.
	 * <p>
	 * Equivalent to calling {@link #add(double)} for each element, but
	 * invalidates the statistics cache only once.
	 * </p>
	 *
	 * @param values array of sample values (non-null)
	 * @throws NullPointerException if {@code values} is {@code null}
	 */
	public void addAll(double[] values) {
		Objects.requireNonNull(values, "values");
		stats = null;

		for (double v : values) {
			int bin = getBin(v);
			if (bin == UNDERFLOW) {
				underCount++;
			} else if (bin == OVERFLOW) {
				overCount++;
			} else {
				counts[bin]++;
			}
		}
	}

	/**
	 * Sets the count for the bin that contains {@code val}.
	 * <p>
	 * Values outside the range are added to the under/overflow accumulators.
	 * </p>
	 *
	 * @param val   value identifying the target bin
	 * @param count count to assign
	 */
	public void setCount(double val, int count) {
		stats = null;
		int bin = getBin(val);
		if (bin == UNDERFLOW) {
			underCount += count;
		} else if (bin == OVERFLOW) {
			overCount += count;
		} else {
			counts[bin] = count;
		}
	}

	/**
	 * Replaces the histogram counts from an external array.
	 * <p>
	 * Intended for deserialization / persistence only; not for normal filling.
	 * </p>
	 *
	 * @param countsSrc  source counts array (length must equal
	 *                   {@link #getNumberBins()})
	 * @param underCount underflow count to restore
	 * @param overCount  overflow count to restore
	 * @throws IllegalArgumentException if {@code countsSrc} length mismatches
	 */
	public synchronized void setCountsForDeserialization(long[] countsSrc,
	                                                     long underCount,
	                                                     long overCount) {
		if (countsSrc == null || countsSrc.length != counts.length) {
			throw new IllegalArgumentException(
					"countsSrc length mismatch: expected " + counts.length);
		}
		System.arraycopy(countsSrc, 0, counts, 0, counts.length);
		this.underCount = underCount;
		this.overCount  = overCount;
		this.stats      = null;
	}

	// -----------------------------------------------------------------------
	// Count queries
	// -----------------------------------------------------------------------

	/**
	 * Returns the live counts array.
	 * <p>
	 * The returned array is the internal storage &mdash; do not modify it.
	 * For a safe copy use {@link #getCountsCopy()}.
	 * </p>
	 *
	 * @return live counts array (length = {@link #getNumberBins()})
	 */
	public long[] getCounts() {
		return counts;
	}

	/**
	 * Returns a defensive copy of the counts array.
	 *
	 * @return copy of the counts array
	 */
	public long[] getCountsCopy() {
		return counts.clone();
	}

	/**
	 * Returns the count for a specific bin.
	 *
	 * @param bin zero-based bin index
	 * @return count, or {@code 0} if {@code bin} is out of range
	 */
	public long getCount(int bin) {
		if (bin < 0 || bin >= counts.length) {
			return 0L;
		}
		return counts[bin];
	}

	/**
	 * Returns the total number of in-range entries (excludes underflow/overflow).
	 *
	 * @return sum of all bin counts
	 */
	public long getGoodCount() {
		long sum = 0L;
		for (long c : counts) {
			sum += c;
		}
		return sum;
	}

	/**
	 * Returns the total entry count including underflow and overflow.
	 *
	 * @return {@code getGoodCount() + getUnderCount() + getOverCount()}
	 */
	public long getTotalCount() {
		return getGoodCount() + getUnderCount() + getOverCount();
	}

	/**
	 * Returns the number of values that fell below the histogram range.
	 *
	 * @return underflow count
	 */
	public long getUnderCount() {
		return underCount;
	}

	/**
	 * Returns the number of values that fell above the histogram range.
	 *
	 * @return overflow count
	 */
	public long getOverCount() {
		return overCount;
	}

	// -----------------------------------------------------------------------
	// Statistics
	// -----------------------------------------------------------------------

	/**
	 * Computes (and caches) the mean, standard deviation, and RMS of the
	 * histogram, weighted by bin counts.
	 *
	 * @return array {@code [mean, stdDev, rms]}, all {@link Double#NaN} if the
	 *         histogram is empty
	 */
	public double[] getBasicStatistics() {
		if (stats != null) {
			return stats;
		}

		stats = new double[] { Double.NaN, Double.NaN, Double.NaN };

		int  nbin = getNumberBins();
		long tot  = getGoodCount();
		if (nbin > 0 && tot > 0) {
			double sum   = 0.0;
			double sumsq = 0.0;

			for (int bin = 0; bin < nbin; bin++) {
				double x = getBinMidValue(bin);
				double w = counts[bin];
				sum   += w * x;
				sumsq += w * x * x;
			}

			double mean  = sum / tot;
			double avgSq = sumsq / tot;

			stats[0] = mean;
			stats[1] = Math.sqrt(Math.max(0.0, avgSq - mean * mean));
			stats[2] = Math.sqrt(Math.max(0.0, avgSq));
		}

		return stats;
	}

	/**
	 * Returns a single-line status string showing mean and either rms or sigma,
	 * plus under/overflow counts. The choice between rms and sigma is controlled
	 * by {@link #useRmsInHistoLegend()}.
	 *
	 * @return formatted statistics string
	 */
	public String statStr() {
		double[] res = getBasicStatistics();
		if (rmsInHistoLegend) {
			return String.format(
					UnicodeUtils.SMALL_MU + ": %-4.2g rms: %-4.2g under: %d over: %d",
					res[0], res[2], underCount, overCount);
		}
		return String.format(
				UnicodeUtils.SMALL_MU + ": %-4.2g "
						+ UnicodeUtils.SMALL_SIGMA + ": %-4.2g under: %d over: %d",
				res[0], res[1], underCount, overCount);
	}

	/**
	 * Returns a string describing the bin(s) with the maximum count (1-based
	 * indices).
	 *
	 * @return descriptive string, or {@code ""} if the histogram is empty
	 */
	public String maxBinString() {
		long maxCount = -1;
		for (long lv : counts) {
			maxCount = Math.max(maxCount, lv);
		}
		if (maxCount < 1) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Max count: ").append(maxCount).append(" in 1-based bin(s):");
		for (int bin = 0; bin < getNumberBins(); bin++) {
			if (counts[bin] == maxCount) {
				sb.append(' ').append(bin + 1);
			}
		}
		return sb.toString();
	}

	// -----------------------------------------------------------------------
	// Display preferences
	// -----------------------------------------------------------------------

	/**
	 * Sets whether rms (rather than sigma) is shown in histogram legends.
	 *
	 * @param useRMS {@code true} to show rms; {@code false} to show sigma
	 */
	public void setRmsInHistoLegend(boolean useRMS) {
		this.rmsInHistoLegend = useRMS;
	}

	/**
	 * Returns whether rms (rather than sigma) is shown in histogram legends.
	 *
	 * @return {@code true} if rms is shown
	 */
	public boolean useRmsInHistoLegend() {
		return rmsInHistoLegend;
	}

	/**
	 * Sets whether sqrt(n) statistical error bars are drawn on histogram bins.
	 *
	 * @param statErr {@code true} to draw error bars
	 */
	public void setDrawStatisticalErrors(boolean statErr) {
		this.statErrors = statErr;
	}

	/**
	 * Returns whether sqrt(n) statistical error bars should be drawn.
	 *
	 * @return {@code true} if statistical errors are drawn
	 */
	public boolean drawStatisticalErrors() {
		return statErrors;
	}

	// -----------------------------------------------------------------------
	// Convenience delegation to HistoFitPrep
	// -----------------------------------------------------------------------

	/**
	 * Convenience delegation: prepares fit vectors over the full x-range.
	 * <p>
	 * Equivalent to {@link HistoFitPrep#prepareForFit(HistoData, boolean)}.
	 * </p>
	 *
	 * @param includeZeroBins if {@code true}, bins with zero counts are included
	 * @return fit vectors ({@code x}, {@code y}, {@code weights = null})
	 * @see HistoFitPrep
	 */
	public FitVectors prepareForFit(boolean includeZeroBins) {
		return HistoFitPrep.prepareForFit(this, includeZeroBins);
	}

	/**
	 * Convenience delegation: prepares fit vectors over a data-space x-range.
	 *
	 * @param includeZeroBins if {@code true}, bins with zero counts are included
	 * @param xmin            minimum x to include (data space)
	 * @param xmax            maximum x to include (data space)
	 * @param poissonWeights  if {@code true}, include Poisson (1/count) weights
	 * @return fit vectors
	 * @see HistoFitPrep#prepareForFit(HistoData, boolean, double, double, boolean)
	 */
	public FitVectors prepareForFit(boolean includeZeroBins,
	                                double xmin, double xmax,
	                                boolean poissonWeights) {
		return HistoFitPrep.prepareForFit(this, includeZeroBins, xmin, xmax, poissonWeights);
	}

	/**
	 * Convenience delegation: prepares fit vectors over an inclusive bin-index
	 * range.
	 *
	 * @param includeZeroBins if {@code true}, bins with zero counts are included
	 * @param bin0            inclusive start bin (clamped to valid range)
	 * @param bin1            inclusive end bin (clamped to valid range)
	 * @param poissonWeights  if {@code true}, include Poisson weights
	 * @return fit vectors
	 * @see HistoFitPrep#prepareForFit(HistoData, boolean, int, int, boolean)
	 */
	public FitVectors prepareForFit(boolean includeZeroBins,
	                                int bin0, int bin1,
	                                boolean poissonWeights) {
		return HistoFitPrep.prepareForFit(this, includeZeroBins, bin0, bin1, poissonWeights);
	}

	/**
	 * Convenience delegation: prepares fit vectors in a symmetric window around a
	 * given peak bin.
	 *
	 * @param includeZeroBins if {@code true}, bins with zero counts are included
	 * @param peakBin         center bin of the window (0-based)
	 * @param halfWindowBins  half-width of the window in bins ({@code >= 0})
	 * @param poissonWeights  if {@code true}, include Poisson weights
	 * @return fit vectors
	 * @see HistoFitPrep#prepareForFitAroundPeak(HistoData, boolean, int, int, boolean)
	 */
	public FitVectors prepareForFitAroundPeak(boolean includeZeroBins,
	                                          int peakBin,
	                                          int halfWindowBins,
	                                          boolean poissonWeights) {
		return HistoFitPrep.prepareForFitAroundPeak(
				this, includeZeroBins, peakBin, halfWindowBins, poissonWeights);
	}

	/**
	 * Convenience delegation: finds the raw peak bin (maximum count).
	 *
	 * @return peak bin index (0-based), or {@code -1} if there are no bins
	 * @see HistoFitPrep#findPeakBin(HistoData)
	 */
	public int findPeakBin() {
		return HistoFitPrep.findPeakBin(this);
	}

	/**
	 * Convenience delegation: finds the raw peak bin in a restricted range.
	 *
	 * @param bin0 inclusive start bin (0-based)
	 * @param bin1 inclusive end bin (0-based)
	 * @return peak bin index (0-based), or {@code -1} if there are no bins
	 * @see HistoFitPrep#findPeakBin(HistoData, int, int)
	 */
	public int findPeakBin(int bin0, int bin1) {
		return HistoFitPrep.findPeakBin(this, bin0, bin1);
	}

	/**
	 * Convenience delegation: finds the best peak using smoothing and robust
	 * plateau handling, then prepares fit vectors in a guarded window around it.
	 *
	 * @param includeZeroBins if {@code true}, include zero-count bins in fit
	 *                        vectors
	 * @param halfWindowBins  requested half-window size ({@code >= 0})
	 * @param smoothRadius    smoothing radius for peak detection ({@code >= 0})
	 * @param ignoreZeroBins  if {@code true}, zero-count bins are skipped as peak
	 *                        candidates
	 * @param poissonWeights  if {@code true}, include Poisson weights
	 * @param minPoints       minimum points required after filtering ({@code >= 1})
	 * @return fit window data including metadata about the window that was used
	 * @see HistoFitPrep#prepareForFitAroundBestPeakGuarded(HistoData, boolean, int, int, boolean, boolean, int)
	 */
	public HistoFitPrep.FitWindowData prepareForFitAroundBestPeakGuarded(
			boolean includeZeroBins,
			int halfWindowBins,
			int smoothRadius,
			boolean ignoreZeroBins,
			boolean poissonWeights,
			int minPoints) {
		return HistoFitPrep.prepareForFitAroundBestPeakGuarded(
				this, includeZeroBins, halfWindowBins,
				smoothRadius, ignoreZeroBins, poissonWeights, minPoints);
	}

	// -----------------------------------------------------------------------
	// Object overrides
	// -----------------------------------------------------------------------

	@Override
	public String toString() {
		String nm   = (name == null) ? "" : name.trim();
		int    nbin = getNumberBins();
		long   good = getGoodCount();
		long   total = getTotalCount();

		double xmin = (grid.length > 0) ? getMinX() : Double.NaN;
		double xmax = (grid.length > 0) ? getMaxX() : Double.NaN;

		double[] st    = getBasicStatistics();
		double   mean  = (st.length > 0) ? st[0] : Double.NaN;
		double   sigma = (st.length > 1) ? st[1] : Double.NaN;
		double   rms   = (st.length > 2) ? st[2] : Double.NaN;

		int  peak      = (nbin > 0) ? findPeakBin() : -1;
		long peakCount = (peak >= 0) ? getCount(peak) : 0L;
		double peakX   = (peak >= 0) ? getBinMidValue(peak) : Double.NaN;

		StringBuilder sb = new StringBuilder(160);
		sb.append("HistoData");
		if (!nm.isEmpty()) {
			sb.append('[').append(nm).append(']');
		}
		sb.append("{bins=").append(nbin)
		  .append(", x=[").append(xmin).append(", ").append(xmax).append(']')
		  .append(", good=").append(good)
		  .append(", under=").append(underCount)
		  .append(", over=").append(overCount)
		  .append(", total=").append(total);

		if (!Double.isNaN(mean))  { sb.append(", mean=").append(mean);   }
		if (!Double.isNaN(sigma)) { sb.append(", sigma=").append(sigma); }
		if (!Double.isNaN(rms))   { sb.append(", rms=").append(rms);     }

		if (peak >= 0) {
			sb.append(", peakBin=").append(peak)
			  .append(", peakX=").append(peakX)
			  .append(", peakCount=").append(peakCount);
		}
		sb.append('}');
		return sb.toString();
	}

	// -----------------------------------------------------------------------
	// Private helpers
	// -----------------------------------------------------------------------

	/**
	 * Builds a uniformly-spaced bin-edge array.
	 *
	 * @param vmin    range minimum
	 * @param vmax    range maximum (must be {@code > vmin})
	 * @param numBins number of bins ({@code >= 1})
	 * @return bin-edge array of length {@code numBins + 1}
	 */
	private static double[] evenBins(double vmin, double vmax, int numBins) {
		if (numBins <= 0) {
			throw new IllegalArgumentException("numBins must be >= 1");
		}
		if (!(vmax > vmin)) {
			throw new IllegalArgumentException("valMax must be > valMin");
		}

		double[] g   = new double[numBins + 1];
		double   del = (vmax - vmin) / numBins;
		g[0] = vmin;
		for (int i = 1; i < numBins; i++) {
			g[i] = vmin + i * del;
		}
		g[numBins] = vmax;
		return g;
	}

	/**
	 * Validates and returns a defensive copy of a bin-edge array.
	 *
	 * @param grid source grid (non-null, length {@code >= 2}, strictly ascending)
	 * @return validated copy
	 * @throws NullPointerException     if {@code grid} is {@code null}
	 * @throws IllegalArgumentException if the array is too short or not ascending
	 */
	private static double[] validateAndCopyGrid(double[] grid) {
		Objects.requireNonNull(grid, "grid");
		if (grid.length < 2) {
			throw new IllegalArgumentException("grid must have length >= 2");
		}

		double[] g = grid.clone();
		for (int i = 1; i < g.length; i++) {
			if (!(g[i] > g[i - 1])) {
				throw new IllegalArgumentException(
						"grid must be strictly ascending (duplicate or decreasing at index " + i + ")");
			}
		}
		return g;
	}
}