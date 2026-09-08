package edu.cnu.mdi.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;

class ViewConfigurationTest {

	// Never actually invoked in these tests: wasOpenInSavedLayout(Properties)
	// answers without realizing the view, and asserting that stays true is
	// exactly the point of the "never called" factory below.
	private static final ViewFactory<BaseView> NEVER_CALLED = () -> {
		throw new AssertionError("the view must not be realized just to probe saved-layout state");
	};

	@Test
	void reportsOpenWhenSavedLayoutMarksItPresentAndVisible() {
		ViewConfiguration<BaseView> config = ViewConfiguration.lazy(
				"FMT XY", NEVER_CALLED, 0, 0, 0, VirtualView.CENTER);
		Properties saved = new Properties();
		saved.put("FMT_XY.x", "100");
		saved.put("FMT_XY.visible", "true");

		assertTrue(config.wasOpenInSavedLayout(saved));
	}

	@Test
	void reportsClosedWhenSavedLayoutMarksItPresentButHidden() {
		// Present as a saved frame (it has an x) but not visible -- e.g. the
		// user explicitly closed it before the layout was last saved. Must
		// not be resurrected.
		ViewConfiguration<BaseView> config = ViewConfiguration.lazy(
				"FMT XY", NEVER_CALLED, 0, 0, 0, VirtualView.CENTER);
		Properties saved = new Properties();
		saved.put("FMT_XY.x", "100");
		saved.put("FMT_XY.visible", "false");

		assertFalse(config.wasOpenInSavedLayout(saved));
	}

	@Test
	void reportsClosedWhenNotPresentInSavedLayoutAtAll() {
		ViewConfiguration<BaseView> config = ViewConfiguration.lazy(
				"FMT XY", NEVER_CALLED, 0, 0, 0, VirtualView.CENTER);

		assertFalse(config.wasOpenInSavedLayout(new Properties()));
		assertFalse(config.wasOpenInSavedLayout(null));
	}

	@Test
	void sanitizesTheMenuTitleTheSameWayBaseViewSanitizesATitle() {
		// Spaces (and any other non [A-Za-z0-9_-] character) become
		// underscores -- must match BaseView's own derivation exactly, or a
		// real view's saved property-name prefix would never be found.
		ViewConfiguration<BaseView> config = ViewConfiguration.lazy(
				"Sectors 1 and 4", NEVER_CALLED, 0, 0, 0, VirtualView.CENTER);
		Properties saved = new Properties();
		saved.put("Sectors_1_and_4.x", "50");
		saved.put("Sectors_1_and_4.visible", "true");

		assertTrue(config.wasOpenInSavedLayout(saved));
	}

	@Test
	void presentButMissingVisibleKeyIsTreatedAsNotOpen() {
		// Defensive default: an absent .visible key (a malformed or
		// partially-written file) must not silently reopen the view.
		ViewConfiguration<BaseView> config = ViewConfiguration.lazy(
				"FMT XY", NEVER_CALLED, 0, 0, 0, VirtualView.CENTER);
		Properties saved = new Properties();
		saved.put("FMT_XY.x", "100");

		assertFalse(config.wasOpenInSavedLayout(saved));
	}
}
