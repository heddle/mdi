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
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Palette panel that displays NATO military symbols in a compact table and
 * supports drag-and-drop placement onto a map.
 *
 * <p>
 * Dragging is initiated only from icon cells (Friendly, Hostile, Neutral,
 * Unknown). When supported by the platform, the drag uses the symbol image
 * itself as the drag image. Symbol placement is handled by the map canvas
 * drop target.
 * </p>
 */
@SuppressWarnings("serial")
public class NatoIconPicker extends JPanel {

    /** Table icon size in pixels. */
    private static final int ICON_SIZE = 20;

    /** Drag image size in pixels. */
    private static final int DRAG_IMAGE_SIZE = 28;

    /** Preferred width of the type column. */
    private static final int COL_LABEL_W = 75;

    /** Preferred width of each icon column. */
    private static final int COL_ICON_W = 36;

    /** Preferred total panel width. */
    private static final int PANEL_WIDTH = COL_LABEL_W + 4 * COL_ICON_W + 12;

    /** Shared background color. */
    private static final Color PANEL_BG = new Color(235, 235, 235);

    /** Symbol table. */
    private JTable table;

    /** Bottom status label. */
    private JLabel statusLabel;

    /**
     * Creates the NATO symbol picker.
     */
    public NatoIconPicker() {
        setLayout(new BorderLayout(2, 2));
        setBackground(PANEL_BG);
        setPreferredSize(new Dimension(PANEL_WIDTH, 0));

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

        DefaultTableCellRenderer labelRenderer = new DefaultTableCellRenderer();
        labelRenderer.setBackground(PANEL_BG);
        labelRenderer.setFont(Fonts.tinyFont);
        table.getColumnModel().getColumn(0).setCellRenderer(labelRenderer);

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

        table.getSelectionModel().addListSelectionListener(e -> updateStatus());
        table.getColumnModel().getSelectionModel().addListSelectionListener(e -> updateStatus());

        installDragGesture();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(PANEL_BG);
        scroll.getViewport().setBackground(PANEL_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder());

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

        add(scroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        
        setBorder(BorderFactory.createEtchedBorder());
    }

    /**
     * Installs the drag gesture recognizer on the table.
     */
    private void installDragGesture() {
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                table, DnDConstants.ACTION_COPY, this::startDrag);
    }

    /**
     * Starts a drag operation if the gesture began on an icon cell.
     *
     * @param dge the drag gesture event
     */
    private void startDrag(DragGestureEvent dge) {
        Point origin = dge.getDragOrigin();
        int row = table.rowAtPoint(origin);
        int col = table.columnAtPoint(origin);

        if (row < 0 || col < 1) {
            return;
        }

        Object cellValue = table.getValueAt(row, col);
        if (!(cellValue instanceof ImageIcon cellIcon)) {
            return;
        }

        String resourcePath = cellIcon.getDescription();
        MilSymbolDescriptor descriptor =
                MilSymbolDescriptor.fromResourcePath(resourcePath, cellIcon);
        if (descriptor == null) {
            return;
        }

        table.setRowSelectionInterval(row, row);
        table.setColumnSelectionInterval(col, col);
        updateStatus();

        Transferable payload = new MilSymbolTransferable(descriptor);

        DragSourceAdapter dragListener = new DragSourceAdapter() {
            @Override
            public void dragDropEnd(DragSourceDropEvent dsde) {
                SwingUtilities.invokeLater(() -> {
                    table.clearSelection();
                    updateStatus();
                });
            }
        };

        try {
            if (DragSource.isDragImageSupported()) {
                BufferedImage dragImage = buildDragImage(resourcePath);
                if (dragImage != null) {
                    Point offset = new Point(dragImage.getWidth() / 2, dragImage.getHeight() / 2);
                    dge.startDrag(
                            DragSource.DefaultCopyDrop,
                            dragImage,
                            offset,
                            payload,
                            dragListener);
                    return;
                }
            }

            dge.startDrag(DragSource.DefaultCopyDrop, payload, dragListener);

        } catch (Exception ex) {
            table.clearSelection();
            updateStatus();
        }
    }

    /**
     * Builds a drag image from the original icon resource.
     *
     * @param resourcePath icon resource path
     * @return drag image, or {@code null} if the resource cannot be loaded
     */
    private BufferedImage buildDragImage(String resourcePath) {
        try {
            ImageIcon original = ImageManager.getInstance().loadImageIcon(resourcePath);
            if (original == null) {
                return null;
            }

            BufferedImage bi = new BufferedImage(
                    DRAG_IMAGE_SIZE, DRAG_IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = bi.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(original.getImage(), 0, 0, DRAG_IMAGE_SIZE, DRAG_IMAGE_SIZE, null);
            g2.dispose();

            return bi;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Loads icon data into the table model.
     *
     * @param model the table model to populate
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
     * Loads and scales a palette icon for table display.
     *
     * @param folder   icon category folder
     * @param filename icon file name
     * @return scaled icon, or {@code null} if not found
     */
    private ImageIcon loadScaledIcon(String folder, String filename) {
        String fullPath = Environment.MDI_RESOURCE_PATH
                + "images/nato_icons/" + folder + "/" + filename;

        ImageIcon original = ImageManager.getInstance().loadImageIcon(fullPath);
        if (original == null) {
            return null;
        }

        BufferedImage bi = new BufferedImage(
                ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(original.getImage(), 0, 0, ICON_SIZE, ICON_SIZE, null);
        g2.dispose();

        ImageIcon scaled = new ImageIcon(bi);
        scaled.setDescription(fullPath);
        return scaled;
    }

    /**
     * Updates the status label to reflect the current cell selection.
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
     * Returns the last two path segments of a resource path.
     *
     * @param path full resource path
     * @return short display form
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