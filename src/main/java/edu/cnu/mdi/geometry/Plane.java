package edu.cnu.mdi.geometry;

/**
 * A plane in three-dimensional Cartesian space.
 * <p>
 * The plane is stored in the form {@code a*x + b*y + c*z = d}. It can also be
 * viewed as {@code (r - r0) dot n = 0}, where {@code r0} is a point on the plane
 * and {@code n = (a, b, c)} is a normal vector. The sign of the signed distance
 * is positive on the side pointed to by the normal.
 * </p>
 *
 * @author heddle
 */
public class Plane {

	/** The {@code a} coefficient in {@code a*x + b*y + c*z = d}. */
	public final double a;

	/** The {@code b} coefficient in {@code a*x + b*y + c*z = d}. */
	public final double b;

	/** The {@code c} coefficient in {@code a*x + b*y + c*z = d}. */
	public final double c;

	/** The {@code d} coefficient in {@code a*x + b*y + c*z = d}. */
	public final double d;

	/** Length of the normal vector, used to normalize signed distances. */
	private final double _denom;

	/**
	 * Creates a plane from a normal vector and a point on the plane.
	 * <p>
	 * The stored normal is normalized to unit length.
	 * </p>
	 *
	 * @param anorm the plane normal vector
	 * @param p0    a point on the plane
	 * @throws NullPointerException     if either argument is {@code null}
	 * @throws IllegalArgumentException if either argument contains non-finite values,
	 *                                  or the normal is too close to zero to normalize
	 */
	public Plane(Vector anorm, Point p0) {
		if (!Double.isFinite(anorm.x) || !Double.isFinite(anorm.y)
				|| !Double.isFinite(anorm.z) || !Double.isFinite(p0.x)
				|| !Double.isFinite(p0.y) || !Double.isFinite(p0.z)) {
			throw new IllegalArgumentException("Plane normal and point coordinates must be finite.");
		}
		Vector norm = anorm.unitVector();
		if (norm == null) {
			throw new IllegalArgumentException("A plane normal must be nonzero.");
		}
		a = norm.x;
		b = norm.y;
		c = norm.z;
		d = a * p0.x + b * p0.y + c * p0.z;
		_denom = 1.0;
	}

	/**
	 * Creates a plane from the coefficients of {@code a*x + b*y + c*z = d}.
	 *
	 * @param a the x coefficient
	 * @param b the y coefficient
	 * @param c the z coefficient
	 * @param d the right-hand side coefficient
	 * @throws IllegalArgumentException if a coefficient is non-finite, or if
	 *                                  {@code a}, {@code b}, and {@code c} are all
	 *                                  too close to zero to define a plane
	 */
	public Plane(double a, double b, double c, double d) {
		if (!Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(c)
				|| !Double.isFinite(d)) {
			throw new IllegalArgumentException("Plane coefficients must be finite.");
		}
		if (GeoUtil.tiny(a) && GeoUtil.tiny(b) && GeoUtil.tiny(c)) {
			throw new IllegalArgumentException("At least one plane normal coefficient must be nonzero.");
		}
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		_denom = Math.sqrt(a * a + b * b + c * c);
	}

	/**
	 * Creates a plane from array-based normal and point values.
	 *
	 * @param norm  an array containing the normal components {@code nx}, {@code ny},
	 *              and {@code nz}
	 * @param point an array containing point coordinates {@code x}, {@code y}, and
	 *              {@code z}
	 * @throws NullPointerException      if either array is {@code null}
	 * @throws IndexOutOfBoundsException if either array has fewer than three values
	 * @throws IllegalArgumentException  if the normal vector is zero or too close to
	 *                                   zero to normalize
	 */
	public Plane(double norm[], double point[]) {
		this(new Vector(norm[0], norm[1], norm[2]), new Point(point[0], point[1], point[2]));
	}

	/**
	 * Creates a plane from a normal vector and a point on the plane.
	 *
	 * @param nx x component of the normal vector
	 * @param ny y component of the normal vector
	 * @param nz z component of the normal vector
	 * @param px x coordinate of a point on the plane
	 * @param py y coordinate of a point on the plane
	 * @param pz z coordinate of a point on the plane
	 * @throws IllegalArgumentException if the normal vector is zero or too close to
	 *                                  zero to normalize
	 */
	public Plane(double nx, double ny, double nz, double px, double py, double pz) {
		this(new Vector(nx, ny, nz), new Point(px, py, pz));
	}

	/**
	 * Creates a line through two points and computes the line-plane intersection.
	 *
	 * @param p1 one point defining the line
	 * @param p2 another point defining the line
	 * @param p  upon return, contains the intersection point; set to NaN components
	 *           if the line is parallel to the plane
	 * @return the line parameter {@code t}; {@code NaN} means the line is parallel
	 *         to the plane. If {@code 0 <= t <= 1}, the finite segment intersects
	 *         the plane. If {@code t} is outside {@code [0, 1]}, the infinite line
	 *         intersects the plane but the finite segment does not.
	 * @throws NullPointerException     if any point argument is {@code null}
	 * @throws IllegalArgumentException if {@code p1} and {@code p2} are coincident
	 *                                  or too close to define a stable line
	 */
	public double interpolate(Point p1, Point p2, Point p) {
		Line line = new Line(p1, p2);
		return lineIntersection(line, p);
	}

	/**
	 * Computes the absolute distance from coordinates to the plane.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return the absolute distance to the plane
	 */
	public double distance(double x, double y, double z) {
		return Math.abs(signedDistance(x, y, z));
	}

	/**
	 * Computes the signed distance from coordinates to the plane.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return the signed distance; positive is the side pointed to by the normal
	 */
	public double signedDistance(double x, double y, double z) {
		return (a * x + b * y + c * z - d) / _denom;
	}

