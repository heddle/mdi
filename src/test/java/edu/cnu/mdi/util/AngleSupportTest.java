package edu.cnu.mdi.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.Point2D;


class AngleSupportTest {

    private static final double EPS = 1.0e-9;

    @Test
    void signedAngleDegReturnsExpectedOrientation() {
        Point2D.Double xAxis = new Point2D.Double(1, 0);
        Point2D.Double yAxis = new Point2D.Double(0, 1);

        assertEquals(90.0, AngleSupport.signedAngleDeg(xAxis, yAxis), EPS);
        assertEquals(-90.0, AngleSupport.signedAngleDeg(yAxis, xAxis), EPS);
    }

    @Test
    void ccwSweepMapsNegativeAnglesIntoZeroTo360Range() {
        Point2D.Double up = new Point2D.Double(0, 1);
        Point2D.Double right = new Point2D.Double(1, 0);

        assertEquals(270.0, AngleSupport.ccwSweepDeg(up, right), EPS);
        assertEquals(90.0, AngleSupport.ccwSweepDeg(right, up), EPS);
    }

    @Test
    void vectorHelpersComputeExpectedDeltas() {
        Point2D.Double center = new Point2D.Double(2.0, 3.0);
        Point2D.Double p = new Point2D.Double(6.5, -1.0);

        Point2D.Double vec = AngleSupport.vec(center, p);
        Point2D.Double flipped = AngleSupport.vecFlipY(center, p);

        assertEquals(4.5, vec.x, EPS);
        assertEquals(-4.0, vec.y, EPS);
        assertEquals(4.5, flipped.x, EPS);
        assertEquals(4.0, flipped.y, EPS);
    }
    
    @Test
    void signedAngleDegHandlesColinearVectors() {
        Point2D.Double a = new Point2D.Double(1, 0);
        Point2D.Double b = new Point2D.Double(2, 0);

        assertEquals(0.0, AngleSupport.signedAngleDeg(a, b), EPS);
    }
}
