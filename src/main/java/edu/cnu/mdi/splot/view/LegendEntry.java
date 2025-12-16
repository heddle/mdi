package edu.cnu.mdi.splot.view;

import edu.cnu.mdi.splot.model.FitResult;

/**
 * Single legend entry. This is a view-model object (still no drawing).
 */
public final class LegendEntry {

    private final String curveName;
    private final FitResult fit; // nullable
    private final String summary; // preformatted summary line(s)

    public LegendEntry(String curveName, FitResult fit, String summary) {
        this.curveName = curveName;
        this.fit = fit;
        this.summary = summary;
    }

    public String getCurveName() {
        return curveName;
    }

    public FitResult getFit() {
        return fit;
    }

    public String getSummary() {
        return summary;
    }
}
