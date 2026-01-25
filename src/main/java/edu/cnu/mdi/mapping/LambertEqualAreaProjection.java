package edu.cnu.mdi.mapping;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;

/**
 * Lambert azimuthal equal-area projection (spherical Earth, R = 1).
 * <p>
 * Maps the entire sphere to a disk of radius 2 in projection space, preserving
 * area. The projection is centered on a chosen point (λ₀, φ₀).
 * </p>
 *
 * <h2>Forward projection</h2> From Snyder (spherical form), with Earth radius R
 * = 1:
 *
 * <pre>
 *   k = sqrt( 2 / (1 + sin φ₀ sin φ + cos φ₀ cos φ cos(λ - λ₀)) )
 *
 *   x = k cos φ sin(λ - λ₀)
 *   y = k (cos φ₀ sin φ - sin φ₀ cos φ cos(λ - λ₀))
 * </pre>
 *
 * where:
 * <ul>
 * <li>λ, φ are the input longitude and latitude (in radians)</li>
 * <li>λ₀, φ₀ are the center longitude and latitude (in radians)</li>
 * </ul>
 *
 * <h2>Inverse projection</h2>
 *
 * <pre>
 *   ρ = sqrt(x² + y²)
 *   c = 2 asin(ρ / 2)
 *
 *   φ = asin( cos c sin φ₀ + (y sin c cos φ₀) / ρ )
 *   λ = λ₀ + atan2( x sin c,
 *                   ρ cos φ₀ cos c - y sin φ₀ sin c )
 * </pre>
 *
 * with the special case {@code ρ = 0} corresponding to the center point (λ₀,
 * φ₀).
 * <p>
 * Conventions:
 * <ul>
 * <li>Geographic coords are passed as {@link java.awt.geom.Point2D.Double} with
 * {@code x = λ} (longitude, radians) and {@code y = φ} (latitude,
 * radians).</li>
 * <li>Projection-space coords are also {@code Point2D.Double} in "world"
 * units.</li>
 * </ul>
 */
public class LambertEqualAreaProjection implements IMapProjection {

	/** Sphere radius in projection space. */
	private static final double R = 1.0;

	/** Maximum latitude used for sampling / numeric safety. */
	private static final double MAX_LAT = Math.toRadians(89.999);
	private static final double MIN_LAT = -MAX_LAT;

	/** Maximum radial distance in projection space (for the antipode). */
	private static final double RHO_MAX = 2.0 * R;

	/** Number of segments for drawing the circular outline and clip. */
	private static final int NUM_SEGMENTS = 360;

	/** Center longitude λ₀ (radians). */
	private double centerLon;

	/** Center latitude φ₀ (radians). */
	private double centerLat;

	/** Theme used for drawing. May be {@code null} until set by client code. */
	private MapTheme theme;

	/**
	 * Creates a Lambert azimuthal equal-area projection centered at the specified
	 * longitude and latitude on a unit sphere.
	 * <p>
	 * This mirrors the primary constructor style of {@link OrthographicProjection},
	 * leaving the {@link MapTheme} to be provided later via
	 * {@link #setTheme(MapTheme)} (e.g., from a factory).
	 *
	 * @param centerLon central longitude λ₀ in radians
	 * @param centerLat central latitude φ₀ in radians
	 */
	public LambertEqualAreaProjection(double centerLon, double centerLat) {
		this.centerLon = centerLon;
		this.centerLat = centerLat;
	}

	/**
	 * Convenience constructor centered at (0, 0).
	 * <p>
	 * Equivalent to:
	 *
	 * <pre>
	 * new LambertEqualAreaProjection(0.0, 0.0);
	 * </pre>
	 */
	public LambertEqualAreaProjection() {
		this(0.0, 0.0);
	}

	/**
	 * Convenience constructor that also sets the {@link MapTheme}.
	 *
	 * @param centerLon central longitude λ₀ in radians
	 * @param centerLat central latitude φ₀ in radians
	 * @param theme     map theme; must not be {@code null}
	 */
	public LambertEqualAreaProjection(double centerLon, double centerLat, MapTheme theme) {
		this(centerLon, centerLat);
		setTheme(theme);
	}

