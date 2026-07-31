package edu.cnu.mdi.mapping.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GeoJsonLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    public void testCityLoaderSkipsNonNumericAndOutOfRangeCoordinates()
            throws IOException {

        Path path = tempDir.resolve("cities.geojson");
        Files.writeString(path, """
                {"type":"FeatureCollection","features":[
                  {"type":"Feature","properties":{"NAME":"Valid"},
                   "geometry":{"type":"Point","coordinates":[12,34]}},
                  {"type":"Feature","properties":{"NAME":"Text"},
                   "geometry":{"type":"Point","coordinates":["east","north"]}},
                  {"type":"Feature","properties":{"NAME":"Invalid latitude"},
                   "geometry":{"type":"Point","coordinates":[12,91]}}
                ]}
                """);

        var cities = GeoJsonCityLoader.loadStatic(path);

        assertEquals(1, cities.size());
        assertEquals("Valid", cities.get(0).getName());
        assertEquals(Math.toRadians(12), cities.get(0).getLongitude());
        assertEquals(Math.toRadians(34), cities.get(0).getLatitude());
    }

    @Test
    public void testCountryLoaderSkipsDegenerateAndMalformedRings()
            throws IOException {

        Path path = tempDir.resolve("countries.geojson");
        Files.writeString(path, """
                {"type":"FeatureCollection","features":[
                  {"type":"Feature","properties":{"ADMIN":"Valid","ISO_A3":"VAL"},
                   "geometry":{"type":"Polygon","coordinates":[
                     [[0,0],[1,0],[0,1],[0,0]]
                   ]}},
                  {"type":"Feature","properties":{"ADMIN":"Short","ISO_A3":"SHT"},
                   "geometry":{"type":"Polygon","coordinates":[
                     [[0,0],[1,0]]
                   ]}},
                  {"type":"Feature","properties":{"ADMIN":"Text","ISO_A3":"TXT"},
                   "geometry":{"type":"Polygon","coordinates":[
                     [["west","south"],["east","south"],["west","north"]]
                   ]}}
                ]}
                """);

        var countries = GeoJsonCountryLoader.loadStatic(path);

        assertEquals(1, countries.size());
        assertEquals("VAL", countries.get(0).getIsoA3());
        assertEquals(4, countries.get(0).getPolygons().get(0).size());
    }
}
