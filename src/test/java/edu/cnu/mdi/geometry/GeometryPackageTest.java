package edu.cnu.mdi.geometry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class GeometryPackageTest {

	private static final double TOL = 1.0e-12;

	private static void assertPoint(double x, double y, double z, Point p) {
		assertEquals(x, p.x, TOL);
		assertEquals(y, p.y, TOL);
		assertEquals(z, p.z, TOL);
	}

	@Test
	public void testPointBasicOperations() {
		Point p = new Point(1, 2, 3);
		Point q = new Point(4, -1, 5);

		assertPoint(-3, 3, -2, Point.difference(p, q));
		assertPoint(5, 1, 8, p.add(q));
		assertPoint(2, 4, 6, p.scale(2));

		assertEquals(17, p.dot(q), TOL);
		assertEquals(Math.sqrt(22), p.distance(q), TOL);
		
		Point copy = new Point(p);
		assertPoint(1, 2, 3, copy);

		copy.set(7, 8, 9);
		assertPoint(7, 8, 9, copy);
		assertPoint(1, 2, 3, p); // copy should not alias original
	}

	@Test
	public void testVectorLengthCrossProductAndUnitVector() {
		Vector v = new Vector(3, 4, 12);

		assertEquals(169, v.lengthSquared(), TOL);
		assertEquals(13, v.length(), TOL);

		Vector i = new Vector(1, 0, 0);
		Vector j = new Vector(0, 1, 0);
		Vector k = Vector.cross(i, j);

		assertPoint(0, 0, 1, k);

		Vector u = v.unitVector();
		assertNotNull(u);
		assertEquals(1.0, u.length(), TOL);
		assertPoint(3.0 / 13.0, 4.0 / 13.0, 12.0 / 13.0, u);

		assertNull(new Vector().unitVector());
	}

	@Test
	public void testVectorCrossAllowsOutputAlias() {
		Vector a = new Vector(1, 0, 0);
		Vector b = new Vector(0, 1, 0);

		Vector.cross(a, b, a);

		assertPoint(0, 0, 1, a);
	}

	@Test
	public void testLineParametricMethods() {
		Line line = new Line(new Point(1, 2, 3), new Point(5, 6, 7));

		assertPoint(1, 2, 3, line.getP0());
		assertPoint(5, 6, 7, line.getP1());
		assertPoint(4, 4, 4, line.getDelP());

		assertPoint(3, 4, 5, line.getP(0.5));
		assertPoint(3, 4, 5, line.getCenter());

		Point out = new Point();
		line.getP(2.0, out);
		assertPoint(9, 10, 11, out);
	}

	@Test
	public void testLineDistanceAndClosestPoint() {
		Line xAxis = new Line(new Point(0, 0, 0), new Point(10, 0, 0));
		Point p = new Point(3, 4, 0);

		assertEquals(4.0, xAxis.distance(p), TOL);
		assertEquals(0.3, xAxis.parameterOfClosestPoint(p), TOL);
		assertPoint(3, 0, 0, xAxis.closestPointOnLine(p));
	}

	@Test
	public void testLineRequiresDistinctPoints() {
		assertThrows(IllegalArgumentException.class,
				() -> new Line(new Point(1, 2, 3), new Point(1, 2, 3)));

		assertThrows(IllegalArgumentException.class,
				() -> new Line(new Vector(0, 0, 0)));
	}

	@Test
	public void testPlaneDistancesAndSigns() {
		Plane plane = new Plane(0, 0, 2, 10); // z = 5

		assertEquals(0.0, plane.signedDistance(1, 2, 5), TOL);
		assertEquals(1.0, plane.signedDistance(1, 2, 6), TOL);
		assertEquals(-1.0, plane.signedDistance(1, 2, 4), TOL);
		assertEquals(1.0, plane.distance(1, 2, 4), TOL);

		assertEquals(1, plane.sign(0, 0, 6));
		assertEquals(-1, plane.sign(0, 0, 4));
		assertEquals(0, plane.sign(0, 0, 5));
	}

	@Test
	public void testPlaneLineIntersection() {
		Plane plane = new Plane(0, 0, 1, 5); // z = 5
		Line line = new Line(new Point(1, 2, 0), new Point(1, 2, 10));

		Point intersection = new Point();
		double t = plane.lineIntersection(line, intersection);

		assertEquals(0.5, t, TOL);
		assertPoint(1, 2, 5, intersection);
	}

	@Test
	public void testPlaneParallelLineIntersectionReturnsNaN() {
		Plane plane = new Plane(0, 0, 1, 5); // z = 5
		Line line = new Line(new Point(0, 0, 1), new Point(1, 0, 1));

		Point intersection = new Point();
		double t = plane.lineIntersection(line, intersection);

		assertTrue(Double.isNaN(t));
		assertTrue(Double.isNaN(intersection.x));
		assertTrue(Double.isNaN(intersection.y));
		assertTrue(Double.isNaN(intersection.z));
	}

	@Test
	public void testPlaneConstantPhiPlane() {
		Plane phi0 = Plane.constantPhiPlane(0);
		assertEquals(0.0, phi0.signedDistance(3, 0, 7), TOL);
		assertEquals(0.0, phi0.signedDistance(-3, 0, -7), TOL);

		Plane phi90 = Plane.constantPhiPlane(90);
		assertEquals(0.0, phi90.signedDistance(0, 3, 7), TOL);
		assertEquals(0.0, phi90.signedDistance(0, -3, -7), TOL);
	}

	@Test
	public void testPlaneRejectsZeroNormal() {
		assertThrows(IllegalArgumentException.class,
				() -> new Plane(0, 0, 0, 1));

		assertThrows(IllegalArgumentException.class,
				() -> new Plane(new Vector(0, 0, 0), new Point(1, 2, 3)));
	}

	@Test
	public void testSphereDistancesInsideAndSegments() {
		Sphere sphere = new Sphere(new Point(1, 2, 3), 5);

		assertEquals(-5.0, sphere.signedDistance(new Point(1, 2, 3)), TOL);
		assertEquals(0.0, sphere.signedDistance(6, 2, 3), TOL);
		assertEquals(1.0, sphere.signedDistance(7, 2, 3), TOL);

		assertTrue(sphere.isInside(1, 2, 3));
		assertFalse(sphere.isInside(6, 2, 3));

		Sphere unit = new Sphere(1);
		assertTrue(unit.segmentIntersects(-2, 0, 0, 2, 0, 0));      // through sphere
		assertTrue(unit.segmentIntersects(-2, 1, 0, 2, 1, 0));      // tangent
		assertFalse(unit.segmentIntersects(-2, 1.1, 0, 2, 1.1, 0)); // misses
	}

	@Test
	public void testSphereRejectsNegativeRadius() {
		assertThrows(IllegalArgumentException.class,
				() -> new Sphere(-1));

		assertThrows(IllegalArgumentException.class,
				() -> new Sphere(new Point(0, 0, 0), -1));
	}

	@Test
	public void testCylinderDistancesInsideAndCenteredOnZ() {
		Cylinder cylinder = new Cylinder(
				new Line(new Point(0, 0, -10), new Point(0, 0, 10)), 2);

		assertEquals(-2.0, cylinder.signedDistance(0, 0, 0), TOL);
		assertEquals(-1.0, cylinder.signedDistance(1, 0, 5), TOL);
		assertEquals(0.0, cylinder.signedDistance(2, 0, -5), TOL);
		assertEquals(1.0, cylinder.signedDistance(3, 0, 100), TOL);

		assertTrue(cylinder.isInside(1, 0, 0));
		assertFalse(cylinder.isInside(2, 0, 0));
		assertTrue(cylinder.centeredOnZ());

		Cylinder offAxis = new Cylinder(
				new Line(new Point(1, 0, 0), new Point(1, 0, 10)), 2);
		assertFalse(offAxis.centeredOnZ());
	}

	@Test
	public void testCylinderRejectsNegativeRadius() {
		Line zAxis = new Line(new Point(0, 0, 0), new Point(0, 0, 1));

		assertThrows(IllegalArgumentException.class,
				() -> new Cylinder(zAxis, -1));
	}

	@Test
	public void testGeoUtilTinyAndArrayUtilities() {
		assertTrue(GeoUtil.tiny(0.5 * GeoUtil.TINY));
		assertFalse(GeoUtil.tiny(GeoUtil.TINY));

		assertArrayEquals(
				new double[] { 2, 4, 6 },
				GeoUtil.scalarMultiply(new double[] { 1, 2, 3 }, 2),
				TOL);

		assertArrayEquals(
				new double[] { 5, 7, 9 },
				GeoUtil.addVectors(
						new double[] { 1, 2, 3 },
						new double[] { 4, 5, 6 }),
				TOL);

		assertArrayEquals(
				new double[] { 12, 15, 18 },
				GeoUtil.addVectors(
						new double[] { 1, 2, 3 },
						new double[] { 4, 5, 6 },
						new double[] { 7, 8, 9 }),
				TOL);

		assertThrows(IllegalArgumentException.class,
				() -> GeoUtil.addVectors());

		assertThrows(IllegalArgumentException.class,
				() -> GeoUtil.addVectors(
						new double[] { 1, 2 },
						new double[] { 1, 2, 3 }));
	}
}