package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Objects;

import javax.swing.JToggleButton;

import edu.cnu.mdi.graphics.rubberband.ARubberband;
import edu.cnu.mdi.graphics.rubberband.IRubberbanded;
import edu.cnu.mdi.graphics.rubberband.RubberbandFactory;

@SuppressWarnings("serial")
public abstract class ARubberbandButton extends JToggleButton
		implements MouseMotionListener, MouseListener, IRubberbanded {

	private final int minSizePx;

	protected Component canvas;
	protected AToolBar toolBar;
	protected ARubberband.Policy policy;

	protected ARubberband rubberband;
	protected boolean startOnDrag;
	protected GestureContext gesture;

	protected ARubberbandButton(Component canvas, AToolBar toolBar, ARubberband.Policy policy, int minSizePx) {
		Objects.requireNonNull(canvas, "canvas");
		Objects.requireNonNull(toolBar, "toolBar");
		Objects.requireNonNull(policy, "policy");

		this.canvas = canvas;
		this.toolBar = toolBar;
		this.policy = policy;
		this.minSizePx = Math.max(1, minSizePx);

		startOnDrag = (policy == ARubberband.Policy.RECTANGLE
				|| policy == ARubberband.Policy.RECTANGLE_PRESERVE_ASPECT);
	}

	protected ARubberband.Policy rubberbandPolicy() {
		return policy;
	}

	protected Cursor activeCursor() {
		return (policy == ARubberband.Policy.NONE)
				? Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
				: Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}

	@Override
	public final void doneRubberbanding() {
		if (policy == ARubberband.Policy.NONE) return;

		final ARubberband rb = rubberband;
		final GestureContext gc = gesture;

		rubberband = null;
		gesture = null;

		if (rb == null) return;

		if (!rb.isGestureValid(minSizePx)) return;

		Rectangle bounds = rb.getRubberbandBounds();
		Point[] vertices = rb.getRubberbandVertices();

		rubberbanding(gc, bounds, vertices);
		canvas.repaint();
	}

	protected final void cancelRubberband() {
		ARubberband rb = rubberband;
		rubberband = null;
		gesture = null;
		if (rb != null) rb.cancel();
	}

	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }
	@Override public void mouseMoved(MouseEvent e) { }

	@Override
	public void mouseDragged(MouseEvent e) {
		if (gesture != null) gesture.update(e);
		if (startOnDrag && rubberband == null) init(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (policy == ARubberband.Policy.NONE) return;

		gesture = new GestureContext(toolBar, canvas, null, e.getPoint(), e);

		if (!startOnDrag && rubberband == null) init(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (gesture != null) gesture.update(e);
	}

	private void init(MouseEvent e) {
		if (policy == ARubberband.Policy.NONE) return;
		if (gesture == null) return;
		if (rubberband != null) return;

		ARubberband.Policy pol = Objects.requireNonNull(rubberbandPolicy(), "rubberbandPolicy");
		rubberband = RubberbandFactory.create(canvas, this, pol);
		if (rubberband == null) return;

		rubberband.setActive(true);

		// Click tools must see the click that created them.
		if (rubberband.isClickBased()) {
			rubberband.mousePressed(e);
		} else {
			rubberband.begin(gesture.getPressPoint());
		}
	}

	public abstract void rubberbanding(GestureContext gesture, Rectangle bounds, Point[] vertices);
}
