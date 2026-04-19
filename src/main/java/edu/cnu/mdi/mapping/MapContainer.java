package edu.cnu.mdi.mapping;

import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.SwingUtilities;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.hover.HoverEvent;
import edu.cnu.mdi.hover.HoverInfoWindow;
import edu.cnu.mdi.hover.HoverListener;
import edu.cnu.mdi.hover.HoverManager;
import edu.cnu.mdi.util.UnicodeUtils;
import edu.cnu.mdi.view.ContainerFactory;

/**
 * Map-specific {@link edu.cnu.mdi.container.IContainer} that adds:
 * <ul>
 *   <li><b>Projection-aware recentering</b> — double-click (or toolbar
 *       recenter gesture) re-centers the active projection on the geographic
 *       point under the cursor rather than simply scrolling the viewport.</li>
 *   <li><b>Lat/lon coordinate conversion</b> — convenience helpers that
 *       convert between screen, world, and geographic coordinates.</li>
 *   <li><b>Country-name hover popup</b> — a {@link HoverInfoWindow} that
 *       appears after the cursor dwells over a country polygon.</li>
 * </ul>
 *
 * <h2>ContainerFactory compatibility</h2>
 * <p>The single-argument constructor
 * {@link #MapContainer(Rectangle2D.Double)} satisfies the
 * {@link ContainerFactory} functional interface and can therefore be
 * referenced as a constructor reference:</p>
 * <pre>{@code
 * ContainerFactory factory = MapContainer::new;
 * }</pre>
 * <p>This reference is stored under
 * {@link edu.cnu.mdi.util.PropertyUtils#CONTAINERFACTORY} and used by the
 * framework to instantiate the container without reflection.</p>
 *
 * <h2>Standard panning</h2>
 * <p>Standard viewport panning is disabled in the constructor because map
 * views handle panning differently: a right-click recenter updates the
 * projection center rather than scrolling a fixed background image.</p>
 *
 * <h2>Hover popup lifecycle</h2>
 * <p>The {@link HoverInfoWindow} is created lazily on the first
 * {@link #hoverUp(HoverEvent)} event because the component's window ancestor
 * is not guaranteed to be available at construction time. If the window
 * ancestor is still unavailable when the first hover fires, the popup is
 * silently suppressed for that event and attempted again on the next one.</p>
 *
 * <h2>Resource cleanup</h2>
 * <p>Call {@link #prepareForExit()} when the owning view is closing to
 * unregister the hover listener and dispose the popup window. Failing to do
 * so leaves a {@link HoverManager} registration and a potentially visible
 * {@link HoverInfoWindow} in memory.</p>
 */
@SuppressWarnings("serial")
public class MapContainer extends BaseContainer implements HoverListener {

    /**
     * Lazily-created popup window used to display a country name near the
     * cursor.
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
     * projection on right-click rather than scrolling a viewport.</p>
     *
     * @param worldSystem the initial world coordinate rectangle; must not be
     *                    {@code null}
     */
    public MapContainer(Rectangle2D.Double worldSystem) {
        super(worldSystem);
        setStandardPanning(false);

        // Register for hover events so the country-name popup fires after
        // the cursor dwells over the map.
        HoverManager.getInstance().registerComponent(getComponent(), this);
    }

