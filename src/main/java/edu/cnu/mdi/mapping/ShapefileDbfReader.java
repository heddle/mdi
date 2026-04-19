package edu.cnu.mdi.mapping;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Zero-dependency reader for dBASE III ({@code .dbf}) attribute tables as
 * used in ESRI Shapefiles.
 *
 * <h2>File format overview</h2>
 * <p>A {@code .dbf} file consists of:
 * <ol>
 *   <li>A 32-byte file header containing the record count, header size, and
 *       record size.</li>
 *   <li>One 32-byte field descriptor per column, terminated by a {@code 0x0D}
 *       byte.</li>
 *   <li>A sequence of fixed-width data records, each preceded by a one-byte
 *       deletion flag ({@code 0x20} = valid, {@code 0x2A} = deleted).</li>
 * </ol>
 * All integers in the header are little-endian. Field values are stored as
 * fixed-width, space-padded ASCII (or the file's declared charset).</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * try (ShapefileDbfReader dbf = new ShapefileDbfReader(path)) {
 *     List<Map<String, String>> records = dbf.readAllRecords();
 *     for (Map<String, String> record : records) {
 *         String name = record.get("ADMIN");
 *         String iso  = record.get("ISO_A3");
 *     }
 * }
 * }</pre>
 *
 * <h2>Character encoding</h2>
 * <p>Natural Earth shapefiles use UTF-8 or ISO-8859-1. By default this reader
 * uses UTF-8 ({@link StandardCharsets#UTF_8}); callers can override this with
 * {@link #ShapefileDbfReader(Path, Charset)}. The dBASE language-driver byte
 * at offset 29 is read but not acted upon — explicit charset specification is
 * more reliable in practice.</p>
 *
 * <h2>Thread safety</h2>
 * <p>Instances are not thread-safe. Each calling thread should open its own
 * reader.</p>
 */
public final class ShapefileDbfReader implements Closeable {

    // -------------------------------------------------------------------------
    // dBASE III format constants
    // -------------------------------------------------------------------------

    /** Byte offset of the record count in the file header. */
    private static final int OFFSET_RECORD_COUNT  = 4;

    /** Byte offset of the header size (in bytes) in the file header. */
    private static final int OFFSET_HEADER_SIZE   = 8;

    /** Byte offset of the record size (in bytes) in the file header. */
    private static final int OFFSET_RECORD_SIZE   = 10;

    /** Size in bytes of each field descriptor sub-record. */
    private static final int FIELD_DESCRIPTOR_SIZE = 32;

    /** Deletion-flag value meaning the record is valid (a space character). */
    private static final byte FLAG_VALID   = 0x20;

    /** Deletion-flag value meaning the record is marked deleted (an asterisk). */
    private static final byte FLAG_DELETED = 0x2A;

    /** Header terminator byte that follows the last field descriptor. */
    private static final byte HEADER_TERMINATOR = 0x0D;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final FileChannel channel;
    private final Charset     charset;

    /** Number of data records in the file (from header). */
    private final int recordCount;

    /** Total size of the file header in bytes (from header). */
    private final int headerSize;

    /** Size of one data record in bytes, including the deletion-flag byte. */
    private final int recordSize;

    /**
     * Ordered list of field descriptors describing the column layout.
     * Order matches the column order in each data record.
     */
    private final List<FieldDescriptor> fields;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Opens the {@code .dbf} file at the given path using UTF-8 encoding for
     * field values.
     *
     * @param path path to the {@code .dbf} file; must not be {@code null}
     * @throws IOException if the file cannot be opened or the header is
     *                     malformed
     */
    public ShapefileDbfReader(Path path) throws IOException {
        this(path, StandardCharsets.UTF_8);
    }

    /**
     * Opens the {@code .dbf} file at the given path using the specified
     * character encoding for field values.
     *
     * @param path    path to the {@code .dbf} file; must not be {@code null}
     * @param charset character encoding to use when decoding field values;
     *                must not be {@code null}
     * @throws IOException if the file cannot be opened or the header is
     *                     malformed
     */
    public ShapefileDbfReader(Path path, Charset charset) throws IOException {
        this.charset = charset;
        this.channel = FileChannel.open(path, StandardOpenOption.READ);
        try {
            // Read and parse the 32-byte file header.
            ByteBuffer header = readBytes(32);
            header.order(ByteOrder.LITTLE_ENDIAN);

            recordCount = header.getInt(OFFSET_RECORD_COUNT);
            headerSize  = header.getShort(OFFSET_HEADER_SIZE) & 0xFFFF;
            recordSize  = header.getShort(OFFSET_RECORD_SIZE) & 0xFFFF;

            if (recordCount < 0 || headerSize < 32 || recordSize < 1) {
                throw new IOException("DBF header values are out of range: "
                        + "recordCount=" + recordCount
                        + ", headerSize=" + headerSize
                        + ", recordSize=" + recordSize);
            }

            fields = parseFieldDescriptors();
        } catch (IOException e) {
            channel.close();
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the number of data records declared in the file header.
     *
     * <p>This includes records marked as deleted. The count returned by
     * {@link #readAllRecords()} may be smaller if deleted records are
     * skipped.</p>
     *
     * @return declared record count (&ge; 0)
     */
    public int getRecordCount() { return recordCount; }

    /**
     * Returns an unmodifiable view of the field descriptors in column order.
     *
     * @return list of {@link FieldDescriptor} instances
     */
    public List<FieldDescriptor> getFields() { return Collections.unmodifiableList(fields); }

    /**
     * Returns the names of all fields in column order.
     *
     * @return ordered list of field name strings
     */
    public List<String> getFieldNames() {
        List<String> names = new ArrayList<>(fields.size());
        for (FieldDescriptor f : fields) names.add(f.name());
        return names;
    }

    /**
     * Reads and returns all non-deleted records as an ordered list of
     * {@code String}-valued maps.
     *
     * <p>Each map key is the field name (trimmed, upper-cased as stored in the
     * dBASE header). Each map value is the decoded, whitespace-trimmed field
     * value, or an empty string if the field contained only spaces.</p>
     *
     * <p>This is a sequential full-scan; the channel is rewound to the first
     * data record before reading begins. Deleted records (deletion flag
     * {@code 0x2A}) are silently skipped.</p>
     *
     * @return unmodifiable list of records, each a field-name → value map
     * @throws IOException if a read error occurs
     */
    public List<Map<String, String>> readAllRecords() throws IOException {
        // Seek to first data record (immediately after the header).
        channel.position(headerSize);

        ByteBuffer recordBuf = ByteBuffer.allocate(recordSize);
        List<Map<String, String>> result = new ArrayList<>(recordCount);

        for (int i = 0; i < recordCount; i++) {
            recordBuf.clear();
            int bytesRead = channel.read(recordBuf);
            if (bytesRead < recordSize) break; // truncated file

            recordBuf.flip();
            byte deletionFlag = recordBuf.get(); // consume the deletion-flag byte

            if (deletionFlag == FLAG_DELETED) continue; // skip deleted records

            Map<String, String> record = new LinkedHashMap<>(fields.size() * 2);
            for (FieldDescriptor field : fields) {
                byte[] raw = new byte[field.length()];
                recordBuf.get(raw);
                String value = new String(raw, charset).trim();
                record.put(field.name(), value);
            }
            result.add(Collections.unmodifiableMap(record));
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Reads a single record by its zero-based index.
     *
     * <p>This performs a seek to the record's position rather than a
     * sequential scan, making it efficient for random access. Returns
     * {@code null} if the record is marked deleted.</p>
     *
     * @param index zero-based record index; must be in [0, recordCount)
     * @return field-name → value map, or {@code null} if the record is deleted
     * @throws IOException              if a read error occurs
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public Map<String, String> readRecord(int index) throws IOException {
        if (index < 0 || index >= recordCount) {
            throw new IndexOutOfBoundsException(
                    "Record index " + index + " out of range [0, " + recordCount + ")");
        }

        long position = (long) headerSize + (long) index * recordSize;
        channel.position(position);

        ByteBuffer buf = readBytes(recordSize);
        byte deletionFlag = buf.get();
        if (deletionFlag == FLAG_DELETED) return null;

        Map<String, String> record = new LinkedHashMap<>(fields.size() * 2);
        for (FieldDescriptor field : fields) {
            byte[] raw = new byte[field.length()];
            buf.get(raw);
            record.put(field.name(), new String(raw, charset).trim());
        }
        return Collections.unmodifiableMap(record);
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        channel.close();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Reads exactly {@code n} bytes from the current channel position into a
     * newly allocated, flipped {@link ByteBuffer}.
     *
     * @param n number of bytes to read
     * @return a flipped buffer containing exactly {@code n} bytes
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

    /**
     * Parses the field descriptor sub-records from the current channel
     * position (immediately after the 32-byte file header) until a
     * {@link #HEADER_TERMINATOR} byte is encountered.
     *
     * <p>Each field descriptor is exactly {@link #FIELD_DESCRIPTOR_SIZE} (32)
     * bytes long:
     * <ul>
     *   <li>Bytes 0–10:  field name, null-terminated, ASCII</li>
     *   <li>Byte 11:     field type character (C, N, L, D, M…)</li>
     *   <li>Bytes 12–15: reserved / field data address</li>
     *   <li>Byte 16:     field length in bytes</li>
     *   <li>Byte 17:     decimal count (for numeric fields)</li>
     *   <li>Bytes 18–31: reserved</li>
     * </ul>
     *
     * @return ordered list of field descriptors
     * @throws IOException if a read error occurs or the descriptor count
     *                     is implausibly large
     */
    private List<FieldDescriptor> parseFieldDescriptors() throws IOException {
        // The number of field descriptors can be inferred from the header size:
        // headerSize = 32 (file header) + N * 32 (field descriptors) + 1 (terminator)
        // => N = (headerSize - 33) / 32
        int expectedCount = (headerSize - 33) / FIELD_DESCRIPTOR_SIZE;
        if (expectedCount < 0 || expectedCount > 4096) {
            throw new IOException("Implausible DBF field count: " + expectedCount);
        }

        List<FieldDescriptor> result = new ArrayList<>(expectedCount);
        ByteBuffer buf = ByteBuffer.allocate(FIELD_DESCRIPTOR_SIZE);

        while (true) {
            // Peek at the first byte to detect the header terminator.
            ByteBuffer peek = readBytes(1);
            byte first = peek.get();
            if (first == HEADER_TERMINATOR) break;

            // Not a terminator — read the remaining 31 bytes of this descriptor.
            buf.clear();
            buf.put(first);
            ByteBuffer rest = readBytes(FIELD_DESCRIPTOR_SIZE - 1);
            buf.put(rest);
            buf.flip();

            // Field name: bytes 0–10, null-terminated ASCII.
            byte[] nameBytes = new byte[11];
            buf.get(nameBytes);
            int nameLen = 0;
            while (nameLen < nameBytes.length && nameBytes[nameLen] != 0) nameLen++;
            String name = new String(nameBytes, 0, nameLen, StandardCharsets.US_ASCII).trim();

            char type   = (char) (buf.get() & 0xFF); // byte 11
            buf.getInt();                              // bytes 12–15 (reserved)
            int  length = buf.get() & 0xFF;            // byte 16
            int  decimals = buf.get() & 0xFF;          // byte 17
            // bytes 18–31 are reserved padding — already consumed by next descriptor read

            if (!name.isEmpty() && length > 0) {
                result.add(new FieldDescriptor(name, type, length, decimals));
            }
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Field descriptor record
    // -------------------------------------------------------------------------

    /**
     * Immutable description of a single {@code .dbf} field (column).
     *
     * @param name     field name as stored in the header (trimmed, may be
     *                 upper-case)
     * @param type     dBASE field type character: {@code 'C'} = character,
     *                 {@code 'N'} = numeric, {@code 'L'} = logical,
     *                 {@code 'D'} = date, {@code 'M'} = memo
     * @param length   field width in bytes (for character fields this is the
     *                 maximum string length)
     * @param decimals number of decimal places (for numeric fields; 0 for
     *                 character fields)
     */
    public record FieldDescriptor(String name, char type, int length, int decimals) {}
}