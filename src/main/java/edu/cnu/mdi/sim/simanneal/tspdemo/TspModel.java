package edu.cnu.mdi.sim.simanneal.tspdemo;

import java.awt.geom.Point2D;
import java.util.Random;
import java.util.Objects;

/**
 * Model for the Traveling Salesman Problem (TSP), optionally including
 * a vertical "river" that adds a penalty (or bonus) when crossed.
 */
public class TspModel {

    public final int cityCount;
    public final boolean includeRiver;
    public final double riverX;

    /** Penalty (positive) or bonus (negative) for crossing the river. */
    private double riverPenalty;

    /** Whether river crossings are currently enabled. */
    private boolean riverEnabled = true;

    public final Point2D.Double[] cities;

	/**
	 * Create a randomized TSP model.
	 *
	 * @param cityCount number of cities; must be at least four
	 * @param includeRiver whether the model includes a vertical river
	 * @param riverPenalty finite cost added to each river crossing
	 * @param rng source of randomness
	 */
    public TspModel(int cityCount, boolean includeRiver, double riverPenalty, Random rng) {
		if (cityCount < 4) {
			throw new IllegalArgumentException("cityCount must be >= 4");
		}
		if (!Double.isFinite(riverPenalty)) {
			throw new IllegalArgumentException("riverPenalty must be finite");
		}
		Objects.requireNonNull(rng, "rng");
        this.cityCount = cityCount;
        this.includeRiver = includeRiver;

        if (includeRiver) {
            this.riverX = 0.25 + rng.nextDouble() * 0.5;
            this.riverPenalty = riverPenalty;
        } else {
            this.riverX = Double.NaN;
            this.riverPenalty = 0.0;
        }

        this.cities = new Point2D.Double[cityCount];
        for (int i = 0; i < cityCount; i++) {
            cities[i] = new Point2D.Double(rng.nextDouble(), rng.nextDouble());
        }
    }

    /** Enable or disable the river penalty dynamically. */
    public void setRiverEnabled(boolean enabled) {
        this.riverEnabled = enabled;
    }

	/** @return whether river-crossing costs are enabled */
    public boolean isRiverEnabled() {
        return riverEnabled;
    }

	/** @return current river-crossing cost */
    public double getRiverPenalty() {
        return riverPenalty;
    }

	/**
	 * Set the river-crossing cost.
	 * @param riverPenalty finite penalty or bonus
	 */
    public void setRiverPenalty(double riverPenalty) {
		if (!Double.isFinite(riverPenalty)) {
			throw new IllegalArgumentException("riverPenalty must be finite");
		}
        this.riverPenalty = riverPenalty;
    }

    /**
     * Distance between two cities including optional river penalty.
     * Note: because of the river penalty, this "distance"  can
     * be negative!!!
     * @param cityA index of first city
     * @param cityB index of second city
	 * @return Euclidean distance plus any applicable river cost
     */
    public double getDistance(int cityA, int cityB) {
        Point2D.Double a = cities[cityA];
        Point2D.Double b = cities[cityB];

        double d = a.distance(b);

        if (includeRiver && riverEnabled) {
            boolean crosses = (a.x < riverX && b.x > riverX) ||
                              (a.x > riverX && b.x < riverX);
            if (crosses) {
                d += riverPenalty;
             }
        }
        return d;
    }
}