	/**
	 * Convenience constructor centered at (0, 0) with the given theme.
	 *
	 * @param theme map theme; must not be {@code null}
	 */
	public LambertEqualAreaProjection(MapTheme theme) {
		this(0.0, 0.0, theme);
	}

	/**
	 * Set the center of the projection.
	 *
	 * @param centerLon central longitude λ₀ in radians
	 * @param centerLat central latitude φ₀ in radians
	 */
	public void setCenter(double centerLon, double centerLat) {
		this.centerLat = centerLat;
		this.centerLon = centerLon;
	}

	@Override
	public void latLonToXY(Point2D.Double latLon, Point2D.Double xy) {
		double lon = latLon.x;
		double lat = latLon.y;

		// Clamp latitude for numerical stability
		lat = Math.max(MIN_LAT, Math.min(MAX_LAT, lat));

		double dLon = lon - centerLon;

		double sinLat = Math.sin(lat);
		double cosLat = Math.cos(lat);
		double sinLat0 = Math.sin(centerLat);
		double cosLat0 = Math.cos(centerLat);
		double cosDLon = Math.cos(dLon);

		// Denominator in Snyder's formula (1 + cos γ)
		double denom = 1.0 + sinLat0 * sinLat + cosLat0 * cosLat * cosDLon;

		// When denom is very small we are at or near the antipode.
		if (denom <= 1e-15) {
			// Map the antipode to a point on the boundary of the disk.
			// Choose some arbitrary direction; here we use angle 0.
			xy.x = RHO_MAX;
			xy.y = 0.0;
			return;
		}

		double k = Math.sqrt(2.0 / denom);

		double x = k * cosLat * Math.sin(dLon);
		double y = k * (cosLat0 * sinLat - sinLat0 * cosLat * cosDLon);

		xy.setLocation(x, y);
	}

	@Override
	public void latLonFromXY(Point2D.Double latLon, Point2D.Double xy) {
		double x = xy.x;
		double y = xy.y;

		double rho2 = x * x + y * y;
		double rho = Math.sqrt(rho2);

		if (rho > RHO_MAX + 1e-9) {
			// Outside valid disk; no inverse mapping.
			latLon.setLocation(Double.NaN, Double.NaN);
			return;
		}

		if (rho < 1e-15) {
			// Center point
			latLon.x = centerLon;
			latLon.y = centerLat;
			return;
		}

		double sinLat0 = Math.sin(centerLat);
		double cosLat0 = Math.cos(centerLat);

		// Great-circle distance from center to point
		double c = 2.0 * Math.asin(rho / (2.0 * R));
		double sinC = Math.sin(c);
		double cosC = Math.cos(c);

		double phi = Math.asin(cosC * sinLat0 + (y * sinC * cosLat0) / rho);

		double lambda = centerLon + Math.atan2(x * sinC, rho * cosLat0 * cosC - y * sinLat0 * sinC);

		latLon.x = wrapLongitude(lambda);
		latLon.y = phi;
	}

	@Override
	public void drawMapOutline(Graphics2D g2, IContainer container) {
		// Outline is the circle x^2 + y^2 = RHO_MAX^2
		Path2D path = new Path2D.Double();
		Point2D.Double world = new Point2D.Double();
		Point screen = new Point();

		for (int i = 0; i <= NUM_SEGMENTS; i++) {
			double theta = 2.0 * Math.PI * i / NUM_SEGMENTS;
			double x = RHO_MAX * Math.cos(theta);
			double y = RHO_MAX * Math.sin(theta);

			world.setLocation(x, y);
			container.worldToLocal(screen, world);

			if (i == 0) {
				path.moveTo(screen.x, screen.y);
			} else {
				path.lineTo(screen.x, screen.y);
			}
		}
		path.closePath();

		Color oldColor = g2.getColor();
		Stroke oldStroke = g2.getStroke();

		g2.setColor(theme.getOutlineColor());
		g2.setStroke(new BasicStroke(theme.getOutlineStrokeWidth()));
		g2.draw(path);

		g2.setColor(oldColor);
		g2.setStroke(oldStroke);
	}

