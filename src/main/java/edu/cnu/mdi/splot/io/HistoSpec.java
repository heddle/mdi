package edu.cnu.mdi.splot.io;

/**
 * Persisted 1D histogram payload.
 */
public final class HistoSpec {
    public double[] grid;    // length n+1
    public long[] counts;    // length n
    public long underCount;
    public long overCount;
}
