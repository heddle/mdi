package edu.cnu.mdi.container;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.table.AbstractTableModel;

import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;

/**
 * A lightweight inspector dialog for viewing and editing a container's z-layers.
 * <p>
 * Displays all layers in draw order (bottom → top), including protected layers:
 * <ul>
 *   <li>Connections (protected)</li>
 *   <li>User layers (reorderable)</li>
 *   <li>Annotations (protected)</li>
 * </ul>
 *
 * <h2>Operations</h2>
 * <ul>
 *   <li>Toggle {@link Layer#setVisible(boolean)} for any layer.</li>
 *   <li>Toggle {@link Layer#setLocked(boolean)} for any layer.</li>
 *   <li>Reorder user layers via drag & drop and Up/Down buttons.</li>
 *   <li>Add/Rename/Delete user layers.</li>
 * </ul>
 *
 * <h2>Delete semantics</h2>
 * When deleting a user layer, the user can choose to move items into the container's
 * default layer (typically "Content") or discard them. Item moves use
 * {@code Layer.removeSilently(AItem)} / {@code Layer.addSilently(AItem)} so the
 * layer-level ADDED/DELETED notifications are not fired.
 */
@SuppressWarnings("serial")
public class LayerInspectorDialog extends JDialog {

    private final BaseContainer container;
    private final LayerTableModel model;
    private final JTable table;

    /**
     * Convenience to show a modeless dialog positioned relative to a parent component.
     *
     * @param parent    parent component for positioning (may be null)
     * @param container target container (must be a {@link BaseContainer})
     */
    public static void show(Component parent, BaseContainer container) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        LayerInspectorDialog dlg = new LayerInspectorDialog(owner, container);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    /**
     * Construct the dialog.
     *
     * @param owner     owner window (may be null)
     * @param container target container (non-null)
     */
    public LayerInspectorDialog(Window owner, BaseContainer container) {
        super(owner, "Layer Inspector", ModalityType.MODELESS);
        if (container == null) {
            throw new IllegalArgumentException("container cannot be null");
        }
        this.container = container;
        this.model = new LayerTableModel(container);
        this.table = new JTable(model);

        buildUI();
    }

