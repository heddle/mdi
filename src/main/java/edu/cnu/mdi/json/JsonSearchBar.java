package edu.cnu.mdi.json;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A compact search bar that drives text highlighting in the raw pane and
 * match navigation in the tree pane.
 *
 * <h2>Layout</h2>
 * <pre>
 *  [ Search: ][______________text field______________][ ▲ ][ ▼ ][ ✕ ][ 3 of 12 ]
 * </pre>
 * <p>
 * The bar is placed in the {@code SOUTH} slot of the {@link JsonSplitPane}
 * below both content panes so it spans the full width and is always visible.
 * </p>
 *
 * <h2>Behaviour</h2>
 * <ul>
 *   <li>Typing in the field immediately highlights all matching spans in the
 *       raw pane via JsonRawPane#highlight(String) and filters the
 *       tree to show only matching paths via
 *       {@link JsonTreePane#search(String)}.</li>
 *   <li>The <b>▲</b> / <b>▼</b> buttons (and F3 / Shift+F3 key bindings)
 *       navigate between raw-pane highlights.</li>
 *   <li>The <b>✕</b> button clears the query and removes all highlights.</li>
 *   <li>The match count label ({@code "3 of 12"}) is updated after every
 *       search by a callback from {@link JsonRawPane}.</li>
 *   <li>The field background turns a muted red when there are no matches,
 *       normal dark when there are matches or the field is empty.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * <p>All callbacks arrive and all mutations happen on the EDT.</p>
 */
@SuppressWarnings("serial")
public class JsonSearchBar extends JPanel {

    // -------------------------------------------------------------------------
    // Colors
    // -------------------------------------------------------------------------

    /** Normal field background. */
    private static final Color FIELD_BG         = new Color(50, 50, 50);

    /** Field background when the query has no matches. */
    private static final Color FIELD_BG_NO_MATCH = new Color(100, 40, 40);

    /** Field foreground. */
    private static final Color FIELD_FG         = new Color(212, 212, 212);

    /** Panel background — one shade lighter than the content panes. */
    private static final Color BAR_BG           = new Color(45, 45, 45);

    /** Label foreground. */
    private static final Color LABEL_FG         = new Color(160, 160, 160);

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------

    /** Text field the user types into. */
    private final JTextField searchField;

    /** Shows the current match position and total count. */
    private final JLabel countLabel;

    /** The raw pane to highlight matches in. */
    private final JsonRawPane rawPane;

    /** The tree pane to filter matches in. */
    private final JsonTreePane treePane;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Create a search bar wired to the given raw and tree panes.
     *
     * @param rawPane  the raw text pane to highlight; must not be {@code null}
     * @param treePane the tree pane to filter; must not be {@code null}
     */
    public JsonSearchBar(JsonRawPane rawPane, JsonTreePane treePane) {
        this.rawPane  = rawPane;
        this.treePane = treePane;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(BAR_BG);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 60, 60)));

        // "Search:" label.
        JLabel searchLabel = new JLabel(" Search: ");
        searchLabel.setForeground(LABEL_FG);
        searchLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        add(searchLabel);

        // Text field.
        searchField = new JTextField();
        searchField.setBackground(FIELD_BG);
        searchField.setForeground(FIELD_FG);
        searchField.setCaretColor(FIELD_FG);
        searchField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        add(searchField);

        add(Box.createHorizontalStrut(4));

        // Navigation buttons.
        JButton prevButton = makeButton("\u25B2", "Previous match (Shift+F3)");
        JButton nextButton = makeButton("\u25BC", "Next match (F3)");
        JButton clearButton = makeButton("\u2715", "Clear search");
        add(prevButton);
        add(nextButton);
        add(clearButton);

        add(Box.createHorizontalStrut(6));

        // Match count.
        countLabel = new JLabel("  ");
        countLabel.setForeground(LABEL_FG);
        countLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        countLabel.setPreferredSize(new Dimension(80, 20));
        add(countLabel);

        add(Box.createHorizontalStrut(4));

        // ---- Listeners ----

        // Live search: fire on every keystroke.
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate (DocumentEvent e) { onQueryChanged(); }
            @Override public void removeUpdate (DocumentEvent e) { onQueryChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onQueryChanged(); }
        });

        // Enter / F3 = next; Shift+F3 = prev.
        searchField.addActionListener(e -> rawPane.nextHighlight());

        nextButton .addActionListener(e -> rawPane.nextHighlight());
        prevButton .addActionListener(e -> rawPane.prevHighlight());
        clearButton.addActionListener(e -> clearSearch());

        // Global F3 / Shift+F3 bindings (WHEN_IN_FOCUSED_WINDOW not needed
        // here; the field catches them when focused).
        searchField.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "next");
        searchField.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F3,
                        java.awt.event.InputEvent.SHIFT_DOWN_MASK), "prev");
        searchField.getActionMap().put("next",
                new AbstractAction() {
                    @Override public void actionPerformed(ActionEvent e) {
                        rawPane.nextHighlight();
                    }
                });
        searchField.getActionMap().put("prev",
                new AbstractAction() {
                    @Override public void actionPerformed(ActionEvent e) {
                        rawPane.prevHighlight();
                    }
                });
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Clear the search field and remove all highlights.
     *
     * <p>Also called by {@link JsonSplitPane} when a new file is loaded, so
     * stale highlights from the previous file do not persist.</p>
     */
    public void clearSearch() {
        searchField.setText("");
        // onQueryChanged fires via the DocumentListener, which clears highlights.
    }

    /**
     * Update the match count label.
     *
     * <p>Called by {@link JsonRawPane} after each highlight pass so the bar
     * stays in sync without needing to know the count itself.</p>
     *
     * @param current 1-based index of the currently selected match, or 0 if
     *                none is selected
     * @param total   total number of matches found
     */
    void updateCount(int current, int total) {
        if (total == 0) {
            String q = searchField.getText();
            countLabel.setText(q.isEmpty() ? "" : "no matches");
            searchField.setBackground(q.isEmpty() ? FIELD_BG : FIELD_BG_NO_MATCH);
        } else {
            countLabel.setText(current + " of " + total);
            searchField.setBackground(FIELD_BG);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Called on every document change in the search field. Fires a highlight
     * pass in the raw pane and a filter pass in the tree pane.
     */
    private void onQueryChanged() {
        String query = searchField.getText();
        rawPane.highlight(query, this::updateCount);
        treePane.search(query);
    }

    /**
     * Create a small toolbar-style button with a tooltip.
     *
     * @param label   button text (typically a Unicode symbol)
     * @param tooltip tooltip string
     * @return the configured button
     */
    private JButton makeButton(String label, String tooltip) {
        JButton btn = new JButton(label);
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        btn.setToolTipText(tooltip);
        btn.setBackground(BAR_BG);
        btn.setForeground(LABEL_FG);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        btn.setMaximumSize(new Dimension(36, 26));
        return btn;
    }
}