package edu.cnu.mdi.app;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.mdi.desktop.Desktop;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.mapping.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.mapping.MapContainer;
import edu.cnu.mdi.mapping.MapView2D;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.item3D.Line3D;
import edu.cnu.mdi.mdi3D.item3D.PointSet3D;
import edu.cnu.mdi.mdi3D.item3D.Sphere;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;
import edu.cnu.mdi.mdi3D.view3D.PlainView3D;
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
	
	static {
	    System.setProperty("sun.java2d.noddraw", "true"); // Prevent conflict with Java2D
	    com.jogamp.opengl.GLProfile.initSingleton();
	}

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
	private final int virtualDesktopCols = 7;

	// -------------------------------------------------------------------------
	// Sample views used by the demo. None are meant to be completely realistic.
	// or functional, except for the LogView.
	// -------------------------------------------------------------------------

	private PlainView3D view3D;
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

		// 3D view
		view3D = create3DView();

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

		// Column 5: 3D centered
		virtualView.moveTo(view3D, 5, VirtualView.CENTER);

	// column 6: log view upper left (is not vis by default)
		virtualView.moveTo(logView, 6, VirtualView.UPPERLEFT);

	}

	// -------------------------------------------------------------------------
	// Demo view creation helpers
	// -------------------------------------------------------------------------

	/**
	 * Create a sample 3D view with a few items to demonstrate rendering.
	 *
	 * @return a new {@link PlainView3D}
	 */
	private PlainView3D create3DView() {
		final float xymax = 600f;
		final float zmax = 600f;
		final float zmin = -100f;
		final float xdist = 0f;
		final float ydist = 0f;
		final float zdist = -2.75f * xymax;

		final float thetax = 45f;
		final float thetay = 45f;
		final float thetaz = 45f;

		PlainView3D view3D = new PlainView3D(PropertyUtils.TITLE, "Sample 3D View", PropertyUtils.ANGLE_X, thetax,
				PropertyUtils.ANGLE_Y, thetay, PropertyUtils.ANGLE_Z, thetaz, PropertyUtils.DIST_X, xdist,
				PropertyUtils.DIST_Y, ydist, PropertyUtils.DIST_Z, zdist, PropertyUtils.LEFT, 0,
				PropertyUtils.TOP, 0, PropertyUtils.FRACTION, 0.75, PropertyUtils.ASPECT, 1.25) {

			@Override
			protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist,
					float zDist) {

				return new Panel3D(thetax, thetay, thetaz, xdist, ydist, zdist) {

					@Override
					public void createInitialItems() {

						final float xymax = 600f;
						final float zmin = -200f;
						final float zmax = 600f;

						// 1) Axes
						addItem(new Axes3D(this, -xymax, xymax, -xymax, xymax, zmin, zmax, null, Color.darkGray, 1f, 7,
								7, 8, Color.black, Color.blue, new Font("SansSerif", Font.PLAIN, 11), 0));

						// 2) Floor grid (y=0 plane), using many Line3D items
						final float gridHalf = 600f;
						final float step = 100f;
						final float yGrid = 0f;
						final Color gridColor = new Color(0, 0, 0, 40);

						for (float x = -gridHalf; x <= gridHalf; x += step) {
							addItem(new Line3D(this, x, yGrid, -gridHalf, x, yGrid, gridHalf, gridColor, 1f));
						}
						for (float z = -gridHalf; z <= gridHalf; z += step) {
							addItem(new Line3D(this, -gridHalf, yGrid, z, gridHalf, yGrid, z, gridColor, 1f));
						}

						// 3) “Earth” sphere at origin with gridlines
						Sphere earth = new Sphere(this, 0f, 0f, 0f, 140f, new Color(40, 100, 220, 128));
						earth.setFillAlpha(128);
						earth.setResolution(32, 24);

						// latitude + longitude lines
						float[] theta = new float[] { (float) (Math.PI * 0.25), (float) (Math.PI * 0.5),
								(float) (Math.PI * 0.75) };
						float[] phi = new float[] { 0f, (float) (Math.PI / 3), (float) (2 * Math.PI / 3),
								(float) (Math.PI) };
						earth.setGridlines(theta, phi);
						earth.setGridColor(new Color(0, 0, 0, 90));
						addItem(earth);

						// 4) Orbit ring (tilted circle polyline) – anonymous Item3D
						final float moonOrbitalRad = 320f;
						final float satOrbitalRad = 220f;

						// Kepler's 3rd law: (Tm/Ts)^2 = (Rm/Rs)^3
						final double periodFact = Math.pow(moonOrbitalRad / satOrbitalRad, 1.5f);
						final float tilt = (float) Math.toRadians(25);
						final int n = 240;

						final float[] orbitCoords = new float[3 * (n + 1)];
						for (int i = 0; i <= n; i++) {
							float a = (float) (2 * Math.PI * i / n);
							float x = moonOrbitalRad * (float) Math.cos(a);
							float z = moonOrbitalRad * (float) Math.sin(a);

							// tilt about x-axis -> rotates y/z
							float y = z * (float) Math.sin(tilt);
							float zt = z * (float) Math.cos(tilt);

							orbitCoords[3 * i] = x;
							orbitCoords[3 * i + 1] = y;
							orbitCoords[3 * i + 2] = zt;
						}

						addItem(new Item3D(this) {
							@Override
							public void draw(GLAutoDrawable drawable) {
								Support3D.drawPolyLine(drawable, orbitCoords, new Color(0, 0, 0, 110), 2f);
							}
						});

						// “Moon” sphere we will animate by updating its center
						final Sphere moon = new Sphere(this, moonOrbitalRad, 0f, 0f, 45f, new Color(200, 200, 200));
						moon.setResolution(24, 18);
						moon.setGridlines(theta, phi);
						moon.setGridColor(new Color(0, 0, 0, 90));
						addItem(moon);

						// A simple “satellite” point (or small sphere/cylinder)
						final float[] sat = new float[] { moonOrbitalRad, 0f, 0f };
						addItem(new PointSet3D(this, sat, new Color(255, 120, 0), 10f, true));

						final int tstep = 500;
						final int nstep = 28;
						final long startTime = System.currentTimeMillis();
						// 7) Optional animation: orbit the moon + satellite
						javax.swing.Timer timer = new javax.swing.Timer(tstep, e -> {
							double t = System.currentTimeMillis() - startTime;
							t = t * 2 * Math.PI / (nstep * tstep);

							float xmoon = moonOrbitalRad * (float) Math.cos(t);
							float zmoon = moonOrbitalRad * (float) Math.sin(t);

							float ymoon = zmoon * (float) Math.sin(tilt);
							float zt = zmoon * (float) Math.cos(tilt);
							moon.setCenter(xmoon, ymoon, zt);

							t *= periodFact; // faster orbit for satellite
							float xsat = satOrbitalRad * (float) Math.cos(t);
							float zsat = satOrbitalRad * (float)Math.sin(t);
							float ysat = zsat * (float)Math.sin(tilt);
							float zsatTilt = zsat * (float)Math.cos(tilt);

							sat[0] = xsat;
							sat[1] = ysat;
							sat[2] = zsatTilt;

							refresh(); // redraw
						});
						timer.start();
					}

					@Override
					public float getZStep() {
						// Step size for zooming in/out.
						return (zmax - zmin) / 50f;
					}
				};
			}
		};

		return view3D;
	}

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
				new Rectangle2D.Double(0.0, 0.0, 1, 1));
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
				"Network Layout Demo View ");
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
			List<CountryFeature> countries = GeoJsonCountryLoader.loadFromResource(resPrefix + "/geo/countries.geojson");
			MapView2D.setCountries(countries);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Load cities as well (optional).
		try {
			List<GeoJsonCityLoader.CityFeature> cities = GeoJsonCityLoader.loadFromResource(resPrefix + "/geo/cities.geojson");
			MapView2D.setCities(cities);
		} catch (IOException e) {
			e.printStackTrace();
		}

		long toolBits =  ToolBits.STATUS | ToolBits.CENTER | ToolBits.ZOOMTOOLS | ToolBits.DRAWINGTOOLS | ToolBits.MAGNIFY ;

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