    private void buildUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // ESC closes
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);

        JLabel hint = new JLabel("Bottom → Top (draw order). Drag user layers to reorder. Protected layers are fixed.");
        hint.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 8));
        add(hint, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);

        // Drag reorder rows (user layers only; protected rows reject drop)
        table.setDragEnabled(true);
        table.setDropMode(javax.swing.DropMode.INSERT_ROWS);
        table.setTransferHandler(new LayerRowTransferHandler(container, model));

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(640, 300));
        add(sp, BorderLayout.CENTER);

        add(buildButtons(), BorderLayout.SOUTH);

        pack();
    }

    private JPanel buildButtons() {
        JPanel south = new JPanel(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton(new AbstractAction("Add…") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                String name = promptForName("New layer name:", "Layer");
                if (name == null) {
                    return;
                }
                Layer layer = new Layer(container, name); // auto-registers to container
                // Ensure it lands in user list (BaseContainer.addLayer already does this)
                model.refresh();
                selectLayer(layer);
                container.setDirty(true);
                container.refresh();
            }
        });

        JButton rename = new JButton(new AbstractAction("Rename…") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Layer layer = selectedLayer();
                if (layer == null) {
                    return;
                }
                if (container.isProtectedLayer(layer)) {
                    info("Protected layers cannot be renamed.");
                    return;
                }
                String name = promptForName("Layer name:", layer.getName());
                if (name == null) {
                    return;
                }
                layer.setName(name);
                model.refresh();
                selectLayer(layer);
                container.setDirty(true);
                container.refresh();
            }
        });

        JButton delete = new JButton(new AbstractAction("Delete…") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Layer layer = selectedLayer();
                if (layer == null) {
                    return;
                }
                if (container.isProtectedLayer(layer)) {
                    info("Protected layers cannot be deleted.");
                    return;
                }
                if (layer == container.getDefaultLayer()) {
                    info("The default Content layer cannot be deleted.");
                    return;
                }

                int choice = JOptionPane.showOptionDialog(
                        LayerInspectorDialog.this,
                        "Delete layer \"" + layer.getName() + "\"?\n\n"
                                + "Choose whether to move its items to the default layer (\""
                                + container.getDefaultLayer().getName() + "\") or discard them.",
                        "Delete Layer",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new Object[] { "Move Items", "Discard Items", "Cancel" },
                        "Move Items");

                if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                    return;
                }

                boolean moveItems = (choice == 0);
                deleteUserLayer(layer, moveItems);

                model.refresh();
                container.setDirty(true);
                container.refresh();
            }
        });

        left.add(add);
        left.add(rename);
        left.add(delete);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton up = new JButton(new AbstractAction("Up") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Layer layer = selectedLayer();
                if (layer == null || container.isProtectedLayer(layer) || layer == container.getDefaultLayer() && container._layers.size() == 1) {
                    return;
                }
                moveUserLayer(layer, -1);
            }
        });

        JButton down = new JButton(new AbstractAction("Down") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Layer layer = selectedLayer();
                if (layer == null || container.isProtectedLayer(layer) || layer == container.getDefaultLayer() && container._layers.size() == 1) {
                    return;
                }
                moveUserLayer(layer, +1);
            }
        });

        JButton close = new JButton(new AbstractAction("Close") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });

        right.add(up);
        right.add(down);
        right.add(close);

        south.add(left, BorderLayout.WEST);
        south.add(right, BorderLayout.EAST);

        return south;
    }

    private void deleteUserLayer(Layer layer, boolean moveItemsToDefault) {
        // Remove from user list
        int idx = container._layers.indexOf(layer);
        if (idx < 0) {
            return;
        }

        if (moveItemsToDefault) {
            Layer dst = container.getDefaultLayer();
            if (dst == null) {
                // If no default, we just discard by removing the layer.
                container._layers.remove(idx);
                return;
            }

            // Move items using normal remove/add. This will fire the standard layer notifications,
            // and will invoke prepareForRemoval() on the source removal path as per Layer.remove().
            // If you later decide you truly need "silent" moves, we can wire those in at the
            // BaseContainer level instead.
            List<AItem> items = new ArrayList<>(layer.getAllItems());
            for (AItem it : items) {
                layer.remove(it);
                dst.add(it);
            }
        }

        container._layers.remove(idx);
    }


    private void moveUserLayer(Layer layer, int delta) {
        int idx = container._layers.indexOf(layer);
        if (idx < 0) {
            return;
        }
        int newIdx = idx + delta;
        newIdx = Math.max(0, Math.min(newIdx, container._layers.size() - 1));
        if (newIdx == idx) {
            return;
        }

        container._layers.remove(idx);
        container._layers.add(newIdx, layer);

        model.refresh();
        selectLayer(layer);
        container.setDirty(true);
        container.refresh();
    }

    private Layer selectedLayer() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.layerAt(row);
    }

    private void selectLayer(Layer layer) {
        int row = model.indexOf(layer);
        if (row >= 0) {
            table.getSelectionModel().setSelectionInterval(row, row);
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
        }
    }

    private static void info(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Layers", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String promptForName(String label, String initial) {
        JTextField tf = new JTextField(initial == null ? "" : initial, 22);
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.add(new JLabel(label), BorderLayout.WEST);
        p.add(tf, BorderLayout.CENTER);

        int rc = JOptionPane.showConfirmDialog(null, p, "Layers", JOptionPane.OK_CANCEL_OPTION);
        if (rc != JOptionPane.OK_OPTION) {
            return null;
        }
        String s = tf.getText();
        if (s == null) {
            return null;
        }
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * Table model over all layers (bottom → top).
     */
    private static final class LayerTableModel extends AbstractTableModel {

        private static final int COL_NAME = 0;
        private static final int COL_VISIBLE = 1;
        private static final int COL_LOCKED = 2;
        private static final int COL_PROTECTED = 3;

        private final BaseContainer container;
        private List<Layer> snapshot;

        LayerTableModel(BaseContainer container) {
            this.container = container;
            refresh();
        }

        void refresh() {
            snapshot = new ArrayList<>(container.getAllLayers()); // bottom -> top
            fireTableDataChanged();
        }

        Layer layerAt(int row) {
            return (row < 0 || row >= snapshot.size()) ? null : snapshot.get(row);
        }

        int indexOf(Layer layer) {
            return snapshot.indexOf(layer);
        }

        @Override
        public int getRowCount() {
            return snapshot == null ? 0 : snapshot.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_NAME -> "Layer";
                case COL_VISIBLE -> "Visible";
                case COL_LOCKED -> "Locked";
                case COL_PROTECTED -> "Protected";
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case COL_VISIBLE, COL_LOCKED, COL_PROTECTED -> Boolean.class;
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == COL_VISIBLE || columnIndex == COL_LOCKED;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Layer layer = layerAt(rowIndex);
            if (layer == null) {
                return null;
            }
            return switch (columnIndex) {
                case COL_NAME -> layer.getName();
                case COL_VISIBLE -> layer.isVisible();
                case COL_LOCKED -> layer.isLocked();
                case COL_PROTECTED -> container.isProtectedLayer(layer);
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Layer layer = layerAt(rowIndex);
            if (layer == null) {
                return;
            }
            boolean val = Boolean.TRUE.equals(aValue);

            if (columnIndex == COL_VISIBLE) {
                layer.setVisible(val);
            } else if (columnIndex == COL_LOCKED) {
                layer.setLocked(val);
            }

            container.setDirty(true);
            container.refresh();
            fireTableRowsUpdated(rowIndex, rowIndex);
        }
    }

    /**
     * Transfer handler that supports row drag-reorder for user layers.
     * <p>
     * The table contains protected rows at bottom/top; only user-layer rows may be moved.
     */
    private static final class LayerRowTransferHandler extends TransferHandler {

        private static final DataFlavor ROW_FLAVOR = new DataFlavor(Integer.class, "RowIndex");

        private final BaseContainer container;
        private final LayerTableModel model;
        private int sourceRow = -1;

        LayerRowTransferHandler(BaseContainer container, LayerTableModel model) {
            this.container = container;
            this.model = model;
        }

        @Override
        protected Transferable createTransferable(javax.swing.JComponent c) {
            JTable t = (JTable) c;
            sourceRow = t.getSelectedRow();
            final int row = sourceRow;

            return new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[] { ROW_FLAVOR };
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return ROW_FLAVOR.equals(flavor);
                }

                @Override
                public Object getTransferData(DataFlavor flavor) {
                    return Integer.valueOf(row);
                }
            };
        }

        @Override
        public int getSourceActions(javax.swing.JComponent c) {
            return MOVE;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop() || !support.isDataFlavorSupported(ROW_FLAVOR)) {
                return false;
            }

            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            int targetRow = dl.getRow();

            Layer srcLayer = model.layerAt(sourceRow);
            if (srcLayer == null || container.isProtectedLayer(srcLayer) || srcLayer == container.getDefaultLayer() && container._layers.size() == 1) {
                return false;
            }

            // Allow drop within table; we clamp to user range in importData.
            return targetRow >= 0 && targetRow <= model.getRowCount();
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            int targetRow = dl.getRow();

            Layer dragged = model.layerAt(sourceRow);
            if (dragged == null || container.isProtectedLayer(dragged)) {
                return false;
            }

            // Map table row -> user-layer index:
            // Table is bottom->top: [0]=Connections, [1..userCount]=user layers, [last]=Annotations
            int userCount = container._layers.size();
            if (userCount <= 0) {
                return false;
            }

            int srcUser = container._layers.indexOf(dragged);
            if (srcUser < 0) {
                return false;
            }

            int minRow = 1;                  // first user row in table
            int maxRowExclusive = 1 + userCount; // one past last user row in table
            int clamped = Math.max(minRow, Math.min(targetRow, maxRowExclusive));
            int dstUser = clamped - 1;

            // Adjust for remove/insert if moving down
            if (dstUser > srcUser) {
                dstUser--;
            }

            dstUser = Math.max(0, Math.min(dstUser, userCount - 1));
            if (dstUser == srcUser) {
                return true;
            }

            container._layers.remove(srcUser);
            container._layers.add(dstUser, dragged);

            model.refresh();
            container.setDirty(true);
            container.refresh();
            return true;
        }
    }

    /**
     * Convenience for adding a "Layers…" entry to a view popup menu.
     * <p>
     * This returns a menu item you can add to {@code view.getViewPopupMenu()}.
     *
     * @param view the owning view
     * @return a menu item that opens the inspector
     */
    public static JMenuItem createMenuItem(edu.cnu.mdi.view.BaseView view) {
        return new JMenuItem(new AbstractAction("Layers…") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (view == null || view.getContainer() == null) {
                    return;
                }
                if (view.getContainer() instanceof BaseContainer bc) {
                    LayerInspectorDialog.show(view, bc);
                }
            }
        });
    }
}
