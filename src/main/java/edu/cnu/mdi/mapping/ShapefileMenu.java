package edu.cnu.mdi.mapping;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.cnu.mdi.log.Log;

/**
 * A {@link JMenu} that provides interactive shapefile loading and per-layer
 * visibility control for a {@link MapView2D}.
 *
 * <h2>Menu structure</h2>
 * <pre>
 * Shapefiles
 *   Open Shapefile...
 *   ─────────────────
 *   ☑ Rivers          ← added when first layer is loaded
 *   ☑ Lakes
 *   ☐ Urban Areas     ← unchecked = hidden
 * </pre>
 *
 * <h2>Opening shapefiles</h2>
 * <p>"Open Shapefile..." presents a {@link JFileChooser} filtered to
 * {@code .shp} files. The companion {@code .dbf} is located automatically
 * from the same directory. After loading, a default style is chosen based
 * on the geometry type of the first feature in the file:
 * <ul>
 *   <li><b>Polygon</b> — semi-transparent blue fill with a slightly darker
 *       border, suitable for lakes, urban areas, or any filled area.</li>
 *   <li><b>Polyline</b> — blue stroke, suitable for rivers, roads, or
 *       coastlines.</li>
 *   <li><b>Point</b> — red dot markers.</li>
 * </ul>
 * The style can be refined programmatically via
 * {@link ShapeFeatureRenderer#setStyle} after the layer is added.</p>
 *
 * <h2>Programmatic layers</h2>
 * <p>Layers added at startup via
 * {@link MapView2D#addLayer(ShapeFeatureRenderer, String)} are automatically
 * reflected in the menu with a checkbox. The menu does not need to know
 * whether a layer came from user interaction or from code.</p>
 *
 * <h2>Visibility</h2>
 * <p>Toggling a checkbox calls {@link ShapeFeatureRenderer#setVisible(boolean)}
 * and triggers a repaint. The layer remains in the layer list; it simply
 * produces no output when hidden, so re-showing it is instantaneous.</p>
 *
 * <h2>Usage in MapView2D</h2>
 * <pre>{@code
 * ShapefileMenu shpMenu = new ShapefileMenu(this);
 * applyFocusFix(shpMenu, this);
 * getJMenuBar().add(shpMenu, 1);  // insert after File menu
 * }</pre>
 */
@SuppressWarnings("serial")
public class ShapefileMenu extends JMenu {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** The map view this menu controls. */
    private final MapView2D mapView;

    /** Persistent file chooser so the last-used directory is remembered. */
    private final JFileChooser fileChooser;

    /** The "Open Shapefile..." menu item. */
    private final JMenuItem openItem;

    /** Separator between the Open item and the layer checkboxes. */
    private final JSeparator separator;

    /** Whether the separator has been added yet (deferred until first layer). */
    private boolean separatorAdded = false;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a Shapefiles menu bound to the given map view.
     *
     * @param mapView the map view to control; must not be {@code null}
     */
    public ShapefileMenu(MapView2D mapView) {
        super("Shapefiles");
        this.mapView = mapView;

        fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Shapefile");
        fileChooser.setFileFilter(
                new FileNameExtensionFilter("ESRI Shapefiles (*.shp)", "shp"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        separator = new JSeparator();

        openItem = new JMenuItem("Open Shapefile\u2026");
        openItem.addActionListener(e -> openShapefile());
        add(openItem);
    }

    // -------------------------------------------------------------------------
    // Public API — called by MapView2D when a layer is added
    // -------------------------------------------------------------------------

    /**
     * Registers a newly added layer in the menu by appending a
     * {@link JCheckBoxMenuItem} for it. Called automatically by
     * {@link MapView2D#addLayer(ShapeFeatureRenderer, String)} so that both
     * programmatic and interactive layers appear in the menu.
     *
     * @param renderer the layer renderer; must not be {@code null}
     * @param name     the display name shown in the menu item
     */
    public void registerLayer(ShapeFeatureRenderer renderer, String name) {
        // Add the separator before the first checkbox item.
        if (!separatorAdded) {
            add(separator);
            separatorAdded = true;
        }

        JCheckBoxMenuItem item = new JCheckBoxMenuItem(name, renderer.isVisible());
        item.addActionListener(e -> {
            renderer.setVisible(item.isSelected());
            mapView.refresh();
        });
        add(item);
    }

    // -------------------------------------------------------------------------
    // Private — interactive file open
    // -------------------------------------------------------------------------

    /**
     * Shows the file chooser dialog and, on confirmation, loads the selected
     * shapefile and adds it to the map as a new layer.
     *
     * <p>The display name is derived from the filename without extension
     * (e.g. {@code ne_10m_lakes} → {@code "ne_10m_lakes"}). The default
     * style is chosen based on the geometry type of the first feature.</p>
     */
    private void openShapefile() {
        int result = fileChooser.showOpenDialog(mapView);
        if (result != JFileChooser.APPROVE_OPTION) return;

        Path shpPath = fileChooser.getSelectedFile().toPath();
        String name  = baseName(shpPath);

        try {
            ShapefileFeatureLoader loader = new ShapefileFeatureLoader();
            List<ShapeFeature> features   = loader.load(shpPath);

            if (features.isEmpty()) {
                Log.getInstance().warning("Shapefile is empty: " + shpPath);
                return;
            }

            ShapeFeatureStyle style = defaultStyle(features.get(0).getShapeType());
            ShapeFeatureRenderer renderer =
                    new ShapeFeatureRenderer(features, mapView.getProjection(), style);

            // addLayer notifies the menu via registerLayer automatically.
            mapView.addLayer(renderer, name);

        } catch (IOException ex) {
            Log.getInstance().error("Failed to load shapefile: " + shpPath
                    + " — " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a sensible default {@link ShapeFeatureStyle} based on the
     * given shape type. Polygon layers get a semi-transparent fill; polyline
     * layers get a stroked line; point layers get dot markers.
     *
     * @param shapeType one of the {@code TYPE_*} constants in
     *                  {@link ShapefileGeometryReader}
     * @return a pre-configured style
     */
    private static ShapeFeatureStyle defaultStyle(int shapeType) {
        return switch (shapeType) {

            case ShapefileGeometryReader.TYPE_POLYGON ->
                new ShapeFeatureStyle()
                        .fillColor(new Color(107, 159, 212, 160))
                        .strokeColor(new Color(74, 127, 181, 200))
                        .strokeWidth(0.5f)
                        .tooltipFields("name", "NAME");

            case ShapefileGeometryReader.TYPE_POLYLINE ->
                new ShapeFeatureStyle()
                        .strokeColor(new Color(107, 159, 212, 200))
                        .strokeWidth(0.8f)
                        .tooltipFields("name", "NAME");

            default -> // Point / MultiPoint
                new ShapeFeatureStyle()
                        .pointColor(Color.RED)
                        .pointRadius(3.0)
                        .tooltipFields("NAME", "name");
        };
    }

    /**
     * Returns the filename without its extension, used as the default layer
     * display name.
     *
     * @param path path to the {@code .shp} file
     * @return base filename without extension
     */
    private static String baseName(Path path) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(0, dot) : filename;
    }
}