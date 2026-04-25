package edu.cnu.mdi.mapping.milsym;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.transfer.PaletteDragSupport;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.Environment;

import java.awt.BorderLayout;

/**
 * Palette panel that displays NATO military symbols in a compact table and
 * supports drag-and-drop placement onto a map canvas.
 *
 * <h2>Layout</h2>
 * <p>
 * The panel shows one symbol category per row. The first column contains the
 * category name (e.g. "Infantry", "Armor"); the remaining four columns show
 * the Friendly, Hostile, Neutral, and Unknown affiliation icons for that
 * category. Any icon cell can be dragged onto a {@link edu.cnu.mdi.mapping.container.MapContainer}
 * to place the corresponding symbol on the map.
 * </p>
 *
 * <h2>Drag-and-drop</h2>
 * <p>
 * Drag support is managed by a {@link PaletteDragSupport} instance created in
 * the constructor. {@code NatoIconPicker} is responsible only for:
 * </p>
 * <ol>
 *   <li>Deciding whether the drag origin is on a draggable icon cell and, if
 *       so, building the {@link MilSymbolTransferable} payload.</li>
 *   <li>Clearing the table selection when the drag ends
 *       ({@link PaletteDragSupport.PaletteDragSource#onDragEnd()}).</li>
 * </ol>
 * <p>
 * All Swing DnD ceremony — gesture recognition, drag image display, cursor
 * management, and end-of-drag cleanup — is handled by
 * {@link PaletteDragSupport}.
 * </p>
 *
 * <h2>Icon resources</h2>
 * <p>
 * Icons are loaded at construction time from the classpath under
 * {@code <MDI_RESOURCE_PATH>/images/nato_icons/<category>/<affiliation>.png}.
 * Both IDE and JAR execution are supported via the JAR filesystem fallback in
 * {@link #loadIconData}.
 * </p>
 */
@SuppressWarnings("serial")
public class NatoIconPicker extends JPanel {

    // -------------------------------------------------------------------------
    // Display constants
    // -------------------------------------------------------------------------

    /** Table icon size in pixels. */
    private static final int ICON_SIZE = 20;

    /** Drag image size in pixels (square). */
    private static final int DRAG_IMAGE_SIZE = 28;

    /** Preferred width of the category-name column. */
    private static final int COL_LABEL_W = 75;

    /** Preferred width of each affiliation-icon column. */
    private static final int COL_ICON_W = 36;

    /** Preferred total panel width derived from column widths. */
    private static final int PANEL_WIDTH = COL_LABEL_W + 4 * COL_ICON_W + 12;

    /** Background color shared by all sub-components. */
    private static final Color PANEL_BG = new Color(235, 235, 235);

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------

    /** The symbol table. */
    private JTable table;

    /** Status label at the bottom of the panel. */
    private JLabel statusLabel;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates the NATO symbol picker, loads icon data, and installs
     * drag-and-drop support.
     */
    public NatoIconPicker() {
        setLayout(new BorderLayout(2, 2));
        setBackground(PANEL_BG);
        setPreferredSize(new Dimension(PANEL_WIDTH, 0));

        buildTable();
        buildBottomPanel();
        setBorder(BorderFactory.createEtchedBorder());

        // Delegate all Swing DnD ceremony to PaletteDragSupport.
        // We supply:
        //   1. A PaletteDragSource lambda that decides what to drag and clears
        //      the selection when the drag ends.
        //   2. A DragImageProvider lambda that scales the icon image for display
        //      under the cursor.
        new PaletteDragSupport(
                table,
                new PaletteDragSupport.PaletteDragSource() {
                    @Override
                    public Transferable createTransferable(Point dragOrigin) {
                        return buildTransferable(dragOrigin);
                    }

                    @Override
                    public void onDragEnd() {
                        // Defer to EDT — dragDropEnd may not be on the EDT on all platforms.
                        SwingUtilities.invokeLater(() -> {
                            table.clearSelection();
                            updateStatus();
                        });
                    }
                },
                this::buildDragImage);
    }

    // -------------------------------------------------------------------------
    // PaletteDragSource callbacks
    // -------------------------------------------------------------------------

    /**
     * Decide what to drag from the given gesture origin.
     *
     * <p>Returns a {@link MilSymbolTransferable} when the gesture began on an
     * icon cell (column ≥ 1) and the cell holds a recognized
     * {@link ImageIcon}. Returns {@code null} — cancelling the drag — if the
     * gesture started on the label column or on an empty cell.</p>
     *
     * <p>As a side effect, the cell under the cursor is highlighted by setting
     * the table selection, so the user sees which symbol is being dragged.</p>
     *
     * @param dragOrigin the point, in table coordinates, where the gesture began
     * @return the transferable payload, or {@code null} to cancel
     */
    private Transferable buildTransferable(Point dragOrigin) {
        int row = table.rowAtPoint(dragOrigin);
        int col = table.columnAtPoint(dragOrigin);

        // Column 0 is the label column — not draggable.
        if (row < 0 || col < 1) {
            return null;
        }

        Object cellValue = table.getValueAt(row, col);
        if (!(cellValue instanceof ImageIcon cellIcon)) {
            return null;
        }

        String resourcePath = cellIcon.getDescription();
        MilSymbolDescriptor descriptor =
                MilSymbolDescriptor.fromResourcePath(resourcePath, cellIcon);
        if (descriptor == null) {
            return null;
        }

        // Highlight the dragged cell so the user sees the selection.
        table.setRowSelectionInterval(row, row);
        table.setColumnSelectionInterval(col, col);
        updateStatus();

        return new MilSymbolTransferable(descriptor);
    }

