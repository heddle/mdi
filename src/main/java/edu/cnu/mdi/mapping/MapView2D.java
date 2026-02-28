package edu.cnu.mdi.mapping;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.JPanel;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.graphics.drawable.IDrawable;
import edu.cnu.mdi.graphics.text.UnicodeUtils;
import edu.cnu.mdi.mapping.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.view.AbstractViewInfo;
import edu.cnu.mdi.view.BaseView;

/**
 * A two-dimensional map view that renders world maps using different
 * projections and themes. This view is responsible for:
 * <ul>
 * <li>Managing the current map projection and world coordinate system.</li>
 * <li>Rendering graticules, country boundaries, and cities.</li>
 * <li>Providing feedback on the current mouse position and picked geographic
 * features.</li>
 * <li>Exposing a side control panel for interactive control of projection, city
 * display, population threshold, and theme.</li>
 * </ul>
 * <p>
 * The control panel is placed on the east side of the view, above the feedback
 * pane, and uses the default Swing look-and-feel background color.
 * </p>
 */
@SuppressWarnings("serial")
public class MapView2D extends BaseView {

	// share country boundaries across all map views
	private static List<CountryFeature> _countries;

	// share cities across all map views
	private static List<GeoJsonCityLoader.CityFeature> _cities;

	// the map projection
	private IMapProjection _projection;
	private GraticuleRenderer _gratRenderer;

	// control panel (projection / city / theme controls)
	private MapControlPanel _controlPanel;

	// country renderer
	private CountryRenderer _countryRenderer;

	// city renderer
	private CityPointRenderer _cityRenderer;

	// workspace and strings for feedback
	private Point2D.Double _latLon = new Point2D.Double();
	private static String _latPrefix = "$yellow$Lat (" + UnicodeUtils.SMALL_PHI + ")";
	private static String _lonPrefix = "$yellow$Lon (" + UnicodeUtils.SMALL_LAMBDA + ")";
	private static String _deg = UnicodeUtils.DEGREE;

	// default side panel width (control panel + feedback)
	private static final int SIDE_PANEL_WIDTH = 220;

	// max slider value for minimum population
	private static final int MAX_POP_SLIDER_VALUE = 2_000_000;

	/**
	 * Create a map view with the given key-value pairs.
	 *
	 * @param keyVals variable list of arguments used by {@link BaseView} to
	 *                configure the view.
	 */
	public MapView2D(Object... keyVals) {
		super(keyVals);

		// create the control panel
		_controlPanel = new MapControlPanel(this);

		// default to Mercator projection
		setProjection(EProjection.MERCATOR);

		// set the feedback and side (control + feedback) UI
		initSidePanel();

		// set the after draw
		setAfterDraw();

	}

