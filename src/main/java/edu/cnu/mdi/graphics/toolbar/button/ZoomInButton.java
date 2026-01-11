package edu.cnu.mdi.graphics.toolbar.button;

import edu.cnu.mdi.graphics.toolbar.ToolContext;
import edu.cnu.mdi.util.Environment;

/**
 * One-shot toolbar action that zooms in by scaling the world system.
 */
@SuppressWarnings("serial")
public class ZoomInButton extends ToolActionButton {

	/** Typical zoom in factor (< 1 zooms in). */
	private final double factor;

	public ZoomInButton(ToolContext ctx) {
		this(ctx, 0.8);
	}

	public ZoomInButton(ToolContext ctx, double factor) {
		super(ctx, Environment.MDI_RESOURCE_PATH + "images/svg/zoom_in.svg", "Zoom in");
		this.factor = factor;
	}

	@Override
	protected void perform(ToolContext ctx) {
		if (ctx.container() != null) {
			ctx.container().scale(factor);
		}
	}
}
