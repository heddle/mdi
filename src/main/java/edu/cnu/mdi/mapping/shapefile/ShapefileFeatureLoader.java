package edu.cnu.mdi.mapping.shapefile;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.cnu.mdi.mapping.loader.ShapefileCityLoader;
import edu.cnu.mdi.mapping.loader.ShapefileCountryLoader;
import edu.cnu.mdi.mapping.theme.MapUtils;

/**
 * Loads arbitrary ESRI Shapefile data into {@link ShapeFeature} instances
 * without any assumptions about the attribute schema or geometry type.
 *
 * <p>This loader is the generic counterpart to the schema-specific
 * {@link ShapefileCountryLoader} and {@link ShapefileCityLoader}. It is
 * appropriate for any shapefile layer whose content is not known at compile
 * time — rivers, lakes, urban areas, administrative boundaries at any level,
 * protected areas, coastlines, and so on.</p>
 *
 * <h2>Supported geometry types</h2>
 * <ul>
 *   <li>{@link ShapefileGeometryReader#TYPE_POINT} — single points</li>
 *   <li>{@link ShapefileGeometryReader#TYPE_MULTIPOINT} — multiple points per
 *       feature</li>
 *   <li>{@link ShapefileGeometryReader#TYPE_POLYLINE} — connected line
 *       segments (rivers, roads, coastlines)</li>
 *   <li>{@link ShapefileGeometryReader#TYPE_POLYGON} — filled areas (lakes,
 *       land cover, urban footprints)</li>
 * </ul>
 * <p>Null-shape records are silently skipped.</p>
 *
 * <h2>Attribute loading</h2>
 * <p>All {@code .dbf} fields are included in each feature's property map.
 * Values are trimmed strings; callers use
 * {@link ShapeFeature#getProperty(String)} to retrieve them by field name.
 * No field names are required or assumed.</p>
 *
 * <h2>Coordinate handling</h2>
 * <p>Input coordinates are assumed to be WGS84 geographic degrees (longitude,
 * latitude order), as used by all Natural Earth shapefiles. They are converted
 * to radians and longitude is wrapped to (-π, π] before being stored in the
 * returned features.</p>
 *
 * <h2>File layout contract</h2>
 * <p>The {@code .dbf} companion file is derived from the {@code .shp} path
 * by replacing the extension. Both files must reside in the same directory
 * with the same base name. The {@code .shx} index file is not required;
 * records are read sequentially from the {@code .shp} file directly.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ShapefileFeatureLoader loader = new ShapefileFeatureLoader();
 *
 * // Load rivers
 * List<ShapeFeature> rivers = loader.load(Path.of("ne_10m_rivers_lake_centerlines.shp"));
 * ShapeFeatureStyle riverStyle = new ShapeFeatureStyle()
 *         .strokeColor(new Color(0x6B9FD4))
 *         .strokeWidth(0.8f);
 * mapView.addLayer(new ShapeFeatureRenderer(rivers, riverStyle));
 *
 * // Load lakes
 * List<ShapeFeature> lakes = loader.load(Path.of("ne_10m_lakes.shp"));
 * ShapeFeatureStyle lakeStyle = new ShapeFeatureStyle()
 *         .fillColor(new Color(0x6B9FD4))
 *         .strokeColor(new Color(0x4A7FB5))
 *         .strokeWidth(0.5f)
 *         .labelField("name");
 * mapView.addLayer(new ShapeFeatureRenderer(lakes, lakeStyle));
 * }</pre>
 *
 * <p>This class is stateless and thread-safe; a single instance may be reused
 * across multiple {@link #load} calls.</p>
 */
public final class ShapefileFeatureLoader {

    /**
     * Creates a new loader. No configuration is required; all behaviour is
     * determined by the shapefile itself and the {@link ShapeFeatureStyle}
     * applied at render time.
     */
    public ShapefileFeatureLoader() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Loads all features from the shapefile at the given path.
     *
     * <p>Opens the {@code .shp} geometry file and the companion {@code .dbf}
     * attribute file (derived by replacing the extension). Both files are read
     * sequentially and the results are combined into one
     * {@link ShapeFeature} per shapefile record.</p>
     *
     * @param shpPath path to the {@code .shp} file; the companion {@code .dbf}
     *                must exist in the same directory with the same base name;
     *                must not be {@code null}
     * @return unmodifiable list of {@link ShapeFeature} instances in file
     *         order; never {@code null}
     * @throws IOException if either file cannot be opened or a parse error
     *                     occurs
     */
    public List<ShapeFeature> load(Path shpPath) throws IOException {
        Objects.requireNonNull(shpPath, "shpPath");
        Path dbfPath = replaceExtension(shpPath, ".dbf");
        return loadFromPaths(shpPath, dbfPath);
    }

    // -------------------------------------------------------------------------
    // Core loading logic
    // -------------------------------------------------------------------------

