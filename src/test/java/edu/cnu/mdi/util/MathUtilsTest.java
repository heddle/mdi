package edu.cnu.mdi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

class MathUtilsTest {

    private static final double EPS = 1.0e-9;

    @Test
    void perpendicularIntersectionHandlesPointsAcrossSegmentRegions() {
        Point2D.Double intersect = new Point2D.Double();

        double tMid = MathUtils.perpendicularIntersection(0, 0, 10, 0, new Point2D.Double(5, 4), intersect);
        assertEquals(0.5, tMid, EPS);
        assertEquals(5.0, intersect.x, EPS);
        assertEquals(0.0, intersect.y, EPS);

        double tBefore = MathUtils.perpendicularIntersection(0, 0, 10, 0, new Point2D.Double(-3, 2), intersect);
        assertEquals(-0.3, tBefore, EPS);

        double tAfter = MathUtils.perpendicularIntersection(0, 0, 10, 0, new Point2D.Double(14, -2), intersect);
        assertEquals(1.4, tAfter, EPS);
    }
}
