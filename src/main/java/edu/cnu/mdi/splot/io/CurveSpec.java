package edu.cnu.mdi.splot.io;

import edu.cnu.mdi.splot.fit.CurveDrawingMethod;

/**
 * Persisted curve record: supports XY(E) and H1D via histo block.
 */
public final class CurveSpec {

    public String name;
    public boolean visible = true;

    public CurveDrawingMethod drawingMethod = CurveDrawingMethod.NONE;
    public int fitOrder = 2;

    public StyleSpec style;

    // XY / XYEXYE
    public double[] x;
    public double[] y;
    public double[] e; // optional, only for XYEXYE

    // Histogram (H1D)
    public HistoSpec histo;
}
