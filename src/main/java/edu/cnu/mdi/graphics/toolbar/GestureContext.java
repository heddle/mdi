package edu.cnu.mdi.graphics.toolbar;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Objects;

/**
 * Context for a single mouse gesture (press -> move(s) -> release).
 * The press-time target and press point are immutable.
 */
public final class GestureContext {

    private final AToolBar toolBar;
    private final Component canvas;
    private final Object target;
    private final Point pressPoint;

    private Point previousPoint;
    private Point currentPoint;

    private boolean shift;
    private boolean ctrl;
    private boolean alt;
    private boolean meta;

    /** Create context at press time.
     * @param toolBar The toolbar.
     * @param canvas The canvas component.
     * @param target The target object at press time (may be null).
     * @param pressPoint The press point (screen coords).
     * @param e The mouse event at press time.
     */
    public GestureContext(AToolBar toolBar, Component canvas, Object target, Point pressPoint, MouseEvent e) {
        this.toolBar = Objects.requireNonNull(toolBar, "toolBar");
        this.canvas = Objects.requireNonNull(canvas, "canvas");
        this.target = target;
        this.pressPoint = new Point(Objects.requireNonNull(pressPoint, "pressPoint"));
        this.previousPoint = new Point(this.pressPoint);
        this.currentPoint = new Point(this.pressPoint);
        updateModifiers(e);
    }

    /** Update using event point + modifiers. */
    public void update(MouseEvent e) {
        update(e.getPoint(), e);
    }

    /** Update current point + modifiers. */
    public void update(Point current, MouseEvent e) {
        previousPoint = currentPoint;
        currentPoint = new Point(Objects.requireNonNull(current, "current"));
        updateModifiers(e);
    }

    //
    private void updateModifiers(MouseEvent e) {
        if (e == null) return;
        shift = e.isShiftDown();
        ctrl  = e.isControlDown();
        alt   = e.isAltDown();
        meta  = e.isMetaDown();
    }

    public AToolBar getToolBar() { return toolBar; }
    public Component getCanvas() { return canvas; }

    public Object getTarget() { return target; }

    public Point getPressPoint() { return new Point(pressPoint); }
    public Point getPreviousPoint() { return new Point(previousPoint); }
    public Point getCurrentPoint() { return new Point(currentPoint); }

    public int deltaX() { return currentPoint.x - previousPoint.x; }
    public int deltaY() { return currentPoint.y - previousPoint.y; }

    public int totalDeltaX() { return currentPoint.x - pressPoint.x; }
    public int totalDeltaY() { return currentPoint.y - pressPoint.y; }

    public boolean isShiftDown() { return shift; }
    public boolean isControlDown() { return ctrl; }
    public boolean isAltDown() { return alt; }
    public boolean isMetaDown() { return meta; }
}
