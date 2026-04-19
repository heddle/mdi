package edu.cnu.mdi.mapping;

import java.awt.geom.Point2D;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Zero-dependency reader for the binary geometry data in ESRI Shapefile
 * ({@code .shp}) files.
 *
 * <h2>File structure</h2>
 * <p>A {@code .shp} file consists of:
 * <ol>
 *   <li>A 100-byte file header (mixed endianness — see below).</li>
 *   <li>A sequence of variable-length geometry records, each preceded by an
 *       8-byte big-endian record header.</li>
 * </ol>
 *
 * <h2>Endianness quirk</h2>
 * <p>The ESRI specification uses <em>big-endian</em> integers in the file
 * header (file code, file length) and in each record header (record number,
 * content length), but <em>little-endian</em> integers and doubles everywhere
 * else. This reader handles both transparently using {@link ByteBuffer#order}.
 * </p>
 *
 * <h2>Supported shape types</h2>
 * <table border="1">
 *   <tr><th>Type value</th><th>Name</th><th>Used for</th></tr>
 *   <tr><td>0</td><td>Null Shape</td><td>Silently skipped</td></tr>
 *   <tr><td>1</td><td>Point</td><td>City / populated-place data</td></tr>
 *   <tr><td>5</td><td>Polygon</td><td>Country boundary data</td></tr>
 *   <tr><td>8</td><td>MultiPoint</td><td>Multi-point city data</td></tr>
 * </table>
 * <p>Shape types 3 (PolyLine), 11–15 (Z/M variants), and 21–25 (M variants)
 * are read but produce empty geometry lists; callers should filter them out.
 * Unrecognized type values cause an {@link IOException}.</p>
 *
 * <h2>Polygon ring handling</h2>
 * <p>ESRI Polygons store all rings (outer shells and interior holes) in a
 * flat {@code Parts} array. The winding order distinguishes shells (clockwise
 * in the ESRI convention, which is counter-clockwise in geographic north-up
 * orientation) from holes (opposite winding). This reader returns <em>all
 * rings</em> without filtering holes so that the country renderer — which
 * simply fills every ring — produces correct results for filled rendering.
 * Callers that need topologically correct hole handling can use
 * {@link #isClockwise(List)} to classify rings.</p>
 *
 * <h2>Coordinate output</h2>
 * <p>All returned coordinates are in the source file's native units. For
 * Natural Earth data this is decimal degrees (WGS84). Callers must convert
 * to radians and wrap longitude before passing to {@link IMapProjection}.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * try (ShapefileGeometryReader shp = new ShapefileGeometryReader(path)) {
 *     ShapefileGeometryReader.ShapeRecord record;
 *     while ((record = shp.nextRecord()) != null) {
 *         int type = record.shapeType();
 *         if (type == ShapefileGeometryReader.TYPE_POLYGON) {
 *             for (List<Point2D.Double> ring : record.rings()) {
 *                 // process ring
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread safety</h2>
 * <p>Instances are not thread-safe. Each calling thread should open its own
 * reader.</p>
 */
public final class ShapefileGeometryReader implements Closeable {

    // -------------------------------------------------------------------------
    // Shape type constants (ESRI specification values)
    // -------------------------------------------------------------------------

    /** Shape type: no geometry present. Records of this type are skipped. */
    public static final int TYPE_NULL       = 0;

    /** Shape type: a single (X, Y) point. */
    public static final int TYPE_POINT      = 1;

    /** Shape type: an ordered set of vertices forming connected line segments. */
    public static final int TYPE_POLYLINE   = 3;

    /** Shape type: one or more rings forming a polygon. */
    public static final int TYPE_POLYGON    = 5;

    /** Shape type: a set of points. */
    public static final int TYPE_MULTIPOINT = 8;

    // -------------------------------------------------------------------------
    // File header constants
    // -------------------------------------------------------------------------

    /** Expected file code at bytes 0–3 (big-endian). */
    private static final int FILE_CODE    = 9994;

    /** Expected version at bytes 28–31 (little-endian). */
    private static final int VERSION      = 1000;

    /** Total size of the file header in bytes. */
    private static final int HEADER_BYTES = 100;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final FileChannel channel;

    /**
     * Shape type declared in the file header. All records in a single
     * {@code .shp} file must be of this type (or {@link #TYPE_NULL}).
     */
    private final int fileShapeType;

    /** Total file length in bytes (from the header). */
    private final long fileLength;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Opens the {@code .shp} file at the given path and validates the file
     * header.
     *
     * @param path path to the {@code .shp} file; must not be {@code null}
     * @throws IOException if the file cannot be opened, the file code is
     *                     wrong, or the version is unsupported
     */
    public ShapefileGeometryReader(Path path) throws IOException {
        channel = FileChannel.open(path, StandardOpenOption.READ);
        try {
            ByteBuffer header = readBytes(HEADER_BYTES);

            // Bytes 0–3: file code (big-endian)
            header.order(ByteOrder.BIG_ENDIAN);
            int fileCode = header.getInt(0);
            if (fileCode != FILE_CODE) {
                throw new IOException(
                        "Not a valid shapefile: expected file code " + FILE_CODE
                        + " but got " + fileCode);
            }

            // Bytes 24–27: file length in 16-bit words (big-endian)
            // Multiply by 2 to convert to bytes.
            fileLength = (long) header.getInt(24) * 2L;

            // Bytes 28–35: version and shape type (little-endian)
            header.order(ByteOrder.LITTLE_ENDIAN);
            int version = header.getInt(28);
            if (version != VERSION) {
                throw new IOException(
                        "Unsupported shapefile version: " + version
                        + " (expected " + VERSION + ")");
            }
            fileShapeType = header.getInt(32);

        } catch (IOException e) {
            channel.close();
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the shape type declared in the file header.
     *
     * <p>All records in a well-formed shapefile have this shape type (or
     * {@link #TYPE_NULL}). Use the {@code TYPE_*} constants to identify the
     * type.</p>
     *
     * @return file-level shape type
     */
    public int getFileShapeType() { return fileShapeType; }

    /**
     * Reads and returns the next geometry record from the file, or
     * {@code null} when the end of the file is reached.
     *
     * <p>Null-shape records ({@link #TYPE_NULL}) are silently skipped and do
     * not produce a return value — the next non-null record is returned
     * instead. This matches the common convention that null shapes are
     * placeholders for deleted or missing features.</p>
     *
     * <p>The returned {@link ShapeRecord} contains:
     * <ul>
     *   <li>The record's one-based {@link ShapeRecord#recordNumber()}.</li>
     *   <li>The {@link ShapeRecord#shapeType()} for this record.</li>
     *   <li>For {@link #TYPE_POINT}: a single-element
     *       {@link ShapeRecord#points()} list.</li>
     *   <li>For {@link #TYPE_POLYGON} and {@link #TYPE_POLYLINE}: a
     *       {@link ShapeRecord#rings()} list of ring/part coordinate
     *       lists.</li>
     *   <li>For {@link #TYPE_MULTIPOINT}: a flat
     *       {@link ShapeRecord#points()} list.</li>
     * </ul>
     *
     * @return the next record, or {@code null} at end of file
     * @throws IOException if a read error occurs or the record contains an
     *                     unrecognized shape type
     */
    public ShapeRecord nextRecord() throws IOException {
        while (channel.position() < fileLength - 8) {
            // Record header: 8 bytes, big-endian.
            ByteBuffer recHeader = readBytes(8);
            recHeader.order(ByteOrder.BIG_ENDIAN);
            int recordNumber  = recHeader.getInt();       // 1-based
            int contentWords  = recHeader.getInt();       // content length in 16-bit words
            int contentBytes  = contentWords * 2;

            if (contentBytes < 4) {
                // Degenerate record — skip.
                channel.position(channel.position() + contentBytes);
                continue;
            }

            ByteBuffer content = readBytes(contentBytes);
            content.order(ByteOrder.LITTLE_ENDIAN);

            int shapeType = content.getInt();

            // Silently skip null shapes.
            if (shapeType == TYPE_NULL) continue;

            return switch (shapeType) {
                case TYPE_POINT      -> readPoint(recordNumber, content);
                case TYPE_POLYLINE,
                     TYPE_POLYGON    -> readRings(recordNumber, shapeType, content);
                case TYPE_MULTIPOINT -> readMultiPoint(recordNumber, content);
                default -> throw new IOException(
                        "Unsupported shape type " + shapeType
                        + " in record " + recordNumber);
            };
        }
        return null; // end of file
    }

    /**
     * Reads all records from the file sequentially and returns them as an
     * unmodifiable list.
     *
     * <p>This is a convenience method that repeatedly calls
     * {@link #nextRecord()} until {@code null} is returned. The channel is
     * not rewound before reading — callers that need a fresh read should
     * close and reopen the reader.</p>
     *
     * @return unmodifiable list of all non-null geometry records
     * @throws IOException if any read error occurs
     */
    public List<ShapeRecord> readAllRecords() throws IOException {
        List<ShapeRecord> records = new ArrayList<>();
        ShapeRecord rec;
        while ((rec = nextRecord()) != null) {
            records.add(rec);
        }
        return Collections.unmodifiableList(records);
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        channel.close();
    }

    // -------------------------------------------------------------------------
    // Static geometry helpers
    // -------------------------------------------------------------------------

    /**
     * Tests whether a polygon ring is oriented clockwise in the ESRI
     * convention (which uses a screen-space y-axis pointing downward).
     *
     * <p>Uses the shoelace formula: a positive signed area indicates
     * clockwise orientation in ESRI coordinates (outer shells are clockwise;
     * holes are counter-clockwise).</p>
     *
     * <p>Note that geographic coordinates have the y-axis pointing upward, so
     * the convention is inverted relative to screen space. For Natural Earth
     * data this distinction rarely matters because the renderer fills all
     * rings regardless of winding order.</p>
     *
     * @param ring list of (longitude, latitude) points forming a closed ring
     * @return {@code true} if the ring is clockwise in ESRI screen convention
     */
    public static boolean isClockwise(List<Point2D.Double> ring) {
        int n = ring.size();
        if (n < 3) return false;
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            Point2D.Double a = ring.get(i);
            Point2D.Double b = ring.get((i + 1) % n);
            sum += (b.x - a.x) * (b.y + a.y);
        }
        return sum > 0.0;
    }

    // -------------------------------------------------------------------------
    // Private geometry parsers
    // -------------------------------------------------------------------------

    /**
     * Parses a Point record (type 1).
     *
     * <p>Content layout (after the shape-type int already consumed):
     * <pre>
     *   X (double, little-endian)
     *   Y (double, little-endian)
     * </pre>
     *
     * @param recordNumber one-based record number
     * @param content      content buffer positioned after the shape-type int
     * @return a {@link ShapeRecord} with a single point
     */
    private static ShapeRecord readPoint(int recordNumber, ByteBuffer content) {
        double x = content.getDouble();
        double y = content.getDouble();
        List<Point2D.Double> pts = Collections.singletonList(new Point2D.Double(x, y));
        return new ShapeRecord(recordNumber, TYPE_POINT, pts, Collections.emptyList());
    }

    /**
     * Parses a MultiPoint record (type 8).
     *
     * <p>Content layout (after shape-type int):
     * <pre>
     *   Bounding box: 4 doubles (Xmin, Ymin, Xmax, Ymax)
     *   NumPoints:    int
     *   Points:       NumPoints × (X double, Y double)
     * </pre>
     *
     * @param recordNumber one-based record number
     * @param content      content buffer positioned after the shape-type int
     * @return a {@link ShapeRecord} with all points in a flat list
     */
    private static ShapeRecord readMultiPoint(int recordNumber, ByteBuffer content) {
        content.position(content.position() + 32); // skip bounding box
        int numPoints = content.getInt();
        List<Point2D.Double> pts = new ArrayList<>(numPoints);
        for (int i = 0; i < numPoints; i++) {
            pts.add(new Point2D.Double(content.getDouble(), content.getDouble()));
        }
        return new ShapeRecord(recordNumber, TYPE_MULTIPOINT,
                               Collections.unmodifiableList(pts), Collections.emptyList());
    }

    /**
     * Parses a Polygon (type 5) or PolyLine (type 3) record, both of which
     * use the same binary layout.
     *
     * <p>Content layout (after shape-type int):
     * <pre>
     *   Bounding box: 4 doubles (Xmin, Ymin, Xmax, Ymax)  — 32 bytes
     *   NumParts:     int       — number of rings / parts
     *   NumPoints:    int       — total point count across all parts
     *   Parts[]:      NumParts ints  — start index of each part in Points[]
     *   Points[]:     NumPoints × (X double, Y double)
     * </pre>
     *
     * <p>Each element of {@code Parts[]} is the index of the first point of
     * that part in the flat {@code Points[]} array. The last part extends to
     * the end of the array.</p>
     *
     * @param recordNumber one-based record number
     * @param shapeType    {@link #TYPE_POLYGON} or {@link #TYPE_POLYLINE}
     * @param content      content buffer positioned after the shape-type int
     * @return a {@link ShapeRecord} whose {@link ShapeRecord#rings()} list
     *         contains one sub-list per part
     * @throws IOException if the part or point counts are out of range
     */
    private static ShapeRecord readRings(int recordNumber, int shapeType,
                                         ByteBuffer content) throws IOException {
        content.position(content.position() + 32); // skip bounding box

        int numParts  = content.getInt();
        int numPoints = content.getInt();

        if (numParts < 0 || numParts > 65536) {
            throw new IOException("Implausible part count " + numParts
                                  + " in record " + recordNumber);
        }
        if (numPoints < 0 || numPoints > 2_000_000) {
            throw new IOException("Implausible point count " + numPoints
                                  + " in record " + recordNumber);
        }

        // Read start indices for each part.
        int[] partStart = new int[numParts];
        for (int i = 0; i < numParts; i++) {
            partStart[i] = content.getInt();
        }

        // Read the flat point array.
        Point2D.Double[] allPoints = new Point2D.Double[numPoints];
        for (int i = 0; i < numPoints; i++) {
            allPoints[i] = new Point2D.Double(content.getDouble(), content.getDouble());
        }

        // Split the flat array into per-part ring lists.
        List<List<Point2D.Double>> rings = new ArrayList<>(numParts);
        for (int p = 0; p < numParts; p++) {
            int start = partStart[p];
            int end   = (p + 1 < numParts) ? partStart[p + 1] : numPoints;
            if (start < 0 || end > numPoints || start >= end) continue;

            List<Point2D.Double> ring = new ArrayList<>(end - start);
            for (int i = start; i < end; i++) {
                ring.add(allPoints[i]);
            }
            rings.add(Collections.unmodifiableList(ring));
        }

        return new ShapeRecord(recordNumber, shapeType,
                               Collections.emptyList(),
                               Collections.unmodifiableList(rings));
    }

    // -------------------------------------------------------------------------
    // Channel helper
    // -------------------------------------------------------------------------

    /**
     * Reads exactly {@code n} bytes from the current channel position.
     *
     * @param n bytes to read
     * @return a flipped {@link ByteBuffer} containing exactly {@code n} bytes
     * @throws IOException if fewer than {@code n} bytes are available
     */
    private ByteBuffer readBytes(int n) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(n);
        int total = 0;
        while (total < n) {
            int read = channel.read(buf);
            if (read < 0) throw new IOException(
                    "Unexpected end of file: expected " + n + " bytes, got " + total);
            total += read;
        }
        buf.flip();
        return buf;
    }

    // -------------------------------------------------------------------------
    // Record value object
    // -------------------------------------------------------------------------

    /**
     * Immutable container for the geometry data of a single shapefile record.
     *
     * @param recordNumber one-based record number as stored in the file
     * @param shapeType    shape type for this record; one of the
     *                     {@code TYPE_*} constants
     * @param points       flat list of points; non-empty for
     *                     {@link #TYPE_POINT} and {@link #TYPE_MULTIPOINT};
     *                     empty for ring-based types
     * @param rings        list of ring/part coordinate lists; non-empty for
     *                     {@link #TYPE_POLYGON} and {@link #TYPE_POLYLINE};
     *                     empty for point types
     */
    public record ShapeRecord(
            int recordNumber,
            int shapeType,
            List<Point2D.Double> points,
            List<List<Point2D.Double>> rings) {}
}