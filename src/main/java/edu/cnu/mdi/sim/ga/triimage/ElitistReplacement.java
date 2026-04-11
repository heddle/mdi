package edu.cnu.mdi.sim.ga.triimage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import edu.cnu.mdi.sim.ga.GASolution;
import edu.cnu.mdi.sim.ga.ReplacementOperator;
/**
 * Elitist replacement operator for genetic algorithms. This operator preserves a specified number of the best 
 * individuals (eliteCount) from the current population and fills the rest of the next generation with offspring.
 * The elite individuals are selected based on their fitness, ensuring that the best solutions are retained across
 *  generations.
 */
public final class ElitistReplacement<C extends GASolution> implements ReplacementOperator<C> {

	private final int eliteCount;

	/**
	 * Create an elitist replacement operator with the given number of elite individuals to preserve.
	 * @param eliteCount the number of best individuals to preserve from the current population
	 */
	public ElitistReplacement(int eliteCount) {
		this.eliteCount = eliteCount;
	}

	@Override
	public List<C> replace(List<C> population, List<C> offspring, double[] popFitness, double[] offFitness,
			Random rng) {
// Find elite indices from current population
		Integer[] idx = IntStream.range(0, population.size()).boxed()
				.sorted((a, b) -> Double.compare(popFitness[b], popFitness[a])).toArray(Integer[]::new);

		List<C> next = new ArrayList<>(population.size());

		for (int i = 0; i < eliteCount && i < population.size(); i++) {
			next.add(population.get(idx[i])); // elite slots: no copy needed
		}
		next.addAll(offspring.subList(0, Math.min(offspring.size(), population.size() - eliteCount)));
		return next;
	}
}