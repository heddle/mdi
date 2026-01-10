package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.event.MouseEvent;

import edu.cnu.mdi.container.IContainer;

/**
 * Provides shared services and state to {@link ITool} implementations.
 * <p>
 * Tools should not depend on Swing toolbar button subclasses or reach broadly
 * into UI classes to obtain dependencies. Instead, any runtime services a tool
 * needs (container, canvas component, toolbar) are provided via this context.
 * </p>
 * <p>
 * This reduces coupling, improves tool reuse across views/containers, and makes
 * tools easier to test.
 * </p>
 *
 * <h2>Popup support</h2>
 * <p>
 * Tools generally should not handle context menus themselves; popups are best
 * handled centrally by the mouse router (the toolbar/controller). However, this
 * context provides a convenience method if a tool needs to request the standard
 * popup policy.
 * </p>
 *
 * @author heddle
 */
public class ToolContext {

	private final IContainer container;
	private final BaseToolBar toolBar;
	private final CursorManager cursors = new CursorManager();

	/**
	 * Create a new tool context.
	 *
	 * @param container the container that owns the canvas and provides interaction
	 *                  hooks.
	 * @param toolBar   the owning toolbar (may be useful for status text and UI
	 *                  coordination).
	 */
	public ToolContext(IContainer container, BaseToolBar toolBar) {
		this.container = container;
		this.toolBar = toolBar;
	}

	/**
	 * Get the container that owns the canvas.
	 *
	 * @return the container (typically non-null).
	 */
	public IContainer container() {
		return container;
	}

	/**
	 * Get the toolbar that is routing events to tools.
	 *
	 * @return the toolbar (may be null if constructed that way).
	 */
	public BaseToolBar toolBar() {
		return toolBar;
	}

	/**
	 * Convenience accessor for the Swing component that receives mouse events.
	 *
	 * @return the canvas component (may be null if the container has no component).
	 */
	public Component canvas() {
		return (container == null) ? null : container.getComponent();
	}

	/**
	 * Get the cursor manager for this tool context.
	 *
	 * @return the cursor manager (never null).
	 */
	public CursorManager cursors() {
		return cursors;
	}

	/**
	 * Request that the standard popup policy be applied for the given event.
	 * <p>
	 * Prefer handling popups centrally in {@link BaseToolBar}, but this is
	 * available for tools that explicitly need it.
	 * </p>
	 *
	 * @param e the mouse event.
	 */
	public void showPopup(MouseEvent e) {
		PopupTriggerSupport.showPopup(container, e);
	}

	public ToolController controller() {
		if (toolBar == null) {
			throw new IllegalStateException("ToolContext has no ToolBar");
		}
		return toolBar.getToolController();
	}

	public void resetToDefaultTool() {
		if (toolBar() != null) {
			controller().resetToDefault();
		}
	}

}
