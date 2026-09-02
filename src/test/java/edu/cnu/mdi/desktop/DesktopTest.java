package edu.cnu.mdi.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JDesktopPane;

import org.junit.jupiter.api.Test;

class DesktopTest {

	@Test
	void usesOutlineDragModeNotLiveDragMode() {
		// LIVE_DRAG_MODE repaints every child view's full content on every
		// mouse-move while dragging it, which visibly lags for a
		// content-heavy view (e.g. a detector event display). Regression
		// guard against reverting to it. Desktop is a process-wide
		// singleton, so this may return an instance another test already
		// created rather than constructing a fresh one -- fine here, since
		// the drag mode is fixed once at construction and never changes.
		Desktop desktop = Desktop.createDesktop(null, null);
		assertEquals(JDesktopPane.OUTLINE_DRAG_MODE, desktop.getDragMode());
	}
}
