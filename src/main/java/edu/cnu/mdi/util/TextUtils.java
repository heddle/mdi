package edu.cnu.mdi.util;

import java.util.List;

public class TextUtils {

	/** Minimum font size returned by sizing helpers. */
	public static final int MIN_FONT_SIZE = 4;

	/** Maximum font size returned by sizing helpers (defensive upper bound). */
	public static final int MAX_FONT_SIZE = 512;


	/**
	 * Check to see if two vectors of strings are equal. Used by feedback to avoid
	 * redrawing identical strings.
	 *
	 * @param list1 the first String vector.
	 * @param list2 the other String vector.
	 * @return <code>true</code> if they are equal.
	 */
	public static boolean equalStringLists(List<String> list1, List<String> list2) {
		if ((list1 == null) && (list2 == null)) {
			return true;
		}

		// if just one is null, not equal
		// must have the same size
		if ((list1 == null) || (list2 == null) || (list1.size() != list2.size())) {
			return false;
		}

		// all strings must be equal
		for (int i = 0; i < list1.size(); i++) {
			String s1 = list1.get(i);
			String s2 = list2.get(i);

			if (((s1 == null) && (s2 != null)) || ((s1 != null) && (s2 == null))) {
				return false;
			}

			if ((s1 != null) && (s2 != null) && !(s1.equals(s2))) {
				return false;
			}

		}

		return true;
	}

}
