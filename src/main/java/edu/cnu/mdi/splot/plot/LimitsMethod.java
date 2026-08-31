package edu.cnu.mdi.splot.plot;

import edu.cnu.mdi.component.EnumComboBox;

/**
 * How {@link PlotCanvas} determines an axis's displayed {@code [min, max]}
 * range. Set independently per axis via {@link PlotParameters}.
 *
 * <ul>
 * <li>{@link #MANUALLIMITS} — use the explicit bounds set via
 * {@link PlotParameters#setXRange} / {@link PlotParameters#setYRange}.</li>
 * <li>{@link #ALGORITHMICLIMITS} (the default) — for a linear axis, round the
 * data bounds out to "nice" tick-aligned values via {@link NiceScale}; for a
 * log axis, use the positive data's exact min/max instead (a "nice" rounding
 * doesn't apply the same way on a log scale).</li>
 * <li>{@link #USEDATALIMITS} — use the data's exact bounds unrounded, on a
 * linear axis; on a log axis this behaves the same as
 * {@code ALGORITHMICLIMITS} (both use the positive data's exact min/max).</li>
 * </ul>
 */
public enum LimitsMethod {

	MANUALLIMITS("Manually enter limits"), ALGORITHMICLIMITS("Algorithmic limits"), USEDATALIMITS("Use data limits");

	private final String displayName;

	LimitsMethod(String displayName) {
		this.displayName = displayName;
	}

	/** Nice label for UI (combo boxes, menus, etc.). */
	public String getName() {
		return displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}

	/**
	 * Returns the enum value from a string. Matches either the nice label (getName)
	 * or the enum constant name() (case-insensitive).
	 */
	public static LimitsMethod getValue(String s) {
		if (s == null) {
			return null;
		}

		for (LimitsMethod val : values()) {
			if (s.equalsIgnoreCase(val.getName()) || s.equalsIgnoreCase(val.name())) {
				return val; // constant id
			}
		}
		return null;
	}

	/**
	 * Obtain a combo box of choices.
	 *
	 * @param defaultChoice which enum should be initially selected (nullable)
	 * @return the combo box
	 */
	public static EnumComboBox<LimitsMethod> getComboBox(LimitsMethod defaultChoice) {
		return new EnumComboBox<>(LimitsMethod.class, defaultChoice, null, LimitsMethod::getName);
	}
}
