package edu.cnu.mdi.util;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Comparator;

public class MathUtils {

	// small number test
	private static final double TINY = 1.0e-16;

	/**
	 * Get an int as an unsigned long
	 *
	 * @param x the int
	 * @return the unsigned long
	 */
	public static long getUnsignedInt(int x) {
		return x & 0x00000000ffffffffL;
	}

	/**
	 * Given two points p0 and p1, imagine a line from p0 to p1. Take the line to be
	 * parameterized by parameter t so that at t = 0 we are at p0 and t = 1 we are
	 * at p1.
	 *
	 * @param p0         start point of main line
	 * @param p1         end point of main line
	 * @param wp         the point from which we drop a perpendicular to p0 -> p1
	 * @param pintersect the intersection point of the perpendicular and the line
	 *                   containing p0-p1. It may or may not actually be between p0
	 *                   and p1, as specified by the value of t.
	 * @return the perpendicular distance to the line. If t is between 0 and 1 the
	 *         intersection is on the line. If t < 0 the intersection is on the
	 *         "infinite line" but not on p0->p1, it is on the p0 side; this returns
	 *         the distance to p0. If t > 1 the intersection is on the p1 side; this
	 *         returns the distance to p1.
	 */
	public static double perpendicularDistance(Point2D.Double p0, Point2D.Double p1, Point2D.Double wp,
			Point2D.Double pintersect) {
		double delx = p1.x - p0.x;
		double dely = p1.y - p0.y;

		double numerator = delx * (wp.x - p0.x) + dely * (wp.y - p0.y);
		double denominator = delx * delx + dely * dely;
		double t = numerator / denominator;
		pintersect.x = p0.x + t * delx;
		pintersect.y = p0.y + t * dely;

		if (t < 0.0) { // intersection not on line, on p0 side
			return p0.distance(wp);
		} else if (t > 1.0) {// intersection not on line, on p1 side
			return p1.distance(wp);
		}
		// intersection on line
		return pintersect.distance(wp);
	}

	/**
	 * Given two points p0 and p1, imagine a line from p0 to p1. Take the line to be
	 * parameterized by parameter t so that at t = 0 we are at p0 and t = 1 we are
	 * at p1.
	 *
	 * @param p0         start point of main line
	 * @param p1         end point of main line
	 * @param wp         the point from which we drop a perpendicular to p0 -> p1
	 * @param pintersect the intersection point of the perpendicular and the line
	 *                   containing p0-p1. It may or may not actually be between p0
	 *                   and p1, as specified by the return argument.
	 * @return the value of the t parameter. If it is between 0 and 1 the
	 *         intersection is on the line. If t < 0 the intersection is on the
	 *         "infinite line" but not on p0->p1, it is on the p0 side. If t > 1 the
	 *         intersection is on the p1 side.
	 */
	public static double perpendicularIntersection(Point2D.Double p0, Point2D.Double p1, Point2D.Double wp,
			Point2D.Double pintersect) {
		double delx = p1.x - p0.x;
		double dely = p1.y - p0.y;

		double numerator = delx * (wp.x - p0.x) + dely * (wp.y - p0.y);
		double denominator = delx * delx + dely * dely;
		double t = numerator / denominator;
		pintersect.x = p0.x + t * delx;
		pintersect.y = p0.y + t * dely;
		return t;
	}

