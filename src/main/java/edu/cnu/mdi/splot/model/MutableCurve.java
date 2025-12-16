package edu.cnu.mdi.splot.model;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Mutable, appendable curve suitable for dynamic plots.
 * <p>
 * This class is model-only (no graphics). It supports:
 * <ul>
 *   <li>initializing empty or with data</li>
 *   <li>appending points</li>
 *   <li>optional sigmaX and sigmaY per point</li>
 *   <li>optional validity mask (masking points without deleting)</li>
 *   <li>an optional {@link FitResult}</li>
 *   <li>snapshots for stable read access</li>
 * </ul>
 * </p>
 *
 * <h3>Missing data</h3>
 * You can represent gaps by either:
 * <ul>
 *   <li>appending NaN values, which makes {@link ICurveData#isValid(int)} false, or</li>
 *   <li>explicitly masking points with {@link #setValid(int, boolean)}.</li>
 * </ul>
 *
 * <h3>Threading</h3>
 * This implementation is not inherently thread-safe. If you update from a worker thread
 * while rendering on EDT, either:
 * <ul>
 *   <li>synchronize externally, or</li>
 *   <li>update on EDT, or</li>
 *   <li>use snapshots as your render-side contract and copy in batches.</li>
 * </ul>
 */
public class MutableCurve implements ICurveData {

    /** Change categories for listeners. */
    public enum ChangeType {
        DATA_APPENDED,
        DATA_CLEARED,
        DATA_REPLACED,
        VALIDITY_CHANGED,
        FIT_CHANGED,
        META_CHANGED
    }

    private String name;

    // Store as primitive arrays grown manually for performance.
    private double[] x;
    private double[] y;
    private double[] sigmaX; // optional
    private double[] sigmaY; // optional
    private boolean[] valid; // optional; if null -> validity by NaN rule
    private int size;

    private FitResult fitResult; // optional

    private final List<CurveListener> listeners = new ArrayList<>();

    /** Cached bounds; recomputed lazily when dirty. */
    private boolean boundsDirty = true;
    private Rectangle2D.Double cachedBounds = new Rectangle2D.Double(Double.NaN, Double.NaN, Double.NaN, Double.NaN);

    /**
     * Create an empty curve.
     *
     * @param name curve name (non-null; used for legends)
     * @param capacity initial capacity (0 allowed)
     * @param withSigmaX whether to allocate sigmaX storage
     * @param withSigmaY whether to allocate sigmaY storage
     */
    public MutableCurve(String name, int capacity, boolean withSigmaX, boolean withSigmaY) {
        this.name = Objects.requireNonNull(name, "name");
        int cap = Math.max(0, capacity);
        this.x = new double[cap];
        this.y = new double[cap];
        this.sigmaX = withSigmaX ? new double[cap] : null;
        this.sigmaY = withSigmaY ? new double[cap] : null;
        this.valid = null; // lazily created only if user masks points
        this.size = 0;
    }

    /**
     * Create a curve initialized from arrays.
     *
     * @param name curve name (non-null)
     * @param x x values (non-null)
     * @param y y values (non-null)
     * @param sigmaX optional sigmaX (may be null)
     * @param sigmaY optional sigmaY (may be null)
     * @param valid optional validity mask (may be null)
     */
    public MutableCurve(String name,
                        double[] x,
                        double[] y,
                        double[] sigmaX,
                        double[] sigmaY,
                        boolean[] valid) {

        this.name = Objects.requireNonNull(name, "name");
        Objects.requireNonNull(x, "x");
        Objects.requireNonNull(y, "y");
        if (x.length != y.length) throw new IllegalArgumentException("x and y must match length");
        if (sigmaX != null && sigmaX.length != x.length) throw new IllegalArgumentException("sigmaX must match length");
        if (sigmaY != null && sigmaY.length != x.length) throw new IllegalArgumentException("sigmaY must match length");
        if (valid != null && valid.length != x.length) throw new IllegalArgumentException("valid must match length");

        int n = x.length;
        this.x = x.clone();
        this.y = y.clone();
        this.sigmaX = (sigmaX == null) ? null : sigmaX.clone();
        this.sigmaY = (sigmaY == null) ? null : sigmaY.clone();
        this.valid = (valid == null) ? null : valid.clone();
        this.size = n;
        this.boundsDirty = true;
    }

    /** @return curve name */
    public String getName() {
        return name;
    }

    /**
     * Set the curve name (legend label).
     *
     * @param name new name (non-null)
     */
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
        fireChanged(ChangeType.META_CHANGED);
    }

    /** Add a curve listener. */
    public void addListener(CurveListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /** Remove a curve listener. */
    public void removeListener(CurveListener listener) {
        listeners.remove(listener);
    }

    /** @return current fit result, or null */
    public FitResult getFitResult() {
        return fitResult;
    }

    /**
     * Set/clear the fit result.
     * @param fitResult fit result (may be null)
     */
    public void setFitResult(FitResult fitResult) {
        this.fitResult = fitResult;
        fireChanged(ChangeType.FIT_CHANGED);
    }

    /** Clear all points (capacity retained). */
    public void clear() {
        size = 0;
        boundsDirty = true;
        fireChanged(ChangeType.DATA_CLEARED);
    }

    /** @return number of points currently stored */
    @Override
    public int size() {
        return size;
    }

    @Override
    public double getX(int i) {
        rangeCheck(i);
        return x[i];
    }

    @Override
    public double getY(int i) {
        rangeCheck(i);
        return y[i];
    }

    @Override
    public boolean hasSigmaX() {
        return sigmaX != null;
    }

    @Override
    public boolean hasSigmaY() {
        return sigmaY != null;
    }

    @Override
    public double getSigmaX(int i) {
        if (sigmaX == null) throw new IllegalStateException("sigmaX not present");
        rangeCheck(i);
        return sigmaX[i];
    }

    @Override
    public double getSigmaY(int i) {
        if (sigmaY == null) throw new IllegalStateException("sigmaY not present");
        rangeCheck(i);
        return sigmaY[i];
    }

    @Override
    public boolean isValid(int i) {
        rangeCheck(i);
        if (valid != null) {
            return valid[i];
        }
        return ICurveData.super.isValid(i);
    }

    /**
     * Explicitly set validity for a point (mask/unmask).
     * This creates the validity mask lazily if needed.
     */
    public void setValid(int i, boolean isValid) {
        rangeCheck(i);
        if (valid == null) {
            valid = new boolean[Math.max(1, x.length)];
            // default validity for existing points:
            for (int k = 0; k < size; k++) {
                valid[k] = ICurveData.super.isValid(k);
            }
        }
        valid[i] = isValid;
        boundsDirty = true;
        fireChanged(ChangeType.VALIDITY_CHANGED);
    }

    /** Append point without error bars. */
    public void add(double xVal, double yVal) {
        ensureCapacity(size + 1);
        x[size] = xVal;
        y[size] = yVal;
        if (sigmaX != null) sigmaX[size] = Double.NaN;
        if (sigmaY != null) sigmaY[size] = Double.NaN;
        if (valid != null) valid[size] = !(Double.isNaN(xVal) || Double.isNaN(yVal));
        size++;
        boundsDirty = true;
        fireChanged(ChangeType.DATA_APPENDED);
    }

    /** Append point with sigmaY only (requires sigmaY storage). */
    public void add(double xVal, double yVal, double sigmaYVal) {
        if (sigmaY == null) {
            throw new IllegalStateException("sigmaY not enabled for this curve");
        }
        ensureCapacity(size + 1);
        x[size] = xVal;
        y[size] = yVal;
        sigmaY[size] = sigmaYVal;
        if (sigmaX != null) sigmaX[size] = Double.NaN;
        if (valid != null) valid[size] = !(Double.isNaN(xVal) || Double.isNaN(yVal));
        size++;
        boundsDirty = true;
        fireChanged(ChangeType.DATA_APPENDED);
    }

    /** Append point with sigmaX and sigmaY (requires both storages). */
    public void add(double xVal, double yVal, double sigmaXVal, double sigmaYVal) {
        if (sigmaX == null) {
            throw new IllegalStateException("sigmaX not enabled for this curve");
        }
        if (sigmaY == null) {
            throw new IllegalStateException("sigmaY not enabled for this curve");
        }
        ensureCapacity(size + 1);
        x[size] = xVal;
        y[size] = yVal;
        sigmaX[size] = sigmaXVal;
        sigmaY[size] = sigmaYVal;
        if (valid != null) valid[size] = !(Double.isNaN(xVal) || Double.isNaN(yVal));
        size++;
        boundsDirty = true;
        fireChanged(ChangeType.DATA_APPENDED);
    }

    /**
     * Replace all curve data in one call (useful for batch updates).
     * Fit result is not modified.
     */
    public void setData(double[] newX, double[] newY, double[] newSigmaX, double[] newSigmaY, boolean[] newValid) {
        Objects.requireNonNull(newX, "newX");
        Objects.requireNonNull(newY, "newY");
        if (newX.length != newY.length) throw new IllegalArgumentException("x/y must match length");
        if (newSigmaX != null && newSigmaX.length != newX.length) throw new IllegalArgumentException("sigmaX length mismatch");
        if (newSigmaY != null && newSigmaY.length != newX.length) throw new IllegalArgumentException("sigmaY length mismatch");
        if (newValid != null && newValid.length != newX.length) throw new IllegalArgumentException("valid length mismatch");

        this.x = newX.clone();
        this.y = newY.clone();
        this.sigmaX = (newSigmaX == null) ? null : newSigmaX.clone();
        this.sigmaY = (newSigmaY == null) ? null : newSigmaY.clone();
        this.valid = (newValid == null) ? null : newValid.clone();
        this.size = newX.length;

        boundsDirty = true;
        fireChanged(ChangeType.DATA_REPLACED);
    }

    /**
     * Get bounds of valid data points (ignores invalid/masked points).
     * Cached and recomputed only when dirty.
     *
     * @return bounds rectangle; if no valid points, returns NaN rectangle.
     */
    public Rectangle2D.Double getDataBounds() {
        if (boundsDirty) {
            cachedBounds = computeBounds();
            boundsDirty = false;
        }
        return (Rectangle2D.Double) cachedBounds.clone();
    }

    /**
     * Create an immutable snapshot of the current curve data.
     * This is the recommended read contract for renderers/analysis when the curve is dynamic.
     */
    public CurveSnapshot snapshot() {
        double[] sx = (sigmaX == null) ? null : Arrays.copyOf(sigmaX, size);
        double[] sy = (sigmaY == null) ? null : Arrays.copyOf(sigmaY, size);
        boolean[] vm = (valid == null) ? null : Arrays.copyOf(valid, size);
        return new CurveSnapshot(
                name,
                Arrays.copyOf(x, size),
                Arrays.copyOf(y, size),
                sx,
                sy,
                vm,
                fitResult
        );
    }

    // ---------- internals ----------

    private void fireChanged(ChangeType type) {
        for (CurveListener listener : List.copyOf(listeners)) {
            listener.curveChanged(this, type);
        }
    }

    private void rangeCheck(int i) {
        if (i < 0 || i >= size) {
            throw new IndexOutOfBoundsException("index " + i + " out of [0," + (size - 1) + "]");
        }
    }

    private void ensureCapacity(int needed) {
        if (x.length >= needed) return;
        int newCap = Math.max(needed, Math.max(8, x.length * 2));
        x = Arrays.copyOf(x, newCap);
        y = Arrays.copyOf(y, newCap);
        if (sigmaX != null) sigmaX = Arrays.copyOf(sigmaX, newCap);
        if (sigmaY != null) sigmaY = Arrays.copyOf(sigmaY, newCap);
        if (valid != null) valid = Arrays.copyOf(valid, newCap);
    }

    private Rectangle2D.Double computeBounds() {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        boolean any = false;
        for (int i = 0; i < size; i++) {
            if (!isValid(i)) continue;
            double xi = x[i];
            double yi = y[i];
            if (Double.isNaN(xi) || Double.isNaN(yi)) continue;
            any = true;
            minX = Math.min(minX, xi);
            maxX = Math.max(maxX, xi);
            minY = Math.min(minY, yi);
            maxY = Math.max(maxY, yi);
        }

        if (!any) {
            return new Rectangle2D.Double(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }
        return new Rectangle2D.Double(minX, minY, (maxX - minX), (maxY - minY));
    }
}
