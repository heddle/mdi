package edu.cnu.mdi.sim.ga;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple list-backed implementation of GAPopulation.
 * This is the standard concrete population used by most GA problems.
 */
public final class SimpleGAPopulation<C extends GASolution> implements GAPopulation<C> {

    private final List<C> individuals;

    private SimpleGAPopulation(List<C> individuals) {
        this.individuals = individuals;
    }

    public static <C extends GASolution> SimpleGAPopulation<C> of(List<C> individuals) {
        return new SimpleGAPopulation<>(individuals);
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