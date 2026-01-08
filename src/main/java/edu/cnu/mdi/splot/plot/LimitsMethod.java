package edu.cnu.mdi.splot.plot;

import edu.cnu.mdi.component.EnumComboBox;

public enum LimitsMethod {

    MANUALLIMITS("Manually enter limits"),
    ALGORITHMICLIMITS("Algorithmic limits"),
    USEDATALIMITS("Use data limits");

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
     * Returns the enum value from a string.
     * Matches either the nice label (getName) or the enum constant name() (case-insensitive).
     */
    public static LimitsMethod getValue(String s) {
        if (s == null) {
			return null;
		}

        for (LimitsMethod val : values()) {
            if (s.equalsIgnoreCase(val.getName()) || s.equalsIgnoreCase(val.name())) {
				return val;      // constant id
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
