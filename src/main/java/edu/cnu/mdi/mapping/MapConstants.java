package edu.cnu.mdi.mapping;

import edu.cnu.mdi.mapping.projection.EProjection;

/**
 * Package-wide constants shared across the MDI mapping subsystem.
 *
 * <p>Centralizing these values eliminates the duplication bugs that arise when
 * the same magic number is defined independently in multiple classes (e.g.,
 * {@code MAX_POP_SLIDER_VALUE} previously appeared separately in both
 * {@link MapControlPanel} and {@link MapView2D}, creating a silent divergence
 * risk).</p>
 *
 * <p>All fields are {@code public static final}. This class is not
 * instantiable.</p>
 */
public final class MapConstants {

    // -------------------------------------------------------------------------
    // City / population filter
    // -------------------------------------------------------------------------

    /**
	 * Default value of the minimum-population slider used by
	 * {@link MapControlPanel} and the initial population threshold applied in
	 * {@link MapView2D#setProjection(EProjection)}.
	 *
	 * <p>Cities whose recorded population is below this value are hidden on
	 * first render.</p>
	 */
    public static final int MIN_POP_DEFAULT = 1_000_000;

    /**
     * The default map projection shown when a new {@link MapView2D} is created
     * and the projection selected in {@link MapProjectionMenu} on first render.
     *
     * <p>Keeping this constant in one place ensures the view and the menu
     * always agree on which projection is initially active.</p>
     */
    public static final EProjection DEFAULT_PROJECTION = EProjection.MERCATOR;

    /**
     * The mean radius of the Earth in kilometers, used to scale the
     * dimensionless great-circle arc length returned by
     * {@link edu.cnu.mdi.mapping.graphics.MapGraphics#greatCircleLength}
     * (computed via the spherical law of cosines) into a real-world distance.
     */
    public static final double RADIUS_EARTH_KM = 6371.0;

    // -------------------------------------------------------------------------
    // Construction guard
    // -------------------------------------------------------------------------

    private MapConstants() {
        // not instantiable
    }
}
