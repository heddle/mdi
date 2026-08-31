package edu.cnu.mdi.component;

import java.util.regex.Pattern;

/** Validation and matching support for IPv4 addresses with optional wildcard octets. */
public final class IpAddressSupport {

	private static final String BYTE = "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
	private static final String STAR_OR_BYTE = "(\\*|25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

	public static final String ANY_ADDRESS = "*.*.*.*";
	public static final Pattern BASIC_PATTERN = Pattern.compile("\\b" + BYTE + "\\." + BYTE + "\\." + BYTE + "\\." + BYTE + "\\b");
	public static final Pattern SIMPLE_STAR_PATTERN = Pattern.compile(
			STAR_OR_BYTE + "\\." + STAR_OR_BYTE + "\\." + STAR_OR_BYTE + "\\." + STAR_OR_BYTE);

	private IpAddressSupport() {
	}

	/**
	 * Tests whether a string is a literal IPv4 address (no wildcards).
	 *
	 * @param ipAddress the string to test; {@code null} is not valid
	 * @return {@code true} if {@code ipAddress} is four dot-separated octets,
	 *         each in {@code [0, 255]}
	 */
	public static boolean validate(String ipAddress) {
		return ipAddress != null && BASIC_PATTERN.matcher(ipAddress).matches();
	}

	/**
	 * Tests whether a string is a valid IPv4 address pattern, where each
	 * octet is either a literal {@code [0, 255]} value or a {@code *}
	 * wildcard (e.g. {@code "129.57.*.*"}, or {@link #ANY_ADDRESS}).
	 *
	 * @param ipPattern the string to test; {@code null} is not valid
	 * @return {@code true} if every octet is a literal byte value or {@code *}
	 */
	public static boolean validateSimpleWildcard(String ipPattern) {
		return ipPattern != null && SIMPLE_STAR_PATTERN.matcher(ipPattern).matches();
	}

	/**
	 * Compiles a wildcard IPv4 address pattern into a matching {@link Pattern}.
	 * <p>
	 * Returns {@code null} in two distinct cases that callers may need to
	 * tell apart: {@code filterString} is {@code null} or equals
	 * {@link #ANY_ADDRESS} (meaning "match everything," so no filtering
	 * pattern is needed at all), or {@code filterString} fails
	 * {@link #validateSimpleWildcard} (meaning the input itself is invalid).
	 * Either way, treat a {@code null} result as "no filter to apply" unless
	 * you specifically need to distinguish "no filter requested" from
	 * "invalid filter" — this method does not let you tell them apart.
	 * </p>
	 *
	 * @param filterString the wildcard address pattern to compile, or
	 *                     {@code null}/{@link #ANY_ADDRESS} for no filtering
	 * @return a {@link Pattern} matching literal IPv4 addresses consistent
	 *         with the wildcard pattern, or {@code null} (see above)
	 */
	public static Pattern createPattern(String filterString) {
		if (filterString == null || ANY_ADDRESS.equals(filterString)) {
			return null;
		}
		if (!validateSimpleWildcard(filterString)) {
			return null;
		}

		String[] octets = filterString.split("\\.", -1);
		StringBuilder expression = new StringBuilder();
		for (int index = 0; index < octets.length; index++) {
			if (index > 0) {
				expression.append("\\.");
			}
			expression.append("*".equals(octets[index]) ? BYTE : Pattern.quote(octets[index]));
		}
		return Pattern.compile(expression.toString());
	}
}
