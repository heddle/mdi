package edu.cnu.mdi.mapping.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.mapping.theme.MapTheme;
import edu.cnu.mdi.mapping.theme.MapUtils;

public class MapProjectionTest {

    private static final double TOLERANCE = 1.0e-9;

    @Test
    public void testProjectionRoundTrips() {
        assertRoundTrip(
                new MercatorProjection(MapTheme.light()),
                point(-160, -80), point(-70, 0), point(20, 60));

        assertRoundTrip(
                new MollweideProjection(MapTheme.light()),
                point(-160, -80), point(-70, 0), point(20, 60));

        assertRoundTrip(
                new OrthographicProjection(0.0, 0.0, MapTheme.light()),
                point(0, 0), point(30, 20), point(-60, -30));

        assertRoundTrip(
                new LambertEqualAreaProjection(0.0, 0.0, MapTheme.light()),
                point(0, 0), point(75, 45), point(-170, -20));
    }

    @Test
    public void testInverseProjectionRejectsPointsOutsideMapDomain() {
        Point2D.Double result = new Point2D.Double();

        new MercatorProjection(MapTheme.light())
                .latLonFromXY(result, new Point2D.Double(4.0, 0.0));
        assertTrue(Double.isNaN(result.x));
        assertTrue(Double.isNaN(result.y));

        new MollweideProjection(MapTheme.light())
                .latLonFromXY(result, new Point2D.Double(3.0, 0.0));
        assertTrue(Double.isNaN(result.x));
        assertTrue(Double.isNaN(result.y));
    }

    @Test
    public void testNonFiniteCoordinatesAreNotVisible() {
        Point2D.Double nonFinite = new Point2D.Double(Double.NaN, 0.0);

        assertFalse(new MercatorProjection(MapTheme.light()).isPointVisible(nonFinite));
        assertFalse(new MollweideProjection(MapTheme.light()).isPointVisible(nonFinite));
        assertFalse(new LambertEqualAreaProjection(MapTheme.light()).isPointVisible(nonFinite));
    }

    @Test
    public void testLongitudeWrappingHandlesLargeAndNonFiniteValues() {
        IMapProjection projection = new MercatorProjection(MapTheme.light());

        assertEquals(Math.PI, projection.wrapLongitude(-Math.PI), 0.0);
        assertEquals(Math.PI / 2.0,
                projection.wrapLongitude(1000.0 * Math.PI + Math.PI / 2.0),
                TOLERANCE);
        assertTrue(Double.isNaN(projection.wrapLongitude(Double.POSITIVE_INFINITY)));
        assertTrue(Double.isNaN(projection.wrapLongitude(Double.NaN)));

        assertEquals(Math.PI / 2.0,
                MapUtils.wrapLongitude(1000.0 * Math.PI + Math.PI / 2.0),
                TOLERANCE);
        assertTrue(Double.isNaN(MapUtils.wrapLongitude(Double.NEGATIVE_INFINITY)));
    }

    @Test
    public void testProjectionFactoryAppliesOptionalCenterToGlobalProjections() {
        Point2D.Double center = point(42.0, 10.0);

        MercatorProjection mercator = (MercatorProjection) ProjectionFactory.create(
                EProjection.MERCATOR, MapTheme.light(), center);
        MollweideProjection mollweide = (MollweideProjection) ProjectionFactory.create(
                EProjection.MOLLWEIDE, MapTheme.light(), center);

        assertEquals(center.x, mercator.getCentralLongitude(), TOLERANCE);
        assertEquals(center.x, mollweide.getCentralLongitude(), TOLERANCE);
    }

    @Test
    public void testOnlyFullWidthWrappedProjectionsAreLongitudePeriodic() {
        assertTrue(new MercatorProjection(MapTheme.light()).isLongitudePeriodic());
        assertTrue(new MollweideProjection(MapTheme.light()).isLongitudePeriodic());
        assertFalse(new OrthographicProjection(0.0, 0.0, MapTheme.light())
                .isLongitudePeriodic());
        assertFalse(new LambertEqualAreaProjection(0.0, 0.0, MapTheme.light())
                .isLongitudePeriodic());
    }

    @Test
    public void testLambertProjectionOfTheAntipodeIsNaNNotABoundaryPoint() {
        LambertEqualAreaProjection projection =
                new LambertEqualAreaProjection(0.0, 0.0, MapTheme.light());

        // The antipode of the projection center (0,0) is (180 deg, 0 deg).
        Point2D.Double xy = new Point2D.Double();
        projection.latLonToXY(point(180.0, 0.0), xy);

        assertTrue(Double.isNaN(xy.x), "x must be NaN at the antipode singularity");
        assertTrue(Double.isNaN(xy.y), "y must be NaN at the antipode singularity");
    }

    @Test
    public void testLambertProjectionNearButNotAtTheAntipodeIsFinite() {
        LambertEqualAreaProjection projection =
                new LambertEqualAreaProjection(0.0, 0.0, MapTheme.light());

        Point2D.Double xy = new Point2D.Double();
        projection.latLonToXY(point(179.0, 0.0), xy);

        assertTrue(Double.isFinite(xy.x));
        assertTrue(Double.isFinite(xy.y));
    }

    private static Point2D.Double point(double longitudeDeg, double latitudeDeg) {
        return new Point2D.Double(
                Math.toRadians(longitudeDeg),
                Math.toRadians(latitudeDeg));
    }

    private static void assertRoundTrip(
            IMapProjection projection,
            Point2D.Double... geographicPoints) {

        Point2D.Double projected = new Point2D.Double();
        Point2D.Double restored = new Point2D.Double();

        for (Point2D.Double expected : geographicPoints) {
            projection.latLonToXY(expected, projected);
            assertTrue(projection.isPointOnMap(projected));

            projection.latLonFromXY(restored, projected);
            assertEquals(0.0,
                    projection.wrapLongitude(restored.x - expected.x),
                    TOLERANCE);
            assertEquals(expected.y, restored.y, TOLERANCE);
        }
    }
}
