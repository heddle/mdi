package edu.cnu.mdi.mapping.container;

import java.awt.Point;
import java.awt.Window;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.SwingUtilities;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.BaseToolHandler;
import edu.cnu.mdi.hover.HoverEvent;
import edu.cnu.mdi.hover.HoverInfoWindow;
import edu.cnu.mdi.hover.HoverListener;
import edu.cnu.mdi.hover.HoverManager;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.mapping.MapView2D;
import edu.cnu.mdi.mapping.item.MapMilSymbolItem;
import edu.cnu.mdi.mapping.milsym.MilSymbolDescriptor;
import edu.cnu.mdi.mapping.milsym.MilSymbolTransferable;
import edu.cnu.mdi.mapping.projection.EProjection;
import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.projection.LambertEqualAreaProjection;
import edu.cnu.mdi.mapping.projection.MercatorProjection;
import edu.cnu.mdi.mapping.projection.MollweideProjection;
import edu.cnu.mdi.mapping.projection.OrthographicProjection;
import edu.cnu.mdi.util.UnicodeUtils;
import edu.cnu.mdi.view.ContainerFactory;

/**
 * Map-specific {@link edu.cnu.mdi.container.IContainer} that adds:
 * <ul>
 * <li><b>Projection-aware recentering</b> — double-click (or toolbar recenter
 *     gesture) re-centers the active projection on the geographic point under
 *     the cursor rather than simply scrolling the viewport.</li>
 * <li><b>Lat/lon coordinate conversion</b> — convenience helpers that convert
 *     between screen, world, and geographic coordinates.</li>
 * <li><b>Country-name hover popup</b> — a {@link HoverInfoWindow} that appears
 *     after the cursor dwells over a country polygon.</li>
 * <li><b>Military symbol drop target</b> — accepts a
 *     {@link MilSymbolTransferable} dragged from the
 *     {@link edu.cnu.mdi.mapping.milsym.NatoIconPicker} palette and creates a
 *     {@link MapMilSymbolItem} at the geographic location of the drop point.
 *     The active toolbar tool is <em>not</em> changed by the drop.</li>
 * </ul>
 *
 * <h2>ContainerFactory compatibility</h2>
 * <p>The single-argument constructor {@link #MapContainer(Rectangle2D.Double)}
 * satisfies the {@link ContainerFactory} functional interface and can therefore
 * be referenced as a constructor reference:</p>
 * <pre>{@code
 * ContainerFactory factory = MapContainer::new;
 * }</pre>
 *
 * <h2>Hover popup lifecycle</h2>
 * <p>The {@link HoverInfoWindow} is created lazily on the first
 * {@link #hoverUp(HoverEvent)} event. Call {@link #prepareForExit()} when the
 * owning view is closing to unregister the hover listener and dispose the
 * popup window.</p>
 */
@SuppressWarnings("serial")
public class MapContainer extends BaseContainer implements HoverListener {

    /**
     * Lazily-created popup window used to display a country name near the cursor.
     *
     * <p>{@code null} until the first {@link #hoverUp(HoverEvent)} that
     * successfully resolves the window ancestor. Set back to {@code null} by
     * {@link #prepareForExit()}.</p>
     */
    private HoverInfoWindow hoverWindow;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a map container with the given initial world coordinate system.
     *
     * <p>This constructor intentionally has a single {@link Rectangle2D.Double}
     * parameter so it satisfies the {@link ContainerFactory} functional
     * interface:</p>
     * <pre>{@code
     * ContainerFactory factory = MapContainer::new;
     * }</pre>
     *
     * <p>Standard panning is disabled because map views re-center the
     * projection on right-click rather than scrolling a viewport. A
     * {@link DropTarget} is installed immediately so the canvas can receive
     * military symbol drops from the palette.</p>
     *
     * @param worldSystem the initial world coordinate rectangle; must not be
     *                    {@code null}
     */
    public MapContainer(Rectangle2D.Double worldSystem) {
        super(worldSystem);

        // Register for hover events so the country-name popup fires after
        // the cursor dwells over the map.
        HoverManager.getInstance().registerComponent(getComponent(), this);

        // Install the drop target that accepts MilSymbolTransferable payloads.
        installMilSymbolDropTarget();
    }

    // -------------------------------------------------------------------------
    // Military symbol drop target
    // -------------------------------------------------------------------------

