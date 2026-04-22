package edu.cnu.mdi.mapping.milsym;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Drag-and-drop payload that carries a {@link MilSymbolDescriptor} from the
 * {@link edu.cnu.mdi.mapping.milsym.NatoIconPicker} palette to the map canvas.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // In NatoIconPicker — start the drag:
 * Transferable t = new MilSymbolTransferable(descriptor);
 * dragSource.startDrag(dge, customCursor, t, dragListener);
 *
 * // In MapContainer's DropTarget — receive it:
 * if (event.isDataFlavorSupported(MilSymbolTransferable.FLAVOR)) {
 *     MilSymbolDescriptor d =
 *         (MilSymbolDescriptor) event.getTransferable()
 *                                    .getTransferData(MilSymbolTransferable.FLAVOR);
 * }
 * }</pre>
 */
public final class MilSymbolTransferable implements Transferable {

    /**
     * The single {@link DataFlavor} used for military symbol drag payloads.
     *
     * <p>Uses a JVM-local representation class ({@link MilSymbolDescriptor})
     * so the flavor is only meaningful within the same JVM — which is all we
     * need for intra-application drag-and-drop.</p>
     */
    public static final DataFlavor FLAVOR = new DataFlavor(
            MilSymbolDescriptor.class, "MilSymbolDescriptor");

    private static final DataFlavor[] FLAVORS = { FLAVOR };

    private final MilSymbolDescriptor descriptor;

    /**
     * Creates a transferable wrapping the given descriptor.
     *
     * @param descriptor the symbol to transfer; must not be {@code null}
     */
    public MilSymbolTransferable(MilSymbolDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        this.descriptor = descriptor;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return FLAVORS.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return FLAVOR.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return descriptor;
    }
}