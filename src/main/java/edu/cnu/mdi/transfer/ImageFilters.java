package edu.cnu.mdi.transfer;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

public class ImageFilters {
    // Get all supported extensions (e.g., jpg, png, gif) and store them in a Set for O(1) lookup
    private static final Set<String> SUPPORTED_EXTENSIONS = Arrays.stream(ImageIO.getReaderFileSuffixes())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

    /** Predicate to test if a File is a readable image file based on its extension. */
    public static final Predicate<File> isReadableImage = file -> {
        if (file == null || !file.isFile() || !file.canRead()) {
			return false;
		}

        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
			return false;
		}

        String ext = name.substring(lastDot + 1).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(ext);
    };

    /** Predicate to test if a File is an actual image by attempting to read it.
     * Slower than isReadableImage but provides certainty */
    public static final Predicate<File> isActualImage = file -> {
        try {
            // ImageIO.read returns null if the format is not supported or file is not an image
            return file != null && file.isFile() && ImageIO.read(file) != null;
        } catch (Exception e) {
            return false;
        }
    };
}
