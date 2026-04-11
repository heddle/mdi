package edu.cnu.mdi.sim.ga.triimage;

import edu.cnu.mdi.sim.ga.GAState;
import edu.cnu.mdi.splot.plot.MultiplotPanel;

/**
 * Diagnostics panel for the image evolution GA demo.
 * Currently hosts the MSE vs generation plot; extend with
 * additional plots as needed.
 */
@SuppressWarnings("serial")
public class GADiagnosticPlotPanel extends MultiplotPanel {

    private final MSEvsGenerationPlot msePlot = new MSEvsGenerationPlot();

    public GADiagnosticPlotPanel() {
        super(true);
        addPlot("MSE vs Generation", msePlot);
    }

    /**
     * Add a new data point. Called from the view on each refresh.
     * Must be called on the EDT.
     *
     * @param state current GA state
     */
    public void update(GAState state) {
        msePlot.update(state);
    }

    /** Clear all plot data — call before a reset. */
    public void clearAllPlots() {
        msePlot.clearData();
    }
}