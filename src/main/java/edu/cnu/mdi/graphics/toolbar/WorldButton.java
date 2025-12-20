package edu.cnu.mdi.graphics.toolbar;

/**
 * One-shot toolbar action that restores the container's default world system.
 */
@SuppressWarnings("serial")
public class WorldButton extends ToolActionButton {

    public WorldButton(ToolContext ctx) {
        super(ctx, "images/svg/reset_zoom.svg", "Restore default zoom");
    }

    @Override
    protected void perform(ToolContext ctx) {
        if (ctx.container() != null) {
            ctx.container().restoreDefaultWorld();
        }
    }
}
