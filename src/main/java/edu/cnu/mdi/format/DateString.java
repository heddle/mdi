package edu.cnu.mdi.format;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Thread-safe date and time formatting helpers. */
public final class DateString {

	private DateString() {
		throw new AssertionError("No DateString instances");
	}

	/**
	 * A formatter to get the time in down to minutes.
	 */
	private static final DateTimeFormatter FORMATTER_MM =
			DateTimeFormatter.ofPattern("EEE MMM d  h:mm a");

	/**
	 * A formatter to get the time in down to seconds.
	 */
	private static final DateTimeFormatter FORMATTER_SS =
			DateTimeFormatter.ofPattern("EEE MMM d  h:mm:ss a");

	/**
	 * A formatter to get the time in down to seconds (no day info).
	 */
	private static final DateTimeFormatter FORMATTER_SHORT =
			DateTimeFormatter.ofPattern("h:mm:ss");

	/**
	 * A formatter to get the time in down to seconds (no day info).
	 */
	private static final DateTimeFormatter FORMATTER_LONG =
			DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

	/**
	 * Returns the current time.
	 *
	 * @return a string representation of the current time, down to seconds.
	 */
	public static String dateString() {
		return dateStringSS();
	}

	/**
	 * Returns the current time.
	 *
	 * @return a string representation of the current time, down to minutes.
	 */
	public static String dateStringMM() {
		return dateStringMM(System.currentTimeMillis());
	}

	/**
	 * Returns the current time.
	 *
	 * @param longtime the time in millis.
	 * @return a string representation of the current time, down to minutes.
	 */
	public static String dateStringMM(long longtime) {
		return format(FORMATTER_MM, longtime);
	}

	/**
	 * Returns the current time.
	 *
	 * @return a string representation of the current time, down to seconds.
	 */
	public static String dateStringLong() {
		return format(FORMATTER_LONG, System.currentTimeMillis());
	}

	/**
	 * Returns the current time.
	 *
	 * @param longtime The time in millis.
	 * @return a string representation of the current time, down to seconds.
	 */
	public static String dateStringSS(long longtime) {
		return format(FORMATTER_SS, longtime);
	}

	/**
	 * Returns the current time.
	 *
	 * @return a string representation of the current time, down to seconds.
	 */
	public static String dateStringSS() {
		return dateStringSS(System.currentTimeMillis());
	}

	/**
	 * Returns the current time.
	 *
	 * @param ltime a time in milliseconds.
	 * @return a string representation of the current time, down to seconds but
	 *         without day information.
	 */
	public static String dateStringShort(long ltime) {
		return format(FORMATTER_SHORT, ltime);
	}

	/**
	 * Returns the current time.
	 *
	 * @return a string representation of the current time, down to seconds but
	 *         without day information.
	 */
	public static String dateStringShort() {
		return dateStringShort(System.currentTimeMillis());
	}

	private static String format(DateTimeFormatter formatter, long epochMillis) {
		return formatter.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMillis));
	}

}