    // -------------------------------------------------------------------------
    // Projection-aware recentering
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Overridden to re-center the active map projection on the geographic
     * point under the cursor rather than shifting the viewport. The correct
     * setter is dispatched based on the active {@link EProjection} type:
     * <ul>
     *   <li>{@link EProjection#MERCATOR} →
     *       {@link MercatorProjection#setCentralLongitude(double)}</li>
     *   <li>{@link EProjection#MOLLWEIDE} →
     *       {@link MollweideProjection#setCentralLongitude(double)}</li>
     *   <li>{@link EProjection#ORTHOGRAPHIC} →
     *       {@link OrthographicProjection#setCenter(double, double)}</li>
     *   <li>{@link EProjection#LAMBERT_EQUAL_AREA} →
     *       {@link LambertEqualAreaProjection#setCenter(double, double)}</li>
     * </ul>
     */
    @Override
    public void recenter(Point pp) {
        IMapProjection mp   = getMapView2D().getProjection();
        EProjection    proj = mp.getProjection();

        Point2D.Double wp = new Point2D.Double();
        Point2D.Double ll = new Point2D.Double();
        localToWorld(pp, wp);
        mp.latLonFromXY(ll, wp);

        switch (proj) {
            case MERCATOR       -> ((MercatorProjection)         mp).setCentralLongitude(ll.x);
            case MOLLWEIDE      -> ((MollweideProjection)        mp).setCentralLongitude(ll.x);
            case ORTHOGRAPHIC   -> ((OrthographicProjection)     mp).setCenter(ll.x, ll.y);
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
     * <p>The conversion path is:
     * local → world (via container transform) → lat/lon (via projection
     * inverse).</p>
     *
     * @param pp screen-space pixel coordinate
     * @param ll output lat/lon point in radians ({@code x=λ, y=φ});
     *           populated in-place
     */
    public void localToLatLon(Point pp, Point2D.Double ll) {
        Point2D.Double wp = new Point2D.Double();
        localToWorld(pp, wp);
        getMapView2D().getProjection().latLonFromXY(ll, wp);
    }

    /**
     * Converts a world (projection-space) point to a geographic lat/lon point.
     *
     * @param ll output lat/lon point in radians ({@code x=λ, y=φ});
     *           populated in-place
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
     *
     * <p>Looks up the country polygon under the cursor via
     * {@link MapView2D#getCountryAtPoint(Point, edu.cnu.mdi.container.IContainer)}
     * and shows the country name in the hover popup. If no country is found,
     * or if the popup window cannot yet be created (component not yet
     * realized), this method returns silently.</p>
     *
     * @param he the hover event containing the source component and cursor
     *           location
     */
    @Override
    public void hoverUp(HoverEvent he) {
        Point  p           = he.getLocation();
        String countryName = getMapView2D().getCountryAtPoint(p, this);
        if (countryName == null) return;

        HoverInfoWindow win = getHoverWindow();
        if (win == null) return; // window ancestor not yet available

        SwingUtilities.convertPointToScreen(p, he.getSource());
        win.showMessage(countryName, p);
    }

    /**
     * Called by {@link HoverManager} when the cursor leaves the component or
     * moves after a hover was triggered.
     *
     * <p>Hides the country-name popup. If the popup was never created this is
     * a no-op.</p>
     *
     * @param he the hover event (the location is not used; only the event
     *           occurrence matters)
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
     * the {@link HoverInfoWindow} if one was created, and clears the reference
     * so it can be garbage-collected. Should be called from
     * {@link MapView2D#prepareForExit()} when the view is closing.</p>
     *
     * <p>Safe to call multiple times; subsequent calls after the first are
     * no-ops.</p>
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
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the owning {@link MapView2D} by casting the base-class view
     * reference.
     *
     * @return the parent map view; never {@code null} once the container is
     *         attached to a view
     */
    private MapView2D getMapView2D() {
        return (MapView2D) getView();
    }

    /**
     * Returns the hover popup window, creating it lazily on first call.
     *
     * <p>Returns {@code null} if the component's window ancestor is not yet
     * available (the component has not yet been added to a realized Swing
     * hierarchy). Callers must null-check the return value before using the
     * window.</p>
     *
     * @return the hover popup, or {@code null} if not yet constructable
     */
    private HoverInfoWindow getHoverWindow() {
        if (hoverWindow == null) {
            Window ownerWin = SwingUtilities.getWindowAncestor(getComponent());
            if (ownerWin == null) return null;
            hoverWindow = new HoverInfoWindow(ownerWin);
        }
        return hoverWindow;
    }
}
