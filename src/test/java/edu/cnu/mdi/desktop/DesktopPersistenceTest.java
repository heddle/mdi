package edu.cnu.mdi.desktop;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.view.BaseView;
import edu.cnu.mdi.view.ViewPropertiesBuilder;

/**
 * Saving and loading the desktop's layout file.
 *
 * <p>The configuration directory is redirected to a temporary directory for
 * every test, so nothing here reads or writes the real
 * {@code ~/.<application>.xml}. {@link Desktop} and {@link Environment} are
 * process-wide singletons, so each test cleans up the views it created and
 * clears the loaded layout state it leaves behind. Assertions only look at the
 * keys of this test's own views, because other tests' views may also be on the
 * shared desktop.</p>
 *
 * <p>The interactive save and delete-with-confirmation paths show modal
 * dialogs and are not exercised here.</p>
 */
class DesktopPersistenceTest {

	@TempDir
	Path tempDir;

	private Desktop desktop;
	private final List<BaseView> views = new ArrayList<>();

	@BeforeEach
	void redirectConfigurationFile() {
		desktop = Desktop.createDesktop(null, null);
		// A no-op if an earlier test (or application) already set a name; the
		// file location is controlled by the directory either way.
		Environment.setApplicationName("MDI Desktop Test");
		Environment.getInstance().setConfigurationDirectory(tempDir.toFile());
	}

	@AfterEach
	void restoreEnvironmentAndDesktop() {
		for (BaseView view : views) {
			onEdt(() -> {
				desktop.remove(view);
				return null;
			});
		}
		views.clear();

		// Forget any layout this test loaded, while the directory is still
		// redirected (a load with no file clears the saved state).
		File file = Environment.getInstance().getConfigurationFile();
		if (file != null) {
			file.delete();
		}
		desktop.loadConfigurationFile();
		Environment.getInstance().setConfigurationDirectory(null);
	}

	// ----------------------------------------------------------------- helpers

