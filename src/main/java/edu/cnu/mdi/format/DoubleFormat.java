package edu.cnu.mdi.format;

import java.text.DecimalFormat;
import java.util.Hashtable;

public class DoubleFormat {

	/**
	 * cache formats which are few and often repeated to avoid recreating
	 */
	private static Hashtable<String, DecimalFormat> formats = new Hashtable<>(143);

	/**
	 * Format a double
	 *
	 * @param value  the value to format.
	 * @param numdec the number of digits right of the decimal.
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
	 */
	public static String doubleFormat(double value, int numdec, boolean scinot) {

		StringBuffer pattern = new StringBuffer();
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
		DecimalFormat df = null;
		df = (formats.get(patternStr));

		if (df == null) {
			df = new DecimalFormat(patternStr);
			formats.put(patternStr, df);
		}

		return df.format(value);

	}

	/**
	 * Format a double, using scientific notation if the exponent is less than
	 * the specified minimum.
	 *
	 * @param value       the value to format.
	 * @param numdec      the number of digits right of the decimal.
	 * @param minExponent the minimum exponent for using scientific notation.
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