    /**
     * Installs an AWT {@link DropTarget} on the map canvas that listens for
     * {@link MilSymbolTransferable} payloads.
     *
     * <h2>Behavior on drop</h2>
     * <ol>
     *   <li>Rejects drops that do not carry {@link MilSymbolTransferable#FLAVOR}.</li>
     *   <li>Converts the drop screen point to a geographic lat/lon coordinate.</li>
     *   <li>Creates and adds a {@link MapMilSymbolItem} on the annotation layer.</li>
     *   <li>Does <em>not</em> alter the active toolbar tool — the user continues
     *       with whatever tool was active before the drag (Option C behavior).</li>
     * </ol>
     *
     * <h2>Mid-gesture safety</h2>
     * <p>If the {@link MapToolHandler} reports that a drawing gesture is in
     * progress ({@link MapToolHandler#isDrawing()}), the drop is rejected so
     * that the in-progress shape is not corrupted.</p>
     */
    private void installMilSymbolDropTarget() {
        new DropTarget(getComponent(), DnDConstants.ACTION_COPY,
                new DropTargetAdapter() {

                    @Override
                    public void drop(DropTargetDropEvent event) {

                        // Reject if a drawing gesture is in progress.
                        if (toolHandler instanceof MapToolHandler mth && mth.isDrawing()) {
                            event.rejectDrop();
                            return;
                        }

                        // Reject if the payload doesn't carry our flavor.
                        if (!event.isDataFlavorSupported(MilSymbolTransferable.FLAVOR)) {
                            event.rejectDrop();
                            return;
                        }

                        event.acceptDrop(DnDConstants.ACTION_COPY);

                        try {
                            Transferable t = event.getTransferable();
                            MilSymbolDescriptor descriptor =
                                    (MilSymbolDescriptor) t.getTransferData(
                                            MilSymbolTransferable.FLAVOR);

                            // The drop location is in the canvas's coordinate space.
                            Point dropPoint = event.getLocation();
                            placeSymbol(descriptor, dropPoint);

                            event.dropComplete(true);

                        } catch (Exception ex) {
                            event.dropComplete(false);
                        }
                    }
                },
                true /* active */);
    }

    /**
     * Creates a {@link MapMilSymbolItem} at the geographic location
     * corresponding to {@code screenPoint} and adds it to the annotation layer.
     *
     * <p>This is the single placement method used by the drop target. It is
     * intentionally package-private so {@link MapToolHandler} can call it if
     * needed in the future, but it is not part of the public API.</p>
     *
     * @param descriptor the symbol metadata; must not be {@code null}
     * @param screenPoint the canvas-space drop location
     */
    void placeSymbol(MilSymbolDescriptor descriptor, Point screenPoint) {
        if (descriptor == null || screenPoint == null) {
            return;
        }

        Point2D.Double latLon = new Point2D.Double();
        localToLatLon(screenPoint, latLon);

        Layer layer = getAnnotationLayer();

        new MapMilSymbolItem(layer, latLon, descriptor, descriptor.getIcon());

        setDirty(true);
        refresh();
    }

    // -------------------------------------------------------------------------
    // Projection-aware recentering
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Overridden to re-center the active map projection on the geographic
     * point under the cursor rather than shifting the viewport. The correct
     * setter is dispatched based on the active {@link EProjection} type.</p>
     */
    @Override
    public void recenter(Point pp) {
        IMapProjection mp = getMapView2D().getProjection();
        EProjection proj = mp.getProjection();

        Point2D.Double wp = new Point2D.Double();
        Point2D.Double ll = new Point2D.Double();
        localToWorld(pp, wp);
        mp.latLonFromXY(ll, wp);

        switch (proj) {
            case MERCATOR          -> ((MercatorProjection) mp).setCentralLongitude(ll.x);
            case MOLLWEIDE         -> ((MollweideProjection) mp).setCentralLongitude(ll.x);
            case ORTHOGRAPHIC      -> ((OrthographicProjection) mp).setCenter(ll.x, ll.y);
            case LAMBERT_EQUAL_AREA -> ((LambertEqualAreaProjection) mp).setCenter(ll.x, ll.y);
        }

        getMapView2D().invalidate();
        setDirty(true);
        refresh();
    }

    // -------------------------------------------------------------------------
    // Coordinate conversion helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a screen-space (local) point to a geographic lat/lon point.
     *
     * <p>The conversion path is: local → world (via container transform) →
     * lat/lon (via projection inverse).</p>
     *
     * @param pp screen-space pixel coordinate
     * @param ll output lat/lon point in radians ({@code x=λ, y=φ}); populated
     *           in-place
     */
    public void localToLatLon(Point pp, Point2D.Double ll) {
        Point2D.Double wp = new Point2D.Double();
        localToWorld(pp, wp);
        getMapView2D().getProjection().latLonFromXY(ll, wp);
    }