	private static <T> T onEdt(Supplier<T> body) {
		Object[] result = new Object[1];
		try {
			SwingUtilities.invokeAndWait(() -> result[0] = body.get());
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException re) throw re;
			if (cause instanceof Error err) throw err;
			throw new AssertionError(cause);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while running Swing test", e);
		}
		@SuppressWarnings("unchecked")
		T value = (T) result[0];
		return value;
	}

	/** A container-less view on the shared desktop with the given bounds. */
	private BaseView newView(String title, Rectangle bounds) {
		BaseView view = onEdt(() -> {
			BaseView v = new BaseView(new ViewPropertiesBuilder()
					.title(title)
					.visible(false)
					.useContainer(false)
					.width(300)
					.height(200)
					.build());
			v.setBounds(bounds);
			return v;
		});
		views.add(view);
		return view;
	}

	private static Properties readXml(File file) throws IOException {
		Properties props = new Properties();
		try (FileInputStream in = new FileInputStream(file)) {
			props.loadFromXML(in);
		}
		return props;
	}

	private static File configFile() {
		return Environment.getInstance().getConfigurationFile();
	}

	// -------------------------------------------------------------------- load

	@Test
	void loadingWithNoFileLeavesNoSavedProperties() {
		assertFalse(configFile().exists());

		desktop.loadConfigurationFile();

		assertNull(desktop.getSavedProperties());
	}

	@Test
	void loadingAnUnreadableFileIsIgnoredAndClearsEarlierState() throws IOException {
		newView("Corrupt Load View", new Rectangle(10, 10, 300, 200));
		desktop.writeConfigurationFile();
		desktop.loadConfigurationFile();
		assertNotNull(desktop.getSavedProperties(), "precondition: a valid file loads");

		Files.writeString(configFile().toPath(), "this is not xml");

		assertDoesNotThrow(desktop::loadConfigurationFile);
		assertNull(desktop.getSavedProperties());
	}

	@Test
	void loadingAfterTheFileHasGoneClearsEarlierState() {
		newView("Vanishing File View", new Rectangle(10, 10, 300, 200));
		desktop.writeConfigurationFile();
		desktop.loadConfigurationFile();
		assertNotNull(desktop.getSavedProperties(), "precondition: a valid file loads");

		assertTrue(configFile().delete());
		desktop.loadConfigurationFile();

		assertNull(desktop.getSavedProperties());
	}

	@Test
	void loadingAnEmptyLayoutFileIsIgnored() throws IOException {
		try (FileOutputStream out = new FileOutputStream(configFile())) {
			new Properties().storeToXML(out, null);
		}

		desktop.loadConfigurationFile();

		assertNull(desktop.getSavedProperties());
	}

	// -------------------------------------------------------------------- save

	@Test
	void savingWritesTheFileIntoTheConfiguredDirectoryUnderTheApplicationName() {
		File file = configFile();
		assertEquals(tempDir.toFile(), file.getParentFile());
		assertTrue(file.getName().endsWith(".xml"));
		assertFalse(file.exists());

		desktop.writeConfigurationFile();

		assertTrue(file.exists());
	}

	@Test
	void savedFileHoldsEachViewsStateUnderItsOwnKeyPrefix() throws IOException {
		newView("Save One", new Rectangle(11, 22, 333, 244));
		newView("Save Two", new Rectangle(44, 55, 266, 177));

		desktop.writeConfigurationFile();

		Properties saved = readXml(configFile());
		assertEquals("11", saved.getProperty("Save_One.x"));
		assertEquals("22", saved.getProperty("Save_One.y"));
		assertEquals("333", saved.getProperty("Save_One.w"));
		assertEquals("244", saved.getProperty("Save_One.h"));
		assertEquals("44", saved.getProperty("Save_Two.x"));
		assertEquals("55", saved.getProperty("Save_Two.y"));
		assertEquals("266", saved.getProperty("Save_Two.w"));
		assertEquals("177", saved.getProperty("Save_Two.h"));
	}

	@Test
	void aLaterSaveOverwritesTheEarlierOne() throws IOException {
		BaseView view = newView("Overwrite View", new Rectangle(1, 2, 300, 200));
		desktop.writeConfigurationFile();

		onEdt(() -> {
			view.setBounds(7, 8, 310, 210);
			return null;
		});
		desktop.writeConfigurationFile();

		Properties saved = readXml(configFile());
		assertEquals("7", saved.getProperty("Overwrite_View.x"));
		assertEquals("310", saved.getProperty("Overwrite_View.w"));
	}

	@Test
	void savingToAMissingDirectoryFailsQuietlyWithoutCreatingAFile() {
		File missing = new File(tempDir.toFile(), "does-not-exist");
		Environment.getInstance().setConfigurationDirectory(missing);

		assertDoesNotThrow(desktop::writeConfigurationFile);

		assertFalse(missing.exists());
		assertFalse(configFile().exists());
	}

	// ------------------------------------------------------------- save + load

	@Test
	void loadingExposesWhatWasSaved() {
		newView("Exposed View", new Rectangle(9, 19, 290, 190));
		desktop.writeConfigurationFile();

		desktop.loadConfigurationFile();

		Properties loaded = desktop.getSavedProperties();
		assertNotNull(loaded);
		assertEquals("9", loaded.getProperty("Exposed_View.x"));
		assertEquals("19", loaded.getProperty("Exposed_View.y"));
		assertEquals("290", loaded.getProperty("Exposed_View.w"));
		assertEquals("190", loaded.getProperty("Exposed_View.h"));
	}

	@Test
	void savedLayoutRestoresViewBoundsOntoTheDesktop() {
		BaseView view = newView("Restore View", new Rectangle(30, 40, 350, 250));
		desktop.writeConfigurationFile();
		onEdt(() -> {
			view.setBounds(500, 400, 120, 110); // user moves and resizes it
			return null;
		});

		desktop.loadConfigurationFile();
		onEdt(() -> {
			desktop.configureViews();
			return null;
		});

		assertEquals(new Rectangle(30, 40, 350, 250), view.getBounds());
	}

	@Test
	void configuringViewsWithNothingLoadedChangesNothing() {
		BaseView view = newView("Untouched View", new Rectangle(30, 40, 350, 250));
		desktop.loadConfigurationFile(); // no file: nothing loaded
		assertNull(desktop.getSavedProperties());

		onEdt(() -> {
			desktop.configureViews();
			return null;
		});

		assertEquals(new Rectangle(30, 40, 350, 250), view.getBounds());
	}

	@Test
	void aViewNotInTheSavedLayoutKeepsItsPlace() {
		newView("Was Saved", new Rectangle(30, 40, 350, 250));
		desktop.writeConfigurationFile();
		BaseView latecomer = newView("Came Later", new Rectangle(60, 70, 280, 180));

		desktop.loadConfigurationFile();
		onEdt(() -> {
			desktop.configureViews();
			return null;
		});

		assertEquals(new Rectangle(60, 70, 280, 180), latecomer.getBounds());
	}

	// ------------------------------------------------------------------ delete

	@Test
	void deletingWhenThereIsNoFileIsASilentNoOp() {
		assertFalse(configFile().exists());

		// Would throw HeadlessException if it tried to show its confirm dialog.
		assertDoesNotThrow(desktop::deleteConfigurationFile);
	}
}
