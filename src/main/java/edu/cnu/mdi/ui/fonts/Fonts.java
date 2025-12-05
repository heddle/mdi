package edu.cnu.mdi.ui.fonts;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Centralized font factory for the MDI application. This class ensures
 * consistent font usage, provides caching to avoid unnecessary object creation,
 * and selects a preferred font family (e.g., {@code Lucida Grande}) when
 * available on the host system.
 * </p>
 *
 * <p>
 * All fonts returned by this class are immutable, cached, and suitable for
 * reuse throughout Swing components. Monospaced fonts are also supported via
 * the {@code monoFont} helpers.
 * </p>
 *
 * <p>
 * This class is a static utility and cannot be instantiated.
 * </p>
 *
 * @author DHeddle
 */
public final class Fonts {

    /** Preferred font family to use if available on the host system. */
    private static final String TARGET_FAMILY = "Lucida Grande";

    /** Backup font family when the preferred family cannot be found. */
    private static final String BACKUP_FAMILY = "SansSerif";

    /**
     * The canonical font family name the application will use. This is resolved
     * at class-load time by inspecting the system's available font families.
     */
    private static final String COMMON_FAMILY = resolveCommonFamily();

    /**
     * Cache for all fonts created by this class. Keys are unique strings of
     * the form {@code "<family>$<size>$<style>"}.
     */
    private static final Map<String, Font> FONT_CACHE = new ConcurrentHashMap<>(41);

    /** Very large bold font intended for strong warnings or alerts. */
    public static final Font monsterFont = commonFont(Font.BOLD, 44);

    /** Large plain font intended for headline UI elements. */
    public static final Font hugeFont = commonFont(Font.PLAIN, 18);

    /** Standard large plain font for general UI components. */
    public static final Font largeFont = commonFont(Font.PLAIN, 14);

    /** Slightly larger than default, good for emphasis in UI controls. */
    public static final Font biggerFont = commonFont(Font.PLAIN, 13);

    /** Default plain font for general application text. */
    public static final Font defaultFont = commonFont(Font.PLAIN, 12);

    /** Default bold font at a larger size. */
    public static final Font defaultLargeFont = commonFont(Font.BOLD, 14);

    /** Default bold font at standard size. */
    public static final Font defaultBoldFont = commonFont(Font.BOLD, 12);

    /** Default italic font. */
    public static final Font defaultItalicFont = commonFont(Font.ITALIC, 12);

    /** Medium-sized plain font. */
    public static final Font mediumFont = commonFont(Font.PLAIN, 11);

    /** Medium-sized bold font. */
    public static final Font mediumBoldFont = commonFont(Font.BOLD, 11);

    /** Medium-sized italic font. */
    public static final Font mediumItalicFont = commonFont(Font.ITALIC, 11);

    /** Medium-sized italic + bold font. */
    public static final Font mediumItalicBoldFont = commonFont(Font.ITALIC | Font.BOLD, 11);

    /** Small plain font. */
    public static final Font tweenFont = commonFont(Font.PLAIN, 10);

    /** Small bold font. */
    public static final Font tweenBoldFont = commonFont(Font.BOLD, 10);

    /** Small italic font. */
    public static final Font tweenItalicFont = commonFont(Font.ITALIC, 10);

    /** Very small plain font. */
    public static final Font smallFont = commonFont(Font.PLAIN, 9);

    /** Tiny plain font (minimal readable size). */
    public static final Font tinyFont = commonFont(Font.PLAIN, 8);

    /** Default monospaced plain font. */
    public static final Font defaultMono = monoFont(Font.PLAIN, 12);

    /** Slightly larger monospaced font. */
    public static final Font mono = monoFont(Font.PLAIN, 13);

    /** Small monospaced font. */
    public static final Font smallMono = monoFont(Font.PLAIN, 10);

    /** Tiny monospaced font. */
    public static final Font tinyMono = monoFont(Font.PLAIN, 8);

    /**
     * Private constructor to prevent instantiation.
     */
    private Fonts() {
        // Utility class; not instantiable.
    }

    /**
     * Scales a given font by the provided multiplicative factor.
     *
     * <p>
     * The operation is equivalent to:
     * <pre>
     * font.deriveFont(font.getSize2D() * scaleFactor)
     * </pre>
     * </p>
     *
     * @param font        the font to scale; must not be {@code null}
     * @param scaleFactor the scale factor to apply (e.g., 1.5 for 50% larger)
     * @return a new {@link Font} object derived from the input font
     * @throws IllegalArgumentException if {@code font} is {@code null}
     */
    public static Font scaleFont(Font font, float scaleFactor) {
        if (font == null) {
            throw new IllegalArgumentException("font cannot be null");
        }
        return font.deriveFont(scaleFactor * font.getSize2D());
    }

    /**
     * Returns a cached font instance from the application's common font family.
     * Fonts are cached by family, style, and size to avoid repeated creation.
     *
     * @param style a combination of {@link Font#PLAIN}, {@link Font#BOLD},
     *              {@link Font#ITALIC}, or bitwise OR of these
     * @param size  the point size of the font
     * @return a cached {@code Font} instance of the resolved common family
     */
    public static Font commonFont(int style, int size) {
        String key = COMMON_FAMILY + "$" + size + "$" + style;
        return FONT_CACHE.computeIfAbsent(key, k -> new Font(COMMON_FAMILY, style, size));
    }

    /**
     * Returns a cached monospaced font instance.
     *
     * @param style a standard {@link Font} style mask
     * @param size  the point size of the font
     * @return a monospaced {@code Font}
     */
    private static Font monoFont(int style, int size) {
        String key = "MONO$" + size + "$" + style;
        return FONT_CACHE.computeIfAbsent(key, k -> new Font(Font.MONOSPACED, style, size));
    }

    /**
     * Attempts to locate the preferred font family on the system. If unavailable,
     * this method falls back to the {@link #BACKUP_FAMILY}.
     *
     * <p>
     * If running in a headless environment or if an error occurs while enumerating
     * font families, the backup family is used.
     * </p>
     *
     * @return the resolved font family to be used throughout the application
     */
    private static String resolveCommonFamily() {
        String family = BACKUP_FAMILY;
        try {
            String[] fnames = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames();

            if (fnames != null) {
                for (String s : fnames) {
                    if (TARGET_FAMILY.equalsIgnoreCase(s)) {
                        family = s;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // In headless or error scenarios, fallback family is used.
        }
        return family;
    }
}
