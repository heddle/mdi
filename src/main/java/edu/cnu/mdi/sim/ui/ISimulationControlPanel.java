package edu.cnu.mdi.sim.ui;

import edu.cnu.mdi.sim.ISimulationHost;

/**
 * Optional capability for control panels that can bind to an ISimulationHost.
 * This allows SimulationView to bind generically without knowing the panel class.
 */
public interface ISimulationControlPanel {
	/**
	 * Bind this panel to a simulation host.
	 *
	 * @param host host whose engine the panel controls
	 */
	void bind(ISimulationHost host);
	/** Release any current host binding. */
	default void unbind() { /* optional */ }
}
