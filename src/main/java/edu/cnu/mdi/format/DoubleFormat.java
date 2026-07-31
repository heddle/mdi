package edu.cnu.mdi.format;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/** Thread-safe helpers for fixed-point and scientific decimal formatting. */
public final class DoubleFormat {

	private DoubleFormat() {
		throw new AssertionError("No DoubleFormat instances");
	}

	/**
	 * cache formats which are few and often repeated to avoid recreating
	 */
	private static final ThreadLocal<Map<String, DecimalFormat>> FORMATS =
			ThreadLocal.withInitial(HashMap::new);

	/**
	 * Format a double
	 *
	 * @param value  the value to format.
	 * @param numdec the number of digits right of the decimal.
	 * @return the formatted value
	 * @throws IllegalArgumentException if {@code numdec} is outside 0 through 100
	 */
	public static String doubleFormat(double value, int numdec) {
		return doubleFormat(value, numdec, false);
	}

	/**
	 * Format a double
	 *
	 * @param value  the value to format.
	 * @param numdec the number of digits right of the decimal.
	 * @param scinot if <code>true</code>, use scientific notation.
	 * @return the formatted value
	 * @throws IllegalArgumentException if {@code numdec} is outside 0 through 100
	 */
	public static String doubleFormat(double value, int numdec, boolean scinot) {
		if (numdec < 0 || numdec > 100) {
			throw new IllegalArgumentException("numdec must be between 0 and 100: " + numdec);
		}

		StringBuilder pattern = new StringBuilder();
		if (numdec < 1) {
			pattern.append("0");
		} else {
			pattern.append("0.");
		}

		for (int i = 0; i < numdec; i++) {
			pattern.append("0");
		}

		if (scinot) {
			pattern.append("E0");
		}

		String patternStr = pattern.toString();
		DecimalFormat df = FORMATS.get().computeIfAbsent(patternStr, DecimalFormat::new);

		return df.format(value);

	}

	/**
	 * Format a double, using scientific notation if the exponent is less than
	 * the specified minimum.
	 *
	 * @param value       the value to format.
	 * @param numdec      the number of digits right of the decimal.
	 * @param minExponent the minimum exponent for using scientific notation.
	 * @return the formatted value
	 * @throws IllegalArgumentException if {@code numdec} is outside 0 through 100
	 */
	public static String doubleFormat(double value, int numdec, int minExponent) {

		if (Math.abs(value) < 1.0e-30) {
			return "0.0";
		}

		int exponent = (int) Math.log10(Math.abs(value));
		if (exponent < 0) {
			exponent = -exponent + 1;
		}

		if (exponent < minExponent) {
			return doubleFormat(value, numdec, false);
		} else { // use sci not
			return doubleFormat(value, numdec, true);
		}

	}


}
