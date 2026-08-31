package edu.cnu.mdi.splot.plot;

/** Entry points for concise construction of native MDI plots. */
public final class PlotBuilders {

    private PlotBuilders() { }

    /**
     * Starts an XY plot builder.
     *
     * @param title plot title
     * @return new builder
     */
    public static XYPlotBuilder xy(String title) {
        return new XYPlotBuilder(title);
    }

    /**
     * Builds a one-series XY plot with standard decorations and labels.
     *
     * @param title plot title
     * @param xLabel horizontal-axis label
     * @param yLabel vertical-axis label
     * @param seriesName legend name
     * @param x x coordinates
     * @param y y coordinates
     * @return native plot panel
     */
    public static PlotPanel xy(String title, String xLabel, String yLabel,
            String seriesName, double[] x, double[] y) {
        return xy(title).axes(xLabel, yLabel).series(seriesName, x, y).build();
    }
}
