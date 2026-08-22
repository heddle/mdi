package edu.cnu.mdi.sim.simanneal;

/**
 * Configuration for a simulated-annealing run.
 *
 * @param maxSteps maximum number of iterations; must be positive
 * @param stepsPerTemperature iterations per schedule level, or zero to advance
 *        the schedule on every iteration
 * @param alpha geometric cooling factor in {@code (0,1]}
 * @param minTemperature non-negative absolute-temperature stopping threshold
 * @param progressEverySteps progress interval, or zero to disable progress posts
 * @param refreshEverySteps refresh interval, or zero to disable refresh requests
 * @param randomSeed deterministic seed, or zero to choose a nondeterministic seed
 * @param notificationPolicy accepted-move listener delivery limits
 */
public record SimulatedAnnealingConfig(
        long maxSteps,
		long stepsPerTemperature,
		double alpha,
		double minTemperature,
		long progressEverySteps,
		long refreshEverySteps,
        long randomSeed,
		AnnealingNotificationPolicy notificationPolicy
) {
    public SimulatedAnnealingConfig {
        if (maxSteps <= 0) {
            throw new IllegalArgumentException("maxSteps must be > 0");
        }
        if (stepsPerTemperature < 0) {
            throw new IllegalArgumentException("stepsPerTemperature must be >= 0");
        }
        if (!Double.isFinite(alpha) || alpha <= 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("alpha must be finite and in (0,1]");
        }
        if (!Double.isFinite(minTemperature) || minTemperature < 0.0) {
            throw new IllegalArgumentException(
                    "minTemperature must be finite and >= 0");
        }
        if (progressEverySteps < 0 || refreshEverySteps < 0) {
            throw new IllegalArgumentException("step intervals must be >= 0");
        }
		java.util.Objects.requireNonNull(notificationPolicy, "notificationPolicy");
    }

	/**
	 * Convenience constructor using the default listener-delivery policy.
	 */
	public SimulatedAnnealingConfig(long maxSteps, long stepsPerTemperature,
			double alpha, double minTemperature, long progressEverySteps,
			long refreshEverySteps, long randomSeed) {
		this(maxSteps, stepsPerTemperature, alpha, minTemperature,
				progressEverySteps, refreshEverySteps, randomSeed,
				AnnealingNotificationPolicy.defaults());
	}

	/**
	 * Return a practical general-purpose configuration.
	 *
	 * @return default configuration
	 */
    public static SimulatedAnnealingConfig defaults() {
        return new SimulatedAnnealingConfig(
                2000000L,
                200L,
                0.997,
                1e-9,
                500L,
                50L,
				0L,
				AnnealingNotificationPolicy.defaults()
        );
    }

	/**
	 * Return a copy with a different progress notification interval.
	 *
	 * @param steps new non-negative interval; zero disables progress posts
	 * @return updated configuration
	 */
    public SimulatedAnnealingConfig withProgressEverySteps(long steps) {
        return new SimulatedAnnealingConfig(
            this.maxSteps(),
            this.stepsPerTemperature(),
            this.alpha(),
            this.minTemperature(),
            steps,
            this.refreshEverySteps(),
			this.randomSeed(),
			this.notificationPolicy()
        );
    }

}
