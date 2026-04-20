package edu.cnu.mdi.mapping.theme;

import edu.cnu.mdi.mapping.loader.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.loader.ShapefileCityLoader;
import edu.cnu.mdi.mapping.loader.ShapefileCountryLoader;
import edu.cnu.mdi.mapping.shapefile.ShapefileFeatureLoader;

/**
 * Shared geographic utility methods used across the MDI mapping package.
 *
 * <p>These methods were previously duplicated as {@code private static}
 * helpers in {@link GeoJsonCityLoader}, {@link GeoJsonCountryLoader},
 * {@link ShapefileCountryLoader}, {@link ShapefileCityLoader}, and
 * {@link ShapefileFeatureLoader}. Centralising them here eliminates the
 * duplication and provides a single well-tested implementation.</p>
 *
 * <p>This class is not instantiable.</p>
 */
public final class MapUtils {

    private MapUtils() { /* not instantiable */ }

    /**
     * Wraps a longitude value to the canonical half-open range (-π, π].
     *
     * <p>This is the standard normalization used throughout the mapping
     * subsystem. The range is half-open: values less than or equal to -π
     * are mapped to their equivalent in (-π, π].</p>
     *
     * @param lon longitude in radians (any value)
     * @return equivalent longitude in (-π, π]
     */
    public static double wrapLongitude(double lon) {
        while (lon <= -Math.PI) lon += 2 * Math.PI;
        while (lon >   Math.PI) lon -= 2 * Math.PI;
        return lon;
    }

    /**
     * Converts longitude from degrees to radians and wraps to (-π, π].
     *
     * <p>Convenience method combining {@link Math#toRadians(double)} and
     * {@link #wrapLongitude(double)} — the most common operation when
     * reading geographic data from files that store coordinates in degrees.</p>
     *
     * @param lonDegrees longitude in decimal degrees
     * @return equivalent longitude in radians, in (-π, π]
     */
    public static double lonDegreesToRadians(double lonDegrees) {
        return wrapLongitude(Math.toRadians(lonDegrees));
    }
}