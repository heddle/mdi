package edu.cnu.mdi.splot.io;

import edu.cnu.mdi.splot.plot.LimitsMethod;
import edu.cnu.mdi.splot.plot.PlotParameters;

/**
 * Persisted subset of PlotParameters that materially affects plot appearance/behavior.
 */
public final class PlotParametersSpec {

    // Titles / labels
    public String plotTitle;
    public String xLabel;
    public String yLabel;

    // Axis direction
    public boolean reverseXaxis;
    public boolean reverseYaxis;

    // Axis scaling
    public PlotParameters.AxisScale xScale = PlotParameters.AxisScale.LINEAR;
    public PlotParameters.AxisScale yScale = PlotParameters.AxisScale.LINEAR;

    // Include-zero behavior
    public boolean includeXZero;
    public boolean includeYZero;

    // Limits
    public LimitsMethod xLimitsMethod;
    public LimitsMethod yLimitsMethod;

    public Double manualXmin;
    public Double manualXmax;
    public Double manualYmin;
    public Double manualYmax;

    // Tick formatting knobs
    public int numDecimalX;
    public int minExponentX;
    public int numDecimalY;
    public int minExponentY;

    // Legend drawing bits
    public boolean drawLegend;
    public boolean legendBorder;
    public int legendLineLength;

    // Extra strings
    public boolean drawExtra;
    public boolean extraBorder;
    public String[] extraStrings;
}
