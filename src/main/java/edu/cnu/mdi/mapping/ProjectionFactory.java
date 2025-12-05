package edu.cnu.mdi.mapping;

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
 * <pre>
 *     IMapProjection mercator = ProjectionFactory.create(EProjection.MERCATOR,
 *                                                       MapTheme.light());
 *
 *     LatLon center = new LatLon(Math.toRadians(-30), Math.toRadians(45));
 *     IMapProjection ortho = ProjectionFactory.create(EProjection.ORTHOGRAPHIC,
 *                                                     MapTheme.dark(),
 *                                                     center);
 * </pre>
 * </p>
 */
public final class ProjectionFactory {

    private ProjectionFactory() {
        // not instantiable
    }

    /**
     * Creates a projection with the given type, using a default theme and
     * default center (when applicable).
     * <p>
     * For projections that require a center (e.g., orthographic, Lambert
     * equal-area), the default is (λ = 0, φ = 0).
     * </p>
     *
     * @param type the projection type
     * @return a new {@link IMapProjection} instance
     * @throws IllegalArgumentException if the type is unsupported or {@code null}
     */
    public static IMapProjection create(EProjection type) {
        return create(type, MapTheme.light(), null);
    }

    /**
     * Creates a projection with the given type and theme, using a default
     * center (when applicable).
     * <p>
     * For projections that require a center (e.g., orthographic, Lambert
     * equal-area), the default is (λ = 0, φ = 0).
     * </p>
     *
     * @param type  the projection type
     * @param theme the map theme to use (must not be {@code null})
     * @return a new {@link IMapProjection} instance
     * @throws IllegalArgumentException if the type or theme is {@code null},
     *                                  or the type is unsupported
     */
    public static IMapProjection create(EProjection type, MapTheme theme) {
        return create(type, theme, null);
    }

    /**
     * Creates a projection with the given type, theme, and optional center.
     * <p>
     * For projections that support or require a center:
     * <ul>
     *   <li>If {@code center} is non-null, it is used as the projection center.</li>
     *   <li>If {@code center} is {@code null}, a projection-specific default
     *       center is used.</li>
     * </ul>
     * Projections that do not use a center will simply ignore the
     * {@code center} parameter (e.g., Mercator in this implementation).
     * </p>
     *
     * @param type   the projection type (must not be {@code null})
     * @param theme  the map theme to use (must not be {@code null})
     * @param center optional center of the projection (λ, φ) in radians; may be {@code null}
     * @return a new {@link IMapProjection} instance
     * @throws IllegalArgumentException if the type or theme is {@code null},
     *                                  or the type is unsupported
     */
    public static IMapProjection create(EProjection type, MapTheme theme, LatLon center) {
        if (type == null) {
            throw new IllegalArgumentException("Projection type must not be null");
        }
        if (theme == null) {
            throw new IllegalArgumentException("MapTheme must not be null");
        }

        switch (type) {
            case MERCATOR:
                // This implementation of Mercator ignores center (no central meridian parameter).
                return new MercatorProjection(theme);

            case ORTHOGRAPHIC:
                // Use center if provided, otherwise (0, 0).
                if (center != null) {
                    return new OrthographicProjection(center.lambda(), center.phi(), theme);
                }
                return new OrthographicProjection(0.0, 0.0, theme);

            case MOLLWEIDE:
                // Mollweide uses the central meridian; if center provided, use its λ.
                if (center != null) {
                    return new MollweideProjection(center.lambda(), theme);
                }
                return new MollweideProjection(0.0, theme);

            case LAMBERT_EQUAL_AREA:
                // Lambert azimuthal equal-area uses center.
                if (center != null) {
                    return new LambertEqualAreaProjection(center.lambda(), center.phi(), theme);
                }
                return new LambertEqualAreaProjection(0.0, 0.0, theme);

            default:
                throw new IllegalArgumentException("Unsupported projection type: " + type);
        }
    }
}
