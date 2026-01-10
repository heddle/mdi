package edu.cnu.mdi.mapping;

import java.awt.Graphics2D;

import edu.cnu.mdi.container.IContainer;

/**
 * Helper class that renders a map outline and latitude/longitude graticule
 * lines for a given {@link IMapProjection}.
 * <p>
 * This renderer:
 * <ul>
 * <li>Loops over latitude and longitude ranges using configurable step
 * sizes.</li>
 * <li>Delegates actual drawing to the projection's
 * {@link IMapProjection#drawLatitudeLine(Graphics2D, IContainer, double)} and
 * {@link IMapProjection#drawLongitudeLine(Graphics2D, IContainer, double)}
 * methods.</li>
 * <li>Optionally draws the projection outline via
 * {@link IMapProjection#drawMapOutline(Graphics2D, IContainer)}.</li>
 * </ul>
 * The visual appearance (colors, stroke widths) is controlled by the
 * projection's {@link MapTheme}.
 */
public final class GraticuleRenderer {

	/** The projection used for coordinate transforms and drawing. */
	private final IMapProjection projection;

	/** Step size for latitude lines, in radians. Default: 15 degrees. */
	private double latitudeStepRad = Math.toRadians(15.0);

	/** Step size for longitude lines, in radians. Default: 15 degrees. */
	private double longitudeStepRad = Math.toRadians(15.0);

	/** Whether to draw the map outline before the graticule. */
	private boolean drawOutline = true;

	/**
	 * Creates a new graticule renderer for the given projection.
	 *
	 * @param projection the projection to use (must not be {@code null})
	 */
	public GraticuleRenderer(IMapProjection projection) {
		if (projection == null) {
			throw new IllegalArgumentException("projection must not be null");
		}
		this.projection = projection;
	}

	/**
	 * Returns the associated projection.
	 *
	 * @return the projection used by this renderer
	 */
	public IMapProjection getProjection() {
		return projection;
	}

	/**
	 * Returns the latitude step size in radians.
	 *
	 * @return latitude step size in radians
	 */
	public double getLatitudeStepRad() {
		return latitudeStepRad;
	}

	/**
	 * Sets the latitude step size in radians.
	 *
	 * @param latitudeStepRad the step size in radians (must be &gt; 0)
	 */
	public void setLatitudeStepRad(double latitudeStepRad) {
		if (latitudeStepRad <= 0.0) {
			throw new IllegalArgumentException("latitudeStepRad must be > 0");
		}
		this.latitudeStepRad = latitudeStepRad;
	}

	/**
	 * Sets the latitude step size in degrees.
	 *
	 * @param latitudeStepDeg the step size in degrees (must be &gt; 0)
	 */
	public void setLatitudeStepDeg(double latitudeStepDeg) {
		setLatitudeStepRad(Math.toRadians(latitudeStepDeg));
	}

	/**
	 * Returns the longitude step size in radians.
	 *
	 * @return longitude step size in radians
	 */
	public double getLongitudeStepRad() {
		return longitudeStepRad;
	}

	/**
	 * Sets the longitude step size in radians.
	 *
	 * @param longitudeStepRad the step size in radians (must be &gt; 0)
	 */
	public void setLongitudeStepRad(double longitudeStepRad) {
		if (longitudeStepRad <= 0.0) {
			throw new IllegalArgumentException("longitudeStepRad must be > 0");
		}
		this.longitudeStepRad = longitudeStepRad;
	}

	/**
	 * Sets the longitude step size in degrees.
	 *
	 * @param longitudeStepDeg the step size in degrees (must be &gt; 0)
	 */
	public void setLongitudeStepDeg(double longitudeStepDeg) {
		setLongitudeStepRad(Math.toRadians(longitudeStepDeg));
	}

	/**
	 * Returns whether the map outline will be drawn.
	 *
	 * @return {@code true} if the outline will be drawn
	 */
	public boolean isDrawOutline() {
		return drawOutline;
	}

	/**
	 * Sets whether the map outline will be drawn before the graticule lines.
	 *
	 * @param drawOutline {@code true} to draw the outline, {@code false} otherwise
	 */
	public void setDrawOutline(boolean drawOutline) {
		this.drawOutline = drawOutline;
	}

	/**
	 * Renders the map outline (optionally) and graticule lines using the associated
	 * projection.
	 * <p>
	 * The renderer traverses:
	 * <ul>
	 * <li>latitudes from -π/2 to +π/2 using {@link #getLatitudeStepRad()}</li>
	 * <li>longitudes from -π to +π using {@link #getLongitudeStepRad()}</li>
	 * </ul>
	 * Individual projections may choose to ignore lines that fall outside their
	 * valid domain (for example, a limited-area projection).
	 * </p>
	 *
	 * @param g2        the graphics context to draw into
	 * @param container the container the projection is rendered in
	 */
	public void render(Graphics2D g2, IContainer container) {
		if (drawOutline) {
			projection.drawMapOutline(g2, container);
		}

		// Draw latitude lines (parallels): φ ∈ [-π/2, +π/2]
		double latMin = -Math.PI / 2.0;
		double latMax = Math.PI / 2.0;

		for (double phi = latMin; phi <= latMax + 1e-9; phi += latitudeStepRad) {
			projection.drawLatitudeLine(g2, container, phi);
		}

		// Draw longitude lines (meridians): λ ∈ [-π, +π]
		double lonMin = -Math.PI;
		double lonMax = Math.PI;

		for (double lambda = lonMin; lambda <= lonMax + 1e-9; lambda += longitudeStepRad) {
			projection.drawLongitudeLine(g2, container, lambda);
		}
	}
}
