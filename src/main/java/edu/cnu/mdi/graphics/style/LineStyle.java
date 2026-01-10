package edu.cnu.mdi.graphics.style;

import edu.cnu.mdi.component.EnumComboBox;

public enum LineStyle {
	SOLID("Solid"), DASH("Dash"), DOT_DASH("Dot Dash"), DOT("Dot"), DOUBLE_DASH("Double Dash"), LONG_DASH("Long Dash"),
	LONG_DOT_DASH("Long Dot Dash");

	private final String displayName;

	LineStyle(String displayName) {
		this.displayName = displayName;
	}

	public String getName() {
		return displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}

	/**
	 * Returns the enum value from a name. Accepts either display name
	 * (case-insensitive) or enum constant name().
	 *
	 * @param name the name to match.
	 * @return the corresponding LineStyle, or null if none matches.
	 */
	public static LineStyle getValue(String name) {
		if (name == null) {
			return null;
		}
		for (LineStyle val : values()) {
			if (name.equalsIgnoreCase(val.displayName) || name.equalsIgnoreCase(val.name())) {
				return val;
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
	public static EnumComboBox<LineStyle> getComboBox(LineStyle defaultChoice) {
		return new EnumComboBox<>(LineStyle.class, defaultChoice, null, LineStyle::getName);
	}
}
