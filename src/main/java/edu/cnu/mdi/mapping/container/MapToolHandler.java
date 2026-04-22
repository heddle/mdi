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
 * <ul>
 *   <li>{@link #createLine} — creates a {@link MapLineItem} (great-circle
 *       route).</li>
 *   <li>{@link #createPolyline} — creates a {@link MapPolylineItem} (multi-
 *       vertex great-circle polyline).</li>
 *   <li>{@link #createPolygon} — creates a {@link MapPolygonItem} (closed
 *       great-circle polygon).</li>
 *   <li>{@link #createTextItem} — creates a {@link MapTextItem} anchored to
 *       the geographic location of the click.</li>
 * </ul>
 *
 * <h2>Military symbol placement</h2>
 * <p>Symbol placement is now handled entirely by the AWT drag-and-drop
 * mechanism in {@link MapContainer}. This handler no longer participates in
 * symbol placement and does not override {@link #pointerClick}.</p>
 *
 * <h2>Drawing guard</h2>
 * <p>{@link #isDrawing()} returns {@code true} while a multi-step drawing
 * gesture (polyline, polygon) is in progress. The map's drop target checks
 * this flag and rejects symbol drops while drawing is active so that the
 * in-progress shape is not corrupted.</p>
 */
public class MapToolHandler extends BaseToolHandler {

    /**
     * The owning map container, narrowed from the superclass's generic
     * {@code BaseContainer} reference.
     */
    private final MapContainer mapContainer;

    /**
     * {@code true} while a multi-step drawing gesture (polyline or polygon)
     * is in progress. Read by the drop target via {@link #isDrawing()}.
     */
    private boolean drawing = false;

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

    // -------------------------------------------------------------------------
    // Drawing guard
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} while a multi-step drawing gesture (polyline or
     * polygon rubberband) is in progress.
     *
     * <p>The map's {@link DropTarget} checks this flag before accepting a
     * military symbol drop so that an in-flight drawing gesture is never
     * interrupted.</p>
     *
     * @return {@code true} if a drawing gesture is active
     */
    public boolean isDrawing() {
        return drawing;
    }

    // -------------------------------------------------------------------------
    // Line
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link MapLineItem} (a great-circle route) between the two
     * screen-space endpoints.
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

    // -------------------------------------------------------------------------
    // Polyline
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link MapPolylineItem} from screen-space vertices.
     *
     * <p>Sets {@link #drawing} to {@code false} when the gesture completes.</p>
     *
     * @param gc       the gesture context from the toolbar
     * @param vertices the screen-space vertices in draw order
     */
    @Override
    public void createPolyline(GestureContext gc, Point[] vertices) {
        drawing = false;

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

    // -------------------------------------------------------------------------
    // Polygon
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link MapPolygonItem} from screen-space vertices.
     *
     * <p>Sets {@link #drawing} to {@code false} when the gesture completes.</p>
     *
     * @param gc       the gesture context from the toolbar
     * @param vertices the screen-space vertices in draw order
     */
    @Override
    public void createPolygon(GestureContext gc, Point[] vertices) {
        drawing = false;

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

    // -------------------------------------------------------------------------
    // Text
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link MapTextItem} at the geographic location corresponding to
     * the given screen-space click point.
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
        item.setResizable(false);
        item.setRotatable(false);

        addTextEditMenuItem(item);

        mapContainer.setDirty(true);
        mapContainer.refresh();
    }

    // -------------------------------------------------------------------------
    // Drawing-gesture lifecycle hooks
    // -------------------------------------------------------------------------

    /**
     * Called by the toolbar framework when a multi-vertex rubberband gesture
     * begins (polyline or polygon tool activated).
     *
     * <p>Sets the {@link #drawing} flag so the drop target knows to reject
     * symbol drops while the gesture is live.</p>
     *
     * <p><b>Note:</b> Hook this into whatever callback the rubberband tool
     * fires when the user commits the first vertex. If {@link BaseToolHandler}
     * already exposes such a callback, override it here; otherwise wire it from
     * the toolbar button's action listener.</p>
     */
    public void onDrawingStarted() {
        drawing = true;
    }

    /**
     * Called if a multi-vertex drawing gesture is cancelled (e.g. Escape key).
     * Clears the {@link #drawing} flag.
     */
    public void onDrawingCancelled() {
        drawing = false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Converts an array of screen-space points to geographic lon/lat.
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
     * Adds an "Edit Text…" popup menu item to the given {@link MapTextItem}.
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