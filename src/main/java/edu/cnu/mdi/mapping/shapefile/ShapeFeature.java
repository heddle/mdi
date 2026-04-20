package edu.cnu.mdi.mapping.shapefile;

import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.cnu.mdi.mapping.loader.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.projection.IMapProjection;

/**
 * Immutable representation of a single feature loaded from an arbitrary ESRI
 * Shapefile — a geometry (polygon rings, polyline parts, or points) paired
 * with an open-ended property bag drawn from the companion {@code .dbf} table.
 *
 * <p>Unlike {@link GeoJsonCountryLoader.CountryFeature} and
 * {@link GeoJsonCityLoader.CityFeature}, which model fixed schemas (name, ISO
 * code, population, etc.), {@code ShapeFeature} makes no assumptions about
 * the attribute fields present in the file. Every {@code .dbf} column is
 * stored as a trimmed string in {@link #getProperties()} and callers retrieve
 * values by field name.</p>
 *
 * <h2>Geometry conventions</h2>
 * <ul>
 *   <li>All coordinates are in <em>radians</em> (longitude, latitude order)
 *       with longitude wrapped to (-π, π], matching the convention used by
 *       all {@link IMapProjection} implementations.</li>
 *   <li>{@link #getRings()} is non-empty for polygon and polyline features
 *       ({@link ShapefileGeometryReader#TYPE_POLYGON},
 *       {@link ShapefileGeometryReader#TYPE_POLYLINE}). Each element is one
 *       ring or part.</li>
 *   <li>{@link #getPoints()} is non-empty for point features
 *       ({@link ShapefileGeometryReader#TYPE_POINT},
 *       {@link ShapefileGeometryReader#TYPE_MULTIPOINT}).</li>
 *   <li>Both lists are unmodifiable.</li>
 * </ul>
 *
 * <h2>Property access</h2>
 * <p>Field names are stored exactly as they appear in the {@code .dbf} header
 * (typically upper-case for Natural Earth data). Values are trimmed of
 * whitespace. Missing fields return {@code null} from
 * {@link Map#get(Object)}.</p>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * ShapefileFeatureLoader loader = new ShapefileFeatureLoader();
 * List<ShapeFeature> lakes = loader.load(Path.of("ne_10m_lakes.shp"));
 * for (ShapeFeature lake : lakes) {
 *     String name = lake.getProperty("name");
 *     double area = lake.getPropertyDouble("scalerank", -1.0);
 * }
 * }</pre>
 */
public final class ShapeFeature {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /**
     * Shape type constant from {@link ShapefileGeometryReader} identifying
     * whether this feature is a point, polyline, or polygon.
     */
    private final int shapeType;

    /**
     * Ring/part coordinate lists for polygon and polyline features.
     * Empty for point features.
     */
    private final List<List<Point2D.Double>> rings;

    /**
     * Point coordinate list for point and multi-point features.
     * Empty for polygon and polyline features.
     */
    private final List<Point2D.Double> points;