	/**
	 * Given two points p0 and p1, imagine a line from p0 to p1. Take the line to be
	 * parameterized by parameter t so that at t = 0 we are at p0 and t = 1 we are
	 * at p1.
	 *
	 * @param x1         x coordinate of one endpoint
	 * @param y1         y coordinate of one endpoint
	 * @param x2         x coordinate of other endpoint
	 * @param y2         y coordinate of other endpoint
	 * @param wp         the point from which we drop a perpendicular to p0 -> p1
	 * @param pintersect the intersection point of the perpendicular and the line
	 *                   containing p0-p1. It may or may not actually be between p0
	 *                   and p1, as specified by the return argument.
	 * @return the value of the t parameter. If it is between 0 and 1 the
	 *         intersection is on the line. If t < 0 the intersection is on the
	 *         "infinite line" but not on p0->p1, it is on the p0 side. If t > 1 the
	 *         intersection is on the p1 side.
	 */
	public static double perpendicularIntersection(double x1, double y1, double x2, double y2, Point2D.Double wp,
			Point2D.Double pintersect) {
		double delx = x2 - x1;
		double dely = y2 - y1;

		double numerator = delx * (wp.x - x1) + dely * (wp.y - y1);
		double denominator = delx * delx + dely * dely;
		double t = numerator / denominator;
		pintersect.x = x1 + t * delx;
		pintersect.y = y1 + t * dely;
		return t;
	}

	/**
	 * Given two points p0 and p1, imagine a line from p0 to p1. Take the line to be
	 * parameterized by parameter t so that at t = 0 we are at p0 and t = 1 we are
	 * at p1.
	 *
	 * @param p0 start point of main line
	 * @param p1 end point of main line
	 * @param wp the point from which we drop a perpendicular to p0 -> p1
	 * @return the value of the t parameter. If it is between 0 and 1 the
	 *         intersection is on the line. If t < 0 the intersection is on the
	 *         "infinite line" but not on p0->p1, it is on the p0 side. If t > 1 the
	 *         intersection is on the p1 side.
	 */
	public static double perpendicularIntersection(Point2D.Double p0, Point2D.Double p1, Point2D.Double wp) {
		double delx = p1.x - p0.x;
		double dely = p1.y - p0.y;

		double numerator = delx * (wp.x - p0.x) + dely * (wp.y - p0.y);
		double denominator = delx * delx + dely * dely;
		double t = numerator / denominator;
		return t;
	}

	/**
	 * Given an array of pixel points, this rearranges the array into a convex hull.
	 *
	 * @param points the array of pixel points.
	 * @return an index which is less than or equal to the size of the points array.
	 *         Use that many points as the convex hull. In other words, if
	 *         points.length = 10 and this returns 6, then points[0] through
	 *         points[5] will be the convex hull.
	 */
	public static int getConvexHull(Point points[]) {
		if (points == null) {
			return 0;
		}
		int len = points.length;
		if (len < 4) {
			return len;
		}

		Point2D.Double wp[] = new Point2D.Double[len];
		for (int i = 0; i < len; i++) {
			wp[i] = new Point2D.Double(points[i].x, points[i].y);
		}

		int n = getConvexHull(wp);
		for (int i = 0; i < len; i++) {
			points[i].setLocation(wp[i].x, wp[i].y);
		}

		return n;
	}

