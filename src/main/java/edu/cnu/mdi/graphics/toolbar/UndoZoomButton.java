package edu.cnu.mdi.graphics.toolbar;

/**
 * One-shot toolbar action that undoes the last zoom operation.
 */
@SuppressWarnings("serial")
public class UndoZoomButton extends ToolActionButton {

    public UndoZoomButton(ToolContext ctx) {
        super(ctx, "images/undo_zoom.gif", "Undo zoom");
    }

    @Override
    protected void perform(ToolContext ctx) {
        if (ctx.container() != null) {
            ctx.container().undoLastZoom();
        }
    }
}