	/**
	 * Computes the intersection of an infinite line with this plane.
	 *
	 * @param line         the line to intersect with this plane
	 * @param intersection upon return, contains the intersection point; set to NaN
	 *                     components if the line is parallel to the plane
	 * @return the line parameter {@code t}; {@code NaN} means the line is parallel
	 *         to the plane. If {@code 0 <= t <= 1}, the finite segment intersects
	 *         the plane. If {@code t} is outside {@code [0, 1]}, the infinite line
	 *         intersects the plane but the finite segment does not.
	 * @throws NullPointerException if either argument is {@code null}
	 */
	public double lineIntersection(Line line, Point intersection) {
		Vector lineDir = line.getDelP();
		Point p0 = line.getP0();

		double dotProduct = a * lineDir.x + b * lineDir.y + c * lineDir.z;
		if (GeoUtil.tiny(dotProduct)) {
			intersection.set(Double.NaN, Double.NaN, Double.NaN);
			return Double.NaN;
		}

		double t = (d - a * p0.x - b * p0.y - c * p0.z) / dotProduct;
		line.getP(t, intersection);
		return t;
	}

	/**
	 * Computes the side of the plane on which coordinates lie.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return {@code +1} if {@code a*x + b*y + c*z > d}, {@code -1} if it is less
	 *         than {@code d}, and {@code 0} if it is exactly equal
	 */
	public int sign(double x, double y, double z) {
		double result = a * x + b * y + c * z;
		if (result > d) {
			return +1;
		} else if (result < d) {
			return -1;
		} else {
			return 0;
		}
	}

	/**
	 * Creates the vertical plane of constant azimuthal angle {@code phi}.
	 * <p>
	 * The angle is measured in degrees in the usual xy-plane sense: {@code phi = 0}
	 * is the positive x axis and {@code phi = 90} is the positive y axis. The plane
	 * contains the z axis and the ray at the given azimuth.
	 * </p>
	 *
	 * @param phi the azimuthal angle in degrees
	 * @return the plane of constant phi
	 */
	public static Plane constantPhiPlane(double phi) {
		phi = Math.toRadians(phi);

		double cphi = Math.cos(phi);
		double sphi = Math.sin(phi);

		Point p = new Point(cphi, sphi, 0);
		Vector norm = new Vector(sphi, -cphi, 0);

		return new Plane(norm, p);
	}

	/**
	 * Returns a string representation of this plane's coefficients.
	 *
	 * @return a string containing {@code a}, {@code b}, {@code c}, and {@code d}
	 */
	@Override
	public String toString() {
		return String.format("abcd = [%10.6G, %10.6G, %10.6G, %10.6G]", a, b, c, d);
	}

	/**
	 * Computes four vertices that can be used to draw a quadrilateral patch of this
	 * plane.
	 * <p>
	 * The returned array contains four xyz triples, suitable for JOGL-style vertex
	 * submission. The quadrilateral is not a mathematically bounded plane; it is a
	 * drawing convenience whose size is controlled by {@code scale}.
	 * </p>
	 *
	 * @param scale a drawing scale, typically larger than the scene extent
	 * @return an array of twelve floats containing four xyz vertices
	 */
	public float[] planeQuadCoordinates(float scale) {

		int[] i1 = { -1, -1, 1, 1 };
		int[] i2 = { -1, 1, 1, -1 };

		float[] coords = new float[12];

		if (GeoUtil.tiny(b) && GeoUtil.tiny(c)) {
			float fx = (float) (d / a);
			for (int k = 0; k < 4; k++) {
				int j = 3 * k;
				coords[j] = fx;
				coords[j + 1] = scale * i1[k];
				coords[j + 2] = scale * i2[k];
			}
		} else if (GeoUtil.tiny(a) && GeoUtil.tiny(c)) {
			float fy = (float) (d / b);
			for (int k = 0; k < 4; k++) {
				int j = 3 * k;
				coords[j] = scale * i1[k];
				coords[j + 1] = fy;
				coords[j + 2] = scale * i2[k];
			}
		} else if (GeoUtil.tiny(a) && GeoUtil.tiny(b)) {
			float fz = (float) (d / c);
			for (int k = 0; k < 4; k++) {
				int j = 3 * k;
				coords[j] = scale * i1[k];
				coords[j + 1] = scale * i2[k];
				coords[j + 2] = fz;
			}
		} else if (GeoUtil.tiny(a)) {
			for (int k = 0; k < 4; k++) {
				int j = 3 * k;
				float x = scale * i1[k];
				float y = scale * i2[k];
				float z = (float) ((d - b * y) / c);
				coords[j] = x;
				coords[j + 1] = y;
				coords[j + 2] = z;
			}
		} else if (GeoUtil.tiny(b)) {
			for (int k = 0; k < 4; k++) {
				int j = 3 * k;
				float x = scale * i1[k];
				float y = scale * i2[k];
				float z = (float) ((d - a * x) / c);
				coords[j] = x;
				coords[j + 1] = y;
				coords[j + 2] = z;
			}
		} else if (GeoUtil.tiny(c)) {
			for (int k = 0; k < 4; k++) {
				int j = 3 * k;
				float x = scale * i1[k];
				float z = scale * i2[k];
				float y = (float) ((d - a * x) / b);
				coords[j] = x;
				coords[j + 1] = y;
				coords[j + 2] = z;
			}
		} else {
			for (int k = 0; k < 4; k++) {
				int j = 3 * k;
				float x = scale * i1[k];
				float y = scale * i2[k];
				float z = (float) ((d - a * x - b * y) / c);
				coords[j] = x;
				coords[j + 1] = y;
				coords[j + 2] = z;
			}
		}

		return coords;
	}
}
