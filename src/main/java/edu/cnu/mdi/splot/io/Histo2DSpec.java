package edu.cnu.mdi.splot.io;

/**
 * Persisted representation of a 2D histogram (heatmap).
 * <p>
 * Stores the bin geometry, bin contents, and over/under/corner counts.
 * </p>
 */
public final class Histo2DSpec {

    public String name;

    // Geometry
    public int nx;
    public int ny;
    public double xmin;
    public double xmax;
    public double ymin;
    public double ymax;

    // Bin contents [nx][ny]
    public double[][] bins;

    // Counts (Histo2DData-like)
    public long goodCount;

    public long xUnderCount;
    public long xOverCount;

    public long yUnderCount;
    public long yOverCount;

    public long xUnder_yUnder;
    public long xUnder_yOver;
    public long xOver_yUnder;
    public long xOver_yOver;
}
