package edu.cnu.mdi.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/**
 * {@link GuardedDesktopManager} keeps a dragged frame's title bar from leaving
 * the top of the desktop, where it could no longer be grabbed.
 */
class GuardedDesktopManagerTest {

	/** Mirror of the manager's private breathing-room constant. */
	private static final int TOP_MARGIN = 5;

	private static void onEdt(Runnable body) {
		try {
			SwingUtilities.invokeAndWait(body);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException re) throw re;
			if (cause instanceof Error err) throw err;
			throw new AssertionError(cause);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while running Swing test", e);
		}
	}

	/** Drag a 200 x 100 frame to (x, y) and return where it ended up. */
	private static Rectangle dragTo(int x, int y) {
		Rectangle[] result = new Rectangle[1];
		onEdt(() -> {
			JDesktopPane pane = new JDesktopPane();
			pane.setSize(800, 600);
			GuardedDesktopManager manager = new GuardedDesktopManager();
			pane.setDesktopManager(manager);
			JInternalFrame frame = new JInternalFrame("frame");
			frame.setBounds(100, 100, 200, 100);
			pane.add(frame);

			manager.dragFrame(frame, x, y);

			result[0] = frame.getBounds();
		});
		return result[0];
	}

	@Test
	void aDragAboveTheTopEdgeIsClampedToTheMargin() {
		assertEquals(new Rectangle(150, TOP_MARGIN, 200, 100), dragTo(150, -80));
	}

	@Test
	void aDragToYZeroIsStillClampedBecauseTheMarginIsPositive() {
		assertEquals(new Rectangle(150, TOP_MARGIN, 200, 100), dragTo(150, 0));
	}

	@Test
	void aDragJustInsideTheMarginIsClamped() {
		assertEquals(TOP_MARGIN, dragTo(150, TOP_MARGIN - 1).y);
	}

	@Test
	void aDragAtOrBelowTheMarginIsLeftAlone() {
		assertEquals(new Rectangle(150, TOP_MARGIN, 200, 100), dragTo(150, TOP_MARGIN));
		assertEquals(new Rectangle(150, 250, 200, 100), dragTo(150, 250));
	}

	@Test
	void clampingNeverChangesTheHorizontalPositionOrTheSize() {
		Rectangle r = dragTo(-40, -500);

		assertEquals(-40, r.x, "the guard is vertical only");
		assertEquals(TOP_MARGIN, r.y);
		assertEquals(200, r.width);
		assertEquals(100, r.height);
	}
}
