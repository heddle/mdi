package edu.cnu.mdi.dialog;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Describes one file type used by {@link FileDialogs}.
 *
 * <p>Extensions are stored without a leading dot and compared
 * case-insensitively. The first extension is the preferred extension appended
 * by {@link #ensureExtension(Path)} when a save target has none of the accepted
 * suffixes.</p>
 *
 * @param description user-facing filter description
 * @param extensions accepted extensions, without leading dots
 */
public record FileType(String description, List<String> extensions) {

    /**
     * Validates and defensively copies this file-type definition.
     *
     * @throws NullPointerException if the description, list, or an extension is null
     * @throws IllegalArgumentException if the description is blank, the list is
     *                                  empty, or an extension is blank
     */
    public FileType {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(extensions, "extensions");
        if (description.isBlank()) throw new IllegalArgumentException("description must not be blank");
        if (extensions.isEmpty()) throw new IllegalArgumentException("at least one extension is required");
        extensions = extensions.stream().map(extension -> {
            Objects.requireNonNull(extension, "extension");
            String normalized = extension.startsWith(".") ? extension.substring(1) : extension;
            if (normalized.isBlank()) throw new IllegalArgumentException("extension must not be blank");
            return normalized.toLowerCase(Locale.ROOT);
        }).distinct().toList();
    }

    /**
     * Convenience factory accepting varargs extensions.
     *
     * @param description user-facing description
     * @param extensions accepted extensions
     * @return validated file type
     */
    public static FileType of(String description, String... extensions) {
        Objects.requireNonNull(extensions, "extensions");
        return new FileType(description, Arrays.asList(extensions));
    }

    /** @return Swing chooser filter corresponding to this definition */
    public FileNameExtensionFilter toSwingFilter() {
        return new FileNameExtensionFilter(description, extensions.toArray(String[]::new));
    }

    /**
     * Ensures that a save path has one of the accepted extensions.
     *
     * @param path proposed path
     * @return original path when accepted, otherwise a sibling path with the
     *         preferred extension appended
     */
    public Path ensureExtension(Path path) {
        Objects.requireNonNull(path, "path");
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (filename.endsWith("." + extension)) return path;
        }
        return path.resolveSibling(path.getFileName() + "." + extensions.get(0));
    }
}
