package edu.cnu.mdi.sim.simanneal.tspdemo;

/**
 * Capability interface implemented by {@link TspDemoView} so that a control panel
 * can request a model/simulation reset without knowing the concrete view type.
 */
public interface ITspDemoResettable {

	/**
	 * Request a reset/reinitialization of the demo with the provided parameters.
	 * <p>
	 * Implementations should only honor this request when the simulation is in
	 * a safe state (typically READY or TERMINATED).
	 * </p>
	 *
	 * @param cityCount    number of cities (e.g. 10..500)
	 * @param riverPenalty penalty/bonus for crossing the river (e.g. -1..+1)
	 */
	void requestReset(int cityCount, double riverPenalty);
}
