package edu.cnu.mdi.geometry;

/**
 * A three-dimensional parametric line, represented as
 * {@code p(t) = p0 + t * dp}.
 * <p>
 * The same representation can be used as an infinite line or as a directed line
 * segment. For an infinite line, {@code t} may be any real value. For the
 * directed segment from {@code p0} to {@code p1}, the segment corresponds to
 * {@code 0 <= t <= 1}.
 * </p>
 *
 * @author heddle
 */
public class Line {

	/** A point on the line; also the start point if treated as a segment. */
	private final Point _po;

	/** The direction vector, equal to {@code p1 - p0}. */
	private final Vector _dp;

	/** The length of {@link #_dp}. */
	private final double _dpLen;

	/**
	 * Creates a line from two points.
	 * <p>
	 * If the line is interpreted as a directed segment, it runs from {@code po} to
	 * {@code p1}.
	 * </p>
	 *
	 * @param po one point on the line
	 * @param p1 another point on the line
	 * @throws NullPointerException     if either point is {@code null}
	 * @throws IllegalArgumentException if the two points are coincident, non-finite,
	 *                                  or too close to define a stable direction
	 */
	public Line(Point po, Point p1) {
		_po = new Point(po);
		_dp = new Vector(Point.difference(p1, po));
		_dpLen = _dp.length();
		if (!Double.isFinite(_dpLen) || _dpLen < GeoUtil.TINY) {
			throw new IllegalArgumentException("A line requires two distinct points.");
		}
	}

	/**
	 * Creates a line from two arrays containing {@code x}, {@code y}, and {@code z}
	 * values.
	 *
	 * @param p1 one point as an xyz array
	 * @param p2 another point as an xyz array
	 * @throws NullPointerException      if either array is {@code null}
	 * @throws IndexOutOfBoundsException if either array has fewer than three values
	 * @throws IllegalArgumentException  if the two points are coincident or too
	 *                                   close to define a stable direction
	 */
	public Line(double[] p1, double[] p2) {
		this(new Point(p1), new Point(p2));
	}

	/**
	 * Creates a copy of another line.
	 *
	 * @param line the line to copy
	 * @throws NullPointerException if {@code line} is {@code null}
	 */
	public Line(Line line) {
		_po = new Point(line._po);
		_dp = new Vector(line._dp);
		_dpLen = line._dpLen;
	}

	/**
	 * Creates a line through the origin in the direction of a vector.
	 *
	 * @param v the direction vector
	 * @throws NullPointerException     if {@code v} is {@code null}
	 * @throws IllegalArgumentException if {@code v} is the zero vector or too close
	 *                                  to zero to define a stable direction
	 */
	public Line(Vector v) {
		this(new Point(0, 0, 0), new Point(v.x, v.y, v.z));
	}

	/**
	 * Gets the line's base point.
	 * <p>
	 * This is an arbitrary point on an infinite line and the start point if this
	 * object is interpreted as a directed segment.
	 * </p>
	 *
	 * @return a copy of the base point
	 */
	public Point getP0() {
		return new Point(_po);
	}

	/**
	 * Gets the line direction vector {@code dp = p1 - p0}.
	 *
	 * @return a copy of the direction vector
	 */
	public Vector getDelP() {
		return new Vector(_dp);
	}

	/**
	 * Gets the endpoint corresponding to {@code t = 1}.
	 * <p>
	 * This is an arbitrary second point on an infinite line and the end point if
	 * this object is interpreted as a directed segment.
	 * </p>
	 *
	 * @return the point {@code p0 + dp}
	 */
	public Point getP1() {
		return new Point(_po.x + _dp.x, _po.y + _dp.y, _po.z + _dp.z);
	}

	/**
	 * Gets a point on the parametric line.
	 *
	 * @param t the line parameter; use {@code 0 <= t <= 1} for the finite directed
	 *          segment
	 * @return the point {@code p0 + t*dp}
	 */
	public Point getP(double t) {
		Point p = new Point();
		getP(t, p);
		return p;
	}

	/**
	 * Gets a point on the parametric line and stores it in an existing point.
	 *
	 * @param t the line parameter; use {@code 0 <= t <= 1} for the finite directed
	 *          segment
	 * @param p upon return, contains {@code p0 + t*dp}
	 * @throws NullPointerException if {@code p} is {@code null}
	 */
	public void getP(double t, Point p) {
		p.x = _po.x + t * _dp.x;
		p.y = _po.y + t * _dp.y;
		p.z = _po.z + t * _dp.z;
	}

	/**
	 * Computes the shortest distance from this infinite line to a point.
	 * <p>
	 * This method ignores segment endpoints. To use the finite segment distance,
	 * clamp the parameter returned by {@link #parameterOfClosestPoint(Point)} to the
	 * interval {@code [0, 1]}.
	 * </p>
	 *
	 * @param p the point
	 * @return the perpendicular distance from {@code p} to the infinite line
	 * @throws NullPointerException if {@code p} is {@code null}
	 */
	public double distance(Point p) {
		Vector ap = new Vector(Point.difference(p, _po));
		Vector c = Vector.cross(ap, _dp);
		return c.length() / _dpLen;
	}

	/**
	 * Computes the line parameter of the point on this infinite line closest to a
	 * given point.
	 *
	 * @param p the point
	 * @return the parameter {@code t} for the closest point {@code p0 + t*dp}
	 * @throws NullPointerException if {@code p} is {@code null}
	 */
	public double parameterOfClosestPoint(Point p) {
		Point pointVec = p.subtract(_po);
		return pointVec.dot(_dp) / _dp.dot(_dp);
	}

	/**
	 * Finds the point on this infinite line closest to a given point.
	 *
	 * @param p the given point
	 * @return the closest point on the infinite line
	 * @throws NullPointerException if {@code p} is {@code null}
	 */
	public Point closestPointOnLine(Point p) {
		return getP(parameterOfClosestPoint(p));
	}

	/**
	 * Returns a string representation of this line.
	 *
	 * @return a string containing the two defining points
	 */
	@Override
	public String toString() {
		return "Line from " + getP0() + " to " + getP1();
	}

	/**
	 * Gets the midpoint of the directed segment from {@code p0} to {@code p1}.
	 *
	 * @return the point corresponding to {@code t = 0.5}
	 */
	public Point getCenter() {
		return getP(0.5);
	}
}
