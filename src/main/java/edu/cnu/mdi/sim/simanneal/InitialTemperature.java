package edu.cnu.mdi.sim.simanneal;

/**
 * Result of estimating the initial annealing temperature.
 *
 * @param T0 positive absolute initial temperature
 * @param energyMedian median energy in the heuristic's sample
 * @param energyMad median absolute deviation of sampled energies
 * @param samples number of solutions sampled
 */
public record InitialTemperature(
        double T0,
        double energyMedian,
        double energyMad,
        int samples
) {}
