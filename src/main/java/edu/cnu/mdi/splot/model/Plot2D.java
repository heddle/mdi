package edu.cnu.mdi.splot.model;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Graphics-free 2D plot model: a collection of curves plus axis metadata and bounds policy.
 * <p>
 * This does not store any visual styling. Rendering concerns belong elsewhere.
 * </p>
 */
public class Plot2D implements CurveListener {

    /** Change categories for plot listeners. */
    public enum ChangeType {
        CURVE_ADDED,
        CURVE_REMOVED,
        CURVES_CLEARED,
        AXIS_META_CHANGED,
        BOUNDS_POLICY_CHANGED,
        VIEW_BOUNDS_CHANGED,
        TITLE_CHANGED
    }

    private final List<MutableCurve> curves = new ArrayList<>();
    private final List<PlotListener> listeners = new ArrayList<>();

    private String title = "";

    private AxisMeta xAxis = new AxisMeta("x", "");
    private AxisMeta yAxis = new AxisMeta("y", "");

    private BoundsPolicy boundsPolicy = BoundsPolicy.AUTO;
    private Rectangle2D.Double manualViewBounds = new Rectangle2D.Double(0, 0, 1, 1);

    /**
     * Add a plot listener.
     */
    public void addListener(PlotListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Remove a plot listener.
     */
    public void removeListener(PlotListener listener) {
        listeners.remove(listener);
    }

    /** @return plot title (may be empty) */
    public String getTitle() {
        return title;
    }

    /** Set plot title. */
    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title, "title");
        firePlotChanged(ChangeType.TITLE_CHANGED);
    }

    public AxisMeta getXAxis() {
        return xAxis;
    }

    public AxisMeta getYAxis() {
        return yAxis;
    }

    public void setXAxis(AxisMeta xAxis) {
        this.xAxis = Objects.requireNonNull(xAxis, "xAxis");
        firePlotChanged(ChangeType.AXIS_META_CHANGED);
    }

    public void setYAxis(AxisMeta yAxis) {
        this.yAxis = Objects.requireNonNull(yAxis, "yAxis");
        firePlotChanged(ChangeType.AXIS_META_CHANGED);
    }

    public BoundsPolicy getBoundsPolicy() {
        return boundsPolicy;
    }

    /**
     * Set bounds policy.
     * If switching to MANUAL, the current manual bounds are kept as-is.
     */
    public void setBoundsPolicy(BoundsPolicy policy) {
        this.boundsPolicy = Objects.requireNonNull(policy, "policy");
        firePlotChanged(ChangeType.BOUNDS_POLICY_CHANGED);
    }

    /**
     * Set manual view bounds (used only when boundsPolicy == MANUAL).
     */
    public void setManualViewBounds(Rectangle2D.Double bounds) {
        this.manualViewBounds = (Rectangle2D.Double) Objects.requireNonNull(bounds, "bounds").clone();
        firePlotChanged(ChangeType.VIEW_BOUNDS_CHANGED);
    }

    /**
     * @return manual view bounds (defensive copy)
     */
    public Rectangle2D.Double getManualViewBounds() {
        return (Rectangle2D.Double) manualViewBounds.clone();
    }

    /**
     * @return unmodifiable list of curves.
     */
    public List<MutableCurve> getCurves() {
        return Collections.unmodifiableList(curves);
    }

    /**
     * Add a curve to the plot. The plot attaches as a listener to propagate changes.
     */
    public void addCurve(MutableCurve curve) {
        Objects.requireNonNull(curve, "curve");
        curves.add(curve);
        curve.addListener(this);
        firePlotChanged(ChangeType.CURVE_ADDED);
    }

    /**
     * Remove a curve from the plot.
     */
    public boolean removeCurve(MutableCurve curve) {
        Objects.requireNonNull(curve, "curve");
        boolean removed = curves.remove(curve);
        if (removed) {
            curve.removeListener(this);
            firePlotChanged(ChangeType.CURVE_REMOVED);
        }
        return removed;
    }

    /**
     * Remove all curves.
     */
    public void clearCurves() {
        for (MutableCurve c : curves) {
            c.removeListener(this);
        }
        curves.clear();
        firePlotChanged(ChangeType.CURVES_CLEARED);
    }

    /**
     * Compute data bounds across all curves, unioned together.
     * Ignores curves with no valid points.
     *
     * @return union bounds; if no curve has valid points, returns NaN rectangle.
     */
    public Rectangle2D.Double getDataBounds() {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        boolean any = false;

        for (MutableCurve c : curves) {
            Rectangle2D.Double b = c.getDataBounds();
            if (Double.isNaN(b.x) || Double.isNaN(b.y) || Double.isNaN(b.width) || Double.isNaN(b.height)) {
                continue;
            }
            any = true;
            minX = Math.min(minX, b.x);
            maxX = Math.max(maxX, b.x + b.width);
            minY = Math.min(minY, b.y);
            maxY = Math.max(maxY, b.y + b.height);
        }

        if (!any) {
            return new Rectangle2D.Double(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }
        return new Rectangle2D.Double(minX, minY, (maxX - minX), (maxY - minY));
    }

    /**
     * @return the current view bounds according to {@link #getBoundsPolicy()}:
     * <ul>
     *   <li>AUTO: derived from {@link #getDataBounds()}</li>
     *   <li>MANUAL: returns {@link #getManualViewBounds()}</li>
     * </ul>
     */
    public Rectangle2D.Double getViewBounds() {
        if (boundsPolicy == BoundsPolicy.MANUAL) {
            return getManualViewBounds();
        }
        return getDataBounds();
    }

    // ---- CurveListener: propagate curve changes to plot listeners ----

    @Override
    public void curveChanged(MutableCurve curve, MutableCurve.ChangeType change) {
        for (PlotListener listener : List.copyOf(listeners)) {
            listener.curveChangedInPlot(this, curve, change);
        }
        // Depending on your view layer, you can either:
        // - always repaint on curve change, or
        // - only update derived caches when needed.
    }

    private void firePlotChanged(ChangeType change) {
        for (PlotListener listener : List.copyOf(listeners)) {
            listener.plotChanged(this, change);
        }
    }
}
