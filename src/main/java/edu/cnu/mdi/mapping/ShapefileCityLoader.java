package edu.cnu.mdi.mapping;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads {@link GeoJsonCityLoader.CityFeature} instances from an ESRI Shapefile
 * set ({@code .shp} + {@code .dbf}) without any third-party dependencies.
 *
 * <h2>Expected shapefile schema</h2>
 * <p>The {@code .dbf} attribute table should contain the following fields
 * (tried in priority order; the first non-empty value is used):</p>
 * <ul>
 *   <li><b>Name</b> — {@code NAME}, {@code name}, {@code NAMEASCII},
 *       {@code NAME_EN} — city display name.</li>
 *   <li><b>Country</b> — {@code ADM0NAME}, {@code adm0name},
 *       {@code SOV0NAME}, {@code COUNTRY} — country name.</li>
 *   <li><b>Population</b> — {@code POP_MAX}, {@code pop_max},
 *       {@code POPULATION}, {@code POP2015} — population estimate as an
 *       integer string; {@code -1} if absent or non-numeric.</li>
 *   <li><b>Scalerank</b> — {@code SCALERANK}, {@code scalerank},
 *       {@code SCALE_RANK} — importance rank as an integer string; {@code -1}
 *       if absent or non-numeric.</li>
 * </ul>
 * <p>Features where the name field is absent or empty are silently skipped.
 * Features with unknown population or scalerank are included but will report
 * {@code -1} for those values, which the renderers treat as "unknown".</p>
 *
 * <h2>Supported geometry types</h2>
 * <p>Only {@link ShapefileGeometryReader#TYPE_POINT} and
 * {@link ShapefileGeometryReader#TYPE_MULTIPOINT} records are used. City
 * shapefiles (e.g. Natural Earth
 * {@code ne_10m_populated_places.shp}) use Point geometry exclusively. For
 * MultiPoint records, the first point in the set is used as the city
 * location.</p>
 *
 * <h2>Coordinate assumptions</h2>
 * <p>Input coordinates are assumed to be WGS84 geographic degrees (longitude,
 * latitude). They are converted to radians and the longitude is wrapped to
 * (-π, π] before being stored in the returned features. If the shapefile has
 * a {@code .prj} companion declaring a different CRS, it is ignored.</p>
 *
 * <h2>File layout contract</h2>
 * <p>The {@code .dbf} file is derived from the {@code .shp} path by replacing
 * the extension. Both files must reside in the same directory with the same
 * base name.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ICityLoader loader = new ShapefileCityLoader();
 * List<GeoJsonCityLoader.CityFeature> cities =
 *         loader.load(Path.of("/data/ne_10m_populated_places.shp"));
 * mapView.setCities(cities);
 * }</pre>
 *
 * <p>This class is stateless and thread-safe; a single instance may be
 * reused across multiple {@link #load} calls.</p>
 */
public final class ShapefileCityLoader implements ICityLoader {

    // -------------------------------------------------------------------------
    // DBF field name candidates (tried in priority order)
    // -------------------------------------------------------------------------

    private static final String[] NAME_FIELDS       =
            { "NAME", "name", "NAMEASCII", "nameascii", "NAME_EN" };

    private static final String[] COUNTRY_FIELDS    =
            { "ADM0NAME", "adm0name", "SOV0NAME", "sov0name", "COUNTRY" };

    private static final String[] POPULATION_FIELDS =
            { "POP_MAX", "pop_max", "POPULATION", "POP2015", "POP_MIN" };

    private static final String[] SCALERANK_FIELDS  =
            { "SCALERANK", "scalerank", "SCALE_RANK" };

    // -------------------------------------------------------------------------
    // ICityLoader
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Opens {@code path} as the {@code .shp} geometry file and derives the
     * companion {@code .dbf} attribute path by replacing the file extension.</p>
     *
     * @param path path to the {@code .shp} file; the companion {@code .dbf}
     *             must exist in the same directory with the same base name
     * @throws IOException if either file cannot be opened or read
     */
    @Override
    public List<GeoJsonCityLoader.CityFeature> load(Path path) throws IOException {
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
    public List<GeoJsonCityLoader.CityFeature> loadFromResource(String resourcePath) {
        throw new UnsupportedOperationException(
                "Shapefile loading requires a filesystem path. "
              + "Classpath resource loading is not supported for multi-file "
              + "shapefile sets. Use load(Path) instead.");
    }

    // -------------------------------------------------------------------------
    // Core loading logic
    // -------------------------------------------------------------------------

    /**
     * Reads city features from a matched pair of {@code .shp} and {@code .dbf}
     * files.
     *
     * <p>The geometry and attribute files are iterated in lock-step by record
     * index. The attribute cursor advances once per geometry record (including
     * skipped null-shape records) to maintain alignment.</p>
     *
     * @param shpPath path to the {@code .shp} geometry file
     * @param dbfPath path to the {@code .dbf} attribute file
     * @return unmodifiable list of city features
     * @throws IOException if either file cannot be read
     */
    private static List<GeoJsonCityLoader.CityFeature> loadFromPaths(
            Path shpPath, Path dbfPath) throws IOException {

        List<GeoJsonCityLoader.CityFeature> result = new ArrayList<>();

        try (ShapefileGeometryReader shp = new ShapefileGeometryReader(shpPath);
             ShapefileDbfReader      dbf = new ShapefileDbfReader(dbfPath)) {

            // Read all attributes up-front. For populated-places data
            // (~7,000 records) this is well within acceptable memory use.
            List<Map<String, String>> attributes = dbf.readAllRecords();
            int attrIndex = 0;

            ShapefileGeometryReader.ShapeRecord geomRecord;
            while ((geomRecord = shp.nextRecord()) != null) {

                Map<String, String> attrs = (attrIndex < attributes.size())
                        ? attributes.get(attrIndex)
                        : Collections.emptyMap();
                attrIndex++;

                // Extract the point coordinate from the geometry record.
                double lonDeg;
                double latDeg;

                int shapeType = geomRecord.shapeType();
                if (shapeType == ShapefileGeometryReader.TYPE_POINT) {
                    if (geomRecord.points().isEmpty()) continue;
                    lonDeg = geomRecord.points().get(0).x;
                    latDeg = geomRecord.points().get(0).y;

                } else if (shapeType == ShapefileGeometryReader.TYPE_MULTIPOINT) {
                    if (geomRecord.points().isEmpty()) continue;
                    // Use the first point as the representative location.
                    lonDeg = geomRecord.points().get(0).x;
                    latDeg = geomRecord.points().get(0).y;

                } else {
                    // Non-point geometry type — skip.
                    continue;
                }

                // Convert to radians and wrap longitude.
                double lon = wrapLongitude(Math.toRadians(lonDeg));
                double lat = Math.toRadians(latDeg);

                // Extract string attributes.
                String name        = firstNonEmpty(attrs, NAME_FIELDS);
                String countryName = firstNonEmpty(attrs, COUNTRY_FIELDS);

                if (name == null || name.isEmpty()) {
                    continue; // a city without a name is not useful
                }

                // Extract numeric attributes with graceful fallback to -1.
                long population = parseLong(firstNonEmpty(attrs, POPULATION_FIELDS), -1L);
                int  scalerank  = parseInt(firstNonEmpty(attrs, SCALERANK_FIELDS),   -1);

                result.add(new GeoJsonCityLoader.CityFeature(
                        name, countryName, lon, lat, population, scalerank));
            }
        }

        return Collections.unmodifiableList(result);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the first non-empty value found in {@code attrs} for any of the
     * given field names, or {@code null} if none are present or all are empty.
     *
     * @param attrs      attribute map for one record
     * @param fieldNames candidate field names in priority order
     * @return first non-empty string value, or {@code null}
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
     * Parses a string as a {@code long}, returning {@code defaultValue} if the
     * string is {@code null}, empty, or not a valid integer representation.
     *
     * @param s            string to parse (may be {@code null})
     * @param defaultValue value returned on parse failure
     * @return parsed value or {@code defaultValue}
     */
    private static long parseLong(String s, long defaultValue) {
        if (s == null || s.isEmpty()) return defaultValue;
        try {
            // Strip any decimal part (DBF numeric fields sometimes include ".0")
            int dot = s.indexOf('.');
            return Long.parseLong(dot >= 0 ? s.substring(0, dot) : s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parses a string as an {@code int}, returning {@code defaultValue} if the
     * string is {@code null}, empty, or not a valid integer representation.
     *
     * @param s            string to parse (may be {@code null})
     * @param defaultValue value returned on parse failure
     * @return parsed value or {@code defaultValue}
     */
    private static int parseInt(String s, int defaultValue) {
        if (s == null || s.isEmpty()) return defaultValue;
        try {
            int dot = s.indexOf('.');
            return Integer.parseInt(dot >= 0 ? s.substring(0, dot) : s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns a new path with the file extension replaced by
     * {@code newExtension}.
     *
     * @param path         original path
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