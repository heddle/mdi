package edu.cnu.mdi.sim.demo.network;

public interface IResettable {

	/**
	 * Request a reset/reinitialization of the demo with the provided parameters.
	 * <p>
	 * Implementations should only honor this request when the simulation is in
	 * a safe state (typically READY or TERMINATED).
	 * </p>
	 *
	 * @param numServers  number of servers
	 * @param numClients  number of clients
	 * @param numPrinters number of printers
	 */
	void requestReset(int numServers, int numClients, int numPrinters);
}
