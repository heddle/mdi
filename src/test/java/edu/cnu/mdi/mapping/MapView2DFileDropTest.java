package edu.cnu.mdi.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.cnu.mdi.mapping.layer.ShapefileLayer;
import edu.cnu.mdi.mapping.shapefile.ShapefileGeometryReader;

/**
 * Tests that dropping a {@code .shp} file on a {@link MapView2D} (and therefore on
 * any subclass, e.g. {@code RadarView}) actually loads it as a new layer — the gap
 * this class covers, since {@link #enableFileDrop} was previously never called at all.
 */
class MapView2DFileDropTest {

	@TempDir
	Path tempDir;

	@Test
	void fileDropFilterAcceptsShpAndRejectsOtherExtensions() {
		MapView2D mapView = new MapView2D();
		Predicate<File> filter = mapView.getFileFilter();

		assertNotNull(filter, "MapView2D should have called enableFileDrop() in its constructor");
		assertTrue(filter.test(new File("countries.shp")));
		assertTrue(filter.test(new File("COUNTRIES.SHP")), "extension match should be case-insensitive");
		assertFalse(filter.test(new File("countries.dbf")));
		assertFalse(filter.test(new File("readme.txt")));
	}

	@Test
	void filesDroppedLoadsAShapefileAsANewLayerNamedForTheFile() throws IOException {
		Path shpPath = tempDir.resolve("features.shp");
		Path dbfPath = tempDir.resolve("features.dbf");
		writeShapefileWithOnePoint(shpPath);
		writeDbf(dbfPath);

		MapView2D mapView = new MapView2D();
		assertEquals(0, mapView.getLayers().size());

		mapView.filesDropped(List.of(shpPath.toFile()));

		List<ShapefileLayer> layers = mapView.getLayers();
		assertEquals(1, layers.size());
		assertEquals("features", layers.get(0).getName());
	}

	@Test
	void filesDroppedSkipsAFailedFileButStillLoadsTheRest() throws IOException {
		Path badPath = tempDir.resolve("corrupt.shp");
		Files.write(badPath, new byte[] { 1, 2, 3 }); // not a valid shapefile header

		Path goodPath = tempDir.resolve("good.shp");
		Path goodDbfPath = tempDir.resolve("good.dbf");
		writeShapefileWithOnePoint(goodPath);
		writeDbf(goodDbfPath);

		MapView2D mapView = new MapView2D();
		mapView.filesDropped(List.of(badPath.toFile(), goodPath.toFile()));

		List<ShapefileLayer> layers = mapView.getLayers();
		assertEquals(1, layers.size(), "the corrupt file should be skipped, not abort the whole drop");
		assertEquals("good", layers.get(0).getName());
	}

	// -------------------------------------------------------------------------
	// Synthetic shapefile/dbf writers, adapted from ShapefileFeatureLoaderTest's
	// minimal single-point fixture.
	// -------------------------------------------------------------------------

	private static void writeShapefileWithOnePoint(Path path) throws IOException {
		int fileBytes = 100 + 28;
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
		ByteBuffer file = ByteBuffer.allocate(headerSize + recordSize)
				.order(ByteOrder.LITTLE_ENDIAN);

		file.put((byte) 0x03);
		file.position(4);
		file.putInt(1);
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
		file.put("POINT1".getBytes(StandardCharsets.US_ASCII));

		Files.write(path, file.array());
	}
}
