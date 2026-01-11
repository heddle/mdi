package edu.cnu.mdi.graphics.toolbar.button;

import edu.cnu.mdi.graphics.toolbar.ToolContext;
import edu.cnu.mdi.util.Environment;

/**
 * One-shot toolbar action that restores the container's default world system.
 */
@SuppressWarnings("serial")
public class WorldButton extends ToolActionButton {

	public WorldButton(ToolContext ctx) {
		super(ctx, Environment.MDI_RESOURCE_PATH + "images/svg/reset_zoom.svg", "Restore default zoom");
	}

	@Override
	protected void perform(ToolContext ctx) {
		if (ctx.container() != null) {
			ctx.container().restoreDefaultWorld();
		}
	}
}
