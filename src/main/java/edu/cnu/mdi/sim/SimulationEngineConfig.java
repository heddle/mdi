package edu.cnu.mdi.sim;

/**
 * Configuration for {@link SimulationEngine}.
 * <p>
 * Intervals are used to rate-limit EDT events so the simulation thread can run fast
 * while UI updates remain smooth and responsive.
 * </p>
 */
public final class SimulationEngineConfig {

    /**
     * Target interval for posting refresh events to the EDT (milliseconds).
     * <p>
     * Typical values: 16 (~60 Hz), 33 (~30 Hz), 50 (~20 Hz).
     * Use 0 to disable periodic refresh.
     * </p>
     */
    public final int refreshIntervalMs;

    /**
     * Target interval for posting progress "ping" events to the EDT (milliseconds).
     * <p>
     * This is a fallback heartbeat. Many simulations will call
     * {@link SimulationEngine#postProgress(ProgressInfo)} directly for determinate progress.
     * Use 0 to disable periodic progress pings.
     * </p>
     */
    public final int progressIntervalMs;

    /**
     * Optional cooperative yield/sleep on the simulation thread (milliseconds).
     * <p>
     * Use this if you want to intentionally reduce CPU usage. Use 0 for maximum speed.
     * </p>
     */
    public final int cooperativeYieldMs;

    /**
     * Whether the engine should transition to RUNNING immediately after READY.
     * <p>
     * If false, the engine will stop at READY until {@link SimulationEngine#requestResume()}
     * (or {@link SimulationEngine#requestRun()}) is called.
     * </p>
     */
    public final boolean autoRun;

    /**
     * Create a configuration.
     *
     * @param refreshIntervalMs refresh interval in milliseconds (0 disables periodic refresh)
     * @param progressIntervalMs progress interval in milliseconds (0 disables periodic progress ping)
     * @param cooperativeYieldMs cooperative yield in milliseconds (0 disables sleeping)
     * @param autoRun if true, RUNNING starts immediately after READY
     */
    public SimulationEngineConfig(int refreshIntervalMs, int progressIntervalMs, int cooperativeYieldMs, boolean autoRun) {
        this.refreshIntervalMs = Math.max(0, refreshIntervalMs);
        this.progressIntervalMs = Math.max(0, progressIntervalMs);
        this.cooperativeYieldMs = Math.max(0, cooperativeYieldMs);
        this.autoRun = autoRun;
    }

    /**
     * Reasonable defaults for interactive graphics.
     *
     * @return default config (refresh ~30 Hz, progress ~5 Hz, no sleep, autoRun enabled)
     */
    public static SimulationEngineConfig defaults() {
        return new SimulationEngineConfig(33, 200, 0, true);
    }
}
