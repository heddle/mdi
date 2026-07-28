package edu.cnu.mdi.mapping.layer;

/**
 * Capability bits used to assemble a {@link MapLayerStyleDialog}.
 */
public final class MapLayerStyleBits {

    private MapLayerStyleBits() {
    }

    public static final long FILL_COLOR =
            1L << 0;

    public static final long BOUNDARY_COLOR =
            1L << 1;

    public static final long LINE_WIDTH =
            1L << 2;

    public static final long POINT_COLOR =
            1L << 3;

    public static final long LABEL_COLOR =
            1L << 4;

    public static final long POINT_SIZE =
            1L << 5;
    
    public static final long ADAPTIVE =
            1L << 7;

    public static final long DRAW_LABELS =
            1L << 8;

    public static final long DRAW_OUTLINE =
            1L << 9;

    public static final long LATITUDE_STEP =
            1L << 10;

    public static final long LONGITUDE_STEP =
            1L << 11;
    /**
     * Minimum population required for a city to be displayed.
     */
    public static final long MIN_POPULATION =
            1L << 12;
    
    /**
     * Global layer opacity. This is distinct from alpha embedded in an
     * individual color and is mainly useful for multicolor layers such as
     * terrain.
     */
    public static final long OPACITY =
            1L << 6;
}