package edu.cnu.mdi.mapping;

import java.awt.geom.Point2D;

/**
 * Represents a mutable projected Cartesian coordinate (x, y) in the map's
 * projection space. This class serves as a semantic wrapper around a projected
 * point rather than reusing {@link Point2D.Double} directly, improving clarity
 * when distinguishing geographic coordinates ({@link LatLon}) from projected
 * coordinates ({@code XY}).
 */
public class XY {

    /** Projected X coordinate. */
    private double x;

    /** Projected Y coordinate. */
    private double y;

    /**
     * Creates a new {@code XY} point at the origin.
     */
    public XY() {
        this(0.0, 0.0);
    }

    /**
     * Creates a new {@code XY} point with the given coordinates.
     *
     * @param x projected x value
     * @param y projected y value
     */
    public XY(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the projected x coordinate.
     *
     * @return the x coordinate
     */
    public double x() {
        return x;
    }

    /**
     * Returns the projected y coordinate.
     *
     * @return the y coordinate
     */
    public double y() {
        return y;
    }

    /**
     * Sets the projected x coordinate.
     *
     * @param x the new x value
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Sets the projected y coordinate.
     *
     * @param y the new y value
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Sets both projected coordinates at once.
     *
     * @param x projected x value
     * @param y projected y value
     */
    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Converts this coordinate to a {@link Point2D.Double}.
     *
     * @return a new {@link Point2D.Double} containing (x, y)
     */
    public Point2D.Double toPoint2D() {
        return new Point2D.Double(x, y);
    }

    @Override
    public String toString() {
        return String.format("XY[x=%.6f, y=%.6f]", x, y);
    }
}
