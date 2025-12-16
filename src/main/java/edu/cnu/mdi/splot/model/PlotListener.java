package edu.cnu.mdi.splot.model;

/**
 * Listener for {@link Plot2D} changes. View code can use this to trigger repaint/layout updates.
 */
public interface PlotListener {

    /**
     * Called when the plot changes (curves added/removed, bounds policy changes, metadata changes, etc.).
     *
     * @param plot plot that changed
     * @param change change type
     */
    void plotChanged(Plot2D plot, Plot2D.ChangeType change);

    /**
     * Called when a curve inside the plot changes (delegated from curve listeners).
     *
     * @param plot plot containing the curve
     * @param curve curve that changed
     * @param change curve change category
     */
    void curveChangedInPlot(Plot2D plot, MutableCurve curve, MutableCurve.ChangeType change);
}
