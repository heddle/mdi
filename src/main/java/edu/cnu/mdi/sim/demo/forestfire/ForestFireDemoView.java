package edu.cnu.mdi.sim.demo.forestfire;

import java.util.Random;

import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.ui.SimulationView;

public class ForestFireDemoView extends SimulationView {

	// Default grid dimensions for the forest fire simulation
	public static final int DEFAULT_GRID_WIDTH = 50;
	public static final int DEFAULT_GRID_HEIGHT = 50;

	// the critical value of pSpread is around 0.592, where the fire transitions
	// from dying out quickly to spreading across the entire forest
	public static final double DEFAULT_P_SPREAD = 0.6;

	// The simulation instance that this view will display
	private ForestFireSimulation sim;

	public ForestFireDemoView(Object... keyVals) {
		super(createSimulation(), 
				new SimulationEngineConfig(60, 250, 60, false), 
				true,
				(SimulationView.ControlPanelFactory) ForestFireControlPanel::new, 
				keyVals);
	}

	// Helper to create the simulation instance
	private static ForestFireSimulation createSimulation() {
		ForestFireModel m = ForestFireModel.random(DEFAULT_GRID_WIDTH, 
				DEFAULT_GRID_HEIGHT, DEFAULT_P_SPREAD,
				new Random());

		return new ForestFireSimulation(m);
	}

}
