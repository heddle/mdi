package edu.cnu.mdi.ui.fonts;

import java.awt.Font;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.UIManager;

/**
 * Centralized font factory for MDI.
 * <p>
 * Under FlatLaf the canonical "common" font is the LookAndFeel default font (UI
 * key {@code "defaultFont"}). This class derives every application font from
 * that base so typography stays consistent across platforms and themes.
 * <p>
 * The named fonts form a size ladder expressed as point deltas from the base UI
 * size, so they track the platform/theme default rather than using hard-coded
 * absolute sizes:
 * <pre>
 *   monster (+8) &gt; huge (+5) &gt; large (+2) &gt; bigger (+1) &gt; default (0)
 *     &gt; medium (-1) &gt; tween (-2) &gt; small (-3) &gt; tiny (-4)
 * </pre>
 * with bold/italic variants derived at the same sizes. Monospaced fonts follow a
 * parallel ladder built on {@link Font#MONOSPACED}.
 * <p>
 * Call {@link #refresh()} once after installing FlatLaf, and again after any
 * theme switch, to (re)populate the named fonts. The named fields are intended
 * to be read on the Swing event dispatch thread.
 */
public final class Fonts {

	/** Smallest point size any derived font is allowed to shrink to. */
	private static final int MIN_FONT_SIZE = 8;

	/** Point size used when no UI font can be resolved at all. */
	private static final int FALLBACK_FONT_SIZE = 12;

	/** Cache of derived fonts, keyed by {@code "<mode>$<style>$<sizeDelta>"} (e.g. {@code "UI$0$2"}). */
	private static final Map<String, Font> FONT_CACHE = new ConcurrentHashMap<>(41);

	/** Base UI font (from the active LookAndFeel). */
	private static volatile Font BASE_UI_FONT;

	/** Base monospaced font (can be tuned separately if desired). */
	private static volatile Font BASE_MONO_FONT;

	// ---- Public named fonts (not final; refreshed when the LAF/theme changes) ----
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

	private Fonts() {
	}

	/**
	 * Refresh the named fonts from the current LookAndFeel.
	 * <p>
	 * Clears the derived-font cache and rebuilds every named font relative to the
	 * current base UI size. Call this after installing FlatLaf and again after any
	 * theme switch.
	 */
	public static void refresh() {
		FONT_CACHE.clear();

		BASE_UI_FONT = uiDefaultFont();
		BASE_MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, BASE_UI_FONT.getSize());

		// Old absolute sizes are expressed as deltas from the base UI size so the
		// ladder looks right on Windows/macOS/Linux under FlatLaf.
		defaultFont = deriveFromBase(Font.PLAIN, 0);
		biggerFont = deriveFromBase(Font.PLAIN, +1);
		largeFont = deriveFromBase(Font.PLAIN, +2);
		hugeFont = deriveFromBase(Font.PLAIN, +5);
		monsterFont = deriveFromBase(Font.PLAIN, +8);

		defaultBoldFont = deriveFromBase(Font.BOLD, 0);
		defaultItalicFont = deriveFromBase(Font.ITALIC, 0);
		defaultLargeFont = deriveFromBase(Font.BOLD, +2);

		mediumFont = deriveFromBase(Font.PLAIN, -1);
		mediumBoldFont = deriveFromBase(Font.BOLD, -1);
		mediumItalicFont = deriveFromBase(Font.ITALIC, -1);
		mediumItalicBoldFont = deriveFromBase(Font.BOLD | Font.ITALIC, -1);

		tweenFont = deriveFromBase(Font.PLAIN, -2);
		tweenBoldFont = deriveFromBase(Font.BOLD, -2);
		tweenItalicFont = deriveFromBase(Font.ITALIC, -2);

		smallFont = deriveFromBase(Font.PLAIN, -3);
		tinyFont = deriveFromBase(Font.PLAIN, -4);

		// Monospace derived from base size (consistent across platforms).
		defaultMono = deriveMono(Font.PLAIN, 0);
		mono = deriveMono(Font.PLAIN, +1);
		smallMono = deriveMono(Font.PLAIN, -2);
		tinyMono = deriveMono(Font.PLAIN, -4);
	}

	/**
	 * Convenience method to derive a plain font from the base UI font using a size
	 * delta (in points) relative to the base.
	 *
	 * @param sizeDelta points to add to the base size (may be negative)
	 * @return the derived plain font
	 */
	public static Font plainFontDelta(int sizeDelta) {
		return deriveFromBase(Font.PLAIN, sizeDelta);
	}

	/**
	 * Convenience method to derive a bold font from the base UI font using a size
	 * delta (in points) relative to the base.
	 *
	 * @param sizeDelta points to add to the base size (may be negative)
	 * @return the derived bold font
	 */
	public static Font boldFontDelta(int sizeDelta) {
		return deriveFromBase(Font.BOLD, sizeDelta);
	}

	/**
	 * Resolve the current base UI font from UI defaults. Prefers FlatLaf's
	 * {@code "defaultFont"}, falls back to {@code "Label.font"}, and finally to a
	 * plain {@value #FALLBACK_FONT_SIZE}pt sans-serif font.
	 *
	 * @return a non-{@code null} base UI font
	 */
	private static Font uiDefaultFont() {
		Font f = UIManager.getFont("defaultFont");
		if (f == null) {
			f = UIManager.getFont("Label.font");
		}
		if (f == null) {
			f = new Font(Font.SANS_SERIF, Font.PLAIN, FALLBACK_FONT_SIZE);
		}
		return f;
	}

	/**
	 * Derive a font from the base UI font, clamped to {@value #MIN_FONT_SIZE}pt and
	 * cached by style/delta.
	 *
	 * @param style   an AWT font style bitmask ({@link Font#PLAIN}, {@link Font#BOLD}, ...)
	 * @param deltaPt points to add to the base UI size
	 * @return the derived (possibly cached) font
	 */
	private static Font deriveFromBase(int style, int deltaPt) {
		final Font base = (BASE_UI_FONT != null) ? BASE_UI_FONT : uiDefaultFont();
		final int size = Math.max(MIN_FONT_SIZE, base.getSize() + deltaPt);

		String key = "UI$" + style + "$" + deltaPt;
		return FONT_CACHE.computeIfAbsent(key, k -> base.deriveFont(style, size));
	}

	/**
	 * Derive a monospaced font from the base mono font, clamped to
	 * {@value #MIN_FONT_SIZE}pt and cached by style/delta.
	 *
	 * @param style   an AWT font style bitmask ({@link Font#PLAIN}, {@link Font#BOLD}, ...)
	 * @param deltaPt points to add to the base mono size
	 * @return the derived (possibly cached) font
	 */
	private static Font deriveMono(int style, int deltaPt) {
		final Font base = (BASE_MONO_FONT != null) ? BASE_MONO_FONT
				: new Font(Font.MONOSPACED, Font.PLAIN, FALLBACK_FONT_SIZE);
		final int size = Math.max(MIN_FONT_SIZE, base.getSize() + deltaPt);

		String key = "MONO$" + style + "$" + deltaPt;
		return FONT_CACHE.computeIfAbsent(key, k -> base.deriveFont(style, size));
	}

}