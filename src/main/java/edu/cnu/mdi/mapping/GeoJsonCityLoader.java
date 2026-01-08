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
 * {@code FeatureCollection}. Designed to work with Natural Earth "populated
 * places" datasets (e.g. {@code ne_10m_populated_places}), but will tolerate
 * variations in property naming (upper/lower case).
 * <p>
 * Expected schema (loosely):
 * <ul>
 *   <li>{@code type}: "FeatureCollection"</li>
 *   <li>{@code features}: array of features</li>
 *   <li>Each feature has a {@code Point} geometry with coordinates
 *       {@code [lon, lat]} in radians</li>
 *   <li>Properties such as (names may vary in case):
 *       <ul>
 *         <li>{@code NAME} / {@code name} – city name</li>
 *         <li>{@code ADM0NAME} / {@code adm0name} – country name</li>
 *         <li>{@code POP_MAX} / {@code pop_max} – maximum population estimate</li>
 *         <li>{@code SCALERANK} / {@code scalerank} – importance rank</li>
 *       </ul>
 *   </li>
 * </ul>
 */
public final class GeoJsonCityLoader {

    private GeoJsonCityLoader() {
        // utility class
    }

    /**
     * Immutable representation of a single city (populated place) feature.
     */
    public static final class CityFeature {

        private final String name;
        private final String countryName;
        private final double longitude;
        private final double latitude;
        private final long population;
        private final int scalerank;

        /**
         * Construct a new city feature.
         *
         * @param name        display name of the city (may be null)
         * @param countryName name of the country (may be null)
         * @param longitide      longitude in radians (east positive)
         * @param latitude      latitude in radians (north positive)
         * @param population  population estimate (may be -1 if unknown)
         * @param scalerank   importance rank (0 = largest cities, larger = less important; -1 if unknown)
         */
        public CityFeature(String name,
                           String countryName,
                           double longitude,
                           double latitude,
                           long population,
                           int scalerank) {
            this.name = name;
            this.countryName = countryName;
            this.longitude = longitude;
            this.latitude = latitude;
            this.population = population;
            this.scalerank = scalerank;
        }

        /** City display name (may be null). */
        public String getName() {
            return name;
        }

        /** Country name (may be null). */
        public String getCountryName() {
            return countryName;
        }

        /** Longitude in radians (east positive). */
        public double getLongitude() {
            return longitude;
        }

        /** Latitude in radians (north positive). */
        public double getLatitude() {
            return latitude;
        }

        /** Population estimate (or -1 if not provided). */
        public long getPopulation() {
            return population;
        }

        /**
         * Importance rank: smaller values generally indicate larger / more
         * prominent cities. {@code -1} indicates unknown.
         */
        public int getScalerank() {
            return scalerank;
        }

        @Override
        public String toString() {
            return "CityFeature[" +
                    "name=" + name +
                    ", country=" + countryName +
                    ", lonDeg=" + Math.toDegrees(longitude) +
                    ", latDeg=" + Math.toDegrees(latitude) +
                    ", pop=" + population +
                    ", scalerank=" + scalerank +
                    ']';
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Load city features from a GeoJSON file at the given path.
     *
     * @param geoJsonPath path to a GeoJSON FeatureCollection file
     * @return immutable list of city features
     * @throws IOException if an I/O or parse error occurs
     */
    public static List<CityFeature> load(Path geoJsonPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(geoJsonPath, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    /**
     * Load city features from a GeoJSON resource on the classpath.
     *
     * @param resourcePath absolute resource path (e.g. {@code "/geo/ne_10m_populated_places.geojson"})
     * @return immutable list of city features
     * @throws IOException if the resource cannot be read or parsed
     */
    public static List<CityFeature> loadFromResource(String resourcePath) throws IOException {
        Objects.requireNonNull(resourcePath, "resourcePath");
        InputStream in = GeoJsonCityLoader.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return load(reader);
        }
    }

    // Internal loader from text
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

    private static CityFeature parseFeature(JsonNode featureNode) {
        if (!"Feature".equals(featureNode.path("type").asText())) {
            return null;
        }

        JsonNode geometry = featureNode.path("geometry");
        String geomType = geometry.path("type").asText();

        if (!"Point".equalsIgnoreCase(geomType)) {
            // Skip non-point geometries
            return null;
        }

        JsonNode coords = geometry.path("coordinates");
        if (!coords.isArray() || coords.size() < 2) {
            return null;
        }

        //json file had degrees. Convert to radians.
        double lon = Math.toRadians(coords.get(0).asDouble());
        double lat = Math.toRadians(coords.get(1).asDouble());
        lon = wrapLongitude(lon);

        JsonNode props = featureNode.path("properties");

        // Be tolerant about property names (upper/lower case and a few aliases)
        String name = nullSafeText(props,
                "NAME", "name", "Name",
                "NAMEASCII", "nameascii", "nameascii_en");

        String countryName = nullSafeText(props,
                "ADM0NAME", "adm0name", "Adm0Name",
                "SOV0NAME", "sov0name");

        long population = longFrom(props, -1L,
                "POP_MAX", "pop_max", "Pop_Max");

        int scalerank = intFrom(props, -1,
                "SCALERANK", "scalerank", "ScaleRank");

        return new CityFeature(name, countryName, lon, lat, population, scalerank);
    }

    /**
     * Wrap a longitude value to the canonical range [-π, π).
     *
     * @param lon longitude in radians
     * @return wrapped longitude in [-π, π)
     */
    private static double wrapLongitude(double lon) {
		while (lon <= -Math.PI) {
			lon += 2 * Math.PI;
		}
		while (lon > Math.PI) {
			lon -= 2 * Math.PI;
		}
		return lon;
	}


    /**
     * Return the first non-empty text value among the given fields, or null
     * if none exist or all are empty.
     */
    private static String nullSafeText(JsonNode node, String... fields) {
        if (node == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode child = node.path(field);
            if (child.isMissingNode()) {
                continue;
            }
            String value = child.asText(null);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /**
     * Return the first present numeric field (parsed as long) among the given
     * fields, or {@code defaultValue} if none are present.
     */
    private static long longFrom(JsonNode node, long defaultValue, String... fields) {
        if (node == null || fields == null) {
            return defaultValue;
        }
        for (String field : fields) {
            JsonNode child = node.path(field);
            if (child.isMissingNode() || !child.isNumber()) {
                continue;
            }
            return child.asLong(defaultValue);
        }
        return defaultValue;
    }

    /**
     * Return the first present numeric field (parsed as int) among the given
     * fields, or {@code defaultValue} if none are present.
     */
    private static int intFrom(JsonNode node, int defaultValue, String... fields) {
        if (node == null || fields == null) {
            return defaultValue;
        }
        for (String field : fields) {
            JsonNode child = node.path(field);
            if (child.isMissingNode() || !child.isNumber()) {
                continue;
            }
            return child.asInt(defaultValue);
        }
        return defaultValue;
    }
}
