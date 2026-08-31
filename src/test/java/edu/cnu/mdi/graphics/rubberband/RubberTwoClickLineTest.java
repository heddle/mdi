package edu.cnu.mdi.graphics.rubberband;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

class RubberTwoClickLineTest {

	@Test
	void reportsTheTwoClickLinePolicyNotPlainLine() {
		JPanel component = new JPanel();
		RubberTwoClickLine rubberband = new RubberTwoClickLine(component, () -> { });

		// RubberTwoClickLine shares RubberLine's gesture behavior but must be
		// registered under its own policy, not the LINE policy it borrows
		// implementation from.
		assertEquals(ARubberband.Policy.TWO_CLICK_LINE, rubberband.policy);
	}

	@Test
	void plainRubberLineStillReportsTheLinePolicy() {
		JPanel component = new JPanel();
		RubberLine rubberband = new RubberLine(component, () -> { });

		assertEquals(ARubberband.Policy.LINE, rubberband.policy);
	}

	@Test
	void bothAreClickBasedGestures() {
		JPanel component = new JPanel();
		assertTrue(new RubberLine(component, () -> { }).isClickBased());
		assertTrue(new RubberTwoClickLine(component, () -> { }).isClickBased());
	}
}
