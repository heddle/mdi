package edu.cnu.mdi.graphics.toolbar;

import edu.cnu.mdi.graphics.toolbar.tool.ITool;

/**
 * Listener notified when the active tool changes.
 * <p>
 * Intended for UI synchronization (toolbar buttons, menus, status panels), not
 * for tool behavior or cursor management.
 * </p>
 */
@FunctionalInterface
public interface ToolSelectionListener {

	/**
	 * Called after a new tool becomes active.
	 *
	 * @param activeTool the newly active tool (never null)
	 */
	void activeToolChanged(ITool activeTool);
}
