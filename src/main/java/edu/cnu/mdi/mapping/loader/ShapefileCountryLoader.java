package edu.cnu.mdi.mapping.loader;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.cnu.mdi.mapping.render.CountryRenderer;
import edu.cnu.mdi.mapping.shapefile.ShapefileDbfReader;
import edu.cnu.mdi.mapping.shapefile.ShapefileGeometryReader;
import edu.cnu.mdi.mapping.shapefile.ShapefileGeometryReader.ShapeRecord;
import edu.cnu.mdi.mapping.theme.MapUtils;

/**
 * Loads {@link GeoJsonCountryLoader.CountryFeature} instances from an ESRI
 * Shapefile set ({@code .shp} + {@code .dbf}) without any third-party
 * dependencies.
 *
 * <h2>Expected shapefile schema</h2>
 * <p>The {@code .dbf} attribute table must contain at minimum:</p>
 * <ul>
 *   <li><b>{@code ADMIN}</b> (or {@code NAME} / {@code NAME_LONG}) — the
 *       administrative / country name, e.g. "United States of America".</li>
 *   <li><b>{@code ISO_A3}</b> (or {@code ADM0_A3} / {@code GU_A3}) — the
 *       ISO 3166-1 alpha-3 code, e.g. "USA".</li>
 * </ul>
 * <p>Both fields are tried in priority order; the first non-empty value found
 * is used. Features where both the name and code fields are absent or empty
 * are silently skipped.</p>
 *
 * <h2>Supported geometry types</h2>
 * <p>Only {@link ShapefileGeometryReader#TYPE_POLYGON} records are used.
 * Records of other types are silently skipped. Country shapefiles (e.g.
 * Natural Earth {@code ne_10m_admin_0_countries.shp}) use Polygon geometry
 * exclusively.</p>
 *
 * <h2>Ring handling</h2>
 * <p>All rings (parts) within a Polygon record are included — outer shells
 * and interior holes alike. The {@link CountryRenderer} fills all rings,
 * which is visually correct for the typical use case. Callers that need to
 * distinguish shells from holes can use
 * {@link ShapefileGeometryReader#isClockwise(List)}.</p>
 *
 * <h2>Coordinate assumptions</h2>
 * <p>Input coordinates are assumed to be in WGS84 geographic degrees
 * (longitude, latitude order) as used by all Natural Earth shapefiles. They
 * are converted to radians and the longitude is wrapped to (-π, π] before
 * being stored in the returned features. If the shapefile has a {@code .prj}
 * companion file declaring a different CRS, it is ignored — callers are
 * responsible for ensuring the file uses WGS84.</p>
 *
 * <h2>Multi-record countries</h2>
 * <p>Natural Earth shapefiles use one Polygon record per country, so each
 * shapefile record maps to exactly one {@link GeoJsonCountryLoader.CountryFeature}.
 * The parts (rings) within that record become the feature's polygon list,
 * which is equivalent to the ring list from a GeoJSON MultiPolygon.</p>
 *
 * <h2>File layout contract</h2>
 * <p>The {@code .dbf} file is assumed to have the same base name and parent
 * directory as the {@code .shp} file. Given a path
 * {@code /data/countries.shp}, the companion file is expected at
 * {@code /data/countries.dbf}. An {@link IOException} is thrown if the
 * companion file is not found.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ICountryLoader loader = new ShapefileCountryLoader();
 * List<GeoJsonCountryLoader.CountryFeature> features =
 *         loader.load(Path.of("/data/ne_10m_admin_0_countries.shp"));
 * mapView.setCountries(features);
 * }</pre>
 *
 * <p>This class is stateless and thread-safe; a single instance may be
 * reused across multiple {@link #load} calls.</p>
 */
public final class ShapefileCountryLoader implements ICountryLoader {

    // -------------------------------------------------------------------------
    // DBF field name candidates (tried in priority order)
    // -------------------------------------------------------------------------

    /**
     * Field name candidates for the country's administrative name, tried left
     * to right. The first field present in the {@code .dbf} with a non-empty
     * value is used.
     */
    private static final String[] ADMIN_FIELDS =
            { "ADMIN", "NAME_LONG", "NAME", "SOVEREIGNT", "GEOUNIT" };

    /**
     * Field name candidates for the ISO 3166-1 alpha-3 code, tried left to
     * right.
     */
    private static final String[] ISO_FIELDS =
            { "ISO_A3", "ADM0_A3", "GU_A3", "SU_A3", "BRK_A3" };

