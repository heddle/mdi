package edu.cnu.mdi.sim.demo.network;

import edu.cnu.mdi.splot.plot.MultiplotPanel;

@SuppressWarnings("serial")
public class DiagnosticPlotPanel extends MultiplotPanel {

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
