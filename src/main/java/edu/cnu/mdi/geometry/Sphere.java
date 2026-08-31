package edu.cnu.mdi.geometry;

/**
 * A sphere in three-dimensional Cartesian space.
 * <p>
 * Distances are measured relative to the sphere surface: signed distances are
 * negative inside the sphere, zero on the surface, and positive outside the
 * sphere.
 * </p>
 *
 * @author heddle
 */
public class Sphere {

	/** The center of the sphere. */
	private final Point _center;

	/** The radius of the sphere. */
	private final double _radius;

	/**
	 * Creates a sphere with the given center and radius.
	 *
	 * @param center the center of the sphere
	 * @param radius the radius of the sphere; must be non-negative
	 * @throws NullPointerException     if {@code center} is {@code null}
	 * @throws IllegalArgumentException if {@code radius} is negative or non-finite,
	 *                                  or a center coordinate is non-finite
	 */
	public Sphere(Point center, double radius) {
		if (!Double.isFinite(radius) || radius < 0) {
			throw new IllegalArgumentException("Sphere radius must be finite and non-negative.");
		}
		if (!Double.isFinite(center.x) || !Double.isFinite(center.y)
				|| !Double.isFinite(center.z)) {
			throw new IllegalArgumentException("Sphere center coordinates must be finite.");
		}
		_center = new Point(center);
		_radius = radius;
	}

	/**
	 * Creates a sphere from an xyz center array and a radius.
	 *
	 * @param center an array containing the center coordinates {@code x}, {@code y},
	 *               and {@code z}
	 * @param radius the radius of the sphere; must be non-negative
	 * @throws NullPointerException      if {@code center} is {@code null}
	 * @throws IndexOutOfBoundsException if {@code center.length < 3}
	 * @throws IllegalArgumentException  if {@code radius} is negative or non-finite,
	 *                                   or a center coordinate is non-finite
	 */
	public Sphere(double[] center, double radius) {
		this(new Point(center), radius);
	}

	/**
	 * Creates a sphere centered at the origin.
	 *
	 * @param radius the radius of the sphere; must be non-negative
	 * @throws IllegalArgumentException if {@code radius} is negative or non-finite
	 */
	public Sphere(double radius) {
		this(new Point(0, 0, 0), radius);
	}

	/**
	 * Gets the center of the sphere.
	 *
	 * @return a copy of the center point
	 */
	public Point getCenter() {
		return new Point(_center);
	}

	/**
	 * Gets the radius of the sphere.
	 *
	 * @return the radius of the sphere
	 */
	public double getRadius() {
		return _radius;
	}

	/**
	 * Computes the signed shortest distance from a point to the sphere surface.
	 *
	 * @param p the point
	 * @return negative inside the sphere, zero on the surface, and positive outside
	 * @throws NullPointerException if {@code p} is {@code null}
	 */
	public double signedDistance(Point p) {
		double centDist = _center.distance(p);
		return centDist - _radius;
	}

	/**
	 * Computes the signed shortest distance from coordinates to the sphere surface.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return negative inside the sphere, zero on the surface, and positive outside
	 */
	public double signedDistance(double x, double y, double z) {
		return signedDistance(new Point(x, y, z));
	}

	/**
	 * Computes the absolute shortest distance from coordinates to the sphere surface.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return the absolute distance to the sphere surface
	 */
	public double distance(double x, double y, double z) {
		return Math.abs(signedDistance(x, y, z));
	}

	/**
	 * Tests whether coordinates are strictly inside the sphere.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return {@code true} if the point is strictly inside the sphere
	 */
	public boolean isInside(double x, double y, double z) {
		return signedDistance(x, y, z) < 0;
	}

	/**
	 * Tests whether a finite line segment intersects the sphere.
	 * <p>
	 * Tangency counts as intersection.
	 * </p>
	 *
	 * @param x1 the x coordinate of one segment endpoint
	 * @param y1 the y coordinate of one segment endpoint
	 * @param z1 the z coordinate of one segment endpoint
	 * @param x2 the x coordinate of the other segment endpoint
	 * @param y2 the y coordinate of the other segment endpoint
	 * @param z2 the z coordinate of the other segment endpoint
	 * @return {@code true} if the segment intersects or is tangent to the sphere
	 */
	public boolean segmentIntersects(double x1, double y1, double z1, double x2, double y2, double z2) {
		return distToSegment(_center.x, _center.y, _center.z, x1, y1, z1, x2, y2, z2) <= _radius;
	}

	/**
	 * Computes the shortest distance from a point to a finite line segment.
	 *
	 * @param px the x coordinate of the point
	 * @param py the y coordinate of the point
	 * @param pz the z coordinate of the point
	 * @param x1 the x coordinate of one segment endpoint
	 * @param y1 the y coordinate of one segment endpoint
	 * @param z1 the z coordinate of one segment endpoint
	 * @param x2 the x coordinate of the other segment endpoint
	 * @param y2 the y coordinate of the other segment endpoint
	 * @param z2 the z coordinate of the other segment endpoint
	 * @return the shortest distance from the point to the segment
	 */
	private double distToSegment(double px, double py, double pz, double x1, double y1, double z1, double x2, double y2,
			double z2) {

		double lineDistSq = distSq(x1, y1, z1, x2, y2, z2);
		if (lineDistSq == 0) {
			return Math.sqrt(distSq(px, py, pz, x1, y1, z1));
		}
		double t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1) + (pz - z1) * (z2 - z1)) / lineDistSq;
		t = Math.max(0, Math.min(1, t));
		return Math.sqrt(distSq(px, py, pz, x1 + t * (x2 - x1), y1 + t * (y2 - y1), z1 + t * (z2 - z1)));
	}

	/**
	 * Computes the square of the Euclidean distance between two coordinate triples.
	 *
	 * @param x1 the x coordinate of the first point
	 * @param y1 the y coordinate of the first point
	 * @param z1 the z coordinate of the first point
	 * @param x2 the x coordinate of the second point
	 * @param y2 the y coordinate of the second point
	 * @param z2 the z coordinate of the second point
	 * @return the squared distance between the points
	 */
	private double distSq(double x1, double y1, double z1, double x2, double y2, double z2) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		double dz = z2 - z1;
		return dx * dx + dy * dy + dz * dz;
	}
}
