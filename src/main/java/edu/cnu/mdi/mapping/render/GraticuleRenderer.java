package edu.cnu.mdi.mapping.render;

import java.awt.Graphics2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.theme.MapTheme;

/**
 * Renders a map outline and latitude/longitude graticule lines for a given
 * {@link IMapProjection}.
 *
 * <h2>Rendering steps</h2>
 * <ol>
 *   <li>Optionally draws the projection's outer boundary via
 *       {@link IMapProjection#drawMapOutline(Graphics2D, IContainer)}.</li>
 *   <li>Draws latitude parallels from -π/2 to +π/2 in steps of
 *       {@link #getLatitudeStepRad()}.</li>
 *   <li>Draws longitude meridians from -π to +π in steps of
 *       {@link #getLongitudeStepRad()}.</li>
 * </ol>
 *
 * <p>Actual drawing is fully delegated to the projection, so the graticule
 * appearance (color, stroke, curve shape) automatically matches the
 * projection and its {@link MapTheme}. This class is intentionally thin — it
 * is only responsible for iterating over the lat/lon grid and dispatching to
 * the projection.</p>
 *
 * <h2>Graticule color</h2>
 * <p>The line color is determined by each projection's
 * {@link IMapProjection#drawLatitudeLine} and
 * {@link IMapProjection#drawLongitudeLine} implementations, which are required
 * to use {@link MapTheme#getGraticuleColor()} rather than a hardcoded value.
 * Changing the theme on the projection automatically changes the graticule
 * color on the next repaint.</p>
 *
 * <h2>Step precision</h2>
 * <p>A small epsilon ({@code 1e-9} rad) is added to the loop upper bound so
 * that the final step (e.g. exactly +π/2 or +π) is not silently skipped due
 * to floating-point accumulation errors.</p>
 */
public final class GraticuleRenderer {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** The projection to which graticule drawing is delegated. */
    private final IMapProjection projection;

    /**
     * Step size for latitude parallels in radians.
     * Defaults to 15° ({@code Math.toRadians(15)}).
     */
    private double latitudeStepRad = Math.toRadians(15.0);

    /**
     * Step size for longitude meridians in radians.
     * Defaults to 15° ({@code Math.toRadians(15)}).
     */
    private double longitudeStepRad = Math.toRadians(15.0);

    /**
     * Whether the map outline is drawn before the graticule lines.
     * Defaults to {@code true}.
     */
    private boolean drawOutline = true;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a graticule renderer backed by the given projection.
     *
     * @param projection the projection to use for outline and line drawing;
     *                   must not be {@code null}
     * @throws IllegalArgumentException if {@code projection} is {@code null}
     */
    public GraticuleRenderer(IMapProjection projection) {
        if (projection == null) {
            throw new IllegalArgumentException("projection must not be null");
        }
        this.projection = projection;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the associated projection.
     *
     * @return the projection used by this renderer; never {@code null}
     */
    public IMapProjection getProjection() { return projection; }

    /**
     * Returns the latitude step size in radians.
     *
     * @return latitude step in radians (&gt; 0)
     */
    public double getLatitudeStepRad() { return latitudeStepRad; }

    /**
     * Sets the latitude step size in radians.
     *
     * @param latitudeStepRad step size in radians; must be &gt; 0
     * @throws IllegalArgumentException if {@code latitudeStepRad} &le; 0
     */
    public void setLatitudeStepRad(double latitudeStepRad) {
        if (latitudeStepRad <= 0.0) {
            throw new IllegalArgumentException("latitudeStepRad must be > 0");
        }
        this.latitudeStepRad = latitudeStepRad;
    }

    /**
     * Convenience method that sets the latitude step size in degrees.
     *
     * @param latitudeStepDeg step size in degrees; must be &gt; 0
     * @throws IllegalArgumentException if {@code latitudeStepDeg} &le; 0
     */
    public void setLatitudeStepDeg(double latitudeStepDeg) {
        setLatitudeStepRad(Math.toRadians(latitudeStepDeg));
    }

    /**
     * Returns the longitude step size in radians.
     *
     * @return longitude step in radians (&gt; 0)
     */
    public double getLongitudeStepRad() { return longitudeStepRad; }

    /**
     * Sets the longitude step size in radians.
     *
     * @param longitudeStepRad step size in radians; must be &gt; 0
     * @throws IllegalArgumentException if {@code longitudeStepRad} &le; 0
     */
    public void setLongitudeStepRad(double longitudeStepRad) {
        if (longitudeStepRad <= 0.0) {
            throw new IllegalArgumentException("longitudeStepRad must be > 0");
        }
        this.longitudeStepRad = longitudeStepRad;
    }

    /**
     * Convenience method that sets the longitude step size in degrees.
     *
     * @param longitudeStepDeg step size in degrees; must be &gt; 0
     * @throws IllegalArgumentException if {@code longitudeStepDeg} &le; 0
     */
    public void setLongitudeStepDeg(double longitudeStepDeg) {
        setLongitudeStepRad(Math.toRadians(longitudeStepDeg));
    }

    /**
     * Returns whether the map outline is drawn before the graticule lines.
     *
     * @return {@code true} if the outline will be drawn
     */
    public boolean isDrawOutline() { return drawOutline; }

    /**
     * Sets whether the map outline is drawn before the graticule lines.
     *
     * @param drawOutline {@code true} to draw the outline; {@code false} to
     *                    suppress it
     */
    public void setDrawOutline(boolean drawOutline) { this.drawOutline = drawOutline; }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /**
     * Renders the map outline (if enabled) followed by all graticule lines.
     *
     * <p>Latitude parallels are drawn for φ ∈ [-π/2, +π/2] and longitude
     * meridians for λ ∈ [-π, +π], both using the configured step size. A
     * small epsilon is added to the upper bound of each loop to ensure the
     * endpoint is always included despite floating-point accumulation.</p>
     *
     * <p>Drawing appearance is fully controlled by the projection and its
     * {@link MapTheme}; this method does not set any graphics state.</p>
     *
     * @param g2        graphics context to draw into; must not be {@code null}
     * @param container container providing the world-to-local transform;
     *                  must not be {@code null}
     */
    public void render(Graphics2D g2, IContainer container) {
        if (drawOutline) {
            projection.drawMapOutline(g2, container);
        }

        // Latitude parallels: φ ∈ [-π/2, +π/2]
        double latMin = -Math.PI / 2.0;
        double latMax =  Math.PI / 2.0;
        for (double phi = latMin; phi <= latMax + 1e-9; phi += latitudeStepRad) {
            projection.drawLatitudeLine(g2, container, phi);
        }

        // Longitude meridians: λ ∈ [-π, +π]
        double lonMin = -Math.PI;
        double lonMax =  Math.PI;
        for (double lambda = lonMin; lambda <= lonMax + 1e-9; lambda += longitudeStepRad) {
            projection.drawLongitudeLine(g2, container, lambda);
        }
    }
}
