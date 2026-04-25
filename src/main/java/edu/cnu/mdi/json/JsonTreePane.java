package edu.cnu.mdi.json;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * The right half of the {@link JsonSplitPane}: a collapsible tree view of a
 * parsed JSON structure.
 *
 * <h2>Tree mapping</h2>
 * <p>
 * Gson's {@link JsonElement} hierarchy maps onto a {@link DefaultMutableTreeNode}
 * tree as follows:
 * </p>
 * <ul>
 *   <li>{@link JsonObject} — one child node per key, labelled {@code "key"}.
 *       The value is recursively expanded as that child's subtree.</li>
 *   <li>{@link JsonArray} — one child node per element, labelled
 *       {@code [0]}, {@code [1]}, etc. Each element is recursively
 *       expanded.</li>
 *   <li>{@link JsonPrimitive} (string, number, boolean) and
 *       {@link com.google.gson.JsonNull} — leaf nodes whose label includes
 *       both the key/index and the value, e.g. {@code name: "Alice"} or
 *       {@code [2]: 42}.</li>
 * </ul>
 *
 * <h2>Expand policy</h2>
 * <p>
 * After building the model, only the root and its immediate children are
 * expanded. Deeper nodes start collapsed so large documents do not flood the
 * display. The user can expand/collapse nodes normally with mouse clicks or
 * the arrow keys.
 * </p>
 *
 * <h2>Path breadcrumb</h2>
 * <p>
 * A label at the bottom of the pane shows the full dot-notation JSON path of
 * the currently selected node (e.g. {@code parameters.curves[0].xData}).
 * It updates on every selection change.
 * </p>
 *
 * <h2>Copy value</h2>
 * <p>
 * Right-clicking any leaf node shows a context menu with a "Copy value"
 * item that puts the node's raw value string onto the system clipboard.
 * Right-clicking a key/array node offers "Copy path" instead, which copies
 * the full dot-notation path.
 * </p>
 *
 * <h2>Search / filter</h2>
 * <p>
 * {@link #search(String)} filters the tree to show only nodes whose label
 * contains the query string (case-insensitive), along with their ancestor
 * chain so the matching node is reachable. Clearing the query restores the
 * full tree.
 * </p>
 */
@SuppressWarnings("serial")
public class JsonTreePane extends JPanel {

    // -------------------------------------------------------------------------
    // Colors (dark theme, matching JsonRawPane)
    // -------------------------------------------------------------------------

    /** Background color matching the raw pane. */
    private static final Color TREE_BG = JsonRawPane.PANE_BG;

    /** Default text color for tree rows. */
    private static final Color TREE_FG = JsonRawPane.COLOR_PLAIN;

    /** Breadcrumb label background — one shade lighter than the tree. */
    private static final Color CRUMB_BG = new Color(40, 40, 40);

    /** Breadcrumb label foreground. */
    private static final Color CRUMB_FG = new Color(150, 150, 150);

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------

    /** The Swing tree component. */
    private final JTree tree;

    /** Label showing the JSON path of the selected node. */
    private final JLabel pathLabel;

    /**
     * The full (unfiltered) root node, retained so the filter can be cleared
     * and the original model restored without re-parsing.
     */
    private DefaultMutableTreeNode fullRoot = null;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Create an empty {@code JsonTreePane} with a dark background, a path
     * breadcrumb label at the bottom, and context-menu support.
     */
    public JsonTreePane() {
        setLayout(new BorderLayout());
        setBackground(TREE_BG);

        tree = new JTree(emptyModel());
        tree.setBackground(TREE_BG);
        tree.setForeground(TREE_FG);
        tree.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new JsonTreeCellRenderer());

        // Enable tooltips so long paths are readable on hover.
        ToolTipManager.sharedInstance().registerComponent(tree);

        installSelectionListener();
        installContextMenu();

        JScrollPane scroll = new JScrollPane(tree);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(TREE_BG);
        scroll.getViewport().setBackground(TREE_BG);
        add(scroll, BorderLayout.CENTER);

        // Path breadcrumb label at the bottom.
        pathLabel = new JLabel(" ", SwingConstants.LEFT);
        pathLabel.setForeground(CRUMB_FG);
        pathLabel.setBackground(CRUMB_BG);
        pathLabel.setOpaque(true);
        pathLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        pathLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(55, 55, 55)),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        add(pathLabel, BorderLayout.SOUTH);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Build and display a tree from the given Gson element.
     *
     * <p>The root node is labelled with {@code filename}. After setting the
     * new model, the root and its immediate children are expanded; all
     * deeper nodes start collapsed.</p>
     *
     * <p>Must be called on the EDT.</p>
     *
     * @param root     the top-level {@link JsonElement} returned by Gson;
     *                 must not be {@code null}
     * @param filename display label for the root node
     */
    public void setRoot(JsonElement root, String filename) {
        fullRoot = new DefaultMutableTreeNode(
                new JsonNodeData(filename, "", JsonNodeData.Kind.ROOT));
        buildTree(fullRoot, root);
        tree.setModel(new DefaultTreeModel(fullRoot));
        expandTopLevel(fullRoot);
        pathLabel.setText(" ");
    }

    /**
     * Clear the tree back to an empty state and reset the path label.
     *
     * <p>Must be called on the EDT.</p>
     */
    public void clear() {
        fullRoot = null;
        tree.setModel(emptyModel());
        pathLabel.setText(" ");
    }

    /**
     * Filter the tree to show only nodes whose label contains {@code query}
     * (case-insensitive), along with their ancestor chain.
     *
     * <p>If {@code query} is {@code null} or blank the full unfiltered tree
     * is restored. If no file is loaded this is a no-op.</p>
     *
     * <p>Must be called on the EDT.</p>
     *
     * @param query the filter string; {@code null} or blank restores the full
     *              tree
     */
    public void search(String query) {
        if (fullRoot == null) {
            return;
        }
        if (query == null || query.isBlank()) {
            tree.setModel(new DefaultTreeModel(fullRoot));
            expandTopLevel(fullRoot);
            return;
        }

        // Build a filtered copy of the tree containing only matching paths.
        DefaultMutableTreeNode filteredRoot =
                filterNode(fullRoot, query.toLowerCase());
        if (filteredRoot == null) {
            // No matches — show a placeholder root with no children.
            filteredRoot = new DefaultMutableTreeNode(
                    new JsonNodeData(
                            ((JsonNodeData) fullRoot.getUserObject()).key,
                            "", JsonNodeData.Kind.ROOT));
        }
        DefaultTreeModel filteredModel = new DefaultTreeModel(filteredRoot);
        tree.setModel(filteredModel);

        // Expand all nodes in the filtered tree so matches are visible.
        expandAll(filteredRoot);
    }

    // -------------------------------------------------------------------------
    // Private — selection listener and path building
    // -------------------------------------------------------------------------

    /**
     * Install a tree selection listener that updates the path breadcrumb label
     * whenever the selection changes.
     */
    private void installSelectionListener() {
        tree.addTreeSelectionListener(e -> {
            TreePath selPath = tree.getSelectionPath();
            if (selPath == null) {
                pathLabel.setText(" ");
                return;
            }
            String path = buildJsonPath(selPath);
            pathLabel.setText(path);
            pathLabel.setToolTipText(path);
        });
    }

    /**
     * Build a dot-notation JSON path string from a {@link TreePath}.
     *
     * <p>Array indices are rendered as {@code [n]} and appended directly
     * (no dot prefix). Object keys are joined with {@code .}. The root node
     * (the file name) is omitted from the path.</p>
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code parameters.curves[0].xData}</li>
     *   <li>{@code renderHints[0]}</li>
     *   <li>{@code formatVersion}</li>
     * </ul>
     * </p>
     *
     * @param selPath the selected path in the tree
     * @return a JSON path string, or an empty string for the root
     */
    private static String buildJsonPath(TreePath selPath) {
        Object[] nodes = selPath.getPath();
        StringBuilder sb = new StringBuilder();

        // Skip index 0 (the root / file name node).
        for (int i = 1; i < nodes.length; i++) {
            if (!(nodes[i] instanceof DefaultMutableTreeNode dmtn)) {
                continue;
            }
            if (!(dmtn.getUserObject() instanceof JsonNodeData data)) {
                continue;
            }
            String key = data.key;
            if (key.isEmpty()) {
                continue;
            }
            if (key.startsWith("[")) {
                // Array index — append directly without a dot separator.
                sb.append(key);
            } else {
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(key);
            }
        }
        return sb.length() > 0 ? sb.toString() : "(root)";
    }

    // -------------------------------------------------------------------------
    // Private — context menu
    // -------------------------------------------------------------------------

    /**
     * Install a right-click context menu on the tree with "Copy value" for
     * leaf nodes and "Copy path" for all nodes.
     */
    private void installContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(new Color(50, 50, 50));

        JMenuItem copyValue = makeMenuItem("Copy value");
        JMenuItem copyPath  = makeMenuItem("Copy path");

        menu.add(copyValue);
        menu.add(copyPath);

        copyValue.addActionListener(e -> {
            DefaultMutableTreeNode node = selectedNode();
            if (node == null) {
                return;
            }
            if (!(node.getUserObject() instanceof JsonNodeData data)) {
                return;
            }
            // For leaf nodes copy the raw value; for branch nodes copy the
            // summary (e.g. "{3 keys}") — the user's most likely intent.
            copyToClipboard(data.value.isEmpty() ? data.key : data.value);
        });

        copyPath.addActionListener(e -> {
            TreePath sel = tree.getSelectionPath();
            if (sel != null) {
                copyToClipboard(buildJsonPath(sel));
            }
        });

        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                maybeShowMenu(e);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                maybeShowMenu(e);
            }

            private void maybeShowMenu(java.awt.event.MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }
                // Select the row under the cursor so the user sees which node
                // the menu will act on.
                int row = tree.getRowForLocation(e.getX(), e.getY());
                if (row >= 0) {
                    tree.setSelectionRow(row);
                }
                DefaultMutableTreeNode node = selectedNode();
                // "Copy value" only makes sense for leaf nodes.
                copyValue.setEnabled(node != null && node.isLeaf());
                copyPath.setEnabled(node != null);
                menu.show(tree, e.getX(), e.getY());
            }
        });
    }

    /**
     * Create a styled menu item for the context menu.
     *
     * @param text the item label
     * @return the configured {@link JMenuItem}
     */
    private static JMenuItem makeMenuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setBackground(new Color(50, 50, 50));
        item.setForeground(JsonRawPane.COLOR_PLAIN);
        item.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        return item;
    }

    /**
     * Return the currently selected {@link DefaultMutableTreeNode}, or
     * {@code null} if nothing is selected.
     *
     * @return the selected node, or {@code null}
     */
    private DefaultMutableTreeNode selectedNode() {
        TreePath sel = tree.getSelectionPath();
        if (sel == null) {
            return null;
        }
        Object last = sel.getLastPathComponent();
        return (last instanceof DefaultMutableTreeNode dmtn) ? dmtn : null;
    }

    /**
     * Put {@code text} onto the system clipboard.
     *
     * @param text the text to copy; must not be {@code null}
     */
    private static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit()
               .getSystemClipboard()
               .setContents(new StringSelection(text), null);
    }

    // -------------------------------------------------------------------------
    // Private — tree filtering
    // -------------------------------------------------------------------------

    /**
     * Return a deep copy of the subtree rooted at {@code node} containing only
     * nodes whose label matches {@code lowerQuery} (case-insensitive), along
     * with all ancestor nodes needed to reach them.
     *
     * <p>A node is included if its own label matches <em>or</em> if any of its
     * descendants match (so the ancestor chain is always preserved).
     *
     * @param node       the source node to filter
     * @param lowerQuery the lower-cased query string
     * @return a new node subtree, or {@code null} if nothing in this subtree
     *         matched
     */
    private static DefaultMutableTreeNode filterNode(
            DefaultMutableTreeNode node, String lowerQuery) {

        JsonNodeData data = (node.getUserObject() instanceof JsonNodeData d) ? d : null;
        String label = (data != null) ? data.toString().toLowerCase() : "";
        boolean selfMatches = label.contains(lowerQuery);

        // Recursively filter children.
        List<DefaultMutableTreeNode> matchingChildren = new ArrayList<>();
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            Object child = children.nextElement();
            if (child instanceof DefaultMutableTreeNode childNode) {
                DefaultMutableTreeNode filtered = filterNode(childNode, lowerQuery);
                if (filtered != null) {
                    matchingChildren.add(filtered);
                }
            }
        }

        if (!selfMatches && matchingChildren.isEmpty()) {
            return null; // nothing matched in this subtree
        }

        // Build a copy of this node with the matching children attached.
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(node.getUserObject());
        for (DefaultMutableTreeNode child : matchingChildren) {
            copy.add(child);
        }
        return copy;
    }

    // -------------------------------------------------------------------------
    // Private — tree building
    // -------------------------------------------------------------------------

    /**
     * Recursively convert a {@link JsonElement} subtree into
     * {@link DefaultMutableTreeNode} children of {@code parent}.
     *
     * @param parent  the node to attach children to
     * @param element the Gson element to convert
     */
    private void buildTree(DefaultMutableTreeNode parent, JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (var entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();

                if (isLeaf(value)) {
                    parent.add(new DefaultMutableTreeNode(leafData(key, value)));
                } else {
                    DefaultMutableTreeNode keyNode = new DefaultMutableTreeNode(
                            new JsonNodeData(key, summarize(value),
                                    JsonNodeData.Kind.KEY));
                    parent.add(keyNode);
                    buildTree(keyNode, value);
                }
            }

        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement item = arr.get(i);
                String index = "[" + i + "]";

                if (isLeaf(item)) {
                    parent.add(new DefaultMutableTreeNode(leafData(index, item)));
                } else {
                    DefaultMutableTreeNode indexNode = new DefaultMutableTreeNode(
                            new JsonNodeData(index, summarize(item),
                                    JsonNodeData.Kind.KEY));
                    parent.add(indexNode);
                    buildTree(indexNode, item);
                }
            }

        } else {
            parent.add(new DefaultMutableTreeNode(leafData("", element)));
        }
    }

    /**
     * Returns {@code true} when {@code element} should be rendered as a leaf.
     *
     * @param element the element to test
     * @return {@code true} for primitives and null
     */
    private static boolean isLeaf(JsonElement element) {
        return element.isJsonPrimitive() || element.isJsonNull();
    }

    /**
     * Build a {@link JsonNodeData} for a leaf node.
     *
     * @param key     key or array index; may be empty for top-level primitives
     * @param element a primitive or null element
     * @return leaf node data
     */
    private static JsonNodeData leafData(String key, JsonElement element) {
        if (element.isJsonNull()) {
            return new JsonNodeData(key, "null", JsonNodeData.Kind.NULL);
        }
        JsonPrimitive p = element.getAsJsonPrimitive();
        if (p.isBoolean()) {
            return new JsonNodeData(key, String.valueOf(p.getAsBoolean()),
                    JsonNodeData.Kind.BOOLEAN);
        }
        if (p.isNumber()) {
            return new JsonNodeData(key, p.getAsString(), JsonNodeData.Kind.NUMBER);
        }
        return new JsonNodeData(key, "\"" + p.getAsString() + "\"",
                JsonNodeData.Kind.STRING);
    }

    /**
     * Return a brief size-summary label for a non-leaf element.
     *
     * @param element an object or array element
     * @return summary string such as {@code {3 keys}} or {@code [12 items]}
     */
    private static String summarize(JsonElement element) {
        if (element.isJsonObject()) {
            int n = element.getAsJsonObject().size();
            return "{" + n + (n == 1 ? " key}" : " keys}");
        }
        if (element.isJsonArray()) {
            int n = element.getAsJsonArray().size();
            return "[" + n + (n == 1 ? " item]" : " items]");
        }
        return "";
    }

    /**
     * Expand the root node and each of its immediate children.
     *
     * @param root the root {@link DefaultMutableTreeNode}
     */
    private void expandTopLevel(DefaultMutableTreeNode root) {
        tree.expandPath(new TreePath(root.getPath()));
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode child =
                    (DefaultMutableTreeNode) root.getChildAt(i);
            tree.expandPath(new TreePath(child.getPath()));
        }
    }

    /**
     * Expand every node in the tree — used after building a filtered model so
     * all matching nodes are immediately visible.
     *
     * @param root the root of the tree to expand
     */
    private void expandAll(DefaultMutableTreeNode root) {
        Enumeration<TreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            TreeNode node = e.nextElement();
            if (node instanceof DefaultMutableTreeNode dmtn) {
                tree.expandPath(new TreePath(dmtn.getPath()));
            }
        }
    }

    /**
     * Return an empty tree model used when no file is loaded.
     *
     * @return an empty {@link DefaultTreeModel}
     */
    private static DefaultTreeModel emptyModel() {
        return new DefaultTreeModel(
                new DefaultMutableTreeNode(
                        new JsonNodeData("(no file)", "", JsonNodeData.Kind.ROOT)));
    }
}