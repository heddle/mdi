package edu.cnu.mdi.mapping;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.JMenuBar;
import javax.swing.JPanel;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.graphics.SymbolDraw;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.graphics.drawable.IDrawable;
import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.hover.HoverEvent;
import edu.cnu.mdi.hover.HoverInfoWindow;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.mapping.container.MapContainer;
import edu.cnu.mdi.mapping.layer.CityLayer;
import edu.cnu.mdi.mapping.layer.CountryBoundaryLayer;
import edu.cnu.mdi.mapping.layer.CountryFillLayer;
import edu.cnu.mdi.mapping.layer.Etopo5Layer;
import edu.cnu.mdi.mapping.layer.GraticuleLayer;
import edu.cnu.mdi.mapping.layer.ShapefileLayer;
import edu.cnu.mdi.mapping.loader.Etopo5Loader;
import edu.cnu.mdi.mapping.loader.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.mapping.projection.EProjection;
import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.projection.ProjectionFactory;
import edu.cnu.mdi.mapping.render.CityPointRenderer;
import edu.cnu.mdi.mapping.render.CountryRenderer;
import edu.cnu.mdi.mapping.render.Etopo5Renderer;
import edu.cnu.mdi.mapping.render.GraticuleRenderer;
import edu.cnu.mdi.mapping.render.IPickable;
import edu.cnu.mdi.mapping.shapefile.ShapeFeature;
import edu.cnu.mdi.mapping.shapefile.ShapeFeatureRenderer;
import edu.cnu.mdi.mapping.shapefile.ShapeFeatureStyle;
import edu.cnu.mdi.mapping.shapefile.ShapefileFeatureLoader;
import edu.cnu.mdi.mapping.shapefile.ShapefileMenu;
import edu.cnu.mdi.mapping.theme.MapTheme;
import edu.cnu.mdi.mapping.util.GeoUtils;
import edu.cnu.mdi.mapping.util.UTMCoordinate;
import edu.cnu.mdi.swing.SwingSizingUtils;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.util.UnicodeUtils;
import edu.cnu.mdi.view.AbstractViewInfo;
import edu.cnu.mdi.view.BaseView;
import edu.cnu.mdi.view.ContainerFactory;

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
 * whose preferred width in pixels is controlled by {@link #getSidePanelWidth()}.
 * </p>
 */
@SuppressWarnings("serial")
public class MapView2D extends BaseView {

	// -------------------------------------------------------------------------
	// Constants
	// -------------------------------------------------------------------------

	/**
	 * Default preferred width in pixels of the combined east-side strip containing
	 * the control panel and the feedback pane.
	 */
	private static final int DEFAULT_SIDE_PANEL_WIDTH = 230;

	// -------------------------------------------------------------------------
	// Feedback label prefixes (static because they never change)
	// -------------------------------------------------------------------------

	/** Feedback-pane prefix used for latitude values. */
	protected static final String LAT_PREFIX = "$yellow$Lat (" + UnicodeUtils.SMALL_PHI + ")";

	/** Feedback-pane prefix used for longitude values. */
	protected static final String LON_PREFIX = "$yellow$Lon (" + UnicodeUtils.SMALL_LAMBDA + ")";

	/** Unicode degree symbol used by geographic feedback text. */
	protected static final String DEG = UnicodeUtils.DEGREE;

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

	/** MDI layer that renders country polygon interiors. */
	private CountryFillLayer countryFillLayer;

	/** MDI layer that renders country political boundaries. */
	private CountryBoundaryLayer countryBoundaryLayer;

	/** MDI layer that renders the loaded GeoJSON city features. */
	private CityLayer cityLayer;

	// -------------------------------------------------------------------------
	// Instance state — renderers
	// -------------------------------------------------------------------------

	/** Active map projection. Rebuilt whenever {@link #setProjection} is called. */
	private IMapProjection projection;

	/** Graticule renderer backed by the active projection. */
	private GraticuleRenderer gratRenderer;

	/** Renderer for optional ETOPO5 terrain and bathymetry. */
	private Etopo5Renderer etopo5Renderer;

	/** MDI layer that hosts the ETOPO5 renderer. */
	private Layer etopo5Layer;

	/**
	 * Persisted graticule "adaptive spacing" preference. Stored on the view
	 * (not just the renderer) because {@link #setProjection} rebuilds the
	 * renderer; the new instance is initialized from this value so the user's
	 * choice survives a projection switch. Defaults to {@code true}.
	 */
	private boolean graticuleAdaptive = true;

	/** MDI layer that renders the standard map graticule. */
	private GraticuleLayer graticuleLayer;

	/**
	 * Persisted graticule "edge labels" preference. See
	 * {@link #graticuleAdaptive} for why this lives on the view. Defaults to
	 * {@code true}.
	 */
	private boolean graticuleLabels = true;

	/** Renderer for country polygons. */
	private CountryRenderer countryRenderer;

	/** Renderer for city marker dots and labels. */
	private CityPointRenderer cityRenderer;

	// Menu bar
	private JMenuBar menuBar;

	/**
	 * Ordered list of additional rendering layers added via
	 * {@link #addShapefile(ShapeFeatureRenderer)}. Drawn after countries and
	 * before cities so that vector overlays (rivers, lakes, etc.) appear beneath
	 * city markers.
	 */
	private final List<ShapefileLayer> extraLayers =
	        new ArrayList<>();
	
	// -------------------------------------------------------------------------
	// Instance state — UI
	// -------------------------------------------------------------------------

	/**
	 * Default side control panel. May be {@code null} if a subclass replaces the
	 * control panel with a custom component.
	 */
	protected MapControlPanel controlPanel;

	/**
	 * The component currently occupying the top control-panel position in the
	 * side strip.
	 */
	private JPanel sidePanel;

	/**
	 * Stack used for the control panel and custom application panels above the
	 * feedback pane.
	 */
	private JPanel sideTopStack;

	/**
	 * Host panel for application-supplied side-panel components.
	 */
	private JPanel customSidePanelHost;

	/** Menu providing shapefile open and per-layer visibility controls. */
	private ShapefileMenu shapefileMenu;

	/** Whether delayed mouse-over feedback windows are enabled. Default false. */
	private boolean hoveringEnabled;

	// -------------------------------------------------------------------------
	// Workspace — reused per feedback call to avoid allocation
	// -------------------------------------------------------------------------

	/** Reusable lat/lon workspace for the feedback method. */
	protected final Point2D.Double latLon = new Point2D.Double();

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
		super(withMapContainerFactory(keyVals));

		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		controlPanel = new MapControlPanel(this);

		setProjection(MapConstants.DEFAULT_PROJECTION);

		/*
		 * Create built-in map layers in their default bottom-to-top order.
		 *
		 * Country Fill lies beneath ETOPO5 so opaque land coloring does not hide
		 * terrain. Country Boundaries lie above ETOPO5 and graticules so political
		 * borders remain crisp.
		 */
		createCountryFillLayer();
		createEtopo5Layer();
		createGraticuleLayer();
		createCountryBoundaryLayer();

		initSidePanel();
		setViewBeforeDraw();
		initShapefileMenu();
	}

	/**
	 * Creates the standard country-fill layer.
	 *
	 * <p>
	 * The layer is created before country data are loaded so its position in the
	 * built-in map stack is deterministic. Until a country renderer exists, the
	 * layer draws nothing.
	 * </p>
	 */
	private void createCountryFillLayer() {
	    if (countryFillLayer != null) {
	        return;
	    }

	    countryFillLayer = new CountryFillLayer(
	            getIContainer(),
	            "Country Fill",
	            this::getCountryRenderer);
	    countryFillLayer.setEditable(true);
	    countryFillLayer.setLocked(true);
	}

	/**
	 * Returns the current country renderer.
	 *
	 * @return current renderer, or {@code null} before country data are loaded
	 */
	protected CountryRenderer getCountryRenderer() {
	    return countryRenderer;
	}

	/**
	 * Creates the standard country-boundary layer.
	 *
	 * <p>
	 * Boundaries are rendered independently from country fills so they may remain
	 * above terrain and other basemap layers.
	 * </p>
	 */
	private void createCountryBoundaryLayer() {
	    if (countryBoundaryLayer != null) {
	        return;
	    }

	    countryBoundaryLayer = new CountryBoundaryLayer(
	            getIContainer(),
	            "Country Boundaries",
	            this::getCountryRenderer);

	    countryBoundaryLayer.setEditable(true);
	    countryBoundaryLayer.setLocked(true);
	}

	/**
	 * Creates the standard ETOPO5 layer in its default position in the map layer
	 * stack.
	 *
	 * <p>
	 * The layer is created before an ETOPO5 renderer is installed so its draw order
	 * is deterministic. Until a renderer is supplied through
	 * {@link #installEtopo5Layer(Etopo5Loader, boolean)}, the layer remains hidden
	 * and draws nothing.
	 * </p>
	 */
	private void createEtopo5Layer() {
		if (etopo5Layer != null) {
			return;
		}

		etopo5Layer = new Etopo5Layer(
		        getIContainer(),
		        "ETOPO5",
		        this::getEtopo5Renderer,
		        this::getProjection);
		etopo5Layer.setEditable(true);
		etopo5Layer.setLocked(true);
		etopo5Layer.setVisible(false);
	}

	/**
	 * Rebuilds the country renderer for the current country data and projection
	 * while preserving user-selected style overrides.
	 */
	private void rebuildCountryRenderer() {
	    if (countries == null || projection == null) {
	        countryRenderer = null;
	        return;
	    }

	    CountryRenderer previous = countryRenderer;

	    CountryRenderer replacement =
	            new CountryRenderer(
	                    countries,
	                    projection);

	    if (previous != null) {
	        replacement.copyStyleFrom(previous);
	    }

	    countryRenderer = replacement;
	}
	/**
	 * Ensures that a {@link MapView2D} is created with a {@link MapContainer}
	 * unless the caller has explicitly supplied a container, container class, or
	 * container factory.
	 *
	 * <p>
	 * This helper exists because {@link BaseView} creates the container during
	 * superclass construction. Therefore the map-container choice must be inserted
	 * into the key/value list before {@code super(...)} is called.
	 * </p>
	 *
	 * @param keyVals original key/value arguments
	 * @return key/value arguments with a map container factory added if needed
	 */
	private static Object[] withMapContainerFactory(Object... keyVals) {
	    if (keyVals == null || keyVals.length == 0) {
	        return new Object[] {
	                PropertyUtils.CONTAINERFACTORY,
	                (ContainerFactory) MapContainer::new
	        };
	    }

	    if ((keyVals.length % 2) != 0) {
	        throw new IllegalArgumentException("Key/value arguments must come in pairs.");
	    }

	    for (int i = 0; i < keyVals.length; i += 2) {
	        Object key = keyVals[i];

	        if (PropertyUtils.CONTAINER.equals(key)
	                || PropertyUtils.CONTAINERCLASS.equals(key)
	                || PropertyUtils.CONTAINERFACTORY.equals(key)) {
	            return keyVals;
	        }
	    }

	    Object[] augmented = Arrays.copyOf(keyVals, keyVals.length + 2);
	    augmented[keyVals.length] = PropertyUtils.CONTAINERFACTORY;
	    augmented[keyVals.length + 1] = (ContainerFactory) MapContainer::new;

	    return augmented;
	}

	/**
	 * Creates the standard MDI layer used to render map graticules.
	 *
	 * <p>
	 * The layer delegates its background drawing to the current
	 * {@link GraticuleRenderer}. Because the renderer field is read on each
	 * draw, changing projections may replace the renderer without requiring
	 * the layer itself to be recreated.
	 * </p>
	 *
	 * <p>
	 * The layer is locked because graticules contain no interactive items.
	 * It remains visible and reorderable through the Layer Inspector.
	 * </p>
	 */
	private void createGraticuleLayer() {
	    if (graticuleLayer != null) {
	        return;
	    }

	    graticuleLayer = new GraticuleLayer(
	            getIContainer(),
	            "Graticules",
	            this::getGraticuleRenderer,
	            this::useStandardGraticules);

	    graticuleLayer.setEditable(true);
	    graticuleLayer.setLocked(true);
	}

	/**
	 * Installs ETOPO5 terrain and bathymetry into the standard ETOPO5 layer.
	 *
	 * @param loader         loaded ETOPO5 data
	 * @param initiallyShown initial layer visibility
	 * @return the ETOPO5 layer
	 */
	protected Layer installEtopo5Layer(
	        Etopo5Loader loader,
	        boolean initiallyShown) {

	    Objects.requireNonNull(loader, "loader");

	    /*
	     * Defensive in case this method is used in an unusual construction path.
	     */
	    createEtopo5Layer();

	    etopo5Renderer = new Etopo5Renderer(loader);
	    etopo5Layer.setVisible(initiallyShown);

	    refresh();
	    return etopo5Layer;
	}

	/**
	 * Returns whether the standard graticule layer should be included in this view.
	 *
	 * <p>
	 * Subclasses that don't want the standard graticule can override this to return
	 * {@code false} and then provide their own graticule implementation as an extra
	 * layer.
	 * </p>
	 *
	 * @return {@code true} to include the standard graticule, {@code false} to
	 *         omit it
	 */
	protected boolean useStandardGraticules() {
	    return true;
	}

	/**
	 * Gets the preferred width of the east-side strip containing the map control
	 * panel, custom application panels, and feedback pane.
	 *
	 * <p>
	 * Subclasses may override this to request a wider or narrower side panel. The
	 * override should not depend on subclass instance fields, because this method is
	 * called during {@link MapView2D} construction before the subclass constructor
	 * body has run. Returning a literal or subclass static constant is safe.
	 * </p>
	 *
	 * @return preferred side-panel width in pixels
	 */
	protected int getSidePanelWidth() {
	    return DEFAULT_SIDE_PANEL_WIDTH;
	}


	/**
	 * Returns interpolated elevation or bathymetry at a geographic location.
	 *
	 * <p>
	 * Elevation remains available even when the ETOPO5 layer is hidden. Layer
	 * visibility is a presentation choice and should not affect calculations such
	 * as radar line-of-sight or land-height feedback.
	 * </p>
	 *
	 * @param lat latitude in degrees
	 * @param lon longitude in degrees
	 * @return elevation in metres, or {@code NaN} if ETOPO5 is not installed
	 *         or either coordinate is non-finite
	 */
	public double getElevation(double lat, double lon) {
	    return (etopo5Renderer == null
	            || !Double.isFinite(lat)
	            || !Double.isFinite(lon))
	            ? Double.NaN
	            : etopo5Renderer.getElevation(lat, lon);
	}

	/**
	 * Sets the country features rendered by the standard country-fill and
	 * country-boundary layers.
	 *
	 * @param countries country features; must not be {@code null}
	 */
	public void setCountries(
	        List<CountryFeature> countries) {

	    this.countries =
	            Objects.requireNonNull(
	                    countries,
	                    "countries");

	    rebuildCountryRenderer();
	    refresh();
	}

	/**
	 * Sets the city features used by this view.
	 *
	 * <p>
	 * The features are rendered by the standard Cities MDI layer. If the
	 * active projection already exists, the city renderer is rebuilt
	 * immediately.
	 * </p>
	 *
	 * @param cities list of city features; must not be {@code null}
	 */
	public void setCities(
	        List<GeoJsonCityLoader.CityFeature> cities) {

	    this.cities = Objects.requireNonNull(
	            cities, "cities");

	    if (projection != null) {
	        rebuildCityRenderer();
	    }

	    ensureCityLayer();
	    refresh();
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
	 * Switches the active projection to one of MDI's built-in projection
	 * types, rebuilding all dependent renderers and resetting the container's
	 * world coordinate system.
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
	 * <p>
	 * This method preserves the existing MDI enum-based projection API. For
	 * application-supplied projections, use {@link #setProjection(IMapProjection)}.
	 * </p>
	 *
	 * @param projectionType the new built-in projection type; must not be
	 *        {@code null}
	 */
	public void setProjection(EProjection projectionType) {
	    IMapProjection builtInProjection = ProjectionFactory.create(
	            projectionType, getCurrentMapTheme());
	    setProjection(builtInProjection);
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
	 * Returns the active graticule renderer.
	 *
	 * <p>
	 * Used by {@link MapControlPanel} to toggle adaptive spacing and edge
	 * labels. The instance is replaced whenever the projection changes (see
	 * {@link #setProjection}), so callers should re-read it rather than caching
	 * the reference; the view re-applies the persisted toggle state to each
	 * newly built renderer, so the user's choices survive a projection switch.
	 * </p>
	 *
	 * @return the graticule renderer; never {@code null} after construction
	 */
	protected GraticuleRenderer getGraticuleRenderer() {
		return gratRenderer;
	}

	/**
	 * Enables or disables zoom-adaptive graticule spacing, persisting the
	 * choice across projection switches and refreshing the view.
	 *
	 * @param adaptive {@code true} for adaptive spacing; {@code false} for the
	 *                 fixed step
	 */
	protected void setGraticuleAdaptive(boolean adaptive) {
		graticuleAdaptive = adaptive;
		if (gratRenderer != null) {
			gratRenderer.setAdaptive(adaptive);
			refresh();
		}
	}

	/**
	 * Enables or disables graticule edge labels, persisting the choice across
	 * projection switches and refreshing the view.
	 *
	 * @param labels {@code true} to draw coordinate labels along the viewport
	 *               edges
	 */
	protected void setGraticuleLabels(boolean labels) {
		graticuleLabels = labels;
		if (gratRenderer != null) {
			gratRenderer.setDrawLabels(labels);
			refresh();
		}
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
	public void addShapefile(ShapeFeatureRenderer renderer) {
		addShapefile(renderer, "Layer " + (extraLayers.size() + 1));
	}

	/**
	 * Adds a named shapefile renderer as an editable MDI layer.
	 *
	 * @param renderer shapefile renderer
	 * @param name     layer display name
	 */
	public void addShapefile(
	        ShapeFeatureRenderer renderer,
	        String name) {

	    Objects.requireNonNull(renderer, "renderer");

	    ShapefileLayer layer =
	            new ShapefileLayer(
	                    getIContainer(),
	                    name,
	                    renderer);

	    extraLayers.add(layer);
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
			mapView.addShapefile(renderer, name);
			Log.getInstance().info("Shapefile loaded: " + path.toAbsolutePath());
			return features;
		} catch (IOException e) {
			Log.getInstance().error("Error loading shapefile layer: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Removes the shapefile layer hosting the supplied renderer.
	 *
	 * @param renderer renderer whose layer should be removed
	 */
	public void removeLayer(
	        ShapeFeatureRenderer renderer) {

	    if (renderer == null) {
	        return;
	    }

	    ShapefileLayer match = null;

	    for (ShapefileLayer layer : extraLayers) {
	        if (layer.getRenderer() == renderer) {
	            match = layer;
	            break;
	        }
	    }

	    if (match != null) {
	        extraLayers.remove(match);

	        /*
	         * Use the container's normal layer-removal operation here if removal
	         * is not already performed by the Layer Inspector.
	         */
	        refresh();
	    }
	}
	
	/**
	 * Removes all extra layers added via addLayer. The base layers
	 * (countries, cities, graticule) are unaffected.
	 */
	public void clearLayers() {
		if (!extraLayers.isEmpty()) {
			extraLayers.clear();
			refresh();
		}
	}

	/**
	 * Draws a map symbol at the given latitude and longitude using the active
	 * projection to convert to screen coordinates.
	 *
	 * <p>
	 * Used by {@link MapControlPanel} to draw the projection center marker. Can also
	 * be used by application code to draw symbols on top of the map without
	 * managing coordinate transforms.
	 * </p>
	 *
	 * @param g         graphics context to draw on; must not be {@code null}
	 * @param lat       latitude in degrees; must be between -90 and 90
	 * @param lon       longitude in degrees; must be between -180 and 180
	 * @param type      symbol type; must not be {@code null}
	 * @param size      symbol size in pixels; must be positive
	 * @param lineColor color for symbol outlines and lines; must not be {@code null}
	 * @param fillColor color for symbol fills; must not be {@code null}
	 */
	public void drawSymbol(Graphics2D g, double lat, double lon, SymbolType type, int size,
			Color lineColor, Color fillColor) {
		MapContainer container = (MapContainer) getIContainer();
		Point2D.Double ll = new Point2D.Double(
				Math.toRadians(lon),
				Math.toRadians(lat));
		Point2D.Double xy = new Point2D.Double();
		projection.latLonToXY(ll, xy);
		if (!projection.isPointOnMap(xy)) {
			return;
		}
		Point pp = new Point();
		container.worldToLocal(pp, xy);
		SymbolDraw.drawSymbol(g, pp.x, pp.y, type, size, lineColor, fillColor);
	}

	/**
	 * Returns the imported shapefile layers.
	 *
	 * @return unmodifiable layer list
	 */
	public List<ShapefileLayer> getLayers() {
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
	public String getCountryAtPoint(
	        Point pp,
	        IContainer container) {

	    boolean countryDisplayVisible =
	            (countryFillLayer != null
	                    && countryFillLayer.isVisible())
	            || (countryBoundaryLayer != null
	                    && countryBoundaryLayer.isVisible());

	    if (countryRenderer == null || !countryDisplayVisible) {
	        return null;
	    }

	    CountryFeature hit =
	            countryRenderer.pickCountry(pp, container);

	    return (hit != null)
	            ? String.format(
	                    "%s (%s)",
	                    hit.getAdminName(),
	                    hit.getIsoA3())
	            : null;
	}

	/**
	 * Appends country and shapefile hit-test feedback for a map location.
	 * This is the common source used by both the feedback pane and hover popup.
	 */
	private void appendMapFeatureFeedback(Point point, IContainer sourceContainer,
	        List<String> destination) {
	    String countryName = getCountryAtPoint(point, sourceContainer);
	    if (countryName != null) {
	        destination.add(countryName);
	    }

	    for (ShapefileLayer layer : extraLayers) {
	        String layerHit = layer.pick(point, sourceContainer);
	        if (layerHit != null) {
	            destination.add(layerHit);
	        }
	    }
	}

	/**
	 * Returns whether the given screen-space point is on land (inside a country
	 * polygon).
	 *
	 * <p>
	 * Delegates to {@link CountryRenderer#pickCountry} and returns {@code true} if
	 * a hit is found.
	 * </p>
	 *
	 * @param pp        screen-space point to test; must not be {@code null}
	 * @param container container providing the coordinate transform
	 * @return {@code true} if the point is on land, {@code false} otherwise
	 */
	public boolean onLand(Point pp, IContainer container) {
		GeoJsonCountryLoader.CountryFeature hit = countryRenderer.pickCountry(pp, container);
		return (hit != null);
	}

	/**
	 * Converts a screen-space point to latitude and longitude in degrees.
	 *
	 * <p>
	 * Delegates to {@link MapContainer#localToLatLon} for the actual conversion.
	 * </p>
	 *
	 * @param pp     screen-space point to convert; must not be {@code null}
	 * @param latLon output parameter to receive the latitude and longitude in
	 *               degrees; must not be {@code null}
	 */
	public void localToLatLonDeg(Point pp, Point2D.Double latLon) {
		MapContainer container = (MapContainer) getIContainer();
		container.localToLatLon(pp, latLon);
		latLon.x = Math.toDegrees(latLon.x);
		latLon.y = Math.toDegrees(latLon.y);
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
	 * <li>Styled feedback text from any extra layers ({@link ShapeFeatureRenderer}) whose
	 * {@link IPickable#pick} returns a non-null result. Each layer contributes at
	 * most one string; all hit layers are reported.</li>
	 * <li>Picked city name and population (only when cursor is near a city
	 * dot).</li>
	 * </ol>
	 */
	@Override
	public void getFeedbackStrings(IContainer container, Point pp,
			Point2D.Double wp, List<String> feedbackStrings) {

		feedbackStrings.add(String.format("Countries loaded: %d", getCountryCount()));
		feedbackStrings.add(String.format("Cities loaded: %d", getCityCount()));
		feedbackStrings.add(String.format("Projection: %s", projection.name()));
		feedbackStrings.add(String.format("Screen [%d, %d]", pp.x, pp.y));
		feedbackStrings.add(String.format("World [%.2f, %.2f]", wp.x, wp.y));


		if (projection.isPointOnMap(wp)) {
			projection.latLonFromXY(latLon, wp);
			if (!Double.isFinite(latLon.x) || !Double.isFinite(latLon.y)) {
				return;
			}
			double dLat = Math.toDegrees(latLon.y);
			double dLon = Math.toDegrees(latLon.x);
			feedbackStrings.add(String.format("%s %.2f%s", LAT_PREFIX, dLat, DEG));
			feedbackStrings.add(String.format("%s %.2f%s", LON_PREFIX, dLon, DEG));
		    UTMCoordinate utm = GeoUtils.fromDecimalDegrees(dLat, dLon);
		    feedbackStrings.add("UTM " + utm.toString());

		    double elevation = getElevation(dLat, dLon);
		    if (!Double.isNaN(elevation)) {
		        boolean onLand = onLand(pp, getIContainer());
		        String eStr = MapContainer.formatElevationStatus(elevation, onLand);
		        feedbackStrings.add(eStr);
		    }


		    appendMapFeatureFeedback(pp, container, feedbackStrings);
		    
			if (cityRenderer != null
			        && cityLayer != null
			        && cityLayer.isVisible()) {
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
	 * Releases resources held by the {@link MapContainer} when the
	 * view is closing.
	 */
	@Override
	public void prepareForExit() {
	    IContainer container = getIContainer();

	    if (container instanceof MapContainer mapContainer) {
	        mapContainer.prepareForExit();
	    }
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
		if (!includeShapeFileMenu()) {
			return; // Subclass has opted out of shapefile functionality
		}
		shapefileMenu = new ShapefileMenu(this);
		applyFocusFix(shapefileMenu, this);
		getJMenuBar().add(shapefileMenu);
	}

	/**
	 * Initializes and lays out the east-side strip containing the map control
	 * panel, optional application-supplied panels, and the feedback pane.
	 */
	private void initSidePanel() {
		int fontSize = PropertyUtils.getFeedbackFontSize(properties);
	    FeedbackPane fbp = initFeedback(Color.cyan, Color.black, fontSize);

	    sidePanel = new JPanel(new BorderLayout());
	    sideTopStack = new JPanel();
	    sideTopStack.setLayout(new javax.swing.BoxLayout(sideTopStack, javax.swing.BoxLayout.Y_AXIS));

	    customSidePanelHost = new JPanel();
	    customSidePanelHost.setLayout(new javax.swing.BoxLayout(customSidePanelHost, javax.swing.BoxLayout.Y_AXIS));

	    int sidePanelWidth = SwingSizingUtils.preferredWidth(getSidePanelWidth(), controlPanel);
	    fbp.setPreferredSize(new Dimension(sidePanelWidth, fbp.getPreferredSize().height));

	    if (controlPanel != null) {
	        controlPanel.setMaximumSize(new Dimension(sidePanelWidth, Integer.MAX_VALUE));
	        sideTopStack.add(controlPanel);
	    }

	    sideTopStack.add(customSidePanelHost);

	    sidePanel.add(sideTopStack, BorderLayout.NORTH);
	    sidePanel.add(fbp, BorderLayout.CENTER);
	    sidePanel.setPreferredSize(new Dimension(sidePanelWidth, getHeight()));

	    add(sidePanel, BorderLayout.EAST);
	}


	/**
	 * Registers the view-level map background renderer.
	 *
	 * <p>
	 * Feature data such as countries and cities are rendered by ordinary MDI
	 * layers. This view-level renderer is now limited to content that belongs
	 * beneath the layer stack.
	 * </p>
	 */
	private void setViewBeforeDraw() {
	    IDrawable beforeDraw = new DrawableAdapter() {

	        @Override
	        public void draw(
	                Graphics2D g,
	                IContainer container) {

	            // 1. Background outside the projection.
	            g.setColor(
	                    projection.getTheme().getBackgroundColor());

	            g.fillRect(
	                    0, 0,
	                    getWidth(), getHeight());

	            // 2. Ocean inside the projection boundary.
	            projection.fillOcean(g, container);

	            // 3. Application-supplied basemap content.
	            drawCustomMapContent(g, container);

	        }
	    };

	    setBeforeDraw(beforeDraw);
	}


	/**
	 * Returns whether the Shapefile menu should be included in this view's menu
	 * bar. Subclasses that don't support shapefile layers can override this to
	 * return {@code false} to exclude the menu and its associated functionality.
	 *
	 * @return {@code true} to include the Shapefile menu, {@code false} to omit it
	 */
	protected boolean includeShapeFileMenu() { return true; }

	/**
	 * Placeholder method for subclasses to override and draw custom map content
	 * after the standard layers. Not called by default since most views won't need
	 * it, but available for extensibility when needed.
	 *
	 * <p>
	 * This is not registered as an after-draw {@link IDrawable} since that would
	 * require a separate registration and coordinate transform for each subclass;
	 * instead, subclasses can call this method directly from their own after-draw
	 * implementation after calling {@code super.drawCustomMapContent(g2)}.
	 * </p>
	 *
	 * @param g2 the Graphics2D context to draw on
	 */
	protected void drawCustomMapContent(Graphics2D g2, IContainer container) {
		//no-op by default; intended for subclasses to override and draw custom content
	}

	/**
	 * Rebuilds the graticule renderer for the active projection while preserving
	 * its user-selected settings.
	 */
	private void rebuildGraticuleRenderer() {
	    if (projection == null) {
	        gratRenderer = null;
	        return;
	    }

	    GraticuleRenderer previous =
	            gratRenderer;

	    GraticuleRenderer replacement =
	            new GraticuleRenderer(projection);

	    if (previous != null) {
	        replacement.copyStyleFrom(previous);
	    }
	    else {
	        replacement.setAdaptive(
	                graticuleAdaptive);

	        replacement.setDrawLabels(
	                graticuleLabels);
	    }

	    gratRenderer = replacement;
	}

	/**
	 * Rebuilds the city renderer for the current city data and projection while
	 * preserving user-selected rendering settings.
	 */
	private void rebuildCityRenderer() {
	    if (cities == null || projection == null) {
	        cityRenderer = null;
	        return;
	    }

	    CityPointRenderer previous =
	            cityRenderer;

	    CityPointRenderer replacement =
	            new CityPointRenderer(
	                    cities,
	                    projection);

	    if (previous != null) {
	        replacement.copyStyleFrom(previous);
	    }
	    else {
	        replacement.setPointRadius(1.5);
	        replacement.setMinPopulation(
	                MapConstants.MIN_POP_DEFAULT);
	        replacement.setDrawLabels(true);
	    }

	    cityRenderer = replacement;
	}
	/**
	 * Returns a world coordinate rectangle suitable for displaying the supplied
	 * projection.
	 *
	 * <p>
	 * For both built-in and custom projections, this uses the projection's own
	 * {@link IMapProjection#getXYBounds()} and adds a small margin. This avoids
	 * requiring {@link MapView2D} to know about every possible application-defined
	 * projection.
	 * </p>
	 *
	 * @param mapProjection the projection
	 * @return the initial world rectangle
	 */
	private Rectangle2D.Double getWorldSystem(IMapProjection mapProjection) {
	    Rectangle2D.Double bounds = mapProjection.getXYBounds();

	    double marginX = 0.05 * bounds.width;
	    double marginY = 0.05 * bounds.height;

	    return new Rectangle2D.Double(
	            bounds.x - marginX,
	            bounds.y - marginY,
	            bounds.width + 2.0 * marginX,
	            bounds.height + 2.0 * marginY);
	}

	/**
	 * Returns the map theme that should be used when creating a new projection.
	 *
	 * <p>
	 * If the standard {@link MapControlPanel} is installed, this delegates to that
	 * panel. If a subclass has replaced the control panel, this falls back to the
	 * active projection's theme, and finally to the light theme.
	 * </p>
	 *
	 * @return the current map theme
	 */
	public MapTheme getCurrentMapTheme() {
	    if (controlPanel != null) {
	        return controlPanel.getCurrentTheme();
	    }

	    if (projection != null && projection.getTheme() != null) {
	        return projection.getTheme();
	    }

	    return MapTheme.light();
	}

	/**
	 * Switches the active projection to an application-supplied projection.
	 *
	 * <p>
	 * This is the extensibility path for projections that are not represented by
	 * {@link EProjection}. The supplied projection must be fully initialized,
	 * including its {@link MapTheme}. The view rebuilds its graticule, country,
	 * city, and extra-layer renderers and resets the world coordinate system using
	 * the projection's own {@link IMapProjection#getXYBounds()}.
	 * </p>
	 *
	 * @param newProjection the new projection; must not be {@code null}
	 */
	public void setProjection(IMapProjection newProjection) {
	    projection = Objects.requireNonNull(newProjection, "newProjection");

	    if (projection.getTheme() == null) {
	        projection.setTheme(getCurrentMapTheme());
	    }

	    rebuildGraticuleRenderer();
	    getIContainer().resetWorldSystem(
	            getWorldSystem(projection));

	    rebuildCountryRenderer();
	    rebuildCityRenderer();

	    for (ShapefileLayer layer : extraLayers) {
	        layer.setProjection(projection);
	    }
	    
	    refresh();
	}

	/**
	 * Replaces the standard map control panel with a subclass/application supplied
	 * component.
	 *
	 * <p>
	 * This method is intended for subclasses such as Mosaic views that want their
	 * own projection selector and display controls. It is safe to call from the
	 * subclass constructor after {@code super(...)} returns.
	 * </p>
	 *
	 * @param newControlPanel the replacement control panel; must not be
	 *        {@code null}
	 */
	protected void setMapControlPanel(JPanel newControlPanel) {
	    Objects.requireNonNull(newControlPanel, "newControlPanel");

	    if (sideTopStack == null) {
	        return;
	    }

	    if (controlPanel != null) {
	        sideTopStack.remove(controlPanel);
	    }

	    controlPanel = (newControlPanel instanceof MapControlPanel mcp) ? mcp : null;
	    sideTopStack.add(newControlPanel, 0);

	    sideTopStack.revalidate();
	    sideTopStack.repaint();
	}

	/**
	 * Returns the standard map control panel, or {@code null} if a subclass has
	 * replaced it with a custom component.
	 *
	 * @return the map control panel, or {@code null}
	 */
	public MapControlPanel getMapControlPanel() {
	    return controlPanel;
	}

	/**
	 * Adds an application-supplied component to the side panel, below the map
	 * control panel and above the feedback pane.
	 *
	 * <p>
	 * This is intended for domain-specific controls such as Mosaic buttons for
	 * Monte Carlo points, prepatches, theta patches, final patches, and patch
	 * labels.
	 * </p>
	 *
	 * @param component the component to add; ignored if {@code null}
	 */
	protected void addCustomSidePanelComponent(JPanel component) {
	    if (component == null || customSidePanelHost == null) {
	        return;
	    }

	    customSidePanelHost.add(component);
	    customSidePanelHost.revalidate();
	    customSidePanelHost.repaint();
	}


	/**
	 * Returns the ETOPO5 layer.
	 *
	 * @return ETOPO5 layer, or {@code null} if not installed
	 */
	public Layer getEtopo5Layer() {
	    return etopo5Layer;
	}

	/**
	 * Returns the ETOPO5 renderer.
	 *
	 * @return renderer, or {@code null} if not installed
	 */
	protected Etopo5Renderer getEtopo5Renderer() {
	    return etopo5Renderer;
	}

	/**
	 * Returns the country-fill layer.
	 *
	 * @return country-fill layer
	 */
	public Layer getCountryFillLayer() {
	    return countryFillLayer;
	}

	/**
	 * Returns the country-boundary layer.
	 *
	 * @return country-boundary layer
	 */
	public Layer getCountryBoundaryLayer() {
	    return countryBoundaryLayer;
	}
	/**
	 * Returns the standard city layer.
	 *
	 * @return city layer, or {@code null} if city data have not been set
	 */
	public Layer getCityLayer() {
	    return cityLayer;
	}

	/**
	 * Returns the standard graticule layer.
	 *
	 * @return the graticule layer; never {@code null} after construction
	 */
	public Layer getGraticuleLayer() {
	    return graticuleLayer;
	}

	/**
	 * Creates the standard city layer if it has not already been created.
	 */
	private void ensureCityLayer() {
	    if (cityLayer != null) {
	        return;
	    }

	    cityLayer = new CityLayer(
	            getIContainer(),
	            "Cities",
	            this::getCityRenderer);

	    cityLayer.setEditable(true);
	    cityLayer.setLocked(true);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * When the user hovers over the map, this method displays a small window with
	 * the country name and any configured shapefile feedback fields found under
	 * the cursor. The information is shown in the {@link HoverInfoWindow}
	 * provided by the {@link MapContainer}.
	 * </p>
	 */
	@Override
	public void hoverUpdate(HoverEvent he) {
		if (!hoveringEnabled) {
			return;
		}

		Point pp = he.getLocation();
		HoverInfoWindow win = container.getHoverWindow();

		if ((win == null) || (pp == null)) {
			return;
		}
		String hoverText = getMapHoverText(pp, container);
		if (hoverText == null) {
			return;
		}

		win.showMessage(he, hoverText);
	}

	/**
	 * Builds the unified country and shapefile text shown by the hover popup.
	 * Registered shapefile feedback fields are included in layer order.
	 *
	 * @param point cursor location in container coordinates
	 * @param sourceContainer container used for hit testing
	 * @return plain, possibly multi-line hover text, or {@code null} for no hit
	 */
	protected String getMapHoverText(Point point, IContainer sourceContainer) {
	    List<String> hits = new ArrayList<>();
	    appendMapFeatureFeedback(point, sourceContainer, hits);
	    if (hits.isEmpty()) {
	        return null;
	    }

	    StringBuilder text = new StringBuilder();
	    for (String hit : hits) {
	        String plain = stripFeedbackStyle(hit);
	        if (plain.isEmpty()) {
	            continue;
	        }
	        if (text.length() > 0) {
	            text.append('\n');
	        }
	        text.append(plain);
	    }
	    return text.length() == 0 ? null : text.toString();
	}

	/** Removes an optional {@code $color$} feedback-pane prefix. */
	private static String stripFeedbackStyle(String value) {
	    if (value != null && value.startsWith("$")) {
	        int closing = value.indexOf('$', 1);
	        if (closing > 1) {
	            return value.substring(closing + 1);
	        }
	    }
	    return value == null ? "" : value;
	}

	/**
	 * Returns whether delayed mouse-over feedback windows are enabled.
	 * Hover feedback is disabled by default.
	 *
	 * @return {@code true} when hover feedback is enabled
	 */
	public boolean isHoveringEnabled() {
		return hoveringEnabled;
	}

	/**
	 * Enables or disables delayed mouse-over feedback windows. The feature is
	 * disabled by default. Disabling it immediately hides any popup that is
	 * currently visible.
	 *
	 * @param enabled {@code true} to enable map hovering
	 */
	public void setHoveringEnabled(boolean enabled) {
		hoveringEnabled = enabled;
		if (!enabled && container != null) {
			HoverInfoWindow win = container.getHoverWindow();
			if (win != null) {
				win.hideMessage();
			}
		}
	}

	/**
	 * Removes all custom application components from the side panel.
	 */
	protected void clearCustomSidePanelComponents() {
	    if (customSidePanelHost == null) {
	        return;
	    }

	    customSidePanelHost.removeAll();
	    customSidePanelHost.revalidate();
	    customSidePanelHost.repaint();
	}
}
