package edu.cnu.mdi.sim.simanneal;

public record SimulatedAnnealingConfig(
        long maxSteps,
        long stepsPerTemperature,     // e.g. 50–500
        double alpha,                 // geometric cooling, e.g. 0.995–0.9999
        double minTemperature,        // optional hard floor
        long progressEverySteps,      // how often to post progress/message
        long refreshEverySteps,       // how often to request refresh
        long randomSeed
) {
    public static SimulatedAnnealingConfig defaults() {
        return new SimulatedAnnealingConfig(
                2000000L,
                200L,
                0.997,
                1e-9,
                500L,
                50L,
                0L
        );
    }

    public SimulatedAnnealingConfig withProgressEverySteps(long steps) {
        return new SimulatedAnnealingConfig(
            this.maxSteps(),
            this.stepsPerTemperature(),
            this.alpha(),
            this.minTemperature(),
            steps,
            this.refreshEverySteps(),
            this.randomSeed()
        );
    }

}