	/**
	 * Given an array of world points, this rearranges the array into a convex hull.
	 *
	 * @param points the array of world points.
	 * @return an index which is less than or equal to the size of the points array.
	 *         Use that many points as the convex hull. In other words, if
	 *         points.length = 10 and this returns 6, then points[0] through
	 *         points[5] will be the convex hull.
	 */
	public static int getConvexHull(Point2D.Double points[]) {

		if ((points == null) || (points.length < 1)) {
			return -1;
		}

		if (points.length < 3) {
			return points.length;
		}

		// /*
		// * PSEUDO CODE
		// let N = number of points
		// let points[N+1] = the array of points
		// swap points[1] with the point with the lowest y-coordinate
		// sort points by polar angle with points[1]
		//
		// # We want points[0] to be a sentinel point that will stop the loop.
		// let points[0] = points[N]
		//
		// # M will denote the number of points on the convex hull.
		// let M = 2
		// for i = 3 to N:
		// # Find next valid point on convex hull.
		// while ccw(points[M-1], points[M], points[i]) <= 0:
		// M -= 1
		//
		// # Swap points[i] to the correct place and update M.
		// M += 1
		// swap points[M] with points[i]
		//
		// */

		int np = points.length;

		/* now the graham scan -- first find point with min y */
		int min = 0;
		for (int i = 1; i < np; i++) {
			if (points[i].y < points[min].y) {
				min = i;
			}
			// break an unlikely tie
			if (points[i].y == points[min].y) {
				if (points[i].x < points[min].x) {
					min = i;
				}
			}
		}

		/* swap min with zeroth */
		Point2D.Double twp = points[0];
		points[0] = points[min];
		points[min] = twp;

		// sort base on polar angle wrt points[0]

		final Point2D.Double p0 = points[0];
		Comparator<Point2D.Double> comparator = new Comparator<>() {

			@Override
			public int compare(Point2D.Double wp1, Point2D.Double wp2) {
				Point2D.Double v1 = new Point2D.Double(wp1.x - p0.x, wp1.y - p0.y);
				Point2D.Double v2 = new Point2D.Double(wp2.x - p0.x, wp2.y - p0.y);
				double ang1 = Math.atan2(v1.y, v1.x);
				double ang2 = Math.atan2(v2.y, v2.x);
				if (ang1 < ang2) {
					return -1;
				}
				if (ang1 > ang2) {
					return 1;
				}
				return 0;
			}
		};

		Arrays.sort(points, comparator);

		int M = 2;
		for (int i = 3; i < np; i++) {
			while ((M > 0) && (ccw(points[M - 1], points[M], points[i]) <= 0.0)) {
				M--;
			}

			M++;

			twp = points[M];
			points[M] = points[i];
			points[i] = twp;
		}

		return M + 1;

	}

	// Three points are a counter-clockwise turn if ccw > 0, clockwise if
	// ccw < 0, and collinear if ccw = 0 because ccw is a determinant that
	// gives the signed area of the triangle formed by p1, p2, and p3.
	// Used by convex hull algorithm
	private static double ccw(Point2D.Double p1, Point2D.Double p2, Point2D.Double p3) {
		return (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x);
	}

	/**
	 * Sort an array with an index sort
	 *
	 * @param <T>
	 * @param a   the array to sort
	 * @param c   the comparator
	 * @return the index sorted array
	 */
	public static <T> int[] indexSort(final T[] a, final Comparator<? super T> c) {
		Integer indexArray[] = new Integer[a.length];
		for (int i = 0; i < indexArray.length; i++) {
			indexArray[i] = i;
		}

		Comparator<Integer> comparator = new Comparator<>() {

			@Override
			public int compare(Integer i1, Integer i2) {
				return c.compare(a[i1], a[i2]);
			}
		};

		Arrays.sort(indexArray, comparator);

		int iarray[] = new int[indexArray.length];
		for (int i = 0; i < indexArray.length; i++) {
			iarray[i] = indexArray[i];
		}
		return iarray;
	}

	/**
	 * Sort an array of doubles with an index sort
	 *
	 * @param a the array to sort
	 * @return the resulting index sorted array.
	 */
	@SuppressWarnings("unchecked")
	public static int[] indexSort(final double[] a) {
		Integer indexArray[] = new Integer[a.length];
		for (int i = 0; i < indexArray.length; i++) {
			indexArray[i] = i;
		}

		final Comparator cdouble = new Comparator() {

			@Override
			public int compare(Object o1, Object o2) {
				Double d1 = (Double) o1;
				Double d2 = (Double) o2;

				if (d1 < d2) {
					return -1;
				}
				if (d1 > d2) {
					return 1;
				}
				return 0;
			}
		};

		Comparator<Integer> comparator = new Comparator<>() {

			@Override
			public int compare(Integer i1, Integer i2) {
				return cdouble.compare(a[i1], a[i2]);
			}
		};

		Arrays.sort(indexArray, comparator);

		int iarray[] = new int[indexArray.length];
		for (int i = 0; i < indexArray.length; i++) {
			iarray[i] = indexArray[i];
		}
		return iarray;
	}

