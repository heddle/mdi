package edu.cnu.mdi.feedback;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import javax.swing.JTextPane;

import org.junit.jupiter.api.Test;

/**
 * Regression coverage for {@link FeedbackPane#append(String)}'s prefix
 * parser, which had zero prior test coverage.
 */
class FeedbackPaneTest {

	private static String textOf(FeedbackPane pane) {
		JTextPane textPane = (JTextPane) pane.getViewport().getView();
		return textPane.getText();
	}

	@Test
	void colorPrefixStripsTheTagAndRendersOnlyTheRemainder() {
		FeedbackPane pane = new FeedbackPane(Color.white, Color.black, 11);
		pane.append("$red$hello");
		assertEquals("hello", textOf(pane).trim());
	}

	@Test
	void colorPrefixIsCaseInsensitive() {
		FeedbackPane pane = new FeedbackPane(Color.white, Color.black, 11);
		pane.append("$RED$hello");
		assertEquals("hello", textOf(pane).trim());
	}

	@Test
	void monoPrefixStripsTheTagAndRendersOnlyTheRemainder() {
		FeedbackPane pane = new FeedbackPane(Color.white, Color.black, 11);
		pane.append("$mono$1.234, 5.678");
		assertEquals("1.234, 5.678", textOf(pane).trim());
	}

	@Test
	void unknownColorNameFallsThroughAndShowsTheFullOriginalTag() {
		FeedbackPane pane = new FeedbackPane(Color.white, Color.black, 11);
		pane.append("$notacolor$hello");
		// The malformed/unknown tag is rendered as-is, not stripped, so the
		// developer notices the mistake.
		assertEquals("$notacolor$hello", textOf(pane).trim());
	}

	@Test
	void tooShortNameIsNotTreatedAsAColorTag() {
		FeedbackPane pane = new FeedbackPane(Color.white, Color.black, 11);
		// "$$text" has a zero-length name between the two '$' characters,
		// below the minimum, so it must not be parsed as a color tag.
		pane.append("$$text");
		assertEquals("$$text", textOf(pane).trim());
	}

	@Test
	void plainTextWithNoPrefixIsRenderedUnchanged() {
		FeedbackPane pane = new FeedbackPane(Color.white, Color.black, 11);
		pane.append("plain feedback");
		assertEquals("plain feedback", textOf(pane).trim());
	}

	@Test
	void nullMessageIsANoOp() {
		FeedbackPane pane = new FeedbackPane(Color.white, Color.black, 11);
		assertDoesNotThrow(() -> pane.append((String) null));
		assertEquals("", textOf(pane).trim());
	}

	@Test
	void eachAppendedMessageEndsUpOnItsOwnLine() {
		FeedbackPane pane = new FeedbackPane(Color.white, Color.black, 11);
		pane.append("first");
		pane.append("second");
		String text = textOf(pane);
		assertTrue(text.startsWith("first"));
		assertTrue(text.trim().endsWith("second"));
		assertTrue(text.indexOf("first") < text.indexOf("second"));
	}
}
