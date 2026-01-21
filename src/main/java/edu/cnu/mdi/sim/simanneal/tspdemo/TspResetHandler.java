package edu.cnu.mdi.sim.simanneal.tspdemo;

@FunctionalInterface
public interface TspResetHandler {
    void resetRequested(int cityCount, double riverPenalty);
}
