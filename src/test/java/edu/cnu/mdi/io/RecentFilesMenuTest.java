package edu.cnu.mdi.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.junit.jupiter.api.Test;

class RecentFilesMenuTest {

	@Test
	void rebuildsNumberedMenuAndInvokesOpener() throws Exception {
		Preferences preferences = Preferences.userRoot().node(
				"edu/cnu/mdi/tests/recent-menu-" + System.nanoTime());
		Path file = Files.createTempFile("mdi-recent-menu-", ".dat");
		try {
			RecentFiles recent = new RecentFiles(preferences, 5);
			recent.add(file.toFile());
			AtomicReference<java.io.File> opened = new AtomicReference<>();
			JMenu menu = new JMenu("Recent Files");
			new RecentFilesMenu(recent, opened::set).rebuild(menu);
			JMenuItem item = menu.getItem(0);
			assertEquals("1  " + file.getFileName(), item.getText());
			item.doClick();
			assertEquals(file.toFile().getCanonicalFile(), opened.get());
			assertTrue(menu.getItem(2).isEnabled());
		} finally {
			preferences.removeNode();
			Files.deleteIfExists(file);
		}
	}
}
