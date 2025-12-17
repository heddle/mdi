package edu.cnu.mdi.splot.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import edu.cnu.mdi.splot.model.FitResult;

/**
 * Single legend entry (view-model).
 * Holds the curve name, optional fit, optional detail lines, and the paint used for glyphs.
 */
public final class LegendEntry {

    private final String curveName;
    private final CurvePaint paint; // non-null
    private final FitResult fit;     // nullable
    private final List<String> detailLines;

    public LegendEntry(String curveName, CurvePaint paint, FitResult fit, List<String> detailLines) {
        this.curveName = Objects.requireNonNull(curveName, "curveName");
        this.paint = Objects.requireNonNull(paint, "paint");
        this.fit = fit;
        this.detailLines = Collections.unmodifiableList(new ArrayList<>(detailLines == null ? List.of() : detailLines));
    }

    public String getCurveName() {
        return curveName;
    }

    public CurvePaint getPaint() {
        return paint;
    }

    public FitResult getFit() {
        return fit;
    }

    /** Detail lines shown under the main curve name (often fit summary lines). */
    public List<String> getDetailLines() {
        return detailLines;
    }

    /** All legend lines, with curveName first, then any details. */
    public List<String> getLines() {
        List<String> lines = new ArrayList<>(1 + detailLines.size());
        lines.add(curveName);
        lines.addAll(detailLines);
        return lines;
    }
}
