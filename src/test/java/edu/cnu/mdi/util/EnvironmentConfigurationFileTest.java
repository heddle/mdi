package edu.cnu.mdi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Where {@link Environment#getConfigurationFile()} puts the layout file. */
class EnvironmentConfigurationFileTest {

	@TempDir
	Path tempDir;

	@BeforeEach
	void ensureAnApplicationName() {
		// A no-op if a name was already set; the tests only need one to exist.
		Environment.setApplicationName("MDI Environment Test");
	}

	@AfterEach
	void restoreDefaultDirectory() {
		Environment.getInstance().setConfigurationDirectory(null);
	}

	@Test
	void defaultsToTheUsersHomeDirectory() {
		File file = Environment.getInstance().getConfigurationFile();

		assertNotNull(file);
		assertEquals(new File(Environment.getInstance().getHomeDirectory()), file.getParentFile());
		assertTrue(file.getName().endsWith(".xml"));
	}

	@Test
	void aConfiguredDirectoryReplacesTheHomeDirectoryAndKeepsTheFileName() {
		String defaultName = Environment.getInstance().getConfigurationFile().getName();

		Environment.getInstance().setConfigurationDirectory(tempDir.toFile());

		File file = Environment.getInstance().getConfigurationFile();
		assertEquals(tempDir.toFile(), file.getParentFile());
		assertEquals(defaultName, file.getName());
	}

	@Test
	void clearingTheConfiguredDirectoryRestoresTheHomeDirectory() {
		File home = Environment.getInstance().getConfigurationFile();
		Environment.getInstance().setConfigurationDirectory(tempDir.toFile());

		Environment.getInstance().setConfigurationDirectory(null);

		assertEquals(home, Environment.getInstance().getConfigurationFile());
	}
}
