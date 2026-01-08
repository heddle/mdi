package edu.cnu.mdi.graphics.style;

import edu.cnu.mdi.component.EnumComboBox;

public enum SymbolType {
    NOSYMBOL("No Symbol"),
    SQUARE("Square"),
    CIRCLE("Circle"),
    CROSS("Cross"),
    UPTRIANGLE("Up Triangle"),
    DOWNTRIANGLE("Down Triangle"),
    X("X"),
    BOWTIE("Bow Tie"),
    DIAMOND("Diamond"),
    STAR("Star");

    private final String displayName;

    SymbolType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Get the nice name of the enum (for combo boxes, menus, etc.).
     *
     * @return the display name.
     */
    public String getName() {
        return displayName;
    }

    /**
     * Returns the enum value from a name.
     * Accepts either display name (case-insensitive) or enum constant name().
     *
     * @param name the name to match.
     * @return the corresponding SymbolType, or null if none matches.
     */
    public static SymbolType getValue(String name) {
        if (name == null) {
            return null;
        }
        for (SymbolType val : values()) {
            if (name.equalsIgnoreCase(val.displayName) || name.equalsIgnoreCase(val.name())) {
                return val;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        // Optional, but convenient if someone drops SymbolType into a default renderer.
        return displayName;
    }

    /**
     * Obtain a combo box of choices.
     *
     * @param defaultChoice which enum should be initially selected (nullable)
     * @return the combo box
     */
    public static EnumComboBox<SymbolType> getComboBox(SymbolType defaultChoice) {
    	return new EnumComboBox<>(SymbolType.class, defaultChoice, null, SymbolType::getName);
    }
}
