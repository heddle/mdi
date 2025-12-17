package edu.cnu.mdi.splot.view;

import java.util.ArrayList;
import java.util.List;

import edu.cnu.mdi.splot.model.FitResult;

/** Centralized fit-to-text formatting for legend and dialogs. */
public final class FitSummaryFormatter {

    private FitSummaryFormatter() {}

    public static List<String> defaultLines(FitResult fit) {
        List<String> lines = new ArrayList<>();
        lines.add(fit.getSpec().getModelName()); // or getModelId if you prefer

        var names = fit.getSpec().getParameterNames(); // List<String>
        double[] p = fit.getParameters();

        int n = Math.min(names.size(), p.length);
        for (int i = 0; i < n; i++) {
            lines.add(names.get(i) + " = " + format(p[i]));
        }

        lines.add(String.format("fit range: [%.5f, %.5f]", fit.getXMin(), fit.getXMax()));
        return lines;
    }

    private static String format(double v) {
        return String.format("%.6g", v);
    }
}