    /**
     * Reads features from a matched {@code .shp} / {@code .dbf} pair.
     *
     * <p>The attribute table is loaded entirely into memory before iterating
     * the geometry file, so that geometry and attributes can be paired by
     * record index without multiple passes. For the dataset sizes typical of
     * Natural Earth layers (a few thousand records) this is well within
     * acceptable memory bounds.</p>
     *
     * @param shpPath path to the {@code .shp} file
     * @param dbfPath path to the {@code .dbf} file
     * @return unmodifiable list of features
     * @throws IOException if either file cannot be read
     */
    private static List<ShapeFeature> loadFromPaths(Path shpPath, Path dbfPath)
            throws IOException {

        List<ShapeFeature> result = new ArrayList<>();

        try (ShapefileGeometryReader shp = new ShapefileGeometryReader(shpPath);
             ShapefileDbfReader      dbf = new ShapefileDbfReader(dbfPath)) {

            List<Map<String, String>> attributes = dbf.readAllRecords();
            int attrIndex = 0;

            ShapefileGeometryReader.ShapeRecord geomRecord;
            while ((geomRecord = shp.nextRecord()) != null) {

                // Pair this geometry record with its attribute row.
                // The attribute cursor always advances once per geometry
                // record (including those that produce no feature) to keep
                // the two files in sync.
                Map<String, String> attrs = (attrIndex < attributes.size())
                        ? attributes.get(attrIndex)
                        : Collections.emptyMap();
                attrIndex++;

                ShapeFeature feature = buildFeature(geomRecord, attrs);
                if (feature != null) {
                    result.add(feature);
                }
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Converts one geometry record and its attribute map into a
     * {@link ShapeFeature}, or returns {@code null} if the record produces no
     * usable geometry.
     *
     * <p>Polygon and polyline records with fewer than 3 points in every ring
     * are skipped as degenerate. Point records with an empty point list are
     * skipped. The attribute map is stored as-is (it is already unmodifiable
     * from {@link ShapefileDbfReader#readAllRecords()}).</p>
     *
     * @param geom  geometry record from the {@code .shp} reader
     * @param attrs attribute map from the {@code .dbf} reader
     * @return a new {@link ShapeFeature}, or {@code null} to skip
     */
    private static ShapeFeature buildFeature(ShapefileGeometryReader.ShapeRecord geom,
                                             Map<String, String> attrs) {
        int type = geom.shapeType();

        return switch (type) {

            case ShapefileGeometryReader.TYPE_POLYGON,
                 ShapefileGeometryReader.TYPE_POLYLINE -> {
                List<List<Point2D.Double>> rings = convertRings(geom.rings());
                if (rings.isEmpty()) yield null;
                yield new ShapeFeature(type, rings, Collections.emptyList(), attrs);
            }

            case ShapefileGeometryReader.TYPE_POINT,
                 ShapefileGeometryReader.TYPE_MULTIPOINT -> {
                List<Point2D.Double> pts = convertPoints(geom.points());
                if (pts.isEmpty()) yield null;
                yield new ShapeFeature(type, Collections.emptyList(), pts, attrs);
            }

            default -> null; // unsupported type — skip silently
        };
    }

    // -------------------------------------------------------------------------
    // Coordinate conversion helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a list of degree-coordinate rings to radian-coordinate rings,
     * skipping any ring with fewer than 3 points.
     *
     * @param degreeRings rings in decimal degrees (longitude, latitude)
     * @return converted rings in radians; may be empty if all rings were
     *         degenerate
     */
    private static List<List<Point2D.Double>> convertRings(
            List<List<Point2D.Double>> degreeRings) {

        List<List<Point2D.Double>> result = new ArrayList<>(degreeRings.size());
        for (List<Point2D.Double> ring : degreeRings) {
            if (ring.size() < 3) continue; // degenerate ring — skip
            List<Point2D.Double> converted = new ArrayList<>(ring.size());
            for (Point2D.Double pt : ring) {
                converted.add(toRadians(pt));
            }
            result.add(Collections.unmodifiableList(converted));
        }
        return result;
    }

    /**
     * Converts a flat list of degree-coordinate points to radian coordinates.
     *
     * @param degreePoints points in decimal degrees
     * @return converted points in radians
     */
    private static List<Point2D.Double> convertPoints(List<Point2D.Double> degreePoints) {
        List<Point2D.Double> result = new ArrayList<>(degreePoints.size());
        for (Point2D.Double pt : degreePoints) {
            result.add(toRadians(pt));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Converts a single point from decimal degrees to radians, wrapping
     * longitude to (-π, π].
     *
     * @param pt point with {@code x = longitude degrees}, {@code y = latitude
     *           degrees}
     * @return new point with {@code x = longitude radians}, {@code y =
     *         latitude radians}
     */
    private static Point2D.Double toRadians(Point2D.Double pt) {
        return new Point2D.Double(
                MapUtils.lonDegreesToRadians(pt.x),
                Math.toRadians(pt.y));
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    // wrapLongitude delegated to MapUtils.MapUtils.wrapLongitude(double)

    /**
     * Returns a new path derived from {@code path} by replacing its file
     * extension with {@code newExtension}.
     *
     * @param path         the original path
     * @param newExtension new extension including the leading dot (e.g.
     *                     {@code ".dbf"})
     * @return path with the extension replaced
     */
    private static Path replaceExtension(Path path, String newExtension) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        String base = (dot >= 0) ? filename.substring(0, dot) : filename;
        return path.resolveSibling(base + newExtension);
    }
}