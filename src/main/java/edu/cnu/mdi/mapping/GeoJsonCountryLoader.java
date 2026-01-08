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
 * Utility class for loading a simplified set of country boundaries from
 * a GeoJSON <em>FeatureCollection</em>. The loader is intentionally generic
 * and does not depend on any particular projection or rendering system.
 * <p>
 * The expected GeoJSON schema is:
 * <ul>
 *   <li>{@code type}: "FeatureCollection"</li>
 *   <li>{@code features}: array of features</li>
 *   <li>Each feature has {@code properties.ADMIN} and {@code properties.ISO_A3}</li>
 *   <li>Geometry type is "Polygon" or "MultiPolygon" with coordinates in
 *       longitude/latitude order.</li>
 * </ul>
 */
public final class GeoJsonCountryLoader {

    private GeoJsonCountryLoader() {
        // utility class
    }

    /**
     * Representation of a single country feature with a name, ISO code and
     * one or more polygon rings expressed as lists of geographic points.
     */
    public static final class CountryFeature {
        private final String adminName;
        private final String isoA3;
        private final List<List<Point2D.Double>> polygons;

        /**
         * Construct a new immutable country feature.
         *
         * @param adminName the administrative name (e.g., "United States of America")
         * @param isoA3     the ISO 3166-1 alpha-3 code (e.g., "USA")
         * @param polygons  the polygon rings in longitude/latitude order
         */
        public CountryFeature(String adminName, String isoA3, List<List<Point2D.Double>> polygons) {
            this.adminName = Objects.requireNonNull(adminName, "adminName");
            this.isoA3 = Objects.requireNonNull(isoA3, "isoA3");
            this.polygons = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(polygons, "polygons")));
        }

        /**
         * Get the administrative name of the country.
         *
         * @return the country name
         */
        public String getAdminName() {
            return adminName;
        }

        /**
         * Get the ISO 3166-1 alpha-3 country code.
         *
         * @return the ISO code
         */
        public String getIsoA3() {
            return isoA3;
        }

        /**
         * Get the immutable list of polygon rings. Each ring is a list of
         * {@link Point2D.Double} instances in longitude/latitude order.
         *
         * @return the list of polygon rings
         */
        public List<List<Point2D.Double>> getPolygons() {
            return polygons;
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Load countries from a GeoJSON file located at the given path.
     *
     * @param geoJsonPath the path to the GeoJSON file
     * @return list of country features parsed from the file
     * @throws IOException if an I/O or parse error occurs
     */
    public static List<CountryFeature> load(Path geoJsonPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(geoJsonPath, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    /**
     * Load countries from a GeoJSON resource found on the classpath.
     *
     * @param resourcePath the absolute resource path (e.g., "/geo/countries.geojson")
     * @return list of country features parsed from the resource
     * @throws IOException if an I/O or parse error occurs
     */
    public static List<CountryFeature> loadFromResource(String resourcePath) throws IOException {
        Objects.requireNonNull(resourcePath, "resourcePath");
        InputStream in = GeoJsonCountryLoader.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return load(reader);
        }
    }

    private static List<CountryFeature> load(BufferedReader reader) throws IOException {
        JsonNode root = MAPPER.readTree(reader);
        if (root == null || !"FeatureCollection".equals(root.path("type").asText())) {
            throw new IOException("GeoJSON root is not a FeatureCollection");
        }

        JsonNode featuresNode = root.path("features");
        if (!featuresNode.isArray()) {
            throw new IOException("GeoJSON 'features' array is missing or not an array");
        }

        List<CountryFeature> result = new ArrayList<>();
        for (JsonNode featureNode : featuresNode) {
            CountryFeature feature = parseFeature(featureNode);
            if (feature != null) {
                result.add(feature);
            }
        }
        return result;
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


    private static CountryFeature parseFeature(JsonNode featureNode) {
        JsonNode properties = featureNode.path("properties");
        String admin = properties.path("ADMIN").asText(null);
        String isoA3 = properties.path("ISO_A3").asText(null);

        if (admin == null || isoA3 == null) {
            // skip malformed features
            return null;
        }

        JsonNode geometry = featureNode.path("geometry");
        String geomType = geometry.path("type").asText();
        JsonNode coords = geometry.path("coordinates");

        List<List<Point2D.Double>> polygons = new ArrayList<>();

        if ("Polygon".equalsIgnoreCase(geomType)) {
            parsePolygon(coords, polygons);
        } else if ("MultiPolygon".equalsIgnoreCase(geomType)) {
            for (JsonNode polygonNode : coords) {
                parsePolygon(polygonNode, polygons);
            }
        } else {
            // ignore unsupported geometry types
            return null;
        }

        if (polygons.isEmpty()) {
            return null;
        }

        return new CountryFeature(admin, isoA3, polygons);
    }

    private static void parsePolygon(JsonNode polygonNode, List<List<Point2D.Double>> out) {
        // A polygon is an array of linear rings; we keep all rings.
        for (JsonNode ringNode : polygonNode) {
            List<Point2D.Double> ring = new ArrayList<>();
            for (JsonNode coordNode : ringNode) {
                if (coordNode.isArray() && coordNode.size() >= 2) {

                	//convert to radians
                    double lon = Math.toRadians(coordNode.get(0).asDouble());
                    double lat = Math.toRadians(coordNode.get(1).asDouble());
                    lon = wrapLongitude(lon);
                    ring.add(new Point2D.Double(lon, lat));
                }
            }
            if (!ring.isEmpty()) {
                out.add(ring);
            }
        }
    }
}