    // -------------------------------------------------------------------------
    // ICountryLoader
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Opens {@code path} as the {@code .shp} geometry file and derives the
     * {@code .dbf} attribute file path by replacing the extension. Both files
     * are read sequentially; the record order in each file must match (which
     * is guaranteed by the shapefile specification).</p>
     *
     * @param path path to the {@code .shp} file; the companion {@code .dbf}
     *             must exist in the same directory with the same base name
     * @throws IOException if either file cannot be opened, the files have
     *                     differing record counts, or a geometry error occurs
     */
    @Override
    public List<GeoJsonCountryLoader.CountryFeature> load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        Path dbfPath = replaceExtension(path, ".dbf");
        return loadFromPaths(path, dbfPath);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Shapefiles are multi-file formats and cannot be loaded from a single
     * classpath resource. This method always throws
     * {@link UnsupportedOperationException}. Use {@link #load(Path)} with a
     * filesystem path instead.</p>
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<GeoJsonCountryLoader.CountryFeature> loadFromResource(String resourcePath) {
        throw new UnsupportedOperationException(
                "Shapefile loading requires a filesystem path. "
              + "Classpath resource loading is not supported for multi-file "
              + "shapefile sets. Use load(Path) instead.");
    }

    // -------------------------------------------------------------------------
    // Core loading logic
    // -------------------------------------------------------------------------

    /**
     * Loads country features by reading geometry from {@code shpPath} and
     * attributes from {@code dbfPath}.
     *
     * <p>The two files are iterated in lock-step by record index. If the
     * geometry record is not a Polygon, or if required attributes are missing,
     * the record is skipped without incrementing the attribute index — the DBF
     * cursor always advances one step per geometry record regardless, because
     * the attribute table has one row per shapefile record including null-shape
     * placeholders.</p>
     *
     * @param shpPath path to the {@code .shp} file
     * @param dbfPath path to the {@code .dbf} file
     * @return unmodifiable list of country features
     * @throws IOException if either file cannot be read
     */
    private static List<GeoJsonCountryLoader.CountryFeature> loadFromPaths(
            Path shpPath, Path dbfPath) throws IOException {

        List<GeoJsonCountryLoader.CountryFeature> result = new ArrayList<>();

        try (ShapefileGeometryReader shp = new ShapefileGeometryReader(shpPath);
             ShapefileDbfReader      dbf = new ShapefileDbfReader(dbfPath)) {

            // Read all attribute records up-front. For country-level data
            // (~250 records) this is negligible in memory; it simplifies the
            // lock-step iteration significantly.
            List<Map<String, String>> attributes = dbf.readAllRecords();
            int attrIndex = 0;

            ShapefileGeometryReader.ShapeRecord geomRecord;
            while ((geomRecord = shp.nextRecord()) != null) {

                // Retrieve the corresponding attribute row.
                // Null-shape records are skipped by the geometry reader but
                // still have a corresponding DBF row, so we must advance the
                // attribute cursor regardless of whether we produced a feature.
                Map<String, String> attrs = (attrIndex < attributes.size())
                        ? attributes.get(attrIndex)
                        : Collections.emptyMap();
                attrIndex++;

                // We only handle Polygon geometry for countries.
                if (geomRecord.shapeType() != ShapefileGeometryReader.TYPE_POLYGON) {
                    continue;
                }

                // Extract required attributes.
                String adminName = firstNonEmpty(attrs, ADMIN_FIELDS);
                String isoA3    = firstNonEmpty(attrs, ISO_FIELDS);

                if (adminName == null || adminName.isEmpty()
                        || isoA3 == null || isoA3.isEmpty()) {
                    continue; // skip features with missing required fields
                }

                // Convert each ring from degrees to radians and build the
                // polygon list expected by CountryFeature.
                List<List<Point2D.Double>> polygons = new ArrayList<>();
                for (List<Point2D.Double> ring : geomRecord.rings()) {
                    if (ring.size() < 3) continue; // degenerate ring
                    List<Point2D.Double> converted = new ArrayList<>(ring.size());
                    for (Point2D.Double pt : ring) {
                        double lon = MapUtils.lonDegreesToRadians(pt.x);
                        double lat = Math.toRadians(pt.y);
                        converted.add(new Point2D.Double(lon, lat));
                    }
                    polygons.add(converted);
                }

                if (polygons.isEmpty()) continue;

                result.add(new GeoJsonCountryLoader.CountryFeature(
                        adminName, isoA3, polygons));
            }
        }

        return Collections.unmodifiableList(result);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the first non-empty value found in {@code attrs} for any of the
     * given {@code fieldNames}, or {@code null} if none of the names are
     * present or all values are empty.
     *
     * @param attrs      attribute map for one record
     * @param fieldNames candidate field names in priority order
     * @return first non-empty value, or {@code null}
     */
    private static String firstNonEmpty(Map<String, String> attrs,
                                        String[] fieldNames) {
        for (String name : fieldNames) {
            String value = attrs.get(name);
            if (value != null && !value.isEmpty()) return value;
        }
        return null;
    }

    /**
     * Returns a new path with the file extension replaced by
     * {@code newExtension}.
     *
     * <p>For example, given {@code /data/countries.shp} and {@code ".dbf"},
     * returns {@code /data/countries.dbf}.</p>
     *
     * @param path         original path
     * @param newExtension new extension including the leading dot
     * @return path with the extension replaced
     */
    private static Path replaceExtension(Path path, String newExtension) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        String base = (dot >= 0) ? filename.substring(0, dot) : filename;
        return path.resolveSibling(base + newExtension);
    }

    // wrapLongitude delegated to MapUtils.MapUtils.wrapLongitude(double)
}