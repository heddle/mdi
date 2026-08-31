package edu.cnu.mdi.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Persistent, preference-backed most-recently-used file list. */
public final class RecentFiles {

	private static final String DEFAULT_KEY = "recentFile";

	private final Preferences preferences;
	private final int maximumSize;
	private final String keyPrefix;

	/**
	 * Create a list using the default preference key prefix.
	 *
	 * @param preferences the preference node backing this list; must not be
	 *                    {@code null}
	 * @param maximumSize maximum number of entries retained; must be &gt;= 1
	 * @throws NullPointerException     if {@code preferences} is {@code null}
	 * @throws IllegalArgumentException if {@code maximumSize < 1}
	 */
	public RecentFiles(Preferences preferences, int maximumSize) {
		this(preferences, maximumSize, DEFAULT_KEY);
	}

	/**
	 * Create a list with an explicit key prefix, useful when one preference node
	 * owns more than one independent recent-file list.
	 *
	 * @param preferences the preference node backing this list; must not be
	 *                    {@code null}
	 * @param maximumSize maximum number of entries retained; must be &gt;= 1
	 * @param keyPrefix   preference key prefix distinguishing this list from
	 *                    others sharing the same node; must not be blank
	 * @throws NullPointerException     if {@code preferences} is {@code null}
	 * @throws IllegalArgumentException if {@code maximumSize < 1} or
	 *                                  {@code keyPrefix} is blank
	 */
	public RecentFiles(Preferences preferences, int maximumSize, String keyPrefix) {
		this.preferences = Objects.requireNonNull(preferences, "preferences");
		if (maximumSize < 1) throw new IllegalArgumentException("maximumSize must be >= 1");
		if (keyPrefix == null || keyPrefix.isBlank()) {
			throw new IllegalArgumentException("keyPrefix must not be blank");
		}
		this.maximumSize = maximumSize;
		this.keyPrefix = keyPrefix;
		normalize();
	}

	/** @return the maximum number of entries this list retains */
	public int getMaximumSize() {
		return maximumSize;
	}

	/**
	 * Add an existing regular file to the front of the list, moving it there
	 * if already present.
	 * <p>
	 * The file is canonicalized (via {@link File#getCanonicalFile()}, falling
	 * back to {@link File#getAbsoluteFile()} on {@link IOException}) before
	 * being compared/stored, so equivalent paths (e.g. differing only by
	 * {@code .} or symlink segments) are recognized as the same entry. A
	 * {@code null} file, or one that is not an existing regular file, is
	 * silently ignored.
	 * </p>
	 *
	 * @param file the file to add; may be {@code null}
	 */
	public void add(File file) {
		File normalized = normalizeFile(file);
		if (normalized == null || !normalized.isFile()) return;
		List<String> paths = readRaw();
		paths.remove(normalized.getAbsolutePath());
		paths.add(0, normalized.getAbsolutePath());
		writeRaw(paths);
	}

	/**
	 * Remove a file from the list, if present. Uses the same canonicalization
	 * as {@link #add(File)} for comparison. A no-op if {@code file} is
	 * {@code null} or not currently in the list.
	 *
	 * @param file the file to remove; may be {@code null}
	 */
	public void remove(File file) {
		File normalized = normalizeFile(file);
		if (normalized == null) return;
		List<String> paths = readRaw();
		if (paths.remove(normalized.getAbsolutePath())) writeRaw(paths);
	}

	/** Remove all entries from the list. */
	public void clear() {
		writeRaw(Collections.emptyList());
	}

	/** Return existing regular files in MRU order, pruning stale entries. */
	public List<File> getRecentFiles() {
		List<String> raw = readRaw();
		List<File> files = new ArrayList<>();
		for (String path : new LinkedHashSet<>(raw)) {
			if (path == null || path.isBlank()) continue;
			File file = new File(path);
			if (file.isFile()) files.add(file);
			if (files.size() == maximumSize) break;
		}
		List<String> normalized = files.stream().map(File::getAbsolutePath).toList();
		if (!normalized.equals(raw)) writeRaw(normalized);
		return List.copyOf(files);
	}

	/**
	 * @return the same entries as {@link #getRecentFiles()}, as absolute path
	 *         strings, in the same MRU order
	 */
	public List<String> getRecentPaths() {
		return getRecentFiles().stream().map(File::getAbsolutePath).toList();
	}

	private File normalizeFile(File file) {
		if (file == null) return null;
		try {
			return file.getCanonicalFile();
		} catch (IOException exception) {
			return file.getAbsoluteFile();
		}
	}

	private String countKey() {
		return keyPrefix + ".count";
	}

	private String entryKey(int index) {
		return keyPrefix + "." + index;
	}

	private List<String> readRaw() {
		int count = Math.max(0, preferences.getInt(countKey(), 0));
		List<String> paths = new ArrayList<>(Math.min(count, maximumSize));
		for (int index = 0; index < count; index++) {
			String path = preferences.get(entryKey(index), null);
			if (path != null && !path.isBlank()) paths.add(path);
		}
		return paths;
	}

	private void writeRaw(List<String> paths) {
		int oldCount = Math.max(0, preferences.getInt(countKey(), 0));
		for (int index = 0; index < oldCount; index++) preferences.remove(entryKey(index));
		int count = Math.min(paths == null ? 0 : paths.size(), maximumSize);
		for (int index = 0; index < count; index++) {
			preferences.put(entryKey(index), paths.get(index));
		}
		preferences.putInt(countKey(), count);
		try {
			preferences.flush();
		} catch (BackingStoreException ignored) {
			// Persistence failure is non-fatal for an MRU convenience list.
		}
	}

	private void normalize() {
		getRecentFiles();
	}
}
