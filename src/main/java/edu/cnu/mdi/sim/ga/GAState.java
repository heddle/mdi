package edu.cnu.mdi.sim.ga;

public record GAState(
	    long   generation,
	    double bestFitness,
	    double meanFitness,
	    double worstFitness,
	    double diversityIndex    // e.g. mean pairwise Hamming distance / length
	) {}