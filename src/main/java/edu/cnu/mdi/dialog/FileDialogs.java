package edu.cnu.mdi.dialog;

import java.awt.Component;
import java.awt.EventQueue;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
 * {@code "problem-json"} or {@code "plot-export"}. Separate purposes remember
 * separate directories for the life of the JVM. Before a purpose has history,
 * MDI's general data directory is used as the initial location.</p>
 *
 * <h2>Threading</h2>
 * <p>Like other modal Swing dialogs, these methods must be invoked on the EDT.
 * An {@link IllegalStateException} is thrown otherwise so an accidental worker
 * thread call cannot create platform-dependent UI behavior.</p>
 */
public final class FileDialogs {

    private static final ConcurrentMap<String, Path> LAST_DIRECTORIES = new ConcurrentHashMap<>();

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
        remember(purpose, selected);
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
        remember(purpose, target);
        return Optional.of(target);
    }

    private static JFileChooser chooser(String purpose, String title, FileType type) {
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(type, "type");
        if (purpose.isBlank()) throw new IllegalArgumentException("purpose must not be blank");
        Path directory = LAST_DIRECTORIES.get(purpose);
        if (directory == null) {
            String dataDirectory = Environment.getInstance().getDataDirectory();
            if (dataDirectory != null && !dataDirectory.isBlank()) directory = Path.of(dataDirectory);
        }
        JFileChooser chooser = directory == null ? new JFileChooser()
                : new JFileChooser(directory.toFile());
        chooser.setDialogTitle(title);
        chooser.setFileFilter(type.toSwingFilter());
        DialogUtils.requestDetailsView(chooser);
        return chooser;
    }

    private static void remember(String purpose, Path selected) {
        Path absolute = selected.toAbsolutePath();
        Path directory = absolute.toFile().isDirectory() ? absolute : absolute.getParent();
        if (directory != null) {
            LAST_DIRECTORIES.put(purpose, directory);
            Environment.getInstance().setDataDirectory(directory.toString());
        }
    }

    private static void requireEdt() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("File dialogs must be shown on the Swing EDT");
        }
    }
}
