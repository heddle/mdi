package edu.cnu.mdi.sim;

/**
 * Configuration for {@link SimulationEngine}.
 * <p>
 * Intervals are used to rate-limit EDT events so the simulation thread can run
 * fast while UI updates remain smooth and responsive.
 * </p>
 */
public final class SimulationEngineConfig {
	
	public static final int DEFAULT_REFRESH_INTERVAL_MS = 33; // ~30 Hz
	public static final int DEFAULT_PROGRESS_INTERVAL_MS = 200; // ~5 Hz
	public static final int DEFAULT_COOPERATIVE_YIELD_MS = 0; // no yielding
	public static final boolean DEFAULT_AUTO_RUN = false; // start in READY, not RUNNING

	/**
	 * Target interval for posting refresh events to the EDT (milliseconds).
	 * <p>
	 * Typical values: 16 (~60 Hz), 33 (~30 Hz), 50 (~20 Hz). Use 0 to disable
	 * periodic refresh.
	 * </p>
	 */
	public final int refreshIntervalMs;

	/**
	 * Target interval for posting progress "ping" events to the EDT (milliseconds).
	 * <p>
	 * This is a fallback heartbeat. Many simulations will call
	 * {@link SimulationEngine#postProgress(ProgressInfo)} directly for determinate
	 * progress. Use 0 to disable periodic progress pings. If your simulation is determinate, 
	 * you can set this to a large value (e.g., 1000 ms) to get occasional progress updates 
	 * without flooding the ED, or set to 0 to disable entirely and rely on explicit progress posts.
	 * </p>
	 */
	public final int progressIntervalMs;

	/**
	 * Cooperative-yield rate limit for the simulation thread (milliseconds).
	 * <p>
	 * <b>Important:</b> this value is <em>not</em> interpreted as "sleep this long
	 * each step". Instead, it acts as a <em>minimum wall-clock interval</em> between
	 * opportunities for the engine to yield CPU time.
	 * </p>
	 * <p>
	 * The engine implements this by calling {@link Thread#yield()} at most once per
	 * {@code cooperativeYieldMs}. This is intentionally light-weight: using
	 * {@code park/sleep} in a tight simulation loop can deschedule the thread and
	 * incur OS timer granularity delays (often orders of magnitude larger than the
	 * requested duration), which can devastate performance.
	 * </p>
	 * <p>
	 * Use 0 to disable cooperative yielding for maximum throughput. If you need to
	 * intentionally reduce CPU usage, try small values such as 1–10 ms.
	 * </p>
	 */
	public final int cooperativeYieldMs;

	/**
	 * Whether the engine should transition to RUNNING immediately after READY.
	 * <p>
	 * If false, the engine will stop at READY until
	 * {@link SimulationEngine#requestResume()} (or
	 * {@link SimulationEngine#requestRun()}) is called.
	 * </p>
	 */
	public final boolean autoRun;

	/**
	 * Create a configuration.
	 *
	 * @param refreshIntervalMs  refresh interval in milliseconds (0 disables
	 *                           periodic refresh)
	 * @param progressIntervalMs progress interval in milliseconds (0 disables
	 *                           periodic progress ping)
	 * @param cooperativeYieldMs minimum interval between cooperative yields in
	 *                           milliseconds (0 disables yielding)
	 * @param autoRun            if true, RUNNING starts immediately after READY
	 */
	public SimulationEngineConfig(int refreshIntervalMs, int progressIntervalMs, int cooperativeYieldMs,
			boolean autoRun) {
		this.refreshIntervalMs = Math.max(0, refreshIntervalMs);
		this.progressIntervalMs = Math.max(0, progressIntervalMs);
		this.cooperativeYieldMs = Math.max(0, cooperativeYieldMs);
		this.autoRun = autoRun;
	}

	/**
	 * Reasonable defaults for interactive graphics.
	 *
	 * @return default config (refresh ~30 Hz, progress ~5 Hz, no yield, autoRun
	 *         enabled)
	 */
	public static SimulationEngineConfig defaults() {
		return new SimulationEngineConfig(DEFAULT_REFRESH_INTERVAL_MS, 
				DEFAULT_PROGRESS_INTERVAL_MS, 
				DEFAULT_COOPERATIVE_YIELD_MS,	
				DEFAULT_AUTO_RUN);
	}
}
