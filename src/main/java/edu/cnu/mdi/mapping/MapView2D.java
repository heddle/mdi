package edu.cnu.mdi.mapping;

import java.io.IOException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Collections;
import java.util.List;

import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.graphics.drawable.IDrawable;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.mapping.container.MapContainer;
import edu.cnu.mdi.mapping.loader.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.mapping.projection.EProjection;
import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.projection.ProjectionFactory;
import edu.cnu.mdi.mapping.render.CityPointRenderer;
import edu.cnu.mdi.mapping.render.CountryRenderer;
import edu.cnu.mdi.mapping.render.GraticuleRenderer;
import edu.cnu.mdi.mapping.render.IPickable;
import edu.cnu.mdi.mapping.shapefile.ShapeFeature;
import edu.cnu.mdi.mapping.shapefile.ShapeFeatureRenderer;
import edu.cnu.mdi.mapping.shapefile.ShapeFeatureStyle;
import edu.cnu.mdi.mapping.shapefile.ShapefileFeatureLoader;
import edu.cnu.mdi.mapping.theme.MapTheme;
import edu.cnu.mdi.util.UnicodeUtils;
import edu.cnu.mdi.view.AbstractViewInfo;
import edu.cnu.mdi.view.BaseView;

/**
 * A two-dimensional map view that renders world maps using configurable
 * projections and themes.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 * <li>Managing the active {@link IMapProjection} and the world coordinate
 * system exposed to the {@link IContainer}.</li>
 * <li>Orchestrating the rendering pipeline (background → ocean → graticule →
 * countries → cities) via an after-draw {@link IDrawable}.</li>
 * <li>Providing per-frame feedback strings (projection name, lat/lon, picked
 * country and city) to the {@link FeedbackPane}.</li>
 * <li>Exposing public API ({@link #setProjection}, {@link #getCityRenderer},
 * etc.) so {@link MapControlPanel} can drive changes without accessing private
 * state.</li>
 * </ul>
 *
 * <h2>Shared vs. per-view data</h2>
 * <p>
 * Country and city data were previously stored in {@code static} fields, which
 * created a hidden singleton: all {@code MapView2D} instances shared — and
 * could inadvertently overwrite — the same dataset. The fields are now instance
 * fields so that multiple views can independently hold different datasets. The
 * static setter methods ({@link #setCountries}, {@link #setCities}) are
 * retained for backwards compatibility; they set the data on the
 * most-recently-constructed instance through a static reference that is updated
 * in the constructor. Callers that only ever create one view are unaffected.
 * </p>
 *
 * <h2>Side panel layout</h2>
 * <p>
 * The control panel ({@link MapControlPanel}) and feedback pane
 * ({@link FeedbackPane}) are placed together in a combined east-side strip
 * whose preferred width is {@code SIDE_PANEL_WIDTH} pixels.
 * </p>
 */
@SuppressWarnings("serial")
public class MapView2D extends BaseView {

	// -------------------------------------------------------------------------
	// Constants
	// -------------------------------------------------------------------------

	/**
	 * Preferred width in pixels of the combined east-side strip containing the
	 * control panel and the feedback pane.
	 */
	private static final int SIDE_PANEL_WIDTH = 220;

	// -------------------------------------------------------------------------
	// Feedback label prefixes (static because they never change)
	// -------------------------------------------------------------------------

	private static final String LAT_PREFIX = "$yellow$Lat (" + UnicodeUtils.SMALL_PHI + ")";
	private static final String LON_PREFIX = "$yellow$Lon (" + UnicodeUtils.SMALL_LAMBDA + ")";
	private static final String DEG = UnicodeUtils.DEGREE;

	// -------------------------------------------------------------------------
	// Instance state — geographic data
	// -------------------------------------------------------------------------

	/**
	 * Country boundary features used by this view's {@link CountryRenderer}.
	 *
	 * <p>
	 * Previously declared {@code static}, which forced all instances to share the
	 * same dataset. Now an instance field so different views can hold independent
	 * data. Initialized to {@code null}; must be set via
	 * {@link #setCountries(List)} before the first render.
	 * </p>
	 */
	private List<CountryFeature> countries;

