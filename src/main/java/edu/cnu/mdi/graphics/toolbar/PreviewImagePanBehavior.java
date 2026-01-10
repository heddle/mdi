package edu.cnu.mdi.graphics.toolbar;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import edu.cnu.mdi.graphics.GraphicsUtils;

/**
 * A {@link PanBehavior} that provides "preview-image" panning.
 * <p>
 * On mouse press, a snapshot of the canvas is captured. During dragging, the
 * snapshot is drawn offset from its starting position to provide a smooth
 * visual preview without continuously updating the container's world transform.
 * </p>
 * <p>
 * On mouse release, the pan is committed exactly once via
 * {@link edu.cnu.mdi.container.IContainer#pan(int, int)} using the total drag
 * delta from the initial press.
 * </p>
 * <p>
 * This approach is useful when continuous transform updates are expensive, or
 * when a view has special panning rules and prefers to commit the pan only at
 * the end of the gesture.
 * </p>
 *
 * @author heddle
 */
public class PreviewImagePanBehavior implements PanBehavior {

	private static final int PAN_MIN_DELTA = 4;

	private int startx = Integer.MIN_VALUE;
	private int starty = Integer.MIN_VALUE;

	private int lastx = Integer.MIN_VALUE;
	private int lasty = Integer.MIN_VALUE;

	private BufferedImage base;
	private BufferedImage buffer;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void begin(ToolContext ctx, MouseEvent e) {
		startx = e.getX();
		starty = e.getY();
		lastx = startx;
		lasty = starty;

		base = GraphicsUtils.getComponentImage(ctx.canvas());
		buffer = GraphicsUtils.getComponentImageBuffer(ctx.canvas());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void drag(ToolContext ctx, MouseEvent e) {
		if (startx == Integer.MIN_VALUE || base == null || buffer == null) {
			return;
		}

		int dx = e.getX() - lastx;
		int dy = e.getY() - lasty;

		if (Math.abs(dx) <= PAN_MIN_DELTA && Math.abs(dy) <= PAN_MIN_DELTA) {
			return;
		}

		lastx = e.getX();
		lasty = e.getY();

		int totalDx = e.getX() - startx;
		int totalDy = e.getY() - starty;

		Graphics gg = buffer.getGraphics();
		gg.setColor(ctx.canvas().getBackground());
		gg.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
		gg.drawImage(base, totalDx, totalDy, ctx.canvas());
		gg.dispose();

		Graphics g = ctx.canvas().getGraphics();
		g.drawImage(buffer, 0, 0, ctx.canvas());
		g.dispose();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void end(ToolContext ctx, MouseEvent e) {
		if (startx != Integer.MIN_VALUE) {
			int totalDx = e.getX() - startx;
			int totalDy = e.getY() - starty;

			if (Math.abs(totalDx) > PAN_MIN_DELTA || Math.abs(totalDy) > PAN_MIN_DELTA) {
				ctx.container().pan(totalDx, totalDy);
			}
		}

		startx = Integer.MIN_VALUE;
		starty = Integer.MIN_VALUE;
		lastx = Integer.MIN_VALUE;
		lasty = Integer.MIN_VALUE;

		base = null;
		buffer = null;
	}
}
