package edu.cnu.mdi.json;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;

import edu.cnu.mdi.component.TextPaneScrollPane;

/**
 * The left half of the {@link JsonSplitPane}: a read-only, syntax-colored
 * view of raw JSON text with incremental search highlighting.
 *
 * <h2>Design</h2>
 * <p>
 * Extends {@link TextPaneScrollPane} so all the Nimbus-safe background wiring,
 * {@link javax.swing.text.StyledDocument} setup, and
 * {@link #createStyle(Color, String, int, boolean, boolean)} factory method are
 * inherited without duplication.
 * </p>
 *
 * <h2>Colorization strategy</h2>
 * <p>
 * Text is inserted in one shot with the {@link #STYLE_PLAIN} base style, then
 * {@link JsonTokenPainter} makes a second pass to apply token-level color runs
 * using {@link StyledDocument#setCharacterAttributes}. Doing the insert first
 * and the coloring second keeps insertion fast regardless of file size —
 * {@code setCharacterAttributes} on an already-inserted document is much
 * cheaper than inserting with per-character attributes.
 * </p>
 *
 * <h2>Search highlighting</h2>
 * <p>
 * {@link #highlight(String, BiConsumer)} uses the {@link Highlighter} API
 * (layered on top of the {@link StyledDocument} coloring) so that syntax
 * colors are never disturbed by a search. Two highlight painter instances are
 * maintained: one for all matches (dim yellow) and one for the currently
 * selected match (bright orange). {@link #nextHighlight()} and
 * {@link #prevHighlight()} cycle through the match list and scroll the pane to
 * keep the current match visible.
 * </p>
 *
 * <h2>Color scheme</h2>
 * <p>Dark background ({@link #PANE_BG}) with the following token colors:</p>
 * <ul>
 *   <li><b>Keys</b> — cornflower blue, bold</li>
 *   <li><b>String values</b> — pale green</li>
 *   <li><b>Numbers</b> — gold</li>
 *   <li><b>Booleans</b> ({@code true}/{@code false}) — orange</li>
 *   <li><b>Null</b> — medium gray, italic</li>
 *   <li><b>Structural</b> ({@code { } [ ] , :}) — light gray</li>
 *   <li><b>Plain / unrecognised</b> — near-white</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class JsonRawPane extends TextPaneScrollPane {

    // -------------------------------------------------------------------------
    // Color palette
    // -------------------------------------------------------------------------

    /** Dark background used by the pane, its scroll pane, and its viewport. */
    static final Color PANE_BG = new Color(30, 30, 30);

    /** Near-white base foreground for unrecognised / plain text. */
    static final Color COLOR_PLAIN      = new Color(212, 212, 212);

    /** Cornflower blue for object keys. */
    static final Color COLOR_KEY        = new Color(100, 149, 237);

    /** Pale green for string values. */
    static final Color COLOR_STRING     = new Color(152, 195, 121);

    /** Gold for numeric values. */
    static final Color COLOR_NUMBER     = new Color(209, 154, 102);

    /** Orange for {@code true} and {@code false}. */
    static final Color COLOR_BOOLEAN    = new Color(229, 192, 123);

    /** Medium gray for {@code null}. */
    static final Color COLOR_NULL       = new Color(128, 128, 128);

    /** Light gray for structural characters ({@code { } [ ] , :}). */
    static final Color COLOR_STRUCTURAL = new Color(171, 178, 191);

    /** Red used for inline error messages. */
    private static final Color COLOR_ERROR = new Color(220, 80, 80);

    /** Dim yellow used for non-current search match highlights. */
    private static final Color COLOR_MATCH         = new Color(180, 160, 50, 120);

    /** Bright orange used for the currently selected search match. */
    private static final Color COLOR_MATCH_CURRENT = new Color(230, 120, 20, 200);

    // -------------------------------------------------------------------------
    // Shared styles  (package-visible so JsonTokenPainter can use them)
    // -------------------------------------------------------------------------

    /** Base style: near-white, monospaced 12 pt, not bold, not italic. */
    static final SimpleAttributeSet STYLE_PLAIN =
            createStyle(COLOR_PLAIN,      PANE_BG, "monospaced", 12, false, false);

    /** Key style: cornflower blue, bold. */
    static final SimpleAttributeSet STYLE_KEY =
            createStyle(COLOR_KEY,        PANE_BG, "monospaced", 12, false, true);

    /** String-value style: pale green. */
    static final SimpleAttributeSet STYLE_STRING =
            createStyle(COLOR_STRING,     PANE_BG, "monospaced", 12, false, false);

    /** Number style: gold. */
    static final SimpleAttributeSet STYLE_NUMBER =
            createStyle(COLOR_NUMBER,     PANE_BG, "monospaced", 12, false, false);

    /** Boolean style: orange. */
    static final SimpleAttributeSet STYLE_BOOLEAN =
            createStyle(COLOR_BOOLEAN,    PANE_BG, "monospaced", 12, false, false);

    /** Null style: gray, italic. */
    static final SimpleAttributeSet STYLE_NULL =
            createStyle(COLOR_NULL,       PANE_BG, "monospaced", 12, true,  false);

    /** Structural style: light gray. */
    static final SimpleAttributeSet STYLE_STRUCTURAL =
            createStyle(COLOR_STRUCTURAL, PANE_BG, "monospaced", 12, false, false);

    /** Error style: red, italic. */
    private static final SimpleAttributeSet STYLE_ERROR =
            createStyle(COLOR_ERROR,      PANE_BG, "monospaced", 12, true,  false);

    // -------------------------------------------------------------------------
    // Highlight painters (stateless — reused for every match)
    // -------------------------------------------------------------------------

    /** Painter for all non-current matches. */
    private static final Highlighter.HighlightPainter PAINTER_MATCH =
            new DefaultHighlighter.DefaultHighlightPainter(COLOR_MATCH);

    /** Painter for the currently selected match. */
    private static final Highlighter.HighlightPainter PAINTER_CURRENT =
            new DefaultHighlighter.DefaultHighlightPainter(COLOR_MATCH_CURRENT);

    // -------------------------------------------------------------------------
    // Search state
    // -------------------------------------------------------------------------

    /**
     * Offsets of all current match spans (start offset of each match).
     * Parallel to the highlight tags stored in the {@link Highlighter}.
     * Empty when no search is active or the query is blank.
     */
    private final List<Integer> matchOffsets = new ArrayList<>();

    /**
     * Highlight tag objects returned by {@link Highlighter#addHighlight}, in
     * the same order as {@link #matchOffsets}. Kept so we can swap the
     * "current" painter without re-running the full search.
     */
    private final List<Object> matchTags = new ArrayList<>();

    /**
     * Length of the current search query — needed when navigating to know
     * how wide each match is.
     */
    private int queryLength = 0;

    /**
     * Index into {@link #matchOffsets} of the currently selected match, or
     * {@code -1} when no match is selected.
     */
    private int currentMatchIndex = -1;

    /**
     * The callback supplied to the last {@link #highlight} call, used to
     * push count updates to {@link JsonSearchBar} after navigation.
     */
    private BiConsumer<Integer, Integer> countCallback = null;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Create a {@code JsonRawPane} with a dark background and no content.
     *
     * <p>The Nimbus-safe background wiring (opaque scroll pane + viewport,
     * transparent text pane) is handled by {@link TextPaneScrollPane}'s
     * constructor.</p>
     */
    public JsonRawPane() {
        super(PANE_BG);
        setDefaultStyle(STYLE_PLAIN);

        // Override the text-pane font so it starts monospaced even before
        // any text is inserted (affects line-height calculation on first show).
        if (textPane != null) {
            textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            textPane.setCaretColor(COLOR_PLAIN);
        }
    }

    // -------------------------------------------------------------------------
    // Public API — content
    // -------------------------------------------------------------------------

    /**
     * Display a raw JSON string with syntax highlighting.
     *
     * <p>The existing content is cleared, the raw text is inserted in one
     * operation using {@link #STYLE_PLAIN}, and then
     * {@link JsonTokenPainter#paint(StyledDocument, String)} applies
     * token-level color runs. The caret is reset to position 0 so the top of
     * the file is visible immediately.</p>
     *
     * <p>Must be called on the EDT.</p>
     *
     * @param json the raw JSON text to display; must not be {@code null}
     */
    public void showJson(String json) {
        clearHighlights();
        clear();
        StyledDocument doc = textPane.getStyledDocument();
        try {
            doc.insertString(0, json, STYLE_PLAIN);
        } catch (BadLocationException ex) {
            // Offset 0 on an empty document is always valid — cannot happen.
        }
        JsonTokenPainter.paint(doc, json);
        textPane.setCaretPosition(0);
    }

    /**
     * Display an inline error message instead of JSON content.
     *
     * <p>Used by {@link JsonSplitPane} when the file cannot be read or the
     * JSON is malformed. No dialog is shown; the error appears in the raw
     * pane itself.</p>
     *
     * <p>Must be called on the EDT.</p>
     *
     * @param cause the exception that caused the failure; must not be
     *              {@code null}
     */
    public void showError(Exception cause) {
        clearHighlights();
        clear();
        String msg = (cause.getMessage() != null)
                ? cause.getMessage()
                : cause.getClass().getSimpleName();
        append("JSON error:\n\n" + msg, STYLE_ERROR);
        textPane.setCaretPosition(0);
    }

    // -------------------------------------------------------------------------
    // Public API — search
    // -------------------------------------------------------------------------

    /**
     * Highlight all occurrences of {@code query} in the raw text and select
     * the first match.
     *
     * <p>All previous highlights are removed before the new search. If
     * {@code query} is {@code null} or blank, highlights are cleared and
     * {@code countCallback} is called with {@code (0, 0)}.</p>
     *
     * <p>Must be called on the EDT.</p>
     *
     * @param query         the literal string to search for (case-insensitive);
     *                      {@code null} or blank clears highlights
     * @param countCallback receives {@code (currentMatch, totalMatches)} after
     *                      the search completes; must not be {@code null}
     */
    public void highlight(String query, BiConsumer<Integer, Integer> countCallback) {
        this.countCallback = countCallback;
        clearHighlights();

        if (query == null || query.isBlank()) {
            countCallback.accept(0, 0);
            return;
        }

        String docText;
        try {
            StyledDocument doc = textPane.getStyledDocument();
            docText = doc.getText(0, doc.getLength());
        } catch (BadLocationException ex) {
            countCallback.accept(0, 0);
            return;
        }

        String lowerDoc   = docText.toLowerCase();
        String lowerQuery = query.toLowerCase();
        queryLength = query.length();

        Highlighter highlighter = textPane.getHighlighter();
        int from = 0;
        int idx;
        while ((idx = lowerDoc.indexOf(lowerQuery, from)) >= 0) {
            try {
                Object tag = highlighter.addHighlight(
                        idx, idx + queryLength, PAINTER_MATCH);
                matchOffsets.add(idx);
                matchTags.add(tag);
            } catch (BadLocationException ex) {
                // skip
            }
            from = idx + 1;
        }

        if (!matchOffsets.isEmpty()) {
            selectMatch(0);
        }
        countCallback.accept(
                matchOffsets.isEmpty() ? 0 : currentMatchIndex + 1,
                matchOffsets.size());
    }

    /**
     * Move the selection to the next match, wrapping around at the end.
     *
     * <p>No-op if there are no matches. Must be called on the EDT.</p>
     */
    public void nextHighlight() {
        if (matchOffsets.isEmpty()) {
            return;
        }
        int next = (currentMatchIndex + 1) % matchOffsets.size();
        selectMatch(next);
        if (countCallback != null) {
            countCallback.accept(currentMatchIndex + 1, matchOffsets.size());
        }
    }

    /**
     * Move the selection to the previous match, wrapping around at the start.
     *
     * <p>No-op if there are no matches. Must be called on the EDT.</p>
     */
    public void prevHighlight() {
        if (matchOffsets.isEmpty()) {
            return;
        }
        int prev = (currentMatchIndex - 1 + matchOffsets.size())
                % matchOffsets.size();
        selectMatch(prev);
        if (countCallback != null) {
            countCallback.accept(currentMatchIndex + 1, matchOffsets.size());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Make match at {@code index} the current selection: swap its painter to
     * the "current" style, restore the previous current match to the plain
     * match style, and scroll the pane to make it visible.
     *
     * @param index the index into {@link #matchOffsets} to select
     */
    private void selectMatch(int index) {
        Highlighter highlighter = textPane.getHighlighter();

        // Restore the old current match to the plain match painter.
        if (currentMatchIndex >= 0 && currentMatchIndex < matchTags.size()) {
            highlighter.removeHighlight(matchTags.get(currentMatchIndex));
            try {
                int off = matchOffsets.get(currentMatchIndex);
                Object tag = highlighter.addHighlight(
                        off, off + queryLength, PAINTER_MATCH);
                matchTags.set(currentMatchIndex, tag);
            } catch (BadLocationException ex) {
                // ignore
            }
        }

        currentMatchIndex = index;

        // Promote the new current match to the current painter.
        highlighter.removeHighlight(matchTags.get(index));
        try {
            int off = matchOffsets.get(index);
            Object tag = highlighter.addHighlight(
                    off, off + queryLength, PAINTER_CURRENT);
            matchTags.set(index, tag);

            // Scroll to make the match visible.
            textPane.setCaretPosition(off);
            textPane.scrollRectToVisible(
                    textPane.modelToView2D(off).getBounds());
        } catch (BadLocationException ex) {
            // ignore
        }
    }

    /**
     * Remove all search highlights and reset search state.
     */
    private void clearHighlights() {
        if (textPane != null) {
            textPane.getHighlighter().removeAllHighlights();
        }
        matchOffsets.clear();
        matchTags.clear();
        currentMatchIndex = -1;
        queryLength = 0;
    }
}