package edu.cnu.mdi.mapping;

import edu.cnu.mdi.mapping.loader.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader;

/**
 * Centralizes classpath resource paths used by the mapping package.
 *
 * <p>All paths are absolute classpath references beginning with {@code /} so
 * they work correctly when passed to
 * {@link Class#getResourceAsStream(String)}. The leading slash is required;
 * without it the resource lookup is relative to the class's own package and
 * will silently fail to locate top-level resources.</p>
 *
 * <p>GeoJSON data files are sourced from
 * <a href="https://www.naturalearthdata.com/downloads/10m-cultural-vectors/">
 * Natural Earth 10m cultural vectors</a>.</p>
 *
 * <p>This class is not instantiable.</p>
 */
public final class MapResources {

    /**
     * Absolute classpath path to the country-boundary GeoJSON file.
     *
     * <p>Expected format: a {@code FeatureCollection} whose features carry
     * {@code Polygon} or {@code MultiPolygon} geometries and at minimum the
     * properties {@code ADMIN} (country name) and {@code ISO_A3} (alpha-3
     * code). Parsed by {@link GeoJsonCountryLoader}.</p>
     */
    public static final String COUNTRIES_GEOJSON = "/geo/countries.geojson";

    /**
     * Absolute classpath path to the populated-places (city) GeoJSON file.
     *
     * <p>Expected format: a {@code FeatureCollection} of {@code Point}
     * features with properties including {@code NAME}, {@code ADM0NAME},
     * {@code POP_MAX}, and {@code SCALERANK}. Parsed by
     * {@link GeoJsonCityLoader}.</p>
     */
    public static final String CITIES_GEOJSON = "/geo/cities.geojson";

    private MapResources() {
        // not instantiable
    }
}
