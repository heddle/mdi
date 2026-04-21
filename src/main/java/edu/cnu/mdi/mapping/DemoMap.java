package edu.cnu.mdi.mapping;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.mapping.container.MapContainer;
import edu.cnu.mdi.mapping.loader.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.mapping.shapefile.ShapeFeature;
import edu.cnu.mdi.mapping.shapefile.ShapeFeatureRenderer;
import edu.cnu.mdi.mapping.shapefile.ShapeFeatureStyle;
import edu.cnu.mdi.mapping.shapefile.ShapefileFeatureLoader;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.ContainerFactory;

public class DemoMap {

	// Extracted from MapView2D — move this method into DemoApp.java
	// (or a new MapViewFactory class)

		public static MapView2D createDemoMapView() {
			// used to load the GeoJson data from the resources folder, which is necessary
			// for
			// the demo to work when run from a JAR file. We do NOT include
			// shapefiles because shapefile data cannot be reliably loaded from a
			// classpath resource. We do however demo loading shapefiles from the
			// filesystem,
			// which to work requires the user to download the shapefile data from Natural
			// Earth
			// and place it in the expected location relative to the JAR file.
			String resPrefix = Environment.MDI_RESOURCE_PATH;

			//subset of drawing tools for maps because some do not make sense
			long toolBits = ToolBits.MAPTOOLS | ToolBits.ZOOMTOOLS;

			ContainerFactory mapContainerFactory = MapContainer::new;

			MapView2D mapView = new MapView2D(PropertyUtils.TITLE, "Sample 2D Map View", PropertyUtils.FRACTION, 0.6,
					PropertyUtils.ASPECT, 1.5, PropertyUtils.CONTAINERFACTORY, mapContainerFactory,
					PropertyUtils.TOOLBARBITS, toolBits, PropertyUtils.WHEELZOOM, true);

			// now for the data loading. We load the GeoJson data (countries and cities)
			// which
			// should work both in the IDE and from a JAR file. The lakes, rivers, and state
			// boundaries shapefiles
			// are loaded from the filesystem. For the shape demo to work out of the box
			// with no code mods
			// down these three data sets from Natural Earth and place them in your home
			// dir. Alternatively
			// you can load shapefiles from any location using the "ShapefileA" menu.
			// @uri
			// https://www.naturalearthdata.com/downloads/10m-cultural-vectors/10m-admin-1-states-provinces/
			// @uri
			// https://www.naturalearthdata.com/downloads/10m-physical-vectors/10m-rivers-lake-centerlines/
			// @uri
			// https://www.naturalearthdata.com/downloads/10m-physical-vectors/10m-lakes/

			try {

				//for demo make root dir for shapefiles the user's home directory
				String shapefileRootDir = Environment.getInstance().getHomeDirectory();

				// Countries from GeoJSON resource — small enough to load from a single file,
				// and the
				// population slider works when cities are loaded from GeoJSON, so use GeoJSON
				// for both
				List<CountryFeature> countries = GeoJsonCountryLoader
						.loadFromResourceStatic(resPrefix + MapResources.COUNTRIES_GEOJSON);
				mapView.setCountries(countries);

				// colors used by demo shapefiles
				Color stateFillColor = X11Colors.getX11Color("burlywood", 120);
				Color stateBorderColor = X11Colors.getX11Color("sienna", 180);
				Color waterFillColor = new Color(107, 159, 212, 220);
				Color waterBorderColor = new Color(74, 127, 181, 180);

				// US States from shapefile — demonstrates loading from shapefile and
				// filtering to a subset of features. Note that the Natural Earth "Admin 1
				// - States, Provinces" shapefile contains both US states and first-level
				// admin units for other countries, so we filter to just the US states here.
				// We could have also loaded all the states and colored them differently based
				// on the "admin" property, but this is just a demo.

				List<ShapeFeature> usStates = new ShapefileFeatureLoader()
						.load(Path.of(shapefileRootDir,
								"ne_10m_admin_1_states_provinces/ne_10m_admin_1_states_provinces.shp"))
						.stream().filter(f -> "United States of America".equals(f.getProperty("admin")))
						.collect(Collectors.toList());
				mapView.addShapefileLayer(new ShapeFeatureRenderer(usStates, mapView.getProjection(),
						new ShapeFeatureStyle().fillColor(stateFillColor).strokeColor(stateBorderColor).strokeWidth(0.4f)
								.tooltipFields("name")),
						"US States");

				// Major rivers only — add BEFORE lakes so lakes paint over river ends
				List<ShapeFeature> allRivers = new ShapefileFeatureLoader().load(
						Path.of(shapefileRootDir, "ne_10m_rivers_lake_centerlines/ne_10m_rivers_lake_centerlines.shp"));
				List<ShapeFeature> majorRivers = allRivers.stream().filter(f -> f.getPropertyInt("scalerank", 99) <= 6)
						.collect(Collectors.toList());
				mapView.addShapefileLayer(new ShapeFeatureRenderer(majorRivers, mapView.getProjection(),
						new ShapeFeatureStyle().strokeColor(waterBorderColor).strokeWidth(0.5f).tooltipFields("name")),
						"Rivers (major)");

				// Lakes — drawn after rivers so they cover river centerlines cleanly
				MapView2D.loadShapefileLayer(mapView, Path.of(shapefileRootDir, "ne_10m_lakes/ne_10m_lakes.shp"), "Lakes",
						new ShapeFeatureStyle().fillColor(waterFillColor).strokeColor(waterBorderColor).strokeWidth(0.4f)
								.tooltipFields("name"));

				// Cities — use GeoJSON so the population slider works
				mapView.setCities(GeoJsonCityLoader.loadFromResourceStatic(resPrefix + MapResources.CITIES_GEOJSON));

			} catch (IOException e) {
				e.printStackTrace();
			}

			return mapView;
		}
}
