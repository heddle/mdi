package edu.cnu.mdi.component;

import java.awt.Color;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import edu.cnu.mdi.format.DateString;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.ui.colors.X11Colors;

/**
 * A {@link JScrollPane} that owns a non-editable {@link JTextPane} using a
 * {@link StyledDocument} and a collection of convenience styles for terminal /
 * status-like output.
 * <p>
 * This class is also written to behave well under the Nimbus look &amp; feel:
 * the scroll pane and its viewport are opaque and painted with the configured
 * background color, while the embedded {@code JTextPane} is non-opaque with a
 * transparent background. This avoids Nimbus painting only behind the text
 * while leaving the rest of the area white.
 * </p>
 *
 * <p>
 * Typical usage:
 * </p>
 *
 * <pre>
 * TextPaneScrollPane pane = new TextPaneScrollPane("Feedback", Color.BLACK);
 * pane.append("Hello world\n", TextPaneScrollPane.CYAN_TERMINAL);
 * </pre>
 */
@SuppressWarnings("serial")
public class TextPaneScrollPane extends JScrollPane {

	/** Fully transparent color used for text pane backgrounds. */
	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

	// ------------------------------------------------------------------------
	// Common style definitions
	// ------------------------------------------------------------------------

	/** Small blue monospaced style on a light blue background. */
	public static final SimpleAttributeSet BLUE_M_10_B = createStyle(Color.blue, X11Colors.getX11Color("Alice Blue"),
			"monospaced", 10, false, true);

	/** Red monospaced terminal style on a transparent background. */
	public static final SimpleAttributeSet RED_TERMINAL = createStyle(Color.red, TRANSPARENT, "monospaced", 11, false,
			true);

	/** Yellow monospaced terminal style on a transparent background. */
	public static final SimpleAttributeSet YELLOW_TERMINAL = createStyle(Color.yellow, TRANSPARENT, "monospaced", 11,
			false, true);

	/** Cyan monospaced terminal style on a transparent background. */
	public static final SimpleAttributeSet CYAN_TERMINAL = createStyle(Color.cyan, TRANSPARENT, "monospaced", 11, false,
			true);

	/** Blue monospaced terminal style. */
	public static final SimpleAttributeSet BLUE_TERMINAL = createStyle(Color.blue, "monospaced", 12, false, true);

	/** Green monospaced terminal style on a transparent background. */
	public static final SimpleAttributeSet GREEN_TERMINAL = createStyle(X11Colors.getX11Color("Dark Green"),
			TRANSPARENT, "monospaced", 11, false, true);

	/** Blue bold sans-serif on a light background. */
	public static final SimpleAttributeSet BLUE_SS_12_B = createStyle(Color.blue, X11Colors.getX11Color("Alice Blue"),
			"sansserif", 12, false, true);

	/** Blue plain sans-serif. */
	public static final SimpleAttributeSet BLUE_SS_12_P = createStyle(Color.blue, "sansserif", 12, false, false);

	/** Dark green bold sans-serif on a wheat background. */
	public static final SimpleAttributeSet GREEN_SS_12_B = createStyle(X11Colors.getX11Color("Dark Green"),
			X11Colors.getX11Color("wheat"), "sansserif", 12, false, true);

	/** Red plain sans-serif. */
	public static final SimpleAttributeSet RED_SS_12_P = createStyle(Color.red, "sansserif", 12, false, false);

	/** Dark green plain sans-serif on a wheat background. */
	public static final SimpleAttributeSet GREEN_SS_12_P = createStyle(X11Colors.getX11Color("Dark Green"),
			X11Colors.getX11Color("wheat"), "sansserif", 12, false, false);

	/** Black plain sans-serif. */
	public static final SimpleAttributeSet BLACK_SS_12_P = createStyle(Color.black, "sansserif", 12, false, false);

	/** Small blue bold sans-serif on a light background. */
	public static final SimpleAttributeSet BLUE_SS_10_B = createStyle(Color.blue, X11Colors.getX11Color("Alice Blue"),
			"sansserif", 10, false, true);