    /**
     * Build the drag image for the icon at the given gesture origin.
     *
     * <p>Loads the full-resolution icon from the classpath via
     * {@link ImageManager} and scales it to {@link #DRAG_IMAGE_SIZE} pixels
     * using {@link PaletteDragSupport#scaleToDragImage}. Returns {@code null}
     * if the resource cannot be loaded, in which case
     * {@link PaletteDragSupport} falls back to the default system cursor.</p>
     *
     * @param dragOrigin the point where the drag gesture began (used to identify
     *                   the cell and therefore the resource path)
     * @return the scaled drag image, or {@code null}
     */
    private BufferedImage buildDragImage(Point dragOrigin) {
        int row = table.rowAtPoint(dragOrigin);
        int col = table.columnAtPoint(dragOrigin);

        if (row < 0 || col < 1) {
            return null;
        }

        Object cellValue = table.getValueAt(row, col);
        if (!(cellValue instanceof ImageIcon cellIcon)) {
            return null;
        }

        String resourcePath = cellIcon.getDescription();
        try {
            ImageIcon original = ImageManager.getInstance().loadImageIcon(resourcePath);
            if (original == null) {
                return null;
            }
            return PaletteDragSupport.scaleToDragImage(original.getImage(), DRAG_IMAGE_SIZE);
        } catch (Exception ex) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // UI construction helpers
    // -------------------------------------------------------------------------

    /**
     * Build and configure the symbol table, including column renderers, column
     * widths, and selection listeners. Populates the model via
     * {@link #loadIconData}.
     */
    private void buildTable() {
        DefaultTableModel model = new DefaultTableModel(
                new String[] { "Type", "F", "H", "N", "U" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return (c == 0) ? String.class : Icon.class;
            }
        };
        loadIconData(model);

        table = new JTable(model);
        table.setBackground(PANEL_BG);
        table.setRowHeight(ICON_SIZE + 8);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setCellSelectionEnabled(true);
        table.setFont(Fonts.tinyFont);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(1, 1));

        // Label column renderer.
        DefaultTableCellRenderer labelRenderer = new DefaultTableCellRenderer();
        labelRenderer.setBackground(PANEL_BG);
        labelRenderer.setFont(Fonts.tinyFont);
        table.getColumnModel().getColumn(0).setCellRenderer(labelRenderer);

        // Icon column renderer — centers the icon and applies selection color.
        DefaultTableCellRenderer iconRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = new JLabel((Icon) val, JLabel.CENTER);
                lbl.setBackground(sel ? t.getSelectionBackground() : PANEL_BG);
                lbl.setOpaque(true);
                return lbl;
            }
        };
        for (int c = 1; c <= 4; c++) {
            table.getColumnModel().getColumn(c).setCellRenderer(iconRenderer);
        }

        // Fix column widths so the panel does not reflow on selection changes.
        TableColumn labelCol = table.getColumnModel().getColumn(0);
        labelCol.setPreferredWidth(COL_LABEL_W);
        labelCol.setMinWidth(COL_LABEL_W);
        labelCol.setMaxWidth(COL_LABEL_W);

        for (int c = 1; c <= 4; c++) {
            TableColumn tc = table.getColumnModel().getColumn(c);
            tc.setPreferredWidth(COL_ICON_W);
            tc.setMinWidth(COL_ICON_W);
            tc.setMaxWidth(COL_ICON_W);
        }

        table.getTableHeader().setBackground(PANEL_BG);
        table.getTableHeader().setFont(Fonts.tinyFont);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);

        // Keep the status label in sync with the cell selection.
        table.getSelectionModel().addListSelectionListener(e -> updateStatus());
        table.getColumnModel().getSelectionModel().addListSelectionListener(e -> updateStatus());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(PANEL_BG);
        scroll.getViewport().setBackground(PANEL_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        add(scroll, BorderLayout.CENTER);
    }

    /**
     * Build and add the bottom strip containing the status label and the
     * "Clear" button.
     */
    private void buildBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(2, 0));
        bottomPanel.setBackground(PANEL_BG);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        statusLabel = new JLabel("None");
        statusLabel.setFont(Fonts.tinyFont);
        statusLabel.setBackground(PANEL_BG);
        statusLabel.setOpaque(true);

        JButton clearButton = new JButton("Clear");
        clearButton.setFont(Fonts.tinyFont);
        clearButton.setMargin(new Insets(1, 4, 1, 4));
        clearButton.addActionListener(e -> {
            table.clearSelection();
            updateStatus();
        });

        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        bottomPanel.add(clearButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    /**
     * Populate {@code model} with one row per NATO icon category found under
     * the {@code nato_icons} resource directory.
     *
     * <p>Each row contains the category display name (column 0) and one scaled
     * {@link ImageIcon} per affiliation — Friendly, Hostile, Neutral, Unknown
     * (columns 1–4). Rows are sorted alphabetically by category folder name.</p>
     *
     * <p>Works both when run from an IDE (plain filesystem) and when packaged
     * inside a JAR (uses {@link java.nio.file.FileSystems#newFileSystem} to
     * open the JAR as a {@link java.nio.file.FileSystem}).</p>
     *
     * @param model the table model to populate; rows are appended, not replaced
     */
    private void loadIconData(DefaultTableModel model) {
        String resourcePath = Environment.MDI_RESOURCE_PATH + "images/nato_icons/";
        List<String> folders = new ArrayList<>();

        try {
            var resourceUri = getClass().getResource(resourcePath).toURI();
            java.nio.file.Path rootPath;

            if (resourceUri.getScheme().equals("jar")) {
                var fs = java.nio.file.FileSystems.newFileSystem(
                        resourceUri, java.util.Collections.emptyMap());
                rootPath = fs.getPath(resourcePath);
            } else {
                rootPath = java.nio.file.Paths.get(resourceUri);
            }

            try (var stream = java.nio.file.Files.list(rootPath)) {
                folders = stream
                        .filter(java.nio.file.Files::isDirectory)
                        .map(p -> p.getFileName().toString())
                        .filter(name -> !name.startsWith("."))
                        .sorted()
                        .toList();
            }
        } catch (Exception e) {
            System.err.println("NatoIconPicker: failed to enumerate icon folders: " + e.getMessage());
        }

        for (String folder : folders) {
            model.addRow(new Object[] {
                    folder.replace("_", " "),
                    loadScaledIcon(folder, "friendly.png"),
                    loadScaledIcon(folder, "hostile.png"),
                    loadScaledIcon(folder, "neutral.png"),
                    loadScaledIcon(folder, "unknown.png")
            });
        }
    }

    /**
     * Load a single icon from the NATO icon resource tree, scale it to
     * {@link #ICON_SIZE} pixels for table display, and embed its full resource
     * path as the {@link ImageIcon#getDescription() description} so that
     * {@link #buildTransferable} and {@link #buildDragImage} can recover it
     * later without an additional table-to-path lookup.
     *
     * @param folder   the icon category folder name (e.g. {@code "infantry"})
     * @param filename the icon file name (e.g. {@code "friendly.png"})
     * @return a scaled {@link ImageIcon} with its description set, or
     *         {@code null} if the resource cannot be found or loaded
     */
    private ImageIcon loadScaledIcon(String folder, String filename) {
        String fullPath = Environment.MDI_RESOURCE_PATH
                + "images/nato_icons/" + folder + "/" + filename;

        ImageIcon original = ImageManager.getInstance().loadImageIcon(fullPath);
        if (original == null) {
            return null;
        }

        BufferedImage bi = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(original.getImage(), 0, 0, ICON_SIZE, ICON_SIZE, null);
        g2.dispose();

        ImageIcon scaled = new ImageIcon(bi);
        // Store the full path so buildTransferable/buildDragImage can recover it
        // from the icon without a separate row→path lookup.
        scaled.setDescription(fullPath);
        return scaled;
    }

    // -------------------------------------------------------------------------
    // Status label
    // -------------------------------------------------------------------------

    /**
     * Refresh the status label and icon to reflect the currently selected cell.
     *
     * <p>Shows the last two path segments of the selected icon's resource path
     * (e.g. {@code "infantry/friendly.png"}) alongside a thumbnail. Resets to
     * {@code "None"} when nothing is selected or the selected cell is not an
     * icon column.</p>
     */
    private void updateStatus() {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();

        if (row >= 0 && col > 0) {
            Object val = table.getValueAt(row, col);
            if (val instanceof ImageIcon icon) {
                statusLabel.setText(shortPath(icon.getDescription()));
                statusLabel.setIcon(icon);
                return;
            }
        }

        statusLabel.setText("None");
        statusLabel.setIcon(null);
    }

    /**
     * Return the last two slash-separated segments of {@code path}.
     *
     * <p>Used to produce a compact display string such as
     * {@code "infantry/friendly.png"} from a full classpath resource path.</p>
     *
     * @param path the full resource path; may be {@code null}
     * @return a two-segment short form, or the original path if it has fewer
     *         than two segments; never {@code null}
     */
    private static String shortPath(String path) {
        if (path == null) {
            return "";
        }
        String[] parts = path.replace('\\', '/').split("/");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + "/" + parts[parts.length - 1];
        }
        return path;
    }
}