package edu.cnu.mdi.sim.simanneal;

import java.util.EventListener;

public interface IAcceptedMoveListener extends EventListener {

	/**
	 * Callback invoked whenever a proposed move is accepted. 
	 * @param temperature the current temperature
	 * @param energy the current energy, not necessarily the best energy
	 * 		  found so far because accepted moves can be uphill moves.
	 */
	public void acceptedMove(double temperature, double energy);
	
	/**
	 * Callback invoked whenever a new best solution is found.
	 * @param temperature the current temperature
	 * @param energy the new best energy
	 */
	public void newBest(double temperature, double energy);
}
