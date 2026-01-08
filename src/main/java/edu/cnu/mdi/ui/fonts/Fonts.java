package edu.cnu.mdi.ui.fonts;

import java.awt.Font;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.UIManager;

/**
 * Centralized font factory for MDI.
 * <p>
 * With FlatLaf, the correct "common" font is the LookAndFeel default font
 * (UI key "defaultFont"). This class derives all application fonts from that
 * base font to ensure consistent typography across platforms and themes.
 * </p>
 */
public final class Fonts {

    /** Cache for derived fonts: "<style>$<deltaOrSize>$<mode>" */
    private static final Map<String, Font> FONT_CACHE = new ConcurrentHashMap<>(41);

    /** Base UI font (from the active LookAndFeel). */
    private static volatile Font BASE_UI_FONT;

    /** Base monospaced font (can be tuned separately if you like). */
    private static volatile Font BASE_MONO_FONT;

    // ---- Public “named” fonts (not final; refreshed when LAF/theme changes) ----
    public static volatile Font monsterFont;
    public static volatile Font hugeFont;
    public static volatile Font largeFont;
    public static volatile Font biggerFont;
    public static volatile Font defaultFont;
    public static volatile Font defaultLargeFont;
    public static volatile Font defaultBoldFont;
    public static volatile Font defaultItalicFont;
    public static volatile Font mediumFont;
    public static volatile Font mediumBoldFont;
    public static volatile Font mediumItalicFont;
    public static volatile Font mediumItalicBoldFont;
    public static volatile Font tweenFont;
    public static volatile Font tweenBoldFont;
    public static volatile Font tweenItalicFont;
    public static volatile Font smallFont;
    public static volatile Font tinyFont;

    public static volatile Font defaultMono;
    public static volatile Font mono;
    public static volatile Font smallMono;
    public static volatile Font tinyMono;

    private Fonts() {}

    /**
     * Refresh font constants from the current LookAndFeel.
     * <p>
     * Call this AFTER installing FlatLaf (and again after any theme switch).
     * </p>
     */
    public static void refresh() {
        FONT_CACHE.clear();

        BASE_UI_FONT = uiDefaultFont();
        BASE_MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, BASE_UI_FONT.getSize());

        // Map your old “absolute sizes” to relative deltas from base UI size.
        // This keeps Windows/macOS/Linux looking “right” under FlatLaf. :contentReference[oaicite:4]{index=4}
        defaultFont = deriveFromBase(Font.PLAIN, 0);
        biggerFont  = deriveFromBase(Font.PLAIN, +1);
        largeFont   = deriveFromBase(Font.PLAIN, +2);
        hugeFont    = deriveFromBase(Font.PLAIN, +5);

        defaultBoldFont   = deriveFromBase(Font.BOLD, 0);
        defaultItalicFont = deriveFromBase(Font.ITALIC, 0);
        defaultLargeFont  = deriveFromBase(Font.BOLD, +2);

        mediumFont           = deriveFromBase(Font.PLAIN, -1);
        mediumBoldFont       = deriveFromBase(Font.BOLD, -1);
        mediumItalicFont     = deriveFromBase(Font.ITALIC, -1);
        mediumItalicBoldFont = deriveFromBase(Font.BOLD | Font.ITALIC, -1);

        tweenFont       = deriveFromBase(Font.PLAIN, -2);
        tweenBoldFont   = deriveFromBase(Font.BOLD, -2);
        tweenItalicFont = deriveFromBase(Font.ITALIC, -2);

        smallFont = deriveFromBase(Font.PLAIN, -3);
        tinyFont  = deriveFromBase(Font.PLAIN, -4);

        // "monster" is special: keep it absolute-ish, but still based on family/metrics.
        monsterFont = BASE_UI_FONT.deriveFont(Font.BOLD, Math.max(28f, BASE_UI_FONT.getSize2D() * 3.0f));

        // Monospace derived from base size (consistent across platforms).
        defaultMono = deriveMono(Font.PLAIN, 0);
        mono        = deriveMono(Font.PLAIN, +1);
        smallMono   = deriveMono(Font.PLAIN, -2);
        tinyMono    = deriveMono(Font.PLAIN, -4);
    }

    /** Returns the current base UI font from UI defaults (FlatLaf uses "defaultFont"). :contentReference[oaicite:5]{index=5} */
    private static Font uiDefaultFont() {
        Font f = UIManager.getFont("defaultFont");
        if (f == null) {
            f = UIManager.getFont("Label.font");
        }
        if (f == null) {
            f = new Font("SansSerif", Font.PLAIN, 12);
        }
        return f;
    }

    /**
	 * Convenience method to derive a plain font from BASE_UI_FONT
	 * using a size delta (in points) relative to base.
	 */
	public static Font plainFontDelta(int sizeDelta) {
		return deriveFromBase(Font.PLAIN, sizeDelta);
	}

    /**
     * Derive a font from BASE_UI_FONT using a size delta (in points) relative to base.
     */
    private static Font deriveFromBase(int style, int deltaPt) {
        final Font base = (BASE_UI_FONT != null) ? BASE_UI_FONT : uiDefaultFont();
        final int size = Math.max(8, base.getSize() + deltaPt);

        String key = "UI$" + style + "$" + deltaPt;
        return FONT_CACHE.computeIfAbsent(key, k -> base.deriveFont(style, size));
    }

    /**
     * Derive a monospaced font from BASE_MONO_FONT using a size delta.
     */
    private static Font deriveMono(int style, int deltaPt) {
        final Font base = (BASE_MONO_FONT != null) ? BASE_MONO_FONT : new Font(Font.MONOSPACED, Font.PLAIN, 12);
        final int size = Math.max(8, base.getSize() + deltaPt);

        String key = "MONO$" + style + "$" + deltaPt;
        return FONT_CACHE.computeIfAbsent(key, k -> base.deriveFont(style, size));
    }

    /** Keep your scale helper exactly as-is. */
    public static Font scaleFont(Font font, float scaleFactor) {
        if (font == null) {
			throw new IllegalArgumentException("font cannot be null");
		}
        return font.deriveFont(scaleFactor * font.getSize2D());
    }
}
