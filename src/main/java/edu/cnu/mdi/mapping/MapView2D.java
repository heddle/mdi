package edu.cnu.mdi.mapping;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.component.EnumComboBox;
import edu.cnu.mdi.component.RangeSlider;
import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.FeedbackControl;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.feedback.IFeedbackProvider;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.graphics.drawable.IDrawable;
import edu.cnu.mdi.graphics.text.UnicodeSupport;
import edu.cnu.mdi.mapping.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.properties.PropertySupport;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.view.BaseView;


/**
 * A two-dimensional map view that renders world maps using different
 * projections and themes. This view is responsible for:
 * <ul>
 *   <li>Managing the current map projection and world coordinate system.</li>
 *   <li>Rendering graticules, country boundaries, and cities.</li>
 *   <li>Providing feedback on the current mouse position and picked
 *       geographic features.</li>
 *   <li>Exposing a side control panel for interactive control of
 *       projection, city display, population threshold, and theme.</li>
 * </ul>
 * <p>
 * The control panel is placed on the east side of the view, above the
 * feedback pane, and uses the default Swing look-and-feel background color.
 * </p>
 */
@SuppressWarnings("serial")
public class MapView2D extends BaseView implements IFeedbackProvider, MouseMotionListener {

	// share country boundaries across all map views
	private static List<CountryFeature> _countries;

	// share cities across all map views
	private static List<GeoJsonCityLoader.CityFeature> _cities;
	
	//whether labels are shown
	private boolean _showNames = true;
	
	//current map theme
	private MapTheme _darkTheme = MapTheme.dark();
	private MapTheme _lightTheme = MapTheme.light();
	private MapTheme _currentTheme = _darkTheme;

	// the map projection
	private IMapProjection _projection;
	private GraticuleRenderer _gratRenderer;

	// feedback pane
	private FeedbackPane _feedbackPane;

	// control panel (projection / city / theme controls)
	private JPanel _controlPanel;

	// country renderer
	private CountryFeatureRenderer _countryRenderer;

	// city renderer
	private CityPointRenderer _cityRenderer;

	// workspace and strings for feedback
	private Point2D.Double _latLon = new Point2D.Double();
	private static String _latPrefix = "$yellow$Lat (" + UnicodeSupport.SMALL_PHI + ")";
	private static String _lonPrefix = "$yellow$Lon (" + UnicodeSupport.SMALL_LAMBDA + ")";
	private static String _deg = UnicodeSupport.DEGREE;

	// UI controls that we may want to reference later
	private RangeSlider _minPopSlider;
	private JCheckBox _showCityNamesCheckBox;
	private JRadioButton _lightThemeButton;
	private JRadioButton _darkThemeButton;

	// default side panel width (control panel + feedback)
	private static final int SIDE_PANEL_WIDTH = 220;

	// max slider value for minimum population
	private static final int MAX_POP_SLIDER_VALUE = 2_000_000;

	/**
	 * Create a map view with the given key-value pairs.
	 *
	 * @param keyVals variable list of arguments used by {@link BaseView}
	 *                to configure the view.
	 */
	public MapView2D(Object... keyVals) {
		super(keyVals);

		// default to Mercator projection
		setProjection(EProjection.MERCATOR);

		// set the feedback and side (control + feedback) UI
		initSidePanel();

		// set the before and after draws
		setBeforeDraw();
		setAfterDraw();

		// listen for mouse motion over the view's container
		getContainer().getComponent().addMouseMotionListener(this);
	}
	

