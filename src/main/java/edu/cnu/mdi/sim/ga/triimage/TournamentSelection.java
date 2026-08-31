package edu.cnu.mdi.sim.ga.triimage;

import java.util.List;
import java.util.Random;
import java.util.Objects;

import edu.cnu.mdi.sim.ga.GASolution;
import edu.cnu.mdi.sim.ga.SelectionOperator;

/**
 * Tournament selection operator for genetic algorithms. This operator selects parents by running a
 * "tournament" among a random subset of the population.
 * The tournament size is a parameter that controls the selection pressure: larger tournaments
 * favor fitter individuals more strongly.
 */
public final class TournamentSelection<C extends GASolution<C>>
        implements SelectionOperator<C> {

	// The number of individuals to compete in each tournament
    private final int tournamentSize;

    /**
	 * Create a tournament selection operator with the given tournament size.
	 * @param tournamentSize the number of individuals to compete in each tournament
	 */
    public TournamentSelection(int tournamentSize) {
		if (tournamentSize <= 0) {
			throw new IllegalArgumentException("tournamentSize must be > 0");
		}
        this.tournamentSize = tournamentSize;
    }

    @Override
    public C select(List<C> population, double[] fitnesses, Random rng) {
		Objects.requireNonNull(population, "population");
		Objects.requireNonNull(fitnesses, "fitnesses");
		Objects.requireNonNull(rng, "rng");
		if (population.isEmpty()) {
			throw new IllegalArgumentException("population must not be empty");
		}
		if (fitnesses.length != population.size()) {
			throw new IllegalArgumentException(
					"fitnesses length must match population size");
		}
        int best = rng.nextInt(population.size());
        for (int i = 1; i < tournamentSize; i++) {
            int challenger = rng.nextInt(population.size());
            if (fitnesses[challenger] > fitnesses[best]) {
                best = challenger;
            }
        }
        return population.get(best);
    }
}
