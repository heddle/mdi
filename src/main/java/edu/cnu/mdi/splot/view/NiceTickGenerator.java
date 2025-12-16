package edu.cnu.mdi.splot.view;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates "nice" axis ticks using a 1-2-5 sequence.
 * <p>
 * Improvements vs. the basic implementation:
 * <ul>
 *   <li>Snaps tick values to exact step multiples to avoid 1.9999999998 style labels</li>
 *   <li>Removes negative zero ("-0") labels</li>
 * </ul>
 * </p>
 */
public class NiceTickGenerator {

    private final int targetMajorCount;
    private final int minorPerMajor;

    public NiceTickGenerator(int targetMajorCount, int minorPerMajor) {
        this.targetMajorCount = Math.max(2, targetMajorCount);
        this.minorPerMajor = Math.max(0, minorPerMajor);
    }

    public TickSet generate(double min, double max) {
        if (!(max > min) || Double.isNaN(min) || Double.isNaN(max) || Double.isInfinite(min) || Double.isInfinite(max)) {
            return new TickSet(List.of(), min, max);
        }

        double span = max - min;
        double rawStep = span / targetMajorCount;
        double majorStep = niceStep(rawStep);

        // Expand nice bounds to multiples of majorStep
        double niceMin = Math.floor(min / majorStep) * majorStep;
        double niceMax = Math.ceil(max / majorStep) * majorStep;

        DecimalFormat fmt = makeFormatter(majorStep);

        List<Tick> ticks = new ArrayList<>();

        // tolerance scaled to the step (prevents label jitter & "-0")
        final double eps = stepEps(majorStep);

        // Major ticks
        for (double v = niceMin; v <= niceMax + 0.5 * majorStep; v += majorStep) {

            double vv = sanitize(snapToStep(v, majorStep, eps), eps);
            ticks.add(new Tick(vv, safeFormat(fmt, vv, eps), true));

            // Minor ticks between v and v+majorStep
            if (minorPerMajor > 1) {
                double minorStep = majorStep / minorPerMajor;
                final double epsMinor = stepEps(minorStep);

                for (int k = 1; k < minorPerMajor; k++) {
                    double vm = v + k * minorStep;
                    if (vm > niceMax + 1e-12) break;

                    double vmm = sanitize(snapToStep(vm, minorStep, epsMinor), epsMinor);
                    ticks.add(new Tick(vmm, "", false));
                }
            }
        }

        // Trim ticks slightly outside min/max (keeps axis clean)
        ticks.removeIf(t -> t.getValue() < min - 1e-12 || t.getValue() > max + 1e-12);

        return new TickSet(ticks, min, max);
    }

    private static double niceStep(double x) {
        double exp = Math.floor(Math.log10(x));
        double f = x / Math.pow(10.0, exp);

        double nf;
        if (f <= 1.0) nf = 1.0;
        else if (f <= 2.0) nf = 2.0;
        else if (f <= 5.0) nf = 5.0;
        else nf = 10.0;

        return nf * Math.pow(10.0, exp);
    }

    private static DecimalFormat makeFormatter(double step) {
        double abs = Math.abs(step);

        if (abs >= 1.0) {
            // Still allow decimals if step is not an integer (rare but possible)
            if (isNearlyInteger(abs, stepEps(step))) {
                return new DecimalFormat("0");
            }
            return new DecimalFormat("0.########");
        }

        int decimals = (int) Math.ceil(-Math.log10(abs)) + 1;
        decimals = Math.min(10, Math.max(0, decimals));

        StringBuilder p = new StringBuilder("0");
        if (decimals > 0) {
            p.append(".");
            p.append("0".repeat(decimals));
        }
        return new DecimalFormat(p.toString());
    }

    private static String safeFormat(DecimalFormat fmt, double v, double eps) {
        double vv = sanitize(v, eps);
        String s = fmt.format(vv);

        // DecimalFormat can still emit "-0" in some locales/edge cases
        if (s.equals("-0") || s.equals("-0.0") || s.equals("-0.00") || s.equals("-0.000")) {
            return s.replace("-", "");
        }
        return s;
    }

    /**
     * Snap v to the nearest multiple of step if it is already very close.
     */
    private static double snapToStep(double v, double step, double eps) {
        if (!(step > 0)) return v;
        double k = Math.rint(v / step);     // nearest integer
        double snapped = k * step;
        return (Math.abs(snapped - v) <= eps) ? snapped : v;
    }

    /**
     * Coerce very small magnitudes to +0.0 to avoid "-0".
     */
    private static double sanitize(double v, double eps) {
        return (Math.abs(v) <= eps) ? 0.0 : v;
    }

    private static double stepEps(double step) {
        // relative + absolute tolerance
        double s = Math.abs(step);
        return Math.max(1e-15, s * 1e-12);
    }

    private static boolean isNearlyInteger(double v, double eps) {
        return Math.abs(v - Math.rint(v)) <= eps;
    }
}
