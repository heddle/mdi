package edu.cnu.mdi.json;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * Cell renderer for the JSON tree that applies the same dark-theme color
 * palette used by the raw-text pane.
 *
 * <h2>Color mapping</h2>
 * <table border="1">
 * <caption>JSON tree renderer color mapping</caption>
 *   <tr><th>Kind</th><th>Key color</th><th>Value color</th></tr>
 *   <tr><td>ROOT</td>
 *       <td colspan="2">plain ({@link JsonRawPane#COLOR_PLAIN})</td></tr>
 *   <tr><td>KEY / array index with children</td>
 *       <td>{@link JsonRawPane#COLOR_KEY} bold</td>
 *       <td>{@link JsonRawPane#COLOR_STRUCTURAL} (the size summary)</td></tr>
 *   <tr><td>STRING</td>
 *       <td>{@link JsonRawPane#COLOR_KEY}</td>
 *       <td>{@link JsonRawPane#COLOR_STRING}</td></tr>
 *   <tr><td>NUMBER</td>
 *       <td>{@link JsonRawPane#COLOR_KEY}</td>
 *       <td>{@link JsonRawPane#COLOR_NUMBER}</td></tr>
 *   <tr><td>BOOLEAN</td>
 *       <td>{@link JsonRawPane#COLOR_KEY}</td>
 *       <td>{@link JsonRawPane#COLOR_BOOLEAN}</td></tr>
 *   <tr><td>NULL</td>
 *       <td>{@link JsonRawPane#COLOR_KEY}</td>
 *       <td>{@link JsonRawPane#COLOR_NULL} italic</td></tr>
 * </table>
 *
 * <h2>Implementation note</h2>
 * <p>
 * {@link DefaultTreeCellRenderer} is a {@link JLabel} subclass. We configure
 * the label's text using an HTML snippet so that key and value can be styled
 * with independent colors inside a single label — no second component is
 * needed. The HTML is kept intentionally minimal (inline {@code <font>} and
 * {@code <b>} tags only) to avoid the overhead of the full CSS engine.
 * </p>
 * <p>
 * Selection and focus decorations are preserved by delegating to
 * {@code super.getTreeCellRendererComponent} first and then overriding only
 * the text.
 * </p>
 */
@SuppressWarnings("serial")
public class JsonTreeCellRenderer extends DefaultTreeCellRenderer {

    // -------------------------------------------------------------------------
    // Colors
    // -------------------------------------------------------------------------

    /** Background used for all tree rows (normal and selected). */
    private static final Color BG           = JsonRawPane.PANE_BG;

    /** Selection highlight — slightly lighter than the background. */
    private static final Color BG_SELECTED  = new Color(60, 60, 80);

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Create a {@code JsonTreeCellRenderer} and configure the default
     * background and icon colors for the dark theme.
     */
    public JsonTreeCellRenderer() {
        setBackgroundNonSelectionColor(BG);
        setBackgroundSelectionColor(BG_SELECTED);
        setTextNonSelectionColor(JsonRawPane.COLOR_PLAIN);
        setTextSelectionColor(Color.WHITE);
        setBorderSelectionColor(BG_SELECTED);

        // Remove the default tree icons — they look odd on a dark background.
        setLeafIcon(null);
        setOpenIcon(null);
        setClosedIcon(null);
    }

    // -------------------------------------------------------------------------
    // DefaultTreeCellRenderer override
    // -------------------------------------------------------------------------

    /**
     * Configure this label for the given tree node.
     *
     * <p>Delegates to {@code super} first so that selection and focus
     * painting are handled correctly, then replaces the label text with an
     * HTML snippet that colors key and value independently.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value,
            boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {

        // Let the superclass handle selection background and border.
        super.getTreeCellRendererComponent(
                tree, value, selected, expanded, leaf, row, hasFocus);

        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        if (!(value instanceof DefaultMutableTreeNode node)) {
            return this;
        }
        Object userObject = node.getUserObject();
        if (!(userObject instanceof JsonNodeData data)) {
            return this;
        }

        setText(buildHtml(data));
        return this;
    }

    // -------------------------------------------------------------------------
    // Private HTML builder
    // -------------------------------------------------------------------------

    /**
     * Build an HTML label string for the given node data.
     *
     * <p>The label has the form:
     * <code>&lt;html&gt;&lt;font color="#rrggbb"&gt;key&lt;/font&gt;:
     * &lt;font color="#rrggbb"&gt;value&lt;/font&gt;&lt;/html&gt;</code>.
     * Coloring is determined by {@link JsonNodeData#kind}.</p>
     *
     * @param data the node data; must not be {@code null}
     * @return an HTML string suitable for {@link JLabel#setText}
     */
    private static String buildHtml(JsonNodeData data) {
        return switch (data.kind) {

            case ROOT ->
                    html(colorSpan(JsonRawPane.COLOR_PLAIN, data.key));

            case KEY -> {
                // "key  {3 keys}" — key in blue-bold, summary in gray.
                String keyPart = boldSpan(JsonRawPane.COLOR_KEY, data.key);
                String sumPart = colorSpan(JsonRawPane.COLOR_STRUCTURAL,
                        "  " + data.value);
                yield html(keyPart + sumPart);
            }

            case STRING -> {
                String keyPart = boldSpan(JsonRawPane.COLOR_KEY,
                        data.key.isEmpty() ? "" : data.key + ": ");
                String valPart = colorSpan(JsonRawPane.COLOR_STRING, data.value);
                yield html(keyPart + valPart);
            }

            case NUMBER -> {
                String keyPart = boldSpan(JsonRawPane.COLOR_KEY,
                        data.key.isEmpty() ? "" : data.key + ": ");
                String valPart = colorSpan(JsonRawPane.COLOR_NUMBER, data.value);
                yield html(keyPart + valPart);
            }

            case BOOLEAN -> {
                String keyPart = boldSpan(JsonRawPane.COLOR_KEY,
                        data.key.isEmpty() ? "" : data.key + ": ");
                String valPart = colorSpan(JsonRawPane.COLOR_BOOLEAN, data.value);
                yield html(keyPart + valPart);
            }

            case NULL -> {
                String keyPart = boldSpan(JsonRawPane.COLOR_KEY,
                        data.key.isEmpty() ? "" : data.key + ": ");
                String valPart = "<i>" + colorSpan(JsonRawPane.COLOR_NULL,
                        data.value) + "</i>";
                yield html(keyPart + valPart);
            }
        };
    }

    // -------------------------------------------------------------------------
    // HTML snippet helpers
    // -------------------------------------------------------------------------

    /**
     * Wrap {@code content} in an HTML root element.
     *
     * @param content inner HTML content
     * @return {@code <html>content</html>}
     */
    private static String html(String content) {
        return "<html>" + content + "</html>";
    }

    /**
     * Wrap {@code text} in a {@code <font>} tag with the given color.
     *
     * @param color the foreground color
     * @param text  the text to color; HTML-special characters are
     *              <em>not</em> escaped here — callers must not pass
     *              user-controlled text that could contain {@code <} or
     *              {@code &} without escaping first
     * @return HTML fragment
     */
    private static String colorSpan(Color color, String text) {
        return "<font color=\"" + hex(color) + "\">" + escapeHtml(text) + "</font>";
    }

    /**
     * Wrap {@code text} in bold and color tags.
     *
     * @param color the foreground color
     * @param text  the text to render
     * @return HTML fragment
     */
    private static String boldSpan(Color color, String text) {
        return "<b><font color=\"" + hex(color) + "\">" + escapeHtml(text) + "</font></b>";
    }

    /**
     * Convert a {@link Color} to a {@code #rrggbb} hex string for use in an
     * HTML {@code color} attribute.
     *
     * @param c the color; must not be {@code null}
     * @return six-digit lowercase hex color string prefixed with {@code #}
     */
    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    /**
     * Escape the characters {@code &}, {@code <}, and {@code >} so that
     * node labels containing those characters render correctly inside the
     * Swing HTML label.
     *
     * @param text raw text that may contain HTML-special characters
     * @return HTML-safe string
     */
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }
}