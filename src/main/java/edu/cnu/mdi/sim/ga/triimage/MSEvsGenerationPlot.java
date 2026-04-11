package edu.cnu.mdi.sim.ga.triimage;

import java.awt.Color;

import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.sim.ga.GAState;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotPanel;
import edu.cnu.mdi.splot.plot.PlotParameters;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Plot of MSE vs generation (log x-axis) for the image evolution GA demo.
 * <p>
 * Two curves are shown:
 * </p>
 * <ul>
 *   <li><b>Best MSE</b> — the best (lowest) MSE found so far. Monotonically
 *       non-increasing by definition.</li>
 *   <li><b>Mean MSE</b> — population mean MSE. Tracks best MSE closely in a
 *       converged population; diverges when diversity is high.</li>
 * </ul>
 * <p>
 * The gap between mean and best is a useful diversity proxy: a large gap
 * means the population is diverse; a gap near zero means it has converged.
 * </p>
 */
@SuppressWarnings("serial")
public class MSEvsGenerationPlot extends PlotPanel {

    private static final String TITLE  = "MSE vs Generation";
    private static final String XLABEL = "Generation";
    private static final String YLABEL = "MSE";

    private static final String BEST_CURVE = "Best MSE";
    private static final String MEAN_CURVE = "Mean MSE";

    private final Curve bestCurve;
    private final Curve meanCurve;

    public MSEvsGenerationPlot() {
        super(createCanvas());

        PlotData plotData = getPlotCanvas().getPlotData();
        this.bestCurve = (Curve) plotData.getCurve(BEST_CURVE);
        this.meanCurve = (Curve) plotData.getCurve(MEAN_CURVE);

        setParameters();
    }

    private void setParameters() {
        PlotParameters params = getPlotCanvas().getParameters();

        // Log x-axis — equal visual weight per decade of generations
        params.setXScale(PlotParameters.AxisScale.LOG10);
        params.setNumDecimalX(0);
        params.setTitleFont(Fonts.plainFontDelta(2));

        setCurveStyle(bestCurve, SymbolType.CIRCLE,
                X11Colors.getX11Color("Cadet Blue"), Color.blue);

        setCurveStyle(meanCurve, SymbolType.CIRCLE,
                X11Colors.getX11Color("Light Coral"), Color.red);
    }

    private void setCurveStyle(Curve curve, SymbolType stype,
                                Color symbolColor, Color border) {
        IStyled style = curve.getStyle();
        style.setSymbolType(stype);
        style.setSymbolSize(4);
        style.setFillColor(symbolColor);
        style.setBorderColor(border);
    }

    /**
     * Add a data point from the current GA state.
     * <p>
     * Must be called on the EDT. Typically called from
     * {@code onSimulationRefresh} in the view.
     * </p>
     *
     * @param state current GA state
     */
    public void update(GAState state) {
        if (state.generation() <= 0) return;
        double mse     = -state.bestFitness();
        double meanMse = -state.meanFitness();
        bestCurve.add(state.generation(), mse);
        meanCurve.add(state.generation(), meanMse);
        getPlotCanvas().repaint();
    }

    /** Clear all curve data. */
    public void clearData() {
        bestCurve.clearData();
        meanCurve.clearData();
        getPlotCanvas().repaint();
    }

    private static PlotCanvas createCanvas() {
        try {
            PlotData pd = new PlotData(PlotDataType.XYEXYE,
                    new String[]{ BEST_CURVE, MEAN_CURVE }, null);
            return new PlotCanvas(pd, TITLE, XLABEL, YLABEL);
        } catch (PlotDataException e) {
            e.printStackTrace();
            return null;
        }
    }
}