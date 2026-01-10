package edu.cnu.mdi.util;

/**
 * A compact collection of static utilities for manipulating bit masks stored in
 * a 64-bit {@code long}. This class provides common operations such as
 * checking, setting, clearing, toggling individual bits or masks, identifying
 * the positions of the first or highest set bit, counting bits, and
 * constraining a mask to a fixed width.
 *
 * <p>
 * All operations treat bit index {@code 0} as the least significant bit (LSB)
 * of the 64-bit word and bit index {@code 63} as the most significant bit.
 * Since {@code long} is inherently unsigned in its bit representation, all
 * operations use Java’s standard two's-complement bitwise semantics.
 * </p>
 *
 * <p>
 * This class is {@code final} with a private constructor because it is intended
 * purely as a static utility library.
 * </p>
 */
public final class Bits {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private Bits() {
	}

	/**
	 * Tests whether <em>all</em> bits specified by {@code mask} are set within
	 * {@code bits}. This is the typical idiom for multi-bit presence checks.
	 *
	 * <p>
	 * Example: to test whether bit 5 is set:
	 * </p>
	 * 
	 * <pre>
	 * Bits.check(bits, 1L << 5);
	 * </pre>
	 *
	 * @param bits the 64-bit value to check
	 * @param mask the bit mask to test (usually {@code 1L << n} or a combination)
	 * @return {@code true} if all mask bits are set, {@code false} otherwise
	 */
	public static boolean check(long bits, long mask) {
		return (bits & mask) == mask;
	}

	/**
	 * Returns a value in which all bits specified by {@code mask} have been set to
	 * {@code 1}, leaving all other bits unchanged.
	 *
	 * @param bits the original 64-bit value
	 * @param mask the mask of bits to set
	 * @return the value with the specified bits set
	 */
	public static long set(long bits, long mask) {
		return bits | mask;
	}

	/**
	 * Returns a value in which all bits specified by {@code mask} have been cleared
	 * (set to {@code 0}), leaving all other bits unchanged.
	 *
	 * @param bits the original value
	 * @param mask the mask of bits to clear
	 * @return the value with the specified bits cleared
	 */
	public static long clear(long bits, long mask) {
		return bits & ~mask;
	}

	/**
	 * Returns a value in which all bits specified by {@code mask} have been toggled
	 * (flipped from {@code 0} to {@code 1} or {@code 1} to {@code 0}).
	 *
	 * @param bits the original value
	 * @param mask the mask of bits to toggle
	 * @return the value with the specified bits toggled
	 */
	public static long toggle(long bits, long mask) {
		return bits ^ mask;
	}

	/**
	 * Sets the bit at the specified index to {@code 1}.
	 *
	 * @param bits  the original value
	 * @param index the bit index to set (valid range: {@code 0}–{@code 63})
	 * @return the value with the selected bit set
	 * @throws IllegalArgumentException if {@code index} is outside {@code 0..63}
	 */
	public static long setAt(long bits, int index) {
		return bits | (1L << index);
	}

	/**
	 * Clears the bit at the specified index (sets it to {@code 0}).
	 *
	 * @param bits  the original value
	 * @param index the bit index to clear (valid range: {@code 0}–{@code 63})
	 * @return the value with the selected bit cleared
	 */
	public static long clearAt(long bits, int index) {
		return bits & ~(1L << index);
	}

	/**
	 * Toggles the bit at the specified index.
	 *
	 * @param bits  the original value
	 * @param index the bit index to toggle (valid range: {@code 0}–{@code 63})
	 * @return the value with the selected bit toggled
	 */
	public static long toggleAt(long bits, int index) {
		return bits ^ (1L << index);
	}

	/**
	 * Tests whether the bit at the specified index is set.
	 *
	 * @param bits  the 64-bit word to inspect
	 * @param index the bit index to test (valid range: {@code 0}–{@code 63})
	 * @return {@code true} if the bit is set, {@code false} otherwise
	 */
	public static boolean checkAt(long bits, int index) {
		long mask = 1L << index;
		return (bits & mask) == mask;
	}

	/**
	 * Returns the index (0-based) of the lowest set bit (LSB-first). That is, it
	 * returns the position of the first bit whose value is {@code 1}.
	 *
	 * <p>
	 * This method is equivalent to {@link Long#numberOfTrailingZeros(long)} when
	 * {@code bits != 0}.
	 * </p>
	 *
	 * @param bits the 64-bit word
	 * @return index of the lowest set bit, or {@code -1} if {@code bits == 0}
	 */
	public static int firstSet(long bits) {
		if (bits == 0L) {
			return -1;
		}
		return Long.numberOfTrailingZeros(bits);
	}

	/**
	 * Returns the index (0-based) of the highest set bit (MSB-first). That is, it
	 * returns the position of the most significant bit whose value is {@code 1}.
	 *
	 * <p>
	 * This method is equivalent to:
	 * 
	 * <pre>
	 * 63 - Long.numberOfLeadingZeros(bits)
	 * </pre>
	 *
	 * @param bits the 64-bit word
	 * @return index of the highest set bit, or {@code -1} if {@code bits == 0}
	 */
	public static int highestSet(long bits) {
		if (bits == 0L) {
			return -1;
		}
		return 63 - Long.numberOfLeadingZeros(bits);
	}

	/**
	 * Counts and returns the number of set bits in the given word.
	 *
	 * <p>
	 * This method delegates to {@link Long#bitCount(long)}.
	 * </p>
	 *
	 * @param bits the 64-bit word
	 * @return number of set bits (range {@code 0}–{@code 64})
	 */
	public static int count(long bits) {
		return Long.bitCount(bits);
	}

	/**
	 * Determines whether the given word contains no set bits.
	 *
	 * @param bits the word to test
	 * @return {@code true} if the value is {@code 0}, {@code false} otherwise
	 */
	public static boolean isEmpty(long bits) {
		return bits == 0L;
	}

	/**
	 * Clears all bits and returns {@code 0L}. This is functionally equivalent to
	 * assigning {@code 0L} directly but provided for symmetry with other helpers.
	 *
	 * @return {@code 0L}
	 */
	public static long unsetAll() {
		return 0L;
	}

	/**
	 * Returns the value of {@code bits} truncated to a field of width {@code width}
	 * bits (from the least significant side).
	 *
	 * <p>
	 * If {@code width <= 0}, the method returns {@code 0L}. If {@code width >= 64},
	 * the method returns {@code bits} unchanged.
	 * </p>
	 *
	 * <p>
	 * Internally this method constructs a low-order mask of the form
	 * {@code (1L << width) - 1} and applies it with bitwise AND.
	 * </p>
	 *
	 * @param bits  the original value
	 * @param width number of low bits to preserve (range {@code 1}–{@code 64})
	 * @return the masked value
	 */
	public static long mask(long bits, int width) {
		if (width <= 0) {
			return 0L;
		}
		if (width >= 64) {
			return bits;
		}
		long m = (1L << width) - 1;
		return bits & m;
	}
}
