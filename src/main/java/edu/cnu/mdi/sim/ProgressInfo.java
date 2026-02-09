package edu.cnu.mdi.sim;

/**
 * Immutable progress payload for progress updates delivered to
 * {@link SimulationListener}.
 * <p>
 * Supports determinate progress (0..1 fraction) and indeterminate progress
 * (spinner). A message may be provided for UI display.
 * </p>
 */
public final class ProgressInfo {

	/** True if progress is indeterminate (spinner / "still working"). */
	public final boolean indeterminate;

	/**
	 * Determinate progress fraction in the range [0,1]. Ignored if
	 * {@link #indeterminate} is true.
	 */
	public final double fraction;

	/** Optional UI message such as "Cooling..." or "Loading data...". */
	public final String message;

	// Private constructor; use static factory methods for clarity.
	private ProgressInfo(boolean indeterminate, double fraction, String message) {
		this.indeterminate = indeterminate;
		this.fraction = fraction;
		this.message = message;
	}

	/**
	 * Create an indeterminate progress update.
	 *
	 * @param message optional UI text (may be null)
	 * @return an indeterminate progress object
	 */
	public static ProgressInfo indeterminate(String message) {
		return new ProgressInfo(true, 0.0, message);
	}

	/**
	 * Create a determinate progress update.
	 *
	 * @param fraction progress fraction; values are clamped into [0,1]
	 * @param message  optional UI text (may be null)
	 * @return a determinate progress object
	 */
	public static ProgressInfo determinate(double fraction, String message) {
		double f = Math.max(0.0, Math.min(1.0, fraction));
		return new ProgressInfo(false, f, message);
	}

	@Override
	public String toString() {
		if (indeterminate) {
			return "ProgressInfo[indeterminate, message=" + message + "]";
		}
		return "ProgressInfo[fraction=" + fraction + ", message=" + message + "]";
	}
}
