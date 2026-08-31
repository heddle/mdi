package edu.cnu.mdi.mapping.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.mapping.projection.IMapProjection;
import edu.cnu.mdi.mapping.projection.MercatorProjection;
import edu.cnu.mdi.mapping.projection.MollweideProjection;
import edu.cnu.mdi.mapping.theme.MapTheme;

class MapGraphicsTest {

	@Test
	void seamCrossingsAreRefinedOnBothWrappedProjections() {
		assertRefinedCrossing(new MercatorProjection(MapTheme.light()));
		assertRefinedCrossing(new MollweideProjection(MapTheme.light()));
	}

	private static void assertRefinedCrossing(IMapProjection projection) {
		Point2D.Double before = point(108.0, -12.0);
		Point2D.Double after = point(114.0, -28.0);

		MapGraphics.SeamCrossing crossing =
				MapGraphics.refineSeamCrossing(projection, before, after);

		assertTrue(projection.crossesSeam(crossing.before().x, crossing.after().x));
		assertEquals(crossing.before().y, crossing.after().y, 1.0e-9);

		Point2D.Double beforeXy = new Point2D.Double();
		Point2D.Double afterXy = new Point2D.Double();
		projection.latLonToXY(crossing.before(), beforeXy);
		projection.latLonToXY(crossing.after(), afterXy);
		assertTrue(beforeXy.x * afterXy.x < 0.0);
		assertEquals(Math.abs(beforeXy.x), Math.abs(afterXy.x), 1.0e-10);
	}

	@Test
	void greatCircleLengthOfAKnownQuarterCircleIsHalfPi() {
		// Equator, 0 lon to 90 lon: a quarter of the way around the globe.
		double length = MapGraphics.greatCircleLength(point(0.0, 0.0), point(90.0, 0.0));
		assertEquals(Math.PI / 2.0, length, 1.0e-12);
	}

	@Test
	void greatCircleLengthOfCoincidentPointsIsZero() {
		// acos() is ill-conditioned near its cosOmega=1 argument, so this needs a
		// looser tolerance than the other length assertions.
		Point2D.Double p = point(30.0, 40.0);
		assertEquals(0.0, MapGraphics.greatCircleLength(p, new Point2D.Double(p.x, p.y)), 1.0e-6);
	}

	@Test
	void greatCircleLengthRejectsNullArguments() {
		Point2D.Double p = point(0.0, 0.0);
		assertThrows(IllegalArgumentException.class, () -> MapGraphics.greatCircleLength(null, p));
		assertThrows(IllegalArgumentException.class, () -> MapGraphics.greatCircleLength(p, null));
	}

	@Test
	void greatCircleAzimuthMatchesCardinalDirections() {
		Point2D.Double origin = point(0.0, 0.0);
		// A point due east along the equator: azimuth pi/2.
		assertEquals(Math.PI / 2.0, MapGraphics.greatCircleAzimuth(origin, point(1.0, 0.0)), 1.0e-9);
		// A point due north: azimuth 0.
		assertEquals(0.0, MapGraphics.greatCircleAzimuth(origin, point(0.0, 1.0)), 1.0e-9);
		// A point due south: azimuth pi.
		assertEquals(Math.PI, MapGraphics.greatCircleAzimuth(origin, point(0.0, -1.0)), 1.0e-9);
	}

	@Test
	void greatCircleAzimuthOfCoincidentPointsIsNaN() {
		Point2D.Double p = point(12.0, -34.0);
		assertTrue(Double.isNaN(
				MapGraphics.greatCircleAzimuth(p, new Point2D.Double(p.x, p.y))));
	}

	@Test
	void greatCircleEndPointRoundTripsWithLengthAndAzimuth() {
		Point2D.Double start = point(-40.0, 20.0);
		double azimuth = Math.toRadians(65.0);
		double length = Math.toRadians(30.0);

		Point2D.Double end = MapGraphics.greatCircleEndPoint(start, azimuth, length);

		assertEquals(length, MapGraphics.greatCircleLength(start, end), 1.0e-9);
		assertEquals(azimuth, MapGraphics.greatCircleAzimuth(start, end), 1.0e-9);
	}

	@Test
	void greatCircleEndPointOfZeroLengthIsTheStartPoint() {
		Point2D.Double start = point(15.0, -25.0);
		Point2D.Double end = MapGraphics.greatCircleEndPoint(start, Math.toRadians(200.0), 0.0);

		assertEquals(start.x, end.x, 1.0e-12);
		assertEquals(start.y, end.y, 1.0e-12);
	}

	@Test
	void greatCircleEndPointWrapsLongitudeToTheCanonicalRange() {
		// Heading due east from just west of the antimeridian must wrap past +-180.
		Point2D.Double start = point(170.0, 0.0);
		Point2D.Double end = MapGraphics.greatCircleEndPoint(start, Math.PI / 2.0, Math.toRadians(20.0));

		assertTrue(end.x > -Math.PI && end.x <= Math.PI, "longitude must stay within (-pi, pi]");
		assertEquals(Math.toRadians(-170.0), end.x, 1.0e-9);
	}

	private static Point2D.Double point(double longitudeDegrees, double latitudeDegrees) {
		return new Point2D.Double(Math.toRadians(longitudeDegrees), Math.toRadians(latitudeDegrees));
	}
}
