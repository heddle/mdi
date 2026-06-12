package edu.cnu.mdi.geometry;

/**
 * A mutable point in three-dimensional Cartesian space.
 * <p>
 * The coordinates are public for lightweight geometry use and for compatibility
 * with existing MDI code. The same class is also used in places where a tuple is
 * naturally treated as a vector, for example in dot products, subtraction, and
 * scaling. For operations that should explicitly represent a direction rather
 * than a location, use {@link Vector}.
 * </p>
 *
 * @author heddle
 */
public class Point {

	/** The x coordinate. */
	public double x;

	/** The y coordinate. */
	public double y;

	/** The z coordinate. */
	public double z;

	/**
	 * Creates a point at the origin.
	 */
	public Point() {
		this(0, 0, 0);
	}

	/**
	 * Creates a copy of another point.
	 *
	 * @param p the point to copy
	 * @throws NullPointerException if {@code p} is {@code null}
	 */
	public Point(Point p) {
		this(p.x, p.y, p.z);
	}

	/**
	 * Creates a point from its Cartesian coordinates.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 */
	public Point(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Creates a point from an array containing {@code x}, {@code y}, and {@code z},
	 * in that order.
	 *
	 * @param p an array containing at least three values
	 * @throws NullPointerException      if {@code p} is {@code null}
	 * @throws IndexOutOfBoundsException if {@code p.length < 3}
	 */
	public Point(double[] p) {
		this(p[0], p[1], p[2]);
	}

	/**
	 * Sets this point's coordinates.
	 *
	 * @param x the new x coordinate
	 * @param y the new y coordinate
	 * @param z the new z coordinate
	 */
	public void set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Sets this point's coordinates from another point.
	 *
	 * @param p the point whose coordinates will be copied
	 * @throws NullPointerException if {@code p} is {@code null}
	 */
	public void set(Point p) {
		set(p.x, p.y, p.z);
	}

	/**
	 * Computes the coordinate-wise difference {@code a - b}.
	 *
	 * @param a the minuend point
	 * @param b the subtrahend point
	 * @return a new point containing {@code a - b}
	 * @throws NullPointerException if either argument is {@code null}
	 */
	public static Point difference(Point a, Point b) {
		return new Point(a.x - b.x, a.y - b.y, a.z - b.z);
	}

	/**
	 * Subtracts another point from this point.
	 *
	 * @param other the point to subtract
	 * @return a new point containing {@code this - other}
	 * @throws NullPointerException if {@code other} is {@code null}
	 */
	public Point subtract(Point other) {
		return new Point(x - other.x, y - other.y, z - other.z);
	}

	/**
	 * Computes the coordinate-wise difference {@code a - b} and stores it in an
	 * existing point.
	 *
	 * @param a the minuend point
	 * @param b the subtrahend point
	 * @param c upon return, contains {@code a - b}
	 * @throws NullPointerException if any argument is {@code null}
	 */
	public static void difference(Point a, Point b, Point c) {
		c.set(a.x - b.x, a.y - b.y, a.z - b.z);
	}

	/**
	 * Computes the dot product of this tuple with another tuple.
	 *
	 * @param v the other point or vector
	 * @return {@code this.x*v.x + this.y*v.y + this.z*v.z}
	 * @throws NullPointerException if {@code v} is {@code null}
	 */
	public double dot(Point v) {
		return x * v.x + y * v.y + z * v.z;
	}

	/**
	 * Computes the dot product of two tuples.
	 *
	 * @param a one point or vector
	 * @param b the other point or vector
	 * @return {@code a.x*b.x + a.y*b.y + a.z*b.z}
	 * @throws NullPointerException if either argument is {@code null}
	 */
	public static double dot(Point a, Point b) {
		return a.dot(b);
	}

	/**
	 * Returns a formatted string representation of this point.
	 *
	 * @return a string containing the three coordinates
	 */
	@Override
	public String toString() {
		return String.format("(%-10.6f, %-10.6f, %-10.6f)", x, y, z);
	}

	/**
	 * Computes the Euclidean distance from this point to another set of coordinates.
	 *
	 * @param x the x coordinate of the other point
	 * @param y the y coordinate of the other point
	 * @param z the z coordinate of the other point
	 * @return the Euclidean distance between the two points
	 */
	public double distance(double x, double y, double z) {
		double dx = x - this.x;
		double dy = y - this.y;
		double dz = z - this.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/**
	 * Computes the Euclidean distance from this point to another point.
	 *
	 * @param p the other point
	 * @return the Euclidean distance between the points
	 * @throws NullPointerException if {@code p} is {@code null}
	 */
	public double distance(Point p) {
		return distance(p.x, p.y, p.z);
	}

	/**
	 * Computes the Euclidean distance between two points.
	 *
	 * @param p1 one point
	 * @param p2 the other point
	 * @return the Euclidean distance between the two points
	 * @throws NullPointerException if either argument is {@code null}
	 */
	public static double distance(Point p1, Point p2) {
		return p1.distance(p2);
	}

	/**
	 * Adds another point or vector to this point, coordinate by coordinate.
	 *
	 * @param other the point or vector to add
	 * @return a new point containing {@code this + other}
	 * @throws NullPointerException if {@code other} is {@code null}
	 */
	public Point add(Point other) {
		return new Point(x + other.x, y + other.y, z + other.z);
	}

	/**
	 * Multiplies this point's coordinates by a scalar.
	 *
	 * @param scalar the scalar multiplier
	 * @return a new point containing {@code scalar * this}
	 */
	public Point scale(double scalar) {
		return new Point(x * scalar, y * scalar, z * scalar);
	}
}
