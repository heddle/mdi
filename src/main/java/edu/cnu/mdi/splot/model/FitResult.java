package edu.cnu.mdi.splot.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

/**
 * Result of a curve fit. Contains:
 * <ul>
 *   <li>{@link FitSpec} describing the model</li>
 *   <li>final fit parameters</li>
 *   <li>an evaluator function f(x) usable by renderers</li>
 *   <li>the fit domain (xMin..xMax) used when fitting (important for drawing)</li>
 *   <li>optional diagnostics (chi2, covariance-derived errors, etc.)</li>
 * </ul>
 *
 * This is graphics-free and does not depend on Apache Commons Math types,
 * but it is designed to be populated from Commons Math fitting outputs.
 */
public final class FitResult {

    private final FitSpec spec;
    private final double[] parameters;
    private final DoubleUnaryOperator evaluator;
    private final double xMin;
    private final double xMax;
    private final FitDiagnostics diagnostics;

    /**
     * Create a fit result.
     *
     * @param spec model spec (non-null)
     * @param parameters parameter vector (non-null; defensively copied)
     * @param evaluator function f(x) that evaluates the fit (non-null)
     * @param xMin minimum x used for the fit (may be -Inf if unknown)
     * @param xMax maximum x used for the fit (may be +Inf if unknown)
     * @param diagnostics optional diagnostics (may be null)
     */
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

    /** @return defensive copy of parameter vector */
    public double[] getParameters() {
        return parameters.clone();
    }

    /** @return evaluator function f(x) */
    public DoubleUnaryOperator getEvaluator() {
        return evaluator;
    }

    /** @return min x used when fitting (or -Inf if unknown) */
    public double getXMin() {
        return xMin;
    }

    /** @return max x used when fitting (or +Inf if unknown) */
    public double getXMax() {
        return xMax;
    }

    /** @return optional diagnostics; may be null */
    public FitDiagnostics getDiagnostics() {
        return diagnostics;
    }

    /** Convenience. */
    public double f(double x) {
        return evaluator.applyAsDouble(x);
    }

    @Override
    public String toString() {
        return "FitResult[" + spec + ", params=" + Arrays.toString(parameters)
                + ", xRange=[" + xMin + "," + xMax + "], diagnostics=" + diagnostics + "]";
    }
}
