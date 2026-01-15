package edu.cnu.mdi.util;

import java.awt.geom.Point2D;

public class MathUtils {


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

}
