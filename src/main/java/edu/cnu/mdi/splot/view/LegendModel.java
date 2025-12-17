package edu.cnu.mdi.splot.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.cnu.mdi.splot.model.CurveSnapshot;
import edu.cnu.mdi.splot.model.FitResult;
import edu.cnu.mdi.splot.model.MutableCurve;
import edu.cnu.mdi.splot.model.Plot2D;

/**
 * Legend view-model: one entry per curve.
 */
public final class LegendModel {

    private final List<LegendEntry> entries;

    public LegendModel(List<LegendEntry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public List<LegendEntry> getEntries() {
        return entries;
    }

    /**
     * Build legend entries with glyph paint.
     *
     * Fit detail rule:
     * - show fit detail lines ONLY if there is exactly one distinct FitResult across the plot.
     */
    public static LegendModel fromPlot(Plot2D plot, CurvePaintProvider paintProvider) {
        Objects.requireNonNull(plot, "plot");
        Objects.requireNonNull(paintProvider, "paintProvider");

        // Count distinct FitResult instances (identity is fine; FitResult is immutable)
        Map<FitResult, Boolean> distinct = new IdentityHashMap<>();
        for (MutableCurve c : plot.getCurves()) {
            FitResult fr = c.getFitResult();
            if (fr != null) distinct.put(fr, Boolean.TRUE);
        }

        List<LegendEntry> list = new ArrayList<>();

        Map<FitResult, Boolean> printed = new IdentityHashMap<>();
        for (MutableCurve c : plot.getCurves()) {
            CurveSnapshot snap = c.snapshot();
            CurvePaint paint = paintProvider.paintFor(snap);
            FitResult fit = snap.getFitResult();

            List<String> details = new ArrayList<>();

            if (fit != null) {
                boolean alreadyPrinted = printed.containsKey(fit);

                boolean looksLikeFitCurve =
                        paint.getDrawMode() == CurveDrawMode.LINES ||
                        paint.getDrawMode() == CurveDrawMode.LINES_AND_POINTS;

                if (!alreadyPrinted && looksLikeFitCurve) {
                    details.addAll(FitSummaryFormatter.defaultLines(fit));
                    printed.put(fit, Boolean.TRUE);
                }
            }

            list.add(new LegendEntry(c.getName(), paint, fit, details));
        }
        return new LegendModel(list);
    }
}
