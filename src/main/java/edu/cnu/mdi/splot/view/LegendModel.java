package edu.cnu.mdi.splot.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cnu.mdi.splot.model.FitDiagnostics;
import edu.cnu.mdi.splot.model.FitResult;
import edu.cnu.mdi.splot.model.MutableCurve;
import edu.cnu.mdi.splot.model.Plot2D;

/**
 * Builds a legend model from a {@link Plot2D}.
 * <p>
 * The renderer can use this to draw a legend without coupling itself to plot internals.
 * </p>
 */
public class LegendModel {

    private final List<LegendEntry> entries;

    public LegendModel(List<LegendEntry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public List<LegendEntry> getEntries() {
        return entries;
    }

    /**
     * Build a default legend model: one entry per curve. If a fit exists,
     * include a compact summary line.
     */
    public static LegendModel fromPlot(Plot2D plot) {
        List<LegendEntry> list = new ArrayList<>();
        for (MutableCurve c : plot.getCurves()) {
            FitResult fit = c.getFitResult();
            String summary = (fit == null) ? c.getName() : (c.getName() + " — " + summarizeFit(fit));
            list.add(new LegendEntry(c.getName(), fit, summary));
        }
        return new LegendModel(list);
    }

    private static String summarizeFit(FitResult fit) {
        String model = fit.getSpec().getModelId();
        FitDiagnostics d = fit.getDiagnostics();
        if (d != null && !Double.isNaN(d.getReducedChi2())) {
            return model + ", χ²ᵣ=" + format3(d.getReducedChi2());
        }
        return model;
    }

    private static String format3(double v) {
        return String.format("%.3g", v);
    }
}
