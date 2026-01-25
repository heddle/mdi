package edu.cnu.mdi.ui.colors;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for looking up colors by X11-style name.
 *
 * <p>
 * Names are case-insensitive and normalized by:
 * <ul>
 * <li>trimming leading/trailing whitespace</li>
 * <li>converting to lower-case</li>
 * <li>collapsing internal whitespace to a single space</li>
 * </ul>
 *
 * Example:
 *
 * <pre>
 * Color c1 = X11Colors.getX11Color("Deep Sky Blue");
 * Color c2 = X11Colors.getX11Color("deep sky blue", 128); // half transparent
 * </pre>
 */
public final class X11Colors {

	// normalizedName -> Color (immutable)
	private static final Map<String, Color> COLORS;

	static {
		Map<String, Color> m = new HashMap<>(180);

		// ---- base X11 color set ----

		put(m, "alice blue", 240, 248, 255);
		put(m, "antique white", 250, 235, 215);
		put(m, "aqua", 0, 255, 255);
		put(m, "aquamarine", 127, 255, 212);
		put(m, "azure", 240, 255, 255);

		put(m, "beige", 245, 245, 220);
		put(m, "bisque", 255, 228, 196);
		put(m, "black", 0, 0, 0);
		put(m, "blanched almond", 255, 235, 205);
		put(m, "blue", 0, 0, 255);
		put(m, "blue violet", 138, 43, 226);
		put(m, "brown", 165, 42, 42);
		put(m, "burlywood", 222, 184, 135);

		put(m, "cadet blue", 95, 158, 160);
		put(m, "chartreuse", 127, 255, 0);
		put(m, "chocolate", 210, 105, 30);
		put(m, "coral", 255, 127, 80);
		put(m, "cornflower blue", 100, 149, 237);
		put(m, "cornsilk", 255, 248, 220);
		put(m, "crimson", 220, 20, 60);
		put(m, "cyan", 0, 255, 255);

		put(m, "dark blue", 0, 0, 139);
		put(m, "dark cyan", 0, 139, 139);
		put(m, "dark goldenrod", 184, 134, 11);
		put(m, "dark gray", 169, 169, 169);
		put(m, "dark green", 0, 100, 0);
		put(m, "dark khaki", 189, 183, 107);
		put(m, "dark magenta", 139, 0, 139);
		put(m, "dark olive green", 85, 107, 47);
		put(m, "dark orange", 255, 140, 0);
		put(m, "dark orchid", 153, 50, 204);
		put(m, "dark red", 139, 0, 0);
		put(m, "dark salmon", 233, 150, 122);
		put(m, "dark sea green", 143, 188, 143);
		put(m, "dark slate blue", 72, 61, 139);
		put(m, "dark slate gray", 47, 79, 79);
		put(m, "dark turquoise", 0, 206, 209);
		put(m, "dark violet", 148, 0, 211);

		put(m, "deep pink", 255, 20, 147);
		put(m, "deep sky blue", 0, 191, 255);
		put(m, "dim gray", 105, 105, 105);
		put(m, "dodger blue", 30, 144, 255);

		put(m, "firebrick", 178, 34, 34);
		put(m, "floral white", 255, 250, 240);
		put(m, "forest green", 34, 139, 34);
		put(m, "fuchsia", 255, 0, 255);

		put(m, "gainsboro", 220, 220, 220);
		put(m, "ghost white", 248, 248, 255);
		put(m, "gold", 255, 215, 0);
		put(m, "goldenrod", 218, 165, 32);
		put(m, "gray", 128, 128, 128); // X11 gray
		put(m, "green", 0, 128, 0); // X11 green (note: different from CSS 'lime')
		put(m, "green yellow", 173, 255, 47);

		put(m, "honeydew", 240, 255, 240);
		put(m, "hot pink", 255, 105, 180);

		put(m, "indian red", 205, 92, 92);
		put(m, "indigo", 75, 0, 130);
		put(m, "ivory", 255, 255, 240);

		put(m, "khaki", 240, 230, 140);

		put(m, "lavender", 230, 230, 250);
		put(m, "lavender blush", 255, 240, 245);
		put(m, "lawn green", 124, 252, 0);
		put(m, "lemon chiffon", 255, 250, 205);
		put(m, "light blue", 173, 216, 230);
		put(m, "light coral", 240, 128, 128);
		put(m, "light cyan", 224, 255, 255);
		put(m, "light goldenrod yellow", 250, 250, 210);
		put(m, "light green", 144, 238, 144);
		put(m, "light gray", 211, 211, 211);
		put(m, "light pink", 255, 182, 193);
		put(m, "light salmon", 255, 160, 122);
		put(m, "light sea green", 32, 178, 170);
		put(m, "light sky blue", 135, 206, 250);
		put(m, "light slate gray", 119, 136, 153);
		put(m, "light steel blue", 176, 196, 222);
		put(m, "light yellow", 255, 255, 224);

		put(m, "lime", 0, 255, 0);
		put(m, "lime green", 50, 205, 50);
		put(m, "linen", 250, 240, 230);

		put(m, "magenta", 255, 0, 255);
		put(m, "maroon", 128, 0, 0);
		put(m, "medium aquamarine", 102, 205, 170);
		put(m, "medium blue", 0, 0, 205);
		put(m, "medium orchid", 186, 85, 211);
		put(m, "medium purple", 147, 112, 219);
		put(m, "medium sea green", 60, 179, 113);
		put(m, "medium slate blue", 123, 104, 238);
		put(m, "medium spring green", 0, 250, 154);
		put(m, "medium turquoise", 72, 209, 204);
		put(m, "medium violet red", 199, 21, 133);
		put(m, "midnight blue", 25, 25, 112);
		put(m, "mint cream", 245, 255, 250);
		put(m, "misty rose", 255, 228, 225);
		put(m, "moccasin", 255, 228, 181);

		put(m, "navajo white", 255, 222, 173);
		put(m, "navy", 0, 0, 128);

		put(m, "old lace", 253, 245, 230);
		put(m, "olive", 128, 128, 0);
		put(m, "olive drab", 107, 142, 35);
		put(m, "orange", 255, 165, 0);
		put(m, "orange red", 255, 69, 0);
		put(m, "orchid", 218, 112, 214);

		put(m, "pale goldenrod", 238, 232, 170);
		put(m, "pale green", 152, 251, 152);
		put(m, "pale turquoise", 175, 238, 238);
		put(m, "pale violet red", 219, 112, 147);
		put(m, "papaya whip", 255, 239, 213);
		put(m, "peach puff", 255, 218, 185);
		put(m, "peru", 205, 133, 63);
		put(m, "pink", 255, 192, 203);
		put(m, "plum", 221, 160, 221);
		put(m, "powder blue", 176, 224, 230);
		put(m, "purple", 128, 0, 128);

		put(m, "red", 255, 0, 0);
		put(m, "rosy brown", 188, 143, 143);
		put(m, "royal blue", 65, 105, 225);

		put(m, "saddle brown", 139, 69, 19);
		put(m, "salmon", 250, 128, 114);
		put(m, "sandy brown", 244, 164, 96);
		put(m, "sea green", 46, 139, 87);
		put(m, "seashell", 255, 245, 238);
		put(m, "sienna", 160, 82, 45);
		put(m, "silver", 192, 192, 192);
		put(m, "sky blue", 135, 206, 235);
		put(m, "slate blue", 106, 90, 205);
		put(m, "slate gray", 112, 128, 144);
		put(m, "snow", 255, 250, 250);
		put(m, "spring green", 0, 255, 127);
		put(m, "steel blue", 70, 130, 180);

		put(m, "tan", 210, 180, 140);
		put(m, "teal", 0, 128, 128);
		put(m, "thistle", 216, 191, 216);
		put(m, "tomato", 255, 99, 71);
		put(m, "turquoise", 64, 224, 208);

		put(m, "violet", 238, 130, 238);

		put(m, "wheat", 245, 222, 179);
		put(m, "white", 255, 255, 255);
		put(m, "white smoke", 245, 245, 245);

		put(m, "yellow", 255, 255, 0);
		put(m, "yellow green", 154, 205, 50);

		COLORS = Collections.unmodifiableMap(m);
	}

	private X11Colors() {
		// utility class; no instances
	}

	private static void put(Map<String, Color> map, String name, int r, int g, int b) {
		map.put(normalizeName(name), new Color(r, g, b));
	}

	private static String normalizeName(String name) {
		// lower-case, trim, collapse internal whitespace to single spaces
		String trimmed = name.trim().toLowerCase();
		return trimmed.replaceAll("\\s+", " ");
	}

	/**
	 * Gets an X11 color and sets the alpha.
	 *
	 * @param name  the name of the color
	 * @param alpha 0..255 (255 is fully opaque)
	 * @return a new Color with the given alpha, or null if not found
	 */
	public static Color getX11Color(String name, int alpha) {
		Color base = getX11Color(name);
		if (base == null) {
			return null;
		}
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}

	/**
	 * Gets the named color from the X11 color list.
	 *
	 * @param name the name of the X11 color
	 * @return the named color, or null if not found
	 */
	public static Color getX11Color(String name) {
		if (name == null) {
			return null;
		}
		return COLORS.get(normalizeName(name));
	}

	/**
	 * Returns an unmodifiable view of all available X11 colors. Keys are normalized
	 * (lowercase, single spaces).
	 */
	public static Map<String, Color> getAllColors() {
		return COLORS;
	}
}
