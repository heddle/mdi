package edu.cnu.mdi.util;

/**
 * Utility constants and helpers for rendering mathematical and Greek text using
 * Unicode code points instead of a custom font or markup.
 * <p>
 * The class exposes three things:
 * <ul>
 *   <li>A large set of named {@code String} constants for individual Unicode
 *       characters (Greek letters, sub/superscripts, operators, arrows, ...).</li>
 *   <li>{@link #specialCharReplace(String)}, which rewrites a small set of
 *       LaTeX-like escapes (e.g. {@code \alpha}, {@code \leq}) into the matching
 *       Unicode characters.</li>
 *   <li>{@link #getSuperscript(int, boolean)}, which renders an integer using
 *       superscript digit glyphs.</li>
 * </ul>
 * <p>
 * All members are {@code static}; the class is not meant to be instantiated.
 *
 * <h2>Implementation notes</h2>
 * The replacement set and the superscript digits are kept in small lookup tables
 * ({@link #SPECIAL_CHAR_REPLACEMENTS} and {@link #SUPERSCRIPT_DIGITS}) rather than
 * being hard-coded into the method bodies. The public constants and method
 * signatures are unchanged, so this is a drop-in replacement.
 */
public final class UnicodeUtils {

	// subscripts and superscripts
	public static final String SUB0 = "₀";
	public static final String SUB1 = "₁";
	public static final String SUB2 = "₂";
	public static final String SUB3 = "₃";
	public static final String SUB4 = "₄";
	public static final String SUB5 = "₅";
	public static final String SUB6 = "₆";
	public static final String SUB7 = "₇";
	public static final String SUB8 = "₈";
	public static final String SUB9 = "₉";
	public static final String SUPER0 = "⁰";
	public static final String SUPER1 = "¹";
	public static final String SUPER2 = "²";
	public static final String SUPER3 = "³";
	public static final String SUPER4 = "⁴";
	public static final String SUPER5 = "⁵";
	public static final String SUPER6 = "⁶";
	public static final String SUPER7 = "⁷";
	public static final String SUPER8 = "⁸";
	public static final String SUPER9 = "⁹";

	public static final String SUPERN = "ⁿ";
	public static final String SUBN = "ₙ";

	public static final String LOG10 = "log₁₀";

	// erase left
	public static final String BACKSPACE = "⌫";

	// enter
	public static final String ENTER = "↵";

	public static final String SMILEYFACE = "☺";
	public static final String OVERLINE = "\u0305";

	public static final String THINSPACE = "\u2009";

	public static final String SUPERPLUS = "⁺";
	public static final String SUPERMINUS = "⁻";
	public static final String SUBMINUS = "₋";
	public static final String DEGREE = "°";
	public static final String TIMES = "×";
	public static final String PLUSMINUS = "±";
	public static final String APPROX = "≈";
	public static final String BULLET = "∙";
	public static final String DAGGER = "†";

	public static final String LEQ = "≤";
	public static final String GEQ = "≥";
	public static final String LL = "≪"; // much less than
	public static final String GG = "≫"; // much greater than
	public static final String PROPTO = "∝";
	public static final String EQUIV = "≡";
	public static final String SIM = "∼";
	public static final String SIMEQ = "≃";
	public static final String NEQ = "≠";
	public static final String PERP = "⊥";
	public static final String INTEGRAL = "∫";
	public static final String PARALLEL = "∥";
	public static final String INFINITY = "∞";
	public static final String LARROW = "←";
	public static final String UARROW = "↑";
	public static final String RARROW = "→";
	public static final String DARROW = "↓";
	public static final String LRARROW = "↔";
	public static final String UDARROW = "↕";

