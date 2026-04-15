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
 * Map-specific {@link edu.cnu.mdi.container.IContainer} that adds projection-
 * aware recentering, lat/lon coordinate conversion, and hover-driven country-
 * name popups to the standard {@link BaseContainer} behaviour.
 *
 * <h2>ContainerFactory compatibility</h2>
 * <p>
 * The single-argument constructor {@link #MapContainer(Rectangle2D.Double)}
 * satisfies the {@link ContainerFactory} functional interface contract:
 * </p>
 * <pre>
 *   ContainerFactory factory = MapContainer::new;
 * </pre>
 * <p>
 * This constructor reference can be passed to
 * {@link edu.cnu.mdi.view.ViewPropertiesBuilder#containerFactory(ContainerFactory)}
 * (stored under {@link edu.cnu.mdi.util.PropertyUtils#CONTAINERFACTORY}),
 * replacing the older reflection-based
 * {@link edu.cnu.mdi.util.PropertyUtils#CONTAINERCLASS} approach. No
 * changes to this class are required for the migration — the existing
 * constructor already has the correct signature.
 * </p>
 *
 * <h2>Hover popup</h2>
 * <p>
 * A {@link HoverInfoWindow} is created lazily on first use because the
 * component's window ancestor is not guaranteed to be available at
 * construction time. The popup is hidden on {@link #hoverDown} and shown
 * on {@link #hoverUp} only when the cursor is over a recognized country
 * polygon. If the window ancestor is still unavailable when the first hover
 * fires, the popup is silently suppressed for that event and will be
 * attempted again on the next {@link #hoverUp}.
 * </p>
 *
 * <h2>Resource cleanup</h2>
 * <p>
 * Call {@link #prepareForExit()} when the owning view is closing to
 * unregister the hover listener and dispose the popup window. Failing to do
 * so will leave a {@link HoverManager} registration and a visible (but
 * orphaned) {@link HoverInfoWindow} in memory.
 * </p>
 */
@SuppressWarnings("serial")
public class MapContainer extends BaseContainer implements HoverListener {

    /**
     * Lazily-created popup window used to display a country name near the cursor.
     *
     * <p>Null until the first {@link #hoverUp} that successfully resolves
     * the window ancestor. Disposed and set back to {@code null} by
     * {@link #prepareForExit()}.</p>
     */
    private HoverInfoWindow hoverWindow;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Create a map container with the given initial world coordinate system.
     *
     * <p>This constructor is intentionally kept to a single
     * {@link Rectangle2D.Double} argument so that it satisfies the
     * {@link ContainerFactory} functional interface and can be referenced as
     * a constructor reference:</p>
     * <pre>
     *   ContainerFactory factory = MapContainer::new;
     * </pre>
     *
     * <p>Standard panning is disabled here because map views handle panning
     * differently (re-centering the projection rather than scrolling a
     * viewport).</p>
     *
     * @param worldSystem the initial world coordinate rectangle; must not be
     *                    {@code null}
     */
    public MapContainer(Rectangle2D.Double worldSystem) {
        super(worldSystem);
        setStandardPanning(false);

        // Register for hover events so country names appear near the cursor.
        HoverManager.getInstance().registerComponent(getComponent(), this);
    }

    // -------------------------------------------------------------------------
    // Projection-aware recentering
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Overridden to re-center the active map projection on the geographic
     * point under the cursor rather than simply shifting the viewport.</p>
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
        case MERCATOR:
            ((MercatorProjection) mp).setCentralLongitude(ll.x);
            break;

        case MOLLWEIDE:
            ((MollweideProjection) mp).setCentralLongitude(ll.x);
            break;

        case ORTHOGRAPHIC:
            ((OrthographicProjection) mp).setCenter(ll.x, ll.y);
            break;

        case LAMBERT_EQUAL_AREA:
            ((LambertEqualAreaProjection) mp).setCenter(ll.x, ll.y);
            break;
        }

        getMapView2D().invalidate();
        setDirty(true);
        refresh();
    }

    // -------------------------------------------------------------------------
    // Coordinate conversion helpers
    // -------------------------------------------------------------------------

    /**
     * Convert a local (screen-space) point to a geographic lat/lon point.
     *
     * @param pp the local pixel coordinate
     * @param ll the output lat/lon point (radians); populated in-place
     */
    public void localToLatLon(Point pp, Point2D.Double ll) {
        Point2D.Double wp = new Point2D.Double();
        localToWorld(pp, wp);
        getMapView2D().getProjection().latLonFromXY(ll, wp);
    }

    /**
     * Convert a world (projection) point to a geographic lat/lon point.
     *
     * @param ll the output lat/lon point (radians); populated in-place
     * @param wp the input world coordinate
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
     * <p>Overridden to push a formatted lat/lon string to the toolbar status
     * area in addition to the standard feedback-pane update.</p>
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
     * <p>Looks up the country under the cursor and shows its name in the
     * hover popup. If no country is found, or if the popup window cannot yet
     * be created (component not yet realized), this method returns silently.</p>
     *
     * @param he the hover event containing the component and cursor location
     */
    @Override
    public void hoverUp(HoverEvent he) {
        Point  p           = he.getLocation();
        String countryName = getMapView2D().getCountryAtPoint(p, this);

        if (countryName == null) {
            return; // Cursor is not over a recognized country polygon.
        }

        HoverInfoWindow win = getHoverWindow();
        if (win == null) {
            return; // Window ancestor not yet available; suppress this event.
        }

        SwingUtilities.convertPointToScreen(p, he.getSource());
        win.showMessage(countryName, p);
    }

    /**
     * Called by {@link HoverManager} when the cursor leaves the component or
     * moves after a hover was triggered.
     *
     * <p>Hides the country-name popup. If the popup was never created this
     * is a no-op.</p>
     *
     * @param he the hover event (not used beyond triggering the hide)
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
     * Release all hover-related resources held by this container.
     *
     * <p>Unregisters the container's canvas from {@link HoverManager},
     * disposes the {@link HoverInfoWindow} if one was created, and clears
     * the reference so it can be GC'd. This method should be called from
     * {@link MapView2D#prepareForExit()} when the view is closing.</p>
     *
     * <p>Safe to call multiple times.</p>
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
     * Return the owning {@link MapView2D}.
     *
     * @return the parent map view; never {@code null} once the container is
     *         attached to a view
     */
    private MapView2D getMapView2D() {
        return (MapView2D) getView();
    }

    /**
     * Return the hover popup window, creating it lazily on first call.
     *
     * <p>Returns {@code null} if the component's window ancestor is not yet
     * available (i.e. the component has not been added to a realized Swing
     * hierarchy). Callers must null-check the return value.</p>
     *
     * <p>Note: the previous implementation called
     * {@code HoverManager.getInstance().unregisterComponent(hoverWindow)}
     * in {@link #prepareForExit()}, but {@link HoverInfoWindow} is never
     * registered with {@link HoverManager} — only the canvas component is.
     * That spurious call has been removed.</p>
     *
     * @return the hover window, or {@code null} if the hierarchy is not yet
     *         realized
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