package edu.cnu.mdi.mapping.util;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

public class GeoUtilsTest {

	private static final double DEG_TOL = 1.0e-7;
	private static final double RAD_TOL = 1.0e-10;
	private static final double METER_TOL = 1.0e-2;

	private static void assertRoundTripDegrees(double latDeg, double lonDeg) {
		UTMCoordinate utm = GeoUtils.fromDecimalDegrees(latDeg, lonDeg);
		double[] result = GeoUtils.toDecimalDegrees(utm);

		assertEquals(latDeg, result[0], DEG_TOL);
		assertEquals(normalizeExpectedLongitude(lonDeg), result[1], DEG_TOL);
	}

	private static double normalizeExpectedLongitude(double lonDeg) {
		lonDeg = lonDeg % 360.0;
		if (lonDeg >= 180.0) {
			lonDeg -= 360.0;
		}
		if (lonDeg < -180.0) {
			lonDeg += 360.0;
		}
		return lonDeg;
	}

	@Test
	public void testEquatorPrimeMeridianToUtm() {
		UTMCoordinate utm = GeoUtils.fromDecimalDegrees(0.0, 0.0);

		assertEquals(31, utm.zone);
		assertEquals('N', utm.letter);
		assertEquals(166021.443, utm.easting, METER_TOL);
		assertEquals(0.0, utm.northing, METER_TOL);
	}

	@Test
	public void testKnownCNURegionCoordinate() {
		UTMCoordinate utm = GeoUtils.fromDecimalDegrees(37.0, -76.0);

		assertEquals(18, utm.zone);
		assertEquals('S', utm.letter);

		double[] back = GeoUtils.toDecimalDegrees(utm);
		assertEquals(37.0, back[0], DEG_TOL);
		assertEquals(-76.0, back[1], DEG_TOL);
	}

	@Test
	public void testSouthernHemisphereCoordinate() {
		UTMCoordinate utm = GeoUtils.fromDecimalDegrees(-33.9, 151.2);

		assertEquals(56, utm.zone);
		assertEquals('H', utm.letter);
		assertTrue(utm.northing > 0.0);

		double[] back = GeoUtils.toDecimalDegrees(utm);
		assertEquals(-33.9, back[0], DEG_TOL);
		assertEquals(151.2, back[1], DEG_TOL);
	}

	@Test
	public void testRoundTripsForRepresentativeLocations() {
		assertRoundTripDegrees(0.0, 0.0);
		assertRoundTripDegrees(37.0, -76.0);
		assertRoundTripDegrees(-33.9, 151.2);
		assertRoundTripDegrees(60.0, 6.0);
		assertRoundTripDegrees(78.0, 15.0);
		assertRoundTripDegrees(83.999, -40.0);
		assertRoundTripDegrees(-79.999, 20.0);
	}

	@Test
	public void testFromRadiansAndToRadiansUseLonLatConvention() {
		Point2D.Double lonLatRad = new Point2D.Double(
				Math.toRadians(-76.0),
				Math.toRadians(37.0));

		UTMCoordinate utm = GeoUtils.fromRadians(lonLatRad);
		Point2D.Double back = GeoUtils.toRadians(utm);

		assertEquals(Math.toRadians(-76.0), back.x, RAD_TOL);
		assertEquals(Math.toRadians(37.0), back.y, RAD_TOL);
	}

	@Test
	public void testOutsideUtmLatitudeRangeReturnsSpecialCoordinate() {
		UTMCoordinate tooFarNorth = GeoUtils.fromDecimalDegrees(84.1, 0.0);
		UTMCoordinate tooFarSouth = GeoUtils.fromDecimalDegrees(-80.1, 0.0);

		assertEquals("outside valid range", tooFarNorth.toString());
		assertEquals("outside valid range", tooFarSouth.toString());
	}

	@Test
	public void testLatitudeLimitsAreAccepted() {
		UTMCoordinate southLimit = GeoUtils.fromDecimalDegrees(-80.0, 0.0);
		UTMCoordinate northLimit = GeoUtils.fromDecimalDegrees(84.0, 0.0);

		assertNotEquals("outside valid range", southLimit.toString());
		assertNotEquals("outside valid range", northLimit.toString());
	}

	@Test
	public void testToDecimalDegreesRejectsInvalidZone() {
		assertThrows(IllegalArgumentException.class,
				() -> GeoUtils.toDecimalDegrees(new UTMCoordinate(500000.0, 0.0, 0, 'N')));

		assertThrows(IllegalArgumentException.class,
				() -> GeoUtils.toDecimalDegrees(new UTMCoordinate(500000.0, 0.0, 61, 'N')));
	}

	@Test
	public void testLongitudeToZoneStandardCases() {
		assertEquals(1, GeoUtils.longitudeToZone(-180.0, 0.0));
		assertEquals(1, GeoUtils.longitudeToZone(-177.0, 0.0));
		assertEquals(30, GeoUtils.longitudeToZone(-3.1, 0.0));
		assertEquals(31, GeoUtils.longitudeToZone(0.0, 0.0));
		assertEquals(60, GeoUtils.longitudeToZone(179.999, 0.0));
	}

	@Test
	public void testLongitudeToZoneNorwaySpecialCase() {
		assertEquals(32, GeoUtils.longitudeToZone(6.0, 60.0));
		assertEquals(32, GeoUtils.longitudeToZone(11.999, 63.999));
	}

	@Test
	public void testLongitudeToZoneSvalbardSpecialCases() {
		assertEquals(31, GeoUtils.longitudeToZone(3.0, 78.0));
		assertEquals(33, GeoUtils.longitudeToZone(15.0, 78.0));
		assertEquals(35, GeoUtils.longitudeToZone(27.0, 78.0));
		assertEquals(37, GeoUtils.longitudeToZone(36.0, 78.0));
	}

	@Test
	public void testZoneToCentralMeridian() {
		assertEquals(-177.0, GeoUtils.zoneTocentralMeridianDeg(1), DEG_TOL);
		assertEquals(3.0, GeoUtils.zoneTocentralMeridianDeg(31), DEG_TOL);
		assertEquals(177.0, GeoUtils.zoneTocentralMeridianDeg(60), DEG_TOL);
	}

	@Test
	public void testLatitudeToZoneLetter() {
		assertEquals('C', GeoUtils.latitudeToZoneLetter(-80.0));
		assertEquals('C', GeoUtils.latitudeToZoneLetter(-79.999));
		assertEquals('D', GeoUtils.latitudeToZoneLetter(-72.0));
		assertEquals('M', GeoUtils.latitudeToZoneLetter(-0.001));
		assertEquals('N', GeoUtils.latitudeToZoneLetter(0.0));
		assertEquals('P', GeoUtils.latitudeToZoneLetter(8.0));
		assertEquals('X', GeoUtils.latitudeToZoneLetter(72.0));
		assertEquals('X', GeoUtils.latitudeToZoneLetter(84.0));
	}

	@Test
	public void testUtmCoordinateToString() {
		UTMCoordinate utm = new UTMCoordinate(123456.789, 9876543.219, 18, 'S');

		assertEquals("18S 123456.79 E, 9876543.22 N", utm.toString());
	}

	@Test
	public void testOutsideRangeUtmCoordinateToString() {
		assertEquals("outside valid range", new UTMCoordinate(true).toString());
	}
}