	/**
	 * Initialize the side panel that contains the control panel (on top) and the
	 * feedback pane (on the bottom). The combined side panel is added to the east
	 * side of the view.
	 */
	private void initSidePanel() {
		// feedback control and provider (use default coloring)
		FeedbackPane fbp = initFeedback();

		// container panel holding control panel (NORTH) and feedback (CENTER)
		JPanel sidePanel = new JPanel(new BorderLayout());

		// ensure a consistent preferred width for the whole side strip
		Dimension feedbackPref = fbp.getPreferredSize();
		fbp.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, feedbackPref.height));
		_controlPanel.setMaximumSize(new Dimension(SIDE_PANEL_WIDTH, Integer.MAX_VALUE));

		sidePanel.add(_controlPanel, BorderLayout.NORTH);
		sidePanel.add(fbp, BorderLayout.CENTER);
		sidePanel.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, getHeight()));

		add(sidePanel, BorderLayout.EAST);
	}

	/**
	 * Get the view info object for this map view, which provides metadata and
	 * content for the "Info" dialog. This method is called by the base view when
	 * the user clicks the "Info" button.
	 *
	 * @return an instance of {@link MapViewInfo} containing information about this
	 *         map view.
	 */
	@Override
	public AbstractViewInfo getViewInfo() {
		return new MapViewInfo();
	}


	/**
	 * Set the view's "after draw" operation, which performs the actual map
	 * rendering steps in the correct order:
	 * <ol>
	 * <li>Clear the background using the current theme's background color.</li>
	 * <li>Fill the ocean within the projection's clipping shape.</li>
	 * <li>Render the graticule and map outline.</li>
	 * <li>Render country polygons.</li>
	 * <li>Render cities.</li>
	 * </ol>
	 */
	private void setAfterDraw() {
		IDrawable afterDraw = new DrawableAdapter() {
			@Override
			public void draw(Graphics2D g, IContainer container) {
				// 1. Clear background
				g.setColor(_projection.getTheme().getBackgroundColor());
				g.fillRect(0, 0, getWidth(), getHeight());

				// 2. Fill ocean inside projection clip
				_projection.fillOcean(g, container);

				// 3. Draw graticule and outline on top
				_gratRenderer.render(g, container);

				// 4. Draw land polygons, labels, etc...
				// countryFeatureRenderer.render(g, container);
				_countryRenderer.render(g, container);
				_cityRenderer.render(g, container);
			}
		};

		getContainer().setAfterDraw(afterDraw);
	}

	/**
	 * Get the current map projection.
	 *
	 * @return the current {@link IMapProjection}.
	 */
	public IMapProjection getProjection() {
		return _projection;
	}

	/**
	 * Set the current map projection, updating the world coordinate system,
	 * graticule renderer, country renderer, and city renderer accordingly.
	 *
	 * @param projection the new {@link EProjection} to set.
	 */
	public void setProjection(EProjection projection) {

		_projection = ProjectionFactory.create(projection, _controlPanel.getCurrentTheme());
		_gratRenderer = new GraticuleRenderer(_projection);

		getContainer().resetWorldSystem(getWorldSystem(_projection.getProjection()));

		_countryRenderer = new CountryRenderer(_countries, _projection);

		_cityRenderer = new CityPointRenderer(_cities, _projection);
		_cityRenderer.setPointRadius(1.5);
		_cityRenderer.setMinPopulation(MAX_POP_SLIDER_VALUE);
		_cityRenderer.setDrawLabels(true);

		refresh();

	}

	/**
	 * Get the default world coordinate system bounds for the given projection.
	 *
	 * @param eprojection the projection enum.
	 * @return the world bounds as a rectangle in projection coordinates.
	 */
	private Rectangle2D.Double getWorldSystem(EProjection eprojection) {

		double xLim;
		double yLim;

		switch (eprojection) {
		case MOLLWEIDE:
			xLim = 2.9;
			yLim = xLim;
			break;
		case MERCATOR:
			xLim = 1.1 * Math.PI;
			yLim = xLim;
			break;
		case ORTHOGRAPHIC:
			xLim = 1.1;
			yLim = xLim;
			break;
		case LAMBERT_EQUAL_AREA:
			xLim = 1.5 * Math.PI / 2;
			yLim = xLim;
			break;
		default:
			xLim = 1.1;
			yLim = xLim;
		}
		return new Rectangle2D.Double(-xLim, -yLim, 2 * xLim, 2 * yLim);
	}
	
	/**
	 * Get the country at the given screen point, if any. This method uses the
	 * country renderer's picking method to determine if a country is under the
	 * cursor and returns a formatted string with the country's name and ISO code.
	 *
	 * @param pp        the screen-space point to check for a country.
	 * @param container the host container, used for coordinate transformations.
	 * @return a string with the country's name and ISO code if a country is hit,
	 *         or null if no country is under the cursor.
	 */
	public String getCountryAtPoint(Point pp, IContainer container) {
		GeoJsonCountryLoader.CountryFeature countryHit = _countryRenderer.pickCountry(pp, container);
		if (countryHit != null) {
			return String.format("%s (%s)", countryHit.getAdminName(), countryHit.getIsoA3());
		}
		return null;
	}

	/**
	 * Provide feedback strings for the current cursor position. This includes:
	 * <ul>
	 * <li>Numbers of countries and cities loaded.</li>
	 * <li>Current projection name.</li>
	 * <li>Screen and world coordinates.</li>
	 * <li>Latitude and longitude (if on map).</li>
	 * <li>Picked country and city, if any.</li>
	 * </ul>
	 *
	 * @param container       the host container.
	 * @param pp              the screen-space point.
	 * @param wp              the corresponding world-space point.
	 * @param feedbackStrings the list to which feedback strings are appended.
	 */
	@Override
	public void getFeedbackStrings(IContainer container, Point pp, Double wp, List<String> feedbackStrings) {

		boolean onMap = _projection.isPointOnMap(wp);

		String numCountryStr = String.format("Countries loaded: %d", (_countries != null) ? _countries.size() : 0);
		String numCityStr = String.format("Cities loaded: %d", (_cities != null) ? _cities.size() : 0);
		String projStr = String.format("projection %s", _projection.name());
		String screenStr = String.format("screen [%d, %d] ", pp.x, pp.y);
		String worldStr = String.format("world [%6.2f, %6.2f] ", wp.x, wp.y);
		feedbackStrings.add(numCountryStr);
		feedbackStrings.add(numCityStr);
		feedbackStrings.add(projStr);
		feedbackStrings.add(screenStr);
		feedbackStrings.add(worldStr);

		if (onMap) {
			_projection.latLonFromXY(_latLon, wp);
			double dLon = Math.toDegrees(_latLon.x);
			double dLat = Math.toDegrees(_latLon.y);
			String latStr = String.format("%s %.2f%s", _latPrefix, dLat, _deg);
			String lonStr = String.format("%s %.2f%s", _lonPrefix, dLon, _deg);
			feedbackStrings.add(latStr);
			feedbackStrings.add(lonStr);

			// on a country?
			GeoJsonCountryLoader.CountryFeature countryHit = _countryRenderer.pickCountry(pp, container);
			if (countryHit != null) {
				String countryStr = String.format("%s (%s)", countryHit.getAdminName(), countryHit.getIsoA3());
				feedbackStrings.add(countryStr);
			}

			// on a city?
			GeoJsonCityLoader.CityFeature cityHit = _cityRenderer.pickCity(pp, container);

			if (cityHit != null) {
				String cityStr = String.format("%s (pop: %d)", cityHit.getName(), cityHit.getPopulation());
				feedbackStrings.add(cityStr);
			}
		}
	}

	/**
	 * Set the static list of countries to be used by all map views.
	 *
	 * @param countries the list of {@link CountryFeature} loaded from GeoJSON.
	 */
	public static void setCountries(List<CountryFeature> countries) {
		_countries = countries;
	}

	/**
	 * Set the static list of cities to be used by all map views.
	 *
	 * @param cities the list of {@link GeoJsonCityLoader.CityFeature} loaded from
	 *               GeoJSON.
	 */
	public static void setCities(List<GeoJsonCityLoader.CityFeature> cities) {
		_cities = cities;
	}

	/**
	 * Get the country renderer.
	 *
	 * @return the country renderer
	 */
	public CountryRenderer getCountryRenderer() {
		return _countryRenderer;
	}

	/**
	 * Get the city renderer.
	 *
	 * @return the city renderer
	 */
	protected CityPointRenderer getCityRenderer() {
		return _cityRenderer;
	}

	/**
	 * Get the map projection.
	 *
	 * @return the current map projection
	 */
	protected IMapProjection getMapProjection() {
		return _projection;
	}

}
