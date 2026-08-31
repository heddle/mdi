package edu.cnu.mdi.mapping.shapefile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Validates that shapefile coordinates can safely be interpreted as WGS84
 * longitude and latitude in decimal degrees.
 *
 * <p>MDI intentionally does not provide general coordinate-reference-system
 * transformations. If a companion {@code .prj} file is present, this class
 * therefore accepts WGS84 geographic coordinates and unprojected NAD83/GRS80
 * geographic coordinates with degree angular units. NAD83 coordinates are
 * treated as WGS84 without a datum transformation; their metre-scale
 * difference is negligible for MDI's map-display use cases. Projected,
 * malformed, older North American datums, and unrecognized definitions are
 * rejected before coordinates can be mistaken for longitude and latitude.</p>
 *
 * <p>A missing {@code .prj} remains permitted for compatibility with existing
 * shapefile sets. In that case the caller is explicitly relying on MDI's
 * documented WGS84 longitude/latitude assumption.</p>
 */
public final class ShapefileCrsValidator {

    private ShapefileCrsValidator() {}

    /**
     * Validates the companion projection file for a shapefile, if present.
     *
     * @param shpPath path to the {@code .shp} file
     * @throws IOException if the projection file cannot be read or does not
     *                     describe a supported WGS84 or NAD83 geographic CRS
     *                     in degrees
     */
    public static void validate(Path shpPath) throws IOException {
        Objects.requireNonNull(shpPath, "shpPath");

        Path prjPath = companionPath(shpPath, ".prj");
        if (!Files.exists(prjPath)) {
            Path upperCasePath = companionPath(shpPath, ".PRJ");
            if (!Files.exists(upperCasePath)) {
                return;
            }
            prjPath = upperCasePath;
        }

        String wkt = Files.readString(prjPath, StandardCharsets.UTF_8).trim();
        if (wkt.startsWith("\uFEFF")) {
            wkt = wkt.substring(1).trim();
        }
        validateWkt(wkt, prjPath);
    }

    private static void validateWkt(String wkt, Path prjPath) throws IOException {
        String upper = wkt.toUpperCase(Locale.ROOT);
        int openingBracket = upper.indexOf('[');
        String rootType = (openingBracket < 0 ? upper
                : upper.substring(0, openingBracket)).trim();
        boolean geographic = rootType.equals("GEOGCS")
                || rootType.equals("GEOGCRS")
                || rootType.equals("GEODCRS");

        if (!geographic) {
            throw unsupported(prjPath,
                    "the top-level CRS is not a geographic CRS");
        }

        String identifiers = upper.replaceAll("[^A-Z0-9]", "");
        boolean wgs84 = identifiers.contains("WGS84")
                || identifiers.contains("WGS1984")
                || identifiers.contains("WORLDGEODETICSYSTEM1984");
        boolean nad83 = (identifiers.contains("NAD83")
                || identifiers.contains("NORTHAMERICAN1983")
                || identifiers.contains("NORTHAMERICANDATUM1983"))
                && identifiers.contains("GRS1980");
        if (!wgs84 && !nad83) {
            throw unsupported(prjPath,
                    "the datum is not recognized as WGS84 or NAD83/GRS80");
        }

        String compact = upper.replaceAll("\\s+", "");
        boolean degrees = compact.contains("UNIT[\"DEGREE\"")
                || compact.contains("UNIT[\"DEGREES\"")
                || compact.contains("ANGLEUNIT[\"DEGREE\"")
                || compact.contains("ANGLEUNIT[\"DEGREES\"");
        if (!degrees) {
            throw unsupported(prjPath,
                    "the geographic angular unit is not recognized as degrees");
        }
    }

    private static IOException unsupported(Path prjPath, String reason) {
        return new IOException("Unsupported shapefile coordinate reference system in "
                + prjPath + ": " + reason
                + ". MDI requires WGS84 or NAD83/GRS80 longitude/latitude coordinates in decimal degrees.");
    }

    private static Path companionPath(Path path, String extension) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        String base = (dot >= 0) ? filename.substring(0, dot) : filename;
        return path.resolveSibling(base + extension);
    }
}
