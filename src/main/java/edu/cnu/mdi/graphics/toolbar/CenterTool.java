package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;

import edu.cnu.mdi.container.IContainer;

/**
 * Tool that recenters the view on the clicked canvas location.
 * <p>
 * When active, a single left-click recenters the owning {@link IContainer} on
 * the clicked screen point. This is the tool-framework replacement for the
 * legacy {@code CenterButton}.
 * </p>
 *
 * <h2>Behavior</h2>
 * <ul>
 * <li>On click: calls {@link IContainer#prepareToZoom()} and then
 * {@link IContainer#recenter(Point)} using the mouse position.</li>
 * <li>Does not start a drag gesture; it is a click-to-act tool.</li>
 * </ul>
 *
 * <h2>Cursor</h2>
 * <p>
 * The legacy implementation used a custom cursor image. If you have a cursor
 * factory/caching utility in the new framework, you can swap {@link #cursor()}
 * to return that custom cursor. For now, a predefined crosshair cursor is used.
 * </p>
 *
 * @author heddle
 */
public class CenterTool implements ITool {

	/** Tool id used for registration/selection. */
	public static final String ID = "center";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String toolTip() {
		return "Recenter the view";
	}

	@Override
	public Cursor cursor(ToolContext ctx) {
		// Replace with a custom cursor if you add a CursorFactory later.
		return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}

	/**
	 * Recenters the container on a single click.
	 *
	 * @param ctx tool context (non-null).
	 * @param e   mouse event (non-null).
	 */
	@Override
	public void mouseClicked(ToolContext ctx, MouseEvent e) {
		IContainer c = ctx.container();
		if (c == null || c.getComponent() == null || !c.getComponent().isEnabled()) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		c.prepareToZoom();
		c.recenter(e.getPoint());
		c.refresh();
	}
}
