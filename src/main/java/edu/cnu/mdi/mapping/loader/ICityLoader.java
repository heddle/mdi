package edu.cnu.mdi.mapping.loader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import edu.cnu.mdi.mapping.MapView2D;

/**
 * Format-agnostic contract for loading populated-place (city) features.
 *
 * <p>Implementations exist for GeoJSON ({@link GeoJsonCityLoader}) and ESRI
 * Shapefile ({@link ShapefileCityLoader}). Callers such as {@link MapView2D}
 * depend only on this interface so that switching the source format requires
 * no changes to rendering or hit-testing code.</p>
 *
 * <h2>Output contract</h2>
 * <p>All load methods must return an <em>unmodifiable</em> list of
 * {@link GeoJsonCityLoader.CityFeature} instances whose coordinates are in
 * radians with longitude wrapped to (-π, π].</p>
 *
 * <h2>Error handling</h2>
 * <p>Implementations must throw {@link IOException} for any I/O or parse
 * error. Individual malformed features may be silently skipped rather than
 * aborting the entire load, but the method must never return {@code null}.</p>
 */
public interface ICityLoader {

    /**
     * Loads city features from the given filesystem path.
     *
     * <p>The path may point to the primary file of a multi-file format (e.g.
     * the {@code .shp} file of a shapefile set) or to a self-contained file
     * (e.g. a {@code .geojson} file). Implementations are responsible for
     * locating companion files (e.g. {@code .dbf}, {@code .shx}) relative to
     * the supplied path.</p>
     *
     * @param path path to the data file; must not be {@code null}
     * @return unmodifiable list of city features; never {@code null}
     * @throws IOException if the file cannot be read or parsed
     */
    List<GeoJsonCityLoader.CityFeature> load(Path path) throws IOException;

    /**
     * Loads city features from a classpath resource.
     *
     * <p>The resource path must be absolute (begin with {@code /}). For
     * multi-file formats this may not be supportable; implementations should
     * throw {@link UnsupportedOperationException} with a clear message in that
     * case.</p>
     *
     * @param resourcePath absolute classpath resource path; must not be
     *                     {@code null}
     * @return unmodifiable list of city features; never {@code null}
     * @throws IOException                  if the resource cannot be read or
     *                                      parsed
     * @throws UnsupportedOperationException if the format does not support
     *                                       classpath resource loading
     */
    List<GeoJsonCityLoader.CityFeature> loadFromResource(String resourcePath)
            throws IOException;
}