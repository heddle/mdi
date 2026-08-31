package edu.cnu.mdi.mapping.shapefile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.cnu.mdi.mapping.MapView2D;

/**
 * Tests the "Recent Shapefiles" submenu: entries populated by successful
 * loads (via either the menu or, transitively, {@link MapView2D#filesDropped}),
 * pruned on failure, and re-opening a recent entry re-loads it as a layer.
 * <p>
 * Uses a throwaway {@link Preferences} node (via the package-private
 * constructor) rather than {@link ShapefileMenu}'s real production one, so
 * running this test doesn't write into the developer's actual preference
 * store — see {@code edu.cnu.mdi.io.RecentFilesTest} for the same pattern.
 */
class ShapefileMenuTest {

	private final Preferences testPreferences = Preferences.userRoot()
			.node("edu/cnu/mdi/tests/shapefile-menu-" + System.nanoTime());

	@AfterEach
	void cleanUpTestPreferencesNode() throws Exception {
		testPreferences.removeNode();
	}

	private ShapefileMenu newMenu() {
		return new ShapefileMenu(new MapView2D(), testPreferences);
	}

	private JMenu recentSubmenu(ShapefileMenu menu) {
		// item 0 is "Open Shapefile...", item 1 is the "Recent Shapefiles" submenu
		return (JMenu) menu.getItem(1);
	}

	@Test
	void startsWithAnEmptyRecentShapefilesSubmenu() {
		JMenu recent = recentSubmenu(newMenu());

		assertEquals("Recent Shapefiles", recent.getText());
		JMenuItem placeholder = recent.getItem(0);
		assertEquals("(none)", placeholder.getText());
		assertFalse(placeholder.isEnabled());
	}

	@Test
	void recordRecentlyOpenedAddsAnEntryToTheSubmenu(@TempDir Path tempDir) throws IOException {
		Path shpPath = tempDir.resolve("countries.shp");
		Files.writeString(shpPath, "placeholder"); // RecentFiles.add() only requires it to exist

		ShapefileMenu menu = newMenu();
		menu.recordRecentlyOpened(shpPath.toFile());

		JMenu recent = recentSubmenu(menu);
		assertEquals("1  countries.shp", recent.getItem(0).getText());
	}

	@Test
	void recordRecentlyOpenedWithNullIsANoOp() {
		ShapefileMenu menu = newMenu();
		menu.recordRecentlyOpened(null); // must not throw

		JMenu recent = recentSubmenu(menu);
		assertFalse(recent.getItem(0).isEnabled(), "still just the disabled (none) placeholder");
	}

	@Test
	void openingAValidShapefileAddsALayerAndARecentEntry(@TempDir Path tempDir) throws IOException {
		Path shpPath = tempDir.resolve("features.shp");
		Path dbfPath = tempDir.resolve("features.dbf");
		writeShapefileWithOnePoint(shpPath);
		writeDbf(dbfPath);

		MapView2D mapView = new MapView2D();
		ShapefileMenu menu = new ShapefileMenu(mapView, testPreferences);
		assertEquals(0, mapView.getLayers().size());

		menu.openShapefile(shpPath.toFile());

		assertEquals(1, mapView.getLayers().size());
		assertEquals("features", mapView.getLayers().get(0).getName());
		assertEquals("1  features.shp", recentSubmenu(menu).getItem(0).getText());
	}

	@Test
	void clickingARecentEntryLoadsItAgainAsANewLayer(@TempDir Path tempDir) throws IOException {
		Path shpPath = tempDir.resolve("features.shp");
		Path dbfPath = tempDir.resolve("features.dbf");
		writeShapefileWithOnePoint(shpPath);
		writeDbf(dbfPath);

		MapView2D mapView = new MapView2D();
		ShapefileMenu menu = new ShapefileMenu(mapView, testPreferences);
		menu.openShapefile(shpPath.toFile());
		assertEquals(1, mapView.getLayers().size());

		// Fire the actual action listener RecentFilesMenu.rebuild() attached to
		// the entry, rather than re-calling openShapefile() directly, so this
		// exercises the real click-to-reopen wiring end to end.
		JMenuItem recentItem = recentSubmenu(menu).getItem(0);
		assertEquals(1, recentItem.getActionListeners().length);
		recentItem.getActionListeners()[0]
				.actionPerformed(new java.awt.event.ActionEvent(recentItem, java.awt.event.ActionEvent.ACTION_PERFORMED, "click"));

		assertEquals(2, mapView.getLayers().size(), "re-opening should add a second layer, not replace the first");
	}

	@Test
	void openingACorruptFileDoesNotAddARecentEntry(@TempDir Path tempDir) throws IOException {
		Path badPath = tempDir.resolve("corrupt.shp");
		Files.write(badPath, new byte[] { 1, 2, 3 });

		ShapefileMenu menu = newMenu();
		menu.openShapefile(badPath.toFile());

		JMenu recent = recentSubmenu(menu);
		assertFalse(recent.getItem(0).isEnabled(), "a failed load should not be recorded as recent");
	}

	@Test
	void openingAnEmptyShapefileDoesNotAddARecentEntry(@TempDir Path tempDir) throws IOException {
		Path emptyPath = tempDir.resolve("empty.shp");
		writeEmptyShapefile(emptyPath);

		ShapefileMenu menu = newMenu();
		menu.openShapefile(emptyPath.toFile());

		JMenu recent = recentSubmenu(menu);
		assertFalse(recent.getItem(0).isEnabled(), "a shapefile with zero features should not be recorded as recent");
	}

	// -------------------------------------------------------------------------
	// Synthetic shapefile writers, matching the format ShapefileFeatureLoader
	// expects (adapted from ShapefileFeatureLoaderTest's minimal fixtures).
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

	private static void writeEmptyShapefile(Path path) throws IOException {
		int fileBytes = 100;
		ByteBuffer file = ByteBuffer.allocate(fileBytes);

		file.order(ByteOrder.BIG_ENDIAN);
		file.putInt(0, 9994);
		file.putInt(24, fileBytes / 2);

		file.order(ByteOrder.LITTLE_ENDIAN);
		file.putInt(28, 1000);
		file.putInt(32, ShapefileGeometryReader.TYPE_POINT);

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
