package edu.cnu.mdi.json;

/**
 * Immutable value object carried as the user-object of every
 * {@link javax.swing.tree.DefaultMutableTreeNode} in the JSON tree.
 *
 * <h2>Purpose</h2>
 * <p>
 * Rather than encoding type information in the label string and re-parsing it
 * in the renderer, each node carries an explicit {@link Kind} tag. This lets
 * {@link JsonTreeCellRenderer} select colors in a single {@code switch}
 * without any string analysis.
 * </p>
 *
 * <h2>Label format</h2>
 * <p>
 * {@link #toString()} returns the string that {@link JsonTreeCellRenderer}
 * passes to the underlying label. The format is:
 * </p>
 * <ul>
 *   <li><b>Leaf nodes with a key</b> — {@code key: value}
 *       (e.g. {@code name: "Alice"}, {@code count: 42})</li>
 *   <li><b>Non-leaf key/index nodes</b> — {@code key  summary}
 *       (e.g. {@code address  {3 keys}}, {@code [0]  [2 items]})</li>
 *   <li><b>Root node</b> — the file name</li>
 *   <li><b>Top-level primitives (no key)</b> — the value string alone</li>
 * </ul>
 */
public final class JsonNodeData {

    // -------------------------------------------------------------------------
    // Kind enum
    // -------------------------------------------------------------------------

    /**
     * Classifies a JSON tree node for color selection in
     * {@link JsonTreeCellRenderer}.
     */
    public enum Kind {
        /** Root node (the file name). */
        ROOT,
        /**
         * An object key or array index that has children (object or array
         * value). Rendered in key color with a gray size summary.
         */
        KEY,
        /** A quoted string value leaf. */
        STRING,
        /** A numeric value leaf. */
        NUMBER,
        /** A {@code true} or {@code false} value leaf. */
        BOOLEAN,
        /** A {@code null} value leaf. */
        NULL
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /**
     * The key or array index label.
     *
     * <p>Empty for top-level primitive nodes that have no enclosing key.</p>
     */
    public final String key;

    /**
     * The value string for leaf nodes, or a size summary
     * (e.g. {@code {3 keys}}) for non-leaf key/index nodes.
     *
     * <p>Empty for the root node.</p>
     */
    public final String value;

    /**
     * The token kind, used by {@link JsonTreeCellRenderer} to pick a color
     * without re-parsing the label.
     */
    public final Kind kind;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Create a {@code JsonNodeData}.
     *
     * @param key   key or array index; may be empty but not {@code null}
     * @param value value string or size summary; may be empty but not
     *              {@code null}
     * @param kind  token kind; must not be {@code null}
     */
    public JsonNodeData(String key, String value, Kind kind) {
        this.key   = key;
        this.value = value;
        this.kind  = kind;
    }

    // -------------------------------------------------------------------------
    // Object
    // -------------------------------------------------------------------------

    /**
     * Returns the display string used by {@link JsonTreeCellRenderer}.
     *
     * <ul>
     *   <li>Root: just the key (the file name).</li>
     *   <li>No key (top-level primitive): just the value.</li>
     *   <li>Otherwise: {@code "key: value"} for leaves or
     *       {@code "key  summary"} for non-leaf keys.</li>
     * </ul>
     *
     * @return the display string for this node
     */
    @Override
    public String toString() {
        if (kind == Kind.ROOT)   return key;
        if (key.isEmpty())       return value;
        if (kind == Kind.KEY)    return key + "  " + value;
        return key + ": " + value;
    }
}