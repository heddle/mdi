package edu.cnu.mdi.mapping;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;

/**
 * Defines the contract for map projections used within the MDI framework.
 * <p>
 * Conventions:
 * <ul>
 *   <li>Geographic coordinates are always in radians.</li>
 *   <li>A geographic point is passed as a {@link java.awt.geom.Point2D.Double}
 *       where {@code x = λ} (longitude, in radians) and
 *       {@code y = φ} (latitude, in radians).</li>
 *   <li>Projected (map) coordinates are also represented by
 *       {@link java.awt.geom.Point2D.Double} and are referred to as "world"
 *       or "projection-space" coordinates. The {@link IContainer} is
 *       responsible for converting world coordinates to device coordinates
 *       (pixels).</li>
 * </ul>
 */
public interface IMapProjection {

    // ---------------------------------------------------------------------
    // Transformations
    // ---------------------------------------------------------------------

    /**
     * Forward projection from geographic coordinates (λ, φ) to projection-space
     * coordinates (x, y).
     *
     * @param latLon geographic point (in radians). {@code x = λ}, {@code y = φ}
     * @param xy     output projection point; must not be {@code null}
     */
    void latLonToXY(Point2D.Double latLon, Point2D.Double xy);

    /**
     * Inverse projection from projection-space coordinates (x, y) back to
     * geographic coordinates (λ, φ).
     *
     * @param latLon output geographic point (in radians).
     *               {@code x = λ}, {@code y = φ}; must not be {@code null}
     * @param xy     input projection point
     */
    void latLonFromXY(Point2D.Double latLon, Point2D.Double xy);

    // ---------------------------------------------------------------------
    // Drawing support
    // ---------------------------------------------------------------------

    /**
     * Draw the outline of the map supported by this projection using the
     * provided graphics context and container. Implementations typically:
     * <ol>
     *   <li>Construct a {@link java.awt.geom.Path2D} in world coordinates
     *       describing the boundary of the valid projected domain.</li>
     *   <li>Transform those points to screen space using the container's
     *       world-to-local transform.</li>
     *   <li>Stroke the resulting path using colors and strokes from the
     *       current {@link MapTheme}.</li>
     * </ol>
     *
     * @param g2        graphics context to draw into
     * @param container container providing world-to-local transforms
     */
    void drawMapOutline(Graphics2D g2, IContainer container);

    /**
     * Test whether a projection-space point lies within the valid domain of
     * this projection.
     *
     * @param xy the projection-space point
     * @return {@code true} if the point is on the map, {@code false} otherwise
     */
    boolean isPointOnMap(Point2D.Double xy);

    /**
     * Draw a line of constant latitude (a parallel) in this projection.
     * Implementations typically approximate the parallel by sampling in
     * longitude, projecting each sample, and connecting them with a polyline.
     *
     * @param g2        graphics context
     * @param container container providing world-to-local transforms
     * @param latitude  latitude φ in radians (usually in [-π/2, π/2])
     */
    void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude);

    /**
     * Draw a line of constant longitude (a meridian) in this projection.
     * Implementations typically approximate the meridian by sampling in
     * latitude, projecting each sample, and connecting them with a polyline.
     *
     * @param g2        graphics context
     * @param container container providing world-to-local transforms
     * @param longitude longitude λ in radians (usually in [-π, π])
     */
    void drawLongitudeLine(Graphics2D g2, IContainer container, double longitude);

    /**
     * Test whether a geographic location is visible in this projection.
     * <ul>
     *   <li>Global projections (e.g. Mercator, Mollweide) usually return
     *       {@code true} for all valid lat/lon values.</li>
     *   <li>Hemispherical projections (e.g. orthographic) typically return
     *       {@code true} only for locations on the visible hemisphere.</li>
     * </ul>
     *
     * @param latLon geographic point in radians; {@code x = λ}, {@code y = φ}
     * @return {@code true} if the point is visible, {@code false} otherwise
     */
    boolean isPointVisible(Point2D.Double latLon);

    // ---------------------------------------------------------------------
    // Metadata & theme
    // ---------------------------------------------------------------------

    /**
     * Projection type enum for this implementation.
     *
     * @return the corresponding {@link EProjection} value
     */
    EProjection getProjection();

    /**
     * Human-readable name for this projection. The default implementation
     * delegates to {@link EProjection#getName()} if available, otherwise
     * falls back to the simple class name.
     *
     * @return projection name suitable for UI labels
     */
    default String name() {
        EProjection projection = getProjection();
        return (projection != null) ? projection.getName()
                                    : getClass().getSimpleName();
    }

    /**
     * Bounding box of the projection-space domain, in world coordinates.
     * This rectangle is typically used by containers to build a world-to-screen
     * transform.
     *
     * @return a rectangle describing the min/max x and y in projection space
     */
    Rectangle2D.Double getXYBounds();

    /**
     * Get the current {@link MapTheme}. Implementations should never return
     * {@code null} once initialized.
     *
     * @return the active map theme
     */
    MapTheme getTheme();

    /**
     * Set the {@link MapTheme} used by this projection for outlines and
     * graticule drawing.
     *
     * @param theme non-{@code null} map theme
     * @throws IllegalArgumentException if {@code theme} is {@code null}
     */
    void setTheme(MapTheme theme);

    /**
     * Create a clip shape in <em>device coordinates</em> corresponding to
     * the map's valid domain. This is typically used to:
     * <ul>
     *   <li>Clip land/ocean rendering to the map region.</li>
     *   <li>Fill the ocean background via {@link #fillOcean(Graphics2D, IContainer)}.</li>
     * </ul>
     *
     * @param container container providing the world-to-local transform
     * @return a device-space {@link Shape}, or {@code null} if no clip is defined
     */
    Shape createClipShape(IContainer container);

    // ---------------------------------------------------------------------
    // Default helpers
    // ---------------------------------------------------------------------

    /**
     * Convenience helper that fills the map domain with the ocean color from
     * the current {@link MapTheme}.
     *
     * @param g2        graphics context
     * @param container container providing world-to-local transforms
     */
    default void fillOcean(Graphics2D g2, IContainer container) {
        Shape clip = createClipShape(container);
        if (clip == null) {
            return;
        }

        java.awt.Color oldColor = g2.getColor();
        g2.setColor(getTheme().getOceanColor());
        g2.fill(clip);
        g2.setColor(oldColor);
    }

    /**
     * Wrap a longitude value to the canonical range [-π, π).
     *
     * @param lon longitude in radians
     * @return wrapped longitude in [-π, π)
     */
    default double wrapLongitude(double lon) {
		while (lon <= -Math.PI) {
			lon += 2 * Math.PI;
		}
		while (lon > Math.PI) {
			lon -= 2 * Math.PI;
		}
		return lon;
	}

    /**
     * Wrap a latitude value to the range [-π/2, π/2]. This is mainly useful
     * for helper code; most projections should clamp, not wrap, latitude.
     *
     * @param lat latitude in radians
     * @return wrapped latitude in [-π/2, π/2]
     */
    default double wrapLatitude(double lat) {
        return lat - Math.PI * Math.floor((lat + Math.PI / 2.0) / Math.PI);
    }

    /**
     * Test to see if the line between two longitudes crosses the
     * seam (the line at the central longitude). This is a test
     * for the dreaded wrapping problem.
     * @param lon1 one longitude in radians
     * @param lon2 the other longitude in radians
     * @return {@code true} if the line between the two longitudes
     * 	   crosses the seam; {@code false} otherwise
     */
    default boolean crossesSeam(double lon1, double lon2) {
    	return false;
    }
}
