package edu.cnu.mdi.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

class RecentFilesTest {

	@Test
	void maintainsMruOrderLimitAndPrunesMissingFiles() throws Exception {
		Preferences preferences = Preferences.userRoot().node(
				"edu/cnu/mdi/tests/recent-files-" + System.nanoTime());
		Path directory = Files.createTempDirectory("mdi-recent-files-");
		try {
			Path first = Files.createFile(directory.resolve("first.dat"));
			Path second = Files.createFile(directory.resolve("second.dat"));
			Path third = Files.createFile(directory.resolve("third.dat"));
			RecentFiles recent = new RecentFiles(preferences, 2);
			recent.add(first.toFile());
			recent.add(second.toFile());
			recent.add(first.toFile());
			assertEquals(List.of(first.toFile().getCanonicalFile(),
					second.toFile().getCanonicalFile()), recent.getRecentFiles());

			recent.add(third.toFile());
			assertEquals(List.of(third.toFile().getCanonicalFile(),
					first.toFile().getCanonicalFile()), recent.getRecentFiles());
			Files.delete(third);
			assertEquals(List.of(first.toFile().getCanonicalFile()), recent.getRecentFiles());
			recent.clear();
			assertTrue(recent.getRecentFiles().isEmpty());
		} finally {
			preferences.removeNode();
			Files.walk(directory).sorted((a, b) -> b.compareTo(a)).forEach(path -> {
				try { Files.deleteIfExists(path); } catch (Exception ignored) { }
			});
		}
	}
}
