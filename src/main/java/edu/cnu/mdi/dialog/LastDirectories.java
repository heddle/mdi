package edu.cnu.mdi.dialog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Persistent, preference-backed "last visited directory" per caller-defined
 * purpose.
 *
 * <p>Mirrors {@link edu.cnu.mdi.io.RecentFiles}'s own Preferences-backed,
 * constructor-injected design, for the same reason: production code gets a
 * stable, real backing store (a node keyed off this class), while tests get
 * an isolated, disposable one so runs never read or leave behind real user
 * preferences.</p>
 */
final class LastDirectories {

	private static final String KEY_PREFIX = "lastDirectory.";

	private final Preferences preferences;

	LastDirectories(Preferences preferences) {
		this.preferences = Objects.requireNonNull(preferences, "preferences");
	}

	/**
	 * Returns the last remembered directory for {@code purpose}, provided it
	 * still exists on disk.
	 *
	 * @param purpose stable directory-history key
	 * @return the remembered directory, or empty if none is stored or the
	 *         stored path is no longer a directory
	 */
	Optional<Path> get(String purpose) {
		String stored = preferences.get(KEY_PREFIX + purpose, null);
		if (stored == null || stored.isBlank()) return Optional.empty();
		Path path = Path.of(stored);
		return Files.isDirectory(path) ? Optional.of(path) : Optional.empty();
	}

	/**
	 * Remembers {@code directory} as the last visited directory for
	 * {@code purpose}, persisted across JVM restarts.
	 *
	 * @param purpose stable directory-history key
	 * @param directory the directory to remember
	 */
	void remember(String purpose, Path directory) {
		preferences.put(KEY_PREFIX + purpose, directory.toAbsolutePath().toString());
		try {
			preferences.flush();
		} catch (BackingStoreException ignored) {
			// Persistence failure is non-fatal for a convenience feature: the
			// in-JVM Preferences cache still has the value for this run.
		}
	}
}
