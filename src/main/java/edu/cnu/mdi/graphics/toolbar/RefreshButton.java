package edu.cnu.mdi.graphics.toolbar;

/**
 * One-shot toolbar action that refreshes the container.
 */
@SuppressWarnings("serial")
public class RefreshButton extends ToolActionButton {

	public RefreshButton(ToolContext ctx) {
		super(ctx, "images/svg/refresh.svg", "Refresh the view");
	}

	@Override
	protected void perform(ToolContext ctx) {
		if (ctx.container() != null) {
			ctx.container().refresh();
		}
	}
}
