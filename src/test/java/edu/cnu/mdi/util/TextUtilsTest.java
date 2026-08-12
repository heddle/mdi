package edu.cnu.mdi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class TextUtilsTest {

	@Test
	void convertsCommaSeparatedValues() {
		assertArrayEquals(new String[] { "FMT", "DC", "REC" },
				TextUtils.commaSeparatedToArray(" FMT, DC, REC "));
		assertEquals("FMT, DC, REC", TextUtils.arrayToCommaSeparated(new String[] { "FMT", "DC", "REC" }));
		assertNull(TextUtils.commaSeparatedToArray("  "));
		assertEquals("", TextUtils.arrayToCommaSeparated(null));
	}

	@Test
	void adjustsFontSizeWithoutChangingFamilyOrStyle() {
		Font base = new Font(Font.SANS_SERIF, Font.BOLD, 12);
		assertEquals(14, TextUtils.nextBiggerFont(base, 2).getSize());
		assertEquals(10, TextUtils.nextSmallerFont(base, 2).getSize());
		assertEquals(base.getStyle(), TextUtils.nextBiggerFont(base, 2).getStyle());
	}

	private static FontMetrics metrics() {
		BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try {
			return graphics.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		} finally {
			graphics.dispose();
		}
	}

	@Test
	void boundsCountEmptyAndTrailingLinesConsistently() {
		FontMetrics fm = metrics();
		Rectangle empty = TextUtils.textBounds("", fm, 0, 0, 0, 0, 1);
		Rectangle trailing = TextUtils.textBounds("one\n", fm, 0, 0, 0, 0, 1);
		assertEquals(fm.getHeight(), empty.height);
		assertEquals(2 * fm.getHeight(), trailing.height);
	}

	@Test
	void boundsClampSpacingAndRejectNonFiniteSpacing() {
		FontMetrics fm = metrics();
		Rectangle clamped = TextUtils.textBounds(new String[] { "a", "b" }, fm, 0, 0, 0, 0, 100);
		assertEquals(Math.round(3f * fm.getHeight()) + fm.getHeight(), clamped.height);
		assertThrows(IllegalArgumentException.class,
				() -> TextUtils.textBounds("text", fm, 0, 0, 0, 0, Float.NaN));
	}
}
