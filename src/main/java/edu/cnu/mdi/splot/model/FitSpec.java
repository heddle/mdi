package edu.cnu.mdi.splot.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Describes the fit model configuration used to produce a {@link FitResult}.
 * This makes fit results reproducible and self-describing.
 */
public final class FitSpec {

    private final FitModelType modelType;
    private final String modelId; // optional extra identifier; useful for CUSTOM or variants
    private final List<String> parameterNames;
    private final int polynomialDegree; // meaningful for POLYNOMIAL; otherwise -1

    private FitSpec(FitModelType modelType, String modelId, List<String> parameterNames, int polynomialDegree) {
        this.modelType = Objects.requireNonNull(modelType, "modelType");
        this.modelId = (modelId == null) ? "" : modelId;
        this.parameterNames = Collections.unmodifiableList(Objects.requireNonNull(parameterNames, "parameterNames"));
        this.polynomialDegree = polynomialDegree;
    }

    public static FitSpec gaussian() {
        return new FitSpec(FitModelType.GAUSSIAN, "gaussian", List.of("A", "mu", "sigma"), -1);
    }

    public static FitSpec harmonic() {
        return new FitSpec(FitModelType.HARMONIC, "harmonic", List.of("A", "omega", "phi"), -1);
    }

    public static FitSpec polynomial(int degree) {
        if (degree < 0) throw new IllegalArgumentException("degree must be >= 0");
        String[] names = new String[degree + 1];
        for (int i = 0; i <= degree; i++) names[i] = "a" + i;
        return new FitSpec(FitModelType.POLYNOMIAL, "poly-" + degree, List.of(names), degree);
    }

    public static FitSpec custom(String modelId, List<String> parameterNames) {
        return new FitSpec(
                FitModelType.CUSTOM,
                (modelId == null) ? "custom" : modelId,
                (parameterNames == null) ? List.of() : parameterNames,
                -1
        );
    }

    public static FitSpec custom(String modelId) {
        return custom(modelId, java.util.List.of());
    }

    public FitModelType getModelType() {
        return modelType;
    }

    public String getModelId() {
        return modelId;
    }

    /** Parameter names (immutable). */
    public List<String> getParameterNames() {
        return parameterNames;
    }

    /** Convenience for legacy callers that want an array. */
    public String[] getParameterNamesArray() {
        return parameterNames.toArray(new String[0]);
    }

    public int getPolynomialDegree() {
        return polynomialDegree;
    }

    /**
     * A human-friendly model name suitable for legends and summaries.
     *
     * @return model name
     */
    public String getModelName() {
        return switch (modelType) {
            case GAUSSIAN -> "Gaussian";
            case HARMONIC -> "Harmonic";
            case POLYNOMIAL -> "Polynomial (deg " + polynomialDegree + ")";
            case EXPONENTIAL -> "Exponential";
            case LOGISTIC -> "Logistic";
            case POWER -> "Power";
            case CUSTOM -> (modelId == null || modelId.isBlank()) ? "Custom" : modelId;
        };
    }

    @Override
    public String toString() {
        return "FitSpec[type=" + modelType + ", id=" + modelId + ", params=" + parameterNames + "]";
    }
}
