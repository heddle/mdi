package edu.cnu.mdi.splot.model;

import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Small helpers to adapt Apache Commons Math fitting outputs to {@link FitResult}.
 * Keep these in a separate package if you want your core model to be Apache-free.
 */
public final class CommonsMathFitAdapters {

    private CommonsMathFitAdapters() {
        // no instantiation
    }

    /**
     * Create a {@link FitResult} from a Commons Math parametric function and fitted parameters.
     *
     * @param spec your model spec
     * @param parametric commons-math parametric function
     * @param parameters fitted parameters
     * @param xMin fit domain min
     * @param xMax fit domain max
     * @param diagnostics optional diagnostics (may be null)
     */
    public static FitResult fromParametric(FitSpec spec,
                                          ParametricUnivariateFunction parametric,
                                          double[] parameters,
                                          double xMin,
                                          double xMax,
                                          FitDiagnostics diagnostics) {

        Objects.requireNonNull(parametric, "parametric");

        DoubleUnaryOperator eval = x -> parametric.value(x, parameters);
        return new FitResult(spec, parameters, eval, xMin, xMax, diagnostics);
    }

    /**
     * Create a {@link FitResult} from a Commons Math {@link UnivariateFunction}.
     * Useful for polynomials and other non-parametric function objects.
     */
    public static FitResult fromUnivariate(FitSpec spec,
                                          UnivariateFunction function,
                                          double[] parameters,
                                          double xMin,
                                          double xMax,
                                          FitDiagnostics diagnostics) {

        Objects.requireNonNull(function, "function");

        DoubleUnaryOperator eval = function::value;
        return new FitResult(spec, parameters, eval, xMin, xMax, diagnostics);
    }
}
