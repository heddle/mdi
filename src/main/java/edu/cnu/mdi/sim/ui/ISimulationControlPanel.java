package edu.cnu.mdi.sim.ui;

import edu.cnu.mdi.sim.ISimulationHost;

/**
 * Optional capability for control panels that can bind to an ISimulationHost.
 * This allows SimulationView to bind generically without knowing the panel class.
 */
public interface ISimulationControlPanel {
	void bind(ISimulationHost host);
	default void unbind() { /* optional */ }
}
