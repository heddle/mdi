package edu.cnu.mdi.hover;

import java.awt.Component;
import java.awt.Point;

/**
 * Event representing a hover action over a component, containing the source
 * component and the location of the hover in the component's coordinate space.
 */
public class HoverEvent {
    private final Component source;
    private final Point location;

    public HoverEvent(Component source, Point location) {
        this.source = source;
        this.location = location;
    }
    public Point getLocation() { return location; }
    public Component getSource() { return source; }
}