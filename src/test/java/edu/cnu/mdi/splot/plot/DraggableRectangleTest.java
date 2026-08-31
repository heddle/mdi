package edu.cnu.mdi.splot.plot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Regression coverage for {@link DraggableRectangle}'s "been moved" tracking.
 *
 * <p>{@code ExtraText} relies on {@code _beenMoved} staying {@code false}
 * until a real drag occurs, so it can keep defaulting to its upper-right
 * anchor. {@code PlotMouseHandler} calls {@code setDragging(false)} on
 * every mouse release, even a plain click that never primed or started a
 * drag, so clearing the dragging state must never by itself mark the
 * overlay as moved.</p>
 */
class DraggableRectangleTest {

	@Test
	void clearingDraggingWithoutEverStartingOneDoesNotMarkAsMoved() {
		DraggableRectangle rect = new DraggableRectangle();

		// Simulates PlotMouseHandler.mouseReleased() firing on a plain click
		// that never primed or entered a drag.
		rect.setDragging(false);

		assertFalse(rect._beenMoved,
				"clearing dragging without ever starting one must not mark the overlay as moved");
	}

	@Test
	void startingADragMarksAsMoved() {
		DraggableRectangle rect = new DraggableRectangle();

		rect.setDragging(true);

		assertTrue(rect._beenMoved, "starting a drag must mark the overlay as moved");
	}

	@Test
	void movedFlagStaysSetAfterTheDragEnds() {
		DraggableRectangle rect = new DraggableRectangle();

		rect.setDragging(true);
		rect.setDragging(false);

		assertTrue(rect._beenMoved,
				"once a real drag has occurred, ending it must not un-mark the overlay as moved");
	}
}
