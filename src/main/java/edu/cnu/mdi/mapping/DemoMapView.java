package edu.cnu.mdi.mapping;

import java.awt.geom.Rectangle2D;
import java.awt.Color;
import java.io.IOException;
import java.util.List;

import edu.cnu.mdi.mapping.projection.MercatorProjection;
import edu.cnu.mdi.graphics.rubberband.ARubberband;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.mapping.container.MapContainer;
import edu.cnu.mdi.mapping.loader.Etopo5Loader;
import edu.cnu.mdi.mapping.loader.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.mapping.milsym.NatoIconPicker;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.ContainerFactory;

/**
 * Demonstration map view that assembles the built-in terrain, country, city,
 * and NATO-symbol components.
 *
 * <p>The constructor loads the bundled datasets and is intended as an
 * executable example rather than a general-purpose application view.</p>
 */
@SuppressWarnings("serial")
public class DemoMapView extends MapView2D {

	/**
	 * Constructor for the DemoMapView class.
	 * Initializes the map view with default settings, loads ETOPO5 terrain data,
	 * country and city data from GeoJSON resources, and sets up a NATO icon picker.
	 * Also adds a checkbox to toggle the display of ETOPO5 terrain.
	 */
	public DemoMapView() {
	    super(defaults());
	    setBackground(Color.BLACK);

	    try {
	        String resPrefix = Environment.MDI_RESOURCE_PATH;

	        installEtopo5Layer(
	                Etopo5Loader.loadDefaultResource(),
	                false);

	        List<CountryFeature> countries =
	                GeoJsonCountryLoader.loadFromResourceStatic(
	                        resPrefix
	                                + MapResources.COUNTRIES_GEOJSON);

	        setCountries(countries);

	        setCities(
	                GeoJsonCityLoader.loadFromResourceStatic(
	                        resPrefix
	                                + MapResources.CITIES_GEOJSON));

	        addWestPanel(new NatoIconPicker());

	    } catch (IOException e) {
	        edu.cnu.mdi.log.Log.getInstance().exception(e);
	    }
	}

	private static Object[] defaults() {
		ContainerFactory mapContainerFactory = ProportionedMapContainer::new;

		return new Object[] {
				PropertyUtils.TITLE, "Sample 2D Map View",
				PropertyUtils.FRACTION, 0.7,
				// a box of the canvas's shape, so zooming keeps the proportions
				PropertyUtils.BOXZOOMRBPOLICY, ARubberband.Policy.RECTANGLE_PRESERVE_ASPECT,
				PropertyUtils.ASPECT, 1.2,
				PropertyUtils.FEEDBACKFONTSIZE, 11,
				PropertyUtils.CONTAINERFACTORY, mapContainerFactory,
				PropertyUtils.TOOLBARBITS, ToolBits.MAPTOOLS | ToolBits.ZOOMTOOLS,
				PropertyUtils.WHEELZOOM, true		
				};
	}

	/**
	 * A map container that keeps the Mercator projection in proportion, whatever
	 * the shape of the canvas: a world unit is drawn as tall as it is wide. The
	 * world rectangle keeps its width and takes its height from the canvas, so a
	 * wide canvas leaves the highest latitudes out of view. Other projections
	 * are left to fill the canvas.
	 */
	static final class ProportionedMapContainer extends MapContainer {

		ProportionedMapContainer(Rectangle2D.Double worldSystem) {
			super(worldSystem);
		}

		@Override
		protected void setAffineTransforms() {
			if (getView() instanceof MapView2D map && map.getProjection() instanceof MercatorProjection
					&& getWidth() > 0 && getHeight() > 0) {
				double height = _worldSystem.width * getHeight() / getWidth();
				_worldSystem.y = _worldSystem.getCenterY() - height / 2;
				_worldSystem.height = height;
			}
			super.setAffineTransforms();
		}
	}

}
