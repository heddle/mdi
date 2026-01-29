package edu.cnu.mdi.splot.pdata;

import java.util.Arrays;

/**
 * Container class for 2D histogram (heatmap) data.
 * <p>
 * Data coordinates are binned into a uniform grid of {@code nx * ny} cells over
 * {@code [xmin, xmax]} and {@code [ymin, ymax]}.
 * </p>
 *
 * <h3>Bin conventions</h3>
 * <ul>
 *   <li>Bins are uniform in x and y.</li>
 *   <li>Values strictly below min or strictly above max are out of range.</li>
 *   <li>Values exactly equal to max are included in the last bin.</li>
 * </ul>
 *
 * <h3>Under/over handling</h3>
 * <p>
 * Like {@link HistoData}, values outside the range are not binned; instead
 * they are counted in under/over counters. In 2D there are edge regions and
 * corners (both x and y out of range).
 * </p>
 *
 * <h3>Threading</h3>
 * This class is safe to fill from a worker thread while the EDT paints by using
 * {@link #snapshotBins()} in the painter. All mutations and snapshots are guarded
 * by an internal lock.
 *
 * @author heddle
 */
public final class Histo2DData {

    // ---- identity ----
    private final String _name;

    // ---- geometry ----
    private final int _nx;
    private final int _ny;
    private final double _xmin;
    private final double _xmax;
    private final double _ymin;
    private final double _ymax;
    private final double _dx;
    private final double _dy;

    // ---- storage ----
    private final double[][] _bins;

    // ---- counts (HistoData-like) ----
    /** In-range fill count (good points). */
    private long _goodCount;

    /** Out-of-range counts when ONLY x is out-of-range (y in-range). */
    private long _xUnderCount;
    private long _xOverCount;

    /** Out-of-range counts when ONLY y is out-of-range (x in-range). */
    private long _yUnderCount;
    private long _yOverCount;

    /** Corner counts (both x and y out-of-range). */
    private long _xUnder_yUnder;
    private long _xUnder_yOver;
    private long _xOver_yUnder;
    private long _xOver_yOver;

    // ---- stats/cache ----
    private boolean _minMaxDirty = true;
    private double _cachedMax = 0.0;
    private double _cachedMinNonZero = 0.0;

    // single lock for simplicity
    private final Object _lock = new Object();

    /**
     * Constructor for 2D histogram data.
     * @param name histogram name
     * @param xmin the minimum x value
     * @param xmax the maximum x value
     * @param nx number of x bins
     * @param ymin the minimum y value
     * @param ymax the maximum y value
     * @param ny number of y bins
     */
    public Histo2DData(String name,
                       double xmin, double xmax, int nx,
                       double ymin, double ymax, int ny) {

        if (name == null) {
            throw new IllegalArgumentException("Histo2DData: name is null");
        }
        if (nx < 1 || ny < 1) {
            throw new IllegalArgumentException("Histo2DData: nx and ny must be >= 1");
        }
        if (!(xmax > xmin)) {
            throw new IllegalArgumentException("Histo2DData: xmax must be > xmin");
        }
        if (!(ymax > ymin)) {
            throw new IllegalArgumentException("Histo2DData: ymax must be > ymin");
        }

        _name = name;

        _nx = nx;
        _ny = ny;

        _xmin = xmin;
        _xmax = xmax;
        _ymin = ymin;
        _ymax = ymax;

        //make uniform bins
        _dx = (xmax - xmin) / nx;
        _dy = (ymax - ymin) / ny;

        _bins = new double[nx][ny];
    }

    public String name() {
        return _name;
    }

    // ---- geometry ----
    public int nx() {
        return _nx;
    }

    public int ny() {
        return _ny;
    }

    public double xMin() {
        return _xmin;
    }

    public double xMax() {
        return _xmax;
    }

    public double yMin() {
        return _ymin;
    }

    public double yMax() {
        return _ymax;
    }

    public double xBinWidth() {
        return _dx;
    }

    public double yBinWidth() {
        return _dy;
    }

    // ---- binning ----

    /**
     * Get the x-bin index for a data x.
     * Returns -1 if out of range.
     * Includes x==xMax in the last bin.
     */
    public int xIndex(double x) {
        if (!Double.isFinite(x)) {
            return -1;
        }
        if (x < _xmin || x > _xmax) {
            return -1;
        }
        if (x == _xmax) {
            return _nx - 1;
        }
        int ix = (int) ((x - _xmin) / _dx);
        return (ix >= 0 && ix < _nx) ? ix : -1;
    }

