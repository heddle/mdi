package edu.cnu.mdi.feedback;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.text.SimpleAttributeSet;

import edu.cnu.mdi.component.TextPaneScrollPane;
import edu.cnu.mdi.ui.colors.X11Colors;

/**
 * A scrollable pane specialized for displaying mouse-over feedback in the MDI
 * application.
 *
 * <p>This class is a "low-tech" alternative to a heads-up display (HUD). It
 * renders feedback lines with simple styling rules driven by prefixes in the
 * feedback text:</p>
 *
 * <ul>
 *   <li>{@code "$mono$"} — renders the remainder of the line using a small
 *       monospaced style (e.g. for coordinate dumps or debug info).</li>
 *   <li>{@code "$colorName$"} — renders the remainder of the line using the
 *       specified X11 color (e.g. {@code "$red$hit"},
 *       {@code "$dark blue$selection"}).  See {@link #append(String)} for the
 *       full validation rules.</li>
 *   <li>No recognized prefix — renders using the default feedback style.</li>
 * </ul>
 *
 * <p>Styles for X11 colors are cached so subsequent lines with the same color
 * reuse the same {@link SimpleAttributeSet}.</p>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class FeedbackPane extends TextPaneScrollPane {

    /** Background color applied to all text styles. */
    private final Color bg;

    /** Base font size used for the default and color-named styles. */
    private final int fontSize;

    /**
     * Cache of styles keyed by lower-case X11 color names.
     *
     * <p>For example, the key {@code "red"} maps to a style that renders red
     * text on the configured background.</p>
     */
    private final Map<String, SimpleAttributeSet> styleCache =
            new ConcurrentHashMap<>(101);

    /** Default style: cyan text, configured background, SansSerif, bold. */
    private SimpleAttributeSet DEFAULT_STYLE;

    /**
     * Small monospaced style: cyan text, configured background, Monospaced
     * font, smaller size, bold.
     */
    private SimpleAttributeSet SMALL_MONO_STYLE;

    /**
     * Minimum color-name length (inclusive) accepted by the {@code $name$}
     * prefix parser.
     *
     * <p>No valid X11 color name is shorter than 2 characters (e.g. the
     * shortest is "red" at 3 chars, but we use 2 as the floor to be
     * permissive). Values shorter than this are treated as malformed.</p>
     */
    private static final int COLOR_NAME_MIN_LEN = 2;

    /**
     * Maximum color-name length (inclusive) accepted by the {@code $name$}
     * prefix parser.
     *
     * <p>The longest X11 color names are around 25 characters. A ceiling of
     * 40 characters is generous enough to cover all valid names while
     * preventing the parser from scanning arbitrarily far into a string that
     * happens to start with {@code $}.</p>
     */
    private static final int COLOR_NAME_MAX_LEN = 40;

    /**
     * Constructs a {@code FeedbackPane} with a dark background suitable for
     * overlay-style feedback. Uses cyan foreground and 9 pt font size.
     */
    public FeedbackPane() {
        this(Color.cyan, Color.black, 9);
    }

    /**
     * Constructs a {@code FeedbackPane} with explicit foreground color,
     * background color, and font size.
     *
     * @param fg       foreground color for the default and monospaced styles
     * @param bg       background color applied to all text styles and the pane
     * @param fontSize base font size in points; monospaced style uses a
     *                 reduced size derived from this value
     */
    public FeedbackPane(Color fg, Color bg, int fontSize) {
        super(bg);
        this.bg       = bg;
        this.fontSize = fontSize;

        DEFAULT_STYLE = createStyle(fg, bg, "SansSerif", fontSize, false, true);

        int smallFontSize = Math.max(6, fontSize - 3);
        SMALL_MONO_STYLE  = createStyle(fg, bg, "Monospaced", smallFontSize, false, true);
    }

    /**
     * Ensures the message text ends with a newline so that appended messages
     * each appear on their own line.
     *
     * @param message the message text; may be {@code null}
     * @return the message text, never {@code null}, always ending with
     *         {@code '\n'}
     */
    private String fixMessage(String message) {
        if (message == null) {
            return "";
        }
        if (!message.endsWith("\n")) {
            return message + "\n";
        }
        return message;
    }

    /**
     * Appends a feedback line, applying the simple prefix-based styling rules.
     *
     * <h3>Prefix syntax</h3>
     * <ul>
     *   <li>{@code "$mono$"} — the prefix is stripped and the remainder is
     *       rendered in the small monospaced style.</li>
     *   <li>{@code "$<name>$"} where {@code <name>} is a valid X11 color name
     *       (case-insensitive, {@value #COLOR_NAME_MIN_LEN}–{@value
     *       #COLOR_NAME_MAX_LEN} characters) — the prefix is stripped and the
     *       remainder is rendered using a style derived from that color.
     *       Styles are cached per lower-cased name.</li>
     *   <li>No recognized prefix, or an unrecognized / malformed prefix — the
     *       <em>entire, unmodified string</em> (including any {@code $…$}
     *       fragment) is rendered in the default style. This ensures malformed
     *       tags are visible to the developer rather than being silently
     *       swallowed.</li>
     * </ul>
     *
     * <h3>Validation rules for the color prefix</h3>
     * <ol>
     *   <li>The message must start with {@code $}.</li>
     *   <li>A closing {@code $} must appear such that the enclosed name is
     *       between {@value #COLOR_NAME_MIN_LEN} and
     *       {@value #COLOR_NAME_MAX_LEN} characters long.</li>
     *   <li>The enclosed name must resolve to a known X11 color via
     *       {@link X11Colors#getX11Color}; if it does not, the entire original
     *       string is rendered in the default style.</li>
     * </ol>
     *
     * <p>The {@code $mono$} special case is tested first and is exempt from
     * the length validation.</p>
     *
     * @param message the feedback line; ignored if {@code null}
     */
    @Override
    public void append(String message) {
        if (message == null) {
            return;
        }

        // Fast-path: monospaced small style.
        if (message.startsWith("$mono$")) {
            appendSmallMono(message.substring(6));
            return;
        }

        // Attempt color-name prefix detection.
        SimpleAttributeSet style = null;
        if (message.startsWith("$")) {
            int closingDollar = message.indexOf('$', 1);

            // The closing $ must exist and enclose a name of acceptable length.
            if (closingDollar >= 0) {
                int nameLen = closingDollar - 1; // characters between the two $
                if (nameLen >= COLOR_NAME_MIN_LEN && nameLen <= COLOR_NAME_MAX_LEN) {
                    String candidate = message.substring(1, closingDollar).toLowerCase();

                    style = styleCache.get(candidate);
                    if (style == null) {
                        Color color = X11Colors.getX11Color(candidate);
                        if (color != null) {
                            style = createStyle(color, bg, "SansSerif", fontSize,
                                    false, true);
                            styleCache.put(candidate, style);
                        }
                    }

                    if (style != null) {
                        // Recognized X11 color — strip the prefix and render
                        // only the content that follows it.
                        message = message.substring(closingDollar + 1);
                    }
                    // If style is still null the name was not a known X11 color.
                    // Fall through: render the full original string (including
                    // the $…$ tag) in DEFAULT_STYLE so the malformed tag is
                    // visible to the developer.
                }
            }
        }

        append((style == null) ? DEFAULT_STYLE : style, message);
    }

    /**
     * Appends a line using the {@link #SMALL_MONO_STYLE} style.
     *
     * @param message the message text (without any styling prefixes)
     */
    private void appendSmallMono(String message) {
        append(SMALL_MONO_STYLE, message);
    }

    /**
     * Appends the given message using the specified style.
     *
     * @param style   the style to apply; must not be {@code null}
     * @param message the message text; may be {@code null} (treated as empty)
     */
    public void append(SimpleAttributeSet style, String message) {
        append(fixMessage(message), style, false);
    }

    /**
     * Replaces the current content with the provided feedback strings, then
     * appends each one using {@link #append(String)}.
     *
     * <p>Called by {@link FeedbackControl} on every mouse-move update that
     * produces a changed set of feedback strings.</p>
     *
     * @param feedbackStrings the list of feedback strings to display; may be
     *                        {@code null} or empty, in which case the pane is
     *                        simply cleared
     */
    public void updateFeedback(List<String> feedbackStrings) {
        clear();

        if (feedbackStrings == null || feedbackStrings.isEmpty()) {
            return;
        }

        for (String s : feedbackStrings) {
            append(s);
        }
    }

    /**
     * Returns a short descriptive name for this component, useful for
     * debugging.
     *
     * @return the string {@code "FeedbackPane"}
     */
    @Override
    public String toString() {
        return "FeedbackPane";
    }
}