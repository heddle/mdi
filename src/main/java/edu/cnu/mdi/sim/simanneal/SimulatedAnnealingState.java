package edu.cnu.mdi.sim.simanneal;

/**
 * Immutable state snapshot of a simulated annealing process.
 *
 * @param step number of completed annealing iterations
 * @param temperature absolute temperature used for the next iteration
 * @param currentEnergy energy of the current solution
 * @param bestEnergy lowest energy observed during the run
 * @param acceptedMoves total number of accepted moves
 * @param uphillAcceptedMoves number of accepted moves that increased energy
 */
public record SimulatedAnnealingState(
        long step,
        double temperature,
        double currentEnergy,
        double bestEnergy,
        long acceptedMoves,
        long uphillAcceptedMoves
) {}
