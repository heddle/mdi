package edu.cnu.mdi.sim.simanneal;

/**
 * State snapshot of a simulated annealing process.
 */
public record SimulatedAnnealingState(
        long step, // current step index
        double temperature, // current temperature
        double currentEnergy, // current energy
        double bestEnergy, // best (lowest) energy found so far
        long acceptedMoves, // total accepted moves
        long uphillAcceptedMoves // total accepted uphill moves
) {}
