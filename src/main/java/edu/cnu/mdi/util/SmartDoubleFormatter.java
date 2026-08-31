package edu.cnu.mdi.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Formats floating-point values with a requested number of significant digits. */
public final class SmartDoubleFormatter {

	private SmartDoubleFormatter() {
	}

	/**
	 * Format a value with the requested significant-digit precision.
	 *
	 * @param value value to format
	 * @param sigDigits positive number of significant digits
	 * @return plain or scientific representation, whichever is more compact
	 * @throws IllegalArgumentException if {@code sigDigits <= 0}
	 */
	public static String doubleFormat(double value, int sigDigits) {
		if (sigDigits <= 0) {
			throw new IllegalArgumentException("sigDigits must be positive");
		}

		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return String.valueOf(value);
		}

		BigDecimal bd = new BigDecimal(value, new MathContext(sigDigits, RoundingMode.HALF_UP));

		String plain = bd.toPlainString();
		String scientific = bd.toString();

		// Use scientific if plain string is ugly/long
		if (plain.length() > sigDigits + 4) {
			return scientific;
		} else {
			return plain;
		}
	}
}
