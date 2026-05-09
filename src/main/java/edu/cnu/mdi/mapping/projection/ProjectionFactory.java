package edu.cnu.mdi.mapping.projection;

import java.awt.geom.Point2D;

import edu.cnu.mdi.mapping.MapView2D;
import edu.cnu.mdi.mapping.theme.MapTheme;

/**
 * Factory for creating {@link IMapProjection} instances from
 * {@link EProjection} identifiers.
 *
 * <h2>Purpose</h2>
 * <p>Centralizing projection construction here means that calling code
 * (e.g. {@link MapView2D}) never depends directly on concrete classes such as
 * {@link MercatorProjection} or {@link OrthographicProjection}. Adding a new
 * projection requires only: a new {@link EProjection} constant, a new
 * {@code case} in {@link #create(EProjection, MapTheme, Point2D.Double)}, and
 * the concrete implementation class itself.</p>
 *
 * <h2>Theme guarantee</h2>
 * <p>Every projection returned by this factory has its {@link MapTheme} set
 * before the instance is returned. Callers can therefore call drawing methods
 * immediately without risk of {@link NullPointerException}.</p>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * // Default center
 * IMapProjection mercator = ProjectionFactory.create(EProjection.MERCATOR, MapTheme.light());
 *
 * // Explicit center (orthographic centred on the USA)
 * Point2D.Double center = new Point2D.Double(Math.toRadians(-100.0), Math.toRadians(40.0));
 * IMapProjection ortho  = ProjectionFactory.create(EProjection.ORTHOGRAPHIC, MapTheme.dark(), center);
 * }</pre>
 *
 * <p>This class is not instantiable.</p>
 */
public final class ProjectionFactory {

    private ProjectionFactory() { /* not instantiable */ }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a projection with the given type and theme, using each
     * projection's built-in default center.
     *
     * <p>This is a convenience overload of
     * {@link #create(EProjection, MapTheme, Point2D.Double)} that passes
     * {@code null} for the center.</p>
     *
     * @param type  projection type; must not be {@code null}
     * @param theme map theme; must not be {@code null}
     * @return a fully initialized {@link IMapProjection} with the theme set
     * @throws IllegalArgumentException if {@code type} or {@code theme} is
     *                                  {@code null}, or {@code type} is not
     *                                  yet handled by this factory
     */
    public static IMapProjection create(EProjection type, MapTheme theme) {
        return create(type, theme, null);
    }

    /**
     * Creates a projection with the given type, theme, and optional center.
     *
     * <p>Center conventions</p>
     * <ul>
     *   <li>The {@code center} parameter is a {@link Point2D.Double} with
     *       {@code x = λ₀} (longitude, radians) and {@code y = φ₀}
     *       (latitude, radians).</li>
     *   <li>If {@code center} is {@code null} a projection-specific default
     *       is used (see individual cases below).</li>
     *   <li>Projections that do not have a configurable center (currently
     *       none, but Mercator and Mollweide only use the longitude component)
     *       ignore the latitude component.</li>
     * </ul>
     *
     * <p>Default centers</p>
     * <ul>
     *   <li><b>MERCATOR</b> — central longitude fixed at -70° by the
     *       {@link MercatorProjection} constructor; the {@code center}
     *       argument is ignored because recenter support is handled via
     *       {@link MercatorProjection#setCentralLongitude(double)}.</li>
     *   <li><b>ORTHOGRAPHIC</b> — defaults to (λ = 0°, φ = 50°), a
     *       balanced mid-latitude northern hemisphere view.</li>
     *   <li><b>MOLLWEIDE</b> — central longitude fixed at -70° by the
     *       constructor; the {@code center} argument is ignored.</li>
     *   <li><b>LAMBERT_EQUAL_AREA</b> — defaults to (λ = 0°, φ = 0°),
     *       i.e., centered on the intersection of the equator and prime
     *       meridian.</li>
     * </ul>
     *
     * @param type   projection type; must not be {@code null}
     * @param theme  map theme; must not be {@code null}
     * @param center optional projection center in radians ({@code x=λ, y=φ});
     *               {@code null} uses the projection's built-in default
     * @return a fully initialized {@link IMapProjection} with the theme set
     * @throws IllegalArgumentException if {@code type} or {@code theme} is
     *                                  {@code null}, or {@code type} is
     *                                  not yet handled by this factory
     */
    public static IMapProjection create(EProjection type, MapTheme theme, Point2D.Double center) {
        if (type  == null) throw new IllegalArgumentException("Projection type must not be null");
        if (theme == null) throw new IllegalArgumentException("MapTheme must not be null");

        return switch (type) {

            case MERCATOR -> new MercatorProjection(theme);

            case ORTHOGRAPHIC -> {
                // Default: mid-latitude northern-hemisphere view.
                double lambda0 = (center != null) ? center.x : 0.0;
                double phi0    = (center != null) ? center.y : Math.toRadians(50.0);
                // Use the three-argument constructor so the theme is set atomically,
                // eliminating the NPE risk if drawing methods are called immediately.
                yield new OrthographicProjection(lambda0, phi0, theme);
            }

            case MOLLWEIDE -> new MollweideProjection(theme);

            case LAMBERT_EQUAL_AREA -> {
                double lambda0 = (center != null) ? center.x : 0.0;
                double phi0    = (center != null) ? center.y : 0.0;
                yield new LambertEqualAreaProjection(lambda0, phi0, theme);
            }
        };
    }
}
