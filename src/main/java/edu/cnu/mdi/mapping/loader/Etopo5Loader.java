package edu.cnu.mdi.mapping.loader;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Loader/query class for the NOAA ETOPO5 global relief grid.
 *
 * <p>ETOPO5.DAT is a global 5 arc-minute grid stored as signed 16-bit
 * big-endian integers. Each value is an elevation in meters, with negative
 * values representing ocean depth.</p>
 *
 * <h2>Grid layout</h2>
 * <ul>
 *   <li>2160 latitude rows</li>
 *   <li>4320 longitude columns</li>
 *   <li>5 arc-minute spacing = 1/12 degree</li>
 *   <li>first row: 90 degrees north</li>
 *   <li>rows proceed southward</li>
 *   <li>columns proceed eastward from 0 degrees longitude</li>
 * </ul>
 *
 * <p>This class loads the whole grid into memory. That is only about
 * 18.7 MB of raw data, plus Java array overhead, so it is quite reasonable
 * for a MDI mapping demo.</p>
 */
public final class Etopo5Loader {

    /** Default classpath location based on src/main/resources layout. */
    public static final String DEFAULT_RESOURCE =
            "/edu/cnu/mdi/etopo5/ETOPO5.DAT";

    /** Number of latitude rows in ETOPO5. */
    public static final int ROWS = 2160;

    /** Number of longitude columns in ETOPO5. */
    public static final int COLS = 4320;

    /** Grid spacing in degrees: 5 arc-minutes = 1/12 degree. */
    public static final double STEP_DEGREES = 1.0 / 12.0;

    /** Number of grid values. */
    private static final int VALUE_COUNT = ROWS * COLS;

    /** Expected file size in bytes: one signed short per grid point. */
    private static final long EXPECTED_BYTE_COUNT = 2L * VALUE_COUNT;

    /**
     * Elevation/depth values in meters, row-major order.
     *
     * <p>Index = row * COLS + col.</p>
     */
    private final short[] elevation;

    private Etopo5Loader(short[] elevation) {
        this.elevation = Objects.requireNonNull(elevation, "elevation");
        if (elevation.length != VALUE_COUNT) {
            throw new IllegalArgumentException(
                    "Expected " + VALUE_COUNT + " values, got " + elevation.length);
        }
    }

    /**
     * Loads ETOPO5 from the default classpath resource:
     *
     * <pre>
     * /edu/cnu/mdi/etopo5/ETOPO5.DAT
     * </pre>
     *
     * @return loaded ETOPO5 grid
     * @throws IOException if the resource cannot be found or read
     */
    public static Etopo5Loader loadDefaultResource() throws IOException {
        return loadFromResource(DEFAULT_RESOURCE);
    }

    /**
     * Loads ETOPO5 from a classpath resource.
     *
     * @param resourcePath absolute classpath resource path
     * @return loaded ETOPO5 grid
     * @throws IOException if the resource cannot be found or read
     */
    public static Etopo5Loader loadFromResource(String resourcePath)
            throws IOException {

        Objects.requireNonNull(resourcePath, "resourcePath");

        // Match the defensive cleanup style used in the GeoJSON loaders.
        resourcePath = resourcePath.replaceAll("/{2,}", "/");

        InputStream in = Etopo5Loader.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        try (InputStream bin = new BufferedInputStream(in)) {
            return load(bin, resourcePath);
        }
    }

    /**
     * Loads ETOPO5 from a filesystem path.
     *
     * @param path path to ETOPO5.DAT
     * @return loaded ETOPO5 grid
     * @throws IOException if the file cannot be read or has the wrong size
     */
    public static Etopo5Loader load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");

        long size = Files.size(path);
        if (size != EXPECTED_BYTE_COUNT) {
            throw new IOException("Unexpected ETOPO5 file size for " + path
                    + ": expected " + EXPECTED_BYTE_COUNT
                    + " bytes, got " + size);
        }

