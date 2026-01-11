package edu.cnu.mdi.graphics.toolbar.button;

import edu.cnu.mdi.graphics.toolbar.ToolContext;
import edu.cnu.mdi.util.Environment;

/**
 * One-shot toolbar action that undoes the last zoom operation.
 */
@SuppressWarnings("serial")
public class UndoZoomButton extends ToolActionButton {

	public UndoZoomButton(ToolContext ctx) {
		super(ctx, Environment.MDI_RESOURCE_PATH + "images/svg/undo_zoom.svg", "Undo zoom");
	}

	@Override
	protected void perform(ToolContext ctx) {
		if (ctx.container() != null) {
			ctx.container().undoLastZoom();
		}
	}
}
