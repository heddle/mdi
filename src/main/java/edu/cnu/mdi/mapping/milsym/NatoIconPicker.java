package edu.cnu.mdi.mapping.milsym;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import edu.cnu.mdi.graphics.ImageManager;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.Environment;

import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Palette panel that displays NATO military symbols in a compact grid and lets
 * the user <b>drag</b> an icon onto the map canvas to place a
 * {@link MapMilSymbolItem}.
 *
 * <h2>Drag-and-drop protocol</h2>
 * <ol>
 *   <li>The user clicks a cell in the icon table. The row selects normally.</li>
 *   <li>The user begins dragging that cell. A {@link DragGestureRecognizer}
 *       detects the gesture and calls {@link #startDrag}.</li>
 *   <li>{@link #startDrag} converts the selected {@link ImageIcon} to a
 *       {@link Cursor} that follows the pointer, then initiates the AWT drag
 *       with a {@link MilSymbolTransferable} payload.</li>
 *   <li>The map's {@link edu.cnu.mdi.mapping.container.MapContainer} drop
 *       target receives the drop, converts the drop point to lat/lon, and
 *       creates a {@link edu.cnu.mdi.mapping.item.MapMilSymbolItem}.</li>
 *   <li>On drag end (success or cancel) the cursor is restored and the table
 *       selection is cleared so the status bar shows "None".</li>
 * </ol>
 *
 * <h2>Toolbar interaction</h2>
 * <p>The drag gesture is entirely independent of the active toolbar tool.
 * Whatever tool is selected remains active after the drop (Option C behavior):
 * the symbol placement is additive and does not switch modes.</p>
 */
@SuppressWarnings("serial")
public class NatoIconPicker extends JPanel {

    // -------------------------------------------------------------------------
    // Layout constants
    // -------------------------------------------------------------------------

    /** Icon size in the table cells, kept small to minimise panel width. */
    private static final int ICON_SIZE    = 20;

    /** Size of the drag cursor built from the icon image. */
    private static final int CURSOR_SIZE  = 28;

    private static final int COL_LABEL_W  = 75;
    private static final int COL_ICON_W   = 36;
    private static final int PANEL_WIDTH  = COL_LABEL_W + 4 * COL_ICON_W + 12;

    /** Light gray shared by all surfaces. */
    private static final Color PANEL_BG   = new Color(235, 235, 235);

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private JTable  table;
    private JLabel  statusLabel;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates the NATO Icon Picker panel.
     *
     * <p>No external listener is needed: symbol placement is handled entirely
     * by the drag-and-drop mechanism. The constructor no longer accepts a
     * {@code Consumer<String>} callback.</p>
     */
    public NatoIconPicker() {

        setLayout(new BorderLayout(2, 2));
        setBackground(PANEL_BG);
        setPreferredSize(new Dimension(PANEL_WIDTH, 0));

        // ---- table model ----
        String[] columnNames = { "Type", "F", "H", "N", "U" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 ? String.class : Icon.class;
            }
        };
        loadIconData(model);

        // ---- table ----
        table = new JTable(model);
        table.setBackground(PANEL_BG);
        table.setRowHeight(ICON_SIZE + 8);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setCellSelectionEnabled(true);
        table.setFont(Fonts.tinyFont);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(1, 1));

        // Label column
        DefaultTableCellRenderer labelRenderer = new DefaultTableCellRenderer();
        labelRenderer.setBackground(PANEL_BG);
        labelRenderer.setFont(Fonts.tinyFont);
        table.getColumnModel().getColumn(0).setCellRenderer(labelRenderer);

        // Icon columns
        DefaultTableCellRenderer iconRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JLabel lbl = new JLabel((Icon) val, JLabel.CENTER);
                lbl.setBackground(sel ? t.getSelectionBackground() : PANEL_BG);
                lbl.setOpaque(true);
                return lbl;
            }
        };
        for (int c = 1; c <= 4; c++) {
            table.getColumnModel().getColumn(c).setCellRenderer(iconRenderer);
        }

        // Lock column widths
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

        // Header styling
        table.getTableHeader().setBackground(PANEL_BG);
        table.getTableHeader().setFont(Fonts.tinyFont);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);

        // Update status bar when selection changes
        table.getSelectionModel().addListSelectionListener(e -> updateStatus());
        table.getColumnModel().getSelectionModel().addListSelectionListener(e -> updateStatus());

        // ---- drag-and-drop ----
        installDragGesture();

        // ---- scroll pane ----
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(PANEL_BG);
        scroll.getViewport().setBackground(PANEL_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        // ---- bottom bar ----
        JPanel bottomPanel = new JPanel(new BorderLayout(2, 0));
        bottomPanel.setBackground(PANEL_BG);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        statusLabel = new JLabel("None");
        statusLabel.setFont(Fonts.tinyFont);
        statusLabel.setBackground(PANEL_BG);
        statusLabel.setOpaque(true);

        JButton deselectBtn = new JButton("Clear");
        deselectBtn.setFont(Fonts.tinyFont);
        deselectBtn.setMargin(new Insets(1, 4, 1, 4));
        deselectBtn.addActionListener(e -> {
            table.clearSelection();
            updateStatus();
        });

        bottomPanel.add(statusLabel,  BorderLayout.CENTER);
        bottomPanel.add(deselectBtn,  BorderLayout.EAST);

        add(scroll,       BorderLayout.CENTER);
        add(bottomPanel,  BorderLayout.SOUTH);
    }

    // -------------------------------------------------------------------------
    // Drag-and-drop
    // -------------------------------------------------------------------------

    /**
     * Registers an AWT {@link DragGestureRecognizer} on the table so that a
     * click-and-drag on any icon cell initiates the symbol placement gesture.
     *
     * <p>Only icon cells (columns 1–4) produce a drag; clicking the label
     * column (column 0) or an empty cell is silently ignored.</p>
     */
    private void installDragGesture() {
        DragSource ds = DragSource.getDefaultDragSource();
        ds.createDefaultDragGestureRecognizer(
                table,
                DnDConstants.ACTION_COPY,
                new DragGestureListener() {
                    @Override
                    public void dragGestureRecognized(DragGestureEvent dge) {
                        startDrag(dge);
                    }
                });
    }

    /**
     * Initiates an AWT drag if the gesture originated over a valid icon cell.
     *
     * <p>The drag payload is a {@link MilSymbolTransferable} built from the
     * icon's embedded {@link MilSymbolDescriptor}. The drag cursor is a
     * scaled version of the icon image so the user sees exactly what they are
     * about to place.</p>
     *
     * @param dge the gesture event from the recognizer
     */
    private void startDrag(DragGestureEvent dge) {
        // Resolve the table cell under the drag origin.
        Point origin = dge.getDragOrigin();
        int row = table.rowAtPoint(origin);
        int col = table.columnAtPoint(origin);

        // Only icon columns (1-4) are draggable.
        if (row < 0 || col < 1) {
            return;
        }

        Object cellValue = table.getValueAt(row, col);
        if (!(cellValue instanceof ImageIcon icon)) {
            return;
        }

        // The descriptor is stored in the icon's description field as a
        // resource path. Resolve it to a MilSymbolDescriptor.
        String resourcePath = icon.getDescription();
        MilSymbolDescriptor descriptor = MilSymbolDescriptor.fromResourcePath(resourcePath, icon);
        if (descriptor == null) {
            return;
        }

        // Select the cell so the status bar reflects the drag source.
        table.setRowSelectionInterval(row, row);
        table.setColumnSelectionInterval(col, col);
        updateStatus();

        // Build a cursor from the icon image.
        Cursor dragCursor = buildDragCursor(icon);

        // Build the transferable payload.
        Transferable transferable = new MilSymbolTransferable(descriptor);

        // Register a drag-source listener that clears selection when the drag ends,
        // whether dropped successfully or cancelled.
        dge.getDragSource().addDragSourceListener(new DragSourceAdapter() {
            @Override
            public void dragDropEnd(DragSourceDropEvent dsde) {
                SwingUtilities.invokeLater(() -> {
                    table.clearSelection();
                    updateStatus();
                });
                // Self-removing listener to avoid accumulation.
                dge.getDragSource().removeDragSourceListener(this);
            }
        });

        // Start the drag. The cursor changes back automatically when the drag ends.
        dge.startDrag(dragCursor, transferable);
    }

    /**
     * Builds a {@link Cursor} from the given icon by scaling it to
     * {@value #CURSOR_SIZE}×{@value #CURSOR_SIZE} pixels and using the
     * icon centre as the hot-spot.
     *
     * <p>Falls back to the default move cursor if the toolkit does not support
     * custom cursors at the required size.</p>
     *
     * @param icon source icon; its {@code Image} is scaled
     * @return a custom cursor, or {@link Cursor#getDefaultCursor()} on failure
     */
    private static Cursor buildDragCursor(ImageIcon icon) {
        try {
            Toolkit tk = Toolkit.getDefaultToolkit();

            // Scale the icon image to the preferred cursor size.
            // The toolkit may round this to its supported size.
            Image scaled = icon.getImage()
                    .getScaledInstance(CURSOR_SIZE, CURSOR_SIZE, Image.SCALE_SMOOTH);

            // Convert to BufferedImage so we can hand it to createCustomCursor.
            BufferedImage bi = new BufferedImage(CURSOR_SIZE, CURSOR_SIZE,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = bi.createGraphics();
            g2.drawImage(scaled, 0, 0, null);
            g2.dispose();

            Point hotSpot = new Point(CURSOR_SIZE / 2, CURSOR_SIZE / 2);
            return tk.createCustomCursor(bi, hotSpot, "milsym-drag");

        } catch (Exception ex) {
            return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void loadIconData(DefaultTableModel model) {
        String resourcePath = Environment.MDI_RESOURCE_PATH + "images/nato_icons/";
        List<String> folders = new ArrayList<>();

        try {
            var resourceUri = getClass().getResource(resourcePath).toURI();
            java.nio.file.Path rootPath;
            if (resourceUri.getScheme().equals("jar")) {
                var fileSystem = java.nio.file.FileSystems.newFileSystem(
                        resourceUri, java.util.Collections.emptyMap());
                rootPath = fileSystem.getPath(resourcePath);
            } else {
                rootPath = java.nio.file.Paths.get(resourceUri);
            }
            try (var stream = java.nio.file.Files.list(rootPath)) {
                folders = stream
                        .filter(java.nio.file.Files::isDirectory)
                        .map(path -> path.getFileName().toString())
                        .filter(name -> !name.startsWith("."))
                        .sorted()
                        .toList();
            }
        } catch (Exception e) {
            System.err.println("Failed to load NATO icon folders: " + e.getMessage());
        }

        for (String folder : folders) {
            model.addRow(new Object[]{
                folder.replace("_", " "),
                getIcon(folder, "friendly.png"),
                getIcon(folder, "hostile.png"),
                getIcon(folder, "neutral.png"),
                getIcon(folder, "unknown.png")
            });
        }
    }

    // Loads an icon from the given folder and filename, scales it to the table cell size,
    private ImageIcon getIcon(String folder, String filename) {
        String fullPath = Environment.MDI_RESOURCE_PATH
                + "images/nato_icons/" + folder + "/" + filename;
        ImageIcon original = ImageManager.getInstance().loadImageIcon(fullPath);
        if (original == null) {
            return null;
        }
        Image scaled = original.getImage()
                .getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(scaled);
        icon.setDescription(fullPath); // full resource path stored for DnD
        return icon;
    }

    /** Updates the status bar to reflect the current table selection. */
    private void updateStatus() {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();

        if (row != -1 && col > 0) {
            ImageIcon icon = (ImageIcon) table.getValueAt(row, col);
            if (icon != null) {
                statusLabel.setText(shortPath(icon.getDescription()));
                statusLabel.setIcon(icon);
                return;
            }
        }
        statusLabel.setText("None");
        statusLabel.setIcon(null);
    }

    /**
     * Returns the last two path segments — parent folder + filename —
     * e.g. {@code "Armour/friendly.png"}.
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

    // -------------------------------------------------------------------------
    // Standalone test
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("NATO Icon Picker — Drag to map");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new NatoIconPicker());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}