package edu.cnu.mdi.ui.fonts;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common font definitions for uniform typography across the application.
 *
 * @author DHeddle
 */
public final class Fonts {

	// target or desired family
	private static final String TARGET_FAMILY = "Lucida Grande";

	// backup if the target is not found
	private static final String BACKUP_FAMILY = "SansSerif";

	// resolved common family used throughout the app
	private static final String COMMON_FAMILY = resolveCommonFamily();

	// cache of created fonts
	private static final Map<String, Font> FONT_CACHE = new ConcurrentHashMap<>(41);

	// for huge warnings
	public static final Font monsterFont = commonFont(Font.BOLD, 44);

	// common large fonts used for components
	public static final Font hugeFont = commonFont(Font.PLAIN, 18);

	public static final Font largeFont = commonFont(Font.PLAIN, 14);

	// slightly bigger font used for components
	public static final Font biggerFont = commonFont(Font.PLAIN, 13);

	// common default fonts
	public static final Font defaultFont = commonFont(Font.PLAIN, 12);

	public static final Font defaultLargeFont = commonFont(Font.BOLD, 14);

	public static final Font defaultBoldFont = commonFont(Font.BOLD, 12);

	public static final Font defaultItalicFont = commonFont(Font.ITALIC, 12);

	// medium fonts
	public static final Font mediumFont = commonFont(Font.PLAIN, 11);

	public static final Font mediumBoldFont = commonFont(Font.BOLD, 11);

	public static final Font mediumItalicFont = commonFont(Font.ITALIC, 11);

	public static final Font mediumItalicBoldFont = commonFont(Font.ITALIC | Font.BOLD, 11);

	// tween / small fonts
	public static final Font tweenFont = commonFont(Font.PLAIN, 10);

	public static final Font tweenBoldFont = commonFont(Font.BOLD, 10);

	public static final Font tweenItalicFont = commonFont(Font.ITALIC, 10);

	public static final Font smallFont = commonFont(Font.PLAIN, 9);

	public static final Font tinyFont = commonFont(Font.PLAIN, 8);

	// monospaced fonts
	public static final Font defaultMono = monoFont(Font.PLAIN, 12);

	public static final Font mono = monoFont(Font.PLAIN, 13);

	public static final Font smallMono = monoFont(Font.PLAIN, 10);

	public static final Font tinyMono = monoFont(Font.PLAIN, 8);

	private Fonts() {
		// utility class; no instances
	}

	/**
	 * Scale a font.
	 *
	 * @param font        the font to scale (must not be null)
	 * @param scaleFactor the multiplicative scale factor
	 * @return the derived font
	 */
	public static Font scaleFont(Font font, float scaleFactor) {
		if (font == null) {
			throw new IllegalArgumentException("font cannot be null");
		}
		return font.deriveFont(scaleFactor * font.getSize2D());
	}

	/**
	 * Obtain a font from the common family.
	 *
	 * @param style bitwise Font.PLAIN, Font.BOLD, etc.
	 * @param size  the size
	 * @return the common font.
	 */
	public static Font commonFont(int style, int size) {
		String key = COMMON_FAMILY + "$" + size + "$" + style;
		return FONT_CACHE.computeIfAbsent(key, k -> new Font(COMMON_FAMILY, style, size));
	}

	/**
	 * Obtain a monospaced font with caching similar to commonFont.
	 *
	 * @param style bitwise Font.PLAIN, Font.BOLD, etc.
	 * @param size  the size
	 * @return the monospaced font.
	 */
	private static Font monoFont(int style, int size) {
		String key = "MONO$" + size + "$" + style;
		return FONT_CACHE.computeIfAbsent(key, k -> new Font(Font.MONOSPACED, style, size));
	}

	/**
	 * Resolve the common font family name, falling back if necessary.
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
			// In headless or error cases, we just fall back to BACKUP_FAMILY
		}
		return family;
	}
}
