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
 * <p>
 * Military symbol placement is handled entirely by drag-and-drop in
 * {@link MapContainer}. This handler is responsible only for the normal
 * map drawing tools such as line, polyline, polygon, and text.
 * </p>
 */
public class MapToolHandler extends BaseToolHandler {

    /** Owning map container. */
    private final MapContainer mapContainer;

    /**
     * Creates a map tool handler bound to the given container.
     *
     * @param container the owning map container
     */
    public MapToolHandler(BaseContainer container) {
        super(container);
        this.mapContainer = (MapContainer) container;
    }

    /**
     * Creates a great-circle map line between the two screen-space endpoints.
     *
     * @param gc    the gesture context
     * @param start the screen-space start point
     * @param end   the screen-space end point
     */
    @Override
    public void createLine(GestureContext gc, Point start, Point end) {
        Point2D.Double ll1 = new Point2D.Double();
        Point2D.Double ll2 = new Point2D.Double();
        mapContainer.localToLatLon(start, ll1);
        mapContainer.localToLatLon(end, ll2);

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
     * Creates a great-circle polyline from the supplied screen-space vertices.
     *
     * @param gc       the gesture context
     * @param vertices screen-space vertices in draw order
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
     * Creates a great-circle polygon from the supplied screen-space vertices.
     *
     * @param gc       the gesture context
     * @param vertices screen-space vertices in draw order
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
     * Creates a geolocated text item at the clicked map position.
     *
     * @param gc       the gesture context
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
        item.setResizable(false);
        item.setRotatable(false);

        addTextEditMenuItem(item);

        mapContainer.setDirty(true);
        mapContainer.refresh();
    }

    /**
     * Converts screen-space points to geographic lon/lat.
     *
     * @param screenPoints device-space points
     * @return geographic points in radians, or {@code null} if input is null
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
     * Applies the standard interactive behavior flags to a newly created item.
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
     * Adds an "Edit Text…" popup menu item to the given text item.
     *
     * @param item the text item
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