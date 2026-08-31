package edu.cnu.mdi.mapping.shapefile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ShapefileDbfReaderTest {

    private static final byte FLAG_VALID = 0x20;
    private static final byte FLAG_DELETED = 0x2A;

    @TempDir
    Path tempDir;

    @Test
    public void testDeletedRecordsPreserveAttributeAlignment() throws IOException {
        Path path = writeDbf(
                new byte[] { FLAG_VALID, FLAG_DELETED, FLAG_VALID },
                new String[] { "ONE", "TWO", "THREE" });

        try (ShapefileDbfReader reader = new ShapefileDbfReader(path)) {
            List<Map<String, String>> records = reader.readAllRecords();

            assertEquals(3, records.size());
            assertEquals("ONE", records.get(0).get("NAME"));
            assertTrue(records.get(1).isEmpty());
            assertEquals("THRE", records.get(2).get("NAME"));
            assertNull(reader.readRecord(1));
        }
    }

    @Test
    public void testInvalidRecordFlagIsRejected() throws IOException {
        Path path = writeDbf(new byte[] { 0x00 }, new String[] { "BAD" });

        try (ShapefileDbfReader reader = new ShapefileDbfReader(path)) {
            IOException error = assertThrows(IOException.class, reader::readAllRecords);
            assertTrue(error.getMessage().contains("index 0"));
            assertTrue(error.getMessage().contains("0x00"));
        }

        try (ShapefileDbfReader reader = new ShapefileDbfReader(path)) {
            assertThrows(IOException.class, () -> reader.readRecord(0));
        }
    }

    @Test
    public void testTruncatedRecordIsRejected() throws IOException {
        Path path = writeDbf(
                new byte[] { FLAG_VALID, FLAG_VALID },
                new String[] { "ONE", "TWO" });
        byte[] completeFile = Files.readAllBytes(path);
        Files.write(path, java.util.Arrays.copyOf(completeFile, completeFile.length - 1));

        try (ShapefileDbfReader reader = new ShapefileDbfReader(path)) {
            IOException error = assertThrows(IOException.class, reader::readAllRecords);
            assertTrue(error.getMessage().contains("Unexpected end of file"));
        }
    }

    private Path writeDbf(byte[] flags, String[] values) throws IOException {
        int headerSize = 65;
        int recordSize = 5;
        ByteBuffer file = ByteBuffer.allocate(headerSize + flags.length * recordSize)
                .order(ByteOrder.LITTLE_ENDIAN);

        file.put((byte) 0x03);
        file.position(4);
        file.putInt(flags.length);
        file.putShort((short) headerSize);
        file.putShort((short) recordSize);

        file.position(32);
        file.put("NAME".getBytes(StandardCharsets.US_ASCII));
        file.position(43);
        file.put((byte) 'C');
        file.position(48);
        file.put((byte) 4);
        file.position(64);
        file.put((byte) 0x0D);

        for (int i = 0; i < flags.length; i++) {
            file.put(flags[i]);
            byte[] value = String.format("%-4.4s", values[i])
                    .getBytes(StandardCharsets.US_ASCII);
            file.put(value);
        }

        Path path = tempDir.resolve("test.dbf");
        Files.write(path, file.array());
        return path;
    }
}
