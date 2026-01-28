package edu.cnu.mdi.sim.demo.network;

import edu.cnu.mdi.splot.plot.MultiplotPanel;

@SuppressWarnings("serial")
public class DiagnosticPlotPanel extends MultiplotPanel {

	private EnergyVsStep energyVsStep;

	public DiagnosticPlotPanel() {
		super(true);
		energyVsStep = new EnergyVsStep();

		addPlot("Energy Vs Step", energyVsStep);
	}


	/**
	 * Add new diagnostic data point to plots.
	 *
	 * @param d diagnostic data (from the simulation)
	 */
	protected void newDiagnosticData(NetworkDeclutterSimulation.Diagnostics d) {
        int step = d.step;

         energyVsStep.updatePlot(d);

//        energyChart.addPoint("Total", step, d.total());
//        energyChart.addPoint("Potential", step, d.potential());
//        energyChart.addPoint("Kinetic", step, d.Kinetic);

        // Optional but VERY informative:
//        energyChart.addPoint("avgSpeed", step, d.avgSpeed);
//        energyChart.addPoint("Frms", step, d.Frms);
//        energyChart.addPoint("vmaxFrac", step, d.vmaxHitFraction);

	}


}
