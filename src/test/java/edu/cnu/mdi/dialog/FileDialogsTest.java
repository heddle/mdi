package edu.cnu.mdi.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.cnu.mdi.util.Environment;

class FileDialogsTest {

	private Preferences testPreferences;
	private String originalDataDirectory;

	@BeforeEach
	void isolateState() {
		// A throwaway node per test, exactly like RecentFilesTest -- production
		// code keeps using the real per-package node, tests never touch it.
		testPreferences = Preferences.userRoot().node(
				"edu/cnu/mdi/tests/file-dialogs-" + System.nanoTime());
		FileDialogs.useLastDirectoriesForTesting(testPreferences);
		originalDataDirectory = Environment.getInstance().getDataDirectory();
	}

	@AfterEach
	void restoreState() throws Exception {
		testPreferences.removeNode();
		FileDialogs.useLastDirectoriesForTesting(Preferences.userNodeForPackage(FileDialogs.class));
		Environment.getInstance().setDataDirectory(originalDataDirectory);
	}

	@Test
	void rememberDirectoryMakesItTheNextLastDirectory() throws IOException {
		Path directory = Files.createTempDirectory("mdi-file-dialogs-");
		try {
			FileDialogs.rememberDirectory("test-purpose", directory);
			assertEquals(Optional.of(directory.toRealPath()),
					FileDialogs.lastDirectory("test-purpose").map(this::realPath));
		} finally {
			Files.deleteIfExists(directory);
		}
	}

	@Test
	void rememberDirectoryExtractsTheParentFromAFilePath() throws IOException {
		Path directory = Files.createTempDirectory("mdi-file-dialogs-");
		Path file = Files.createFile(directory.resolve("chosen.txt"));
		try {
			FileDialogs.rememberDirectory("test-purpose", file);
			assertEquals(Optional.of(directory.toRealPath()),
					FileDialogs.lastDirectory("test-purpose").map(this::realPath));
		} finally {
			Files.deleteIfExists(file);
			Files.deleteIfExists(directory);
		}
	}

	@Test
	void lastDirectoryIsEmptyWhenTheRememberedDirectoryNoLongerExists() throws IOException {
		// No Environment default set either, so a pruned purpose-history entry
		// falls all the way through to empty, not silently to some other
		// directory -- isolates the pruning behavior from the fallback chain
		// covered by lastDirectoryFallsBackToTheEnvironmentDefaultWhenThereIsNoHistory.
		Environment.getInstance().setDataDirectory("/no/such/directory/mdi-file-dialogs-test");
		Path directory = Files.createTempDirectory("mdi-file-dialogs-");
		FileDialogs.rememberDirectory("test-purpose", directory);
		Files.delete(directory);

		assertEquals(Optional.empty(), FileDialogs.lastDirectory("test-purpose"));
	}

	@Test
	void lastDirectoryFallsBackToTheEnvironmentDefaultWhenThereIsNoHistory() throws IOException {
		Path directory = Files.createTempDirectory("mdi-file-dialogs-");
		try {
			Environment.getInstance().setDataDirectory(directory.toString());
			assertEquals(Optional.of(directory.toRealPath()),
					FileDialogs.lastDirectory("never-used-purpose").map(this::realPath));
		} finally {
			Files.deleteIfExists(directory);
		}
	}

	@Test
	void lastDirectoryPrefersPurposeHistoryOverTheEnvironmentDefault() throws IOException {
		Path remembered = Files.createTempDirectory("mdi-file-dialogs-remembered-");
		Path fallback = Files.createTempDirectory("mdi-file-dialogs-fallback-");
		try {
			Environment.getInstance().setDataDirectory(fallback.toString());
			FileDialogs.rememberDirectory("test-purpose", remembered);

			assertEquals(Optional.of(remembered.toRealPath()),
					FileDialogs.lastDirectory("test-purpose").map(this::realPath));
		} finally {
			Files.deleteIfExists(remembered);
			Files.deleteIfExists(fallback);
		}
	}

	@Test
	void lastDirectoryIsEmptyWhenNeitherHistoryNorEnvironmentDefaultExist() {
		Environment.getInstance().setDataDirectory("/no/such/directory/mdi-file-dialogs-test");
		assertEquals(Optional.empty(), FileDialogs.lastDirectory("never-used-purpose"));
	}

	@Test
	void blankPurposeIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> FileDialogs.lastDirectory(" "));
	}

	@Test
	void openAndSaveRequireTheEventDispatchThread() {
		// Test execution is not on the EDT, so both must reject the call before
		// ever touching Swing -- this is what stops an accidental worker-thread
		// call from creating platform-dependent UI behavior.
		FileType type = FileType.of("Text", "txt");
		assertThrows(IllegalStateException.class,
				() -> FileDialogs.openFile(null, "test-purpose", "Open", type));
		assertThrows(IllegalStateException.class,
				() -> FileDialogs.saveFile(null, "test-purpose", "Save", null, type));
	}

	private Path realPath(Path path) {
		try {
			return path.toRealPath();
		} catch (IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
	}
}
