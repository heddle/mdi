package edu.cnu.mdi.geometry;

/**
 * A mutable three-dimensional Cartesian vector.
 * <p>
 * This class extends {@link Point} so that existing MDI code can use the same
 * public {@code x}, {@code y}, and {@code z} fields for points and vectors. The
 * distinction is semantic: a {@code Point} represents a location, while a
 * {@code Vector} represents a displacement or direction.
 * </p>
 *
 * @author heddle
 */
public class Vector extends Point {

	/**
	 * Creates the zero vector.
	 */
	public Vector() {
		super();
	}

	/**
	 * Creates a vector from a point-like tuple.
	 *
	 * @param p the tuple whose components will be copied
	 * @throws NullPointerException if {@code p} is {@code null}
	 */
	public Vector(Point p) {
		this(p.x, p.y, p.z);
	}

	/**
	 * Creates a vector from its Cartesian components.
	 *
	 * @param x the x component
	 * @param y the y component
	 * @param z the z component
	 */
	public Vector(double x, double y, double z) {
		super(x, y, z);
	}

	/**
	 * Computes the square of this vector's Euclidean length.
	 *
	 * @return {@code x*x + y*y + z*z}
	 */
	public double lengthSquared() {
		return x * x + y * y + z * z;
	}

	/**
	 * Computes this vector's Euclidean length.
	 *
	 * @return the vector length
	 */
	public double length() {
		return Math.sqrt(lengthSquared());
	}

	/**
	 * Computes the cross product {@code a x b}.
	 *
	 * @param a the first vector
	 * @param b the second vector
	 * @return a new vector containing {@code a x b}
	 * @throws NullPointerException if either argument is {@code null}
	 */
	public static Vector cross(Vector a, Vector b) {
		Vector c = new Vector();
		cross(a, b, c);
		return c;
	}

	/**
	 * Computes the cross product {@code a x b} and stores it in an existing vector.
	 * <p>
	 * All three result components are computed into temporaries before any are
	 * written, so the output vector may safely be the same object as either input.
	 * </p>
	 *
	 * @param a the first vector
	 * @param b the second vector
	 * @param c upon return, contains {@code a x b}
	 * @throws NullPointerException if any argument is {@code null}
	 */
	public static void cross(Vector a, Vector b, Vector c) {
		double cx = a.y * b.z - a.z * b.y;
		double cy = a.z * b.x - a.x * b.z;
		double cz = a.x * b.y - a.y * b.x;
		c.set(cx, cy, cz);
	}

	/**
	 * Computes a unit vector in the same direction as this vector.
	 *
	 * @return a new unit vector, or {@code null} if this vector is too close to zero
	 *         to normalize
	 */
	public Vector unitVector() {
		double len = length();
		if (len < GeoUtil.TINY) {
			return null;
		}
		return new Vector(x / len, y / len, z / len);
	}
}