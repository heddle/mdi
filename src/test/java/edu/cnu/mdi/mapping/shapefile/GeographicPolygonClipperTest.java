package edu.cnu.mdi.mapping.shapefile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.util.List;

import org.junit.jupiter.api.Test;

public class GeographicPolygonClipperTest {

    private static final double TOLERANCE = 1.0e-10;

    @Test
    public void testAntarcticCapIsClosedAlongSeamAndLatitudeLimit() {
        List<Point2D.Double> ring = List.of(
                degrees(100.0, -80.0),
                degrees(120.0, -80.0),
                degrees(120.0, -90.0),
                degrees(100.0, -90.0),
                degrees(100.0, -80.0));

        double center = Math.toRadians(-70.0);
        double minLon = center - Math.PI;
        double maxLon = center + Math.PI;
        double minLat = Math.toRadians(-89.0);
        List<List<Point2D.Double>> pieces =
                GeographicPolygonClipper.clipPeriodicRing(
                        ring, minLon, maxLon, minLat, Math.toRadians(89.0));

        assertEquals(2, pieces.size());
        assertTrue(pieces.stream().allMatch(piece -> piece.size() >= 4));
        assertTrue(pieces.stream().flatMap(List::stream)
                .allMatch(point -> point.x >= minLon - TOLERANCE
                        && point.x <= maxLon + TOLERANCE
                        && point.y >= minLat - TOLERANCE));
        assertTrue(hasPoint(pieces, minLon, Math.toRadians(-80.0)));
        assertTrue(hasPoint(pieces, minLon, minLat));
        assertTrue(hasPoint(pieces, maxLon, Math.toRadians(-80.0)));
        assertTrue(hasPoint(pieces, maxLon, minLat));
    }

    @Test
    public void testOrdinaryRingInsideDomainIsUnchanged() {
        List<Point2D.Double> ring = List.of(
                degrees(-10.0, -10.0),
                degrees(10.0, -10.0),
                degrees(10.0, 10.0),
                degrees(-10.0, 10.0),
                degrees(-10.0, -10.0));

        List<List<Point2D.Double>> pieces =
                GeographicPolygonClipper.clipPeriodicRing(
                        ring, -Math.PI, Math.PI,
                        -Math.PI / 2.0, Math.PI / 2.0);

        assertEquals(1, pieces.size());
        assertEquals(4, pieces.get(0).size());
    }

    private static boolean hasPoint(
            List<List<Point2D.Double>> pieces, double lon, double lat) {
        return pieces.stream().flatMap(List::stream)
                .anyMatch(point -> Math.abs(point.x - lon) < TOLERANCE
                        && Math.abs(point.y - lat) < TOLERANCE);
    }

    private static Point2D.Double degrees(double lon, double lat) {
        return new Point2D.Double(Math.toRadians(lon), Math.toRadians(lat));
    }
}