	// greek characters
	public static final String CAPITAL_ALPHA = "Α";
	public static final String CAPITAL_BETA = "Β";
	public static final String CAPITAL_GAMMA = "Γ";
	public static final String CAPITAL_DELTA = "Δ";
	public static final String CAPITAL_EPSILON = "Ε";
	public static final String CAPITAL_ZETA = "Ζ";
	public static final String CAPITAL_ETA = "Η";
	public static final String CAPITAL_THETA = "Θ";
	public static final String CAPITAL_IOTA = "Ι";
	public static final String CAPITAL_KAPPA = "Κ";
	public static final String CAPITAL_LAMBDA = "Λ";
	public static final String CAPITAL_MU = "Μ";
	public static final String CAPITAL_NU = "Ν";
	public static final String CAPITAL_XI = "Ξ";
	public static final String CAPITAL_OMICRON = "Ο";
	public static final String CAPITAL_PI = "Π";
	public static final String CAPITAL_RHO = "Ρ";
	public static final String CAPITAL_SIGMA = "Σ";
	public static final String CAPITAL_TAU = "Τ";
	public static final String CAPITAL_UPSILON = "Υ";
	public static final String CAPITAL_PHI = "Φ";
	public static final String CAPITAL_CHI = "Χ";
	public static final String CAPITAL_PSI = "Ψ";
	public static final String CAPITAL_OMEGA = "Ω";
	public static final String CAPITAL_IOTA_WITH_DIALYTIKA = "Ϊ";
	public static final String CAPITAL_UPSILON_WITH_DIALYTIKA = "Ϋ";
	public static final String SMALL_ALPHA_WITH_TONOS = "ά";
	public static final String SMALL_EPSILON_WITH_TONOS = "έ";
	public static final String SMALL_ETA_WITH_TONOS = "ή";
	public static final String SMALL_IOTA_WITH_TONOS = "ί";
	public static final String SMALL_ALPHA = "α";
	public static final String SMALL_BETA = "β";
	public static final String SMALL_GAMMA = "γ";
	public static final String SMALL_DELTA = "δ";
	public static final String SMALL_EPSILON = "ε";
	public static final String SMALL_ZETA = "ζ";
	public static final String SMALL_ETA = "η";
	public static final String SMALL_THETA = "θ";
	public static final String SMALL_IOTA = "ι";
	public static final String SMALL_KAPPA = "κ";
	public static final String SMALL_LAMBDA = "λ";
	public static final String SMALL_MU = "μ";
	public static final String SMALL_NU = "ν";
	public static final String SMALL_XI = "ξ";
	public static final String SMALL_OMICRON = "ο";
	public static final String SMALL_PI = "π";
	public static final String SMALL_RHO = "ρ";
	public static final String SMALL_FINAL_SIGMA = "ς";
	public static final String SMALL_SIGMA = "σ";
	public static final String SMALL_TAU = "τ";
	public static final String SMALL_UPSILON = "υ";
	public static final String SMALL_PHI = "φ";
	public static final String SMALL_CHI = "χ";
	public static final String SMALL_PSI = "ψ";
	public static final String SMALL_OMEGA = "ω";
	public static final String SMALL_IOTA_WITH_DIALYTIKA = "ϊ";
	public static final String SMALL_UPSILON_WITH_DIALYTIKA = "ϋ";
	public static final String SMALL_OMICRON_WITH_TONOS = "ό";
	public static final String SMALL_UPSILON_WITH_TONOS = "ύ";
	public static final String SMALL_OMEGA_WITH_TONOS = "ώ";

	/**
	 * Ordered {@code {escape, replacement}} table used by
	 * {@link #specialCharReplace(String)}.
	 * <p>
	 * Order matters: when one escape is a prefix of another (for example
	 * {@code \sim} is a prefix of {@code \simeq}), the longer escape must appear
	 * first, otherwise the shorter one consumes part of it. The original code
	 * listed {@code \sim} before {@code \simeq}, which meant {@code \simeq} could
	 * never match; that ordering is corrected here.
	 */
	private static final String[][] SPECIAL_CHAR_REPLACEMENTS = {
		// Greek letters (capital then small)
		{ "\\Alpha", CAPITAL_ALPHA }, { "\\alpha", SMALL_ALPHA },
		{ "\\Beta", CAPITAL_BETA }, { "\\beta", SMALL_BETA },
		{ "\\Gamma", CAPITAL_GAMMA }, { "\\gamma", SMALL_GAMMA },
		{ "\\Delta", CAPITAL_DELTA }, { "\\delta", SMALL_DELTA },
		{ "\\Epsilon", CAPITAL_EPSILON }, { "\\epsilon", SMALL_EPSILON },
		{ "\\Zeta", CAPITAL_ZETA }, { "\\zeta", SMALL_ZETA },
		{ "\\Eta", CAPITAL_ETA }, { "\\eta", SMALL_ETA },
		{ "\\Theta", CAPITAL_THETA }, { "\\theta", SMALL_THETA },
		{ "\\Iota", CAPITAL_IOTA }, { "\\iota", SMALL_IOTA },
		{ "\\Kappa", CAPITAL_KAPPA }, { "\\kappa", SMALL_KAPPA },
		{ "\\Lambda", CAPITAL_LAMBDA }, { "\\lambda", SMALL_LAMBDA },
		{ "\\Mu", CAPITAL_MU }, { "\\mu", SMALL_MU },
		{ "\\Nu", CAPITAL_NU }, { "\\nu", SMALL_NU },
		{ "\\Xi", CAPITAL_XI }, { "\\xi", SMALL_XI },
		{ "\\Omicron", CAPITAL_OMICRON }, { "\\omicron", SMALL_OMICRON },
		{ "\\Pi", CAPITAL_PI }, { "\\pi", SMALL_PI },
		{ "\\Rho", CAPITAL_RHO }, { "\\rho", SMALL_RHO },
		{ "\\Sigma", CAPITAL_SIGMA }, { "\\sigma", SMALL_SIGMA },
		{ "\\Tau", CAPITAL_TAU }, { "\\tau", SMALL_TAU },
		{ "\\Upsilon", CAPITAL_UPSILON }, { "\\upsilon", SMALL_UPSILON },
		{ "\\Phi", CAPITAL_PHI }, { "\\phi", SMALL_PHI },
		{ "\\Chi", CAPITAL_CHI }, { "\\chi", SMALL_CHI },
		{ "\\Psi", CAPITAL_PSI }, { "\\psi", SMALL_PSI },
		{ "\\Omega", CAPITAL_OMEGA }, { "\\omega", SMALL_OMEGA },

		// math symbols
		{ "\\times", TIMES },
		{ "\\degree", DEGREE },
		{ "\\pm", PLUSMINUS },
		{ "\\approx", APPROX },
		{ "\\bullet", BULLET },
		{ "\\leq", LEQ },
		{ "\\geq", GEQ },
		{ "\\ll", LL },
		{ "\\gg", GG },
		{ "\\propto", PROPTO },
		{ "\\equiv", EQUIV },
		{ "\\simeq", SIMEQ }, // must precede "\\sim" (prefix conflict)
		{ "\\sim", SIM },
		{ "\\neq", NEQ },
		{ "\\perp", PERP },
		{ "\\parallel", PARALLEL },
		{ "\\infinity", INFINITY },
		{ "\\larrow", LARROW },
		{ "\\uarrow", UARROW },
		{ "\\rarrow", RARROW },
		{ "\\darrow", DARROW },
		{ "\\lrarrow", LRARROW },
		{ "\\udarrow", UDARROW },
		{ "\\dagger", DAGGER },
	};

