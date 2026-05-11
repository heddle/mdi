package edu.cnu.mdi.splot.io;

import edu.cnu.mdi.splot.fit.CurveDrawingMethod;

/**
 * Persisted curve record: supports XY(E) and H1D via histo block.
 */
public final class CurveSpec {

	/** The curve name, used for legends and such. */
    public String name;
    
    /** Whether the curve is visible. This is persisted so that it can be toggled on and off in the UI and remembered across sessions. */
    public boolean visible = true;

    /** The method to use for drawing the curve, such as straight lines, splines, or no curve (just points). */
    public CurveDrawingMethod drawingMethod = CurveDrawingMethod.NONE;
    
    /** The order of the fit to apply to the curve, if any. This is relevant when drawingMethod specifies a fitting method. */
    public int fitOrder = 2;

    /** The style to use for drawing the curve, such as color, line width, and point shape. This is persisted so that the curve can be rendered consistently across sessions. */
    public StyleSpec style;

    /** The data points for the curve. For XY(E) curves, x and y are the coordinates of the points, and e is the error (if applicable). For H1D curves, x and y may represent bin centers and counts, respectively, and e may represent uncertainties in the counts. */
    public double[] x;
    
    /** The y values corresponding to the x values. For XY(E) curves, these are the y coordinates of the points. For H1D curves, these may represent the counts in each bin. */
    public double[] y;
    
    /** The error values corresponding to the x and y values. For XY(E) curves, these represent the uncertainties in the y values. For H1D curves, these may represent the uncertainties in the counts. This field is optional and may be null if no error information is available. */
    public double[] e; 


    /** The histogram specification for this curve, if it represents a histogram. For XY(E) curves, this may be null. For H1D curves, this may contain information about the binning and other histogram properties. */
    public HistoSpec histo;

    /** The 2D histogram specification for this curve, if it represents a 2D histogram. For XY(E) and H1D curves, this may be null. For H2D curves, this may contain information about the binning and other properties of the 2D histogram. */
    public Histo2DSpec histo2d;
}