	@Override
	public boolean isPointOnMap(Point2D.Double xy) {
		double x = xy.x;
		double y = xy.y;
		return x * x + y * y <= RHO_MAX * RHO_MAX + 1e-9;
	}

	@Override
	public void drawLatitudeLine(Graphics2D g2, IContainer container, double latitude) {
		double lat = Math.max(MIN_LAT, Math.min(MAX_LAT, latitude));

		Path2D path = new Path2D.Double();
		Point2D.Double latLon = new Point2D.Double();
		Point2D.Double xy = new Point2D.Double();
		Point screen = new Point();

		latLon.y = lat;

		int nSamples = 360;
		for (int i = 0; i <= nSamples; i++) {
			double lon = -Math.PI + 2.0 * Math.PI * i / nSamples;
			latLon.x = lon;

			latLonToXY(latLon, xy);
			if (Double.isNaN(xy.x) || Double.isNaN(xy.y) || !isPointOnMap(xy)) {
				continue;
			}

			container.worldToLocal(screen, xy);

			if (i == 0) {
				path.moveTo(screen.x, screen.y);
			} else {
				path.lineTo(screen.x, screen.y);
			}
		}

		Color oldColor = g2.getColor();
		g2.setColor(Color.LIGHT_GRAY);
		g2.draw(path);
		g2.setColor(oldColor);
	}

	@Override
	public void drawLongitudeLine(Graphics2D g2, IContainer container, double longitude) {
		double lon = wrapLongitude(longitude);

		Path2D path = new Path2D.Double();
		Point2D.Double latLon = new Point2D.Double();
		Point2D.Double xy = new Point2D.Double();
		Point screen = new Point();

		latLon.x = lon;

		int nSamples = 180;
		for (int i = 0; i <= nSamples; i++) {
			double t = (double) i / nSamples;
			double lat = MIN_LAT + (MAX_LAT - MIN_LAT) * t;
			latLon.y = lat;

			latLonToXY(latLon, xy);
			if (Double.isNaN(xy.x) || Double.isNaN(xy.y) || !isPointOnMap(xy)) {
				continue;
			}

			container.worldToLocal(screen, xy);

			if (i == 0) {
				path.moveTo(screen.x, screen.y);
			} else {
				path.lineTo(screen.x, screen.y);
			}
		}

		Color oldColor = g2.getColor();
		g2.setColor(Color.LIGHT_GRAY);
		g2.draw(path);
		g2.setColor(oldColor);
	}

	@Override
	public boolean isPointVisible(Point2D.Double latLon) {
		// Entire globe is mapped; just enforce numerical latitude limits.
		double lat = latLon.y;
		return lat >= MIN_LAT && lat <= MAX_LAT;
	}

	@Override
	public EProjection getProjection() {
		// Make sure to add LAMBERT_EQUAL_AREA to your EProjection enum.
		return EProjection.LAMBERT_EQUAL_AREA;
	}

	@Override
	public Rectangle2D.Double getXYBounds() {
		// Bounding box of the disk [-RHO_MAX, RHO_MAX]^2
		return new Rectangle2D.Double(-RHO_MAX, -RHO_MAX, 2.0 * RHO_MAX, 2.0 * RHO_MAX);
	}

	@Override
	public MapTheme getTheme() {
		return theme;
	}

	@Override
	public void setTheme(MapTheme theme) {
		if (theme == null) {
			throw new IllegalArgumentException("MapTheme must not be null");
		}
		this.theme = theme;
	}

	@Override
	public Shape createClipShape(IContainer container) {
		// Same circle as the outline, but used as a device-space clip.
		Path2D path = new Path2D.Double();
		Point2D.Double world = new Point2D.Double();
		Point screen = new Point();

		for (int i = 0; i <= NUM_SEGMENTS; i++) {
			double theta = 2.0 * Math.PI * i / NUM_SEGMENTS;
			double x = RHO_MAX * Math.cos(theta);
			double y = RHO_MAX * Math.sin(theta);

			world.setLocation(x, y);
			container.worldToLocal(screen, world);

			if (i == 0) {
				path.moveTo(screen.x, screen.y);
			} else {
				path.lineTo(screen.x, screen.y);
			}
		}
		path.closePath();
		return path;
	}
}