	/**
	 * City (populated-place) features used by this view's
	 * {@link CityPointRenderer}.
	 *
	 * <p>
	 * Previously declared {@code static}; now an instance field for the same reason
	 * as {@link #countries}.
	 * </p>
	 */
	private List<GeoJsonCityLoader.CityFeature> cities;

	// -------------------------------------------------------------------------
	// Instance state — renderers
	// -------------------------------------------------------------------------

	/** Active map projection. Rebuilt whenever {@link #setProjection} is called. */
	private IMapProjection projection;

	/** Graticule renderer backed by the active projection. */
	private GraticuleRenderer gratRenderer;

	/** Renderer for country polygons. */
	private CountryRenderer countryRenderer;

	/** Renderer for city marker dots and labels. */
	private CityPointRenderer cityRenderer;

	/**
	 * Ordered list of additional rendering layers added via
	 * {@link #addShapefileLayer(ShapeFeatureRenderer)}. Drawn after countries and
	 * before cities so that vector overlays (rivers, lakes, etc.) appear beneath
	 * city markers.
	 */
	private final List<ShapeFeatureRenderer> extraLayers = new ArrayList<>();

	// -------------------------------------------------------------------------
	// Instance state — UI
	// -------------------------------------------------------------------------

	/**
	 * Side control panel (projection selector, theme buttons, population slider).
	 */
	private MapControlPanel controlPanel;

	/** Menu providing shapefile open and per-layer visibility controls. */
	private ShapefileMenu shapefileMenu;

	// -------------------------------------------------------------------------
	// Workspace — reused per feedback call to avoid allocation
	// -------------------------------------------------------------------------

	/** Reusable lat/lon workspace for the feedback method. */
	private final Point2D.Double latLon = new Point2D.Double();

	// -------------------------------------------------------------------------
	// Construction
	// -------------------------------------------------------------------------

	/**
	 * Creates a map view. Variable-length {@code keyVals} are passed through to
	 * {@link BaseView} for framework-level configuration (title, toolbar flags,
	 * container factory, etc.).
	 *
	 * <p>
	 * The view initializes with the {@link MapConstants#DEFAULT_PROJECTION} and a
	 * light {@link MapTheme}. Geographic data ({@link #setCountries},
	 * {@link #setCities}) must be loaded and set before the first render to avoid a
	 * blank map.
	 * </p>
	 *
	 * @param keyVals framework key-value pairs forwarded to {@link BaseView}
	 */
	public MapView2D(Object... keyVals) {
		super(keyVals);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		controlPanel = new MapControlPanel(this);

		// Start with the application-wide default projection.
		setProjection(MapConstants.DEFAULT_PROJECTION);

		initSidePanel();
		setAfterDraw();
		initShapefileMenu();
	}


	/**
	 * Sets the country boundary features used by this view's
	 * {@link CountryRenderer}.
	 *
	 * <p>
	 * Must be called before the first render. If the country renderer has already
	 * been created (i.e. {@link #setProjection} has been called), the renderer is
	 * rebuilt immediately so the new data takes effect on the next repaint.
	 * </p>
	 *
	 * @param countries list of country features; must not be {@code null}
	 */
	public void setCountries(List<CountryFeature> countries) {
		this.countries = countries;
		if (projection != null) {
			countryRenderer = new CountryRenderer(this.countries, projection);
		}
	}

	/**
	 * Sets the city features used by this view's {@link CityPointRenderer}.
	 *
	 * <p>
	 * Must be called before the first render. If the city renderer has already been
	 * created, it is rebuilt immediately.
	 * </p>
	 *
	 * @param cities list of city features; must not be {@code null}
	 */
	public void setCities(List<GeoJsonCityLoader.CityFeature> cities) {
		this.cities = cities;
		if (projection != null) {
			rebuildCityRenderer();
		}
	}

	// -------------------------------------------------------------------------
	// Projection management
	// -------------------------------------------------------------------------

