package edu.cnu.mdi.splot.model;

import java.util.Arrays;

/**
 * Optional diagnostics produced by a fitter: goodness-of-fit and parameter uncertainty.
 * Model-layer information useful for display/export, independent of graphics.
 */
public final class FitDiagnostics {

    private final double chi2;
    private final int dof;
    private final double reducedChi2;
    private final double[] parameterStdErr; // may be null if not available

    /**
     * @param chi2 chi-square (or SSE for unweighted fits)
     * @param dof degrees of freedom (typically n - p)
     * @param reducedChi2 chi2/dof (NaN if dof <= 0)
     * @param parameterStdErr optional parameter standard errors (may be null)
     */
    public FitDiagnostics(double chi2, int dof, double reducedChi2, double[] parameterStdErr) {
        this.chi2 = chi2;
        this.dof = dof;
        this.reducedChi2 = reducedChi2;
        this.parameterStdErr = (parameterStdErr == null) ? null : parameterStdErr.clone();
    }

    public double getChi2() {
        return chi2;
    }

    /** Alias used by FitResult summary code. */
    public int getNdof() {
        return dof;
    }

    public int getDof() {
        return dof;
    }

    public double getReducedChi2() {
        return reducedChi2;
    }

    /** @return a defensive copy of parameter standard errors, or null if unavailable */
    public double[] getParameterStdErr() {
        return (parameterStdErr == null) ? null : parameterStdErr.clone();
    }

    /** Alias used by FitResult summary code. */
    public double[] getParameterErrorsOrNull() {
        return (parameterStdErr == null) ? null : parameterStdErr.clone();
    }

    public boolean hasChi2() {
        return Double.isFinite(chi2);
    }

    @Override
    public String toString() {
        return "FitDiagnostics[chi2=" + chi2 + ", dof=" + dof + ", redChi2=" + reducedChi2
                + ", stderr=" + (parameterStdErr == null ? "null" : Arrays.toString(parameterStdErr)) + "]";
    }
}
