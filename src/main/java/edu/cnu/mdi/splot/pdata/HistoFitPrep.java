package edu.cnu.mdi.splot.pdata;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless utility class for histogram fit preparation.
 *
 * <p>This class extracts the peak-finding and fit-vector building logic that
 * was previously embedded in {@link HistoData}, keeping {@code HistoData}
 * focused on data storage.</p>
 *
 * <h3>Fit-vector layout</h3>
 * <p>All {@code prepare*} methods return a {@link FitVectors} (or the richer
 * {@link FitWindowData} subtype) whose arrays follow this convention:</p>
 * <ul>
 *   <li>{@code x[i]} &mdash; bin center (mid-point of bin {@code i})</li>
 *   <li>{@code y[i]} &mdash; bin count as a {@code double}</li>
 *   <li>{@code w[i]} &mdash; optional Poisson weight {@code 1/count} for
 *       {@code count > 0}, {@code 1.0} for zero bins (when weights are
 *       requested and the bin is included)</li>
 * </ul>
 *
 * <h3>Peak-finding strategies</h3>
 * <ul>
 *   <li>{@link #findPeakBin(HistoData)} &mdash; raw maximum (first occurrence
 *       on ties)</li>
 *   <li>{@link #findPeakBin(HistoData, int, int)} &mdash; raw maximum in a
 *       restricted bin range</li>
 *   <li>{@link #findPeakBinSmoothed} &mdash; flat moving-average smoothing</li>
 *   <li>{@link #findPeakBinTriangularSmoothed} &mdash; triangular-kernel
 *       smoothing</li>
 *   <li>{@link #findPeakBinBest} &mdash; triangular smoothing plus
 *       plateau-handling (preferred general-purpose strategy)</li>
 * </ul>
 *
 * @author heddle (original), refactored for extraction
 * @see HistoData
 * @see FitVectors
 */
public final class HistoFitPrep {

	/** Not instantiable. */
	private HistoFitPrep() {}

	// -----------------------------------------------------------------------
	// Public result type
	// -----------------------------------------------------------------------

	/**
	 * A {@link FitVectors} that also records which peak bin and window were
	 * actually used.
	 *
	 * <p>Returned by the guarded best-peak methods so callers can log or display
	 * the window that was chosen.</p>
	 */
	public static final class FitWindowData extends FitVectors {

		/** The (0-based) peak bin that was selected. {@code -1} if empty. */
		public final int peakBin;

		/** Inclusive start bin of the window that was used. */
		public final int bin0;

		/** Inclusive end bin of the window that was used. */
		public final int bin1;

		/** Half-window size in bins that was actually applied (may be
		 *  smaller than requested if the peak was near an edge). */
		public final int halfWindowBinsUsed;

		/**
		 * Creates a {@code FitWindowData}.
		 *
		 * @param x                 bin centers
		 * @param y                 bin counts
		 * @param weights           Poisson weights, or {@code null}
		 * @param peakBin           selected peak bin (0-based; {@code -1} if none)
		 * @param bin0              inclusive start bin
		 * @param bin1              inclusive end bin
		 * @param halfWindowBinsUsed half-window actually used
		 */
		public FitWindowData(double[] x, double[] y, double[] weights,
		                     int peakBin, int bin0, int bin1,
		                     int halfWindowBinsUsed) {
			super(x, y, weights);
			this.peakBin            = peakBin;
			this.bin0               = bin0;
			this.bin1               = bin1;
			this.halfWindowBinsUsed = halfWindowBinsUsed;
		}
	}

	// -----------------------------------------------------------------------
	// Fit-vector preparation
	// -----------------------------------------------------------------------

	/**
	 * Prepares fit vectors over the full x-range using unit weights.
	 *
	 * @param h               source histogram (non-null)
	 * @param includeZeroBins if {@code true}, bins with zero count are included
	 * @return fit vectors ({@code weights = null})
	 */
	public static FitVectors prepareForFit(HistoData h, boolean includeZeroBins) {
		return prepareForFit(h, includeZeroBins, h.getMinX(), h.getMaxX(), false);
	}

	/**
	 * Prepares fit vectors over a data-space x-range.
	 *
	 * @param h               source histogram (non-null)
	 * @param includeZeroBins if {@code true}, include bins with zero count
	 * @param xmin            minimum x to include (data space, inclusive)
	 * @param xmax            maximum x to include (data space, inclusive)
	 * @param poissonWeights  if {@code true}, include Poisson (1/count) weights
	 * @return fit vectors; {@code weights} array is {@code null} unless
	 *         {@code poissonWeights} is {@code true}
	 */
	public static FitVectors prepareForFit(HistoData h,
	                                       boolean includeZeroBins,
	                                       double xmin, double xmax,
	                                       boolean poissonWeights) {
		List<Double> xs = new ArrayList<>();
		List<Double> ys = new ArrayList<>();
		List<Double> ws = poissonWeights ? new ArrayList<>() : null;

		int nbin = h.getNumberBins();
		for (int bin = 0; bin < nbin; bin++) {
			long c = h.counts[bin];
			if (!includeZeroBins && c == 0L) {
				continue;
			}

			double xc = h.getBinMidValue(bin);
			if (xc < xmin || xc > xmax) {
				continue;
			}

			xs.add(xc);
			ys.add((double) c);
			if (poissonWeights) {
				ws.add(poissonWeight(c));
			}
		}

		return new FitVectors(
				toDoubleArray(xs),
				toDoubleArray(ys),
				ws == null ? null : toDoubleArray(ws));
	}

	/**
	 * Prepares fit vectors over an inclusive bin-index range.
	 *
	 * @param h               source histogram (non-null)
	 * @param includeZeroBins if {@code true}, include bins with zero count
	 * @param bin0            inclusive start bin (0-based; clamped to valid range)
	 * @param bin1            inclusive end bin (0-based; clamped to valid range)
	 * @param poissonWeights  if {@code true}, include Poisson weights
	 * @return fit vectors
	 */
	public static FitVectors prepareForFit(HistoData h,
	                                       boolean includeZeroBins,
	                                       int bin0, int bin1,
	                                       boolean poissonWeights) {
		int nbin = h.getNumberBins();
		if (nbin <= 0) {
			return new FitVectors(
					new double[0], new double[0],
					poissonWeights ? new double[0] : null);
		}

		int b0 = clamp(bin0, nbin);
		int b1 = clamp(bin1, nbin);
		if (b0 > b1) {
			int tmp = b0; b0 = b1; b1 = tmp;
		}

		// Pre-count so we can allocate precisely.
		int keep = 0;
		for (int bin = b0; bin <= b1; bin++) {
			if (includeZeroBins || h.counts[bin] != 0L) {
				keep++;
			}
		}

		double[] xArr = new double[keep];
		double[] yArr = new double[keep];
		double[] wArr = poissonWeights ? new double[keep] : null;

		int j = 0;
		for (int bin = b0; bin <= b1; bin++) {
			long c = h.counts[bin];
			if (!includeZeroBins && c == 0L) {
				continue;
			}

			xArr[j] = h.getBinMidValue(bin);
			yArr[j] = c;
			if (poissonWeights) {
				wArr[j] = poissonWeight(c);
			}
			j++;
		}

		return new FitVectors(xArr, yArr, wArr);
	}

	/**
	 * Prepares fit vectors in a symmetric window around a given peak bin.
	 *
	 * @param h               source histogram (non-null)
	 * @param includeZeroBins if {@code true}, include bins with zero count
	 * @param peakBin         center bin of the window (0-based)
	 * @param halfWindowBins  half-width of the window in bins ({@code >= 0})
	 * @param poissonWeights  if {@code true}, include Poisson weights
	 * @return fit vectors
	 * @throws IllegalArgumentException if {@code halfWindowBins < 0}
	 */
	public static FitVectors prepareForFitAroundPeak(HistoData h,
	                                                 boolean includeZeroBins,
	                                                 int peakBin,
	                                                 int halfWindowBins,
	                                                 boolean poissonWeights) {
		if (halfWindowBins < 0) {
			throw new IllegalArgumentException("halfWindowBins must be >= 0");
		}
		return prepareForFit(h, includeZeroBins,
				peakBin - halfWindowBins, peakBin + halfWindowBins,
				poissonWeights);
	}

	// -----------------------------------------------------------------------
	// Peak finders
	// -----------------------------------------------------------------------

	/**
	 * Returns the zero-based index of the bin with the highest count (raw
	 * maximum). On ties, the first occurrence is returned.
	 *
	 * @param h source histogram (non-null)
	 * @return peak bin index, or {@code -1} if the histogram has no bins
	 */
	public static int findPeakBin(HistoData h) {
		int nbin = h.getNumberBins();
		if (nbin <= 0) {
			return -1;
		}

		int  peak = 0;
		long best = h.counts[0];
		for (int bin = 1; bin < nbin; bin++) {
			if (h.counts[bin] > best) {
				best = h.counts[bin];
				peak = bin;
			}
		}
		return peak;
	}

	/**
	 * Returns the zero-based index of the bin with the highest count within an
	 * inclusive bin range. On ties, the first occurrence is returned.
	 *
	 * @param h    source histogram (non-null)
	 * @param bin0 inclusive start bin (0-based; clamped)
	 * @param bin1 inclusive end bin (0-based; clamped)
	 * @return peak bin index in the range, or {@code -1} if there are no bins
	 */
	public static int findPeakBin(HistoData h, int bin0, int bin1) {
		int nbin = h.getNumberBins();
		if (nbin <= 0) {
			return -1;
		}

		int b0 = clamp(bin0, nbin);
		int b1 = clamp(bin1, nbin);
		if (b0 > b1) { int tmp = b0; b0 = b1; b1 = tmp; }

		int  peak = b0;
		long best = h.counts[b0];
		for (int bin = b0 + 1; bin <= b1; bin++) {
			if (h.counts[bin] > best) {
				best = h.counts[bin];
				peak = bin;
			}
		}
		return peak;
	}

	/**
	 * Finds the peak bin using a flat moving-average smoothing kernel.
	 *
	 * <p>For each candidate bin {@code i} in {@code [bin0, bin1]}, the score is
	 * the average count over {@code [i-radius, i+radius]}. The bin with the
	 * highest score wins; on a tie the bin with the higher raw count is
	 * preferred. Falls back to {@link #findPeakBin(HistoData, int, int)} if no
	 * candidate is found.</p>
	 *
	 * @param h              source histogram (non-null)
	 * @param bin0           inclusive start bin for the search (0-based; clamped)
	 * @param bin1           inclusive end bin for the search (0-based; clamped)
	 * @param radius         smoothing half-width in bins ({@code >= 0})
	 * @param ignoreZeroBins if {@code true}, zero-count bins are skipped as
	 *                       candidates
	 * @return peak bin index, or {@code -1} if there are no bins
	 * @throws IllegalArgumentException if {@code radius < 0}
	 */
	public static int findPeakBinSmoothed(HistoData h,
	                                      int bin0, int bin1,
	                                      int radius,
	                                      boolean ignoreZeroBins) {
		int nbin = h.getNumberBins();
		if (nbin <= 0) {
			return -1;
		}
		if (radius < 0) {
			throw new IllegalArgumentException("radius must be >= 0");
		}

		int b0 = clamp(bin0, nbin);
		int b1 = clamp(bin1, nbin);
		if (b0 > b1) { int tmp = b0; b0 = b1; b1 = tmp; }

		int    bestBin   = -1;
		double bestScore = Double.NEGATIVE_INFINITY;

		for (int i = b0; i <= b1; i++) {
			if (ignoreZeroBins && h.counts[i] == 0L) {
				continue;
			}

			int lo = Math.max(0,      i - radius);
			int hi = Math.min(nbin-1, i + radius);

			long sum = 0L;
			int  cnt = 0;
			for (int j = lo; j <= hi; j++) { sum += h.counts[j]; cnt++; }

			double avg = (cnt > 0) ? ((double) sum / cnt) : Double.NEGATIVE_INFINITY;

			if (avg > bestScore
					|| (avg == bestScore && bestBin >= 0 && h.counts[i] > h.counts[bestBin])) {
				bestScore = avg;
				bestBin   = i;
			}
		}

		return (bestBin >= 0) ? bestBin : findPeakBin(h, b0, b1);
	}

	/**
	 * Convenience overload of {@link #findPeakBinSmoothed} over the full
	 * histogram range.
	 *
	 * @param h              source histogram (non-null)
	 * @param radius         smoothing half-width in bins ({@code >= 0})
	 * @param ignoreZeroBins if {@code true}, zero-count bins are skipped
	 * @return peak bin index, or {@code -1} if there are no bins
	 */
	public static int findPeakBinSmoothed(HistoData h,
	                                      int radius,
	                                      boolean ignoreZeroBins) {
		return findPeakBinSmoothed(h, 0, h.getNumberBins() - 1, radius, ignoreZeroBins);
	}

	/**
	 * Finds the peak bin using a triangular (linear-decay) smoothing kernel.
	 *
	 * <p>Bins closer to the candidate bin {@code i} contribute more to the
	 * score. Weight of bin {@code j} is {@code (radius + 1 - |j - i|)}. On a
	 * tie, prefer the bin with the higher raw count; if still tied, prefer the
	 * bin closer to the centre of the search range.</p>
	 *
	 * @param h              source histogram (non-null)
	 * @param bin0           inclusive start bin (0-based; clamped)
	 * @param bin1           inclusive end bin (0-based; clamped)
	 * @param radius         smoothing half-width ({@code >= 0})
	 * @param ignoreZeroBins if {@code true}, zero-count bins are skipped
	 * @return peak bin index, or {@code -1} if there are no bins
	 * @throws IllegalArgumentException if {@code radius < 0}
	 */
	public static int findPeakBinTriangularSmoothed(HistoData h,
	                                                int bin0, int bin1,
	                                                int radius,
	                                                boolean ignoreZeroBins) {
		int nbin = h.getNumberBins();
		if (nbin <= 0) {
			return -1;
		}
		if (radius < 0) {
			throw new IllegalArgumentException("radius must be >= 0");
		}

		int b0 = clamp(bin0, nbin);
		int b1 = clamp(bin1, nbin);
		if (b0 > b1) { int tmp = b0; b0 = b1; b1 = tmp; }

		int    bestBin   = -1;
		double bestScore = Double.NEGATIVE_INFINITY;
		int    mid       = (b0 + b1) / 2;

		for (int i = b0; i <= b1; i++) {
			if (ignoreZeroBins && h.counts[i] == 0L) {
				continue;
			}

			double score = triangularScore(h, i, radius, nbin);

			if (score > bestScore) {
				bestScore = score;
				bestBin   = i;
			} else if (score == bestScore && bestBin >= 0) {
				long ci = h.counts[i];
				long cb = h.counts[bestBin];
				if (ci > cb
						|| (ci == cb && Math.abs(i - mid) < Math.abs(bestBin - mid))) {
					bestBin = i;
				}
			}
		}

		return (bestBin >= 0) ? bestBin : findPeakBin(h, b0, b1);
	}

	/**
	 * Convenience overload of {@link #findPeakBinTriangularSmoothed} over the
	 * full histogram range.
	 *
	 * @param h              source histogram (non-null)
	 * @param radius         smoothing half-width ({@code >= 0})
	 * @param ignoreZeroBins if {@code true}, zero-count bins are skipped
	 * @return peak bin index, or {@code -1} if there are no bins
	 */
	public static int findPeakBinTriangularSmoothed(HistoData h,
	                                                int radius,
	                                                boolean ignoreZeroBins) {
		return findPeakBinTriangularSmoothed(
				h, 0, h.getNumberBins() - 1, radius, ignoreZeroBins);
	}

	/**
	 * Finds the best peak using triangular smoothing plus robust plateau
	 * handling.
	 *
	 * <p>When multiple adjacent bins share the top smoothed score (a plateau),
	 * this method picks the best raw-count bin within that plateau, resolving
	 * further ties by proximity to the plateau's own midpoint and then to the
	 * search-range midpoint. This strategy is preferred over pure
	 * triangular-smoothed search for most fitting use cases.</p>
	 *
	 * @param h              source histogram (non-null)
	 * @param bin0           inclusive start bin (0-based; clamped)
	 * @param bin1           inclusive end bin (0-based; clamped)
	 * @param smoothRadius   smoothing half-width ({@code >= 0}); {@code 0}
	 *                       degenerates to raw-peak-with-plateau
	 * @param ignoreZeroBins if {@code true}, zero-count bins are skipped as
	 *                       candidates
	 * @return peak bin index, or {@code -1} if there are no bins
	 * @throws IllegalArgumentException if {@code smoothRadius < 0}
	 */
	public static int findPeakBinBest(HistoData h,
	                                  int bin0, int bin1,
	                                  int smoothRadius,
	                                  boolean ignoreZeroBins) {
		int nbin = h.getNumberBins();
		if (nbin <= 0) {
			return -1;
		}
		if (smoothRadius < 0) {
			throw new IllegalArgumentException("smoothRadius must be >= 0");
		}

		int b0 = clamp(bin0, nbin);
		int b1 = clamp(bin1, nbin);
		if (b0 > b1) { int tmp = b0; b0 = b1; b1 = tmp; }

		if (smoothRadius == 0) {
			return rawPeakWithPlateau(h, b0, b1, ignoreZeroBins);
		}

		double bestScore = Double.NEGATIVE_INFINITY;
		int    p0 = -1, p1 = -1;

		for (int i = b0; i <= b1; i++) {
			if (ignoreZeroBins && h.counts[i] == 0L) {
				continue;
			}

			double score = triangularScore(h, i, smoothRadius, nbin);

			if (score > bestScore) {
				bestScore = score;
				p0 = i;
				p1 = i;
			} else if (score == bestScore && p0 >= 0) {
				if (i == p1 + 1) {
					p1 = i; // extend plateau
				} else {
					// non-contiguous tie: keep the better representative
					int oldChoice = chooseBestInPlateau(h, p0, p1, b0, b1);
					if (h.counts[i] > h.counts[oldChoice]
							|| (h.counts[i] == h.counts[oldChoice]
							    && Math.abs(i - (b0+b1)/2) < Math.abs(oldChoice - (b0+b1)/2))) {
						p0 = i;
						p1 = i;
					}
				}
			}
		}

		if (p0 < 0) {
			return findPeakBin(h, b0, b1);
		}
		if (p0 == p1) {
			return p0;
		}
		return chooseBestInPlateau(h, p0, p1, b0, b1);
	}

	/**
	 * Convenience overload of {@link #findPeakBinBest} over the full histogram
	 * range.
	 *
	 * @param h              source histogram (non-null)
	 * @param smoothRadius   smoothing half-width ({@code >= 0})
	 * @param ignoreZeroBins if {@code true}, zero-count bins are skipped
	 * @return peak bin index, or {@code -1} if there are no bins
	 */
	public static int findPeakBinBest(HistoData h,
	                                  int smoothRadius,
	                                  boolean ignoreZeroBins) {
		return findPeakBinBest(h, 0, h.getNumberBins() - 1, smoothRadius, ignoreZeroBins);
	}

	// -----------------------------------------------------------------------
	// Guarded best-peak fit-prep
	// -----------------------------------------------------------------------

	/**
	 * Prepares fit vectors around the best peak with edge-safety and
	 * minimum-points guarantees.
	 *
	 * <p>The strategy:</p>
	 * <ol>
	 *   <li>Prefer peaks that can support the full requested window without
	 *       clipping (search in the "inner" sub-range first).</li>
	 *   <li>Auto-shrink the half-window if the peak is near an edge.</li>
	 *   <li>If the result has fewer than {@code minPoints} entries, expand to
	 *       the maximum available window.</li>
	 *   <li>Last resort: if the caller excluded zero bins, retry with zeros
	 *       included.</li>
	 *   <li>If still insufficient, return an empty {@link FitWindowData} with
	 *       metadata preserved.</li>
	 * </ol>
	 *
	 * @param h               source histogram (non-null)
	 * @param includeZeroBins if {@code true}, include zero-count bins normally
	 * @param halfWindowBins  requested half-window in bins ({@code >= 0})
	 * @param smoothRadius    smoothing radius for peak detection ({@code >= 0})
	 * @param ignoreZeroBins  if {@code true}, zero-count bins are skipped as peak
	 *                        candidates
	 * @param poissonWeights  if {@code true}, include Poisson weights
	 * @param minPoints       minimum number of points required ({@code >= 1})
	 * @return {@link FitWindowData} including metadata about the selected window
	 * @throws IllegalArgumentException if {@code halfWindowBins < 0} or
	 *                                  {@code minPoints < 1}
	 */
	public static FitWindowData prepareForFitAroundBestPeakGuarded(
			HistoData h,
			boolean includeZeroBins,
			int halfWindowBins,
			int smoothRadius,
			boolean ignoreZeroBins,
			boolean poissonWeights,
			int minPoints) {
		return prepareForFitAroundBestPeakGuarded(
				h, includeZeroBins,
				0, h.getNumberBins() - 1,
				halfWindowBins, smoothRadius,
				ignoreZeroBins, poissonWeights, minPoints);
	}

	/**
	 * Prepares fit vectors around the best peak within a restricted search range,
	 * with edge-safety and minimum-points guarantees.
	 *
	 * @param h               source histogram (non-null)
	 * @param includeZeroBins if {@code true}, include zero-count bins normally
	 * @param searchBin0      inclusive start bin to search for a peak (0-based;
	 *                        clamped)
	 * @param searchBin1      inclusive end bin to search for a peak (0-based;
	 *                        clamped)
	 * @param halfWindowBins  requested half-window in bins ({@code >= 0})
	 * @param smoothRadius    smoothing radius for peak detection ({@code >= 0})
	 * @param ignoreZeroBins  if {@code true}, zero-count bins are skipped as peak
	 *                        candidates
	 * @param poissonWeights  if {@code true}, include Poisson weights
	 * @param minPoints       minimum number of points required ({@code >= 1})
	 * @return {@link FitWindowData} including metadata about the selected window
	 * @throws IllegalArgumentException if {@code halfWindowBins < 0} or
	 *                                  {@code minPoints < 1}
	 */
	public static FitWindowData prepareForFitAroundBestPeakGuarded(
			HistoData h,
			boolean includeZeroBins,
			int searchBin0, int searchBin1,
			int halfWindowBins,
			int smoothRadius,
			boolean ignoreZeroBins,
			boolean poissonWeights,
			int minPoints) {

		int nbin = h.getNumberBins();
		if (nbin <= 0) {
			return empty(poissonWeights);
		}
		if (halfWindowBins < 0) {
			throw new IllegalArgumentException("halfWindowBins must be >= 0");
		}
		if (minPoints < 1) {
			throw new IllegalArgumentException("minPoints must be >= 1");
		}

		int s0 = clamp(searchBin0, nbin);
		int s1 = clamp(searchBin1, nbin);
		if (s0 > s1) { int tmp = s0; s0 = s1; s1 = tmp; }

		// Prefer peaks that have room for the full window.
		int inner0 = s0 + halfWindowBins;
		int inner1 = s1 - halfWindowBins;
		int peak = (inner0 <= inner1)
				? findPeakBinBest(h, inner0, inner1, smoothRadius, ignoreZeroBins)
				: findPeakBinBest(h, s0,     s1,     smoothRadius, ignoreZeroBins);

		if (peak < 0) {
			return empty(poissonWeights);
		}

		int maxHalf  = maxHalfWindow(peak, s0, s1, nbin);
		int halfUsed = Math.min(halfWindowBins, maxHalf);

		FitWindowData out = buildWindow(h, includeZeroBins, peak, halfUsed, s0, s1, poissonWeights);
		if (out.x.length >= minPoints) {
			return out;
		}

		// Expand to maximum possible window.
		if (halfUsed < maxHalf) {
			halfUsed = maxHalf;
			out = buildWindow(h, includeZeroBins, peak, halfUsed, s0, s1, poissonWeights);
			if (out.x.length >= minPoints) {
				return out;
			}
		}

		// Last resort: include zeros.
		if (!includeZeroBins) {
			FitWindowData out2 = buildWindow(h, true, peak, halfUsed, s0, s1, poissonWeights);
			if (out2.x.length >= minPoints) {
				return out2;
			}
		}

		// Still insufficient: return empty with metadata.
		int b0 = Math.max(s0, peak - halfUsed);
		int b1 = Math.min(s1, peak + halfUsed);
		return new FitWindowData(
				new double[0], new double[0],
				poissonWeights ? new double[0] : null,
				peak, b0, b1, halfUsed);
	}

	// -----------------------------------------------------------------------
	// Private helpers
	// -----------------------------------------------------------------------

	/** Clamps a bin index to {@code [0, nbin - 1]}. */
	private static int clamp(int bin, int nbin) {
		return Math.max(0, Math.min(nbin - 1, bin));
	}

	/** Poisson weight for a count: {@code 1/count} for {@code count > 0},
	 *  {@code 1.0} otherwise. */
	private static double poissonWeight(long c) {
		return (c > 0L) ? (1.0 / c) : 1.0;
	}

	/** Converts a {@code List<Double>} to a primitive {@code double[]}. */
	private static double[] toDoubleArray(java.util.List<Double> list) {
		double[] a = new double[list.size()];
		for (int i = 0; i < a.length; i++) {
			a[i] = list.get(i);
		}
		return a;
	}

	/** Triangular-kernel weighted score centred at bin {@code i}. */
	private static double triangularScore(HistoData h, int i, int radius, int nbin) {
		int  lo          = Math.max(0,      i - radius);
		int  hi          = Math.min(nbin-1, i + radius);
		long weightedSum = 0L;
		long weightSum   = 0L;
		for (int j = lo; j <= hi; j++) {
			int w = (radius + 1 - Math.abs(j - i));
			weightedSum += w * h.counts[j];
			weightSum   += w;
		}
		return (weightSum > 0L) ? ((double) weightedSum / weightSum) : Double.NEGATIVE_INFINITY;
	}

	/**
	 * Finds the raw-maximum bin, handling plateau ties by picking the bin closest
	 * to the plateau's midpoint (and the search-range midpoint as a tiebreaker).
	 */
	private static int rawPeakWithPlateau(HistoData h, int b0, int b1, boolean ignoreZero) {
		long best = Long.MIN_VALUE;
		int  p0 = -1, p1 = -1;

		for (int i = b0; i <= b1; i++) {
			long c = h.counts[i];
			if (ignoreZero && c == 0L) {
				continue;
			}

			if (c > best) {
				best = c; p0 = i; p1 = i;
			} else if (c == best && p0 >= 0) {
				if (i == p1 + 1) {
					p1 = i;
				} else {
					int oldC = chooseBestInPlateau(h, p0, p1, b0, b1);
					if (Math.abs(i - (b0+b1)/2) < Math.abs(oldC - (b0+b1)/2)) {
						p0 = i; p1 = i;
					}
				}
			}
		}

		if (p0 < 0) {
			return findPeakBin(h, b0, b1);
		}
		if (p0 == p1) {
			return p0;
		}
		return chooseBestInPlateau(h, p0, p1, b0, b1);
	}

	/**
	 * Within a plateau {@code [p0, p1]}, picks the bin with the highest raw
	 * count, resolving ties by proximity to the plateau midpoint then to the
	 * search-range midpoint.
	 */
	private static int chooseBestInPlateau(HistoData h, int p0, int p1, int b0, int b1) {
		long bestRaw   = Long.MIN_VALUE;
		for (int i = p0; i <= p1; i++) {
			bestRaw = Math.max(bestRaw, h.counts[i]);
		}

		int plateauMid = (p0 + p1) / 2;
		int rangeMid   = (b0 + b1) / 2;
		int bestBin    = p0;
		int bestDP     = Integer.MAX_VALUE;
		int bestDR     = Integer.MAX_VALUE;

		for (int i = p0; i <= p1; i++) {
			if (h.counts[i] != bestRaw) {
				continue;
			}
			int dP = Math.abs(i - plateauMid);
			int dR = Math.abs(i - rangeMid);
			if (dP < bestDP || (dP == bestDP && dR < bestDR)) {
				bestBin = i; bestDP = dP; bestDR = dR;
			}
		}
		return bestBin;
	}

	/**
	 * Maximum half-window around {@code peak} that stays within
	 * {@code [s0, s1]} and within the histogram bounds.
	 */
	private static int maxHalfWindow(int peak, int s0, int s1, int nbin) {
		return Math.max(0, Math.min(
				Math.min(peak - s0, s1 - peak),
				Math.min(peak, (nbin - 1) - peak)));
	}

	/** Builds a {@link FitWindowData} for the given peak and half-window. */
	private static FitWindowData buildWindow(HistoData h,
	                                         boolean includeZeroBins,
	                                         int peak,
	                                         int halfUsed,
	                                         int s0, int s1,
	                                         boolean poissonWeights) {
		int b0 = Math.max(s0, peak - halfUsed);
		int b1 = Math.min(s1, peak + halfUsed);
		FitVectors fv = prepareForFit(h, includeZeroBins, b0, b1, poissonWeights);
		return new FitWindowData(fv.x, fv.y, fv.w, peak, b0, b1, halfUsed);
	}

	/** Returns an empty {@link FitWindowData}. */
	private static FitWindowData empty(boolean poissonWeights) {
		return new FitWindowData(
				new double[0], new double[0],
				poissonWeights ? new double[0] : null,
				-1, 0, -1, 0);
	}
}