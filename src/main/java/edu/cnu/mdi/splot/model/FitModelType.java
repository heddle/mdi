package edu.cnu.mdi.splot.model;

/**
 * Common fit model types. Use {@link #CUSTOM} for user-defined parametric models.
 *
 * This is intentionally a light enum; the authoritative model definition is in
 * {@link FitSpec} and the evaluator function in {@link FitResult}.
 */
public enum FitModelType {
    POLYNOMIAL,
    GAUSSIAN,
    HARMONIC,
    EXPONENTIAL,
    LOGISTIC,
    POWER,
    CUSTOM
}
