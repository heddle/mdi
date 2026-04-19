package edu.cnu.mdi.mapping;

import java.awt.geom.Point2D;
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
 * Utility for loading country boundary polygons from a GeoJSON
 * {@code FeatureCollection}.
 *
 * <p>Designed primarily for
 * <a href="https://www.naturalearthdata.com/downloads/10m-cultural-vectors/">
 * Natural Earth 10m cultural vectors</a>, but tolerates minor schema
 * variations. Features with missing required properties or unsupported
 * geometry types are silently skipped rather than causing a parse failure.</p>
 *
 * <h2>Expected GeoJSON schema</h2>
 * <pre>
 * {
 *   "type": "FeatureCollection",
 *   "features": [
 *     {
 *       "type": "Feature",
 *       "properties": {
 *         "ADMIN":  "United States of America",
 *         "ISO_A3": "USA"
 *       },
 *       "geometry": {
 *         "type": "Polygon" | "MultiPolygon",
 *         "coordinates": [...]   // degrees, longitude first
 *       }
 *     }, ...
 *   ]
 * }
 * </pre>
 *
 * <h2>Coordinate handling</h2>
 * <p>Input coordinates are in degrees (longitude, latitude order as required
 * by GeoJSON RFC 7946). They are converted to radians and the longitude is
 * wrapped to (-π, π] before being stored in the returned
 * {@link CountryFeature}.</p>
 *
 * <h2>Thread safety</h2>
 * <p>This class has no mutable state. The internal {@link ObjectMapper} is
 * thread-safe for reading and may be shared freely. The returned lists are
 * unmodifiable.</p>
 *
 * <p>This class is not instantiable.</p>
 */
public final class GeoJsonCountryLoader implements ICountryLoader {

    public GeoJsonCountryLoader() { /* utility class */ }

    // =========================================================================
    // CountryFeature
    // =========================================================================

    /**
     * Immutable representation of a single country feature consisting of an
     * administrative name, an ISO-3166-1 alpha-3 code, and one or more polygon
     * rings expressed as lists of geographic points in radians.
     *
     * <p>A country may have multiple polygon rings because:
     * <ul>
     *   <li>The GeoJSON geometry type is {@code MultiPolygon} (e.g. France
     *       with its overseas territories).</li>
     *   <li>A {@code Polygon} has interior holes (the outer ring plus one or
     *       more inner rings are all stored).</li>
     * </ul>
     *
     * <p>The list returned by {@link #getPolygons()} is unmodifiable. Each
     * inner ring list is modifiable but callers should treat them as
     * read-only.</p>
     */
    public static final class CountryFeature {

        private final String adminName;
        private final String isoA3;
        private final List<List<Point2D.Double>> polygons;

        /**
         * Constructs a new country feature. The supplied polygon list is
         * defensively copied and wrapped in an unmodifiable view.
         *
         * @param adminName administrative name (e.g. "United States of America");
         *                  must not be {@code null}
         * @param isoA3     ISO 3166-1 alpha-3 code (e.g. "USA");
         *                  must not be {@code null}
         * @param polygons  polygon rings in radians (longitude, latitude order);
         *                  must not be {@code null}
         */
        public CountryFeature(String adminName, String isoA3,
                              List<List<Point2D.Double>> polygons) {
            this.adminName = Objects.requireNonNull(adminName, "adminName");
            this.isoA3     = Objects.requireNonNull(isoA3,     "isoA3");
            this.polygons  = Collections.unmodifiableList(
                    new ArrayList<>(Objects.requireNonNull(polygons, "polygons")));
        }

        /**
         * Returns the administrative (display) name of the country.
         *
         * @return country name; never {@code null}
         */
        public String getAdminName() { return adminName; }

        /**
         * Returns the ISO 3166-1 alpha-3 country code.
         *
         * @return three-letter ISO code; never {@code null}
         */
        public String getIsoA3() { return isoA3; }

        /**
         * Returns the unmodifiable list of polygon rings. Each ring is a list
         * of {@link Point2D.Double} instances with {@code x = λ} (longitude,
         * radians) and {@code y = φ} (latitude, radians).
         *
         * <p>A ring with fewer than 3 points is degenerate and renderers may
         * safely skip it (see {@link CountryRenderer}).</p>
         *
         * @return unmodifiable list of polygon rings
         */
        public List<List<Point2D.Double>> getPolygons() { return polygons; }

        @Override
        public String toString() {
            return "CountryFeature[admin=" + adminName + ", iso=" + isoA3
                    + ", rings=" + polygons.size() + ']';
        }
    }

    // =========================================================================
    // Internal state
    // =========================================================================

    /**
     * Shared, thread-safe Jackson {@link ObjectMapper}. {@code ObjectMapper}
     * is safe for concurrent reads once configured, and construction is
     * expensive, so it is reused across calls.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // =========================================================================
    // Public load methods
    // =========================================================================

    /** {@inheritDoc} — delegates to {@link #loadStatic(Path)}. */
    public List<CountryFeature> load(Path path) throws IOException {
        return loadStatic(path);
    }