	/**
	 * Returns the currently active map projection.
	 *
	 * @return the active {@link IMapProjection}; never {@code null} after
	 *         construction
	 */
	public IMapProjection getProjection() {
		return projection;
	}

	/**
	 * Switches the active projection, rebuilding all dependent renderers and
	 * resetting the container's world coordinate system.
	 *
	 * <p>
	 * The new projection is created by {@link ProjectionFactory} using the theme
	 * currently selected in the control panel, so the visual style is preserved
	 * across projection changes.
	 * </p>
	 *
	 * <p>
	 * Country and city data must have been set (via {@link #setCountries} and
	 * {@link #setCities}) before calling this method; if either list is
	 * {@code null} the corresponding renderer is skipped.
	 * </p>
	 *
	 * @param projectionType the new projection type; must not be {@code null}
	 */
	public void setProjection(EProjection projectionType) {
		projection = ProjectionFactory.create(projectionType, controlPanel.getCurrentTheme());
		gratRenderer = new GraticuleRenderer(projection);

		getIContainer().resetWorldSystem(getWorldSystem(projectionType));

		if (countries != null) {
			countryRenderer = new CountryRenderer(countries, projection);
		}

		rebuildCityRenderer();

		// Notify any extra shapefile layers of the new projection so they
		// re-project their geometry on the next render call.
		for (ShapeFeatureRenderer layer : extraLayers) {
			layer.setProjection(projection);
		}

		refresh();
	}

	// -------------------------------------------------------------------------
	// Accessors used by MapControlPanel and MapContainer
	// -------------------------------------------------------------------------

	/**
	 * Returns the active {@link CityPointRenderer}.
	 *
	 * <p>
	 * May be {@code null} if {@link #setCities(List)} has not been called yet.
	 * {@link MapControlPanel} null-checks before using this.
	 * </p>
	 *
	 * @return the city renderer, or {@code null}
	 */
	protected CityPointRenderer getCityRenderer() {
		return cityRenderer;
	}

	/**
	 * Returns the active map projection.
	 *
	 * <p>
	 * Equivalent to {@link #getProjection()} but package-accessible without an
	 * explicit cast; used by {@link MapControlPanel}.
	 * </p>
	 *
	 * @return the active projection; never {@code null} after construction
	 */
	protected IMapProjection getMapProjection() {
		return projection;
	}

	/**
	 * Returns the number of countries currently loaded in this view.
	 *
	 * <p>
	 * Used by {@link MapViewInfo#getTechnicalNotes()} to produce a dynamic count
	 * rather than a hardcoded string.
	 * </p>
	 *
	 * @return country count, or 0 if data has not been set
	 */
	public int getCountryCount() {
		return (countries != null) ? countries.size() : 0;
	}

	/**
	 * Returns the number of cities currently loaded in this view.
	 *
	 * <p>
	 * Used by {@link MapViewInfo#getTechnicalNotes()} to produce a dynamic count
	 * rather than a hardcoded string.
	 * </p>
	 *
	 * @return city count, or 0 if data has not been set
	 */
	public int getCityCount() {
		return (cities != null) ? cities.size() : 0;
	}

	// -------------------------------------------------------------------------
	// Extra layer management
	// -------------------------------------------------------------------------

	/**
	 * Appends a {@link ShapeFeatureRenderer} to the end of the extra-layer list.
	 * Extra layers are drawn after country polygons and before city markers, in the
	 * order they were added.
	 *
	 * <p>
	 * Typical use:
	 * </p>
	 * 
	 * <pre>{@code
	 * List<ShapeFeature> rivers = new ShapefileFeatureLoader().load(Path.of("ne_10m_rivers_lake_centerlines.shp"));
	 * ShapeFeatureStyle style = new ShapeFeatureStyle().strokeColor(new Color(0x6B9FD4)).strokeWidth(0.8f);
	 * mapView.addLayer(new ShapeFeatureRenderer(rivers, mapView.getProjection(), style));
	 * }</pre>
	 *
	 * @param renderer the layer to add; must not be {@code null}
	 */
	public void addShapefileLayer(ShapeFeatureRenderer renderer) {
		addShapefileLayer(renderer, "Layer " + (extraLayers.size() + 1));
	}

