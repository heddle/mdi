package edu.cnu.mdi.app;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.io.IOException;
import java.util.List;

import edu.cnu.mdi.graphics.toolbar.ToolBarBits;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.mapping.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.MapView2D;
import edu.cnu.mdi.mapping.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.Cube;
import edu.cnu.mdi.mdi3D.item3D.Cylinder;
import edu.cnu.mdi.mdi3D.item3D.PointSet3D;
import edu.cnu.mdi.mdi3D.item3D.Triangle3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.view3D.PlainView3D;
import edu.cnu.mdi.properties.PropertySupport;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.util.FileUtils;
import edu.cnu.mdi.view.DrawingView;
import edu.cnu.mdi.view.LogView;
import edu.cnu.mdi.view.ViewManager;

/**
 * Demonstrates and tests the generic views
 *
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public class DemoApp extends BaseMDIApplication {

	// the singleton
	private static DemoApp instance;

	/**
	 * Constructor (private--used to create singleton)
	 *
	 * @param keyVals an optional variable length list of attributes in type-value
	 *                pairs. For example, AttributeType.NAME, "my application",
	 *                AttributeType.CENTER, true, etc.
	 */
	private DemoApp(Object... keyVals) {
		super(keyVals);
	}

	/**
	 * Public access to the singleton.
	 *
	 * @return the singleton (the main application frame.)(
	 */
	public static DemoApp getInstance() {
		if (instance == null) {
			instance = new DemoApp(PropertySupport.TITLE, "Demo Application of Generic bCNU Views",
					PropertySupport.BACKGROUNDIMAGE, "images/mdilogo.png", PropertySupport.FRACTION, 0.8);

			instance.addInitialViews();
		}
		return instance;
	}

	/**
	 * Add the initial views to the desktop.
	 */
	private void addInitialViews() {

		// add logview
		LogView logView = new LogView();
		logView.setVisible(false);
		ViewManager.getInstance().getViewMenu().addSeparator();

		// log some environment info
		Log.getInstance().info(Environment.getInstance().toString());

		// drawing view
		DrawingView drawingView = DrawingView.createDrawingView();
		drawingView.setVisible(true);

		// sample 3D view
		PlainView3D view3D = create3DView();

		// map view
		MapView2D mapView = createMapView();
	}

	// create the sample 3D view
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

		PlainView3D view3D = new PlainView3D(PropertySupport.TITLE, "Sample 3D View", PropertySupport.ANGLE_X, thetax,
				PropertySupport.ANGLE_Y, thetay, PropertySupport.ANGLE_Z, thetaz, PropertySupport.DIST_X, xdist,
				PropertySupport.DIST_X, ydist, PropertySupport.DIST_X, zdist, PropertySupport.LEFT, 800,
				PropertySupport.TOP, 600,

				PropertySupport.WIDTH, 400, PropertySupport.HEIGHT, 400) {
			@Override
			protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist,
					float zDist) {
				return new Panel3D(thetax, thetay, thetaz, xdist, ydist, zdist) {
					@Override
					public void createInitialItems() {
						// coordinate axes

						Axes3D axes = new Axes3D(this, -xymax, xymax, -xymax, xymax, zmin, zmax, null, Color.darkGray,
								1f, 7, 7, 8, Color.black, Color.blue, new Font("SansSerif", Font.PLAIN, 11), 0);
						addItem(axes);

						// add some triangles

						// addItem(new Triangle3D(this,
						// 0f, 0f, 0f, 100f, 0f, -100f, 50f, 100, 100f, new Color(255,
						// 0, 0, 64), 2f, true));

						addItem(new Triangle3D(this, 500f, 0f, -200f, -500f, 500f, 0f, 0f, -100f, 500f,
								new Color(255, 0, 0, 64), 1f, true));

						addItem(new Triangle3D(this, 0f, 500f, 0f, -300f, -500f, 500f, 0f, -100f, 500f,
								new Color(0, 0, 255, 64), 2f, true));

						addItem(new Triangle3D(this, 0f, 0f, 500f, 0f, -400f, -500f, 500f, -100f, 500f,
								new Color(0, 255, 0, 64), 2f, true));

						addItem(new Cylinder(this, 0f, 0f, 0f, 300f, 300f, 300f, 50f, new Color(0, 255, 255, 128)));

						addItem(new Cube(this, 0f, 0f, 0f, 600, new Color(0, 0, 255, 32), true));

						// point set test
						int numPnt = 100;
						Color color = Color.orange;
						float pntSize = 10;
						float coords[] = new float[3 * numPnt];
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

					/**
					 * This gets the z step used by the mouse and key adapters, to see how fast we
					 * move in or in in response to mouse wheel or up/down arrows. It should be
					 * overridden to give something sensible. like the scale/100;
					 *
					 * @return the z step (changes to zDist) for moving in and out
					 */
					@Override
					public float getZStep() {
						return (zmax - zmin) / 50f;
					}

				};
			}

		};
		return view3D;
	}

	// create the demo map view
	private MapView2D createMapView() {
		
		//load a small set of countries just for demo purposes
		try {
			List<CountryFeature> countries = GeoJsonCountryLoader.loadFromResource("/geo/countries.geojson");
			MapView2D.setCountries(countries);
		} catch (IOException e) {
			e.printStackTrace();
		}


		long toolbarBits = ToolBarBits.CENTERBUTTON | ToolBarBits.PANBUTTON;

		MapView2D mapView = new MapView2D(PropertySupport.TITLE, "Sample 2D Map View", 
				PropertySupport.LEFT, 300,
				PropertySupport.TOP, 300, 
				PropertySupport.WIDTH, 700, 
				PropertySupport.HEIGHT, 500,
				PropertySupport.BACKGROUND, X11Colors.getX11Color("alice blue"), 
				PropertySupport.TOOLBARBITS,
				toolbarBits);
		
		return mapView;
	}

	/**
	 * Main program used for testing only.
	 * <p>
	 * Command line arguments:</br>
	 * -p [dir] dir is the optional default directory for the file manager
	 *
	 * @param arg the command line arguments (ignored).
	 */
	public static void main(String[] arg) {

		final DemoApp frame = getInstance();

		// now make the frame visible, in the AWT thread
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				frame.setVisible(true);
			}

		});
		Log.getInstance().info("DemoApp is ready.");
	}
}
