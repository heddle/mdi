package edu.cnu.mdi.app;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import edu.cnu.mdi.desktop.Desktop;
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
import edu.cnu.mdi.sim.simanneal.tspdemo.TspDemoView;
import edu.cnu.mdi.splot.example.AnotherGaussian;
import edu.cnu.mdi.splot.example.CubicLogLog;
import edu.cnu.mdi.splot.example.ErfTest;
import edu.cnu.mdi.splot.example.ErfcTest;
import edu.cnu.mdi.splot.example.Gaussian;
import edu.cnu.mdi.splot.example.GrowingHisto;
import edu.cnu.mdi.splot.example.Heatmap;
import edu.cnu.mdi.splot.example.Histo;
import edu.cnu.mdi.splot.example.Scatter;
import edu.cnu.mdi.splot.example.StraightLine;
import edu.cnu.mdi.splot.example.StripChart;
import edu.cnu.mdi.splot.example.ThreeGaussians;
import edu.cnu.mdi.splot.example.TwoHisto;
import edu.cnu.mdi.splot.example.TwoLinesWithErrors;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.plot.BarPlot;
import edu.cnu.mdi.splot.plot.PlotView;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.view.BaseView;
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
	// Optional virtual desktop support
	// -------------------------------------------------------------------------

	/** Virtual desktop view (optional). */
	private VirtualView virtualView;

	/** If true, install the VirtualView and place views into columns. */
	private final boolean enableVirtualDesktop = true;

	/** Number of "columns"/cells in the virtual desktop. */
	private final int virtualDesktopCols = 6;

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
	private NetworkLayoutDemoView networkLayoutDemoView;

	/**
	 * Private constructor: use {@link #getInstance()}.
	 *
	 * @param keyVals optional key-value pairs passed to {@link BaseMDIApplication}
	 */
	private DemoApp(Object... keyVals) {
		super(keyVals);

		// Enable the framework-managed virtual desktop lifecycle (one-shot ready +
		// debounced relayout).
		prepareForVirtualDesktop();

		// Log environment information early.
		Log.getInstance().info(Environment.getInstance().toString());

		// Create internal views. (Do not depend on the outer frame being visible here.)
		addInitialViews();

		// Optionally create the virtual desktop overview.
		// Note: VirtualView now resolves its parent frame lazily in addNotify().
		if (enableVirtualDesktop) {
			virtualView = VirtualView.createVirtualView(virtualDesktopCols);
			virtualView.toFront();
		}
	}

	/**
	 * Public access to the singleton.
	 *
	 * @return the singleton main application frame
	 */
	public static DemoApp getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new DemoApp(PropertyUtils.TITLE, "Demo Application of MDI Views",
					PropertyUtils.BACKGROUNDIMAGE,
					Environment.MDI_RESOURCE_PATH + "images/mdilogo.png",
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
		plotView = createPlotView();

		// Network declutter demo view
		networkDeclutterDemoView = createNetworkDeclutterDemoView();

		// TSP demo view
		tspDemoView = createTspDemoView();

		// Network layout demo view
		networkLayoutDemoView = createNetworkLayoutDemoView();

	}

	@Override
    protected String getApplicationId() {
        return "mdiDemoApp";
    }

	/**
	 * Runs once after the outer frame is showing and Swing layout has stabilized.
	 * <p>
	 * This is the correct place to:
	 * <ul>
	 * <li>reconfigure the {@link VirtualView} based on the real frame size</li>
	 * <li>apply the demo's default view placement</li>
	 * <li>then load/apply any persisted layout (which may override defaults)</li>
	 * </ul>
	 */
	@Override
	protected void onVirtualDesktopReady() {
	    standardVirtualDesktopReady(virtualView, this::restoreDefaultViewLocations, true);
	    Log.getInstance().info("Classifier is ready.");		Log.getInstance().info("DemoApp is ready.");
	}

	/**
	 * Runs after the outer frame is resized or moved (debounced).
	 * <p>
	 * Keep this lightweight. Reconfiguring the virtual desktop updates its world
	 * sizing and refreshes the thumbnail items.
	 */
	@Override
	protected void onVirtualDesktopRelayout() {
		standardVirtualDesktopRelayout(virtualView);
	}

	/**
	 * Places the demo views into a reasonable "default" arrangement on the virtual
	 * desktop.
	 * <p>
	 * If a user has a saved configuration, {@link Desktop#configureViews()} will
	 * typically override these positions.
	 */
	private void restoreDefaultViewLocations() {
		// Column 0: map centered; drawing upper-left
		virtualView.moveTo(mapView, 0, VirtualView.BOTTOMRIGHT);
		virtualView.moveTo(drawingView, 0, VirtualView.TOPCENTER);

		// Column 1: plot view centered
		virtualView.moveTo(plotView, 1, VirtualView.CENTER);

		// Column 2: network declutter demo center
		virtualView.moveTo(networkDeclutterDemoView, 2, VirtualView.CENTER);
		networkDeclutterDemoView.setVisible(true);

		// Column 3: TSP demo center
		virtualView.moveTo(tspDemoView, 3, VirtualView.CENTER);
		tspDemoView.setVisible(true);

		// Column 4: network layout demo lower left
		virtualView.moveTo(networkLayoutDemoView, 4, 0, -50, VirtualView.BOTTOMLEFT);
		networkLayoutDemoView.setVisible(true);


	// column 5: log view upper left (is not vis by default)
		virtualView.moveTo(logView, 5, VirtualView.UPPERLEFT);

	}

	// -------------------------------------------------------------------------
	// Demo view creation helpers
	// -------------------------------------------------------------------------



	/**
	 * Create the demo plot view.
	 */
	PlotView createPlotView() {
		final PlotView view = new PlotView(PropertyUtils.TITLE, "Demo Plots",
				PropertyUtils.FRACTION, 0.7, PropertyUtils.ASPECT, 1.2, PropertyUtils.VISIBLE, true);

		// add the examples menu and call "hack" to fix focus issues
		JMenu examplesMenu = new JMenu("Gallery");
		BaseView.applyFocusFix(examplesMenu, view);
		view.getJMenuBar().add(examplesMenu, 1); // after File menu

		JMenuItem gaussianItem = new JMenuItem("Gaussian Fit");
		JMenuItem anotherGaussianItem = new JMenuItem("Another Gaussian");
		JMenuItem logItem = new JMenuItem("Log-log Plot");
		JMenuItem erfcItem = new JMenuItem("Erfc Fit");
		JMenuItem erfItem = new JMenuItem("Erf Fit");
		JMenuItem histoItem = new JMenuItem("Histogram");
		JMenuItem growingHistoItem = new JMenuItem("Growing Histogram");
		JMenuItem heatmapItem = new JMenuItem("Heatmap");
		JMenuItem lineItem = new JMenuItem("Straight Line Fit");
		JMenuItem stripItem = new JMenuItem("Memory Use Strip Chart");
		JMenuItem threeGaussiansItem = new JMenuItem("Three Gaussians");
		JMenuItem twoHistoItem = new JMenuItem("Two Histograms");
		JMenuItem twoLines = new JMenuItem("Two Lines with Errors");
		JMenuItem scatterItem = new JMenuItem("Scatter Example");
		JMenuItem barItem = new JMenuItem("Barplot Example");


		gaussianItem.addActionListener(e -> {
			Gaussian example = new Gaussian(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		anotherGaussianItem.addActionListener(e -> {
			AnotherGaussian example = new AnotherGaussian(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		logItem.addActionListener(e -> {
			CubicLogLog example = new CubicLogLog(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		erfcItem.addActionListener(e -> {
			ErfcTest example = new ErfcTest(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		erfItem.addActionListener(e -> {
			ErfTest example = new ErfTest(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		histoItem.addActionListener(e -> {
			Histo example = new Histo(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		growingHistoItem.addActionListener(e -> {
			GrowingHisto example = new GrowingHisto(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		heatmapItem.addActionListener(e -> {
			Heatmap example = new Heatmap(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		lineItem.addActionListener(e -> {
			StraightLine example = new StraightLine(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		stripItem.addActionListener(e -> {
			StripChart example = new StripChart(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		threeGaussiansItem.addActionListener(e -> {
			ThreeGaussians example = new ThreeGaussians(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		twoHistoItem.addActionListener(e -> {
			TwoHisto example = new TwoHisto(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		twoLines.addActionListener(e -> {
			TwoLinesWithErrors example = new TwoLinesWithErrors(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		scatterItem.addActionListener(e -> {
			Scatter example = new Scatter(true);
			view.switchToPlotPanel(example.getPlotPanel());
		});

		barItem.addActionListener(e -> {
			try {
				view.switchToPlotPanel(BarPlot.demoBarPlot());
			} catch (PlotDataException e1) {
				e1.printStackTrace();
			}

		});

		examplesMenu.add(gaussianItem);
		examplesMenu.add(anotherGaussianItem);
		examplesMenu.add(logItem);
		examplesMenu.add(erfcItem);
		examplesMenu.add(erfItem);
		examplesMenu.add(histoItem);
		examplesMenu.add(growingHistoItem);
		examplesMenu.add(heatmapItem);
		examplesMenu.add(lineItem);
		examplesMenu.add(stripItem);
		examplesMenu.add(threeGaussiansItem);
		examplesMenu.add(twoHistoItem);
		examplesMenu.add(twoLines);
		examplesMenu.add(scatterItem);
		examplesMenu.add(barItem);
		return view;
	}

	/**
	 * Create the network declutter demo view.
	 */
	NetworkDeclutterDemoView createNetworkDeclutterDemoView() {
		NetworkDeclutterDemoView view = new NetworkDeclutterDemoView(PropertyUtils.TITLE,
				"Network Declutter Demo View",
				PropertyUtils.FRACTION, 0.7, PropertyUtils.ASPECT, 1.2, PropertyUtils.VISIBLE, false,
				PropertyUtils.BACKGROUND, Color.white, PropertyUtils.WORLDSYSTEM,
				new Rectangle2D.Double(0.0, 0.0, 1, 1),
				PropertyUtils.INFOBUTTON, true);
		return view;
	}

	/**
	 * Create the TSP demo view.
	 */
	TspDemoView createTspDemoView() {
		TspDemoView view = new TspDemoView(PropertyUtils.TITLE,
				"TSP Demo View",
				PropertyUtils.FRACTION, 0.6, PropertyUtils.ASPECT, 1.2, PropertyUtils.VISIBLE, false,
				PropertyUtils.BACKGROUND, X11Colors.getX11Color("lavender blush"), PropertyUtils.WORLDSYSTEM,
				new Rectangle2D.Double(0.0,	 0.0, 1, 1));
		return view;
	}

	/**
	 * Create the network layout demo view.
	 * @return a new {@link NetworkLayoutDemoView}
	 */
	private NetworkLayoutDemoView createNetworkLayoutDemoView() {
		long toolBits = ToolBits.NAVIGATIONTOOLS | ToolBits.DELETE | ToolBits.CONNECTOR;
		NetworkLayoutDemoView view = new NetworkLayoutDemoView(PropertyUtils.FRACTION, 0.7, PropertyUtils.ASPECT,
				1.2, PropertyUtils.TOOLBARBITS, toolBits,
				PropertyUtils.VISIBLE, false,
				PropertyUtils.BACKGROUND, X11Colors.getX11Color("alice blue"), PropertyUtils.TITLE,
				"Network Layout Demo View");
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
			List<CountryFeature> countries = GeoJsonCountryLoader.loadFromResource(resPrefix + MapResources.COUNTRIES_GEOJSON);
			MapView2D.setCountries(countries);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Load cities as well.
		try {
			List<GeoJsonCityLoader.CityFeature> cities = GeoJsonCityLoader.loadFromResource(resPrefix + MapResources.CITIES_GEOJSON);
			MapView2D.setCities(cities);
		} catch (IOException e) {
			e.printStackTrace();
		}

		long toolBits =  ToolBits.INFO | ToolBits.STATUS | ToolBits.CENTER | ToolBits.ZOOMTOOLS | ToolBits.DRAWINGTOOLS | ToolBits.MAGNIFY ;

		// Create the view with a reasonable default configuration.
		return new MapView2D(PropertyUtils.TITLE, "Sample 2D Map View",
				PropertyUtils.FRACTION, 0.6, PropertyUtils.ASPECT, 1.5, PropertyUtils.CONTAINERCLASS,
				MapContainer.class, PropertyUtils.TOOLBARBITS, toolBits);
	}

	/**
	 * Entry point for the demo.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			DemoApp frame = DemoApp.getInstance();
			frame.restoreDefaultViewLocations();
			frame.setVisible(true);
		});
	}
}
