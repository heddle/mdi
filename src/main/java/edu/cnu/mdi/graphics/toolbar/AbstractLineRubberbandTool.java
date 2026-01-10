package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Objects;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.rubberband.IRubberbanded;
import edu.cnu.mdi.graphics.rubberband.Rubberband;

/**
 * Base class for tools that use a {@link Rubberband.Policy#LINE}-style gesture
 * (i.e., a start point and a current/end point).
 * <p>
 * This is designed for tools like "Line" and similar gestures that are not
 * naturally represented by a bounding rectangle.
 * </p>
 * <p>
 * {@link #doneRubberbanding()} is invoked by {@link Rubberband} without a
 * {@link ToolContext}, so this class snapshots the necessary state at the start
 * of completion to avoid NPEs and re-entrancy issues.
 * </p>
 */
public abstract class AbstractLineRubberbandTool implements ITool, IRubberbanded {

	/** Minimum pixel delta to accept a line gesture. */
	private final int minDeltaPx;

	private Rubberband rubberband;
	private IContainer owner;
	private ToolController controller;

	protected AbstractLineRubberbandTool(int minDeltaPx) {
		this.minDeltaPx = Math.max(1, minDeltaPx);
	}

	/**
	 * @return the rubberband policy to use (typically
	 *         {@link Rubberband.Policy#LINE}).
	 */
	protected abstract Rubberband.Policy rubberbandPolicy();

	/** Called when the gesture completes with valid points. */
	protected abstract void createFromLine(IContainer owner, Point p0, Point p1);

	/** Override for custom cursor; default is crosshair. */
	protected Cursor activeCursor() {
		return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}

	@Override
	public Cursor cursor(ToolContext ctx) {
		return activeCursor();
	}

	@Override
	public void onSelected(ToolContext ctx) {
		controller = safeController(ctx);
	}

	@Override
	public final void mousePressed(ToolContext ctx, MouseEvent e) {
		if (ctx == null || e == null || (rubberband != null)) {
			return;
		}

		owner = ctx.container();
		if (owner == null) {
			return;
		}

		controller = safeController(ctx);

		Rubberband.Policy policy = Objects.requireNonNull(rubberbandPolicy(), "rubberbandPolicy");
		rubberband = new Rubberband(owner.getComponent(), this, policy);
		rubberband.setActive(true);
		rubberband.startRubberbanding(e.getPoint());
	}

	@Override
	public final void doneRubberbanding() {
		final Rubberband rb = rubberband;
		final IContainer c = owner;
		final ToolController tc = controller;

		rubberband = null;
		owner = null;
		controller = null;

		try {
			if (rb == null || c == null) {
				return;
			}

			Point p0 = rb.getStartPt();
			Point p1 = rb.getCurrentPt();

			if (!isValidLine(p0, p1)) {
				return;
			}

			createFromLine(c, p0, p1);

			c.selectAllItems(false);
			if (tc != null) {
				tc.resetToDefault();
			}
			c.refresh();

		} finally {
		}
	}

	@Override
	public void onDeselected(ToolContext ctx) {
		cancelRubberband();
		owner = null;
		controller = null;
	}

	protected final void cancelRubberband() {
		Rubberband rb = rubberband;
		rubberband = null;
		if (rb != null) {
			rb.cancel();
		}
	}

	private boolean isValidLine(Point p0, Point p1) {
		if (p0 == null || p1 == null) {
			return false;
		}
		return (Math.abs(p0.x - p1.x) > minDeltaPx) || (Math.abs(p0.y - p1.y) > minDeltaPx);
	}

	private static ToolController safeController(ToolContext ctx) {
		try {
			return (ctx == null) ? null : ctx.controller();
		} catch (RuntimeException ex) {
			return null;
		}
	}
}
