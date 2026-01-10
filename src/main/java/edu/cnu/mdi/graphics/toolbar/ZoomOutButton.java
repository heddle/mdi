package edu.cnu.mdi.graphics.toolbar;

/**
 * One-shot toolbar action that zooms out by scaling the world system.
 */
@SuppressWarnings("serial")
public class ZoomOutButton extends ToolActionButton {

	/** Typical zoom out factor (> 1 zooms out). */
	private final double factor;

	public ZoomOutButton(ToolContext ctx) {
		this(ctx, 1.25);
	}

	public ZoomOutButton(ToolContext ctx, double factor) {
		super(ctx, "images/svg/zoom_out.svg", "Zoom out");
		this.factor = factor;
	}

	@Override
	protected void perform(ToolContext ctx) {
		if (ctx.container() != null) {
			ctx.container().scale(factor);
		}
	}
}
