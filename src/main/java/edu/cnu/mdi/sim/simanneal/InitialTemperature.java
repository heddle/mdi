package edu.cnu.mdi.sim.simanneal;

public record InitialTemperature(
        double T0,
        double energyMedian,
        double energyMad,
        int samples
) {}
