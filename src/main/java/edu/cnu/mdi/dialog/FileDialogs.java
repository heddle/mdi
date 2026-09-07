package edu.cnu.mdi.dialog;

import java.awt.Component;
import java.awt.EventQueue;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import edu.cnu.mdi.util.Environment;

/**
 * Centralized open/save dialogs for MDI applications.
 *
 * <p>The service consistently configures filters, remembers the last directory
 * independently for each caller-supplied purpose, appends save extensions, and
 * confirms overwrites. Methods return {@link Optional#empty()} when the user
 * cancels, allowing action handlers to avoid chooser result-code boilerplate.</p>
 *
 * <h2>Purpose keys</h2>
 * <p>A purpose is a stable application-defined identifier such as
 * {@code "problem-json"} or {@code "plot-export"}. Open and save share one
 * remembered directory per purpose (not separate ones) -- in practice a file
 * of a given kind is saved and reopened from the same folder far more often
 * than not, so splitting the two would make the common case worse. A caller
 * that genuinely wants independent histories can already get that by using
 * distinct purpose strings for each (e.g. {@code "shapefile-import"} vs.
 * {@code "shapefile-export"}).</p>
 *
 * <p>The remembered directory is persisted (via {@link Preferences}) across
 * JVM restarts, keyed by purpose -- see {@link #lastDirectory(String)} and
 * {@link #rememberDirectory(String, Path)}, which are also available to
 * callers that build their own {@link JFileChooser} (for filtering a caller
 * cannot express as a simple extension list) but still want to participate
 * in the same per-purpose directory memory. Before a purpose has any history
 * (or its remembered directory no longer exists), {@link Environment}'s
 * data directory is used as the initial location -- this is purely a
 * developer-configured default (see {@link Environment#setDataDirectory},
 * typically set once at startup from a command-line argument or environment
 * variable) and is <em>not</em> updated by every dialog use, so it stays
 * whatever the developer intended rather than drifting to wherever the user
 * last happened to browse.</p>
 *
 * <h2>Threading</h2>
 * <p>Like other modal Swing dialogs, {@link #openFile} and {@link #saveFile}
 * must be invoked on the EDT. An {@link IllegalStateException} is thrown
 * otherwise so an accidental worker thread call cannot create
 * platform-dependent UI behavior. {@link #lastDirectory(String)} and
 * {@link #rememberDirectory(String, Path)} have no such requirement.</p>
 */
public final class FileDialogs {

    private static volatile LastDirectories lastDirectories =
            new LastDirectories(Preferences.userNodeForPackage(FileDialogs.class));

    private FileDialogs() { }

    /**
     * Shows a single-file open dialog.
     *
     * @param parent parent component, or {@code null}
     * @param purpose stable directory-history key
     * @param title dialog title
     * @param type accepted file type
     * @return selected path, or empty if cancelled
     */
    public static Optional<Path> openFile(Component parent, String purpose, String title,
            FileType type) {
        requireEdt();
        JFileChooser chooser = chooser(purpose, title, type);
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return Optional.empty();
        Path selected = chooser.getSelectedFile().toPath();
        rememberDirectory(purpose, selected);
        return Optional.of(selected);
    }

    /**
     * Shows a save dialog with extension normalization and overwrite confirmation.
     *
     * @param parent parent component, or {@code null}
     * @param purpose stable directory-history key
     * @param title dialog title
     * @param suggestedFilename initial filename, or {@code null} for none
     * @param type saved file type
     * @return normalized target path, or empty if cancelled or overwrite declined
     */
    public static Optional<Path> saveFile(Component parent, String purpose, String title,
            String suggestedFilename, FileType type) {
        requireEdt();
        JFileChooser chooser = chooser(purpose, title, type);
        if (suggestedFilename != null && !suggestedFilename.isBlank()) {
            chooser.setSelectedFile(new File(suggestedFilename));
        }
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return Optional.empty();
        Path target = type.ensureExtension(chooser.getSelectedFile().toPath());
        if (target.toFile().exists()) {
            int response = JOptionPane.showConfirmDialog(parent,
                    "Overwrite existing file?\n" + target.toAbsolutePath(),
                    "Confirm Save", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (response != JOptionPane.OK_OPTION) return Optional.empty();
        }
        rememberDirectory(purpose, target);
        return Optional.of(target);
    }

    /**
     * Returns the best initial directory for {@code purpose}: the last
     * remembered directory if it still exists, otherwise
     * {@link Environment}'s developer-configured data directory if that
     * exists, otherwise empty -- leaving it to the caller (or, for
     * {@link #openFile}/{@link #saveFile}, to {@link JFileChooser}'s own
     * platform default, typically the home directory).
     *
     * <p>Exposed for callers that build their own {@link JFileChooser} (for
     * filtering {@link FileType} cannot express, such as compound
     * extensions) but still want to share this same per-purpose directory
     * memory with the rest of the application.</p>
     *
     * @param purpose stable directory-history key
     * @return the resolved initial directory, or empty
     */
    public static Optional<Path> lastDirectory(String purpose) {
        Objects.requireNonNull(purpose, "purpose");
        if (purpose.isBlank()) throw new IllegalArgumentException("purpose must not be blank");
        Optional<Path> remembered = lastDirectories.get(purpose);
        if (remembered.isPresent()) return remembered;
        String dataDirectory = Environment.getInstance().getDataDirectory();
        if (dataDirectory == null || dataDirectory.isBlank()) return Optional.empty();
        Path defaultDirectory = Path.of(dataDirectory);
        return Files.isDirectory(defaultDirectory) ? Optional.of(defaultDirectory) : Optional.empty();
    }

    /**
     * Remembers {@code fileOrDirectory}'s directory (or itself, if it is
     * already a directory) as the last visited directory for
     * {@code purpose}, for {@link #lastDirectory(String)} and subsequent
     * {@link #openFile}/{@link #saveFile} calls to pick up -- persisted
     * across JVM restarts.
     *
     * @param purpose stable directory-history key
     * @param fileOrDirectory a file just opened/saved, or a directory just
     *                        navigated to
     */
    public static void rememberDirectory(String purpose, Path fileOrDirectory) {
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(fileOrDirectory, "fileOrDirectory");
        if (purpose.isBlank()) throw new IllegalArgumentException("purpose must not be blank");
        Path absolute = fileOrDirectory.toAbsolutePath();
        Path directory = Files.isDirectory(absolute) ? absolute : absolute.getParent();
        if (directory != null) {
            lastDirectories.remember(purpose, directory);
        }
    }

    private static JFileChooser chooser(String purpose, String title, FileType type) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(type, "type");
        Optional<Path> directory = lastDirectory(purpose);
        JFileChooser chooser = directory.isEmpty() ? new JFileChooser()
                : new JFileChooser(directory.get().toFile());
        chooser.setDialogTitle(title);
        chooser.setFileFilter(type.toSwingFilter());
        DialogUtils.requestDetailsView(chooser);
        return chooser;
    }

    private static void requireEdt() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("File dialogs must be shown on the Swing EDT");
        }
    }

    /**
     * Test-only hook to swap in an isolated, disposable {@link Preferences}
     * node so tests never read or leave behind real user preferences. Not
     * part of the public API.
     */
    static void useLastDirectoriesForTesting(Preferences preferences) {
        lastDirectories = new LastDirectories(preferences);
    }
}
