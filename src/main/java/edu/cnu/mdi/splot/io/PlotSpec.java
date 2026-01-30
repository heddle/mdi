package edu.cnu.mdi.splot.io;

import java.util.ArrayList;
import java.util.List;

import edu.cnu.mdi.splot.pdata.PlotDataType;

/**
 * Versioned, UI-agnostic persisted representation of a plot.
 */
public final class PlotSpec {
    public int formatVersion = 3;

    public PlotDataType plotDataType;

    public PlotParametersSpec parameters = new PlotParametersSpec();

    public Histo2DSpec histo2d;

    public List<CurveSpec> curves = new ArrayList<>();
}
