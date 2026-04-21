package edu.cnu.mdi.mapping.container;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Point2D;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.BaseToolHandler;
import edu.cnu.mdi.dialog.TextEditDialog;
import edu.cnu.mdi.graphics.toolbar.GestureContext;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.item.MapLineItem;
import edu.cnu.mdi.mapping.item.MapPolygonItem;
import edu.cnu.mdi.mapping.item.MapPolylineItem;
import edu.cnu.mdi.mapping.item.MapTextItem;
import edu.cnu.mdi.swing.WindowPlacement;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.UnicodeUtils;

/**
 * Map-specific tool handler that overrides creation gestures to produce
 * map-native items whose geometry is stored in geographic coordinates rather
 * than projection-space world coordinates.
 *
 * <h2>Responsibilities</h2>
 * <p>
 * This handler supplements {@link BaseToolHandler} with overrides for
 * operations that have a geographic interpretation on a map view:
 * </p>
 * <ul>
 *   <li>{@link #createLine} — creates a {@link MapLineItem} (a great-circle
 *       route) rather than a plain {@code LineItem}.</li>
 *   <li>{@link #createPolyline} — creates a {@link MapPolylineItem} (a
 *       multi-vertex great-circle polyline).</li>
 *   <li>{@link #createPolygon} — creates a {@link MapPolygonItem} (a closed
 *       great-circle polygon).</li>
 *   <li>{@link #createTextItem} — creates a {@link MapTextItem} anchored to
 *       the geographic location of the click, with the text collected via a
 *       {@link edu.cnu.mdi.dialog.TextEditDialog}.</li>
 * </ul>
 * <p>
 * All other tool operations (pan, zoom, rubber-band selection, pointer, style
 * editing, deletion, etc.) delegate to the superclass.
 * </p>
 *
 * <h2>Obtaining an instance</h2>
 * <p>
 * Instances are created by {@link MapContainer#createToolHandler()}, which is
 * called from {@link edu.cnu.mdi.container.BaseContainer#setToolBar}.
 * Application code does not normally construct this class directly.
 * </p>
 */
public class MapToolHandler extends BaseToolHandler {

    /**
     * The owning map container, narrowed from the superclass's generic
     * {@code BaseContainer} reference.
     *
     * <p>This is stored as a {@link MapContainer} so that map-specific
     * operations ({@link MapContainer#localToLatLon}, etc.) are directly
     * accessible without repeated casting.</p>
     */
    private final MapContainer mapContainer;

    /**
     * Creates a map tool handler bound to the given container.
     *
     * @param container the owning map container; must not be {@code null} and
     *                  must be an instance of {@link MapContainer}
     * @throws ClassCastException if {@code container} is not a
     *                            {@link MapContainer}
     */
    public MapToolHandler(BaseContainer container) {
        super(container);
        this.mapContainer = (MapContainer) container;
    }

    /**
     * Creates a {@link MapLineItem} (a great-circle route) between the two
     * screen-space endpoints.
     *
     * <p>Both endpoints are converted from device pixels to geographic
     * coordinates (longitude/latitude in radians) via
     * {@link MapContainer#localToLatLon} before the item is constructed.
     * The item is placed on the annotation layer and given a default red
     * style with a 2-pixel line width.</p>
     *
     * @param gc    the gesture context from the toolbar
     * @param start the screen-space start point of the drawn line
     * @param end   the screen-space end point of the drawn line
     */
    @Override
    public void createLine(GestureContext gc, Point start, Point end) {
        Point2D.Double ll1 = new Point2D.Double();
        Point2D.Double ll2 = new Point2D.Double();
        mapContainer.localToLatLon(start, ll1);
        mapContainer.localToLatLon(end,   ll2);

        AItem item = new MapLineItem(mapContainer.getAnnotationLayer(), ll1, ll2);
        item.setRightClickable(true);
        item.setDraggable(true);
        item.setSelectable(true);
        item.setResizable(true);
        item.setRotatable(true);
        item.setDeletable(true);
        item.setLocked(false);
        item.setDisplayName("GC line");
        item.getStyleSafe().setLineColor(Color.red);
        item.getStyleSafe().setLineWidth(2.0f);

        mapContainer.setDirty(true);
        mapContainer.refresh();
    }


    /**
     * Creates a {@link MapPolylineItem} (a sequence of great-circle arcs) from
     * the screen-space vertices produced by the polyline rubberband tool.
     *
     * <p>Vertices are converted from device pixels to geographic coordinates via
     * {@link MapContainer#localToLatLon}. At least 2 valid vertices are required;
     * the call is silently ignored if the vertex array is too short.</p>
     *
     * @param gc       the gesture context from the toolbar
     * @param vertices the screen-space vertices in draw order
     */
    @Override
    public void createPolyline(GestureContext gc, Point[] vertices) {
        if (vertices == null || vertices.length < 2) {
			return;
		}

        Point2D.Double[] latLons = toLatLons(vertices);
        if (latLons == null) {
			return;
		}

        MapPolylineItem item = new MapPolylineItem(
                mapContainer.getAnnotationLayer(), latLons);
        defaultConfigureItem(item);

        mapContainer.setDirty(true);
        mapContainer.refresh();
    }

