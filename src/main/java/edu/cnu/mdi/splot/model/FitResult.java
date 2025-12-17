package edu.cnu.mdi.splot.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

/**
 * Result of a curve fit (model-layer; graphics-free).
 */
public final class FitResult {

    private final FitSpec spec;
    private final double[] parameters;
    private final DoubleUnaryOperator evaluator;
    private final double xMin;
    private final double xMax;
    private final FitDiagnostics diagnostics;

    public FitResult(FitSpec spec,
                     double[] parameters,
                     DoubleUnaryOperator evaluator,
                     double xMin,
                     double xMax,
                     FitDiagnostics diagnostics) {

        this.spec = Objects.requireNonNull(spec, "spec");
        this.parameters = Objects.requireNonNull(parameters, "parameters").clone();
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.xMin = xMin;
        this.xMax = xMax;
        this.diagnostics = diagnostics;
    }

    public FitSpec getSpec() {
        return spec;
    }

    public double[] getParameters() {
        return parameters.clone();
    }

    public int getParameterCount() {
        return parameters.length;
    }

    public DoubleUnaryOperator getEvaluator() {
        return evaluator;
    }

    public double getXMin() {
        return xMin;
    }

    public double getXMax() {
        return xMax;
    }

    public FitDiagnostics getDiagnostics() {
        return diagnostics;
    }

    public double f(double x) {
        return evaluator.applyAsDouble(x);
    }

    public double getParameter(int index) {
        return parameters[index];
    }

    public double getParameter(String name) {
        Objects.requireNonNull(name, "name");
        List<String> names = spec.getParameterNames();
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("FitSpec has no parameter names");
        }
        for (int i = 0; i < names.size() && i < parameters.length; i++) {
            if (name.equals(names.get(i))) {
                return parameters[i];
            }
        }
        throw new IllegalArgumentException("Unknown parameter: " + name);
    }

    public String formatSummaryOneLine() {
        String model = spec.getModelName();
        StringBuilder sb = new StringBuilder();
        sb.append((model == null || model.isEmpty()) ? "Fit" : model);

        List<String> names = spec.getParameterNames();
        if (names == null || names.isEmpty()) {
            sb.append(": ").append(Arrays.toString(parameters));
            return sb.toString();
        }

        sb.append(": ");
        for (int i = 0; i < names.size() && i < parameters.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(names.get(i)).append("=").append(FitText.formatNumber(parameters[i]));
        }
        return sb.toString();
    }

    public List<String> summaryLines() {
        List<String> lines = new ArrayList<>();
        String model = spec.getModelName();
        lines.add((model == null || model.isEmpty()) ? "Fit" : model);

        List<String> names = spec.getParameterNames();
        double[] errs = (diagnostics == null) ? null : diagnostics.getParameterErrorsOrNull();

        if (names == null || names.isEmpty()) {
            lines.add("params: " + Arrays.toString(parameters));
        } else {
            for (int i = 0; i < names.size() && i < parameters.length; i++) {
                String v = FitText.formatNumber(parameters[i]);
                if (errs != null && i < errs.length && Double.isFinite(errs[i]) && errs[i] >= 0) {
                    String e = FitText.formatNumber(errs[i]);
                    lines.add(names.get(i) + " = " + v + " ± " + e);
                } else {
                    lines.add(names.get(i) + " = " + v);
                }
            }
        }

        if (diagnostics != null && diagnostics.hasChi2()) {
            String chi = FitText.formatNumber(diagnostics.getChi2());
            if (diagnostics.getNdof() > 0) {
                lines.add("χ²/ndf = " + FitText.formatNumber(diagnostics.getChi2() / diagnostics.getNdof())
                        + "   (χ²=" + chi + ", ndf=" + diagnostics.getNdof() + ")");
            } else {
                lines.add("χ² = " + chi);
            }
        }

        if (Double.isFinite(xMin) || Double.isFinite(xMax)) {
            lines.add("fit range: [" + FitText.formatNumber(xMin) + ", " + FitText.formatNumber(xMax) + "]");
        }

        return lines;
    }

    @Override
    public String toString() {
        return "FitResult[" + spec + ", params=" + Arrays.toString(parameters)
                + ", xRange=[" + xMin + "," + xMax + "], diagnostics=" + diagnostics + "]";
    }
}
