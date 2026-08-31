package edu.cnu.mdi.graphics.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

/**
 * Regression coverage for {@link WorldGraphicsUtils}'s sweep-angle helpers
 * ({@code signedSweepDeg}, {@code ccwSweepDeg}, {@code unwrapSweepDeg}),
 * which had zero prior test coverage.
 */
class WorldGraphicsUtilsSweepTest {

	private static final double EPS = 1.0e-9;
	private static final Point2D.Double ORIGIN = new Point2D.Double(0, 0);

	private static Point2D.Double p(double x, double y) {
		return new Point2D.Double(x, y);
	}

	@Test
	void signedSweepDegIsPositiveForCounterClockwiseRotation() {
		// From +x axis to +y axis is a 90 degree CCW turn.
		assertEquals(90.0, WorldGraphicsUtils.signedSweepDeg(ORIGIN, p(1, 0), p(0, 1)), EPS);
	}

	@Test
	void signedSweepDegIsNegativeForClockwiseRotation() {
		// From +x axis to -y axis is a 90 degree CW turn.
		assertEquals(-90.0, WorldGraphicsUtils.signedSweepDeg(ORIGIN, p(1, 0), p(0, -1)), EPS);
	}

	@Test
	void signedSweepDegOfIdenticalVectorsIsZero() {
		assertEquals(0.0, WorldGraphicsUtils.signedSweepDeg(ORIGIN, p(1, 0), p(1, 0)), EPS);
	}

	@Test
	void ccwSweepDegAlwaysReturnsANonNegativeAngle() {
		// The same clockwise 90 degree turn that signedSweepDeg reports as -90
		// must be reported as 270 by the CCW-only variant.
		assertEquals(270.0, WorldGraphicsUtils.ccwSweepDeg(ORIGIN, p(1, 0), p(0, -1)), EPS);
		assertEquals(90.0, WorldGraphicsUtils.ccwSweepDeg(ORIGIN, p(1, 0), p(0, 1)), EPS);
	}

	@Test
	void unwrapSweepDegContinuesPastThePositive180Boundary() {
		// Continuing a CCW sweep past 180 degrees: the raw signed measurement
		// wraps to -170, but the unwrapped angle should continue to 190.
		assertEquals(190.0, WorldGraphicsUtils.unwrapSweepDeg(170.0, -170.0), EPS);
	}

	@Test
	void unwrapSweepDegContinuesPastTheNegative180Boundary() {
		// Continuing a CW sweep past -180 degrees: the raw signed measurement
		// wraps to 170, but the unwrapped angle should continue to -190.
		assertEquals(-190.0, WorldGraphicsUtils.unwrapSweepDeg(-170.0, 170.0), EPS);
	}

	@Test
	void unwrapSweepDegIsStableForASmallStepAwayFromTheBoundary() {
		// A small, non-boundary-crossing step must not be perturbed.
		assertEquals(46.0, WorldGraphicsUtils.unwrapSweepDeg(45.0, 46.0), EPS);
	}

	@Test
	void unwrapSweepDegHandlesAPreviousAngleAlreadyFarOutsideOneRevolution() {
		// prevUnwrapped can itself already be several revolutions large (e.g.
		// 530 degrees == 170 degrees, wrapped); the method must normalize it
		// internally before computing the continuation.
		assertEquals(550.0, WorldGraphicsUtils.unwrapSweepDeg(530.0, -170.0), EPS);
	}
}
