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

	public static boolean validate(String ipAddress) {
		return ipAddress != null && BASIC_PATTERN.matcher(ipAddress).matches();
	}

	public static boolean validateSimpleWildcard(String ipPattern) {
		return ipPattern != null && SIMPLE_STAR_PATTERN.matcher(ipPattern).matches();
	}

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
