package edu.cnu.mdi.splot.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Simple "recent files" manager backed by {@link Preferences}.
 * <p>
 * Stores absolute paths, MRU order, with a fixed maximum size.
 */
public final class RecentPlotFiles {

    private static final String KEY_PREFIX = "recentPlotFile.";
    private static final String KEY_COUNT = "recentPlotFile.count";

    private final Preferences prefs;
    private final int maxSize;

    /**
     * @param prefsNode preferences node to store values in (e.g. Preferences.userNodeForPackage(SomeClass.class))
     * @param maxSize maximum number of recent entries to retain (typical: 8–15)
     */
    public RecentPlotFiles(Preferences prefsNode, int maxSize) {
        if (prefsNode == null) throw new IllegalArgumentException("prefsNode is null");
        if (maxSize < 1) throw new IllegalArgumentException("maxSize must be >= 1");
        this.prefs = prefsNode;
        this.maxSize = maxSize;
        normalize(); // clean any stale/duplicate entries on startup
    }

    /** Default constructor using this class' package node and max 10 entries. */
    public RecentPlotFiles() {
        this(Preferences.userNodeForPackage(RecentPlotFiles.class), 10);
    }

    /** @return max number of entries retained */
    public int getMaxSize() {
        return maxSize;
    }

    /** Add/update a path at the front of the MRU list. Ignores nulls and non-existent files. */
    public void add(File file) {
        if (file == null) return;
        File f = file;
        try {
            f = file.getCanonicalFile();
        } catch (Exception ignored) {
            // keep original
        }

        if (!f.exists() || f.isDirectory()) return;

        String path = f.getAbsolutePath();
        List<String> list = readRaw();

        // Remove any existing occurrence
        list.removeIf(path::equals);

        // Add to front
        list.add(0, path);

        // Trim
        if (list.size() > maxSize) {
            list = new ArrayList<>(list.subList(0, maxSize));
        }

        writeRaw(list);
    }

    /** Remove a file from the list (if present). */
    public void remove(File file) {
        if (file == null) return;
        String path = file.getAbsolutePath();
        List<String> list = readRaw();
        if (list.removeIf(path::equals)) {
            writeRaw(list);
        }
    }

    /** Clears all recent entries. */
    public void clear() {
        writeRaw(Collections.emptyList());
    }

    /**
     * Returns recent files as File objects, filtered to those that still exist.
     * (Non-existent entries are automatically dropped.)
     */
    public List<File> getRecentFiles() {
        List<String> list = readRaw();
        boolean changed = false;

        List<File> out = new ArrayList<>();
        for (String p : list) {
            if (p == null || p.isBlank()) {
                changed = true;
                continue;
            }
            File f = new File(p);
            if (f.exists() && f.isFile()) {
                out.add(f);
            } else {
                changed = true;
            }
        }

        if (changed) {
            // Write back normalized list
            List<String> normalized = new ArrayList<>();
            for (File f : out) normalized.add(f.getAbsolutePath());
            writeRaw(normalized);
        }

        return out;
    }

    /** For menus: returns absolute paths, MRU order, filtered to existing. */
    public List<String> getRecentPaths() {
        List<File> files = getRecentFiles();
        List<String> paths = new ArrayList<>(files.size());
        for (File f : files) paths.add(f.getAbsolutePath());
        return paths;
    }

    // -----------------------
    // Internal read/write
    // -----------------------

    private List<String> readRaw() {
        int n = prefs.getInt(KEY_COUNT, 0);
        if (n < 0) n = 0;

        List<String> list = new ArrayList<>(Math.min(n, maxSize));
        for (int i = 0; i < n; i++) {
            String p = prefs.get(KEY_PREFIX + i, null);
            if (p != null && !p.isBlank()) {
                list.add(p);
            }
        }
        return list;
    }

    private void writeRaw(List<String> list) {
        if (list == null) list = Collections.emptyList();

        // First clear old keys
        int old = prefs.getInt(KEY_COUNT, 0);
        for (int i = 0; i < old; i++) {
            prefs.remove(KEY_PREFIX + i);
        }

        // Write new
        int n = Math.min(list.size(), maxSize);
        for (int i = 0; i < n; i++) {
            prefs.put(KEY_PREFIX + i, list.get(i));
        }
        prefs.putInt(KEY_COUNT, n);

        try {
            prefs.flush();
        } catch (BackingStoreException ignored) {
            // Not fatal; will likely persist later.
        }
    }

    /** Removes duplicates, drops missing, trims to max size. */
    private void normalize() {
        List<File> files = getRecentFiles(); // this already drops missing & rewrites if changed
        // also ensure uniqueness and trim (belt-and-suspenders)
        List<String> uniq = new ArrayList<>();
        for (File f : files) {
            String p = f.getAbsolutePath();
            if (!uniq.contains(p)) uniq.add(p);
            if (uniq.size() >= maxSize) break;
        }
        writeRaw(uniq);
    }
}
