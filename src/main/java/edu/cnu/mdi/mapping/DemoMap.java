package edu.cnu.mdi.mapping;

import java.io.IOException;
import java.util.List;

import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.mapping.container.MapContainer;
import edu.cnu.mdi.mapping.loader.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.mapping.milsym.NatoIconPicker;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.ContainerFactory;

public class DemoMap {

	// Extracted from MapView2D — move this method into DemoApp.java
	// (or a new MapViewFactory class)

		public static MapView2D createDemoMapView() {
			// used to load the GeoJson data from the resources folder, which is necessary
			// for the demo to work when run from a JAR file. We do NOT include
			// shapefiles because shapefile data cannot be reliably loaded from a
			// classpath resource.
			String resPrefix = Environment.MDI_RESOURCE_PATH;

			//subset of drawing tools for maps because some do not make sense
			long toolBits = ToolBits.MAPTOOLS | ToolBits.ZOOMTOOLS;


			ContainerFactory mapContainerFactory = MapContainer::new;

			MapView2D mapView = new MapView2D(PropertyUtils.TITLE, "Sample 2D Map View",
					PropertyUtils.FRACTION, 0.6,
					PropertyUtils.ASPECT, 1.5, PropertyUtils.CONTAINERFACTORY, mapContainerFactory,
					PropertyUtils.TOOLBARBITS, toolBits, PropertyUtils.WHEELZOOM, true);

			// now for the data loading. We load the GeoJson data (countries and cities)
			// which should work both in the IDE and from a JAR file.
			try {


				// Countries from GeoJSON resource — small enough to load from a single file,
				// and the
				// population slider works when cities are loaded from GeoJSON, so use GeoJSON
				// for both
				List<CountryFeature> countries = GeoJsonCountryLoader
						.loadFromResourceStatic(resPrefix + MapResources.COUNTRIES_GEOJSON);
				mapView.setCountries(countries);

				// Cities — use GeoJSON so the population slider works
				mapView.setCities(GeoJsonCityLoader.loadFromResourceStatic(resPrefix + MapResources.CITIES_GEOJSON));

				NatoIconPicker picker = new NatoIconPicker();

				// addWestPanel uses a double-invokeLater to run after all
				// construction placement has settled.
				mapView.addWestPanel(picker);

			} catch (IOException e) {
				e.printStackTrace();
			}

			return mapView;
		}
}