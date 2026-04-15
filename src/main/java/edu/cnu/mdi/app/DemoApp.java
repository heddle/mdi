package edu.cnu.mdi.app;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;

import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.mapping.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.mapping.MapContainer;
import edu.cnu.mdi.mapping.MapResources;
import edu.cnu.mdi.mapping.MapView2D;
import edu.cnu.mdi.sim.demo.network.NetworkDeclutterDemoView;
import edu.cnu.mdi.sim.ga.triimage.ImageEvolutionDemoView;
import edu.cnu.mdi.sim.simanneal.tspdemo.TspDemoView;
import edu.cnu.mdi.splot.example.SplotDemoView;
import edu.cnu.mdi.splot.plot.PlotView;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.ContainerFactory;
import edu.cnu.mdi.view.DrawingView;
import edu.cnu.mdi.view.LogView;
import edu.cnu.mdi.view.ViewManager;
import edu.cnu.mdi.view.VirtualView;
import edu.cnu.mdi.view.demo.NetworkLayoutDemoView;

/**
 * Demo application for the MDI framework.
 *
 * <p>This class is intentionally "example-first": it demonstrates how a typical
 * application:</p>
 * <ol>
 *   <li>Creates the main application frame ({@link BaseMDIApplication})</li>
 *   <li>Creates a few internal views (2D map, drawing, log, plot, demos)</li>
 *   <li>Optionally enables a {@link VirtualView} to simulate a virtual
 *       desktop</li>
 *   <li>Applies default view placement, then applies any persisted
 *       layout/config</li>
 * </ol>
 *
 * <p>The "virtual desktop" logic is driven by {@link BaseMDIApplication}'s
 * virtual desktop lifecycle hooks:</p>
 * <ul>
 *   <li>{@link #onVirtualDesktopReady()} runs once after the frame is
 *       showing</li>
 *   <li>{@link #onVirtualDesktopRelayout()} runs (debounced) after
 *       resizes/moves</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class DemoApp extends BaseMDIApplication {

    /** Singleton instance of the demo app. */
    private static DemoApp INSTANCE;

    // -------------------------------------------------------------------------
    // Sample views used by the demo. None are meant to be completely realistic
    // or functional, except for the LogView.
    // -------------------------------------------------------------------------

    private DrawingView drawingView;
    private MapView2D mapView;
    private LogView logView;
    private PlotView plotView;
    private NetworkDeclutterDemoView networkDeclutterDemoView;
    private TspDemoView tspDemoView;
    private ImageEvolutionDemoView imageEvolutionDemoView;

    /**
     * Private constructor: use {@link #getInstance()}.
     *
     * @param keyVals optional key-value pairs passed to
     *                {@link BaseMDIApplication}
     */
    private DemoApp(Object... keyVals) {
        super(keyVals);

        // Create internal views.
        addInitialViews();

        // Log environment information early.
        Log.getInstance().info(Environment.getInstance().toString());
    }

    @Override
    protected int getVirtualDesktopColumns() {
        return 7;
    } // opts in; 0 = disabled

    /**
     * Public access to the singleton.
     *
     * @return the singleton main application frame
     */
    public static DemoApp getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DemoApp(
                    PropertyUtils.TITLE,           "Demo Application of MDI Views",
                    PropertyUtils.BACKGROUNDIMAGE, Environment.MDI_RESOURCE_PATH + "images/mdilogo.png",
                    PropertyUtils.CONSOLELOG,      true,
                    PropertyUtils.FRACTION,        0.8);
        }
        return INSTANCE;
    }

    /**
     * Create and register the initial set of views shown in the demo.
     *
     * <p>This method only builds views; it should not depend on the outer
     * frame being shown or on final geometry.</p>
     */
    private void addInitialViews() {

        // Log view is useful but not always visible.
        logView = new LogView();
        logView.setVisible(false);

        ViewManager.getInstance().getViewMenu().addSeparator();

        // Drawing view
        drawingView = DrawingView.createDrawingView();

        // Map view (also loads demo GeoJSON)
        mapView = createMapView();

        // Plot view
        plotView = SplotDemoView.createDemoView();

        // Network declutter demo view
        networkDeclutterDemoView = createNetworkDeclutterDemoView();

        // Network layout demo view — created lazily (as an example)
        ViewManager.getInstance().addConfiguration(NetworkLayoutDemoView.getConfiguration());

        // TSP (Traveling Salesperson) demo view
        tspDemoView = createTspDemoView();

        // Image evolution demo view
        imageEvolutionDemoView = createImageEvolutionDemoView();
    }

    @Override
    protected String getApplicationId() {
        return "mdiDemoApp";
    }

    /**
     * Place the views in the virtual desktop in a reasonable default layout.
     */
    @Override
    protected void defaultViewLayout() {
        VirtualView vv = VirtualView.getInstance();
        vv.moveTo(mapView,                   0, VirtualView.BOTTOMRIGHT);
        vv.moveTo(drawingView,               0, VirtualView.TOPCENTER);
        vv.moveTo(plotView,                  1, VirtualView.CENTER);
        vv.moveTo(networkDeclutterDemoView,  2, VirtualView.CENTER);
        vv.moveTo(tspDemoView,               3, VirtualView.CENTER);
        vv.moveTo(imageEvolutionDemoView,    5, VirtualView.CENTER);
        vv.moveTo(logView,                   6, VirtualView.UPPERLEFT);
    }

    /**
     * Create the network declutter demo view.
     *
     * @return a new {@link NetworkDeclutterDemoView}
     */
    NetworkDeclutterDemoView createNetworkDeclutterDemoView() {
        long toolBits = ToolBits.INFO;
        return new NetworkDeclutterDemoView(
                PropertyUtils.TITLE,       "Network Declutter Demo View",
                PropertyUtils.FRACTION,    0.7,
                PropertyUtils.ASPECT,      1.2,
                PropertyUtils.BACKGROUND,  Color.white,
                PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(0.0, 0.0, 1, 1),
                PropertyUtils.TOOLBARBITS, toolBits);
    }

    /**
     * Create the TSP demo view.
     *
     * @return a new {@link TspDemoView}
     */
    TspDemoView createTspDemoView() {
        return new TspDemoView(
                PropertyUtils.TITLE,       "TSP Demo View",
                PropertyUtils.FRACTION,    0.6,
                PropertyUtils.ASPECT,      1.2,
                PropertyUtils.BACKGROUND,  X11Colors.getX11Color("lavender blush"),
                PropertyUtils.WORLDSYSTEM, new Rectangle2D.Double(0.0, 0.0, 1, 1));
    }

    /**
     * Create the image evolution demo view.
     *
     * @return a new {@link ImageEvolutionDemoView}
     */
    ImageEvolutionDemoView createImageEvolutionDemoView() {
        long toolBits = ToolBits.INFO;
        return new ImageEvolutionDemoView(
                PropertyUtils.TITLE,       "Image Evolution Demo View",
                PropertyUtils.FRACTION,    0.8,
                PropertyUtils.ASPECT,      1.2,
                PropertyUtils.BACKGROUND,  X11Colors.getX11Color("honeydew"),
                PropertyUtils.TOOLBARBITS, toolBits);
    }

    /**
     * Create the demo map view and load small GeoJSON datasets from resources.
     *
     * <p>The map view uses a {@link ContainerFactory} lambda
     * ({@code MapContainer::new}) instead of the legacy
     * {@link PropertyUtils#CONTAINERCLASS} reflection path. The constructor
     * reference compiles to the same {@code (Rectangle2D.Double)} signature
     * that {@link MapContainer} already exposes, so no change to
     * {@link MapContainer} is required.</p>
     *
     * <p>The key practical differences from the old approach:</p>
     * <ul>
     *   <li>A typo or wrong class is caught at compile time, not at runtime
     *       when the view is first shown.</li>
     *   <li>An IDE can navigate from {@code MapContainer::new} directly to
     *       the constructor; it cannot follow a {@link Class} stored in a
     *       {@link java.util.Properties} map.</li>
     *   <li>If {@link MapContainer} ever needs extra post-construction
     *       initialization, a lambda can add those calls without touching
     *       {@link MapContainer} itself:
     *       <pre>
     *         ws -&gt; { MapContainer c = new MapContainer(ws); c.setFoo(bar); return c; }
     *       </pre>
     *   </li>
     * </ul>
     *
     * @return a new, fully configured {@link MapView2D}
     */
    private MapView2D createMapView() {

        String resPrefix = Environment.MDI_RESOURCE_PATH;

        // Load a small set of countries just for demo purposes.
        try {
            List<CountryFeature> countries = GeoJsonCountryLoader
                    .loadFromResource(resPrefix + MapResources.COUNTRIES_GEOJSON);
            MapView2D.setCountries(countries);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Load cities as well.
        try {
            List<GeoJsonCityLoader.CityFeature> cities = GeoJsonCityLoader
                    .loadFromResource(resPrefix + MapResources.CITIES_GEOJSON);
            MapView2D.setCities(cities);
        } catch (IOException e) {
            e.printStackTrace();
        }

        long toolBits = ToolBits.INFO | ToolBits.STATUS | ToolBits.CENTER
                | ToolBits.ZOOMTOOLS | ToolBits.DRAWINGTOOLS | ToolBits.MAGNIFY;

        // Use a ContainerFactory constructor reference instead of a raw Class.
        // MapContainer::new satisfies ContainerFactory.create(Rectangle2D.Double)
        // because MapContainer already declares the matching single-argument
        // constructor — no change to MapContainer is needed.
        ContainerFactory mapContainerFactory = MapContainer::new;

        return new MapView2D(
                PropertyUtils.TITLE,            "Sample 2D Map View",
                PropertyUtils.FRACTION,         0.6,
                PropertyUtils.ASPECT,           1.5,
                PropertyUtils.CONTAINERFACTORY, mapContainerFactory,
                PropertyUtils.TOOLBARBITS,      toolBits,
                PropertyUtils.WHEELZOOM,        true);
    }

    /**
     * Entry point for the demo.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        BaseMDIApplication.launch(DemoApp::getInstance);
    }
}