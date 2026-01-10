package edu.cnu.mdi.graphics.toolbar;

import java.awt.event.MouseEvent;

/**
 * A {@link PanBehavior} that pans by applying the drag delta directly to the
 * container via {@link edu.cnu.mdi.container.IContainer#pan(int, int)}.
 * <p>
 * This behavior is simple and responsive: the container is updated continuously
 * as the mouse drags.
 * </p>
 *
 * @author heddle
 */
public class DirectPanBehavior implements PanBehavior {

	private static final int PAN_MIN_DELTA = 4;

	private int startx = Integer.MIN_VALUE;
	private int starty = Integer.MIN_VALUE;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void begin(ToolContext ctx, MouseEvent e) {
		startx = e.getX();
		starty = e.getY();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void drag(ToolContext ctx, MouseEvent e) {
		if (startx == Integer.MIN_VALUE) {
			return;
		}

		int dx = e.getX() - startx;
		int dy = e.getY() - starty;

		if (Math.abs(dx) > PAN_MIN_DELTA || Math.abs(dy) > PAN_MIN_DELTA) {
			ctx.container().pan(dx, dy);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void end(ToolContext ctx, MouseEvent e) {
		drag(ctx, e);
		startx = Integer.MIN_VALUE;
		starty = Integer.MIN_VALUE;
	}
}
