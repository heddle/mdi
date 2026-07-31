package edu.cnu.mdi.mapping.layer;

/**
 * Capability bits used to assemble a {@link MapLayerStyleDialog}.
 */
public final class MapLayerStyleBits {

    private MapLayerStyleBits() {
    }

    /** Enables editing of polygon fill color. */
    public static final long FILL_COLOR =
            1L << 0;

    /** Enables editing of line or polygon-boundary color. */
    public static final long BOUNDARY_COLOR =
            1L << 1;

    /** Enables editing of line width. */
    public static final long LINE_WIDTH =
            1L << 2;

    /** Enables editing of point-marker color. */
    public static final long POINT_COLOR =
            1L << 3;

    /** Enables editing of feature-label color. */
    public static final long LABEL_COLOR =
            1L << 4;

    /** Enables editing of point-marker size. */
    public static final long POINT_SIZE =
            1L << 5;

    /** Enables the adaptive graticule-spacing control. */
    public static final long ADAPTIVE =
            1L << 7;

    /** Enables the feature-label visibility control. */
    public static final long DRAW_LABELS =
            1L << 8;

    /** Enables the projection or layer outline control. */
    public static final long DRAW_OUTLINE =
            1L << 9;

    /** Enables editing of fixed latitude-line spacing. */
    public static final long LATITUDE_STEP =
            1L << 10;

    /** Enables editing of fixed longitude-line spacing. */
    public static final long LONGITUDE_STEP =
            1L << 11;

    /**
     * Minimum population required for a city to be displayed.
     */
    public static final long MIN_POPULATION =
            1L << 12;

    /** Enables multiple selection of DBF fields used for hit-test feedback. */
    public static final long FEEDBACK_FIELDS =
            1L << 13;
    
    /**
     * Global layer opacity. This is distinct from alpha embedded in an
     * individual color and is mainly useful for multicolor layers such as
     * terrain.
     */
    public static final long OPACITY =
            1L << 6;
}