    /**
     * Converts a geographic lat/lon point to a screen-space (local) point.
     *
     * @param pp output screen-space pixel coordinate; populated in-place
     * @param ll input lat/lon point in radians ({@code x=λ, y=φ})
     */
    public void latLonToLocal(Point pp, Point2D.Double ll) {
        Point2D.Double wp = new Point2D.Double();
        getMapView2D().getProjection().latLonToXY(ll, wp);
        worldToLocal(pp, wp);
    }

    /**
     * Converts a world (projection-space) point to a geographic lat/lon point.
     *
     * @param ll output lat/lon point in radians ({@code x=λ, y=φ}); populated
     *           in-place
     * @param wp input world-space coordinate
     */
    public void worldToLatLon(Point2D.Double ll, Point2D.Double wp) {
        getMapView2D().getProjection().latLonFromXY(ll, wp);
    }

    // -------------------------------------------------------------------------
    // Feedback
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Overridden to push a formatted lat/lon string (e.g.
     * {@code "37.77°N, 122.41°W"}) to the toolbar status area in addition to
     * the standard feedback-pane update performed by the superclass.</p>
     */
    @Override
    public void feedbackTrigger(MouseEvent mouseEvent, boolean dragging) {
        Point2D.Double wp = getLocation(mouseEvent);

        if (_feedbackControl != null) {
            _feedbackControl.updateFeedback(mouseEvent, wp, dragging);
        }

        if (_toolBar != null) {
            Point2D.Double ll = new Point2D.Double();
            worldToLatLon(ll, wp);
            String latLon = String.format("%.2f%s %s, %.2f%s %s",
                    Math.abs(Math.toDegrees(ll.y)), UnicodeUtils.DEGREE,
                    (ll.y >= 0) ? "N" : "S",
                    Math.abs(Math.toDegrees(ll.x)), UnicodeUtils.DEGREE,
                    (ll.x >= 0) ? "E" : "W");
            _toolBar.updateStatusText(latLon);
        }
    }

    // -------------------------------------------------------------------------
    // HoverListener — country-name popup
    // -------------------------------------------------------------------------

    /**
     * Called by {@link HoverManager} after the cursor has rested over this
     * component for the configured dwell time.
     */
    @Override
    public void hoverUp(HoverEvent he) {
        Point p = he.getLocation();
        String countryName = getMapView2D().getCountryAtPoint(p, this);
        if (countryName == null) {
            return;
        }

        HoverInfoWindow win = getHoverWindow();
        if (win == null) {
            return;
        }

        SwingUtilities.convertPointToScreen(p, he.getSource());
        win.showMessage(countryName, p);
    }

    /**
     * Called by {@link HoverManager} when the cursor leaves the component or
     * moves after a hover was triggered.
     */
    @Override
    public void hoverDown(HoverEvent he) {
        if (hoverWindow != null) {
            hoverWindow.hideMessage();
        }
    }

    // -------------------------------------------------------------------------
    // Resource cleanup
    // -------------------------------------------------------------------------

    /**
     * Releases all hover-related resources held by this container.
     *
     * <p>Unregisters the canvas from {@link HoverManager}, hides and disposes
     * the {@link HoverInfoWindow} if one was created. Safe to call multiple
     * times.</p>
     */
    public void prepareForExit() {
        HoverManager.getInstance().unregisterComponent(getComponent());

        if (hoverWindow != null) {
            hoverWindow.hideMessage();
            hoverWindow.dispose();
            hoverWindow = null;
        }
    }

    // -------------------------------------------------------------------------
    // Tool handler factory
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Returns a {@link MapToolHandler} so that map-specific gestures are
     * used instead of the generic {@link BaseToolHandler} defaults.</p>
     */
    @Override
    protected BaseToolHandler createToolHandler() {
        return new MapToolHandler(this);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the owning {@link MapView2D} by casting the base-class view
     * reference.
     */
    private MapView2D getMapView2D() {
        return (MapView2D) getView();
    }

    /**
     * Returns the hover popup window, creating it lazily on first call.
     *
     * @return the hover popup, or {@code null} if not yet constructable
     */
    private HoverInfoWindow getHoverWindow() {
        if (hoverWindow == null) {
            Window ownerWin = SwingUtilities.getWindowAncestor(getComponent());
            if (ownerWin == null) {
                return null;
            }
            hoverWindow = new HoverInfoWindow(ownerWin);
        }
        return hoverWindow;
    }
}