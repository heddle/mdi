package edu.cnu.mdi.mapping;


/**
 * A utility class that provides constants for resource paths used in the mapping package. 
 * This includes paths to GeoJSON files for countries and cities, which are sourced from Natural Earth. 
 * The class is designed to centralize resource path management, making it easier to maintain and update resource locations in the future.
 */
public final class MapResources {

	/** GeoJSON files are from Natural Earth: https://www.naturalearthdata.com/downloads/10m-cultural-vectors/ */
	public static final String COUNTRIES_GEOJSON = "geo/countries.geojson";

	/** GeoJSON files are from Natural Earth: https://www.naturalearthdata.com/downloads/10m-cultural-vectors/ */
	public static final String CITIES_GEOJSON = "geo/cities.geojson";

	private MapResources() {
	}
}
