package edu.cnu.mdi.mapping;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;

/**
 * Defines the contract for map projections used within the MDI framework.
 * <p>
 * Conventions:
 * <ul>
 *   <li>Geographic coordinates are represented by {@link LatLon} using
 *       lambda (longitude) and phi (latitude), in radians.</li>
 *   <li>Projected coordinates are represented by {@link XY}.</li>
 *   <li>Visual styling is controlled via a {@link MapTheme} instance.</li>
 * </ul>
 * </p>
 */
public interface IMapProjection {
	

    // ---------------------------------------------------------------------
    // Transformations
    // ---------------------------------------------------------------------

    /**
     * Converts a geographic location (latitude/longitude) to projected
     * Cartesian coordinates.
     *
     * @param latLon the geographic location (λ, φ), in radians
     * @param xy     the output projected point to be written
     */
    void latLonToXY(LatLon latLon, XY xy);

    /**
     * Converts a projected Cartesian point to a geographic location
     * (latitude/longitude).
     *
     * @param xy     the projected Cartesian point
     * @param latLon the output geographic location (λ, φ), in radians
     */
    void xyToLatLon(XY xy, LatLon latLon);

    /**
     * Convenience method that converts a geographic location to a new
     * projected Cartesian point.
     *
     * @param latLon the geographic location (λ, φ), in radians
     * @return a new {@link XY} containing the projected coordinates
     */
    default XY latLonToXY(LatLon latLon) {
        XY xy = new XY();
        latLonToXY(latLon, xy);
        return xy;
    }

    /**
     * Convenience method that converts a projected Cartesian point to a new
     * geographic location.
     *
     * @param xy the projected Cartesian point
     * @return a new {@link LatLon} containing the geographic location
     */
    default LatLon xyToLatLon(XY xy) {
        LatLon latLon = new LatLon();
        xyToLatLon(xy, latLon);
        return latLon;
    }

    // ---------------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------------

    /**
     * Draws the outline of the map supported by this projection into the given
     * container using the provided graphics context and current {@link MapTheme}.
     *
     * @param g2        the {@link Graphics2D} context to draw into
     * @param container the drawing container
     */
    void drawMapOutline(Graphics2D g2, IContainer container);

    /**
     * Determines whether a projected Cartesian point lies within the valid
     * region of this map projection.
     *
     * @param xy the projected Cartesian point to test
     * @return {@code true} if the point lies on the map, {@code false} otherwise
     */
    boolean isPointOnMap(XY xy);
    
    /**
	 * Convenience method to determine whether a projected Cartesian point
	 * lies within the valid region of this map projection.
	 */
    default boolean isPointOnMap(double x, double y) {
		return isPointOnMap(new XY(x, y));
	}
    
    /**
	 * Convenience method to get the LatLon for a given projected x,y point.
	 * 
	 * @param x     the projected x coordinate
	 * @param y     the projected y coordinate
	 * @param latlon the output LatLon object to populate
	 * @return true if the point is on the map, false otherwise
	 */
    default boolean getLatLon(double x, double y, LatLon latlon) {
    	XY xy = new XY(x, y);
    	boolean onMap = isPointOnMap(xy);
    	if (!onMap) {
    		return false;
    	}
		xyToLatLon(new XY(x, y), latlon);
		return true;
    }

    /**
     * Draws a line of constant latitude (a parallel) as part of the graticule.
     * The current {@link MapTheme} should be used to choose color and stroke.
     *
     * @param g2        the {@link Graphics2D} context to draw into
     * @param container the drawing container
     * @param latitude  the latitude of the line in radians (typically in [-π/2, π/2])
     */
    void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude);

    /**
     * Draws a line of constant longitude (a meridian) as part of the graticule.
     * The current {@link MapTheme} should be used to choose color and stroke.
     *
     * @param g2        the {@link Graphics2D} context to draw into
     * @param container the drawing container
     * @param longitude the longitude of the line in radians, typically in [-π, π]
     */
    void drawLongitudeLine(Graphics2D g2, IContainer container, double longitude);

    /**
     * Determines whether a given geographic location is visible in this
     * projection.
     *
     * @param latLon the geographic location to test (λ, φ), in radians
     * @return {@code true} if the point is visible in this projection;
     *         {@code false} otherwise
     */
    boolean isPointVisible(LatLon latLon);

    // ---------------------------------------------------------------------
    // Projection metadata
    // ---------------------------------------------------------------------

    /**
     * Returns the projection type for this implementation.
     *
     * @return the projection enum value
     */
    EProjection getProjection();

    /**
     * Returns a human-readable name for this projection.
     *
     * @return the name of the projection
     */
    default String name() {
        EProjection projection = getProjection();
        return (projection != null) ? projection.getName() : getClass().getSimpleName();
    }

    /**
     * Returns the bounding rectangle in projected XY space that this projection
     * covers.
     *
     * @return the XY bounding rectangle of the projection
     */
    Rectangle2D.Double getXYBounds();

    // ---------------------------------------------------------------------
    // Theme handling
    // ---------------------------------------------------------------------

    /**
     * Returns the {@link MapTheme} currently used by this projection.
     *
     * @return the active map theme
     */
    MapTheme getTheme();

    /**
     * Sets the {@link MapTheme} used by this projection for choosing colors and
     * stroke widths during rendering.
     *
     * @param theme the new theme (must not be {@code null})
     */
    void setTheme(MapTheme theme);
      
    /**
     * Creates a clipping shape in local (container) coordinates representing
     * the visible map region for this projection.
     * <p>
     * The returned shape is suitable for use with
     * {@link java.awt.Graphics2D#setClip(Shape)} in order to restrict drawing
     * (e.g., to fill oceans) to the projected map area.
     * </p>
     *
     * @param container the container providing the world-to-local transform
     * @return a shape in local coordinates representing the visible map area
     */
    Shape createClipShape(IContainer container);

    /**
     * Convenience method to fill the map area with the theme's ocean color
     * using clipping.
     *
     * @param g2        graphics context
     * @param container container used for coordinate transforms
     */
    default void fillOcean(Graphics2D g2, IContainer container) {
        Shape clip = createClipShape(container);
        if (clip == null) {
            return;
        }

        java.awt.Color oldColor = g2.getColor();

        g2.setColor(getTheme().getOceanColor());
        g2.fill(clip);           // <-- fill the map shape itself
        g2.setColor(oldColor);    }
}