	/** Superscript glyphs indexed by digit value, i.e. {@code SUPERSCRIPT_DIGITS[7]} is {@code ⁷}. */
	private static final String[] SUPERSCRIPT_DIGITS = {
		SUPER0, SUPER1, SUPER2, SUPER3, SUPER4,
		SUPER5, SUPER6, SUPER7, SUPER8, SUPER9
	};

	/** Not instantiable. */
	private UnicodeUtils() {
	}

	/**
	 * Replace the LaTeX-like escape sequences in the given string with their
	 * Unicode equivalents.
	 * <p>
	 * Recognized escapes include the Greek letters ({@code \alpha}, {@code \Omega},
	 * ...) and a selection of math operators and arrows ({@code \leq},
	 * {@code \approx}, {@code \rarrow}, ...). See
	 * {@link #SPECIAL_CHAR_REPLACEMENTS} for the full set. Unrecognized text,
	 * including unknown backslash sequences, is left untouched.
	 * <p>
	 * Replacement is purely textual and applied in table order, so an escape that
	 * forms the prefix of a longer one is handled before the shorter one.
	 *
	 * @param s the input string; may be {@code null}
	 * @return {@code null} if {@code s} is {@code null}; otherwise {@code s} with
	 *         every recognized escape replaced by its Unicode character
	 */
	public static String specialCharReplace(String s) {
		if (s == null) {
			return null;
		}

		// Fast path: nothing to do if there is no backslash to begin an escape.
		if (s.indexOf('\\') < 0) {
			return s;
		}

		for (String[] replacement : SPECIAL_CHAR_REPLACEMENTS) {
			s = s.replace(replacement[0], replacement[1]);
		}
		return s;
	}

	/**
	 * Render an integer's digits as Unicode superscript characters.
	 * <p>
	 * The sign is supplied separately via {@code isNegative} rather than being
	 * read from {@code n}; callers normally pass the magnitude in {@code n}. Only
	 * the decimal digits {@code 0}-{@code 9} are converted, so any non-digit
	 * character in the textual form of {@code n} (such as a leading minus from a
	 * negative value) is ignored.
	 *
	 * @param n          the number whose digits are rendered as superscripts
	 * @param isNegative if {@code true}, a superscript minus sign is prepended
	 * @return the superscript representation, e.g. {@code getSuperscript(12, true)}
	 *         returns {@code "⁻¹²"}
	 */
	public static String getSuperscript(int n, boolean isNegative) {
		StringBuilder sb = new StringBuilder();
		if (isNegative) {
			sb.append(SUPERMINUS);
		}
		String numStr = Integer.toString(n);
		for (int i = 0; i < numStr.length(); i++) {
			char c = numStr.charAt(i);
			if (c >= '0' && c <= '9') {
				sb.append(SUPERSCRIPT_DIGITS[c - '0']);
			}
		}
		return sb.toString();
	}

}