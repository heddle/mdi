package edu.cnu.mdi.mapping.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    public void testLoadersRejectMalformedJsonAsIOException() throws IOException {
        for (String text : new String[] {
                "",                                          // empty file
                "{\"type\":\"FeatureCollection\",",          // truncated
                "[1,2,3]",                                   // root is not an object
                "{\"type\":\"Topology\",\"features\":[]}",   // wrong type
                "{\"type\":\"FeatureCollection\"}",          // no features
                "{\"type\":\"FeatureCollection\",\"features\":{}}", // features not an array
                "{\"type\":\"FeatureCollection\",\"features\":[]} junk" // trailing content
        }) {
            Path path = tempDir.resolve("bad.geojson");
            Files.writeString(path, text);
            assertThrows(IOException.class, () -> GeoJsonCityLoader.loadStatic(path), text);
            assertThrows(IOException.class, () -> GeoJsonCountryLoader.loadStatic(path), text);
        }
    }

    @Test
    public void testLoadersToleratePropertyAndGeometryTypeMismatches()
            throws IOException {

        Path path = tempDir.resolve("mixed.geojson");
        Files.writeString(path, """
                {"type":"FeatureCollection","features":[
                  "not a feature",
                  {"type":"Feature","properties":null,
                   "geometry":{"type":"Point","coordinates":[1,2]}},
                  {"type":"Feature","properties":{"NAME":null,"name":"Lower",
                                                  "POP_MAX":"many","pop_max":8908081.0,
                                                  "SCALERANK":3},
                   "geometry":{"type":"Point","coordinates":[3,4,100]}},
                  {"type":"Feature","geometry":{"type":"Point","coordinates":{}}},
                  {"type":"Feature","geometry":null}
                ]}
                """);

        var cities = GeoJsonCityLoader.loadStatic(path);

        assertEquals(2, cities.size());
        assertNull(cities.get(0).getName());
        assertEquals(-1L, cities.get(0).getPopulation());
        assertEquals("Lower", cities.get(1).getName());
        assertEquals(8908081L, cities.get(1).getPopulation());
        assertEquals(3, cities.get(1).getScalerank());
        assertEquals(Math.toRadians(4), cities.get(1).getLatitude());

        // Same document through the country loader: nothing there is a valid country.
        assertEquals(0, GeoJsonCountryLoader.loadStatic(path).size());
    }

    @Test
    public void testCountryLoaderSkipsNonArrayCoordinateEntries() throws IOException {
        Path path = tempDir.resolve("odd.geojson");
        Files.writeString(path, """
                {"type":"FeatureCollection","features":[
                  {"type":"Feature","properties":{"ADMIN":"Odd","ISO_A3":"ODD"},
                   "geometry":{"type":"Polygon","coordinates":[
                     [[0,0],5,"x",null,{"a":1},[1,0],[0,1]]
                   ]}}
                ]}
                """);

        var countries = GeoJsonCountryLoader.loadStatic(path);

        assertEquals(1, countries.size());
        assertEquals(3, countries.get(0).getPolygons().get(0).size());
    }
}
