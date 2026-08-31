package edu.cnu.mdi.mapping.shapefile;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.cnu.mdi.dialog.DialogUtils;
import edu.cnu.mdi.io.RecentFiles;
import edu.cnu.mdi.io.RecentFilesMenu;
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
 *   Recent Shapefiles ▸
 *     1  ne_10m_lakes
 *     2  ne_10m_rivers_lake_centerlines
 *     ─────────────────
 *     Clear Recent
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
 * <h2>Recent shapefiles</h2>
 * <p>Every successfully loaded {@code .shp} file — whether opened via this
 * menu or dropped directly onto the map (see {@link
 * MapView2D#filesDropped}) — is recorded in a persistent, most-recently-used
 * list (backed by {@link RecentFiles}/{@link Preferences}, capped at 10
 * entries) and offered again under "Recent Shapefiles". A file that fails to
 * load, or is later found missing, is dropped from the list rather than left
 * as a dead entry.</p>
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

    /** Persistent most-recently-used list of successfully loaded {@code .shp} files. */
    private final RecentFiles recentFiles;

    /** Binds {@link #recentFiles} to {@link #recentMenu}. */
    private final RecentFilesMenu recentFilesMenu;

    /** The "Recent Shapefiles" submenu. */
    private final JMenu recentMenu;


    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a Shapefiles menu bound to the given map view.
     *
     * @param mapView the map view to control; must not be {@code null}
     */
    public ShapefileMenu(MapView2D mapView) {
        this(mapView, Preferences.userNodeForPackage(ShapefileMenu.class));
    }

    /**
     * Package-private constructor for tests: lets an isolated, throwaway
     * {@link Preferences} node stand in for the real, persistent production
     * one {@link #ShapefileMenu(MapView2D)} uses, so tests don't write
     * "Recent Shapefiles" entries into the developer's actual preference
     * store. Mirrors how {@link RecentFiles} itself takes an explicit
     * {@code Preferences} for the same reason.
     *
     * @param mapView                 the map view to control; must not be {@code null}
     * @param recentFilesPreferences  the preferences node backing the "Recent
     *                                Shapefiles" list
     */
    ShapefileMenu(MapView2D mapView, Preferences recentFilesPreferences) {
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

        recentFiles = new RecentFiles(recentFilesPreferences, 10, "recentShapefile");
        recentMenu = new JMenu("Recent Shapefiles");
        recentFilesMenu = new RecentFilesMenu(recentFiles, this::openShapefile, "shapefiles");
        recentFilesMenu.rebuild(recentMenu);
        add(recentMenu);
    }

    // -------------------------------------------------------------------------
    // Public API — called by MapView2D when a layer is added
    // -------------------------------------------------------------------------

    /**
     * Records a file as recently opened and refreshes the "Recent Shapefiles"
     * submenu, without (re)loading it.
     * <p>
     * Called by {@link MapView2D#filesDropped} after a dropped shapefile has
     * already been loaded successfully, so drag-and-drop and the "Open
     * Shapefile..." action populate the same recent list.
     * </p>
     *
     * @param file the shapefile that was just loaded; ignored if {@code null}
     */
    public void recordRecentlyOpened(File file) {
        if (file == null) return;
        recentFiles.add(file);
        recentFilesMenu.rebuild(recentMenu);
    }

    // -------------------------------------------------------------------------
    // Private — interactive file open
    // -------------------------------------------------------------------------

    /**
     * Shows the file chooser dialog and, on confirmation, loads the selected
     * shapefile via {@link #openShapefile(File)}.
     */
    private void openShapefile() {
        int result = fileChooser.showOpenDialog(mapView);
        if (result != JFileChooser.APPROVE_OPTION) return;
        openShapefile(fileChooser.getSelectedFile());
    }

    /**
     * Loads the given shapefile and adds it to the map as a new layer — the
     * shared implementation behind both the file chooser above and choosing
     * an entry from {@link #recentMenu}.
     *
     * <p>The display name is derived from the filename without extension
     * (e.g. {@code ne_10m_lakes} → {@code "ne_10m_lakes"}). The default
     * style is chosen based on the geometry type of the first feature. On
     * success, {@code file} is (re)recorded at the front of the recent list;
     * on failure (or an empty shapefile), it is removed from the recent list
     * so a broken entry doesn't linger.</p>
     *
     * <p>Package-private (not {@code private}) so tests can exercise it
     * directly, the same way the recent-menu click handler does.</p>
     *
     * @param file the {@code .shp} file to load; ignored if {@code null}
     */
    void openShapefile(File file) {
        if (file == null) return;

        Path shpPath = file.toPath();
        String name  = baseName(shpPath);

        try {
            ShapefileFeatureLoader loader = new ShapefileFeatureLoader();
            List<ShapeFeature> features   = loader.load(shpPath);

            if (features.isEmpty()) {
                Log.getInstance().warning("Shapefile is empty: " + shpPath);
                recentFiles.remove(file);
                recentFilesMenu.rebuild(recentMenu);
                return;
            }

            ShapeFeatureStyle style = defaultStyle(features.get(0).getShapeType());
            ShapeFeatureRenderer renderer =
                    new ShapeFeatureRenderer(features, mapView.getProjection(), style);

            // addLayer notifies the menu via registerLayer automatically.
            mapView.addShapefile(renderer, name);

            recordRecentlyOpened(file);

        } catch (IOException ex) {
            Log.getInstance().error("Failed to load shapefile: " + shpPath
                    + " — " + ex.getMessage());
            recentFiles.remove(file);
            recentFilesMenu.rebuild(recentMenu);
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
