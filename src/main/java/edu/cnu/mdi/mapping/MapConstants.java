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
     * Maximum value (inclusive) of the minimum-population slider used by
     * {@link MapControlPanel} and the initial population threshold applied in
     * {@link MapView2D#setProjection(EProjection)}.
     *
     * <p>Cities whose recorded population is below the slider's current value
     * are hidden. At the slider maximum every city is hidden; at zero every
     * city with a known population is shown.</p>
     */
    public static final int MAX_POP_SLIDER_VALUE = 2_000_000;

    /**
     * The default map projection shown when a new {@link MapView2D} is created
     * and the projection selected in {@link MapProjectionMenu} on first render.
     *
     * <p>Keeping this constant in one place ensures the view and the menu
     * always agree on which projection is initially active.</p>
     */
    public static final EProjection DEFAULT_PROJECTION = EProjection.MERCATOR;
    
    /**
     * The mean radius of the Earth in kilometers, used for distance calculations
     * in the Haversine formula when computing distances between geographic coordinates.
     */
    public static final double RADIUS_EARTH_KM = 6371.0; 

    // -------------------------------------------------------------------------
    // Construction guard
    // -------------------------------------------------------------------------

    private MapConstants() {
        // not instantiable
    }
}
