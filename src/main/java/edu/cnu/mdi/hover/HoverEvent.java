package edu.cnu.mdi.hover;

import java.awt.Component;
import java.awt.Point;
import java.util.Objects;

/**
 * Immutable hover event containing source component and the mouse location (in source coordinates).
 */
public final class HoverEvent {

    private final Component source;
    private final Point location;

    public HoverEvent(Component source, Point location) {
        this.source = Objects.requireNonNull(source, "source");
        // Defensive copy: prevents accidental mutation by listeners.
        this.location = (location == null) ? new Point(0, 0) : new Point(location);
    }

    public Component getSource() {
        return source;
    }

    /**
     * Returns a defensive copy of the hover location.
     */
    public Point getLocation() {
        return new Point(location);
    }
}