package edu.cnu.mdi.sim.ga;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A simple list-backed implementation of GAPopulation.
 * This is the standard concrete population used by most GA problems.
 */
public final class SimpleGAPopulation<C extends GASolution> implements GAPopulation<C> {

    private final List<C> individuals;

    private SimpleGAPopulation(List<C> individuals) {
        this.individuals = individuals;
    }

	/**
	 * Create an immutable-list-backed population from the supplied individuals.
	 * The individual objects themselves are not copied.
	 *
	 * @param <C> solution type
	 * @param individuals non-null individuals
	 * @return new population
	 */
    public static <C extends GASolution> SimpleGAPopulation<C> of(List<C> individuals) {
		Objects.requireNonNull(individuals, "individuals");
		if (individuals.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("individuals must not contain null");
		}
        return new SimpleGAPopulation<>(List.copyOf(individuals));
    }

    @Override
    public List<C> individuals() {
        return individuals;
    }

    @Override
    public int size() {
        return individuals.size();
    }

    @Override
    public GAPopulation<C> copy() {
        List<C> copied = new ArrayList<>(individuals.size());
        for (C individual : individuals) {
            @SuppressWarnings("unchecked")
            C c = (C) individual.copy();
            copied.add(c);
        }
        return new SimpleGAPopulation<>(copied);
    }
}
