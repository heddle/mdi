package edu.cnu.mdi.mapping;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;

/**
 * Contract for the map projections used within the MDI framework.
 *
 * <h2>Coordinate conventions</h2>
 * <ul>
 *   <li>All geographic coordinates are in <em>radians</em>.</li>
 *   <li>A geographic point is represented as a {@link Point2D.Double} where
 *       {@code x = λ} (longitude) and {@code y = φ} (latitude).</li>
 *   <li>Projected "world" coordinates are also {@link Point2D.Double} values
 *       in the projection's own XY plane (e.g. [-π, π]×[MIN_Y, MAX_Y] for
 *       Mercator, or a unit disk for Orthographic). The {@link IContainer} is
 *       responsible for converting world coordinates to device pixels.</li>
 * </ul>
 *
 * <h2>Implementing a new projection</h2>
 * <ol>
 *   <li>Add a constant to {@link EProjection}.</li>
 *   <li>Implement this interface (forward, inverse, visibility, bounds,
 *       outline, graticule drawing).</li>
 *   <li>Add a {@code case} to {@link ProjectionFactory#create(EProjection, MapTheme, Point2D.Double)}.</li>
 *   <li>Add a {@code case} to {@link MapContainer#recenter(java.awt.Point)} if
 *       the projection supports re-centering.</li>
 * </ol>
 *
 * <h2>Graticule color</h2>
 * <p>Implementations of {@link #drawLatitudeLine} and
 * {@link #drawLongitudeLine} <strong>must</strong> use
 * {@link MapTheme#getGraticuleColor()} rather than hardcoding a color.
 * Hardcoding defeats theme switching (light / dark / blue).</p>
 */
public interface IMapProjection {

    // =========================================================================
    // Coordinate transforms
    // =========================================================================

    /**
     * Forward projection: geographic (λ, φ) → projection-space (x, y).
     *
     * <p>Implementations should clamp the latitude to their valid range
     * before computing to avoid {@code NaN} or {@code Infinity} results at
     * the poles.</p>
     *
     * <p>For hemispherical projections (e.g. orthographic) a point on the
     * far side of the globe should set {@code xy} to
     * {@code (Double.NaN, Double.NaN)} rather than throwing, because callers
     * test {@code isPointVisible} separately.</p>
     *
     * @param latLon input geographic point in radians ({@code x=λ, y=φ});
     *               must not be {@code null}
     * @param xy     output projection-space point; must not be {@code null}
     */
    void latLonToXY(Point2D.Double latLon, Point2D.Double xy);

    /**
     * Inverse projection: projection-space (x, y) → geographic (λ, φ).
     *
     * <p>If the input point lies outside the valid projection domain,
     * implementations should set {@code latLon} to
     * {@code (Double.NaN, Double.NaN)} rather than throwing.</p>
     *
     * @param latLon output geographic point in radians ({@code x=λ, y=φ});
     *               must not be {@code null}
     * @param xy     input projection-space point; must not be {@code null}
     */
    void latLonFromXY(Point2D.Double latLon, Point2D.Double xy);

    // =========================================================================
    // Visibility tests
    // =========================================================================

    /**
     * Tests whether a geographic location is visible in this projection.
     *
     * <p>Global projections (Mercator, Mollweide, Lambert) generally return
     * {@code true} for any point within their latitude range. Hemispherical
     * projections (Orthographic) return {@code true} only for points on the
     * visible hemisphere.</p>
     *
     * @param latLon geographic point in radians ({@code x=λ, y=φ})
     * @return {@code true} if the point is visible; {@code false} otherwise
     */
    boolean isPointVisible(Point2D.Double latLon);

    /**
     * Tests whether a <em>projection-space</em> point lies within the valid
     * domain of this projection.
     *
     * <p>This is used after {@link #latLonToXY} to guard against rendering
     * points that fall slightly outside the projection boundary due to
     * floating-point rounding.</p>
     *
     * @param xy projection-space point
     * @return {@code true} if the point is on (or inside) the map;
     *         {@code false} otherwise
     */
    boolean isPointOnMap(Point2D.Double xy);

    // =========================================================================
    // Drawing helpers
    // =========================================================================

    /**
     * Draws the outer boundary of the projection domain.
     *
     * <p>Typical implementations build a {@link java.awt.geom.Path2D} in
     * world coordinates, transform each vertex to screen space via
     * {@code container.worldToLocal}, then stroke the path using colors and
     * widths from the current {@link MapTheme}.</p>
     *
     * @param g2        graphics context to draw into
     * @param container container providing the world-to-local transform
     */
    void drawMapOutline(Graphics2D g2, IContainer container);