    /**
     * Get the y-bin index for a data y.
     * Returns -1 if out of range.
     * Includes y==yMax in the last bin.
     */
    public int yIndex(double y) {
        if (!Double.isFinite(y)) {
            return -1;
        }
        if (y < _ymin || y > _ymax) {
            return -1;
        }
        if (y == _ymax) {
            return _ny - 1;
        }
        int iy = (int) ((y - _ymin) / _dy);
        return (iy >= 0 && iy < _ny) ? iy : -1;
    }

    public void fill(double x, double y) {
        fill(x, y, 1.0);
    }

    /**
     * Fill the histogram.
     * <p>
     * Out-of-range points increment under/over counters; in-range points increment
     * the appropriate bin. Weight may be any finite value (including negative),
     * but note that negative weights make "max" less meaningful.
     * </p>
     */
    public void fill(double x, double y, double weight) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(weight)) {
            return;
        }
        if (weight == 0.0) {
            // still arguably a "fill", but doesn't affect anything; treat as no-op
            return;
        }

        // Range tests (use strict comparisons; include equals on max)
        final boolean xIn = (x >= _xmin) && (x <= _xmax);
        final boolean yIn = (y >= _ymin) && (y <= _ymax);

        synchronized (_lock) {

            if (xIn && yIn) {
                // in-range -> bin
                final int ix = (x == _xmax) ? (_nx - 1) : (int) ((x - _xmin) / _dx);
                final int iy = (y == _ymax) ? (_ny - 1) : (int) ((y - _ymin) / _dy);

                if (ix >= 0 && ix < _nx && iy >= 0 && iy < _ny) {
                    _bins[ix][iy] += weight;
                    _goodCount++;
                    _minMaxDirty = true;
                }
                // else: should never happen, but don't count as good if it does
                return;
            }

            // out-of-range -> counters
            if (!xIn && yIn) {
                if (x < _xmin) _xUnderCount++;
                else _xOverCount++;
            }
            else if (xIn && !yIn) {
                if (y < _ymin) _yUnderCount++;
                else _yOverCount++;
            }
            else { // both out-of-range -> corner
                final boolean xUnder = (x < _xmin);
                final boolean yUnder = (y < _ymin);

                if (xUnder && yUnder) _xUnder_yUnder++;
                else if (xUnder)      _xUnder_yOver++;
                else if (yUnder)      _xOver_yUnder++;
                else                  _xOver_yOver++;
            }
        }
    }

    // ---- access ----

    /**
     * Get the bin content. Returns 0 for out-of-range indices.
     */
    public double bin(int ix, int iy) {
        if (ix < 0 || ix >= _nx || iy < 0 || iy >= _ny) {
            return 0.0;
        }
        synchronized (_lock) {
            return _bins[ix][iy];
        }
    }

    /**
     * Backwards-compatible name: this returns the in-range (good) fill count.
     */
    public int entries() {
        synchronized (_lock) {
            // safe narrowing: you can also change return type if you want later
            return (int) Math.min(Integer.MAX_VALUE, _goodCount);
        }
    }

    /** @return in-range fill count (good points). */
    public long getGoodCount() {
        synchronized (_lock) {
            return _goodCount;
        }
    }

    /** @return total fills including out-of-range. */
    public long getTotalCount() {
        synchronized (_lock) {
            return _goodCount
                    + _xUnderCount + _xOverCount
                    + _yUnderCount + _yOverCount
                    + _xUnder_yUnder + _xUnder_yOver + _xOver_yUnder + _xOver_yOver;
        }
    }

    /** @return x-underflow count (y in range). */
    public long getXUnderCount() { synchronized (_lock) { return _xUnderCount; } }

    /** @return x-overflow count (y in range). */
    public long getXOverCount()  { synchronized (_lock) { return _xOverCount; } }

    /** @return y-underflow count (x in range). */
    public long getYUnderCount() { synchronized (_lock) { return _yUnderCount; } }

    /** @return y-overflow count (x in range). */
    public long getYOverCount()  { synchronized (_lock) { return _yOverCount; } }

    /** @return corner: x under, y under. */
    public long getXUnderYUnderCount() { synchronized (_lock) { return _xUnder_yUnder; } }

    /** @return corner: x under, y over. */
    public long getXUnderYOverCount()  { synchronized (_lock) { return _xUnder_yOver; } }

    /** @return corner: x over, y under. */
    public long getXOverYUnderCount()  { synchronized (_lock) { return _xOver_yUnder; } }

    /** @return corner: x over, y over. */
    public long getXOverYOverCount()   { synchronized (_lock) { return _xOver_yOver; } }

    public double maxBin() {
        synchronized (_lock) {
            recomputeMinMaxIfNeeded();
            return _cachedMax;
        }
    }

    public double minNonZero() {
        synchronized (_lock) {
            recomputeMinMaxIfNeeded();
            return _cachedMinNonZero;
        }
    }

    // ---- maintenance ----
    public void clear() {
        synchronized (_lock) {
            for (int ix = 0; ix < _nx; ix++) {
                Arrays.fill(_bins[ix], 0.0);
            }

            _goodCount = 0;
            _xUnderCount = 0;
            _xOverCount = 0;
            _yUnderCount = 0;
            _yOverCount = 0;
            _xUnder_yUnder = 0;
            _xUnder_yOver = 0;
            _xOver_yUnder = 0;
            _xOver_yOver = 0;

            _cachedMax = 0.0;
            _cachedMinNonZero = 0.0;
            _minMaxDirty = false;
        }
    }

    /**
     * Deep-copy bins for safe use during painting.
     */
    public double[][] snapshotBins() {
        synchronized (_lock) {
            double[][] snap = new double[_nx][_ny];
            for (int ix = 0; ix < _nx; ix++) {
                System.arraycopy(_bins[ix], 0, snap[ix], 0, _ny);
            }
            return snap;
        }
    }

    /**
     * Static utility to get the maximum z value from a 2D bins array.
     * @param bins 2D array of bin values
     * @return maximum finite bin value, or 0 if none
     */
    public static double maxZ(double[][] bins) {
        double max = 0.0;
        for (int ix = 0; ix < bins.length; ix++) {
            for (int iy = 0; iy < bins[ix].length; iy++) {
                double v = bins[ix][iy];
                if (Double.isFinite(v) && v > max) {
                    max = v;
                }
            }
        }
        return max;
    }

    // ---- helpers ----
    private void recomputeMinMaxIfNeeded() {
        if (!_minMaxDirty) {
            return;
        }

        double max = 0.0;
        double minNZ = 0.0;

        for (int ix = 0; ix < _nx; ix++) {
            for (int iy = 0; iy < _ny; iy++) {
                double v = _bins[ix][iy];
                if (!Double.isFinite(v)) {
                    continue;
                }
                if (v > max) {
                    max = v;
                }
                if (v > 0.0) {
                    if (minNZ == 0.0 || v < minNZ) {
                        minNZ = v;
                    }
                }
            }
        }

        _cachedMax = max;
        _cachedMinNonZero = minNZ;
        _minMaxDirty = false;
    }
    
    /**
     * Return a human-readable summary of in-range and out-of-range fills.
     * <p>
     * Mirrors the intent of {@link HistoData#rangeSummary()} but extended to 2D.
     * Only non-zero out-of-range categories are reported.
     * </p>
     *
     * @return summary string
     */
    public String rangeSummary() {
        synchronized (_lock) {

            long total = getTotalCount();
            long good  = _goodCount;

            StringBuilder sb = new StringBuilder(128);
            sb.append("good=").append(good);

            if (total > good) {
                sb.append(" / total=").append(total);
            }

            // x-only out-of-range
            if (_xUnderCount > 0) {
                sb.append(", x<").append(_xmin).append(":").append(_xUnderCount);
            }
            if (_xOverCount > 0) {
                sb.append(", x>").append(_xmax).append(":").append(_xOverCount);
            }

            // y-only out-of-range
            if (_yUnderCount > 0) {
                sb.append(", y<").append(_ymin).append(":").append(_yUnderCount);
            }
            if (_yOverCount > 0) {
                sb.append(", y>").append(_ymax).append(":").append(_yOverCount);
            }

            // corners (both x and y out-of-range)
            if (_xUnder_yUnder > 0) {
                sb.append(", x<").append(_xmin)
                  .append(" & y<").append(_ymin)
                  .append(":").append(_xUnder_yUnder);
            }
            if (_xUnder_yOver > 0) {
                sb.append(", x<").append(_xmin)
                  .append(" & y>").append(_ymax)
                  .append(":").append(_xUnder_yOver);
            }
            if (_xOver_yUnder > 0) {
                sb.append(", x>").append(_xmax)
                  .append(" & y<").append(_ymin)
                  .append(":").append(_xOver_yUnder);
            }
            if (_xOver_yOver > 0) {
                sb.append(", x>").append(_xmax)
                  .append(" & y>").append(_ymax)
                  .append(":").append(_xOver_yOver);
            }

            return sb.toString();
        }
    }

}