	/**
	 * Initialize the side panel that contains the control panel (on top)
	 * and the feedback pane (on the bottom). The combined side panel is
	 * added to the east side of the view.
	 */
	private void initSidePanel() {
		// feedback control and provider
		FeedbackControl fbc = getContainer().getFeedbackControl();
		fbc.addFeedbackProvider(this);

		_feedbackPane = new FeedbackPane();
		getContainer().setFeedbackPane(_feedbackPane);

		// create the control panel
		_controlPanel = createControlPanel();

		// container panel holding control panel (NORTH) and feedback (CENTER)
		JPanel sidePanel = new JPanel(new BorderLayout());

		// ensure a consistent preferred width for the whole side strip
		Dimension feedbackPref = _feedbackPane.getPreferredSize();
		_feedbackPane.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, feedbackPref.height));
		_controlPanel.setMaximumSize(new Dimension(SIDE_PANEL_WIDTH, Integer.MAX_VALUE));

		sidePanel.add(_controlPanel, BorderLayout.NORTH);
		sidePanel.add(_feedbackPane, BorderLayout.CENTER);
		sidePanel.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, getHeight()));

		add(sidePanel, BorderLayout.EAST);
	}

	/**
	 * Create the control panel placed above the feedback pane on the east side.
	 * <p>
	 * The control panel contains:
	 * </p>
	 * <ul>
	 *   <li>A projection combo box (moved from the toolbar).</li>
	 *   <li>A toggle (checkbox) for showing/hiding city names.</li>
	 *   <li>A slider for minimum city population (0 to 1,000,000).</li>
	 *   <li>A light vs. dark theme radio button group.</li>
	 * </ul>
	 * <p>
	 * Labels use {@link Fonts#mediumFont}. The panel background uses the
	 * default UI manager background (no explicit background is set).
	 * </p>
	 *
	 * @return the newly created control panel.
	 */
	private JPanel createControlPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		// 1. Projection selector
		JLabel projLabel = new JLabel("Projection");
		projLabel.setFont(Fonts.mediumFont);
		EnumComboBox<EProjection> projCombo = createProjectionComboBox();

		panel.add(projLabel);
		panel.add(Box.createVerticalStrut(4));
		panel.add(projCombo);
		panel.add(Box.createVerticalStrut(12));

		// 2. Show city names toggle
		_showCityNamesCheckBox = new JCheckBox("Show city names", true);
		_showCityNamesCheckBox.setFont(Fonts.mediumFont);
		_showCityNamesCheckBox.addActionListener(e -> {
			_showNames = _showCityNamesCheckBox.isSelected();
			updateCityLabelVisibility();
		});
		panel.add(_showCityNamesCheckBox);
		panel.add(Box.createVerticalStrut(12));



		//create the min pop slider
        createMinPopRangeSlider();


		panel.add(_minPopSlider);
		panel.add(Box.createVerticalStrut(12));

		// 4. Theme radio buttons (light / dark)
		JLabel themeLabel = new JLabel("Theme");
		themeLabel.setFont(Fonts.mediumFont);

		_lightThemeButton = new JRadioButton("Light");
		_lightThemeButton.setFont(Fonts.mediumFont);

		_darkThemeButton = new JRadioButton("Dark");
		_darkThemeButton.setFont(Fonts.mediumFont);

		ButtonGroup themeGroup = new ButtonGroup();
		themeGroup.add(_lightThemeButton);
		themeGroup.add(_darkThemeButton);

		boolean isDark = (_currentTheme == _darkTheme);
		_darkThemeButton.setSelected(isDark);
		_lightThemeButton.setSelected(!isDark);

		_lightThemeButton.addActionListener(e -> {
			if (_lightThemeButton.isSelected()) {
				_currentTheme = _lightTheme;
				updateTheme();
			}
		});
		_darkThemeButton.addActionListener(e -> {
			if (_darkThemeButton.isSelected()) {
				_currentTheme = _darkTheme;
				updateTheme();
			}
		});

		panel.add(themeLabel);
		panel.add(Box.createVerticalStrut(4));
		panel.add(_lightThemeButton);
		panel.add(_darkThemeButton);
		panel.add(Box.createVerticalStrut(8));

		return panel;
	}
	
	// Create the minimum population range slider
	private void createMinPopRangeSlider() {
		_minPopSlider = new RangeSlider(0, MAX_POP_SLIDER_VALUE, MAX_POP_SLIDER_VALUE/2, true);
		_minPopSlider.setOnChange(this::updateMinPopulationFilter);
		_minPopSlider.setBorder(new CommonBorder("Minimum Population"));
	}

	/**
	 * Create the projection combo box for selecting an {@link EProjection}.
	 * The combo box is wired so that selecting a projection automatically
	 * updates the view.
	 *
	 * @return a configured {@link EnumComboBox} of {@link EProjection}.
	 */
	private EnumComboBox<EProjection> createProjectionComboBox() {
		EnumComboBox<EProjection> combo = EProjection.createComboBox();

		combo.addActionListener(e -> {
			EProjection selected = combo.getSelectedEnum();
			setProjection(selected);
		});

		return combo;
	}

	/**
	 * Update whether city names (labels) are drawn.
	 *
	 * @param showNames {@code true} to show city names, {@code false} to hide.
	 */
	private void updateCityLabelVisibility() {
		if (_cityRenderer != null) {
			// Adjust to match your CityPointRenderer API
			_cityRenderer.setDrawLabels(_showNames);
			refresh();
		}
	}

	/**
	 * Update the minimum population filter for displayed cities and refresh
	 * the view. The population value is provided directly by the slider.
	 *
	 * @param pop minimum population (inclusive) for city display.
	 */
	private void updateMinPopulationFilter(int pop) {
		if (_cityRenderer != null) {
			long minPop = pop;
			_cityRenderer.setMinPopulation(minPop);
			refresh();
		}
	}

	/**
	 * Format a population value for display in the control panel.
	 *
	 * @param pop the population as an integer.
	 * @return a human-readable formatted string, e.g. "≥ 1,000,000".
	 */
	private String formatPopulationLabel(int pop) {
		return String.format("≥ %,d", pop);
	}

	/**
	 * Update the map theme to light or dark and refresh the display.
	 *
	 */
	private void updateTheme() {
		System.out.println("Updating theme");
		if (_projection != null) {
			_projection.setTheme(_currentTheme);
			refresh();
		}
	}

	/**
	 * Set the view's "before draw" operation. This is currently a no-op,
	 * but is provided for completeness and future extension.
	 */
	private void setBeforeDraw() {
		IDrawable beforeDraw = new DrawableAdapter() {
			@Override
			public void draw(Graphics g, IContainer container) {
				// intentionally empty
			}
		};

		getContainer().setBeforeDraw(beforeDraw);
	}

	/**
	 * Set the view's "after draw" operation, which performs the actual map
	 * rendering steps in the correct order:
	 * <ol>
	 *   <li>Clear the background using the current theme's background color.</li>
	 *   <li>Fill the ocean within the projection's clipping shape.</li>
	 *   <li>Render the graticule and map outline.</li>
	 *   <li>Render country polygons.</li>
	 *   <li>Render cities.</li>
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
			//	_countryRenderer.render(g, container);
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

	
	public void setProjection(EProjection projection) {

		_projection = ProjectionFactory.create(projection, _currentTheme);
		_gratRenderer = new GraticuleRenderer(_projection);

		getContainer().resetWorldSystem(getWorldSystem(_projection.getProjection()));

		_countryRenderer = new CountryFeatureRenderer(_countries, _projection);
		_countryRenderer.invalidateCache();

	    _cityRenderer = new CityPointRenderer(_cities, _projection);
	    _cityRenderer.setPointRadius(1.5);
	    _cityRenderer.setMinPopulation(MAX_POP_SLIDER_VALUE);
	    _cityRenderer.setDrawLabels(true);

		refresh();

	}



	@Override
	public void mouseDragged(MouseEvent e) {
		// no-op
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// no-op (feedback is computed via getFeedbackStrings)
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
			xLim = 1.5*Math.PI / 2;
			yLim = xLim;
			break;
		default:
			xLim = 1.1;
			yLim = xLim;
		}
		return new Rectangle2D.Double(-xLim, -yLim, 2 * xLim, 2 * yLim);
	}

	/**
	 * Provide feedback strings for the current cursor position. This includes:
	 * <ul>
	 *   <li>Numbers of countries and cities loaded.</li>
	 *   <li>Current projection name.</li>
	 *   <li>Screen and world coordinates.</li>
	 *   <li>Latitude and longitude (if on map).</li>
	 *   <li>Picked country and city, if any.</li>
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
				String countryStr = String.format("%s (%s)", countryHit.getAdminName(),
						countryHit.getIsoA3());
				feedbackStrings.add(countryStr);
			}

			// on a city?
			GeoJsonCityLoader.CityFeature cityHit = _cityRenderer.pickCity(pp, container);

			if (cityHit != null) {
				String cityStr = String.format("%s (pop: %d)", cityHit.getName(),
						cityHit.getPopulation());
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
	 * @param cities the list of {@link GeoJsonCityLoader.CityFeature} loaded
	 *               from GeoJSON.
	 */
	public static void setCities(List<GeoJsonCityLoader.CityFeature> cities) {
		_cities = cities;
	}
		

}
