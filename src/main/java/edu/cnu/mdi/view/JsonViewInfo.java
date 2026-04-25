package edu.cnu.mdi.view;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes the {@link JsonView} for the in-app help / info dialog.
 *
 * <p>Returned by {@link JsonView#getViewInfo()} and displayed when the user
 * clicks the info button in the view's title bar.</p>
 */
public class JsonViewInfo extends AbstractViewInfo {

    // -------------------------------------------------------------------------
    // Mandatory content
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @return {@code "JSON Viewer"}
     */
    @Override
    public String getTitle() {
        return "JSON Viewer";
    }

    /**
     * {@inheritDoc}
     *
     * @return a short description of the viewer's purpose
     */
    @Override
    public String getPurpose() {
        return "Displays a JSON file as colorized raw text on the left and a "
             + "collapsible tree on the right. Supports incremental search, "
             + "node path inspection, and clipboard copy.";
    }

    // -------------------------------------------------------------------------
    // Structured usage
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns the steps a user takes to open and work with a file.</p>
     */
    @Override
    public List<String> getUsageSteps() {
        return List.of(
                "Drag a .json file onto the view \u2014 it loads immediately.",
                "Use the search bar at the bottom to find text in both panes. "
              + "Results are highlighted in the raw pane and filtered in the tree.",
                "Click \u25b2 / \u25bc or press F3 / Shift+F3 to step through "
              + "matches. The count label shows your position.",
                "Click any tree node to see its full JSON path in the breadcrumb "
              + "label at the bottom of the tree.",
                "Right-click a leaf node and choose Copy value to put the value "
              + "on the clipboard. Right-click any node and choose Copy path to "
              + "copy its dot-notation path (e.g. parameters.curves[0].xData).",
                "Drag the centre divider left or right to give more room to "
              + "whichever pane you need."
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns additional notes about the viewer's behaviour.</p>
     */
    @Override
    public List<String> getUsageBullets() {
        return List.of(
                "Only .json files are accepted by drag-and-drop. Other file "
              + "types are silently rejected.",
                "File I/O and parsing happen on a background thread \u2014 "
              + "the UI stays responsive even for large files.",
                "If the file contains invalid JSON, an error message is shown "
              + "inline in the raw pane. No dialog is raised.",
                "The search is case-insensitive and matches literal substrings. "
              + "The tree filter shows matching nodes together with their "
              + "ancestor chain so the path is always navigable.",
                "Clearing the search field restores the full unfiltered tree "
              + "without re-parsing the file.",
                "The raw pane is read-only. Syntax colors are applied on top "
              + "of the document and are not disturbed by search highlights."
        );
    }

    // -------------------------------------------------------------------------
    // Keyboard shortcuts
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns keyboard shortcuts in display order. A {@link LinkedHashMap}
     * is used so insertion order is preserved in the rendered dialog.</p>
     */
    @Override
    public Map<String, String> getKeyboardShortcuts() {
        Map<String, String> shortcuts = new LinkedHashMap<>();
        shortcuts.put("F3",            "Next search match");
        shortcuts.put("Shift+F3",      "Previous search match");
        shortcuts.put("Enter",         "Next search match (when search field is focused)");
        shortcuts.put("\u2190 \u2192", "Collapse / expand tree node");
        shortcuts.put("\u2191 \u2193", "Navigate tree rows");
        return shortcuts;
    }

    // -------------------------------------------------------------------------
    // Technical notes
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns implementation notes useful to developers extending or
     * integrating the viewer.</p>
     */
    @Override
    public String getTechnicalNotes() {
        return "JSON parsing uses Gson (com.google.gson). "
             + "The raw pane extends TextPaneScrollPane and uses a StyledDocument "
             + "with token-level character attributes for colorization. "
             + "Search highlighting uses the Swing Highlighter API layered on "
             + "top of the document, so syntax colors are never modified by a "
             + "search. The tree model is built once from the Gson element tree "
             + "and a filtered copy is produced for each search query without "
             + "re-parsing. "
             + "File loading (JsonView.loadFile) is thread-safe and may be "
             + "called from any thread.";
    }

    // -------------------------------------------------------------------------
    // Appearance
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Uses a blue accent to echo the key color used in the raw pane
     * ({@link edu.cnu.mdi.json.JsonRawPane#COLOR_KEY}, cornflower blue).</p>
     */
    @Override
    protected String getAccentColorHex() {
        return "#6495ed";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "JSON Viewer \u2014 MDI Framework"}
     */
    @Override
    public String getFooter() {
        return "JSON Viewer \u2014 MDI Framework";
    }
}