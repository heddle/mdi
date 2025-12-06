package edu.cnu.mdi.feedback;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.text.SimpleAttributeSet;

import edu.cnu.mdi.component.TextPaneScrollPane;
import edu.cnu.mdi.ui.colors.X11Colors;

/**
 * A scrollable pane specialized for displaying mouse-over feedback in the MDI
 * application.
 * <p>
 * This class is a "low-tech" alternative to a heads-up display (HUD). It
 * renders feedback lines with simple styling rules driven by prefixes in the
 * feedback text:
 * </p>
 *
 * <ul>
 *   <li><code>"$mono$"</code> — renders the remainder of the line using a small
 *       monospaced style (e.g. for coordinate dumps or debug info).</li>
 *   <li><code>"$colorName$"</code> — renders the remainder of the line using
 *       the specified X11 color (e.g. <code>"$red$hit"</code>,
 *       <code>"$dark blue$selection"</code>).</li>
 *   <li>No prefix — renders using the default feedback style.</li>
 * </ul>
 *
 * <p>
 * Styles for X11 colors are cached so subsequent lines with the same color
 * reuse the same {@link SimpleAttributeSet}.
 * </p>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class FeedbackPane extends TextPaneScrollPane {

    /**
     * Default font size for feedback text.
     */
    private static final int DEFAULT_FONT_SIZE = 10;

    /**
     * Background color for the feedback pane.
     */
    private static final Color DEFAULT_BACKGROUND = Color.black;

    /**
     * Cache of styles keyed by lower-case X11 color names.
     * <p>
     * For example, the key {@code "red"} maps to a style that renders cyan text
     * on the configured background.
     * </p>
     */
    private final Map<String, SimpleAttributeSet> styleCache = new ConcurrentHashMap<>(101);

    /**
     * Default style: cyan text, black background, SansSerif, bold, non-italic.
     */
    public static final SimpleAttributeSet DEFAULT_STYLE =
            createStyle(Color.cyan, DEFAULT_BACKGROUND, "SansSerif", DEFAULT_FONT_SIZE, false, true);

    /**
     * Small monospaced style: cyan text, black background, monospaced font,
     * small size, bold, non-italic.
     */
    public static final SimpleAttributeSet SMALL_MONO_STYLE =
            createStyle(Color.cyan, DEFAULT_BACKGROUND, "Monospaced", 6, false, true);

    /**
     * Constructs a {@code FeedbackPane} with an etched and line border and a
     * dark background suitable for overlay-style feedback.
     */
    public FeedbackPane() {
        super(DEFAULT_BACKGROUND);

        Border etchedBorder = BorderFactory.createEtchedBorder();
        Border lineBorder = BorderFactory.createLineBorder(Color.black, 2);
        setBorder(BorderFactory.createCompoundBorder(etchedBorder, lineBorder));
    }

    /**
     * Ensures the message text ends with a newline so that appended messages
     * each appear on their own line.
     *
     * @param message the message text, may be {@code null}
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
     * Appends a feedback line using the simple prefix-based styling rules.
     * <p>
     * Rules:
     * </p>
     * <ul>
     *   <li>If the message begins with <code>"$mono$"</code>, the prefix is
     *       stripped and the remainder is rendered using the small monospaced
     *       style.</li>
     *   <li>If the message begins with <code>"$colorName$"</code>, where
     *       {@code colorName} is a valid X11 color name, the remainder is
     *       rendered using a style derived from that color. Styles are cached
     *       per color name.</li>
     *   <li>Otherwise, the message is rendered using
     *       {@link #DEFAULT_STYLE}.</li>
     * </ul>
     *
     * @param message the message text (may contain styling prefixes).
     */
    @Override
    public void append(String message) {
        if (message == null) {
            return;
        }

        // Special-case monospaced small style
        if (message.startsWith("$mono$")) {
            appendSmallMono(message.substring(6));
            return;
        }

        SimpleAttributeSet style = null;

        // Look for $colorName$ prefix
        if (message.startsWith("$")) {
            int nextIndex = message.indexOf("$", 1);
            if (nextIndex > 3 && nextIndex < 30) {
                String x11ColorName = message.substring(1, nextIndex).toLowerCase();
                message = message.substring(nextIndex + 1);

                style = styleCache.get(x11ColorName);
                if (style == null) {
                    Color color = X11Colors.getX11Color(x11ColorName);
                    if (color != null) {
                        style = createStyle(color, DEFAULT_BACKGROUND, "SansSerif",
                                            DEFAULT_FONT_SIZE, false, true);
                        styleCache.put(x11ColorName, style);
                    }
                }
            }
        }

        append((style == null) ? DEFAULT_STYLE : style, message);
    }

    /**
     * Appends a line using the {@link #SMALL_MONO_STYLE} style.
     *
     * @param message the message text (without any styling prefixes).
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
     * Replaces the current content with the provided feedback strings and
     * appends each one using {@link #append(String)}. Called by a FeedbackControl
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