	/**
	 *
	 * @param p1 First endpoint of first segment
	 * @param p2 Second endpoint of first segment
	 * @param q1 First endpoint of second segment
	 * @param q2 Second endpoint of second segment
	 * @param u  Will hold intersection, or NaN s if no intersection
	 * @return <code>true</code> if intersection is found
	 */
	public static boolean segmentCrossing(Point2D.Double p1, Point2D.Double p2, Point2D.Double q1, Point2D.Double q2,
			Point2D.Double u) {
		u.x = Double.NaN;
		u.y = Double.NaN;

		double x1o = p1.x;
		double dx1 = p2.x - p1.x;

		double y1o = p1.y;
		double dy1 = p2.y - p1.y;

		double x2o = q1.x;
		double dx2 = q2.x - q1.x;

		double y2o = q1.y;
		double dy2 = q2.y - q1.y;

		double denom = dx2 * dy1 - dx1 * dy2;

		if (Math.abs(denom) > TINY) {
			// t is the "t parameter" for the segment 1 parameterization

			double t = (dy2 * x1o - dy2 * x2o - dx2 * y1o + dx2 * y2o) / denom;
			double s = (dy1 * x1o - dy1 * x2o - dx1 * y1o + dx1 * y2o) / denom;

			if ((t > 0) && (t < 1) && (s > 0) && (s < 1)) {
				u.x = x1o + dx1 * t;
				u.y = y1o + dy1 * t;
				return true;
			}
		}

		return false;

	}

	/**
	 * Does a segment intersect a rectangle
	 * 
	 * @param p1
	 * @param p2
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return <code>true</code> if it intersects
	 */
	public static boolean segmentCutsRectangle(Point2D.Double p1, Point2D.Double p2, double x, double y, double w,
			double h, Point2D.Double u1, Point2D.Double u2) {

		Point2D.Double q1 = new Point2D.Double();
		Point2D.Double q2 = new Point2D.Double();

		Point2D.Double u = new Point2D.Double();

		int count = 0;

		q1.setLocation(x, y);
		q2.setLocation(x, y + h);
		if (segmentCrossing(p1, p2, q1, q2, u)) {
			u1.setLocation(u);
			count = 1;
		}

		q2.setLocation(x + w, y);
		if (segmentCrossing(p1, p2, q1, q2, u)) {
			if (count == 0) {
				u1.setLocation(u);
				count = 1;
			} else {
				u2.setLocation(u);
				return true;
			}
		}

		q1.setLocation(x + w, y + h);
		q2.setLocation(x, y + h);
		if (segmentCrossing(p1, p2, q1, q2, u)) {
			if (count == 0) {
				u1.setLocation(u);
				count = 1;
			} else {
				u2.setLocation(u);
				return true;
			}
		}

		if (count == 0) {
			return false;
		}

		q2.setLocation(x + w, y);
		if (segmentCrossing(p1, p2, q1, q2, u)) {
			u2.setLocation(u);
			return true;
		}

		return false;
	}

	// main program for testing
	public static void main(String arg[]) {
//		public static boolean segmentCrossing(Point2D.Double p1, Point2D.Double p2,
//				Point2D.Double q1, Point2D.Double q2, Point2D.Double u) {

		Point2D.Double p1 = new Point2D.Double();
		Point2D.Double p2 = new Point2D.Double();
		Point2D.Double q1 = new Point2D.Double();
		Point2D.Double q2 = new Point2D.Double();
		Point2D.Double u1 = new Point2D.Double();
		Point2D.Double u2 = new Point2D.Double();

		p1.setLocation(2, 2);
		p2.setLocation(6, 6);

		double x = 3;
		double y = 2;
		double w = 2;
		double h = 2;

		boolean result = segmentCutsRectangle(p1, p2, x, y, w, h, u1, u2);
		System.out.println("done");

	}

}