    /**
     * Draws a line of constant latitude (a parallel) at the given latitude.
     *
     * <p>Implementations should approximate the curve by sampling at a
     * sufficient number of longitude values, projecting each sample, and
     * connecting the resulting screen points with a polyline. The line color
     * <strong>must</strong> be {@link MapTheme#getGraticuleColor()} so that
     * theme switching takes full effect.</p>
     *
     * @param g2        graphics context
     * @param container container providing the world-to-local transform
     * @param latitude  latitude φ in radians, typically in [-π/2, π/2]
     */
    void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude);

    /**
     * Draws a line of constant longitude (a meridian) at the given longitude.
     *
     * <p>Implementations should approximate the curve by sampling at a
     * sufficient number of latitude values, projecting each sample, and
     * connecting the resulting screen points with a polyline. The line color
     * <strong>must</strong> be {@link MapTheme#getGraticuleColor()} so that
     * theme switching takes full effect.</p>
     *
     * @param g2        graphics context
     * @param container container providing the world-to-local transform
     * @param longitude longitude λ in radians, typically in [-π, π]
     */
    void drawLongitudeLine(Graphics2D g2, IContainer container, double longitude);

    /**
     * Creates a clip {@link Shape} in <em>device</em> coordinates that matches
     * the projection's valid domain.
     *
     * <p>This shape is used to:
     * <ul>
     *   <li>Clip land/ocean rendering to the map region.</li>
     *   <li>Fill the ocean background via {@link #fillOcean}.</li>
     * </ul>
     *
     * @param container container providing the world-to-local transform
     * @return a device-space {@link Shape}, or {@code null} if no clip is
     *         defined for this projection
     */
    Shape createClipShape(IContainer container);

    // =========================================================================
    // Metadata and theme
    // =========================================================================

    /**
     * Returns the {@link EProjection} enum constant that identifies this
     * implementation.
     *
     * @return the projection type; never {@code null}
     */
    EProjection getProjection();

    /**
     * Returns a human-readable name for this projection, suitable for UI
     * labels.
     *
     * <p>The default implementation delegates to
     * {@link EProjection#getName()} when {@link #getProjection()} returns a
     * non-null value, and falls back to the simple class name otherwise.</p>
     *
     * @return projection display name; never {@code null}
     */
    default String name() {
        EProjection projection = getProjection();
        return (projection != null) ? projection.getName() : getClass().getSimpleName();
    }

    /**
     * Returns the bounding rectangle of the projection-space domain in world
     * coordinates.
     *
     * <p>This rectangle is used by {@link IContainer} implementations to
     * build the initial world-to-screen transform. The returned rectangle
     * should tightly enclose the entire valid projected domain (e.g.
     * [-π, π] × [MIN_Y, MAX_Y] for Mercator).</p>
     *
     * @return bounding rectangle in projection space; never {@code null}
     */
    Rectangle2D.Double getXYBounds();

    /**
     * Returns the {@link MapTheme} currently used by this projection for
     * drawing outlines and graticule lines.
     *
     * <p>Implementations must never return {@code null} once the theme has
     * been set.</p>
     *
     * @return the active map theme
     */
    MapTheme getTheme();

    /**
     * Sets the {@link MapTheme} used by this projection for all subsequent
     * drawing operations.
     *
     * @param theme the new theme; must not be {@code null}
     * @throws IllegalArgumentException if {@code theme} is {@code null}
     */
    void setTheme(MapTheme theme);

    // =========================================================================
    // Default helpers
    // =========================================================================

    /**
     * Fills the projection domain with the ocean color from the current
     * {@link MapTheme}.
     *
     * <p>This default implementation delegates to {@link #createClipShape} to
     * obtain the fill boundary and then fills it with
     * {@link MapTheme#getOceanColor()}. Implementations that need different
     * ocean-fill behaviour may override this method.</p>
     *
     * @param g2        graphics context
     * @param container container providing the world-to-local transform
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
     * Wraps a longitude value to the canonical half-open range (-π, π].
     *
     * <p>This is the standard normalization used throughout the mapping
     * subsystem. Note the range is half-open: -π maps to π.</p>
     *
     * @param lon longitude in radians (any value)
     * @return equivalent longitude in (-π, π]
     */
    default double wrapLongitude(double lon) {
        while (lon <= -Math.PI) lon += 2 * Math.PI;
        while (lon >   Math.PI) lon -= 2 * Math.PI;
        return lon;
    }

    /**
     * Wraps a latitude value to the range [-π/2, π/2].
     *
     * <p>This helper exists primarily for completeness; most projection code
     * should <em>clamp</em> rather than wrap latitude because values outside
     * the range represent the interior of the Earth, not a point on the
     * opposite side of the globe.</p>
     *
     * @param lat latitude in radians (any value)
     * @return equivalent latitude in [-π/2, π/2]
     */
    default double wrapLatitude(double lat) {
        return lat - Math.PI * Math.floor((lat + Math.PI / 2.0) / Math.PI);
    }

    /**
     * Tests whether the segment between two longitudes crosses the projection
     * seam (the anti-meridian relative to the central longitude).
     *
     * <p>Global cylindrical projections such as {@link MercatorProjection} and
     * {@link MollweideProjection} override this method to detect wrap-around
     * so that polygon edges that cross the seam can be split rather than drawn
     * as a horizontal streak across the map.</p>
     *
     * <p>The default implementation returns {@code false}, which is correct
     * for hemispherical projections ({@link OrthographicProjection},
     * {@link LambertEqualAreaProjection}) where no seam split is needed.</p>
     *
     * @param lon1 first longitude in radians
     * @param lon2 second longitude in radians
     * @return {@code true} if the segment crosses the seam; {@code false}
     *         otherwise
     */
    default boolean crossesSeam(double lon1, double lon2) {
        return false;
    }
}
