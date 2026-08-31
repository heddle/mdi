package edu.cnu.mdi.mapping.shapefile;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.cnu.mdi.dialog.DialogUtils;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.mapping.MapView2D;

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
 * </p>
 * <ul>
 *   <li><b>Polygon</b> — semi-transparent blue fill with a slightly darker
 *       border, suitable for lakes, urban areas, or any filled area.</li>
 *   <li><b>Polyline</b> — blue stroke, suitable for rivers, roads, or
 *       coastlines.</li>
 *   <li><b>Point</b> — red dot markers.</li>
 * </ul>
 * The style can be refined programmatically via
 * {@link ShapeFeatureRenderer#setStyle} after the layer is added.
 *
 * <h2>Programmatic layers</h2>
 * <p>Layers added at startup via
 * {@link MapView2D#addShapefile(ShapeFeatureRenderer, String)} are automatically
 * reflected in the menu with a checkbox. The menu does not need to know
 * whether a layer came from user interaction or from code.</p>
 *
 * <h2>Visibility</h2>
 * <p>Toggling a checkbox changes the corresponding layer's visibility and
 * triggers a repaint. The layer remains in the layer list; it simply
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
        
        //try to open in details view, but don't fail if the L&F doesn't support it
        SwingUtilities.invokeLater(
                () -> DialogUtils.requestDetailsView(fileChooser));
        
        fileChooser.setDialogTitle("Open Shapefile");
        fileChooser.setFileFilter(
                new FileNameExtensionFilter("ESRI Shapefiles (*.shp)", "shp"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        openItem = new JMenuItem("Open Shapefile\u2026");
        openItem.addActionListener(e -> openShapefile());
        add(openItem);
    }

    // -------------------------------------------------------------------------
    // Public API — called by MapView2D when a layer is added
    // -------------------------------------------------------------------------


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
            mapView.addShapefile(renderer, name);

        } catch (IOException ex) {
            Log.getInstance().error("Failed to load shapefile: " + shpPath
                    + " — " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers — also reused by MapView2D's drag-and-drop shapefile loading, so
    // both entry points (the "Open Shapefile..." menu item and dropping a
    // .shp file on the map) style and name a newly loaded layer identically.
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
    public static ShapeFeatureStyle defaultStyle(int shapeType) {
        return switch (shapeType) {

            case ShapefileGeometryReader.TYPE_POLYGON ->
                new ShapeFeatureStyle()
                        .fillColor(new Color(107, 159, 212, 160))
                        .strokeColor(new Color(74, 127, 181, 200))
                        .strokeWidth(0.5f)
                        .feedbackFields("name", "NAME", "FULLNAME");

            case ShapefileGeometryReader.TYPE_POLYLINE ->
                new ShapeFeatureStyle()
                        .strokeColor(new Color(107, 159, 212, 200))
                        .strokeWidth(0.8f)
                        .feedbackFields("name", "NAME", "FULLNAME");

            default -> // Point / MultiPoint
                new ShapeFeatureStyle()
                        .pointColor(Color.RED)
                        .pointRadius(3.0)
                        .feedbackFields("NAME", "name", "FULLNAME");
        };
    }

    /**
     * Returns the filename without its extension, used as the default layer
     * display name.
     *
     * @param path path to the {@code .shp} file
     * @return base filename without extension
     */
    public static String baseName(Path path) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(0, dot) : filename;
    }
}
