package edu.cnu.mdi.sim.demo.forestfire;

/**
 * Reset hook for the forest fire demo view.
 */
public interface IForestFireResettable {
    /**
     * Reset with new parameters.
     */
    void requestReset(int width, int height, double pSpread);
}
