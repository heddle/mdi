package edu.cnu.mdi.app;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import edu.cnu.mdi.desktop.Desktop;
import edu.cnu.mdi.graphics.toolbar.ToolBarBits;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.mapping.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.mapping.MapContainer;
import edu.cnu.mdi.mapping.MapView2D;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.Cube;
import edu.cnu.mdi.mdi3D.item3D.Cylinder;
import edu.cnu.mdi.mdi3D.item3D.PointSet3D;
import edu.cnu.mdi.mdi3D.item3D.Triangle3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.view3D.PlainView3D;
import edu.cnu.mdi.properties.PropertySupport;
import edu.cnu.mdi.sim.demo.network.NetworkDeclutterDemoView;
import edu.cnu.mdi.splot.example.AnotherGaussian;
import edu.cnu.mdi.splot.example.ErfTest;
import edu.cnu.mdi.splot.example.ErfcTest;
import edu.cnu.mdi.splot.example.Gaussian;
import edu.cnu.mdi.splot.example.GrowingHisto;
import edu.cnu.mdi.splot.example.Histo;
import edu.cnu.mdi.splot.example.Scatter;
import edu.cnu.mdi.splot.example.StraightLine;
import edu.cnu.mdi.splot.example.StripChart;
import edu.cnu.mdi.splot.example.ThreeGaussians;
import edu.cnu.mdi.splot.example.TwoHisto;
import edu.cnu.mdi.splot.example.TwoLinesWithErrors;
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
 *   <li>Creates the main application frame ({@link BaseMDIApplication})</li>
 *   <li>Creates a few internal views (2D map, 3D, drawing, log)</li>
 *   <li>Optionally enables a {@link VirtualView} to simulate a virtual desktop</li>
 *   <li>Applies default view placement, then applies any persisted layout/config</li>
 * </ol>
 * <p>
 * The "virtual desktop" logic is driven by {@link BaseMDIApplication}'s virtual
 * desktop lifecycle hooks:
 * <ul>
 *   <li>{@link #onVirtualDesktopReady()} runs once after the frame is showing</li>
 *   <li>{@link #onVirtualDesktopRelayout()} runs (debounced) after resizes/moves</li>
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

    private PlainView3D view3D;
    private DrawingView drawingView;
    private MapView2D mapView;
    private LogView logView;
    private PlotView plotView;
    private NetworkDeclutterDemoView networkDeclutterDemoView;
    private NetworkLayoutDemoView networkLayoutDemoView;

    /**
     * Private constructor: use {@link #getInstance()}.
     *
     * @param keyVals optional key-value pairs passed to {@link BaseMDIApplication}
     */
    private DemoApp(Object... keyVals) {
        super(keyVals);

        // Enable the framework-managed virtual desktop lifecycle (one-shot ready + debounced relayout).
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
            INSTANCE = new DemoApp(
                    PropertySupport.TITLE, "Demo Application of MDI Views",
                    PropertySupport.BACKGROUNDIMAGE, "images/mdilogo.png",
                    PropertySupport.FRACTION, 0.8
            );
        }
        return INSTANCE;
    }

    /**
     * Create and register the initial set of views shown in the demo.
     * <p>
     * This method only builds views; it should not depend on the outer frame
     * being shown or on final geometry.
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

        // Network layout demo view
        networkLayoutDemoView = createNetworkLayoutDemoView();


    }

    /**
     * Runs once after the outer frame is showing and Swing layout has stabilized.
     * <p>
     * This is the correct place to:
     * <ul>
     *   <li>reconfigure the {@link VirtualView} based on the real frame size</li>
     *   <li>apply the demo's default view placement</li>
     *   <li>then load/apply any persisted layout (which may override defaults)</li>
     * </ul>
     */
    @Override
    protected void onVirtualDesktopReady() {

        // If virtual desktop is enabled, apply the demo defaults first.
        if (virtualView != null) {
            virtualView.reconfigure();
            restoreDefaultViewLocations();
        }

        // Apply persisted configuration last, so saved layouts override demo defaults.
        Desktop.getInstance().loadConfigurationFile();
        Desktop.getInstance().configureViews();

        Log.getInstance().info("DemoApp is ready.");
    }

    /**
     * Runs after the outer frame is resized or moved (debounced).
     * <p>
     * Keep this lightweight. Reconfiguring the virtual desktop updates its world
     * sizing and refreshes the thumbnail items.
     */
    @Override
    protected void onVirtualDesktopRelayout() {
        if (virtualView != null) {
            virtualView.reconfigure();
        }
    }

    /**
     * Places the demo views into a reasonable "default" arrangement on the
     * virtual desktop.
     * <p>
     * If a user has a saved configuration, {@link Desktop#configureViews()}
     * will typically override these positions.
     */
    private void restoreDefaultViewLocations() {
        // Column 0: map centered; drawing upper-left
        virtualView.moveTo(mapView, 0, VirtualView.BOTTOMRIGHT);
        virtualView.moveTo(drawingView, 0, VirtualView.TOPCENTER);

        // Column 1: 3D centered
        virtualView.moveTo(view3D, 1, VirtualView.CENTER);

        // Column 2: plot view centered
        virtualView.moveTo(plotView, 2, VirtualView.CENTER);

        // Column 3: network declutter demo centered
        virtualView.moveTo(networkDeclutterDemoView, 3, VirtualView.CENTER);
        networkDeclutterDemoView.setVisible(true);

        // Column 4: network layout demo lower left
        virtualView.moveTo(networkLayoutDemoView, 4, 0, -50, VirtualView.BOTTOMLEFT);
        networkLayoutDemoView.setVisible(true);

        //column 5: log view upper left (is not vis by default)
        virtualView.moveTo(logView, 5, VirtualView.UPPERLEFT);

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

        PlainView3D view3D = new PlainView3D(
                PropertySupport.TITLE, "Sample 3D View",
                PropertySupport.ANGLE_X, thetax,
                PropertySupport.ANGLE_Y, thetay,
                PropertySupport.ANGLE_Z, thetaz,
                PropertySupport.DIST_X, xdist,
                PropertySupport.DIST_Y, ydist,
                PropertySupport.DIST_Z, zdist,
                PropertySupport.LEFT, 0,
                PropertySupport.TOP, 0,
                PropertySupport.FRACTION, 0.75,
                PropertySupport.ASPECT, 1.25) {

            @Override
            protected Panel3D make3DPanel(float angleX, float angleY, float angleZ,
                                          float xDist, float yDist, float zDist) {

                return new Panel3D(thetax, thetay, thetaz, xdist, ydist, zdist) {

                    @Override
                    public void createInitialItems() {

                        // Coordinate axes
                        Axes3D axes = new Axes3D(this,
                                -xymax, xymax, -xymax, xymax, zmin, zmax,
                                null, Color.darkGray, 1f,
                                7, 7, 8,
                                Color.black, Color.blue,
                                new Font("SansSerif", Font.PLAIN, 11), 0);
                        addItem(axes);

                        // Some triangles
                        addItem(new Triangle3D(this,
                                500f, 0f, -200f,
                                -500f, 500f, 0f,
                                0f, -100f, 500f,
                                new Color(255, 0, 0, 64), 1f, true));

                        addItem(new Triangle3D(this,
                                0f, 500f, 0f,
                                -300f, -500f, 500f,
                                0f, -100f, 500f,
                                new Color(0, 0, 255, 64), 2f, true));

                        addItem(new Triangle3D(this,
                                0f, 0f, 500f,
                                0f, -400f, -500f,
                                500f, -100f, 500f,
                                new Color(0, 255, 0, 64), 2f, true));

                        addItem(new Cylinder(this,
                                0f, 0f, 0f,
                                300f, 300f, 300f,
                                50f, new Color(0, 255, 255, 128)));

                        addItem(new Cube(this,
                                0f, 0f, 0f,
                                600, new Color(0, 0, 255, 32), true));

                        // Point set test
                        int numPnt = 100;
                        Color color = Color.orange;
                        float pntSize = 10;
                        float[] coords = new float[3 * numPnt];

                        for (int i = 0; i < numPnt; i++) {
                            int j = i * 3;
                            float x = (float) (-xymax + 2 * xymax * Math.random());
                            float y = (float) (-xymax + 2 * xymax * Math.random());
                            float z = (float) (zmin + (zmax - zmin) * Math.random());
                            coords[j] = x;
                            coords[j + 1] = y;
                            coords[j + 2] = z;
                        }

                        addItem(new PointSet3D(this, coords, color, pntSize, true));
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
    			PlotView view = new PlotView(
    					PropertySupport.TITLE, "Demo Plots",
    					PropertySupport.PROPNAME, "PLOTVIEW",
    					PropertySupport.FRACTION, 0.7,
    					PropertySupport.ASPECT, 1.2,
    					PropertySupport.BACKGROUND, X11Colors.getX11Color("light yellow"),
    					PropertySupport.VISIBLE, true
    					);

    			//add the examples menu
    			JMenu examplesMenu = new JMenu("Gallery");
    			view.getJMenuBar().add(examplesMenu);

				JMenuItem gaussianItem = new JMenuItem("Gaussian Fit");
				JMenuItem anotheGaussianItem = new JMenuItem("Another Gaussian");
				JMenuItem erfcItem = new JMenuItem("Erfc Fit");
				JMenuItem erfItem = new JMenuItem("Erf Fit");
				JMenuItem histoItem = new JMenuItem("Histogram");
				JMenuItem growingHistoItem = new JMenuItem("Growing Histogram");
				JMenuItem lineItem = new JMenuItem("Straight Line Fit");
				JMenuItem stripItem = new JMenuItem("Memory Use Strip Chart");
				JMenuItem threeGaussiansItem = new JMenuItem("Three Gaussians");
				JMenuItem twoHistoItem = new JMenuItem("Two Histograms");
				JMenuItem twoLines = new JMenuItem("Two Lines with Errors");

				gaussianItem.addActionListener(e -> {
					Gaussian example = new Gaussian(true);
					view.switchToExample(example);
				});

				anotheGaussianItem.addActionListener(e -> {
					AnotherGaussian example = new AnotherGaussian(true);
					view.switchToExample(example);
				});
				
				erfcItem.addActionListener(e -> {
					ErfcTest example = new ErfcTest(true);
					view.switchToExample(example);
				});
				
				erfItem.addActionListener(e -> {
					ErfTest example = new ErfTest(true);
					view.switchToExample(example);
				});

				histoItem.addActionListener(e -> {
					Histo example = new Histo(true);
					view.switchToExample(example);
				});
				
				growingHistoItem.addActionListener(e -> {
					GrowingHisto example = new GrowingHisto(true);
					view.switchToExample(example);
				});
				
				lineItem.addActionListener(e -> {
					StraightLine example = new StraightLine(true);
					view.switchToExample(example);
				});
				
				stripItem.addActionListener(e -> {
					StripChart example = new StripChart(true);
					view.switchToExample(example);
				});
				
				threeGaussiansItem.addActionListener(e -> {
					ThreeGaussians example = new ThreeGaussians(true);
					view.switchToExample(example);
				});
				
				twoHistoItem.addActionListener(e -> {
					TwoHisto example = new TwoHisto(true);
					view.switchToExample(example);
				});
				
				twoLines.addActionListener(e -> {
					TwoLinesWithErrors example = new TwoLinesWithErrors(true);
					view.switchToExample(example);
				});
				

				examplesMenu.add(gaussianItem);
				examplesMenu.add(anotheGaussianItem);
				examplesMenu.add(erfcItem);
				examplesMenu.add(erfItem);
				examplesMenu.add(histoItem);
				examplesMenu.add(growingHistoItem);
				examplesMenu.add(lineItem);
				examplesMenu.add(stripItem);
				examplesMenu.add(threeGaussiansItem);
				examplesMenu.add(twoHistoItem);
				examplesMenu.add(twoLines);
				

    			return view;
    }
    /**
	 * Create the network declutter demo view.
	 */
    NetworkDeclutterDemoView createNetworkDeclutterDemoView() {
		NetworkDeclutterDemoView view = new NetworkDeclutterDemoView(
				PropertySupport.TITLE, "Network Declutter Demo View",
				PropertySupport.PROPNAME, "NETWORKDECLUTTERDEMO",
				PropertySupport.FRACTION, 0.7,
				PropertySupport.ASPECT, 1.2,
				PropertySupport.VISIBLE, false,
				PropertySupport.BACKGROUND, Color.white,
				PropertySupport.WORLDSYSTEM, new Rectangle2D.Double(0.0, 0.0, 1, 1)
		);
		return view;
	}

    /**
 	 * Create the network layout demo view.
 	 */
     NetworkLayoutDemoView createNetworkLayoutDemoView() {
    	 NetworkLayoutDemoView view = new NetworkLayoutDemoView(
    			PropertySupport.FRACTION, 0.7,
    			PropertySupport.ASPECT, 1.2,
		    	PropertySupport.TOOLBARBITS, ToolBarBits.DEFAULTS | ToolBarBits.CONNECTORBUTTON,
 				PropertySupport.VISIBLE, false,
 				PropertySupport.PROPNAME, "NETWORKLAYOUTDEMO",
 				PropertySupport.BACKGROUND, X11Colors.getX11Color("alice blue"),
 				PropertySupport.TITLE, "Network Layout Demo View "
 		);
 		return view;
 	}


    /**
     * Create the demo map view and load small GeoJSON datasets from resources.
     *
     * @return a new {@link MapView2D}
     */
    private MapView2D createMapView() {

        // Load a small set of countries just for demo purposes.
        try {
            List<CountryFeature> countries =
                    GeoJsonCountryLoader.loadFromResource("/geo/countries.geojson");
            MapView2D.setCountries(countries);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Load cities as well (optional).
        try {
            List<GeoJsonCityLoader.CityFeature> cities =
                    GeoJsonCityLoader.loadFromResource("/geo/cities.geojson");
            MapView2D.setCities(cities);
        } catch (IOException e) {
            e.printStackTrace();
        }

        long toolbarBits = ToolBarBits.DRAWING| ToolBarBits.MAGNIFYBUTTON;

        return new MapView2D(
                PropertySupport.TITLE, "Sample 2D Map View",
                PropertySupport.PROPNAME, "MAPVIEW2D",
                PropertySupport.FRACTION, 0.6,
                PropertySupport.ASPECT, 1.5,
                PropertySupport.CONTAINERCLASS, MapContainer.class,
                PropertySupport.TOOLBARBITS, toolbarBits
        );
    }

    /**
     * Entry point for the demo.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
       EventQueue.invokeLater(() -> {

             DemoApp frame = DemoApp.getInstance();
             frame.setVisible(true);
        });
    }
}
