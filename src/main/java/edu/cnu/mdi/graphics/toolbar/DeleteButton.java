package edu.cnu.mdi.graphics.toolbar;

import edu.cnu.mdi.container.IContainer;

/**
 * One-shot toolbar button that deletes all currently selected items from the
 * active container.
 * <p>
 * This is the tool-framework replacement for the legacy {@code DeleteButton}.
 * It does not participate in tool selection; it simply performs an action via
 * {@link ToolContext}.
 * </p>
 * <p>
 * After deletion, this button:
 * </p>
 * <ul>
 * <li>resets the active tool back to the default tool (typically pointer)</li>
 * <li>updates toolbar enable/disable state (e.g. delete enabled)</li>
 * <li>refreshes the container</li>
 * </ul>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class DeleteButton extends ToolActionButton {

	/**
	 * Create a delete action button.
	 *
	 * @param ctx the tool context (must not be null)
	 */
	public DeleteButton(ToolContext ctx) {
		super(ctx, "images/svg/delete.svg", "Delete selected items");
	}

	@Override
	protected void perform(ToolContext ctx) {
		if (ctx == null) {
			return;
		}

		IContainer c = ctx.container();
		if (c == null) {
			return;
		}

		c.deleteSelectedItems();

		// Return UI to a sane default state.
		ctx.resetToDefaultTool();

		// Keep toolbar widgets (like delete enabled state) in sync.
		if (c.getToolBar() != null) {
			c.getToolBar().updateButtonState();
		}

		c.refresh();
	}
}
