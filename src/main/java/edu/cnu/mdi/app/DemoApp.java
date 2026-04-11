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
import edu.cnu.mdi.properties.PropertyUtils;
import edu.cnu.mdi.sim.demo.network.NetworkDeclutterDemoView;
import edu.cnu.mdi.sim.ga.triimage.ImageEvolutionDemoView;
import edu.cnu.mdi.sim.simanneal.tspdemo.TspDemoView;
import edu.cnu.mdi.splot.example.SplotDemoView;
import edu.cnu.mdi.splot.plot.PlotView;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.view.DrawingView;
import edu.cnu.mdi.view.LogView;
import edu.cnu.mdi.view.ViewManager;
import edu.cnu.mdi.view.VirtualView;
import edu.cnu.mdi.view.demo.NetworkLayoutDemoView;

/**
 * Demo application for the MDI framework.
 * <p>
 * This class is intentionally "example-first": it demonstrates how a typical
 * application:
 * <ol>
 * <li>Creates the main application frame ({@link BaseMDIApplication})</li>
 * <li>Creates a few internal views (2D map, 3D, drawing, log)</li>
 * <li>Optionally enables a {@link VirtualView} to simulate a virtual
 * desktop</li>
 * <li>Applies default view placement, then applies any persisted
 * layout/config</li>
 * </ol>
 * <p>
 * The "virtual desktop" logic is driven by {@link BaseMDIApplication}'s virtual
 * desktop lifecycle hooks:
 * <ul>
 * <li>{@link #onVirtualDesktopReady()} runs once after the frame is
 * showing</li>
 * <li>{@link #onVirtualDesktopRelayout()} runs (debounced) after
 * resizes/moves</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class DemoApp extends BaseMDIApplication {

	/** Singleton instance of the demo app. */
	private static DemoApp INSTANCE;

	// -------------------------------------------------------------------------
	// Sample views used by the demo. None are meant to be completely realistic.
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
	 * @param keyVals optional key-value pairs passed to {@link BaseMDIApplication}
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
			INSTANCE = new DemoApp(PropertyUtils.TITLE, "Demo Application of MDI Views", 
					PropertyUtils.BACKGROUNDIMAGE, Environment.MDI_RESOURCE_PATH + "images/mdilogo.png", 
					PropertyUtils.CONSOLELOG, true,
					PropertyUtils.FRACTION, 0.8);
		}
		return INSTANCE;
	}

	/**
	 * Create and register the initial set of views shown in the demo.
	 * <p>
	 * This method only builds views; it should not depend on the outer frame being
	 * shown or on final geometry.
	 */
	private void addInitialViews() {

		// Log view is useful but not always visible.
		logView = new LogView();
		logView.setVisible(false);

		ViewManager.getInstance().getViewMenu().addSeparator();

		// Drawing view
		drawingView = DrawingView.createDrawingView();

		// Map view (also loads demo geojson)
		mapView = createMapView();

		// Plot view
		plotView = SplotDemoView.createDemoView();

		// Network declutter demo view
		networkDeclutterDemoView = createNetworkDeclutterDemoView();

		// Network layout demo view create lazily (as an example)
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

	// put the views in the virtual desktop in a reasonable default layout.
	@Override
	protected void defaultViewLayout() {
		VirtualView vv = VirtualView.getInstance(); // framework already owns it
		vv.moveTo(mapView, 0, VirtualView.BOTTOMRIGHT);
		vv.moveTo(drawingView, 0, VirtualView.TOPCENTER);
		vv.moveTo(plotView, 1, VirtualView.CENTER);
		vv.moveTo(networkDeclutterDemoView, 2, VirtualView.CENTER);
		vv.moveTo(tspDemoView, 3, VirtualView.CENTER);
		vv.moveTo(imageEvolutionDemoView, 5, VirtualView.CENTER);
		vv.moveTo(logView, 6, VirtualView.UPPERLEFT);
	}

	/**
	 * Create the network declutter demo view.
	 */
	NetworkDeclutterDemoView createNetworkDeclutterDemoView() {
		long toolBits = ToolBits.INFO;
		NetworkDeclutterDemoView view = new NetworkDeclutterDemoView(PropertyUtils.TITLE, "Network Declutter Demo View",
				PropertyUtils.FRACTION, 0.7, PropertyUtils.ASPECT, 1.2,
				PropertyUtils.BACKGROUND, Color.white, PropertyUtils.WORLDSYSTEM,
				new Rectangle2D.Double(0.0, 0.0, 1, 1), PropertyUtils.TOOLBARBITS, toolBits);
		return view;
	}

	/**
	 * Create the TSP demo view.
	 */
	TspDemoView createTspDemoView() {
		TspDemoView view = new TspDemoView(PropertyUtils.TITLE, "TSP Demo View", PropertyUtils.FRACTION, 0.6,
				PropertyUtils.ASPECT, 1.2, PropertyUtils.BACKGROUND,
				X11Colors.getX11Color("lavender blush"), PropertyUtils.WORLDSYSTEM,
				new Rectangle2D.Double(0.0, 0.0, 1, 1));
		return view;
	}
	
	/**
	 * Create the image evolution demo view.
	 */
	ImageEvolutionDemoView createImageEvolutionDemoView() {
		long toolBits = ToolBits.INFO;

		ImageEvolutionDemoView view = new ImageEvolutionDemoView(PropertyUtils.TITLE, "Image Evolution Demo View",
				PropertyUtils.FRACTION, 0.8, PropertyUtils.ASPECT, 1.2, PropertyUtils.BACKGROUND,
				X11Colors.getX11Color("honeydew"), PropertyUtils.TOOLBARBITS, toolBits);
		return view;
	}

	/**
	 * Create the demo map view and load small GeoJSON datasets from resources.
	 *
	 * @return a new {@link MapView2D}
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

		long toolBits = ToolBits.INFO | ToolBits.STATUS | ToolBits.CENTER | ToolBits.ZOOMTOOLS | ToolBits.DRAWINGTOOLS
				| ToolBits.MAGNIFY;

		// Create the view with a reasonable default configuration.
		return new MapView2D(PropertyUtils.TITLE, "Sample 2D Map View", PropertyUtils.FRACTION, 0.6,
				PropertyUtils.ASPECT, 1.5, PropertyUtils.CONTAINERCLASS, MapContainer.class, PropertyUtils.TOOLBARBITS,
				toolBits, PropertyUtils.WHEELZOOM, true);
	}

	/**
	 * Entry point for the demo.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		BaseMDIApplication.launch(DemoApp::getInstance); // launch() already exists in base
	}
}
