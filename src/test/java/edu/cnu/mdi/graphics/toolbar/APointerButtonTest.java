package edu.cnu.mdi.graphics.toolbar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.graphics.rubberband.ARubberband;

/**
 * Regression coverage for {@link APointerButton}'s gesture-start dispatch.
 *
 * <p>A click-based rubberbanding policy (e.g. {@code POLYGON}) must have its
 * originating mouse event forwarded via {@code mousePressed(e)} so the first
 * vertex is captured, rather than being anchored via {@code begin(pressPt)},
 * which is the drag-based-policy path and never records a vertex.</p>
 */
class APointerButtonTest {

	private static MouseEvent pressAt(JPanel canvas, int x, int y) {
		return new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
				0, x, y, 1, false);
	}

	private static MouseEvent dragTo(JPanel canvas, int x, int y) {
		return new MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
				0, x, y, 0, false);
	}

	@Test
	void clickBasedPolicyForwardsFirstEventAsAVertexNotAnAnchor() {
		JPanel canvas = new JPanel();
		AToolBar toolBar = new AToolBar() {
			@Override
			protected void activeToggleButtonChanged(javax.swing.JToggleButton newlyActive) { }
		};

		APointerButton button = new APointerButton(canvas, toolBar, ARubberband.Policy.POLYGON, 5) {
			@Override
			public void rubberbanding(Rectangle bounds, Point[] vertices) { }
		};

		button.mousePressed(pressAt(canvas, 10, 10));
		// Past the default 5px drag threshold, so this promotes to RUBBERBANDING
		// and triggers beginRubberband().
		button.mouseDragged(dragTo(canvas, 30, 30));

		assertNotNull(button.rubberband,
				"a rubberband should have been created once the drag threshold was crossed");

		Point[] vertices = button.rubberband.getRubberbandVertices();
		assertNotNull(vertices,
				"a click-based rubberband must have captured its first vertex via mousePressed(e), "
						+ "not merely been anchored via begin(pressPt)");
		assertEquals(1, vertices.length);
		assertEquals(new Point(30, 30), vertices[0]);
	}

	@Test
	void dragBasedPolicyIsAnchoredAtThePressPoint() {
		JPanel canvas = new JPanel();
		AToolBar toolBar = new AToolBar() {
			@Override
			protected void activeToggleButtonChanged(javax.swing.JToggleButton newlyActive) { }
		};

		APointerButton button = new APointerButton(canvas, toolBar, ARubberband.Policy.RECTANGLE, 5) {
			@Override
			public void rubberbanding(Rectangle bounds, Point[] vertices) { }
		};

		button.mousePressed(pressAt(canvas, 10, 10));
		button.mouseDragged(dragTo(canvas, 30, 30));

		assertNotNull(button.rubberband,
				"a rubberband should have been created once the drag threshold was crossed");
		assertEquals(new Point(10, 10), button.rubberband.getStart(),
				"a drag-based rubberband is anchored at the original press point");
	}
}
