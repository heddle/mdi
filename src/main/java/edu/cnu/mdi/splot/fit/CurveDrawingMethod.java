package edu.cnu.mdi.splot.fit;

/**
 * Specifies how a curve should be drawn for a data set.
 * <p>
 * Historically this was named {@code FitType}, but that name was misleading
 * because several options are not fits at all (e.g. connect, stairs, none).
 * </p>
 * <p>
 * Some drawing methods optionally consume a {@link FitResult} (Apache Commons
 * Math based), or a {@link CubicSpline}.
 * </p>
 */
public enum CurveDrawingMethod {

	/** No curve drawn. */
	NONE("No Line"),

	/** Simple line segments connecting points. */
	CONNECT("Simple Connect"),

	/** Stair-step connection between points. */
	STAIRS("Stairs"),

	/** Natural cubic spline interpolation (not a fit). */
	CUBICSPLINE("Cubic Spline"),

	/** Polynomial least-squares fit. */
	POLYNOMIAL("Polynomial"),

	/** Single Gaussian fit. */
	GAUSSIAN("Gaussian"),

	/** Multiple Gaussian fit (sum of Gaussians). */
	GAUSSIANS("Gaussians"),

	/** Error function fit. */
	ERF("Erf Function"),

	/** Complementary error function fit. */
	ERFC("Erfc Function"),

	/** Exponential decay fit: A·exp(-x/τ) + C. */
	EXPONENTIAL_DECAY("Exponential Decay"),

	/** Power law fit: A·x^n + C. */
	POWER_LAW("Power Law"),

	/** Lorentzian (Breit-Wigner / Cauchy) resonance peak fit. */
	LORENTZIAN("Lorentzian");

	// ------------------------------------------------------------------------
	// Display name handling
	// ------------------------------------------------------------------------

	private final String displayName;

	CurveDrawingMethod(String displayName) {
		this.displayName = displayName;
	}

	/** UI-friendly name for combo boxes, menus, etc. 
	* @return display name
	*/
	public String getName() {
		return displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}

	/**
	 * Find a method by display name or enum constant name (case-insensitive).
	 *
	 * @param s display name or enum name
	 * @return matching method, or {@code null} if none
	 */
	public static CurveDrawingMethod getValue(String s) {
		if (s == null) {
			return null;
		}

		for (CurveDrawingMethod m : values()) {
			if (s.equalsIgnoreCase(m.getName()) || s.equalsIgnoreCase(m.name())) {
				return m;
			}
		}
		return null;
	}


}