    /** {@inheritDoc} — delegates to {@link #loadFromResourceStatic(String)}. */
    public List<CountryFeature> loadFromResource(String resourcePath) throws IOException {
        return loadFromResourceStatic(resourcePath);
    }

    /**
     * Static convenience overload: loads country features from a GeoJSON file.
     *
     * @param geoJsonPath path to a GeoJSON {@code FeatureCollection} file
     * @return unmodifiable list of {@link CountryFeature} instances
     * @throws IOException if an I/O or JSON parse error occurs
     */
    public static List<CountryFeature> loadStatic(Path geoJsonPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(geoJsonPath, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    /**
     * Static convenience overload: loads country features from a classpath resource.
     * The resource path must begin with {@code /}.
     *
     * @param resourcePath absolute classpath resource path
     * @return unmodifiable list of {@link CountryFeature} instances
     * @throws IOException if the resource cannot be found, read, or parsed
     */
    public static List<CountryFeature> loadFromResourceStatic(String resourcePath)
            throws IOException {
        Objects.requireNonNull(resourcePath, "resourcePath");
        InputStream in = GeoJsonCountryLoader.class.getResourceAsStream(resourcePath);
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
     * Parses the entire GeoJSON document from a {@link BufferedReader} and
     * returns an <em>unmodifiable</em> list of country features.
     *
     * <p>Previously this method returned a plain {@link ArrayList}, meaning
     * callers could mutate the shared list. It now wraps the result in
     * {@link Collections#unmodifiableList} to match the behaviour of
     * {@link GeoJsonCityLoader} and prevent accidental mutation.</p>
     *
     * @param reader source of GeoJSON text
     * @return unmodifiable list of parsed features
     * @throws IOException if the root node is missing or not a
     *                     {@code FeatureCollection}
     */
    private static List<CountryFeature> load(BufferedReader reader) throws IOException {
        JsonNode root = MAPPER.readTree(reader);
        if (root == null || !"FeatureCollection".equals(root.path("type").asText())) {
            throw new IOException("GeoJSON root is not a FeatureCollection");
        }

        JsonNode featuresNode = root.path("features");
        if (!featuresNode.isArray()) {
            throw new IOException("GeoJSON 'features' is missing or not an array");
        }

        List<CountryFeature> result = new ArrayList<>();
        for (JsonNode featureNode : featuresNode) {
            CountryFeature feature = parseFeature(featureNode);
            if (feature != null) {
                result.add(feature);
            }
        }
        // Wrap to match GeoJsonCityLoader behaviour and prevent external mutation.
        return Collections.unmodifiableList(result);
    }

    /**
     * Parses a single GeoJSON {@code Feature} node into a
     * {@link CountryFeature}.
     *
     * <p>Returns {@code null} if:
     * <ul>
     *   <li>The {@code ADMIN} or {@code ISO_A3} property is absent.</li>
     *   <li>The geometry type is not {@code Polygon} or
     *       {@code MultiPolygon}.</li>
     *   <li>No valid polygon rings could be extracted.</li>
     * </ul>
     *
     * @param featureNode a single GeoJSON feature node
     * @return parsed {@link CountryFeature}, or {@code null} to skip
     */
    private static CountryFeature parseFeature(JsonNode featureNode) {
        JsonNode properties = featureNode.path("properties");
        String admin = properties.path("ADMIN").asText(null);
        String isoA3 = properties.path("ISO_A3").asText(null);

        if (admin == null || isoA3 == null) {
            return null; // required properties missing
        }

        JsonNode geometry = featureNode.path("geometry");
        String   geomType = geometry.path("type").asText();
        JsonNode coords   = geometry.path("coordinates");

        List<List<Point2D.Double>> polygons = new ArrayList<>();

        if ("Polygon".equalsIgnoreCase(geomType)) {
            parsePolygon(coords, polygons);
        } else if ("MultiPolygon".equalsIgnoreCase(geomType)) {
            for (JsonNode polygonNode : coords) {
                parsePolygon(polygonNode, polygons);
            }
        } else {
            return null; // unsupported geometry type
        }

        return polygons.isEmpty() ? null : new CountryFeature(admin, isoA3, polygons);
    }

    /**
     * Parses a GeoJSON {@code Polygon} coordinate array (an array of linear
     * rings) and appends each ring to {@code out}.
     *
     * <p>Each ring coordinate pair is converted from degrees to radians, and
     * the longitude is wrapped to (-π, π] via {@link #wrapLongitude(double)}.</p>
     *
     * @param polygonNode JSON node for a single polygon (array of rings)
     * @param out         destination list; rings are appended in order
     */
    private static void parsePolygon(JsonNode polygonNode, List<List<Point2D.Double>> out) {
        for (JsonNode ringNode : polygonNode) {
            List<Point2D.Double> ring = new ArrayList<>();
            for (JsonNode coordNode : ringNode) {
                if (coordNode.isArray() && coordNode.size() >= 2) {
                    double lon = wrapLongitude(Math.toRadians(coordNode.get(0).asDouble()));
                    double lat = Math.toRadians(coordNode.get(1).asDouble());
                    ring.add(new Point2D.Double(lon, lat));
                }
            }
            if (!ring.isEmpty()) {
                out.add(ring);
            }
        }
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
