package edu.cnu.mdi.mapping;

import java.awt.geom.Point2D;

/**
 * Factory for creating {@link IMapProjection} instances based on
 * {@link EProjection} identifiers.
 * <p>
 * This centralizes projection construction so that calling code does not need
 * to depend directly on concrete projection classes such as
 * {@link MercatorProjection}, {@link OrthographicProjection},
 * {@link MollweideProjection}, or {@link LambertEqualAreaProjection}.
 * </p>
 * <p>
 * Typical usage:
 *
 * <pre>
 * IMapProjection mercator = ProjectionFactory.create(EProjection.MERCATOR, MapTheme.light());
 *
 * // Centered orthographic projection
 * Point2D.Double center = new Point2D.Double(Math.toRadians(-100.0), // λ
 * 		Math.toRadians(40.0)); // φ
 * IMapProjection ortho = ProjectionFactory.create(EProjection.ORTHOGRAPHIC, MapTheme.dark(), center);
 * </pre>
 * </p>
 */
public final class ProjectionFactory {

	private ProjectionFactory() {
		// not instantiable
	}

	/**
	 * Creates a projection with the given type and theme, using a default center
	 * (when applicable).
	 * <p>
	 * For projections that require a center (e.g., orthographic, Lambert
	 * equal-area), the default is (λ = 0, φ = 0) unless noted otherwise.
	 * </p>
	 *
	 * @param type  the projection type (must not be {@code null})
	 * @param theme the map theme to use (must not be {@code null})
	 * @return a new {@link IMapProjection} instance
	 * @throws IllegalArgumentException if {@code type} or {@code theme} is
	 *                                  {@code null}, or the type is unsupported
	 */
	public static IMapProjection create(EProjection type, MapTheme theme) {
		return create(type, theme, null);
	}

	/**
	 * Creates a projection with the given type, theme, and optional center.
	 * <p>
	 * Conventions:
	 * <ul>
	 * <li>Geographic center is represented by a {@link Point2D.Double} where
	 * {@code x = λ} (longitude, radians) and {@code y = φ} (latitude,
	 * radians).</li>
	 * <li>If {@code center} is {@code null}, a projection-specific default center
	 * is used.</li>
	 * <li>Projections that do not support a center will ignore the {@code center}
	 * value.</li>
	 * </ul>
	 *
	 * @param type   the projection type (must not be {@code null})
	 * @param theme  the map theme to use (must not be {@code null})
	 * @param center optional projection center in radians; {@code null} allows the
	 *               factory to choose a sensible default
	 * @return a new {@link IMapProjection} instance
	 * @throws IllegalArgumentException if the type or theme is {@code null}, or the
	 *                                  type is unsupported
	 */
	public static IMapProjection create(EProjection type, MapTheme theme, Point2D.Double center) {
		if (type == null) {
			throw new IllegalArgumentException("Projection type must not be null");
		}
		if (theme == null) {
			throw new IllegalArgumentException("MapTheme must not be null");
		}

		switch (type) {

		case MERCATOR: {
			return new MercatorProjection(theme);
		}

		case ORTHOGRAPHIC: {
			// Orthographic is centered on (λ0, φ0)
			double lambda0 = 0.0;
			double phi0 = Math.toRadians(50.0); // default: a nice mid-latitude view
			if (center != null) {
				lambda0 = center.x;
				phi0 = center.y;
			}
			OrthographicProjection proj = new OrthographicProjection(lambda0, phi0);
			proj.setTheme(theme);
			return proj;
		}

		case MOLLWEIDE: {
			return new MollweideProjection(theme);
		}

		case LAMBERT_EQUAL_AREA: {
			// Lambert azimuthal equal-area centered on (λ0, φ0)
			double lambda0 = 0.0;
			double phi0 = 0.0;
			if (center != null) {
				lambda0 = center.x;
				phi0 = center.y;
			}
			return new LambertEqualAreaProjection(lambda0, phi0, theme);
		}

		default:
			throw new IllegalArgumentException("Unsupported projection type: " + type);
		}
	}
}
