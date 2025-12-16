package edu.cnu.mdi.splot.model;

/**
 * Listener for changes to a {@link MutableCurve}.
 * Intended for view models / renderers to repaint efficiently.
 */
public interface CurveListener {

    /**
     * Called when curve data changes (append, clear, set point validity, etc.).
     *
     * @param curve the curve that changed
     * @param change an enum describing the type of change
     */
    void curveChanged(MutableCurve curve, MutableCurve.ChangeType change);
}
