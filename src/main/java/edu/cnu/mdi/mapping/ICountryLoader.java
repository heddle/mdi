package edu.cnu.mdi.mapping;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Format-agnostic contract for loading country boundary features.
 *
 * <p>Implementations exist for GeoJSON ({@link GeoJsonCountryLoader}) and
 * ESRI Shapefile ({@link ShapefileCountryLoader}). Callers such as
 * {@link MapView2D} depend only on this interface so that switching the
 * source format requires no changes to rendering or hit-testing code.</p>
 *
 * <h2>Output contract</h2>
 * <p>All load methods must return an <em>unmodifiable</em> list of
 * {@link GeoJsonCountryLoader.CountryFeature} instances whose polygon
 * coordinates are in radians with longitude wrapped to (-π, π].</p>
 *
 * <h2>Error handling</h2>
 * <p>Implementations must throw {@link IOException} for any I/O or parse
 * error. Individual malformed features may be silently skipped rather than
 * aborting the entire load, but the method must never return {@code null}.</p>
 */
public interface ICountryLoader {

    /**
     * Loads country features from the given filesystem path.
     *
     * <p>The path may point to the primary file of a multi-file format (e.g.
     * the {@code .shp} file of a shapefile set) or to a self-contained file
     * (e.g. a {@code .geojson} file). Implementations are responsible for
     * locating any companion files (e.g. {@code .dbf}, {@code .shx}) relative
     * to the supplied path.</p>
     *
     * @param path path to the data file; must not be {@code null}
     * @return unmodifiable list of country features; never {@code null}
     * @throws IOException if the file cannot be read or parsed
     */
    List<GeoJsonCountryLoader.CountryFeature> load(Path path) throws IOException;

    /**
     * Loads country features from a classpath resource.
     *
     * <p>The resource path must be absolute (begin with {@code /}) so that
     * {@link Class#getResourceAsStream(String)} resolves it correctly. For
     * multi-file formats this method may not be supportable (e.g. a shapefile
     * set cannot be fully represented as a single classpath resource); in that
     * case implementations should throw {@link UnsupportedOperationException}
     * with a clear message.</p>
     *
     * @param resourcePath absolute classpath resource path; must not be
     *                     {@code null}
     * @return unmodifiable list of country features; never {@code null}
     * @throws IOException                  if the resource cannot be read or
     *                                      parsed
     * @throws UnsupportedOperationException if the format does not support
     *                                       classpath resource loading
     */
    List<GeoJsonCountryLoader.CountryFeature> loadFromResource(String resourcePath)
            throws IOException;
}