package edu.cnu.mdi.sim;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Context object shared with the simulation.
 * <p>
 * This object is safe to read from both simulation thread and EDT. The engine
 * owns mutation of internal bookkeeping, except for cancellation which can be
 * requested externally via {@link SimulationEngine#requestCancel()}.
 * </p>
 */
public final class SimulationContext {

    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    private volatile long startNanos;
    private volatile long stepCount;

    SimulationContext() {
        // package-private construction (owned by engine)
    }

    void markStarted() {
        startNanos = System.nanoTime();
    }

    void incrementStep() {
        stepCount++;
    }

    void requestCancel() {
        cancelRequested.set(true);
    }

    /**
     * Check whether cancellation has been requested.
     *
     * @return true if cancellation has been requested
     */
    public boolean isCancelRequested() {
        return cancelRequested.get();
    }

    /**
     * Get the number of completed steps.
     *
     * @return step count
     */
    public long getStepCount() {
        return stepCount;
    }

    /**
     * Get elapsed time since the engine started, in seconds.
     *
     * @return elapsed seconds
     */
    public double getElapsedSeconds() {
        return (System.nanoTime() - startNanos) * 1e-9;
    }
}
