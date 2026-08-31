package edu.cnu.mdi.mapping.loader;

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

import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.mapping.shapefile.ShapefileGeometryReader;

class ShapefileCountryLoaderTest {

	@TempDir
	Path tempDir;

	@Test
	void loadsAPolygonWithRequiredAttributes() throws IOException {
		Path shpPath = tempDir.resolve("countries.shp");
		Path dbfPath = tempDir.resolve("countries.dbf");
		writeOneTrianglePolygonShapefile(shpPath);
		writeCountryDbf(dbfPath, "Testland", "TST");

		List<CountryFeature> features = new ShapefileCountryLoader().load(shpPath);

		assertEquals(1, features.size());
		assertEquals("Testland", features.get(0).getAdminName());
		assertEquals("TST", features.get(0).getIsoA3());
		assertEquals(1, features.get(0).getPolygons().size());
		assertEquals(3, features.get(0).getPolygons().get(0).size());
	}

	@Test
	void moreGeometryRecordsThanDbfRowsThrowsRatherThanSilentlyDroppingCountries() throws IOException {
		Path shpPath = tempDir.resolve("countries.shp");
		Path dbfPath = tempDir.resolve("countries.dbf");
		writeOneTrianglePolygonShapefile(shpPath);
		// Zero attribute rows: the geometry record has no corresponding .dbf row.
		writeCountryDbfHeaderOnly(dbfPath);

		IOException error = assertThrows(IOException.class,
				() -> new ShapefileCountryLoader().load(shpPath));
		assertTrue(error.getMessage().contains("record count mismatch"),
				"expected a record-count-mismatch message, got: " + error.getMessage());
	}

	// -------------------------------------------------------------------
	// .shp fixture: a single-part, 3-point polygon record.
	// -------------------------------------------------------------------

	private static void writeOneTrianglePolygonShapefile(Path path) throws IOException {
		int contentBytes = 4 + 32 + 4 + 4 + 4 + 3 * 16; // shapeType+bbox+numParts+numPoints+parts+points
		int recordBytes = 8 + contentBytes; // record header + content
		int fileBytes = 100 + recordBytes;

		ByteBuffer file = ByteBuffer.allocate(fileBytes);

		file.order(ByteOrder.BIG_ENDIAN);
		file.putInt(0, 9994);
		file.putInt(24, fileBytes / 2);

		file.order(ByteOrder.LITTLE_ENDIAN);
		file.putInt(28, 1000);
		file.putInt(32, ShapefileGeometryReader.TYPE_POLYGON);

		file.position(100);
		file.order(ByteOrder.BIG_ENDIAN);
		file.putInt(1); // record number
		file.putInt(contentBytes / 2); // content length in 16-bit words

		file.order(ByteOrder.LITTLE_ENDIAN);
		file.putInt(ShapefileGeometryReader.TYPE_POLYGON);
		file.position(file.position() + 32); // bounding box, unused by the reader
		file.putInt(1); // numParts
		file.putInt(3); // numPoints
		file.putInt(0); // partStart[0]
		file.putDouble(0.0);
		file.putDouble(0.0);
		file.putDouble(1.0);
		file.putDouble(0.0);
		file.putDouble(0.0);
		file.putDouble(1.0);

		Files.write(path, file.array());
	}

	// -------------------------------------------------------------------
	// .dbf fixtures with ADMIN/ISO_A3 columns.
	// -------------------------------------------------------------------

	private static void writeCountryDbf(Path path, String admin, String isoA3) throws IOException {
		writeCountryDbfRecords(path, 1, admin, isoA3);
	}

	private static void writeCountryDbfHeaderOnly(Path path) throws IOException {
		writeCountryDbfRecords(path, 0, null, null);
	}

	private static void writeCountryDbfRecords(Path path, int numRecords, String admin, String isoA3)
			throws IOException {
		final int adminLen = 20;
		final int isoLen = 3;
		final int headerSize = 32 + 32 * 2 + 1;
		final int recordSize = 1 + adminLen + isoLen;

		ByteBuffer file = ByteBuffer
				.allocate(headerSize + numRecords * recordSize)
				.order(ByteOrder.LITTLE_ENDIAN);

		file.put((byte) 0x03);
		file.position(4);
		file.putInt(numRecords);
		file.putShort((short) headerSize);
		file.putShort((short) recordSize);

		// Field descriptor 1: ADMIN, character, length 20
		file.position(32);
		file.put("ADMIN".getBytes(StandardCharsets.US_ASCII));
		file.position(32 + 11);
		file.put((byte) 'C');
		file.position(32 + 16);
		file.put((byte) adminLen);

		// Field descriptor 2: ISO_A3, character, length 3
		file.position(64);
		file.put("ISO_A3".getBytes(StandardCharsets.US_ASCII));
		file.position(64 + 11);
		file.put((byte) 'C');
		file.position(64 + 16);
		file.put((byte) isoLen);

		file.position(headerSize - 1);
		file.put((byte) 0x0D); // header terminator

		if (numRecords > 0) {
			file.position(headerSize);
			file.put((byte) 0x20); // not deleted
			file.put(padded(admin, adminLen));
			file.put(padded(isoA3, isoLen));
		}

		Files.write(path, file.array());
	}

	private static byte[] padded(String value, int length) {
		byte[] out = new byte[length];
		java.util.Arrays.fill(out, (byte) ' ');
		byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
		System.arraycopy(bytes, 0, out, 0, Math.min(bytes.length, length));
		return out;
	}
}
