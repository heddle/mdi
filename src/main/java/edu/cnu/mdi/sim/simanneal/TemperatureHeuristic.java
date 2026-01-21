package edu.cnu.mdi.sim.simanneal;

import java.util.Random;

public interface TemperatureHeuristic<S extends AnnealingSolution> {
    InitialTemperature estimate(AnnealingProblem<S> problem, Random rng);
}
