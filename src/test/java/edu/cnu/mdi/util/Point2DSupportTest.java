package edu.cnu.mdi.util;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

public class Point2DSupportTest {

	private static final double TOL = 1.0e-12;

	private static void assertPoint(double x, double y, Point2D.Double p) {
		assertNotNull(p);
		assertEquals(x, p.x, TOL);
		assertEquals(y, p.y, TOL);
	}

	@Test
	public void testPointDelta() {
		Point2D.Double p1 = new Point2D.Double(1, 2);
		Point2D.Double p2 = new Point2D.Double(5, -1);

		Point2D.Double delta = Point2DSupport.pointDelta(p1, p2);

		assertPoint(4, -3, delta);
	}

	@Test
	public void testLengthAndLengthSquare() {
		Point2D.Double p = new Point2D.Double(3, 4);

		assertEquals(25, Point2DSupport.lengthSquare(p), TOL);
		assertEquals(5, Point2DSupport.length(p), TOL);
	}

	@Test
	public void testLengthAndDistanceAvoidIntermediateOverflow() {
		assertTrue(Double.isFinite(Point2DSupport.length(new Point2D.Double(1.0e200, 1.0e200))));
		assertTrue(Double.isFinite(Point2DSupport.distance(
				new Point2D.Double(0, 0), new Point2D.Double(1.0e200, 1.0e200))));
	}

	@Test
	public void testUnitVector() {
		Point2D.Double p = new Point2D.Double(3, 4);

		Point2D.Double u = Point2DSupport.unitVector(p);

		assertPoint(0.6, 0.8, u);
		assertEquals(1.0, Point2DSupport.length(u), TOL);
	}

	@Test
	public void testUnitVectorReturnsNullForZeroVector() {
		assertNull(Point2DSupport.unitVector(new Point2D.Double(0, 0)));
	}

	@Test
	public void testDotProduct() {
		Point2D.Double v1 = new Point2D.Double(1, 2);
		Point2D.Double v2 = new Point2D.Double(3, 4);

		assertEquals(11, Point2DSupport.dot(v1, v2), TOL);
	}

	@Test
	public void testCrossProductSignAndMagnitude() {
		Point2D.Double x = new Point2D.Double(1, 0);
		Point2D.Double y = new Point2D.Double(0, 1);

		assertEquals(1, Point2DSupport.cross(x, y), TOL);
		assertEquals(-1, Point2DSupport.cross(y, x), TOL);
		assertEquals(0, Point2DSupport.cross(x, new Point2D.Double(2, 0)), TOL);
	}

	@Test
	public void testAngleBetween() {
		Point2D.Double x = new Point2D.Double(1, 0);
		Point2D.Double y = new Point2D.Double(0, 1);
		Point2D.Double diagonal = new Point2D.Double(1, 1);

		assertEquals(90, Point2DSupport.angleBetween(x, y), TOL);
		assertEquals(45, Point2DSupport.angleBetween(x, diagonal), TOL);
		assertEquals(0, Point2DSupport.angleBetween(x, x), TOL);
	}

	@Test
	public void testAngleBetweenReturnsZeroForZeroVector() {
		Point2D.Double zero = new Point2D.Double(0, 0);
		Point2D.Double x = new Point2D.Double(1, 0);

		assertEquals(0, Point2DSupport.angleBetween(zero, x), TOL);
		assertEquals(0, Point2DSupport.angleBetween(x, zero), TOL);
	}

	@Test
	public void testProjectOntoAxis() {
		Point2D.Double v = new Point2D.Double(3, 4);
		Point2D.Double xAxis = new Point2D.Double(10, 0);
		Point2D.Double yAxis = new Point2D.Double(0, 5);

		assertPoint(3, 0, Point2DSupport.project(v, xAxis));
		assertPoint(0, 4, Point2DSupport.project(v, yAxis));
	}

	@Test
	public void testProjectOntoDiagonal() {
		Point2D.Double v = new Point2D.Double(2, 0);
		Point2D.Double diagonal = new Point2D.Double(1, 1);

		Point2D.Double projection = Point2DSupport.project(v, diagonal);

		assertPoint(1, 1, projection);
	}

	@Test
	public void testProjectReturnsNullForZeroDirection() {
		Point2D.Double v = new Point2D.Double(3, 4);
		Point2D.Double zero = new Point2D.Double(0, 0);

		assertNull(Point2DSupport.project(v, zero));
	}

	@Test
	public void testPolarAngle() {
		assertEquals(0, Point2DSupport.angle(new Point2D.Double(1, 0)), TOL);
		assertEquals(90, Point2DSupport.angle(new Point2D.Double(0, 1)), TOL);
		assertEquals(180, Point2DSupport.angle(new Point2D.Double(-1, 0)), TOL);
		assertEquals(-90, Point2DSupport.angle(new Point2D.Double(0, -1)), TOL);
	}

	@Test
	public void testDistance() {
		Point2D.Double p0 = new Point2D.Double(1, 2);
		Point2D.Double p1 = new Point2D.Double(4, 6);

		assertEquals(5, Point2DSupport.distance(p0, p1), TOL);
		assertEquals(5, Point2DSupport.distance(p1, p0), TOL);
	}

	@Test
	public void testAzimuth() {
		Point2D.Double origin = new Point2D.Double(0, 0);

		assertEquals(0, Point2DSupport.azimuth(origin, new Point2D.Double(0, 1)), TOL);
		assertEquals(90, Point2DSupport.azimuth(origin, new Point2D.Double(1, 0)), TOL);
		assertEquals(180, Point2DSupport.azimuth(origin, new Point2D.Double(0, -1)), TOL);
		assertEquals(-90, Point2DSupport.azimuth(origin, new Point2D.Double(-1, 0)), TOL);	}

	@Test
	public void testToString() {
		Point2D.Double p = new Point2D.Double(1.23456, -7.89);

		assertEquals("(1.2346 , -7.8900)", Point2DSupport.toString(p));
	}
}
