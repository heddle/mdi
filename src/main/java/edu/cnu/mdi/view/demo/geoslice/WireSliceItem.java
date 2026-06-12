package edu.cnu.mdi.view.demo.geoslice;

import edu.cnu.mdi.item.PointItem;

/**
 * Association between a rendered 2D wire point item and its source 3D wire.
 *
 * <p>
 * This is display-state glue. The source model object is {@link Wire3D}; the
 * visible MDI item is {@link PointItem}. Keeping the association lets the view
 * perform hit testing in 2D and then report meaningful 3D/model metadata in
 * the feedback panel.
 * </p>
 */
final class WireSliceItem {

    /** Rendered 2D point item. */
    private final PointItem item;

    /** Source 3D wire that produced the point item. */
    private final Wire3D wire;

    /**
     * Create an association between a rendered point item and a source wire.
     *
     * @param item rendered MDI point item
     * @param wire source 3D wire
     */
    WireSliceItem(PointItem item, Wire3D wire) {
        this.item = item;
        this.wire = wire;
    }

    /**
     * Return the rendered 2D point item.
     *
     * @return point item
     */
    PointItem getItem() {
        return item;
    }

    /**
     * Return the source 3D wire.
     *
     * @return source wire
     */
    Wire3D getWire() {
        return wire;
    }
}