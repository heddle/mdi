package edu.cnu.mdi.mapping.shapefile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ShapefileFeatureLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    public void testNullShapeDoesNotShiftFollowingAttributes() throws IOException {
        Path shpPath = tempDir.resolve("features.shp");
        Path dbfPath = tempDir.resolve("features.dbf");
        writeShapefileWithNullThenPoint(shpPath);
        writeDbf(dbfPath);

        List<ShapeFeature> features = new ShapefileFeatureLoader().load(shpPath);

        assertEquals(1, features.size());
        assertEquals("SECOND", features.get(0).getProperties().get("NAME"));
        assertEquals(Math.toRadians(12.0), features.get(0).getPoints().get(0).x);
        assertEquals(Math.toRadians(34.0), features.get(0).getPoints().get(0).y);
    }

    @Test
    public void testRecordContentCannotExtendPastDeclaredFileLength() throws IOException {
        Path path = tempDir.resolve("truncated.shp");
        writeShapefileWithNullThenPoint(path);

        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).putInt(116, 11);
        Files.write(path, bytes);

        try (ShapefileGeometryReader reader = new ShapefileGeometryReader(path)) {
            assertEquals(ShapefileGeometryReader.TYPE_NULL, reader.nextRecord().shapeType());
            IOException error = assertThrows(IOException.class, reader::nextRecord);
            assertTrue(error.getMessage().contains("only 20 remain"));
        }
    }

    @Test
    public void testProjectedCrsIsRejectedBeforeCoordinatesAreLoaded() throws IOException {
        Path shpPath = tempDir.resolve("features.shp");
        Path dbfPath = tempDir.resolve("features.dbf");
        writeShapefileWithNullThenPoint(shpPath);
        writeDbf(dbfPath);
        Files.writeString(tempDir.resolve("features.prj"),
                "PROJCS[\"WGS_1984_Web_Mercator\","
              + "GEOGCS[\"GCS_WGS_1984\"],UNIT[\"Meter\",1.0]]");

        IOException error = assertThrows(IOException.class,
                () -> new ShapefileFeatureLoader().load(shpPath));
        assertTrue(error.getMessage().contains("not a geographic CRS"));
    }

    private static void writeShapefileWithNullThenPoint(Path path) throws IOException {
        int fileBytes = 100 + 12 + 28;
        ByteBuffer file = ByteBuffer.allocate(fileBytes);

        file.order(ByteOrder.BIG_ENDIAN);
        file.putInt(0, 9994);
        file.putInt(24, fileBytes / 2);

        file.order(ByteOrder.LITTLE_ENDIAN);
        file.putInt(28, 1000);
        file.putInt(32, ShapefileGeometryReader.TYPE_POINT);

        file.position(100);
        file.order(ByteOrder.BIG_ENDIAN);
        file.putInt(1);
        file.putInt(2);
        file.order(ByteOrder.LITTLE_ENDIAN);
        file.putInt(ShapefileGeometryReader.TYPE_NULL);

        file.order(ByteOrder.BIG_ENDIAN);
        file.putInt(2);
        file.putInt(10);
        file.order(ByteOrder.LITTLE_ENDIAN);
        file.putInt(ShapefileGeometryReader.TYPE_POINT);
        file.putDouble(12.0);
        file.putDouble(34.0);

        Files.write(path, file.array());
    }

    private static void writeDbf(Path path) throws IOException {
        int headerSize = 65;
        int recordSize = 7;
        ByteBuffer file = ByteBuffer.allocate(headerSize + 2 * recordSize)
                .order(ByteOrder.LITTLE_ENDIAN);

        file.put((byte) 0x03);
        file.position(4);
        file.putInt(2);
        file.putShort((short) headerSize);
        file.putShort((short) recordSize);

        file.position(32);
        file.put("NAME".getBytes(StandardCharsets.US_ASCII));
        file.position(43);
        file.put((byte) 'C');
        file.position(48);
        file.put((byte) 6);
        file.position(64);
        file.put((byte) 0x0D);

        file.put((byte) 0x20);
        file.put("FIRST ".getBytes(StandardCharsets.US_ASCII));
        file.put((byte) 0x20);
        file.put("SECOND".getBytes(StandardCharsets.US_ASCII));

        Files.write(path, file.array());
    }
}