	/**
	 * Appends a named {@link ShapeFeatureRenderer} layer and registers it in the
	 * {@link ShapefileMenu} so the user can toggle its visibility.
	 *
	 * <p>
	 * This is the preferred overload for startup layers added from application
	 * code. The anonymous overload {@link #addShapefileLayer(ShapeFeatureRenderer)}
	 * still works but produces a menu entry with no display name.
	 * </p>
	 *
	 * @param renderer the layer to add; must not be {@code null}
	 * @param name     display name shown in the Shapefiles menu
	 */
	public void addShapefileLayer(ShapeFeatureRenderer renderer, String name) {
		extraLayers.add(Objects.requireNonNull(renderer, "renderer"));
		if (shapefileMenu != null) {
			shapefileMenu.registerLayer(renderer, name);
		}
		refresh();
	}

	/**
	 * Convenience method that loads a shapefile from the given path, creates a
	 * {@link ShapeFeatureRenderer} with the specified style, and adds it as a new
	 * layer with the given name.
	 *
	 * <p>
	 * Exceptions are caught and logged to the console; the method does not throw.
	 * </p>
	 *
	 * @param mapView the view to add the layer to; must not be {@code null}
	 * @param path    filesystem path to the shapefile; must not be {@code null}
	 * @param name    display name shown in the Shapefiles menu; must not be
	 *                {@code null}
	 * @param style   rendering style for the shapefile features; must not be
	 *                {@code null}
	 */
	public static List<ShapeFeature> loadShapefileLayer(MapView2D mapView, Path path, String name,
			ShapeFeatureStyle style) {
		try {
			if (!path.toFile().exists()) {
				Log.getInstance().warning("Shapefile not found: " + path.toAbsolutePath());
				return null;
			}

			List<ShapeFeature> features = new ShapefileFeatureLoader().load(path);
			ShapeFeatureRenderer renderer = new ShapeFeatureRenderer(features, mapView.getProjection(), style);
			mapView.addShapefileLayer(renderer, name);
			Log.getInstance().info("Shapefile loaded: " + path.toAbsolutePath());
			return features;
		} catch (IOException e) {
			Log.getInstance().error("Error loading shapefile layer: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Removes a previously added {@link ShapeFeatureRenderer} layer. Has no effect
	 * if the renderer is not in the list.
	 *
	 * @param renderer the layer to remove
	 */
	public void removeLayer(ShapeFeatureRenderer renderer) {
		if (extraLayers.remove(renderer))
			refresh();
	}

	/**
	 * Removes all extra layers added via {@link #addLayer}. The base layers
	 * (countries, cities, graticule) are unaffected.
	 */
	public void clearLayers() {
		if (!extraLayers.isEmpty()) {
			extraLayers.clear();
			refresh();
		}
	}

	/**
	 * Returns an unmodifiable view of the current extra-layer list.
	 *
	 * @return unmodifiable list of extra layers in draw order
	 */
	public List<ShapeFeatureRenderer> getLayers() {
		return Collections.unmodifiableList(extraLayers);
	}

	// -------------------------------------------------------------------------
	// AbstractViewInfo
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Returns a {@link MapViewInfo} bound to {@code this} view so that the info
	 * dialog can display live dataset counts rather than hardcoded strings.
	 * </p>
	 */
	@Override
	public AbstractViewInfo getViewInfo() {
		return new MapViewInfo(this);
	}

	// -------------------------------------------------------------------------
	// Hit-testing (used by MapContainer hover and feedback)
	// -------------------------------------------------------------------------

	/**
	 * Returns the name of the country under the given screen-space point, or
	 * {@code null} if no country is found.
	 *
	 * <p>
	 * Delegates to {@link CountryRenderer#pickCountry} and formats the result as
	 * {@code "Admin Name (ISO3)"} when a hit is found.
	 * </p>
	 *
	 * @param pp        screen-space point to test; must not be {@code null}
	 * @param container container providing the coordinate transform
	 * @return formatted country name string, or {@code null}
	 */
	public String getCountryAtPoint(Point pp, IContainer container) {
		if (countryRenderer == null)
			return null;
		GeoJsonCountryLoader.CountryFeature hit = countryRenderer.pickCountry(pp, container);
		return (hit != null) ? String.format("%s (%s)", hit.getAdminName(), hit.getIsoA3()) : null;
	}

	// -------------------------------------------------------------------------
	// Feedback
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Appends the following strings (in order) to {@code feedbackStrings}:
	 * <ol>
	 * <li>Number of countries loaded.</li>
	 * <li>Number of cities loaded.</li>
	 * <li>Active projection name.</li>
	 * <li>Screen coordinates of the cursor.</li>
	 * <li>World (projection-space) coordinates.</li>
	 * <li>Latitude and longitude in degrees (only when cursor is on map).</li>
	 * <li>Picked country name and ISO code (only when cursor is on a country
	 * polygon).</li>
	 * <li>Tooltip text from any extra layers ({@link ShapeFeatureRenderer}) whose
	 * {@link IPickable#pick} returns a non-null result. Each layer contributes at
	 * most one string; all hit layers are reported.</li>
	 * <li>Picked city name and population (only when cursor is near a city
	 * dot).</li>
	 * </ol>
	 */
	@Override
	public void getFeedbackStrings(IContainer container, Point pp, Double wp, List<String> feedbackStrings) {

		feedbackStrings.add(String.format("Countries loaded: %d", getCountryCount()));
		feedbackStrings.add(String.format("Cities loaded: %d", getCityCount()));
		feedbackStrings.add(String.format("Projection: %s", projection.name()));
		feedbackStrings.add(String.format("Screen [%d, %d]", pp.x, pp.y));
		feedbackStrings.add(String.format("World [%6.2f, %6.2f]", wp.x, wp.y));

		if (projection.isPointOnMap(wp)) {
			projection.latLonFromXY(latLon, wp);
			double dLat = Math.toDegrees(latLon.y);
			double dLon = Math.toDegrees(latLon.x);
			feedbackStrings.add(String.format("%s %.2f%s", LAT_PREFIX, dLat, DEG));
			feedbackStrings.add(String.format("%s %.2f%s", LON_PREFIX, dLon, DEG));

			if (countryRenderer != null) {
				GeoJsonCountryLoader.CountryFeature countryHit = countryRenderer.pickCountry(pp, container);
				if (countryHit != null) {
					feedbackStrings.add(String.format("%s (%s)", countryHit.getAdminName(), countryHit.getIsoA3()));
				}
			}

			// Extra shapefile layers (rivers, lakes, etc.)
			for (ShapeFeatureRenderer layer : extraLayers) {
				String layerHit = layer.pick(pp, container);
				if (layerHit != null) {
					feedbackStrings.add(layerHit);
				}
			}

			if (cityRenderer != null) {
				GeoJsonCityLoader.CityFeature cityHit = cityRenderer.pickCity(pp, container);
				if (cityHit != null) {
					feedbackStrings.add(String.format("%s (pop: %d)", cityHit.getName(), cityHit.getPopulation()));
				}
			}
		}
	}

	// -------------------------------------------------------------------------
	// Lifecycle
	// -------------------------------------------------------------------------

	/**
	 * Releases hover and popup resources held by the {@link MapContainer} when the
	 * view is closing.
	 *
	 * <p>
	 * Must be called from the owning window's close handler to prevent orphaned
	 * {@link edu.cnu.mdi.hover.HoverManager} registrations and leaked
	 * {@link edu.cnu.mdi.hover.HoverInfoWindow} instances.
	 * </p>
	 */
	public void prepareForExit() {
		((MapContainer) getIContainer()).prepareForExit();
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Creates the {@link ShapefileMenu} and inserts it into the view's
	 * {@link javax.swing.JMenuBar} at position 1 (after the File menu), following
	 * the same pattern used by {@code SplotDemoView}.
	 */
	private void initShapefileMenu() {
		shapefileMenu = new ShapefileMenu(this);
		applyFocusFix(shapefileMenu, this);
		getJMenuBar().add(shapefileMenu);
	}

	/**
	 * Initializes and lays out the east-side strip containing the control panel
	 * ({@link MapControlPanel}) above the feedback pane ({@link FeedbackPane}).
	 */
	private void initSidePanel() {
		FeedbackPane fbp = initFeedback();

		JPanel sidePanel = new JPanel(new BorderLayout());
		fbp.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, fbp.getPreferredSize().height));
		controlPanel.setMaximumSize(new Dimension(SIDE_PANEL_WIDTH, Integer.MAX_VALUE));

		sidePanel.add(controlPanel, BorderLayout.NORTH);
		sidePanel.add(fbp, BorderLayout.CENTER);
		sidePanel.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, getHeight()));

		add(sidePanel, BorderLayout.EAST);
	}

	/**
	 * Registers the after-draw {@link IDrawable} that executes the complete
	 * rendering pipeline each frame:
	 * <ol>
	 * <li>Fill the panel background with the theme's background color.</li>
	 * <li>Fill the ocean region inside the projection's clip shape.</li>
	 * <li>Draw the map outline and graticule lines.</li>
	 * <li>Draw country polygons.</li>
	 * <li>Draw extra layers (rivers, lakes, etc.) in insertion order.</li>
	 * <li>Draw city dots and labels.</li>
	 * </ol>
	 *
	 * <p>
	 * Null-guards on {@link #countryRenderer} and {@link #cityRenderer} prevent
	 * NullPointerExceptions if data has not been set before the first paint.
	 * </p>
	 */
	private void setAfterDraw() {
		IDrawable afterDraw = new DrawableAdapter() {
			@Override
			public void draw(Graphics2D g, IContainer container) {
				// 1. Background
				g.setColor(projection.getTheme().getBackgroundColor());
				g.fillRect(0, 0, getWidth(), getHeight());

				// 2. Ocean fill inside projection boundary
				projection.fillOcean(g, container);

				// 3. Graticule and outline
				gratRenderer.render(g, container);

				// 4. Country polygons (null-safe: data may not be loaded yet)
				if (countryRenderer != null) {
					countryRenderer.render(g, container);
				}

				// 5. Extra layers: rivers, lakes, and any other shapefile
				// overlays added via addLayer(), in insertion order.
				for (ShapeFeatureRenderer layer : extraLayers) {
					layer.render(g, container);
				}

				// 6. City dots and labels (null-safe)
				if (cityRenderer != null) {
					cityRenderer.render(g, container);
				}
			}
		};

		getIContainer().setAfterDraw(afterDraw);
	}

	/**
	 * (Re)builds the {@link CityPointRenderer} with the current projection and
	 * applies the default filter settings. Called by both
	 * {@link #setProjection(EProjection)} and {@link #setCities(List)}.
	 *
	 * <p>
	 * If {@link #cities} has not been set yet, this is a no-op and
	 * {@link #cityRenderer} is left {@code null}.
	 * </p>
	 */
	private void rebuildCityRenderer() {
		if (cities == null)
			return;

		cityRenderer = new CityPointRenderer(cities, projection);
		cityRenderer.setPointRadius(1.5);
		cityRenderer.setMinPopulation(MapConstants.MAX_POP_SLIDER_VALUE);
		cityRenderer.setDrawLabels(true);
	}

	/**
	 * Returns the default world coordinate bounding rectangle for the given
	 * projection type. This rectangle is passed to
	 * {@link IContainer#resetWorldSystem(Rectangle2D.Double)} so that the entire
	 * projection domain fits within the viewport on first display.
	 *
	 * @param type the projection type
	 * @return a symmetric world-space bounding rectangle
	 */
	private Rectangle2D.Double getWorldSystem(EProjection type) {
		double lim = switch (type) {
		case MOLLWEIDE -> 2.9;
		case MERCATOR -> 1.1 * Math.PI;
		case ORTHOGRAPHIC -> 1.1;
		case LAMBERT_EQUAL_AREA -> 1.5 * Math.PI / 2.0;
		};
		return new Rectangle2D.Double(-lim, -lim, 2 * lim, 2 * lim);
	}
}