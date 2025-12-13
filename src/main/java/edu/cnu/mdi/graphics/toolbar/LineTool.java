package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.Point;

import edu.cnu.mdi.container.DrawingContainer;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.rubberband.Rubberband;
import edu.cnu.mdi.item.AItem;

/**
 * Tool that creates a line item by rubber-banding a line segment.
 */
public class LineTool extends AbstractLineRubberbandTool {

    public static final String ID = "line";

    public LineTool() {
        super(2); // legacy MIN_DELTA_PIXELS
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String toolTip() {
        return "Create a line";
    }

    @Override
    protected Rubberband.Policy rubberbandPolicy() {
        return Rubberband.Policy.LINE;
    }

    @Override
    protected Cursor activeCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    protected void createFromLine(IContainer owner, Point p0, Point p1) {
        if (owner instanceof DrawingContainer dc) {
            AItem item = dc.createLineItem(owner.getAnnotationList(), p0, p1);
            if (item != null) {
                item.setRightClickable(true);
                item.setDraggable(true);
                item.setRotatable(false);
                item.setResizable(true);
                item.setDeletable(true);
                item.setLocked(false);
            }
        }
    }
}
