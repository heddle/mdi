package edu.cnu.mdi.graphics.style;

import java.util.EnumMap;

import edu.cnu.mdi.component.EnumComboBox;

public enum SymbolType {
	NOSYMBOL, SQUARE, CIRCLE, CROSS, UPTRIANGLE, DOWNTRIANGLE, X, DAVID, DIAMOND, STAR;

	/**
	 * A map for the names of the symbols
	 */
	public static EnumMap<SymbolType, String> names = new EnumMap<>(SymbolType.class);

	static {
		names.put(SQUARE, "Square");
		names.put(CIRCLE, "Circle");
		names.put(CROSS, "Cross");
		names.put(DOWNTRIANGLE, "Down Triangle");
		names.put(UPTRIANGLE, "Up Triangle");
		names.put(X, "X");
		names.put(DAVID, "David");
		names.put(DIAMOND, "Diamond");
		names.put(NOSYMBOL, "No Symbol");
	}

	/**
	 * Get the nice name of the enum.
	 *
	 * @return the nice name, for combo boxes, menus, etc.
	 */
	public String getName() {
		return names.get(this);
	}

	/**
	 * Returns the enum value from the name.
	 *
	 * @param name the name to match.
	 * @return the <code>SymbolType</code> that corresponds to the name. Returns
	 *         <code>null</code> if no match is found. Note it will check (case
	 *         insensitive) both the map and the <code>name()</code> result, thus
	 *         "Up Triangle" or "UPTRIANGLE" or "dUpTrIaNgLe" will return the
	 *         <code>UPTRIANGLE</code> value.
	 */
	public static SymbolType getValue(String name) {
		if (name == null) {
			return null;
		}

		for (SymbolType val : values()) {
			// check the nice name
			// check the base name
			if (name.equalsIgnoreCase(val.toString()) || name.equalsIgnoreCase(val.name())) {
				return val;
			}
		}
		return null;
	}


}