	/** Small red plain sans-serif. */
	public static final SimpleAttributeSet RED_SS_10_P = createStyle(Color.red, "sansserif", 10, false, false);

	/** Small green plain sans-serif on a yellow background. */
	public static final SimpleAttributeSet GREEN_SS_10_P = createStyle(X11Colors.getX11Color("Dark Green"),
			Color.yellow, "sansserif", 10, false, false);

	/** Small black plain sans-serif. */
	public static final SimpleAttributeSet BLACK_SS_10_P = createStyle(Color.black, "sansserif", 10, false, false);

	// ------------------------------------------------------------------------
	// Instance fields
	// ------------------------------------------------------------------------

	/** The non-editable text pane hosted by this scroll pane. */
	protected JTextPane textPane;

	/** The styled document backing the text pane. */
	protected StyledDocument document;

	/** Default style used when no explicit style is supplied. */
	protected SimpleAttributeSet defaultStyle = BLACK_SS_12_P;

	// ------------------------------------------------------------------------
	// Constructors
	// ------------------------------------------------------------------------

	/**
	 * Creates a new {@code TextPaneScrollPane} with a white background and no
	 * border label.
	 */
	public TextPaneScrollPane() {
		this(null, Color.white);
	}

	/**
	 * Creates a new {@code TextPaneScrollPane} with no border label.
	 *
	 * @param bgColor the background color to use; if {@code null}, white is used
	 */
	public TextPaneScrollPane(Color bgColor) {
		this(null, bgColor);
	}

	/**
	 * Creates a new {@code TextPaneScrollPane}, optionally with a labeled border.
	 *
	 * @param label   optional border label; if {@code null}, no labeled border is
	 *                installed
	 * @param bgColor the background color to use; if {@code null}, white is used
	 */
	public TextPaneScrollPane(String label, Color bgColor) {
		super();

		if (bgColor == null) {
			bgColor = Color.white;
		}

		if (label != null) {
			setBorder(new CommonBorder(label));
		}

		createTextPane();

		// Nimbus-friendly background behavior:
		// - The scroll pane and viewport are opaque and painted with bgColor.
		// - The text pane is transparent so the bgColor shows everywhere.
		setOpaque(true);
		setBackground(bgColor);

		getViewport().setOpaque(true);
		getViewport().setBackground(bgColor);

		textPane.setOpaque(false);
		textPane.setBackground(TRANSPARENT);

		getViewport().add(textPane);
	}

	// ------------------------------------------------------------------------
	// Style creation helpers
	// ------------------------------------------------------------------------

	/**
	 * Creates a style with the given characteristics and a default white background
	 * and spacing.
	 *
	 * @param fg         the foreground color
	 * @param fontFamily the font family to use
	 * @param fontSize   the font size to use
	 * @param italic     {@code true} for italic text
	 * @param bold       {@code true} for bold text
	 * @return the created style
	 */
	public static SimpleAttributeSet createStyle(Color fg, String fontFamily, int fontSize, boolean italic,
			boolean bold) {
		return createStyle(fg, Color.white, fontFamily, fontSize, italic, bold, false, 0, 2);
	}

	/**
	 * Creates a style with the given foreground and background colors and default
	 * spacing.
	 *
	 * @param fg         the foreground color
	 * @param bg         the background color
	 * @param fontFamily the font family to use
	 * @param fontSize   the font size to use
	 * @param italic     {@code true} for italic text
	 * @param bold       {@code true} for bold text
	 * @return the created style
	 */
	public static SimpleAttributeSet createStyle(Color fg, Color bg, String fontFamily, int fontSize, boolean italic,
			boolean bold) {
		return createStyle(fg, bg, fontFamily, fontSize, italic, bold, false, 0, 2);
	}