        try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            return load(in, path.toString());
        }
    }

    /**
     * Core binary reader.
     *
     * <p>ETOPO5.DAT is big-endian, so {@link DataInputStream#readShort()}
     * is exactly what we want. Do not use this for ETOPO5.DOS, which has
     * the opposite byte order.</p>
     */
    private static Etopo5Loader load(InputStream in, String sourceDescription)
            throws IOException {

        short[] data = new short[VALUE_COUNT];

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(in))) {
            for (int i = 0; i < VALUE_COUNT; i++) {
                data[i] = dis.readShort(); // big-endian signed 16-bit
            }

            // Check for extra bytes. A clean read should now be at EOF.
            if (dis.read() != -1) {
                throw new IOException("ETOPO5 source has extra data: "
                        + sourceDescription);
            }
        } catch (IOException e) {
            throw new IOException("Could not read complete ETOPO5 grid from "
                    + sourceDescription, e);
        }

        return new Etopo5Loader(data);
    }

    /**
     * Returns the nearest-neighbor elevation in meters for a latitude and
     * longitude given in degrees.
     *
     * <p>Latitude is clamped to the legal range [-90, 90]. Longitude is wrapped
     * into [0, 360).</p>
     *
     * @param latitudeDegrees  latitude in degrees, north positive
     * @param longitudeDegrees longitude in degrees, east positive
     * @return elevation in meters; negative values are below sea level
     */
    public int getElevationMeters(double latitudeDegrees, double longitudeDegrees) {
        int row = nearestRow(latitudeDegrees);
        int col = nearestCol(longitudeDegrees);
        return elevation[index(row, col)];
    }

    /**
     * Returns bilinearly interpolated elevation in meters.
     *
     * <p>This is smoother for mouse-over feedback or color shading. The result
     * is a {@code double} because it is an interpolation of nearby integer
     * grid values.</p>
     *
     * @param latitudeDegrees  latitude in degrees, north positive
     * @param longitudeDegrees longitude in degrees, east positive
     * @return interpolated elevation in meters
     */
    public double getInterpolatedElevationMeters(
            double latitudeDegrees, double longitudeDegrees) {

        double lat = clamp(latitudeDegrees, -90.0, 90.0);
        double lon = normalizeLon360(longitudeDegrees);

        double r = (90.0 - lat) / STEP_DEGREES;
        double c = lon / STEP_DEGREES;

        r = clamp(r, 0.0, ROWS - 1.0);

        int r0 = (int) Math.floor(r);
        int c0 = (int) Math.floor(c) % COLS;

        int r1 = Math.min(r0 + 1, ROWS - 1);
        int c1 = (c0 + 1) % COLS;

        double fr = r - r0;
        double fc = c - Math.floor(c);

        double z00 = elevation[index(r0, c0)];
        double z01 = elevation[index(r0, c1)];
        double z10 = elevation[index(r1, c0)];
        double z11 = elevation[index(r1, c1)];

        double z0 = z00 * (1.0 - fc) + z01 * fc;
        double z1 = z10 * (1.0 - fc) + z11 * fc;

        return z0 * (1.0 - fr) + z1 * fr;
    }

    /**
     * Returns the raw grid value at a row and column.
     *
     * @param row row index in the range {@code 0 <= row < 2160}
     * @param col column index in the range {@code 0 <= col < 4320}
     * @return elevation in meters
     */
    public int getElevationMetersAtGridPoint(int row, int col) {
        checkRow(row);
        checkCol(col);
        return elevation[index(row, col)];
    }

    /**
     * Converts a grid row to latitude in degrees.
     *
     * @param row grid row
     * @return latitude in degrees
     */
    public static double rowToLatitudeDegrees(int row) {
        checkStaticRow(row);
        return 90.0 - row * STEP_DEGREES;
    }

    /**
     * Converts a grid column to longitude in degrees, in [0, 360).
     *
     * @param col grid column
     * @return longitude in degrees
     */
    public static double colToLongitudeDegrees(int col) {
        checkStaticCol(col);
        return col * STEP_DEGREES;
    }

    private static int nearestRow(double latitudeDegrees) {
        double lat = clamp(latitudeDegrees, -90.0, 90.0);
        int row = (int) Math.round((90.0 - lat) / STEP_DEGREES);
        return Math.max(0, Math.min(ROWS - 1, row));
    }

    private static int nearestCol(double longitudeDegrees) {
        double lon = normalizeLon360(longitudeDegrees);
        int col = (int) Math.round(lon / STEP_DEGREES);
        return Math.floorMod(col, COLS);
    }

    private static int index(int row, int col) {
        return row * COLS + col;
    }

    private static double normalizeLon360(double lonDegrees) {
        double lon = lonDegrees % 360.0;
        return (lon < 0.0) ? lon + 360.0 : lon;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void checkRow(int row) {
        checkStaticRow(row);
    }

    private void checkCol(int col) {
        checkStaticCol(col);
    }

    private static void checkStaticRow(int row) {
        if (row < 0 || row >= ROWS) {
            throw new IndexOutOfBoundsException(
                    "row must be in [0, " + (ROWS - 1) + "]: " + row);
        }
    }

    private static void checkStaticCol(int col) {
        if (col < 0 || col >= COLS) {
            throw new IndexOutOfBoundsException(
                    "col must be in [0, " + (COLS - 1) + "]: " + col);
        }
    }
}
