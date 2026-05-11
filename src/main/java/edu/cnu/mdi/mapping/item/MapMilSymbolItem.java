package edu.cnu.mdi.mapping.item;

import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.milsym.MilSymbolDescriptor;
import edu.cnu.mdi.mapping.util.GeoUtils;
import edu.cnu.mdi.mapping.util.LatLongEditorDialog;
import edu.cnu.mdi.mapping.util.UTMCoordinate;

/**
 * Map-native military symbol item.
 *
 * <p>The anchor position is stored geographically as longitude/latitude
 * in radians, so the item stays attached to the same location when the
 * map projection changes. The icon itself is drawn at a fixed pixel size
 * for readability.</p>
 *
 * <h2>Context menu</h2>
 * <p>In addition to the standard ordering/lock items inherited from
 * AItem, the right-click menu includes an <em>Edit Location…</em>
 * entry (after a separator) that opens a {@link LatLongEditorDialog}.
 * If the user saves a new position the item is moved there and the map
 * is refreshed; cancelling the dialog leaves the item unchanged.</p>
 */
public class MapMilSymbolItem extends MapPointItem {

    /** Default drawn size in pixels. */
    public static final int DEFAULT_DRAW_SIZE = 20;

    /** Symbol metadata. */
    private final MilSymbolDescriptor descriptor;

    /** Cached symbol image. */
    private final ImageIcon icon;

    /** Drawn size in pixels. */
    private int drawSize = DEFAULT_DRAW_SIZE;

    /**
     * Creates a new geolocated military symbol item.
     *
     * @param layer      annotation layer
     * @param location   geographic location in radians ({@code .x=lon, .y=lat})
     * @param descriptor symbol metadata
     * @param icon       rendered icon image
     */
    public MapMilSymbolItem(Layer layer, Point2D.Double location,
            MilSymbolDescriptor descriptor, ImageIcon icon) {
        super(layer, location);
        this.descriptor = descriptor;
        this.icon = icon;

        setDisplayName((descriptor == null) ? "MIL Symbol"
                : descriptor.getDisplayName());
        setSelectable(true);
        setDraggable(true);
        setRightClickable(true);
        setDeletable(true);
        setResizable(false);
        setRotatable(false);
        setLocked(false);
        setStyleEditable(false); // it is an icon, so no style properties to edit
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Gets the symbol descriptor.
     *
     * @return descriptor; may be {@code null} if none was supplied
     */
    public MilSymbolDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Sets the on-screen icon size in pixels.
     *
     * @param drawSize new draw size, clamped to at least 8 pixels
     */
    public void setDrawSize(int drawSize) {
        this.drawSize = Math.max(8, drawSize);
        setDirty(true);
    }

    /**
     * Gets the on-screen icon size in pixels.
     *
     * @return draw size
     */
    public int getDrawSize() {
        return drawSize;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void drawItem(Graphics2D g2, IContainer container) {
        Point p = getFocusPoint(container);
        if (p == null) {
            return;
        }

        if (icon != null) {
            int half = drawSize / 2;
            g2.drawImage(icon.getImage(), p.x - half, p.y - half, drawSize, drawSize, null);

            if (isSelected()) {
                g2.drawRect(p.x - half - 2, p.y - half - 2,
                        drawSize + 4, drawSize + 4);
            }
        } else {
            super.drawItem(g2, container);
        }
    }

    @Override
    public Rectangle getBounds(IContainer container) {
        Point p = getFocusPoint(container);
        if (p == null) {
            return null;
        }
        int half = drawSize / 2;
        return new Rectangle(p.x - half, p.y - half, drawSize, drawSize);
    }

    @Override
    public boolean contains(IContainer container, Point screenPoint) {
        Rectangle r = getBounds(container);
        return (r != null) && r.contains(screenPoint);
    }

    // -------------------------------------------------------------------------
    // Context menu
    // -------------------------------------------------------------------------

    /**
     * Builds the popup menu for this item.
     *
     * <p>Calls the superclass implementation to get the standard ordering and
     * lock items, then appends a separator followed by an
     * <em>Edit Location…</em> menu item.</p>
     *
     * @return the fully populated popup menu
     */
    @Override
    protected JPopupMenu createPopupMenu() {
        JPopupMenu menu = super.createPopupMenu();
        menu.addSeparator();
        JMenuItem editLocation = new JMenuItem("Edit Location\u2026");
        editLocation.addActionListener(e -> openLocationEditor());
        menu.add(editLocation);
        return menu;
    }

    /**
     * Opens the {@link LatLongEditorDialog} pre-populated with this item's
     * current geographic location.
     *
     * <p>The current position is read from {@link #_focus}, which
     * {@link MapPointItem} uses directly as the geographic anchor
     * ({@code x = longitude, y = latitude} in radians). If the focus is
     * unset or NaN — indicating an unplaced item — the dialog is not opened.
     *
     * <p>If the user saves a new position:
     * <ol>
     *   <li>The item's focus is updated via {@link MapPointItem#setFocus}.</li>
     *   <li>The item is marked dirty so any cached bounds are invalidated.</li>
     *   <li>The container is refreshed so the map redraws immediately.</li>
     * </ol>
     *
     * <p>If the user cancels, nothing changes.
     */
    private void openLocationEditor() {
        // _focus holds the geographic location directly in MapPointItem.
        // Guard against an unplaced item (NaN coordinates) before opening.
        if (_focus == null || Double.isNaN(_focus.x) || Double.isNaN(_focus.y)) {
            return;
        }

        // Defensive copy — the dialog must not alias our internal _focus.
        Point2D.Double current = new Point2D.Double(_focus.x, _focus.y);

        // Walk the AWT hierarchy to find an owner Frame for proper modality
        // and centering. Passing null is safe — the dialog will still be modal.
        Frame owner = null;
        IContainer container = getContainer();
        if (container != null) {
            Window window = SwingUtilities.getWindowAncestor(container.getComponent());
            if (window instanceof Frame f) {
                owner = f;
            }
        }

        LatLongEditorDialog dialog = new LatLongEditorDialog(owner, current);
        dialog.setVisible(true);  // blocks (modal) until save or cancel

        Point2D.Double result = dialog.getResult();
        if (result == null) {
            return;  // user cancelled — leave item unchanged
        }

        setFocus(result);
        setDirty(true);

        if (container != null) {
            container.refresh();
        }
    }
    
    // -------------------------------------------------------------------------
    // Feedback
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Reports the geographic coordinates of this point when the cursor is
     * near it.</p>
     */
    @Override
    public void getFeedbackStrings(IContainer container, Point pp,
            Point2D.Double wp, List<String> feedbackStrings) {
        if (feedbackStrings == null || !contains(container, pp)) return;
        if (_focus == null || Double.isNaN(_focus.x)) return;
        feedbackStrings.add("$yellow$" + getDisplayName());
        
        double dLon = Math.toDegrees(_focus.x);
        double dLat = Math.toDegrees(_focus.y);
        
        feedbackStrings.add(String.format("$orange$MilSymbol lat %.3f°, lon %.3f°",
                dLat, dLon));
        
        UTMCoordinate utm = GeoUtils.fromDecimalDegrees(dLat, dLon);
        feedbackStrings.add("$orange$UTM " + utm.toString());
    }

}