	/**
	 * Creates a fully specified style.
	 *
	 * @param fg         the foreground color
	 * @param bg         the background color
	 * @param fontFamily the font family to use
	 * @param fontSize   the font size to use
	 * @param italic     {@code true} for italic text
	 * @param bold       {@code true} for bold text
	 * @param underline  {@code true} to underline
	 * @param spaceAbove additional space above the paragraph, in points
	 * @param spaceBelow additional space below the paragraph, in points
	 * @return the created style
	 */
	public static SimpleAttributeSet createStyle(Color fg, Color bg, String fontFamily, int fontSize, boolean italic,
			boolean bold, boolean underline, int spaceAbove, int spaceBelow) {
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setForeground(style, fg);
		StyleConstants.setBackground(style, bg);
		StyleConstants.setFontFamily(style, fontFamily);
		StyleConstants.setFontSize(style, fontSize);
		StyleConstants.setItalic(style, italic);
		StyleConstants.setBold(style, bold);
		StyleConstants.setUnderline(style, underline);
		StyleConstants.setSpaceAbove(style, spaceAbove);
		StyleConstants.setSpaceBelow(style, spaceBelow);
		return style;
	}

	// ------------------------------------------------------------------------
	// Internal setup
	// ------------------------------------------------------------------------

	/**
	 * Creates the backing {@link StyledDocument} and {@link JTextPane}.
	 */
	private void createTextPane() {
		StyleContext context = new StyleContext();
		document = new DefaultStyledDocument(context);
		textPane = new JTextPane(document);
		textPane.setEditable(false);
	}

	// ------------------------------------------------------------------------
	// Public API
	// ------------------------------------------------------------------------

	/**
	 * Forces a repaint of the underlying text pane.
	 */
	public void refresh() {
		if (textPane != null) {
			textPane.repaint();
		}
	}

	/**
	 * Appends plain text using the current {@link #defaultStyle}.
	 *
	 * @param text the text to append; {@code null} is ignored
	 */
	public void append(String text) {
		append(text, defaultStyle);
	}

	/**
	 * Appends text using a specific style.
	 *
	 * @param text  the text to append; {@code null} is ignored
	 * @param style the style to use (for example {@link #CYAN_TERMINAL})
	 */
	public void append(String text, AttributeSet style) {
		append(text, style, false);
	}

	/**
	 * Appends text using a specific style and optional timestamp.
	 *
	 * @param text      the text to append; {@code null} is ignored
	 * @param style     the style to use
	 * @param writeTime {@code true} to prepend a timestamp using
	 *                  {@link #BLUE_M_10_B}
	 */
	public void append(final String text, final AttributeSet style, final boolean writeTime) {
		baseAppend(text, style, writeTime);
	}

	/**
	 * Clears all text and resets the caret position to the beginning.
	 */
	public void clear() {
		if (textPane == null) {
			return;
		}
		textPane.setText(null);
		textPane.setCaretPosition(0);
	}

	/**
	 * Returns the current default style used by {@link #append(String)}.
	 *
	 * @return the default style
	 */
	public SimpleAttributeSet getDefaultStyle() {
		return defaultStyle;
	}

	/**
	 * Sets the default style used by {@link #append(String)}.
	 *
	 * @param defaultStyle the new default style
	 */
	public void setDefaultStyle(SimpleAttributeSet defaultStyle) {
		this.defaultStyle = defaultStyle;
	}

	// ------------------------------------------------------------------------
	// Internal helpers
	// ------------------------------------------------------------------------

	/**
	 * Internal implementation of append that performs the actual document insertion
	 * and caret update.
	 */
	private void baseAppend(final String text, final AttributeSet style, final boolean writeTime) {
		if ((text == null) || (document == null)) {
			return;
		}

		try {
			if (writeTime) {
				writeTime();
			}
			document.insertString(document.getLength(), text, style);
		} catch (BadLocationException e) {
			Log.getInstance().exception(e);
		}

		try {
			textPane.setCaretPosition(Math.max(0, document.getLength() - 1));
		} catch (Exception e) {
			// ignore; caret position is best-effort
		}
	}

	/**
	 * Writes a timestamp using {@link DateString#dateStringSS()} with a small blue
	 * monospaced style followed by two spaces.
	 */
	private void writeTime() {
		String s = DateString.dateStringSS();
		try {
			document.insertString(document.getLength(), s, BLUE_M_10_B);
			document.insertString(document.getLength(), "  ", BLACK_SS_12_P);
		} catch (BadLocationException e) {
			Log.getInstance().exception(e);
			e.printStackTrace();
		}
	}
}