    /**
     * All {@code .dbf} attribute fields as trimmed strings, keyed by field
     * name. Unmodifiable.
     */
    private final Map<String, String> properties;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Constructs a new shapefile feature. All list and map parameters are
     * stored directly (callers should pass unmodifiable views).
     *
     * @param shapeType  shape type constant (see {@link ShapefileGeometryReader}
     *                   {@code TYPE_*} constants)
     * @param rings      ring/part lists for polygon/polyline features;
     *                   must not be {@code null}; use
     *                   {@link Collections#emptyList()} for point features
     * @param points     point list for point/multipoint features;
     *                   must not be {@code null}; use
     *                   {@link Collections#emptyList()} for ring-based features
     * @param properties attribute map from the {@code .dbf} table;
     *                   must not be {@code null}
     */
    public ShapeFeature(int shapeType,
                        List<List<Point2D.Double>> rings,
                        List<Point2D.Double> points,
                        Map<String, String> properties) {
        this.shapeType  = shapeType;
        this.rings      = Objects.requireNonNull(rings,      "rings");
        this.points     = Objects.requireNonNull(points,     "points");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    // -------------------------------------------------------------------------
    // Geometry accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the shape type of this feature, corresponding to one of the
     * {@code TYPE_*} constants in {@link ShapefileGeometryReader}.
     *
     * @return shape type (e.g. {@link ShapefileGeometryReader#TYPE_POLYGON})
     */
    public int getShapeType() { return shapeType; }

    /**
     * Returns {@code true} if this feature is a polygon.
     *
     * @return {@code true} for {@link ShapefileGeometryReader#TYPE_POLYGON}
     */
    public boolean isPolygon() {
        return shapeType == ShapefileGeometryReader.TYPE_POLYGON;
    }

    /**
     * Returns {@code true} if this feature is a polyline (also called a
     * line string or arc in other formats).
     *
     * @return {@code true} for {@link ShapefileGeometryReader#TYPE_POLYLINE}
     */
    public boolean isPolyline() {
        return shapeType == ShapefileGeometryReader.TYPE_POLYLINE;
    }

    /**
     * Returns {@code true} if this feature is a point or multi-point.
     *
     * @return {@code true} for {@link ShapefileGeometryReader#TYPE_POINT} or
     *         {@link ShapefileGeometryReader#TYPE_MULTIPOINT}
     */
    public boolean isPoint() {
        return shapeType == ShapefileGeometryReader.TYPE_POINT
            || shapeType == ShapefileGeometryReader.TYPE_MULTIPOINT;
    }

    /**
     * Returns the unmodifiable list of rings or parts for polygon and
     * polyline features.
     *
     * <p>For polygon features each element is one ring (outer shell or
     * interior hole). For polyline features each element is one connected
     * sequence of vertices (a "part" or arc). Empty for point features.</p>
     *
     * <p>All coordinates are in radians with longitude in (-π, π].</p>
     *
     * @return unmodifiable list of rings/parts; never {@code null}
     */
    public List<List<Point2D.Double>> getRings() { return rings; }

    /**
     * Returns the unmodifiable list of points for point and multi-point
     * features. Empty for polygon and polyline features.
     *
     * <p>All coordinates are in radians with longitude in (-π, π].</p>
     *
     * @return unmodifiable list of points; never {@code null}
     */
    public List<Point2D.Double> getPoints() { return points; }

    // -------------------------------------------------------------------------
    // Property accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the unmodifiable property map containing all {@code .dbf}
     * attribute fields for this feature.
     *
     * <p>Keys are field names exactly as stored in the {@code .dbf} header
     * (typically upper-case for Natural Earth files). Values are trimmed
     * strings and may be empty but not {@code null}.</p>
     *
     * @return unmodifiable attribute map; never {@code null}
     */
    public Map<String, String> getProperties() { return properties; }

    /**
     * Returns the value of the named property as a string, or {@code null}
     * if the field is not present in this feature's attribute table.
     *
     * @param fieldName the {@code .dbf} field name (case-sensitive)
     * @return trimmed string value, or {@code null} if absent
     */
    public String getProperty(String fieldName) {
        return properties.get(fieldName);
    }

    /**
     * Returns the value of the named property as a string, or
     * {@code defaultValue} if the field is absent or empty.
     *
     * @param fieldName    the {@code .dbf} field name (case-sensitive)
     * @param defaultValue value returned when the field is absent or empty
     * @return trimmed string value, or {@code defaultValue}
     */
    public String getProperty(String fieldName, String defaultValue) {
        String v = properties.get(fieldName);
        return (v != null && !v.isEmpty()) ? v : defaultValue;
    }

    /**
     * Returns the value of the named property parsed as a {@code double}, or
     * {@code defaultValue} if the field is absent, empty, or not numeric.
     *
     * @param fieldName    the {@code .dbf} field name (case-sensitive)
     * @param defaultValue value returned on parse failure
     * @return parsed double value, or {@code defaultValue}
     */
    public double getPropertyDouble(String fieldName, double defaultValue) {
        String v = properties.get(fieldName);
        if (v == null || v.isEmpty()) return defaultValue;
        try {
            return java.lang.Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the value of the named property parsed as an {@code int}, or
     * {@code defaultValue} if the field is absent, empty, or not numeric.
     *
     * @param fieldName    the {@code .dbf} field name (case-sensitive)
     * @param defaultValue value returned on parse failure
     * @return parsed int value, or {@code defaultValue}
     */
    public int getPropertyInt(String fieldName, int defaultValue) {
        String v = properties.get(fieldName);
        if (v == null || v.isEmpty()) return defaultValue;
        try {
            int dot = v.indexOf('.');
            return Integer.parseInt(dot >= 0 ? v.substring(0, dot) : v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // -------------------------------------------------------------------------
    // Object
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        String typeName = switch (shapeType) {
            case ShapefileGeometryReader.TYPE_POINT      -> "Point";
            case ShapefileGeometryReader.TYPE_MULTIPOINT -> "MultiPoint";
            case ShapefileGeometryReader.TYPE_POLYLINE   -> "Polyline";
            case ShapefileGeometryReader.TYPE_POLYGON    -> "Polygon";
            default                                      -> "Unknown(" + shapeType + ")";
        };
        return "ShapeFeature[type=" + typeName
                + ", rings=" + rings.size()
                + ", points=" + points.size()
                + ", props=" + properties.size() + ']';
    }
}