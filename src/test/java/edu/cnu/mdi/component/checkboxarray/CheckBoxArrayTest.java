package edu.cnu.mdi.component.checkboxarray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.Test;

/**
 * Regression coverage for {@link CheckBoxArray}, which had zero prior test
 * coverage.
 */
class CheckBoxArrayTest {

	@Test
	void constructorRejectsNonPositiveColumnCount() {
		assertThrows(IllegalArgumentException.class, () -> new CheckBoxArray(0, 4, 4));
	}

	@Test
	void bitmaskGatedAddOnlyAddsWhenAllMaskBitsAreSet() {
		CheckBoxArray array = new CheckBoxArray(1, 4, 4);

		// bits has all the mask's bits set -> added.
		CheckBoxData added = array.add("shown", false, true, 0b111L, 0b101L, null, Color.black);
		assertNotNull(added);
		assertNotNull(array.getButton("shown"));

		// bits is missing one of the mask's bits -> not added.
		CheckBoxData notAdded = array.add("hidden", false, true, 0b010L, 0b101L, null, Color.black);
		assertNull(notAdded);
		assertNull(array.getButton("hidden"));
	}

	@Test
	void labelConstructorAddsOneUncheckedEnabledBoxPerLabel() {
		CheckBoxArray array = new CheckBoxArray(2, 4, 4, "a", "b", "c");

		assertNotNull(array.getButton("a"));
		assertNotNull(array.getButton("b"));
		assertNotNull(array.getButton("c"));
		assertFalse(array.isSelected("a"));
		assertTrue(array.isEnabled("a"));
	}

	@Test
	void selectedAndEnabledStateCanBeQueriedAndChanged() {
		CheckBoxArray array = new CheckBoxArray(1, 4, 4);
		array.add("a", false, true, null, Color.black);

		array.setSelected("a", true);
		assertTrue(array.isSelected("a"));

		array.setEnabled("a", false);
		assertFalse(array.isEnabled("a"));
	}

	@Test
	void queryingAnUnknownLabelIsSafeAndFalsy() {
		CheckBoxArray array = new CheckBoxArray(1, 4, 4);

		assertFalse(array.isSelected("missing"));
		assertFalse(array.isEnabled("missing"));
		assertNull(array.getButton("missing"));
		// Must not throw.
		array.setSelected("missing", true);
		array.setEnabled("missing", true);
	}

	@Test
	void radioGroupEnforcesMutualExclusivity() {
		CheckBoxArray array = new CheckBoxArray(1, 4, 4);
		array.add("x", true, true, "group", null, Color.black);
		array.add("y", false, true, "group", null, Color.black);

		assertEquals("x", array.getActiveButton("group").getText());

		array.setSelected("y", true);
		// Swing's ButtonGroup deselects the previous member when a sibling is selected.
		assertFalse(array.isSelected("x"));
		assertTrue(array.isSelected("y"));
	}

	@Test
	void getActiveButtonOfAnUnknownGroupIsNull() {
		CheckBoxArray array = new CheckBoxArray(1, 4, 4);
		assertNull(array.getActiveButton("no-such-group"));
	}
}
