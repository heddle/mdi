package edu.cnu.mdi.geometry;

/**
 * An infinite circular cylinder in three-dimensional Cartesian space.
 * <p>
 * The cylinder is defined by an infinite center line and a radius. Distances are
 * measured relative to the cylinder surface: signed distances are negative
 * inside the cylinder, zero on the surface, and positive outside the cylinder.
 * </p>
 *
 * @author heddle
 */
public class Cylinder {

	/** The infinite center line of the cylinder. */
	private final Line _centerLine;

	/** The cylinder radius. */
	private final double _radius;

	/**
	 * Creates an infinite cylinder.
	 *
	 * @param centerLine the cylinder center line
	 * @param radius     the cylinder radius; must be non-negative
	 * @throws NullPointerException     if {@code centerLine} is {@code null}
	 * @throws IllegalArgumentException if {@code radius} is negative or non-finite
	 */
	public Cylinder(Line centerLine, double radius) {
		if (!Double.isFinite(radius) || radius < 0) {
			throw new IllegalArgumentException("Cylinder radius must be finite and non-negative.");
		}
		_centerLine = new Line(centerLine);
		_radius = radius;
	}

	/**
	 * Creates an infinite cylinder from two points on its center line.
	 *
	 * @param p1     one point on the center line as an xyz array
	 * @param p2     another point on the center line as an xyz array
	 * @param radius the cylinder radius; must be non-negative
	 * @throws NullPointerException      if either array is {@code null}
	 * @throws IndexOutOfBoundsException if either array has fewer than three values
	 * @throws IllegalArgumentException  if {@code radius} is negative or non-finite,
	 *                                   or if the two center
	 *                                   points do not define a stable line
	 */
	public Cylinder(double[] p1, double[] p2, double radius) {
		this(new Line(p1, p2), radius);
	}

	/**
	 * Gets the center line of the cylinder.
	 *
	 * @return a copy of the center line
	 */
	public Line getCenterLine() {
		return new Line(_centerLine);
	}

	/**
	 * Gets the radius of the cylinder.
	 *
	 * @return the radius of the cylinder
	 */
	public double getRadius() {
		return _radius;
	}

	/**
	 * Computes the signed shortest distance from a point to the cylinder surface.
	 *
	 * @param p the point
	 * @return negative inside the cylinder, zero on the surface, and positive outside
	 * @throws NullPointerException if {@code p} is {@code null}
	 */
	public double signedDistance(Point p) {
		double lineDist = _centerLine.distance(p);
		return lineDist - _radius;
	}

	/**
	 * Computes the signed shortest distance from coordinates to the cylinder surface.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return negative inside the cylinder, zero on the surface, and positive outside
	 */
	public double signedDistance(double x, double y, double z) {
		return signedDistance(new Point(x, y, z));
	}

	/**
	 * Computes the absolute shortest distance from coordinates to the cylinder
	 * surface.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return the absolute distance to the cylinder surface
	 */
	public double distance(double x, double y, double z) {
		return Math.abs(signedDistance(x, y, z));
	}

	/**
	 * Tests whether a point is strictly inside the cylinder.
	 *
	 * @param p the point
	 * @return {@code true} if the point is strictly inside the cylinder
	 * @throws NullPointerException if {@code p} is {@code null}
	 */
	public boolean isInside(Point p) {
		return signedDistance(p) < 0;
	}

	/**
	 * Tests whether coordinates are strictly inside the cylinder.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return {@code true} if the point is strictly inside the cylinder
	 */
	public boolean isInside(double x, double y, double z) {
		return signedDistance(x, y, z) < 0;
	}

	/**
	 * Tests whether the cylinder center line lies on the z axis.
	 *
	 * @return {@code true} if both defining center-line points have x and y
	 *         effectively zero
	 */
	public boolean centeredOnZ() {
		Point p0 = _centerLine.getP0();
		Point p1 = _centerLine.getP1();
		return GeoUtil.tiny(p0.x) && GeoUtil.tiny(p0.y) && GeoUtil.tiny(p1.x) && GeoUtil.tiny(p1.y);
	}
}
