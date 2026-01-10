package edu.cnu.mdi.graphics.toolbar;

import java.awt.event.MouseEvent;

import edu.cnu.mdi.graphics.toolbar.tool.PanTool;

/**
 * Strategy interface for implementing panning behavior.
 * <p>
 * In the old framework, panning customization leaked into {@code PanButton} and
 * required hacks such as {@code container.isStandardPanning()}.
 * </p>
 * <p>
 * In the new framework, {@link PanTool} delegates the details of how panning
 * should occur to a {@code PanBehavior}. This supports:
 * </p>
 * <ul>
 * <li>direct panning that updates container transforms continuously</li>
 * <li>preview-image panning that visually drags a snapshot and commits
 * once</li>
 * <li>map-specific panning rules (wrap-around longitude, constrained axes,
 * etc.)</li>
 * </ul>
 *
 * @author heddle
 */
public interface PanBehavior {

	/**
	 * Begin a pan gesture (typically on mouse press).
	 *
	 * @param ctx the tool context (never null).
	 * @param e   the initiating mouse event (never null).
	 */
	void begin(ToolContext ctx, MouseEvent e);

	/**
	 * Update the pan gesture (typically on mouse drag).
	 *
	 * @param ctx the tool context (never null).
	 * @param e   the current mouse event (never null).
	 */
	void drag(ToolContext ctx, MouseEvent e);

	/**
	 * End the pan gesture (typically on mouse release).
	 *
	 * @param ctx the tool context (never null).
	 * @param e   the terminating mouse event (never null).
	 */
	void end(ToolContext ctx, MouseEvent e);
}
