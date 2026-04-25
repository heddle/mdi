package edu.cnu.mdi.json;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Split-pane panel that displays a JSON file in two synchronized views:
 * colorized raw text on the left and a collapsible tree on the right, with a
 * shared search bar along the bottom.
 *
 * <h2>Layout</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────┐
 * │  filename                        (header label) │
 * ├───────────────────────┬─────────────────────────┤
 * │  Raw JSON (colored)   │  Collapsible tree        │
 * │  JsonRawPane          │  JsonTreePane            │
 * │                       │  path breadcrumb (SOUTH) │
 * ├───────────────────────┴─────────────────────────┤
 * │  [ Search: ][_____________][ ▲ ][ ▼ ][ ✕ ] n/m  │
 * └─────────────────────────────────────────────────┘
 * </pre>
 * <p>
 * The divider starts at 45 % of the panel width so the raw-text side gets a
 * little more room by default — raw JSON tends to be wide while tree nodes
 * tend to be narrow.
 * </p>
 *
 * <h2>Threading</h2>
 * <p>
 * {@link #loadFile(File)} returns immediately. File I/O and Gson parsing are
 * done on a {@link SwingWorker} background thread; both panes are updated on
 * the EDT when parsing completes. Parse errors are displayed inline — the raw
 * pane shows the error message in red and the tree pane is cleared.
 * </p>
 *
 * <h2>Delegation</h2>
 * <ul>
 *   <li>Raw-text colorization and search highlighting are handled by
 *       {@link JsonRawPane}.</li>
 *   <li>Tree construction, path breadcrumb, copy menu, and tree filtering are
 *       handled by {@link JsonTreePane}.</li>
 *   <li>The search field, navigation buttons, and match count are handled by
 *       {@link JsonSearchBar}, which calls into both panes.</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class JsonSplitPane extends JPanel {

    // -------------------------------------------------------------------------
    // Layout constants
    // -------------------------------------------------------------------------

    /**
     * Proportion of total width assigned to the raw-text (left) pane when the
     * divider is first shown.
     */
    private static final double DIVIDER_PROPORTION = 0.45;

    /** Background color of the header strip and overall panel. */
    private static final Color HEADER_BG = new Color(45, 45, 45);

    /** Foreground color of the header label. */
    private static final Color HEADER_FG = new Color(180, 180, 180);

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------

    /** Left pane: colorized raw JSON text. */
    private final JsonRawPane rawPane;

    /** Right pane: collapsible JSON tree. */
    private final JsonTreePane treePane;

    /** Search bar spanning the full width at the bottom. */
    private final JsonSearchBar searchBar;

    /** File name / status label shown above the split pane. */
    private final JLabel headerLabel;
    
    //so that it can be added to
    private final JPanel header;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Create an empty {@code JsonSplitPane} with a "drop a file" prompt in
     * the header, empty content in both halves, and a ready search bar.
     */
    public JsonSplitPane() {
        setLayout(new BorderLayout());
        setBackground(JsonRawPane.PANE_BG);

        // Header strip — shows the current file name or a prompt.
        header = new JPanel(new BorderLayout());
        headerLabel = new JLabel(
                "Drop a .json file onto this view", SwingConstants.CENTER);
        headerLabel.setForeground(HEADER_FG);
        headerLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        headerLabel.setBackground(HEADER_BG);
        headerLabel.setOpaque(true);
        header.add(headerLabel, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // Content panes.
        rawPane  = new JsonRawPane();
        treePane = new JsonTreePane();

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, rawPane, treePane);
        split.setResizeWeight(DIVIDER_PROPORTION);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setBackground(JsonRawPane.PANE_BG);
        add(split, BorderLayout.CENTER);

        // Search bar — spans full width below both panes.
        searchBar = new JsonSearchBar(rawPane, treePane);
        add(searchBar, BorderLayout.SOUTH);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------
    
    /** Access the header panel, so that it can be added to. */
    public JPanel getHeader() {
		return header;
	}

    /**
     * Load, parse, and display the given JSON file.
     *
     * <p>Returns immediately. File I/O and parsing happen on a background
     * thread; both panes and the search bar are updated on the EDT when the
     * worker finishes. If the file cannot be read or contains invalid JSON the
     * error is shown inline — no dialog is raised.</p>
     *
     * @param file the JSON file to display; must not be {@code null}
     */
    public void loadFile(File file) {
        if (file == null) {
            return;
        }

        headerLabel.setText("Loading: " + file.getName() + " \u2026");
        searchBar.clearSearch();   // remove stale highlights before new content arrives
        rawPane.clear();
        treePane.clear();

        new SwingWorker<ParseResult, Void>() {

            @Override
            protected ParseResult doInBackground() {
                try {
                    String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    JsonElement root = JsonParser.parseString(raw);
                    return new ParseResult(raw, root, null);
                } catch (IOException | JsonSyntaxException ex) {
                    return new ParseResult(null, null, ex);
                }
            }

            @Override
            protected void done() {
                try {
                    ParseResult result = get();
                    if (result.error != null) {
                        showError(file.getName(), result.error);
                    } else {
                        showResult(file.getName(), result.raw, result.root);
                    }
                } catch (Exception ex) {
                    showError(file.getName(), ex);
                }
            }
        }.execute();
    }

    // -------------------------------------------------------------------------
    // Private EDT helpers
    // -------------------------------------------------------------------------

    /**
     * Populate both panes from a successful parse.
     *
     * @param filename short name shown in the header
     * @param raw      raw JSON text for the left pane
     * @param root     parsed Gson element tree for the right pane
     */
    private void showResult(String filename, String raw, JsonElement root) {
        headerLabel.setText(filename);
        rawPane.showJson(raw);
        treePane.setRoot(root, filename);
    }

    /**
     * Display an error in the raw pane and clear the tree pane.
     *
     * @param filename short name shown in the header
     * @param cause    the exception that caused the failure
     */
    private void showError(String filename, Exception cause) {
        headerLabel.setText("Error \u2014 " + filename);
        rawPane.showError(cause);
        treePane.clear();
    }

    // -------------------------------------------------------------------------
    // Worker result carrier
    // -------------------------------------------------------------------------

    /**
     * Immutable result of a background parse attempt.
     *
     * <p>Invariant: exactly one of {@code root} and {@code error} is
     * non-{@code null}. {@code raw} is non-{@code null} iff {@code root}
     * is non-{@code null}.</p>
     */
    private static final class ParseResult {

        /** Raw JSON text, or {@code null} on error. */
        final String raw;

        /** Parsed element tree, or {@code null} on error. */
        final JsonElement root;

        /** Parse / IO failure, or {@code null} on success. */
        final Exception error;

        ParseResult(String raw, JsonElement root, Exception error) {
            this.raw   = raw;
            this.root  = root;
            this.error = error;
        }
    }
}