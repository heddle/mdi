package edu.cnu.mdi.sim.demo.network;

import java.awt.Dimension;

import javax.swing.JViewport;

import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.splot.plot.MultiplotPanel;
import edu.cnu.mdi.splot.plot.PlotPanel;

@SuppressWarnings("serial")
public class DiagnosticPlotPanel extends MultiplotPanel {

	/** Lines of feedback shown under each plot. */
	private static final int FEEDBACK_LINES = 6;

	// the individual plots
	private EnergyVsStep energyVsStep;
	private ConvergenceVsStep convergenceVsStep;
	private MinPairwiseSeparation minPairwiseDistanceVsStep;

	/**
	 * Create the diagnostic plot panel.This will contain multiple plots.
	 */
	public DiagnosticPlotPanel() {
		super(true);
		energyVsStep = new EnergyVsStep();
		convergenceVsStep = new ConvergenceVsStep();
		minPairwiseDistanceVsStep = new MinPairwiseSeparation();

		addPlot("Energy Vs Step", energyVsStep);
		addPlot("Convergence Metrics Vs Step", convergenceVsStep);
		addPlot("Min Pairwise Separation Vs Step", minPairwiseDistanceVsStep);

		showMoreFeedback(energyVsStep);
		showMoreFeedback(convergenceVsStep);
		showMoreFeedback(minPairwiseDistanceVsStep);
	}

	// These plots report several curves, more than the usual three lines of
	// feedback can show. The plot above gets what is left.
	private static void showMoreFeedback(PlotPanel plot) {
		FeedbackPane feedback = plot.getFeedbackPane();
		JViewport viewport = feedback.getViewport();
		int lineHeight = feedback.getFontMetrics(feedback.getFont()).getHeight() + 2;
		viewport.setPreferredSize(new Dimension(viewport.getPreferredSize().width,
				FEEDBACK_LINES * lineHeight + 4));
	}


	/**
	 * Add new diagnostic data point to plots.
	 *
	 * @param d diagnostic data (from the simulation)
	 */
	protected void newDiagnosticData(NetworkDeclutterSimulation.Diagnostics d) {
         energyVsStep.updatePlot(d);
         convergenceVsStep.updatePlot(d);
         minPairwiseDistanceVsStep.updatePlot(d);
	}


}
