package edu.cnu.mdi.splot.view;

import edu.cnu.mdi.splot.model.FitResult;

/** Result of a curve hit-test in screen space. */
public final class HoverInfo {
    public final String curveName;
    public final double x;
    public final double y;
    public final FitResult fit; // nullable

    public HoverInfo(String curveName, double x, double y, FitResult fit) {
        this.curveName = curveName;
        this.x = x;
        this.y = y;
        this.fit = fit;
    }
}
