package edu.cnu.mdi.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UnicodeUtilsTest {

	@Test
	public void testSpecialCharReplaceReturnsNullForNullInput() {
		assertNull(UnicodeUtils.specialCharReplace(null));
	}

	@Test
	public void testSpecialCharReplaceReturnsSameTextWhenNoEscapesPresent() {
		String s = "plain text with no special escapes";

		assertEquals(s, UnicodeUtils.specialCharReplace(s));
	}

	@Test
	public void testSpecialCharReplaceGreekLetters() {
		assertEquals("α β γ Δ Ω",
				UnicodeUtils.specialCharReplace("\\alpha \\beta \\gamma \\Delta \\Omega"));
	}

	@Test
	public void testSpecialCharReplaceMathSymbols() {
		assertEquals("x ≤ y ≥ z",
				UnicodeUtils.specialCharReplace("x \\leq y \\geq z"));

		assertEquals("a ≠ b ≈ c ± d",
				UnicodeUtils.specialCharReplace("a \\neq b \\approx c \\pm d"));

		assertEquals("2 × 3 = 6",
				UnicodeUtils.specialCharReplace("2 \\times 3 = 6"));
	}

	@Test
	public void testSpecialCharReplaceArrows() {
		assertEquals("← ↑ → ↓ ↔ ↕",
				UnicodeUtils.specialCharReplace("\\larrow \\uarrow \\rarrow \\darrow \\lrarrow \\udarrow"));
	}

	@Test
	public void testSpecialCharReplaceLeavesUnknownEscapesAlone() {
		assertEquals("\\notAThing α",
				UnicodeUtils.specialCharReplace("\\notAThing \\alpha"));
	}

	@Test
	public void testSpecialCharReplaceHandlesMultipleOccurrences() {
		assertEquals("α + α = 2α",
				UnicodeUtils.specialCharReplace("\\alpha + \\alpha = 2\\alpha"));
	}

	@Test
	public void testSpecialCharReplaceHandlesPrefixConflictSimeqBeforeSim() {
		assertEquals("≃ ∼",
				UnicodeUtils.specialCharReplace("\\simeq \\sim"));
	}

	@Test
	public void testSpecialCharReplaceDoesNotNeedWhitespaceBoundary() {
		assertEquals("αβγ",
				UnicodeUtils.specialCharReplace("\\alpha\\beta\\gamma"));
	}

	@Test
	public void testGetSuperscriptPositiveNumber() {
		assertEquals("¹²³", UnicodeUtils.getSuperscript(123, false));
	}

	@Test
	public void testGetSuperscriptZero() {
		assertEquals("⁰", UnicodeUtils.getSuperscript(0, false));
	}

	@Test
	public void testGetSuperscriptWithExplicitNegativeFlag() {
		assertEquals("⁻¹²³", UnicodeUtils.getSuperscript(123, true));
	}

	@Test
	public void testGetSuperscriptNegativeNumberWithoutNegativeFlagIgnoresMinusSign() {
		assertEquals("¹²³", UnicodeUtils.getSuperscript(-123, false));
	}

	@Test
	public void testGetSuperscriptNegativeNumberWithNegativeFlagDoesNotDuplicateMinusSign() {
		assertEquals("⁻¹²³", UnicodeUtils.getSuperscript(-123, true));
	}

	@Test
	public void testSelectedConstants() {
		assertEquals("°", UnicodeUtils.DEGREE);
		assertEquals("×", UnicodeUtils.TIMES);
		assertEquals("≤", UnicodeUtils.LEQ);
		assertEquals("≥", UnicodeUtils.GEQ);
		assertEquals("∞", UnicodeUtils.INFINITY);
		assertEquals("π", UnicodeUtils.SMALL_PI);
		assertEquals("Ω", UnicodeUtils.CAPITAL_OMEGA);
	}
}