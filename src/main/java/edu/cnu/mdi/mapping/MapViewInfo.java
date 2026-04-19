package edu.cnu.mdi.mapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.cnu.mdi.view.AbstractViewInfo;

/**
 * Provides metadata and help text for the {@link MapView2D} "Info" dialog.
 *
 * <p>This object is constructed by {@link MapView2D#getViewInfo()} and passed
 * to the framework's info-dialog machinery. It supplies the dialog title,
 * purpose description, usage bullets, keyboard shortcuts, and technical
 * notes.</p>
 *
 * <h2>Dynamic city count</h2>
 * <p>Previously {@link #getTechnicalNotes()} hardcoded the string
 * {@code "Currently loading 1251 cities"}, which would silently become wrong
 * whenever the GeoJSON dataset changed. The count is now obtained from the
 * live static field via {@link MapView2D#getCityCount()} so the displayed
 * number always reflects reality.</p>
 */
public class MapViewInfo extends AbstractViewInfo {

    /**
     * Reference to the owning view, used to obtain the live city and country
     * counts for the technical notes string.
     */
    private final MapView2D mapView;

    /**
     * Creates a view-info object for the given map view.
     *
     * @param mapView the owning view; must not be {@code null}
     */
    public MapViewInfo(MapView2D mapView) {
        this.mapView = mapView;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "Sample 2D Map View"}
     */
    @Override
    public String getTitle() {
        return "Sample 2D Map View";
    }

    /**
     * {@inheritDoc}
     *
     * @return a short paragraph describing the purpose of this view
     */
    @Override
    public String getPurpose() {
        return "This view provides a flattened, 2D representation of geospatial "
             + "data using configurable projections. It is designed for analyzing "
             + "city density and global coordinates.";
    }

    /**
     * {@inheritDoc}
     *
     * @return a list of usage hints displayed as bullet points in the dialog
     */
    @Override
    public List<String> getUsageBullets() {
        return List.of(
            "Use the Toolbar to zoom, pan, and measure distances.",
            "Adjust the Minimum Population slider to filter visible cities.",
            "Toggle Map Theme to switch between Light, Dark, and Blue modes."
        );
    }

    /**
     * {@inheritDoc}
     *
     * @return an ordered map of keyboard shortcut labels to descriptions
     */
    @Override
    public Map<String, String> getKeyboardShortcuts() {
        Map<String, String> keys = new LinkedHashMap<>();
        keys.put("Scroll Wheel",       "Zoom In / Out");
        keys.put("Right Click + Drag", "Pan Map");
        keys.put("Ctrl + R",           "Reset Projection");
        return keys;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a dynamically computed string that includes the actual number
     * of cities and countries currently loaded, rather than a hardcoded value
     * that would become stale if the dataset changes.</p>
     *
     * @return technical notes including live dataset sizes
     */
    @Override
    public String getTechnicalNotes() {
        return String.format(
            "Rendering Engine: Spherical Mercator (unit sphere). "
          + "Countries loaded: %d. Cities loaded: %d.",
            mapView.getCountryCount(),
            mapView.getCityCount()
        );
    }
}
