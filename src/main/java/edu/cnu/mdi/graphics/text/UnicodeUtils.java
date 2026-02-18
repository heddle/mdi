package edu.cnu.mdi.graphics.text;

public class UnicodeUtils {

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
	public static final String TIMES = "✕";
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
	public static final String SIM = "∶";
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
	 * Replace the Latex-like special characters with their unicode equivalents.
	 *
	 * @param s the input string
	 * @return the output, where special character sequences are replaced by unicode
	 *         characters.
	 */
	public static String specialCharReplace(String s) {
		if (s == null) {
			return null;
		}

		if (s.indexOf("\\") < 0) {
			return s;
		}

		s = s.replace("\\Alpha", CAPITAL_ALPHA);
		s = s.replace("\\alpha", SMALL_ALPHA);

		s = s.replace("\\Beta", CAPITAL_BETA);
		s = s.replace("\\beta", SMALL_BETA);

		s = s.replace("\\Gamma", CAPITAL_GAMMA);
		s = s.replace("\\gamma", SMALL_GAMMA);

		s = s.replace("\\Delta", CAPITAL_DELTA);
		s = s.replace("\\delta", SMALL_DELTA);

		s = s.replace("\\Epsilon", CAPITAL_EPSILON);
		s = s.replace("\\epsilon", SMALL_EPSILON);

		s = s.replace("\\Zeta", CAPITAL_ZETA);
		s = s.replace("\\zeta", SMALL_ZETA);

		s = s.replace("\\Eta", CAPITAL_ETA);
		s = s.replace("\\eta", SMALL_ETA);

		s = s.replace("\\Theta", CAPITAL_THETA);
		s = s.replace("\\theta", SMALL_THETA);

		s = s.replace("\\Iota", CAPITAL_IOTA);
		s = s.replace("\\iota", SMALL_IOTA);

		s = s.replace("\\Kappa", CAPITAL_KAPPA);
		s = s.replace("\\kappa", SMALL_KAPPA);

		s = s.replace("\\Lambda", CAPITAL_LAMBDA);
		s = s.replace("\\lambda", SMALL_LAMBDA);

		s = s.replace("\\Mu", CAPITAL_MU);
		s = s.replace("\\mu", SMALL_MU);

		s = s.replace("\\Nu", CAPITAL_NU);
		s = s.replace("\\nu", SMALL_NU);

		s = s.replace("\\Xi", CAPITAL_XI);
		s = s.replace("\\xi", SMALL_XI);

		s = s.replace("\\Omicron", CAPITAL_OMICRON);
		s = s.replace("\\omicron", SMALL_OMICRON);

		s = s.replace("\\Pi", CAPITAL_PI);
		s = s.replace("\\pi", SMALL_PI);

		s = s.replace("\\Rho", CAPITAL_RHO);
		s = s.replace("\\rho", SMALL_RHO);

		s = s.replace("\\Sigma", CAPITAL_SIGMA);
		s = s.replace("\\sigma", SMALL_SIGMA);

		s = s.replace("\\Tau", CAPITAL_TAU);
		s = s.replace("\\tau", SMALL_TAU);

		s = s.replace("\\Upsilon", CAPITAL_UPSILON);
		s = s.replace("\\upsilon", SMALL_UPSILON);

		s = s.replace("\\Phi", CAPITAL_PHI);
		s = s.replace("\\phi", SMALL_PHI);

		s = s.replace("\\Chi", CAPITAL_CHI);
		s = s.replace("\\chi", SMALL_CHI);

		s = s.replace("\\Psi", CAPITAL_PSI);
		s = s.replace("\\psi", SMALL_PSI);

		s = s.replace("\\Omega", CAPITAL_OMEGA);
		s = s.replace("\\omega", SMALL_OMEGA);

		// some math symbols
		s = s.replace("\\times", TIMES);
		s = s.replace("\\degree", DEGREE);
		s = s.replace("\\pm", PLUSMINUS);
		s = s.replace("\\approx", APPROX);
		s = s.replace("\\bullet", BULLET);

		s = s.replace("\\leq", LEQ);
		s = s.replace("\\geq", GEQ);
		s = s.replace("\\ll", LL);
		s = s.replace("\\gg", GG);
		s = s.replace("\\propto", PROPTO);
		s = s.replace("\\equiv", EQUIV);
		s = s.replace("\\sim", SIM);
		s = s.replace("\\simeq", SIMEQ);
		s = s.replace("\\neq", NEQ);
		s = s.replace("\\perp", PERP);
		s = s.replace("\\parallel", PARALLEL);
		s = s.replace("\\infinity", INFINITY);

		s = s.replace("\\larrow", LARROW);
		s = s.replace("\\uarrow", UARROW);
		s = s.replace("\\rarrow", RARROW);
		s = s.replace("\\darrow", DARROW);
		s = s.replace("\\lrarrow", LRARROW);
		s = s.replace("\\udarrow", UDARROW);

		s = s.replace("\\dagger", DAGGER);
		return s;
	}

	/**
	 * Get the superscript string for the given integer.
	 *
	 * @param n the integer
	 * @return the superscript string
	 */
	public static String getSuperscript(int n, boolean isNegative) {
		StringBuilder sb = new StringBuilder();
		String numStr = Integer.toString(n);
		for (int i = 0; i < numStr.length(); i++) {
			char c = numStr.charAt(i);
			switch (c) {
			case '0':
				sb.append(SUPER0);
				break;
			case '1':
				sb.append(SUPER1);
				break;
			case '2':
				sb.append(SUPER2);
				break;
			case '3':
				sb.append(SUPER3);
				break;
			case '4':
				sb.append(SUPER4);
				break;
			case '5':
				sb.append(SUPER5);
				break;
			case '6':
				sb.append(SUPER6);
				break;
			case '7':
				sb.append(SUPER7);
				break;
			case '8':
				sb.append(SUPER8);
				break;
			case '9':
				sb.append(SUPER9);
				break;
			}
		}
		if (isNegative) {
			sb.insert(0, SUPERMINUS);
		}
		return sb.toString();
	}

}