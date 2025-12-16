package edu.cnu.mdi.splot.view;

import java.text.DecimalFormat;

/**
 * Default tick formatter using a compact general format.
 */
public class DefaultTickLabelFormatter implements TickLabelFormatter {

    @Override
    public String format(double value) {
        // Similar to %.6g behavior but without locale surprises
        return String.format("%.6g", value);
    }

    /** A scientific notation formatter like 1.23E4. */
    public static TickLabelFormatter scientific(int sigFigs) {
        final int sf = Math.max(1, Math.min(12, sigFigs));
        // DecimalFormat pattern for sig figs is annoying; use String.format
        return v -> String.format("%." + (sf - 1) + "E", v);
    }

    /** Fixed decimals formatter. */
    public static TickLabelFormatter fixed(int decimals) {
        final int d = Math.max(0, Math.min(12, decimals));
        final String pattern = "0" + (d > 0 ? "." + "0".repeat(d) : "");
        final DecimalFormat df = new DecimalFormat(pattern);
        return df::format;
    }
}
