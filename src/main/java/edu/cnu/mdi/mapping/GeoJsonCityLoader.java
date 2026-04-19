package edu.cnu.mdi.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility for loading city (populated place) point features from a GeoJSON
 * {@code FeatureCollection}.
 *
 * <p>Designed primarily for
 * <a href="https://www.naturalearthdata.com/downloads/10m-cultural-vectors/">
 * Natural Earth 10m populated places</a> data (e.g.
 * {@code ne_10m_populated_places.geojson}), but tolerates schema variations
 * such as mixed-case property names. Features with unsupported geometry types
 * or missing coordinate data are silently skipped.</p>
 *
 * <h2>Expected GeoJSON schema</h2>
 * <pre>
 * {
 *   "type": "FeatureCollection",
 *   "features": [
 *     {
 *       "type": "Feature",
 *       "geometry": {
 *         "type": "Point",
 *         "coordinates": [longitude_degrees, latitude_degrees]
 *       },
 *       "properties": {
 *         "NAME":      "London",
 *         "ADM0NAME":  "United Kingdom",
 *         "POP_MAX":   8908081,
 *         "SCALERANK": 0
 *       }
 *     }, ...
 *   ]
 * }
 * </pre>
 *
 * <h2>Property name tolerance</h2>
 * <p>The loader tries several case variants for each property (e.g.
 * {@code NAME}, {@code name}, {@code Name}) and accepts the first non-empty
 * match. Properties that are entirely absent result in a {@code null} string
 * or the sentinel value {@code -1} for numeric fields.</p>
 *
 * <h2>Coordinate handling</h2>
 * <p>Input coordinates are in degrees (GeoJSON RFC 7946 convention). They are
 * converted to radians and the longitude is wrapped to (-π, π] before storage.
 * All {@link CityFeature} coordinate accessors return radians.</p>
 *
 * <h2>Thread safety</h2>
 * <p>This class has no mutable state. The shared {@link ObjectMapper} is
 * thread-safe for reading. Both {@link #load(Path)} and
 * {@link #loadFromResource(String)} return unmodifiable lists.</p>
 *
 * <p>This class is not instantiable.</p>
 */
public final class GeoJsonCityLoader {

    private GeoJsonCityLoader() { /* utility class */ }

    // =========================================================================
    // CityFeature
    // =========================================================================

    /**
     * Immutable representation of a single populated-place (city) feature.
     *
     * <p>Coordinate fields use radians throughout, matching the convention
     * used by all {@link IMapProjection} implementations. Use
     * {@link Math#toDegrees(double)} when displaying values to users.</p>
     */
    public static final class CityFeature {

        private final String name;
        private final String countryName;
        private final double longitude; // radians
        private final double latitude;  // radians
        private final long   population;
        private final int    scalerank;

        /**
         * Constructs a new city feature.
         *
         * @param name        display name of the city (may be {@code null} if
         *                    absent from the source data)
         * @param countryName name of the country (may be {@code null})
         * @param longitude   longitude in radians (east positive); should be
         *                    in (-π, π]
         * @param latitude    latitude in radians (north positive); should be
         *                    in [-π/2, π/2]
         * @param population  population estimate, or {@code -1} if unknown
         * @param scalerank   importance rank where 0 denotes the largest /
         *                    most prominent cities and larger values denote
         *                    progressively less prominent ones; {@code -1} if
         *                    unknown
         */
        public CityFeature(String name, String countryName,
                           double longitude, double latitude,
                           long population, int scalerank) {
            this.name        = name;
            this.countryName = countryName;
            this.longitude   = longitude;
            this.latitude    = latitude;
            this.population  = population;
            this.scalerank   = scalerank;
        }

        /**
         * Returns the city's display name, or {@code null} if absent in the
         * source data.
         *
         * @return city name
         */
        public String getName() { return name; }

        /**
         * Returns the name of the country in which this city is located, or
         * {@code null} if absent in the source data.
         *
         * @return country name
         */
        public String getCountryName() { return countryName; }

        /**
         * Returns the city's longitude in <em>radians</em> (east positive),
         * normalized to (-π, π].
         *
         * @return longitude in radians
         */
        public double getLongitude() { return longitude; }

        /**
         * Returns the city's latitude in <em>radians</em> (north positive),
         * in the range [-π/2, π/2].
         *
         * @return latitude in radians
         */
        public double getLatitude() { return latitude; }

        /**
         * Returns the city's population estimate, or {@code -1} if the value
         * was not present in the source data.
         *
         * @return population estimate or {@code -1}
         */
        public long getPopulation() { return population; }

        /**
         * Returns the city's scalerank: a small integer where {@code 0}
         * indicates the most prominent cities (capitals, megacities) and
         * larger values indicate progressively less prominent ones.
         * Returns {@code -1} if the value was not present in the source data.
         *
         * <p>Scalerank is used by {@link CityPointRenderer} to filter which
         * cities and labels are rendered at a given zoom level.</p>
         *
         * @return scalerank or {@code -1}
         */
        public int getScalerank() { return scalerank; }

        @Override
        public String toString() {
            return "CityFeature[name=" + name
                    + ", country=" + countryName
                    + ", lonDeg=" + Math.toDegrees(longitude)
                    + ", latDeg=" + Math.toDegrees(latitude)
                    + ", pop=" + population
                    + ", scalerank=" + scalerank + ']';
        }
    }

    // =========================================================================
    // Internal state
    // =========================================================================

    /**
     * Shared, thread-safe Jackson {@link ObjectMapper}. Reused across calls
     * because construction is expensive.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // =========================================================================
    // Public load methods
    // =========================================================================

    /** {@inheritDoc} — delegates to {@link #loadStatic(Path)}. */
    public List<CityFeature> load(Path path) throws IOException {
        return loadStatic(path);
    }

    /** {@inheritDoc} — delegates to {@link #loadFromResourceStatic(String)}. */
    public List<CityFeature> loadFromResource(String resourcePath) throws IOException {
        return loadFromResourceStatic(resourcePath);
    }

    /**
     * Static convenience overload: loads city features from a GeoJSON file.
     *
     * @param geoJsonPath path to a GeoJSON {@code FeatureCollection} file
     * @return unmodifiable list of {@link CityFeature} instances
     * @throws IOException if an I/O or JSON parse error occurs
     */
    public static List<CityFeature> loadStatic(Path geoJsonPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(geoJsonPath, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    /**
     * Static convenience overload: loads city features from a classpath resource.
     * The resource path must begin with {@code /}.
     *
     * @param resourcePath absolute classpath resource path
     * @return unmodifiable list of {@link CityFeature} instances
     * @throws IOException if the resource cannot be found, read, or parsed
     */
    public static List<CityFeature> loadFromResourceStatic(String resourcePath)
            throws IOException {
        Objects.requireNonNull(resourcePath, "resourcePath");
        InputStream in = GeoJsonCityLoader.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return load(reader);
        }
    }

    // =========================================================================
    // Private parsing
    // =========================================================================

    /**
     * Parses the GeoJSON document from a {@link BufferedReader} and returns an
     * unmodifiable list of city features.
     *
     * @param reader source of GeoJSON text
     * @return unmodifiable list of parsed features
     * @throws IOException if the root is missing or not a
     *                     {@code FeatureCollection}
     */
    private static List<CityFeature> load(BufferedReader reader) throws IOException {
        JsonNode root = MAPPER.readTree(reader);
        if (root == null || !"FeatureCollection".equals(root.path("type").asText())) {
            throw new IOException("GeoJSON root is not a FeatureCollection");
        }

        JsonNode featuresNode = root.path("features");
        if (!featuresNode.isArray()) {
            throw new IOException("GeoJSON 'features' array missing or not an array");
        }

        List<CityFeature> result = new ArrayList<>();
        for (JsonNode featureNode : featuresNode) {
            CityFeature city = parseFeature(featureNode);
            if (city != null) {
                result.add(city);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Parses a single GeoJSON {@code Feature} node into a {@link CityFeature}.
     *
     * <p>Returns {@code null} if:
     * <ul>
     *   <li>The feature type is not {@code Feature}.</li>
     *   <li>The geometry type is not {@code Point}.</li>
     *   <li>The coordinate array has fewer than 2 elements.</li>
     * </ul>
     *
     * @param featureNode a single GeoJSON feature node
     * @return parsed {@link CityFeature}, or {@code null} to skip
     */
    private static CityFeature parseFeature(JsonNode featureNode) {
        if (!"Feature".equals(featureNode.path("type").asText())) return null;

        JsonNode geometry = featureNode.path("geometry");
        if (!"Point".equalsIgnoreCase(geometry.path("type").asText())) return null;

        JsonNode coords = geometry.path("coordinates");
        if (!coords.isArray() || coords.size() < 2) return null;

        // GeoJSON coordinates are in degrees; convert and wrap longitude.
        double lon = wrapLongitude(Math.toRadians(coords.get(0).asDouble()));
        double lat = Math.toRadians(coords.get(1).asDouble());

        JsonNode props       = featureNode.path("properties");
        String   name        = nullSafeText(props, "NAME", "name", "Name", "NAMEASCII", "nameascii");
        String   countryName = nullSafeText(props, "ADM0NAME", "adm0name", "SOV0NAME", "sov0name");
        long     population  = longFrom(props,  -1L, "POP_MAX", "pop_max", "Pop_Max");
        int      scalerank   = intFrom(props,   -1,  "SCALERANK", "scalerank", "ScaleRank");

        return new CityFeature(name, countryName, lon, lat, population, scalerank);
    }

    // =========================================================================
    // Property extraction helpers
    // =========================================================================

    /**
     * Returns the first non-empty string value found among the given property
     * names, or {@code null} if none of the names are present or all values
     * are empty strings.
     *
     * @param node   JSON object node containing properties
     * @param fields property names to try in order
     * @return first non-empty string value, or {@code null}
     */
    private static String nullSafeText(JsonNode node, String... fields) {
        if (node == null || fields == null) return null;
        for (String field : fields) {
            JsonNode child = node.path(field);
            if (child.isMissingNode()) continue;
            String value = child.asText(null);
            if (value != null && !value.isEmpty()) return value;
        }
        return null;
    }

    /**
     * Returns the first present numeric field parsed as {@code long}, or
     * {@code defaultValue} if none of the named properties exist or are
     * numeric.
     *
     * @param node         JSON object node
     * @param defaultValue value returned when no matching field is found
     * @param fields       property names to try in order
     * @return parsed long value, or {@code defaultValue}
     */
    private static long longFrom(JsonNode node, long defaultValue, String... fields) {
        if (node == null || fields == null) return defaultValue;
        for (String field : fields) {
            JsonNode child = node.path(field);
            if (!child.isMissingNode() && child.isNumber()) return child.asLong(defaultValue);
        }
        return defaultValue;
    }

    /**
     * Returns the first present numeric field parsed as {@code int}, or
     * {@code defaultValue} if none of the named properties exist or are
     * numeric.
     *
     * @param node         JSON object node
     * @param defaultValue value returned when no matching field is found
     * @param fields       property names to try in order
     * @return parsed int value, or {@code defaultValue}
     */
    private static int intFrom(JsonNode node, int defaultValue, String... fields) {
        if (node == null || fields == null) return defaultValue;
        for (String field : fields) {
            JsonNode child = node.path(field);
            if (!child.isMissingNode() && child.isNumber()) return child.asInt(defaultValue);
        }
        return defaultValue;
    }

    /**
     * Wraps a longitude value to the canonical half-open range (-π, π].
     *
     * @param lon longitude in radians
     * @return equivalent longitude in (-π, π]
     */
    private static double wrapLongitude(double lon) {
        while (lon <= -Math.PI) lon += 2 * Math.PI;
        while (lon >   Math.PI) lon -= 2 * Math.PI;
        return lon;
    }
}
