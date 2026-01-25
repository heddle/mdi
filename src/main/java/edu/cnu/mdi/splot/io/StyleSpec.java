package edu.cnu.mdi.splot.io;

import edu.cnu.mdi.graphics.style.LineStyle;
import edu.cnu.mdi.graphics.style.SymbolType;

/**
 * Persisted curve style.
 * Colors are stored as ARGB ints.
 */
public final class StyleSpec {
    public SymbolType symbolType;
    public LineStyle lineStyle;

    public Integer fillColorARGB;
    public Integer borderColorARGB;
    public Integer lineColorARGB;

    public Integer symbolSize;   // usually int
    public Float lineWidth;
}
