package edu.cnu.mdi.mapping.shapefile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ShapefileCrsValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testMissingProjectionFileUsesDocumentedWgs84Assumption() {
        assertDoesNotThrow(() -> ShapefileCrsValidator.validate(
                tempDir.resolve("countries.shp")));
    }

    @Test
    public void testWkt1Wgs84GeographicCrsIsAccepted() throws IOException {
        Path shp = tempDir.resolve("countries.shp");
        Files.writeString(tempDir.resolve("countries.prj"),
                "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\","
              + "SPHEROID[\"WGS_1984\",6378137,298.257223563]],"
              + "PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.0174532925199433]]");

        assertDoesNotThrow(() -> ShapefileCrsValidator.validate(shp));
    }

    @Test
    public void testWkt2Wgs84GeographicCrsIsAccepted() throws IOException {
        Path shp = tempDir.resolve("cities.shp");
        Files.writeString(tempDir.resolve("cities.PRJ"),
                "GEOGCRS[\"WGS 84\",DATUM[\"World Geodetic System 1984\","
              + "ELLIPSOID[\"WGS 84\",6378137,298.257223563]],"
              + "CS[ellipsoidal,2],ANGLEUNIT[\"degree\",0.0174532925199433],"
              + "ID[\"EPSG\",4326]]");

        assertDoesNotThrow(() -> ShapefileCrsValidator.validate(shp));
    }

    @Test
    public void testNad83Grs80GeographicCrsIsAccepted() throws IOException {
        Path shp = tempDir.resolve("primaryroads.shp");
        Files.writeString(tempDir.resolve("primaryroads.prj"),
                "GEOGCS[\"GCS_North_American_1983\","
              + "DATUM[\"D_North_American_1983\","
              + "SPHEROID[\"GRS_1980\",6378137,298.257222101]],"
              + "PRIMEM[\"Greenwich\",0],"
              + "UNIT[\"Degree\",0.017453292519943295]]");

        assertDoesNotThrow(() -> ShapefileCrsValidator.validate(shp));
    }

    @Test
    public void testProjectedCrsIsRejectedEvenWhenBasedOnWgs84() throws IOException {
        Path shp = tempDir.resolve("roads.shp");
        Files.writeString(tempDir.resolve("roads.prj"),
                "PROJCS[\"WGS_1984_UTM_Zone_18N\","
              + "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\"]],"
              + "UNIT[\"Meter\",1.0]]");

        IOException error = assertThrows(IOException.class,
                () -> ShapefileCrsValidator.validate(shp));
        assertTrue(error.getMessage().contains("not a geographic CRS"));
    }

    @Test
    public void testNonWgs84GeographicCrsIsRejected() throws IOException {
        Path shp = tempDir.resolve("legacy.shp");
        Files.writeString(tempDir.resolve("legacy.prj"),
                "GEOGCS[\"GCS_North_American_1927\","
              + "DATUM[\"D_North_American_1927\"],"
              + "UNIT[\"Degree\",0.0174532925199433]]");

        IOException error = assertThrows(IOException.class,
                () -> ShapefileCrsValidator.validate(shp));
        assertTrue(error.getMessage().contains("not recognized as WGS84 or NAD83/GRS80"));
    }

    @Test
    public void testEmptyProjectionFileIsRejected() throws IOException {
        Path shp = tempDir.resolve("empty.shp");
        Files.writeString(tempDir.resolve("empty.prj"), "  ");

        assertThrows(IOException.class, () -> ShapefileCrsValidator.validate(shp));
    }
}
