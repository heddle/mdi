package edu.cnu.mdi.splot.view;

/**
 * Tick label formatter that uses plain formatting for normal magnitudes and
 * switches to scientific notation when values are very large or very small.
 *
 * <p>Rules:
 * <ul>
 *   <li>If |v| >= largeThreshold -> scientific</li>
 *   <li>If 0 < |v| <= smallThreshold -> scientific</li>
 *   <li>Otherwise -> plain (%.6g-like)</li>
 *   <li>Values extremely close to 0 are formatted as "0"</li>
 * </ul>
 * </p>
 */
public class SmartScientificFormatter implements TickLabelFormatter {

    private final double largeThreshold;
    private final double smallThreshold;
    private final int sigFigs;
    private final double zeroEps;

    /**
     * @param largeThreshold switch to scientific when |v| >= this (e.g. 1e4)
     * @param smallThreshold switch to scientific when 0 < |v| <= this (e.g. 1e-3)
     * @param sigFigs number of significant figures in scientific output (e.g. 3)
     */
    public SmartScientificFormatter(double largeThreshold, double smallThreshold, int sigFigs) {
        if (!(largeThreshold > 0)) throw new IllegalArgumentException("largeThreshold must be > 0");
        if (!(smallThreshold > 0)) throw new IllegalArgumentException("smallThreshold must be > 0");
        if (smallThreshold >= largeThreshold) throw new IllegalArgumentException("smallThreshold must be < largeThreshold");

        this.largeThreshold = largeThreshold;
        this.smallThreshold = smallThreshold;
        this.sigFigs = Math.max(1, Math.min(12, sigFigs));

        // For cleaning up "-0" and tiny magnitudes:
        this.zeroEps = 1e-12 * smallThreshold;
    }

    @Override
    public String format(double value) {
        double v = sanitizeZero(value);

        if (v == 0.0) {
            return "0";
        }

        double a = Math.abs(v);
        boolean sci = (a >= largeThreshold) || (a <= smallThreshold);

        if (sci) {
            // Scientific: "1.23E4" style
            // sigFigs -> decimals = sigFigs-1 in E-format
            return sanitizeMinusZero(String.format("%." + (sigFigs - 1) + "E", v));
        }

        // Plain: compact general format
        // Use ~6 significant digits; this keeps axis labels readable.
        return sanitizeMinusZero(String.format("%.6g", v));
    }

    private double sanitizeZero(double v) {
        return (Math.abs(v) <= zeroEps) ? 0.0 : v;
    }

    private static String sanitizeMinusZero(String s) {
        // Catch common "-0" outputs (plain or scientific)
        if (s.startsWith("-0")) {
            // "-0", "-0.0", "-0.00", "-0E0", "-0.0E0", etc.
            // If everything before 'E' is zero-ish, drop leading '-'.
            int e = s.indexOf('E');
            String mant = (e >= 0) ? s.substring(0, e) : s;

            // Remove sign and punctuation and zeros; if empty -> it's zero
            String test = mant.replace("-", "").replace("+", "").replace(".", "").replace("0", "");
            if (test.isEmpty()) {
                return s.substring(1); // drop '-'
            }
        }
        return s;
    }
}