    /**
     * Creates a {@link MapPolygonItem} (a closed great-circle polygon) from
     * the screen-space vertices produced by the polygon rubberband tool.
     *
     * <p>Vertices are converted from device pixels to geographic coordinates via
     * {@link MapContainer#localToLatLon}. At least 3 valid vertices are required;
     * the call is silently ignored if the vertex array is too short.</p>
     *
     * @param gc       the gesture context from the toolbar
     * @param vertices the screen-space vertices in draw order
     */
    @Override
    public void createPolygon(GestureContext gc, Point[] vertices) {
        if (vertices == null || vertices.length < 3) {
			return;
		}

        Point2D.Double[] latLons = toLatLons(vertices);
        if (latLons == null) {
			return;
		}

        MapPolygonItem item = new MapPolygonItem(
                mapContainer.getAnnotationLayer(), latLons);
        defaultConfigureItem(item);

        mapContainer.setDirty(true);
        mapContainer.refresh();
    }

    /**
     * Creates a {@link MapTextItem} at the geographic location corresponding to
     * the given screen-space click point.
     *
     * <p>Opens a {@link TextEditDialog} to collect the text, font, and colors
     * from the user. If the dialog is cancelled, or the user enters empty text,
     * no item is created. The item is placed on the annotation layer.</p>
     *
     * <p>The text item is not resizable (its size is determined by the font and
     * content), but it is draggable, selectable, and deletable. Its popup menu
     * includes an "Edit Text…" entry that re-opens the dialog.</p>
     *
     * @param gc       the gesture context from the toolbar
     * @param location the screen-space click point
     */
    @Override
    public void createTextItem(GestureContext gc, Point location) {
        TextEditDialog dialog = new TextEditDialog();
        WindowPlacement.centerComponent(dialog);
        dialog.setVisible(true);

        if (dialog.isCancelled()) {
			return;
		}

        String text = UnicodeUtils.specialCharReplace(dialog.getText());
        if (text == null || text.isBlank()) {
			return;
		}

        Font font = dialog.getSelectedFont();
        if (font == null) {
			font = Fonts.defaultFont;
		}

        Point2D.Double anchor = new Point2D.Double();
        mapContainer.localToLatLon(location, anchor);

        Layer layer = mapContainer.getAnnotationLayer();
        MapTextItem item = new MapTextItem(layer, anchor, font, text,
                dialog.getLineColor(), dialog.getFillColor(), dialog.getTextColor());

        defaultConfigureItem(item);
        item.setResizable(false);    // size is driven by font + content
        item.setRotatable(false);    // geographic labels are always upright

        addTextEditMenuItem(item);

        mapContainer.setDirty(true);
        mapContainer.refresh();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Converts an array of screen-space points to geographic lon/lat coordinates.
     *
     * @param screenPoints device-space points
     * @return geographic points in radians, or {@code null} if the array is null
     */
    private Point2D.Double[] toLatLons(Point[] screenPoints) {
        if (screenPoints == null) {
			return null;
		}
        Point2D.Double[] latLons = new Point2D.Double[screenPoints.length];
        for (int i = 0; i < screenPoints.length; i++) {
            latLons[i] = new Point2D.Double();
            mapContainer.localToLatLon(screenPoints[i], latLons[i]);
        }
        return latLons;
    }

    /**
     * Applies the standard interactive behavior flags to a newly created item:
     * right-clickable, draggable, selectable, resizable, rotatable, deletable,
     * and unlocked.
     *
     * <p>Callers that need to suppress a specific behavior (e.g. text items
     * are not resizable) should call the relevant setter after this method.</p>
     *
     * @param item the item to configure
     */
    private static void defaultConfigureItem(AItem item) {
        item.setRightClickable(true);
        item.setDraggable(true);
        item.setSelectable(true);
        item.setResizable(true);
        item.setRotatable(true);
        item.setDeletable(true);
        item.setLocked(false);
    }

    /**
     * Adds an "Edit Text…" popup menu item to the given {@link MapTextItem}.
     *
     * <p>Selecting the menu item re-opens the {@link TextEditDialog} and applies
     * any changes. The item is re-populated from its current text, font, and
     * style so edits are cumulative.</p>
     *
     * @param item the item to attach the menu item to
     */
    private void addTextEditMenuItem(MapTextItem item) {
        JPopupMenu menu = item.getPopupMenu();
        menu.addSeparator();
        JMenuItem editMenuItem = new JMenuItem("Edit Text\u2026");
        editMenuItem.addActionListener(e -> {
            TextEditDialog d = new TextEditDialog(item);
            WindowPlacement.centerComponent(d);
            d.setVisible(true);
            if (!d.isCancelled()) {
                d.updateTextItem(item);
                item.setDirty(true);
                mapContainer.refresh();
            }
        });
        menu.add(editMenuItem);
    }
}