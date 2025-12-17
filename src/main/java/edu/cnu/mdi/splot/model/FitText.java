package edu.cnu.mdi.splot.model;

/** Small formatting helpers for fit summaries (graphics-free). */
public final class FitText {

    private FitText() {}

    public static String formatNumber(double v) {
        double vv = (Math.abs(v) < 1e-15) ? 0.0 : v;
        String s = String.format("%.6g", vv);

        // scrub "-0" variants
        if (s.startsWith("-0")) {
            String t = s.replace("-", "").replace("+", "").replace(".", "").replace("0", "");
            if (t.isEmpty()) s = s.substring(1);
        }
        return s;
    }
}
